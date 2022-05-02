package gov.usgs.traveltime;

/**
 * Organize all the auxiliary travel-time information by phase.
 *
 * @author Ray Buland
 */
public class TravelTimeFlags {
  final String PhaseGroup; // Phase group
  final String auxGroup; // Auxiliary phase group
  final boolean isRegionalPhase; // True if phase is regional
  final boolean isDepth; // True if phase is depth sensitive
  final boolean canUse; // True if phase can be used in a location
  final boolean dis; // True if phase is to be down weighted
  final TravelTimeStatistics phaseStatistics; // Phase statistics

  /** A Ellipticity object containing the ellipticity corrections */
  final Ellipticity ellipticityCorrections;

  final Ellipticity upEllipticity; // ellipticity correction for up-going P and S

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
   * Each phase is created all at once.
   *
   * @param PhaseGroup Phase group
   * @param auxGroup Auxiliary (complementary) phase group
   * @param isRegionalPhase True if the phase is flagged as regional
   * @param isDepth True if the phase is depth sensitive
   * @param canUse True if the phase may be used in an earthquake location
   * @param dis True if the phase should be down weighted
   * @param phaseStatistics Travel-time statistics object
   * @param Ellipticity ellipticityCorrections correction object
   * @param upEllipticity Ellipticity correction object for an associated up going branch if any
   */
  public TravelTimeFlags(
      String PhaseGroup,
      String auxGroup,
      boolean isRegionalPhase,
      boolean isDepth,
      boolean canUse,
      boolean dis,
      TravelTimeStatistics phaseStatistics,
      Ellipticity ellipticityCorrections,
      Ellipticity upEllipticity) {
    this.PhaseGroup = PhaseGroup;
    this.auxGroup = auxGroup;
    this.isRegionalPhase = isRegionalPhase;
    this.isDepth = isDepth;
    this.canUse = canUse;
    this.dis = dis;
    this.phaseStatistics = phaseStatistics;
    this.ellipticityCorrections = ellipticityCorrections;
    this.upEllipticity = upEllipticity;
  }
}
