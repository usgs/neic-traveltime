package gov.usgs.traveltime.tables;

import gov.usgs.traveltime.ModelConversions;
import java.util.ArrayList;

/**
 * The TablePieces class puts together the bits and pieces we'll need to make the travel-time branch
 * layout and decimation come out right.
 *
 * @author Ray Buland
 */
public class TablePieces {
  /** A BranchIntegrals object containing the P model pieces */
  private BranchIntegrals modelPiecesP;

  /** A BranchIntegrals object containing the S model pieces */
  private BranchIntegrals modelPiecesS;

  /** An ArrayList of ModelShell objects containing the P shells */
  private ArrayList<ModelShell> shellModelP;

  /** An ArrayList of ModelShell objects containing the S shells */
  private ArrayList<ModelShell> shellModelS;

  /** A ModelConversions object containing the model dependant conversions */
  private ModelConversions modelConversions;

  /**
   * TablePieces constructor. Initialize bits and pieces for each phase type.
   *
   * @param finalTauModel Final tau model
   */
  public TablePieces(TauModel finalTauModel) {
    modelConversions = finalTauModel.getModelConversions();
    shellModelP = finalTauModel.getShellModelP();
    shellModelS = finalTauModel.getShellModelS();
    modelPiecesP = new BranchIntegrals('P', finalTauModel);
    modelPiecesS = new BranchIntegrals('S', finalTauModel);
  }

  /** Function to decimate the ray parameter arrays for both the P and S models. */
  public void decimateRayParameters() {
    modelPiecesP.decimateRayParameters();
    modelPiecesS.decimateRayParameters();
  }

  /**
   * Get the integral pieces for one phase type.
   *
   * @param phaseType A char containing the model phase type (P = P slowness, S = S slowness)
   * @return A BranchIntegrals object containing the integral pieces
   */
  public BranchIntegrals getPiece(char phaseType) {
    if (phaseType == 'P') {
      return modelPiecesP;
    } else {
      return modelPiecesS;
    }
  }

  /**
   * Get a ray parameter by index. Note that the S-wave ray parameters include the P-wave ray
   * parameters.
   *
   * @param index Ray parameter index
   * @return Ray parameter
   */
  public double getP(int index) {
    return modelPiecesS.getRayParameters()[index];
  }

  /**
   * Function to get all the ray parameters.
   *
   * @return Ray parameter array
   */
  public double[] getP() {
    return modelPiecesS.getRayParameters();
  }

  /**
   * Function to get a major shell tau value by index.
   *
   * @param phaseType A char containing the model phase type (P = P slowness, S = S slowness)
   * @param tauIndex An integer containing the Tau index
   * @param shellIndex An integer containing the major shell index (0 = mantle, 1 = outer core, 2 =
   *     inner core)
   * @return A double containing the tau value
   */
  public double getTau(char phaseType, int tauIndex, int shellIndex) {
    switch (shellIndex) {
      case 0:
        if (phaseType == 'P') {
          return modelPiecesP.getMantleTauIntegrals()[tauIndex];
        } else {
          return modelPiecesS.getMantleTauIntegrals()[tauIndex];
        }
      case 1:
        if (phaseType == 'P') {
          return modelPiecesP.getOuterCoreTauIntegrals()[tauIndex];
        } else {
          return modelPiecesS.getOuterCoreTauIntegrals()[tauIndex];
        }
      case 2:
        if (phaseType == 'P') {
          return modelPiecesP.getInnerCoreTauIntegrals()[tauIndex];
        } else {
          return modelPiecesS.getInnerCoreTauIntegrals()[tauIndex];
        }
      default:
        return Double.NaN;
    }
  }

  /**
   * Function to get a major shell range value by index.
   *
   * @param phaseType A char containing the model phase type (P = P slowness, S = S slowness)
   * @param rangeIndex Range index
   * @param shellIndex An integer containing the major shell index (0 = mantle, 1 = outer core, 2 =
   *     inner core)
   * @return A double containign the shell range value
   */
  public double getShellRange(char phaseType, int rangeIndex, int shellIndex) {
    switch (shellIndex) {
      case 0:
        if (phaseType == 'P') {
          return modelPiecesP.getMantleRangeIntegrals()[rangeIndex];
        } else {
          return modelPiecesS.getMantleRangeIntegrals()[rangeIndex];
        }
      case 1:
        if (phaseType == 'P') {
          return modelPiecesP.getOuterCoreRangeIntegrals()[rangeIndex];
        } else {
          return modelPiecesS.getOuterCoreRangeIntegrals()[rangeIndex];
        }
      case 2:
        if (phaseType == 'P') {
          return modelPiecesP.getInnerCoreRangeIntegrals()[rangeIndex];
        } else {
          return modelPiecesS.getInnerCoreRangeIntegrals()[rangeIndex];
        }
      default:
        return Double.NaN;
    }
  }

  /**
   * Function to get the radial sampling interval associated with a shell by index.
   *
   * @param phaseType A char containing the model phase type (P = P slowness, S = S slowness)
   * @param shellIndex An integer containing the major shell index (0 = mantle, 1 = outer core, 2 =
   *     inner core)
   * @return A double containing the non-dimensional radial sampling
   */
  public double getRadialSampling(char phaseType, int shellIndex) {
    if (phaseType == 'P') {
      return modelConversions.normalizeRadius(
          shellModelP.get(shellIndex).getRangeIncrementTarget());
    } else {
      return modelConversions.normalizeRadius(
          shellModelS.get(shellIndex).getRangeIncrementTarget());
    }
  }

  /**
   * Function to get the radial sampling interval associated with the shell above the current one.
   *
   * @param phaseType A char containing the model phase type (P = P slowness, S = S slowness)
   * @param shellIndex An integer containing the major shell index (0 = mantle, 1 = outer core, 2 =
   *     inner core)
   * @return Non-dimensional radial sampling
   */
  public double getNextRadialSampling(char phaseType, int shellIndex) {
    if (phaseType == 'P') {
      for (int j = shellIndex + 1; j < shellModelP.size(); j++) {
        if (!shellModelP.get(j).getIsDiscontinuity()) {
          return modelConversions.normalizeRadius(shellModelP.get(j).getRangeIncrementTarget());
        }
      }
    } else {
      for (int j = shellIndex + 1; j < shellModelS.size(); j++) {
        if (!shellModelS.get(j).getIsDiscontinuity()) {
          return modelConversions.normalizeRadius(shellModelS.get(j).getRangeIncrementTarget());
        }
      }
    }

    return Double.NaN;
  }

  /**
   * Function to retrieve the master decimation array.
   *
   * @param phaseType A char containing the model phase type (P = P slowness, S = S slowness)
   * @return An array of boolean values containing the the master decimation array
   */
  public boolean[] getDecimation(char phaseType) {
    if (phaseType == 'P') {
      return modelPiecesP.getDecimationKeep();
    } else {
      return modelPiecesS.getDecimationKeep();
    }
  }

  /** Function to print out the proxy ranges. */
  public void printProxy() {
    System.out.println("\n\t\t\tProxy Ranges");
    System.out.println("                  P                            S");
    System.out.println("    slowness      X       delX   slowness" + "      X       delX");
    int nP = modelPiecesP.getProxyRanges().length;
    int nS = modelPiecesS.getProxyRanges().length;

    System.out.format(
        "%3d %8.6f %8.2f            %8.6f %8.2f\n",
        0,
        modelPiecesP.getProxyRayParameters()[0],
        modelConversions.convertDimensionalRadius(modelPiecesP.getProxyRanges()[0]),
        modelPiecesS.getProxyRayParameters()[0],
        modelConversions.convertDimensionalRadius(modelPiecesS.getProxyRanges()[0]));

    for (int j = 1; j < nP; j++) {
      System.out.format(
          "%3d %8.6f %8.2f %8.2f   %8.6f %8.2f %8.2f\n",
          j,
          modelPiecesP.getProxyRayParameters()[j],
          modelConversions.convertDimensionalRadius(modelPiecesP.getProxyRanges()[j]),
          modelConversions.convertDimensionalRadius(
              modelPiecesP.getProxyRanges()[j] - modelPiecesP.getProxyRanges()[j - 1]),
          modelPiecesS.getProxyRayParameters()[j],
          modelConversions.convertDimensionalRadius(modelPiecesS.getProxyRanges()[j]),
          modelConversions.convertDimensionalRadius(
              modelPiecesS.getProxyRanges()[j + 1] - modelPiecesS.getProxyRanges()[j]));
    }

    for (int j = nP; j < nS; j++) {
      System.out.format(
          "%3d                              " + "%8.6f %8.2f %8.2f\n",
          j,
          modelPiecesS.getProxyRayParameters()[j],
          modelConversions.convertDimensionalRadius(modelPiecesS.getProxyRanges()[j]),
          modelConversions.convertDimensionalRadius(
              modelPiecesS.getProxyRanges()[j] - modelPiecesS.getProxyRanges()[j - 1]));
    }
  }

  /**
   * Function to print the integrals for the whole mantle, outer core, and inner core.
   *
   * @param phaseType A char containing the model phase type (P = P slowness, S = S slowness)
   */
  public void printShellIntegrals(char phaseType) {
    if (phaseType == 'P') {
      modelPiecesP.printShellIntegrals();
    } else {
      modelPiecesS.printShellIntegrals();
    }
  }

  /** Function to print the ray parameter arrays for both the P and S branches. */
  public void printP() {
    System.out.println("\nMaster Ray Parameters");
    System.out.println("       P        S");

    for (int j = 0; j < modelPiecesP.getRayParameters().length; j++) {
      System.out.format(
          "%3d %8.6f %8.6f\n",
          j, modelPiecesP.getRayParameters()[j], modelPiecesS.getRayParameters()[j]);
    }

    for (int j = modelPiecesP.getRayParameters().length;
        j < modelPiecesS.getRayParameters().length;
        j++) {
      System.out.format("%3d          %8.6f\n", j, modelPiecesS.getRayParameters()[j]);
    }
  }
}
