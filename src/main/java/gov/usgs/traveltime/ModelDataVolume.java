package gov.usgs.traveltime;

import java.util.logging.Logger;

/**
 * The ModelDataVolume class stores Earth model data for one wave type. Note that the model is
 * normalized and the depths have undergone a flat Earth transformation. Also, only the upper 800
 * kilometers of the Earth model are available here. The reference version contains only information
 * that is invariant.
 *
 * @author Ray Buland
 */
public class ModelDataVolume {
  /** An integer containing the model index of the current source depth */
  private int currentSourceDepthIndex;

  /** A boolean flag indicating whether the source depth is exactly on a grid point. */
  private boolean sourceDepthOnGridPoint;

  /**
   * A double containing the slowness found by the last call of the findDepth, findSlowness, or
   * findMaxSlowness functions
   */
  private double foundSlowness = Double.NaN;

  /**
   * A double containing the depth found by the last call of the findDepth or findSlowness functions
   */
  private double foundDepth = Double.NaN;

  /**
   * A double containing the maximum slowness found by the last call of the findSlowness or
   * findMaxSlowness functions
   */
  private double maximumSlowness = Double.NaN;

  /** A ModelDataReference object containing the non-volatile model information */
  private ModelDataReference modelReference;

  /** A ModelConversions object containing model dependent constants and conversions */
  private final ModelConversions modelConversions;

  /** Private logging object. */
  private static final Logger LOGGER = Logger.getLogger(ModelDataVolume.class.getName());

  /**
   * Get the non-volatile model information
   *
   * @return A ModelDataReference object containing the non-volatile model information
   */
  public ModelDataReference getModelReference() {
    return modelReference;
  }

  /**
   * Get the model index of the current source depth
   *
   * @return An integer containing the model index of the current source depth
   */
  public int getCurrentSourceDepthIndex() {
    return currentSourceDepthIndex;
  }

  /**
   * Get whether the source depth is exactly on a grid point.
   *
   * @return A boolean flag, True if the source is exactly on a model grid point.
   */
  public boolean getSourceDepthOnGridPoint() {
    return sourceDepthOnGridPoint;
  }

  /**
   * Function to get an element of the depth array.
   *
   * @param index An integer containing the array index
   * @return A double containing the non-dimensional Earth flattened depth
   */
  public double getDepth(int index) {
    return modelReference.getModelDepths()[index];
  }

  /**
   * Function to get an element of the slowness array.
   *
   * @param index An integer containing the array index
   * @return A double containing the non-dimensional model slowness
   */
  public double getP(int index) {
    return modelReference.getModelSlownesses()[index];
  }

  /**
   * ModelDataVolume Constructor, sets the model reference and conversions
   *
   * @param modelReference A ModelDataReference object holding the model reference data source
   * @param modelConversions A ModelConversions object containing model dependent constants and
   *     conversions
   */
  public ModelDataVolume(ModelDataReference modelReference, ModelConversions modelConversions) {
    this.modelReference = modelReference;
    this.modelConversions = modelConversions;
  }

  /**
   * Function to find the model slowness for a desired depth.
   *
   * @param depth A double holding the desired normalized, flattened depth
   * @return A double containing the normalized slowness at the desired depth
   * @throws BadDepthException If the desired depth is too deep
   */
  public double findSlowness(double depth) throws BadDepthException {
    // Search the model to bracket the source depth.
    for (currentSourceDepthIndex = 0;
        currentSourceDepthIndex < modelReference.getUpGoingIndexes().length;
        currentSourceDepthIndex++) {
      if (modelReference.getModelDepths()[currentSourceDepthIndex] <= depth) {
        break;
      }
    }

    // If we went off the end of the model, throw and exception.
    if (currentSourceDepthIndex >= modelReference.getUpGoingIndexes().length) {
      System.out.println("findSlowness: source depth is too deep");
      throw new BadDepthException(
          String.format("%3.1f km", modelConversions.computeDimensionalDepth(depth)));
    }

    foundDepth = depth;
    maximumSlowness = Double.NaN;

    // If we're on a grid point, return that.
    if (Math.abs(depth - modelReference.getModelDepths()[currentSourceDepthIndex])
        <= TauUtilities.DOUBLETOLERANCE) {
      sourceDepthOnGridPoint = true;
      foundSlowness = modelReference.getModelSlownesses()[currentSourceDepthIndex];
    } else {
      // Otherwise interpolate to find the correct slowness.
      foundSlowness =
          modelReference.getModelSlownesses()[currentSourceDepthIndex - 1]
              + (modelReference.getModelSlownesses()[currentSourceDepthIndex]
                      - modelReference.getModelSlownesses()[currentSourceDepthIndex - 1])
                  * (Math.exp(depth - modelReference.getModelDepths()[currentSourceDepthIndex - 1])
                      - 1d)
                  / (Math.exp(
                          modelReference.getModelDepths()[currentSourceDepthIndex]
                              - modelReference.getModelDepths()[currentSourceDepthIndex - 1])
                      - 1d);
      sourceDepthOnGridPoint = false;
    }

    return foundSlowness;
  }

  /**
   * Function to find the model depth for a desired slowness.
   *
   * @param slowness A double containing the desired normalized model slowness
   * @param findLVZTop A boolean flag, if true, find the top of a low velocity zone, if false, find
   *     the bottom
   * @return A double containing the normalized depth at the desired slowness
   * @throws BadDepthException If the desired slowness is too small
   */
  public double findDepth(double slowness, boolean findLVZTop) throws BadDepthException {
    // Search the model to bracket the source depth.
    if (findLVZTop) {
      if (slowness > modelReference.getModelSlownesses()[0]) {
        throw new BadDepthException(
            String.format(
                "< %3.1f km",
                modelConversions.computeDimensionalDepth(modelReference.getModelDepths()[0])));
      }

      for (currentSourceDepthIndex = 0;
          currentSourceDepthIndex < modelReference.getUpGoingIndexes().length;
          currentSourceDepthIndex++) {
        if (modelReference.getModelSlownesses()[currentSourceDepthIndex] <= slowness) {
          break;
        }
      }
    } else {
      for (currentSourceDepthIndex = modelReference.getUpGoingIndexes().length - 1;
          currentSourceDepthIndex >= 0;
          currentSourceDepthIndex--) {
        if (modelReference.getModelSlownesses()[currentSourceDepthIndex] >= slowness) {
          if (Math.abs(modelReference.getModelSlownesses()[currentSourceDepthIndex] - slowness)
              <= TauUtilities.DOUBLETOLERANCE) currentSourceDepthIndex++;
          break;
        }
      }
    }

    // If we went off the end of the model, throw and exception.
    if (currentSourceDepthIndex >= modelReference.getUpGoingIndexes().length
        || currentSourceDepthIndex < 0) {
      System.out.println("findDepth: source depth not found.");
      throw new BadDepthException(
          String.format(
              "> %f3.1f km",
              modelConversions.computeDimensionalDepth(
                  modelReference.getModelDepths()[modelReference.getUpGoingIndexes().length - 1])));
    }

    foundSlowness = slowness;

    // If we're on a grid point, return that.
    if (Math.abs(slowness - modelReference.getModelSlownesses()[currentSourceDepthIndex])
        <= TauUtilities.DOUBLETOLERANCE) {
      foundDepth = modelReference.getModelDepths()[currentSourceDepthIndex];
      sourceDepthOnGridPoint = true;
    } else {
      // Otherwise interpolate to find the correct slowness.
      foundDepth =
          modelReference.getModelDepths()[currentSourceDepthIndex - 1]
              + Math.log(
                  Math.max(
                      (slowness - modelReference.getModelSlownesses()[currentSourceDepthIndex - 1])
                              * (Math.exp(
                                      modelReference.getModelDepths()[currentSourceDepthIndex]
                                          - modelReference
                                              .getModelDepths()[currentSourceDepthIndex - 1])
                                  - 1d)
                              / (modelReference.getModelSlownesses()[currentSourceDepthIndex]
                                  - modelReference
                                      .getModelSlownesses()[currentSourceDepthIndex - 1])
                          + 1d,
                      TauUtilities.MINIMUMDOUBLE));

      sourceDepthOnGridPoint = false;
    }

    return foundDepth;
  }

  /**
   * Function to find the maximum slowness between the surface and the source. If the source is in a
   * low velocity zone, this will be the slowness at the top. Otherwise, it will be the source
   * slowness. Note that the parameters determined by the last call to findSlowness are assumed.
   *
   * @return A double containing the normalized maximum slowness above the source
   */
  public double findMaximumSlowness() {
    maximumSlowness = foundSlowness;

    for (int j = 0; j < currentSourceDepthIndex; j++) {
      maximumSlowness = Math.min(maximumSlowness, modelReference.getModelSlownesses()[j]);
    }

    return maximumSlowness;
  }

  /**
   * Function to print the result of the latest findSlowness or findDepth call.
   *
   * @param nice A boolean flag, if true print the model in dimensional units
   */
  public void printFind(boolean nice) {
    if (nice) {
      if (Double.isNaN(maximumSlowness)) {
        System.out.format(
            "\nFind: type = %c  isource = %d  z = %5.1f  " + "v = %4.1f  onGrid = %b\n",
            modelReference.getWaveType(),
            currentSourceDepthIndex,
            modelConversions.computeDimensionalDepth(foundDepth),
            modelConversions.computeDimensionalVelocity(foundSlowness, foundDepth),
            sourceDepthOnGridPoint);
      } else {
        System.out.format(
            "\nFind: type = %c  isource = %d  z = %5.1f  "
                + "v = %4.1f  vMax = %4.1f  onGrid = %b\n",
            modelReference.getWaveType(),
            currentSourceDepthIndex,
            modelConversions.computeDimensionalDepth(foundDepth),
            modelConversions.computeDimensionalVelocity(foundSlowness, foundDepth),
            modelConversions.computeDimensionalVelocity(maximumSlowness, foundDepth),
            sourceDepthOnGridPoint);
      }
    } else {
      if (Double.isNaN(maximumSlowness)) {
        System.out.format(
            "\nFind: type = %c  isource = %d  z = %9.6f  " + "p = %8.6f  onGrid = %b\n",
            modelReference.getWaveType(),
            currentSourceDepthIndex,
            foundDepth,
            foundSlowness,
            sourceDepthOnGridPoint);
      } else {
        System.out.format(
            "\nFind: type = %c  isource = %d  z = %9.6f  "
                + "p = %8.6f  pMax = %8.6f  onGrid = %b\n",
            modelReference.getWaveType(),
            currentSourceDepthIndex,
            foundDepth,
            foundSlowness,
            maximumSlowness,
            sourceDepthOnGridPoint);
      }
    }
  }
}
