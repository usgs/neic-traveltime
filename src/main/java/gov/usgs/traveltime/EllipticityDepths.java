package gov.usgs.traveltime;

import java.io.Serializable;

/**
 * Virtual array of Ellipticity correction sample depths. In this case the virtual array maps to a
 * physical array.
 *
 * @author Ray Buland
 */
public class EllipticityDepths implements GeneralizedIndex, Serializable {
  private static final long serialVersionUID = 1L;
  // Ellipticity depths in kilometers.
  final double[] EllipticityDepths = {0d, 100d, 200d, 300d, 500d, 700d};

  @Override
  public int getIndex(double value) {
    // Get the array index for this value.
    int indDep = 1;
    for (; indDep < EllipticityDepths.length; indDep++) {
      if (value <= EllipticityDepths[indDep]) break;
    }
    return Math.min(--indDep, EllipticityDepths.length - 2);
  }

  @Override
  public double getValue(int index) {
    // Get the value for this index.
    return EllipticityDepths[index];
  }
}
