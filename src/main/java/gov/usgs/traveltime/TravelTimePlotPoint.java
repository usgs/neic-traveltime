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
