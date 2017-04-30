package edu.bu.vip.singlepixel.util;

import edu.bu.vip.singlepixel.Protos.SinglePixelSensorReading;
import java.util.ArrayList;
import java.util.List;

public class ReadingUtils {

  /**
   * Generates the sensor id for the reading.
   */
  public static String sensorKey(SinglePixelSensorReading reading) {
    return reading.getGroupId() + "/" + reading.getSensorId();
  }

  /**
   * Calculates the luminance from RGB.
   */
  public static double luminance(SinglePixelSensorReading reading) {
    final double luminance = 0.2126 * reading.getRed()
        + 0.7152 * reading.getGreen()
        + 0.0722 * reading.getBlue();
    return luminance;
  }

  /**
   * Calculates the luminance of the list using {@link #luminance(SinglePixelSensorReading)}
   */
  public static List<Double> luminance(List<SinglePixelSensorReading> readings) {
    List<Double> values = new ArrayList<>();
    readings.forEach(reading -> {
      values.add(luminance(reading));
    });
    return values;
  }

  /**
   * Splits the values into four separate lists, one per channel.
   *
   * @return Four lists, in order: (red, green, blue, clear)
   */
  public static List<List<Double>> splitValues(List<SinglePixelSensorReading> readings) {
    List<Double> red = new ArrayList<>(readings.size());
    List<Double> green = new ArrayList<>(readings.size());
    List<Double> blue = new ArrayList<>(readings.size());
    List<Double> clear = new ArrayList<>(readings.size());
    readings.forEach(reading -> {
      red.add(reading.getRed());
      green.add(reading.getGreen());
      blue.add(reading.getBlue());
      clear.add(reading.getClear());
    });

    List<List<Double>> values = new ArrayList<>(4);
    values.add(red);
    values.add(green);
    values.add(blue);
    values.add(clear);
    return values;
  }
}
