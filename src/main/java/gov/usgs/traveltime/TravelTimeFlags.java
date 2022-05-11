package gov.usgs.traveltime;

/**
 * The TravelTimeFlags class organizes all the auxiliary travel-time information by phase.
 *
 * @author Ray Buland
 */
public class TravelTimeFlags {
  /** A String holding the teleseismic phase group code */
  private String groupPhaseCode;

  /** A String holding the auxiliary phase group code */
  private String auxiliaryGroupPhaseCode;

  /** A boolean flag, if true, the phase is regional */
  private boolean isRegional;

  /** A boolean flag, if true, the phase is depth sensitive */
  private boolean isDepthSensitive;

  /** A boolean flag, if true, the phase can be used for locations */
  private boolean locationCanUse;

  /**
   * A boolean flag, if true, the phase should be down weighted (disrespected) for assocation and
   * identification
   */
  private boolean assocDownWeight;

  /** A TravelTimeStatistics object containing the phase statistics */
  private final TravelTimeStatistics phaseStatistics;

  /** A Ellipticity object containing the ellipticity corrections */
  private final Ellipticity ellipticityCorrections;

  /** A Ellipticity object containing the ellipticity corrections for up-going P and S branch */
  private final Ellipticity upGoingEllipticityCorrections;

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
  public String getAuxiliaryGroupPhaseCode() {
    return auxiliaryGroupPhaseCode;
  }

  /**
   * Get whether the phase is regional
   *
   * @return A boolean flag, if true, the phase is regional
   */
  public boolean getIsRegional() {
    return isRegional;
  }

  /**
   * Get whether the phase is depth sensitive
   *
   * @return A boolean flag, if true, the phase is depth sensitive
   */
  public boolean getIsDepthSensitive() {
    return isDepthSensitive;
  }

  /**
   * Get whether the phase can be used for locations
   *
   * @return A boolean flag, if true, the phase can be used for locations
   */
  public boolean getLocationCanUse() {
    return locationCanUse;
  }

  /**
   * Get whether the phase should be down weighted (disrespected) for assocation and identification
   *
   * @return A boolean flag, if true, the phase should be down weighted (disrespected) for
   *     assocation and identification
   */
  public boolean getAssocDownWeight() {
    return assocDownWeight;
  }

  /**
   * Get the phase statistics
   *
   * @return A TravelTimeStatistics object containing the phase statistics
   */
  public TravelTimeStatistics getPhaseStatistics() {
    return phaseStatistics;
  }

  /**
   * Get the ellipticity corrections
   *
   * @return A Ellipticity object containing the ellipticity corrections
   */
  public Ellipticity getEllipticityCorrections() {
    return ellipticityCorrections;
  }

  /**
   * Get the ellipticity corrections for up-going P and S branch
   *
   * @return A Ellipticity object containing the ellipticity corrections for up-going P and S branch
   */
  public Ellipticity getUpGoingEllipticityCorrections() {
    return upGoingEllipticityCorrections;
  }

  /**
   * TravelTimeFlags Constructor
   *
   * @param groupPhaseCode A String holding the teleseismic phase group code
   * @param auxiliaryGroupPhaseCode A String holding the auxiliary phase group code
   * @param isRegional A boolean flag, if true, the phase is regional
   * @param isDepthSensitive A boolean flag, if true, the phase is depth sensitive
   * @param locationCanUse A boolean flag, if true, the phase can be used for locations
   * @param assocDownWeight A boolean flag, if true, the phase should be down weighted
   *     (disrespected) for assocation and identification
   * @param phaseStatistics A TravelTimeStatistics object containing the phase statistics
   * @param ellipticityCorrections A Ellipticity object containing the ellipticity corrections
   * @param upGoingEllipticityCorrections A Ellipticity object containing the ellipticity
   *     corrections for up-going P and S branch, if any
   */
  public TravelTimeFlags(
      String groupPhaseCode,
      String auxiliaryGroupPhaseCode,
      boolean isRegional,
      boolean isDepthSensitive,
      boolean locationCanUse,
      boolean assocDownWeight,
      TravelTimeStatistics phaseStatistics,
      Ellipticity ellipticityCorrections,
      Ellipticity upGoingEllipticityCorrections) {
    this.groupPhaseCode = groupPhaseCode;
    this.auxiliaryGroupPhaseCode = auxiliaryGroupPhaseCode;
    this.isRegional = isRegional;
    this.isDepthSensitive = isDepthSensitive;
    this.locationCanUse = locationCanUse;
    this.assocDownWeight = assocDownWeight;
    this.phaseStatistics = phaseStatistics;
    this.ellipticityCorrections = ellipticityCorrections;
    this.upGoingEllipticityCorrections = upGoingEllipticityCorrections;
  }
}
