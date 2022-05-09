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
 * The AuxiliaryTTReference class contains auxiliary data augmenting the basic travel-times. This
 * data is common to all models and only need be read once during the travel-time server
 * initialization. Auxiliary data includes phase groups (needed for association in the Locator) and
 * travel-time statistics. The statistics are used in the travel-time package for the arrival times
 * of add-on phases (e.g., PKPpre) and in the Locator for association and phase weighting.
 *
 * @author Ray Buland
 */
public class AuxiliaryTTReference {
  /** A PhaseGroup object holding the regional phase group */
  private final PhaseGroup regionalPhases;

  /** A PhaseGroup object holding the depth sensitive phase group */
  private final PhaseGroup depthPhases;

  /** A PhaseGroup object holding the to phasesbe down weighted */
  private final PhaseGroup downWeightPhases;

  /** A PhaseGroup object holding the phases that can be used for location */
  private final PhaseGroup locationPhases;

  /** A PhaseGroup object holding the phases that are useless phases according to NEIC analysts */
  private final PhaseGroup uselessPhases;

  /** An ArrayList of PhaseGroups containing the list of primary phase groups */
  private final ArrayList<PhaseGroup> primaryPhaseGroups;

  /** An ArrayList of PhaseGroups containing the list of auxiliary phase groups */
  private final ArrayList<PhaseGroup> auxiliaryPhaseGroups;

  /** A TreeMap holding the list of phase statistics */
  private final TreeMap<String, TravelTimeStatistics> travelTimeStatistics;

  /** A TreeMap holding the list of ellipticity corrections */
  private final TreeMap<String, Ellipticity> ellipticityCorrections;

  /** A TreeMap holding the list of phase flags */
  private final TreeMap<String, TravelTimeFlags> phaseFlags;

  /** A Topography object containing the topography for bounce points */
  private final Topography bouncePointTopography;

  /** A TravelTimeStatistics object containing the phase statistics */
  private TravelTimeStatistics phaseStatistics;

  /** An Ellipticity object containing the ellipticity correction */
  private Ellipticity ellipticityCorrection;

  /** An Ellipticity object containing the up-going ellipticity correction */
  Ellipticity upEllipticityCorrection;

  /** A String containing the name of the file to use during serialization */
  private String serializedFileName = "ttaux.ser";

  /** An array of Strings containing the auxiliary input data file names */
  private String[] dataFileNames = {"groups.txt", "ttstats.txt", "ellip.txt", "topo.dat"};

  /** A Scanner object used to read the auxiliary input data files */
  private Scanner fileReader;

  /** A boolean flag indicating whether the last phase group found was primary */
  private boolean isPrimaryGroup = false;

  /** An integer holding the number of ellipticity depths */
  private int numEllipticityDepths;

  /** A String used in reading auxiliary input data */
  private String nextCode;

  /**
   * Get the topography for bounce points
   *
   * @return An Topography object containing the topography for bounce points
   */
  public Topography getBouncePointTopography() {
    return bouncePointTopography;
  }

  /**
   * Get whether last phase group found was primary. The phase identification algorithm depends on
   * knowing which set of phase groups was found.
   *
   * @return A boolean flag, true if the last phase group found was primary
   */
  public boolean getIsPrimaryGroup() {
    return isPrimaryGroup;
  }

  /**
   * AuxiliaryTTReference constructor, Reads and organizes the auxiliary data. Note that for
   * convenience some processing is done on the travel-time statistics. This eliminates a stand
   * alone program that processed the raw (maintainable) statistics into an intermediate form for
   * the Locator. Only the phase groups are mandatory. If other information isn't read, the relevant
   * processing will simply not be done.
   *
   * @param readStats A boolean flag, if true, read the phase statistics
   * @param readEllipticity A boolean flag, if true, read the Ellipticity corrections
   * @param readTopo A boolean flag, if true, read the topography file
   * @param modelPath A String, if not null, contains the path to model files
   * @param serializedPath A String, if not null, contains the path to serialized files
   * @throws IOException If one of the file input data file reading operations fails
   * @throws ClassNotFoundException If the reading of the serialization input file fails
   */
  @SuppressWarnings("unchecked")
  public AuxiliaryTTReference(
      boolean readStats,
      boolean readEllipticity,
      boolean readTopo,
      String modelPath,
      String serializedPath)
      throws IOException, ClassNotFoundException {
    // set the model path
    if (modelPath != null) {
      TauUtilities.setDefaultModelPath(modelPath);
    }

    // set the serialization path
    if (serializedPath != null) {
      TauUtilities.setDefaulSerializedPath(serializedPath);
    }

    // Create absolute path names.
    String[] absolutePaths = new String[dataFileNames.length];
    for (int j = 0; j < dataFileNames.length; j++) {
      absolutePaths[j] = TauUtilities.getModelPath(dataFileNames[j]);
    }

    // If any of the raw input files have changed, regenerate the
    // serialized file.
    if (FileChanged.isChanged(TauUtilities.getSerializedPath(serializedFileName), absolutePaths)) {
      // Open and read the phase groups file.
      BufferedInputStream inGroup = new BufferedInputStream(new FileInputStream(absolutePaths[0]));
      fileReader = new Scanner(inGroup);

      // Prime the pump.
      nextCode = fileReader.next();

      // Handle local-regionalPhases phases separately.
      regionalPhases = readOnePhaseGroup();

      // Handle depthPhases phases separately.
      depthPhases = readOnePhaseGroup();

      // Handle down weighted phases separately.
      downWeightPhases = readOnePhaseGroup();

      // Handle used phases separately.
      locationPhases = readOnePhaseGroup();

      // Handle useless phases separately.
      uselessPhases = readOnePhaseGroup();

      // Handle "normal" groups.
      primaryPhaseGroups = new ArrayList<PhaseGroup>();
      auxiliaryPhaseGroups = new ArrayList<PhaseGroup>();
      readGroups();
      inGroup.close();

      // Open and read the travel-time statistics file.
      BufferedInputStream inStats = new BufferedInputStream(new FileInputStream(absolutePaths[1]));
      fileReader = new Scanner(inStats);
      travelTimeStatistics = new TreeMap<String, TravelTimeStatistics>();

      // Prime the pump.
      nextCode = fileReader.next();

      // Scan phase statistics until we run out.
      do {
        phaseStatistics = readOneStatisticsHeader();
        travelTimeStatistics.put(phaseStatistics.phaseCode, phaseStatistics);
        readOneStatisticsData(new TravelTimeStatisticsLinearFit(phaseStatistics));
      } while (fileReader.hasNext());
      inStats.close();

      // Open and read the Ellipticity correction file.
      BufferedInputStream inEllipticity =
          new BufferedInputStream(new FileInputStream(absolutePaths[2]));
      fileReader = new Scanner(inEllipticity);
      ellipticityCorrections = new TreeMap<String, Ellipticity>();
      EllipticityDepths ellipticityDepths = new EllipticityDepths();
      numEllipticityDepths = ellipticityDepths.getEllipticityDepths().length;

      do {
        ellipticityCorrection = readOneEllipticityData();
        ellipticityCorrections.put(ellipticityCorrection.getPhaseCode(), ellipticityCorrection);
      } while (fileReader.hasNext());
      inEllipticity.close();

      // Set up the topography data.
      bouncePointTopography = new Topography(absolutePaths[3]);

      // Write out the serialized file.
      FileOutputStream serializeOutput =
          new FileOutputStream(TauUtilities.getSerializedPath(serializedFileName));
      ObjectOutputStream objectOutput = new ObjectOutputStream(serializeOutput);

      // Wait for an exclusive lock for writing.
      FileLock lock = serializeOutput.getChannel().lock();

      //		System.out.println("AuxiliaryTTReference write lock: valid = "+lock.isValid()+
      //				" shared = "+lock.isShared());

      /*
       * The auxiliary data can be read and written very quickly, so for persistent
       * applications such as the travel time or location server, serialization is
       * not necessary.  However, if the travel times are needed for applications
       * that start and stop frequently, the serialization should save some set up
       * time.
       */
      objectOutput.writeObject(regionalPhases);
      objectOutput.writeObject(depthPhases);
      objectOutput.writeObject(downWeightPhases);
      objectOutput.writeObject(locationPhases);
      objectOutput.writeObject(uselessPhases);
      objectOutput.writeObject(primaryPhaseGroups);
      objectOutput.writeObject(auxiliaryPhaseGroups);
      objectOutput.writeObject(travelTimeStatistics);
      objectOutput.writeObject(ellipticityCorrections);
      objectOutput.writeObject(bouncePointTopography);

      if (lock.isValid()) {
        lock.release();
      }

      objectOutput.close();
      serializeOutput.close();
    } else {
      // Read in the serialized file.
      FileInputStream serializeInput =
          new FileInputStream(TauUtilities.getSerializedPath(serializedFileName));
      ObjectInputStream objectInput = new ObjectInputStream(serializeInput);

      // Wait for a shared lock for reading.
      FileLock lock = serializeInput.getChannel().lock(0, Long.MAX_VALUE, true);

      //		System.out.println("AuxiliaryTTReference read lock: valid = "+lock.isValid()+
      //				" shared = "+lock.isShared());
      regionalPhases = (PhaseGroup) objectInput.readObject();
      depthPhases = (PhaseGroup) objectInput.readObject();
      downWeightPhases = (PhaseGroup) objectInput.readObject();
      locationPhases = (PhaseGroup) objectInput.readObject();
      uselessPhases = (PhaseGroup) objectInput.readObject();
      primaryPhaseGroups = (ArrayList<PhaseGroup>) objectInput.readObject();
      auxiliaryPhaseGroups = (ArrayList<PhaseGroup>) objectInput.readObject();

      /*
       * Note that there is a theoretical inconsistency here.  If you ask for
       * Ellipticity, but not statistics, you have to read the statistics anyway.
       * Unfortunately, since the data is all final, once read, it can't easily
       * be unread.  In practice, the auxiliary data should be all or nothing
       * anyway (e.g., you need the corrections for location, but not for
       * association).
       */
      if (readStats || readEllipticity || readTopo) {
        travelTimeStatistics = (TreeMap<String, TravelTimeStatistics>) objectInput.readObject();

        if (readEllipticity || readTopo) {
          ellipticityCorrections = (TreeMap<String, Ellipticity>) objectInput.readObject();

          if (readTopo) {
            bouncePointTopography = (Topography) objectInput.readObject();
          } else {
            bouncePointTopography = null;
          }
        } else {
          ellipticityCorrections = null;
          bouncePointTopography = null;
        }
      } else {
        travelTimeStatistics = null;
        ellipticityCorrections = null;
        bouncePointTopography = null;
      }

      if (lock.isValid()) {
        lock.release();
      }

      objectInput.close();
      serializeInput.close();
    }

    // Rearrange group flags, phase flags and statistics and the
    // Ellipticity correction by phase.
    phaseFlags = new TreeMap<String, TravelTimeFlags>();
    makePhaseFlags();
  }

  /**
   * Function to read one phase group (i.e., one line in the phase group file).
   *
   * @return A PhaseGroup object containing the phase group just read
   */
  private PhaseGroup readOnePhaseGroup() {
    if (nextCode.contains(":")) {
      PhaseGroup group = new PhaseGroup(nextCode.substring(0, nextCode.indexOf(':')));
      nextCode = fileReader.next();

      while (!nextCode.contains(":") & !nextCode.contains("-")) {
        group.addPhase(nextCode);
        nextCode = fileReader.next();
      }

      return group;
    } else {
      if (fileReader.hasNext()) {
        nextCode = fileReader.next();
      }
      return null;
    }
  }

  /**
   * Function to read in all the "normal" phase groups. Note that they are read in pairs, typically
   * crust-mantle phase in the primary group and core phases in the auxiliary group. These pair-
   * wise groups are used for phase identification in the Locator.
   */
  private void readGroups() {
    do {
      // Groups are added to the ArrayLists as they are created.
      primaryPhaseGroups.add(readOnePhaseGroup());
      auxiliaryPhaseGroups.add(readOnePhaseGroup());
    } while (fileReader.hasNext());
  }

  /**
   * Function to find the group a given phase belongs to.
   *
   * @param phaseCode A String containing the phase code
   * @return A String cotnaining the phase group name
   */
  public String findGroup(String phaseCode) {
    // Search the primary phase group.
    for (int j = 0; j < primaryPhaseGroups.size(); j++) {
      PhaseGroup group = primaryPhaseGroups.get(j);
      for (int k = 0; k < group.getPhaseList().size(); k++) {
        if (phaseCode.equals(group.getPhaseList().get(k))) {
          isPrimaryGroup = true;

          return group.getGroupName();
        }
      }

      // Search the auxiliary phase group.
      group = auxiliaryPhaseGroups.get(j);
      if (group != null) {
        for (int k = 0; k < group.getPhaseList().size(); k++) {
          if (phaseCode.equals(group.getPhaseList().get(k))) {
            isPrimaryGroup = false;

            return group.getGroupName();
          }
        }
      }
    }

    // OK, that didn't work.  See if the phase is generic.
    for (int j = 0; j < primaryPhaseGroups.size(); j++) {
      // Try the primary group name.
      if (phaseCode.equals(primaryPhaseGroups.get(j).getGroupName())) {
        isPrimaryGroup = true;

        return primaryPhaseGroups.get(j).getGroupName();
      }

      // Try the auxiliary group name.
      if (auxiliaryPhaseGroups.get(j) != null) {
        if (phaseCode.equals(auxiliaryPhaseGroups.get(j).getGroupName())) {
          isPrimaryGroup = false;

          return auxiliaryPhaseGroups.get(j).getGroupName();
        }
      }
    }

    // OK, that didn't work.  Let's just give up.
    isPrimaryGroup = true;
    return "";
  }

  /**
   * Special version of findGroup function that deals with real world problems like a blank phase
   * code and automatic picks that are all identified as P.
   *
   * @param phaseCode A String containing the phase code
   * @param auto A boolean flag, true if the pick was done automatically
   * @returnA String cotnaining the phase group name
   */
  public String findGroup(String phaseCode, boolean auto) {
    isPrimaryGroup = true;

    if (phaseCode.equals("")) {
      return "Any";
    } else if (auto && phaseCode.equals("P")) {
      return "Reg";
    } else {
      return findGroup(phaseCode);
    }
  }

  /**
   * Function to find the complementary phase group. That is, if the phase group is primary, return
   * the associated auxiliary phase group and vice versa.
   *
   * @param phaseGroupName A String containing the phase group name
   * @return A String containing the complementary phase group name
   */
  public String findCompGroup(String phaseGroupName) {
    for (int j = 0; j < primaryPhaseGroups.size(); j++) {
      if (phaseGroupName.equals(primaryPhaseGroups.get(j).getGroupName())) {
        if (auxiliaryPhaseGroups.get(j) != null) {
          return auxiliaryPhaseGroups.get(j).getGroupName();
        } else {
          return null;
        }
      }

      if (auxiliaryPhaseGroups.get(j) != null) {
        if (phaseGroupName.equals(auxiliaryPhaseGroups.get(j).getGroupName())) {
          return primaryPhaseGroups.get(j).getGroupName();
        }
      }
    }

    return null;
  }

  /**
   * Function to check if this phase group can be used for earthquake location.
   *
   * @param phaseCode A String containing the phase code
   * @return A boolean flag, true if this phase can be used for earthquake location
   */
  public boolean usePhaseForLocation(String phaseCode) {
    for (int k = 0; k < locationPhases.getPhaseList().size(); k++) {
      if (phaseCode.equals(locationPhases.getPhaseList().get(k))) {
        return true;
      }
    }

    return false;
  }

  /**
   * Function to check if this phase group is useless for earthquake location (according to the NEIC
   * analysts). Most of these phases are crustal reverberations that end up in the coda of more
   * useful phases.
   *
   * @param phaseCode A String containing the phase code
   * @return A boolean flag, true if this phase is useless
   */
  public boolean isUselessPhase(String phaseCode) {
    for (int k = 0; k < uselessPhases.getPhaseList().size(); k++) {
      if (phaseCode.equals(uselessPhases.getPhaseList().get(k))) {
        return true;
      }
    }

    return false;
  }

  /**
   * Function to check if this is a local-regional phase.
   *
   * @param phaseCode A String containing the phase code
   * @return A boolean flag, true if the phase is a local-regional phase
   */
  public boolean isRegionalPhase(String phaseCode) {
    for (int k = 0; k < regionalPhases.getPhaseList().size(); k++) {
      if (phaseCode.equals(regionalPhases.getPhaseList().get(k))) {
        return true;
      }
    }

    return false;
  }

  /**
   * Function to check if this is a phase is a depth sensitive phase.
   *
   * @param phaseCode A String containing the phase code
   * @return A boolean flag, true if the phase is depth sensitive
   */
  public boolean isDepthPhase(String phaseCode) {
    for (int k = 0; k < depthPhases.getPhaseList().size(); k++) {
      if (phaseCode.equals(depthPhases.getPhaseList().get(k))) {
        return true;
      }
    }

    return false;
  }

  /**
   * See if this is a down weighted phase.
   *
   * @param phaseCode A String containing the phase code
   * @return True if the phase is to be down weighted
   */
  public boolean isDownWeightedPhase(String phaseCode) {
    for (int k = 0; k < downWeightPhases.getPhaseList().size(); k++) {
      if (phaseCode.equals(downWeightPhases.getPhaseList().get(k))) {
        return true;
      }
    }

    return false;
  }

  /** Function to print phase groups. */
  public void printGroups() {
    // Dump regionalPhases phases.
    regionalPhases.dumpGroup();
    System.out.println();

    // Dump depthPhases phases.
    depthPhases.dumpGroup();
    System.out.println();

    // Dump down weighted phases.
    downWeightPhases.dumpGroup();
    System.out.println();

    // Dump phases that can be used for location.
    locationPhases.dumpGroup();
    System.out.println();

    // Dump phases that are NEIC-useless.
    uselessPhases.dumpGroup();
    System.out.println();

    // Dump normal groups.
    for (int j = 0; j < primaryPhaseGroups.size(); j++) {
      primaryPhaseGroups.get(j).dumpGroup();
      if (auxiliaryPhaseGroups.get(j) != null) {
        auxiliaryPhaseGroups.get(j).dumpGroup();
      } else {
        System.out.println("    *null*");
      }
    }
  }

  /**
   * Function to read in the statistics header for one phase.
   *
   * @return TravelTimeStatistics object containing the statistics header
   */
  private TravelTimeStatistics readOneStatisticsHeader() {
    // Get the phase header.
    String phaseCode = nextCode;
    int minimumDistance = fileReader.nextInt();
    int maxiumumDistance = fileReader.nextInt();

    return new TravelTimeStatistics(phaseCode, minimumDistance, maxiumumDistance);
  }

  /**
   * Function to read in the statistics data for one phase. Rather than reading in the linear fits
   * as in the FORTRAN version, the raw statistics file is read and the fits are done on the fly.
   * This makes it easier to maintain the statistics as the utility program that did the fits
   * becomes redundant.
   *
   * @param fit LinearFit object
   */
  private void readOneStatisticsData(TravelTimeStatisticsLinearFit fit) {

    boolean linearFitBreak, spreadBreak, observabilityBreak;
    boolean done = false;

    // Scan for the phase linear fit, spread, and observability.
    do {
      int distance = fileReader.nextInt();
      double linearFit = fileReader.nextDouble();

      // Check for a linear fit break flag.
      if (fileReader.hasNextDouble()) {
        linearFitBreak = false;
      } else {
        linearFitBreak = true;

        if (!fileReader.next().equals("*")) {
          System.out.println(
              "read1Stat: warning--the next field is " + "neither a number nor an astrisk?");
        }
      }

      double spread = fileReader.nextDouble();

      // Check for a linear fit break flag.
      if (fileReader.hasNextDouble()) {
        spreadBreak = false;
      } else {
        spreadBreak = true;

        if (!fileReader.next().equals("*")) {
          System.out.println(
              "read1Stat: warning--the next field is " + "neither a number nor an astrisk?");
        }
      }

      double observability = fileReader.nextDouble();

      // Check for a linear fit break flag.
      if (fileReader.hasNextInt()) {
        observabilityBreak = false;
      } else {
        // This is especially fraught at the EOF.
        if (fileReader.hasNext()) {
          // If it's not an EOF there are still several possibilities.
          nextCode = fileReader.next();

          if (nextCode.equals("*")) {
            observabilityBreak = true;

            if (!fileReader.hasNextInt()) {
              done = true;

              if (fileReader.hasNext()) {
                nextCode = fileReader.next();
              } else {
                nextCode = "~";
              }
            }
          } else {
            observabilityBreak = false;
            done = true;
          }
        } else {
          observabilityBreak = false;
          done = true;
        }
      }

      fit.add(
          distance,
          linearFit,
          linearFitBreak,
          spread,
          spreadBreak,
          observability,
          observabilityBreak);
    } while (!done);

    // Crunch the linear fits.
    fit.fitAll();
    fit = null;
  }

  /**
   * Function to retrieve the statistics associated with the desired phase.
   *
   * @param phaseCode A String containing the phase code
   * @return A TravelTimeStatistics object containing the phase statistics
   */
  public TravelTimeStatistics findPhaseStatistics(String phaseCode) {
    if (travelTimeStatistics == null) {
      return null;
    } else {
      return travelTimeStatistics.get(phaseCode);
    }
  }

  /**
   * Function to get the phase bias.
   *
   * @param phaseStatistics A TravelTimeStatistics object holding the associated phase statistics
   * @param distance A double containing the distance in degrees
   * @return A double containing the phase bias in seconds at the given distance
   */
  public double getPhaseBias(TravelTimeStatistics phaseStatistics, double distance) {
    if (phaseStatistics == null) {
      return TauUtilities.DEFAULTTTBIAS;
    } else {
      return phaseStatistics.getPhaseBias(distance);
    }
  }

  /**
   * Get the phase spread.
   *
   * @param phaseStatistics Pointer to the associated phase statistics
   * @param distance A double containing the distance in degrees
   * @param upGoing A boolean flag, true if the phase is an up-going P or S
   * @return A double containing the phase spread in seconds at the given distance
   */
  public double getPhaseSpread(
      TravelTimeStatistics phaseStatistics, double distance, boolean upGoing) {
    if (phaseStatistics == null) {
      return TauUtilities.DEFAULTTTSPREAD;
    } else {
      return phaseStatistics.getPhaseSpread(distance, upGoing);
    }
  }

  /**
   * Get the phase observability.
   *
   * @param phaseStatistics Pointer to the associated phase statistics
   * @param distance A double containing the distance in degrees
   * @param upGoing A boolean flag, true if the phase is an up-going P or S
   * @return A double containing the relative phase observability at the given distance
   */
  public double getPhaseObservability(
      TravelTimeStatistics phaseStatistics, double distance, boolean upGoing) {
    if (phaseStatistics == null) {
      return TauUtilities.DEFAULTTTOBSERVABILITY;
    } else {
      return phaseStatistics.getPhaseObservability(distance, upGoing);
    }
  }

  /** Print phase statistics. */
  public void printPhaseStatistics() {
    NavigableMap<String, TravelTimeStatistics> map = travelTimeStatistics.headMap("~", true);

    System.out.println("\n     Phase Statistics:");
    for (@SuppressWarnings("rawtypes") Map.Entry entry : map.entrySet()) {
      TravelTimeStatistics stats = (TravelTimeStatistics) entry.getValue();
      stats.dumpStats();
    }
  }

  /**
   * Function to read in ellipticity correction data for one phase.
   *
   * @return An Ellipticity object containing the ellipicity correction data
   */
  private Ellipticity readOneEllipticityData() {
    // Read the header.
    String phaseCode = fileReader.next();
    int numDistances = fileReader.nextInt();
    double minimumDistance = fileReader.nextDouble();
    double maxiumumDistance = fileReader.nextDouble();

    // Allocate storage.
    double[][] t0 = new double[numDistances][numEllipticityDepths];
    double[][] t1 = new double[numDistances][numEllipticityDepths];
    double[][] t2 = new double[numDistances][numEllipticityDepths];

    // Read in the tau profiles.
    for (int j = 0; j < numDistances; j++) {
      @SuppressWarnings("unused")
      double distance = fileReader.nextDouble(); // The distance is cosmetic

      for (int i = 0; i < numEllipticityDepths; i++) {
        t0[j][i] = fileReader.nextDouble();
      }

      for (int i = 0; i < numEllipticityDepths; i++) {
        t1[j][i] = fileReader.nextDouble();
      }

      for (int i = 0; i < numEllipticityDepths; i++) {
        t2[j][i] = fileReader.nextDouble();
      }
    }

    // Return the result.
    return new Ellipticity(phaseCode, minimumDistance, maxiumumDistance, t0, t1, t2);
  }

  /**
   * Function to get the Ellipticity correction data for the desired phase.
   *
   * @param phaseCode A String containing the phase code
   * @return An Ellipticity object holding the ellipicity correction data
   */
  public Ellipticity findEllipticity(String phaseCode) {
    if (ellipticityCorrections == null) {
      return null;
    } else {
      return ellipticityCorrections.get(phaseCode);
    }
  }

  /**
   * FUnction to reorganize the phase flags from ArrayLists of phases by group into a TreeMap of
   * flags by phase.
   */
  private void makePhaseFlags() {
    // Search the phase groups for phases.
    for (int j = 0; j < primaryPhaseGroups.size(); j++) {
      String PhaseGroup = primaryPhaseGroups.get(j).getGroupName();
      for (int i = 0; i < primaryPhaseGroups.get(j).getPhaseList().size(); i++) {
        String phaseCode = primaryPhaseGroups.get(j).getPhaseList().get(i);

        unTangle(phaseCode, PhaseGroup);

        phaseFlags.put(
            phaseCode,
            new TravelTimeFlags(
                PhaseGroup,
                findCompGroup(PhaseGroup),
                isRegionalPhase(phaseCode),
                isDepthPhase(phaseCode),
                usePhaseForLocation(phaseCode),
                isDownWeightedPhase(phaseCode),
                phaseStatistics,
                ellipticityCorrection,
                upEllipticityCorrection));
      }
    }

    // Search the auxiliary phase groups for phases.
    for (int j = 0; j < auxiliaryPhaseGroups.size(); j++) {
      if (auxiliaryPhaseGroups.get(j) != null) {
        String PhaseGroup = auxiliaryPhaseGroups.get(j).getGroupName();
        for (int i = 0; i < auxiliaryPhaseGroups.get(j).getPhaseList().size(); i++) {
          String phaseCode = auxiliaryPhaseGroups.get(j).getPhaseList().get(i);

          unTangle(phaseCode, PhaseGroup);

          phaseFlags.put(
              phaseCode,
              new TravelTimeFlags(
                  PhaseGroup,
                  findCompGroup(PhaseGroup),
                  isRegionalPhase(phaseCode),
                  isDepthPhase(phaseCode),
                  usePhaseForLocation(phaseCode),
                  isDownWeightedPhase(phaseCode),
                  phaseStatistics,
                  ellipticityCorrection,
                  upEllipticityCorrection));
        }
      }
    }
  }

  /**
   * Do some fiddling to add the statistics and Ellipticity correction.
   *
   * @param phaseCode A String containing the phase code
   * @param phaseGroup A String containing the phase group code
   */
  private void unTangle(String phaseCode, String phaseGroup) {
    // Get the travel-time statistics.
    phaseStatistics = findPhaseStatistics(phaseCode);

    // The Ellipticity is a little messier.
    ellipticityCorrection = findEllipticity(phaseCode);
    if (ellipticityCorrection == null) {
      ellipticityCorrection = findEllipticity(phaseGroup);
    }

    if (ellipticityCorrection == null) {
      if (phaseCode.equals("pwP")) {
        ellipticityCorrection = findEllipticity("pP");
      } else if (phaseCode.equals("PKPpre")) {
        ellipticityCorrection = findEllipticity("PKPdf");
      } else if (phaseGroup.contains("PKP")) {
        ellipticityCorrection = findEllipticity(phaseGroup + "bc");
      }
    }

    // Add up-going Ellipticity corrections.
    if ((phaseGroup.equals("P") || phaseGroup.equals("S")) && !phaseCode.contains("dif")) {
      upEllipticityCorrection = findEllipticity(phaseGroup + "up");
    } else {
      upEllipticityCorrection = null;
    }
  }

  /**
   * Function to get flags, etc. by phase code.
   *
   * @param phaseCode A String containing the phase code
   * @return A TravelTimeFlags containing the travel time flags
   */
  public TravelTimeFlags findPhaseFlags(String phaseCode) {
    return phaseFlags.get(phaseCode);
  }

  /** Function to print phase flags. */
  public void printPhaseFlags() {
    NavigableMap<String, TravelTimeFlags> map = phaseFlags.headMap("~", true);
    System.out.println("\n     Phase Flags:");

    for (@SuppressWarnings("rawtypes") Map.Entry entry : map.entrySet()) {
      TravelTimeFlags flags = (TravelTimeFlags) entry.getValue();
      System.out.format(
          "%8s: %8s %8s  flags = %5b %5b %5b %5b",
          entry.getKey(),
          flags.PhaseGroup,
          flags.auxGroup,
          flags.canUse,
          flags.isRegionalPhase,
          flags.isDepth,
          flags.dis);

      if (flags.getPhaseStatistics() == null) {
        System.out.print("   stats = null    ");
      } else {
        System.out.format("   stats = %-8s", flags.getPhaseStatistics().phaseCode);
      }

      if (flags.getEllipticityCorrections() == null) {
        System.out.print(" Ellipticity = null    ");
      } else {
        System.out.format(" Ellipticity = %-8s", flags.getEllipticityCorrections().getPhaseCode());
      }

      if (flags.upEllipticity == null) {
        System.out.println(" upEllipticityCorrection = null");
      } else {
        System.out.format(" upEllipticityCorrection = %-8s\n", flags.upEllipticity.getPhaseCode());
      }
    }
  }
}
