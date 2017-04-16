#!/bin/bash

# check for root
if [ "$EUID" -ne 0 ]
  then echo "Please run as root"
  exit
fi

# install pip
apt-get install python-pip

# insall the mqtt client
pip install paho-mqtt
pip install protobuf
