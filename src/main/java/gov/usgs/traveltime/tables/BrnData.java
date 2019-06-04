package gov.usgs.traveltime.tables;

import java.util.Arrays;

/**
 * Header information needed to set up a branch.
 *
 * @author Ray Buland
 */
public class BrnData {
  // Header:
  String phCode; // Branch phase code
  String phSeg; // Segment code
  String turnShell; // Name of the shell where the rays turn
  boolean isUpGoing; // True if this is an up-going branch
  char[] typeSeg; // Phase type for correction, descending, ascending
  int signSeg; // Sign of the up-going correction
  int countSeg; // Number of mantle traversals
  double[] pRange; // Slowness range for this branch
  double[] xRange; // Distance range for this branch
  double[] rRange; // Radius range of turning shell in kilometers
  // Data:
  double[] p = null; // Non-dimensional ray parameters
  double[] tau = null; // Non-dimensional tau
  double[] x = null; // Non-dimensional range
  double[][] basis; // Interpolation basis coefficients

  /**
   * Set up an up-going branch.
   *
   * @param upType Phase type ('P' or 'S')
   */
  public BrnData(char upType) {
    phCode = "" + upType;
    phSeg = phCode;
    isUpGoing = true;
    typeSeg = new char[3];
    signSeg = 1;
    Arrays.fill(typeSeg, upType);
    countSeg = 0;
  }

  /**
   * Set up a down-going branch.
   *
   * @param phCode Phase code
   * @param typeSeg Types of up-going phase, down-going phase, and phase coming back up
   * @param countSeg Number of mantle traversals
   * @param shell Model shell where the rays in this branch turn
   */
  public BrnData(String phCode, char[] typeSeg, int countSeg, ModelShell shell) {
    this.phCode = phCode;
    phSeg = phCode;
    isUpGoing = false;
    this.typeSeg = Arrays.copyOf(typeSeg, typeSeg.length);
    if (typeSeg[0] == 'p' || typeSeg[0] == 's') {
      signSeg = 1;
      if (typeSeg[0] == 'p') {
        this.typeSeg[0] = 'P';
      } else {
        this.typeSeg[0] = 'S';
      }
    } else {
      signSeg = -1;
    }
    this.countSeg = countSeg;
    if (shell != null) {
      turnShell = shell.name;
      rRange = new double[2];
      rRange[0] = shell.rBot;
      rRange[1] = shell.rTop;
    } else {
      turnShell = null;
      rRange = null;
    }
  }

  /** Do a partial update for the up-going branch. */
  public void update() {
    tau = new double[p.length];
    x = new double[p.length];
    Arrays.fill(tau, 0d);
    Arrays.fill(x, 0d);
    pRange = new double[2];
    pRange[0] = p[0];
    pRange[1] = p[p.length - 1];
    xRange = new double[2];
    xRange[0] = 0d;
    xRange[1] = 0d;
  }

  /**
   * Update the data arrays with the decimated versions.
   *
   * @param len New length
   * @param pNew Decimated slowness sampling
   * @param tauNew Decimated tau values
   * @param xNew Decimated range values
   */
  public void update(int len, double[] pNew, double[] tauNew, double[] xNew) {
    p = Arrays.copyOf(pNew, len);
    tau = Arrays.copyOf(tauNew, len);
    x = Arrays.copyOf(xNew, len);

    // Figure ranges.
    pRange = new double[2];
    pRange[0] = p[0];
    pRange[1] = p[p.length - 1];
    xRange = new double[2];
    xRange[0] = x[0];
    xRange[1] = x[x.length - 1];
  }

  /** @return Phase code */
  public String getPhCode() {
    return phCode;
  }

  /** @return Segment code */
  public String getPhSeg() {
    return phSeg;
  }

  /** @return the turning shell name */
  public String getTurnShell() {
    return turnShell;
  }
  /** @return The up-going branch flag */
  public boolean getIsUpGoing() {
    return isUpGoing;
  }

  /** @return The ray segment type array */
  public char[] getTypeSeg() {
    return typeSeg;
  }

  /** @return The sign of the up-going depth correction */
  public int getSignSeg() {
    return signSeg;
  }

  /** @return The count of the up-going depth correction */
  public int getCountSeg() {
    return countSeg;
  }

  /** @return The summary non-dimensional ray parameter range */
  public double[] getPrange() {
    return pRange;
  }

  /** @return The summary non-dimensional distance range */
  public double[] getXrange() {
    return xRange;
  }

  /** @return The radius range of the turning shell */
  public double[] getRrange() {
    return rRange;
  }

  /** @return The non-dimensional ray parameter sampling */
  public double[] getP() {
    return p;
  }

  /** @return The non-dimensional tau values */
  public double[] getTau() {
    return tau;
  }

  /** @return The non-dimensional ray travel distances */
  public double[] getX() {
    return x;
  }

  /**
   * @param k The basis function row to return
   * @return One row of the interpolation basis functions
   */
  public double[] getBasis(int k) {
    return basis[k];
  }

  /**
   * Print this branch.
   *
   * @param full If true, print the branch data as well as the header
   * @param nice If true, convert range to degrees
   */
  public void dumpBrn(boolean full, boolean nice) {
    if (p == null) {
      System.out.format(
          "%-8s %2d %c %c %c %1d %5b\n",
          phCode, signSeg, typeSeg[0], typeSeg[1], typeSeg[2], countSeg, isUpGoing);
    } else {
      if (full) System.out.println();
      if (!nice) {
        System.out.format(
            "%-8s %2d %c %c %c %1d %5b %8.6f %8.6f %8.6f %8.6f %3d\n",
            phCode,
            signSeg,
            typeSeg[0],
            typeSeg[1],
            typeSeg[2],
            countSeg,
            isUpGoing,
            pRange[0],
            pRange[1],
            xRange[0],
            xRange[1],
            p.length);
        if (turnShell != null) {
          System.out.format("     shell: %7.2f %7.2f %s\n", rRange[0], rRange[1], turnShell);
        }
        if (full) {
          System.out.println(
              "         p       tau      X                " + "basis function coefficients");
          if (x != null) {
            for (int j = 0; j < p.length; j++) {
              System.out.format(
                  "%3d: %8.6f %8.6f %8.6f %9.2e %9.2e %9.2e %9.2e " + "%9.2e\n",
                  j,
                  p[j],
                  tau[j],
                  x[j],
                  basis[0][j],
                  basis[1][j],
                  basis[2][j],
                  basis[3][j],
                  basis[4][j]);
            }
          } else {
            for (int j = 0; j < p.length; j++) {
              System.out.format(
                  "%3d: %8.6f %8.6f %8.6f %9.2e %9.2e %9.2e %9.2e " + "%9.2e\n",
                  j, p[j], 0d, 0d, basis[0][j], basis[1][j], basis[2][j], basis[3][j], basis[4][j]);
            }
          }
        }
      } else {
        System.out.format(
            "%-8s %2d %c %c %c %1d %5b %8.6f %8.6f %6.2f %6.2f %3d\n",
            phCode,
            signSeg,
            typeSeg[0],
            typeSeg[1],
            typeSeg[2],
            countSeg,
            isUpGoing,
            pRange[0],
            pRange[1],
            Math.toDegrees(xRange[0]),
            Math.toDegrees(xRange[1]),
            p.length);
        if (turnShell != null) {
          System.out.format("     shell: %7.2f-%7.2f %s\n", rRange[0], rRange[1], turnShell);
        }
        if (full) {
          System.out.println(
              "         p       tau      X                " + "basis function coefficients");
          if (x != null) {
            for (int j = 0; j < p.length; j++) {
              System.out.format(
                  "%3d: %8.6f %8.6f %6.2f %9.2e %9.2e %9.2e %9.2e " + "%9.2e\n",
                  j,
                  p[j],
                  tau[j],
                  Math.toDegrees(x[j]),
                  basis[0][j],
                  basis[1][j],
                  basis[2][j],
                  basis[3][j],
                  basis[4][j]);
            }
          } else {
            for (int j = 0; j < p.length; j++) {
              System.out.format(
                  "%3d: %8.6f %8.6f %6.2f %9.2e %9.2e %9.2e %9.2e " + "%9.2e\n",
                  j, p[j], 0d, 0d, basis[0][j], basis[1][j], basis[2][j], basis[3][j], basis[4][j]);
            }
          }
        }
      }
    }
  }

  public String toString() {
    if (p == null) {
      return String.format(
          "%-8s %2d %c %c %c %1d %5b",
          phCode, signSeg, typeSeg[0], typeSeg[1], typeSeg[2], countSeg, isUpGoing);
    } else {
      return String.format(
          "%-8s %2d %c %c %c %1d %5b %8.6f %8.6f %8.6f %8.6f %3d",
          phCode,
          signSeg,
          typeSeg[0],
          typeSeg[1],
          typeSeg[2],
          countSeg,
          isUpGoing,
          pRange[0],
          pRange[1],
          xRange[0],
          xRange[1],
          p.length);
    }
  }
}
