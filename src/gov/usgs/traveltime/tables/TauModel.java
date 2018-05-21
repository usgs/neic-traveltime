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
	ArrayList<Double> slowness;
	
	/**
	 * Allocate lists for independent P and S models.
	 */
	public TauModel() {
		pModel = new ArrayList<TauSample>();
		sModel = new ArrayList<TauSample>();
	}
	
	/**
	 * Add a sample to the model.
	 * 
	 * @param type Model type (P = P slowness, S = S slowness)
	 * @param sample Complete sample created externally
	 */
	public void add(char type, TauSample sample) {
		if(type == 'P') {
			pModel.add(new TauSample(sample));
		} else {
			sModel.add(new TauSample(sample));
		}
	}
	
	/**
	 * Get a tau model sample.
	 * 
	 * @param type Model type (P = P slowness, S = S slowness)
	 * @param index Model index
	 * @return Model sample
	 */
	public TauSample getSample(char type, int index) {
		if(type == 'P') {
			return pModel.get(index);
		} else {
			return sModel.get(index);
		}
	}
	
	/**
	 * Get the last model sample added.
	 * 
	 * @param type Model type (P = P slowness, S = S slowness)
	 * @return Last model sample
	 */
	public TauSample getLast(char type) {
		if(type == 'P') {
			return pModel.get(pModel.size()-1);
		} else {
			return sModel.get(sModel.size()-1);
		}
	}
	
	/**
	 * Replace the last sample in the model.
	 * 
	 * @param type Model type (P = P slowness, S = S slowness)
	 * @param sample New model sample
	 */
	public void setLast(char type, TauSample sample) {
		if(type == 'P') {
			pModel.set(pModel.size()-1, new TauSample(sample));
		} else {
			sModel.set(sModel.size()-1, new TauSample(sample));
		}
	}
	
	/**
	 * Get the number of model samples.
	 * 
	 * @param type Model type (P = P slowness, S = S slowness)
	 * @return Number of model samples
	 */
	public int size(char type) {
		if(type == 'P') {
			return pModel.size();
		} else {
			return sModel.size();
		}
	}
	
	/**
	 * Merge the P- and S-wave slownesses into a single list.  To 
	 * avoid making the spacing very non-uniform, the merge is done 
	 * between critical slownesses.  For each interval, the sampling 
	 * that seems most complete (judging by the number of samples) 
	 * is used.
	 * 
	 * @param locModel Internal Earth model
	 */
	public void merge(InternalModel locModel) {
		int pBeg = 1, pEnd, sBeg = 0, sEnd;
		ArrayList<CritSlowness> critical;
		CritSlowness crit0, crit1;
		
		critical = locModel.critical;
		slowness = new ArrayList<Double>();
		crit1 = critical.get(critical.size()-1);
		for(int iCrit=critical.size()-2; iCrit>=0; iCrit--) {
			crit0 = crit1;
			crit1 = critical.get(iCrit);
			System.out.format("\tInterval: %8.6f %8.6f\n", crit0.slowness, crit1.slowness);
			if(crit0.slowness <= pModel.get(pBeg-1).slow) {
				for(pEnd=pBeg; pEnd<pModel.size(); pEnd++) {
					if(crit1.slowness == pModel.get(pEnd).slow) break;
				}
			} else {
				pEnd = 0;
			}
			for(sEnd=sBeg; sEnd<sModel.size(); sEnd++) {
				if(crit1.slowness == sModel.get(sEnd).slow) break;
			}
			
			System.out.format("\tIndices: P: %d %d S: %d %d\n", pBeg, pEnd, sBeg, sEnd);
			if(pEnd-pBeg > sEnd-sBeg) {
				for(int j=pBeg; j<=pEnd; j++) {
					slowness.add(pModel.get(j).slow);
				}
			} else {
				for(int j=sBeg; j<=sEnd; j++) {
					slowness.add(sModel.get(j).slow);
				}
			}
			pBeg = ++pEnd;
			sBeg = ++sEnd;
		}
	}
	
	/**
	 * Print out the slowness model.
	 * 
	 * @param type Model type (P = P slowness, S = S slowness)
	 */
	public void printModel(char type) {
		System.out.println("\nTau model for "+type+" slowness");
		if(type == 'P') {
			for(int j=0; j<pModel.size(); j++) {
				System.out.format("%3d %s\n", j, pModel.get(j));
			}
		} else {
			for(int j=0; j<sModel.size(); j++) {
				System.out.format("%3d %s\n", j, sModel.get(j));
			}
		}
	}
	
	/**
	 * Print out the merged slownesses.
	 */
	public void printMerge() {
		System.out.println("\n Merged slownesses");
		for(int j=0; j<slowness.size(); j++) {
			System.out.format("%3d %8.6f\n", j, slowness.get(j));
		}
	}
}
