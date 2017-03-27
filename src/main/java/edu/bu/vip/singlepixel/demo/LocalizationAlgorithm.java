package edu.bu.vip.singlepixel.demo;

import com.google.common.collect.ImmutableMap;
import edu.bu.vip.singlepixel.Protos.SinglePixelSensorReading;
import java.util.List;
import java.util.function.Consumer;

public interface LocalizationAlgorithm {
  void setListener(Consumer<ImmutableMap<Long, List<Double>>> listener);
  void receivedReading(SinglePixelSensorReading reading);
}
