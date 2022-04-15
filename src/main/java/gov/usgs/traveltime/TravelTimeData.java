package gov.usgs.traveltime;

/**
 * Holds everything known about one seismic phase at a particular source depth and source-receiver
 * distance.
 *
 * @author Ray Buland
 */
public class TravelTimeData implements Comparable<TravelTimeData> {
  String phaseCode; // Phase code
  String[] uniqueCode; // Unique phase codes
  double tt; // Travel time (s)
  double dTdD; // Derivative of time with respect to distance (s/degree)
  double dTdZ; // Derivative of time with respect to depth (s/km)
  double dXdP; // Derivative of distance with respect to ray parameter (degree-s)
  double spread; // Statistical spread (s)
  double observ; // Relative statistical observability
  double dSdD; // Derivative of spread with respect to distance (s/degree)
  double window; // Association window in seconds
  String PhaseGroup; // Teleseismic phase group
  String auxGroup; // Auxiliary phase group
  boolean isRegionalPhase; // If true, phase is regional
  boolean isDepth; // If true, phase is depth sensitive
  boolean canUse; // If true, can use the phase for location
  boolean dis; // Disrespect (down weight) this phase
  boolean corrTt; // If true, get the arrival time from the phase statistics

  /**
   * The constructor accepts basic travel time information.
   *
   * @param phaseCode Phase code
   * @param uniqueCode Unique phase code
   * @param tt Travel time
   * @param dTdD Derivative of time with respect to distance
   * @param dTdZ Derivative of time with respect to depth
   * @param dXdP Derivative of distance with respect to ray parameter
   * @param corrTt True if the travel-time itself needs to be modified by the statistical bias
   */
  public TravelTimeData(
      String phaseCode,
      String[] uniqueCode,
      double tt,
      double dTdD,
      double dTdZ,
      double dXdP,
      boolean corrTt) {
    this.phaseCode = phaseCode;
    this.uniqueCode = uniqueCode;
    this.tt = tt;
    this.dTdD = dTdD;
    this.dTdZ = dTdZ;
    this.dXdP = dXdP;
    this.corrTt = corrTt;
  }

  /**
   * Add phase statistical parameters.
   *
   * @param spread Statistical spread
   * @param observ Relative statistical observability
   * @param dSdD Derivative of spread with respect to distance
   */
  public void addStats(double spread, double observ, double dSdD) {
    this.spread = spread;
    this.observ = observ;
    this.dSdD = dSdD;
    window = Math.max(TauUtilities.ASSOCFACTOR * spread, TauUtilities.WINDOWMIN);
  }

  /**
   * Add phase flags.
   *
   * @param PhaseGroup Base phase group
   * @param auxGroup Auxiliary phase group
   * @param isRegionalPhase True if this is a regional phase
   * @param isDepth True if this is a depth phase
   * @param canUse True if this phase can be used in an earthquake location
   * @param dis True if this phase should be down weighted during phase identification
   */
  public void addFlags(
      String PhaseGroup,
      String auxGroup,
      boolean isRegionalPhase,
      boolean isDepth,
      boolean canUse,
      boolean dis) {
    this.PhaseGroup = PhaseGroup;
    this.auxGroup = auxGroup;
    this.isRegionalPhase = isRegionalPhase;
    this.isDepth = isDepth;
    this.canUse = canUse;
    this.dis = dis;
  }

  /**
   * Find all instances of one string and replace them with another. Used to turn Pb into Pg and Sb
   * into Sg. Also, resets the disrespect flag.
   *
   * @param find String to replace
   * @param replace Replacement string
   */
  public void replace(String find, String replace) {
    phaseCode = phaseCode.replace(find, replace);
    dis = false;
  }

  /**
   * Getter for phase code.
   *
   * @return Phase code
   */
  public String getPhaseCode() {
    return phaseCode;
  }

  /**
   * Getter for travel time.
   *
   * @return Travel time in seconds
   */
  public double getTT() {
    return tt;
  }

  /**
   * Getter for the tangential derivative.
   *
   * @return Derivative of travel time with distance
   */
  public double getDTdD() {
    return dTdD;
  }

  /**
   * Getter for the vertical derivative.
   *
   * @return Derivative of travel time with source depth
   */
  public double getDTdZ() {
    return dTdZ;
  }

  /**
   * Getter for the ray parameter derivative.
   *
   * @return Derivative of distance with ray parameter
   */
  public double getDXdP() {
    return dXdP;
  }

  /**
   * Getter for spread.
   *
   * @return Spread (scatter) of travel times in seconds
   */
  public double getPhaseSpread() {
    return spread;
  }

  /**
   * Getter for observability.
   *
   * @return Relative number of observations
   */
  public double getPhaseObservability() {
    return observ;
  }

  /**
   * Getter for the derivative of spread with respect to distance.
   *
   * @return Derivative of spread with respect to distance
   */
  public double getDSdD() {
    return dSdD;
  }

  /**
   * Getter for association window.
   *
   * @return Association window in seconds
   */
  public double getWindow() {
    return window;
  }

  /**
   * Getter for phase group.
   *
   * @return Phase group
   */
  public String getPhaseGroup() {
    return PhaseGroup;
  }

  /**
   * Getter for the auxiliary phase group.
   *
   * @return Auxiliary phase group
   */
  public String getAuxGroup() {
    return auxGroup;
  }

  /**
   * Getter for the regional flag.
   *
   * @return True if the phase is regional
   */
  public boolean isRegionalPhase() {
    return isRegionalPhase;
  }

  /**
   * Getter for the depth flag.
   *
   * @return True if the phase is depth sensitive
   */
  public boolean isDepth() {
    return isDepth;
  }

  /**
   * Getter for the phase use flag.
   *
   * @return True if the phase can be used in an earthquake location
   */
  public boolean canUse() {
    return canUse;
  }

  /**
   * Getter for the disrespect flag.
   *
   * @return True if this phase should be down weighted
   */
  public boolean getDis() {
    return dis;
  }

  /**
   * Make arrival times sortable into time order.
   *
   * @param arrival An travel-time data object.
   * @return +1, 0, or -1 if arrival is later, the same time or earlier
   */
  @Override
  public int compareTo(TravelTimeData arrival) {
    // Sort into arrival time order.
    if (this.tt < arrival.tt) return +1;
    else if (this.tt == arrival.tt) return 0;
    else return -1;
  }

  /**
   * Return this arrival formatted similarly to the arrival time list produced by the Locator
   * version of Ttim.
   */
  @Override
  public String toString() {
    return String.format(
        "%-8s %7.2f %10.2e %10.2e %6.2f %7.1f  " + "%-6s %-6s %-6b %-6b %-6b %-6b",
        phaseCode,
        tt,
        dTdD,
        dTdZ,
        spread,
        observ,
        PhaseGroup,
        auxGroup,
        canUse,
        isRegionalPhase,
        isDepth,
        dis);
  }
}