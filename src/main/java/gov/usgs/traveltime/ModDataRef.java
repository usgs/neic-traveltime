package gov.usgs.traveltime;

import gov.usgs.traveltime.tables.TauModel;
import gov.usgs.traveltime.tables.TauSample;
import java.io.Serializable;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Store Earth model data for one wave type. Note that the model is normalized and the depths have
 * undergone a flat Earth transformation. Also, only the upper 800 kilometers of the Earth model are
 * available here. The reference version contains only information that is invariant.
 *
 * @author Ray Buland
 */
public class ModDataRef implements Serializable {
  private static final long serialVersionUID = 1L;
  final char typeMod; // Type of model ('P' or 'S')
  final double[] zMod; // Flat Earth depths
  final double[] pMod; // Slowness samples
  final int[] indexUp; // Index into the up-going branch data
  final ModConvert cvt; // Model dependent conversions

  /** Private logging object. */
  private static final Logger LOGGER = Logger.getLogger(ModDataRef.class.getName());

  /**
   * Load data from the FORTRAN file reader for the Earth model for one wave type. The file data
   * should have already been loaded from the *.hed and *.tbl files.
   *
   * @param in Branch input data source.
   * @param cvt The Earth model units converter
   * @param typeMod Wave type ('P' or 'S')
   */
  public ModDataRef(ReadTau in, ModConvert cvt, char typeMod) {
    this.typeMod = typeMod;
    this.cvt = cvt;

    if (typeMod == 'P') {
      zMod = Arrays.copyOf(in.zMod[0], in.zMod[0].length);
      pMod = Arrays.copyOf(in.pMod[0], in.pMod[0].length);
      indexUp = Arrays.copyOf(in.indexMod[0], in.indexMod[0].length);
      for (int j = 0; j < indexUp.length; j++) {
        indexUp[j]--;
      }
    } else {
      zMod = Arrays.copyOf(in.zMod[1], in.zMod[1].length);
      pMod = Arrays.copyOf(in.pMod[1], in.pMod[1].length);
      indexUp = Arrays.copyOf(in.indexMod[1], in.indexMod[1].length);
      indexUp[0] = -1;
      for (int j = indexUp.length - 1; j >= 1; j--) {
        indexUp[j] = indexUp[j] - indexUp[1];
      }
    }
  }

  /**
   * Load data for the Earth model for one wave type from the table generation process.
   *
   * @param finModel Travel-time table generation final tau model
   * @param cvt The Earth model units converter
   * @param typeMod Wave type ('P' or 'S')
   */
  public ModDataRef(TauModel finModel, ModConvert cvt, char typeMod) {
    int n, indexOffset;
    TauSample sample;

    this.typeMod = typeMod;
    this.cvt = cvt;

    n = finModel.size(typeMod);
    zMod = new double[n];
    pMod = new double[n];
    indexUp = new int[n - 3];
    sample = finModel.getSample(typeMod, 1);
    indexOffset = sample.getIndex();
    for (int j = 0; j < n; j++) {
      sample = finModel.getSample(typeMod, j);
      zMod[j] = sample.getZ();
      pMod[j] = sample.getSlow();
      if (j < n - 3) indexUp[j] = Math.max(sample.getIndex() - indexOffset, -1);
    }
  }

  /**
   * Get the non-dimensional depth corresponding to an up-going branch.
   *
   * @param iUp Index of the up-going branch
   * @return Non-dimensional depth
   */
  public double getDepth(int iUp) {
    for (int j = 0; j < indexUp.length; j++) {
      if (iUp == indexUp[j]) {
        return zMod[j];
      }
    }
    return Double.NaN;
  }

  /**
   * Print out model data for debugging purposes.
   *
   * @param nice If true print in dimensional units.
   */
  public void dumpMod(boolean nice) {
    String modelString = "\n     " + typeMod + " Model:";

    if (nice) {
      modelString += "\n         Z      p   index";

      for (int j = 0; j < indexUp.length; j++) {
        modelString +=
            String.format(
                "%3d: %6.1f  %5.2f  %3d\n",
                j, cvt.realZ(zMod[j]), cvt.realV(pMod[j], zMod[j]), indexUp[j]);
      }

      for (int j = indexUp.length; j < pMod.length - 1; j++) {
        modelString +=
            String.format(
                "%3d: %6.1f  %5.2f\n", j, cvt.realZ(zMod[j]), cvt.realV(pMod[j], zMod[j]));
      }

      modelString +=
          String.format(
              "%3d: center  %5.2f\n",
              pMod.length - 1, cvt.realV(pMod[pMod.length - 1], zMod[zMod.length - 1]));
    } else {
      modelString += "\n          Z         p     index";

      for (int j = 0; j < indexUp.length; j++) {
        modelString += String.format("%3d: %9.6f  %8.6f  %3d\n", j, zMod[j], pMod[j], indexUp[j]);
      }

      for (int j = indexUp.length; j < pMod.length - 1; j++) {
        modelString += String.format("%3d: %9.6f  %8.6f\n", j, zMod[j], pMod[j]);
      }

      modelString +=
          String.format("%3d: -infinity  %8.6f\n", pMod.length - 1, pMod[pMod.length - 1]);
    }

    LOGGER.fine(modelString);
  }
}
