package gov.usgs.traveltime.tables;

import gov.usgs.traveltime.ModConvert;
import java.util.ArrayList;

/**
 * Put together the bits and pieces we'll need to make the travel-time branch layout and decimation
 * come out right.
 *
 * @author Ray Buland
 */
public class TablePieces {
  BranchIntegrals pPieces, sPieces;
  ArrayList<ModelShell> pShells, sShells;
  ModConvert convert;

  /**
   * Initialize bits and pieces for each phase type.
   *
   * @param finModel Final tau model
   */
  public TablePieces(TauModel finModel) {
    convert = finModel.convert;
    pShells = finModel.pShells;
    sShells = finModel.sShells;
    pPieces = new BranchIntegrals('P', finModel);
    sPieces = new BranchIntegrals('S', finModel);
  }

  /** Decimate the ray parameter arrays for both the P and S models. */
  public void decimateRayParameters() {
    pPieces.decimateRayParameters();
    sPieces.decimateRayParameters();
  }

  /**
   * Get the integral pieces for one phase type.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @return Integral pieces
   */
  public BranchIntegrals getPiece(char type) {
    if (type == 'P') {
      return pPieces;
    } else {
      return sPieces;
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
    return sPieces.getRayParameters()[index];
  }

  /**
   * Get all the ray parameters.
   *
   * @return Ray parameter array
   */
  public double[] getP() {
    return sPieces.getRayParameters();
  }

  /**
   * Get a major shell tau value by index.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @param index Tau index
   * @param shell Major shell index (0 = mantle, 1 = outer core, 2 = inner core)
   * @return Tau value
   */
  public double getTau(char type, int index, int shell) {
    switch (shell) {
      case 0:
        if (type == 'P') {
          return pPieces.getMantleTauIntegrals()[index];
        } else {
          return sPieces.getMantleTauIntegrals()[index];
        }
      case 1:
        if (type == 'P') {
          return pPieces.getOuterCoreTauIntegrals()[index];
        } else {
          return sPieces.getOuterCoreTauIntegrals()[index];
        }
      case 2:
        if (type == 'P') {
          return pPieces.getInnerCoreTauIntegrals()[index];
        } else {
          return sPieces.getInnerCoreTauIntegrals()[index];
        }
      default:
        return Double.NaN;
    }
  }

  /**
   * Get a major shell range value by index.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @param index Range index
   * @param shell Major shell index (0 = mantle, 1 = outer core, 2 = inner core)
   * @return Range value
   */
  public double getX(char type, int index, int shell) {
    switch (shell) {
      case 0:
        if (type == 'P') {
          return pPieces.getMantleRangeIntegrals()[index];
        } else {
          return sPieces.getMantleRangeIntegrals()[index];
        }
      case 1:
        if (type == 'P') {
          return pPieces.getOuterCoreRangeIntegrals()[index];
        } else {
          return sPieces.getOuterCoreRangeIntegrals()[index];
        }
      case 2:
        if (type == 'P') {
          return pPieces.getInnerCoreRangeIntegrals()[index];
        } else {
          return sPieces.getInnerCoreRangeIntegrals()[index];
        }
      default:
        return Double.NaN;
    }
  }

  /**
   * Get the radial sampling interval associated with a shell by index.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @param index Shell index
   * @return Non-dimensional radial sampling
   */
  public double getDelX(char type, int index) {
    if (type == 'P') {
      return convert.normR(pShells.get(index).getRangeIncrementTarget());
    } else {
      return convert.normR(sShells.get(index).getRangeIncrementTarget());
    }
  }

  /**
   * Get the radial sampling interval associated with the shell above the current one.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @param index Shell index
   * @return Non-dimensional radial sampling
   */
  public double getNextDelX(char type, int index) {
    if (type == 'P') {
      for (int j = index + 1; j < pShells.size(); j++) {
        if (!pShells.get(j).getIsDiscontinuity()) {
          return convert.normR(pShells.get(j).getRangeIncrementTarget());
        }
      }
    } else {
      for (int j = index + 1; j < sShells.size(); j++) {
        if (!sShells.get(j).getIsDiscontinuity()) {
          return convert.normR(sShells.get(j).getRangeIncrementTarget());
        }
      }
    }
    return Double.NaN;
  }

  /**
   * Get the master decimation array.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @return The master decimation array
   */
  public boolean[] getDecimation(char type) {
    if (type == 'P') {
      return pPieces.getDecimationKeep();
    } else {
      return sPieces.getDecimationKeep();
    }
  }

  /** Print out the proxy ranges. */
  public void printProxy() {
    int nP, nS;

    System.out.println("\n\t\t\tProxy Ranges");
    System.out.println("                  P                            S");
    System.out.println("    slowness      X       delX   slowness" + "      X       delX");
    nP = pPieces.getProxyRanges().length;
    nS = sPieces.getProxyRanges().length;
    System.out.format(
        "%3d %8.6f %8.2f            %8.6f %8.2f\n",
        0,
        pPieces.getProxyRayParameters()[0],
        convert.dimR(pPieces.getProxyRanges()[0]),
        sPieces.getProxyRayParameters()[0],
        convert.dimR(sPieces.getProxyRanges()[0]));
    for (int j = 1; j < nP; j++) {
      System.out.format(
          "%3d %8.6f %8.2f %8.2f   %8.6f %8.2f %8.2f\n",
          j,
          pPieces.getProxyRayParameters()[j],
          convert.dimR(pPieces.getProxyRanges()[j]),
          convert.dimR(pPieces.getProxyRanges()[j] - pPieces.getProxyRanges()[j - 1]),
          sPieces.getProxyRayParameters()[j],
          convert.dimR(sPieces.getProxyRanges()[j]),
          convert.dimR(sPieces.getProxyRanges()[j + 1] - sPieces.getProxyRanges()[j]));
    }
    for (int j = nP; j < nS; j++) {
      System.out.format(
          "%3d                              " + "%8.6f %8.2f %8.2f\n",
          j,
          sPieces.getProxyRayParameters()[j],
          convert.dimR(sPieces.getProxyRanges()[j]),
          convert.dimR(sPieces.getProxyRanges()[j] - sPieces.getProxyRanges()[j - 1]));
    }
  }

  /**
   * Print the integrals for the whole mantle, outer core, and inner core.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   */
  public void printShellInts(char type) {
    if (type == 'P') {
      pPieces.printShellInts();
    } else {
      sPieces.printShellInts();
    }
  }

  /** Print the ray parameter arrays for both the P and S branches. */
  public void printP() {
    System.out.println("\nMaster Ray Parameters");
    System.out.println("       P        S");
    for (int j = 0; j < pPieces.getRayParameters().length; j++) {
      System.out.format(
          "%3d %8.6f %8.6f\n", j, pPieces.getRayParameters()[j], sPieces.getRayParameters()[j]);
    }
    for (int j = pPieces.getRayParameters().length; j < sPieces.getRayParameters().length; j++) {
      System.out.format("%3d          %8.6f\n", j, sPieces.getRayParameters()[j]);
    }
  }
}
