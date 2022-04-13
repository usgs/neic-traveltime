package gov.usgs.traveltime;

public class PhaseNotFoundException extends Exception {
  private static final long serialVersionUID = 1L;
  String phaseCode;

  public PhaseNotFoundException(String phaseCode) {
    super(phaseCode);
    this.phaseCode = phaseCode;
  }

  public String toString() {
    return "Phase not found (" + phaseCode + ")";
  }
}
