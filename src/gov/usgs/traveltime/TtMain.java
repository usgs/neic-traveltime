package gov.usgs.traveltime;

import java.io.IOException;
// import java.util.TreeMap;

/**
 * Sample driver for the travel-time package.
 * 
 * @author Ray Buland
 *
 */
public class TtMain {

	public static void main(String[] args) throws Exception {
		// Simulate a session request.
		String earthModel = "ak135";
		double sourceDepth = 10.0d;
		String[] phList = null;
//	String[] phList = {"PKP", "SKP"};
		boolean useful = true;
		boolean noBackBrn = true;
		boolean rstt = false;
		// Simulate a simple travel time request.
//	double delta = 79.8967d;
		double delta = 12.0d;
		double elev = 0.0d;
		boolean tectonic = true;
		// Simulate a complex travel time request.
		double sourceLat = 50.2075d;
		double sourceLon = -114.8603d;
		double staLat = 49.0586d;
		double staLon = -113.9115d;
		double azimuth = 151.4299d;
		// Classes we will need.
		TravelTimeLocal ttLocal;
		TTime ttList;
//	TtPlot ttPlot;
		
		// Initialize the local travel-time manager.
		ttLocal = new TravelTimeLocal(false, false, false);
		
//	TauUtil.noCorr = true;
		try {
			// Set up a session.
			ttLocal.travelTimeSession(earthModel, sourceDepth, phList, sourceLat, 
					sourceLon, !useful, !noBackBrn, tectonic, rstt);
//		ttLocal.printRefBranches(false);
//		ttLocal.printBranches(false, false, false, useful);
			ttLocal.printTable(useful);
			// Get the travel times.
			ttList = ttLocal.getTT(staLat, staLon, elev, delta, azimuth);
			// Print them.
			ttList.print();
			
//		ttPlot = ttLocal.getPlot(earthModel, sourceDepth, phList, !useful, 
//				true, tectonic);
//		ttPlot.printBranches();
			
		} catch(IOException e) {
			System.out.println("Source depth out of range");
		}
	}
}
