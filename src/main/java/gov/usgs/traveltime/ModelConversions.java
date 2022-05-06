package gov.usgs.traveltime;

import java.io.Serializable;

/**
 * THe ModelConversions class contains the earth model dependent unit conversions and constants.
 *
 * @author Ray Buland
 */
public class ModelConversions implements Serializable {
  /** A long containing the version id used in serialization */
  private static final long serialVersionUID = 1L;

  /**
   * A double containing the normalization for distance (depth). Note that this is Xn in the Fortran
   * programs. You need to multiply the distance by Xn to normalize it.
   */
  private final double distanceNormalization;

  /**
   * A double containing the normalization for velocity. Called Pn in the Fortran code, this
   * constant was intended as the normalization for slowness. Dividing velocity by Pn normalizes it.
   */
  private final double velocityNormalization;

  /**
   * A double containing the normalization for travel-time and tau. This is Tn in the Fortran code
   * to compute travel times, but 1/Tn in the code to generate the tables (yuck!).
   */
  private final double tauTTNormalization;

  /**
   * A double containing the normalization for slowness and ray parameter. This is a new constant to
   * resolve the dichotomy in Tn. It corresponds to Tn in the table generation.
   */
  private final double raySlownessNormalization;

  /** A double contaning the conversion of dT/dDelta to dimensional units of radians */
  private final double dTdDelta;

  /** A double containing the conversion factor for degrees to kilometers at the surface radius. */
  private final double degreesToKilometers;

  /** A double containing the depth of the upper mantle in kilometers. */
  private final double upperMantleDepth;

  /** A double containing the depth of the Moho in kilometers. */
  private final double mohoDepth;

  /** A double containing the depth of the Conrad discontinuity in kilometers. */
  private final double conradDepth;

  /** A double containing the radius of the free surface of the Earth in kilometers. */
  private final double surfaceRadius;

  /**
   * A double containing the up-going branch replacement depth in kilometers. Note that this is set
   * to the mohoDepth
   */
  private final double upGoingReplacementDepth;

  /** A double holding the typical dT/dDelta for Lg in seconds/degree. */
  private final double dTdDelta_Lg;

  /** A double holding the typical dT/dDelta for LR in seconds/degree. */
  private final double dTdDelta_LR;

  /**
   * Get the normalization for distance
   *
   * @return A double containing the normalization for distance
   */
  public double getDistanceNormalization() {
    return distanceNormalization;
  }

  /**
   * Get the normalization for velocity
   *
   * @return A double containing the normalization for velocity
   */
  public double getVelocityNormalization() {
    return velocityNormalization;
  }

  /**
   * Get the normalization for travel-time and tau.
   *
   * @return A double containing the normalization for travel-time and tau.
   */
  public double getTauTTNormalization() {
    return tauTTNormalization;
  }

  /**
   * Get dT/dDelta in dimensional units
   *
   * @return A double containing dT/dDelta in dimensional units of radians
   */
  public double get_dTdDelta() {
    return dTdDelta;
  }

  /**
   * Get the conversion factor for degrees to kilometers
   *
   * @return A double containing the conversion factor for degrees to kilometers at the surface
   *     radius.
   */
  public double getDegreesToKilometers() {
    return degreesToKilometers;
  }

  /**
   * Get the depth of the upper mantle
   *
   * @return A double containing the depth of the upper mantle in kilometers.
   */
  public double getUpperMantleDepth() {
    return upperMantleDepth;
  }

  /**
   * Get the depth of the Moho
   *
   * @return A double containing the depth of the Moho in kilometers.
   */
  public double getMohoDepth() {
    return mohoDepth;
  }

  /**
   * Get the depth of the Conrad discontinuity
   *
   * @return A double containing the depth of the Conrad discontinuity in kilometers.
   */
  public double getConradDepth() {
    return conradDepth;
  }

  /**
   * Get the radius of the free surface of the Earth
   *
   * @return A double containing the radius of the free surface of the Earth in kilometers.
   */
  public double getSurfaceRadius() {
    return surfaceRadius;
  }

  /**
   * Getthe up-going branch replacement depth
   *
   * @return A double containing the up-going branch replacement depth in kilometers.
   */
  public double getUpGoingReplacementDepth() {
    return upGoingReplacementDepth;
  }

  /**
   * Get he typical dT/dDelta for Lg
   *
   * @return A double holding the typical dT/dDelta for Lg in seconds/degree.
   */
  public double get_dTdDelta_Lg() {
    return dTdDelta_Lg;
  }

  /**
   * Get the typical dT/dDelta for Lr
   *
   * @return A double holding the typical dT/dDelta for LR in seconds/degree.
   */
  public double get_dTdDelta_LR() {
    return dTdDelta_LR;
  }

  /**
   * ModelConversions constructor, sets constants from the Fortran generated files.
   *
   * @param in A ReadTau object containing data from the Fortran *.hed and *.tbl files
   */
  public ModelConversions(ReadTau in) {
    // Set up the normalization.
    distanceNormalization = in.xNorm;
    velocityNormalization = in.pNorm;
    tauTTNormalization = in.tNorm;

    // Compute some useful constants.
    raySlownessNormalization = 1d / tauTTNormalization;
    dTdDelta = Math.toRadians(tauTTNormalization);
    surfaceRadius = in.rSurface;
    degreesToKilometers = Math.PI * surfaceRadius / 180d;
    dTdDelta_Lg = degreesToKilometers / TauUtilities.LGGRPVEL;
    dTdDelta_LR = degreesToKilometers / TauUtilities.LRGRPVEL;

    // Compute some useful depths.
    upperMantleDepth = surfaceRadius - in.rUpperMantle;
    mohoDepth = surfaceRadius - in.rMoho;
    conradDepth = surfaceRadius - in.rConrad;
    upGoingReplacementDepth = mohoDepth;
  }

  /**
   * ModelConversions constructor, sets constants from the Java generated model data.
   *
   * @param upperMantleRadius A double holding the radius of the upper mantle discontinuity in
   *     kilometers
   * @param mohoRadius A double holding the radius of the Moho discontinuity in kilometers
   * @param conradRadius A double holding the radius of the Conrad discontinuity in kilometers
   * @param surfaceRadius A double containing the free surface radius of the Earth in kilometers
   * @param surfaceShearVelocity A double holding the shear velocity in kilometers/second at the
   *     surface of the Earth
   */
  public ModelConversions(
      double upperMantleRadius,
      double mohoRadius,
      double conradRadius,
      double surfaceRadius,
      double surfaceShearVelocity) {
    distanceNormalization = 1d / surfaceRadius;
    velocityNormalization = surfaceShearVelocity;

    // Compute some useful constants.
    raySlownessNormalization = distanceNormalization * velocityNormalization;
    tauTTNormalization = 1d / raySlownessNormalization;
    dTdDelta = Math.toRadians(tauTTNormalization);
    this.surfaceRadius = surfaceRadius;
    degreesToKilometers = Math.PI * surfaceRadius / 180d;
    dTdDelta_Lg = degreesToKilometers / TauUtilities.LGGRPVEL;
    dTdDelta_LR = degreesToKilometers / TauUtilities.LRGRPVEL;

    // Compute some useful depths.
    upperMantleDepth = surfaceRadius - upperMantleRadius;
    mohoDepth = surfaceRadius - mohoRadius;
    conradDepth = surfaceRadius - conradRadius;
    upGoingReplacementDepth = mohoDepth;
  }

  /**
   * Function to compute the dimensional radius given a normalized, Earth flattened depth
   *
   * @param depth A double containing the normalized, Earth flattened depth in kilometers
   * @return A double containing the dimensional radius in kilometers
   */
  public double computeDimensionalRadius(double depth) {
    return Math.exp(depth) / distanceNormalization;
  }

  /**
   * Function to compute the dimensional depth given a normalized, Earth flattened depth
   *
   * @param depth double containing the normalized, Earth flattened depth in kilometers
   * @return A double containing the dimensional depth in kilometers
   */
  public double computeDimensionalDepth(double depth) {
    return (1d - Math.exp(depth)) / distanceNormalization;
  }

  /**
   * Function to compute the dimensional velocity given the normalized slowness and depth
   *
   * @param slowness A double containing the normalized slowness
   * @param depth A double containing the normalized, earth flattened depth in kilometers
   * @return A double containing the velocity at that depth in kilometers/second
   */
  public double computeDimensionalVelocity(double slowness, double depth) {
    return raySlownessNormalization * computeDimensionalRadius(depth) / slowness;
  }

  /**
   * Function to normalize radius (or depth) into units of the radius of the Earth
   *
   * @param radius A double containing the radius or depth in kilometers
   * @return A double containing the non-dimensional radius or depth in units of the radius of the
   *     Earth
   */
  public double normalizeRadius(double radius) {
    return distanceNormalization * radius;
  }

  /**
   * Function to convert non-dimensional radius (or depth) into kilometers.
   *
   * @param radius A double containing the non-dimensional radius or depth in units of the radius of
   *     the Earth
   * @return A doube containing the dimensional radius or depth in kilometers
   */
  public double convertDimensionalRadius(double radius) {
    return radius / distanceNormalization;
  }

  /**
   * Function to compute the normalized, Earth flattened depth given a dimensional radius
   *
   * @param radius A double containing the radius in kilometers
   * @return A double containing the normalized, Earth flattened depth in kilometers
   */
  public double computeFlatDepth(double radius) {
    return Math.log(distanceNormalization * radius);
  }

  /**
   * Function to compute the normalized slowness given the normalized velocity and radius
   *
   * @param velocity A double containing the velocity at radius in kilometers/second
   * @param radius A double holding the radius in kilometers
   * @return A double containing the normalized slowness
   */
  public double computeFlatSlowness(double velocity, double radius) {
    return raySlownessNormalization * radius / velocity;
  }
}
