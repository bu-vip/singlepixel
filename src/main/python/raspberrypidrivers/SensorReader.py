import smbus
from time import sleep

from Adafruit_TCS34725 import TCS34725

import subprocess
import os
import singlepixel_pb2


class SensorReader:
    FIRST_MUX_ADDRESS = 0x70  # MUX Address
    LAST_MUX_ADDRESS = 0x77
    DEVICE_REG_MODE1 = 0x00
    DEVICE_REG_LEDOUT0 = 0x1d
    COLOR_SENSOR_ADDRESS = 0x29  # Color sensor address

    def __init__(self, integration_time, gain):
        self.muxes = []
        self.bus = smbus.SMBus(1)
        self.integration_time_ms = integration_time
        if integration_time < 2.4 or integration_time > 612:
            raise Exception("Integration time not in range: [2.4, 612]")
        self.integration_time_int = int(256 - integration_time / 2.4)
        self.gain = gain
        if gain == 1:
            self.gain_index = 0
        elif gain == 4:
            self.gain_index = 1
        elif gain == 16:
            self.gain_index = 2
        elif gain == 60:
            self.gain_index = 3
        else:
            raise Exception("Gain not equal to: [1, 4, 16, 60]")
        # Calculate the max value the senors will return for the given integration time
        self.max_sensor_val = min((256 - self.integration_time_int) * 1024, 65535)

    def initialize(self):
        # Initialization routine
        self.reset_channels()
        self.muxes = self.i2cdetect_mux()
        self.sensor_present = [[0 for q in range(8)] for q in range(8)]  # initialize 8x8 data matrix
        x = 1
        self.mux_present = [0 for q in range(8)]
        counter = 0
        for m in range(SensorReader.FIRST_MUX_ADDRESS, SensorReader.LAST_MUX_ADDRESS + 1):
            x = 1
            for s in range(0, 8):
                try:
                    self.bus.write_byte_data(m, SensorReader.DEVICE_REG_MODE1, x)
                    self.mux_present[m - SensorReader.FIRST_MUX_ADDRESS] = True

                    # print("Mux present at: ", hex(m))
                except IOError:
                    # print  ("There's no mux at this address: ", hex(m))
                    self.mux_present[m - SensorReader.FIRST_MUX_ADDRESS] = False
                # bus.write_byte_data(m, SensorReader.DEVICE_REG_MODE1, x) #Choose sensor x on mux m
                x = x * 2
                # print ("initializing sensor at mux: ", hex(m), " channel: ", str(s))
                self.tcs = TCS34725(integrationTime=self.integration_time_int,
                                    gain=self.gain_index)  # 2.4 ms integration time, 60X amp
                id = self.tcs.getID()
                # print (id)
                if id == 0x44:
                    self.sensor_present[m - SensorReader.FIRST_MUX_ADDRESS][s] = 1
                else:
                    self.sensor_present[m - SensorReader.FIRST_MUX_ADDRESS][s] = 0
                sleep(.03)
                counter += 1
            if self.mux_present[m - SensorReader.FIRST_MUX_ADDRESS]:
                self.bus.write_byte_data(m, SensorReader.DEVICE_REG_MODE1, 0x00)  # Choose sensor x on mux m
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
                muxes[i - 1] = parse[i]
            return muxes

    def i2cdetect_sensor(self):
        sensors = [0 for x in range(16)]
        with open(os.devnull, "wb") as limbo:
            result = subprocess.check_output(["i2cdetect", "-y", "1"], stderr=limbo)
            parse = result.split(":")
            parse = parse[3].split(" ")
            for i in range(1, 17):
                sensors[i - 1] = parse[i]
            # print sensors[9]
            if sensors[9] == '29':
                return 1
            else:
                return 0

    def reset_channels(self):
        self.muxes = self.i2cdetect_mux()
        for u in range(0, 8):
            mux = self.muxes[u]
            if mux != '--':
                mux = '0x' + mux
                mux = int(mux, 16)
                self.bus.write_byte_data(mux, SensorReader.DEVICE_REG_MODE1, 0x00)  # Choose no channels
                print ("mux ", mux, " is clear")

    def stream_data(self):
        """Reads the raw red, green, blue and clear channel values"""
        color = {}
        colors = self.tcs.getRawDataList()
        return colors

    def _normalize_reading(self, value):
        """Normalizes a sensor value to range [0, 1]"""
        return value / float(self.max_sensor_val)

    def read_sensors(self):
        """Reads sensor data. Returns a list of lists of sensor readings. Empty lists / readings mean there aren't 
        any sensors or multiplexers for that index. """
        data = []
        for u in range(0, 8):
            mux = self.muxes[u]
            mux_data = []
            data.append(mux_data)
            if mux != '--' and self.mux_present[u]:
                mux = '0x' + mux
                mux = int(mux, 16)
                x2 = 1
                for s2 in range(0, 8):
                    sensor_data = None
                    # Choose sensor x on mux m
                    self.bus.write_byte_data(mux, SensorReader.DEVICE_REG_MODE1, x2)
                    if self.sensor_present[u][s2]:
                        # Read from sensor
                        rgb = self.stream_data()

                        # Store data in proto
                        sensor_data = singlepixel_pb2.SinglePixelSensorReading()
                        sensor_data.red = self._normalize_reading(rgb['r'])
                        sensor_data.green = self._normalize_reading(rgb['g'])
                        sensor_data.blue = self._normalize_reading(rgb['b'])
                        sensor_data.clear = self._normalize_reading(rgb['c'])
                        sensor_data.ntp_capture_time.GetCurrentTime()

                    x2 = x2 * 2
                    mux_data.append(sensor_data)
                # Choose sensor x on mux m
                self.bus.write_byte_data(mux, SensorReader.DEVICE_REG_MODE1, 0x00)
        return data
