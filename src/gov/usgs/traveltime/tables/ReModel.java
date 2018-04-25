package gov.usgs.traveltime.tables;

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
	 */
	public static void main(String[] args) {
		String earthModel = "ak135";
		EarthModel reModel;
		TtStatus status;
		
		reModel = new EarthModel(earthModel, true);
		// Read the model.
		status = reModel.readModel();
		System.out.println("Read status = "+status);
		// Print it out.
		if(status == TtStatus.SUCCESS) {
			// Print out the radial version.
			reModel.printModel(false, false);
			// Print out the Earth flattened version.
			reModel.printModel(true, true);
			reModel.printCritical();
		}
	}
}
