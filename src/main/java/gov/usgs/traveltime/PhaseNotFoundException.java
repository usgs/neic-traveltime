package gov.usgs.traveltime;

public class PhaseNotFoundException extends Exception {
  private static final long serialVersionUID = 1L;
  String phCode;

  public PhaseNotFoundException(String phCode) {
    super(phCode);
    this.phCode = phCode;
  }

  public String toString() {
    return "Phase not found (" + phCode + ")";
  }
}
