package edu.bu.vip.singlepixel.multikinect;

import com.google.protobuf.GeneratedMessageV3;
import edu.bu.vip.multikinect.controller.plugin.Plugin;
import edu.bu.vip.singlepixel.Grpc.SinglePixelSensorReading;
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
    String payload = new String(message.getPayload());
    // Remove prefix
    String topic = aTopic.substring(this.topicPrefix.length());
    String[] levels = topic.split("/");

    // Basic topic checking
    if (levels.length == 5 && levels[1].equals("group") && levels[3].equals("sensor")) {
      // Get sensor info from topic structure: <prefix>/group/<id>/sensor/<id>
      String groupId = levels[2];
      String sensorId = levels[4];

      // More checking
      String[] readings = (payload).split(",");
      if (readings.length != 6) {
        // Log invalid sensor reading
        logger.warn("Received an invalid sensor reading. Topic: {} Payload: {}", aTopic,
            payload);
      } else {
        try {
          double red = Double.parseDouble(readings[0].trim());
          double green = Double.parseDouble(readings[1].trim());
          double blue = Double.parseDouble(readings[2].trim());
          double white = Double.parseDouble(readings[3].trim());
          int time1 = Integer.parseInt(readings[4].trim());
          int time2 = Integer.parseInt(readings[5].trim());

          SinglePixelSensorReading.Builder builder = SinglePixelSensorReading.newBuilder();
          builder.setGroupId(groupId);
          builder.setSensorId(sensorId);
          builder.setRed(red);
          builder.setGreen(green);
          builder.setBlue(blue);
          builder.setWhite(white);

          // TODO(doug) - time

          synchronized (recordingLock) {
            if (recording) {
              readingConsumer.accept(builder.build());
            }
          }
        } catch (NumberFormatException e) {
          logger.warn("An error occurred processing a sensor reading. Topic: {} Payload: {}",
              aTopic, payload, e);
        }
      }
    } else {
      logger.warn("Received an invalid topic. Topic: {} Payload: {}", aTopic, payload);
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
