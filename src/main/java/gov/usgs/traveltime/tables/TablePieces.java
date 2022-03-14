package gov.usgs.traveltime.tables;

import gov.usgs.traveltime.ModConvert;
import java.util.ArrayList;

/**
 * The TablePieces class puts together the bits and pieces we'll need to make the travel-time branch
 * layout and decimation come out right.
 *
 * @author Ray Buland
 */
public class TablePieces {
  /** A BranchIntegrals object containing the P model pieces */
  private BranchIntegrals pModelPieces;

  /** A BranchIntegrals object containing the S model pieces */
  private BranchIntegrals sModelPieces;

  /** An ArrayList of ModelShell objects containing the P shells */
  private ArrayList<ModelShell> pShells;

  /** An ArrayList of ModelShell objects containing the S shells */
  private ArrayList<ModelShell> sShells;

  /** A ModConvert object containing the model dependant conversions */
  private ModConvert modelConversions;

  /**
   * TablePieces constructor. Initialize bits and pieces for each phase type.
   *
   * @param finalTauModel Final tau model
   */
  public TablePieces(TauModel finalTauModel) {
    modelConversions = finalTauModel.convert;
    pShells = finalTauModel.pShells;
    sShells = finalTauModel.sShells;
    pModelPieces = new BranchIntegrals('P', finalTauModel);
    sModelPieces = new BranchIntegrals('S', finalTauModel);
  }

  /** Function to decimate the ray parameter arrays for both the P and S models. */
  public void decimateRayParameters() {
    pModelPieces.decimateRayParameters();
    sModelPieces.decimateRayParameters();
  }

  /**
   * Get the integral pieces for one phase type.
   *
   * @param phaseType A char containing the model phase type (P = P slowness, S = S slowness)
   * @return A BranchIntegrals object containing the integral pieces
   */
  public BranchIntegrals getPiece(char phaseType) {
    if (phaseType == 'P') {
      return pModelPieces;
    } else {
      return sModelPieces;
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
    return sModelPieces.getRayParameters()[index];
  }

  /**
   * Function to get all the ray parameters.
   *
   * @return Ray parameter array
   */
  public double[] getP() {
    return sModelPieces.getRayParameters();
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
  public double getTau(char type, int tauIndex, int shellIndex) {
    switch (shellIndex) {
      case 0:
        if (type == 'P') {
          return pModelPieces.getMantleTauIntegrals()[tauIndex];
        } else {
          return sModelPieces.getMantleTauIntegrals()[tauIndex];
        }
      case 1:
        if (type == 'P') {
          return pModelPieces.getOuterCoreTauIntegrals()[tauIndex];
        } else {
          return sModelPieces.getOuterCoreTauIntegrals()[tauIndex];
        }
      case 2:
        if (type == 'P') {
          return pModelPieces.getInnerCoreTauIntegrals()[tauIndex];
        } else {
          return sModelPieces.getInnerCoreTauIntegrals()[tauIndex];
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
          return pModelPieces.getMantleRangeIntegrals()[rangeIndex];
        } else {
          return sModelPieces.getMantleRangeIntegrals()[rangeIndex];
        }
      case 1:
        if (phaseType == 'P') {
          return pModelPieces.getOuterCoreRangeIntegrals()[rangeIndex];
        } else {
          return sModelPieces.getOuterCoreRangeIntegrals()[rangeIndex];
        }
      case 2:
        if (phaseType == 'P') {
          return pModelPieces.getInnerCoreRangeIntegrals()[rangeIndex];
        } else {
          return sModelPieces.getInnerCoreRangeIntegrals()[rangeIndex];
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
      return modelConversions.normR(pShells.get(shellIndex).getRangeIncrementTarget());
    } else {
      return modelConversions.normR(sShells.get(shellIndex).getRangeIncrementTarget());
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
      for (int j = shellIndex + 1; j < pShells.size(); j++) {
        if (!pShells.get(j).getIsDiscontinuity()) {
          return modelConversions.normR(pShells.get(j).getRangeIncrementTarget());
        }
      }
    } else {
      for (int j = shellIndex + 1; j < sShells.size(); j++) {
        if (!sShells.get(j).getIsDiscontinuity()) {
          return modelConversions.normR(sShells.get(j).getRangeIncrementTarget());
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
      return pModelPieces.getDecimationKeep();
    } else {
      return sModelPieces.getDecimationKeep();
    }
  }

  /** Function to print out the proxy ranges. */
  public void printProxy() {
    System.out.println("\n\t\t\tProxy Ranges");
    System.out.println("                  P                            S");
    System.out.println("    slowness      X       delX   slowness" + "      X       delX");
    int nP = pModelPieces.getProxyRanges().length;
    int nS = sModelPieces.getProxyRanges().length;

    System.out.format(
        "%3d %8.6f %8.2f            %8.6f %8.2f\n",
        0,
        pModelPieces.getProxyRayParameters()[0],
        modelConversions.dimR(pModelPieces.getProxyRanges()[0]),
        sModelPieces.getProxyRayParameters()[0],
        modelConversions.dimR(sModelPieces.getProxyRanges()[0]));

    for (int j = 1; j < nP; j++) {
      System.out.format(
          "%3d %8.6f %8.2f %8.2f   %8.6f %8.2f %8.2f\n",
          j,
          pModelPieces.getProxyRayParameters()[j],
          modelConversions.dimR(pModelPieces.getProxyRanges()[j]),
          modelConversions.dimR(
              pModelPieces.getProxyRanges()[j] - pModelPieces.getProxyRanges()[j - 1]),
          sModelPieces.getProxyRayParameters()[j],
          modelConversions.dimR(sModelPieces.getProxyRanges()[j]),
          modelConversions.dimR(
              sModelPieces.getProxyRanges()[j + 1] - sModelPieces.getProxyRanges()[j]));
    }

    for (int j = nP; j < nS; j++) {
      System.out.format(
          "%3d                              " + "%8.6f %8.2f %8.2f\n",
          j,
          sModelPieces.getProxyRayParameters()[j],
          modelConversions.dimR(sModelPieces.getProxyRanges()[j]),
          modelConversions.dimR(
              sModelPieces.getProxyRanges()[j] - sModelPieces.getProxyRanges()[j - 1]));
    }
  }

  /**
   * Function to print the integrals for the whole mantle, outer core, and inner core.
   *
   * @param phaseType A char containing the model phase type (P = P slowness, S = S slowness)
   */
  public void printShellInts(char phaseType) {
    if (phaseType == 'P') {
      pModelPieces.printShellInts();
    } else {
      sModelPieces.printShellInts();
    }
  }

  /** Function to print the ray parameter arrays for both the P and S branches. */
  public void printP() {
    System.out.println("\nMaster Ray Parameters");
    System.out.println("       P        S");

    for (int j = 0; j < pModelPieces.getRayParameters().length; j++) {
      System.out.format(
          "%3d %8.6f %8.6f\n",
          j, pModelPieces.getRayParameters()[j], sModelPieces.getRayParameters()[j]);
    }

    for (int j = pModelPieces.getRayParameters().length;
        j < sModelPieces.getRayParameters().length;
        j++) {
      System.out.format("%3d          %8.6f\n", j, sModelPieces.getRayParameters()[j]);
    }
  }
}
