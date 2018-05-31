package gov.usgs.traveltime;

/**
 * Earth model dependent unit conversions and constants.
 * 
 * @author Ray Buland
 *
 */
public class ModConvert {
	/**
	 * Normalization for distance (delta and depth).
	 */
	public final double xNorm;		// Internal normalization constant for distance.
	/**
	 * Normalization for slowness and ray parameter.
	 */
	public final double pNorm;		// Internal normalization constant for slowness.
	/**
	 * Normalization for travel-time and tau.
	 */
	public final double tNorm;		// Internal normalization constant for time.
	final double dTdDelta;				// Convert dT/dDelta to dimensional units.
	final double deg2km;					// Convert degrees to kilometers.
	final double zUpperMantle;		// Depth of the upper mantle in kilometers.
	final double zMoho;						// Depth of the Moho in kilometers.
	final double zConrad;					// Depth of the Conrad discontinuity in kilometers.
	final double rSurface;				// Radius of the free surface of the Earth in kilometers.
	final double zNewUp;					// Up-going branch replacement depth.
	final double dTdDLg;					// dT/dDelta for Lg in seconds/degree.
	final double dTdDLR;					// dT/dDelta for LR in seconds/degree.
	
	/**
	 * Set constants from the Fortran generated *.hed file.
	 * 
	 * @param in Data from the *.hed and *.tbl files
	 */
	public ModConvert(ReadTau in) {
		// Set up the normalization.
		xNorm = in.xNorm;
		pNorm = in.pNorm;
		tNorm = in.tNorm;
		// Compute some useful constants.
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
		pNorm = vsSurface;
		// Compute some useful constants.
		tNorm = xNorm*pNorm;
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
		return realR(z)/(tNorm*p);
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
		return tNorm*r/v;
	}
}
