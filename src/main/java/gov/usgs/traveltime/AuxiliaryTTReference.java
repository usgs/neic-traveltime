package gov.usgs.traveltime;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Scanner;
import java.util.TreeMap;

/**
 * Auxiliary data augmenting the basic travel-times. This data is common to all models and only need
 * be read once during the travel-time server initialization. Auxiliary data includes phase groups
 * (needed for association in the Locator) and travel-time statistics. The statistics are used in
 * the travel-time package for the arrival times of add-on phases (e.g., PKPpre) and in the Locator
 * for association and phase weighting.
 *
 * @author Ray Buland
 */
public class AuxiliaryTTReference {
  // Phase group storage.
  final PhaseGroup regional; // Regional phase group
  final PhaseGroup depth; // Depth sensitive phase group
  final PhaseGroup downWeight; // Phases to be down weighted
  final PhaseGroup canUse; // Phases that can be used for location
  final PhaseGroup chaff; // Useless phases according to NEIC analysts
  final ArrayList<PhaseGroup> PhaseGroups; // List of primary phase groups
  final ArrayList<PhaseGroup> auxGroups; // List of auxiliary phase groups
  // Phase statistics storage.
  final TreeMap<String, TravelTimeStatistics> TravelTimeStatisticss; // List of phase statistics
  // Ellipticity storage.
  final TreeMap<String, Ellipticity> Ellipticitys; // List of Ellipticity corrections
  // Flag storage by phase
  final TreeMap<String, TravelTimeFlags> phFlags; // Phase group information by phase
  // Topography.
  final Topography topoMap; // Topography for bounce points
  TravelTimeStatistics TravelTimeStatistics; // Phase statistics
  Ellipticity Ellipticity, upEllipticity; // Ellipticity correction(s)
  // Set up serialization.
  String serName = "ttaux.ser"; // Serialized file name
  String[] fileNames = {
    "groups.txt", "ttstats.txt", "ellip.txt", "topo.dat"
  }; // Raw input file names
  // Set up the reader.
  Scanner scan;
  boolean priGroup = false;
  int nDepth;
  String nextCode;

  /**
   * Read and organize auxiliary data. Note that for convenience some processing is done on the
   * travel-time statistics. This eliminates a stand alone program that processed the raw
   * (maintainable) statistics into an intermediate form for the Locator. Only the phase groups are
   * mandatory. If other information isn't read, the relevant processing will simply not be done.
   *
   * @param readStats If true, read the phase statistics
   * @param readEllipticity If true, read the Ellipticity corrections
   * @param readTopo If true, read the topography file
   * @param modelPath If not null, path to model files
   * @param serializedPath If not null, path to serialized files
   * @throws IOException If opens fail
   * @throws ClassNotFoundException Serialization input fails
   */
  @SuppressWarnings("unchecked")
  public AuxiliaryTTReference(
      boolean readStats,
      boolean readEllipticity,
      boolean readTopo,
      String modelPath,
      String serializedPath)
      throws IOException, ClassNotFoundException {
    String[] absNames;
    BufferedInputStream inGroup, inStats, inEllipticity;
    FileInputStream serIn;
    FileOutputStream serOut;
    ObjectInputStream objIn;
    ObjectOutputStream objOut;
    FileLock lock;
    EllipticityDepths eDepth;

    if (modelPath != null) {
      TauUtilities.modelPath = modelPath;
    }
    if (serializedPath != null) {
      TauUtilities.serializedPath = serializedPath;
    }

    // Create absolute path names.
    absNames = new String[fileNames.length];
    for (int j = 0; j < fileNames.length; j++) {
      absNames[j] = TauUtilities.model(fileNames[j]);
    }

    // If any of the raw input files have changed, regenerate the
    // serialized file.
    if (FileChanged.isChanged(TauUtilities.serialize(serName), absNames)) {
      // Open and read the phase groups file.
      inGroup = new BufferedInputStream(new FileInputStream(absNames[0]));
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
      PhaseGroups = new ArrayList<PhaseGroup>();
      auxGroups = new ArrayList<PhaseGroup>();
      readGroups();
      inGroup.close();

      // Open and read the travel-time statistics file.
      inStats = new BufferedInputStream(new FileInputStream(absNames[1]));
      scan = new Scanner(inStats);
      TravelTimeStatisticss = new TreeMap<String, TravelTimeStatistics>();
      // Prime the pump.
      nextCode = scan.next();
      // Scan phase statistics until we run out.
      do {
        TravelTimeStatistics = read1StatHead();
        TravelTimeStatisticss.put(TravelTimeStatistics.phCode, TravelTimeStatistics);
        read1StatData(new TravelTimeStatisticsLinearFit(TravelTimeStatistics));
      } while (scan.hasNext());
      inStats.close();

      // Open and read the Ellipticity correction file.
      inEllipticity = new BufferedInputStream(new FileInputStream(absNames[2]));
      scan = new Scanner(inEllipticity);
      Ellipticitys = new TreeMap<String, Ellipticity>();
      eDepth = new EllipticityDepths();
      nDepth = eDepth.EllipticityDepths.length;
      do {
        Ellipticity = read1Ellipticity();
        Ellipticitys.put(Ellipticity.phCode, Ellipticity);
      } while (scan.hasNext());
      inEllipticity.close();

      // Set up the topography data.
      topoMap = new Topography(absNames[3]);

      // Write out the serialized file.
      serOut = new FileOutputStream(TauUtilities.serialize(serName));
      objOut = new ObjectOutputStream(serOut);
      // Wait for an exclusive lock for writing.
      lock = serOut.getChannel().lock();
      //		System.out.println("AuxiliaryTTReference write lock: valid = "+lock.isValid()+
      //				" shared = "+lock.isShared());
      /*
       * The auxiliary data can be read and written very quickly, so for persistent
       * applications such as the travel time or location server, serialization is
       * not necessary.  However, if the travel times are needed for applications
       * that start and stop frequently, the serialization should save some set up
       * time.
       */
      objOut.writeObject(regional);
      objOut.writeObject(depth);
      objOut.writeObject(downWeight);
      objOut.writeObject(canUse);
      objOut.writeObject(chaff);
      objOut.writeObject(PhaseGroups);
      objOut.writeObject(auxGroups);
      objOut.writeObject(TravelTimeStatisticss);
      objOut.writeObject(Ellipticitys);
      objOut.writeObject(topoMap);
      if (lock.isValid()) lock.release();
      objOut.close();
      serOut.close();
    } else {
      // Read in the serialized file.
      serIn = new FileInputStream(TauUtilities.serialize(serName));
      objIn = new ObjectInputStream(serIn);
      // Wait for a shared lock for reading.
      lock = serIn.getChannel().lock(0, Long.MAX_VALUE, true);
      //		System.out.println("AuxiliaryTTReference read lock: valid = "+lock.isValid()+
      //				" shared = "+lock.isShared());
      regional = (PhaseGroup) objIn.readObject();
      depth = (PhaseGroup) objIn.readObject();
      downWeight = (PhaseGroup) objIn.readObject();
      canUse = (PhaseGroup) objIn.readObject();
      chaff = (PhaseGroup) objIn.readObject();
      PhaseGroups = (ArrayList<PhaseGroup>) objIn.readObject();
      auxGroups = (ArrayList<PhaseGroup>) objIn.readObject();
      /*
       * Note that there is a theoretical inconsistency here.  If you ask for
       * Ellipticity, but not statistics, you have to read the statistics anyway.
       * Unfortunately, since the data is all final, once read, it can't easily
       * be unread.  In practice, the auxiliary data should be all or nothing
       * anyway (e.g., you need the corrections for location, but not for
       * association).
       */
      if (readStats || readEllipticity || readTopo) {
        TravelTimeStatisticss = (TreeMap<String, TravelTimeStatistics>) objIn.readObject();
        if (readEllipticity || readTopo) {
          Ellipticitys = (TreeMap<String, Ellipticity>) objIn.readObject();
          if (readTopo) {
            topoMap = (Topography) objIn.readObject();
          } else {
            topoMap = null;
          }
        } else {
          Ellipticitys = null;
          topoMap = null;
        }
      } else {
        TravelTimeStatisticss = null;
        Ellipticitys = null;
        topoMap = null;
      }
      if (lock.isValid()) lock.release();
      objIn.close();
      serIn.close();
    }

    // Rearrange group flags, phase flags and statistics and the
    // Ellipticity correction by phase.
    phFlags = new TreeMap<String, TravelTimeFlags>();
    makePhFlags();
  }

  /**
   * Read one phase group (i.e., one line in the phase group file).
   *
   * @return Phase group just read
   */
  private PhaseGroup read1Group() {
    if (nextCode.contains(":")) {
      PhaseGroup group = new PhaseGroup(nextCode.substring(0, nextCode.indexOf(':')));
      nextCode = scan.next();
      while (!nextCode.contains(":") & !nextCode.contains("-")) {
        group.addPhase(nextCode);
        nextCode = scan.next();
      }
      return group;
    } else {
      if (scan.hasNext()) nextCode = scan.next();
      return null;
    }
  }

  /**
   * Read in all the "normal" phase groups. Note that they are read in pairs, typically crust-mantle
   * phase in the primary group and core phases in the auxiliary group. These pair- wise groups are
   * used for phase identification in the Locator.
   *
   * @param print List the auxiliary data as it's read if true
   */
  private void readGroups() {
    do {
      // Groups are added to the ArrayLists as they are created.
      PhaseGroups.add(read1Group());
      auxGroups.add(read1Group());
    } while (scan.hasNext());
  }

  /**
   * Find the group a phase belongs to.
   *
   * @param phase Phase code
   * @return Phase group name
   */
  public String findGroup(String phase) {
    PhaseGroup group;
    // Search the primary phase group.
    for (int j = 0; j < PhaseGroups.size(); j++) {
      group = PhaseGroups.get(j);
      for (int k = 0; k < group.phases.size(); k++) {
        if (phase.equals(group.phases.get(k))) {
          priGroup = true;
          return group.groupName;
        }
      }
      // Search the auxiliary phase group.
      group = auxGroups.get(j);
      if (group != null) {
        for (int k = 0; k < group.phases.size(); k++) {
          if (phase.equals(group.phases.get(k))) {
            priGroup = false;
            return group.groupName;
          }
        }
      }
    }
    // OK, that didn't work.  See if the phase is generic.
    for (int j = 0; j < PhaseGroups.size(); j++) {
      // Try the primary group name.
      if (phase.equals(PhaseGroups.get(j).groupName)) {
        priGroup = true;
        return PhaseGroups.get(j).groupName;
      }
      // Try the auxiliary group name.
      if (auxGroups.get(j) != null) {
        if (phase.equals(auxGroups.get(j).groupName)) {
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
   * Special version of findGroup that deals with real world problems like a blank phase code and
   * automatic picks that are all identified as P.
   *
   * @param phase Phase code
   * @param auto True if the pick was done automatically
   * @return Phase group name
   */
  public String findGroup(String phase, boolean auto) {
    priGroup = true;
    if (phase.equals("")) return "Any";
    else if (auto && phase.equals("P")) return "Reg";
    else return findGroup(phase);
  }

  /**
   * The phase identification algorithm depends on knowing which set of phase groups was found.
   *
   * @return True if the last phase group found was primary
   */
  public boolean isPrimary() {
    return priGroup;
  }

  /**
   * Find the complementary phase group. That is, if the phase group is primary, return the
   * associated auxiliary phase group and vice versa.
   *
   * @param groupName Phase group name
   * @return Complementary phase group name
   */
  public String compGroup(String groupName) {
    for (int j = 0; j < PhaseGroups.size(); j++) {
      if (groupName.equals(PhaseGroups.get(j).groupName)) {
        if (auxGroups.get(j) != null) return auxGroups.get(j).groupName;
        else return null;
      }
      if (auxGroups.get(j) != null) {
        if (groupName.equals(auxGroups.get(j).groupName)) {
          return PhaseGroups.get(j).groupName;
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
    for (int k = 0; k < canUse.phases.size(); k++) {
      if (phase.equals(canUse.phases.get(k))) {
        return true;
      }
    }
    return false;
  }

  /**
   * See if this phase group is useless for earthquake location (according to the NEIC analysts).
   * Most of these phases are crustal reverberations that end up in the coda of more useful phases.
   *
   * @param phase Phase name
   * @return True if this phase is useless
   */
  public boolean isChaff(String phase) {
    for (int k = 0; k < chaff.phases.size(); k++) {
      if (phase.equals(chaff.phases.get(k))) {
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
    for (int k = 0; k < regional.phases.size(); k++) {
      if (phase.equals(regional.phases.get(k))) {
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
    for (int k = 0; k < depth.phases.size(); k++) {
      if (phase.equals(depth.phases.get(k))) {
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
    for (int k = 0; k < downWeight.phases.size(); k++) {
      if (phase.equals(downWeight.phases.get(k))) {
        return true;
      }
    }
    return false;
  }

  /** Print phase groups. */
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
    for (int j = 0; j < PhaseGroups.size(); j++) {
      PhaseGroups.get(j).dumpGroup();
      if (auxGroups.get(j) != null) auxGroups.get(j).dumpGroup();
      else System.out.println("    *null*");
    }
  }

  /**
   * Read in the statistics header for one phase.
   *
   * @return Statistics object
   */
  private TravelTimeStatistics read1StatHead() {
    String phCode;
    int minDelta, maxDelta;
    TravelTimeStatistics TravelTimeStatistics;

    // Get the phase header.
    phCode = nextCode;
    minDelta = scan.nextInt();
    maxDelta = scan.nextInt();
    TravelTimeStatistics = new TravelTimeStatistics(phCode, minDelta, maxDelta);
    return TravelTimeStatistics;
  }

  /**
   * Read in the statistics data for one phase. Rather than reading in the linear fits as in the
   * FORTRAN version, the raw statistics file is read and the fits are done on the fly. This makes
   * it easier to maintain the statistics as the utility program that did the fits becomes
   * redundant.
   *
   * @param fit LinearFit object
   */
  private void read1StatData(TravelTimeStatisticsLinearFit fit) {
    int delta;
    double res, spd, obs;
    boolean resBrk, spdBrk, obsBrk;
    boolean done;

    done = false;

    // Scan for the phase bias, spread, and observability.
    do {
      delta = scan.nextInt();
      res = scan.nextDouble();
      // Check for a linear fit break flag.
      if (scan.hasNextDouble()) resBrk = false;
      else {
        resBrk = true;
        if (!scan.next().equals("*")) {
          System.out.println(
              "read1Stat: warning--the next field is " + "neither a number nor an astrisk?");
        }
      }
      spd = scan.nextDouble();
      // Check for a linear fit break flag.
      if (scan.hasNextDouble()) spdBrk = false;
      else {
        spdBrk = true;
        if (!scan.next().equals("*")) {
          System.out.println(
              "read1Stat: warning--the next field is " + "neither a number nor an astrisk?");
        }
      }
      obs = scan.nextDouble();
      // Check for a linear fit break flag.
      if (scan.hasNextInt()) obsBrk = false;
      else {
        // This is especially fraught at the EOF.
        if (scan.hasNext()) {
          // If it's not an EOF there are still several possibilities.
          nextCode = scan.next();
          if (nextCode.equals("*")) {
            obsBrk = true;
            if (!scan.hasNextInt()) {
              done = true;
              if (scan.hasNext()) {
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
    } while (!done);

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
  public TravelTimeStatistics findStats(String phase) {
    if (TravelTimeStatisticss == null) return null;
    else return TravelTimeStatisticss.get(phase);
  }

  /**
   * get the phase bias.
   *
   * @param TravelTimeStatistics Pointer to the associated phase statistics
   * @param delta Distance in degrees
   * @return Bias in seconds at distance delta
   */
  public double getBias(TravelTimeStatistics TravelTimeStatistics, double delta) {
    if (TravelTimeStatistics == null) return TauUtilities.DEFBIAS;
    else return TravelTimeStatistics.getBias(delta);
  }

  /**
   * Get the phase spread.
   *
   * @param TravelTimeStatistics Pointer to the associated phase statistics
   * @param delta Distance in degrees
   * @param upGoing True if the phase is an up-going P or S
   * @return Spread in seconds at distance delta
   */
  public double getSpread(
      TravelTimeStatistics TravelTimeStatistics, double delta, boolean upGoing) {
    if (TravelTimeStatistics == null) return TauUtilities.DEFSPREAD;
    else return TravelTimeStatistics.getSpread(delta, upGoing);
  }

  /**
   * Get the phase observability.
   *
   * @param TravelTimeStatistics Pointer to the associated phase statistics
   * @param delta Distance in degrees
   * @param upGoing True if the phase is an up-going P or S
   * @return Relative observability at distance delta
   */
  public double getObserv(
      TravelTimeStatistics TravelTimeStatistics, double delta, boolean upGoing) {
    if (TravelTimeStatistics == null) return TauUtilities.DEFOBSERV;
    else return TravelTimeStatistics.getObserv(delta, upGoing);
  }

  /** Print phase statistics. */
  public void printStats() {
    TravelTimeStatistics TravelTimeStatistics;

    NavigableMap<String, TravelTimeStatistics> map = TravelTimeStatisticss.headMap("~", true);
    System.out.println("\n     Phase Statistics:");
    for (@SuppressWarnings("rawtypes") Map.Entry entry : map.entrySet()) {
      TravelTimeStatistics = (TravelTimeStatistics) entry.getValue();
      TravelTimeStatistics.dumpStats();
    }
  }

  /**
   * Read in Ellipticity correction data for one phase.
   *
   * @return Ellipticity object
   */
  private Ellipticity read1Ellipticity() {
    String phCode;
    int nDelta;
    @SuppressWarnings("unused")
    double delta;
    double minDelta, maxDelta;
    double[][] t0, t1, t2;
    Ellipticity Ellipticity;

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
    for (int j = 0; j < nDelta; j++) {
      delta = scan.nextDouble(); // The distance is cosmetic
      for (int i = 0; i < nDepth; i++) t0[j][i] = scan.nextDouble();
      for (int i = 0; i < nDepth; i++) t1[j][i] = scan.nextDouble();
      for (int i = 0; i < nDepth; i++) t2[j][i] = scan.nextDouble();
    }
    // Return the result.
    Ellipticity = new Ellipticity(phCode, minDelta, maxDelta, t0, t1, t2);
    return Ellipticity;
  }

  /**
   * Get the Ellipticity correction data for the desired phase.
   *
   * @param phCode Phase code
   * @return Ellipticity data
   */
  public Ellipticity findEllipticity(String phCode) {
    if (Ellipticitys == null) return null;
    else return Ellipticitys.get(phCode);
  }

  /** Reorganize the flags from ArrayLists of phases by group to a TreeMap of flags by phase. */
  private void makePhFlags() {
    String phCode, PhaseGroup;

    // Search the phase groups for phases.
    for (int j = 0; j < PhaseGroups.size(); j++) {
      PhaseGroup = PhaseGroups.get(j).groupName;
      for (int i = 0; i < PhaseGroups.get(j).phases.size(); i++) {
        phCode = PhaseGroups.get(j).phases.get(i);
        unTangle(phCode, PhaseGroup);
        phFlags.put(
            phCode,
            new TravelTimeFlags(
                PhaseGroup,
                compGroup(PhaseGroup),
                isRegional(phCode),
                isDepthPh(phCode),
                canUse(phCode),
                isDisPh(phCode),
                TravelTimeStatistics,
                Ellipticity,
                upEllipticity));
      }
    }
    // Search the auxiliary phase groups for phases.
    for (int j = 0; j < auxGroups.size(); j++) {
      if (auxGroups.get(j) != null) {
        PhaseGroup = auxGroups.get(j).groupName;
        for (int i = 0; i < auxGroups.get(j).phases.size(); i++) {
          phCode = auxGroups.get(j).phases.get(i);
          unTangle(phCode, PhaseGroup);
          phFlags.put(
              phCode,
              new TravelTimeFlags(
                  PhaseGroup,
                  compGroup(PhaseGroup),
                  isRegional(phCode),
                  isDepthPh(phCode),
                  canUse(phCode),
                  isDisPh(phCode),
                  TravelTimeStatistics,
                  Ellipticity,
                  upEllipticity));
        }
      }
    }
  }

  /**
   * Do some fiddling to add the statistics and Ellipticity correction.
   *
   * @param phCode Phase code
   * @param PhaseGroup Group code
   */
  private void unTangle(String phCode, String PhaseGroup) {
    // Get the travel-time statistics.
    TravelTimeStatistics = findStats(phCode);
    // The Ellipticity is a little messier.
    Ellipticity = findEllipticity(phCode);
    if (Ellipticity == null) Ellipticity = findEllipticity(PhaseGroup);
    if (Ellipticity == null) {
      if (phCode.equals("pwP")) Ellipticity = findEllipticity("pP");
      else if (phCode.equals("PKPpre")) Ellipticity = findEllipticity("PKPdf");
      else if (PhaseGroup.contains("PKP")) Ellipticity = findEllipticity(PhaseGroup + "bc");
    }
    // Add up-going Ellipticity corrections.
    if ((PhaseGroup.equals("P") || PhaseGroup.equals("S")) && !phCode.contains("dif")) {
      upEllipticity = findEllipticity(PhaseGroup + "up");
    } else {
      upEllipticity = null;
    }
  }

  /**
   * Get flags, etc. by phase code.
   *
   * @param phCode Phase code
   * @return Flags object
   */
  public TravelTimeFlags findFlags(String phCode) {
    return phFlags.get(phCode);
  }

  /** Print phase flags. */
  public void printFlags() {
    TravelTimeFlags flags;

    NavigableMap<String, TravelTimeFlags> map = phFlags.headMap("~", true);
    System.out.println("\n     Phase Flags:");
    for (@SuppressWarnings("rawtypes") Map.Entry entry : map.entrySet()) {
      flags = (TravelTimeFlags) entry.getValue();
      System.out.format(
          "%8s: %8s %8s  flags = %5b %5b %5b %5b",
          entry.getKey(),
          flags.PhaseGroup,
          flags.auxGroup,
          flags.canUse,
          flags.isRegional,
          flags.isDepth,
          flags.dis);
      if (flags.TravelTimeStatistics == null) System.out.print("   stats = null    ");
      else System.out.format("   stats = %-8s", flags.TravelTimeStatistics.phCode);
      if (flags.Ellipticity == null) System.out.print(" Ellipticity = null    ");
      else System.out.format(" Ellipticity = %-8s", flags.Ellipticity.phCode);
      if (flags.upEllipticity == null) System.out.println(" upEllipticity = null");
      else System.out.format(" upEllipticity = %-8s\n", flags.upEllipticity.phCode);
    }
  }
}
