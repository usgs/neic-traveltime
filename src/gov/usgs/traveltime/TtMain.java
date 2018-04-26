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

	/**
	 * Test main program for the travel-time package.
	 * 
	 * @param args Command line arguments (not used)
	 * @throws Exception If the travel-time setup fails
	 */
	public static void main(String[] args) throws Exception {
		// Simulate a session request.
		String earthModel = "ak135";
		double sourceDepth = 10d;
		String[] phList = null;
//	String[] phList = {"PKP", "SKP"};
		boolean useful = true;
		boolean noBackBrn = true;
		boolean rstt = false;
		// Simulate a simple travel time request.
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
		TTSessionLocal ttLocal;
		TTime ttList;
//	TtPlot ttPlot;
		
		// Initialize the local travel-time manager.
		ttLocal = new TTSessionLocal(true, true, true);
		
//	TauUtil.noCorr = true;
		try {
			// Set up a session.
			ttLocal.newSession(earthModel, sourceDepth, phList, sourceLat, 
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
