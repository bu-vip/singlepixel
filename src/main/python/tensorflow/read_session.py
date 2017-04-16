import csv
import os
from glob import glob
from os.path import basename

import numpy as np
import realtime_pb2
import singlepixel_pb2
from frame_pb2 import Frame, Joint
from google.protobuf.internal import decoder


def unpack_messages(type, buffer):
  messages = []
  buffer_pos = 0
  while buffer_pos < len(buffer):
    # Read the length of the next message
    message_size, new_pos = decoder._DecodeVarint32(buffer, buffer_pos)
    # Update current position to after decoded size
    buffer_pos = new_pos

    # Get message data, increment position
    msg_buf = buffer[buffer_pos:message_size + buffer_pos]
    buffer_pos += message_size

    # Create and parse message data
    message = type()
    message.ParseFromString(msg_buf)
    messages.append(message)

  return messages


def read_delimited_protos_file(message_type, filename):
  with open(filename, 'rb') as synced_reader:
    buffer = synced_reader.read()
    return unpack_messages(message_type, buffer)


def sp_reading_key(reading):
  return reading.group_id + '/' + reading.sensor_id


def sp_separate_readings_by_sensor(all_readings):
  # Separate readings into separate lists, 1 per sensor
  sp_readings = {}
  for reading in all_readings:
    key = sp_reading_key(reading)
    if key not in sp_readings:
      sp_readings[key] = []
    sp_readings[key].append(reading)
  return sp_readings


def ntp_compare(time_a, time_b):
  if time_a.seconds < time_b.seconds:
    return -1
  elif time_a.seconds > time_b.seconds:
    return 1
  else:
    if time_a.nanos < time_b.nanos:
      return -1
    elif time_a.nanos > time_b.nanos:
      return 1
    return 0


def ntp_time_less_than(time_a, time_b):
  return ntp_compare(time_a, time_b) == -1


def find_sp_readings_for_time(ntp_time, sp_all_readings, sp_indexes):
  current_readings = {}
  reading_groups = []
  finished = False
  changed = True
  while changed is True and finished is False:
    changed = False
    for sp_id in sp_all_readings:
      readings = sp_all_readings[sp_id]
      current_reading = readings[sp_indexes[sp_id]]

      # Check if time is before reading
      if ntp_compare(current_reading.ntp_capture_time, ntp_time) == 1:
        finished = True
        break

      # No more readings, at end
      if sp_indexes[sp_id] == len(readings) - 1:
        continue
      else:
        if sp_id in current_readings:
          # Check if next reading is before the desired time
          sp_indexes[sp_id] += 1

        # Update current reading
        current_readings[sp_id] = current_reading
        # Only return mappings with all sensors
        if len(current_readings) == len(sp_all_readings):
          reading_groups.append(current_readings.copy())

        # A sensor reading was updated
        changed = True

  return reading_groups


def read_raw_camera_data(recording_dir):
  camera_readings = {}
  for camera_file in glob(os.path.join(recording_dir, "cameras", "*.pbdat")):
    file_name = basename(camera_file)
    camera_id = os.path.splitext(file_name)[0]
    camera_readings[camera_id] = read_delimited_protos_file(Frame, camera_file)
  return camera_readings


def find_raw_frames_for_synced_frame(synced_frame, camera_readings,
    camera_indexes):
  raw_frames = {}
  for frame_point in synced_frame.frame_points:
    # Increment the index until the frame is found
    camera_frames = camera_readings[frame_point.camera_id]
    # TODO(doug): Change < to != when bug is fixed
    while camera_frames[
      camera_indexes[frame_point.camera_id]].time < frame_point.frame_id:
      camera_indexes[frame_point.camera_id] += 1
      if camera_indexes[frame_point.camera_id] >= len(camera_frames):
        raise Exception("Invalid frame id")
    # Store the found frame
    raw_frames[frame_point.camera_id] = camera_frames[
      camera_indexes[frame_point.camera_id]]

  return raw_frames


def calculate_average_ntp_time_for_frames(raw_frames):
  # TODO(doug) - Not correct, but should be close
  return raw_frames['camera1'].ntp_capture_time


def calculate_ntp_time_of_frame(synced_frame, camera_readings, camera_indexes):
  # Get the relevant raw frames
  raw_frames = find_raw_frames_for_synced_frame(synced_frame,
                                                camera_readings,
                                                camera_indexes)
  # Calculate the NTP time for the synced frame
  synced_ntp_time = calculate_average_ntp_time_for_frames(raw_frames)
  return synced_ntp_time


def combine_data(recording_dir):
  # Read the sensor readings
  sp_file = os.path.join(recording_dir, "plugins", "singlepixel.pbdat")
  sp_all_readings = read_delimited_protos_file(
      singlepixel_pb2.SinglePixelSensorReading, sp_file)
  # sp_readings = sp_separate_readings_by_sensor(sp_all_readings)
  # TODO(doug) - Don't hardcode!
  num_sensors = 11

  # Read all of the raw camera frames
  camera_readings = read_raw_camera_data(recording_dir)

  # Read the synced frames
  synced_frame_file = os.path.join(recording_dir, "synced.pbdat")
  synced_frames = read_delimited_protos_file(realtime_pb2.SyncedFrame,
                                             synced_frame_file)

  camera_indexes = {}
  for camera_id in camera_readings:
    camera_indexes[camera_id] = 0

  synced_frame = synced_frames[0]
  synced_next_frame_index = 1
  synced_next_ntp_time = calculate_ntp_time_of_frame(synced_frames[synced_next_frame_index],
                                                camera_readings,
                                                camera_indexes)

  # Iterate over sensor readings
  combined = []
  current_readings = {}
  done = False
  for reading in sp_all_readings:
    sp_id = sp_reading_key(reading)

    # Check if reading comes after frame
    if ntp_compare(synced_next_ntp_time, reading.ntp_capture_time) != 1:
      # Iterate through frames until reading comes after frame
      while ntp_compare(synced_next_ntp_time, reading.ntp_capture_time) != 1:
        # Check for end of synced frames
        if synced_next_frame_index == len(synced_frames) - 1:
          done = True
          break

        synced_frame = synced_frames[synced_next_frame_index]
        synced_next_frame_index += 1
        synced_next_ntp_time = calculate_ntp_time_of_frame(synced_frames[synced_next_frame_index],
                                                      camera_readings,
                                                      camera_indexes)

    if done:
      break

    # Update readings buffer
    current_readings[sp_id] = reading
    # Check that current_readings has all sensors
    if len(current_readings) == num_sensors:
      # Create new data point
      new_point = {
        'synced': synced_frame,
        'sp': current_readings.copy(),
      }
      combined.append(new_point)

  return combined


CENTER_JOINTS = [
  Joint.SPINE_BASE,
  Joint.SPINE_MID,
  Joint.NECK_,
  Joint.SPINE_SHOULDER
]


def calculate_average_position(combined_skeleton):
  pos_sum = np.array([0, 0, 0])
  all_pos_sum = np.array([0, 0, 0])
  joint_count = 0
  all_joint_count = 0
  for joint in combined_skeleton.skeleton.joints:
    joint_pos = [joint.position.x, joint.position.y, joint.position.z]
    if np.isnan(joint_pos).any() == False:
      if joint.type in CENTER_JOINTS or True:
        pos_sum = np.add(pos_sum, joint_pos)
        joint_count += 1
      all_pos_sum = np.add(all_pos_sum, joint_pos)
      all_joint_count += 1

  if joint_count == 0:
    return all_pos_sum * 1.0 / all_joint_count

  return pos_sum * 1.0 / joint_count


def reading_to_feature(reading):
  luminance = 0.2126 * reading.red + 0.7152 * reading.green + 0.0722 * reading.blue
  return np.array([luminance])
  # return np.array([reading.red, reading.green, reading.blue, reading.clear])


def readings_dict_to_features(dict):
  final = None
  for key in sorted(dict):
    converted = reading_to_feature(dict[key])
    if final is None:
      final = converted
    else:
      final = np.concatenate([final, converted])
  return final


def combined_to_feature(combined):
  labels = []
  for skeleton in combined['synced'].skeletons:
    avg_pos = calculate_average_position(skeleton)
    labels += [avg_pos[0], avg_pos[2]]
  features = readings_dict_to_features(combined['sp'])
  return np.array(labels), features


def combined_to_features(combined, average_size=5000):
  all_labels = []
  all_features = []
  previous_features = []
  for data in combined:
    labels, features = combined_to_feature(data)
    # Add the current reading to the buffer
    previous_features.append(features)
    # Remove old element to maintain length
    while len(previous_features) > average_size:
      previous_features.pop(0)

    # If buffer is full, calculate final feature
    if len(previous_features) == average_size:
      all_labels.append(labels)
      buffer_mean = np.mean(np.array(previous_features), axis=0)
      buffer_std = np.std(np.array(previous_features), axis=0)
      final_feature = (features - buffer_mean) / buffer_std
      all_features.append(final_feature)

  return np.array(all_labels), np.array(all_features)


def filter_by_number_skeletons(combined):
  clips = {}
  last_skeleton_count = -1
  current_clip = []
  for frame in combined:
    synced = frame['synced']
    if last_skeleton_count != len(synced.skeletons):
      if len(current_clip) > 0:
        # Add list if needed
        if last_skeleton_count not in clips:
          clips[last_skeleton_count] = []
        clips[last_skeleton_count].append(current_clip)
      current_clip = []
      last_skeleton_count = len(synced.skeletons)

    current_clip.append(frame)

  if len(current_clip) > 0:
    # Add list if needed
    if last_skeleton_count not in clips:
      clips[last_skeleton_count] = []
    clips[last_skeleton_count].append(current_clip)

  return clips


def get_bounds(labels):
  mins = np.amin(labels, axis=0)
  maxs = np.amax(labels, axis=0)
  return mins[0], maxs[0], mins[1], maxs[1]


def save_as_csv(filename, labels, data):
  with open(filename, 'w') as file:
    for label, point in zip(labels, data):
      writer = csv.writer(file)
      row = label.tolist() + point.tolist()
      row = [float(x) for x in row]
      writer.writerow(row)
