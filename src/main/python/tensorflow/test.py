import argparse
import socketserver
import singlepixel_pb2

import paho.mqtt.client as mqtt


def on_connect(client, userdata, flags, rc):
    print("Connected with result code " + str(rc))


def on_message(client, userdata, msg):
    reading = singlepixel_pb2.SinglePixelSensorReading()
    reading.ParseFromString(msg.payload)
    if reading.clear > 0.5:
        print(reading.sensor_id)


def main():
    parser = argparse.ArgumentParser(description='Send senor data over MQTT.')
    parser.add_argument('--host', action="store", dest="mqtt_host", default="localhost",
                        help='Location of the MQTT broker. Defaults to localhost.')
    parser.add_argument('--port', action="store", dest="mqtt_port", default=1883, type=int,
                        help='MQTT port for the broker. Defaults to 1883.')
    parser.add_argument('--topic', action="store", dest="mqtt_prefix", default="",
                        help="Base topic to broadcast on. Defaults to none.")
    args = parser.parse_args()

    # Create MQTT client
    client = mqtt.Client()
    client.on_connect = on_connect
    client.on_message = on_message
    client.connect(args.mqtt_host, args.mqtt_port, 60)

    # Subscribe to all messages
    client.subscribe(args.mqtt_prefix + "/#")

    # Start a background thread to handle the client
    #client.loop_start()

    while True:
        client.loop()


    print('Closing the MQTT client...')
    # Stop the client
    client.loop_stop()


if __name__ == "__main__":
    main()
