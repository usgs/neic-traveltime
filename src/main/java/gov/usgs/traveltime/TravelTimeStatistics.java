package gov.usgs.traveltime;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Holds a set of observed travel-time statistics for one phase. There are three statistics in a
 * set: bias from the theoretical travel time, spread about the mean, and relative observability.
 * The observability is simply the number of hits each phase received in the trial data set. It's
 * absolute value is meaningless, but the relative value is useful.
 *
 * <p>Each of the three statistics is represented by a sequence of one or more linear fit segments.
 * These segments are constructed on the fly from the raw observations in one degree bins.
 *
 * @author Ray Buland
 */
public class TravelTimeStatistics implements Serializable {
  private static final long serialVersionUID = 1L;
  String phaseCode; // Phase code
  int minDelta; // Minimum distance in degrees
  int maxDelta; // Maximum distance in degrees
  ArrayList<TravelTimeStatisticsSegment> bias; // Measured arrival time bias (s)
  ArrayList<TravelTimeStatisticsSegment> spread; // Measured residual spread (s)
  ArrayList<TravelTimeStatisticsSegment> observ; // Measured observability

  /**
   * Initialize the phase statistics.
   *
   * @param phaseCode Phase code
   * @param minDelta Minimum observed distance in degrees
   * @param maxDelta Maximum observed distance in degrees
   */
  protected TravelTimeStatistics(String phaseCode, int minDelta, int maxDelta) {
    this.phaseCode = phaseCode;
    this.minDelta = minDelta;
    this.maxDelta = maxDelta;
    // set up storage for the linear fits.
    bias = new ArrayList<TravelTimeStatisticsSegment>();
    spread = new ArrayList<TravelTimeStatisticsSegment>();
    observ = new ArrayList<TravelTimeStatisticsSegment>();
  }

  /**
   * get the phase bias.
   *
   * @param delta Distance in degrees
   * @return Bias in seconds at distance delta
   */
  public double getPhaseBias(double delta) {
    TravelTimeStatisticsSegment seg;

    for (int k = 0; k < bias.size(); k++) {
      seg = bias.get(k);
      if (delta >= seg.minDelta && delta <= seg.maxDelta) {
        return seg.interp(delta);
      }
    }
    return TauUtilities.DEFAULTTTBIAS;
  }

  /**
   * Get the phase spread.
   *
   * @param delta Distance in degrees
   * @param upGoing True if the phase is an up-going P or S
   * @return Spread in seconds at distance delta
   */
  public double getPhaseSpread(double delta, boolean upGoing) {
    TravelTimeStatisticsSegment seg;

    for (int k = 0; k < spread.size(); k++) {
      seg = spread.get(k);
      if (delta >= seg.minDelta && delta <= seg.maxDelta) {
        return Math.min(seg.interp(delta), TauUtilities.DEFAULTTTSPREAD);
      }
    }
    if (upGoing) {
      return Math.min(spread.get(0).interp(delta), TauUtilities.DEFAULTTTSPREAD);
    }
    return TauUtilities.DEFAULTTTSPREAD;
  }

  /**
   * Get the phase observability.
   *
   * @param delta Distance in degrees
   * @param upGoing True if the phase is an up-going P or S
   * @return Relative observability at distance delta
   */
  public double getPhaseObservability(double delta, boolean upGoing) {
    TravelTimeStatisticsSegment seg;

    for (int k = 0; k < observ.size(); k++) {
      seg = observ.get(k);
      if (delta >= seg.minDelta && delta <= seg.maxDelta) {
        return Math.max(seg.interp(delta), TauUtilities.DEFAULTTTOBSERVABILITY);
      }
    }
    if (upGoing) {
      return Math.max(observ.get(0).interp(delta), TauUtilities.DEFAULTTTOBSERVABILITY);
    }
    return TauUtilities.DEFAULTTTOBSERVABILITY;
  }

  /**
   * Get the derivative of the phase spread with respect to distance.
   *
   * @param delta Distance in degrees
   * @param upGoing True if the phase is an up-going P or S
   * @return Spread in seconds at distance delta
   */
  public double getSpreadDerivative(double delta, boolean upGoing) {
    double deriv;
    TravelTimeStatisticsSegment seg;

    for (int k = 0; k < spread.size(); k++) {
      seg = spread.get(k);
      if (delta >= seg.minDelta && delta <= seg.maxDelta) {
        deriv = seg.deriv(delta);
        if (delta == seg.maxDelta) {
          if (++k < spread.size()) {
            seg = spread.get(k);
            if (delta == seg.minDelta) {
              deriv = (deriv + seg.deriv(delta)) / 2d;
            }
          }
        }
        return deriv;
      }
    }
    if (upGoing) {
      return spread.get(0).deriv(delta);
    }
    return 0d;
  }

  /** Print the travel-time statistics. */
  protected void dumpStats() {
    // Print the header.
    System.out.println("\n" + phaseCode + "     " + minDelta + "     " + maxDelta);

    // Print the data.
    System.out.println("Bias:");
    for (int j = 0; j < bias.size(); j++) {
      System.out.format(
          "  %3d  range = %6.2f, %6.2f  fit = %11.4e, " + "%11.4e\n",
          j, bias.get(j).minDelta, bias.get(j).maxDelta, bias.get(j).slope, bias.get(j).offset);
    }
    System.out.println("Spread:");
    for (int j = 0; j < spread.size(); j++) {
      System.out.format(
          "  %3d  range = %6.2f, %6.2f  fit = %11.4e, " + "%11.4e\n",
          j,
          spread.get(j).minDelta,
          spread.get(j).maxDelta,
          spread.get(j).slope,
          spread.get(j).offset);
    }
    System.out.println("Observability:");
    for (int j = 0; j < observ.size(); j++) {
      System.out.format(
          "  %3d  range = %6.2f, %6.2f  fit = %11.4e, " + "%11.4e\n",
          j,
          observ.get(j).minDelta,
          observ.get(j).maxDelta,
          observ.get(j).slope,
          observ.get(j).offset);
    }
  }
}
