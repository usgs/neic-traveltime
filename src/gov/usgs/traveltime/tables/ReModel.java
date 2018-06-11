package gov.usgs.traveltime.tables;

import gov.usgs.traveltime.ModConvert;
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
		EarthModel refModel;
		ModConvert convert;
		InternalModel locModel;
		TauModel tauModel;
		SampleSlowness sample;
		Integrate integrate;
		TtStatus status;
		
		TablesUtil.deBugLevel = 1;
		TablesUtil.deBugOrder = true;
		
		refModel = new EarthModel(earthModel, true);
		// Read the model.
		status = refModel.readModel();
		// Print it out.
		if(status == TtStatus.SUCCESS) {
			// Print the shell summaries.
//		refModel.printShells();
			// Print out the radial version.
//		refModel.printModel();
			
			// Interpolate the model.
			convert = refModel.getConvert();
			locModel = new InternalModel(refModel, convert);
			locModel.interpolate();
			// Print the shell summaries.
			locModel.printShells();
			// Print out the radial version.
//		locModel.printModel(false, false);
			// Print out the Earth flattened version.
			locModel.printModel(true, true);
			locModel.printCritical();
			
			// Make the slowness sampling.
			sample = new SampleSlowness(locModel);
			sample.sample('P');
			sample.printModel('P', true);
			sample.sample('S');
			sample.printModel('S', true);
			sample.merge();
			sample.printMerge();
			sample.depthModel('P');
			sample.printModel('P', false);
			sample.depthModel('S');
			sample.printModel('S', false);
			tauModel = sample.getDepthModel();
			tauModel.printDepShells('P');
			tauModel.printDepShells('S');
			
			// Do the integrals.
			TablesUtil.deBugOrder = false;
			integrate = new Integrate(tauModel);
			integrate.doTauIntegrals('P');
			integrate.doTauIntegrals('S');
		} else {
			System.out.println("Read status = "+status);
		}
	}
}
