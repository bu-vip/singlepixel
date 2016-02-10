#!/usr/bin/python

import paho.mqtt.client as mqtt
from SensorReader import SensorReader

MQTT_HOST = "192.168.1.246"
MQTT_PORT = 1883
MQTT_USER = "admin"
MQTT_PASS = "password"
MQTT_PREFIX = ""

def on_connect(client, userdata, flags, rc):
    print("Connected with result code " + str(rc))

def on_message(client, userdata, msg):
    print(msg.topic+" "+str(msg.payload))

def main():
        client = mqtt.Client()
        client.on_connect = on_connect
        client.on_message = on_message
        client.connect(MQTT_HOST, MQTT_PORT, 60)

        # Start a background thread to handle the client
        client.loop_start()

        # process sensor data
        try:
            print ("Initializing sensors")
            # create and init sensor reader
            sensor_reader = SensorReader()
            sensor_reader.initialize()
            print ("Initialization complete")

            print ("Streaming data")
            while True:
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
                            topic = MQTT_PREFIX + "/group/" + str(muxId) + "/sensor/" + str(sensorId)
                            # payload is "R,G,B,W,t1,t2"
                            payload = str(sensorData[0]) + ", " + str(sensorData[1]) + ", " + str(sensorData[2])
                            payload += ", " + str(sensorData[3]) + ", " + str(sensorData[4]) + ", " + str(sensorData[5])
                            client.publish(topic, payload)
        except KeyboardInterrupt:
            print ('Stopping...')

        print ('Closing the MQTT client...')
        # Stop the client
        client.loop_stop()

        print ('Done')

if __name__=="__main__":
	main()
