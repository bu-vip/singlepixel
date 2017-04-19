from google.protobuf.internal import decoder


def _unpack_messages(type, buffer):
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
  """
  Reads a delimited proto file and parses it with the specified message type.
  """
  with open(filename, 'rb') as synced_reader:
    buffer = synced_reader.read()
    return _unpack_messages(message_type, buffer)


def timestamp_compare(time_a, time_b):
  """
  Compares two timestamps. Behaviour:
    if time_a < time_b:
      return -1
    elif time_a == time_b:
      return 0
    else:
      return 1
  """
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


def timestamp_less_than(time_a, time_b):
  """
  Returns true if time_a < time_b. Convenience wrapper around 
  :func:`timestamp_compare`.
  """
  return timestamp_compare(time_a, time_b) == -1
