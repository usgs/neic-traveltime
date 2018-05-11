package gov.usgs.traveltime.tables;

/**
 * Utilities related to travel-time table generation.
 * 
 * @author Ray Buland
 *
 */
public class TablesUtil {
	/**
	 * Default inner core radius in kilometers.
	 */
	public static double DEFINNERCORE = 1217d;
	/**
	 * Default outer core radius in kilometers.
	 */
	public static double DEFOUTERCORE = 3482d;
	/**
	 * Default upper mantle radius in kilometers.  This corresponds to 
	 * the 410 kilometer depth discontinuity.
	 */
	public static double DEFUPPERMANTLE = 5961d;
	/**
	 * Default Moho radius in kilometers.  This corresponds to a 
	 * crustal thickness of 35 kilometers.
	 */
	public static double DEFMOHO = 6336d;
	/**
	 * Default Conrad radius in kilometers.  This corresponds to an 
	 * upper crust thickness of 20 kilometers.
	 */
	public static double DEFCONRAD = 6351d;
	/**
	 * Default radius of the Earth in kilometers.  This doesn't 
	 * necessarily match what's in the textbooks, but it's the right 
	 * value for a spherically averaged Earth.
	 */
	public static double DEFRADIUS = 6371d;
	/**
	 * Increment in radius to sample the reference Earth model.
	 */
	public static double RESAMPLE = 50d;
	/**
	 * Maximum non-dimensional increment between successive slowness 
	 * samples.
	 */
	public static double DELPMAX = 0.01d;
	/**
	 * Maximum increment between successive radius samples in 
	 * kilometers.
	 */
	public static double DELRMAX = 75d;
	/**
	 * Non-dimensional tolerance for sampling range (ray travel distance).
	 */
	public static double XTOL = 5e-6d;
	/**
	 * Maximum iterations for root finding algorithms (e.g., for 
	 * finding caustics).
	 */
	public static int MAXEVAL = 30;
	/**
	 * Relative velocity tolerance.  If velocity is within this tolerance 
	 * across an apparent Earth model discontinuity, make the velocity 
	 * continuous.
	 */
	public static double VELOCITYTOL = 2e-5d;
	/**
	 * Target increment between successive ray travel distances in 
	 * kilometers.  Different targets are supported for different major 
	 * shells in the order: inner core, outer core, lower mantle, upper 
	 * mantle, lower crust, and upper crust.  The finer sampling at 
	 * shallower depths is necessary to stabilize the results of  
	 * complex regional models.
	 */
	public static double[] DELX = {300d, 300d, 150d, 150d, 100d, 100d};
}
