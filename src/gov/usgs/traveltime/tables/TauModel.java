package gov.usgs.traveltime.tables;

import java.util.ArrayList;

import gov.usgs.traveltime.ModConvert;

/**
 * An alternative version of the earth model with sampling suitable 
 * for the tau-p travel-time calculation.  Unlike the Earth models, the 
 * tau models are in depth order (i.e., the first point is at the free 
 * surface).  This class is used multiple times for various views of 
 * the model.  The final model also includes the tau and range integrals.
 * 
 * @author Ray Buland
 *
 */
public class TauModel {
	EarthModel refModel;
	ArrayList<TauSample> pModel, sModel;
	ArrayList<Double> slowness;
	ArrayList<ModelShell> pShells = null, sShells = null;
	ArrayList<TauXsample> pInts = null, sInts = null;
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
	 * Only the final model actually has integrals, so initialize 
	 * them separately.
	 */
	public void initIntegrals() {
		pInts = new ArrayList<TauXsample>();
		sInts = new ArrayList<TauXsample>();
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
	 * Add a sample with an index to the model and at the same time 
	 * store tau and range integrals for all ray parameters down 
	 * to this bottoming depth for the specified phase type.
	 * 
	 * @param type Model type (P = P slowness, S = S slowness)
	 * @param sample Complete sample created externally
	 * @param index Index
	 * @param tauX Set of tau and range integrals
	 */
	public void add(char type, TauSample sample, int index, TauXsample tauX) {
		if(type == 'P') {
			for(int j=pInts.size(); j<pModel.size(); j++) {
				pInts.add(null);
			}
			pModel.add(new TauSample(sample, index));
			pInts.add(tauX);
		} else {
			for(int j=sInts.size(); j<sModel.size(); j++) {
				sInts.add(null);
			}
			sModel.add(new TauSample(sample, index));
			sInts.add(tauX);
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
	public void merge(EarthModel locModel) {
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
	 * Set the low velocity zone (really high slowness zone) flag.
	 * 
	 * @param type Model type (P = P slowness, S = S slowness)
	 */
	public void setLvz(char type) {
		if(type == 'P') {
			pInts.get(pInts.size()-1).lvz = true;
		} else {
			sInts.get(sInts.size()-1).lvz = true;
		}
	}
	
	/**
	 * Get one of the special integral sets by name.
	 * 
	 * @param type Model type (P = P slowness, S = S slowness)
	 * @param name Special integral set name
	 * @return Tau integrals for all ray parameters
	 */
	public double[] getTauInt(char type, ShellName name) {
		int n;
		
		if(type == 'P') {
			n = pInts.size();
				for(int j=n-3; j<n; j++) {
					if(name == pInts.get(j).name) {
						return pInts.get(j).tau;
					}
				}
		} else {
			n = sInts.size();
			for(int j=n-3; j<n; j++) {
				if(name == sInts.get(j).name) {
					return sInts.get(j).tau;
				}
			}
		}
		return null;
	}
	
	/**
	 * Get one of the special integral sets by name.
	 * 
	 * @param type Model type (P = P slowness, S = S slowness)
	 * @param name Special integral set name
	 * @return Range integrals for all ray parameters
	 */
	public double[] getXInt(char type, ShellName name) {
		int n;
		
		if(type == 'P') {
			n = pInts.size();
				for(int j=n-3; j<n; j++) {
					if(name == pInts.get(j).name) {
						return pInts.get(j).x;
					}
				}
		} else {
			n = sInts.size();
			for(int j=n-3; j<n; j++) {
				if(name == sInts.get(j).name) {
					return sInts.get(j).x;
				}
			}
		}
		return null;
	}
	
	/**
	 * Print out the slowness model.
	 * 
	 * @param type Model type (P = P slowness, S = S slowness)
	 * @param ver Model version ("Tau", "Depth", or "Final")
	 */
	public void printModel(char type, String ver) {
		if(ver.equals("Depth")) {
			System.out.println("\n     Depth model for "+type+" slowness");
			System.out.println("      R     slowness      Z");
			if(TablesUtil.deBugOrder) {
				TablesUtil.deBugOffset = slowness.size()-1;
			}
		} else if(ver.equals("Final")) {
			System.out.println("\n     Final model for "+type+" slowness");
			System.out.println("      R     slowness      Z       length");
		} else {
			System.out.println("\n   Tau model for "+type+" slowness");
			System.out.println("      R     slowness    X");
		}
		if(type == 'P') {
			if(!ver.equals("Final")) {
				for(int j=0; j<pModel.size(); j++) {
					System.out.format("%3d %s\n", j, pModel.get(j));
				}
			} else {
				for(int j=0; j<pModel.size(); j++) {
					if(pInts.get(j) != null) {
						System.out.format("%3d %s %3d %s\n", j, pModel.get(j), 
								pInts.get(j).tau.length, pInts.get(j).name);
					} else {
						System.out.format("%3d %s null\n", j, pModel.get(j));
					}
				}
			}
		} else {
			if(!ver.equals("Final")) {
				for(int j=0; j<sModel.size(); j++) {
					System.out.format("%3d %s\n", j, sModel.get(j));
				}
			} else {
				for(int j=0; j<sModel.size(); j++) {
					if(sInts.get(j) != null) {
						System.out.format("%3d %s %3d %s\n", j, sModel.get(j), 
								sInts.get(j).tau.length, sInts.get(j).name);
					} else {
						System.out.format("%3d %s null\n", j, sModel.get(j));
					}
				}
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
		String shellString;
		
		System.out.println("\n\t"+type+" Model Shells:");
		if(TablesUtil.deBugOrder) {
			TablesUtil.deBugOffset = slowness.size()-1;
		}
		if(type == 'P') {
			for(int j=0; j<pShells.size(); j++) {
				shellString = pShells.get(j).printTau(type);
				if(shellString != null) System.out.format("%3d   %s\n", j, shellString);
			}
		} else {
			for(int j=0; j<sShells.size(); j++) {
				shellString = sShells.get(j).printTau(type);
				if(shellString != null) System.out.format("%3d   %s\n", j, shellString);
			}
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
		int n;
		
		if(type == 'P') {
			n = pModel.size()-1;
			return String.format("%3d %9.6f %8.6f", pInts.get(n).tau.length, 
					pModel.get(n).z, pModel.get(n).slow);
		} else {
			n = sModel.size()-1;
			return String.format("%3d %9.6f %8.6f", sInts.get(n).tau.length, 
					sModel.get(n).z, sModel.get(n).slow);
		}
	}
	
	/**
	 * Print a summary of all the tau-range integrals for the desired 
	 * phase type.
	 * 
	 * @param type Model type (P = P slowness, S = S slowness)
	 */
	public void printInt(char type) {
		int n;
		
		if(type == 'P') {
			n = pInts.size();
			for(int j=0; j<n-3; j++) {
				if(pInts.get(j) != null) {
					System.out.format("Lev1 %3d %3d %9.6f %8.6f\n", j, 
							pInts.get(j).tau.length, pModel.get(j).z, pModel.get(j).slow);
				}
			}
			for(int j=n-3; j<n-1; j++) {
				System.out.format("Lev2 %3d %3d %9.6f %8.6f\n", j, 
						pInts.get(j).tau.length, pModel.get(j).z, pModel.get(j).slow);
			}
			System.out.format("Lev3 %3d %3d %9.6f %8.6f\n", n-1, 
					pInts.get(n-1).tau.length, pModel.get(n-1).z, pModel.get(n-1).slow);
		} else {
			n = sInts.size();
			for(int j=0; j<n-3; j++) {
				if(sInts.get(j) != null) {
					System.out.format("Lev1 %3d %3d %9.6f %8.6f\n", j, 
							sInts.get(j).tau.length, sModel.get(j).z, sModel.get(j).slow);
				}
			}
			for(int j=n-3; j<n-1; j++) {
				System.out.format("Lev2 %3d %3d %9.6f %8.6f\n", j, 
						sInts.get(j).tau.length, sModel.get(j).z, sModel.get(j).slow);
			}
			System.out.format("Lev3 %3d %3d %9.6f %8.6f\n", n-1, 
					sInts.get(n-1).tau.length, sModel.get(n-1).z, sModel.get(n-1).slow);
		}
	}
}
