/*
 *  This software is in the public domain because it contains materials
 *  that originally came from the United States Geological Survey,
 *  an agency of the United States Department of Interior. For more
 *  information, see the official USGS copyright policy at
 *  http://www.usgs.gov/visual-id/credit_usgs.html#copyright
 */
package gov.usgs.traveltime.session;

// import gov.usgs.anss.edgethread.EdgeThread;
import gov.usgs.traveltime.TravelTime;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;

/**
 * This class will control handing out TTSessions and sharing them as needed. In its first form it
 * always creates a new one and releases it when requested. The key for the session is the model and
 * the depth to 2 sigfigs.
 *
 * @author U.S. Geological Survey &lt;ketchum at usgs.gov&gt;
 */
public class TTSessionPool {

  private static final TreeMap<String, ArrayList<TTSession>> active = new TreeMap<>();
  private static final TreeMap<String, ArrayList<TTSession>> free = new TreeMap<>();

  /**
   * Get a TTSession with the following model and settings. Once the caller has the model, the
   * specifics can be changed by calling TTSession.newSession(). The session object remains the
   * same, but the computations needed by the new settings are done.
   *
   * @param model Model name
   * @param depth Depth in km
   * @param phaseList If not null, an array with the desired phase
   * @param lat source Latitude in degrees
   * @param lng source Longitude in degrees
   * @param allPhases If true, calls return all phase
   * @param backBranches If true, call return back branches
   * @param tectonic Region is tectonic
   * @param plot If true, this returns plot mode
   * @return A TTSession set to the arguments
   * @throws IOException If unable to read auxiliary or travel-time information
   */
  public static TTSession getTravelTimeSession(
      String model,
      double depth,
      String[] phaseList,
      double lat,
      double lng,
      boolean allPhases,
      boolean backBranches,
      boolean tectonic,
      boolean plot)
      throws IOException, Exception {
    return common(model, depth, phaseList, lat, lng, allPhases, backBranches, tectonic, plot);
  }

  /**
   * Get a TTSession with the following model and settings. Once the caller has the model, the
   * specifics can be changed by calling TTSession.newSession(). The session object remains the
   * same, but the computations needed by the new settings are done.
   *
   * @param model Model name
   * @param depth Depth in km
   * @param phaseList If not null, an array with the desired phase
   * @param allPhases If true, calls return all phase
   * @param backBranches If true, call return back branches
   * @param tectonic Region is tectonic
   * @param plot If true, this returns plot mode
   * @return A TTSession set to the arguments
   * @throws IOException If unable to read auxiliary or travel-time information
   */
  public static TTSession getTravelTimeSession(
      String model,
      double depth,
      String[] phaseList,
      boolean allPhases,
      boolean backBranches,
      boolean tectonic,
      boolean plot)
      throws IOException, Exception {
    return common(
        model, depth, phaseList, Double.NaN, Double.NaN, allPhases, backBranches, tectonic, plot);
  }

  /**
   * Get a TTSession with the following parmeters
   *
   * @param model Model name
   * @param depth Depth in km
   * @param phaseList If not null, an array with the desired phase
   * @param lat source Latitude in degrees
   * @param lng source Longitude in degrees
   * @param allPhases If true, calls return all phase
   * @param backBranches If true, call return back branches
   * @param tectonic Region is tectonic
   * @param plot If true, this returns plot mode
   * @return A TTSession set to the arguments
   * @throws IOException If unable to read auxiliary or travel-time information
   */
  public static TTSession getTravelTimeSession(
      String model,
      double depth,
      ArrayList<String> phaseList,
      double lat,
      double lng,
      boolean allPhases,
      boolean backBranches,
      boolean tectonic,
      boolean plot)
      throws IOException, Exception {
    // Create the String array with the phase list from the ArrayList of phases
    String[] phList = null;
    if (phaseList != null) {
      if (!phaseList.isEmpty()) {
        phList = new String[phaseList.size()];
        phaseList.toArray(phList);
      }
    }
    return common(model, depth, phList, lat, lng, allPhases, backBranches, tectonic, plot);
  }

  public static TTSession getTravelTimeSession(
      String model,
      double depth,
      ArrayList<String> phaseList,
      boolean allPhases,
      boolean backBranches,
      boolean tectonic,
      boolean plot)
      throws IOException, Exception {
    String[] phList = null;
    if (phaseList != null) {
      if (!phaseList.isEmpty()) {
        phList = new String[phaseList.size()];
        phaseList.toArray(phList);
      }
    }
    return common(
        model, depth, phList, Double.NaN, Double.NaN, allPhases, backBranches, tectonic, plot);
  }

  /**
   * setup a TTSession based on the given parameters
   *
   * @param model Model name
   * @param depth Depth in km
   * @param phaseList If not null, an array with the desired phase
   * @param lat source Latitude in degrees
   * @param lng source Longitude in degrees
   * @param allPhases If true, calls return all phase
   * @param backBranches If true, call return back branches
   * @param tectonic Region is tectonic
   * @param plot If true, this returns plot mode
   * @return A TTSession set to the arguments
   * @throws IOException
   */
  private static synchronized TTSession common(
      String model,
      double depth,
      String[] phList,
      double lat,
      double lng,
      boolean allPhases,
      boolean backBranches,
      boolean tectonic,
      boolean plot)
      throws IOException, Exception {

    String key = model;
    TTSession session;

    // Get the array list that are used and have this model
    ArrayList<TTSession> used = active.get(key);
    if (used == null) {
      used = new ArrayList<TTSession>(10); // It does not exist, create it
      active.put(key, used);
    }

    // Get the free list for the model
    ArrayList<TTSession> frees = free.get(key);
    if (frees == null) {
      frees = new ArrayList<TTSession>(10); // does not exist, add it
      free.put(key, frees);
    }

    // If there is a suitable session on the free list, give it to the user after doing a
    // newSession()
    if (frees.size() > 0) {
      session = frees.get(frees.size() - 1);
      used.add(session);
      frees.remove(frees.size() - 1);
      if (Double.isNaN(lat)) {
        session.newSession(depth, phList, allPhases, backBranches, tectonic, plot);
      } else {
        session.newSession(lat, lng, depth, phList, allPhases, backBranches, tectonic, plot);
      }
    } else {
      // There is not a free session, so create one and add it to the used list
      session =
          new TTSession(model, depth, phList, lat, lng, allPhases, backBranches, tectonic, plot);
      used.add(session);
      System.out.println(
          "Create new TTSession #used=" + used.size() + " #fr=" + frees.size() + " " + session);
    }
    return session;
  }

  /**
   * When the caller is done with the session it can be released to the free list
   *
   * @param s The TTSession to be moved from used to free
   */
  public static synchronized void releaseSession(TTSession s) {
    ArrayList<TTSession> used = active.get(s.getKey());
    ArrayList<TTSession> frees = free.get(s.getKey());
    if (used == null || frees == null) {
      System.out.println(
          "***** releasing a session from key=" + s.getKey() + " but unknown in used/free");
    }
    if (used != null) {
      used.remove(s);
    }
    if (frees != null) {
      frees.add(s);
    }
  }

  /**
   * A simple example of how to use this program. It samples the P at 2 to 82 degrees in 10 degree
   * steps and at depths from 33 to 400 in 10 km steps
   *
   * @param args Command line arguments
   */
  public static void main(String[] args) {
    boolean allPhases = true;
    boolean backBranches = false;
    boolean tectonic = false;
    boolean plot = false;
    String[] phList = new String[0];
    try {
      // Get a TTSession variable
      TTSession session =
          TTSessionPool.getTravelTimeSession(
              "ak135", 100, phList, allPhases, backBranches, tectonic, plot);
      // for depths from 33 to 400 in 10 km steps
      for (int depth = 33; depth < 400; depth = depth + 10) {
        // Set new depth with same flags
        session.newSession(depth, phList, allPhases, backBranches, tectonic, plot);
        System.out.println(
            "         Phase     TTravelTime      dTdD     dTdZ  Spread   Obsrv PhGrp  AuxGrp    Use   Regn Depth  Dis   isDpth="
                + depth);
        // Sample from 2 degrees to 82 degrees in 10 degree steps
        for (int delta = 2; delta < 90; delta = delta + 10) {
          TravelTime TravelTime = session.getTT((double) delta, 0.);
          for (int i = 0; i < TravelTime.getNumPhases(); i++) {
            String phase = TravelTime.getPhase(i).getPhaseCode();
            if (phase.equalsIgnoreCase("P")
                || phase.equalsIgnoreCase("Pn")
                || phase.equalsIgnoreCase("Pn")) {
              System.out.print("delta=" + delta + " " + TravelTime.getPhase(i).toString());
            }
          }
        }
      }
      TTSessionPool.releaseSession(session); // release the session to the free list
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
