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
	char type;				// Slowness type of this critical slowness
	ShellLoc loc;			// Position of the critical slowness within the shell
	int pShell = -1;	// P-wave slowness shell index
	int sShell = -1;	// S-wave slowness shell index
	double slowness;	// Non-dimensional slowness
	
	/**
	 * One critical slowness value.
	 * 
	 * @param type Slowness type (P = P-wave, S = S-wave)
	 * @param iShell Associated Earth model shell index
	 * @param loc Location within the Earth model shell
	 * @param slowness Non-dimensional slowness value
	 */
	public CritSlowness(char type, int iShell, ShellLoc loc, double slowness) {
		this.type = type;
		this.loc = loc;
		this.slowness = slowness;
		if(type == 'P') {
			pShell = iShell;
		} else {
			sShell = iShell;
		}
	}
	
	/**
	 * Add a shell index.
	 * 
	 * @param type Slowness type (P = P-wave, S = S-wave)
	 * @param iShell Associated Earth model shell index
	 */
	public void addShell(char type, int iShell) {
		if(type == 'P') {
			pShell = iShell;
		} else {
			sShell = iShell;
		}
	}
	
	/**
	 * Get the shell index.
	 * 
	 * @param type Slowness type (P = P-wave, S = S-wave)
	 * @return The Associated Earth model shell index
	 */
	public int getShell(char type) {
		if(type == 'P') {
			return pShell;
		} else {
			return sShell;
		}
	}
	
	/**
	 * Compare two critical values.
	 * 
	 * @param crit Critical value
	 * @return True if the critical value matches this one
	 */
	public boolean isSame(CritSlowness crit) {
		if(this.slowness == crit.slowness && this.type == crit.type) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public int compareTo(CritSlowness crit) {
		if(this.slowness > crit.slowness) return +1;
		else if(this.slowness < crit.slowness) return -1;
		else {
			// If the slownesses are the same look at the position.
			if(this.loc == ShellLoc.BOUNDARY) return +1;
			else if(this.loc == ShellLoc.SHELL) return -1;
			else return 0;
		}
	}
	
	@Override
	public String toString() {
		return String.format("%c %-8s %8.6f %3d %3d", type, loc, slowness, 
				pShell, sShell);
	}
}
