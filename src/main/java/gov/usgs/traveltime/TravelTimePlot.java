package gov.usgs.traveltime;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * The TravelTimePlot class stores plot data for all travel-time branches. Note that the travel-time
 * branches are stored by unique code rather than phase code. The phase code is stored with the
 * branch, but may not be unique. This actually makes sense because while branches with duplicate
 * phase codes are the same phase for a different ray parameter range, they may overlap in distance
 * (i.e., there was a triplication). This wouldn't be a problem except that the back branch
 * connecting the pieces hasn't been computed.
 *
 * @author Ray Buland
 */
public class TravelTimePlot {
  /** A double containing the maximum travel time for all branches */
  private double maxiumumTravelTime;

  /** A double containing the maximum travel time spread for all branches */
  private double maxiumumSpread;

  /** A double containing the maximum travel time observability for all branches */
  private double maxObservability;

  /** A TreeMap of Strings and TravelTimePlotBranch objects containing the branches */
  TreeMap<String, TravelTimePlotBranch> branches;

  /**
   * Get the maximum travel time for all branches
   *
   * @return A double containing the maximum travel time for all branches
   */
  public double getMaxiumumTravelTime() {
    return maxiumumTravelTime;
  }

  /**
   * Get the maximum travel time spread for all branches
   *
   * @return A double containing the maximum travel time spread for all branches
   */
  public double getMaxiumumSpread() {
    return maxiumumSpread;
  }

  /**
   * Get the maximum travel time observability for all branches
   *
   * @return A double containing the maximum travel time observability for all branches
   */
  public double getMaxObservability() {
    return maxObservability;
  }

  /**
   * Get the branches
   *
   * @return A TreeMap of Strings and TravelTimePlotBranch objects containing the branches
   */
  public TreeMap<String, TravelTimePlotBranch> getBranches() {
    return branches;
  }

  /** TravelTimePlot constructor, initializes the travel-time plotting information. */
  public TravelTimePlot() {
    maxiumumTravelTime = 0d;
    maxiumumSpread = 0d;
    maxObservability = 0d;
    branches = new TreeMap<String, TravelTimePlotBranch>();
  }

  /**
   * Function to add one plot point for one travel-time branch.
   *
   * @param phaseCode A String containing the phase code
   * @param uniqueCode An array of Strings containing the unique phase code list
   * @param distance A double containing the distance in degrees
   * @param travelTime A double holding the travel time in seconds
   * @param spread A double holding the statistical spread in seconds
   * @param observability A double holding the relative observability
   * @param rayParameter A double holding the ray parameter in seconds/degree
   */
  public void addPoint(
      String phaseCode,
      String[] uniqueCode,
      double distance,
      double travelTime,
      double spread,
      double observability,
      double rayParameter) {

    String key;
    if (!phaseCode.contains("bc")) {
      key = uniqueCode[0];
    } else {
      key = uniqueCode[1];
    }

    TravelTimePlotBranch branch = branches.get(key);
    if (branch == null) {
      branch = new TravelTimePlotBranch(phaseCode);
      branches.put(key, branch);
    }
    branch.addPoint(
        new TravelTimePlotPoint(distance, travelTime, spread, observability, rayParameter));

    // Keep track of maximums for plot scaling purposes.
    maxiumumTravelTime = Math.max(maxiumumTravelTime, travelTime);

    if (!Double.isNaN(spread)) {
      maxiumumSpread = Math.max(maxiumumSpread, spread);
      maxObservability = Math.max(maxObservability, observability);
    }
  }

  /** Function to sort the travel time points in all branches by ray parameter. */
  public void sortBranches() {
    NavigableMap<String, TravelTimePlotBranch> map = branches.headMap("~", true);

    for (@SuppressWarnings("rawtypes") Map.Entry entry : map.entrySet()) {
      TravelTimePlotBranch branch = (TravelTimePlotBranch) entry.getValue();
      branch.sortPoints();
    }
  }

  /** Function to print the plot data for all branches. */
  public void printBranches() {
    if (branches.size() > 0) {
      System.out.format(
          "\n\t\tPlot Data (maximums: travelTime = %7.2f spread = %5.2f "
              + "observability = %7.1f):\n",
          maxiumumTravelTime, maxiumumSpread, maxObservability);

      NavigableMap<String, TravelTimePlotBranch> map = branches.headMap("~", true);

      for (@SuppressWarnings("rawtypes") Map.Entry entry : map.entrySet()) {
        //		System.out.println((String)entry.getKey());
        TravelTimePlotBranch branch = (TravelTimePlotBranch) entry.getValue();
        branch.printBranch();
      }
    } else {
      System.out.print("No plot data found.");
    }
  }
}
