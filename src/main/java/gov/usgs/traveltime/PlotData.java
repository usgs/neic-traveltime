package gov.usgs.traveltime;

import gov.usgs.traveltime.tables.TauIntegralException;

/**
 * Create data for travel-time plots
 *
 * @author Ray Buland
 */
public class PlotData {
  AllBrnVol allBrn;
  TTime ttList;
  TTimeData tTime;
  TtPlot ttPlot;

  /**
   * Remember the travel-time driver.
   *
   * @param allBrn All branches travel-time object
   */
  public PlotData(AllBrnVol allBrn) {
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
   * @throws BadDepthException If the depth is out of range
   * @throws TauIntegralException If the tau integrals fail
   */
  public void makePlot(
      double depth,
      String[] phList,
      boolean returnAllPhases,
      boolean returnBackBranches,
      boolean tectonic)
      		throws BadDepthException, TauIntegralException {
    // Make sure the depth is in range.
    if (!Double.isNaN(depth) && depth >= 0d && depth <= TauUtil.MAXDEPTH) {
      ttPlot = new TtPlot();
      // A simple request is all we can do.
      allBrn.newSession(depth, phList, returnAllPhases, returnBackBranches, tectonic, false);
      // Loop over distances.
      for (double delta = 0d; delta <= 180d; delta += TauUtil.DDELPLOT) {
        ttList = allBrn.getTT(0d, delta);
        // Loop over phases sorting them into branches.
        for (int j = 0; j < ttList.getNumPhases(); j++) {
          tTime = ttList.getPhase(j);
          ttPlot.addPoint(
              tTime.phCode,
              tTime.uniqueCode,
              delta,
              tTime.tt,
              tTime.spread,
              tTime.observ,
              tTime.dTdD);
        }
      }
      ttPlot.sortBranches();
      // If the depth is bad, we can't do anything.
    } else {
      ttPlot = null;
    }
  }

  /**
   * Get the travel-time plot data.
   *
   * @return Travel-time plot data
   */
  public TtPlot getPlot() {
    return ttPlot;
  }

  /** Print plot data for all travel-time branches. */
  public void print() {
    ttPlot.printBranches();
  }
}
