package gov.usgs.traveltime;

/**
 * The ExtraPhases class provides a programmatic way of getting information about diffracted and
 * add-on phases.
 *
 * @author Ray Buland
 */
public class ExtraPhases {
  /** A double containing the limit of the mantle in radians */
  private final double mantleLimit = Math.toRadians(132d);

  /** A double containing the limit of the core in radians */
  private final double coreLimit = Math.toRadians(164d);

  /** A list of Strings containing the possible diffracted phase codes */
  private final String[] diffractionPhaseCodes = {
    "P", "S", "pP", "sP", "pS", "sS", "PKPab", "pPKPab", "sPKPab"
  };

  /** A list of boolean flags indicating that the corresponding diffracted phase code was used */
  private final boolean[] diffractionCodeUsed = {
    false, false, false, false, false, false, false, false, false
  };

  /** A list of Strings containing the possible add-on phase base phase codes */
  private final String[] baseAddOnPhaseCodes = {"PKPdf", "pP", "Sn", "SKiKP", "SKPdf"};

  /** A list of Strings containing the possible add-on phase codes */
  private final String[] addOnPhaseCodes = {"PKPpre", "pwP", "Lg", "LR", "LR"};

  /** A list of boolean flags indicating that the corresponding add-on phase code was used */
  private final boolean[] addOnCodeUsed = {false, false, false, false, false};

  /** A String containing the diffracted phase code */
  private String diffractedPhaseCode;

  /**
   * A double containing the limit (maximum distance) that the diffracted phase can be observed in
   * raidians
   */
  private double diffractedPhaseLimit;

  /** A String containing the phase code of the associated add-on phase */
  private String addOnPhaseCode;

  /** A AuxiliaryTTReference object holding the model independent travel time auxiliary data */
  private final AuxiliaryTTReference auxTTReference;

  /**
   * Function to return the phase code of the associated diffracted branch, if there is one.
   *
   * @return A String containing the phase code of the associated diffracted branch.
   */
  public String getDiffractedPhaseCode() {
    return diffractedPhaseCode;
  }

  /**
   * Function to return the maximum observable distance of the diffracted phase
   *
   * @return A double holding the maximum distance in radians that the diffracted phase can be
   *     observed.
   */
  public double getDiffractedPhaseLimit() {
    return diffractedPhaseLimit;
  }

  /**
   * Function to return the phase code of the add-on phase, if there is one
   *
   * @return A String containing the phase code of the associated add-on phase
   */
  public String getAddOnPhaseCode() {
    return addOnPhaseCode;
  }

  /**
   * ExtraPhases Constructor, sets the auxiliary phase information.
   *
   * @param auxTTReference A AuxiliaryTTReference object holding the model independent travel time
   *     auxiliary data
   */
  public ExtraPhases(AuxiliaryTTReference auxTTReference) {
    this.auxTTReference = auxTTReference;
  }

  /**
   * Function to determine if the phase should be diffracte, given a phase code. Note that this
   * determination depends on the order that the phases are presented because there are multiple
   * mantle P and S branches, but only the deepest is diffracted. For the same reason, the up-going
   * branch must not be presented to this method.
   *
   * @param phaseCode A String containing the phase code to check
   * @return A boolean flag, true if there is a diffracted branch
   */
  public boolean branchHasDiffraction(String phaseCode) {
    for (int j = 0; j < diffractionPhaseCodes.length; j++) {
      if (phaseCode.equals(diffractionPhaseCodes[j])) {
        if (!diffractionCodeUsed[j]) {
          diffractionCodeUsed[j] = true;
          diffractedPhaseCode = TauUtilities.createSegmentCode(diffractionPhaseCodes[j]) + "dif";

          if (phaseCode.contains("ab")) {
            diffractedPhaseLimit = coreLimit;
          } else {
            diffractedPhaseLimit = mantleLimit;
          }

          return true;
        }

        return false;
      }
    }

    return false;
  }

  /**
   * Function to create an add-on phase. Add-on phases are made up out of whole cloth when a base
   * phase is processed for arrivals.
   *
   * @param phaseCode A String containing the phase code to use
   * @param minimumDistance A double containing the minimum distance for this phase in radians
   * @return A boolean flag, true if this phase code has an add-on associated with it
   */
  public boolean createAddOnPhase(String phaseCode, double minimumDistance) {
    TravelTimeFlags flags = auxTTReference.findPhaseFlags(phaseCode);

    // Check for phases missing from the groups file.
    if (flags == null) {
      System.out.println("Warning: " + phaseCode + " is not in the groups.txt file!");
      return false;
    }

    if (flags.getPhaseStatistics() == null) {
      return false;
    }

    // Otherwise, see if an add on phase is listed.
    for (int j = 0; j < baseAddOnPhaseCodes.length; j++) {
      if (phaseCode.equals(baseAddOnPhaseCodes[j])) {
        if (!addOnCodeUsed[j]) {
          if (!baseAddOnPhaseCodes[j].equals("Sn")) {
            // In the general case, we want the first branch.
            addOnCodeUsed[j] = true;
            addOnPhaseCode = addOnPhaseCodes[j];
            return true;
          } else {
            if (minimumDistance < TauUtilities.SNMINIMUMDISTANCE) {
              // For Sn we want the second branch.
              addOnCodeUsed[j] = true;
              addOnPhaseCode = addOnPhaseCodes[j];
              return true;
            } else {
              return false;
            }
          }
        }

        return false;
      }
    }

    return false;
  }
}
