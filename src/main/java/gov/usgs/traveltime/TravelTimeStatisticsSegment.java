package gov.usgs.traveltime;

import java.io.Serializable;

/**
 * Holds one travel-time statistics linear interpolation segment for one statistics variable.
 *
 * @author Ray Buland
 */
public class TravelTimeStatisticsSegment implements Serializable {
  private static final long serialVersionUID = 1L;
  double minDelta; // Minimum distance in degrees
  double maxDelta; // Maximum distance in degrees
  double slope; // Slope of linear interpolation
  double offset; // Offset of linear interpolation

  /**
   * Create the linear segment.
   *
   * @param minDelta Minimum distance in degrees
   * @param maxDelta Maximum distance in degrees
   * @param slope Slope of the linear fit
   * @param offset Offset of the linear fit
   */
  protected TravelTimeStatisticsSegment(
      double minDelta, double maxDelta, double slope, double offset) {
    this.minDelta = minDelta;
    this.maxDelta = maxDelta;
    this.slope = slope;
    this.offset = offset;
  }

  /**
   * Interpolate the linear fit at one distance. Note that the interpolation will be done for
   * distances less than the minimum, but fixed at the minimum distance. This is a hack needed for
   * up-going P and S at short distances from deep sources.
   *
   * @param delta Distance in degrees where statistics are desired
   * @return Interpolated parameter
   */
  protected double interp(double delta) {
    if (delta <= maxDelta) {
      return offset + Math.max(delta, minDelta) * slope;
    }
    return Double.NaN;
  }

  /**
   * Get the derivative of the linear fit at one distance. Note that the interpolation will be done
   * for distances less than the minimum, but fixed at the minimum distance. This is a hack needed
   * for up-going P and S at short distances from deep sources.
   *
   * @param delta Distance in degrees where derivatives are desired
   * @return Derivative of the parameter
   */
  protected double deriv(double delta) {
    if (delta <= maxDelta) {
      return slope;
    }
    return Double.NaN;
  }
}
