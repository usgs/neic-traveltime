package gov.usgs.traveltime;

import java.io.Serializable;

/**
 * Holds one travel-time statistics linear interpolation segment for one statistics variable.
 *
 * @author Ray Buland
 */
public class TravelTimeStatisticsSegment implements Serializable {
  /** A long containing the version id used in serialization */
  private static final long serialVersionUID = 1L;

  /** A double containing the minimum distance in degrees */
  private double minimumDistance;

  /** A double containing the maximum distance in degrees */
  private double maximumDistance;

  /** A double containing the slope of linear interpolation */
  private double linearFitSlope;

  /** A double containing the offset of linear interpolation */
  private double linearFitOffset;

  /**
   * Set the miniumum distance.
   *
   * @param minimumDistance A double value containing the miniumum distance in degrees
   */
  public void setMinimumDistance(double minimumDistance) {
    this.minimumDistance = minimumDistance;
  }

  /**
   * Get the miniumum distance.
   *
   * @return A double value containing the miniumum distance in degrees
   */
  public double getMinimumDistance() {
    return minimumDistance;
  }

  /**
   * Set the maximum distance.
   *
   * @param maximumDistance A double value containing the maximum distance in degrees
   */
  public void setMaximumDistance(double maximumDistance) {
    this.maximumDistance = maximumDistance;
  }

  /**
   * Get the maximum distance.
   *
   * @return A double value containing the maximum distance in degrees
   */
  public double getMaximumDistance() {
    return maximumDistance;
  }

  /**
   * Get the slope of linear interpolation
   *
   * @return A double containing the slope of linear interpolation
   */
  public double getLinearFitSlope() {
    return linearFitSlope;
  }

  /**
   * Get the offset of linear interpolation
   *
   * @return A double containing the offset of linear interpolation
   */
  public double getLinearFitOffset() {
    return linearFitOffset;
  }

  /**
   * TravelTimeStatisticsSegment constructor, creates the linear segment.
   *
   * @param minimumDistance Minimum distance in degrees
   * @param maximumDistance Maximum distance in degrees
   * @param linearFitSlope Slope of the linear fit
   * @param linearFitOffset Offset of the linear fit
   */
  protected TravelTimeStatisticsSegment(
      double minimumDistance,
      double maximumDistance,
      double linearFitSlope,
      double linearFitOffset) {
    this.minimumDistance = minimumDistance;
    this.maximumDistance = maximumDistance;
    this.linearFitSlope = linearFitSlope;
    this.linearFitOffset = linearFitOffset;
  }

  /**
   * Function to interpolate the linear fit at one distance. Note that the interpolation will be
   * done for distances less than the minimum, but fixed at the minimum distance. This is a hack
   * needed for up-going P and S at short distances from deep sources.
   *
   * @param distance Distance in degrees where statistics are desired
   * @return Interpolated parameter
   */
  protected double interpolate(double distance) {
    if (distance <= maximumDistance) {
      return linearFitOffset + Math.max(distance, minimumDistance) * linearFitSlope;
    }

    return Double.NaN;
  }

  /**
   * Function to get the derivative of the linear fit at one distance. Note that the interpolation
   * will be done for distances less than the minimum, but fixed at the minimum distance. This is a
   * hack needed for up-going P and S at short distances from deep sources.
   *
   * @param distance Distance in degrees where derivatives are desired
   * @return Derivative of the parameter
   */
  protected double getDerivative(double distance) {
    if (distance <= maximumDistance) {
      return linearFitSlope;
    }

    return Double.NaN;
  }
}
