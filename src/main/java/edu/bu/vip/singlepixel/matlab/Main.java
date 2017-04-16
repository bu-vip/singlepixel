package edu.bu.vip.singlepixel.matlab;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class Main implements MqttCallback {

  public static void main(String[] args) throws Exception {
    Main main = new Main();
    main.test2();

    MatlabMqtt test = new MatlabMqtt("", "tcp://localhost:1883", 1);
    test.start();
    while (true) {
      test.getReadings().forEach((key, reading) -> {
        if (reading.get(0).getClear() > 0.5) {
          System.out.println(key);
        }
      });
    }
  }

  public void test2() throws Exception {
    MqttClient client;
    try {
      MemoryPersistence persistence = new MemoryPersistence();
      String clientId = "data-recorder-" + (int) (Math.random() * 99999);
      client = new MqttClient("tcp://localhost:1883", clientId, persistence);
    } catch (MqttException e) {
      throw new RuntimeException("Error creating MQTT client.");
    }

    MqttConnectOptions connOpts = new MqttConnectOptions();
    connOpts.setCleanSession(true);
    client.connect(connOpts);
    client.setCallback(this);

    // subscribe to receive sensor readings
    String subscribeDest = "/#";
    client.subscribe(subscribeDest);

    boolean done = false;
    while (!done) {
    }

    if (client.isConnected()) {
      client.disconnect();
    }
  }

  public void messageArrived(String aTopic, MqttMessage message) throws Exception {
    System.out.println(aTopic);
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

  public void stop() throws Exception {
  }

}
