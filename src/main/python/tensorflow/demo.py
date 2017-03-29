import argparse
import json
import socketserver
from http.server import BaseHTTPRequestHandler

import numpy as np
import paho.mqtt.client as mqtt
import singlepixel_pb2
import tensorflow as tf
from tensorflow.contrib.learn import DNNRegressor

from read_session import readings_dict_to_features

state = {
    'bounds': {
      'minX': -3,
        'maxX': 1,
        'minY': -2,
        'maxY': 5,
    },
    'occupants': []
}

class StateHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == '/_/state':
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            response = json.dumps(state).encode("utf-8")
            self.wfile.write(response)
        else:
            self.send_response(404)

def main():
    parser = argparse.ArgumentParser(description='Send senor data over MQTT.')
    parser.add_argument('--host', action="store", dest="mqtt_host", default="localhost",
                        help='Location of the MQTT broker. Defaults to localhost.')
    parser.add_argument('--port', action="store", dest="mqtt_port", default=1883, type=int,
                        help='MQTT port for the broker. Defaults to 1883.')
    parser.add_argument('--topic', action="store", dest="mqtt_prefix", default="",
                        help="Base topic to broadcast on. Defaults to none.")
    args = parser.parse_args()

    feature_len = 11 * 4
    feature_columns = [tf.contrib.layers.real_valued_column("", dimension=feature_len)]

    model_dir = "./models/model_v2"
    hidden_units = [100, 100, 100]
    model_x_dir = model_dir + "_x"
    estimator_x = DNNRegressor(
        feature_columns=feature_columns,
        hidden_units=hidden_units,
        model_dir=model_x_dir
    )

    model_y_dir = model_dir + "_y"
    estimator_y = DNNRegressor(
        feature_columns=feature_columns,
        hidden_units=hidden_units,
        model_dir=model_y_dir
    )


    def on_connect(client, userdata, flags, rc):
        print("Connected with result code " + str(rc))

    last_readings = {}
    def on_message(client, userdata, msg):
        print(msg.topic + " " + str(msg.payload))
        reading = singlepixel_pb2.ParseFromString(msg.payload)
        key = str(reading.group_id) + "/" + str(reading.sensor_id)
        last_readings[key] = reading
        if len(last_readings) > 12:
            input = readings_dict_to_features(last_readings)
            pred_x = estimator_x.predict(x=input)
            pred_y = estimator_y.predict(x=input)
            print(pred_x, pred_y)
            state['occupants'] = [{
                'id': 1,
                'position': {
                    'x': pred_x,
                    'y': pred_y
                }
            }]
            last_readings = {}

    # Create MQTT client
    client = mqtt.Client()
    client.on_connect = on_connect
    client.on_message = on_message
    client.connect(args.mqtt_host, args.mqtt_port, 60)

    # Subscribe to all messages
    client.subscribe(args.mqtt_prefix + "/#")

    # Start a background thread to handle the client
    client.loop_start()

    # Start http server for frontend
    httpd = socketserver.TCPServer(("", 8080), StateHandler)
    httpd.serve_forever()

    print('Closing the MQTT client...')
    # Stop the client
    client.loop_stop()


if __name__ == "__main__":
    main()
