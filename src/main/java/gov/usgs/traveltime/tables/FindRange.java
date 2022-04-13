package gov.usgs.traveltime.tables;

import gov.usgs.traveltime.TravelTimeStatus;
import org.apache.commons.math3.analysis.UnivariateFunction;

/**
 * The FindRange class is an univariate function returning the difference between a non-dimensional
 * range (ray travel distance) and a target range as a function of ray parameter. This is intended
 * to be used with the Apache Commons math library suite of root finders
 *
 * @author Ray Buland
 */
public class FindRange implements UnivariateFunction {
  /** A char containing the wave type ('P' = compressional, 'S' = shear) */
  private char waveType;

  /** An integer containing the index of the deepest model level to integrate to */
  private int modelLimitIndex;

  /** A double containing the non-dimensional target range (ray travel distance) */
  private double targetRange;

  /** A TauInt object containing the Tau-X integration logic */
  private TauIntegrate tauInt;

  /**
   * FindRange constructor, stores the tau-x integration routine.
   *
   * @param tauInt A TauInt object containing the Tau-X integration logic
   */
  public FindRange(TauIntegrate tauInt) {
    this.tauInt = tauInt;
  }

  /**
   * Function to set up the root finding environment.
   *
   * @param waveType A char containing the wave type ('P' = compressional, 'S' = shear)
   * @param targetRange Non-dimensional target range (ray travel distance)
   * @param modelLimitIndex An integer containing the index of the deepest model level to integrate
   *     to
   */
  public void setUp(char waveType, double targetRange, int modelLimitIndex) {
    this.waveType = waveType;
    this.targetRange = targetRange;
    this.modelLimitIndex = modelLimitIndex;
  }

  /**
   * Function to calculate difference between a non-dimensional range (ray travel distance) and a
   * target range as a function of ray parameter.
   *
   * @param rayParameter A double containing the normalized ray parameter
   * @return A double containing the difference between a non-dimensional range (ray travel
   *     distance) and a target range as a function of ray parameter.
   */
  @Override
  public double value(double rayParameter) {
    try {
      tauInt.integrateDist(waveType, rayParameter, modelLimitIndex);
    } catch (Exception e) {
      System.out.println("Bad tau integration interval!");
      e.printStackTrace();
      System.exit(TravelTimeStatus.BAD_TAU_INTERVAL.status());
    }

    return tauInt.getSummaryIntDist() - targetRange;
  }
}
