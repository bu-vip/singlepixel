# Reads MQTT data

import paho.mqtt.client as mqtt
import numpy as np
from gesture_identify import identify_gesture, interp_all
from csv_reader import collect_data

numOfCameras = 12

broker = "192.168.1.219" # IPv4 address, change
port = 1883 # port used
recordTime = 2
method = "diff"

global startTimestamp
global recording
global gesture_start
global gesture_data
global prev_value, interp_data
recording = False
gesture_start = False
gesture_data = [[] for j in range(numOfCameras)]
diff_stack = [[] for j in range(numOfCameras)]
prev_value = [0] * numOfCameras
threshold = 0.001 # above this average gestures should start
queueSize = 5
avgStartSize = 4

# The callback for when the client receives a CONNACK response from the server.
def start_callback(client, userdata, msg):
    global recording, startTimestamp, gesture_start
    msgLoad = msg.payload.decode('UTF-8')
    msgInfo = msgLoad.split(', ') # split by commas
    
    if gesture_start:
        startTimestamp = int(msgInfo[4])+float(int(msgInfo[5])/1000000)
        gesture_data = diff_stack # copy values of diff stack into gesture data
        if not recording:
            print ("Recording started", startTimestamp)
        gesture_start = False
        recording = True # start recording
    on_message(client, userdata, msg)

def check_end_callback(client, userdata, msg):
    global recording, startTimestamp, gesture_data, diff_stack, prev_value, interp_data
    msgLoad = msg.payload.decode('UTF-8')
    msgInfo = msgLoad.split(', ') # split by commas
    timestamp = int(msgInfo[4])+float(int(msgInfo[5])/1000000)
    on_message(client, userdata, msg)
    if recording:
        if ((timestamp - startTimestamp) % 60 >= recordTime):
            # code to record gestures
            for data in gesture_data:
                data.pop(0) # pop first element from data
            print(identify_gesture(gesture_data, interp_data))
            # reset gesture data
            gesture_data = [[] for j in range(numOfCameras)] # empty list
            diff_stack = [[] for j in range(numOfCameras)] # empty list
            prev_value = [0] * numOfCameras
            print ("recording done")
            recording = False

def on_connect(client, userdata, flags, rc):
    print("Connected with result code "+str(rc))

    # Subscribing in on_connect() means that if we lose the connection and
    # reconnect then subscriptions will be renewed.
    lastCameraGroupName="/group/"+str(int((numOfCameras-1)/8))+"/sensor/"+str((numOfCameras-1)%8)
    client.message_callback_add("/group/0/sensor/0", start_callback) #callback for first camera
    client.message_callback_add(lastCameraGroupName, check_end_callback)
    client.subscribe("/group/+/sensor/+") # message for all clients

# The callback for when a PUBLISH message is received from the server.
def on_message(client, userdata, msg):
    global recording, gesture_data, diff_stack, prev_value, gesture_start
    splitTopic = msg.topic.split("/") # split by slashes
    msgLoad = msg.payload.decode('UTF-8')
    msgInfo = msgLoad.split(', ') # split by commas
    whiteValue = float(msgInfo[3])
    timestamp = int(msgInfo[4])+float(int(msgInfo[5])/1000000)
    cameraID = 8*int(splitTopic[2])+int(splitTopic[4])
    if method == "diff":
        if len(diff_stack[cameraID]) == 0:
            diff_stack[cameraID].append(0) # set all initial values to 0
        else:
            diff_stack[cameraID].append(whiteValue-prev_value[cameraID])
            if recording:
                gesture_data[cameraID].append(whiteValue-prev_value[cameraID])
        prev_value[cameraID] = whiteValue
        if not recording:
            if len(diff_stack[cameraID]) > queueSize:
                diff_stack[cameraID].pop(0) # remove earlier element
                if abs(np.average(diff_stack[cameraID][-1*abs(avgStartSize):])) > threshold:
                    # gesture started
                    gesture_start = True

def main():
    global interp_data
    client = mqtt.Client()
    client.on_connect = on_connect
    client.on_message = on_message

    identification_dict, missing = collect_data(numOfCameras = numOfCameras, method=method)
    interp_data = interp_all(identification_dict)

    client.connect(broker, port, 60)

    # Blocking call that processes network traffic, dispatches callbacks and
    # handles reconnecting.
    # Other loop*() functions are available that give a threaded interface and a
    # manual interface.
    client.loop_forever()

if __name__ == "__main__":
    main()
