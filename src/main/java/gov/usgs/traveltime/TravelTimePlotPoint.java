package gov.usgs.traveltime;

/**
 * Store plot data for one distance for one phase.
 *
 * @author Ray Buland
 */
public class TravelTimePlotPoint implements Comparable<TravelTimePlotPoint> {
  /** A double containing the distance in degrees */
  private double distance;

  /** A double holding the travel time in seconds */
  private double travelTime;

  /** A double holding the statistical spread in seconds */
  private double spread;

  /** A double holding the relative observability */
  private double observability;

  /** A double holding the ray parameter in seconds/degree */
  double rayParameter;

  /**
   * Get the distance.
   *
   * @return A double value containing the distance in degrees
   */
  public double getDistance() {
    return distance;
  }

  /**
   * Get the travel time.
   *
   * @return A double value containing the travel time in seconds
   */
  public double getTravelTime() {
    return travelTime;
  }

  /**
   * Get the statistical spread (scatter) of the travel time
   *
   * @return A double value holding the statistical spread (scatter) of the travel time in seconds
   */
  public double getStatisticalSpread() {
    return spread;
  }

  /**
   * Get the relative statistical observability of the travel time
   *
   * @return A double value holding the relative statistical observability of the travel time
   */
  public double getObservability() {
    return observability;
  }

  /**
   * Get the ray parameter derivative.
   *
   * @return A double value containing the derivative of distance with respect to ray parameter in
   *     degree seconds (degree-s)
   */
  public double getRayParameter() {
    return rayParameter;
  }

  /**
   * The TravelTimePlotPoint constructor, saves plot data.
   *
   * @param distance A double containing the distance in degrees
   * @param travelTime A double holding the travel time in seconds
   * @param spread A double holding the statistical spread in seconds
   * @param observability A double holding the relative observability
   * @param rayParameter A double holding the ray parameter in seconds/degree
   */
  public TravelTimePlotPoint(
      double distance,
      double travelTime,
      double spread,
      double observability,
      double rayParameter) {
    this.distance = distance;
    this.travelTime = travelTime;

    if (spread < TauUtilities.DEFAULTTTSPREAD) {
      this.spread = spread;
      this.observability = observability;
    } else {
      this.spread = Double.NaN;
      this.observability = Double.NaN;
    }
    this.rayParameter = Math.abs(rayParameter);
  }

  /**
   * Function to compare a TravelTimePlotPoint with this one for sorting. The sort is by the ray
   * parameter
   *
   * @param point A TravelTimePlotPoint containing the travel time plot value to compare
   * @return An int representing whether the given travel time plot ray paramteter value is
   *     "greater", "lesser", or equal.
   */
  @Override
  public int compareTo(TravelTimePlotPoint point) {
    // Sort into decreasing ray parameter order.
    if (this.rayParameter < point.rayParameter) {
      return +1;
    } else if (this.rayParameter == point.rayParameter) {
      return 0;
    } else {
      return -1;
    }
  }

  /**
   * Function to convert this TravelTimePlotPoint into a string.
   *
   * @return A String containing the string representation of this TravelTimePlotPoint
   */
  @Override
  public String toString() {
    if (!Double.isNaN(spread)) {
      return String.format(
          "%5.1f %7.2f +/- %5.2f (%7.1f)", distance, travelTime, spread, observability);
    } else {
      return String.format("%5.1f %7.2f", distance, travelTime);
    }
  }
}
