package gov.usgs.traveltime;

/**
 * Organize all the auxiliary travel-time information by phase.
 *
 * @author Ray Buland
 */
public class TravelTimeFlags {
  final String PhaseGroup; // Phase group
  final String auxGroup; // Auxiliary phase group
  final boolean isRegional; // True if phase is regional
  final boolean isDepth; // True if phase is depth sensitive
  final boolean canUse; // True if phase can be used in a location
  final boolean dis; // True if phase is to be down weighted
  final TravelTimeStatistics TravelTimeStatistics; // Phase statistics
  final Ellipticity Ellipticity; // Ellipticity correction
  final Ellipticity upEllipticity; // Ellipticity correction for up-going P and S

  /**
   * Each phase is created all at once.
   *
   * @param PhaseGroup Phase group
   * @param auxGroup Auxiliary (complementary) phase group
   * @param isRegional True if the phase is flagged as regional
   * @param isDepth True if the phase is depth sensitive
   * @param canUse True if the phase may be used in an earthquake location
   * @param dis True if the phase should be down weighted
   * @param TravelTimeStatistics Travel-time statistics object
   * @param Ellipticity Ellipticity correction object
   * @param upEllipticity Ellipticity correction object for an associated up going branch if any
   */
  public TravelTimeFlags(
      String PhaseGroup,
      String auxGroup,
      boolean isRegional,
      boolean isDepth,
      boolean canUse,
      boolean dis,
      TravelTimeStatistics TravelTimeStatistics,
      Ellipticity Ellipticity,
      Ellipticity upEllipticity) {
    this.PhaseGroup = PhaseGroup;
    this.auxGroup = auxGroup;
    this.isRegional = isRegional;
    this.isDepth = isDepth;
    this.canUse = canUse;
    this.dis = dis;
    this.TravelTimeStatistics = TravelTimeStatistics;
    this.Ellipticity = Ellipticity;
    this.upEllipticity = upEllipticity;
  }
}
