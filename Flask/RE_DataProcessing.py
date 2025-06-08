import requests
import time
import threading
from flask import Flask, jsonify, send_file, request
import paho.mqtt.client as mqtt



app = Flask(__name__)

SPRING_BOOT_URL_RULE_ENGINE = "http://localhost:8080/evaluate-rule" #Local

alarmLimit3200_Counter = 0
alarmLimit4200_Counter = 0
alarmLimit5100_Counter = 0


# Global variable to store MQTT data
mqtt_data = {
    "xTableCurrent": None,
    "yTableCurrent": None,
    "spindleCurrent": None,
    "drillingCondition1": None,
    "drillingCondition2": None,
    "drillingCondition3": None
}

# MQTT Callbacks
def on_message(client, userdata, msg):
    topic = msg.topic
    payload = msg.payload.decode()

    # Map topic to mqtt_data dictionary
    key_mapping = {
        "home/sensor/xTableCurrent": "xTableCurrent",
        "home/sensor/yTableCurrent": "yTableCurrent",
        "home/sensor/spindleCurrent": "spindleCurrent",
        "home/sensor/drillingCondition1": "drillingCondition1",
        "home/sensor/drillingCondition2": "drillingCondition2",
        "home/sensor/drillingCondition3": "drillingCondition3"
    }

    if topic in key_mapping:
        mqtt_data[key_mapping[topic]] = payload
        print(f"Updated {key_mapping[topic]} to {payload}")

def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print("Connected successfully!")
        topics = [
            "home/sensor/xTableCurrent",
            "home/sensor/yTableCurrent",
            "home/sensor/spindleCurrent",
            "home/sensor/drillingCondition1",
            "home/sensor/drillingCondition2",
            "home/sensor/drillingCondition3"
        ]
        for topic in topics:
            client.subscribe(topic)
    else:
        print(f"Connection failed with code {rc}")

def start_mqtt_client():
    client = mqtt.Client()
    client.on_connect = on_connect
    client.on_message = on_message

    broker_address = "broker.hivemq.com"
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
    payload = {
        "alarmLimit3200": 0.0,
        "alarmLimit4200": 0.0,
        "alarmLimit5100": 0.0,
        "spindleCurrent": mqtt_data.get("spindleCurrent"),
        "xTableCurrent": mqtt_data.get("xTableCurrent"),
        "yTableCurrent": mqtt_data.get("yTableCurrent"),
        "drillingCondition1": mqtt_data.get("drillingCondition1"),
        "drillingCondition2": mqtt_data.get("drillingCondition2"),
        "drillingCondition3": mqtt_data.get("drillingCondition3")
    }
    result = send_rule_to_endpoint(payload, SPRING_BOOT_URL_RULE_ENGINE)
    return result.get('returned_state', '')

if __name__ == "__main__":
    # Start MQTT client in background
    mqtt_thread = threading.Thread(target=start_mqtt_client)
    mqtt_thread.start()
    
    # Start Flask server
    app.run(host="0.0.0.0", port=5000)