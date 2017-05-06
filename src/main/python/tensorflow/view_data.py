import os

import matplotlib
import numpy as np

from src.main.python.tensorflow.feature import _combined_to_feature
from src.main.python.tensorflow.read_session import combine_data
from srcgen.session_pb2 import Session


def test():
  matplotlib.use("Qt5Agg")
  import matplotlib.pyplot as plt

  session_dirs = [
    "/home/doug/Desktop/singlepixel/sessions/",
    "/home/doug/Desktop/singlepixel/sessions/",
    "/home/doug/Desktop/singlepixel/sessions/",
    "/home/doug/Desktop/singlepixel/sessions/",
    "/home/doug/Desktop/multikinect/sessions/",
    "/home/doug/Desktop/multikinect/sessions/",
    ]
  session_ids = [
    "31173029",
    "436310705",
    "539493861",
    "998797274",
    "271320373",
    "612073570",
  ]

  recording_whitelist = [
     "background1",
     #"background2",
     #"metronome1",
     #"metronome2",
  ]

  num_sensors = 11

  recording_names = []
  sensor_data = []
  for session_dir, session_id in zip(session_dirs, session_ids):
    root_dir = os.path.join(session_dir, str(session_id))
    session_file = str(session_id) + ".session"
    session = Session()
    with open(os.path.join(root_dir, session_file), 'rb') as f:
      buffer = f.read()
      session.ParseFromString(buffer)

    # Load and combine data
    for recording in session.recordings:
      if recording.name in recording_whitelist:
        recording_dir = os.path.join(root_dir, str(recording.id))
        combined_data = combine_data(recording_dir, num_sensors=num_sensors)

        features = []
        for combined in combined_data:
          _, feature = _combined_to_feature(combined)
          features.append(feature)

        if len(features) == 0:
          print("WARN: No features", session.name + recording.name)
        else:
          recording_names.append(session.name + recording.name)
          sensor_data.append(np.array(features))

  colors = ['b', 'g', 'r', 'c', 'm', 'y', 'k', 'w']
  for recording_i in range(0, len(recording_names)):
    recording_name = recording_names[recording_i]
    for i in range(0, num_sensors):
      plt.subplot(6, 2, i + 1)
      plt.plot(sensor_data[recording_i][:, i],
               color=colors[recording_i],
               label=recording_name)

  plt.legend(bbox_to_anchor=(1, 1), loc=2, borderaxespad=0.)
  plt.show()


test()
