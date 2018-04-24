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
	 * Default upper mantle radius in kilometers.  This corresponds 
	 * to the 410 kilometer depth discontinuity.
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
}
