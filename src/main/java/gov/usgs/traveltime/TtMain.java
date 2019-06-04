package gov.usgs.traveltime;

import gov.usgs.traveltime.session.*;
import java.io.IOException;

/**
 * Sample driver for the travel-time package.
 *
 * @author Ray Buland
 */
public class TtMain {
  /** A String containing the locator version */
  public static final String VERSION = "v0.1.0";

  /** A String containing the argument for specifying the model file path. */
  public static final String MODELPATH_ARGUMENT = "--modelPath=";

  /** A String containing the argument for specifying the earth model. */
  public static final String EARTHMODEL_ARGUMENT = "--earthModel=";

  /** A String containing the argument for specifying the source depth. */
  public static final String SOURCEDEPTH_ARGUMENT = "--sourceDepth=";

  /** A String containing the argument for requesting the locator version. */
  public static final String VERSION_ARGUMENT = "--version";

  /** A String containing the argument for specifying a log file path. */
  public static final String LOGPATH_ARGUMENT = "--logPath=";

  /** A String containing the argument for specifying a log level. */
  public static final String LOGLEVEL_ARGUMENT = "--logLevel=";

  /**
   * Test main program for the travel-time package.
   *
   * @param args Command line arguments (not used)
   * @throws Exception If the travel-time setup fails
   */
  public static void main(String[] args) throws Exception {
    if (args == null || args.length == 0) {
      System.out.println(
          "Usage:\nneic-traveltime --modelPath=[model path] --earthModel=[earth model] "
              + "\n\t--sourceDepth=[source depth] --logPath=[log file path] --logLevel=[logging level]");
      System.exit(1);
    }

    String modelPath = null;
    String earthModel = "ak135";
    double sourceDepth = 10.0;
    String logPath = "./";
    String logLevel = "INFO";

    // process arguments
    StringBuffer argumentList = new StringBuffer();
    for (String arg : args) {
      // save arguments for logging
      argumentList.append(arg).append(" ");

      if (arg.startsWith(MODELPATH_ARGUMENT)) {
        // get model path
        modelPath = arg.replace(MODELPATH_ARGUMENT, "");
      } else if (arg.startsWith(EARTHMODEL_ARGUMENT)) {
        // get earth model
        earthModel = arg.replace(EARTHMODEL_ARGUMENT, "");
      } else if (arg.startsWith(SOURCEDEPTH_ARGUMENT)) {
        // get source depth
        sourceDepth = Double.parseDouble(arg.replace(SOURCEDEPTH_ARGUMENT, ""));
      } else if (arg.startsWith(LOGPATH_ARGUMENT)) {
        // get log path
        logPath = arg.replace(LOGPATH_ARGUMENT, "");
      } else if (arg.startsWith(LOGLEVEL_ARGUMENT)) {
        // get log level
        logLevel = arg.replace(LOGLEVEL_ARGUMENT, "");
      } else if (arg.equals(VERSION_ARGUMENT)) {
        // print version
        System.err.println("neic-traveltime");
        System.err.println(VERSION);
        System.exit(0);
      }
    }

    String[] phList = null;
    //	String[] phList = {"PKP", "SKP"};
    // Flags for ttlist.
    boolean returnAllPhases = false;
    boolean returnBackBranches = false;
    boolean rstt = false;
    boolean tectonic = false;
    // Simulate a simple travel time request.
    double[] delta = {1d, 2d, 3d, 5d, 10d, 20d, 40d, 60d, 90d, 120d, 150d, 180d};
    //	double[] delta = {40d};
    double elev = 0.0d;
    // Simulate a complex travel time request.
    /*	double sourceLat = 50.2075d;
    double sourceLon = -114.8603d;
    double staLat = 49.0586d;
    double staLon = -113.9115d;
    double azimuth = 151.4299d; */
    // Classes we will need.
    boolean local = true;
    TTSessionLocal ttLocal = null;
    TTSessionPool ttPool = null;
    TTSession ttServer = null;
    TTime ttList;
    //	TtPlot ttPlot;

    if (local) {
      // Initialize the local travel-time manager.
      ttLocal = new TTSessionLocal(true, true, true, modelPath);

      // Generate a list of available Earth models.
      String[] models = TauUtil.availableModels();
      if (models.length > 0) {
        System.out.println("Available Earth models:");
        for (int j = 0; j < models.length; j++) {
          System.out.println("\t" + models[j]);
        }
      } else {
        System.out.println("There are no available Earth models?");
      }
    }

    //	TauUtil.noCorr = true;
    try {
      // Set up a simple session.
      //		TablesUtil.deBugLevel = 3;
      if (local) {
        ttLocal.newSession(
            earthModel, sourceDepth, phList, returnAllPhases, returnBackBranches, tectonic, rstt);
        // Set up a complex session.
        //			ttLocal.newSession(earthModel, sourceDepth, phList, sourceLat,
        //					sourceLon, returnAllPhases, returnBackBranches, tectonic, rstt);
        //			ttLocal.printRefBranches(false);
        //			ttLocal.printBranches(false, false, false, returnAllPhases);
        //			ttLocal.printCaustics(false, false, false, returnAllPhases);
        ttLocal.printTable(returnAllPhases);
      } else {
        ttServer =
            TTSessionPool.getTravelTimeSession(
                earthModel,
                sourceDepth,
                phList,
                returnAllPhases,
                returnBackBranches,
                tectonic,
                rstt,
                false);
      }

      for (int j = 0; j < delta.length; j++) {
        // Get the simple travel times.
        if (local) {
          ttList = ttLocal.getTT(elev, delta[j]);
        } else {
          ttList = ttServer.getTT(delta[j], elev);
        }
        // Get the complex travel times.
        //			ttList = ttLocal.getTT(staLat, staLon, elev, delta, azimuth);
        // Print them.
        ttList.print();
      }

      //		ttPlot = ttLocal.getPlot(earthModel, sourceDepth, phList, returnAllPhases,
      //				returnBackBranches, tectonic);
      //		ttPlot.printBranches();
    } catch (IOException e) {
      System.out.println("Source depth out of range");
    }
  }
}
