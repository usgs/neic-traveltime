package gov.usgs.traveltime.tables;

/**
 * One sample in the alternative view of the Earth model suitable 
 * for the tau-p travel-time calculation.
 * 
 * @author Ray Buland
 *
 */
public class TauSample {
	double r;				// Radius in kilometers
	double slow;		// Non-dimensional model slowness
	double x;				// Non-dimensional ray travel distance
	
	/**
	 * Initialize this sample.
	 * 
	 * @param r Dimensional Earth radius in kilometers
	 * @param slow Non-dimensional slowness
	 * @param x Non-dimensional ray travel distance
	 */
	public TauSample(double r, double slow, double x) {
		this.r = r;
		this.slow = slow;
		this.x = x;
	}
}
