from __future__ import print_function

import random

import tensorflow as tf
import numpy as np


class Regressor():
    def __init__(self, input_dim, out_dim, hidden_layers, learning_rate=0.01):
        self.graph = tf.Graph()
        self.learning_rate = learning_rate
        self.input_dim = input_dim
        self.out_dim = out_dim
        self.hidden_layers = hidden_layers

        with self.graph.as_default():
            self.input = tf.placeholder(tf.float32, [None, self.input_dim], name="input")

            self.layers = [self.input]
            self.weights = []
            self.biases = []

            # Create the hidden layers
            last_dim = self.input_dim
            for hidden_layer_dim in hidden_layers:
                self._create_hidden_layer(last_dim, hidden_layer_dim)
                last_dim = hidden_layer_dim

            # Create output layer
            self.weights.append(tf.Variable(tf.random_normal([last_dim, self.out_dim])))
            self.biases.append(tf.Variable(tf.random_normal([self.out_dim])))
            self.output_layer = tf.add(tf.matmul(self.layers[-1], self.weights[-1]), self.biases[-1], name="output")

            # Define cost
            self.labels = tf.placeholder(tf.float32, [None, self.out_dim], name="labels")
            distance_squared = tf.reduce_sum(tf.square(self.labels - self.output_layer),  1)
            self.cost = tf.reduce_mean(distance_squared)
            self.optimizer = tf.train.AdamOptimizer(learning_rate=self.learning_rate).minimize(self.cost)

            # Test trained model
            self.accuracy = tf.reduce_mean(tf.pow(distance_squared, 0.5))

    def _create_hidden_layer(self, prev_layer_dim, layer_dim):
        # Add layer weights and biases
        self.weights.append(tf.Variable(tf.random_normal([prev_layer_dim, layer_dim])))
        self.biases.append(tf.Variable(tf.random_normal([layer_dim])))

        # Calculate the weighted sum of inputs and biases
        layer_input = self.layers[-1]
        weighted_sum = tf.add(tf.matmul(layer_input, self.weights[-1]), self.biases[-1])
        # Create new ReLu layer
        self.layers.append(tf.nn.relu(weighted_sum))

    def _create_batches(self, labels, data, size):
        indices = list(range(len(labels)))
        random.shuffle(indices)
        label_batches = []
        data_batches = []
        current_label_batch = []
        current_data_batch = []
        for i in range(len(indices)):
            current_label_batch.append(labels[indices[i]])
            current_data_batch.append(data[indices[i]])
            if len(current_label_batch) >= size:
                label_batches.append(np.array(current_label_batch))
                data_batches.append(np.array(current_data_batch))
                current_label_batch = []
                current_data_batch = []

        return np.array(label_batches), np.array(data_batches)

    def train(self, train_labels, train_data, test_labels, test_data, batch_size=100, epochs=1000, save_model=None):
        with tf.Session(graph=self.graph) as sess:
            init = tf.global_variables_initializer()
            sess.run(init)

            for epoch in range(0, epochs):
                label_batches, data_batches = self._create_batches(train_labels, train_data, batch_size)
                avg_cost = 0
                avg_acc = 0
                for batch_i in range(0, len(label_batches)):
                    _, cost, acc = sess.run([self.optimizer,
                                             self.cost,
                                             self.accuracy],
                                            feed_dict={
                        self.input: data_batches[batch_i],
                        self.labels: label_batches[batch_i]})
                    avg_cost += cost / len(label_batches)
                    avg_acc += acc / len(label_batches)

                if epoch % 100 == 0:
                    print(epoch, avg_cost, acc)

                if save_model is not None and epoch % 1000 == 0:
                  output_graph_def = tf.graph_util.convert_variables_to_constants(sess, sess.graph.as_graph_def(), ["output"])
                  tf.train.write_graph(output_graph_def, '.', save_model + "_checkpoint" + str(epoch) + ".pbdat", as_text=False)

            test_accuracy, test_predictions = sess.run([self.accuracy, self.output_layer], feed_dict={
                self.input: test_data,
                self.labels: test_labels})

            if save_model is not None:
                output_graph_def = tf.graph_util.convert_variables_to_constants(sess, sess.graph.as_graph_def(), ["output"])
                tf.train.write_graph(output_graph_def, '.', save_model, as_text=False)

            return test_accuracy, test_predictions
