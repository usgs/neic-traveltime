package gov.usgs.traveltime.tables;

import gov.usgs.traveltime.ModConvert;
import org.apache.commons.math3.analysis.UnivariateFunction;

/**
 * Univariate function returning the difference between a non- dimensional slowness and a target
 * slowness as a function of radius. This is intended to be used with the Apache Commons math
 * library suite of root finders
 *
 * @author Ray Buland
 */
public class FindRadius implements UnivariateFunction {
  char type;
  int iShell;
  double pTarget;
  EarthModel refModel;
  ModConvert convert;

  /**
   * We'll need the reference Earth model and conversion factors.
   *
   * @param refModel Reference Earth model
   * @param convert Model specific conversion factors
   */
  public FindRadius(EarthModel refModel, ModConvert convert) {
    this.refModel = refModel;
    this.convert = convert;
  }

  /**
   * Set up the root finding environment.
   *
   * @param type Wave type (P = P-waves, S = S-waves)
   * @param iShell Shell index
   * @param pTarget Non-dimensional target ray parameter
   */
  public void setUp(char type, int iShell, double pTarget) {
    this.type = type;
    this.iShell = iShell;
    this.pTarget = pTarget;
  }

  @Override
  public double value(double r) {
    return convert.flatP(refModel.getVel(type, iShell, r), r) - pTarget;
  }
}
