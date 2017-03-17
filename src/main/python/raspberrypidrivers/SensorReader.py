from time import sleep
import time
from Adafruit_TCS34725 import TCS34725
import Adafruit_I2C
import smbus
time.ctime()

import subprocess
import os


class SensorReader:
    FIRST_MUX_ADDRESS = 0x70 #MUX Address
    LAST_MUX_ADDRESS = 0x77
    DEVICE_REG_MODE1 = 0x00
    DEVICE_REG_LEDOUT0 = 0x1d
    COLOR_SENSOR_ADDRESS = 0x29 #Color sensor address

    def __init__(self,  aIntegrationTime, aGain):
        self.muxes = []
        self.bus = smbus.SMBus(1)
        self.integration_time_ms = aIntegrationTime;
        if (aIntegrationTime < 2.4 or aIntegrationTime > 612):
            raise Error("Integration time not in range: [2.4, 612]")
        self.integration_time_int = int(256 - aIntegrationTime / 2.4);
        self.gain = aGain;
        if aGain == 1:
            self.gain_index = 0
        elif aGain == 4:
            self.gain_index = 1
        elif aGain == 16:
            self.gain_index = 2
        elif aGain == 60:
            self.gain_index = 3
        else:
            raise Error("Gain not equal to: [1, 4, 16, 60]")
        # calculate the max value the senors will return for the given integration time
        self.max_sensor_val = min((256 - self.integration_time_int) * 1024, 65535)

    def initialize(self):
        #Initialization routine
        self.ResetChannels()
        self.muxes = self.i2cdetect_mux()
        self.sensor_present = [[0 for q in range(8)] for q in range (8)] # initialize 8x8 data matrix
        x = 1
        self.mux_present = [0 for q in range(8)]
        counter = 0
        for m in range(SensorReader.FIRST_MUX_ADDRESS, SensorReader.LAST_MUX_ADDRESS + 1):
                x = 1
                for s in range(0, 8):
                        try:
                                self.bus.write_byte_data(m, SensorReader.DEVICE_REG_MODE1, x)
                                self.mux_present[m - SensorReader.FIRST_MUX_ADDRESS] = True

                                #print("Mux present at: ", hex(m))
                        except (IOError):
                                #print  ("There's no mux at this address: ", hex(m))
                                self.mux_present[m - SensorReader.FIRST_MUX_ADDRESS] = False
                        #bus.write_byte_data(m, SensorReader.DEVICE_REG_MODE1, x) #Choose sensor x on mux m
                        x = x*2
                        #print ("initializing sensor at mux: ", hex(m), " channel: ", str(s))
                        self.tcs = TCS34725(integrationTime=self.integration_time_int, gain=self.gain_index) # 2.4 ms integration time, 60X amp
                        id = self.tcs.getID()
                        #print (id)
                        if (id == 0x44):
                                self.sensor_present[m - SensorReader.FIRST_MUX_ADDRESS][s] = 1
                        else:
                                self.sensor_present[m - SensorReader.FIRST_MUX_ADDRESS][s] = 0
                        sleep(.03)
                        counter += 1
                if(self.mux_present[m - SensorReader.FIRST_MUX_ADDRESS]):
                        self.bus.write_byte_data(m, SensorReader.DEVICE_REG_MODE1, 0x00) #Choose sensor x on mux m
                sleep(.1)
        print ("Init complete")
        print (self.sensor_present)

    def i2cdetect_mux(self):
            muxes = [0 for x in range(8)]
            with open(os.devnull, "wb") as limbo:
                    result = subprocess.check_output(["i2cdetect", "-y", "1"], stderr=limbo)
                    parse = result.split(":")
                    parse = parse[8].split(" ")
                    for i in range(1, 9):
                            muxes[i-1] = parse[i]
                    return muxes

    def i2cdetect_sensor(self):
            sensors = [0 for x in range(16)]
            with open(os.devnull, "wb") as limbo:
                    result = subprocess.check_output(["i2cdetect", "-y", "1"], stderr=limbo)
                    parse = result.split(":")
                    parse = parse[3].split(" ")
                    for i in range(1, 17):
                            sensors[i-1] = parse[i]
                    #print sensors[9]
                    if sensors[9] == '29':
                            return 1
                    else:
                            return 0

    def ResetChannels(self):
            self.muxes = self.i2cdetect_mux()
            for u in range(0, 8):
                    mux = self.muxes[u]
                    if mux != '--':
                        mux = '0x' + mux
                        mux = int(mux, 16)
                        self.bus.write_byte_data(mux, SensorReader.DEVICE_REG_MODE1, 0x00) #Choose no channels
                        print ("mux ", mux, " is clear")

    def timestamp(self):
            now = time.time()
            localtime = time.localtime(now)
            milliseconds = '%03d' % int((now - int(now)) * 1000)
            strtime = str(time.strftime('%m/%d/%Y %H:%M:%S:', localtime) + milliseconds)
            return strtime

    def readList2(self, reg, length):
            "Read a list of bytes from the I2C device"
            try:
              results = self.bus.read_i2c_block_data(self.address, reg, length)
              if self.debug:
                print ("I2C: Device 0x%02X returned the following from reg 0x%02X" % (self.tcs.address, reg))
                print (results)
              return results
            except (IOError, err):
              return tcs.errMsg()

    def streamData(self): #Added 5/18 by Hayleigh, reads single data stream for less overhead
            "Reads the raw red, green, blue and clear channel values"
            color = {}
            colors = self.tcs.getRawDataList()
            return colors

    def time_min(self):
            minute = str(time.strftime('%M'))
            return int(minute)

    def time_sec(self):
            sec = str(time.strftime('%S'))
            return int(sec)

    def time_msec(self):
            msec = int(time.time() * 1000) % 1000
            return msec

    def ReadSensors(self):
            "Reads sensor data. Returns a list of lists of sensor readings. Empty lists / readings mean there aren't any sensors or multiplexers for that index."
            data = []
            for u in range(0, 8):
                    mux = self.muxes[u]
                    muxData = []
                    data.append(muxData)
                    if mux != '--' and self.mux_present[u]:
                            mux = '0x' + mux
                            mux = int(mux, 16)
                            x2 = 1
                            for s2 in range(0, 8):
                                    sensorData = []
                                    self.bus.write_byte_data(mux, SensorReader.DEVICE_REG_MODE1, x2) #Choose sensor x on mux m
                                    if (self.sensor_present[u][s2]):
                                            rgb = self.streamData() #Get sensor data
                                            # scale sensor rating to between 0->1.0
                                            sensorData = [  rgb['r'] / float(self.max_sensor_val), \
                                                            rgb['g'] / float(self.max_sensor_val), \
                                                            rgb['b'] / float(self.max_sensor_val), \
                                                            rgb['c'] / float(self.max_sensor_val), \
                                                            self.time_sec(), self.time_msec()]
                                            x2 = x2*2
                                    else:
                                            x2 = x2*2

                                    muxData.append(sensorData)
                            self.bus.write_byte_data(mux, SensorReader.DEVICE_REG_MODE1, 0x00) #Choose sensor x on mux m
            return data
