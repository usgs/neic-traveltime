package gov.usgs.traveltime.tables;

import gov.usgs.traveltime.AllBrnRef;
import gov.usgs.traveltime.AuxTtRef;
import gov.usgs.traveltime.TauUtil;
import gov.usgs.traveltime.TtStatus;

/**
 * Test main program for travel-time table generation.
 * 
 * @author Ray Buland
 *
 */
public class ReModel {

	/**
	 * Test driver for the model generation (replacing Fortran programs 
	 * Remodl and Setbrn generating *.hed and *.tlb files).  Note that 
	 * in the long run, the Java version will directly populate the 
	 * travel-time reference classes so that the model tables, etc. can 
	 * be generated on the fly.
	 * 
	 * @param args Command line arguments
	 * @throws Exception On an illegal integration interval
	 */
	public static void main(String[] args) throws Exception {
		double sysTime;
		String earthModel = "wus";
		MakeTables make;
		AuxTtRef auxTT = null;
		AllBrnRef allRef;
		TtStatus status;
		
		TablesUtil.deBugLevel = 1;
		sysTime = System.currentTimeMillis();
		make = new MakeTables(earthModel);
		status = make.buildModel(TauUtil.model("m"+earthModel+".mod"), 
				TauUtil.model("phases.txt"));
		if(status == TtStatus.SUCCESS) {
			// Build the branch reference classes.
			auxTT = new AuxTtRef(true, false, false);
			allRef = make.fillAllBrnRef(null, auxTT);
			System.out.println("Table generation time: "+
					0.001*(System.currentTimeMillis()-sysTime));
//		allRef.dumpHead();
//		allRef.dumpMod('P', true);
//		allRef.dumpMod('S', true);
			allRef.dumpBrn(false);
//		allRef.dumpUp('P');
//		allRef.dumpUp('S');
		} else {
			System.out.println("Read status = "+status);
		}
	}
}
