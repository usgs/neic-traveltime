package gov.usgs.traveltime;

import gov.usgs.traveltime.tables.MakeTables;
import gov.usgs.traveltime.tables.TablesUtil;
import gov.usgs.traveltime.tables.TauIntegralException;
import java.io.IOException;
import java.util.TreeMap;

/**
 * The TravelTimeSession class manage travel-time calculations. Calculations are managed in sessions
 * specific to an earth model and source depth.
 *
 * @author Ray Buland
 */
public class TravelTimeSession {
  String lastModel = "";
  TreeMap<String, AllBranchReference> modelData;
  MakeTables make;
  TravelTimeStatus status;
  AuxiliaryTTReference auxTT;
  AllBranchVolume allBrn;
  // Set up serialization.
  String serName; // Serialized file name for this model
  String[] fileNames; // Raw input file names for this model

  /**
   * Get a list of available Earth models.
   *
   * @return A list of available Earth model names
   */
  public String[] getAvailableModels() {
    return TauUtilities.getAvailableModels();
  }

  /**
   * Get a pointer to the auxiliary travel-time information.
   *
   * @return Auxiliary travel-time data
   */
  public AuxiliaryTTReference getAuxTT() {
    return auxTT;
  }

  /**
   * Initialize auxiliary data common to all models.
   *
   * @param readStats A boolean flag, if true, read the phase statistics
   * @param readEllipticity A boolean flag, if true, read the Ellipticity corrections
   * @param readTopo A boolean flag, if true, read the topography file
   * @param modelPath If not null, path to model files
   * @param serializedPath If not null, path to serialized files
   * @throws IOException On any read error
   * @throws ClassNotFoundException In input serialization is hosed
   */
  public TravelTimeSession(
      boolean readStats,
      boolean readEllipticity,
      boolean readTopo,
      String modelPath,
      String serializedPath)
      throws IOException, ClassNotFoundException {

    // Read in data common to all models.
    auxTT =
        new AuxiliaryTTReference(readStats, readEllipticity, readTopo, modelPath, serializedPath);
  }

  /**
   * Function to set up a "simple" travel-time session.
   *
   * @param earthModelName A String containing the earth model name
   * @param sourceDepth Source depth in kilometers
   * @param phases Array of phase use commands
   * @param returnAllPhases A boolean flag, if true, provide all phases
   * @param returnBackBranches A boolean flag, if true, return all back branches
   * @param tectonic A boolean flag, if true, map Pb and Sb onto Pg and Sg
   * @throws BadDepthException If the depth is out of range
   * @throws TauIntegralException If the tau integrals fail
   */
  public void newSession(
      String earthModelName,
      double sourceDepth,
      String[] phases,
      boolean returnAllPhases,
      boolean returnBackBranches,
      boolean tectonic)
      throws BadDepthException, TauIntegralException {

    setModel(earthModelName.toLowerCase());
    allBrn.newSession(sourceDepth, phases, returnAllPhases, returnBackBranches, tectonic);
  }

  /**
   * Function to set up a "complex" travel-time session.
   *
   * @param earthModelName A String containing the earth model name
   * @param sourceDepth Source depth in kilometers
   * @param phases Array of phase use commands
   * @param srcLat Source geographical latitude in degrees
   * @param srcLong Source longitude in degrees
   * @param returnAllPhases A boolean flag, if true, provide all phases
   * @param returnBackBranches A boolean flag, if true, return all back branches
   * @param tectonic A boolean flag, if true, map Pb and Sb onto Pg and Sg
   * @throws BadDepthException If the depth is out of range
   * @throws TauIntegralException If the tau integrals fail
   */
  public void newSession(
      String earthModelName,
      double sourceDepth,
      String[] phases,
      double srcLat,
      double srcLong,
      boolean returnAllPhases,
      boolean returnBackBranches,
      boolean tectonic)
      throws BadDepthException, TauIntegralException {

    setModel(earthModelName.toLowerCase());
    allBrn.newSession(
        srcLat, srcLong, sourceDepth, phases, returnAllPhases, returnBackBranches, tectonic);
  }

  /**
   * Function to get travel times for a "simple" session, i.e. just elevation and distance
   *
   * @param recieverElevation A double containing the reciever (station) elevation in kilometers
   * @param recieverDistance A double containing the source receiver distance desired in degrees
   * @return A TravelTime object containing the list of travel times
   */
  public TravelTime getTravelTimes(double recieverElevation, double recieverDistance) {
    return allBrn.getTravelTime(recieverElevation, recieverDistance);
  }

  /**
   * Function to get travel times for a "complex" session, i.e. elevation, latitude, and logitude
   *
   * @param recieverLatitude A double containing the reciever (station) geographic latitude in
   *     degrees
   * @param recieverLongitude A double containing the reciever (station) longitude in degrees
   * @param recieverElevation A double containing the reciever (station) elevation in kilometers
   * @param recieverDistance A double containing the source receiver distance desired in degrees
   * @param recieverAzimuth Receiver azimuth at the source in degrees
   * @return A TravelTime object containing the list of travel times
   */
  public TravelTime getTravelTimes(
      double recieverLatitude,
      double recieverLongitude,
      double recieverElevation,
      double recieverDistance,
      double recieverAzimuth) {
    return allBrn.getTravelTime(
        recieverLatitude, recieverLongitude, recieverElevation, recieverDistance, recieverAzimuth);
  }

  /**
   * Function to get plot data suitable for a travel-time chart.
   *
   * @param earthModelName A String containing the earth model name
   * @param sourceDepth A double containing the source depth in kilometers
   * @param phases An array of strings containing the phases to use
   * @param returnAllPhases A boolean flag, if true, provide all phases
   * @param returnBackBranches A boolean flag, if true, return all back branches
   * @param tectonic A boolean flag, if true, map Pb and Sb onto Pg and Sg
   * @param maximimDistance A double holding the maximum distance in degrees to generate
   * @param maxTime A double holding the maximum travel time in seconds to allow
   * @param distanceStep A double holding the distance increment in degrees for travel-time plots
   * @return A TravelTimePlot object containing the travel-time plot data
   * @throws BadDepthException If the depth is out of range
   * @throws TauIntegralException If the tau integrals fail
   */
  public TravelTimePlot getPlotTravelTimes(
      String earthModelName,
      double sourceDepth,
      String[] phases,
      boolean returnAllPhases,
      boolean returnBackBranches,
      boolean tectonic,
      double maximimDistance,
      double maxTime,
      double distanceStep)
      throws BadDepthException, TauIntegralException {
    setModel(earthModelName.toLowerCase());

    PlotData plotData = new PlotData(allBrn);
    plotData.makePlot(
        sourceDepth,
        phases,
        returnAllPhases,
        returnBackBranches,
        tectonic,
        maximimDistance,
        maxTime,
        distanceStep);
    return plotData.getPlotData();
  }

  /**
   * Function to set up for a new Earth model.
   *
   * @param earthModelName A String containing the earth model name
   */
  private void setModel(String earthModelName) {
    if (!earthModelName.equals(lastModel)) {
      lastModel = earthModelName;
      // Initialize model storage if necessary.
      if (modelData == null) {
        modelData = new TreeMap<String, AllBranchReference>();
      }

      // See if we know this model.
      AllBranchReference allRef = modelData.get(earthModelName);

      // If not, set it up.
      if (allRef == null) {
        if (modelChanged(earthModelName)) {
          if (TauUtilities.useFortranFiles) {
            ReadTau readTau = null;

            // Read the tables from the Fortran files.
            try {
              readTau = new ReadTau(earthModelName);
              readTau.readHeader(fileNames[0]);
              readTau.readTable(fileNames[1]);
            } catch (IOException e) {
              System.out.println("Unable to read Earth model " + earthModelName + ".");
              System.exit(202);
            }

            // Reorganize the reference data.
            try {
              allRef = new AllBranchReference(serName, readTau, auxTT);
            } catch (IOException e) {
              System.out.println(
                  "Unable to write Earth model " + earthModelName + " serialization file.");
            }
          } else {
            // Generate the tables.
            TablesUtil.deBugLevel = 1;
            make = new MakeTables(earthModelName);

            try {
              status = make.buildModel(fileNames[0], fileNames[1]);
            } catch (Exception e) {
              System.out.println(
                  "Unable to generate Earth model " + earthModelName + " (" + status + ").");
              e.printStackTrace();
              System.exit(202);
            }

            // Build the branch reference classes.
            try {
              allRef = make.fillInBranchReferenceData(serName, auxTT);
            } catch (IOException e) {
              System.out.println(
                  "Unable to write Earth model " + earthModelName + " serialization file.");
            }
          }
        } else {
          // If the model input hasn't changed, just serialize the model in.
          try {
            allRef = new AllBranchReference(serName, earthModelName, auxTT);
          } catch (ClassNotFoundException | IOException e) {
            System.out.println(
                "Unable to read Earth model " + earthModelName + " serialization file.");
            System.exit(202);
          }
        }

        allRef.dumpHeaderData();
        allRef.dumpModelParams('P', true);
        allRef.dumpModelParams('S', true);
        allRef.dumpBranchData(true);
        allRef.dumpBranchData("pS", true);
        allRef.dumpUpGoingData('P');
        allRef.dumpUpGoingData('S');
        modelData.put(earthModelName, allRef);
      }

      // Set up the (depth dependent) volatile part.
      allBrn = new AllBranchVolume(allRef);
      allBrn.dumpHeaderData();
      // allBrn.dumpBranchInformation("PnPn", false, false, true);
    }
  }

  /**
   * Determine if the input files have changed.
   *
   * @param earthModelName A String containing the earth model name
   * @return A boolean flag, if true, the input files have changed
   */
  private boolean modelChanged(String earthModelName) {
    // We need two files in either case.
    fileNames = new String[2];

    if (TauUtilities.useFortranFiles) {
      // Names for the Fortran files.
      serName = TauUtilities.getSerializedPath(earthModelName + "_for.ser");
      fileNames[0] = TauUtilities.getModelPath(earthModelName + ".hed");
      fileNames[1] = TauUtilities.getModelPath(earthModelName + ".tbl");
    } else {
      // Names for generating the model.
      serName = TauUtilities.getSerializedPath(earthModelName + "_gen.ser");
      fileNames[0] = TauUtilities.getModelPath("m" + earthModelName + ".mod");
      fileNames[1] = TauUtilities.getModelPath("phases.txt");
    }

    return FileChanged.isChanged(serName, fileNames);
  }

  /** Function to print phase groups. */
  public void printGroups() {
    auxTT.printGroups();
  }

  /** Function to print phase statistics. */
  public void printPhaseStatistics() {
    auxTT.printPhaseStatistics();
  }

  /** Function to print phase flags. */
  public void printFlags() {
    auxTT.printPhaseFlags();
  }

  /**
   * Function to print phase table.
   *
   * @param returnAllPhases A boolean flag, if false, only print "useful" phases.
   */
  public void logTable(boolean returnAllPhases) {
    allBrn.logTable(returnAllPhases);
  }

  public int getBranchCount(boolean returnAllPhases) {
    return (allBrn.getBranchCount(returnAllPhases));
  }

  /**
   * Function to print volatile phase branch information.
   *
   * @param full A boolean flag, if true, print the detailed branch specification as well
   * @param all A boolean flag, if true, print even more specifications
   * @param scientificNotation A boolean flag, if true, print in scientific notation
   * @param returnAllPhases A boolean flag, if false, only print "useful" crustal phases
   */
  public void printBranches(
      boolean full, boolean all, boolean scientificNotation, boolean returnAllPhases) {
    allBrn.dumpBranchInformation(full, all, scientificNotation, returnAllPhases);
  }

  /**
   * Function to print volatile phase branches that have at least one caustic.
   *
   * @param full A boolean flag, if true, print the detailed branch specification as well
   * @param all A boolean flag, if true, print even more specifications
   * @param scientificNotation A boolean flag, if true, print in scientific notation
   * @param returnAllPhases A boolean flag, if false, only print "useful" crustal phases
   */
  public void printCaustics(
      boolean full, boolean all, boolean scientificNotation, boolean returnAllPhases) {
    allBrn.dumpCaustics(full, all, scientificNotation, returnAllPhases);
  }

  /**
   * Function to print reference phase branch information.
   *
   * @param full A boolean flag, if true, print the detailed branch specification as well
   */
  public void printRefBranches(boolean full) {
    allBrn.getAllBranchReference().dumpBranchData(full);
  }
}
