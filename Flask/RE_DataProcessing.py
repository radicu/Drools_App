import requests
import time
import threading
from flask import Flask, jsonify
import paho.mqtt.client as mqtt
import json

app = Flask(__name__)

SPRING_BOOT_URL_RULE_ENGINE = "http://localhost:8080/evaluate-rule"  # Local

mqtt_data = {
    f"spindle{i}": {
        "yTableCurrent": None,
        "ANC": None,
        "BWO": None,
        "SS": None,
        "NCS": None
    }
    for i in range(1, 7)
}



# MQTT Callbacks
def on_message(client, userdata, msg):
    topic = msg.topic
    payload = msg.payload.decode()

    try:
        data = json.loads(payload)

    
        for i in range(1, 7):
            prefix = f"spindle{i}/"
            if topic.startswith(prefix):
                subtopic = topic.replace(prefix, "")

                if subtopic == "1X":
                    mqtt_data[f"spindle{i}"]["ANC"] = data.get("max_mag")
                elif subtopic == "2X":
                    mqtt_data[f"spindle{i}"]["BWO"] = data.get("max_mag")
                elif subtopic == "3X":
                    mqtt_data[f"spindle{i}"]["SS"] = data.get("max_mag")
                elif subtopic == "0.35X-0.45X":
                    mqtt_data[f"spindle{i}"]["NCS"] = data.get("max_mag")
                elif subtopic == "yTableCurrent":
                    mqtt_data[f"spindle{i}"]["yTableCurrent"] = data.get("Y_Axil")  # assuming structure matches
                break  # no need to check other spindles

    except json.JSONDecodeError as e:
        print(f"Failed to decode JSON: {e}")

def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print("Connected successfully!")
        topics = []

        for i in range(1, 7):
            topics.extend([
                f"spindle{i}/1X",
                f"spindle{i}/2X",
                f"spindle{i}/3X",
                f"spindle{i}/0.35X-0.45X",
                f"spindle{i}/yTableCurrent"  # optional if published
            ])

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
    response = {
        "spindle": []
    }

    for i in range(1, 7):
        spindle_key = f"spindle{i}"
        spindle_data = mqtt_data[spindle_key]

        response["spindle"].append({
            "spindle_id": f"Spindle{i}",
            "yTableCurrent": spindle_data.get("yTableCurrent") if spindle_data.get("yTableCurrent") is not None else "no_data",
            "Spindle Frequency": spindle_data.get("ANC") if spindle_data.get("ANC") is not None else "no_data",
            "Bits Wear Out": spindle_data.get("BWO") if spindle_data.get("BWO") is not None else "no_data",
            "Spindle Stiffness": spindle_data.get("SS") if spindle_data.get("SS") is not None else "no_data",
            "Bits Change Signal": spindle_data.get("NCS") if spindle_data.get("NCS") is not None else "no_data"
        })

    return jsonify(response)



if __name__ == "__main__":
    # Start MQTT client in background
    mqtt_thread = threading.Thread(target=start_mqtt_client)
    mqtt_thread.start()
    
    # Start Flask server
    app.run(host="0.0.0.0", port=5555)
