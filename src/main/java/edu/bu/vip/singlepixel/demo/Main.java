package edu.bu.vip.singlepixel.demo;

import static ratpack.jackson.Jackson.json;

import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Floats;
import com.google.protobuf.InvalidProtocolBufferException;
import edu.bu.vip.singlepixel.Protos.SinglePixelSensorReading;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

  private TensorflowLocalizationAlgorithm algorithm;
  private RatpackServer server;
  private MqttClient client;
  private final Object occupantMapLock = new Object();
  private ImmutableMap<Long, List<Double>> occupantMap;

  public void start() throws Exception {

    String modelDir = "/home/doug/Development/bu_code/research/singlepixellocalization/src/main/python/tensorflow/models/test4_model.pb";
    algorithm = new TensorflowLocalizationAlgorithm(modelDir);
    algorithm.setListener((newMap) -> {
      synchronized (occupantMapLock) {
        occupantMap = newMap;
      }
    });



    /*
    String testFile = "/home/doug/Development/bu_code/research/singlepixellocalization/src/main/python/tensorflow/test.csv";
    try (BufferedReader br = new BufferedReader(new FileReader(testFile))) {
      double totalDistance = 0;
      long total = 0;
      String line;

      while ((line = br.readLine()) != null) {
        String[] tmp = line.split(",");
        List<String> values = Arrays.asList(tmp);
        float[] labelArray = new float[2];
        List<SinglePixelSensorReading> readings = new ArrayList<>();
        SinglePixelSensorReading.Builder builder = null;
        int labelIndex = 0;
        int featureIndex = 0;
        int[] ids = {
            0,
            1,
            10,
            11,
            2,
            3,
            5,
            6,
            7,
            8,
            9
        };
        for (float val : Floats.stringConverter().convertAll(values)) {
          if (labelIndex < labelArray.length) {
            labelArray[labelIndex] = val;
            labelIndex++;
          } else {
            switch (featureIndex % 4) {
              case 0:
                builder = SinglePixelSensorReading.newBuilder();
                builder.setGroupId("0");
                builder.setSensorId("" + ids[featureIndex / 4]);
                builder.setRed(val);
                break;
              case 1:
                builder.setGreen(val);
                break;
              case 2:
                builder.setBlue(val);
                break;
              case 3:
                builder.setClear(val);
                readings.add(builder.build());
                algorithm.receivedReading(builder.build());
                break;
            }
            featureIndex++;
          }
        }

        float[] position = algorithm.predictFromReadings(readings);

        float diffX = position[0] - labelArray[0];
        float diffY = position[1] - labelArray[1];
        double distance = Math.sqrt(diffX * diffX + diffY * diffY);
        totalDistance += distance;
        total++;
      }

      double meanDistance = totalDistance / total;
      System.out.println(meanDistance);
    } catch (IOException e){

    }
    */





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

            Map<String, Object> bounds = new HashMap<>();
            bounds.put("minX", -1.1817513);
            bounds.put("maxX", 1.1170805);
            bounds.put("minY", -1.1817513);
            bounds.put("maxY", 3.3466797);
            state.put("bounds", bounds);

            List<Object> occupants = new ArrayList<>();
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
