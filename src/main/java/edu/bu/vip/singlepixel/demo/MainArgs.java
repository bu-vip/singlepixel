package edu.bu.vip.singlepixel.demo;

import com.beust.jcommander.Parameter;

public class MainArgs {

  @Parameter(names = {"--multikinect_dir"},
      description = "MultiKinect data directory, for locating calibration",
      required = true)
  private String multiKinectDirectory;

  @Parameter(names = {"--recordings_dir"},
      description = "Directory to put saved recording in",
      required = true)
  private String recordedDataDirectory;

  @Parameter(names = {"--model"},
      description = "TensorFlow model",
      required = true)
  private String modelFile;

  @Parameter(names = {"--num_sensors"},
      description = "number of sensors",
      required = true)
  private String numSensors;

  @Parameter(names = {"--calibration_id"},
      description = "MultiKinect calibration id",
      required = true)
  private long calibrationId;

  @Parameter(names = {"--mqtt_host"}, description = "Mqtt Broker url")
  private String mqttHost = "tcp://localhost:1883";

  @Parameter(names = {"--mqtt_prefix"}, description = "Mqtt prefix")
  private String mqttTopicPrefix = "";

  @Parameter(names = {"--help", "-h"}, help = true, description = "Displays this help message")
  private boolean help;

  public String getMultiKinectDirectory() {
    return multiKinectDirectory;
  }

  public String getRecordedDataDirectory() {
    return recordedDataDirectory;
  }

  public String getModelFile() {
    return modelFile;
  }

  public int getNumSensors() {
    return Integer.decode(numSensors);
  }

  public long getCalibrationId() {
    return calibrationId;
  }

  public String getMqttHost() {
    return mqttHost;
  }

  public String getMqttTopicPrefix() {
    return mqttTopicPrefix;
  }

  public boolean isHelp() {
    return help;
  }
}
