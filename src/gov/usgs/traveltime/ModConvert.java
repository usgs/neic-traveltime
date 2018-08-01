package gov.usgs.traveltime;

/**
 * Earth model dependent unit conversions and constants.
 * 
 * @author Ray Buland
 *
 */
public class ModConvert {
	/**
	 * Normalization for distance (delta and depth).  Note that 
	 * this is Xn in the Fortran programs.  You need to multiply 
	 * the distance by Xn to normalize it.
	 */
	public final double xNorm;
	/**
	 * Normalization for velocity.  Called Pn in the Fortran code, 
	 * this constant was intended as the normalization for slowness.  
	 * Dividing velocity by Pn normalizes it.
	 */
	public final double vNorm;
	/**
	 * Normalization for travel-time and tau.  This is Tn in the 
	 * Fortran code to compute travel times, but 1/Tn in the code 
	 * to generate the tables (yuck!).
	 */
	public final double tNorm;
	/**
	 * Normalization for slowness and ray parameter.  This is a new 
	 * constant to resolve the dichotomy in Tn.
	 */
	public final double pNorm;
	/**
	 * Convert dT/dDelta to dimensional units.
	 */
	public final double dTdDelta;
	/**
	 * Convert degrees to kilometers.
	 */
	public final double deg2km;
	/**
	 * Depth of the upper mantle in kilometers.
	 */
	public final double zUpperMantle;
	/**
	 * Depth of the Moho in kilometers.
	 */
	public final double zMoho;
	/**
	 * Depth of the Conrad discontinuity in kilometers.
	 */
	public final double zConrad;
	/**
	 * Radius of the free surface of the Earth in kilometers.
	 */
	public final double rSurface;
	/**
	 * Up-going branch replacement depth.
	 */
	public final double zNewUp;
	/**
	 * Typical dT/dDelta for Lg in seconds/degree.
	 */
	public final double dTdDLg;
	/**
	 * Typical dT/dDelta for LR in seconds/degree.
	 */
	public final double dTdDLR;
	
	/**
	 * Set constants from the Fortran generated *.hed file.
	 * 
	 * @param in Data from the *.hed and *.tbl files
	 */
	public ModConvert(ReadTau in) {
		// Set up the normalization.
		xNorm = in.xNorm;
		vNorm = in.pNorm;
		tNorm = in.tNorm;
		// Compute some useful constants.
		pNorm = 1d/tNorm;
		dTdDelta = Math.toRadians(tNorm);
		rSurface = in.rSurface;
		deg2km = Math.PI*rSurface/180d;
		dTdDLg = deg2km/TauUtil.LGGRPVEL;
		dTdDLR = deg2km/TauUtil.LRGRPVEL;
		// Compute some useful depths.
		zUpperMantle = rSurface-in.rUpperMantle;
		zMoho = rSurface-in.rMoho;
		zConrad = rSurface-in.rConrad;
		zNewUp = zMoho;
	}
	
	/**
	 * Set constants from the Java generated model data.
	 * 
	 * @param rUpperMantle Radius of the upper mantle discontinuity in 
	 * kilometers
	 * @param rMoho Radius of the Moho discontinuity in kilometers
	 * @param rConrad Radius of the Conrad discontinuity in kilometers
	 * @param rSurface Radius of the Earth in kilometers
	 * @param vsSurface Shear velocity in kilometers/second at the 
	 * surface of the Earth
	 */
	public ModConvert(double rUpperMantle, double rMoho, double rConrad, 
			double rSurface, double vsSurface) {
		xNorm = 1d/rSurface;
		vNorm = vsSurface;
		// Compute some useful constants.
		pNorm = xNorm*vNorm;
		tNorm = 1d/pNorm;
		dTdDelta = Math.toRadians(tNorm);
		this.rSurface = rSurface;
		deg2km = Math.PI*rSurface/180d;
		dTdDLg = deg2km/TauUtil.LGGRPVEL;
		dTdDLR = deg2km/TauUtil.LRGRPVEL;
		// Compute some useful depths.
		zUpperMantle = rSurface-rUpperMantle;
		zMoho = rSurface-rMoho;
		zConrad = rSurface-rConrad;
		zNewUp = zMoho;
	}
	
	/**
	 * Given a normalized, Earth flattened depth, return the 
	 * dimensional radius.
	 * 
	 * @param z Normalized, Earth flattened depth
	 * @return Radius in kilometers
	 */
	public double realR(double z) {
		return Math.exp(z)/xNorm;
	}
	
	/**
	 * Given a normalized, Earth flattened depth, return the 
	 * dimensional depth.
	 * 
	 * @param z Normalized, Earth flattened depth
	 * @return Depth in kilometers
	 */
	public double realZ(double z) {
		return (1d-Math.exp(z))/xNorm;
	}

	/**
	 * Given the normalized slowness and depth, return the 
	 * dimensional velocity at that depth.
	 * 
	 * @param p Normalized slowness
	 * @param z Normalized, Earth flattened depth
	 * @return Velocity at that depth in kilometers/second
	 */
	public double realV(double p, double z) {
		return pNorm*realR(z)/p;
	}
	
	/**
	 * Normalize radius (or depth) into units of the radius of the 
	 * Earth
	 * 
	 * @param r Radius or depth in kilometers
	 * @return Non-dimensional radius or depth
	 */
	public double normR(double r) {
		return xNorm*r;
	}	
	
	/**
	 * Convert non-dimensional radius (or depth) into kilometers.
	 * 
	 * @param r Non-dimensional radius or depth
	 * @return Dimensional radius or depth in kilometers
	 */
	public double dimR(double r) {
		return r/xNorm;
	}
	
	/**
	 * Given a dimensional radius, return the normalized, Earth 
	 * flattened depth.
	 * 
	 * @param r Radius in kilometers
	 * @return Normalized, Earth flattened depth
	 */
	public double flatZ(double r) {
		return Math.log(xNorm*r);
	}
	
	/**
	 * Given the normalized velocity and radius, return the normalized 
	 * slowness.
	 * 
	 * @param v Velocity at radius r in kilometers/second
	 * @param r Radius in kilometers
	 * @return Normalized slowness
	 */
	public double flatP(double v, double r) {
		return pNorm*r/v;
	}
}
