package gov.usgs.traveltime;

import java.io.Serializable;

/**
 * The Ellipticity class implements the Dziewonski &amp; Gilbert Ellipticity correction algorithm
 * for one phase.
 *
 * @author Ray Buland
 */
public class Ellipticity implements Serializable {
  /** A long containing the version id used in serialization */
  private static final long serialVersionUID = 1L;

  /** A String containing the phase code */
  private final String phaseCode;

  /** A double containing the minimum distance in degrees */
  private final double minimumDistance;

  /** A double containing the maximum distance in degrees */
  private final double maximumDistance;

  /**
   * A two dimensional array of double values containing the distance-sourceDepth array for
   * parameter t0
   */
  private final double[][] t0;

  /**
   * A two dimensional array of double values containing the distance-sourceDepth array for
   * parameter t1
   */
  private final double[][] t1;

  /**
   * A two dimensional array of double values containing the distance-sourceDepth array for
   * parameter t2
   */
  private final double[][] t2;

  /** An EllipticityDistances object containing the array of distance values */
  private EllipticityDistances distanceValues;

  /** An EllipticityDepths object containing the array of sourceDepth values */
  private EllipticityDepths depthValues;

  /**
   * Function to return the phase code
   *
   * @return A String containing the phase code
   */
  public String getPhaseCode() {
    return phaseCode;
  }

  /**
   * Ellipticity constructor, sets ellipticity correction data for one phase.
   *
   * @param phaseCode A String containing the phase code
   * @param minimumDistance A double containing the desired minimum distance in degrees
   * @param maximumDistance A double containing the desired maximum distance in degrees
   * @param t0 A two dimensional array of double values containing the desired distance-sourceDepth
   *     array for parameter t0
   * @param t1 A two dimensional array of double values containing the desired distance-sourceDepth
   *     array for parameter t1
   * @param t2 A two dimensional array of double values containing the desired distance-sourceDepth
   *     array for parameter t2
   */
  public Ellipticity(
      String phaseCode,
      double minimumDistance,
      double maximumDistance,
      double[][] t0,
      double[][] t1,
      double[][] t2) {
    this.phaseCode = phaseCode;
    this.minimumDistance = minimumDistance;
    this.maximumDistance = maximumDistance;
    this.t0 = t0;
    this.t1 = t1;
    this.t2 = t2;

    distanceValues = new EllipticityDistances(minimumDistance, maximumDistance);
    depthValues = new EllipticityDepths();
  }

  /**
   * Function to calculate the ellipticity correction.
   *
   * @param sourceLatitude A double containing the source latitude in degrees
   * @param sourceDepth A double containing the source sourceDepth in kilometers
   * @param recieverDistance A double containing the source-receiver distance in degrees
   * @param recieverAzimuth A double containing the azimuth of the receiver from the source in
   *     degrees
   * @return A double containing the ellipticity correction in seconds
   */
  public double getEllipticityCorrection(
      double sourceLatitude, double sourceDepth, double recieverDistance, double recieverAzimuth) {

    // Create a few parameters.
    double coLat = Math.toRadians(90d - sourceLatitude);
    double sc0 = 0.25d * (1d + 3d * Math.cos(2d * coLat));
    double sc1 = Math.sqrt(3d) * Math.sin(2d * coLat) / 2d;
    double sc2 = Math.sqrt(3d) * Math.pow(Math.sin(coLat), 2d) / 2d;

    // Interpolate the tau functions.
    double tau0 =
        TauUtilities.biLinearInterpolation(
            recieverDistance, sourceDepth, distanceValues, depthValues, t0);
    double tau1 =
        TauUtilities.biLinearInterpolation(
            recieverDistance, sourceDepth, distanceValues, depthValues, t1);
    double tau2 =
        TauUtilities.biLinearInterpolation(
            recieverDistance, sourceDepth, distanceValues, depthValues, t2);

    // Compute the correction.
    double EllipticityCorr =
        sc0 * tau0
            + sc1 * Math.cos(Math.toRadians(recieverAzimuth)) * tau1
            + sc2 * Math.cos(2d * Math.toRadians(recieverAzimuth)) * tau2;

    return EllipticityCorr;
  }
}
