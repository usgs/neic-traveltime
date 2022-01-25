package gov.usgs.traveltime.tables;

import org.apache.commons.math3.analysis.UnivariateFunction;

/**
 * The FindCaustic class is an univariate function class returning the non-dimensional derivative of
 * range (ray travel distance) as a function of ray parameter. This is intended to be used with the
 * Apache Commons math library suite of root finders
 *
 * @author Ray Buland
 */
public class FindCaustic implements UnivariateFunction {
  /** A char containing the wave type ('P' = compressional, 'S' = shear) */
  private char waveType;

  /** An integer containing the index of the deepest model level to integrate to */
  private int modelLimitIndex;

  /** A TauInt object containing the Tau-X integration logic */
  private TauInt tauInt;

  /**
   * FindCaustic constructor, stores the tau-x integration routine.
   *
   * @param tauInt A TauInt object containing the Tau-X integration logic
   */
  public FindCaustic(TauInt tauInt) {
    this.tauInt = tauInt;
  }

  /**
   * Function to set up the root finding environment.
   *
   * @param waveType A char containing the wave type ('P' = compressional, 'S' = shear)
   * @param modelLimitIndex An integer containing the index of the deepest model level to integrate
   *     to
   */
  public void setUp(char waveType, int modelLimitIndex) {
    this.waveType = waveType;
    this.modelLimitIndex = modelLimitIndex;
  }

  /**
   * Function to calculate the Normalized integrated tau.
   *
   * @param rayParameter A double containing the normalized ray parameter
   * @return A double containing the normalized integrated tau
   */
  @Override
  public double value(double rayParameter) {
    double normIntTau = tauInt.intDxDp(waveType, rayParameter, modelLimitIndex);

    // normIntTau blows up at the top of shells.  Back off until we get
    // a finite value.
    while (Double.isNaN(normIntTau)) {
      rayParameter -= TablesUtil.SLOWOFF;
      normIntTau = tauInt.intDxDp(waveType, rayParameter, modelLimitIndex);
    }

    return normIntTau;
  }
}
