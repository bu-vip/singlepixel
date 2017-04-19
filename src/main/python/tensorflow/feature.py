import numpy as np

from srcgen.frame_pb2 import Joint

CENTER_JOINTS = [
  Joint.SPINE_BASE,
  Joint.SPINE_MID,
  Joint.NECK_,
  Joint.SPINE_SHOULDER
]


def calculate_average_position(combined_skeleton, center_joints=CENTER_JOINTS):
  """
  Calculates the average position using the specified center joints. If none
  of the center joints are available, the average of all joints is returned.
  """
  pos_sum = np.array([0, 0, 0])
  all_pos_sum = np.array([0, 0, 0])
  joint_count = 0
  all_joint_count = 0
  for joint in combined_skeleton.skeleton.joints:
    joint_pos = [joint.position.x, joint.position.y, joint.position.z]
    if not np.isnan(joint_pos).any():
      if joint.type in CENTER_JOINTS or True:
        pos_sum = np.add(pos_sum, joint_pos)
        joint_count += 1
      all_pos_sum = np.add(all_pos_sum, joint_pos)
      all_joint_count += 1

  if joint_count == 0:
    return all_pos_sum * 1.0 / all_joint_count

  return pos_sum * 1.0 / joint_count


def reading_to_feature(reading, raw):
  """
  Converts a sensor reading into a feature. If raw is false, luminance is 
  calculated from the RGB channels.
  """
  if raw:
    return np.array([reading.red, reading.green, reading.blue, reading.clear])
  else:
    luminance = 0.2126 * reading.red + 0.7152 * reading.green + 0.0722 * reading.blue
    return np.array([luminance])


def readings_dict_to_features(dict, raw):
  """
  Converts a dictionary of sensor readings into a feature vector.
  :param dict: Dictionary of readings, keys are sp_reading_keys
  :param raw: If feature should be made of raw sensor data
  :return: 
  """
  final = None
  for key in sorted(dict):
    converted = reading_to_feature(dict[key], raw)
    if final is None:
      final = converted
    else:
      final = np.concatenate([final, converted])
  return final


def _combined_to_feature(combined, sensor_raw):
  labels = []
  for skeleton in combined['synced'].skeletons:
    avg_pos = calculate_average_position(skeleton)
    labels += [avg_pos[0], avg_pos[2]]
  features = readings_dict_to_features(combined['sp'], sensor_raw)
  return np.array(labels), features


def combined_to_features(combined, average_size=5000, sensor_raw=True):
  """
  Converts combined data into labels and features. 
  :param combined: Combined data
  :param average_size: The size of the buffer used for normalization
  :param sensor_raw: If true, raw sensor readings are used 
  :return: labels, features
  """
  all_labels = []
  all_features = []
  previous_features = []
  for data in combined:
    labels, features = _combined_to_feature(data, sensor_raw)
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
