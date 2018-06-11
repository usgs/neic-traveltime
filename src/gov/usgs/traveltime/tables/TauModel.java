package gov.usgs.traveltime.tables;

import java.util.ArrayList;

import gov.usgs.traveltime.ModConvert;

/**
 * An alternative version of the earth model with sampling suitable 
 * for the tau-p travel-time calculation.
 * 
 * @author Ray Buland
 *
 */
public class TauModel {
	EarthModel refModel;
	ArrayList<TauSample> pModel, sModel;
	ArrayList<Double> slowness;
	ArrayList<ModelShell> pShells = null, sShells = null;
	ModConvert convert;
	
	/**
	 * Allocate lists for independent P and S models.
	 * 
	 * @param refModel Reference Earth model
	 * @param convert Model dependent conversions
	 */
	public TauModel(EarthModel refModel, ModConvert convert) {
		this.refModel = refModel;
		this.convert = convert;
		pModel = new ArrayList<TauSample>();
		sModel = new ArrayList<TauSample>();
	}
	
	/**
	 * Add a sample to the model.
	 * 
	 * @param type Model type (P = P slowness, S = S slowness)
	 * @param r Dimensional Earth radius in kilometers
	 * @param slow Non-dimensional slowness
	 * @param x Non-dimensional ray travel distance (range)
	 */
	public void add(char type, double r, double slow, double x) {
		if(type == 'P') {
			pModel.add(new TauSample(r, slow, x));
		} else {
			sModel.add(new TauSample(r, slow, x));
		}
	}
	
	/**
	 * Add a sample to the model.
	 * 
	 * @param type Model type (P = P slowness, S = S slowness)
	 * @param r Dimensional Earth radius in kilometers
	 * @param slow Non-dimensional slowness
	 * @param index Index into the merged slownesses
	 */
	public void add(char type, double r, double slow, int index) {
		if(type == 'P') {
			pModel.add(new TauSample(r, slow, index, convert));
		} else {
			sModel.add(new TauSample(r, slow, index, convert));
		}
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
	 * Add a sample with an index to the model.
	 * 
	 * @param type Model type (P = P slowness, S = S slowness)
	 * @param sample Complete sample created externally
	 * @param index Index
	 */
	public void add(char type, TauSample sample, int index) {
		if(type == 'P') {
			pModel.add(new TauSample(sample, index));
		} else {
			sModel.add(new TauSample(sample, index));
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
	 * Get a depth model shell.
	 * 
	 * @param type Model type (P = P slowness, S = S slowness)
	 * @param index Depth model shell index
	 * @return Depth model shell
	 */
	public ModelShell getShell(char type, int index) {
		if(type == 'P') {
			return pShells.get(index);
		} else {
			return sShells.get(index);
		}
	}
	
	/**
	 * Get the number of shells.
	 * 
	 * @param type Model type (P = P slowness, S = S slowness)
	 * @return Number of shells
	 */
	public int shellSize(char type) {
		if(type == 'P') {
			return pShells.size();
		} else {
			return sShells.size();
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
			if(TablesUtil.deBugLevel > 1) System.out.format("\tInterval: "+
					"%8.6f %8.6f\n", crit0.slowness, crit1.slowness);
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
			
			if(TablesUtil.deBugLevel > 1) System.out.format("\tIndices: "+
					"P: %d %d S: %d %d\n", pBeg, pEnd, sBeg, sEnd);
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
	 * The merged slownesses are created in tau model, but are needed in 
	 * depth model as well.
	 * 
	 * @param slowness Merged list of non-dimensional slownesses
	 */
	public void putSlowness(ArrayList<Double> slowness) {
		this.slowness = slowness;
	}
	
	/**
	 * Make yet another set of shells.  This time, the shell indices are 
	 * into the P- and S-wave tau models, so the indices will be different 
	 * for each model.
	 * 
	 * @param type Model type (P = P slowness, S = S slowness)
	 */
	public void makeDepShells(char type) {
		int iBeg, iEnd;
		double slowTop;
		ModelShell refShell, newShell, lastShell = null;
		
		if(type == 'P') {
			pShells = new ArrayList<ModelShell>();
			iEnd = pModel.size()-1;
			for(int i=0; i<refModel.shells.size(); i++) {
				refShell = refModel.shells.get(i);
				slowTop = refModel.getSlow(type, refShell.iTop);
				iBeg = iEnd;
				for(iEnd=iBeg; iEnd>=0; iEnd--) {
					if(pModel.get(iEnd).slow == slowTop) break;
				}
				newShell = new ModelShell(refShell, pModel.get(iBeg).index);
				newShell.addEnd(pModel.get(iEnd).index, refShell.rTop);
				if(slowTop > refModel.getSlow(type, refShell.iBot)) {
					if(lastShell != null) {
						if(!lastShell.pCode.equals(newShell.pCode)) {
							lastShell.iTop = newShell.iBot;
							pShells.add(newShell);
							lastShell = newShell;
						} else {
							lastShell.iTop = newShell.iTop;
							lastShell.rTop = newShell.rTop;
						}
					} else {
						pShells.add(newShell);
						lastShell = newShell;
					}
				}
			}
		} else {
			sShells = new ArrayList<ModelShell>();
			iEnd = sModel.size()-1;
			for(int i=0; i<refModel.shells.size(); i++) {
				refShell = refModel.shells.get(i);
				slowTop = refModel.getSlow(type, refShell.iTop);
				iBeg = iEnd;
				for(iEnd=iBeg; iEnd>=0; iEnd--) {
					if(sModel.get(iEnd).slow == slowTop) break;
				}
				newShell = new ModelShell(refShell, sModel.get(iBeg).index);
				newShell.addEnd(sModel.get(iEnd).index, refShell.rTop);
				if(slowTop > refModel.getSlow(type, refShell.iBot)) {
					if(lastShell != null) {
						if(!lastShell.sCode.equals(newShell.sCode)) {
							sShells.add(newShell);
							lastShell = newShell;
						} else {
							lastShell.iTop = sModel.get(iEnd).index;
							lastShell.rTop = newShell.rTop;
						}
					} else {
						sShells.add(newShell);
						lastShell = newShell;
					}
				}
			}
		}
	}
	
	/**
	 * Print out the slowness model.
	 * 
	 * @param type Model type (P = P slowness, S = S slowness)
	 * @param tau If true print the slowness model header, if false print the 
	 * depth model header
	 */
	public void printModel(char type, boolean tau) {
		if(tau) {
			System.out.println("\n   Tau model for "+type+" slowness");
			System.out.println("      R     slowness    X");
		} else {
			System.out.println("\n     Depth model for "+type+" slowness");
			System.out.println("      R     slowness      Z");
			if(TablesUtil.deBugOrder) {
				TablesUtil.deBugOffset = slowness.size()-1;
			}
		}
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
		System.out.println("\n\tMerged slownesses");
		for(int j=0; j<slowness.size(); j++) {
			System.out.format("\t%3d %8.6f\n", j, slowness.get(j));
		}
	}
	
	/**
	 * Print the wave type specific shells for the depth model.
	 * 
	 * @param type Wave type (P = compressional, S = shear)
	 */
	public void printDepShells(char type) {
		String shellLine;
		
		System.out.println("\n\t"+type+" Model Shells:");
		if(TablesUtil.deBugOrder) {
			TablesUtil.deBugOffset = slowness.size()-1;
		}
		if(type == 'P') {
			for(int j=0; j<pShells.size(); j++) {
				shellLine = pShells.get(j).printTau(type);
				if(shellLine != null) System.out.format("%3d   %s\n", j, shellLine);
			}
		} else {
			for(int j=0; j<sShells.size(); j++) {
				shellLine = sShells.get(j).printTau(type);
				if(shellLine != null) System.out.format("%3d   %s\n", j, shellLine);
			}
		}
	}
}
