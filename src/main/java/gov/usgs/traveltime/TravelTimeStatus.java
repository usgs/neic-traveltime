package gov.usgs.traveltime;

/**
 * The TravelTimeStatus class is an enumeration containing status and exit conditions.
 *
 * @author Ray Buland
 */
public enum TravelTimeStatus {
  /** Internal success status. */
  SUCCESS(0),

  /** Read failure on Earth model file--intended as an external exit status. */
  BAD_MODEL_READ(202),

  /**
   * Something bad (probably fatal) is wrong with the Earth model file--intended as an external exit
   * status.
   */
  BAD_MODEL_FILE(203),

  /** Unable to do the tau-x partial integrals due to a bad model interval. */
  BAD_TAU_INTERVAL(204),

  /** Unable to open the phase list file or it's empty. */
  BAD_PHASE_LIST(205);

  /** An integer containing the exit status */
  private final int status;

  /**
   * The TravelTimeStatus constructor sets up the exit values.
   *
   * @param status Exit value
   */
  TravelTimeStatus(int status) {
    this.status = status;
  }

  /**
   * Function to get the exit value.
   *
   * @return An integer containing the exit status
   */
  public int getStatus() {
    return status;
  }
}
