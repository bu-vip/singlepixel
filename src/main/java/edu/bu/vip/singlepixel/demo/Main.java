package edu.bu.vip.singlepixel.demo;

import static ratpack.jackson.Jackson.json;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.InvalidProtocolBufferException;
import edu.bu.vip.singlepixel.Protos.SinglePixelSensorReading;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import ratpack.guice.Guice;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.server.RatpackServer;
import smartthings.ratpack.protobuf.CacheConfig;
import smartthings.ratpack.protobuf.ProtobufModule;
import smartthings.ratpack.protobuf.ProtobufModule.Config;

public class Main implements MqttCallback {

  public static void main(String[] args) throws Exception {
    Main main = new Main();
    main.start();

    System.out.println("Press enter to stop");
    Scanner scanner = new Scanner(System.in);
    scanner.nextLine();
    scanner.close();

    main.stop();

  }

  private LocalizationAlgorithm algorithm;
  private RatpackServer server;
  private MqttClient client;
  private final Object occupantMapLock = new Object();
  private ImmutableMap<Long, List<Double>> occupantMap;

  public void start() throws Exception {

    String modelDir = "/home/doug/Development/bu_code/research/singlepixellocalization/src/main/python/tensorflow/models/regress_model.pb";
    algorithm = new TensorflowLocalizationAlgorithm(modelDir);
    algorithm.setListener((newMap) -> {
      synchronized (occupantMapLock) {
        occupantMap = newMap;
      }
    });

    server = RatpackServer.start(s -> {
      s.serverConfig(config -> {
        config.port(8080);
      });
      s.registry(Guice.registry(b -> {
      }));
      s.handlers(chain -> {
        chain.get("_/state", new Handler() {

          @Override
          public void handle(Context context) throws Exception {
            Map<String, Object> state = new HashMap<>();

            Map<String, Object> bounds = new HashMap();
            bounds.put("minX", -3);
            bounds.put("maxX", 1);
            bounds.put("minY", -2);
            bounds.put("maxY", 5);
            state.put("bounds", bounds);

            List<Object> occupants = new ArrayList();
            synchronized (occupantMapLock) {
              if (occupantMap != null) {
                occupantMap.forEach((id, position) -> {
                  Map<String, Object> posMap = new HashMap<>();
                  posMap.put("x", position.get(0));
                  posMap.put("y", position.get(1));

                  Map<String, Object> occupant = new HashMap<>();
                  occupant.put("id", id);
                  occupant.put("position", posMap);

                  occupants.add(occupant);
                });
              }
            }
            state.put("occupants", occupants);

            context.getResponse().getHeaders().set("Access-Control-Allow-Origin", "*" );
            context.getResponse().getHeaders().set("Access-Control-Allow-Headers", "x-requested-with, origin, content-type, accept");
            context.render(json(state));
          }
        });
      });
    });

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
  }


  public void messageArrived(String aTopic, MqttMessage message) throws Exception {
    try {
      SinglePixelSensorReading reading = SinglePixelSensorReading.parseFrom(message.getPayload());

      if (reading.getClear() > 0.5) {
        System.out.println(reading.getSensorId());
      }

      algorithm.receivedReading(reading);
    } catch (InvalidProtocolBufferException ex) {
      // TODO(doug) - Log exception
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

  public void stop() throws Exception {
    server.stop();

    if (client.isConnected()) {
      client.disconnect();
    }
  }
}
