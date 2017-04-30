package edu.bu.vip.singlepixel.demo;

import static ratpack.jackson.Jackson.json;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.InvalidProtocolBufferException;
import edu.bu.vip.multikinect.Protos.Position;
import edu.bu.vip.singlepixel.Protos.SinglePixelSensorReading;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.guice.Guice;
import ratpack.http.Status;
import ratpack.server.RatpackServer;

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

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private Algorithm2 algorithm;
  private RatpackServer server;
  private MqttClient client;
  private final Object occupantMapLock = new Object();
  private ImmutableMap<Long, Position> occupantMap;
  private final Object backgroundLock = new Object();
  private boolean capturingBackground = false;
  private FileOutputStream stream;
  private Timer backgroundTimer = new Timer();

  public void start() throws Exception {
    // Load model with the algorithm
    String modelDir = "/home/doug/Desktop/multikinect/models/running_mean_model.pb";
    algorithm = Algorithm2.builder()
        .setBackgroundSubtraction(true)
        .setCalcLuminance(false)
        .setInputNodeName("input")
        .setNumPastReadings(1)
        .setOutputNodeName("output")
        .setModelPath(modelDir)
        .setNumSensors(11)
        .build();

    stream = new FileOutputStream("/home/doug/Desktop/boundary_black.pbdat");

    server = RatpackServer.start(s -> {
      s.serverConfig(config -> {
        config.port(8080);
      });
      s.registry(Guice.registry(b -> {
      }));
      s.handlers(chain -> {
        chain.all(handler -> {
          // TODO(doug) - This could be handled better
          handler.getResponse().getHeaders().set("Access-Control-Allow-Origin", "*");
          handler.getResponse().getHeaders()
              .set("Access-Control-Allow-Headers", "x-requested-with, origin, content-type, accept");
          handler.next();
        });


        chain.get("_/state", context -> {
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
                posMap.put("x", position.getX());
                posMap.put("y", position.getZ());

                Map<String, Object> occupant = new HashMap<>();
                occupant.put("id", id);
                occupant.put("position", posMap);

                occupants.add(occupant);
              });
            }
          }
          state.put("occupants", occupants);

          context.render(json(state));
        });

        chain.get("_/background", ctx -> {
          synchronized (backgroundLock) {
            if (!capturingBackground) {
              capturingBackground = true;
              backgroundTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                  synchronized (backgroundLock) {
                    capturingBackground = false;
                  }
                }
              }, 30000);
            }
          }

          ctx.getResponse().status(Status.OK).send();
        });
      });
    });

    try {
      MemoryPersistence persistence = new MemoryPersistence();
      String clientId = "data-recorder-" + (int) (Math.random() * 99999);
      client = new MqttClient("tcp://127.0.0.1:1883", clientId, persistence);
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

      synchronized (backgroundLock) {
        if (capturingBackground) {
          algorithm.addBackgroundReading(reading);
        } else {
          algorithm.addReading(reading);

          Position pos = algorithm.predict();
          synchronized (occupantMapLock) {
            occupantMap = ImmutableMap.of(0L, pos);
          }
          reading.writeDelimitedTo(stream);
        }
      }


    } catch (InvalidProtocolBufferException ex) {
      // TODO(doug) - Log exception
    }
  }

  public void deliveryComplete(IMqttDeliveryToken arg0) {

  }

  public void connectionLost(Throwable arg0) {
    try {
      logger.error("Lost connection:", arg0);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void stop() throws Exception {
    server.stop();

    if (client.isConnected()) {
      client.disconnect();
    }

    stream.close();
  }
}
