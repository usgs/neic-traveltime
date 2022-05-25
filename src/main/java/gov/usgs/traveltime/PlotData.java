package gov.usgs.traveltime;

import gov.usgs.traveltime.tables.TauIntegralException;

/**
 * The PlotData class creates data for use in travel-time plots
 *
 * @author Ray Buland
 */
public class PlotData {
  /** An AllBranchVolume object containing the the travel-time branch information */
  private AllBranchVolume ttBranchData;

  /** A TravelTimePlot object containing the plot data generated by makePlot() */
  private TravelTimePlot plotData;

  /**
   * Function to get the travel-time plot data.
   *
   * @return A TravelTimePlot object containing the travel-time plot data
   */
  public TravelTimePlot getPlotData() {
    return plotData;
  }

  /**
   * PlotData constructor, store the branch volume
   *
   * @param ttBranchData A AllBranchVolume containing the travel-time branch information
   */
  public PlotData(AllBranchVolume ttBranchData) {
    this.ttBranchData = ttBranchData;
  }

  /**
   * Function to create the data for travel-time plots.
   *
   * @param depth A double containing the source depth in kilometers
   * @param phaseList An Array strings containing the phases to plot
   * @param returnAllPhases A A boolean flag, if false, only provide "useful" crustal phases
   * @param returnBackBranches A boolean flag, if false, suppress back branches
   * @param convertTectonic A boolean flag, if true, map Pb and Sb onto Pg and Sg
   * @param maximumDistance A double containing them maximum distance in degrees to generate
   * @param maximumTime A double containing the maximum travel time in seconds to allow
   * @param distanceStep A double containing the distance increment in degrees to use
   * @throws BadDepthException If the depth is out of range
   * @throws TauIntegralException If the tau integrals fail
   */
  public void makePlot(
      double depth,
      String[] phaseList,
      boolean returnAllPhases,
      boolean returnBackBranches,
      boolean convertTectonic,
      double maximumDistance,
      double maximumTime,
      double distanceStep)
      throws BadDepthException, TauIntegralException {

    double distanceIncrement = TauUtilities.PLOTDISTANCEINCREMENT;
    if (distanceStep > 0) {
      distanceIncrement = distanceStep;
    }

    // Make sure the depth is in range.
    if (!Double.isNaN(depth) && depth >= 0d && depth <= TauUtilities.MAXIMUMDEPTH) {
      plotData = new TravelTimePlot();

      // A simple request is all we can do.
      ttBranchData.newSession(
          depth, phaseList, returnAllPhases, returnBackBranches, convertTectonic);

      // Loop over distances.
      for (double distance = 0d; distance <= maximumDistance; distance += distanceIncrement) {
        TravelTime ttList = ttBranchData.getTravelTime(0d, distance);

        // Loop over phases sorting them into branches.
        for (int j = 0; j < ttList.getNumPhases(); j++) {
          TravelTimeData travelTime = ttList.getPhase(j);

          if (travelTime.getTravelTime() <= maximumTime) {
            plotData.addPoint(
                travelTime.getPhaseCode(),
                travelTime.getUniquePhaseCodeList(),
                distance,
                travelTime.getTravelTime(),
                travelTime.getStatisticalSpread(),
                travelTime.getObservability(),
                travelTime.getDistanceDerivitive());
          }
        }
      }

      plotData.sortBranches();
    } else {
      // If the depth is bad, we can't do anything.
      plotData = null;
    }
  }

  /** Print plot data for all travel-time branches. */
  public void printBranches() {
    plotData.printBranches();
  }
}
