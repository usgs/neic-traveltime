package gov.usgs.traveltime.tables;

/**
 * Utilities related to travel-time table generation.
 * 
 * @author Ray Buland
 *
 */
public class TablesUtil {
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
	 * Target increment between successive ray travel distances in 
	 * kilometers.  Different targets are supported for different major 
	 * shells in the order: inner core, outer core, lower mantle, upper 
	 * mantle, lower crust, and upper crust.  The finer sampling at 
	 * shallower depths is necessary to stabilize the results of  
	 * complex regional models.
	 */
	public static double[] DELX = {300d, 300d, 150d, 150d, 100d, 100d};
	/**
	 * Maximum iterations for root finding algorithms (e.g., for 
	 * finding caustics).
	 */
	public static int MAXEVAL = 30;
	/**
	 * Non-dimensional tolerance for sampling range (ray travel distance).
	 */
	public static double XTOL = 5e-6d;
	/**
	 * Relative velocity tolerance.  If velocity is within this tolerance 
	 * across an apparent Earth model discontinuity, make the velocity 
	 * continuous.
	 */
	public static double VELOCITYTOL = 2e-5d;
	/**
	 * Non-dimensional back off when dXdP is infinite (at the top of shells).
	 */
	public static double SLOWOFF = 1e-6d;
	/**
	 * The higher the debug level, the more output you get.
	 */
	public static int deBugLevel = 0;
}
