package gov.usgs.traveltime.tables;

/**
 * Integrals for P- and S-wave slowness models.
 * 
 * @author Ray Buland
 *
 */
public class TauXints {
	TauXphase intP, intS;
	
	/**
	 * Initialize classes to store the integrals by phase type.
	 */
	public TauXints() {
		intP = new TauXphase();
		intS = new TauXphase();
	}
	
	/**
	 * Store tau and range integrals for all ray parameters down 
	 * to one bottoming depth for one phase type.
	 * 
	 * @param type Model type (P = P slowness, S = S slowness)
	 * @param sample Set of tau and range integrals
	 */
	public void add(char type, TauXsample sample) {
		if(type == 'P') {
			intP.add(sample);
		} else {
			intS.add(sample);
		}
	}
	
	/**
	 * Add special tau-range integrals for all ray parameters down to one 
	 * model depth for one phase type.
	 * 
	 * @param type Model type (P = P slowness, S = S slowness)
	 * @param shell Name of a region of the Earth
	 * @param sample Tau-range integrals to the bottom of the region of 
	 * the Earth
	 */
	public void add(char type, String shell, TauXsample sample) {
		if(type == 'P') {
			intP.add(shell, sample);
		} else {
			intS.add(shell, sample);
		}
	}
	
	/**
	 * Set the low velocity zone (really high slowness zone) flag.
	 * 
	 * @param type Model type (P = P slowness, S = S slowness)
	 */
	public void setLvz(char type) {
		if(type == 'P') {
			intP.tauX.get(intP.tauX.size()-1).lvz = true;
		} else {
			intS.tauX.get(intS.tauX.size()-1).lvz = true;
		}
	}
	
	/**
	 * Access the toString for the last entry of the tau-range integrals 
	 * for the desired phase type.
	 * 
	 * @param type Model type (P = P slowness, S = S slowness)
	 * @return String summarizing the last tau-range integral sample
	 */
	public String stringLast(char type) {
		if(type == 'P') {
			return intP.stringLast();
		} else {
			return intS.stringLast();
		}
	}
	
	/**
	 * Print a summary of all the tau-range integrals for the desired 
	 * phase type.
	 * 
	 * @param type Model type (P = P slowness, S = S slowness)
	 */
	public void print(char type) {
		if(type == 'P') {
			intP.print();
		} else {
			intS.print();
		}
	}
}
