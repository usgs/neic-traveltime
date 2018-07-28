package gov.usgs.traveltime.tables;

import gov.usgs.traveltime.AllBrnRef;
import gov.usgs.traveltime.AuxTtRef;
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
		String earthModel = "ak135";
		MakeTables make;
		AuxTtRef auxTT = null;
		AllBrnRef allRef;
		TtStatus status;
		
		TablesUtil.deBugLevel = 1;
		make = new MakeTables();
		status = make.buildModel(earthModel);
		if(status == TtStatus.SUCCESS) {
			// Build the branch reference classes.
			allRef = make.fillAllBrnRef(auxTT);
		} else {
			System.out.println("Read status = "+status);
		}
	}
}
