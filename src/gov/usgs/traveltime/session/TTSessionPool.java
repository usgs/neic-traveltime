/*
 *  This software is in the public domain because it contains materials 
 *  that originally came from the United States Geological Survey, 
 *  an agency of the United States Department of Interior. For more 
 *  information, see the official USGS copyright policy at 
 *  http://www.usgs.gov/visual-id/credit_usgs.html#copyright
 */
package gov.usgs.traveltime.session;

//import gov.usgs.anss.edgethread.EdgeThread;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;

/** This class will control handing out TTSessions and sharing them
 * as needed.  In its first form it always creates a new one and releases it
 * when requested.  The key for the session is the model and the depth to 2 sigfigs.
 *
 * @author U.S. Geological Survey  <ketchum at usgs.gov>
 */
public class TTSessionPool {
  private static final TreeMap<String, ArrayList<TTSession>> active= new TreeMap<>();
  private static final TreeMap<String, ArrayList<TTSession>> free = new TreeMap<>();
  
  /** Get a TTSession with the following model and settings.  Once the caller has the model,
   * the specifics can be changed by calling TTSession.newSession().  The session object remains
   * the same, but the computations needed by the new settings are done.
   * 
   * @param model Model name
   * @param depth Depth in km
   * @param phaseList If not null, an array with the desired phase
   * @param lat source Latitude in degrees
   * @param lng source Longitude in degrees
   * @param allPhases If true, calls return all phase
   * @param backBranches If true, call return back branches
   * @param tectonic Region is tectonic
   * @param rstt If true, use RSTT for local travel times
   * @param plot If true, this returns plot mode
   * @return A TTSession set to the arguments
   * @throws IOException 
   */
  public static TTSession getTravelTimeSession(String model, double depth, 
          String [] phaseList, 
          double lat, double lng, boolean allPhases, 
          boolean backBranches, boolean tectonic, boolean rstt, boolean plot/*, EdgeThread parent*/)
          throws IOException, Exception {
    TTSession session  = common(model, depth, phaseList, lat, lng, allPhases, backBranches, tectonic, rstt, plot);
 
    return session;
  }
  /** Get a TTSession with the following parmeters
   * 
   * @param model Model name
   * @param depth Depth in km
   * @param phaseList If not null, an array with the desired phase
   * @param lat source Latitude in degrees
   * @param lng source Longitude in degrees
   * @param allPhases If true, calls return all phase
   * @param backBranches If true, call return back branches
   * @param tectonic Region is tectonic
   * @param rstt If true, use RSTT for local travel times
   * @param plot If true, this returns plot mode
   * @return A TTSession set to the arguments
   * @throws IOException 
   */  
  public static TTSession getTravelTimeSession(String model, double depth, 
          ArrayList<String> phaseList, 
          double lat, double lng, boolean allPhases, 
          boolean backBranches, boolean tectonic, boolean rstt, boolean plot/*, EdgeThread parent*/)
          throws IOException, Exception {
    // Create the String array with the phase list from the ArrayList of phases
    String [] phList = null;
    if(phaseList != null) {
      if(!phaseList.isEmpty()) {
        phList = new String[phaseList.size()];
        phaseList.toArray(phList);
      }
    }    
    TTSession session = common(model, depth, phList, lat, lng, allPhases, backBranches, tectonic, rstt, plot);
    return session;
  }
  private synchronized static TTSession  common(String model, double depth, 
          String [] phList, 
          double lat, double lng, boolean allPhases, 
          boolean backBranches, boolean tectonic, boolean rstt, boolean plot/*, EdgeThread parent*/
  ) throws IOException, Exception {

    String key = model;
    TTSession session;
    
    // Get the array list that are used and have this model
    ArrayList<TTSession>  used = active.get(key);
    if(used == null) {
      used = new ArrayList<TTSession>(10);    // It does not exist, create it
      active.put(key, used);
    }
    
    // Get the free list for the model
    ArrayList<TTSession> frees = free.get(key);
    if(frees == null) {
      frees = new ArrayList<TTSession>(10);   // does not exist, add it
      free.put(key, frees);
    }
    
    // If there is a suitable session on the free list, give it to the user after doing a newSession()
    if(frees.size() > 0) {
      session = frees.get(frees.size() -1);
      used.add(session);
      frees.remove(frees.size() -1);
      session.newSession(depth, phList, rstt, backBranches, tectonic, rstt, plot);
    }
    else {
      // There is not a free session, so create one and add it to the used list
      session = new TTSession(model, depth, phList,lat, lng, allPhases,
                   backBranches, tectonic, rstt, plot/*, parent*/);
      used.add(session);      
    }
    return session;    
  }
  
  /** When the caller is done with the session it can be released to the free list
   * 
   * @param s The TTSession to be moved from used to free
   */
  public synchronized static void releaseSession(TTSession s) {
    ArrayList<TTSession> used = active.get(s.getKey());
    ArrayList<TTSession> frees = free.get(s.getKey());
    if(used == null || frees == null) {
      System.out.println("***** releasing a session from key="+s.getKey()+" but unknown in used/free");
    }
    if(used != null) used.remove(s);
    if(frees != null) frees.add(s);
  }
}
