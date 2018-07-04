package gov.usgs.traveltime.tables;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

import gov.usgs.traveltime.ModConvert;
import gov.usgs.traveltime.TauUtil;
import gov.usgs.traveltime.TtStatus;

/**
 * Take care of everything related to one Earth model.
 * 
 * @author Ray Buland
 *
 */
public class EarthModel {
	String earthModel;									// Model name
	double rInnerCore = ShellName.INNER_CORE.defRadius();
	double rOuterCore = ShellName.OUTER_CORE.defRadius();
	double rUpperMantle = ShellName.LOWER_MANTLE.defRadius();
	double rMoho = ShellName.UPPER_MANTLE.defRadius();
	double rConrad = ShellName.LOWER_CRUST.defRadius();
	double rSurface = ShellName.UPPER_CRUST.defRadius();
	ModelSample innerCore;							// Model at the inner core boundary
	ModelSample outerCore;							// Model at the outer core boundary
	ModelSample upperMantle;						// Model at the upper mantle discontinuity
	ModelSample moho;										// Model at the Moho discontinuity
	ModelSample conrad;									// Model at the Conrad discontinuity
	ModelSample surface;								// Model at the free surface
	ArrayList<ModelSample> model;				// Model storage
	ArrayList<ModelShell> shells;				// Model shell parameters
	ArrayList<CritSlowness> critical;		// Critical slownesses
	EarthModel refModel;
	ModelInterp interp;
	ModConvert convert;
		
	/**
	 * It doesn't take much to get started.  In this version, we're going to 
	 * read in a reference Earth model from a file.
	 * 
	 * @param earthModel Name of the Earth model
	 * @param isCubic True if cubic spline interpolation is to be used
	 */
	public EarthModel(String earthModel, boolean isCubic) {
		this.earthModel = earthModel;
		model = new ArrayList<ModelSample>();
		shells = new ArrayList<ModelShell>();
		interp = new ModelInterp(model, shells, isCubic);
		// Set up the properties.
		if(TauUtil.modelPath == null) {
			TauUtil.getProperties();
		}
	}

	/**
	 * In this version, we already have a reference Earth model and we're 
	 * going to re-interpolate it.
	 * 
	 * @param refModel Reference Earth model information
	 * @param convert Model dependent constants
	 */
	public EarthModel(EarthModel refModel, ModConvert convert) {
		this.refModel = refModel;
		this.convert = convert;
		earthModel = refModel.earthModel;
		model = new ArrayList<ModelSample>();
		shells = new ArrayList<ModelShell>();
		critical = new ArrayList<CritSlowness>();
	}
	
	/**
	 * Read the Earth model file, set up shells, refine internal boundaries, 
	 * and initialize critical points.
	 * 
	 * @return Travel-time status
	 */
	public TtStatus readModel() {
		String modelFile, modelName;
		int n, i = 0, last = 0;
		/*
		 * We have to read everything in, but we don't need density, 
		 * anisotropy, or attenuation for the travel-times.
		 */
		@SuppressWarnings("unused")
		double r, rho, vpv, vph, vsv, vsh, eta, qMu, qKappa, rLast = 0d;
		BufferedInputStream inModel;
		Scanner scan;
		
		// Open and read the phase groups file.
		try {
			modelFile = "m"+earthModel+".mod";
			inModel = new BufferedInputStream(new FileInputStream(
					TauUtil.model(modelFile)));
		} catch (FileNotFoundException e) {
			return TtStatus.BAD_MODEL_READ;
		}
		scan = new Scanner(inModel);
		
		// Read the header.
		modelName = scan.next();
		if(!modelName.equals(earthModel)) {
			System.out.println("\n***** Error: model name mismatch (" +
					earthModel + " != " + modelName + ") *****\n");
			scan.close();
			return TtStatus.BAD_MODEL_FILE;
		}
		n = scan.nextInt();
		if(!scan.hasNextInt()) {
			rSurface = scan.nextDouble();
			rUpperMantle = scan.nextDouble();
			rMoho = scan.nextDouble();
			rConrad = scan.nextDouble();
		}
		
		// Read the model points.
		while(scan.hasNextInt()) {
			i = scan.nextInt();
			if(i != ++last) {
				System.out.format("\n***** Warning: sample %d found, %d " + 
						"expected *****\n\n", last, i);
				last = i;
			}
			r = scan.nextDouble();
			if(r < rLast) {
				System.out.format("\n***** Error: radius %7.2f out of order " + 
						"*****\n\n", r);
				scan.close();
				return TtStatus.BAD_MODEL_FILE;
			}
			rho = scan.nextDouble();
			vpv = scan.nextDouble();
			vph = scan.nextDouble();
			vsv = scan.nextDouble();
			vsh = scan.nextDouble();
			eta = scan.nextDouble();
			qMu = scan.nextDouble();
			qKappa = scan.nextDouble();
			// Trap discontinuities.
			if(r == rLast) {
				if(model.size() > 0) {
					shells.get(shells.size()-1).addEnd(model.size()-1, r);
					shells.add(new ModelShell(model.size()-1, model.size(), r));
				}
				shells.add(new ModelShell(model.size(), r));
			}
			model.add(new ModelSample(r, vpv, vph, vsv, vsh, eta));
			rLast = r;
		}
		// Done, finalize the outermost shell.
		shells.get(shells.size()-1).addEnd(model.size()-1, rLast);
		// Do some crude checks.
		if(i != n) {
			System.out.format("\n***** Warning: %d points found, %d " + 
					"expected *****\n\n", n, i);
		}
		if(rLast != rSurface) {
			System.out.format("\n***** Warning: radius of the model is not the " + 
					"same as the radius of the Earth (%7.2f != %7.2f)\n", rLast, 
					rSurface);
		}
		// OK. We're good (probably).
		scan.close();
		// Set the S velocity to the P velocity in the inner core.
		elimPKJKP();
		// Interpolate velocity.
		interp.interpVel();
		// Find important internal boundaries.
		refineBoundaries();
		// Initialize the model specific conversion constants.
		convert = new ModConvert(upperMantle.r, moho.r, conrad.r, surface.r, 
				surface.vs);
		// Do the Earth flattening transformation.
		flatten();
		return TtStatus.SUCCESS;
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
	 * Interpolate to find V(r).
	 * 
	 * @param type Wave type (P = compressional, S = shear)
	 * @param r Radius in kilometers
	 * @return Velocity in kilometers/second at radius r
	 */
	public double getVel(char type, double r) {
		if(type == 'P') {
			return interp.getVp(r);
		} else {
			return interp.getVs(r);
		}
	}
	
	/**
	 * Interpolate to find V(r) in a particular shell.
	 * 
	 * @param type Wave type (P = compressional, S = shear)
	 * @param shell Shell number
	 * @param r Radius in kilometers
	 * @return Compressional velocity in kilometers/second at radius r
	 */
	public double getVel(char type, int shell, double r) {
		if(type == 'P') {
			return interp.getVp(shell, r);
		} else {
			return interp.getVs(shell, r);
		}
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
	 * Get the size of the model.
	 * 
	 * @return The number of samples in the Earth model.
	 */
	public int size() {
		return model.size();
	}
	
	/**
	 * Getter for ModConvert.
	 * 
	 * @return ModConvert
	 */
	public ModConvert getConvert() {
		return convert;
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
	 * Eliminate the poorly observed PKJKP phase in the inner core by 
	 * replacing the S velocity with the P velocity.
	 */
	private void elimPKJKP() {
		ModelShell shell;
		
		shell = shells.get(0);
		for(int j=shell.iBot; j<=shell.iTop; j++) {
			model.get(j).elimPKJKP();
		}
	}
	
	/**
	 * Match the key radii provided with model discontinuities.  This is 
	 * necessary to figure out the phase codes.
	 * 
	 * @param rUpperMantle Preliminary radius of the upper mantle 
	 * discontinuity in kilometers
	 * @param rMoho Preliminary radius of the Moho discontinuity in kilometers
	 * @param rConrad Preliminary radius of the Conrad discontinuity in 
	 * kilometers
	 * @param rSurface Preliminary radius of the free surface in kilometers
	 */
	private void refineBoundaries() {
		double tempIC = TauUtil.DMAX, tempOC = TauUtil.DMAX, tempUM = TauUtil.DMAX, 
				tempM = TauUtil.DMAX, tempC = TauUtil.DMAX, tempFS = TauUtil.DMAX;
		double rDisc;
		int iTop;
		ModelShell shell;
		
		// Find the closest boundary to target boundaries.
		for(int j=0; j<shells.size(); j++) {
			rDisc = shells.get(j).rTop;
			iTop = shells.get(j).iTop;
			if(Math.abs(rDisc-rInnerCore) < Math.abs(tempIC-rInnerCore)) {
				tempIC = rDisc;
				innerCore = model.get(iTop);
			}
			if(Math.abs(rDisc-rOuterCore) < Math.abs(tempOC-rOuterCore)) {
				tempOC = rDisc;
				outerCore = model.get(iTop);
			}
			if(Math.abs(rDisc-rUpperMantle) < Math.abs(tempUM-rUpperMantle)) {
				tempUM = rDisc;
				upperMantle = model.get(iTop);
			}
			if(Math.abs(rDisc-rMoho) < Math.abs(tempM-rMoho)) {
				tempM = rDisc;
				moho = model.get(iTop);
			}
			if(Math.abs(rDisc-rConrad) < Math.abs(tempC-rConrad)) {
				tempC = rDisc;
				conrad = model.get(iTop);
			}
			if(Math.abs(rDisc-rSurface) < Math.abs(tempFS-rSurface)) {
				tempFS = rDisc;
				surface = model.get(iTop);
			}
		}
		
		// Set the radii.
		rInnerCore = innerCore.r;
		rOuterCore = outerCore.r;
		rUpperMantle = upperMantle.r;
		rMoho = moho.r;
		rConrad = conrad.r;
		rSurface = surface.r;
		
		// Go around again setting up the shells.
		for(int j=0; j<shells.size(); j++) {
			shell = shells.get(j);
			if(!shell.isDisc) {
				rDisc = shell.rTop;
				if(rDisc <= rInnerCore) {
					shell.addName(ShellName.INNER_CORE, Double.NaN, TablesUtil.DELX[0]);
				} else if(rDisc <= rOuterCore) {
					shell.addName(ShellName.OUTER_CORE, Double.NaN, TablesUtil.DELX[1]);
				} else if(rDisc <= rUpperMantle) {
					shell.addName(ShellName.LOWER_MANTLE, Double.NaN, TablesUtil.DELX[2]);
				} else if(rDisc <= rMoho) {
					shell.addName(ShellName.UPPER_MANTLE, Double.NaN, TablesUtil.DELX[3]);
				} else if(rDisc <= rConrad) {
					shell.addName(ShellName.LOWER_CRUST, Double.NaN, TablesUtil.DELX[4]);
				} else {
					shell.addName(ShellName.UPPER_CRUST, Double.NaN, TablesUtil.DELX[5]);
				}
			}
		}
		
		// Go around yet again setting up the discontinuities.
		for(int j=0; j<shells.size(); j++) {
			shell = shells.get(j);
			if(shell.isDisc) {
				rDisc = shell.rTop;
				if(rDisc == rInnerCore) {
					shell.addName(ShellName.INNER_CORE_BOUNDARY, Double.NaN, 
							TablesUtil.DELX[1]);
				} else if(rDisc == rOuterCore) {
					shell.addName(ShellName.CORE_MANTLE_BOUNDARY, Double.NaN, 
							TablesUtil.DELX[2]);
				} else if(rDisc == rMoho) {
					shell.addName(ShellName.MOHO_DISCONTINUITY, Double.NaN, 
							TablesUtil.DELX[4]);
				} else if(rDisc == rConrad) {
					shell.addName(ShellName.CONRAD_DISCONTINUITY, rSurface-rDisc, 
							TablesUtil.DELX[5]);
				} else {
					if(rDisc < rUpperMantle) {
						shell.addName(null, rSurface-rDisc, TablesUtil.DELX[2]);
					} else {
						shell.addName(null, rSurface-rDisc, TablesUtil.DELX[3]);
					}
				}
			}
		}
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
	 */
	public void printModel() {
		System.out.format("\n%s %d %7.2f %7.2f %7.2f %7.2f %7.2f %7.2f\n", 
				earthModel, model.size(), innerCore.r, outerCore.r, upperMantle.r, 
				moho.r, conrad.r, surface.r);
		for(int j=0; j<model.size(); j++) {
			System.out.format("\t%3d: %s\n", j, 
					model.get(j).printSample(false, null));
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
				System.out.println("\t        R         Z    slowP    slowS");
			} else {
				System.out.format("\n%s %d %7.4f %7.4f %7.4f %7.4f %7.4f %7.4f\n", 
						refModel.earthModel, model.size(), refModel.surface.z, 
						refModel.conrad.z, refModel.moho.z, refModel.upperMantle.z, 
						refModel.outerCore.z, refModel.innerCore.z);
				System.out.println("\t        R         Z    slowP    slowS");
			}
			int n = model.size()-1;
			for(int j=n; j>=0; j--) {
				if(nice) {
					if(TablesUtil.deBugOrder) {
						System.out.format("\t%3d: %s\n", j, model.get(j).printSample(true, 
								convert));
					} else {
						System.out.format("\t%3d: %s\n", n-j, model.get(j).printSample(true, 
								convert));
					}
				} else {
					if(TablesUtil.deBugOrder) {
						System.out.format("\t%3d: %s\n", j, model.get(j).printSample(true, 
								null));
					} else {
						System.out.format("\t%3d: %s\n", n-j, model.get(j).printSample(true, 
								null));
					}
				}
			}
		} else {
			System.out.format("\n%s %d %7.2f %7.2f %7.2f %7.2f %7.2f %7.2f\n", 
					refModel.earthModel, model.size(), refModel.innerCore.r, 
					refModel.outerCore.r, refModel.upperMantle.r, refModel.moho.r, 
					refModel.conrad.r, refModel.surface.r);
			System.out.println("     R     Vp     Vs");
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
		System.out.println("\n\t\t  Critical points:");
		int n = critical.size()-1;
		for(int j=n; j>=0; j--) {
			System.out.format("\t  %3d %s\n", n-j, critical.get(j));
		}
	}
}
