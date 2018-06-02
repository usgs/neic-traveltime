package gov.usgs.traveltime.tables;

import gov.usgs.traveltime.ModConvert;
import gov.usgs.traveltime.TauUtil;

/**
 * Compute tau and range integrals for all merged slowness values.  
 * Combining these raw materials for P and S will allow the construction 
 * of all required phases as well as the up-going branches for depth 
 * correcting all phases.
 * 
 * @author Ray Buland
 *
 */
public class Integrate {
	double zMax;				// Non-dimensional flattened maximum earthquake depth
	double zOuterCore;	// Non-dimensional flattened outer core depth
	double zInnerCore;	// Non-dimensional flattened inner core depth
	EarthModel refModel;
	TauModel depModel;
	ModConvert convert;

	/**
	 * Remember various incarnations of the model.
	 * 
	 * @param refModel Reference Earth model
	 * @param depModel Slowness depth model
	 * @param convert Model dependent conversions
	 */
	public Integrate(EarthModel refModel, TauModel depModel, ModConvert convert) {
		this.refModel = refModel;
		this.depModel = depModel;
		this.convert = convert;
		zMax = convert.flatZ(convert.rSurface-TauUtil.MAXDEPTH);
		zOuterCore = refModel.outerCore.z;
		zInnerCore = refModel.innerCore.z;
		System.out.format("\n\tzMax zOC zIC %8.6f %8.6f %8.6f\n", zMax, zOuterCore, 
				zInnerCore);
	}
	
	/**
	 * Do all the tau and range integrals we'll need later.
	 * 
	 * @param type Model type (P = P slowness, S = S slowness)
	 */
	public void doTauIntegrals(char type) {
		int mm, n1;
		
		mm = depModel.size(type);
		n1 = depModel.getShell(type, 0).iBot-depModel.getShell(type, 
				depModel.shellSize(type)-1).iTop;
		System.out.format("mm = %d n1 = %d\n", mm, n1);
	}
}
