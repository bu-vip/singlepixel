package edu.bu.vip.singlepixel.demo;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableMap;
import edu.bu.vip.singlepixel.Protos.SinglePixelSensorReading;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

/**
 * Adapted from:
 * https://github.com/tensorflow/tensorflow/blob/master/tensorflow/contrib/android/java/org/tensorflow/contrib/android/TensorFlowInferenceInterface.java
 */
public class TensorflowLocalizationAlgorithm implements LocalizationAlgorithm {

  private final int windowSize = 1;

  private Consumer<ImmutableMap<Long, List<Double>>> listener;

  private final Object pastReadingsLock = new Object();
  private SortedMap<String, EvictingQueue<SinglePixelSensorReading>> pastReadings = new TreeMap<>();

  // State immutable between initializeTensorFlow calls.
  private final String modelName;
  private final Graph g;
  private final Session sess;

  // State reset on every call to run.
  private Session.Runner runner;
  private List<String> feedNames = new ArrayList<String>();
  private List<Tensor> feedTensors = new ArrayList<Tensor>();
  private List<String> fetchNames = new ArrayList<String>();
  private List<Tensor> fetchTensors = new ArrayList<Tensor>();

  public TensorflowLocalizationAlgorithm(String modelName) {
    this.modelName = modelName;
    this.g = new Graph();
    this.sess = new Session(g);
    this.runner = sess.runner();

    InputStream is = null;
    // Perhaps the model file is not an asset but is on disk.
    try {
      is = new FileInputStream(modelName);
    } catch (IOException e2) {
      throw new RuntimeException("Failed to load model from '" + modelName + "'", e2);
    }

    try {
      loadGraph(is, g);
      is.close();
    } catch (IOException e) {
      throw new RuntimeException("Failed to load model from '" + modelName + "'", e);
    }
  }

  private void loadGraph(InputStream is, Graph g) throws IOException {
    final long startMs = System.currentTimeMillis();

    // TODO(ashankar): Can we somehow mmap the contents instead of copying them?
    byte[] graphDef = new byte[is.available()];
    final int numBytesRead = is.read(graphDef);
    if (numBytesRead != graphDef.length) {
      throw new IOException(
          "read error: read only "
              + numBytesRead
              + " of the graph, expected to read "
              + graphDef.length);
    }

    try {
      g.importGraphDef(graphDef);
    } catch (IllegalArgumentException e) {
      throw new IOException("Not a valid TensorFlow Graph serialization: " + e.getMessage());
    }

    final long endMs = System.currentTimeMillis();
  }

  @Override
  public void setListener(Consumer<ImmutableMap<Long, List<Double>>> listener) {
    this.listener = listener;
  }

  @Override
  public void receivedReading(SinglePixelSensorReading reading) {
    // Store in past readings buffer
    synchronized (pastReadingsLock) {
      String key = reading.getGroupId() + "/" + reading.getSensorId();
      // Create a queue if there isn't one already
      if (!pastReadings.containsKey(key)) {
        EvictingQueue<SinglePixelSensorReading> newQueue = EvictingQueue.create(windowSize);
        pastReadings.put(key, newQueue);
      }
      pastReadings.get(key).add(reading);

      List<SinglePixelSensorReading> feature = new ArrayList<>();
      for (String sensorKey : pastReadings.keySet()) {
        feature.addAll(pastReadings.get(sensorKey));
      }


    }
  }

  private void run() {

    String[] outputNames = {"output"};

    // Release any Tensors from the previous run calls.
    closeFetches();

    // Add fetches.
    for (String o : outputNames) {
      fetchNames.add(o);
      TensorId tid = TensorId.parse(o);
      runner.fetch(tid.name, tid.outputIndex);
    }

    // Run the session.
    try {
      fetchTensors = runner.run();
    } catch (RuntimeException e) {
      // Ideally the exception would have been let through, but since this interface predates the
      // TensorFlow Java API, must return -1.
      throw e;
    } finally {
      // Always release the feeds (to save resources) and reset the runner, this run is
      // over.
      closeFeeds();
      runner = sess.runner();
    }
  }

  private static class TensorId {

    String name;
    int outputIndex;

    // Parse output names into a TensorId.
    //
    // E.g., "foo" --> ("foo", 0), while "foo:1" --> ("foo", 1)
    public static TensorId parse(String name) {
      TensorId tid = new TensorId();
      int colonIndex = name.lastIndexOf(':');
      if (colonIndex < 0) {
        tid.outputIndex = 0;
        tid.name = name;
        return tid;
      }
      try {
        tid.outputIndex = Integer.parseInt(name.substring(colonIndex + 1));
        tid.name = name.substring(0, colonIndex);
      } catch (NumberFormatException e) {
        tid.outputIndex = 0;
        tid.name = name;
      }
      return tid;
    }
  }

  private Tensor getTensor(String outputName) {
    int i = 0;
    for (String n : fetchNames) {
      if (n.equals(outputName)) {
        return fetchTensors.get(i);
      }
      ++i;
    }
    throw new RuntimeException(
        "Node '" + outputName + "' was not provided to run(), so it cannot be read");
  }

  /**
   * Cleans up the state associated with this Object. initializeTensorFlow() can then be called
   * again to initialize a new session.
   */
  public void close() {
    closeFeeds();
    closeFetches();
    sess.close();
    g.close();
  }

  @Override
  protected void finalize() throws Throwable {
    try {
      close();
    } finally {
      super.finalize();
    }
  }

  private void closeFeeds() {
    for (Tensor t : feedTensors) {
      t.close();
    }
    feedTensors.clear();
    feedNames.clear();
  }

  private void closeFetches() {
    for (Tensor t : fetchTensors) {
      t.close();
    }
    fetchTensors.clear();
    fetchNames.clear();
  }
}
