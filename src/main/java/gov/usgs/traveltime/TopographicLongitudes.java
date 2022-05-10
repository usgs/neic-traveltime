package gov.usgs.traveltime;

import java.io.Serializable;

/**
 * The TopographicLongitudes class encapsulates a virtual array of topographic sample longitudes. It
 * would have been nicer to have used the limits in the topography file header, but sadly they
 * weren't accurate enough. Note that the first and last points of the array have been duplicated to
 * avoid the wrap around at +/-180 degrees.
 *
 * @author Ray Buland
 */
public class TopographicLongitudes implements GeneralizedIndex, Serializable {
  /** A long containing the version id used in serialization */
  private static final long serialVersionUID = 1L;

  /** A double value containing the minimum possible longitude in degrees */
  private final double minimumLongitude = -180.1666667;

  /** A double value containing the maximum possible longitude in degrees */
  private final double maximumLongitude = 180.1666667d;

  /** A double value containing the longitude step in degrees */
  private final double longitudeStep = 0.3333333d;

  /** An integer value containing the maximum longitude index */
  private final int maximumIndex = 1080;

  /**
   * Function to calculate longitude index from a value
   *
   * @param value A double containing the longitude in degrees to use
   * @return An integer containing the computed index
   */
  @Override
  public int getIndex(double value) {
    // Get the virtual array index.
    return Math.min((int) Math.max((value - minimumLongitude) / longitudeStep, 0d), maximumIndex);
  }

  /**
   * Function to calculate longitude value from an index
   *
   * @param index An integer containing the index to use
   * @return A double containing the calculated longitude in degrees
   */
  @Override
  public double getValue(int index) {
    // Get the virtual array value.
    return minimumLongitude + index * longitudeStep;
  }
}
