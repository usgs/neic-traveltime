package gov.usgs.traveltime.tables;

/**
 * Providing names for Earth model shells is a convenience, 
 * but turns out to be very handy for understanding the 
 * voluminous output.
 * 
 * @author Ray Buland
 *
 */
public enum ShellName {
	// Shell names:
	/**
	 * Default radius of the inner core in kilometers.
	 */
	INNER_CORE (1217d),
	/**
	 * Default radius of the outer core in kilometers.
	 */
	OUTER_CORE (3482d),
	/**
	 * Default radius of the lower mantle in kilometers (i.e., to 
	 * the 410 kilometer discontinuity).
	 */
	LOWER_MANTLE (5961d),
	/**
	 * Default radius of the upper mantle in kilometers (i.e., to 
	 * the Moho discontinuity).
	 */
	UPPER_MANTLE (6336d),
	/**
	 * Default radius of the lower crust in kilometers (i.e., to 
	 * the Conrad discontinuity).
	 */
	LOWER_CRUST (6351d),
	/**
	 * Default radius of the upper crust in kilometers (i.e., to 
	 * the free surface).  This is, of course, the mean radius of the 
	 * Earth.
	 */
	UPPER_CRUST (6371d),
	
	// Discontinuity names:
	/**
	 * Inner core-outer core boundary radius in kilometers.
	 */
	INNER_CORE_BOUNDARY (1217d),
	/**
	 * Core-mantle boundary radius in kilometers.
	 */
	CORE_MANTLE_BOUNDARY (3482d),
	/**
	 * Moho radius in kilometers.
	 */
	MOHO_DISCONTINUITY (6336d),
	/**
	 * Conrad radius in kilometers.
	 */
	CONRAD_DISCONTINUITY (6351d);
	
	private final double defRadius;
	
	/**
	 * The constructor just sets up the default radius.
	 * 
	 * @param defRadius Default radius of the top discontinuity of 
	 * the shell in kilometers
	 */
	ShellName(double defRadius) {
		this.defRadius = defRadius;
	}
	
	/**
	 * Get the default radius.
	 * 
	 * @return The default radius of the top discontinuity of the 
	 * shell in kilometers
	 */
	public double defRadius() {
		return defRadius;
	}
}
