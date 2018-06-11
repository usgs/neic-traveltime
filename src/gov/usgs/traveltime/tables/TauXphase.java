package gov.usgs.traveltime.tables;

import java.util.ArrayList;

/**
 * Tau and range (X) integrals for all ray parameters and all required 
 * depths for one phase type.  Note that we only need depths down to the 
 * deepest earthquake (providing up-going branches and depth correction) 
 * plus integrals for the full mantle, outer core, and inner core (to 
 * construct composite phases such as PKP).
 * 
 * @author Ray buland
 *
 */
public class TauXphase {
	ArrayList<TauXsample> tauX;
	TauXsample tauXmantle, tauXouterCore, tauXinnerCore;
	
	/**
	 * Allocate list space.
	 */
	public TauXphase() {
		tauX = new ArrayList<TauXsample>();
	}
	
	/**
	 * Add tau-range integrals for all ray parameters down to one model depth.
	 * 
	 * @param sample Set of tau-x integrals at one depth
	 */
	public void add(TauXsample sample) {
		tauX.add(sample);
	}
	
	/**
	 * Add special tau-range integrals for all ray parameters down to one 
	 * model depth.
	 * 
	 * @param shell Name of a region of the Earth
	 * @param sample Tau-range integrals to the bottom of the region of 
	 * the Earth
	 */
	public void add(String shell, TauXsample sample) {
		if(shell.equals("mantle")) {
			tauXmantle = sample;
		} else if(shell.equals("outercore")) {
			tauXouterCore = sample;
		} else if(shell.equals("innercore")) {
			tauXinnerCore = sample;
		} else {
			System.out.println("TauXphase: unknown shell ("+shell+").");
		}
	}
	
	/**
	 * Get a summary of the last entry in the tau-range integral 
	 * storage.
	 * 
	 * @return String summarizing the last tau-range integral sample
	 */
	public String stringLast() {
		return tauX.get(tauX.size()-1).toString();
	}
	
	/**
	 * Print a summary of the integrals for this phase.
	 */
	public void print() {
		for(int j=0; j<tauX.size(); j++) {
			System.out.format("Lev1 %3d %s\n", j, tauX.get(j));
		}
		System.out.format("Lev2     %s", tauXmantle);
		System.out.format("Lev2     %s", tauXouterCore);
		System.out.format("Lev3     %s", tauXinnerCore);
	}
}
