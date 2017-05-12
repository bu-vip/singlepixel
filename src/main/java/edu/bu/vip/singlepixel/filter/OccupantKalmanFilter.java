package edu.bu.vip.singlepixel.filter;

import edu.bu.vip.multikinect.Protos.Position;
import java.util.HashMap;
import java.util.Map;
import org.ejml.data.DenseMatrix64F;
import org.ejml.simple.SimpleMatrix;

/**
 * NOTE(doug): This class doesn't work, I didn't have time to finish it.
 */
public class OccupantKalmanFilter {

  private Map<Long, KalmanFilter> kalmans = new HashMap<>();
  private final DenseMatrix64F kalmanF = new DenseMatrix64F(4,4, true,
      1,0,1,0,
      0,1,0,1,
      0,0,1,0,
      0,0,0,1);
  private final DenseMatrix64F kalmanH = new DenseMatrix64F(4,2, true,
      1,0,0,0,
      0,1,0,0);
  private final DenseMatrix64F kalmanInitX = new DenseMatrix64F(4,1, true,
      0,0,0,0);
  private final DenseMatrix64F kalmanInitP = new DenseMatrix64F(4,4, true,
      1,0,0,0,
      0,1,0,0,
      0,0,1,0,
      0,0,0,1);

  public OccupantKalmanFilter() {

  }

  public Position filter(long id, Position newPos) {
    if (!kalmans.containsKey(id)) {
      KalmanFilter filter = new KalmanFilter();
      filter.configure(kalmanF, SimpleMatrix.identity(4).getMatrix(), kalmanH);
      filter.setState(kalmanInitX, kalmanInitP);
      kalmans.put(id, filter);
    }

    // Update filter
    KalmanFilter filter = kalmans.get(id);
    DenseMatrix64F posMat = new DenseMatrix64F(2,1,true, newPos.getX(), newPos.getZ());
    double error = 0.1;
    DenseMatrix64F errorMat = new DenseMatrix64F(2,2, true, error, error,error,error);
    filter.update(posMat, errorMat);
    filter.predict();
    DenseMatrix64F filteredPos = filter.getState();
    System.out.println(filteredPos);

    return newPos;
  }

  public void removeOccupant(long id) {
    if (kalmans.containsKey(id)) {
      kalmans.remove(id);
    }
  }

}
