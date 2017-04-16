import matplotlib
import numpy as np
import singlepixel_pb2
import tensorflow as tf
from read_session import read_delimited_protos_file, readings_dict_to_features, \
  sp_reading_key
from tensorflow.python.platform import gfile

matplotlib.use("Qt5Agg")
import matplotlib.pyplot as plt


import numpy as np
import matplotlib.pyplot as plt
import matplotlib.collections as mcoll

def colorline(
    x, y, z=None, cmap='copper', norm=plt.Normalize(0.0, 1.0),
    linewidth=3, alpha=1.0):
  """
  http://nbviewer.ipython.org/github/dpsanders/matplotlib-examples/blob/master/colorline.ipynb
  http://matplotlib.org/examples/pylab_examples/multicolored_line.html
  Plot a colored line with coordinates x and y
  Optionally specify colors in the array z
  Optionally specify a colormap, a norm function and a line width
  """

  # Default colors equally spaced on [0,1]:
  if z is None:
    z = np.linspace(0.0, 1.0, len(x))

  # Special case if a single number:
  # to check for numerical input -- this is a hack
  if not hasattr(z, "__iter__"):
    z = np.array([z])

  z = np.asarray(z)

  segments = make_segments(x, y)
  lc = mcoll.LineCollection(segments, array=z, cmap=cmap, norm=norm,
                            linewidth=linewidth, alpha=alpha)

  ax = plt.gca()
  ax.add_collection(lc)

  return lc

def make_segments(x, y):
  """
  Create list of line segments from x and y coordinates, in the correct format
  for LineCollection: an array of the form numlines x (points per line) x 2 (x
  and y) array
  """

  points = np.array([x, y]).T.reshape(-1, 1, 2)
  segments = np.concatenate([points[:-1], points[1:]], axis=1)
  return segments

data_files = [
  #"/home/doug/Development/bu_code/research/singlepixellocalization/src/main/resources/datav2/1597641000/269008718817948824/plugins/singlepixel.pbdat",
  #"/home/doug/Development/bu_code/research/singlepixellocalization/src/main/resources/datav2/1597641000/4490902409508234808/plugins/singlepixel.pbdat",
  #"/home/doug/Development/bu_code/research/singlepixellocalization/src/main/resources/datav2/1597641000/7452919585598622080/plugins/singlepixel.pbdat",
  #"/home/doug/Development/bu_code/research/singlepixellocalization/src/main/resources/datav2/1597641000/8297053638602816068/plugins/singlepixel.pbdat",
  #"/home/doug/Development/bu_code/research/singlepixellocalization/src/main/resources/datav2/1597641000/8482739308465044588/plugins/singlepixel.pbdat",
  "/home/doug/Desktop/boundary.pbdat",
]

for data_file in data_files:
  print(data_file)
  #model_file = "models/test5_model.pb"
  model_file = "models/running_mean_model.pb_checkpoint5000.pbdat"
  min_x = -1.18
  max_x = 1.11
  min_y = 1.19
  max_y = 3.34

  readings = read_delimited_protos_file(
    singlepixel_pb2.SinglePixelSensorReading, data_file)
  num_sensors = 11
  test_data = []
  current_readings = {}
  readings_buffer = []
  buffer_size = 5000
  for reading in readings:
    sp_id = sp_reading_key(reading)

    current_readings[sp_id] = reading
    if len(current_readings) == num_sensors:
      feature = readings_dict_to_features(current_readings)

      readings_buffer.append(feature)

      if len(readings_buffer) < buffer_size:
        continue

      while len(readings_buffer) > buffer_size:
        readings_buffer.pop(0)

      buffer_mean = np.mean(np.array(readings_buffer), axis=0)
      buffer_std = np.std(np.array(readings_buffer), axis=0)
      final_feature = (feature - buffer_mean) / buffer_std

      test_data.append(final_feature)

  test_data = np.array(test_data)

  print("Points: ", len(test_data), len(readings));
  if len(test_data) == 0:
    continue


  for i in range(num_sensors):
    plt.subplot(num_sensors / 2 + 1, 2, i + 1)
    plt.plot(test_data[:, i])
  plt.show()

  with gfile.FastGFile(model_file, 'rb') as f:
    graph_def = tf.GraphDef()
    graph_def.ParseFromString(f.read())
    input_layer, output_layer = tf.import_graph_def(graph_def,
                                                    return_elements=['input:0',
                                                                     'output:0'],
                                                    name='')
    with tf.Session() as sess:
      init = tf.global_variables_initializer()
      sess.run(init)

      predictions = sess.run([output_layer], feed_dict={
        input_layer: test_data
      })
      predictions = predictions[0]

      '''
      plt.subplot(2, 1, 1)
      plt.plot(predictions[:, 0], 'g')
      plt.ylabel("X (m)")

      plt.subplot(2, 1, 2)
      plt.plot(predictions[:, 1], 'g')
      plt.ylabel("Y (m)")
      plt.show()
      '''

      x = predictions[:, 0].tolist()
      y = predictions[:, 1].tolist()
      fig, ax = plt.subplots()
      lc = colorline(x, y, cmap='hsv')
      plt.colorbar(lc)
      plt.xlim(min_x, max_x)
      plt.ylim(min_y, max_y)
      #plt.xlim(np.min(x), np.max(x))
      #plt.ylim(np.min(y), np.max(y))
      plt.show()

      plt.scatter(predictions[:, 0], predictions[:, 1], s=0.1)
      plt.show()
