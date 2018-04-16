/*
 *  This software is in the public domain because it contains materials 
 *  that originally came from the United States Geological Survey, 
 *  an agency of the United States Department of Interior. For more 
 *  information, see the official USGS copyright policy at 
 *  http://www.usgs.gov/visual-id/credit_usgs.html#copyright
 */
package gov.usgs.traveltime.session;
//import gov.usgs.anss.edgethread.EdgeThread;
import gov.usgs.traveltime.AllBrnRef;
import gov.usgs.traveltime.AllBrnVol;
import gov.usgs.traveltime.AuxTtRef;
import gov.usgs.traveltime.ReadTau;
import gov.usgs.traveltime.TTime;
import java.io.IOException;
import java.io.PrintStream;
//import java.util.ArrayList;
import java.util.TreeMap;

/**
 *
 * @author U.S. Geological Survey  &lt;ketchum at usgs.gov&gt;
 */
public class TTSession {
  private static AuxTtRef auxtt;
  private static final TreeMap<String, AllBrnRef> modelData = new TreeMap();
  //private static final TreeMap<String, ArrayList<AllBrnVol>> modelAllBrnVolFree = new TreeMap<>();
  //private static final TreeMap<String, ArrayList<AllBrnVol>> modelAllBrnVolAssigned = new TreeMap<>();
  
// Session variables 
  private String earthModel;
  private double sourceDepth;
  private double sourceLatitude;
  private double sourceLongitude;
  private boolean convertTectonic;
  private boolean returnBackBranches;  // The TT package want nobackBranches which is the opposite
  private boolean returnAllPhases;     // The TT package whats useful which is the opposite
  private boolean useRSTT;
  private boolean isPlot;
  private String[] phList;
  private StringBuilder ttag=new StringBuilder(4);
  private String key;
  
  // Variables used within a Session
  private AllBrnVol allBrn;         // The current session with depth object to query for stuff
  
  @Override
  public String toString() {
    return ttag+" "+allBrn.toString();
  }
  // logging related stuff
  //private EdgeThread par;
  private void prt(String s) {System.out.println(s);}
  private void prta(String s) {System.out.println(s);}
  private void prt(StringBuilder s) {System.out.println(s);}
  private void prta(StringBuilder s) {System.out.println(s);}
  private PrintStream getPrintStream() {return System.out;}
  //private void prt(String s) {if(par == null) Util.prt(s); else par.prt(s);}
  //private void prt(StringBuilder s) {if(par == null) Util.prt(s); else par.prt(s);}
  //private void prta(String s) {if(par == null) Util.prta(s); else par.prta(s);}
  //private void prta(StringBuilder s) {if(par == null) Util.prta(s); else par.prta(s);}
  //private PrintStream getPrintStream() {if(par == null) return Util.getOutput(); else return par.getPrintStream();}
  public String getKey() {return key;}
  /** Return the working AllBrnVol for this session
   * 
   * @return The current AllBrnVol
   */
  public AllBrnVol getAllBrnVol() {return allBrn ;}  
 /**
   * Construct up a session from the session parameters
   *
   * @param earthModel Like ak135
   * @param sourceDepth Depth in km
   * @param phases List of phase desired or null  for all phases
   * @param allPhases If true all phase return (this is !useful for the TT package)
   * @param returnBackBranches Return back branches (TT this is !noBackBranch)
   * @param tectonic source is a tectonic province
   * @param useRSTT Use RSTT instead for all local phases
   * @param isPlot Call in plot mode
   * @throws IOException If unable to read auxiliary or travel-time information
   */
  public TTSession(String earthModel, double sourceDepth, String[] phases, 
          boolean allPhases, boolean returnBackBranches, boolean tectonic,
          boolean useRSTT, boolean isPlot /*, EdgeThread parent*/) throws IOException {
    makeTTSession(earthModel, sourceDepth, phases, Double.NaN, Double.NaN, 
            allPhases, returnBackBranches, tectonic, useRSTT, isPlot);
    
  }
  /**
   * Set up a session from the session parameters
   *
   * @param earthModel Like ak135
   * @param sourceDepth Depth in km
   * @param phases List of phase desired or null  for all phases
   * @param srcLat Source latitude in degrees
   * @param srcLong Source longitude in degrees
   * @param allPhases If true all phase return (this is !useful for the TT package)
   * @param returnBackBranches Return back branches (TT this is !noBackBranch)
   * @param tectonic source is a tectonic province
   * @param useRSTT Use RSTT instead for all local phases
   * @param isPlot Call in plot mode
   * @throws IOException If unable to read auxiliary or travel-time information
   */
  public TTSession(String earthModel, double sourceDepth, String[] phases, 
          double srcLat, double srcLong,
          boolean allPhases, boolean returnBackBranches, boolean tectonic,
          boolean useRSTT, boolean isPlot /*, EdgeThread parent*/) throws IOException {
    makeTTSession(earthModel, sourceDepth, phases, 
            srcLat, srcLong,
            allPhases, returnBackBranches, tectonic, useRSTT, isPlot);
  }
  private void makeTTag() {
    if(ttag.length() >0) ttag.delete(0, ttag.length());
    ttag.append("TTS(").append(earthModel).append("/").append((""+sourceDepth).substring(0,Math.max(4,(""+sourceDepth).length()))).
            append("/").append(returnAllPhases?"P":"p").append(returnBackBranches?"B":"b").
          append(convertTectonic ? "T" : "t").append(useRSTT? "R" : "r").append(isPlot ? "P" : "p").append("):");
  }
 /**
   * Set up a session from the session parameters
   *
   * @param earthModel Like ak135
   * @param sourceDepth Depth in km
   * @param phases List of phase desired or null  for all phases
   * @param srcLat Source latitude in degrees
   * @param srcLong Source longitude in degrees
   * @param allPhases If true all phase return (this is !useful for the TT package)
   * @param returnBackBranches Return back branches (TT this is !noBackBranch)
   * @param tectonic source is a tectonic province
   * @param useRSTT Use RSTT instead for all local phases
   * @param isPlot Call in plot mode
   * @throws IOException
   */  
  private void makeTTSession(String earthModel, double sourceDepth, String[] phases, 
          double srcLat, double srcLong,
          boolean allPhases, boolean returnBackBranches, boolean tectonic,
          boolean useRSTT, boolean isPlot ) throws IOException {
    //par = parent;
    this.earthModel = earthModel;
    this.sourceDepth = sourceDepth;
    key = earthModel.trim();
    if (phases != null) {
      phList = new String[phases.length];
      System.arraycopy(phases, 0, phList, 0, phases.length);
    }
    if ( Double.isNaN(srcLat)) {
      sourceLatitude = Double.NaN;
    }
    else {
      if (srcLat > -90. && srcLat <= 90.) {
        sourceLatitude = srcLat;
      } else {
        sourceLatitude = Double.NaN;
      }
    }
    if( Double.isNaN(srcLong)) {
      sourceLongitude = Double.NaN;
    }
    else {
      if (srcLong >= -180. && srcLong <= 180.) {
        sourceLongitude = srcLong;
      } else {
        sourceLongitude = Double.NaN;
      }
    }
    convertTectonic = tectonic;
    this.returnAllPhases = allPhases;
    this.returnBackBranches = returnBackBranches;
    this.useRSTT = useRSTT;
    this.isPlot = isPlot;
    makeTTag();

    try {
      // Read in data common to all models.
      if (auxtt == null) {
        prta(ttag+" create AuxTtRef ");
        auxtt = new AuxTtRef(true, true, true);
      }

      // See if we know this model.
      AllBrnRef allRef = modelData.get(earthModel);

      // If not, set it up.
      if (allRef == null) {
        try {
          prta(ttag+" Need to read in model="+earthModel);
          ReadTau readTau = new ReadTau(earthModel);
          readTau.readHeader();
          //	readTau.dumpSegments();
          //	readTau.dumpBranches();
          readTau.readTable();
          //	readTau.dumpUp(15);
          allRef = new AllBrnRef(readTau, auxtt);
        } catch (IOException e) {
          e.printStackTrace(getPrintStream());
          throw e;
        }
        modelData.put(earthModel, allRef);
        allRef.dumpBrn(false);
      }
      // At this point, we've either found the reference part of the model 
      // or read it in.  Now Set up the (depth dependent) volatile part.
      // See if this model is on the Free Tree
      /* The modelAllVolFree is a list of AllBrnVol which are not in use (free list)
         The modelAllBrnAssigned is the list of busy ones.  The tremmat is done by
         earth model.  They are moved to free list when the Session is closed.
      */
      /*synchronized (modelAllBrnVolFree) {
        ArrayList<AllBrnVol> free = modelAllBrnVolFree.get(earthModel);
        ArrayList<AllBrnVol> used;
        if (free == null) {
          // create the used and free list for this model type
          free = new ArrayList<AllBrnVol>(10);
          used = new ArrayList<AllBrnVol>(10);
          modelAllBrnVolFree.put(earthModel, free);
          modelAllBrnVolAssigned.put(earthModel, used);
        }*/
        
        // Get the used list and if the free list is empty, put a new AllBrnVol on it
        //used = modelAllBrnVolAssigned.get(earthModel);
        //if (free.isEmpty()) {
          prta(ttag+" creating a new AllBrnVol for "+earthModel/*+" #free="+free.size()+" #used="+used.size()*/);
          allBrn = new AllBrnVol(allRef);
        //  free.add(allBrn);
        //}

        // get a allBrnVol from the free list and put it on the used list
        //allBrn = free.get(free.size() - 1);
        //free.remove(free.size() - 1);
        //used.add(allBrn);
      //}
      //	allBrn.dumpHead();
      //	allBrn.dumpMod('P', true);
      //	allBrn.dumpMod('S', true);
      // Set up a new session.
      try {
        if(Double.isNaN(sourceLatitude) ) {
          allBrn.newSession(sourceDepth, phList, !allPhases, !returnBackBranches, tectonic, useRSTT);      
        }
        else {
          allBrn.newSession(sourceLatitude, sourceLongitude, sourceDepth, phList,!allPhases, !returnBackBranches, tectonic, useRSTT);
        }
      } catch (Exception e) {
        e.printStackTrace(getPrintStream());
        prta(ttag+" Unknown exception while setting sourceDepth and phList in newSession()!");
      }
    } catch (IOException e) {
      e.printStackTrace(getPrintStream());
      throw e;
    }
  }

	/**
	 * Set up a new session.  Note that this sets up the complex 
	 * session parameters of use to the travel-time package.
	 * 
	 * @param latitude Source geographical latitude in degrees
	 * @param longitude Source longitude in degrees
	 * @param depth Source depth in kilometers
	 * @param phList Array of phase use commands 
   * @param allPhases This is passed to the TT package as useful = !allPhases
   * @param backBrn This is passed to the TT package at noBackBranches = !backBrn
   * @param tectonic in a tectonic region
   * @param rstt If true, use RSTT for local phase
   * @param plot If true, call in ploc mode
	 * @throws Exception If the depth is out of range
	 */
	public void newSession(double latitude, double longitude, double depth, 
			String[] phList, boolean allPhases, boolean backBrn, boolean tectonic, 
			boolean rstt, boolean plot) throws Exception {  
    if(Double.isNaN(latitude)) {
      allBrn.newSession(latitude, longitude, depth, phList, !allPhases, !backBrn, tectonic, rstt);
    }
    else {
      allBrn.newSession(depth, phList, !allPhases, !backBrn, tectonic, rstt);
    }
    makeTTag();
  }
  /**
    * Set up a new session.  Note that this just sets up the 
    * simple session parameters of use to the travel-time package.
    * 
    * @param depth Source depth in kilometers
    * @param phList Array of phase use commands
    * @param allPhases This is passed to the TT package as useful = !allPhases
    * @param backBrn This is passed to the TT package at noBackBranches = !backBrn
    * @param tectonic in a tectonic region
    * @param rstt If true, use RSTT for local phase
    * @param plot If true, call in ploc mode
    * @throws Exception If the depth is out of range
    */
	public void newSession(double depth, String[] phList, boolean allPhases, 
			boolean backBrn, boolean tectonic, boolean rstt, boolean plot) 
			throws Exception {
    allBrn.newSession(depth, phList, !allPhases, !backBrn, tectonic, rstt);
    makeTTag();
  }
  
  /**
   * 
   * @param obj 
   */
  /*private void freeAllBrnVol(AllBrnVol obj) {
    String model = obj.getEarthModel();
    synchronized(modelAllBrnVolFree) {
      ArrayList<AllBrnVol> free = modelAllBrnVolFree.get(earthModel);
      ArrayList<AllBrnVol> used = modelAllBrnVolAssigned.get(earthModel);
      if(free != null && used != null) {
        for(int i=0; i<used.size(); i++) {
          if(used.get(i) == obj) {
            used.remove(i);
            free.add(obj);
            prta(ttag+"TTS: free "+obj);
            return;
          }
        }
        prta(ttag+"TTS: **** free of AllBrnVol not found on Used list! "+obj);
        new RuntimeException("TTS: free of AllBrnVold not found "+obj).printStackTrace(getPrintStream());
      }
      else {
        prta(ttag+"TTS: **** free or used not available for model="+earthModel);
      }
    }
  } */
  /**
   * For 'standard' requests giving receiver distance and elevation only
   *
   *
   * @param recDistance In degrees
   * @param recElevation of receiver
   * @return A TTime structure with the travel time
   *
   */
  public synchronized TTime getTT(double recDistance, double recElevation) {
    if(allBrn == null) return null;

    TTime ttime = allBrn.getTT(recElevation, recDistance);
    return ttime;
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
  public synchronized TTime getTT(double recLat, double recLong, double recElev, double delta, double azimuth) {
    if(allBrn == null) return null;
    TTime ttime = allBrn.getTT(recLat, recLong, recElev, delta, azimuth);
    return ttime;
  }
  
  /**
   * Get a pointer to the auxiliary travel-time information.
   * 
   * @return Travel-time auxiliary information
   */
  public AuxTtRef getAuxTT() {
  	return auxtt;
  }
  
  /** close this session which also frees the allBrnVol in use
  
  */
  public void close() {
    TTSessionPool.releaseSession(this);
    //if(allBrn != null) {
    //  freeAllBrnVol(allBrn);
    //
    //allBrn = null;
  }
  public static void main(String [] args) {
    if(args.length == 0) {
      System.out.println("Usage: gettt depth distance [model def=ak135][Ph1:Ph2...:PHn]");
      args = "33 10 ak135".split("\\s");
              
      System.exit(0);
    }
    double depth = Double.parseDouble(args[0]);
    double delta = Double.parseDouble(args[1]);
    String model = "ak135";
    if(args.length >= 3) model = args[2];
    String [] phList = new String[0];
    if(args.length >= 4) phList = args[3].split(":,");
    try {
      TTSession session = TTSessionPool.getTravelTimeSession(model, depth, phList, true, false, false, false, false);
      TTime ttime = session.getTT( delta, 0.);
      System.out.println("Phase     TTtime      dTdD     dTdZ  Spread   Obsrv PhGrp  AuxGrp    Use   Regn Depth  Dis   isDpth=" + depth+ " delta="+delta);

      for (int i = 0; i < ttime.size(); i++) {
        System.out.print(ttime.get(i).toString());    
      }      
    }
    catch(Exception e) {
      e.printStackTrace();
    }
  }
}
