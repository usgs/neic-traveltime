package gov.usgs.traveltime.tables;

import java.util.Arrays;

/**
 * Tau and range integrals for all ray parameters at one depth.  
 * Note that the corresponding ray parameters are stored in the 
 * merged slownesses.
 * 
 * @author Ray Buland
 *
 */
public class TauXsample {
	boolean lvz;						// True if this is the top of a high slowness zone
	TauSample modelSample;	// Model sample
	double[] tau;						// Non-dimensional tau
	double[] x;							// Non-dimensional range (ray travel distance)
	
	/**
	 * Allocate some array space.
	 * 
	 * @param modelSample Model sample for this depth
	 * @param n Length of the tau and x arrays.
	 * @param tau Tau array
	 * @param x X (range) array
	 */
	public TauXsample(TauSample modelSample, int n, double[] tau, double[] x) {
		this.modelSample = modelSample;
		this.tau = Arrays.copyOf(tau, n);
		this.x = Arrays.copyOf(x, n);
		lvz = false;
	}
	
	@Override
	public String toString() {
		return String.format("%3d %9.6f %8.6f", tau.length, modelSample.z, 
				modelSample.slow);
	}
}
