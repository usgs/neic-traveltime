package gov.usgs.traveltime;

import gov.usgs.traveltime.tables.MakeTables;
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
  /** A String containing the name of the last earth model used */
  private String lastEarthModel = "";

  /** A TreeMap of Strings and AllBranchReference objects containing the model data */
  private TreeMap<String, AllBranchReference> modelDataList;

  /** A MakeTables object containing the travel-time table generation driver */
  private MakeTables tableGenerator;

  /** A TravelTimeStatus object containing the model result */
  private TravelTimeStatus modelStatus;

  /** A AuxiliaryTTReference object holding the model independent travel time auxiliary data */
  private AuxiliaryTTReference auxTTReference;

  /** An AllBranchVolume object containing the branch travel time data */
  private AllBranchVolume branchData;

  /** A String containing the path and name of the model serialization file */
  private String serializationFileName; // Serialized file name for this model

  /** An array of Strings contaiing the model input file names */
  private String[] modelFileList;

  /**
   * Get a list of available Earth models.
   *
   * @return An array of Strings containing the list of available Earth model names
   */
  public String[] getAvailableModels() {
    return TauUtilities.getAvailableModels();
  }

  /**
   * Get the model independent auxiliary data
   *
   * @return A AuxiliaryTTReference object holding the model independent auxiliary data
   */
  public AuxiliaryTTReference getAuxTTData() {
    return auxTTReference;
  }

  /**
   * TravelTimeSession Constructor, initializes auxiliary data common to all models.
   *
   * @param readStatistics A boolean flag, if true, read the phase statistics
   * @param readEllipticity A boolean flag, if true, read the Ellipticity corrections
   * @param readTopography A boolean flag, if true, read the topography file
   * @param modelPath A String, if not null, path to model files
   * @param serializedPath A String, if not null, path to serialized files
   * @throws IOException On any read error
   * @throws ClassNotFoundException In input serialization is hosed
   */
  public TravelTimeSession(
      boolean readStatistics,
      boolean readEllipticity,
      boolean readTopography,
      String modelPath,
      String serializedPath)
      throws IOException, ClassNotFoundException {

    // Read in data common to all models.
    auxTTReference =
        new AuxiliaryTTReference(
            readStatistics, readEllipticity, readTopography, modelPath, serializedPath);
  }

  /**
   * Function to set up a "simple" travel-time session.
   *
   * @param earthModelName A String containing the earth model name
   * @param sourceDepth A double containing the desired source depth in kilometers
   * @param phasesToUse An array of Strings containing the list desired phases to use
   * @param returnAllPhases A boolean flag, if true, provide all phases
   * @param returnBackBranches A boolean flag, if true, return all back branches
   * @param tectonic A boolean flag, if true, map Pb and Sb onto Pg and Sg
   * @throws BadDepthException If the depth is out of range
   * @throws TauIntegralException If the tau integrals fail
   */
  public void newSession(
      String earthModelName,
      double sourceDepth,
      String[] phasesToUse,
      boolean returnAllPhases,
      boolean returnBackBranches,
      boolean tectonic)
      throws BadDepthException, TauIntegralException {

    setModel(earthModelName.toLowerCase());
    branchData.newSession(sourceDepth, phasesToUse, returnAllPhases, returnBackBranches, tectonic);
  }

  /**
   * Function to set up a "complex" travel-time session.
   *
   * @param earthModelName A String containing the earth model name
   * @param sourceDepth A double containing the desired source depth in kilometers
   * @param phasesToUseAn array of Strings containing the list desired phases to use
   * @param sourceLatitude A double containing the source geographical latitude in degrees
   * @param sourceLongitude A double containing the source longitude in degrees
   * @param returnAllPhases A boolean flag, if true, provide all phases
   * @param returnBackBranches A boolean flag, if true, return all back branches
   * @param tectonic A boolean flag, if true, map Pb and Sb onto Pg and Sg
   * @throws BadDepthException If the depth is out of range
   * @throws TauIntegralException If the tau integrals fail
   */
  public void newSession(
      String earthModelName,
      double sourceDepth,
      String[] phasesToUse,
      double sourceLatitude,
      double sourceLongitude,
      boolean returnAllPhases,
      boolean returnBackBranches,
      boolean tectonic)
      throws BadDepthException, TauIntegralException {

    setModel(earthModelName.toLowerCase());
    branchData.newSession(
        sourceLatitude,
        sourceLongitude,
        sourceDepth,
        phasesToUse,
        returnAllPhases,
        returnBackBranches,
        tectonic);
  }

  /**
   * Function to get travel times for a "simple" session, i.e. just elevation and distance
   *
   * @param recieverElevation A double containing the reciever (station) elevation in kilometers
   * @param recieverDistance A double containing the source receiver distance desired in degrees
   * @return A TravelTime object containing the list of travel times
   */
  public TravelTime getTravelTimes(double recieverElevation, double recieverDistance) {
    return branchData.getTravelTime(recieverElevation, recieverDistance);
  }

  /**
   * Function to get travel times for a "complex" session, i.e. elevation, latitude, and logitude
   *
   * @param recieverLatitude A double containing the reciever (station) geographic latitude in
   *     degrees
   * @param recieverLongitude A double containing the reciever (station) longitude in degrees
   * @param recieverElevation A double containing the reciever (station) elevation in kilometers
   * @param recieverDistance A double containing the source receiver distance desired in degrees
   * @param recieverAzimuth A double containing the receiver azimuth at the source in degrees
   * @return A TravelTime object containing the list of travel times
   */
  public TravelTime getTravelTimes(
      double recieverLatitude,
      double recieverLongitude,
      double recieverElevation,
      double recieverDistance,
      double recieverAzimuth) {
    return branchData.getTravelTime(
        recieverLatitude, recieverLongitude, recieverElevation, recieverDistance, recieverAzimuth);
  }

  /**
   * Function to get plot data suitable for a travel-time chart.
   *
   * @param earthModelName A String containing the earth model name
   * @param sourceDepth A double containing the source depth in kilometers
   * @param phasesToUse An array of strings containing the phases to use
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
      String[] phasesToUse,
      boolean returnAllPhases,
      boolean returnBackBranches,
      boolean tectonic,
      double maximimDistance,
      double maxTime,
      double distanceStep)
      throws BadDepthException, TauIntegralException {
    setModel(earthModelName.toLowerCase());

    PlotData plotData = new PlotData(branchData);
    plotData.makePlot(
        sourceDepth,
        phasesToUse,
        returnAllPhases,
        returnBackBranches,
        tectonic,
        maximimDistance,
        maxTime,
        distanceStep);
    return plotData.getPlotData();
  }

  /**
   * Function to set up the session for a new Earth model.
   *
   * @param earthModelName A String containing the earth model name
   */
  private void setModel(String earthModelName) {
    if (!earthModelName.equals(lastEarthModel)) {
      lastEarthModel = earthModelName;

      // Initialize model storage if necessary.
      if (modelDataList == null) {
        modelDataList = new TreeMap<String, AllBranchReference>();
      }

      // See if we know this model.
      AllBranchReference branchDataReference = modelDataList.get(earthModelName);

      // If not, set it up.
      if (branchDataReference == null) {
        if (modelChanged(earthModelName)) {
          if (TauUtilities.useFortranFiles) {
            ReadTau readTau = null;

            // Read the tables from the Fortran files.
            try {
              readTau = new ReadTau(earthModelName);
              readTau.readHeader(modelFileList[0]);
              readTau.readTable(modelFileList[1]);
            } catch (IOException e) {
              System.out.println("Unable to read Earth model " + earthModelName + ".");
              System.exit(202);
            }

            // Reorganize the reference data.
            try {
              branchDataReference =
                  new AllBranchReference(serializationFileName, readTau, auxTTReference);
            } catch (IOException e) {
              System.out.println(
                  "Unable to write Earth model " + earthModelName + " serialization file.");
            }
          } else {
            // Generate the tables.
            // TablesUtil.deBugLevel = 1;
            tableGenerator = new MakeTables(earthModelName);

            try {
              modelStatus = tableGenerator.buildModel(modelFileList[0], modelFileList[1]);
            } catch (Exception e) {
              System.out.println(
                  "Unable to generate Earth model " + earthModelName + " (" + modelStatus + ").");
              e.printStackTrace();
              System.exit(202);
            }

            // Build the branch reference classes.
            try {
              branchDataReference =
                  tableGenerator.fillInBranchReferenceData(serializationFileName, auxTTReference);
            } catch (IOException e) {
              System.out.println(
                  "Unable to write Earth model " + earthModelName + " serialization file.");
            }
          }
        } else {
          // If the model input hasn't changed, just serialize the model in.
          try {
            branchDataReference =
                new AllBranchReference(serializationFileName, earthModelName, auxTTReference);
          } catch (ClassNotFoundException | IOException e) {
            System.out.println(
                "Unable to read Earth model " + earthModelName + " serialization file.");
            System.exit(202);
          }
        }

        // branchDataReference.dumpHeaderData();
        // branchDataReference.dumpModelParams('P', true);
        // branchDataReference.dumpModelParams('S', true);
        // branchDataReference.dumpBranchData(true);
        // branchDataReference.dumpBranchData("pS", true);
        // branchDataReference.dumpUpGoingData('P');
        // branchDataReference.dumpUpGoingData('S');
        modelDataList.put(earthModelName, branchDataReference);
      }

      // Set up the (depth dependent) volatile part.
      branchData = new AllBranchVolume(branchDataReference);
      // branchData.dumpHeaderData();
      // branchData.dumpBranchInformation("PnPn", false, false, true);
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
    modelFileList = new String[2];

    if (TauUtilities.useFortranFiles) {
      // Names for the Fortran files.
      serializationFileName = TauUtilities.getSerializedPath(earthModelName + "_for.ser");
      modelFileList[0] = TauUtilities.getModelPath(earthModelName + ".hed");
      modelFileList[1] = TauUtilities.getModelPath(earthModelName + ".tbl");
    } else {
      // Names for generating the model.
      serializationFileName = TauUtilities.getSerializedPath(earthModelName + "_gen.ser");
      modelFileList[0] = TauUtilities.getModelPath("m" + earthModelName + ".mod");
      modelFileList[1] = TauUtilities.getModelPath("phases.txt");
    }

    return FileChanged.isChanged(serializationFileName, modelFileList);
  }

  /** Function to print phase groups. */
  public void printGroups() {
    auxTTReference.printGroups();
  }

  /** Function to print phase statistics. */
  public void printPhaseStatistics() {
    auxTTReference.printPhaseStatistics();
  }

  /** Function to print phase flags. */
  public void printFlags() {
    auxTTReference.printPhaseFlags();
  }

  /**
   * Function to print phase table.
   *
   * @param returnAllPhases A boolean flag, if false, only print "useful" phases.
   */
  public void logTable(boolean returnAllPhases) {
    branchData.logTable(returnAllPhases);
  }

  public int getBranchCount(boolean returnAllPhases) {
    return (branchData.getBranchCount(returnAllPhases));
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
    branchData.dumpBranchInformation(full, all, scientificNotation, returnAllPhases);
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
    branchData.dumpCaustics(full, all, scientificNotation, returnAllPhases);
  }

  /**
   * Function to print reference phase branch information.
   *
   * @param full A boolean flag, if true, print the detailed branch specification as well
   */
  public void printRefBranches(boolean full) {
    branchData.getAllBranchReference().dumpBranchData(full);
  }
}
