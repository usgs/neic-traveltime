package gov.usgs.traveltime.tables;

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
	 * @param x Non-dimensional ray travel distance
	 * respect to slowness
	 */
	public TauSample(double r, double slow, double x) {
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
		return String.format("%7.2f %8.6f %8.6f", r, slow, x);
	}
}
