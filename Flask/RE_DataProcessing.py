import requests
import time
import threading
from flask import Flask, jsonify, send_file, request
import paho.mqtt.client as mqtt
import json

app = Flask(__name__)

SPRING_BOOT_URL_RULE_ENGINE = "http://localhost:8080/evaluate-rule"  # Local

# Global variable to store MQTT data
mqtt_data = {
    "xTableCurrent": None,
    "yTableCurrent": None,
    "drillingCondition1": None,
}

alarmCondition3200 = 0.0
alarmCondition3200_counter = 0 

# MQTT Callbacks
def on_message(client, userdata, msg):
    topic = msg.topic
    payload = msg.payload.decode()

    try:
        data = json.loads(payload)

        if topic == "TableCurrent":
            x_axil = data.get("X_Axil")
            y_axil = data.get("Y_Axil")
            # print(f"TableCurrent - X_Axil: {x_axil}, Y_Axil: {y_axil}")

            # Save to global mqtt_data
            mqtt_data["xTableCurrent"] = x_axil
            mqtt_data["yTableCurrent"] = y_axil

        elif topic == "spindle1/1X":
            max_mag = data.get("max_mag")
            max_freq = data.get("max_freq")
            timestamp = data.get("timestamp")
            # print(f"Spindle1/1X - Max Mag: {max_mag}, Max Freq: {max_freq}, Timestamp: {timestamp}")

            # Save to global mqtt_data
            mqtt_data["drillingCondition1"] = max_mag

    except json.JSONDecodeError as e:
        print(f"Failed to decode JSON: {e}")

def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print("Connected successfully!")
        topics = [
            "TableCurrent",     # The topic for TableCurrent
            "spindle1/1X"       # The topic for spindle1/1X
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
    # Read latest MQTT data

    xTableCurerrent = mqtt_data.get("xTableCurrent")
    yTableCurrent = mqtt_data.get("yTableCurrent")

    if(xTableCurerrent < 1 and yTableCurrent < 1):
        alarmCondition3200_counter += 1

        if alarmCondition3200_counter > 120:
            alarmCondition3200 = 1.0
    else:
        alarmCondition3200_counter = 0
        alarmCondition3200 = 0.0

    payload = {
        "xTableCurrent": xTableCurerrent,
        "yTableCurrent": yTableCurrent,
        "drillingCondition1": mqtt_data.get("drillingCondition1"),
        "alarmCondition3200": alarmCondition3200
    }
    print("Sending payload:", payload)
    result = send_rule_to_endpoint(payload, SPRING_BOOT_URL_RULE_ENGINE)
    return result.get('returned_state', '')


@app.route('/get-mqtt', methods=['GET'])
def get_mqtt():
    # Read latest MQTT data

    xTableCurerrent = mqtt_data.get("xTableCurrent")
    yTableCurrent = mqtt_data.get("yTableCurrent")

    if(xTableCurerrent < 1 and yTableCurrent < 1):
        alarmCondition3200_counter += 1

        if alarmCondition3200_counter > 120:
            alarmCondition3200 = 1.0
    else:
        alarmCondition3200_counter = 0
        alarmCondition3200 = 0.0

    payload = {
        "xTableCurrent": xTableCurerrent,
        "yTableCurrent": yTableCurrent,
        "drillingCondition1": mqtt_data.get("drillingCondition1"),
        "alarmCondition3200": alarmCondition3200
    }
    return jsonify(payload)

if __name__ == "__main__":
    # Start MQTT client in background
    mqtt_thread = threading.Thread(target=start_mqtt_client)
    mqtt_thread.start()
    
    # Start Flask server
    app.run(host="0.0.0.0", port=5555)
