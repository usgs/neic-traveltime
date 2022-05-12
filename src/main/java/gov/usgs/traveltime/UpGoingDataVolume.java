package gov.usgs.traveltime;

import gov.usgs.traveltime.tables.Decimate;
import gov.usgs.traveltime.tables.TauIntegralException;
import gov.usgs.traveltime.tables.TauIntegrate;
import java.util.Arrays;

/**
 * The UpGoingDataVolume class stores volatile up-going branch data for one wave type. Note that all
 * data have been normalized.
 *
 * @author Ray Buland
 */
public class UpGoingDataVolume {
  /** An integer containing the source depth model index */
  private int sourceDepthModelIndex;

  /** A double containing the normalized source depth */
  private double sourceDepth;

  /** A double containing the slowness at the source depth */
  private double sourceSlowness;

  /** A double containing the maximum (lid) slowness if source is in a low velocity zone */
  private double maximumSlowness;

  /** An array of doubles containing the corrected up-going branch ray parameters */
  private double[] upGoingBranchRayParams;

  /** An array of doubles containing the corrected up-going branch tau */
  private double[] upGoingBranchTau;

  /** An array of doubles containing the corrected up-going branch distance */
  private double[] upGoingBranchDistance;

  /** A double containing the tau integral from surface to LVZ for this wave type */
  private double tauIntSurfaceToLVZ;

  /** A double containing the tau integral from LVZ to source for this wave type */
  private double tauIntLVZToSource;

  /** A double containing the tau integral from surface to source for other wave type */
  private double tauIntSurfaceToSource;

  /** A double containing the distance integral from surface to LVZ for this wave type */
  private double distIntSurfaceToLVZ;

  /** A double containing the distance integral from LVZ to source for this wave type */
  private double distIntLVZToSource;

  /** A double containing the distance integral from surface to source for other wave type */
  private double distIntSurfaceToSource;

  /** An integer containing the length of the decimated up-going branch */
  private int decimatedBranchLength;

  /** An array of doubles containing the decimated up-going branch ray parameters */
  private double[] decUpGoingBranchRayParams;

  /** An array of doubles containing the decimated up-going branch tau */
  private double[] decUpGoingBranchTau;

  /** An UpGoingDataReference object containing the up-going reference data source */
  private UpGoingDataReference upGoingReference;

  /** A ModelDataVolume object containing the primary Earth model data source */
  private ModelDataVolume primaryEarthModel;

  /** A ModelDataVolume object containing the secondary Earth model data source */
  private ModelDataVolume secondaryEarthModel;

  /** A ModelConversions object containing model dependent constants and conversions */
  private final ModelConversions modelConversions;

  /** A TauIntegrate object containing the primary tau integration routines */
  private TauIntegrate primaryIntegrations;

  /** A TauIntegrate object containing the secondary tau integration routines */
  private TauIntegrate secondaryIntegrations;

  /** The Decimate object used for decimation */
  private Decimate decimator;

  /**
   * Get the slowness at the source depth
   *
   * @return A double containing the slowness at the source depth
   */
  public double getSourceSlowness() {
    return sourceSlowness;
  }

  /**
   * Get the maximum (lid) slowness if source is in a low velocity zone
   *
   * @return A double containing the maximum (lid) slowness if source is in a low velocity zone
   */
  public double getMaximumSlowness() {
    return maximumSlowness;
  }

  /**
   * Get the corrected up-going branch ray parameters
   *
   * @return An array of doubles containing the corrected up-going branch ray parameters
   */
  public double[] getUpGoingBranchRayParams() {
    return upGoingBranchRayParams;
  }

  /**
   * Get the corrected up-going branch tau
   *
   * @return An array of doubles containing the corrected up-going branch tau
   */
  public double[] getUpGoingBranchTau() {
    return upGoingBranchTau;
  }

  /**
   * Get the corrected up-going branch distance
   *
   * @return An array of doubles containing the corrected up-going branch distance
   */
  public double[] getUpGoingBranchDistance() {
    return upGoingBranchDistance;
  }

  /**
   * Get the decimated tau values associated with the decimated ray parameter grid.
   *
   * @return Decimated, normalized tau on the decimated ray parameter grid
   */
  public double[] getDecimatedTau() {
    return decUpGoingBranchTau;
  }

  /**
   * Get the tau integral from surface to LVZ for this wave type
   *
   * @return A double containing the tau integral from surface to LVZ for this wave type
   */
  public double getTauIntSurfaceToLVZ() {
    return tauIntSurfaceToLVZ;
  }

  /**
   * Get the tau integral from LVZ to source for this wave type
   *
   * @return A double containing the tau integral from LVZ to source for this wave type
   */
  public double getTauIntLVZToSource() {
    return tauIntLVZToSource;
  }

  /**
   * Get the tau integral from surface to source for other wave type
   *
   * @return A double containing the tau integral from surface to source for other wave type
   */
  public double getTauIntSurfaceToSource() {
    return tauIntSurfaceToSource;
  }

  /**
   * Get the distance integral from surface to LVZ for this wave type
   *
   * @return A double containing the distance integral from surface to LVZ for this wave type
   */
  public double getDistIntSurfaceToLVZ() {
    return distIntSurfaceToLVZ;
  }

  /**
   * Get the distance integral from LVZ to source for this wave type
   *
   * @return A double containing the distance integral from LVZ to source for this wave type
   */
  public double getDistIntLVZToSource() {
    return distIntLVZToSource;
  }

  /**
   * Get the distance integral from surface to source for other wave type
   *
   * @return A double containing the distance integral from surface to source for other wave type
   */
  public double getDistIntSurfaceToSource() {
    return distIntSurfaceToSource;
  }

  /**
   * Get the up-going reference data source
   *
   * @return An UpGoingDataReference object containing the up-going reference data source
   */
  public UpGoingDataReference getUpGoingReference() {
    return upGoingReference;
  }

  /**
   * The UpGoingDataVolume constructor, set up volatile copies of data that changes with depth. Note
   * that both P and S models are needed. If this is handling the up-going data for P, the primary
   * model would be for P and the secondary model would be for S.
   *
   * @param upGoingReference An UpGoingDataReference object containing the up-going reference data
   *     source
   * @param primaryEarthModel A ModelDataVolume object containing the primary Earth model data
   *     source
   * @param secondaryEarthModel A ModelDataVolume object containing the secondary Earth model data
   *     source
   * @param modelConversions A ModelConversions object containing model dependent constants and
   *     conversions
   */
  public UpGoingDataVolume(
      UpGoingDataReference upGoingReference,
      ModelDataVolume primaryEarthModel,
      ModelDataVolume secondaryEarthModel,
      ModelConversions modelConversions) {
    this.upGoingReference = upGoingReference;
    this.primaryEarthModel = primaryEarthModel;
    this.secondaryEarthModel = secondaryEarthModel;
    this.modelConversions = modelConversions;

    // Set up the integration routines.
    primaryIntegrations = new TauIntegrate(primaryEarthModel);
    secondaryIntegrations = new TauIntegrate(secondaryEarthModel);

    // Set up the decimation.
    decimator = new Decimate();
  }

  /**
   * Function to correct up-going tau to the desired source depth. The up-going branches are used to
   * correct tau for all ray parameters for all travel-time branches. At the same time, integrals
   * are computed for the largest ray parameter (usually equal to the source depth slowness) needed
   * to correct tau for the largest ray parameter for all branches.
   *
   * @param depth A double containing the normalized source depth
   * @throws BadDepthException If the source depth is too deep
   * @throws TauIntegralException If the tau integral fails
   */
  public void correctTauForDepth(double depth) throws BadDepthException, TauIntegralException {
    // Initialize.
    sourceDepth = depth;
    tauIntSurfaceToLVZ = 0d;
    tauIntLVZToSource = 0d;
    tauIntSurfaceToSource = 0d;
    distIntSurfaceToLVZ = 0d;
    distIntLVZToSource = 0d;
    distIntSurfaceToSource = 0d;

    // Get the source slowness.
    sourceSlowness = primaryEarthModel.findSlowness(sourceDepth);
    sourceDepthModelIndex = primaryEarthModel.getCurrentSourceDepthIndex();
    maximumSlowness = primaryEarthModel.findMaximumSlowness();

    // If the source is at the surface, we're already done.
    if (-sourceDepth <= TauUtilities.DOUBLETOLERANCE) {
      return;
    }

    // Otherwise, copy the desired data into temporary storage.
    int upGoingBranchIndex =
        primaryEarthModel.getModelReference().getUpGoingIndexes()[sourceDepthModelIndex];

    upGoingBranchRayParams =
        Arrays.copyOf(
            upGoingReference.getSlownessGrid(),
            upGoingReference.getUpGoingTauValues()[upGoingBranchIndex].length);

    upGoingBranchTau =
        Arrays.copyOf(
            upGoingReference.getUpGoingTauValues()[upGoingBranchIndex],
            upGoingBranchRayParams.length);

    upGoingBranchDistance =
        Arrays.copyOf(
            upGoingReference.getUpGoingDistanceValues()[upGoingBranchIndex],
            upGoingReference.getUpGoingDistanceValues()[upGoingBranchIndex].length);

    // See if we need to correct upGoingBranchTau.
    boolean correctTau; // True if upGoingBranchTau needs correcting
    if (Math.abs(upGoingReference.getSlownessGrid()[upGoingBranchIndex] - maximumSlowness)
        <= TauUtilities.DOUBLETOLERANCE) {
      correctTau = false;
    } else {
      correctTau = true;
    }

    maximumSlowness = Math.min(maximumSlowness, sourceSlowness);

    // Correct the up-going tau values to the exact source depth.
    int i = 0;
    for (int j = 0; j < upGoingBranchTau.length; j++) {
      if (upGoingReference.getSlownessGrid()[j] <= maximumSlowness) {
        if (correctTau) {
          upGoingBranchTau[j] -=
              primaryIntegrations.integrateLayer(
                  upGoingReference.getSlownessGrid()[j],
                  sourceSlowness,
                  primaryEarthModel.getModelReference().getModelSlownesses()[sourceDepthModelIndex],
                  sourceDepth,
                  primaryEarthModel.getModelReference().getModelDepths()[sourceDepthModelIndex]);

          // See if we need to correct an end point distance as well.
          if (Math.abs(
                  upGoingReference.getSlownessGrid()[j]
                      - upGoingReference.getBranchEndpointSlownesses()[i])
              <= TauUtilities.DOUBLETOLERANCE) {
            double xInt = primaryIntegrations.getLayerIntDist();

            upGoingBranchDistance[i++] -= xInt;
          }
        }
      } else break;
    }

    /**
     * Compute tau and distance for the ray parameter equal to the source slowness (i.e., horizontal
     * take-off angle from the source).
     */
    tauIntSurfaceToLVZ =
        primaryIntegrations.integrateRange(
            maximumSlowness, 0, sourceDepthModelIndex - 1, sourceSlowness, sourceDepth);
    distIntSurfaceToLVZ = primaryIntegrations.getSummaryIntDist();

    /**
     * If the source depth is in a low velocity zone, we need to compute tau and distance down to
     * the shallowest turning ray (the horizontal ray is trapped).
     */
    if (maximumSlowness > sourceSlowness) {
      double maxSlownessDepth = primaryEarthModel.findDepth(maximumSlowness, false);
      int bottomDepthModelIndex = primaryEarthModel.getCurrentSourceDepthIndex();

      tauIntLVZToSource =
          primaryIntegrations.integrateRange(
              maximumSlowness,
              sourceDepthModelIndex,
              bottomDepthModelIndex,
              sourceSlowness,
              sourceDepth,
              maximumSlowness,
              maxSlownessDepth);
      distIntLVZToSource = primaryIntegrations.getSummaryIntDist();
    } else {
      tauIntLVZToSource = 0d;
      distIntLVZToSource = 0d;
    }

    /**
     * Compute tau and distance for the other wave type for the ray parameter equal to the source
     * slowness.
     */
    try {
      double maxSlownessDepth = secondaryEarthModel.findDepth(maximumSlowness, true);
      int bottomDepthModelIndex = secondaryEarthModel.getCurrentSourceDepthIndex();

      tauIntSurfaceToSource =
          secondaryIntegrations.integrateRange(
              maximumSlowness, 0, bottomDepthModelIndex - 1, maximumSlowness, maxSlownessDepth);
      distIntSurfaceToSource = secondaryIntegrations.getSummaryIntDist();
    } catch (BadDepthException | TauIntegralException e) {
      tauIntSurfaceToSource = 0d;
      distIntSurfaceToSource = 0d;
    }
  }

  /**
   * Funtion to generate the up-going branch that will be used to compute travel times. The stored
   * up-going branches must be complete in ray parameter samples in order to correct all other
   * travel-time branches to the desired source depth. However, due to the irregular spacing of the
   * ray parameter grid, the interpolation will be unstable. Therefore, the up-going branch must be
   * decimated to be useful later. For very shallow sources, even the decimated grid will be
   * unstable and must be completely replaced.
   *
   * @param rayRayParamGrid An array of doubles containing the normalized raw ray parameter grid
   * @param tauGrid An array of doubles containing the normalized raw tau grid
   * @param distanceRange An array of doubles containing the normalized distance range
   * @param minimumDistInterval A double containing the normalized minimum distance interval desired
   * @return An array of doubles containing a new grid of ray parameter values for the up-going
   *     branch
   * @throws TauIntegralException If the tau integration fails
   */
  public double[] generateUpGoingRayParams(
      double[] rayRayParamGrid,
      double[] tauGrid,
      double[] distanceRange,
      double minimumDistInterval)
      throws TauIntegralException {
    double depth = modelConversions.computeDimensionalDepth(sourceDepth);
    int power;
    if (depth <= modelConversions.getUpGoingReplacementDepth()) {
      // For shallow sources, recompute tau on a more stable ray
      // parameter grid.  The parameters are depth dependent.
      if (depth < 1.5) {
        decimatedBranchLength = 5;
        power = 6;
      } else if (depth < 10.5) {
        decimatedBranchLength = 6;
        power = 6;
      } else {
        decimatedBranchLength = 6;
        power = 7;
      }

      // Allocate some space.
      decUpGoingBranchRayParams = new double[decimatedBranchLength];
      decUpGoingBranchTau = new double[decimatedBranchLength];

      // Create the up-going branch.
      decUpGoingBranchRayParams[0] = rayRayParamGrid[0];
      decUpGoingBranchTau[0] = tauGrid[0];
      double dp = 0.75d * maximumSlowness / Math.pow(decimatedBranchLength - 2, power);

      for (int j = 1; j < decimatedBranchLength - 1; j++) {
        decUpGoingBranchRayParams[j] =
            maximumSlowness - dp * Math.pow(decimatedBranchLength - j - 1, power--);
        decUpGoingBranchTau[j] =
            primaryIntegrations.integrateRange(
                decUpGoingBranchRayParams[j],
                0,
                sourceDepthModelIndex - 1,
                sourceSlowness,
                sourceDepth);
      }

      decUpGoingBranchRayParams[decimatedBranchLength - 1] = maximumSlowness;
      decUpGoingBranchTau[decimatedBranchLength - 1] = tauIntSurfaceToLVZ;
    } else {
      // For deeper sources, it is enough to decimate the ray
      // parameter grid we already have.
      boolean[] keep =
          decimator.fastDecimation(rayRayParamGrid, tauGrid, distanceRange, minimumDistInterval);

      if (keep != null) {
        // Do the decimation.
        int len = 0;
        for (int k = 0; k < keep.length; k++) {
          if (keep[k]) {
            len++;
          }
        }

        decUpGoingBranchRayParams = new double[len];
        decUpGoingBranchTau = new double[len];

        for (int k = 0, l = 0; k < keep.length; k++) {
          if (keep[k]) {
            decUpGoingBranchRayParams[l] = rayRayParamGrid[k];
            decUpGoingBranchTau[l++] = tauGrid[k];
          }
        }
      } else {
        // We don't need to decimate.
        decUpGoingBranchRayParams = Arrays.copyOf(rayRayParamGrid, rayRayParamGrid.length);
        decUpGoingBranchTau = Arrays.copyOf(tauGrid, tauGrid.length);
      }
    }

    return decUpGoingBranchRayParams;
  }

  /**
   * Function to print out the up-going branch data corrected for the source depth.
   *
   * @param full A boolean flag, if true print the corrected tau array as well.
   */
  public void dumpUpGoingCorrectedBranch(boolean full) {
    System.out.println("\n     Up-going " + upGoingReference.getWaveType() + " corrected");
    System.out.format(
        "TauEnd: %8.6f %8.6f %8.6f  XEnd: %8.6f %8.6f %8.6f\n",
        tauIntSurfaceToLVZ,
        tauIntLVZToSource,
        tauIntSurfaceToSource,
        distIntSurfaceToLVZ,
        distIntLVZToSource,
        distIntSurfaceToSource);

    if (full) {
      System.out.println("          p        tau");

      for (int k = 0; k < upGoingBranchTau.length; k++) {
        System.out.format("%3d  %8.6f %11.4e\n", k, upGoingBranchRayParams[k], upGoingBranchTau[k]);
      }
    }
  }

  /**
   * Function to print out the decimated up-going branch data corrected for the source depth.
   *
   * @param full A boolean flag, if true print the corrected tau array as well.
   */
  public void dumpUpGoingDecimatedBranch(boolean full) {
    System.out.println("\n     Up-going " + upGoingReference.getWaveType() + " decimated");
    System.out.format(
        "TauEnd: %8.6f %8.6f %8.6f  XEnd: %8.6f %8.6f %8.6f\n",
        tauIntSurfaceToLVZ,
        tauIntLVZToSource,
        tauIntSurfaceToSource,
        distIntSurfaceToLVZ,
        distIntLVZToSource,
        distIntSurfaceToSource);

    if (full) {
      System.out.println("          p        tau");

      for (int k = 0; k < decUpGoingBranchTau.length; k++) {
        System.out.format(
            "%3d  %8.6f %11.4e\n", k, decUpGoingBranchRayParams[k], decUpGoingBranchTau[k]);
      }
    }
  }
}
