package gov.usgs.traveltime;

import gov.usgs.traveltime.session.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

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

  /** Private logging object. */
  private static final Logger LOGGER = Logger.getLogger(TtMain.class.getName());

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
    String logLevel = "DEBUG";

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

    TtMain ttMain = new TtMain();

    // setup logging
    ttMain.setupLogging(logPath, getCurrentLocalDateTimeStamp() + "_traveltime.log", logLevel);

    // print out version
    LOGGER.info("neic-traveltime " + VERSION);

    // log args
    LOGGER.fine("Command line arguments: " + argumentList.toString().trim());

    // log java and os information
    LOGGER.config("java.vendor = " + System.getProperty("java.vendor"));
    LOGGER.config("java.version = " + System.getProperty("java.version"));
    LOGGER.config("java.home = " + System.getProperty("java.home"));
    LOGGER.config("os.arch = " + System.getProperty("os.arch"));
    LOGGER.config("os.name = " + System.getProperty("os.name"));
    LOGGER.config("os.version = " + System.getProperty("os.version"));
    LOGGER.config("user.dir = " + System.getProperty("user.dir"));
    LOGGER.config("user.name = " + System.getProperty("user.name"));

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
        String modelString = "Available Earth models:";
        for (int j = 0; j < models.length; j++) {
          modelString += ("\n" + models[j]);
        }
        LOGGER.fine(modelString);
      } else {
        LOGGER.info("There are no available Earth models?");
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
        ttLocal.logTable(returnAllPhases);

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
        ttList.dumpPhases();
      }

      String input = "";
      BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
      while (!"quit".equals(input)) {
        System.out.println("\nEnter Distance (or 'quit' to exit): ");
        input = reader.readLine();

        if (!"quit".equals(input)) {
          double userDelta = Double.parseDouble(input);
          ttList = ttLocal.getTT(elev, userDelta);
          ttList.dumpPhases();
        }
      }

      //		ttPlot = ttLocal.getPlot(earthModel, sourceDepth, phList, returnAllPhases,
      //				returnBackBranches, tectonic);
      //		ttPlot.printBranches();
    } catch (IOException e) {
      LOGGER.info("Source depth out of range");
    }
  }

  /**
   * This function sets up logging for the travel tiem package.
   *
   * @param logPath A String containing the path to write log files to
   * @param logFile A String containing the name of the log file
   * @param logLevel A String holding the desired log level
   */
  public void setupLogging(String logPath, String logFile, String logLevel) {
    LogManager.getLogManager().reset();

    // parse the logging level
    Level level = getLogLevel(logLevel);

    LOGGER.config("Logging Level '" + level + "'");
    LOGGER.config("Log directory '" + logPath + "'");

    Logger rootLogger = Logger.getLogger("");
    rootLogger.setLevel(level);

    // create log directory, log file, and file handler
    try {
      File logDirectoryFile = new File(logPath);
      if (!logDirectoryFile.exists()) {
        LOGGER.fine("Creating log directory");
        if (!logDirectoryFile.mkdirs()) {
          LOGGER.warning("Unable to create log directory");
        }
      }

      FileHandler fileHandler = new FileHandler(logPath + "/" + logFile);
      fileHandler.setLevel(level);

      rootLogger.addHandler(fileHandler);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Unable to create log file handler", e);
    }

    // create console handler
    ConsoleHandler consoleHandler = new ConsoleHandler();
    consoleHandler.setLevel(level);
    rootLogger.addHandler(consoleHandler);

    // set all handlers to the same formatter
    for (Handler handler : rootLogger.getHandlers()) {
      handler.setFormatter(new SimpleLogFormatter());
    }
  }

  /**
   * This function converts a log level string into a logger level. This function converts a couple
   * of non-standard logging levels / abbreviations.
   *
   * @param logLevel A String holding the desired log level
   * @return A Level object containing the desired log level.
   */
  private Level getLogLevel(String logLevel) {
    if (logLevel == null) {
      return null;
    }
    try {
      return Level.parse(logLevel.toUpperCase());
    } catch (IllegalArgumentException e) {
      if (logLevel.equalsIgnoreCase("DEBUG")) {
        return Level.FINE;
      }
      if (logLevel.equalsIgnoreCase("WARN")) {
        return Level.WARNING;
      }
      throw new IllegalArgumentException(
          "Unresolved log level " + logLevel + " for java.util.logging", e);
    }
  }

  /**
   * This function returns the current local time as a string
   *
   * @return A String containing current local time formatted in the form "yyyyMMdd_HHmmss".
   */
  public static String getCurrentLocalDateTimeStamp() {
    return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
  }
}
