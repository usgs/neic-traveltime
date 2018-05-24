package gov.usgs.traveltime.tables;

import gov.usgs.traveltime.ModConvert;

/**
 * One sample in the alternative view of the Earth model suitable 
 * for the tau-p travel-time calculation.
 * 
 * @author Ray Buland
 *
 */
public class TauSample {
	int index;			// Index into the tau versus depth table
	double r;				// Radius in kilometers
	double z;				// Non-dimensional depth
	double slow;		// Non-dimensional model slowness
	double x;				// Non-dimensional ray travel distance
	
	/**
	 * Initialize this sample.
	 * 
	 * @param r Dimensional Earth radius in kilometers
	 * @param slow Non-dimensional slowness
	 * @param x Non-dimensional ray travel distance (range)
	 */
	public TauSample(double r, double slow, double x) {
		this.r = r;
		this.slow = slow;
		this.x = x;
		z = Double.NaN;
	}
	
	/**
	 * Initialize this sample.  For some purposes, we don't need 
	 * range.
	 * 
	 * @param r Dimensional Earth radius in kilometers
	 * @param slow Non-dimensional slowness
	 * @param convert Model dependent constants
	 */
	public TauSample(double r, double slow, ModConvert convert) {
		this.r = r;
		this.slow = slow;
		x = Double.NaN;
		z = Math.log(r*convert.xNorm);
	}
	
	/**
	 * Initialize this sample from another sample.
	 * 
	 * @param sample Existing tau sample
	 */
	public TauSample(TauSample sample) {
		this.r = sample.r;
		this.slow = sample.slow;
		this.x = sample.x;
		this.z = sample.z;
	}
	
	/**
	 * Update this sample.
	 * 
	 * @param r Dimensional Earth radius in kilometers
	 * @param slow Non-dimensional slowness
	 * @param x Non-dimensional ray travel distance (range)
	 */
	public void update(double r, double slow, double x) {
		this.r = r;
		this.slow = slow;
		this.x = x;
	}
	
	/**
	 * Complete the model by adding depth and an index into tau versus 
	 * depth storage.
	 * 
	 * @param z Non-dimensional Earth flattened depth
	 * @param index Index into tau(z) storage
	 */
	public void addZ(double z, int index) {
		this.z = z;
		this.index = index;
	}
	
	@Override
	public String toString() {
		if(!Double.isNaN(x)) {
			return String.format("%7.2f %8.6f %8.6f", r, slow, x);
		} else if(!Double.isNaN(z)) {
			return String.format("%7.2f %8.6f %8.6f", r, slow, z);
		} else {
			return String.format("%7.2f %8.6f", r, slow);
		}
	}
}
