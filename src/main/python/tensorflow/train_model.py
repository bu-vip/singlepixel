import tensorflow as tf


def read_and_decode(filename_queue):
  reader = tf.TFRecordReader()
  _, serialized_example = reader.read(filename_queue)

  example = tf.parse_single_example(
      serialized_example,
      features={
        'num_occupants': tf.FixedLenFeature([], tf.int64),
        'labels': tf.FixedLenFeature([], tf.float32),
        'sensor_data': tf.FixedLenFeature([], tf.float32),
      })

  # Filter out examples when # of skeletons != 1
  occupants_equal = tf.equal(example['num_occupants'], 1)
  filter_out = tf.select(occupants_equal, [1], [0])

  return filter_out, example['sensor_data'], example['labels']


def input_pipeline(filenames, batch_size, num_epochs=None):
  filename_queue = tf.train.string_input_producer(
      filenames,
      num_epochs=num_epochs,
      shuffle=True)
  filter_out, example, label = read_and_decode(filename_queue)

  # min_after_dequeue defines how big a buffer we will randomly sample
  #   from -- bigger means better shuffling but slower start up and more
  #   memory used.
  # capacity must be larger than min_after_dequeue and the amount larger
  #   determines the maximum we will prefetch.  Recommendation:
  #   min_after_dequeue + (num_threads + a small safety margin) * batch_size
  min_after_dequeue = 10000
  capacity = min_after_dequeue + 3 * batch_size
  example_batch, label_batch = tf.train.shuffle_batch(
      [filter_out, example, label],
      enqueue_many=True,
      batch_size=batch_size,
      capacity=capacity,
      min_after_dequeue=min_after_dequeue)

  return example_batch, label_batch

def _create_hidden_layer(prev_layer, prev_layer_dim, layer_dim):
  # Add layer weights and biases
  weights = tf.Variable(tf.random_normal([prev_layer_dim, layer_dim]))
  biases = tf.Variable(tf.random_normal([layer_dim]))

  # Calculate the weighted sum of inputs and biases
  weighted_sum = tf.add(tf.matmul(prev_layer, weights), biases)
  # Create new ReLu layer
  layer = tf.nn.relu(weighted_sum)

  return layer

def create_network(learning_rate, input_dim, out_dim, hidden_layers):
  input = tf.placeholder(tf.float32, [None, input_dim], name="input")

  # Create the hidden layers
  last_dim = input_dim
  last_layer = input
  for hidden_layer_dim in hidden_layers:
    last_layer = _create_hidden_layer(last_layer, last_dim, hidden_layer_dim)
    last_dim = hidden_layer_dim

  # Create output layer
  out_weights = tf.Variable(tf.random_normal([last_dim, out_dim]))
  out_biases = tf.Variable(tf.random_normal([out_dim]))
  output_layer = tf.add(tf.matmul(last_layer, out_weights), out_biases, name="output")

  # Define cost
  labels = tf.placeholder(tf.float32, [None, out_dim], name="labels")
  distance_squared = tf.reduce_sum(tf.square(labels - output_layer),  1)
  cost = tf.reduce_mean(distance_squared)
  optimizer = tf.train.AdamOptimizer(learning_rate=learning_rate).minimize(cost)

  # Define accuracy
  accuracy = tf.reduce_mean(tf.pow(distance_squared, 0.5))

  return optimizer, cost, accuracy, input, output_layer


def train(learning_rate, hidden_layers, save_model=None):
  with tf.Graph().as_default():
    # Create network
    optimizer, cost, accuracy, _, _ = create_network(
        learning_rate,
        11 * 4,
        2,
        hidden_layers
    )



    # Create the graph, etc.
    init_op = tf.global_variables_initializer()

    # Create a session for running operations in the Graph.
    sess = tf.Session()

    # Initialize the variables (like the epoch counter).
    sess.run(init_op)

    # Start input enqueue threads.
    coord = tf.train.Coordinator()
    threads = tf.train.start_queue_runners(sess=sess, coord=coord)

    try:
      while not coord.should_stop():
        # Run training steps or whatever
        _, cost = sess.run([optimizer, cost])

    except tf.errors.OutOfRangeError:
      print('Done training -- epoch limit reached')
    finally:
      # When done, ask the threads to stop.
      coord.request_stop()

    # Wait for threads to finish.
    coord.join(threads)

    if save_model is not None:
      output_graph_def = tf.graph_util.convert_variables_to_constants(sess, sess.graph.as_graph_def(), ["output"])
      tf.train.write_graph(output_graph_def, '.', save_model, as_text=False)

    sess.close()
