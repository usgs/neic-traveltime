package gov.usgs.traveltime.tables;

import java.util.ArrayList;

/**
 * An alternative version of the earth model with sampling suitable 
 * for the tau-p travel-time calculation.
 * 
 * @author Ray Buland
 *
 */
public class TauModel {
	ArrayList<TauSample> pModel, sModel;
	
	/**
	 * Allocate lists for independent P and S models.
	 */
	public TauModel() {
		pModel = new ArrayList<TauSample>();
		sModel = new ArrayList<TauSample>();
	}
	
	/**
	 * Add a point to the model.
	 * 
	 * @param type Model type (P = P slowness, S = S slowness)
	 * @param r Dimensional Earth radius in kilometers
	 * @param slow Non-dimensional slowness
	 * @param x Non-dimensional ray travel distance
	 */
	public void addSample(char type, double r, double slow, double x) {
		if(type == 'P') {
			pModel.add(new TauSample(r, slow, x));
		} else {
			sModel.add(new TauSample(r, slow, x));
		}
	}
}
