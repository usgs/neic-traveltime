package gov.usgs.traveltime.tables;

import java.util.Arrays;

/**
 * Tau and range integrals for all ray parameters from the surface to one source depth.
 *
 * @author Ray Buland
 */
public class TauXsample {
  ShellName name; // Name for depth to which the integrals are done
  boolean lvz; // True if this is the top of a high slowness zone
  double[] tau; // Non-dimensional tau
  double[] x; // Non-dimensional range (ray travel distance)

  /**
   * Copy the partial tau and range integrals. The shell names are only used for the special cases
   * of all integrals in the mantle, outer core, and inner core.
   *
   * @param n Length of the tau and x arrays.
   * @param tau Tau array
   * @param x X (range) array
   * @param name Name associated with this source depth
   */
  public TauXsample(int n, double[] tau, double[] x, ShellName name) {
    this.tau = Arrays.copyOf(tau, n);
    this.x = Arrays.copyOf(x, n);
    this.name = name;
    lvz = false;
  }

  /**
   * Update the tau and range arrays with decimated versions.
   *
   * @param len New array lengths
   * @param newTau Decimated tau array
   * @param newX Decimated range array
   */
  public void update(int len, double[] newTau, double[] newX) {
    tau = Arrays.copyOf(newTau, len);
    x = Arrays.copyOf(newX, len);
  }
}
