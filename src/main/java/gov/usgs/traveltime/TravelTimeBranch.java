package gov.usgs.traveltime;

import java.util.ArrayList;

/**
 * Store plot data for one travel-time branch.
 *
 * @author Ray Buland
 */
public class TravelTimeBranch {
  String phaseCode; // Phase code
  ArrayList<TravelTimePlotPoint> branch;

  /**
   * Initialize the branch.
   *
   * @param phaseCode Phase code
   */
  public TravelTimeBranch(String phaseCode) {
    this.phaseCode = phaseCode;
    branch = new ArrayList<TravelTimePlotPoint>();
  }

  /**
   * Add a plot point to the branch.
   *
   * @param point Plot data
   */
  public void addPoint(TravelTimePlotPoint point) {
    branch.add(point);
  }

  /** Sort the points in the branch by ray parameter. */
  public void sortPoints() {
    branch.sort(null);
  }

  /** Print out the branch data */
  public void printBranch() {
    System.out.println("\n" + phaseCode + ":");
    for (int j = 0; j < branch.size(); j++) {
      System.out.println("\t" + branch.get(j));
    }
  }
}
