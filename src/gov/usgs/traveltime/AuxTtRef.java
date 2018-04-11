package gov.usgs.traveltime;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
// import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Scanner;
import java.util.TreeMap;

/**
 * Auxiliary data augmenting the basic travel-times.  This 
 * data is common to all models and only need be read once 
 * during the travel-time server initialization.  Auxiliary 
 * data includes phase groups (needed for association in the 
 * Locator) and travel-time statistics.  The statistics are 
 * used in the travel-time package for the arrival times of 
 * add-on phases (e.g., PKPpre) and in the Locator for 
 * association and phase weighting.
 * 
 * @author Ray Buland
 *
 */
public class AuxTtRef {
	// Phase group storage.
	final PhGroup regional;									// Regional phase group
	final PhGroup depth;										// Depth sensitive phase group
	final PhGroup downWeight;								// Phases to be down weighted
	final PhGroup canUse;										// Phases that can be used for location
	final PhGroup chaff;										// Useless phases according to NEIC analysts
	final ArrayList<PhGroup> phGroups;			// List of primary phase groups
	final ArrayList<PhGroup> auxGroups;			// List of auxiliary phase groups
	// Phase statistics storage.
	final TreeMap<String, TtStat> ttStats;	// List of phase statistics
	// Ellipticity storage.
	final TreeMap<String, Ellip> ellips;		// List of ellipticity corrections
	// Flag storage by phase
	final TreeMap<String, TtFlags> phFlags;	// Phase group information by phase
	// Topography.
	final Topography topoMap;									// Topography for bounce points
	TtStat ttStat;					// Phase statistics
	Ellip ellip, upEllip;		// Ellipticity correction(s)
	// Set up the reader.
	Scanner scan;
	boolean priGroup = false;
	int nDepth;
	String nextCode;

	/**
	 * Read and organize auxiliary data.  Note that for convenience 
	 * some processing is done on the travel-time statistics.  This 
	 * eliminates a stand alone program that processed the raw 
	 * (maintainable) statistics into an intermediate form for the 
	 * Locator.  Only the phase groups are mandatory.  If other 
	 * information isn't read, the relevant processing will simply not 
	 * be done.
	 * 
	 * @param readStats If true, read the phase statistics
	 * @param readEllip If true, read the ellipticity corrections
	 * @param readTopo If true, read the topography file
	 * @throws IOException If opens fail
	 */
	public AuxTtRef(boolean readStats, boolean readEllip, boolean readTopo) 
			throws IOException {
		BufferedInputStream inGroup, inStats, inEllip;
		EllipDeps eDepth;
		
		// Set up the properties.
		TauUtil.getProperties();
		
		// Open and read the phase groups file.
		inGroup = new BufferedInputStream(new FileInputStream(TauUtil.model("groups.txt")));
		scan = new Scanner(inGroup);
		// Prime the pump.
		nextCode = scan.next();
		// Handle local-regional phases separately.
		regional = read1Group();
		// Handle depth phases separately.
		depth = read1Group();
		// Handle down weighted phases separately.
		downWeight = read1Group();
		// Handle used phases separately.
		canUse = read1Group();
		// Handle useless phases separately.
		chaff = read1Group();
		// Handle "normal" groups.
		phGroups = new ArrayList<PhGroup>();
		auxGroups = new ArrayList<PhGroup>();
		readGroups();
		inGroup.close();
		
		if(readStats) {
			// Open and read the travel-time statistics file.
			inStats = new BufferedInputStream(new FileInputStream(
					TauUtil.model("ttstats.txt")));
			scan = new Scanner(inStats);
			ttStats = new TreeMap<String, TtStat>();
			// Prime the pump.
			nextCode = scan.next();
			// Scan phase statistics until we run out.
			do {
				ttStat = read1StatHead();
				ttStats.put(ttStat.phCode, ttStat);
				read1StatData(new TtStatLinFit(ttStat));
			} while(scan.hasNext());
			inStats.close();
		} else {
			ttStats = null;
		}
		
		if(readEllip) {
			// Open and read the ellipticity correction file.
			inEllip = new BufferedInputStream(new FileInputStream(
					TauUtil.model("ellip.txt")));
			scan = new Scanner(inEllip);
			ellips = new TreeMap<String, Ellip>();
			eDepth = new EllipDeps();
			nDepth = eDepth.ellipDeps.length;
			do {
				ellip = read1Ellip();
				ellips.put(ellip.phCode, ellip);
			} while(scan.hasNext());
			inEllip.close();
		} else {
			ellips = null;
		}
		
		// Rearrange group flags, phase flags and statistics and the 
		// ellipticity correction by phase.
		phFlags = new TreeMap<String, TtFlags>();
		makePhFlags();
		
		if(readTopo) {
			// Set up the topography data.
			topoMap = new Topography();
		} else {
			topoMap = null;
		}
	}
	
	/**
	 * Read one phase group (i.e., one line in the phase group file).
	 * 
	 * @return Phase group just read
	 */
	private PhGroup read1Group() {
		if(nextCode.contains(":")) {
			PhGroup group = new PhGroup(nextCode.substring(0, 
					nextCode.indexOf(':')));
			nextCode = scan.next();
			while(!nextCode.contains(":") & !nextCode.contains("-")) {
				group.addPhase(nextCode);
				nextCode = scan.next();
			}
			return group;
		} else {
			if(scan.hasNext()) nextCode = scan.next();
			return null;
		}
	}
	
	/**
	 * Read in all the "normal" phase groups.  Note that they are 
	 * read in pairs, typically crust-mantle phase in the primary 
	 * group and core phases in the auxiliary group.  These pair-
	 * wise groups are used for phase identification in the Locator.
	 * 
	 * @param print List the auxiliary data as it's read if true
	 */
	private void readGroups() {
		do {
			// Groups are added to the ArrayLists as they are created.
			phGroups.add(read1Group());
			auxGroups.add(read1Group());
		} while(scan.hasNext());
	}
	
	/**
	 * Find the group a phase belongs to.
	 * 
	 * @param phase Phase code
	 * @return Phase group name
	 */
	public String findGroup(String phase) {
		PhGroup group;
		// Search the primary phase group.
		for(int j=0; j<phGroups.size(); j++) {
			group = phGroups.get(j);
			for(int k=0; k<group.phases.size(); k++) {
				if(phase.equals(group.phases.get(k))) {
					priGroup = true;
					return group.groupName;
				}
			}
			// Search the auxiliary phase group.
			group = auxGroups.get(j);
			if(group != null) {
				for(int k=0; k<group.phases.size(); k++) {
					if(phase.equals(group.phases.get(k))) {
						priGroup = false;
						return group.groupName;
					}
				}
			}
		}
		// OK, that didn't work.  See if the phase is generic.
		for(int j=0; j<phGroups.size(); j++) {
			// Try the primary group name.
			if(phase.equals(phGroups.get(j).groupName)) {
				priGroup = true;
				return phGroups.get(j).groupName;
			}
			// Try the auxiliary group name.
			if(auxGroups.get(j) != null) {
				if(phase.equals(auxGroups.get(j).groupName)) {
					priGroup = false;
					return auxGroups.get(j).groupName;
				}
			}
		}
		// OK, that didn't work.  Let's just give up.
		priGroup = true;
		return "";
	}
	
	/**
	 * Special version of findGroup that deals with real world 
	 * problems like a blank phase code and automatic picks 
	 * that are all identified as P.
	 * 
	 * @param phase Phase code
	 * @param auto True if the pick was done automatically
	 * @return Phase group name
	 */
	public String findGroup(String phase, boolean auto) {
		priGroup = true;
		if(phase.equals("")) return "Any";
		else if(auto && phase.equals("P")) return "Reg";
		else return findGroup(phase);
	}
	
	/**
	 * The phase identification algorithm depends on knowing which 
	 * set of phase groups was found.
	 * 
	 * @return True if the last phase group found was primary
	 */
	public boolean isPrimary() {
		return priGroup;
	}
	
	/**
	 * Find the complementary phase group.  That is, if the phase 
	 * group is primary, return the associated auxiliary phase 
	 * group and vice versa.
	 * 
	 * @param groupName Phase group name
	 * @return Complementary phase group name
	 */
	public String compGroup(String groupName) {
		for(int j=0; j<phGroups.size(); j++) {
			if(groupName.equals(phGroups.get(j).groupName)) {
				if(auxGroups.get(j) != null) return auxGroups.get(j).groupName;
				else return null;
			}
			if(auxGroups.get(j) != null) {
				if(groupName.equals(auxGroups.get(j).groupName)) {
					return phGroups.get(j).groupName;
				}
			}
		}
		return null;
	}
	
	/**
	 * See if this phase group can be used for earthquake location.
	 * 
	 * @param phase Phase name
	 * @return True if this phase can be used for earthquake location
	 */
	public boolean canUse(String phase) {
		for(int k=0; k<canUse.phases.size(); k++) {
			if(phase.equals(canUse.phases.get(k))) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * See if this phase group is useless for earthquake location 
	 * (according to the NEIC analysts).  Most of these phases are crustal 
	 * reverberations that end up in the coda of more useful phases.
	 * 
	 * @param phase Phase name
	 * @return True if this phase is useless
	 */
	public boolean isChaff(String phase) {
		for(int k=0; k<chaff.phases.size(); k++) {
			if(phase.equals(chaff.phases.get(k))) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * See if this is a local-regional phase.
	 * 
	 * @param phase Phase code
	 * @return True if the phase is local-regional
	 */
	public boolean isRegional(String phase) {
		for(int k=0; k<regional.phases.size(); k++) {
			if(phase.equals(regional.phases.get(k))) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * See if this is a depth phase.
	 * 
	 * @param phase Phase code
	 * @return True if the phase is depth sensitive
	 */
	public boolean isDepthPh(String phase) {
		for(int k=0; k<depth.phases.size(); k++) {
			if(phase.equals(depth.phases.get(k))) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * See if this is a down weighted phase.
	 * 
	 * @param phase Phase code
	 * @return True if the phase is to be down weighted
	 */
	public boolean isDisPh(String phase) {
		for(int k=0; k<downWeight.phases.size(); k++) {
			if(phase.equals(downWeight.phases.get(k))) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Print phase groups.
	 */
	public void printGroups() {
		// Dump regional phases.
		regional.dumpGroup();
		System.out.println();
		// Dump depth phases.
		depth.dumpGroup();
		System.out.println();
		// Dump down weighted phases.
		downWeight.dumpGroup();
		System.out.println();
		// Dump phases that can be used for location.
		canUse.dumpGroup();
		System.out.println();
		// Dump phases that are NEIC-useless.
		chaff.dumpGroup();
		System.out.println();
		// Dump normal groups.
		for(int j=0; j<phGroups.size(); j++) {
			phGroups.get(j).dumpGroup();
			if(auxGroups.get(j) != null) auxGroups.get(j).dumpGroup();
			else System.out.println("    *null*");
		}
	}
	
	/**
	 * Read in the statistics header for one phase.
	 * 
	 * @return Statistics object
	 */
	private TtStat read1StatHead() {
		String phCode;
		int minDelta, maxDelta;
		TtStat ttStat;
		
		// Get the phase header.
		phCode = nextCode;
		minDelta = scan.nextInt();
		maxDelta = scan.nextInt();
		ttStat = new TtStat(phCode, minDelta, maxDelta);
		return ttStat;
	}
	
	/**
	 * Read in the statistics data for one phase.  Rather than reading 
	 * in the linear fits as in the FORTRAN version, the raw statistics 
	 * file is read and the fits are done on the fly.  This makes it 
	 * easier to maintain the statistics as the utility program that did 
	 * the fits becomes redundant.
	 * 
	 * @param fit LinearFit object
	 */
	private void read1StatData(TtStatLinFit fit) {
		int delta;
		double res, spd, obs;
		boolean resBrk, spdBrk, obsBrk;
		boolean done;
		
		done = false;
		
		// Scan for the phase bias, spread, and observability.
		do{
			delta = scan.nextInt();
			res = scan.nextDouble();
			// Check for a linear fit break flag.
			if(scan.hasNextDouble()) resBrk = false;
			else {
				resBrk = true;
				if(!scan.next().equals("*")) {
					System.out.println("read1Stat: warning--the next field is "+
							"neither a number nor an astrisk?");
				}
			}
			spd = scan.nextDouble();
			// Check for a linear fit break flag.
			if(scan.hasNextDouble()) spdBrk = false;
			else {
				spdBrk = true;
				if(!scan.next().equals("*")) {
					System.out.println("read1Stat: warning--the next field is "+
							"neither a number nor an astrisk?");
				}
			}
			obs = scan.nextDouble();
			// Check for a linear fit break flag.
			if(scan.hasNextInt()) obsBrk = false;
			else {
				// This is especially fraught at the EOF.
				if(scan.hasNext()) {
					// If it's not an EOF there are still several possibilities.
					nextCode = scan.next();
					if(nextCode.equals("*")) {
						obsBrk = true;
						if(!scan.hasNextInt()) {
							done = true;
							if(scan.hasNext()) {
								nextCode = scan.next();
							} else {
								nextCode = "~";
							}
						}
					} else {
						obsBrk = false;
						done = true;
					}
				} else {
					obsBrk = false;
					done = true;
				}
			}
			fit.add(delta, res, resBrk, spd, spdBrk, obs, obsBrk);
		} while(!done);
		
		// Crunch the linear fits.
		fit.fitAll();
		fit = null;
	}
	
	/**
	 * Find the statistics associated with the desired phase.
	 * 
	 * @param phase Phase code
	 * @return A phase statistics object
	 */
	public TtStat findStats(String phase) {
		if(ttStats == null) return null;
		else return ttStats.get(phase);
	}
	
	/**
	 * get the phase bias.
	 * 
	 * @param ttStat Pointer to the associated phase statistics
	 * @param delta Distance in degrees
	 * @return Bias in seconds at distance delta
	 */
	public double getBias(TtStat ttStat, double delta) {
		if(ttStat == null) return TauUtil.DEFBIAS;
		else return ttStat.getBias(delta);
	}
	
	/**
	 * Get the phase spread.
	 * 
	 * @param ttStat Pointer to the associated phase statistics
	 * @param delta Distance in degrees
	 * @param upGoing True if the phase is an up-going P or S
	 * @return Spread in seconds at distance delta
	 */
	public double getSpread(TtStat ttStat, double delta, 
			boolean upGoing) {
		if(ttStat == null) return TauUtil.DEFSPREAD;
		else return ttStat.getSpread(delta, upGoing);
	}
	
	/**
	 * Get the phase observability.
	 * 
	 * @param ttStat Pointer to the associated phase statistics
	 * @param delta Distance in degrees
	 * @param upGoing True if the phase is an up-going P or S
	 * @return Relative observability at distance delta
	 */
	public double getObserv(TtStat ttStat, double delta, 
			boolean upGoing) {
		if(ttStat == null) return TauUtil.DEFOBSERV;
		else return ttStat.getObserv(delta, upGoing);
	}
	
	/**
	 * Print phase statistics.
	 */
	public void printStats() {
		TtStat ttStat;
		
		NavigableMap<String, TtStat> map = ttStats.headMap("~", true);
		System.out.println("\n     Phase Statistics:");
		for(@SuppressWarnings("rawtypes") Map.Entry entry : map.entrySet()) {
			ttStat = (TtStat)entry.getValue();
			ttStat.dumpStats();
		}
	}
	
	/**
	 * Read in ellipticity correction data for one phase.
	 * 
	 * @return Ellipticity object
	 */
	private Ellip read1Ellip() {
		String phCode;
		int nDelta;
		@SuppressWarnings("unused")
		double delta;
		double minDelta, maxDelta;
		double[][] t0, t1, t2;
		Ellip ellip;
		
		// Read the header.
		phCode = scan.next();
		nDelta = scan.nextInt();
		minDelta = scan.nextDouble();
		maxDelta = scan.nextDouble();
		
		// Allocate storage.
		t0 = new double[nDelta][nDepth];
		t1 = new double[nDelta][nDepth];
		t2 = new double[nDelta][nDepth];
		// Read in the tau profiles.
		for(int j=0; j<nDelta; j++) {
			delta = scan.nextDouble();		// The distance is cosmetic
			for(int i=0; i<nDepth; i++) t0[j][i] = scan.nextDouble();
			for(int i=0; i<nDepth; i++) t1[j][i] = scan.nextDouble();
			for(int i=0; i<nDepth; i++) t2[j][i] = scan.nextDouble();
		}
		// Return the result.
		ellip = new Ellip(phCode, minDelta, maxDelta, t0, t1, t2);
		return ellip;
	}
	
	/**
	 * Get the ellipticity correction data for the desired phase.
	 * 
	 * @param phCode Phase code
	 * @return Ellipticity data
	 */
	public Ellip findEllip(String phCode) {
		if(ellips == null) return null;
		else return ellips.get(phCode);
	}
	
	/**
	 * Reorganize the flags from ArrayLists of phases by group to 
	 * a TreeMap of flags by phase.
	 */
	private void makePhFlags() {
		String phCode, phGroup;

		// Search the phase groups for phases.
		for(int j=0; j<phGroups.size(); j++) {
			phGroup = phGroups.get(j).groupName;
			for(int i=0; i<phGroups.get(j).phases.size(); i++) {
				phCode = phGroups.get(j).phases.get(i);
				unTangle(phCode, phGroup);
				phFlags.put(phCode, new TtFlags(phGroup, compGroup(phGroup), 
						isRegional(phCode), isDepthPh(phCode), canUse(phCode), 
						isDisPh(phCode), ttStat, ellip, upEllip));
			}
		}
		// Search the auxiliary phase groups for phases.
		for(int j=0; j<auxGroups.size(); j++) {
			if(auxGroups.get(j) != null) {
				phGroup = auxGroups.get(j).groupName;
				for(int i=0; i<auxGroups.get(j).phases.size(); i++) {
					phCode = auxGroups.get(j).phases.get(i);
					unTangle(phCode, phGroup);
					phFlags.put(phCode, new TtFlags(phGroup, compGroup(phGroup), 
							isRegional(phCode), isDepthPh(phCode), canUse(phCode), 
							isDisPh(phCode), ttStat, ellip, upEllip));
				}
			}
		}
	}
	
	/**
	 * Do some fiddling to add the statistics and ellipticity correction.
	 * 
	 * @param phCode Phase code
	 * @param phGroup Group code
	 */
	private void unTangle(String phCode, String phGroup) {		
		// Get the travel-time statistics.
		ttStat = findStats(phCode);
		// The ellipticity is a little messier.
		ellip = findEllip(phCode);
		if(ellip == null) ellip = findEllip(phGroup);
		if(ellip == null) {
			if(phCode.equals("pwP")) ellip = findEllip("pP");
			else if(phCode.equals("PKPpre")) ellip = findEllip("PKPdf");
			else if(phGroup.contains("PKP")) ellip = findEllip(phGroup+"bc");
		}
		// Add up-going ellipticity corrections.
		if((phGroup.equals("P") || phGroup.equals("S")) && 
				!phCode.contains("dif")) {
			upEllip = findEllip(phGroup+"up");
		} else {
			upEllip = null;
		}
	}
	
	/**
	 * Get flags, etc. by phase code.
	 * 
	 * @param phCode Phase code
	 * @return Flags object
	 */
	public TtFlags findFlags(String phCode) {
		return phFlags.get(phCode);
	}
	
	/**
	 * Print phase flags.
	 */
	public void printFlags() {
		TtFlags flags;
		
		NavigableMap<String, TtFlags> map = phFlags.headMap("~", true);
		System.out.println("\n     Phase Flags:");
		for(@SuppressWarnings("rawtypes") Map.Entry entry : map.entrySet()) {
			flags = (TtFlags)entry.getValue();
			System.out.format("%8s: %8s %8s  flags = %5b %5b %5b %5b", 
					entry.getKey(), flags.phGroup, flags.auxGroup, flags.canUse, 
					flags.isRegional, flags.isDepth, flags.dis);
			if(flags.ttStat == null) System.out.print("   stats = null    ");
			else System.out.format("   stats = %-8s", flags.ttStat.phCode);
			if(flags.ellip == null) System.out.print(" ellip = null    ");
			else System.out.format(" ellip = %-8s", flags.ellip.phCode);
			if(flags.upEllip == null) System.out.println(" upEllip = null");
			else System.out.format(" upEllip = %-8s\n", flags.upEllip.phCode);
		}
	}
}