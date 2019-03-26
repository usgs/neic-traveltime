package gov.usgs.traveltime;

import java.io.IOException;
import gov.usgs.traveltime.session.*;

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
		// Flags for ttlist.
		boolean returnAllPhases = false;
		boolean returnBackBranches = false;
		boolean rstt = false;
		boolean tectonic = false;
		// Simulate a simple travel time request.
		double[] delta = {1d, 2d, 3d, 5d, 10d, 20d, 40d, 60d, 90d, 120d, 
				150d, 180d};
//	double[] delta = {40d};
		double elev = 0.0d;
		// Simulate a complex travel time request.
/*	double sourceLat = 50.2075d;
		double sourceLon = -114.8603d;
		double staLat = 49.0586d;
		double staLon = -113.9115d;
		double azimuth = 151.4299d; */
		// Classes we will need.
		boolean local = true;
		TTSessionLocal ttLocal = null;
		TTSessionPool ttPool = null;
		TTSession ttServer = null;
		TTime ttList;
//	TtPlot ttPlot;
		
		if(local) {
			// Initialize the local travel-time manager.
			// NOTE assumes default model path for now, need to figure out
        	// where to get this path. Cmd line arg?
			ttLocal = new TTSessionLocal(true, true, true, null);
			// Generate a list of available Earth models.
			String[] models = TauUtil.availableModels();
			if(models.length > 0) {
				System.out.println("Available Earth models:");
				for(int j=0; j<models.length; j++) {
					System.out.println("\t" + models[j]);
				}
			} else {
				System.out.println("There are no available Earth models?");
			}
		}
		
//	TauUtil.noCorr = true;
		try {
			// Set up a simple session.
//		TablesUtil.deBugLevel = 3;
			if(local) {
				ttLocal.newSession(earthModel, sourceDepth, phList, returnAllPhases, 
						returnBackBranches, tectonic, rstt);
				// Set up a complex session.
//			ttLocal.newSession(earthModel, sourceDepth, phList, sourceLat, 
//					sourceLon, returnAllPhases, returnBackBranches, tectonic, rstt);
//			ttLocal.printRefBranches(false);
//			ttLocal.printBranches(false, false, false, returnAllPhases);
//			ttLocal.printCaustics(false, false, false, returnAllPhases);
				ttLocal.printTable(returnAllPhases);
			} else {
				ttServer = TTSessionPool.getTravelTimeSession(earthModel, sourceDepth, phList, 
						returnAllPhases, returnBackBranches, tectonic, rstt, false);
			}
			
			for(int j=0; j<delta.length; j++) {
				// Get the simple travel times.
				if(local) {
					ttList = ttLocal.getTT(elev, delta[j]);
				} else {
					ttList = ttServer.getTT(delta[j], elev);
				}
				// Get the complex travel times.
//			ttList = ttLocal.getTT(staLat, staLon, elev, delta, azimuth);
				// Print them.
				ttList.print();
			}
			
//		ttPlot = ttLocal.getPlot(earthModel, sourceDepth, phList, returnAllPhases, 
//				returnBackBranches, tectonic);
//		ttPlot.printBranches();
		} catch(IOException e) {
			System.out.println("Source depth out of range");
		}
	}
}
