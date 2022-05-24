package gov.usgs.traveltime;

import java.io.Serializable;

/**
 * The TopographicLatitudes class encapsulates a virtual array of topographic sample latitudes. It
 * would have been nicer to have used the limits in the topography file header, but sadly they
 * weren't accurate enough. Note that instead of interpolating, extrapolation will be needed at the
 * poles. Also, note that latitude is stored from the north pole to the south pole making
 * interpolation a little odd.
 *
 * @author Ray Buland
 */
public class TopographicLatitudes implements GeneralizedIndex, Serializable {
  /** A long containing the version id used in serialization */
  private static final long serialVersionUID = 1L;

  /** A double value containing the minimum possible latitude in degrees */
  private final double minimumLatitude = -89.8333333d;

  /** A double value containing the maximum possible latitude in degrees */
  private final double maximumLatitude = 89.8333333d;

  /** A double value containing the latitude step in degrees */
  private final double latitudeStep = 0.3333333d;

  /** An integer value containing the maximum latitude index */
  private final int maximumIndex = 538;

  /**
   * Function to calculate latitude index from a value
   *
   * @param value A double containing the latitude in degrees to use
   * @return An integer containing the computed index
   */
  @Override
  public int getIndex(double value) {
    // Get the virtual array index.
    return Math.min((int) Math.max((-value - minimumLatitude) / latitudeStep, 0d), maximumIndex);
  }

  /**
   * Function to calculate latitude value from an index
   *
   * @param index An integer containing the index to use
   * @return A double containing the calculated latitude in degrees
   */
  @Override
  public double getValue(int index) {
    // Get the virtual array value.
    return maximumLatitude - index * latitudeStep;
  }
}
