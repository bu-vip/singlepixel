import os
from glob import glob
from os.path import basename

import numpy as np

from src.main.python.util.proto_utils import read_delimited_protos_file, \
  timestamp_compare
from srcgen.frame_pb2 import Frame
from srcgen.realtime_pb2 import SyncedFrame
from srcgen.singlepixel_pb2 import SinglePixelSensorReading


def sp_reading_key(reading):
  return reading.group_id + '/' + reading.sensor_id


def read_raw_camera_data(recording_dir):
  camera_readings = {}
  for camera_file in glob(os.path.join(recording_dir, "cameras", "*.pbdat")):
    file_name = basename(camera_file)
    camera_id = os.path.splitext(file_name)[0]
    camera_readings[camera_id] = read_delimited_protos_file(Frame, camera_file)
  return camera_readings


def _find_raw_frames_for_synced_frame(synced_frame, camera_readings,
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


def _calculate_average_ntp_time_for_frames(raw_frames):
  # TODO(doug) - Not correct, but should be close
  return raw_frames['camera1'].ntp_capture_time


def _calculate_ntp_time_of_frame(synced_frame, camera_readings, camera_indexes):
  # Get the relevant raw frames
  raw_frames = _find_raw_frames_for_synced_frame(synced_frame,
                                                 camera_readings,
                                                 camera_indexes)
  # Calculate the NTP time for the synced frame
  synced_ntp_time = _calculate_average_ntp_time_for_frames(raw_frames)
  return synced_ntp_time


def combine_data(recording_dir, num_sensors, all_sensors_must_change):
  # Read the sensor readings
  sp_file = os.path.join(recording_dir, "plugins", "singlepixel.pbdat")
  sp_all_readings = read_delimited_protos_file(SinglePixelSensorReading,
                                               sp_file)

  # Read all of the raw camera frames
  camera_readings = read_raw_camera_data(recording_dir)

  # Read the synced frames
  synced_frame_file = os.path.join(recording_dir, "synced.pbdat")
  synced_frames = read_delimited_protos_file(SyncedFrame,
                                             synced_frame_file)

  camera_indexes = {}
  for camera_id in camera_readings:
    camera_indexes[camera_id] = 0

  synced_frame = synced_frames[0]
  synced_next_frame_index = 1
  synced_next_ntp_time = _calculate_ntp_time_of_frame(
      synced_frames[synced_next_frame_index],
      camera_readings,
      camera_indexes)

  # Iterate over sensor readings
  combined = []
  current_readings = {}
  done = False
  for reading in sp_all_readings:
    sp_id = sp_reading_key(reading)

    # Check if reading comes after frame
    if timestamp_compare(synced_next_ntp_time, reading.ntp_capture_time) != 1:
      # Iterate through frames until reading comes after frame
      while timestamp_compare(synced_next_ntp_time,
                              reading.ntp_capture_time) != 1:
        # Check for end of synced frames
        if synced_next_frame_index == len(synced_frames) - 1:
          done = True
          break

        synced_frame = synced_frames[synced_next_frame_index]
        synced_next_frame_index += 1
        synced_next_ntp_time = _calculate_ntp_time_of_frame(
            synced_frames[synced_next_frame_index],
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

      # If all readings must change for a new data point, clear readings dict
      if all_sensors_must_change:
        current_readings = {}

  return combined


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


