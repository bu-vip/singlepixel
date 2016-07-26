import socket
import binascii

TCP_IP = '128.197.164.68'
TCP_PORT = 5000
BUFFER_SIZE = 1024
MESSAGE = ""
MB = [None]*17

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
MB[7] = chr(0xAA) # Command 
MB[8] = chr(7) # RGB
MB[9] = chr(OFF) # Pan
MB[10]= chr(OFF) # Tilt
MB[11]= chr(OFF) # Dimmer, no flashing at FF
MB[12]= chr(OFF) # Red
MB[13]= chr(OFF) # Green
MB[14]= chr(OFF) # Blue
MB[15]= chr(OFF) # White
MB[16]= chr(0x4F) # Checksum

MESSAGE = "".join(MB)

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.connect((TCP_IP, TCP_PORT))
s.send(binascii.b2a_hex(MESSAGE.encode()))
data = s.recv(BUFFER_SIZE)
s.close()
print("received data:", data.decode())

