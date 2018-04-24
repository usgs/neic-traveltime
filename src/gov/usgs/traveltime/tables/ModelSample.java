package gov.usgs.traveltime.tables;

import gov.usgs.traveltime.ModConvert;

/**
 * Deal with all model parameters at one radius.
 * 
 * @author Ray Buland
 *
 */
public class ModelSample {
	/**
	 * The "standard" Earth model representation that emerged after PREM (1981) and 
	 * before AK135 (1997) includes density, anisotropy, and attenuation, none of 
	 * which are needed for the tau-p travel times.  Just to be on the safe side, 
	 * it makes sense to ensure that we make an isotropic model out of a potentially 
	 * anisotropic model.  In this case, vertical polarization should be understood 
	 * as radial and horizontal as tangential in the idealized spherically symmetric 
	 * Earth model.  The spherical approximation works for seismology because the 
	 * oblateness is small enough (~1/300) to be considered a perturbation.
	 * 
	 */
	boolean isDisc;	// True if this point is at a velocity discontinuity
	double r;				// Radius in kilometers
	double vp;			// Isotropic velocity in kilometers/second
	double vs;			// Isotropic velocity in kilometers/second
	double z;				// Non-dimensional Earth flattened depth
	double slowP;		// Non-dimensional compressional wave slowness
	double slowS;		// Non-dimensional shear wave slowness
	
	/**
	 * Create a (possibly) anisotropic model sample and it's isotropic equivalent.
	 * 
	 * @param r	Radius in kilometers
	 * @param Vpv Vertically polarized P velocity in kilometers/second
	 * @param Vph Horizontally polarized P velocity in kilometers/second
	 * @param Vsv Vertically polarized S velocity in kilometers/second
	 * @param Vsh Horizontally polarized S velocity in kilometers/second
	 * @param isDisc True if this sample is at a discontinuity
	 */
	public ModelSample(double r, double vpv, double vph, double vsv, double vsh, 
			double eta, boolean isDisc) {
		
		this.r = r;
		
		// Create the isotropic version.
		if(eta != 1d || vpv != vph || vsv != vsh) {
			vs = Math.sqrt(0.0666666666666667d*((1d-2d*eta)*Math.pow(vph, 2d) + 
					Math.pow(vpv, 2d) + 5d*Math.pow(vsh, 2d) + (6d+4d*eta)*
					Math.pow(vsv, 2d)));
			vp = Math.sqrt(0.0666666666666667d*((8d+4d*eta)*Math.pow(vph, 2d) + 
					3d*Math.pow(vpv, 2d) + (8d-8d*eta)*Math.pow(vsv, 2d)));
		} else {
			vp = vpv;
			vs = vsv;
		}
		// Mask fluid areas.
		if(vs == 0d) vs = vp;
	}
	
	/**
	 * Set the discontinuity flag.
	 */
	public void setDisc() {
		isDisc = true;
	}
	
	/**
	 * Non-dimensionalize and apply the Earth flattening transformation.
	 * 
	 * @param convert Model sensitive conversion constants
	 */
	public void flatten(ModConvert convert) {
		z = Math.log(r*convert.xNorm);
		slowP = r*convert.tNorm/vp;
		slowS = r*convert.tNorm/vs;
	}
	
	/**
	 * Print the model sample.
	 * 
	 * @param j Sample index
	 * @param flat If true print the Earth flattened parameters
	 */
	public void printSample(int j, boolean flat) {
		
		if(flat) {
			System.out.format("\t%3d: %9.4f %7.4f %7.4f\n", j, z, slowP, slowS);
		} else {
			System.out.format("\t%3d: %9.2f %7.4f %7.4f\n", j, r, vp, vs);
		}
	}
}
