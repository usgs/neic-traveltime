package gov.usgs.traveltime;

import java.io.Serializable;

/**
 * The EllipticityDepths class contains a virtual array of ellipticity correction sample depths. In
 * this case the virtual array maps to a physical array.
 *
 * @author Ray Buland
 */
public class EllipticityDepths implements GeneralizedIndex, Serializable {
  /** A long containing the version id used in serialization */
  private static final long serialVersionUID = 1L;

  // An array of double values containing the ellipticity depths in kilometers.
  private final double[] ellipticityDepths = {0d, 100d, 200d, 300d, 500d, 700d};

  /**
   * Function to return the ellipticity depths
   *
   * @return An array of double values containing the ellipticity depths in kilometers.
   */
  public double[] getEllipticityDepths() {
    return ellipticityDepths;
  }

  /**
   * Function to return a depth index given a depth. Note this function overrides the getIndex()
   * function from GeneralizedIndex
   *
   * @param depth A double containing the desired depth in kilometers
   * @return An integer containing the depth index
   */
  @Override
  public int getIndex(double depth) {
    // Get the array index for this depth.
    int depthIndex = 1;

    for (; depthIndex < ellipticityDepths.length; depthIndex++) {
      if (depth <= ellipticityDepths[depthIndex]) {
        break;
      }
    }

    return Math.min(--depthIndex, ellipticityDepths.length - 2);
  }

  /**
   * Function to return the depth given a depth index. Note this function overrides the getValue()
   * function from GeneralizedIndex
   *
   * @param index An integer containing the depth index
   * @return A double ccontaining the depth in kilometers
   */
  @Override
  public double getValue(int index) {
    // Get the depth for this index.
    return ellipticityDepths[index];
  }
}
