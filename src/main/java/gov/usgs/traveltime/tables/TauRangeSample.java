package gov.usgs.traveltime.tables;

import java.util.Arrays;

/**
 * Tau and range integrals for all ray parameters from the surface to one source depth.
 *
 * @author Ray Buland
 */
public class TauRangeSample {
  /** A ShellName object containing the name for depth to which the integrals are done */
  private ShellName name;

  /** A boolean flag indicating if this is the top of a high slowness zone */
  private boolean lowVelocityZone;

  /** An array of doubles containing the non-dimensional tau integrals */
  private double[] tauIntegrals;

  /** An array of doubles containing the non-dimensional range (ray travel distance) integrals */
  private double[] rangeIntegrals;

  /**
   * Get the name for depth to which the integrals are done
   *
   * @return A ShellName object containing the name for depth to which the integrals are done
   */
  public ShellName getName() {
    return name;
  }

  /**
   * Get whether this is the top of a high slowness zone
   *
   * @return A boolean flag indicating if this is the top of a high slowness zone
   */
  public boolean getLowVelocityZone() {
    return lowVelocityZone;
  }

  /**
   * Set whether this is the top of a high slowness zone
   *
   * @param lowVelocityZone A boolean flag indicating if this is the top of a high slowness zone
   */
  public void setLowVelocityZone(boolean lowVelocityZone) {
    this.lowVelocityZone = lowVelocityZone;
  }

  /**
   * Get the non-dimensional tau integrals
   *
   * @return An array of doubles containing the non-dimensional tau integrals
   */
  public double[] getTauIntegrals() {
    return tauIntegrals;
  }

  /**
   * Get the non-dimensional range (ray travel distance) integrals
   *
   * @return An array of doubles containing the non-dimensional range (ray travel distance)
   *     integrals
   */
  public double[] getRangeIntegrals() {
    return rangeIntegrals;
  }

  /**
   * Copy the partial tau and range integrals. The shell names are only used for the special cases
   * of all integrals in the mantle, outer core, and inner core.
   *
   * @param length An integer containing the length of the tau and range integral arrays.
   * @param tauIntegrals An array of double values containing the tau integrals
   * @param rangeIntegrals An array of double values containing the range integrals
   * @param name Name associated with this source depth
   */
  public TauRangeSample(
      int length, double[] tauIntegrals, double[] rangeIntegrals, ShellName name) {
    this.tauIntegrals = Arrays.copyOf(tauIntegrals, length);
    this.rangeIntegrals = Arrays.copyOf(rangeIntegrals, length);
    this.name = name;
    lowVelocityZone = false;
  }

  /**
   * Update the tauIntegrals and range arrays with decimated versions.
   *
   * @param length An integer containing the length of the tau and range integral arrays.
   * @param newTau An array of double values containing the decimated tau integrals
   * @param newRange An array of double values containing the decimated range integrals
   */
  public void update(int length, double[] newTau, double[] newRange) {
    tauIntegrals = Arrays.copyOf(newTau, length);
    rangeIntegrals = Arrays.copyOf(newRange, length);
  }
}
