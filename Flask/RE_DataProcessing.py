import requests
import time
from datetime import datetime
import threading
from flask import Flask, jsonify
import paho.mqtt.client as mqtt
import json
from collections import deque
from pymongo import MongoClient


app = Flask(__name__)

SPRING_BOOT_URL_RULE_ENGINE = "http://localhost:8080/evaluate-rule"  # Local

reset_time = 5# reset time for X, Y and Z current fluctuation filter

# ---- MQTT structure ----
mqtt_data = {
   "xTableCurrent": None,
    "yTableCurrent": None,
    "xTableCurrent_max": None,
    "yTableCurrent_max": None,
    "last_reset_time": time.time(),
    **{
        f"spindle{i}": {
            "ANC_window": deque(maxlen=10),
            "BWO_window": deque(maxlen=10),
            "SS_window": deque(maxlen=10),
            "NCS_window": deque(maxlen=10),
            "PH_window": deque(maxlen=10),
            "SF_window": deque(maxlen=10),
            "ANC": None,
            "BWO": None,
            "SS": None,
            "NCS": None,
            "PH": None,
            "SF": None,
            "xTableCurrent": None,
            "yTableCurrent": None
        } for i in range(1, 7)
    }
}


# ---- MongoDB Setup ----
MACHINE_NAME = "ND61"
MONGO_URI = "mongodb://localhost:27017/"
DB_NAME = MACHINE_NAME
COLLECTION_NAME = "Reasoning"

client = MongoClient(MONGO_URI)
db = client[DB_NAME]
reasoning_collection = db[COLLECTION_NAME]




#---- Function to Store Reasoning Result ----
def store_reasoning_result(response_data):
    reasoning_collection.insert_one({
        "timeStamp": datetime.now().strftime("%Y-%m-%d %H:%M:%S.%f"),
        "reason": response_data["spindle"]
    })

# MQTT Callbacks
def on_message(client, userdata, msg):
    topic = msg.topic
    payload = msg.payload.decode()

    try:
        data = json.loads(payload)

        if topic == "TableCurrent":
            x_axil = data.get("X_Axil")
            y_axil = data.get("Y_Axil")

            mqtt_data["xTableCurrent"] = x_axil
            mqtt_data["yTableCurrent"] = y_axil

            # Use absolute for max tracking
            if mqtt_data["xTableCurrent_max"] is None:
                mqtt_data["xTableCurrent_max"] = abs(x_axil)
            if mqtt_data["yTableCurrent_max"] is None:
                mqtt_data["yTableCurrent_max"] = abs(y_axil)

            if abs(x_axil) > mqtt_data["xTableCurrent_max"]:
                mqtt_data["xTableCurrent_max"] = abs(x_axil)
            if abs(y_axil) > mqtt_data["yTableCurrent_max"]:
                mqtt_data["yTableCurrent_max"] = abs(y_axil)

            # Check if reset_time seconds have passed since last reset
            current_time = time.time()
            if current_time - mqtt_data["last_reset_time"] >= reset_time:
                mqtt_data["xTableCurrent_max"] = abs(x_axil)
                mqtt_data["yTableCurrent_max"] = abs(y_axil)
                mqtt_data["last_reset_time"] = current_time

            # Copy to all spindles
            for i in range(1, 7):
                mqtt_data[f"spindle{i}"]["xTableCurrent"] = abs(x_axil)
                mqtt_data[f"spindle{i}"]["yTableCurrent"] = abs(y_axil)
                mqtt_data[f"spindle{i}"]["xTableCurrent_max"] = mqtt_data["xTableCurrent_max"]
                mqtt_data[f"spindle{i}"]["yTableCurrent_max"] = mqtt_data["yTableCurrent_max"]


        else:
            # Handle spindle1/1X, spindle2/2X, etc.
            windowed_fields = {
                "1X": "ANC",
                "2X": "BWO",
                "3X": "SS",
                "0.35X-0.45X": "NCS",
                "450-550 Hz": "PH",
                "8-36 Hz": "SF"
            }

            for i in range(1, 7):
                prefix = f"spindle{i}/"
                if topic.startswith(prefix):
                    subtopic = topic.replace(prefix, "")
                    field_key = windowed_fields.get(subtopic)
                    if field_key:
                        value = data.get("max_mag")
                        if value is not None:
                            window_key = f"{field_key}_window"
                            mqtt_data[f"spindle{i}"][window_key].append(value)

                            # Compute moving average
                            values = mqtt_data[f"spindle{i}"][window_key]
                            mqtt_data[f"spindle{i}"][field_key] = sum(values) / len(values)
                    break

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
                f"spindle{i}/0.35X-0.45X",
                f"spindle{i}/450-550 Hz",
                f"spindle{i}/8-36 Hz"
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

    
# ---- MQTT Reasoning Loop (to run in background) ----
def reasoning_background_loop():
    while True:
        try:
            full_payload = mqtt_data  # Safe copy reference to the live MQTT data
            response = {
                "spindle": []
            }

            for i in range(1, 7):
                spindle_key = f"spindle{i}"
                spindle_data = full_payload[spindle_key]

                spindle_payload = {
                    "spindleId": f"Spindle{i}",
                    "xTableCurrent": spindle_data.get("xTableCurrent_max", "no_data"),
                    "yTableCurrent": spindle_data.get("yTableCurrent_max", "no_data"),
                    # "anc": spindle_data.get("ANC", "no_data"),  # Do not change this
                    "bwo": spindle_data.get("BWO", "no_data"),
                    "ss": spindle_data.get("SS", "no_data"),
                    "ncs": spindle_data.get("NCS", "no_data"),
                    "ph": spindle_data.get("PH", "no_data"),
                    "sf": spindle_data.get("ANC", "no_data"),  # Do not change this
                }

                result = send_to_reasoning_endpoint(spindle_payload, SPRING_BOOT_URL_RULE_ENGINE)

                response["spindle"].append({
                    "spindleId": spindle_payload["spindleId"],
                    "returned_state": result.get("returned_state", "no_response")
                })

            store_reasoning_result(response)
            time.sleep(0.1)

        except Exception as e:
            print(f"[Reasoning Loop Error] {e}")
            time.sleep(0.1)


# ---- Background thread launcher ----
def start_background_reasoning():
    thread = threading.Thread(target=reasoning_background_loop, daemon=True)
    thread.start()

# ---- Dummy route to show running ----
@app.route('/')
def index():
    return "MQTT Rule Engine Reasoning Background Process Running..."    


# ---- Get MQTT Data ----
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
            "yTableCurrent": spindle_data.get("yTableCurrent_max") if spindle_data.get("yTableCurrent_max") is not None else "no_data",
            "xTableCurrent": spindle_data.get("xTableCurrent_max") if spindle_data.get("xTableCurrent_max") is not None else "no_data",
            "anc": spindle_data.get("ANC") if spindle_data.get("ANC") is not None else "no_data",
            "bwo": spindle_data.get("BWO") if spindle_data.get("BWO") is not None else "no_data",
            "ss": spindle_data.get("SS") if spindle_data.get("SS") is not None else "no_data",
            "ncs": spindle_data.get("NCS") if spindle_data.get("NCS") is not None else "no_data",
            "ph": spindle_data.get("PH") if spindle_data.get("PH") is not None else "no_data",
            "sf": spindle_data.get("SF") if spindle_data.get("SF") is not None else "no_data"
        })

    return jsonify(response)

@app.route('/last-reasoning', methods=['GET'])
def get_latest_reasoning():
    try:
        # Connect to MongoDB
        client = MongoClient("mongodb://localhost:27017/")
        db = client["ND61"]
        collection = db["Reasoning"]

        # Get the latest document by timestamp
        latest_doc = collection.find_one(sort=[("timeStamp", -1)])

        if not latest_doc:
            return jsonify({"message": "No reasoning data found"}), 404

        # Optional: remove MongoDB's _id field from response
        latest_doc.pop("_id", None)

        return jsonify(latest_doc), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500
    
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
            "yTableCurrent": spindle_data.get("yTableCurrent_max") if spindle_data.get("yTableCurrent_max") is not None else "no_data",
            "xTableCurrent": spindle_data.get("xTableCurrent_max") if spindle_data.get("xTableCurrent_max") is not None else "no_data",
            # "anc": spindle_data.get("ANC") if spindle_data.get("ANC") is not None else "no_data",
            "bwo": spindle_data.get("BWO") if spindle_data.get("BWO") is not None else "no_data",
            "ss": spindle_data.get("SS") if spindle_data.get("SS") is not None else "no_data",
            "ncs": spindle_data.get("NCS") if spindle_data.get("NCS") is not None else "no_data",
            "ph": spindle_data.get("PH") if spindle_data.get("PH") is not None else "no_data",
            "sf": spindle_data.get("ANC") if spindle_data.get("ANC") is not None else "no_data"
        }


        # Send this payload to the Spring Boot endpoint
        result = send_to_reasoning_endpoint(payload, SPRING_BOOT_URL_RULE_ENGINE)

        # Append result summary for this spindle
        response["spindle"].append({
            "spindleId": payload["spindleId"],
            "returned_state": result.get("returned_state", "no_response")
        })

    return jsonify(response)


if __name__ == "__main__":
    # Start MQTT client in background
    mqtt_thread = threading.Thread(target=start_mqtt_client)
    mqtt_thread.start()
    
    # Start background reasoning
    start_background_reasoning()

    # Start Flask server
    app.run(host="0.0.0.0", port=5555)
