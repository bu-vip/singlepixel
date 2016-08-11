import socket
import binascii

TCP_IP = '192.168.1.105'
TCP_PORT = 5000
BUFFER_SIZE = 1024
MESSAGE = ""
MB = [None]*18

ON = 0xFF
OFF= 0x00
chksum = 0x00
# Set up message
MB[0] = chr(0xA4)
MB[1] = chr(0x10)
MB[2] = chr(0)
MB[3] = chr(0)
MB[4] = chr(0)
MB[5] = chr(0)
MB[6] = chr(0) # Quick response
MB[7] = chr(0xAA) # Command 
MB[8] = chr(0x3F) # All LEDs
MB[9] = chr(0x08) # ALL_CS
MB[10]= chr(0x00) # tilt
MB[11]= chr(0x00) # Set
MB[12]= chr(0x00)
MB[13]= chr(0x00)
MB[14]= chr(0x00)
MB[15]= chr(0x00)
MB[16]= chr(0xCD)
for i in range(len(MB)-1):
    chksum = chksum ^ ord(MB[i])
MB[len(MB)-1]= chr(chksum) # Checksum

MESSAGE = "".join(MB)

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.connect((TCP_IP, TCP_PORT))
s.send(MESSAGE.encode())
data = s.recv(BUFFER_SIZE)
s.close()
print("received data:", data)

