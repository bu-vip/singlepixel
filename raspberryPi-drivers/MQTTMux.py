#!/usr/bin/python

import paho.mqtt.client as mqtt
import argparse
from SensorReader import SensorReader

import time

def on_connect(client, userdata, flags, rc):
    print("Connected with result code " + str(rc))

def on_message(client, userdata, msg):
    print(msg.topic+" "+str(msg.payload))

def arg_sensor_gain(string):
    value = int(string)
    if value in [1, 4, 16, 60]:
        return value
    else:
        msg = "%r is not a value in range [1, 4]" % string
        raise argparse.ArgumentTypeError(msg)

def arg_sensor_time(string):
    value = float(string)
    if (value < 2.4 or value > 612):
        msg = "%r is not a value in range [2.4, 612] ms" % string
        raise argparse.ArgumentTypeError(msg)
    return value

def main():

    parser = argparse.ArgumentParser(description='Send senor data over MQTT.')
    parser.add_argument('--host', action="store", dest="mqtt_host", default="localhost", help='Location of the MQTT broker. Defaults to localhost.')
    parser.add_argument('--port', action="store", dest="mqtt_port", default=1883, type=int, help='MQTT port for the broker. Defaults to 1883.')
    #parser.add_argument('--user', action="store", dest="mqtt_user", help="Username used to login to the broker.")
    #parser.add_argument('--pass', action="store", dest="mqtt_pass", help="Password used to login to the broker.")
    parser.add_argument('--topic', action="store", dest="mqtt_prefix", default="", help="Base topic to broadcast on. Defaults to none.")
    parser.add_argument('--time', action="store", dest="sensor_time", default=0xD5, type=arg_sensor_time, help="Integration time for the sensors, in milliseconds. Must be in range: [2.4, 612]. Defaults to 100.8ms")
    parser.add_argument('--gain', action="store", dest="sensor_gain", default=1, type=arg_sensor_gain, help="Gain for sensors. Must be one of: [1, 4, 16, 60] Defaults to 1.")
    args = parser.parse_args()

    # create mqtt client
    client = mqtt.Client()
    client.on_connect = on_connect
    client.on_message = on_message
    client.connect(args.mqtt_host, args.mqtt_port, 60)

    # Start a background thread to handle the client
    client.loop_start()

    # process sensor data
    try:
        print ("Initializing sensors")
        # create and init sensor reader
        sensor_reader = SensorReader(args.sensor_time, args.sensor_gain)
        sensor_reader.initialize()
        print ("Initialization complete")

        print ("Streaming data")
        while True:
            startTime = time.time()
            #read sensor data
            data = sensor_reader.ReadSensors();
            # publish
            for muxId in range(0, len(data)):
                muxData = data[muxId]
                # non existant muxes will produce an empty list
                for sensorId in range(0, len(muxData)):
                    sensorData = muxData[sensorId]
                    # check there is sensor data
                    # non existant sensors will produce an empty list
                    if len(sensorData) == 6:
                        # topic for data: <prefix>/group/<group-id>/sensor/<sensor-id>
                        topic = args.mqtt_prefix + "/group/" + str(muxId) + "/sensor/" + str(sensorId)
                        # payload is "R,G,B,W,t1,t2"
                        payload = str(sensorData[0]) + ", " + str(sensorData[1]) + ", " + str(sensorData[2])
                        payload += ", " + str(sensorData[3]) + ", " + str(sensorData[4]) + ", " + str(sensorData[5])
                        client.publish(topic, payload)
            # calculate the time we need to sleep to prevent over-polling the sensors
            endTime = time.time()
            duration = endTime - startTime
            timeLeft = (args.sensor_time / 1000.0) - duration
            if timeLeft > 0:
                time.sleep(timeLeft)
            else:
                # Stop if we can't poll fast enough, no point in continuing
                print ("Can't poll sensors fast enough. This is normally caused by a really short integration time.")
                break

    except KeyboardInterrupt:
        print ('Stopping...')

    print ('Closing the MQTT client...')
    # Stop the client
    client.loop_stop()

    print ('Done')

if __name__=="__main__":
	main()
