import requests
import time
import threading
from flask import Flask, jsonify
import paho.mqtt.client as mqtt
import json

app = Flask(__name__)

SPRING_BOOT_URL_RULE_ENGINE = "http://localhost:8080/evaluate-rule"  # Local

mqtt_data = {
    "read_counter": 0,
    "xTableCurrent": None,
    "yTableCurrent": None,
    "xTableCurrentmax": None,
    "yTableCurrentmax": None,
    **{
        f"spindle{i}": {
            "ANC": None,
            "BWO": None,
            "SS": None,
            "NCS": None,
            "xTableCurrent": None,
            "yTableCurrent": None,
            "xTableCurrentmax": None,
            "yTableCurrentmax": None
        } for i in range(1, 7)
    }
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

            # Update base values
            mqtt_data["xTableCurrent"] = x_axil
            mqtt_data["yTableCurrent"] = y_axil

            # Initialize max if None
            if mqtt_data["xTableCurrentmax"] is None:
                mqtt_data["xTableCurrentmax"] = x_axil
            if mqtt_data["yTableCurrentmax"] is None:
                mqtt_data["yTableCurrentmax"] = y_axil

            # Update max values
            if abs(x_axil) > abs(mqtt_data["xTableCurrentmax"]):
                mqtt_data["xTableCurrentmax"] = x_axil
            if abs(y_axil) > abs(mqtt_data["yTableCurrentmax"]):
                mqtt_data["yTableCurrentmax"] = y_axil

            # Increment and reset counter if needed
            mqtt_data["read_counter"] += 1
            if mqtt_data["read_counter"] >= 10:
                mqtt_data["read_counter"] = 0
                mqtt_data["xTableCurrentmax"] = x_axil
                mqtt_data["yTableCurrentmax"] = y_axil

            # Copy to all spindles
            for i in range(1, 7):
                mqtt_data[f"spindle{i}"]["xTableCurrent"] = x_axil
                mqtt_data[f"spindle{i}"]["yTableCurrent"] = y_axil
                mqtt_data[f"spindle{i}"]["xTableCurrentmax"] = mqtt_data["xTableCurrentmax"]
                mqtt_data[f"spindle{i}"]["yTableCurrentmax"] = mqtt_data["yTableCurrentmax"]

        else:
            # Handle spindle1/1X, spindle2/2X, etc.
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
                    break  # Stop once matched

    except json.JSONDecodeError as e:
        print(f"Failed to decode JSON: {e}")


def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print("Connected successfully!")
        topics = ["TableCurrent"]  

        for i in range(1, 7):
            topics.extend([
                f"spindle{i}/1X",
                f"spindle{i}/2X",
                f"spindle{i}/3X",
                f"spindle{i}/0.35X-0.45X"
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
def send_to_reasoning_endpoint(rule_data, endpoint_url):
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
    response = {
        "spindle": []
    }

    for i in range(1, 7):
        spindle_key = f"spindle{i}"
        spindle_data = mqtt_data[spindle_key]

        # Build payload for this spindle only
        payload = {
            "spindleId": f"Spindle{i}",
            "yTableCurrentmax": abs(spindle_data["yTableCurrentmax"]) if spindle_data.get("yTableCurrentmax") is not None else "no_data",
            "xTableCurrentmax": abs(spindle_data["xTableCurrentmax"]) if spindle_data.get("xTableCurrentmax") is not None else "no_data",
            "anc": spindle_data.get("ANC") if spindle_data.get("ANC") is not None else "no_data",
            "bwo": spindle_data.get("BWO") if spindle_data.get("BWO") is not None else "no_data",
            "ss": spindle_data.get("SS") if spindle_data.get("SS") is not None else "no_data",
            "ncs": spindle_data.get("NCS") if spindle_data.get("NCS") is not None else "no_data"
        }

        # Send this payload to the Spring Boot endpoint
        result = send_to_reasoning_endpoint(payload, SPRING_BOOT_URL_RULE_ENGINE)

        # Append result summary for this spindle
        response["spindle"].append({
            "spindleId": payload["spindleId"],
            "returned_state": result.get("returned_state", "no_response")
        })

    return jsonify(response)



@app.route('/get-mqtt', methods=['GET'])
def get_data():
    response = {
        "spindle": []
    }

    for i in range(1, 7):
        spindle_key = f"spindle{i}"
        spindle_data = mqtt_data[spindle_key]

        response["spindle"].append({
            "spindleId": f"Spindle{i}",
            "yTableCurrent": spindle_data.get("yTableCurrent") if spindle_data.get("yTableCurrent") is not None else "no_data",
            "xTableCurrent": spindle_data.get("xTableCurrent") if spindle_data.get("xTableCurrent") is not None else "no_data",
            "anc": spindle_data.get("ANC") if spindle_data.get("ANC") is not None else "no_data",
            "bwo": spindle_data.get("BWO") if spindle_data.get("BWO") is not None else "no_data",
            "ss": spindle_data.get("SS") if spindle_data.get("SS") is not None else "no_data",
            "ncs": spindle_data.get("NCS") if spindle_data.get("NCS") is not None else "no_data"
        })

    return jsonify(response)



if __name__ == "__main__":
    # Start MQTT client in background
    mqtt_thread = threading.Thread(target=start_mqtt_client)
    mqtt_thread.start()
    
    # Start Flask server
    app.run(host="0.0.0.0", port=5555)
