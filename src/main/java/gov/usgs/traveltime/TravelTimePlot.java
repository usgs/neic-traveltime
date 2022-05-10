package gov.usgs.traveltime;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Store plot data for all travel-time branches. Note that the travel-time branches are stored by
 * unique code rather than phase code. The phase code is stored with the branch, but may not be
 * unique. This actually makes sense because while branches with duplicate phase codes are the same
 * phase for a different ray parameter range, they may overlap in distance (i.e., there was a
 * triplication). This wouldn't be a problem except that the back branch connecting the pieces
 * hasn't been computed.
 *
 * @author Ray Buland
 */
public class TravelTimePlot {
  double ttMax; // Maximum travel time for all branches
  double spreadMax; // Maximum spread for all branches
  double observMax; // Maximum observability for all branches
  TreeMap<String, TravelTimePlotBranch> branches;

  /** Initialize the travel-time plotting information. */
  public TravelTimePlot() {
    ttMax = 0d;
    spreadMax = 0d;
    observMax = 0d;
    branches = new TreeMap<String, TravelTimePlotBranch>();
  }

  /**
   * Add one plot point for one travel-time branch.
   *
   * @param phaseCode Phase code
   * @param uniqueCode A unique phase code for branches with duplicate names (two versions for
   *     PKPab/PKPbc)
   * @param delta Distance in degrees
   * @param tt Travel time in seconds
   * @param spread Statistical spread in seconds
   * @param observ Relative observability
   * @param dTdD Ray parameter in seconds/degree
   */
  public void addPoint(
      String phaseCode,
      String[] uniqueCode,
      double delta,
      double tt,
      double spread,
      double observ,
      double dTdD) {
    String key;
    TravelTimePlotBranch branch;

    if (!phaseCode.contains("bc")) {
      key = uniqueCode[0];
    } else {
      key = uniqueCode[1];
    }
    branch = branches.get(key);
    if (branch == null) {
      branch = new TravelTimePlotBranch(phaseCode);
      branches.put(key, branch);
    }
    branch.addPoint(new TravelTimePlotPoint(delta, tt, spread, observ, dTdD));
    // Keep track of maximums for plot scaling purposes.
    ttMax = Math.max(ttMax, tt);
    if (!Double.isNaN(spread)) {
      spreadMax = Math.max(spreadMax, spread);
      observMax = Math.max(observMax, observ);
    }
  }

  /** Sort the points in all branches by ray parameter. */
  public void sortBranches() {
    TravelTimePlotBranch branch;

    NavigableMap<String, TravelTimePlotBranch> map = branches.headMap("~", true);
    for (@SuppressWarnings("rawtypes") Map.Entry entry : map.entrySet()) {
      branch = (TravelTimePlotBranch) entry.getValue();
      branch.sortPoints();
    }
  }

  /** Print the plot data for all branches. */
  public void printBranches() {
    TravelTimePlotBranch branch;

    if (branches.size() > 0) {
      System.out.format(
          "\n\t\tPlot Data (maximums: tt = %7.2f spread = %5.2f " + "observ = %7.1f):\n",
          ttMax, spreadMax, observMax);
      NavigableMap<String, TravelTimePlotBranch> map = branches.headMap("~", true);
      for (@SuppressWarnings("rawtypes") Map.Entry entry : map.entrySet()) {
        //		System.out.println((String)entry.getKey());
        branch = (TravelTimePlotBranch) entry.getValue();
        branch.printBranch();
      }
    } else {
      System.out.print("No plot data found.");
    }
  }
}
