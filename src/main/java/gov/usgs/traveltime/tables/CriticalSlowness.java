package gov.usgs.traveltime.tables;

/**
 * This class stores the critical slowness value. The critical points are the beginnings and end of
 * travel-time branches. They correspond to velocities above and below discontinuities in the Earth
 * model as well as caustics.
 *
 * @author Ray Buland
 */
public class CriticalSlowness implements Comparable<CriticalSlowness> {
  /** A char containing the slowness type of this critical slowness */
  private char slownessType;

  /** A ShellLoc containing the position of the critical slowness within the shell */
  private ShellLoc location;

  /** An int containing the P-wave slowness shell index */
  private int pWaveSlownessIndex = -1;

  /** An int containing the S-wave slowness shell index */
  private int sWaveSlownessIndex = -1;

  /** A double containing the non-dimensional slowness */
  private double slowness;

  /**
   * CriticalSlowness constructor, creates a single critical slowness value.
   *
   * @param slownessType A char containing the slowness type (P = P-wave, S = S-wave)
   * @param iShell An int containing the associated Earth model shell index
   * @param location A ShellLoc containing the location within the Earth model shell
   * @param slowness A double containing the non-dimensional slowness value
   */
  public CriticalSlowness(char slownessType, int iShell, ShellLoc location, double slowness) {
    this.slownessType = slownessType;
    this.location = location;
    this.slowness = slowness;

    if (slownessType == 'P') {
      pWaveSlownessIndex = iShell;
    } else {
      sWaveSlownessIndex = iShell;
    }
  }

  /**
   * Function to return the non-dimensional slowness
   *
   * @return a double containing thenon-dimensional slowness
   */
  public double getSlowness() {
    return slowness;
  }

  /**
   * Set the shell index for a given slowness type.
   *
   * @param slownessType A char containing the slowness type (P = P-wave, S = S-wave)
   * @param iShell An int containing the associated Earth model shell index
   */
  public void setShellIndex(char slownessType, int iShell) {
    if (slownessType == 'P') {
      pWaveSlownessIndex = iShell;
    } else {
      sWaveSlownessIndex = iShell;
    }
  }

  /**
   * Get the shell index for a given slowness type.
   *
   * @param slownessType A char containing the slowness type (P = P-wave, S = S-wave)
   * @return An int containing the associated Earth model shell index
   */
  public int getShellIndex(char slownessType) {
    if (slownessType == 'P') {
      return pWaveSlownessIndex;
    } else {
      return sWaveSlownessIndex;
    }
  }

  /**
   * Function to compare a given critical value with this one.
   *
   * @param crit A CriticalSlowness containing the critical value to compare
   * @return A boolean flag, true if the given critical value matches this one
   */
  public boolean isSame(CriticalSlowness crit) {
    if (this.slowness == crit.slowness && this.slownessType == crit.slownessType) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Function to compare a given critical value with this one for sorting.
   *
   * @param crit A CriticalSlowness containing the critical value to compare
   * @return An int representing whether the given critical value is "greater", "lesser", or equal.
   */
  @Override
  public int compareTo(CriticalSlowness crit) {
    if (this.slowness > crit.slowness) {
      return +1;
    } else if (this.slowness < crit.slowness) {
      return -1;
    } else {
      // If the slownesses are the same look at the position.
      if (this.location == ShellLoc.BOUNDARY) {
        return +1;
      } else if (this.location == ShellLoc.SHELL) {
        return -1;
      } else {
        return 0;
      }
    }
  }

  /**
   * Function to convert this CriticalSlowness to a string.
   *
   * @return a String representing this CriticalSlowness.
   */
  @Override
  public String toString() {
    return String.format(
        "%c %-8s %8.6f %3d %3d",
        slownessType, location, slowness, pWaveSlownessIndex, sWaveSlownessIndex);
  }
}
