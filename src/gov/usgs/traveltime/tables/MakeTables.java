package gov.usgs.traveltime.tables;

import java.util.ArrayList;
import java.util.TreeSet;

import gov.usgs.traveltime.AllBrnRef;
import gov.usgs.traveltime.AuxTtRef;
import gov.usgs.traveltime.ModConvert;
import gov.usgs.traveltime.TtStatus;

/**
 * Travel-time table generation driver.
 * 
 * @author Ray Buland
 *
 */
public class MakeTables {
	TauModel finModel;
	TablePieces pieces;
	ArrayList<BrnData> brnData;

	/**
	 * Create the travel-time tables out of whole cloth.
	 * 
	 * @param earthModel Name of the Earth model
	 * @return Model read status
	 * @throws Exception If an integration interval is illegal
	 */
	public TtStatus buildModel(String earthModel) throws Exception {
		EarthModel refModel, locModel;
		ModConvert convert;
		TauModel depModel;
		SampleSlowness sample;
		Integrate integrate;
		DecTTbranch decimate;
		MakeBranches layout;
		TreeSet<Double> ends;
		TtStatus status;
		
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
			brnData = layout.getBranches();
			// Do the final decimation.
			pieces.decimateP();
			pieces.printP();
			finModel.decimateTauX('P', pieces.getDecimation('P'));
			finModel.decimateTauX('S', pieces.getDecimation('S'));
			// Print the final branches.
			layout.printBranches(true, true);
			// Build the branch end ranges.
			ends = layout.getBranchEnds();
			depModel.makeXUp('P', ends);
			depModel.makeXUp('S', ends);
			finModel.setXUp(depModel);
		}
		return status;
	}
	
	/**
	 * Fill in all the reference data needed to calculate travel times 
	 * from the table generation.
	 * 
	 * @param auxTT Auxiliary travel-time data
	 * @return The reference data for all branches
	 */
	public AllBrnRef fillAllBrnRef(AuxTtRef auxTT) {
		return new AllBrnRef(finModel, pieces, brnData, auxTT);
	}
}
