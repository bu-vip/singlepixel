# multikinect + singlepixellocalization
This directory contains a plugin for collecting single pixel sensor data with 
[multikinect](https://github.com/bu-vip/multikinect).

## Running
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

From the root of the repo, run:
```bash
bazel run //src/main/java/edu/bu/vip/singlepixel/multikinect:main -- --data_dir <directory-path>
```
For more information about the command line options, see the [MainArgs](MainArgs.java) class.

Then navigate to [http://localhost:8080](http://localhost:8080) in your web browser.