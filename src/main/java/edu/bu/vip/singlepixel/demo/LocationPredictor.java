package edu.bu.vip.singlepixel.demo;

import com.google.common.collect.ImmutableList;
import edu.bu.vip.multikinect.Protos.Position;
import java.util.List;

/**
 * Predicts the location of one or more people. Used by {@link LocalizationAlgorithm}
 */
public interface LocationPredictor {

  /**
   * Predicts locations using the input feature. See {@link LocalizationAlgorithm} for details on the feature
   * vector.
   */
  ImmutableList<Position> predict(float[] feature);

}
