package edu.bu.vip.singlepixel.multikinect;

import com.beust.jcommander.Parameter;

public class MainArgs {

  @Parameter(names = {"--data_dir"}, description = "Directory for storing data", required = true)
  private String dataDirectory;

  @Parameter(names = {"--mqtt_host"}, description = "Mqtt Broker url")
  private String mqttHost = "tcp://localhost:1883";

  @Parameter(names = {"--mqtt_prefix"}, description = "Mqtt prefix")
  private String mqttTopicPrefix = "";

  @Parameter(names = {"--help", "-h"}, help = true, description = "Displays this help message")
  private boolean help;

  public String getDataDirectory() {
    return dataDirectory;
  }

  public String getMqttHost() {
    return mqttHost;
  }

  public boolean isHelp() {
    return help;
  }

  public String getMqttTopicPrefix() {
    return mqttTopicPrefix;
  }
}
