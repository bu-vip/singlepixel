import socket
import binascii

TCP_IP = '128.197.164.68'
TCP_PORT = 80
BUFFER_SIZE = 1024
MESSAGE = ""
MB = [None]*18

ON = 0xFF
OFF= 0x00
# Set up message
MB[0] = chr(0xA4)
MB[1] = chr(len(MB)-2)
MB[2] = chr(0)
MB[3] = chr(0)
MB[4] = chr(0)
MB[5] = chr(0)
MB[6] = chr(0) # Quick response
MB[7] = chr(0x0D) # Command 
MB[8] = chr(0x07) # All LEDs
MB[9] = chr(OFF) # ALL_CS
MB[10]= chr(OFF) # tilt
MB[11]= chr(ON) # Set
MB[12]= chr(ON)
MB[13]= chr(ON)
MB[14]= chr(ON)
MB[15]= chr(ON)
MB[16]= chr(OFF)
MB[len(MB)-1]= chr(0xF1) # Checksum

MESSAGE = "".join(MB)

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.connect((TCP_IP, TCP_PORT))
s.send(MESSAGE.encode())
data = s.recv(BUFFER_SIZE)
s.close()
print("received data:", data.decode())

