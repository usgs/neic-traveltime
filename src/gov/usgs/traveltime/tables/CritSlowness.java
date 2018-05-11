package gov.usgs.traveltime.tables;

/**
 * Critical slowness value.  The critical points are the beginnings 
 * and end of travel-time branches.  They correspond to velocities 
 * above and below discontinuities in the Earth model as well as 
 * caustics.
 * 
 * @author Ray Buland
 *
 */
public class CritSlowness implements Comparable<CritSlowness> {
	char type;				// Slowness type
	int iShell;				// Shell index
	double slowness;	// Non-dimensional slowness
	
	/**
	 * One critical slowness value.
	 * 
	 * @param type Slowness type (P = P-wave, S = S-wave, & = both)
	 * @param iShell Associated Earth model shell index
	 * @param slowness Non-dimensional slowness value
	 */
	public CritSlowness(char type, int iShell, double slowness) {
		this.type = type;
		this.iShell = iShell;
		this.slowness = slowness;
	}

	@Override
	public int compareTo(CritSlowness crit) {
		if(this.slowness > crit.slowness) return +1;
		else if(this.slowness == crit.slowness) return 0;
		else return -1;
	}
	
	@Override
	public String toString() {
		return String.format("%c %8.6f %3d", type, slowness, iShell);
	}
}
