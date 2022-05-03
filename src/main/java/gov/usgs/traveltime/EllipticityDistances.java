package gov.usgs.traveltime;

import java.io.Serializable;

/**
 * The EllipticityDistances class contains a virtual array of Ellipticity correction sample
 * distances. This is needed for the bilinear interpolation because the Ellipticity tables are set
 * up to go from the minimum to the maximum distance in implied 5 degree increments.
 *
 * @author Ray Buland
 */
public class EllipticityDistances implements GeneralizedIndex, Serializable {
  /** A long containing the version id used in serialization */
  private static final long serialVersionUID = 1L;

  /** A double containing the ellipticity distance increment */
  private final double distanceIncrement = 5d;

  /** A double containing the minimum distance in degrees */
  private final double minimumDistance;

  /** A double containing the maximum distance in degrees */
  private final double maximumDistance;

  /** An integer containing the maximum index given the distance range */
  private int maximumIndex;

  /**
   * The EllipticityDistances constructor, sets up the distance range.
   *
   * @param minimumDistance A double containing the minimum distance in degrees
   * @param maximumDistance A double containing the maximum distance in degrees
   */
  public EllipticityDistances(double minimumDistance, double maximumDistance) {
    this.minimumDistance = minimumDistance;
    this.maximumDistance = maximumDistance;

    maximumIndex = (int) ((maximumDistance - minimumDistance) / distanceIncrement - 0.5d);
  }

  /**
   * Function to return a distance index given a distance. Note this function overrides the
   * getIndex() function from GeneralizedIndex
   *
   * @param distance A double containing the desired distance in degrees
   * @return An integer containing the distance index
   */
  @Override
  public int getIndex(double distance) {
    // Get the virtual array index.
    return Math.min(
        (int) Math.max((distance - minimumDistance) / distanceIncrement, 0d), maximumIndex);
  }

  /**
   * Function to return the distance given a distance index. Note this function overrides the
   * getValue() function from GeneralizedIndex
   *
   * @param index An integer containing the distance index
   * @return A double ccontaining the distance in degrees
   */
  @Override
  public double getValue(int index) {
    // Get the virtual array distance.
    return minimumDistance + index * distanceIncrement;
  }
}
