package gov.usgs.traveltime;

import java.util.ArrayList;

/**
 * The TravelTimeStatisticsLinearFit class stores the travel-time statistics data in one degree
 * bins, do the linear fits between the break flags, and release the raw statistics data.
 *
 * @author Ray Buland
 */
public class TravelTimeStatisticsLinearFit {
  /** An array of double values containing the travel-time residual bias in seconds */
  private double[] residualBias;

  /**
   * An array of boolean values indicating when to break the interpolation at the corresponding
   * residualBias if true
   */
  private boolean[] residualBiasBreakFlags;

  /**
   * An array of double values containing a robust estimate of the spread (scatter) of travel-time
   * residuals in seconds
   */
  private double[] spread;

  /**
   * An array of boolean values indicating when to break the interpolation at the corresponding
   * spread if true
   */
  private boolean[] spreadBreakFlags;

  /**
   * An array of double values containing the number of times this phase was observed in the
   * defining study
   */
  private double[] observability;

  /**
   * An array of boolean values indicating when to break the interpolation at the corresponding
   * observability if true
   */
  private boolean[] observabilityBreakFlags;

  /** A TravelTimeStatistics object containing the phase statistics */
  private TravelTimeStatistics phaseStatistics;

  /** An integer containing the minimum distance in degrees */
  private int minimumDistance;

  /** An integer containing the maxiumum distance in degrees */
  private int maximumDistance;

  /**
   * The TravelTimeStatisticsLinearFit constructor, sets up the 1 degree arrays.
   *
   * @param phaseStatistics A TravelTimeStatistics object containing the phase statistics
   */
  protected TravelTimeStatisticsLinearFit(TravelTimeStatistics phaseStatistics) {
    this.phaseStatistics = phaseStatistics;
    minimumDistance = phaseStatistics.getMinimumDistance();
    maximumDistance = phaseStatistics.getMaxiumumDistance();

    // set up the 1 degree statistics arrays.
    residualBias = new double[maximumDistance - minimumDistance + 1];
    residualBiasBreakFlags = new boolean[maximumDistance - minimumDistance + 1];
    spread = new double[maximumDistance - minimumDistance + 1];
    spreadBreakFlags = new boolean[maximumDistance - minimumDistance + 1];
    observability = new double[maximumDistance - minimumDistance + 1];
    observabilityBreakFlags = new boolean[maximumDistance - minimumDistance + 1];

    // Initialize the arrays.
    for (int j = 0; j < residualBias.length; j++) {
      residualBias[j] = Double.NaN;
      residualBiasBreakFlags[j] = false;
      spread[j] = Double.NaN;
      spreadBreakFlags[j] = false;
      observability[j] = Double.NaN;
      observabilityBreakFlags[j] = false;
    }
  }

  /**
   * Function to add statistics for one 1 degree distance bin.
   *
   * @param distance An integer containing the distance in degrees at the bin center
   * @param residualBias A double containing the travel-time residual bias in seconds
   * @param residualBiasBreakFlags A boolean flag, if true, break the interpolation at this distance
   *     bin
   * @param spread A double containing the robust estimate of the scatter of travel-time residuals
   * @param spreadBreakFlags A boolean flag, if true, break the interpolation at this distance bin
   * @param observability A double containing the number of times this phase was observed in the
   *     defining study
   * @param observabilityBreakFlags A boolean flag, if true, break the interpolation at this
   *     distance bin
   */
  protected void add(
      int distance,
      double residualBias,
      boolean residualBiasBreakFlags,
      double spread,
      boolean spreadBreakFlags,
      double observability,
      boolean observabilityBreakFlags) {
    this.residualBias[distance - minimumDistance] = residualBias;
    this.residualBiasBreakFlags[distance - minimumDistance] = residualBiasBreakFlags;
    this.spread[distance - minimumDistance] = spread;
    this.spreadBreakFlags[distance - minimumDistance] = spreadBreakFlags;
    this.observability[distance - minimumDistance] = observability;
    this.observabilityBreakFlags[distance - minimumDistance] = observabilityBreakFlags;
  }

  /** Function to perform the linear fits for all statistics variables. */
  protected void doAllStatisticsFits() {
    doFits(phaseStatistics.getResidualBias(), residualBias, residualBiasBreakFlags);
    doFits(phaseStatistics.getSpread(), spread, spreadBreakFlags);
    doFits(phaseStatistics.getObservability(), observability, observabilityBreakFlags);
  }

  /**
   * Function to perform the linear fits for all segments of one statistics variable.
   *
   * @param interpolation An ArrayList of TravelTimeStatisticsSegment objects where where the fits
   *     will be stored
   * @param value An Array doubles containing the statistics values to fit
   * @param breakFlags Array of booleans holding the break point flags for this statistics variable.
   */
  protected void doFits(
      ArrayList<TravelTimeStatisticsSegment> interpolation, double[] value, boolean[] breakFlags) {
    int start, end = 0;
    double[] fitSlopeOffset;

    // Find the break points and invoke the fitter for each segment.
    double endDistance = minimumDistance;
    for (int j = 0; j < value.length; j++) {
      if (breakFlags[j]) {
        start = end;
        end = j;
        double startDistance = endDistance;
        endDistance = minimumDistance + (double) j;
        fitSlopeOffset = do1Fit(start, end, minimumDistance, value);

        interpolation.add(
            new TravelTimeStatisticsSegment(
                startDistance, endDistance, fitSlopeOffset[0], fitSlopeOffset[1]));
      }
    }

    // Fit the last segment.
    start = end;
    end = value.length - 1;
    double startDistance = endDistance;
    endDistance = minimumDistance + (double) end;
    fitSlopeOffset = do1Fit(start, end, minimumDistance, value);

    interpolation.add(
        new TravelTimeStatisticsSegment(
            startDistance, endDistance, fitSlopeOffset[0], fitSlopeOffset[1]));
    fixEnds(interpolation);
  }

  /**
   * Function to perform the linear fit for one segment of one statistics variable.
   *
   * @param start An integer contaiing the array index of the start of the segment
   * @param end An integer contaiing the array index of the end of the segment
   * @param minimumObservedDistance A double containing the minimum distance where the phase is
   *     observed in degrees
   * @param value An array of doubles containing the raw statistics data to be fit
   * @return An array of doubles containing the fit slope and offset
   */
  protected double[] do1Fit(int start, int end, double minimumObservedDistance, double[] value) {
    double[][] a = new double[2][2];
    double[] y = new double[2];

    // Initialize temporary storage.
    for (int i = 0; i < 2; i++) {
      y[i] = 0d;
      for (int j = 0; j < 2; j++) {
        a[i][j] = 0d;
      }
    }

    // Skip null bins and collect the data available.
    for (int j = start; j <= end; j++) {
      if (!Double.isNaN(value[j])) {
        double distance = minimumObservedDistance + j;
        y[0] += value[j] * distance;
        y[1] += value[j];
        a[0][0] += 1d;
        a[0][1] -= distance;
        a[1][1] += Math.pow(distance, 2);
      }
    }
    a[1][0] = a[0][1];

    // Do the fit.
    double det = a[0][0] * a[1][1] - a[0][1] * a[1][0];
    double[] fitSlopeOffset = new double[2];
    fitSlopeOffset[0] = (a[0][0] * y[0] + a[0][1] * y[1]) / det;
    fitSlopeOffset[1] = (a[1][0] * y[0] + a[1][1] * y[1]) / det;
    return fitSlopeOffset;
  }

  /**
   * Function to fix issues at the break points. Successive linear fits don't quite connect with
   * each other at exactly the break point distances. Compute the actual cross- over distances and
   * apply them to the end of one segment and the start of the next.
   *
   * @param interpolation An ArrayList of TravelTimeStatisticsSegment objects holding the linear fit
   *     segments for one parameter
   */
  protected void fixEnds(ArrayList<TravelTimeStatisticsSegment> interpolation) {
    TravelTimeStatisticsSegment current = interpolation.get(0);

    for (int j = 1; j < interpolation.size(); j++) {
      TravelTimeStatisticsSegment last = current;
      current = interpolation.get(j);
      last.setMaximumDistance(
          -(last.getLinearFitOffset() - current.getLinearFitOffset())
              / (last.getLinearFitSlope() - current.getLinearFitSlope()));
      current.setMinimumDistance(last.getMaximumDistance());
    }
  }

  /** Function to print the travel-time statistics. */
  protected void dumpStats() {
    // Print the header.
    System.out.println(
        "\n"
            + phaseStatistics.getPhaseCode()
            + "     "
            + minimumDistance
            + "     "
            + maximumDistance);

    // If the arrays still exist, dump the raw statistics.
    char[] flag = new char[3];
    for (int j = 0; j < residualBias.length; j++) {
      if (residualBiasBreakFlags[j]) {
        flag[0] = '*';
      } else {
        flag[0] = ' ';
      }

      if (spreadBreakFlags[j]) {
        flag[1] = '*';
      } else {
        flag[1] = ' ';
      }

      if (observabilityBreakFlags[j]) {
        flag[2] = '*';
      } else {
        flag[2] = ' ';
      }

      System.out.format(
          "  %3d  %7.2f%c  %7.2f%c  %8.1f%c\n",
          j + minimumDistance,
          residualBias[j],
          flag[0],
          spread[j],
          flag[1],
          observability[j],
          flag[2]);
    }
  }
}
