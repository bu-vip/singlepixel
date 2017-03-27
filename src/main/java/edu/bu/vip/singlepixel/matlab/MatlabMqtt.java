package edu.bu.vip.singlepixel.matlab;

import com.google.common.collect.EvictingQueue;
import com.google.protobuf.InvalidProtocolBufferException;
import edu.bu.vip.singlepixel.Protos.SinglePixelSensorReading;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MatlabMqtt implements MqttCallback {

  private final String topicPrefix;
  private final String hostname;
  private final int numReadings;
  private MqttClient client;

  private final Object pastReadingsLock = new Object();
  private Map<String, EvictingQueue<SinglePixelSensorReading>> pastReadings = new HashMap<>();

  public MatlabMqtt(String topicPrefix, String hostname, int numReadings) {
    this.topicPrefix = topicPrefix;
    this.hostname = hostname;
    this.numReadings = numReadings;
  }

  public void start() throws Exception {
    try {
      MemoryPersistence persistence = new MemoryPersistence();
      String clientId = "data-recorder-" + (int) (Math.random() * 99999);
      client = new MqttClient(hostname, clientId, persistence);
    } catch (MqttException e) {
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

  public void messageArrived(String aTopic, MqttMessage message) throws Exception {
    // Remove prefix
    String topic = aTopic.substring(this.topicPrefix.length());
    String[] levels = topic.split("/");

    // Basic topic checking
    if (levels.length == 5 && levels[1].equals("group") && levels[3].equals("sensor")) {
      try {
        // Recover protobuf from payload
        SinglePixelSensorReading reading = SinglePixelSensorReading.parseFrom(message.getPayload());
        // Store in past readings buffer
        synchronized (pastReadingsLock) {
          String key = reading.getGroupId() + "/" + reading.getSensorId();
          // Create a queue if there isn't one already
          if (!pastReadings.containsKey(key)) {
            EvictingQueue<SinglePixelSensorReading> newQueue = EvictingQueue.create(numReadings);
            pastReadings.put(key, newQueue);
          }
          pastReadings.get(key).add(reading);
        }
      } catch (InvalidProtocolBufferException ex) {
        // TODO(doug) - Log exception
      }
    } else {
      // TODO(doug) - Log invalid topic
    }
  }

  public void deliveryComplete(IMqttDeliveryToken arg0) {

  }

  public Map<String, List<SinglePixelSensorReading>> getReadings() {
    synchronized (pastReadings) {
      Map<String, List<SinglePixelSensorReading>> readings = new HashMap<>();
      for (String key : pastReadings.keySet()) {
        EvictingQueue<SinglePixelSensorReading> readingQueue = pastReadings.get(key);
        readings.put(key, new ArrayList<>(readingQueue));
      }
      return readings;
    }
  }

  public void connectionLost(Throwable arg0) {
    try {
      this.stop();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void stop() throws Exception {
    if (client.isConnected()) {
      client.disconnect();
    }
  }
}
