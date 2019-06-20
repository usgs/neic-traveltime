package gov.usgs.traveltime.tables;

import gov.usgs.traveltime.TtStatus;
import org.apache.commons.math3.analysis.UnivariateFunction;

/**
 * Univariate function returning the difference between a non- dimensional range (ray travel
 * distance) and a target range as a function of ray parameter. This is intended to be used with the
 * Apache Commons math library suite of root finders
 *
 * @author Ray Buland
 */
public class FindRange implements UnivariateFunction {
  char type;
  int limit;
  double xTarget;
  TauInt tauInt;

  /**
   * Remember the tau-x integration routine.
   *
   * @param tauInt Tau-X integration logic
   */
  public FindRange(TauInt tauInt) {
    this.tauInt = tauInt;
  }

  /**
   * Set up the root finding environment.
   *
   * @param type Wave type (P = P-waves, S = S-waves)
   * @param xTarget Non-dimensional target range (ray travel distance)
   * @param limit Index of the deepest model level to integrate to
   */
  public void setUp(char type, double xTarget, int limit) {
    this.type = type;
    this.xTarget = xTarget;
    this.limit = limit;
  }

  @Override
  public double value(double p) {
    try {
      tauInt.intX(type, p, limit);
    } catch (Exception e) {
      System.out.println("Bad tau integration interval!");
      e.printStackTrace();
      System.exit(TtStatus.BAD_TAU_INTERVAL.status());
    }
    return tauInt.getXSum() - xTarget;
  }
}
