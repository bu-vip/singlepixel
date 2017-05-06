# MultiKinect + SinglePixel
This directory contains a plugin for collecting single pixel sensor data with 
[MultiKinect](https://github.com/bu-vip/multikinect).

## Running
#### Requirements
* Mqtt broker, such as [ActiveMQ](http://activemq.apache.org/download.html)

First, start the mqtt broker:
```bash
./activemq console
```

ssh into the raspberry pi and run:
```bash
ssh pi@192.168.1.202
...
cd doug/singlepixellocalization/src/main/python/raspberrypidrivers/
python MQTTMux.py --host <broker-ip> --time 100 --gain 60
```

From the root of the repo, build and run:
> NOTE: make sure your data directory path is an absolute path (ie: /home/user/Desktop/multikinect_data)
```bash
bazel run //src/main/java/edu/bu/vip/singlepixel/multikinect:main -- --data_dir <directory-path>
```
For more information about the command line options, see the [MainArgs](MainArgs.java) class.

> Make sure you check for errors when starting the controller. If there is an error connecting to the sensors, it will appear in the console.

Next, start up the camera programs for each Kinect and connect them to the controller. For instructions on how to do this, see [here](https://github.com/bu-vip/multikinect/wiki/Usage#connecting-the-cameras).

Then navigate to [http://localhost:8080](http://localhost:8080) in your web browser.
