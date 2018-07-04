package gov.usgs.traveltime.tables;

import java.util.ArrayList;

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
	int nRec = 0;
	double zMax;				// Non-dimensional flattened maximum earthquake depth
	double zOuterCore;	// Non-dimensional flattened outer core depth
	double zInnerCore;	// Non-dimensional flattened inner core depth
	EarthModel refModel;
	TauModel depModel, finModel;
//TauXints integrals;
	TauInt tauInt;
	ModConvert convert;
	ArrayList<Double> slowness;

	/**
	 * Remember various incarnations of the model.
	 * 
	 * @param depModel Slowness depth model
	 */
	public Integrate(TauModel depModel) {
		this.depModel = depModel;
		refModel = depModel.refModel;
		convert = depModel.convert;
		finModel = new TauModel(refModel, convert);
		finModel.initIntegrals();
		tauInt = new TauInt();
		slowness = depModel.slowness;
		finModel.putSlowness(slowness);
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
	 * @throws Exception If the tau integration interval is invalid
	 */
	public void doTauIntegrals(char type) throws Exception {
		boolean disc = false;
		int n1, iRay = 0;
		double zLim, zLast;
		double[] tau, x;
		TauSample sample0, sample1;
		
		zLim = modelDepth(type);
		n1 = depModel.getShell(type, 0).iBot-depModel.getShell(type, 
				depModel.shellSize(type)-1).iTop;
		System.out.format("\nmm = %d n1 = %d\n\n", depModel.size(type), n1);
		tau = new double[n1];
		x = new double[n1];
		
		sample1 = depModel.getSample(type, 0);
		finModel.add(type, sample1, -1);
		zLast = sample1.z;
		// Loop over depth intervals.
		for(int i=1; i<depModel.size(type); i++) {
			sample0 = sample1;
			sample1 = depModel.getSample(type, i);
			if(sample0.z != sample1.z) {
				// Normal interval: do integrals for all ray parameters.
				if(disc) {
					disc = false;
					finModel.add(type, sample0, nRec);
				}
				iRay = 0;
				for(int j=slowness.size()-1; j>=0; j--) {
					tau[iRay] += tauInt.intLayer(slowness.get(j), sample0.slow, 
							sample1.slow, sample0.z, sample1.z);
					x[iRay++] += tauInt.getXLayer();
					if(sample1.slow == slowness.get(j)) break;
				}
				if(sample0.z >= zLim) {
					if(sample0.z >= zMax) {
						nRec++;
						finModel.add(type, sample1, nRec, new TauXsample(iRay, tau, x, 
								ShellName.UPPER_MANTLE));
						System.out.format("lev1 %c %3d %s\n", type, finModel.size(type)-1, 
								finModel.stringLast(type));
					} else {
						finModel.add(type, sample1, nRec);
					}
				}
				zLast = sample0.z;
			} else {
				// We're in a discontinuity.
				if(sample1.z != zLast) {
					// Save the integrals at the bottom of the mantle and outer core.
					if(sample1.z == zOuterCore || sample1.z == zInnerCore) {
						nRec++;
						if(sample1.z == zOuterCore) {
							finModel.add(type, sample1, nRec, new TauXsample(n1, tau, x, 
									ShellName.CORE_MANTLE_BOUNDARY));
						} else {
							finModel.add(type, sample1, nRec, new TauXsample(n1, tau, x, 
									ShellName.INNER_CORE_BOUNDARY));
						}
						System.out.format("lev2 %c %3d %3d %9.6f %8.6f\n", type, 
								finModel.size(type)-1, iRay, sample1.z, sample1.slow);
					} else {
						disc = true;
					}
					// Flag high slowness zones below discontinuities.
					if(sample1.slow > sample0.slow) {
						finModel.setLvz(type);
						System.out.format("lvz  %c %3d %8.6f %8.6f\n", type, iRay, tau[iRay-1], 
								x[iRay-1]);
					}
					zLast = sample0.z;
				}
			}
		}
		// Save the integrals down to the center of the Earth.
		nRec++;
		finModel.add(type, sample1, nRec, new TauXsample(n1, tau, x, ShellName.CENTER));
		System.out.format("lev3 %c %3d %3d %9.6f %8.6f\n", type, 
				finModel.size(type)-1, iRay, sample1.z, sample1.slow);
		finModel.printModel(type, "Final");
		// We'll still need access to the merged slownesses.
		finModel.putSlowness(depModel.slowness);
	}
	
	/**
	 * Find the bottoming depth of converted mantle phases.  Because the P velocity 
	 * is higher than the S velocity, phases from the deepest source depth can't 
	 * bottom any deeper.  However, P to S conversions can go much deeper (nearly to 
	 * the core).
	 * 
	 * @param type Model type (P = P slowness, S = S slowness)
	 * @return The deepest depth that needs to be remembered for the travel-time 
	 * computation
	 */
	private double modelDepth(char type) {
		int j;
		double pLim;
		
		if(type == 'P') {
			return zMax;
		} else {
			for(j=0; j<depModel.size('P'); j++) {
				if(depModel.getSample('P', j).z < zMax) break;
			}
			pLim = depModel.getSample('P', j).slow;
			System.out.format("\ni zMax pLim zm = %3d %9.6f %8.6f %9.6f\n", j, zMax, 
					pLim, depModel.getSample('P', j).z);
			for(j=0; j<depModel.size('S'); j++) {
				if(depModel.getSample('S', j).slow <= pLim) break;
			}
			System.out.format("i pLim zLim = %3d %8.6f %9.6f\n", j, pLim, 
					depModel.getSample('S', j).z);
			return depModel.getSample('S', j).z;
		}
	}
}
