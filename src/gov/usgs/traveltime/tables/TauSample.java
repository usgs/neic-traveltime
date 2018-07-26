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
	 * @param index Index into the merged slownesses
	 * @param convert Model dependent constants
	 */
	public TauSample(double r, double slow, int index, ModConvert convert) {
		this.r = r;
		this.slow = slow;
		this.index = index;
		x = Double.NaN;
		z = convert.flatZ(r);
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
	 * Initialize this sample from another sample and add an index.
	 * 
	 * @param sample Existing tau sample
	 * @param index Index
	 */
	public TauSample(TauSample sample, int index) {
		this.r = sample.r;
		this.slow = sample.slow;
		this.x = sample.x;
		this.z = sample.z;
		this.index = index;
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
	 * Get the model depth.
	 * 
	 * @return Non-dimensional Earth flattened depth
	 */
	public double getZ() {
		return z;
	}
	
	/**
	 * Get the model slowness.
	 * 
	 * @return Non-dimensional slowness
	 */
	public double getSlow() {
		return slow;
	}
	
	/**
	 * Get the depth index.  This used to be a direct access file 
	 * record number.  Now it is used to access the correct up-going 
	 * tau and range for depth correction.
	 * 
	 * @return The depth index
	 */
	public int getIndex() {
		return index;
	}
	
	@Override
	public String toString() {
		if(!Double.isNaN(x)) {
			return String.format("%7.2f %8.6f %8.6f", r, slow, x);
		} else if(!Double.isNaN(z)) {
			if(TablesUtil.deBugOrder) {
				return String.format("%7.2f %8.6f %8.6f %3d", r, slow, z, 
						TablesUtil.deBugOffset-index);
			} else {
				return String.format("%7.2f %8.6f %8.6f %3d", r, slow, z, index);
			}
		} else {
			if(TablesUtil.deBugOrder) {
				return String.format("%7.2f %8.6f   NaN    %3d", r, slow, 
						TablesUtil.deBugOffset-index);
			} else {
				return String.format("%7.2f %8.6f   NaN    %3d", r, slow, index);
			}
		}
	}
}
