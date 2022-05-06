package gov.usgs.traveltime.tables;

import gov.usgs.traveltime.ModelConversions;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * BranchIntegrals is a class that puts together the bits and pieces we'll need to make the
 * travel-time branch layout and decimation come out right by phase type.
 *
 * @author Ray Buland
 */
public class BranchIntegrals {
  /** A char containing the model type ('P' = P slowness, 'S' = S slowness) */
  private char modelType;

  /** A boolean array indicating the decimation values to keep */
  private boolean[] decimationKeep;

  /** An array of doubles containing the ray parameters */
  private double[] rayParameters;

  /** An array of doubles containing the tau integrals through the mantle */
  private double[] mantleTauIntegrals;

  /** An array of doubles containing the range integrals though the mantle */
  private double[] mantleRangeIntegrals;

  /** An array of doubles containing the tau integrals through the outer core */
  private double[] outerCoreTauIntegrals;

  /** An array of doubles containing the range integrals though the outer core */
  private double[] outerCoreRangeIntegrals;

  /** An array of doubles containing the tau integrals through the inner core */
  private double[] innerCoreTauIntegrals;

  /** An array of doubles containing the range integrals though the inner core */
  private double[] innerCoreRangeIntegrals;

  /** An array of doubles containing the proxy ray paramters */
  private double[] proxyRayParameters;

  /** An array of doubles containing the proxy ranges */
  private double[] proxyRanges;

  /** A TauModel object containing the final model */
  private TauModel finalTTModel;

  /** A ModelConversions object containing the model dependant conversions */
  private ModelConversions modelConversions;

  /** An ArrayList of TauXsample objects containing the raw integrals */
  private ArrayList<TauRangeSample> rawIntegrals;

  /**
   * Get the array of decimation keep flags
   *
   * @return An array of booleans indicating the decimation values to keep
   */
  public boolean[] getDecimationKeep() {
    return decimationKeep;
  }

  /**
   * Get the array containing the ray parameters
   *
   * @return An array of booleans indicating the decimation values to keep
   */
  public double[] getRayParameters() {
    return rayParameters;
  }

  /**
   * Get the array containing the mantle tau integrals
   *
   * @return An array of doubles containing the mantle tau integrals
   */
  public double[] getMantleTauIntegrals() {
    return mantleTauIntegrals;
  }

  /**
   * Get the array containing the mantle range integrals
   *
   * @return An array of doubles containing the mantle range integrals
   */
  public double[] getMantleRangeIntegrals() {
    return mantleRangeIntegrals;
  }

  /**
   * Get the array containing the outer core tau integrals
   *
   * @return An array of doubles containing the outer core tau integrals
   */
  public double[] getOuterCoreTauIntegrals() {
    return outerCoreTauIntegrals;
  }

  /**
   * Get the array containing the outer core range integrals
   *
   * @return An array of doubles containing the outer core range integrals
   */
  public double[] getOuterCoreRangeIntegrals() {
    return outerCoreRangeIntegrals;
  }

  /**
   * Get the array containing the inner core tau integrals
   *
   * @return An array of doubles containing the inner core tau integrals
   */
  public double[] getInnerCoreTauIntegrals() {
    return innerCoreTauIntegrals;
  }

  /**
   * Get the array containing the inner core range integrals
   *
   * @return An array of doubles containing the inner core range integrals
   */
  public double[] getInnerCoreRangeIntegrals() {
    return innerCoreRangeIntegrals;
  }

  /**
   * Get the array containing the proxy ray parameters
   *
   * @return An array of doubles containing the proxy ray parameters
   */
  public double[] getProxyRayParameters() {
    return proxyRayParameters;
  }

  /**
   * Get the array containing the proxy ranges
   *
   * @return An array of doubles containing the proxy ranges
   */
  public double[] getProxyRanges() {
    return proxyRanges;
  }

  /**
   * BranchIntegrals Constructor, setting up the model to get started.
   *
   * @param modelType A char containing the model type ('P' = P slowness, 'S' = S slowness)
   * @param finalTTModel A TauModel object containing the Final tau model
   */
  public BranchIntegrals(char modelType, TauModel finalTTModel) {
    this.finalTTModel = finalTTModel;
    this.modelConversions = finalTTModel.getModelConversions();
    this.modelType = modelType;

    if (modelType == 'P') {
      rawIntegrals = finalTTModel.getIntegralsP();
    } else {
      rawIntegrals = finalTTModel.getIntegralsS();
    }

    setShellIntegrals(modelType);
    initDecimation(modelType);
  }

  /**
   * Function to update the proxy ray parameters and ranges with the decimated versions.
   *
   * @param len An integer containing the length of the arrays
   * @param newRayParams An array of doubles containing the update ray parameter sampling
   * @param newRangeParams An array of doubles containing the updated range sampling
   */
  public void updateProxies(int len, double[] newRayParams, double[] newRangeParams) {
    proxyRayParameters = Arrays.copyOf(newRayParams, len);
    proxyRanges = Arrays.copyOf(newRangeParams, len);
  }

  /**
   * Function to Decimate the ray parameter array. Note that decimationKeep is an AND of all the
   * branch decimations. That is, if a ray parameter is needed for any branch, it will be kept.
   */
  public void decimateRayParameters() {
    int k = 0;
    double[] decimatedRayParameters = new double[rayParameters.length];

    for (int j = 0; j < rayParameters.length; j++) {
      if (decimationKeep[j]) {
        decimatedRayParameters[k++] = rayParameters[j];
      }
    }

    rayParameters = Arrays.copyOf(decimatedRayParameters, k);
  }

  /**
   * Function to create the tau and range partial integrals by major shells rather than the
   * cumulative integrals computed in Integrate.
   *
   * @param modelType A char containing the model type ('P' = P slowness, 'S' = S slowness)
   */
  private void setShellIntegrals(char modelType) {
    // Get the pieces we need.
    mantleTauIntegrals = finalTTModel.getTauIntegrals(modelType, ShellName.CORE_MANTLE_BOUNDARY);
    mantleRangeIntegrals =
        finalTTModel.getRangeIntegrals(modelType, ShellName.CORE_MANTLE_BOUNDARY);
    double[] ocCumulativeTauInt =
        finalTTModel.getTauIntegrals(modelType, ShellName.INNER_CORE_BOUNDARY);
    double[] ocCumulativeRangeInt =
        finalTTModel.getRangeIntegrals(modelType, ShellName.INNER_CORE_BOUNDARY);
    double[] icCumulativeTauInt = finalTTModel.getTauIntegrals(modelType, ShellName.CENTER);
    double[] icCumulativeRangeInt = finalTTModel.getRangeIntegrals(modelType, ShellName.CENTER);

    // Initialize the difference arrays.
    innerCoreTauIntegrals = new double[mantleTauIntegrals.length];
    innerCoreRangeIntegrals = new double[mantleTauIntegrals.length];
    outerCoreTauIntegrals = new double[mantleTauIntegrals.length];
    outerCoreRangeIntegrals = new double[mantleTauIntegrals.length];

    // Do the differences.
    for (int j = 0; j < mantleTauIntegrals.length; j++) {
      innerCoreTauIntegrals[j] = icCumulativeTauInt[j] - ocCumulativeTauInt[j];
      innerCoreRangeIntegrals[j] = icCumulativeRangeInt[j] - ocCumulativeRangeInt[j];
      outerCoreTauIntegrals[j] = ocCumulativeTauInt[j] - mantleTauIntegrals[j];
      outerCoreRangeIntegrals[j] = ocCumulativeRangeInt[j] - mantleRangeIntegrals[j];
    }
  }

  /**
   * Function to decimate the up-going branches, we need a proxy for the range spacing at all source
   * depths so that the ray parameter spacing is common.
   *
   * @param modelType A char containing the model type ('P' = P slowness, 'S' = S slowness)
   */
  private void initDecimation(char modelType) {
    /** The master keep list will be an or of branch keeps. */
    decimationKeep = new boolean[mantleTauIntegrals.length];
    Arrays.fill(decimationKeep, false);

    /** Create the proxy ranges. */
    int n1 = mantleRangeIntegrals.length;
    proxyRanges = new double[n1];
    Arrays.fill(proxyRanges, 0d);
    int n = finalTTModel.size(modelType);

    // Put together a list of maximum range differences.
    for (int i = 1; i < n - 3; i++) {
      double[] x = finalTTModel.getRangeIntegrals(modelType, i);

      if (x != null) {
        int m = x.length;

        for (int j = 1; j < m; j++) {
          proxyRanges[j] = Math.max(proxyRanges[j], Math.abs(x[j - 1] - x[j]));
        }

        if (m + 1 == n1) {
          proxyRanges[n1 - 1] = x[m - 1];
        }
      }
    }

    // Now put the range differences back together to sort of look
    // like a range.
    ArrayList<Double> slowness = finalTTModel.getSlowness();
    n = slowness.size() - 1;
    rayParameters = new double[n1];
    proxyRayParameters = new double[proxyRanges.length];

    for (int j = 1; j < proxyRanges.length; j++) {
      rayParameters[j] = slowness.get(n - j);
      proxyRayParameters[j] = slowness.get(n - j);
      proxyRanges[j] = proxyRanges[j - 1] + proxyRanges[j];
    }
  }

  /** Function to print out the proxy ranges. */
  public void printProxy() {
    System.out.format("\n\tProxy Ranges for %c\n", modelType);
    System.out.println("    slowness      X       delX");
    System.out.format(
        "%3d %8.6f %8.2f\n",
        0, proxyRayParameters[0], modelConversions.convertDimensionalRadius(proxyRanges[0]));

    for (int j = 1; j < proxyRanges.length; j++) {
      System.out.format(
          "%3d %8.6f %8.2f %8.2f\n",
          j,
          proxyRayParameters[j],
          modelConversions.convertDimensionalRadius(proxyRanges[j]),
          modelConversions.convertDimensionalRadius(proxyRanges[j] - proxyRanges[j - 1]));
    }
  }

  /** Function to print the ray parameter decimation. */
  public void printDec() {
    System.out.format("\nDecimation for %c\n", modelType);
    System.out.println("    slowness  keep");

    for (int j = 0; j < rayParameters.length; j++) {
      System.out.format("%3d %8.6f %b\n", j, rayParameters[j], decimationKeep[j]);
    }
  }

  /** Function to print out the shell integrals. */
  public void printShellIntegrals() {
    System.out.format("\n\t\tShell Integrals for %c-waves\n", modelType);
    System.out.println("                        Tau                    " + "   X");
    System.out.println("        p     Mantle     OC       IC     Mantle" + "   OC     IC");

    for (int j = 0; j < mantleTauIntegrals.length; j++) {
      System.out.format(
          "%3d %8.6f %8.6f %8.6f %8.6f %6.2f %6.2f %6.2f\n",
          j,
          rayParameters[j],
          mantleTauIntegrals[j],
          outerCoreTauIntegrals[j],
          innerCoreTauIntegrals[j],
          Math.toDegrees(mantleRangeIntegrals[j]),
          Math.toDegrees(outerCoreRangeIntegrals[j]),
          Math.toDegrees(innerCoreRangeIntegrals[j]));
    }
  }
}
