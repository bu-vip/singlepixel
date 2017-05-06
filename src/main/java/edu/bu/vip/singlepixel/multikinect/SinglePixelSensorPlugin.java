package edu.bu.vip.singlepixel.multikinect;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import edu.bu.vip.multikinect.controller.plugin.Plugin;
import edu.bu.vip.singlepixel.Protos.SinglePixelSensorReading;
import java.util.function.Consumer;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SinglePixelSensorPlugin implements Plugin, MqttCallback {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final String topicPrefix;
  private final String hostname;
  private MqttClient client;

  private final Object recordingLock = new Object();
  private boolean recording = false;
  private Consumer<GeneratedMessageV3> readingConsumer;

  public SinglePixelSensorPlugin(String topicPrefix, String hostname) {
    this.topicPrefix = topicPrefix;
    this.hostname = hostname;
  }

  public void start() throws Exception {
    logger.info("Plugin started");

    try {
      MemoryPersistence persistence = new MemoryPersistence();
      String clientId = "data-recorder-" + (int) (Math.random() * 99999);
      client = new MqttClient(hostname, clientId, persistence);
    } catch (MqttException e) {
      logger.error("Error connecting to MQTT server", e);
      throw new RuntimeException("Error creating MQTT client.");
    }

    MqttConnectOptions connOpts = new MqttConnectOptions();
    connOpts.setCleanSession(true);
    client.connect(connOpts);
    client.setCallback(this);

    // subscribe to receive sensor readings
    String subscribeDest = this.topicPrefix + "/#";
    client.subscribe(subscribeDest);
  }

  public void recordingStarted(Consumer<GeneratedMessageV3> readingConsumer) {
    logger.info("Plugin recording started");

    synchronized (recordingLock) {
      this.readingConsumer = readingConsumer;
      recording = true;
    }
  }

  public void messageArrived(String aTopic, MqttMessage message) throws Exception {
    // Remove prefix
    String topic = aTopic.substring(this.topicPrefix.length());
    String[] levels = topic.split("/");

    // Basic topic checking
    if (levels.length == 5 && levels[1].equals("group") && levels[3].equals("sensor")) {
      try {
        // Recover protobuf from payload
        SinglePixelSensorReading reading = SinglePixelSensorReading.parseFrom(message.getPayload());

        // Record if recording
        synchronized (recordingLock) {
          if (recording) {
            readingConsumer.accept(reading);
          }
        }
      } catch (InvalidProtocolBufferException ex) {
        logger.warn("Error parsing payload. Topic: {}", aTopic, ex);
      }
    } else {
      logger.warn("Received an invalid topic. Topic: {}", aTopic);
    }
  }

  public void deliveryComplete(IMqttDeliveryToken arg0) {

  }

  public void connectionLost(Throwable arg0) {
    try {
      this.stop();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void recordingStopped() {
    logger.info("Plugin recording stopped");

    synchronized (recordingLock) {
      readingConsumer = null;
      recording = false;
    }
  }

  public void stop() throws Exception {
    logger.info("Plugin stopped");

    if (client.isConnected()) {
      client.disconnect();
    }
  }
}
