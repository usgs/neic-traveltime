package gov.usgs.traveltime;

/**
 * Status and exit conditions.
 *
 * @author Ray Buland
 */
public enum TtStatus {
  // Travel-time status:
  /** Internal success status. */
  SUCCESS(0), // Success
  /** Read failure on Earth model file--intended as an external exit status. */
  BAD_MODEL_READ(202), // Model read failed
  /**
   * Something bad (probably fatal) is wrong with the Earth model file--intended as an external exit
   * status.
   */
  BAD_MODEL_FILE(203), // Model file is badly formed
  /** Unable to do the tau-x partial integrals due to a bad model interval. */
  BAD_TAU_INTERVAL(204), // Screwed up tau model
  /** Unable to open the phase list file or it's empty. */
  BAD_PHASE_LIST(205); // Phase list is messed up

  private final int status; // Exit flag

  /**
   * The constructor sets up the exit values.
   *
   * @param status Exit value
   */
  TtStatus(int status) {
    this.status = status;
  }

  /**
   * Get the exit value.
   *
   * @return Exit value
   */
  public int status() {
    return status;
  }
}
