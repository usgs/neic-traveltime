package gov.usgs.traveltime.tables;

import gov.usgs.traveltime.ModelConversions;
import org.apache.commons.math3.analysis.UnivariateFunction;

/**
 * The FindRadius class is an Univariate function class returning the difference between a non-
 * dimensional slowness and a target slowness as a function of radius. This is intended to be used
 * with the Apache Commons math library suite of root finders
 *
 * @author Ray Buland
 */
public class FindRadius implements UnivariateFunction {
  /** A char containing the wave type ('P' = compressional, 'S' = shear) */
  private char waveType;

  /** An integer containing the shell index */
  private int shellIndex;

  /** A double holding the non-dimensional target ray parameter */
  private double rayTarget;

  /** A EarthModel object containing the reference earth model */
  private EarthModel referenceModel;

  /** A ModelConversions object containing model dependent constants and conversions */
  private ModelConversions modelConversions;

  /**
   * FindRadius constructor setting up the the reference Earth model and conversion factors.
   *
   * @param referenceModel Reference Earth model
   * @param modelConversions Model specific conversion factors
   */
  public FindRadius(EarthModel referenceModel, ModelConversions modelConversions) {
    this.referenceModel = referenceModel;
    this.modelConversions = modelConversions;
  }

  /**
   * Function to set up the root finding environment.
   *
   * @param waveType A char containing the wave waveType ('P' = compressional, 'S' = shear)
   * @param shellIndex An integer containing the shell index
   * @param rayTarget A double containing Non-dimensional target ray parameter
   */
  public void setUp(char waveType, int shellIndex, double rayTarget) {
    this.waveType = waveType;
    this.shellIndex = shellIndex;
    this.rayTarget = rayTarget;
  }

  /**
   * Function to calculate the difference between a non- dimensional slowness and a target slowness
   * as a function of radius.
   *
   * @param radius A double containing the radius in kilometers
   * @return A double containing the difference between a non- dimensional slowness and a target
   *     slowness as a function of radius.
   */
  @Override
  public double value(double radius) {
    return modelConversions.flatP(referenceModel.getVelocity(waveType, shellIndex, radius), radius)
        - rayTarget;
  }
}
