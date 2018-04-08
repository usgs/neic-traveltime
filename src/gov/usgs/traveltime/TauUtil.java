package gov.usgs.traveltime;

import java.util.ArrayList;
import java.util.TreeMap;

/**
 * Utility methods for the travel-time package.
 * 
 * @author Ray Buland
 *
 */
public class TauUtil {
	/**
	 * Global tolerance value.
	 */
	public static final double DTOL = 1e-9d;
	/**
	 * Global minimum positive value.
	 */
	public static final double DMIN = 1e-30d;
	/**
	 * Global maximum positive value.
	 */
	public static final double DMAX = 1e30d;
	/**
	 * Maximum depth supported by the travel-time tables.
	 */
	public static final double MAXDEPTH = 800d;
	/**
	 * Crude minimum elevation in kilometers (would include a station 
	 * at the bottom of the Mariana Trench).
	 */
	public static final double MINELEV = -11d;
	/**
	 * Crude maximum elevation in kilometers (would include a station 
	 * at the top of Mount Everest).
	 */
	public static final double MAXELEV = 9d;
	/**
	 * Global default shallow crustal P velocity in km/s (from ak135).
	 */
	public static final double DEFVP = 5.80d;
	/**
	 * Global default shallow crustal S velocity in km/s (from ak135).
	 */
	public static final double DEFVS = 3.46d;
	/**
	 * Global default water P velocity in km/s.
	 */
	public static final double DEFVW = 1.50d;
	/**
	 * Lg group velocity in kilometers/second.
	 */
	public static final double LGGRPVEL = 3.4d;
	/**
	 * LR group velocity in kilometers/second.
	 */
	public static final double LRGRPVEL = 3.5d;
	/**
	 * Maximum credible Pn source-receiver distance in degrees.  
	 * Note that these maximum regional phase distances are mostly 
	 * needed for RSTT.  Because of the nature of the algorithm, 
	 * there are no inherent distance limits for any of the phases.
	 */
	public static final double MAXDELPN = 21.5d;
	/**
	 * Maximum credible Sn source-receiver distance in degrees.  
	 * Note that there is a short Sn branch in the WUS model at more  
	 * than 40 degrees that appears to be an artifact of layering the 
	 * WUS crust on top of the AK135 mantle.
	 */
	public static final double MAXDELSN = 30.0d;
	/**
	 * Maximum credible Pg source-receiver distance in degrees.
	 */
	public static final double MAXDELPG = 8.5d;
	/**
	 * Maximum credible Lg source-receiver distance in degrees.
	 */
	public static final double MAXDELLG = 30.0d;
	/**
	 * Global default travel-time statistical bias in seconds.
	 */
	public static final double DEFBIAS = 0d;
	/**
	 * Global default travel-time statistical spread in seconds.
	 */
	public static final double DEFSPREAD = 12d;
	/**
	 * Global default travel-time statistical relative observability.
	 */
	public static final double DEFOBSERV = 0d;
	/**
	 * The association window for theoretical phases will be the 
	 * association factor times the spread.
	 */
	public static final double ASSOCFACTOR = 7d;
	/**
	 * In any case, don't let the association window get smaller than 
	 * the minimum window size no matter how well observed the phase 
	 * is.  The larger window is needed when the location is poor.
	 */
	public static final double WINDOWMIN = 5d;
	/**
	 * The distance increment in degrees for travel-time plots.
	 */
	public static final double DDELPLOT = 1d;
	
	/**
	 * Minimum distance in radians for an Sn branch to proxy for Lg.
	 */
	protected static final double SNDELMIN = 0.035d;
	/**
	 * Maximum depth in kilometers for which Lg will be added.
	 */
	protected static final double LGDEPMAX = 35d;
	/**
	 * Maximum depth in kilometers for which LR will be added.
	 */
	protected static final double LRDEPMAX = 55d;
	/**
	 * Maximum distance in radians for which LR will be added.
	 */
	protected static final double LRDELMAX = 0.698d;
	/**
	 * Minimum time in seconds that phases with the same name should 
	 * be separated.
	 */
	protected static final double DTCHATTER = 0.005d;
	/**
	 * Time interval following a phase where it's nearly impossible 
	 * to pick another phase.
	 */
	protected static final double DTOBSERV = 3d;
	/**
	 * Observability threshold to ensure observability doesn't go to zero.
	 */
	protected static final double MINOBSERV = 1d;
	/**
	 * Frequency for which DTOBSERV is half a cycle.
	 */
	protected static final double FREQOBSERV = Math.PI/DTOBSERV;
	/**
	 * The ellipticity factor needed to compute geocentric co-latitude.
	 */
	protected final static double ELLIPFAC = 0.993305521d;
	
	/**
	 * If true, suppress all travel-time corrections for debugging 
	 * purposes and cases where the corrections aren't needed.
	 */
	public static boolean noCorr = false;
	
	/**
	 * Receiver azimuth relative to the source in degrees clockwise from 
	 * north (available after calling delAz).
	 */
	public static double azimuth = Double.NaN;
	/**
	 * Longitude in degrees projected from an epicenter by a distance 
	 * and azimuth.
	 */
	public static double projLon = Double.NaN;
	
	/**
	 * Path of the travel time/locator properties file.
	 */
//private static String propFile = System.getProperty("user.home")+Util.FS+
//		"Properties"+Util.FS+"traveltime.prop";
	private static String propFile = "Properties"+CWBProperties.FS+"traveltime.prop";
	/**
	 * Paths for model and event files set in getProperties.
	 */
	private static String modelPath;
	private static String eventPath;
	/**
	 * Storage for unique codes.
	 */
	private static TreeMap<String, Integer> unique;
	
	/**
	 * Read the travel time properties file and set up paths to the model 
	 * and event files.
	 */
	public static void getProperties() {		
		CWBProperties.loadProperties(propFile);
		modelPath = CWBProperties.getProperty("modelPath");
		eventPath = CWBProperties.getProperty("eventPath");
	}
	
	/**
	 * Build a path to a model file.
	 * 
	 * @param modelFile Model file name
	 * @return Model file path
	 */
	public static String model(String modelFile) {
		return modelPath+modelFile;
	}
	
	/**
	 * Build a path to an event file.
	 * 
	 * @param eventID Hydra style event file ID number
	 * @return Event file path
	 */
	public static String event(String eventID) {
		return eventPath+"RayLocInput"+eventID+".txt";
	}
	
	/**
	 * Create a segment code by stripping a phase code of unnecessary 
	 * frippery.
	 * 
	 * @param phCode Phase code
	 * @return Segment code
	 */
	public static String phSeg(String phCode) {
		int index;
		String phGen;
		
		if((index = phCode.indexOf("df")) >= 0) return phCode.substring(0, index);
		if((index = phCode.indexOf("ab")) >= 0) return phCode.substring(0, index);
		if((index = phCode.indexOf("ac")) >= 0) return phCode.substring(0, index);
		if((index = phCode.indexOf("g")) >= 0) {
			phGen = phCode.substring(0, index)+phCode.substring(index+1, phCode.length());
			if((index = phGen.indexOf("g")) >= 0) return phGen.substring(0, index);
			else return phGen;
		}
		if((index = phCode.indexOf("b")) >= 0) {
			phGen = phCode.substring(0, index)+phCode.substring(index+1, phCode.length());
			if((index = phGen.indexOf("b")) >= 0) return phGen.substring(0, index);
			else return phGen;
		}
		if((index = phCode.indexOf("n")) >= 0) {
			phGen = phCode.substring(0, index)+phCode.substring(index+1, phCode.length());
			if((index = phGen.indexOf("n")) >= 0) return phGen.substring(0, index);
			else return phGen;
		}
		return phCode;
	}
	
	/**
	 * Make phase codes unique by appending a reference number.  This is needed to 
	 * keep branches straight in the plot data.
	 * 
	 * @param phCode Phase code
	 * @return Unique phase code
	 */
	public static String uniqueCode(String phCode) {
		Integer no;
		
		if(unique == null) unique = new TreeMap<String, Integer>();
		no = unique.get(phCode);
		if(no != null) {
			unique.replace(phCode, ++no);
		} else {
			no = 0;
			unique.put(phCode, no);
		}
		return phCode+no;
	}

	/**
	 * Classify seismic phases according to their wave type at 
	 * the receiver.
	 * 
	 * @param phCode Phase code
	 * @return 'P' for a p-wave, 'S' for an s-wave, 'L' for an Lg, 
	 * and 'R' for an LR.
	 */
	public static char arrivalType(String phCode) {
		// Try the common cases first.
		for(int j=phCode.length()-1; j>=0; j--) {
			if(phCode.charAt(j) == 'P') {
				return 'P';
			} else if(phCode.charAt(j) == 'S') {
				return 'S';
			}
		}
		// Then do the special cases.
		if(phCode.equals("Lg")) return 'L';
		else if(phCode.equals("LR")) return 'R';
		// This should never happen.
		else return ' ';
	}
	
	/**
	 * By default, filter out phases of the same name that are so 
	 * close in time as to be useless.  This is particularly useful 
	 * when there are small instabilities in the tau interpolation.
	 * 
	 * @param tTimes An array list of travel-time objects
	 */
	public static void filterDef(ArrayList<TTimeData> tTimes) {
		for(int j=1; j<tTimes.size(); j++) {
			if(tTimes.get(j).phCode.equals(tTimes.get(j-1).phCode) && 
					tTimes.get(j).tt-tTimes.get(j-1).tt <= DTCHATTER) {
				tTimes.remove(j--);
			}
		}
	}
	
	/**
	 * Modify the of observability of phases closely following other 
	 * phases (in time).  This is a real seat-of-the-pants hack.  The 
	 * problem is that the proximity of phases in time depends on 
	 * depth, but the statistics include all depths (corrected to 
	 * surface focus).
	 * 
	 * @param tTimes An array list of travel-time objects
	 */
	public static void modObserv(ArrayList<TTimeData> tTimes) {
		TTimeData tTimeJ, tTimeI;
		
		// Loop backwards over the phases.
		for(int j=tTimes.size()-2; j>=0; j--) {
			tTimeJ = tTimes.get(j);
			// Loop over the phases later than the current phase.
			for(int i=j+1; i<tTimes.size(); i++) {
				tTimeI = tTimes.get(i);
				// If the phases are close together, modify the later phase.
				if(tTimeI.tt-tTimeJ.tt < DTOBSERV) {
					// If the later phase has some observability, make sure it 
					// might still be used.
					if(tTimeI.observ >= MINOBSERV) {
						tTimeI.observ = Math.max(tTimeI.observ-0.5d*tTimeJ.observ*
								(Math.cos(FREQOBSERV*(tTimeI.tt-tTimeJ.tt))+1d), 
								MINOBSERV);
					// Otherwise, let the later phase observability go to zero.
					} else {
						tTimeI.observ = Math.max(tTimeI.observ-0.5d*tTimeJ.observ*
								(Math.cos(FREQOBSERV*(tTimeI.tt-tTimeJ.tt))+1d), 
								0d);
					}
				} else break;
			}
		}
	}
	
	/**
	 * Optionally, all seismic back branches can be filtered out.  
	 * Triplication back branches confuse the Locator, so this 
	 * filter removes all secondary phases of the same name no 
	 * matter how big the arrival time differences.
	 * 
	 * @param tTimes An array list of travel-time objects
	 */
	public static void filterBack(ArrayList<TTimeData> tTimes) {
		for(int j=0; j<tTimes.size()-1; j++) {
			for(int i=j+1; i<tTimes.size(); i++) {
				if(tTimes.get(j).phCode.equals(tTimes.get(i).phCode)) {
					tTimes.remove(i--);
				}
			}
		}
	}
	
	/**
	 * Optionally, Pb and Sb can be renamed Pg and Sg respectively.  
	 * By default, the travel-time calculations are done for 
	 * continental cratons where there may be clear Pb (P*) and 
	 * Sb (S*) phases.  However, these just annoy the seismic 
	 * analysts in tectonic areas where they are rarely if ever 
	 * observed.  The crude hack implemented here is to rename Pb 
	 * and Sb to Pg and Sg respectively.  This has the effect of 
	 * pretending that the modeled Conrad discontinuity is a smooth 
	 * transition and that Pg and Sg may turn all the way to the 
	 * Moho.  Note that this may create new back branches, so it 
	 * should be run before the default or back branch filters.
	 * 
	 * @param tTimes An array list of travel-time objects
	 */
	public static void filterTect(ArrayList<TTimeData> tTimes) {
		for(int j=0; j<tTimes.size(); j++) {
			// Turn Pbs into Pgs.
			if(tTimes.get(j).phCode.contains("Pb") && 
					tTimes.get(j).phCode.charAt(1) != 'K') {
				tTimes.get(j).replace("Pb", "Pg");
			}
			// Turn Sbs into Sgs.
			if(tTimes.get(j).phCode.contains("Sb") && 
					tTimes.get(j).phCode.charAt(1) != 'K') {
				tTimes.get(j).replace("Sb", "Sg");
			}
		}
	}
	
	/**
	 * By default, canUse only reflects phase types that make sense to 
	 * use in an earthquake Location.  However, phases can't be used 
	 * if they have no statistics either.  Setting canUse to reflect 
	 * both conditions makes it easier for the Locator.
	 * 
	 * @param tTimes An array list of travel-time objects
	 */
	public static void modCanUse(ArrayList<TTimeData> tTimes) {
		for(int j=0; j<tTimes.size(); j++) {
			if(tTimes.get(j).spread >= DEFSPREAD || tTimes.get(j).observ <= 
					DEFOBSERV) tTimes.get(j).canUse = false;
		}
	}
	
	/**
	 * Apply miscellaneous filters to get rid of extraneous phases.
	 * 
	 * @param tTimes An array list of travel-time objects
	 * @param delta Source-receiver distance in degrees
	 */
	public static void filterMisc(ArrayList<TTimeData> tTimes, double delta) {
		for(int j=0; j<tTimes.size(); j++) {
			if(tTimes.get(j).phCode.equals("Sn") && delta > MAXDELSN) {
				tTimes.remove(j);
				break;
			}
		}
	}
	
	/**
	 * Compute the geocentric co-latitude.
	 * 
	 * @param latitude Geographical latitude in degrees
	 * @return Geocentric co-latitude in degrees
	 */
	public static double geoCen(double latitude) {
		if(Math.abs(90d-latitude) < TauUtil.DTOL) {
			return 0d;
		} else if(Math.abs(90d+latitude) < TauUtil.DTOL) {
			return 180d;
		} else {
			return 90d-Math.toDegrees(Math.atan(ELLIPFAC*
					Math.sin(Math.toRadians(latitude))/
					Math.cos(Math.toRadians(latitude))));
		}
	}
	
	/**
	 * Compute the geographic latitude.
	 * 
	 * @param coLat Geocentric co-latitude in degrees
	 * @return Geographic latitude in degrees
	 */
	public static double geoLat(double coLat) {
		return Math.toDegrees(Math.atan(Math.cos(Math.toRadians(coLat))/
				(ELLIPFAC*Math.max(Math.sin(Math.toRadians(coLat)), 
				TauUtil.DTOL))));
	}

	/**
	 * An historically significant subroutine from deep time (1962)!  This 
	 * routine was written by Bob Engdahl in Fortran (actually in the days 
	 * before subroutines) and beaten into it's current Fortran form by 
	 * Ray Buland in the early 1980s.  It's optimized with respect to 
	 * computing sines and cosines (probably still worthwhile) and it 
	 * computes exactly what's needed--no more, no less.  This (much more 
	 * horrible) alternate form to the delAz in LocUtil is much closer to 
	 * Engdahl's original.  It is needed to avoid a build path cycle.  Note 
	 * that the azimuth is returned in static variable azimuth.
	 * 
	 * @param eqLat Geographic source latitude in degrees
	 * @param eqLon Source longitude in degrees
	 * @param staLat Geographic station latitude in degrees
	 * @param staLon Station longitude in degrees
	 * @return Distance (delta) in degrees
	 */
	public static double delAz(double eqLat, double eqLon, double staLat, 
			double staLon) {
		double coLat, eqSinLat, eqCosLat, eqSinLon, eqCosLon;
		double staSinLat, staCosLat, staSinLon, staCosLon;
		double cosdel, sindel, tm1, tm2;	// Use Bob Engdahl's variable names
		
		// Get the hypocenter geocentric co-latitude.
		coLat = geoCen(eqLat);
		// Hypocenter sines and cosines.
		eqSinLat = Math.sin(Math.toRadians(coLat));
		eqCosLat = Math.cos(Math.toRadians(coLat));
		eqSinLon = Math.sin(Math.toRadians(eqLon));
		eqCosLon = Math.cos(Math.toRadians(eqLon));
		
		// Get the station geocentric co-latitude.
		coLat = geoCen(staLat);
		// Station sines and cosines.
		staSinLat = Math.sin(Math.toRadians(coLat));
		staCosLat = Math.cos(Math.toRadians(coLat));
		staSinLon = Math.sin(Math.toRadians(staLon));
		staCosLon = Math.cos(Math.toRadians(staLon));
		
		// South Pole:
		if(staSinLat <= TauUtil.DTOL) {
			azimuth = 180d;
			return Math.toDegrees(Math.PI-Math.acos(eqCosLat));
		}
		
		// Compute some intermediate variables.
		cosdel = eqSinLat*staSinLat*(staCosLon*eqCosLon+
				staSinLon*eqSinLon)+eqCosLat*staCosLat;
		tm1 = staSinLat*(staSinLon*eqCosLon-staCosLon*eqSinLon);
		tm2 = eqSinLat*staCosLat-eqCosLat*staSinLat*
				(staCosLon*eqCosLon+staSinLon*eqSinLon);
		sindel = Math.sqrt(Math.pow(tm1,2d)+Math.pow(tm2,2d));
		
		// Do the azimuth.
		if(Math.abs(tm1) <= TauUtil.DTOL && Math.abs(tm2) <= TauUtil.DTOL) {
			azimuth = 0d;		// North Pole.
		} else {
			azimuth = Math.toDegrees(Math.atan2(tm1,tm2));
			if(azimuth < 0d) azimuth += 360;
		}
		
		// Do delta.
		if(sindel <= TauUtil.DTOL && Math.abs(cosdel) <= TauUtil.DTOL) {
			return 0d;
		} else {
			return Math.toDegrees(Math.atan2(sindel,cosdel));
		}
	}
	
	/**
	 * Project an epicenter using distance and azimuth.  Used in finding 
	 * the bounce point for a surface reflected seismic phase.  Note that 
	 * the projected longitude is returned in static variable projLon.
	 * 
	 * @param latitude Geographic epicenter latitude in degrees
	 * @param longitude Epicenter longitude in degrees
	 * @param delta Distance to project in degrees
	 * @param azimuth Azimuth to project in degrees
	 * @return Projected geographic latitude in degrees
	 */
	public static double projLat(double latitude, double longitude, 
			double delta, double azimuth) {
		double projLat, coLat, sinLat, cosLat, sinDel, cosDel, sinAzim, 
			cosAzim, cTheta, sinNewLat;
		
		coLat = TauUtil.geoCen(latitude);
		if(longitude < 0d) longitude += 360d;
		
		sinLat = Math.sin(Math.toRadians(coLat));
		cosLat = Math.cos(Math.toRadians(coLat));
		sinDel = Math.sin(Math.toRadians(delta));
		cosDel = Math.cos(Math.toRadians(delta));
		sinAzim = Math.sin(Math.toRadians(azimuth));
		cosAzim = Math.cos(Math.toRadians(azimuth));
		
		cTheta = sinDel*sinLat*cosAzim+cosLat*cosDel;
		projLat = Math.acos(cTheta);
		sinNewLat = Math.sin(projLat);
		if(coLat == 0d) {
			projLon = azimuth;
		} else if(projLat == 0d) {
			projLon = 0d;
		} else {
			projLon = longitude+Math.toDegrees(Math.atan2(sinDel*
					sinAzim/sinNewLat, (cosDel-cosLat*cTheta)/
					(sinLat*sinNewLat)));
		}
		if(projLon > 360d) projLon -= 360d;
		if(projLon > 180d) projLon -= 360d;
		return geoLat(Math.toDegrees(projLat));
	}
	
	/**
	 * Elevation correction.
	 * 
	 * @param elev Elevation in kilometers
	 * @param vel Velocity in kilometers/second
	 * @param dTdD Ray parameter in seconds/kilometers
	 * @return Elevation correction in seconds
	 */
	public static double topoCorr(double elev, double vel, double dTdD) {
		return (elev/vel)*Math.sqrt(Math.abs(1.-
				Math.min(Math.pow(vel*dTdD,2d),1d)));
	}
	
	/**
	 * Bilinear interpolation.  The indices are such that 
	 * val[ind] &lt; var &lt;= val[ind+1].  The two dimensional grid of 
	 * values to be interpolated has values val0 associated with 
	 * it's first index and values val1 associated with it's second 
	 * index.
	 * 
	 * @param var0 First variable
	 * @param var1 Second variable
	 * @param val0 Value array for the first grid index
	 * @param val1 Value array for the second grid index
	 * @param grid Two dimensional array of values to be interpolated
	 * @return Interpolated value
	 */
	public static double biLinear(double var0, double var1, 
			GenIndex val0, GenIndex val1, double[][] grid) {
		// Use the virtual arrays to get the interpolation indices.
		int ind0 = val0.getIndex(var0);
		int ind1 = val1.getIndex(var1);
		// Interpolate the first variable at it's lower index.
		double lin00 = grid[ind0][ind1]+
				(grid[ind0+1][ind1]-grid[ind0][ind1])*
				(var0-val0.getValue(ind0))/
				(val0.getValue(ind0+1)-val0.getValue(ind0));
		// Interpolate the first variable at it's upper index.
		double lin01 = grid[ind0][ind1+1]+
				(grid[ind0+1][ind1+1]-grid[ind0][ind1+1])*
				(var0-val0.getValue(ind0))/
				(val0.getValue(ind0+1)-val0.getValue(ind0));
		// Interpolate the second variable.
		return lin00+(lin01-lin00)*(var1-val1.getValue(ind1))/
				(val1.getValue(ind1+1)-val1.getValue(ind1));
	}
	
	/**
	 * Bilinear interpolation.  The indices are such that 
	 * val[ind] &lt; var &lt;= val[ind+1].  The two dimensional grid of 
	 * values to be interpolated has values val0 associated with 
	 * it's first index and values val1 associated with it's second 
	 * index.
	 * 
	 * @param var0 First variable
	 * @param var1 Second variable
	 * @param val0 Value array for the first grid index
	 * @param val1 Value array for the second grid index
	 * @param grid Two dimensional array of values to be interpolated
	 * @return Interpolated value
	 */
	public static double biLinear(double var0, double var1, 
			GenIndex val0, GenIndex val1, short[][] grid) {
		// Use the virtual arrays to get the interpolation indices.
		int ind0 = val0.getIndex(var0);
		int ind1 = val1.getIndex(var1);
		// Interpolate the first variable at it's lower index.
		double lin00 = (double)grid[ind0][ind1]+
				((double)grid[ind0+1][ind1]-(double)grid[ind0][ind1])*
				(var0-val0.getValue(ind0))/
				(val0.getValue(ind0+1)-val0.getValue(ind0));
		// Interpolate the first variable at it's upper index.
		double lin01 = (double)grid[ind0][ind1+1]+
				((double)grid[ind0+1][ind1+1]-(double)grid[ind0][ind1+1])*
				(var0-val0.getValue(ind0))/
				(val0.getValue(ind0+1)-val0.getValue(ind0));
		// Interpolate the second variable.
		return lin00+(lin01-lin00)*(var1-val1.getValue(ind1))/
				(val1.getValue(ind1+1)-val1.getValue(ind1));
	}
}
