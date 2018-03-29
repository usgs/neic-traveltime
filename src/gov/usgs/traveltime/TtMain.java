package gov.usgs.traveltime;

import java.io.IOException;
import java.util.TreeMap;

/**
 * Sample driver for the travel-time package.
 * 
 * @author Ray Buland
 *
 */
public class TtMain {

	public static void main(String[] args) throws Exception {
		// Directory of known models.
		TreeMap<String,AllBrnRef> modelData = null;
		// Simulate a simple session request.
		String earthModel = "ak135";
		double sourceDepth = 0.0d;
		String[] phList = null;
		boolean useful = true;
		boolean noBackBrn = true;
		boolean rstt = false;
		// Simulate a simple travel time request.
		double delta = 79.8967d;
		double elev = 0.0d;
		boolean tectonic = true;
		// Simulate a complex travel time request.
		double sourceLat = 50.2075d;
		double sourceLon = -114.8603d;
		double staLat = 49.0586d;
		double staLon = -113.9115d;
		double azimuth = 151.4299d;
		// Classes we will need.
		ReadTau readTau = null;
		AllBrnRef allRef = null;
		AllBrnVol allBrn;
		AuxTtRef auxtt;
		TTime ttList;
		
		// Read in data common to all models.
		auxtt = new AuxTtRef(false, false, false, false);

		// Initialize model storage if necessary.
		if(modelData == null) {
			modelData = new TreeMap<String,AllBrnRef>();
		}
		
		// See if we know this model.
		allRef = modelData.get(earthModel);
		
		// If not, set it up.
		if(allRef == null) {
			try {
				readTau = new ReadTau(earthModel);
				readTau.readHeader();
		//	readTau.dumpSegments();
		//	readTau.dumpBranches();
				readTau.readTable();
		//	readTau.dumpUp(15);
			} catch(IOException e) {
				System.out.println("Unable to read Earth model "+earthModel);
				System.exit(1);
			}
			allRef = new AllBrnRef(readTau, auxtt);
			modelData.put(earthModel, allRef);
//		allRef.dumpBrn(false);
		}
		
		// At this point, we've either found the reference part of the model 
		// or read it in.  Now Set up the (depth dependent) volatile part.
		allBrn = new AllBrnVol(allRef);
//	allBrn.dumpHead();
		allBrn.dumpMod('P', false);
//	allBrn.dumpMod('S', true);
		// Set up a new session.
		try {
//		allBrn.newSession(sourceDepth, phList);
			allBrn.newSession(sourceLat, sourceLon, sourceDepth, phList, useful, 
					noBackBrn, tectonic, rstt, false);
			
			// A more comprehensive branch listing.
//		allBrn.dumpBrn(false, false, true, true);
			
			// This provides a summary table of all phases--VERY USEFUL!
			allBrn.dumpTable(true);
			
			// Get the travel times.
//		ttList = allBrn.getTT(elev, delta, useful, tectonic, noBackBrn, rstt);
			ttList = allBrn.getTT(staLat, staLon, elev, delta, azimuth);
			// Print them.
			ttList.print();
		} catch(IOException e) {
			System.out.println("Source depth out of range");
		}
	}
}
