package gov.usgs.traveltime;

/**
 * The BadDepthException class is the exception handler class used for when a phase is not found
 *
 * @author Ray Buland
 */
public class PhaseNotFoundException extends Exception {
  /** A long containing the version id used in serialization */
  private static final long serialVersionUID = 1L;

  /** A String containing the phase code of the exception */
  private String phaseCode;

  /**
   * The PhaseNotFoundException constructor
   *
   * @param phaseCode A String containing the exception phase code
   */
  public PhaseNotFoundException(String phaseCode) {
    super(phaseCode);
    this.phaseCode = phaseCode;
  }

  /**
   * Function to convert this PhaseNotFoundException into a string.
   *
   * @return A String containing the string representation of this PhaseNotFoundException
   */
  public String toString() {
    return "Phase not found (" + phaseCode + ")";
  }
}
