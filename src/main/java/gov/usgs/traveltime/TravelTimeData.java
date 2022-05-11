package gov.usgs.traveltime;

/**
 * TravelTimeData holds everything known about one seismic phase at a particular source depth and
 * source-receiver distance.
 *
 * @author Ray Buland
 */
public class TravelTimeData implements Comparable<TravelTimeData> {
  /** A String containing the phase code */
  private String phaseCode;

  /** An array of Strings containing the list of unique phase codes */
  private String[] uniquePhaseCodeList;

  /** A double value containing the travel time in seconds */
  private double travelTime;

  /**
   * A double value containing the tangential derivative of time with respect to distance in seconds
   * per degree (s/degree)
   */
  private double distanceDerivitive;

  /**
   * A double value containing the vertical derivative of time with respect to source depth in
   * seconds per kilometer (s/km)
   */
  private double depthDerivitive;

  /**
   * A double value containing the derivative of distance with respect to ray parameter in degree
   * seconds (degree-s)
   */
  private double rayDerivative;

  /** A double value holding the statistical spread (scatter) of the travel time in seconds */
  private double statisticalSpread;

  /** A double value holding the relative statistical observability of the travel time */
  private double observability;

  /**
   * A double value holding the derivative of the statistical spread of the travel time with respect
   * to distance in seconds per degree (s/degree)
   */
  private double spreadDerivative;

  /**
   * A double value containing the assocation and identification window for this travel time in
   * seconds
   */
  private double assocWindow;

  /** A String holding the teleseismic phase group code */
  private String groupPhaseCode;

  /** A String holding the auxiliary phase group code */
  private String auxiliaryPhaseCode;

  /** A boolean flag, if true, the travel time (and phase) is regional */
  private boolean isRegional;

  /** A boolean flag, if true, the travel time (and phase) is depth sensitive */
  private boolean isDepthSensitive;

  /** A boolean flag, if true, the travel time (and phase) can be used for locations */
  private boolean locationCanUse;

  /**
   * A boolean flag, if true, the travel time (and phase) should be down weighted (disrespected) for
   * assocation and identification
   */
  private boolean assocDownWeight;

  /**
   * A boolan flag, if true the travel-time itself needs to be modified by the statistical bias from
   * the phase statistics
   */
  private boolean modifyFromStatistics;

  /**
   * Function to get the phase code
   *
   * @return A String containing the phase code
   */
  public String getPhaseCode() {
    return phaseCode;
  }

  /**
   * Function to get the list of unique phase codes
   *
   * @return An array of Strings containing the list of unique phase codes
   */
  public String[] getUniquePhaseCodeList() {
    return uniquePhaseCodeList;
  }

  /**
   * Get the travel time.
   *
   * @return A double value containing the travel time in seconds
   */
  public double getTravelTime() {
    return travelTime;
  }

  /**
   * Set the travel time.
   *
   * @param travelTime A double value containing the travel time in seconds
   */
  public void setTravelTime(double travelTime) {
    this.travelTime = travelTime;
  }

  /**
   * Get the tangential derivative of time with respect to distance
   *
   * @return A double value containing the tangential derivative of time with respect to distance in
   *     seconds per degree (s/degree)
   */
  public double getDistanceDerivitive() {
    return distanceDerivitive;
  }

  /**
   * Get the vertical derivative of time with respect to source depth
   *
   * @return A double value containing the vertical derivative of time with respect to source depth
   *     in seconds per kilometer (s/km)
   */
  public double getDepthDerivitive() {
    return depthDerivitive;
  }

  /**
   * Get the ray parameter derivative.
   *
   * @return A double value containing the derivative of distance with respect to ray parameter in
   *     degree seconds (degree-s)
   */
  public double getRayDerivative() {
    return rayDerivative;
  }

  /**
   * Get the statistical spread (scatter) of the travel time
   *
   * @return A double value holding the statistical spread (scatter) of the travel time in seconds
   */
  public double getStatisticalSpread() {
    return statisticalSpread;
  }

  /**
   * Set the relative statistical observability of the travel time
   *
   * @param observability A double value containing the relative statistical observability of the
   *     travel time
   */
  public void setObservability(double observability) {
    this.observability = observability;
  }

  /**
   * Get the relative statistical observability of the travel time
   *
   * @return A double value holding the relative statistical observability of the travel time
   */
  public double getObservability() {
    return observability;
  }

  /**
   * Get the derivative of the statistical spread of the travel time with respect to distance
   *
   * @return A double value holding the derivative of the statistical spread of the travel time with
   *     respect to distance in seconds per degree (s/degree)
   */
  public double getSpreadDerivative() {
    return spreadDerivative;
  }

  /**
   * Get the assocation and identification window for this travel time
   *
   * @return A double value containing the assocation and identification window for this travel time
   *     in seconds
   */
  public double getAssocWindow() {
    return assocWindow;
  }

  /**
   * Get the the teleseismic phase group code
   *
   * @return A String holding the teleseismic phase group code
   */
  public String getGroupPhaseCode() {
    return groupPhaseCode;
  }

  /**
   * Get the auxiliary phase group code
   *
   * @return A String holding the auxiliary phase group code
   */
  public String getAuxiliaryPhaseCode() {
    return auxiliaryPhaseCode;
  }

  /**
   * Get whether the travel time (and phase) is regional
   *
   * @return A boolean flag, if true, the travel time (and phase) is regional
   */
  public boolean getIsRegional() {
    return isRegional;
  }

  /**
   * Get whether the travel time (and phase) is depth sensitive
   *
   * @return A boolean flag, if true, the travel time (and phase) is depth sensitive
   */
  public boolean getIsDepthSensitive() {
    return isDepthSensitive;
  }

  /**
   * Set whether the travel time (and phase) can be used for locations
   *
   * @param locationCanUse A boolean flag, if true, the travel time (and phase) can be used for
   *     locations
   */
  public void setLocationCanUse(boolean locationCanUse) {
    this.locationCanUse = locationCanUse;
  }

  /**
   * Get whether the travel time (and phase) can be used for locations
   *
   * @return A boolean flag, if true, the travel time (and phase) can be used for locations
   */
  public boolean getLocationCanUse() {
    return locationCanUse;
  }

  /**
   * Get whether the travel time (and phase) should be down weighted (disrespected) for assocation
   * and identification
   *
   * @return A boolean flag, if true, the travel time (and phase) should be down weighted
   *     (disrespected) for assocation and identification
   */
  public boolean getAssocDownWeight() {
    return assocDownWeight;
  }

  /**
   * Get whether the travel time (and phase) needs to be modified by the statistical bias from the
   * phase statistics
   *
   * @return A boolan flag, if true the travel-time itself needs to be modified by the statistical
   *     bias from the phase statistics
   */
  public boolean getModifyFromStatistics() {
    return modifyFromStatistics;
  }

  /**
   * The TravelTimeData constructor, accepts basic travel time information.
   *
   * @param phaseCode A String containing the phase code
   * @param uniquePhaseCodeList An array of Strings containing the unique phase code list
   * @param travelTime A double holding the travel time
   * @param distanceDerivitive A double holding the derivative of time with respect to distance
   * @param depthDerivitive A double holding the derivative of time with respect to depth
   * @param rayDerivative A double holding the derivative of distance with respect to ray parameter
   * @param modifyFromStatistics A boolan flag, if true the travel-time itself needs to be modified
   *     by the statistical bias from the phase statistics
   */
  public TravelTimeData(
      String phaseCode,
      String[] uniquePhaseCodeList,
      double travelTime,
      double distanceDerivitive,
      double depthDerivitive,
      double rayDerivative,
      boolean modifyFromStatistics) {
    this.phaseCode = phaseCode;
    this.uniquePhaseCodeList = uniquePhaseCodeList;
    this.travelTime = travelTime;
    this.distanceDerivitive = distanceDerivitive;
    this.depthDerivitive = depthDerivitive;
    this.rayDerivative = rayDerivative;
    this.modifyFromStatistics = modifyFromStatistics;
  }

  /**
   * Function to add phase statistical parameters.
   *
   * @param statisticalSpread A double containing the phase statistical spread
   * @param observability A double containing the phase relative statistical observability
   * @param spreadDerivative A double containing the phase derivative of spread with respect to
   *     distance
   */
  public void addStats(double statisticalSpread, double observability, double spreadDerivative) {
    this.statisticalSpread = statisticalSpread;
    this.observability = observability;
    this.spreadDerivative = spreadDerivative;

    assocWindow =
        Math.max(
            TauUtilities.ASSOCWINDOWFACTOR * statisticalSpread, TauUtilities.ASSOCWINDOWMINIMUM);
  }

  /**
   * Function to set the phase flags.
   *
   * @param groupPhaseCode A String containing the group phase code
   * @param auxiliaryPhaseCode A String holding the auxiliary phase group code
   * @param isRegional A boolean flag, true if this is a regional phase
   * @param isDepthSensitive A boolean flag, true if this is a depth phase
   * @param locationCanUse A boolean flag, true if this phase can be used in an earthquake location
   * @param assocDownWeight A boolean flag, true if this phase should be down weighted during phase
   *     assocation and identification
   */
  public void addFlags(
      String groupPhaseCode,
      String auxiliaryPhaseCode,
      boolean isRegional,
      boolean isDepthSensitive,
      boolean locationCanUse,
      boolean assocDownWeight) {
    this.groupPhaseCode = groupPhaseCode;
    this.auxiliaryPhaseCode = auxiliaryPhaseCode;
    this.isRegional = isRegional;
    this.isDepthSensitive = isDepthSensitive;
    this.locationCanUse = locationCanUse;
    this.assocDownWeight = assocDownWeight;
  }

  /**
   * Function to find find all instances of one phase code string and replace them with another.
   * Used to turn Pb into Pg and Sb into Sg. Also, resets the disrespect flag.
   *
   * @param find A String to holding the string replace
   * @param replace A String holding the replacement string
   */
  public void replacePhaseCode(String find, String replace) {
    phaseCode = phaseCode.replace(find, replace);
    assocDownWeight = false;
  }

  /**
   * TravelTimeData comparision function, makes travel times sortable into arrival time order.
   *
   * @param travelTime A TravelTimeData object to compare to.
   * @return An integer value, +1, 0, or -1 if TravelTimeData object is later, the same time or
   *     earlier
   */
  @Override
  public int compareTo(TravelTimeData travelTime) {
    // Sort into arrival time order.
    if (this.travelTime < travelTime.travelTime) {
      return +1;
    } else if (this.travelTime == travelTime.travelTime) {
      return 0;
    } else {
      return -1;
    }
  }

  /**
   * Function to return this TravelTimeData as a string, formatted similarly to the travel time list
   * produced by the Locator version of Ttim.
   */
  @Override
  public String toString() {
    return String.format(
        "%-8s %7.2f %10.2e %10.2e %6.2f %7.1f  " + "%-6s %-6s %-6b %-6b %-6b %-6b",
        phaseCode,
        travelTime,
        distanceDerivitive,
        depthDerivitive,
        statisticalSpread,
        observability,
        groupPhaseCode,
        auxiliaryPhaseCode,
        locationCanUse,
        isRegional,
        isDepthSensitive,
        assocDownWeight);
  }
}
