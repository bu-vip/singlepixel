package edu.bu.vip.singlepixel.demo;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableList;
import com.google.common.math.Stats;
import edu.bu.vip.multikinect.Protos.Position;
import edu.bu.vip.singlepixel.Protos.SinglePixelSensorReading;
import edu.bu.vip.singlepixel.util.ReadingUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Algorithm2 {

  public static class Builder {

    private int numSensors = -1;
    private boolean calcLuminance = false;
    private boolean backgroundSubtraction = true;
    private int numPastReadings = 1;
    private String modelPath;
    private String inputNodeName;
    private String outputNodeName;

    public Builder() {
    }

    public Builder setCalcLuminance(boolean calcLuminance) {
      this.calcLuminance = calcLuminance;
      return this;
    }

    public Builder setBackgroundSubtraction(boolean backgroundSubtraction) {
      this.backgroundSubtraction = backgroundSubtraction;
      return this;
    }

    public Builder setNumPastReadings(int numPastReadings) {
      this.numPastReadings = numPastReadings;
      return this;
    }

    public Builder setNumSensors(int numSensors) {
      this.numSensors = numSensors;
      return this;
    }

    public Builder setModelPath(String modelPath) {
      this.modelPath = modelPath;
      return this;
    }

    public Builder setInputNodeName(String inputNodeName) {
      this.inputNodeName = inputNodeName;
      return this;
    }

    public Builder setOutputNodeName(String outputNodeName) {
      this.outputNodeName = outputNodeName;
      return this;
    }

    public Algorithm2 build() {
      if (numSensors < 0) {
        throw new RuntimeException("Num sensors not set");
      }

      return new Algorithm2(numSensors, calcLuminance, backgroundSubtraction, numPastReadings,
          modelPath,
          inputNodeName, outputNodeName);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  private static final int NUM_CHANNELS = 4;

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final int numSensors;
  private final boolean calcLuminance;
  private final boolean backgroundSubtraction;
  private final int numPastReadings;
  private final Map<String, List<SinglePixelSensorReading>> backgroundReadings = new HashMap<>();
  private final Map<String, List<Double>> backgroundReadingMeans = new HashMap<>();
  private final Map<String, EvictingQueue<SinglePixelSensorReading>> pastReadings = new HashMap<>();
  private final TensorFlowInterface tensorFlowInterface;
  private final String inputNodeName;
  private final String outputNodeName;

  private Algorithm2(int numSensors, boolean calcLuminance, boolean backgroundSubtraction,
      int numPastReadings, String modelPath, String inputNodeName, String outputNodeName) {
    this.numSensors = numSensors;
    this.calcLuminance = calcLuminance;
    this.backgroundSubtraction = backgroundSubtraction;
    this.numPastReadings = numPastReadings;
    this.tensorFlowInterface = new TensorFlowInterface(modelPath);
    this.inputNodeName = inputNodeName;
    this.outputNodeName = outputNodeName;
  }

  public void addBackgroundReading(SinglePixelSensorReading reading) {
    addBackgroundReadings(ImmutableList.of(reading));
  }

  public void addBackgroundReadings(List<SinglePixelSensorReading> readings) {
    if (!backgroundSubtraction) {
      logger.warn("Background subtraction is not enabled");
    }

    readings.forEach(reading -> {
      // Check if map contains a list already
      final String key = ReadingUtils.sensorKey(reading);
      if (!backgroundReadings.containsKey(key)) {
        backgroundReadings.put(key, new ArrayList<>());
      }

      // Guaranteed to have a list
      backgroundReadings.get(key).add(reading);
    });

    // Recalculate means
    updateBackgroundMeans();
  }

  private void updateBackgroundMeans() {
    backgroundReadingMeans.clear();
    backgroundReadings.forEach((key, readings) -> {

      // Get the channel values
      List<List<Double>> values = null;
      if (calcLuminance) {
        values = new ArrayList<>();
        values.add(ReadingUtils.luminance(readings));
      } else {
        values = ReadingUtils.splitValues(readings);
      }

      // Calculate mean for each channel
      List<Double> means = new ArrayList<>();
      values.forEach(channelValues -> {
        Stats stats = Stats.of(channelValues);
        means.add(stats.mean());
      });

      // Update means
      backgroundReadingMeans.put(key, means);
    });
  }

  public void addReading(SinglePixelSensorReading reading) {
    addReadings(ImmutableList.of(reading));
  }

  public void addReadings(List<SinglePixelSensorReading> readings) {
    readings.forEach(reading -> {
      // Check if map contains a list already
      final String key = ReadingUtils.sensorKey(reading);
      if (!pastReadings.containsKey(key)) {
        pastReadings.put(key, EvictingQueue.create(numPastReadings));
      }

      // Guaranteed to have a list
      pastReadings.get(key).add(reading);
    });
  }

  public Position predict() {
    // Check that we have data from all sensors
    if (pastReadings.size() != numSensors) {
      logger.warn("Can't predict, invalid number of sensor data");
      return Position.getDefaultInstance();
    }

    // Check that we have enough data from each sensor
    for (String key : pastReadings.keySet()) {
      if (pastReadings.get(key).size() < numPastReadings) {
        logger.warn("Not enough data for sensor: {}", key);
        return Position.getDefaultInstance();
      }
    }

    // Order sensor keys
    List<String> sortedKeys = new ArrayList<>();
    sortedKeys.addAll(pastReadings.keySet());
    Collections.sort(sortedKeys);

    int featureIndex = 0;
    float[] featureArray = new float[numSensors * numPastReadings * (calcLuminance ? 1
        : NUM_CHANNELS)];
    // Enumerate through each sensor
    for (String sensorKey : sortedKeys) {

      // Get the background means
      List<Double> means = backgroundReadingMeans.get(sensorKey);
      // Check that background data has been set
      if (backgroundSubtraction) {
        int requiredBackground = (calcLuminance ? 1 : NUM_CHANNELS);
        if (means == null || means.size() != requiredBackground) {
          logger.warn("No background data for sensor: {}", sensorKey);
          return Position.getDefaultInstance();
        }
      }

      // Build feature vector by going through all past readings, oldest first
      Collection<SinglePixelSensorReading> sensorReadings = pastReadings.get(sensorKey);
      for (SinglePixelSensorReading reading : sensorReadings) {
        if (calcLuminance) {
          featureArray[featureIndex] = (float) ReadingUtils.luminance(reading);
          featureArray[featureIndex] -= (backgroundSubtraction ? means.get(0) : 0);
          featureIndex++;
        } else {
          // Red
          featureArray[featureIndex] = (float) reading.getRed();
          featureArray[featureIndex] -= (backgroundSubtraction ? means.get(0) : 0);
          featureIndex++;
          // Green
          featureArray[featureIndex] = (float) reading.getGreen();
          featureArray[featureIndex] -= (backgroundSubtraction ? means.get(1) : 0);
          featureIndex++;
          // Blue
          featureArray[featureIndex] = (float) reading.getBlue();
          featureArray[featureIndex] -= (backgroundSubtraction ? means.get(2) : 0);
          featureIndex++;
          // Clear
          featureArray[featureIndex] = (float) reading.getClear();
          featureArray[featureIndex] -= (backgroundSubtraction ? means.get(3) : 0);
          featureIndex++;
        }
      }
    }

    // Input feature vector into graph
    tensorFlowInterface.feed(inputNodeName, featureArray, 1, featureArray.length);
    // Run graph, calculate output node
    tensorFlowInterface.run(ImmutableList.of(outputNodeName));
    // Get output
    float[] position = new float[2];
    tensorFlowInterface.fetch(outputNodeName, position);

    Position.Builder builder = Position.newBuilder();
    builder.setX(position[0]);
    builder.setY(0);
    builder.setZ(position[1]);
    return builder.build();
  }

}
