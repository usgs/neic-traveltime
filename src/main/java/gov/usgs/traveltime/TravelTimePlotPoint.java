package gov.usgs.traveltime;

/**
 * Store plot data for one distance for one phase.
 *
 * @author Ray Buland
 */
public class TravelTimePlotPoint implements Comparable<TravelTimePlotPoint> {
  double delta; // Distance in degrees
  double tt; // Travel time in seconds
  double spread; // Statistical spread in seconds
  double observ; // Relative observability
  double dTdD; // Ray parameter in seconds/degree

  /**
   * Get the distance.
   *
   * @return A double value containing the distance in degrees
   */
  public double getDistance() {
    return delta;
  }

  /**
   * Get the travel time.
   *
   * @return A double value containing the travel time in seconds
   */
  public double getTravelTime() {
    return tt;
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
    return observ;
  }

  /**
   * Get the ray parameter derivative.
   *
   * @return A double value containing the derivative of distance with respect to ray parameter in
   *     degree seconds (degree-s)
   */
  public double getRayParameter() {
    return dTdD;
  }

  /**
   * Save plot data.
   *
   * @param delta Distance in degrees
   * @param tt Travel time in seconds
   * @param spread Statistical spread in seconds
   * @param observ Relative observability
   * @param dTdD Ray parameter in seconds/degree
   */
  public TravelTimePlotPoint(double delta, double tt, double spread, double observ, double dTdD) {
    this.delta = delta;
    this.tt = tt;
    if (spread < TauUtilities.DEFAULTTTSPREAD) {
      this.spread = spread;
      this.observ = observ;
    } else {
      this.spread = Double.NaN;
      this.observ = Double.NaN;
    }
    this.dTdD = Math.abs(dTdD);
  }

  /**
   * We need to be able to sort the plot points into ray parameter order so caustics will look OK.
   */
  @Override
  public int compareTo(TravelTimePlotPoint point) {
    // Sort into decreasing ray parameter order.
    if (this.dTdD < point.dTdD) return +1;
    else if (this.dTdD == point.dTdD) return 0;
    else return -1;
  }

  @Override
  public String toString() {
    if (!Double.isNaN(spread)) {
      return String.format("%5.1f %7.2f +/- %5.2f (%7.1f)", delta, tt, spread, observ);
    } else {
      return String.format("%5.1f %7.2f", delta, tt);
    }
  }
}
