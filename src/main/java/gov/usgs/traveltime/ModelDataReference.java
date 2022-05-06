package gov.usgs.traveltime;

import gov.usgs.traveltime.tables.TauModel;
import gov.usgs.traveltime.tables.TauSample;
import java.io.Serializable;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * The ModelDataReference class stores Earth model data for one wave type. Note that the model is
 * normalized and the depths have undergone a flat Earth transformation. Also, only the upper 800
 * kilometers of the Earth model are available here. The reference version contains only information
 * that is invariant.
 *
 * @author Ray Buland
 */
public class ModelDataReference implements Serializable {
  /** A long containing the version id used in serialization */
  private static final long serialVersionUID = 1L;

  /** A char containing the model wave type ('P' = compressional, 'S' = shear) */
  private final char waveType;

  /** An array of double values containing the flat earth depths for this model */
  private final double[] modelDepths;

  /** An array of double values containing the slowness samples for this model */
  private final double[] modelSlownesses;

  /** An array of integer values containing the indexes into the up-going branch data */
  private final int[] upGoingIndexes;

  /** A ModelConversions object containing model dependent constants and conversions */
  private final ModelConversions modelConversions;

  /** Private logging object. */
  private static final Logger LOGGER = Logger.getLogger(ModelDataReference.class.getName());

  /**
   * Function to return the model wave type
   *
   * @return A char containing the model wave type ('P' = compressional, 'S' = shear)
   */
  public char getWaveType() {
    return waveType;
  }

  /**
   * Function to return the model flat earth depths
   *
   * @return An array of double values containing the flat earth depths for this model
   */
  public double[] getModelDepths() {
    return modelDepths;
  }

  /**
   * Function to return the model slowness samples
   *
   * @return An array of double values containing the slowness samples for this model
   */
  public double[] getModelSlownesses() {
    return modelSlownesses;
  }

  /**
   * Function to return the indexes into the up-going branch data
   *
   * @return An array of integer values containing the indexes into the up-going branch data
   */
  public int[] getUpGoingIndexes() {
    return upGoingIndexes;
  }

  /**
   * ModelDataReference constuctor, loads data via the FORTRAN file reader for the Earth model for
   * one wave type. The file data should have already been loaded from the *.hed and *.tbl files.
   *
   * @param in A ReadTau object containing data from the Fortran *.hed and *.tbl files
   * @param modelConversions A ModelConversions object holding the model conversion factors and
   *     functions
   * @param waveType A char containing the model wave type ('P' = compressional, 'S' = shear)
   */
  public ModelDataReference(ReadTau in, ModelConversions modelConversions, char waveType) {
    this.waveType = waveType;
    this.modelConversions = modelConversions;

    if (waveType == 'P') {
      modelDepths = Arrays.copyOf(in.zMod[0], in.zMod[0].length);
      modelSlownesses = Arrays.copyOf(in.pMod[0], in.pMod[0].length);
      upGoingIndexes = Arrays.copyOf(in.indexMod[0], in.indexMod[0].length);

      for (int j = 0; j < upGoingIndexes.length; j++) {
        upGoingIndexes[j]--;
      }
    } else {
      modelDepths = Arrays.copyOf(in.zMod[1], in.zMod[1].length);
      modelSlownesses = Arrays.copyOf(in.pMod[1], in.pMod[1].length);
      upGoingIndexes = Arrays.copyOf(in.indexMod[1], in.indexMod[1].length);
      upGoingIndexes[0] = -1;

      for (int j = upGoingIndexes.length - 1; j >= 1; j--) {
        upGoingIndexes[j] = upGoingIndexes[j] - upGoingIndexes[1];
      }
    }
  }

  /**
   * ModelDataReference constructor, loads data for the Earth model for one wave type from the table
   * generation process.
   *
   * @param finModel Travel-time table generation final tau model
   * @param modelConversions A ModelConversions object holding the model conversion factors and
   *     functions
   * @param waveType A char containing the model wave type ('P' = compressional, 'S' = shear)
   */
  public ModelDataReference(TauModel finModel, ModelConversions modelConversions, char waveType) {
    this.waveType = waveType;
    this.modelConversions = modelConversions;

    int n = finModel.size(waveType);
    modelDepths = new double[n];
    modelSlownesses = new double[n];
    upGoingIndexes = new int[n - 3];
    TauSample sample = finModel.getSample(waveType, 1);
    int indexOffset = sample.getIndex();

    for (int j = 0; j < n; j++) {
      sample = finModel.getSample(waveType, j);
      modelDepths[j] = sample.getDepth();
      modelSlownesses[j] = sample.getSlowness();

      if (j < n - 3) {
        upGoingIndexes[j] = Math.max(sample.getIndex() - indexOffset, -1);
      }
    }
  }

  /**
   * Function to get the non-dimensional depth corresponding to an up-going branch.
   *
   * @param upGoingIndex An integer Index of the up-going branch
   * @return A double containing the Non-dimensional depth
   */
  public double getUpGoingDepth(int upGoingIndex) {
    for (int j = 0; j < upGoingIndexes.length; j++) {
      if (upGoingIndex == upGoingIndexes[j]) {
        return modelDepths[j];
      }
    }

    return Double.NaN;
  }

  /**
   * Function to print out model data for debugging purposes.
   *
   * @param nice A boolean flag, if true print in dimensional units.
   */
  public void dumpModel(boolean nice) {
    String modelString = "\n     " + waveType + " Model:";

    if (nice) {
      modelString += "\n         Z      p   index";

      for (int j = 0; j < upGoingIndexes.length; j++) {
        modelString +=
            String.format(
                "%3d: %6.1f  %5.2f  %3d\n",
                j,
                modelConversions.computeDimensionalDepth(modelDepths[j]),
                modelConversions.computeDimensionalVelocity(modelSlownesses[j], modelDepths[j]),
                upGoingIndexes[j]);
      }

      for (int j = upGoingIndexes.length; j < modelSlownesses.length - 1; j++) {
        modelString +=
            String.format(
                "%3d: %6.1f  %5.2f\n",
                j,
                modelConversions.computeDimensionalDepth(modelDepths[j]),
                modelConversions.computeDimensionalVelocity(modelSlownesses[j], modelDepths[j]));
      }

      modelString +=
          String.format(
              "%3d: center  %5.2f\n",
              modelSlownesses.length - 1,
              modelConversions.computeDimensionalVelocity(
                  modelSlownesses[modelSlownesses.length - 1],
                  modelDepths[modelDepths.length - 1]));
    } else {
      modelString += "\n          Z         p     index";

      for (int j = 0; j < upGoingIndexes.length; j++) {
        modelString +=
            String.format(
                "%3d: %9.6f  %8.6f  %3d\n",
                j, modelDepths[j], modelSlownesses[j], upGoingIndexes[j]);
      }

      for (int j = upGoingIndexes.length; j < modelSlownesses.length - 1; j++) {
        modelString += String.format("%3d: %9.6f  %8.6f\n", j, modelDepths[j], modelSlownesses[j]);
      }

      modelString +=
          String.format(
              "%3d: -infinity  %8.6f\n",
              modelSlownesses.length - 1, modelSlownesses[modelSlownesses.length - 1]);
    }

    LOGGER.fine(modelString);
  }
}
