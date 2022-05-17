package gov.usgs.traveltime.tables;

/**
 * Utilities related to travel-time table generation.
 *
 * @author Ray Buland
 */
public class TablesUtil {
  /**
   * A double containing the increment in radius in kilometers to sample the reference Earth model.
   */
  public static double RESAMPLERADIUS = 50d;

  /**
   * A double containing the maximum non-dimensional increment between successive slowness samples.
   */
  public static double MAXSLOWNESSINCREMENT = 0.01d;

  /** A double containing the maximum increment between successive radius samples in kilometers. */
  public static double MAXRADIUSINCREMENT = 75d;

  /**
   * An array of double values holding the target increments between successive ray travel distances
   * in kilometers. Different targets are supported for different major shells in the order: inner
   * core, outer core, lower mantle, upper mantle, lower crust, and upper crust. The finer sampling
   * at shallower depths is necessary to stabilize the results of complex regional models.
   */
  public static double[] TARGETTRAVELDISTANCES = {300d, 300d, 150d, 150d, 100d, 100d};

  /** A double holding the the target range spacing for the up-going branch proxy in kilometers. */
  public static double TARGETUPGOINGSPACING = 400d;

  /**
   * A double that holds the dividing line (as a ratio) between trusting the default up-going
   * decimation and keeping some additional ray parameters.
   */
  public static double RAYPARAMLIMITRATIO = 0.7d;

  /**
   * A double containing the the ray parameter tolerance, ray parameters closer together than this
   * non-dimensional tolerance will use the default up-going decimation even if we're looking to
   * keep some additional ray parameters.
   */
  public static double RAYPARAMTOLERANCE = 0.03d;

  /**
   * A double containing the maximum iterations for root finding algorithms (e.g., for finding
   * caustics).
   */
  public static int MAXROOTFINDINGITERATIONS = 30;

  /** A double holding the non-dimensional tolerance for sampling range (ray travel distance). */
  public static double SAMPLEINGDISTANCETOLERANCE = 5e-6d;

  /**
   * A double holding the relative velocity tolerance. If velocity is within this tolerance across
   * an apparent Earth model discontinuity, make the velocity continuous.
   */
  public static double VELOCITYTOLERANCE = 2e-5d;

  /**
   * A double containing the non-dimensional back off when dXdP is infinite (at the top of shells).
   */
  public static double SLOWNESSOFFSET = 1e-6d;

  /**
   * An integer containing the current debug level, the higher the debug level, the more output you
   * get.
   */
  public static int deBugLevel = 0;
}
