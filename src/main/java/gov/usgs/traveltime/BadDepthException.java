package gov.usgs.traveltime;

/**
 * The BadDepthException class is the exception handler class used for Travel-time depth out of
 * range.
 *
 * @author Ray Buland
 */
public class BadDepthException extends Exception {
  /** A long containing the version id used in serialization */
  private static final long serialVersionUID = 1L;

  /** A String containing the exception message */
  String message;

  /**
   * The BadDepthException constructor
   *
   * @param message A String containing the exception message
   */
  public BadDepthException(String message) {
    super(message);
    this.message = message;
  }

  /**
   * Function to convert this BadDepthException into a string.
   *
   * @return A String containing the string representation of this BadDepthException
   */
  public String toString() {
    return "Bad tau integral (" + message + ")";
  }
}
