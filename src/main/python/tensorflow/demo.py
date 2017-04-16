import argparse
import json
import socketserver
from http.server import BaseHTTPRequestHandler

import paho.mqtt.client as mqtt
import singlepixel_pb2
import tensorflow as tf
from read_session import readings_dict_to_features
from tensorflow.contrib.learn import DNNRegressor
import numpy as np

import time

current_milli_time = lambda: int(round(time.time() * 1000))

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


class Estimator:
    def __init__(self, num_features, model_dir, hidden_units):
        feature_columns = [tf.contrib.layers.real_valued_column("", dimension=num_features)]
        model_x_dir = model_dir + "_x"
        self.estimator_x = DNNRegressor(
            feature_columns=feature_columns,
            hidden_units=hidden_units,
            model_dir=model_x_dir
        )

        model_y_dir = model_dir + "_y"
        self.estimator_y = DNNRegressor(
            feature_columns=feature_columns,
            hidden_units=hidden_units,
            model_dir=model_y_dir
        )

        def input_fn():
            x = tf.constant(np.zeros([1, num_features]))
            y = tf.constant(np.zeros([1, 1]))
            return x, y

        self.estimator_x.fit(input_fn=input_fn, steps=0)
        self.estimator_y.fit(input_fn=input_fn, steps=0)

        self.last_readings = {}
        self.last_predict_time = 0

    def add_reading(self, reading):
        key = str(reading.group_id) + "/" + str(reading.sensor_id)
        self.last_readings[key] = reading
        current_time = current_milli_time()
        predict_delta = current_time - self.last_predict_time
        if len(self.last_readings) >= 11 and predict_delta > 10000:
            self.last_predict_time = current_time
            input = np.array([readings_dict_to_features(self.last_readings)])
            input = input.astype(np.float32)

            def predict_input_fn():
                x = tf.constant(input)
                return x

            pred_x = self.estimator_x.predict(input_fn=predict_input_fn)
            pred_y = self.estimator_y.predict(input_fn=predict_input_fn)
            for x, y in zip(pred_x, pred_y):
                print(input, x, y)
                state['occupants'] = [{
                    'id': 1,
                    'position': {
                        'x': float(x),
                        'y': float(y)
                    }
                }]



def on_connect(client, userdata, flags, rc):
    print("Connected with result code " + str(rc))


def on_message(client, userdata, msg):
    reading = singlepixel_pb2.SinglePixelSensorReading()
    reading.ParseFromString(msg.payload)
    userdata.add_reading(reading)


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
    model_dir = "./models/model_v2"
    hidden_units = [100, 100, 100]
    estimator = Estimator(feature_len, model_dir, hidden_units)

    # Create MQTT client
    client = mqtt.Client(userdata=estimator)
    client.on_connect = on_connect
    client.on_message = on_message
    client.connect(args.mqtt_host, args.mqtt_port, 60)

    # Subscribe to all messages
    client.subscribe(args.mqtt_prefix + "/#")

    # Start a background thread to handle the client
    #client.loop_start()

    # Start http server for frontend
    httpd = socketserver.TCPServer(("", 8080), StateHandler)
    httpd.timeout = 0.1

    while True:
        client.loop()
    #    httpd.handle_request()

    httpd.shutdown()
    httpd.server_close()

    print('Closing the MQTT client...')
    # Stop the client
    client.loop_stop()


if __name__ == "__main__":
    main()
