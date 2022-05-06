/*
 *  This software is in the public domain because it contains materials
 *  that originally came from the United States Geological Survey,
 *  an agency of the United States Department of Interior. For more
 *  information, see the official USGS copyright policy at
 *  http://www.usgs.gov/visual-id/credit_usgs.html#copyright
 */
package gov.usgs.traveltime.session;

import gov.usgs.traveltime.AllBranchReference;
import gov.usgs.traveltime.AllBranchVolume;
import gov.usgs.traveltime.AuxiliaryTTReference;
import gov.usgs.traveltime.FileChanged;
import gov.usgs.traveltime.PlotData;
import gov.usgs.traveltime.ReadTau;
import gov.usgs.traveltime.TauUtilities;
import gov.usgs.traveltime.TravelTime;
import gov.usgs.traveltime.TravelTimePlot;
import gov.usgs.traveltime.tables.MakeTables;
import java.io.IOException;
import java.io.PrintStream;
import java.util.TreeMap;

/** @author U.S. Geological Survey &lt;ketchum at usgs.gov&gt; */
public class TTSession {
  private static AuxiliaryTTReference auxTTReference;
  private static final TreeMap<String, AllBranchReference> modelData =
      new TreeMap<String, AllBranchReference>();
  // private static final TreeMap<String, ArrayList<AllBranchVolume>> modelAllBranchVolumeFree = new
  // TreeMap<>();
  // private static final TreeMap<String, ArrayList<AllBranchVolume>> modelAllBranchVolumeAssigned =
  // new
  // TreeMap<>();

  // Session variables
  private String earthModel;
  private double sourceDepth;
  private double sourceLatitude;
  private double sourceLongitude;
  private boolean convertTectonic;
  private boolean returnBackBranches; // The TT package want nobackBranches which is the opposite
  private boolean returnAllPhases; // The TT package whats useful which is the opposite
  private boolean isPlot;
  private String[] phList;
  private StringBuilder ttag = new StringBuilder(4);
  private String key;

  // Variables used within a Session
  private AllBranchVolume allBrn; // The current session with depth object to query for stuff
  private PlotData plotData; // Travel-time plot data
  private String serName; // Serialized file name for this model
  private String[] fileNames; // Raw input file names for this model

  @Override
  public String toString() {
    return ttag + " " + allBrn.toString();
  }
  // logging related stuff
  private PrintStream getPrintStream() {
    return System.out;
  }

  public String getKey() {
    return key;
  }
  /**
   * Return the working AllBranchVolume for this session
   *
   * @return The current AllBranchVolume
   */
  public AllBranchVolume getAllBranchVolume() {
    return allBrn;
  }
  /**
   * Construct up a session from the session parameters
   *
   * @param earthModel Like ak135
   * @param sourceDepth Depth in km
   * @param phases List of phase desired or null for all phases
   * @param allPhases If true all phase return (this is !useful for the TT package)
   * @param returnBackBranches Return back branches (TT this is !noBackBranch)
   * @param tectonic source is a tectonic province
   * @param isPlot Call in plot mode
   * @throws IOException If unable to read auxiliary or travel-time information
   */
  public TTSession(
      String earthModel,
      double sourceDepth,
      String[] phases,
      boolean allPhases,
      boolean returnBackBranches,
      boolean tectonic,
      boolean isPlot)
      throws Exception {
    makeTTSession(
        earthModel,
        sourceDepth,
        phases,
        Double.NaN,
        Double.NaN,
        allPhases,
        returnBackBranches,
        tectonic,
        isPlot);
  }
  /**
   * Set up a session from the session parameters
   *
   * @param earthModel Like ak135
   * @param sourceDepth Depth in km
   * @param phases List of phase desired or null for all phases
   * @param srcLat Source latitude in degrees
   * @param srcLong Source longitude in degrees
   * @param allPhases If true all phase return (this is !useful for the TT package)
   * @param returnBackBranches Return back branches (TT this is !noBackBranch)
   * @param tectonic source is a tectonic province
   * @param isPlot Call in plot mode
   * @throws IOException If unable to read auxiliary or travel-time information
   */
  public TTSession(
      String earthModel,
      double sourceDepth,
      String[] phases,
      double srcLat,
      double srcLong,
      boolean allPhases,
      boolean returnBackBranches,
      boolean tectonic,
      boolean isPlot)
      throws Exception {
    makeTTSession(
        earthModel,
        sourceDepth,
        phases,
        srcLat,
        srcLong,
        allPhases,
        returnBackBranches,
        tectonic,
        isPlot);
  }

  private void makeTTag() {
    if (ttag.length() > 0) ttag.delete(0, ttag.length());
    ttag.append("TTS(")
        .append(earthModel)
        .append("/")
        .append(("" + sourceDepth).substring(0, Math.max(4, ("" + sourceDepth).length())))
        .append("/")
        .append(returnAllPhases ? "P" : "p")
        .append(returnBackBranches ? "B" : "b")
        .append(convertTectonic ? "T" : "t")
        .append(isPlot ? "P" : "p")
        .append("):");
  }
  /**
   * Set up a session from the session parameters
   *
   * @param earthModel Like ak135
   * @param sourceDepth Depth in km
   * @param phases List of phase desired or null for all phases
   * @param srcLat Source latitude in degrees
   * @param srcLong Source longitude in degrees
   * @param allPhases If true all phase return (this is !useful for the TT package)
   * @param returnBackBranches Return back branches (TT this is !noBackBranch)
   * @param tectonic source is a tectonic province
   * @param isPlot Call in plot mode
   * @throws IOException
   */
  private void makeTTSession(
      String earthModel,
      double sourceDepth,
      String[] phases,
      double srcLat,
      double srcLong,
      boolean allPhases,
      boolean returnBackBranches,
      boolean tectonic,
      boolean isPlot)
      throws Exception {
    // par = parent;
    this.earthModel = earthModel;
    this.sourceDepth = sourceDepth;
    key = earthModel.trim();
    if (phases != null) {
      phList = new String[phases.length];
      System.arraycopy(phases, 0, phList, 0, phases.length);
    }
    if (Double.isNaN(srcLat)) {
      sourceLatitude = Double.NaN;
    } else {
      if (srcLat > -90. && srcLat <= 90.) {
        sourceLatitude = srcLat;
      } else {
        sourceLatitude = Double.NaN;
      }
    }
    if (Double.isNaN(srcLong)) {
      sourceLongitude = Double.NaN;
    } else {
      if (srcLong >= -180. && srcLong <= 180.) {
        sourceLongitude = srcLong;
      } else {
        sourceLongitude = Double.NaN;
      }
    }
    convertTectonic = tectonic;
    this.returnAllPhases = allPhases;
    this.returnBackBranches = returnBackBranches;
    this.isPlot = isPlot;
    makeTTag();

    try {
      // Read in data common to all models.
      if (auxTTReference == null) {
        // prta(ttag + " create AuxiliaryTTReference ");
        // NOTE assumes default model path for now, need to figure out
        // where to get this path. Cmd line arg?
        auxTTReference = new AuxiliaryTTReference(true, true, true, null, null);
      }

      // See if we know this model.
      AllBranchReference allRef = modelData.get(earthModel);

      // If not, set it up.
      if (allRef == null) {
        if (modelChanged(earthModel)) {
          // The Earth model files have changed--regenerate them.
          if (TauUtilities.useFortranFiles) {
            // We're going to read the model tables from the Fortran files.
            try {
              // prta(ttag + " Need to read in model=" + earthModel);
              ReadTau readTau = new ReadTau(earthModel);
              readTau.readHeader(TauUtilities.model(fileNames[0]));
              //	readTau.dumpSegments();
              //	readTau.dumpBranches();
              readTau.readTable(TauUtilities.model(fileNames[1]));
              //	readTau.dumpUp(15);
              allRef = new AllBranchReference(serName, readTau, auxTTReference);
            } catch (IOException e) {
              e.printStackTrace(getPrintStream());
              throw e;
            }
          } else {
            // We're going to generate the model tables from scratch.
            try {
              // prta(ttag + " Need to generate model=" + earthModel);
              MakeTables make = new MakeTables(earthModel);
              make.buildModel(fileNames[0], fileNames[1]);
              allRef = make.fillInBranchReferenceData(serName, auxTTReference);
            } catch (Exception e) {
              e.printStackTrace(getPrintStream());
              throw e;
            }
          }
        } else {
          // We can just read the model from the serialized files.
          try {
            // prta(ttag + " Serialize model in =" + earthModel);
            allRef = new AllBranchReference(serName, earthModel, auxTTReference);
          } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace(getPrintStream());
            throw e;
          }
        }
        modelData.put(earthModel, allRef);
        allRef.dumpBranchData(false);
      }
      // At this point, we've either found the reference part of the model
      // or read it in.  Now Set up the (depth dependent) volatile part.
      // See if this model is on the Free Tree
      /* The modelAllVolFree is a list of AllBranchVolume which are not in use (free list)
         The modelAllBrnAssigned is the list of busy ones.  The tremmat is done by
         earth model.  They are moved to free list when the Session is closed.
      */
      /*synchronized (modelAllBranchVolumeFree) {
      ArrayList<AllBranchVolume> free = modelAllBranchVolumeFree.get(earthModel);
      ArrayList<AllBranchVolume> used;
      if (free == null) {
        // create the used and free list for this model type
        free = new ArrayList<AllBranchVolume>(10);
        used = new ArrayList<AllBranchVolume>(10);
        modelAllBranchVolumeFree.put(earthModel, free);
        modelAllBranchVolumeAssigned.put(earthModel, used);
      }*/

      // Get the used list and if the free list is empty, put a new AllBranchVolume on it
      // used = modelAllBranchVolumeAssigned.get(earthModel);
      // if (free.isEmpty()) {
      // prta(
      //    ttag
      //        + " creating a new AllBranchVolume for "
      //        + earthModel /*+" #free="+free.size()+" #used="+used.size()*/);
      allBrn = new AllBranchVolume(allRef);
      //  free.add(allBrn);
      // }

      // get a AllBranchVolume from the free list and put it on the used list
      // allBrn = free.get(free.size() - 1);
      // free.remove(free.size() - 1);
      // used.add(allBrn);
      // }
      //	allBrn.dumpHead();
      //	allBrn.dumpMod('P', true);
      //	allBrn.dumpMod('S', true);
      // Set up a new session.
      try {
        if (!isPlot) {
          // Set up to generate travel-times (e.g., for earthquake location).
          if (Double.isNaN(sourceLatitude)) {
            allBrn.newSession(sourceDepth, phList, allPhases, returnBackBranches, tectonic);
          } else {
            allBrn.newSession(
                sourceLatitude,
                sourceLongitude,
                sourceDepth,
                phList,
                allPhases,
                returnBackBranches,
                tectonic);
          }
        } else {
          // Generate a travel-time chart.
          double maxDistance = 180.0;
          double maxTime = 3600.0;
          plotData = new PlotData(allBrn);
          plotData.makePlot(
              sourceDepth,
              phList,
              allPhases,
              returnBackBranches,
              tectonic,
              maxDistance,
              maxTime,
              -1.0);
        }
      } catch (Exception e) {
        e.printStackTrace(getPrintStream());
        // prta(ttag + " Unknown exception while setting sourceDepth and phList in newSession()!");
      }
    } catch (IOException | ClassNotFoundException e) {
      e.printStackTrace(getPrintStream());
      throw e;
    }
  }

  /**
   * Determine if the input files have changed.
   *
   * @param earthModel Earth model name
   * @return True if the input files have changed
   */
  private boolean modelChanged(String earthModel) {
    // We need two files in either case.
    fileNames = new String[2];
    if (TauUtilities.useFortranFiles) {
      // Names for the Fortran files.
      serName = TauUtilities.serialize(earthModel + "_for.ser");
      fileNames[0] = TauUtilities.model(earthModel + ".hed");
      fileNames[1] = TauUtilities.model(earthModel + ".tbl");
    } else {
      // Names for generating the model.
      serName = TauUtilities.serialize(earthModel + "_gen.ser");
      fileNames[0] = TauUtilities.model("m" + earthModel + ".mod");
      fileNames[1] = TauUtilities.model("phases.txt");
    }
    return FileChanged.isChanged(serName, fileNames);
  }

  /**
   * Set up a new session. Note that this sets up the complex session parameters of use to the
   * travel-time package.
   *
   * @param latitude Source geographical latitude in degrees
   * @param longitude Source longitude in degrees
   * @param depth Source depth in kilometers
   * @param phList Array of phase use commands
   * @param allPhases This is passed to the TT package as useful = !allPhases
   * @param backBrn This is passed to the TT package at noBackBranches = !backBrn
   * @param tectonic in a tectonic region
   * @param plot If true, call in ploc mode
   * @throws Exception If the depth is out of range
   */
  public void newSession(
      double latitude,
      double longitude,
      double depth,
      String[] phList,
      boolean allPhases,
      boolean backBrn,
      boolean tectonic,
      boolean plot)
      throws Exception {
    if (Double.isNaN(latitude)) {
      allBrn.newSession(latitude, longitude, depth, phList, allPhases, backBrn, tectonic);
    } else {
      allBrn.newSession(depth, phList, allPhases, backBrn, tectonic);
    }
    makeTTag();
  }
  /**
   * Set up a new session. Note that this just sets up the simple session parameters of use to the
   * travel-time package.
   *
   * @param depth Source depth in kilometers
   * @param phList Array of phase use commands
   * @param allPhases This is passed to the TT package as useful = !allPhases
   * @param backBrn This is passed to the TT package at noBackBranches = !backBrn
   * @param tectonic in a tectonic region
   * @param plot If true, call in ploc mode
   * @throws Exception If the depth is out of range
   */
  public void newSession(
      double depth,
      String[] phList,
      boolean allPhases,
      boolean backBrn,
      boolean tectonic,
      boolean plot)
      throws Exception {
    allBrn.newSession(depth, phList, allPhases, backBrn, tectonic);
    makeTTag();
  }

  /** @param obj */
  /*private void freeAllBranchVolume(AllBranchVolume obj) {
    String model = obj.getEarthModel();
    synchronized(modelAllBranchVolumeFree) {
      ArrayList<AllBranchVolume> free = modelAllBranchVolumeFree.get(earthModel);
      ArrayList<AllBranchVolume> used = modelAllBranchVolumeAssigned.get(earthModel);
      if(free != null && used != null) {
        for(int i=0; i<used.size(); i++) {
          if(used.get(i) == obj) {
            used.remove(i);
            free.add(obj);
            prta(ttag+"TTS: free "+obj);
            return;
          }
        }
        prta(ttag+"TTS: **** free of AllBranchVolume not found on Used list! "+obj);
        new RuntimeException("TTS: free of AllBranchVolumed not found "+obj).printStackTrace(getPrintStream());
      }
      else {
        prta(ttag+"TTS: **** free or used not available for model="+earthModel);
      }
    }
  } */
  /**
   * For 'standard' requests giving receiver distance and elevation only
   *
   * @param recDistance In degrees
   * @param recElevation of receiver
   * @return A TravelTime structure with the travel time
   */
  public synchronized TravelTime getTT(double recDistance, double recElevation) {
    if (allBrn == null) return null;

    TravelTime TravelTime = allBrn.getTravelTime(recElevation, recDistance);
    return TravelTime;
  }
  /**
   * NOTE: JSON does not contain the azimuth but contains both distance and lat/long????
   *
   * @param recLat Receiver latitude in degrees
   * @param recLong Receiver longitude in degrees
   * @param recElev Receiver elevation in degrees
   * @param delta Degrees between source and reciever in degrees
   * @param azimuth Azimuth
   * @return List of travel times
   */
  public synchronized TravelTime getTT(
      double recLat, double recLong, double recElev, double delta, double azimuth) {
    if (allBrn == null) return null;
    TravelTime TravelTime = allBrn.getTravelTime(recLat, recLong, recElev, delta, azimuth);
    return TravelTime;
  }

  /**
   * Unlike travel-times, plot data is a one shot deal that was generated in makeTTSession.
   *
   * @return Travel-time plot data
   */
  public synchronized TravelTimePlot getPlotData() {
    return plotData.getPlotData();
  }

  /**
   * Get a pointer to the auxiliary travel-time information.
   *
   * @return Travel-time auxiliary information
   */
  public AuxiliaryTTReference getAuxTT() {
    return auxTTReference;
  }

  /** close this session which also frees the AllBranchVolume in use */
  public void close() {
    TTSessionPool.releaseSession(this);
    // if(allBrn != null) {
    //  freeAllBranchVolume(allBrn);
    //
    // allBrn = null;
  }

  public static void main(String[] args) {
    if (args.length == 0) {
      System.out.println("Usage: gettt depth distance [model def=ak135][Ph1:Ph2...:PHn]");
      args = "33 10 ak135".split("\\s");

      System.exit(0);
    }
    double depth = Double.parseDouble(args[0]);
    double delta = Double.parseDouble(args[1]);
    String model = "ak135";
    if (args.length >= 3) model = args[2];
    String[] phList = new String[0];
    if (args.length >= 4) phList = args[3].split(":,");
    try {
      TTSession session =
          TTSessionPool.getTravelTimeSession(model, depth, phList, true, false, false, false);
      TravelTime TravelTime = session.getTT(delta, 0.);
      System.out.println(
          "Phase     TTravelTime      dTdD     dTdZ  Spread   Obsrv PhGrp  AuxGrp    Use   Regn Depth  Dis   isDpth="
              + depth
              + " delta="
              + delta);

      for (int i = 0; i < TravelTime.getNumPhases(); i++) {
        System.out.print(TravelTime.getPhase(i).toString());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
