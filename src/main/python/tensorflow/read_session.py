import os
from glob import glob
from os.path import basename

import frame_pb2
import realtime_pb2
import singlepixel_pb2
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
    sp_readings = {}
    for sp_id in sp_all_readings:
        readings = sp_all_readings[sp_id]

        # Check if time is before reading
        if ntp_compare(readings[sp_indexes[sp_id]].ntp_capture_time, ntp_time) == 1:
            return None

        # No more readings, at end
        if sp_indexes[sp_id] == len(readings) - 1:
            return None

        # Search for the reading that is closest to before the time
        while ntp_time_less_than(readings[sp_indexes[sp_id] + 1].ntp_capture_time, ntp_time):
            sp_indexes[sp_id] += 1
            if sp_indexes[sp_id] == len(readings) - 1:
                # No more readings, at end
                return None
        sp_readings[sp_id] = readings[sp_indexes[sp_id]]
    return sp_readings


def read_raw_camera_data(recording_dir):
    camera_readings = {}
    for camera_file in glob(os.path.join(recording_dir, "cameras", "*.pbdat")):
        file_name = basename(camera_file)
        camera_id = os.path.splitext(file_name)[0]
        camera_readings[camera_id] = read_delimited_protos_file(frame_pb2.Frame, camera_file)
    return camera_readings


def find_raw_frames_for_synced_frame(synced_frame, camera_readings, camera_indexes):
    raw_frames = {}
    for frame_point in synced_frame.frame_points:
        # Increment the index until the frame is found
        camera_frames = camera_readings[frame_point.camera_id]
        # TODO(doug): Change < to != when bug is fixed
        while camera_frames[camera_indexes[frame_point.camera_id]].time < frame_point.frame_id:
            camera_indexes[frame_point.camera_id] += 1
            if camera_indexes[frame_point.camera_id] >= len(camera_frames):
                raise Exception("Invalid frame id")
        # Store the found frame
        raw_frames[frame_point.camera_id] = camera_frames[camera_indexes[frame_point.camera_id]]

    return raw_frames


def calculate_average_ntp_time_for_frames(raw_frames):
    # TODO(doug) - Not correct, but should be close
    return raw_frames['camera1'].ntp_capture_time


def combine_data(recording_dir):
    # Read the sensor readings
    sp_file = os.path.join(recording_dir, "plugins", "singlepixel.pbdat")
    sp_all_readings = read_delimited_protos_file(singlepixel_pb2.SinglePixelSensorReading, sp_file)
    sp_readings = sp_separate_readings_by_sensor(sp_all_readings)

    # Read all of the raw camera frames
    camera_readings = read_raw_camera_data(recording_dir)

    # Read the synced frames
    synced_frame_file = os.path.join(recording_dir, "synced.pbdat")
    synced_frames = read_delimited_protos_file(realtime_pb2.SyncedFrame, synced_frame_file)

    camera_indexes = {}
    for camera_id in camera_readings:
        camera_indexes[camera_id] = 0

    sp_indexes = {}
    for sensor_id in sp_readings:
        sp_indexes[sensor_id] = 0

    # Link frames and sp data
    combined = []
    for synced_frame in synced_frames:
        # Get the relevant raw frames
        raw_frames = find_raw_frames_for_synced_frame(synced_frame, camera_readings, camera_indexes)
        # Calculate the NTP time for the synced frame
        synced_time = calculate_average_ntp_time_for_frames(raw_frames)
        # Get the SP readings for the frame
        sp_data = find_sp_readings_for_time(synced_time, sp_readings, sp_indexes)

        # Create new combined point if sp data was found
        if sp_data is not None:
            new_point = {
                'synced': synced_frame,
                'sp': sp_data,
            }
            combined.append(new_point)

    print(combined)

    return combined

