package gov.usgs.traveltime.tables;

import gov.usgs.traveltime.ModConvert;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Put together the bits and pieces we'll need to make the travel-time branch layout and decimation
 * come out right by phase type.
 *
 * @author Ray Buland
 */
public class IntPieces {
  char type;
  boolean[] keep;
  double[] p; // Ray parameters
  double[] mantleTau, mantleX; // Integrals through the mantle
  double[] outerCoreTau, outerCoreX; // Integrals through the outer core
  double[] innerCoreTau, innerCoreX; // Integrals through the inner core
  double[] ocCumTau, ocCumX, icCumTau, icCumX;
  double[] proxyP, proxyX;
  TauModel finModel;
  ModConvert convert;
  ArrayList<TauXsample> ints;

  /**
   * We need the model to get started.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @param finModel Final tau model
   */
  public IntPieces(char type, TauModel finModel) {
    this.finModel = finModel;
    convert = finModel.convert;
    this.type = type;
    if (type == 'P') {
      ints = finModel.pInts;
    } else {
      ints = finModel.sInts;
    }
    setShellInts(type);
    initDecimation(type);
  }

  /**
   * Update the proxy ray parameters and ranges with the decimated versions.
   *
   * @param len Length of the arrays
   * @param pNew Update ray parameter sampling
   * @param xNew Updated range sampling
   */
  public void update(int len, double[] pNew, double[] xNew) {
    proxyP = Arrays.copyOf(pNew, len);
    proxyX = Arrays.copyOf(xNew, len);
  }

  /**
   * Decimate the ray parameter array. Note that keep is an AND of all the branch decimations. That
   * is, if a ray parameter is needed for any branch, it will be kept.
   */
  public void decimateP() {
    int k = 0;
    double[] decP;

    decP = new double[p.length];
    for (int j = 0; j < p.length; j++) {
      if (keep[j]) {
        decP[k++] = p[j];
      }
    }
    p = Arrays.copyOf(decP, k);
  }

  /**
   * Create the tau and range partial integrals by major shells rather than the cumulative integrals
   * computed in Integrate.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   */
  private void setShellInts(char type) {
    // Get the pieces we need.
    mantleTau = finModel.getTauInt(type, ShellName.CORE_MANTLE_BOUNDARY);
    mantleX = finModel.getXInt(type, ShellName.CORE_MANTLE_BOUNDARY);
    ocCumTau = finModel.getTauInt(type, ShellName.INNER_CORE_BOUNDARY);
    ocCumX = finModel.getXInt(type, ShellName.INNER_CORE_BOUNDARY);
    icCumTau = finModel.getTauInt(type, ShellName.CENTER);
    icCumX = finModel.getXInt(type, ShellName.CENTER);
    // Initialize the difference arrays.
    innerCoreTau = new double[mantleTau.length];
    innerCoreX = new double[mantleTau.length];
    outerCoreTau = new double[mantleTau.length];
    outerCoreX = new double[mantleTau.length];
    // Do the differences.
    for (int j = 0; j < mantleTau.length; j++) {
      innerCoreTau[j] = icCumTau[j] - ocCumTau[j];
      innerCoreX[j] = icCumX[j] - ocCumX[j];
      outerCoreTau[j] = ocCumTau[j] - mantleTau[j];
      outerCoreX[j] = ocCumX[j] - mantleX[j];
    }
  }

  /**
   * To decimate the up-going branches, we need a proxy for the range spacing at all source depths
   * so that the ray parameter spacing is common.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   */
  private void initDecimation(char type) {
    int n, n1, m;
    double[] x;
    ArrayList<Double> slowness;

    /** The master keep list will be an or of branch keeps. */
    keep = new boolean[mantleTau.length];
    Arrays.fill(keep, false);
    /** Create the proxy ranges. */
    n1 = mantleX.length;
    proxyX = new double[n1];
    Arrays.fill(proxyX, 0d);
    n = finModel.size(type);
    // Put together a list of maximum range differences.
    for (int i = 1; i < n - 3; i++) {
      x = finModel.getXInt(type, i);
      if (x != null) {
        m = x.length;
        for (int j = 1; j < m; j++) {
          proxyX[j] = Math.max(proxyX[j], Math.abs(x[j - 1] - x[j]));
        }
        if (m + 1 == n1) {
          proxyX[n1 - 1] = x[m - 1];
        }
      }
    }
    // Now put the range differences back together to sort of look
    // like a range.
    slowness = finModel.slowness;
    n = slowness.size() - 1;
    p = new double[n1];
    proxyP = new double[proxyX.length];
    for (int j = 1; j < proxyX.length; j++) {
      p[j] = slowness.get(n - j);
      proxyP[j] = slowness.get(n - j);
      proxyX[j] = proxyX[j - 1] + proxyX[j];
    }
  }

  /** Print out the proxy ranges. */
  public void printProxy() {
    System.out.format("\n\tProxy Ranges for %c\n", type);
    System.out.println("    slowness      X       delX");
    System.out.format("%3d %8.6f %8.2f\n", 0, proxyP[0], convert.dimR(proxyX[0]));
    for (int j = 1; j < proxyX.length; j++) {
      System.out.format(
          "%3d %8.6f %8.2f %8.2f\n",
          j, proxyP[j], convert.dimR(proxyX[j]), convert.dimR(proxyX[j] - proxyX[j - 1]));
    }
  }
  /** Print the ray parameter decimation. */
  public void printDec() {
    System.out.format("\nDecimation for %c\n", type);
    System.out.println("    slowness  keep");
    for (int j = 0; j < p.length; j++) {
      System.out.format("%3d %8.6f %b\n", j, p[j], keep[j]);
    }
  }

  /** Print out the shell integrals. */
  public void printShellInts() {
    System.out.format("\n\t\tShell Integrals for %c-waves\n", type);
    System.out.println("                        Tau                    " + "   X");
    System.out.println("        p     Mantle     OC       IC     Mantle" + "   OC     IC");
    for (int j = 0; j < mantleTau.length; j++) {
      System.out.format(
          "%3d %8.6f %8.6f %8.6f %8.6f %6.2f %6.2f %6.2f\n",
          j,
          p[j],
          mantleTau[j],
          outerCoreTau[j],
          innerCoreTau[j],
          Math.toDegrees(mantleX[j]),
          Math.toDegrees(outerCoreX[j]),
          Math.toDegrees(innerCoreX[j]));
    }
  }
}
