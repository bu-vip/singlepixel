from time import sleep
import time
from Adafruit_TCS34725 import TCS34725
import Adafruit_I2C
import numpy
'''import RobotRaconteur as RR'''
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

    def __init__():
        self.muxes = []
        self.bus = smbus.SMBus(1)
        pass

    def initialize():
        #Initialization routine
        ResetChannels()
        self.muxes = i2cdetect_mux()
        self.sensor_present = [[0 for q in range(8)] for q in range (8)] # initialize 8x8 data matrix
        x = 1
        self.mux_present = [0 for q in range(8)]
        counter = 0
        for m in range(FIRST_MUX_ADDRESS, LAST_MUX_ADDRESS + 1):
                x = 1
                for s in range(0, 8):
                        try:
                                self.bus.write_byte_data(m, DEVICE_REG_MODE1, x)
                                self.mux_present[m - FIRST_MUX_ADDRESS] = True

                                print("Mux present at: ", hex(m))
                        except (IOError, err):
                                print  ("There's no mux at this address: ", hex(m))
                                self.mux_present[m - FIRST_MUX_ADDRESS] = False
                        #bus.write_byte_data(m, DEVICE_REG_MODE1, x) #Choose sensor x on mux m
                        x = x*2
                        print ("initializing sensor at mux: ", hex(m), " channel: ", str(s))
                        self.tcs = TCS34725(integrationTime=0xD5, gain=0x03) # 2.4 ms integration time, 60X amp
                        id = self.tcs.getID()
                        print (id)
                        if (id == 0x44):
                                self.sensor_present[m - FIRST_MUX_ADDRESS][s] = 1
                        else:
                                self.sensor_present[m - FIRST_MUX_ADDRESS][s] = 0
                        sleep(.03)
                        counter += 1
                if(self.mux_present[m - FIRST_MUX_ADDRESS]):
                        self.bus.write_byte_data(m, DEVICE_REG_MODE1, 0x00) #Choose sensor x on mux m
                sleep(.1)
        print ("Init complete")
        print (self.sensor_present)

    def i2cdetect_mux():
            muxes = [0 for x in range(8)]
            with open(os.devnull, "wb") as limbo:
                    result = subprocess.check_output(["i2cdetect", "-y", "1"], stderr=limbo)
                    parse = result.split(":")
                    parse = parse[8].split(" ")
                    for i in range(1, 9):
                            muxes[i-1] = parse[i]
                    return muxes

    def i2cdetect_sensor():
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

    def ResetChannels():
            self.muxes = i2cdetect_mux()
            for u in range(0, 8):
                    mux = self.muxes[u]
                    if mux != '--':
                        mux = '0x' + mux
                        mux = int(mux, 16)
                        self.bus.write_byte_data(mux, DEVICE_REG_MODE1, 0x00) #Choose no channels
                        print ("mux ", mux, " is clear")

    def timestamp():
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

    def streamData(): #Added 5/18 by Hayleigh, reads single data stream for less overhead
            "Reads the raw red, green, blue and clear channel values"

            color = {}
            print ("Starting get raw")
            colors = self.tcs.getRawDataList()

            # Set a delay for the integration time
            #delay = self.__integrationTimeDelay.get(self.integrationTime)
            #time.sleep(delay)
            print ("Running get raw data list")
            print (colors)
            return colors

    def time_min():
            minute = str(time.strftime('%M'))
            return int(minute)

    def time_sec():
            sec = str(time.strftime('%S'))
            return int(sec)

    def time_msec():
            msec = str(time.strftime('%L'))
            return int(msec)

    def ReadSensors():
            # print time_min()
            #ResetChannels()
            # data = numpy.zeros(shape=(8,8,6), dtype = 'i4')
            #b= [[[0 for x in range(8)] for y in range (8)] for z in range(6)] #initializing hardcoded array
            #data = numpy.array(a,dtype='i4')

            #while(True): #begin main loop to poll sensors one after the other, repeatedly

            #muxes = i2cdetect_mux()
            '''
            try:
                    bus.write_byte_data(0x73, DEVICE_REG_MODE1, 0)
            except IOError, err:
                    print  "There's no mux at this address"
            '''
            data = []
            for u in range(0, 8):
                    mux = self.muxes[u]
                    muxData = []
                    data.push(muxData)
                    if mux != '--' and self.mux_present[u]:
                            mux = '0x' + mux
                            mux = int(mux, 16)
                            x2 = 1
                            print (self.sensor_present)
                            print (" ")
                            for s2 in range(0, 8):
                                    sensorData = []
                                    self.bus.write_byte_data(mux, DEVICE_REG_MODE1, x2) #Choose sensor x on mux m
                                    #tcs = TCS34725(integrationTime=0xD5, gain=0x03) # 2.4 ms integration time, 60X amp
                                    #sleep(.1)
                                    if (self.sensor_present[u][s2]):
                                            #tcs.enable()
                                            #sleep(.1)
                                            rgb = streamData() #Get sensor data
                                            red  = rgb['r']
                                            green= rgb['g']
                                            blue = rgb['b']
                                            clear= rgb['c']
                                            '''
                                            data[mux-FIRST_MUX_ADDRESS][s2][0] = red
                                            data[mux-FIRST_MUX_ADDRESS][s2][1] = green
                                            data[mux-FIRST_MUX_ADDRESS][s2][2] = blue
                                            data[mux-FIRST_MUX_ADDRESS][s2][3] = clear
                                            data[mux-FIRST_MUX_ADDRESS][s2][4] = time_min()
                                            data[mux-FIRST_MUX_ADDRESS][s2][5] = time_sec()
                                            '''
                                            sensorData = [red, green, blue, clear, time_min(), time_sec()]
                                            #tcs.disable()
                                            x2 = x2*2
                                            #ResetChannels()
                                    else:
                                            x2 = x2*2

                                    muxData.push(sensorData)
                            self.bus.write_byte_data(mux, DEVICE_REG_MODE1, 0x00) #Choose sensor x on mux m
            print (data)
            #return numpy.array(data, dtype = 'i4')
            #return numpy.array(data, shape=(8,8,6),dtype='i4')
            return data
