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
			
			// Make the initial slowness sampling.
			sample = new SampleSlowness(locModel);
			sample.sample('P');
			sample.printModel('P', "Tau");
			sample.sample('S');
			sample.printModel('S', "Tau");
			// We need a merged set of slownesses for converted branches 
			// (e.g., ScP).
			sample.merge();
			sample.printMerge();
			// Fiddle with the sampling so that low velocity zones are 
			// better sampled.
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
			// The final model only includes depth samples that will be 
			// of interest for earthquake location.
			finModel = integrate.getFinalModel();
//		finModel.printShellInts('P');
//		finModel.printShellInts('S');
			// Reorganize the integral data.
			pieces = new TablePieces(finModel);
			pieces.printShellInts('P');
			pieces.printShellInts('S');
//		pieces.printProxy();		// Proxy depth sampling before decimation
			// Decimate the default sampling for the up-going branches.
			decimate = new DecTTbranch(pieces, convert);
			decimate.upGoingDec('P');
			decimate.upGoingDec('S');
//		pieces.pPieces.printDec();
//		pieces.sPieces.printDec();
			pieces.printProxy();		// Proxy depth sampling after decimation
			
			// Make the branches.
			layout = new MakeBranches(finModel, pieces, decimate);
			layout.readPhases();		// Read the desired phases from a file
			layout.printPhases();
			layout.printBranches(false, true);
			// Do the final decimation.
			pieces.decimateP();
			pieces.printP();
			finModel.decimateTauX('P', pieces.getDecimation('P'));
			finModel.decimateTauX('S', pieces.getDecimation('S'));
			// Print the final branches.
			layout.printBranches(true, true);
		} else {
			System.out.println("Read status = "+status);
		}
	}
}
