import argparse

import numpy as np
import paho.mqtt.client as mqtt
import singlepixel_pb2
import tensorflow as tf
from tensorflow.contrib.learn import DNNRegressor


def reading_to_feature(reading):
    return np.array([reading.red, reading.green, reading.blue, reading.clear])


def readings_dict_to_features(dict):
    final = None
    for key in sorted(dict):
        converted = reading_to_feature(dict[key])
    if final is None:
        final = converted
    else:
        final = np.concatenate([final, converted])
    return final


def main():
    parser = argparse.ArgumentParser(description='Send senor data over MQTT.')
    parser.add_argument('--host', action="store", dest="mqtt_host", default="localhost",
                        help='Location of the MQTT broker. Defaults to localhost.')
    parser.add_argument('--port', action="store", dest="mqtt_port", default=1883, type=int,
                        help='MQTT port for the broker. Defaults to 1883.')
    parser.add_argument('--topic', action="store", dest="mqtt_prefix", default="",
                        help="Base topic to broadcast on. Defaults to none.")
    parser.add_argument('--group', action="store", dest="group_id", default="0", help='Group id on MQTT.')
    args = parser.parse_args()

    model_dir = "../tensorflow/models/model"
    feature_len = 11 * 4
    feature_columns = [tf.contrib.layers.real_valued_column("", dimension=feature_len)]

    hidden_units = [100, 100, 100]
    estimator_x = DNNRegressor(
        feature_columns=feature_columns,
        hidden_units=hidden_units,
        model_dir=model_dir + "_x"
    )
    estimator_y = DNNRegressor(
        feature_columns=feature_columns,
        hidden_units=hidden_units,
        model_dir=model_dir + "_y"
    )

    last_readings = {}

    def on_connect(client, userdata, flags, rc):
        print("Connected with result code " + str(rc))

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

    # Create MQTT client
    client = mqtt.Client()
    client.on_connect = on_connect
    client.on_message = on_message
    client.connect(args.mqtt_host, args.mqtt_port, 60)

    # Start a background thread to handle the client
    client.loop_start()

    try:
        while True:
            pass

    except KeyboardInterrupt:
        print('Stopping...')

    print('Closing the MQTT client...')
    # Stop the client
    client.loop_stop()


if __name__ == "__main__":
    main()
