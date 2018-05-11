package gov.usgs.traveltime.tables;

import org.apache.commons.math3.analysis.UnivariateFunction;

/**
 * Univariate function returning the non-dimensional derivative 
 * of range (ray travel distance) as a function of ray parameter.  
 * This is intended to be used with the Apache Commons math 
 * library suite of root finders
 * 
 * @author Ray Buland
 *
 */
public class FindCaustic implements UnivariateFunction {
	char type;
	int limit;
	TauInt tauInt;

	/**
	 * Remember the tau-x integration routine.
	 * 
	 * @param tauInt Tau-X integration logic
	 */
	public FindCaustic(TauInt tauInt) {
		this.tauInt = tauInt;
	}
	
	/**
	 * Set up the root finding environment.
	 * 
	 * @param type Wave type (P = P-waves, S = S-waves)
	 * @param limit Index of the deepest model level to integrate to
	 */
	public void setUp(char type, int limit) {
		this.type = type;
		this.limit = limit;
	}
	
	@Override
	public double value(double p) {
		return tauInt.intDxDp(type, p, limit);
	}
}
