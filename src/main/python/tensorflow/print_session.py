import os
import sys
from glob import glob

from srcgen.session_pb2 import Session


def main():
  """
  Finds all the sessions in from a sessions folder and prints out the contents. 
  
  The session folder should look like this:
    session/
      123/
        123.session
        ...
      456/
        456.session
        ...
      ...
  """
  session_dir = sys.argv[1]
  session_file_pattern = os.path.join(session_dir, "*", "*.session")
  for file in glob(session_file_pattern):
    session = Session()
    with open(file, 'rb') as f:
      buffer = f.read()
      session.ParseFromString(buffer)

    print(session.name, "\t", session.id)
    # Load and combine data
    for recording in session.recordings:
      print("\t", recording.name, "\t", recording.id)


main()
