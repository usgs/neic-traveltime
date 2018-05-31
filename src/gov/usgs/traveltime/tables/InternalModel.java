package gov.usgs.traveltime.tables;

import java.util.ArrayList;

import gov.usgs.traveltime.ModConvert;

/**
 * Rather than using the default model sampling in radius, 
 * it seems useful to be able to control the sampling.  This 
 * can make a (hopefully, small) difference in the result 
 * due to the implied interpolation used to make the integration 
 * closed form.  Of course, having a way of sampling the model 
 * explicitly is essential if a polynomial model such as PREM 
 * is used.
 * 
 * @author Ray Buland
 *
 */
public class InternalModel {
	ArrayList<ModelSample> model;				// Model storage
	ArrayList<ModelShell> shells;				// Model shell parameters
	ArrayList<CritSlowness> critical;		// Critical slownesses
	EarthModel refModel;
	ModConvert convert;

	/**
	 * We need the reference Earth model to get started.
	 * 
	 * @param refModel Reference Earth model information
	 * @param convert Model dependent constants
	 */
	public InternalModel(EarthModel refModel, ModConvert convert) {
		this.refModel = refModel;
		this.convert = convert;
		model = new ArrayList<ModelSample>();
		shells = new ArrayList<ModelShell>();
		critical = new ArrayList<CritSlowness>();
	}
	
	/**
	 * Interpolate the reference Earth model at a standard sampling.
	 */
	public void interpolate() {
		int nSamp;
		double dr, r0, r1, r;
		ModelShell refShell, newShell;
		
		// Loop over the reference Earth model shells.
		r1 = 0d;
		for(int i=0; i<refModel.shells.size(); i++) {
			refShell = refModel.shells.get(i);
			// Initialize the interpolated model shell.
			if(!refShell.isDisc) {
				newShell = new ModelShell(refShell, model.size());
				model.add(new ModelSample(refModel.model.get(refShell.iBot)));
				bridgeVel(model.size()-1);
				// Figure how many samples we'll need.
				r0 = r1;
				r1 = refShell.rTop;
				nSamp = (int)((r1-r0)/TablesUtil.RESAMPLE-0.5d);
				dr = (r1-r0)/(nSamp+1);
				// Fill in the interpolated model.
				for(int j=1; j<=nSamp; j++) {
					r = r0+j*dr;
					model.add(new ModelSample(r, refModel.getVel('P', i, r), 
							refModel.getVel('S', i, r)));
				}
				// Add the top of the shell.
				newShell.addEnd(model.size(), r1);
				model.add(new ModelSample(refModel.model.get(refShell.iTop)));
			} else {
				newShell = new ModelShell(refShell, model.size()-1);
				newShell.addEnd(model.size(), r1);
			}
			shells.add(newShell);
		}
		// Apply the Earth flattening transformation.
		flatten();
		// Find critical slownesses (ends of travel-time branches).
		findCritical();
	}
	
	/**
	 * Get the size of the model.
	 * 
	 * @return The number of samples in the Earth model.
	 */
	public int size() {
		return model.size();
	}
	
	/**
	 * Get the jth model depth.
	 * 
	 * @param j Sample index
	 * @return Non-dimensional Earth flattened depth
	 */
	public double getZ(int j) {
		return model.get(j).z;
	}
	
	/**
	 * Get the jth model radius.
	 * 
	 * @param j Sample index
	 * @return Dimensional Earth radius in kilometers
	 */
	public double getR(int j) {
		return model.get(j).r;
	}
	
	/**
	 * Get the jth model slowness.
	 * 
	 * @param type Wave type (P = compressional, S = shear)
	 * @param j Sample index
	 * @return Non-dimensional P-wave slowness
	 */
	public double getSlow(char type, int j) {
		if(type == 'P') {
			return model.get(j).slowP;
		} else {
			return model.get(j).slowS;
		}
	}
	
	/**
	 * Getter for the list of critical slownesses compiled by the 
	 * reference Earth model processing.
	 * 
	 * @return List of critical slownesses
	 */
	public ArrayList<CritSlowness> getCritical() {
		return critical;
	}
	
	/**
	 * If velocity is nearly continuous across shell boundaries, 
	 * assume that it should be exactly continuous and make it so.
	 * 
	 * @param index Index of the model sample at the bottom of the 
	 * new shell
	 */
	private void bridgeVel(int index) {
		if(index > 0) {
			if(Math.abs(model.get(index).vp-model.get(index-1).vp) <= 
					TablesUtil.VELOCITYTOL*model.get(index).vp) {
				model.get(index).vp = 0.5d*(model.get(index).vp+
						model.get(index-1).vp);
				model.get(index-1).vp = model.get(index).vp;
			}
			if(Math.abs(model.get(index).vs-model.get(index-1).vs) <= 
					TablesUtil.VELOCITYTOL*model.get(index).vs) {
				model.get(index).vs = 0.5d*(model.get(index).vs+
						model.get(index-1).vs);
				model.get(index-1).vs = model.get(index).vs;
			}
		}
	}
	
	/**
	 * Apply the Earth flattening transformation to the model and make all 
	 * flattened parameters non-dimensional at the same time.
	 * 
	 * @param convert Model sensitive conversion constants
	 */
	private void flatten() {
		for(int j=0; j<model.size(); j++) {
			model.get(j).flatten(convert);
		}
	}
	
	/**
	 * Collecting the critical points.  A critical point is a slowness that 
	 * must be sampled exactly because it will be the end of a branch for a 
	 * surface focus event.
	 */
	private void findCritical() {
		boolean inLVZ;
		ModelSample sample, lastSample;
		ModelShell shell = null;
		
		// The slownesses above and below each discontinuity in the model 
		// will be a branch end point.
		for(int j=0; j<shells.size(); j++) {
			shell = shells.get(j);
			critical.add(new CritSlowness('P', j, (shell.isDisc ? 
					ShellLoc.BOUNDARY:ShellLoc.SHELL), model.get(shell.iBot).slowP));
			if(model.get(shell.iBot).slowP != model.get(shell.iBot).slowS) {
				critical.add(new CritSlowness('S', j, (shell.isDisc ? 
					ShellLoc.BOUNDARY:ShellLoc.SHELL), model.get(shell.iBot).slowS));
			}
		}
		critical.add(new CritSlowness('P', shells.size()-1, ShellLoc.SHELL, 
				model.get(shell.iTop).slowP));
		critical.add(new CritSlowness('S', shells.size()-1, ShellLoc.SHELL, 
				model.get(shell.iTop).slowS));
		
		/*
		 * Now look for high slowness zones.  Note that this is not quite the 
		 * same as low velocity zones because of the definition of slowness in 
		 * the spherical earth.  First do the P-wave slowness.
		 */
		inLVZ = false;
		for(int i=0; i<shells.size(); i++) {
			shell = shells.get(i);
			sample = model.get(shell.iBot);
			for(int j=shell.iBot+1; j<shell.iTop; j++) {
				lastSample = sample;
				sample = model.get(j);
				if(!inLVZ) {
					if(sample.slowP <= lastSample.slowP) {
						inLVZ = true;
						critical.add(new CritSlowness('P', i, ShellLoc.SHELL, 
								lastSample.slowP));
					}
				} else {
					if(sample.slowP >= lastSample.slowP) {
						inLVZ = false;
						critical.add(new CritSlowness('P', i, ShellLoc.SHELL, 
								lastSample.slowP));
					}
				}
			}
		}
		// Now do the S-wave slowness.
		inLVZ = false;
		for(int i=0; i<shells.size(); i++) {
			shell = shells.get(i);
			sample = model.get(shell.iBot);
			for(int j=shell.iBot+1; j<shell.iTop; j++) {
				lastSample = sample;
				sample = model.get(j);
				if(!inLVZ) {
					if(sample.slowS <= lastSample.slowS) {
						inLVZ = true;
						critical.add(new CritSlowness('S', i, ShellLoc.SHELL, 
								lastSample.slowS));
					}
				} else {
					if(sample.slowS >= lastSample.slowS) {
						inLVZ = false;
						critical.add(new CritSlowness('S', i, ShellLoc.SHELL, 
								lastSample.slowS));
					}
				}
			}
		}
		
		// Add the missing shells.
		fixShells();
		// Sort the critical slownesses into order.
		critical.sort(null);
		// And remove duplicates.
		for(int j=1; j<critical.size(); j++) {
			if(critical.get(j).isSame(critical.get(j-1))) {
				critical.remove(j);
				j--;
			}
		}
	}
	
	/**
	 * Add the shell indices for the model velocities for which the 
	 * critical slownesses aren't actually critical.  For example, if a 
	 * critical slowness corresponds to the end of a P-wave branch, we 
	 * will need to add the S-wave shell that the slowness falls into.  
	 * At the same time fix any out of order critical points (typically 
	 * due to low velocity zones).
	 */
	private void fixShells() {
		ModelShell shell;
		CritSlowness crit;
		
		for(int i=0; i<critical.size(); i++) {
			crit = critical.get(i);
			// Add or check/fix P slowness shells.
			for(int j=shells.size()-1; j>=0; j--) {
				shell = shells.get(j);
				if(crit.slowness >= model.get(shell.iBot).slowP) {
					crit.addShell('P', j);
					break;
				}
			}
			// Add or check/fix S slowness shells.
			for(int j=shells.size()-1; j>=0; j--) {
				shell = shells.get(j);
				if(crit.slowness >= model.get(shell.iBot).slowS) {
					crit.addShell('S', j);
					break;
				}
			}
		}
	}
	
	/**
	 * Print the model.
	 * 
	 * @param flat If true print the Earth flattened parameters
	 * @param nice If true print dimensional depths
	 */
	public void printModel(boolean flat, boolean nice) {
		
		if(flat) {
			if(nice) {
				System.out.format("\n%s %d %7.2f %7.2f %7.2f %7.2f %7.2f %7.2f\n", 
						refModel.earthModel, model.size(), 
						convert.realZ(refModel.surface.z), 
						convert.realZ(refModel.conrad.z), 
						convert.realZ(refModel.moho.z), 
						convert.realZ(refModel.upperMantle.z), 
						convert.realZ(refModel.outerCore.z), 
						convert.realZ(refModel.innerCore.z));
			} else {
				System.out.format("\n%s %d %7.4f %7.4f %7.4f %7.4f %7.4f %7.4f\n", 
						refModel.earthModel, model.size(), refModel.surface.z, 
						refModel.conrad.z, refModel.moho.z, refModel.upperMantle.z, 
						refModel.outerCore.z, refModel.innerCore.z);
			}
			int n = model.size()-1;
			for(int j=n; j>=0; j--) {
				if(nice) {
					System.out.format("\t%3d: %s\n", n-j, model.get(j).printSample(true, 
							convert));
				} else {
					System.out.format("\t%3d: %s\n", n-j, model.get(j).printSample(true, 
							null));
				}
			}
		} else {
			System.out.format("\n%s %d %7.2f %7.2f %7.2f %7.2f %7.2f %7.2f\n", 
					refModel.earthModel, model.size(), refModel.innerCore.r, 
					refModel.outerCore.r, refModel.upperMantle.r, refModel.moho.r, 
					refModel.conrad.r, refModel.surface.r);
			for(int j=0; j<model.size(); j++) {
				System.out.format("\t%3d: %s\n", j, model.get(j).printSample(false, null));
			}
		}
	}
	
	/**
	 * Print the shell limits.
	 */
	public void printShells() {
		System.out.println("\n\t\tShells:");
		for(int j=0; j<shells.size(); j++) {
			System.out.format("%3d   %s\n", j, shells.get(j).toString());
		}
	}
	
	/**
	 * Print the (potentially) critical points.
	 * 
	 */
	public void printCritical() {
		System.out.println("\n\tCritical points:");
		int n = critical.size()-1;
		for(int j=n; j>=0; j--) {
			System.out.format("\t  %3d %s\n", n-j, critical.get(j));
		}
	}
}
