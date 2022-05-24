package gov.usgs.traveltime;

import java.util.ArrayList;

/**
 * The TravelTimePlotBranch classes stores plot data for one travel-time branch.
 *
 * @author Ray Buland
 */
public class TravelTimePlotBranch {
  /** A String containing the phase code for this branch */
  private String phaseCode;

  /** An ArrayList of TravelTimePlotPoint objects representing the branch points */
  private ArrayList<TravelTimePlotPoint> branchPoints;

  /**
   * Get the phase code for this branch
   *
   * @return A String object containing the phase code for this branch
   */
  public String getPhaseCode() {
    return phaseCode;
  }

  /**
   * Get the branch points
   *
   * @return An ArrayList of TravelTimePlotPoint objects representing the branch points
   */
  public ArrayList<TravelTimePlotPoint> getBranchPoints() {
    return branchPoints;
  }

  /**
   * TravelTimePlotBranch Constructor, initializes the branch.
   *
   * @param phaseCode A String containing the phase code
   */
  public TravelTimePlotBranch(String phaseCode) {
    this.phaseCode = phaseCode;
    branchPoints = new ArrayList<TravelTimePlotPoint>();
  }

  /**
   * Function to add a plot point to the branch.
   *
   * @param point A TravelTimePlotPoint object containing the plot point
   */
  public void addPoint(TravelTimePlotPoint point) {
    branchPoints.add(point);
  }

  /** Function to Sort the points in the branch by ray parameter. */
  public void sortPoints() {
    branchPoints.sort(null);
  }

  /** Function to print out the branch data */
  public void printBranch() {
    System.out.println("\n" + phaseCode + ":");

    for (int j = 0; j < branchPoints.size(); j++) {
      System.out.println("\t" + branchPoints.get(j));
    }
  }
}
