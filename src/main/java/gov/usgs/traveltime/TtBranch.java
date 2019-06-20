package gov.usgs.traveltime;

import java.util.ArrayList;

/**
 * Store plot data for one travel-time branch.
 *
 * @author Ray Buland
 */
public class TtBranch {
  String phCode; // Phase code
  ArrayList<TtPlotPoint> branch;

  /**
   * Initialize the branch.
   *
   * @param phCode Phase code
   */
  public TtBranch(String phCode) {
    this.phCode = phCode;
    branch = new ArrayList<TtPlotPoint>();
  }

  /**
   * Add a plot point to the branch.
   *
   * @param point Plot data
   */
  public void addPoint(TtPlotPoint point) {
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
