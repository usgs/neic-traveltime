package gov.usgs.traveltime;

import java.util.logging.Logger;

/**
 * Store Earth model data for one wave type. Note that the model is normalized and the depths have
 * undergone a flat Earth transformation. Also, only the upper 800 kilometers of the Earth model are
 * available here. The reference version contains only information that is invariant.
 *
 * @author Ray Buland
 */
public class ModelDataVolume {
  int iSource; // Model index of the current source depth
  boolean onModelGrid; // True if the source depth is exactly on a grid point.
  double pFound, zFound, pMax = Double.NaN;
  ModelDataReference ref; // Non-volatile model information
  ModelConversions cvt; // Model dependent conversion factors

  /** Private logging object. */
  private static final Logger LOGGER = Logger.getLogger(ModelDataVolume.class.getName());

  /**
   * Load data from the FORTRAN file reader for the Earth model for one wave type. The file data
   * should have already been loaded from the *.hed and *.tbl files.
   *
   * @param ref Model reference data source
   * @param cvt The Earth model units converter
   */
  public ModelDataVolume(ModelDataReference ref, ModelConversions cvt) {
    this.ref = ref;
    this.cvt = cvt;
  }

  /**
   * Find the model slowness for a desired depth.
   *
   * @param z Desired normalized, flattened depth
   * @return Normalized slowness at the desired depth
   * @throws BadDepthException If the desired depth is too deep
   */
  public double findP(double z) throws BadDepthException {
    // Search the model to bracket the source depth.
    for (iSource = 0; iSource < ref.indexUp.length; iSource++) {
      if (ref.zMod[iSource] <= z) break;
    }
    // If we went off the end of the model, throw and exception.
    if (iSource >= ref.indexUp.length) {
      System.out.println("findP: source depth is too deep");
      throw new BadDepthException(String.format("%3.1f km", cvt.realZ(z)));
    }
    zFound = z;
    pMax = Double.NaN;
    // If we're on a grid point, return that.
    if (Math.abs(z - ref.zMod[iSource]) <= TauUtilities.DTOL) {
      onModelGrid = true;
      pFound = ref.pMod[iSource];
    }
    // Otherwise interpolate to find the correct slowness.
    else {
      pFound =
          ref.pMod[iSource - 1]
              + (ref.pMod[iSource] - ref.pMod[iSource - 1])
                  * (Math.exp(z - ref.zMod[iSource - 1]) - 1d)
                  / (Math.exp(ref.zMod[iSource] - ref.zMod[iSource - 1]) - 1d);
      onModelGrid = false;
    }
    return pFound;
  }

  /**
   * Find the model depth for a desired slowness.
   *
   * @param p Desired normalized model slowness
   * @param first If true, find the top of a low velocity zone, if false, find the bottom
   * @return Normalized depth at the desired slowness
   * @throws BadDepthException If the desired slowness is too small
   */
  public double findZ(double p, boolean first) throws BadDepthException {
    // Search the model to bracket the source depth.
    if (first) {
      if (p > ref.pMod[0]) {
        throw new BadDepthException(String.format("< %3.1f km", cvt.realZ(ref.zMod[0])));
      }
      for (iSource = 0; iSource < ref.indexUp.length; iSource++) {
        if (ref.pMod[iSource] <= p) break;
      }
    } else {
      for (iSource = ref.indexUp.length - 1; iSource >= 0; iSource--) {
        if (ref.pMod[iSource] >= p) {
          if (Math.abs(ref.pMod[iSource] - p) <= TauUtilities.DTOL) iSource++;
          break;
        }
      }
    }
    // If we went off the end of the model, throw and exception.
    if (iSource >= ref.indexUp.length || iSource < 0) {
      System.out.println("findZ: source depth not found.");
      throw new BadDepthException(
          String.format("> %f3.1f km", cvt.realZ(ref.zMod[ref.indexUp.length - 1])));
    }
    pFound = p;
    // If we're on a grid point, return that.
    if (Math.abs(p - ref.pMod[iSource]) <= TauUtilities.DTOL) {
      zFound = ref.zMod[iSource];
      onModelGrid = true;
    }
    // Otherwise interpolate to find the correct slowness.
    else {
      zFound =
          ref.zMod[iSource - 1]
              + Math.log(
                  Math.max(
                      (p - ref.pMod[iSource - 1])
                              * (Math.exp(ref.zMod[iSource] - ref.zMod[iSource - 1]) - 1d)
                              / (ref.pMod[iSource] - ref.pMod[iSource - 1])
                          + 1d,
                      TauUtilities.DMIN));
      onModelGrid = false;
    }
    return zFound;
  }

  /**
   * Find the maximum slowness between the surface and the source. If the source is in a low
   * velocity zone, this will be the slowness at the top. Otherwise, it will be the source slowness.
   * Note that the parameters determined by the last call to findP is assumed.
   *
   * @return The normalized maximum slowness above the source
   */
  public double findMaxP() {
    pMax = pFound;
    for (int j = 0; j < iSource; j++) {
      pMax = Math.min(pMax, ref.pMod[j]);
    }
    return pMax;
  }

  /**
   * Getter for the on a model grid point boolean.
   *
   * @return True if the source is exactly on a model grid point.
   */
  public boolean getOnGrid() {
    return onModelGrid;
  }

  /**
   * Get an element of the depth array.
   *
   * @param j Array index
   * @return Non-dimensional Earth flattened depth
   */
  public double getDepth(int j) {
    return ref.zMod[j];
  }

  /**
   * Get an element of the slowness array.
   *
   * @param j Array index
   * @return Non-dimensional model slowness
   */
  public double getP(int j) {
    return ref.pMod[j];
  }

  /**
   * Print the result of the latest findP or findZ call.
   *
   * @param nice If true, convert the model to SI units
   */
  public void printFind(boolean nice) {
    if (nice) {
      if (Double.isNaN(pMax)) {
        System.out.format(
            "\nFind: type = %c  isource = %d  z = %5.1f  " + "v = %4.1f  onGrid = %b\n",
            ref.typeMod, iSource, cvt.realZ(zFound), cvt.realV(pFound, zFound), onModelGrid);
      } else {
        System.out.format(
            "\nFind: type = %c  isource = %d  z = %5.1f  "
                + "v = %4.1f  vMax = %4.1f  onGrid = %b\n",
            ref.typeMod,
            iSource,
            cvt.realZ(zFound),
            cvt.realV(pFound, zFound),
            cvt.realV(pMax, zFound),
            onModelGrid);
      }
    } else {
      if (Double.isNaN(pMax)) {
        System.out.format(
            "\nFind: type = %c  isource = %d  z = %9.6f  " + "p = %8.6f  onGrid = %b\n",
            ref.typeMod, iSource, zFound, pFound, onModelGrid);
      } else {
        System.out.format(
            "\nFind: type = %c  isource = %d  z = %9.6f  "
                + "p = %8.6f  pMax = %8.6f  onGrid = %b\n",
            ref.typeMod, iSource, zFound, pFound, pMax, onModelGrid);
      }
    }
  }
}
