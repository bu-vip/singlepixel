import os
import shutil
from random import randint

import matplotlib
import numpy as np
import tensorflow as tf
from numpy import genfromtxt
from tensorflow.contrib.learn import DNNRegressor
from tensorflow.python.framework import graph_util

matplotlib.use("Qt5Agg")
import matplotlib.pyplot as plt
from read_session import combine_data, filter_by_number_skeletons, combined_to_features, get_bounds


def batch_of(data, labels, size):
    batch_data = []
    batch_labels = []
    batch_elements = set()
    for i in range(size):
        # Choose a new random index
        next_element = -1
        while next_element is -1 or next_element in batch_elements:
            next_element = randint(0, len(data) - 1)
        # Add element to the batch
        batch_data.append(data[next_element])
        batch_labels.append(labels[next_element])
        batch_elements.add(next_element)

    return batch_data, batch_labels


def column(matrix, i):
    return [row[i] for row in matrix]


def graph_results(data, real_labels, predicted_labels, window_size):
    # Graph the results
    num_features = int(len(data[0]) / window_size)
    for i in range(num_features):
        plt.subplot(num_features, 1, i + 1)
        middle_feature = int(window_size / 2) * num_features
        plt.plot(column(data, middle_feature + i), 'b')
        plt.plot(real_labels, 'g')
        plt.plot(predicted_labels, 'r')
    plt.show()


def train_algorithm(train_data, train_one_hot, test_data, test_one_hot,
                    save_model=None):
    feature_len = len(train_data[0])
    num_classes = len(train_one_hot[0])

    with tf.Graph().as_default():
        # Create the model
        x = tf.placeholder(tf.float32, [None, feature_len], name="input")
        W = tf.Variable(tf.zeros([feature_len, num_classes]))
        b = tf.Variable(tf.zeros([num_classes]))
        # y = tf.matmul(x, W) + b
        y = tf.nn.softmax(tf.matmul(x, W) + b, name="output")

        # Define loss and optimizer
        y_ = tf.placeholder(tf.float32, [None, num_classes])
        cross_entropy = tf.reduce_mean(
            tf.nn.softmax_cross_entropy_with_logits(logits=y, labels=y_))
        train_step = tf.train.GradientDescentOptimizer(0.5).minimize(cross_entropy)

        sess = tf.Session()
        init = tf.global_variables_initializer()
        sess.run(init)
        # Train
        for _ in range(1000):
            batch_data, batch_one_labels = batch_of(train_data, train_one_hot, 100)
            sess.run(train_step, feed_dict={x: batch_data, y_: batch_one_labels})

        # Test trained model
        distance_squared = tf.reduce_sum(tf.square(y - y_), 1)
        accuracy = tf.reduce_mean(tf.pow(distance_squared, 0.5))
        test_accuracy = sess.run(accuracy,
                                 feed_dict={x: test_data, y_: test_one_hot})

        if save_model is not None:
            print('Exporting trained model to', save_model)
            output_graph_def = graph_util.convert_variables_to_constants(
                sess, sess.graph.as_graph_def(), ["output"])
            tf.train.write_graph(output_graph_def, '.', save_model, as_text=False)

        # Get predictions and return
        predictions = sess.run(y, feed_dict={x: test_data, y_: test_one_hot})
        return test_accuracy, predictions


def train_algorithm_v2(train_data, train_labels, test_data, test_labels, model_dir):
    feature_len = len(train_data[0])
    feature_columns = [tf.contrib.layers.real_valued_column("", dimension=feature_len)]

    hidden_units = [100, 100, 100]
    model_x_dir = model_dir + "_x"
    shutil.rmtree(model_x_dir)
    estimator_x = DNNRegressor(
        feature_columns=feature_columns,
        hidden_units=hidden_units,
        model_dir=model_x_dir
    )

    model_y_dir = model_dir + "_y"
    shutil.rmtree(model_y_dir)
    estimator_y = DNNRegressor(
        feature_columns=feature_columns,
        hidden_units=hidden_units,
        model_dir=model_y_dir
    )

    steps = 10000

    def input_fn_train_x():
        x = tf.constant(train_data)
        y = tf.constant(train_labels[:, 0])
        return x, y

    estimator_x.fit(input_fn=input_fn_train_x, steps=steps)

    def input_fn_train_y():
        x = tf.constant(train_data)
        y = tf.constant(train_labels[:, 1])
        return x, y

    estimator_y.fit(input_fn=input_fn_train_y, steps=steps)

    def input_fn_eval_x():
        x = tf.constant(test_data)
        y = tf.constant(test_labels[:, 0])
        return x, y

    estimator_x.evaluate(input_fn=input_fn_eval_x, steps=1)

    def input_fn_eval_y():
        x = tf.constant(test_data)
        y = tf.constant(test_labels[:, 1])
        return x, y

    estimator_y.evaluate(input_fn=input_fn_eval_y, steps=1)

    predictions_x = estimator_x.predict(x=test_data)
    predictions_y = estimator_y.predict(x=test_data)

    sum = 0
    i = 0
    for pred_x, pred_y in zip(predictions_x, predictions_y):
        predicted = [pred_x, pred_y]
        actual = test_labels[i]
        diff = actual - predicted
        sq_dist = diff[0] * diff[0] + diff[1] * diff[1]
        i += 1
        sum += pow(sq_dist, 0.5)
    distance = sum / i

    return distance, zip(predictions_x, predictions_y)


def walk_to_features(walk):
    labels = walk[:, 0:2]
    data = walk[:, 2:]
    return labels, data


def walks_to_feature(walks):
    all_labels, all_data = walk_to_features(walks[0])
    for walk in walks[1:]:
        labels, data = walk_to_features(walk)
        np.append(all_labels, labels, axis=0)
        np.append(all_data, data, axis=0)

    return all_labels, all_data


def train_and_evaluate(training_walks, testing_walks, graph=False,
                       filename=None):
    # Convert data to features
    train_labels, train_data = walks_to_feature(training_walks)
    test_labels, test_data = walks_to_feature(testing_walks)

    # Run the algorithm
    accuracy, predictions = train_algorithm_v2(train_data, train_labels,
                                               test_data, test_labels,
                                               filename)

    return accuracy


def walks_test():
    # Load data
    root_dir = "../../resources/datav1/track5"
    people = ['dan', 'doug', 'jiawei', 'pablo']
    data = []
    for person in people:
        walks = []
        for file in os.listdir(root_dir + "/" + person):
            if file.endswith(".csv"):
                full_path = root_dir + "/" + person + "/" + file
                walk_data = genfromtxt(full_path, delimiter=',', skip_header=1)
                walks.append(walk_data)
        data.append(walks)

    training_walks = data[0] + data[1]
    testing_walks = data[2] + data[3]

    acc = train_and_evaluate(training_walks, testing_walks, filename="./models/model")
    print(acc)


def v2_test():
    root_dir = "../../resources/datav2/882188772/"
    recordings = [
        '316715123336182773'
    ]

    # Load and combine data
    recording_dir = os.path.join(root_dir, recordings[0])
    combined = combine_data(recording_dir)
    clipped = filter_by_number_skeletons(combined)

    # Combine all clips with 1 person into giant matrices
    single_person_clips = clipped[1]
    all_labels, all_data = combined_to_features(single_person_clips[0])
    for clip in single_person_clips[1:]:
        labels, data = combined_to_features(clip)
        all_labels = np.concatenate([all_labels, labels])
        all_data = np.concatenate([all_data, data])

    # Tensorflow works on float32s
    all_labels = all_labels.astype(np.float32)
    all_data = all_data.astype(np.float32)

    print("Bounds: ", get_bounds(all_labels))

    # Split data in half
    data_middle = int(len(all_data) / 2)
    train_data = all_data[:data_middle]
    test_data = all_data[data_middle:]
    label_middle = int(len(all_labels) / 2)
    train_labels = all_labels[:label_middle]
    test_labels = all_labels[label_middle:]
    filename = "./models/model_v2"

    # Run the algorithm
    accuracy, predictions = train_algorithm_v2(train_data, train_labels,
                                               test_data, test_labels,
                                               filename)
    print(accuracy, predictions)

