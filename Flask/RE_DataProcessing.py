import requests
import time
import threading
from flask import Flask, jsonify
import paho.mqtt.client as mqtt
import json

app = Flask(__name__)

SPRING_BOOT_URL_RULE_ENGINE = "http://localhost:8080/evaluate-rule"  # Local

mqtt_data = {
    "xTableCurrent": None,
    "yTableCurrent": None,
    "ANC": None,
    "BWO": None,
    "NCS": None
}



# MQTT Callbacks
def on_message(client, userdata, msg):
    topic = msg.topic
    payload = msg.payload.decode()

    try:
        data = json.loads(payload)

        if topic == "TableCurrent":
            x_axil = data.get("X_Axil")
            y_axil = data.get("Y_Axil")
            print(f"TableCurrent - X_Axil: {x_axil}, Y_Axil: {y_axil}")

            # Save to global mqtt_data
            mqtt_data["xTableCurrent"] = x_axil
            mqtt_data["yTableCurrent"] = y_axil


        elif topic == "spindle1/1X":
            max_mag = data.get("max_mag")
            # timestamp = data.get("timestamp")
            # print(f"Spindle Frequency - Max Mag: {max_mag},  Timestamp: {timestamp}")

            # Save to global mqtt_data
            mqtt_data["ANC"] = max_mag

        elif topic == "spindle1/2X":
            max_mag = data.get("max_mag")
            # timestamp = data.get("timestamp")
            # print(f"Bits Wear-out - Max Mag: {max_mag}, Timestamp: {timestamp}")
            mqtt_data["BWO"] = max_mag

        elif topic == "spindle1/3X":
            max_mag = data.get("max_mag")
            # timestamp = data.get("timestamp")
            # print(f"Spindle Stiffness - Max Mag: {max_mag}, Timestamp: {timestamp}")
            mqtt_data["SS"] = max_mag

        elif topic == "spindle1/0.35X-0.45X":
            max_mag = data.get("max_mag")
            # timestamp = data.get("timestamp")
            # print(f"Change Bit Signal - Max Mag: {max_mag}, Timestamp: {timestamp}")
            mqtt_data["NCS"] = max_mag

        #No spindle Current yet

            

    except json.JSONDecodeError as e:
        print(f"Failed to decode JSON: {e}")

def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print("Connected successfully!")
        topics = [
            "TableCurrent",     #topic for TableCurrent
            "spindle1/1X",       #topic for spindle1/1X
            "spindle1/2X" ,      #topic for spindle1/2X
            "spindle1/3X"  ,     #topic for spindle1/3X
            "spindle1/0.35X-0.45X" #topic for spindle1//0.35X-0.45X
        ]
        for topic in topics:
            client.subscribe(topic)
    else:
        print(f"Connection failed with code {rc}")

def start_mqtt_client():
    client = mqtt.Client()
    client.on_connect = on_connect
    client.on_message = on_message

    broker_address = "localhost"
    port = 1883

    max_retries = 5
    retry_delay = 5

    for attempt in range(1, max_retries + 1):
        try:
            print(f"Attempt {attempt}: Connecting to MQTT broker...")
            client.connect(broker_address, port)
            break
        except Exception as e:
            print(f"Connection attempt {attempt} failed: {e}")
            if attempt < max_retries:
                print(f"Retrying in {retry_delay} seconds...")
                time.sleep(retry_delay)
            else:
                print("Max retries reached. Could not connect to broker.")
                exit(1)

    client.loop_start()  # Start loop in a separate thread

# Function to send a rule to the endpoint
def send_rule_to_endpoint(rule_data, endpoint_url):
    payload = {k: v for k, v in rule_data.items() if k != 'rule'}
    try:
        response = requests.post(endpoint_url, json=payload)
        response_data = response.json() if response.content else {}
        return {
            'success': True,
            'status_code': response.status_code,
            'response': response_data,
            'payload': payload,
            'returned_state': response_data.get('state', '')
        }
    except Exception as e:
        return {
            'success': False,
            'error': str(e),
            'payload': payload,
            'returned_state': ''
        }

@app.route('/run-rule-engine', methods=['GET'])
def reasoning():

    payload = {
        "xTableCurrent": mqtt_data.get("xTableCurrent"),
        "yTableCurrent": mqtt_data.get("yTableCurrent"),
        "Spindle Frequency": mqtt_data.get("ANC"),
        "Bits Wear Out": mqtt_data.get("BWO"),
        "Spindle Stiffness": mqtt_data.get("SS"),
        "Bits Change Signal": mqtt_data.get("NCS")
    }
    print("Sending payload:", payload)
    result = send_rule_to_endpoint(payload, SPRING_BOOT_URL_RULE_ENGINE)
    return result.get('returned_state', '')


@app.route('/get-mqtt', methods=['GET'])
def get_data():
     # Read latest MQTT data



   payload = {
        "xTableCurrent": mqtt_data.get("xTableCurrent"),
        "yTableCurrent": mqtt_data.get("yTableCurrent"),
        "Spindle Frequency": mqtt_data.get("ANC"),
        "Bits Wear Out": mqtt_data.get("BWO"),
        "Spindle Stiffness": mqtt_data.get("SS"),
        "Bits Change Signal": mqtt_data.get("NCS")
    }
   
   return jsonify(payload)

if __name__ == "__main__":
    # Start MQTT client in background
    mqtt_thread = threading.Thread(target=start_mqtt_client)
    mqtt_thread.start()
    
    # Start Flask server
    app.run(host="0.0.0.0", port=5555)
