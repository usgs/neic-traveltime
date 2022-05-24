package gov.usgs.traveltime;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * The TravelTimeStatistics class holds a set of observed travel-time statistics for one phase.
 * There are three statistics in a set: bias from the theoretical travel time, spread about the
 * mean, and relative observability. The observability is simply the number of hits each phase
 * received in the trial data set. It's absolute value is meaningless, but the relative value is
 * useful.
 *
 * <p>Each of the three statistics is represented by a sequence of one or more linear fit segments.
 * These segments are constructed on the fly from the raw observations in one degree bins.
 *
 * @author Ray Buland
 */
public class TravelTimeStatistics implements Serializable {
  /** A long containing the version id used in serialization */
  private static final long serialVersionUID = 1L;

  /** A String containing the phase code for these statistics */
  private String phaseCode;

  /** An integer value containing the miniumum observed distance in degrees */
  private int minimumDistance;

  /** An integer value containing the maximum observed distance in degrees */
  private int maximumDistance;

  /**
   * An ArrayList of TravelTimeStatisticsSegment objects containing the measured residual bias in
   * seconds
   */
  private ArrayList<TravelTimeStatisticsSegment> residualBias;

  /**
   * An ArrayList of TravelTimeStatisticsSegment objects containing the measured residual spread in
   * seconds
   */
  private ArrayList<TravelTimeStatisticsSegment> spread;

  /**
   * An ArrayList of TravelTimeStatisticsSegment objects containing the measured number of times
   * this phase was observed in the defining study
   */
  private ArrayList<TravelTimeStatisticsSegment> observability;

  /**
   * Get the phase code for these statistic
   *
   * @return A String containing the phase code for these statistic
   */
  public String getPhaseCode() {
    return phaseCode;
  }

  /**
   * Get the miniumum observed distance.
   *
   * @return An integer value containing the miniumum observed distance in degrees
   */
  public int getMinimumDistance() {
    return minimumDistance;
  }

  /**
   * Get the maxiumum observed distance.
   *
   * @return An integer value containing the maxiumum observed distance in degrees
   */
  public int getMaxiumumDistance() {
    return maximumDistance;
  }

  /**
   * Get the measured residual bias
   *
   * @return An ArrayList of TravelTimeStatisticsSegment objects containing the measured residual
   *     bias in seconds
   */
  public ArrayList<TravelTimeStatisticsSegment> getResidualBias() {
    return residualBias;
  }

  /**
   * Get the measured residual spread
   *
   * @return An ArrayList of TravelTimeStatisticsSegment objects containing the measured residual
   *     spread in seconds
   */
  public ArrayList<TravelTimeStatisticsSegment> getSpread() {
    return spread;
  }

  /**
   * Get the measured observability
   *
   * @return An ArrayList of TravelTimeStatisticsSegment objects containing the measured number of
   *     times this phase was observed in the defining study
   */
  public ArrayList<TravelTimeStatisticsSegment> getObservability() {
    return observability;
  }

  /**
   * Initialize the phase statistics.
   *
   * @param phaseCode Phase code
   * @param minimumDistance An integer value containing the miniumum observed distance in degrees
   * @param maximumDistance Maximum observed distance in degrees
   */
  protected TravelTimeStatistics(String phaseCode, int minimumDistance, int maximumDistance) {
    this.phaseCode = phaseCode;
    this.minimumDistance = minimumDistance;
    this.maximumDistance = maximumDistance;

    // set up storage for the linear fits.
    residualBias = new ArrayList<TravelTimeStatisticsSegment>();
    spread = new ArrayList<TravelTimeStatisticsSegment>();
    observability = new ArrayList<TravelTimeStatisticsSegment>();
  }

  /**
   * Function to compute the phase bias.
   *
   * @param distance A double containing the distance in degrees
   * @return A double containing the phase bias in seconds at given distance
   */
  public double calcPhaseBias(double distance) {
    for (int k = 0; k < residualBias.size(); k++) {
      TravelTimeStatisticsSegment segment = residualBias.get(k);

      if (distance >= segment.getMinimumDistance() && distance <= segment.getMaximumDistance()) {
        return segment.interpolate(distance);
      }
    }

    return TauUtilities.DEFAULTTTBIAS;
  }

  /**
   * Function to calculate the phase spread.
   *
   * @param distance A double containing the distance in degrees
   * @param isUpGoing A boolean flag, true if the phase is an up-going P or S
   * @return A double containing the spread in seconds at given distance
   */
  public double calcPhaseSpread(double distance, boolean isUpGoing) {
    TravelTimeStatisticsSegment segment;

    for (int k = 0; k < spread.size(); k++) {
      segment = spread.get(k);

      if (distance >= segment.getMinimumDistance() && distance <= segment.getMaximumDistance()) {
        return Math.min(segment.interpolate(distance), TauUtilities.DEFAULTTTSPREAD);
      }
    }

    if (isUpGoing) {
      return Math.min(spread.get(0).interpolate(distance), TauUtilities.DEFAULTTTSPREAD);
    }

    return TauUtilities.DEFAULTTTSPREAD;
  }

  /**
   * Function to calculate the phase observability.
   *
   * @param distance A double containing the distance in degrees
   * @param isUpGoing A boolean flag, true if the phase is an up-going P or S
   * @return A double containing the telative observability at given distance
   */
  public double calcPhaseObservability(double distance, boolean isUpGoing) {
    for (int k = 0; k < observability.size(); k++) {
      TravelTimeStatisticsSegment segment = observability.get(k);

      if (distance >= segment.getMinimumDistance() && distance <= segment.getMaximumDistance()) {
        return Math.max(segment.interpolate(distance), TauUtilities.DEFAULTTTOBSERVABILITY);
      }
    }

    if (isUpGoing) {
      return Math.max(
          observability.get(0).interpolate(distance), TauUtilities.DEFAULTTTOBSERVABILITY);
    }

    return TauUtilities.DEFAULTTTOBSERVABILITY;
  }

  /**
   * Function to calculate the derivative of the phase spread with respect to distance.
   *
   * @param distance A double containing the distance in degrees
   * @param isUpGoing A boolean flag, true if the phase is an up-going P or S
   * @return A double holding the spread in seconds at given distance
   */
  public double calcSpreadDerivative(double distance, boolean isUpGoing) {
    for (int k = 0; k < spread.size(); k++) {
      TravelTimeStatisticsSegment segment = spread.get(k);

      if (distance >= segment.getMinimumDistance() && distance <= segment.getMaximumDistance()) {
        double deriv = segment.getDerivative(distance);

        if (distance == segment.getMaximumDistance()) {
          if (++k < spread.size()) {
            segment = spread.get(k);

            if (distance == segment.getMinimumDistance()) {
              deriv = (deriv + segment.getDerivative(distance)) / 2d;
            }
          }
        }
        return deriv;
      }
    }

    if (isUpGoing) {
      return spread.get(0).getDerivative(distance);
    }

    return 0d;
  }

  /** Function to print the travel-time statistics. */
  protected void dumpStats() {
    // Print the header.
    System.out.println("\n" + phaseCode + "     " + minimumDistance + "     " + maximumDistance);

    // Print the data.
    System.out.println("Bias:");
    for (int j = 0; j < residualBias.size(); j++) {
      System.out.format(
          "  %3d  range = %6.2f, %6.2f  fit = %11.4e, " + "%11.4e\n",
          j,
          residualBias.get(j).getMinimumDistance(),
          residualBias.get(j).getMaximumDistance(),
          residualBias.get(j).getLinearFitSlope(),
          residualBias.get(j).getLinearFitOffset());
    }

    System.out.println("Spread:");
    for (int j = 0; j < spread.size(); j++) {
      System.out.format(
          "  %3d  range = %6.2f, %6.2f  fit = %11.4e, " + "%11.4e\n",
          j,
          spread.get(j).getMinimumDistance(),
          spread.get(j).getMaximumDistance(),
          spread.get(j).getLinearFitSlope(),
          spread.get(j).getLinearFitOffset());
    }

    System.out.println("Observability:");
    for (int j = 0; j < observability.size(); j++) {
      System.out.format(
          "  %3d  range = %6.2f, %6.2f  fit = %11.4e, " + "%11.4e\n",
          j,
          observability.get(j).getMinimumDistance(),
          observability.get(j).getMaximumDistance(),
          observability.get(j).getLinearFitSlope(),
          observability.get(j).getLinearFitOffset());
    }
  }
}
