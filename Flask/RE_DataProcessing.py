import requests
import time
import threading
from flask import Flask, jsonify
import paho.mqtt.client as mqtt
from influxdb_client import InfluxDBClient
import json

app = Flask(__name__)

SPRING_BOOT_URL_RULE_ENGINE = "http://localhost:8080/evaluate-rule"  # Local

mqtt_data = {
    "xTableCurrent": None,
    "yTableCurrent": None,
    # "drillingCondition1": None, #This data only from influx DB
}

alarmCondition3200 = 0.
alarmCondition4200 = 0
alarmCondition3200_counter = 0
alarmCondition4200_counter = 0 


def Exec_Influx(url, token, org, query):
    client = InfluxDBClient(url=url, token=token, org=org)
    result = client.query_api().query(org=org, query=query)

    return result

def fetch_and_append_results(url, token, org, queries):
    output = []

    def append_results(result):
        for table in result:
            for record in table.records:
                output.append(record.get_value())

    for query in queries:
        result = Exec_Influx(url, token, org, query)
        append_results(result)

    return output

def get_XY_Axis():
    # ------------------ CONFIG -------------------
    url = "http://110.25.101.191:8152"  # for X & Y axis (AI2)
    token = "Y1WTN4GKMW_-iZvdGUu9kIZjeyEosIjnz0TKhpqgBwJSxuV-pphsqpDRXRmsYGbSz0GenMR6lkwtP7EjIHrA2w=="
    org = "MTS"  # Replace with your actual org name
    bucket_XYaxis = "Table_Current_from_DAQ"
    bucket_SpindlePS = "spindle_physical_signal"
    # ---------------------------------------------

    #夾針異常/Abnormal Needle Clamping
    query_ANC = f'''
    from(bucket: "{bucket_SpindlePS}")
    |> range(start: -1h)
    |> filter(fn: (r) => r["_measurement"] == "ND61")
    |> filter(fn: (r) => r["channel"] == "spindle1")
    |> filter(fn: (r) => r["frequency_range"] == "1X")
    |> filter(fn: (r) => r["_field"] == "max_mag")
    |> last()
    '''

    queries = query_ANC
    output = fetch_and_append_results(url, token, org, queries)

    return output

def get_Z_Axis():
    # ------------------ CONFIG -------------------
    url = "http://110.25.101.191:7864/"  # for X & Y axis (AI2)
    token = "UzY7nTlbdyhzpT9ky4gkODDUkk2_MmiibfJmYuayTqM9Cvw2mJYe25htNGP0w-jDdoAW6I03uZdGnq5T4qX-0g=="
    org = "MTS"  # Replace with your actual org name
    bucket = "MTL Spidle"
    # ---------------------------------------------


    # Replace Grafana variables with fixed values
    query = f'''
    from(bucket: "{bucket}")
    |> range(start: -1h)
    |> filter(fn: (r) => r["_measurement"] == "ND61")
    |> filter(fn: (r) => r["datatype"] == "current")
    |> filter(fn: (r) => r["_field"] == "spindle2")
    |> last()
    '''

    result = Exec_Influx(url, token, org, query)

    # Print results
    for table in result:
        for record in table.records:
            output = record.get_value()

    return output

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

        # elif topic == "spindle1/1X":
        #     max_mag = data.get("max_mag")
        #     max_freq = data.get("max_freq")
        #     timestamp = data.get("timestamp")
        #     # print(f"Spindle1/1X - Max Mag: {max_mag}, Max Freq: {max_freq}, Timestamp: {timestamp}")

        #     # Save to global mqtt_data
        #     mqtt_data["drillingCondition1"] = max_mag

    except json.JSONDecodeError as e:
        print(f"Failed to decode JSON: {e}")

def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print("Connected successfully!")
        topics = [
            "TableCurrent"     # The topic for TableCurrent
            # "spindle1/1X"       # The topic for spindle1/1X
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
    spindleCurrent = get_Z_Axis()
    drillingCondition1 = get_XY_Axis()

    if(xTableCurerrent < 1 and yTableCurrent < 1 and spindleCurrent < 1):
        alarmCondition3200_counter += 1

        if alarmCondition3200_counter >= 120:
            alarmCondition3200 = 1.0
    else:
        alarmCondition3200_counter = 0
        alarmCondition3200 = 0.0

    if(drillingCondition1 < 1):
        alarmCondition4200_counter += 1

        if alarmCondition3200_counter >= 120:
            alarmCondition4200 = 1.0

    payload = {
        "xTableCurrent": xTableCurerrent,
        "yTableCurrent": yTableCurrent,
        "spindleCurrent": spindleCurrent,
        "drillingCondition1": drillingCondition1,
        "alarmCondition3200": alarmCondition3200,
        "alarmCondition4200": alarmCondition4200
    }
    print("Sending payload:", payload)
    result = send_rule_to_endpoint(payload, SPRING_BOOT_URL_RULE_ENGINE)
    return result.get('returned_state', '')


@app.route('/get-mqtt', methods=['GET'])
def get_data():
     # Read latest MQTT data

    xTableCurerrent = mqtt_data.get("xTableCurrent")
    yTableCurrent = mqtt_data.get("yTableCurrent")
    spindleCurrent = get_Z_Axis()
    drillingCondition1 = get_XY_Axis()

    if(xTableCurerrent < 1 and yTableCurrent < 1 and spindleCurrent < 1):
        alarmCondition3200_counter += 1

        if alarmCondition3200_counter >= 120:
            alarmCondition3200 = 1.0
    else:
        alarmCondition3200_counter = 0
        alarmCondition3200 = 0.0

    if(drillingCondition1 < 1):
        alarmCondition4200_counter += 1

        if alarmCondition3200_counter >= 120:
            alarmCondition4200 = 1.0

    payload = {
        "xTableCurrent": xTableCurerrent,
        "yTableCurrent": yTableCurrent,
        "spindleCurrent": spindleCurrent,
        "drillingCondition1": drillingCondition1,
        "alarmCondition3200": alarmCondition3200,
        "alarmCondition4200": alarmCondition4200
    }
    return jsonify(payload)

if __name__ == "__main__":
    # Start MQTT client in background
    mqtt_thread = threading.Thread(target=start_mqtt_client)
    mqtt_thread.start()
    
    # Start Flask server
    app.run(host="0.0.0.0", port=5555)
