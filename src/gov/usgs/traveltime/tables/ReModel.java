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
		EarthModel refModel, locModel;
		ModConvert convert;
		TauModel depModel, finModel;
		SampleSlowness sample;
		Integrate integrate;
		TablePieces pieces;
		DecTTbranch decimate;
		MakeBranches layout;
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
			locModel = new EarthModel(refModel, convert);
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
			sample.printModel('P', "Tau");
			sample.sample('S');
			sample.printModel('S', "Tau");
			sample.merge();
			sample.printMerge();
			sample.depthModel('P');
			sample.printModel('P', "Depth");
			sample.depthModel('S');
			sample.printModel('S', "Depth");
			depModel = sample.getDepthModel();
//		depModel.printDepShells('P');
//		depModel.printDepShells('S');
			
			// Do the integrals.
			TablesUtil.deBugOrder = false;
			integrate = new Integrate(depModel);
			integrate.doTauIntegrals('P');
			integrate.doTauIntegrals('S');
			finModel = integrate.getFinalModel();
//		finModel.printShellInts('P');
//		finModel.printShellInts('S');
			// Reorganize the integral data.
			pieces = new TablePieces(finModel);
			pieces.printShellInts('P');
			pieces.printShellInts('S');
//		pieces.printProxy();
			decimate = new DecTTbranch(pieces, convert);
			decimate.upGoingDec('P');
			decimate.upGoingDec('S');
//		pieces.pPieces.printDec();
//		pieces.sPieces.printDec();
			pieces.printProxy();
			
			// Make the branches.
			finModel.printDepShells('P');
			finModel.printDepShells('S');
			layout = new MakeBranches(finModel, pieces, decimate);
			layout.readPhases();
			layout.printPhases();
			layout.printBranches(false, true);
		} else {
			System.out.println("Read status = "+status);
		}
	}
}
