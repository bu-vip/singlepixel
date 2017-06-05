package edu.bu.vip.singlepixel.demo;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.Subscribe;
import com.google.protobuf.InvalidProtocolBufferException;
import edu.bu.vip.kinect.controller.calibration.Protos.Calibration;
import edu.bu.vip.kinect.controller.realtime.Protos.SyncedFrame;
import edu.bu.vip.multikinect.Protos.Position;
import edu.bu.vip.multikinect.controller.Controller;
import edu.bu.vip.multikinect.sync.PositionUtils;
import edu.bu.vip.multikinect.sync.SkeletonUtils;
import edu.bu.vip.singlepixel.Protos.SinglePixelSensorReading;
import edu.bu.vip.singlepixel.demo.Protos.Bounds;
import edu.bu.vip.singlepixel.demo.Protos.Occupant;
import edu.bu.vip.singlepixel.filter.OccupantKalmanFilter;
import edu.bu.vip.singlepixel.tensorflow.TensorFlowLocationPredictor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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

public class Demo implements MqttCallback {
  // Time to capture background, in milliseconds
  private static final int BACKGROUND_CAPTURE_TIME = 30000;

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final String modelPath;
  private final String recordingDir;
  private final String multiKinectDataDir;
  private final long calibrationId;
  private final String mqttBroker;

  private Controller controller;
  private LocalizationAlgorithm algorithm;
  private MqttClient client;

  private final Object backgroundLock = new Object();
  private boolean capturingBackground = false;
  private Timer backgroundTimer = new Timer();

  private final Object recordingLock = new Object();
  private boolean recording = false;
  private FileOutputStream stream;

  private final Object estOccupantLock = new Object();
  private ImmutableList<Position> estOccupantList = ImmutableList.of();
  private final Object trueOccupantLock = new Object();
  private ImmutableList<Position> trueOccupants = ImmutableList.of();

  // NOTE(doug): Kalman filter doesn't work. It isn't fully implemented.
  private final OccupantKalmanFilter kalmanFilter = new OccupantKalmanFilter();
  private final boolean applyKalman = false;

  public Demo(String modelPath, String recordingDir, String multiKinectDataDir, long calibrationId,
      String mqttBroker) {
    this.modelPath = modelPath;
    this.recordingDir = recordingDir;
    this.multiKinectDataDir = multiKinectDataDir;
    this.calibrationId = calibrationId;
    this.mqttBroker = mqttBroker;
  }

  public void start(int num_sensors) throws Exception {
    // Create predictor
    LocationPredictor predictor = TensorFlowLocationPredictor.builder()
        .setInputNodeName("input")
        .setModelPath(modelPath)
        .setOutputNodeName("output")
        .build();

    // Load algorithm
    algorithm = LocalizationAlgorithm.builder()
        .setBackgroundSubtraction(true)
        .setCalcLuminance(false)
        .setNumPastReadings(1)
        .setPredictor(predictor)
        .setNumSensors(num_sensors)
        .setClearBufferOnPredict(true)
        .build();

    // Connect to mqtt
    try {
      MemoryPersistence persistence = new MemoryPersistence();
      String clientId = "demo-" + (int) (Math.random() * 99999);
      client = new MqttClient(mqttBroker, clientId, persistence);

      MqttConnectOptions connOpts = new MqttConnectOptions();
      connOpts.setCleanSession(true);
      client.connect(connOpts);
      client.setCallback(this);

      // subscribe to receive sensor readings
      String subscribeDest = "/#";
      client.subscribe(subscribeDest);
    } catch (MqttException e) {
      throw new RuntimeException("Error creating MQTT client.");
    }

    // Start MultiKinect controller
    controller = new Controller(multiKinectDataDir);
    controller.start();

    // Load calibration
    Optional<Calibration> optCal = controller.getCalibrationStore().getCalibration(calibrationId);
    if (!optCal.isPresent()) {
      logger.error("Couldn't get calibration {}", calibrationId);
      throw new RuntimeException("Couldn't get MultiKinect calibration");
    }
    Calibration calibration = optCal.get();
    controller.getRealTimeManager().start(calibration);

    // Subscribe to synced frame events
    controller.getRealTimeManager().getSyncedFrameBus().register(this);
  }

  @Subscribe
  public void onSyncedFrameReceived(SyncedFrame syncedFrame) {
    // Convert frame to occupants
    ImmutableList.Builder<Position> occBuilder = ImmutableList.builder();
    syncedFrame.getSkeletonsList().forEach(skeleton -> {
      Position center = SkeletonUtils.calculateCenter(skeleton.getSkeleton(), false);
      occBuilder.add(center);
    });

    // Update list
    synchronized (trueOccupantLock) {
      trueOccupants = occBuilder.build();
    }
  }

  public boolean isCapturingBackground() {
    return capturingBackground;
  }

  public boolean isRecording() {
    return recording;
  }

  public void captureBackground() {
    synchronized (backgroundLock) {
      if (!capturingBackground) {
        logger.info("Starting background capture");
        capturingBackground = true;
        backgroundTimer.schedule(new TimerTask() {
          @Override
          public void run() {
            synchronized (backgroundLock) {
              logger.info("Finished background capture");
              capturingBackground = false;
            }
          }
        }, BACKGROUND_CAPTURE_TIME);
      } else {
        logger.info("Already capturing background");
      }
    }
  }

  public void toggleRecording() {
    synchronized (recordingLock) {
      if (recording) {
        try {
          stream.close();
        } catch (IOException e) {
          logger.error("Error closing recording stream", e);
        }
        stream = null;
        recording = false;
      } else {
        String filename = Instant.now().toString() + ".pbdat";
        try {
          stream = new FileOutputStream(new File(recordingDir, filename));
          recording = true;
        } catch (FileNotFoundException e) {
          logger.error("Error creating recording stream", e);
        }
      }
    }
  }

  public List<Occupant> getOccupants() {
    synchronized (estOccupantLock) {
      synchronized (trueOccupantLock) {
        if (estOccupantList.size() > 0) {
          Occupant.Builder builder = Occupant.newBuilder();
          builder.setId(0);

          Position orig = estOccupantList.get(0);
          Position filtered = (applyKalman ? kalmanFilter.filter(builder.getId(), orig) : orig);

          builder.setEstimatedPosition(filtered);
          if (trueOccupants.size() > 0) {
            builder.setTruePosition(trueOccupants.get(0));
          }
          builder.setDistance(PositionUtils.distanceXZ(
              builder.getTruePosition(),
              builder.getEstimatedPosition()
          ));

          return ImmutableList.of(builder.build());
        }

        return ImmutableList.of();
      }
    }
  }


  public Bounds getBounds() {
    // TODO(doug) - Don't hardcode, these should be loaded from model / data set
    Bounds.Builder builder = Bounds.newBuilder();
    builder.setMinX(-1.5);
    builder.setMaxX(1.5);
    builder.setMinZ(1.0);
    builder.setMaxZ(3.5);
    return builder.build();
  }


  public void messageArrived(String aTopic, MqttMessage message) throws Exception {
    try {
      SinglePixelSensorReading reading = SinglePixelSensorReading.parseFrom(message.getPayload());

      synchronized (backgroundLock) {
        if (capturingBackground) {
          algorithm.addBackgroundReading(reading);
        } else {
          algorithm.addReading(reading);

          // Run algorithm
          if (algorithm.canPredict()) {
            ImmutableList<Position> positions = algorithm.predict();
            synchronized (estOccupantLock) {
              estOccupantList = ImmutableList.copyOf(positions);
            }
          }

          synchronized (recordingLock) {
            if (recording) {
              reading.writeDelimitedTo(stream);
            }
          }
        }
      }


    } catch (InvalidProtocolBufferException ex) {
      // TODO(doug) - Log exception
    }
    catch (Exception e) {
      logger.error("Error:", e);
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

    controller.getRealTimeManager().getSyncedFrameBus().unregister(this);
    controller.stop();

    if (client.isConnected()) {
      client.disconnect();
    }

    stream.close();
  }
}
