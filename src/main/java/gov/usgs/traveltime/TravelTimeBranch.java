package gov.usgs.traveltime;

import java.util.ArrayList;

/**
 * Store plot data for one travel-time branch.
 *
 * @author Ray Buland
 */
public class TravelTimeBranch {
  String phCode; // Phase code
  ArrayList<TravelTimePlotPoint> branch;

  /**
   * Initialize the branch.
   *
   * @param phCode Phase code
   */
  public TravelTimeBranch(String phCode) {
    this.phCode = phCode;
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
    System.out.println("\n" + phCode + ":");
    for (int j = 0; j < branch.size(); j++) {
      System.out.println("\t" + branch.get(j));
    }
  }
}
