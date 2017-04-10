from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import argparse
import os

import tensorflow as tf
from read_session import combine_data, combined_to_feature
from session_pb2 import Session


def _int64_feature(value):
  return tf.train.Feature(int64_list=tf.train.Int64List(value=[value]))


def _int64_list_feature(value):
  return tf.train.Feature(int64_list=tf.train.Int64List(value=value))


def _float_list_feature(value):
  return tf.train.Feature(float_list=tf.train.FloatList(value=value))


def convert(session_dir, out_dir):
  # Find session file in directory
  session_file = None
  for file in os.listdir(session_dir):
    if file.endswith(".session"):
      # Check if we've already found a session file
      if session_file is not None:
        print("ERROR: Corrupt dir, found multiple session files")
        return
      else:
        session_file = os.path.join(session_dir, file)

  if session_file is None:
    print("ERROR: Couldn't find session file")
    return

  # Read session data
  session = Session()
  with open(session_file, 'rb') as f:
    buffer = f.read()
    session.ParseFromString(buffer)

  # Load and combine data
  for recording in session.recordings:
    print("Loading recording {} {}".format(recording.name, recording.id))
    recording_dir = os.path.join(session_dir, str(recording.id))
    combined = combine_data(recording_dir)

    # Convert to tf record
    tf_record_name = str(session.id) + "_" + str(recording.id) + ".tfrecords"
    tf_record_file = os.path.join(out_dir, tf_record_name)
    print("Converting: ", tf_record_file)
    writer = tf.python_io.TFRecordWriter(tf_record_file)
    for index in range(len(combined)):
      data_point = combined[index]

      # Convert combined frame into feature + label
      labels, sensor_data = combined_to_feature(data_point)

      # Make example proto
      example = tf.train.Example(features=tf.train.Features(feature={
        'num_occupants': _int64_feature(int(len(labels) / 2)),
        'labels': _float_list_feature(labels),
        'sensor_data': _float_list_feature(sensor_data)
      }))
      writer.write(example.SerializeToString())
    writer.close()


def main():
  parser = argparse.ArgumentParser()
  parser.add_argument(
      '--input',
      type=str,
      help='Input session directory'
  )
  parser.add_argument(
      '--out_dir',
      type=str,
      help='Output directory'
  )
  FLAGS = parser.parse_args()
  convert(FLAGS.input, FLAGS.out_dir)


if __name__ == '__main__':
  main()
