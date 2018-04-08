package gov.usgs.traveltime;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Store plot data for all travel-time branches.  Note that the travel-time 
 * branches are stored by unique code rather than phase code.  The phase 
 * code is stored with the branch, but may not be unique.  This actually makes 
 * sense because while branches with duplicate phase codes are the same phase 
 * for a different ray parameter range, they may overlap in distance (i.e., 
 * there was a triplication).  This wouldn't be a problem except that the back 
 * branch connecting the pieces hasn't been computed.
 * 
 * @author Ray Buland
 *
 */
public class TtPlot {
	double ttMax;					// Maximum travel time for all branches
	double spreadMax;			// Maximum spread for all branches
	double observMax;			// Maximum observability for all branches
	TreeMap<String, TtBranch> branches;
	
	public TtPlot() {
		ttMax = 0d;
		spreadMax = 0d;
		observMax = 0d;
		branches = new TreeMap<String, TtBranch>();
	}
	
	/**
	 * Add one plot point for one travel-time branch.
	 * 
	 * @param phCode Phase code
	 * @param delta Distance in degrees
	 * @param tt Travel time in seconds
	 * @param spread Statistical spread in seconds
	 * @param observ Relative observability
	 * @param dTdD Ray parameter in seconds/degree
	 */
	public void addPoint(String phCode, String uniqueCode, double delta, double tt, 
			double spread, double observ, double dTdD) {
		TtBranch branch;
		
		branch = branches.get(uniqueCode);
		if(branch == null) {
			branch = new TtBranch(phCode);
			branches.put(uniqueCode, branch);
		}
		branch.addPoint(new TtPlotPoint(delta, tt, spread, observ, dTdD));
		// Keep track of maximums for plot scaling purposes.
		ttMax = Math.max(ttMax, tt);
		spreadMax = Math.max(spreadMax, spread);
		observMax = Math.max(observMax, observ);
	}
	
	/**
	 * Sort the points in all branches by ray parameter.
	 */
	public void sortBranches() {
		TtBranch branch;
		
		NavigableMap<String, TtBranch> map = branches.headMap("~", true);
		for(@SuppressWarnings("rawtypes") Map.Entry entry : map.entrySet()) {
			branch = (TtBranch)entry.getValue();
			branch.sortPoints();
		}
	}
	
	/**
	 * Print the plot data for all branches.
	 */
	public void printBranches() {
		TtBranch branch;
		
		if(branches.size() > 0) {
			System.out.format("\n\t\tPlot Data (maximums: tt = %7.2f spread = %5.2f "+
					"observ = %7.1):\n", ttMax, spreadMax, observMax);
			NavigableMap<String, TtBranch> map = branches.headMap("~", true);
			for(@SuppressWarnings("rawtypes") Map.Entry entry : map.entrySet()) {
				branch = (TtBranch)entry.getValue();
				branch.printBranch();
			}
		} else {
			System.out.print("No plot data found.");
		}
	}
}
