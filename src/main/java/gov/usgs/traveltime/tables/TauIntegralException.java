package gov.usgs.traveltime.tables;

public class TauIntegralException extends Exception {
  private static final long serialVersionUID = 1L;
  String message;

  public TauIntegralException(String message) {
    super(message);
    this.message = message;
  }

  public String toString() {
    return "Bad Tau Integral (" + message + ")";
  }
}
