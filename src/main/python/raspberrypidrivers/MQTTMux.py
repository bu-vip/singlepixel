#!/usr/bin/python

import paho.mqtt.client as mqtt
import argparse
from SensorReader import SensorReader

import time


def on_connect(client, userdata, flags, rc):
    print("Connected with result code " + str(rc))


def on_message(client, userdata, msg):
    print(msg.topic + " " + str(msg.payload))


def arg_sensor_gain(string):
    value = int(string)
    if value in [1, 4, 16, 60]:
        return value
    else:
        msg = "%r is not a value in range [1, 4]" % string
        raise argparse.ArgumentTypeError(msg)


def arg_sensor_time(string):
    value = float(string)
    if value < 2.4 or value > 612:
        msg = "%r is not a value in range [2.4, 612] ms" % string
        raise argparse.ArgumentTypeError(msg)
    return value


def main():
    parser = argparse.ArgumentParser(description='Send senor data over MQTT.')
    parser.add_argument('--host', action="store", dest="mqtt_host", default="localhost",
                        help='Location of the MQTT broker. Defaults to localhost.')
    parser.add_argument('--port', action="store", dest="mqtt_port", default=1883, type=int,
                        help='MQTT port for the broker. Defaults to 1883.')
    # parser.add_argument('--user', action="store", dest="mqtt_user", help="Username used to login to the broker.")
    # parser.add_argument('--pass', action="store", dest="mqtt_pass", help="Password used to login to the broker.")
    parser.add_argument('--topic', action="store", dest="mqtt_prefix", default="",
                        help="Base topic to broadcast on. Defaults to none.")
    parser.add_argument('--time', action="store", dest="sensor_time", default=0xD5, type=arg_sensor_time,
                        help="""Integration time for the sensors, in milliseconds. Must be in range: [2.4, 612]. 
                        Defaults to 100.8ms""")
    parser.add_argument('--gain', action="store", dest="sensor_gain", default=1, type=arg_sensor_gain,
                        help="Gain for sensors. Must be one of: [1, 4, 16, 60] Defaults to 1.")
    parser.add_argument('--group', action="store", dest="group_id", default="0", help='Group id on MQTT.')
    args = parser.parse_args()

    # Create MQTT client
    client = mqtt.Client()
    client.on_connect = on_connect
    client.on_message = on_message
    client.connect(args.mqtt_host, args.mqtt_port, 60)

    # Start a background thread to handle the client
    client.loop_start()

    # Process sensor data
    try:
        print ("Initializing sensors")
        # Create and init sensor reader
        sensor_reader = SensorReader(args.sensor_time, args.sensor_gain)
        sensor_reader.initialize()
        print ("Initialization complete")

        print ("Streaming data")
        while True:
            start_time = time.time()
            # Get readings from all sensors
            data = sensor_reader.read_sensors()
            # Publish readings
            for mux_id in range(0, len(data)):
                mux_data = data[mux_id]
                # Missing multiplexers will produce an empty list of readings
                for sensor_id in range(0, len(mux_data)):
                    sensor_data = mux_data[sensor_id]
                    # Check for sensor data, missing sensor will produce a None reading
                    if sensor_data is not None:
                        # Topic for data: <prefix>/group/<group-id>/sensor/<sensor-id>
                        topic = args.mqtt_prefix
                        topic += "/group/" + str(args.group_id)
                        topic += "/sensor/" + str(mux_id * 8 + sensor_id)
                        # Payload is protobuf as binary
                        payload = sensor_data.SerializeToString()
                        client.publish(topic, payload)
            # Calculate the time we need to sleep to prevent over-polling the sensors
            end_time = time.time()
            duration = end_time - start_time
            time_left = (args.sensor_time / 1000.0) - duration
            if time_left > 0:
                time.sleep(time_left)
            else:
                # Warn if we can't poll fast enough, if this happens a lot probably should stop
                print ("Can't poll sensors fast enough. This is normally caused by a really short integration time.")

    except KeyboardInterrupt:
        print ('Stopping...')

    print ('Closing the MQTT client...')
    # Stop the client
    client.loop_stop()

    print ('Done')


if __name__ == "__main__":
    main()
