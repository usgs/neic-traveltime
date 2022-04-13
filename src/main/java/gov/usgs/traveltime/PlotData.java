package gov.usgs.traveltime;

import gov.usgs.traveltime.tables.TauIntegralException;

/**
 * Create data for travel-time plots
 *
 * @author Ray Buland
 */
public class PlotData {
  AllBranchVolume allBrn;
  TravelTime ttList;
  TravelTimeData TravelTime;
  TravelTimePlot TravelTimePlot;

  /**
   * Remember the travel-time driver.
   *
   * @param allBrn All branches travel-time object
   */
  public PlotData(AllBranchVolume allBrn) {
    this.allBrn = allBrn;
  }

  /**
   * Create the data for travel-time plots.
   *
   * @param depth Source depth in kilometers
   * @param phList Array of phase use commands
   * @param returnAllPhases If false, only provide "useful" crustal phases
   * @param returnBackBranches If false, suppress back branches
   * @param tectonic If true, map Pb and Sb onto Pg and Sg
   * @param maxDelta Maximum distance in degrees to generate
   * @param maxTime Maximum travel time in seconds to allow
   * @param deltaStep Distance increment in degrees for travel-time plots
   * @throws BadDepthException If the depth is out of range
   * @throws TauIntegralException If the tau integrals fail
   */
  public void makePlot(
      double depth,
      String[] phList,
      boolean returnAllPhases,
      boolean returnBackBranches,
      boolean tectonic,
      double maxDelta,
      double maxTime,
      double deltaStep)
      throws BadDepthException, TauIntegralException {

    double deltaIncrement = TauUtilities.DDELPLOT;
    if (deltaStep > 0) {
      deltaIncrement = deltaStep;
    }

    // Make sure the depth is in range.
    if (!Double.isNaN(depth) && depth >= 0d && depth <= TauUtilities.MAXDEPTH) {
      TravelTimePlot = new TravelTimePlot();
      // A simple request is all we can do.
      allBrn.newSession(depth, phList, returnAllPhases, returnBackBranches, tectonic);
      // Loop over distances.
      for (double delta = 0d; delta <= maxDelta; delta += deltaIncrement) {
        ttList = allBrn.getTT(0d, delta);
        // Loop over phases sorting them into branches.
        for (int j = 0; j < ttList.getNumPhases(); j++) {
          TravelTime = ttList.getPhase(j);
          if (TravelTime.tt <= maxTime) {
            TravelTimePlot.addPoint(
                TravelTime.phaseCode,
                TravelTime.uniqueCode,
                delta,
                TravelTime.tt,
                TravelTime.spread,
                TravelTime.observ,
                TravelTime.dTdD);
          }
        }
      }
      TravelTimePlot.sortBranches();
      // If the depth is bad, we can't do anything.
    } else {
      TravelTimePlot = null;
    }
  }

  /**
   * Get the travel-time plot data.
   *
   * @return Travel-time plot data
   */
  public TravelTimePlot getPlot() {
    return TravelTimePlot;
  }

  /** Print plot data for all travel-time branches. */
  public void print() {
    TravelTimePlot.printBranches();
  }
}
