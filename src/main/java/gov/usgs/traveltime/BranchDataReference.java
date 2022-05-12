package gov.usgs.traveltime;

import gov.usgs.traveltime.tables.BranchData;
import java.io.Serializable;
import java.util.Arrays;

/**
 * The BranchDataReference stores all reference (non-volatile) information associated with one
 * travel-time branch. Note that all data are normalized and transformed for internal use.
 *
 * @author Ray Buland
 */
public class BranchDataReference implements Serializable {
  /** A long containing the version id used in serialization */
  private static final long serialVersionUID = 1L;

  /** A String containing the branch phase code */
  private final String branchPhaseCode;

  /** An array of Strings containing the unique phase codes (oxymoron?) for this branch */
  private final String[] uniquePhaseCodes;

  /** A String containing the generic phase code for all branches in this segment */
  private final String genericPhaseCode;

  /** A String containing the phase code of an associated diffracted phase */
  private final String diffractedPhaseCode;

  /** A String containing the phase code of an associated add-on phase */
  private final String addOnPhaseCode;

  /** A String containing the bounce point phase code (type) */
  private final String reflectionPhaseCode;

  /** A String containing the Reflection or conversion on reflection phase code (type) */
  private final String convertedPhaseCode;

  /** A String containing the name of the model shell where the rays turn */
  private final String turningModelShellName;

  /** A boolean flag indicating whether this is an up-going branch */
  private final boolean isBranchUpGoing;

  /** A boolean flag indicating whether this branch is diffracted */
  private final boolean branchHasDiffraction;

  /** A boolean flag indicating whether this branch has an associated add-on phase */
  private final boolean branchHasAddOn;

  /**
   * A boolean flag indicating whether this phase is useless (phase is always in the coda of another
   * phase)
   *
   * <p>Originally, the useless flag was hard coded and putting it in the reference section made
   * sense. Now, the useless flag is set via the groups.txt file (chaff section) in the auxiliary
   * data. The reference useless flag has been retained to avoid messing with the Earth model files.
   * However, only the volatile (in BranchDataVolume) useless flag is still used, and this one is
   * not exposed
   */
  private final boolean isPhaseUseless;

  /**
   * An array of chars indicating the phase type for the initial correction, descending
   * (down-going), and ascending (up-coming)
   */
  private final char[] correctionPhaseType;

  /** An integer containing the sign of the ascending (up-coming) correction */
  private final int upGoingCorrectionSign;

  /** An integer containing the number of mantle traversals */
  private final int numMantleTraversals;

  /** An array of doubles containing the slowness range for this branch */
  private final double[] slownessRange;

  /** An array of doubles containing the distance range for this branch */
  private final double[] distanceRange;

  /** An array of doubles containing the radius range where the rays turn */
  private final double[] turningRadiusRange;

  /** A double containing the maximum distance in radians of an associated diffracted phase */
  private final double maxDiffractedDistance;

  /** An array of doubles containing the slowness grid for this branc */
  private final double[] slownessGrid;

  /** An array of doubles containing the tau for each point in the slowness grid */
  private final double[] tauGrid;

  /**
   * A two-dimensional array of doubles containing the basis function coefficients for each point in
   * the slowness grid
   */
  final double[][] basisCoefficients;

  /**
   * Function to return the branch phase code
   *
   * @return A String containing the branch phase code
   */
  public String getBranchPhaseCode() {
    return branchPhaseCode;
  }

  /**
   * Function to return the unique phase codes (oxymoron?) for this branch
   *
   * @return An array of String containing the unique phase codes for this branch
   */
  public String[] getUniquePhaseCodes() {
    return uniquePhaseCodes;
  }

  /**
   * Function to return the generic phase code for all branches in this segment
   *
   * @return A String containing the generic phase code for all branches in this segment
   */
  public String getGenericPhaseCode() {
    return genericPhaseCode;
  }

  /**
   * Function to return the phase code of an associated diffracted phase
   *
   * @return A String containing the phase code of an associated diffracted phase
   */
  public String getDiffractedPhaseCode() {
    return diffractedPhaseCode;
  }

  /**
   * Function to return the phase code of an associated add-on phase
   *
   * @return A String containing the phase code of an associated add-on phase
   */
  public String getAddOnPhaseCode() {
    return addOnPhaseCode;
  }

  /**
   * Function to return the bounce point phase code (type)
   *
   * @return A String containing the bounce point phase code (type)
   */
  public String getReflectionPhaseCode() {
    return reflectionPhaseCode;
  }

  /**
   * Function to return the reflection or conversion on reflection phase code (type)
   *
   * @return A String containing the reflection or conversion on reflection phase code (type)
   */
  public String getConvertedPhaseCode() {
    return convertedPhaseCode;
  }

  /**
   * Function to return the name of the model shell where the rays turn
   *
   * @return A String containing the name of the model shell where the rays turn
   */
  public String getTurningModelShellName() {
    return turningModelShellName;
  }

  /**
   * Function to return whether this is an up-going branch
   *
   * @return A boolean flag indicating whether this is an up-going branc
   */
  public boolean getIsBranchUpGoing() {
    return isBranchUpGoing;
  }

  /**
   * Function to return whether this branch is diffracted
   *
   * @return A boolean flag indicating whether this branch is diffracted
   */
  public boolean getBranchHasDiffraction() {
    return branchHasDiffraction;
  }

  /**
   * Function to return whether this branch has an associated add-on phase
   *
   * @return A boolean flag indicating whether this branch has an associated add-on phase
   */
  public boolean getBranchHasAddOn() {
    return branchHasAddOn;
  }

  /**
   * Function to return the phase type for the initial correction, descending (down-going), and
   * ascending (up-coming)
   *
   * @return An array of chars indicating the phase type for the initial correction, descending
   *     (down-going), and ascending (up-coming)
   */
  public char[] getCorrectionPhaseType() {
    return correctionPhaseType;
  }

  /**
   * Function to return the sign of the ascending (up-coming) correction
   *
   * @return An integer containing the sign of the ascending (up-coming) correction
   */
  public int getUpGoingCorrectionSign() {
    return upGoingCorrectionSign;
  }

  /**
   * Function to return the number of mantle traversals
   *
   * @return An integer containing the number of mantle traversals
   */
  public int getNumMantleTraversals() {
    return numMantleTraversals;
  }

  /**
   * Function to return the slowness range for this branch
   *
   * @return An array of doubles containing the slowness range for this branch
   */
  public double[] getSlownessRange() {
    return slownessRange;
  }

  /**
   * Function to return the distance range for this branch
   *
   * @return An array of doubles containing the distance range for this branch
   */
  public double[] getDistanceRange() {
    return distanceRange;
  }

  /**
   * Function to return the radius range where the rays turn
   *
   * @return An array of doubles containing the radius range where the rays turn
   */
  public double[] getTurningRadiusRange() {
    return turningRadiusRange;
  }

  /**
   * Function to return the maximum distance of an associated diffracted phase
   *
   * @return A double containing the maximum distance in radians of an associated diffracted phase
   */
  public double getMaxDiffractedDistance() {
    return maxDiffractedDistance;
  }

  /**
   * Function to return the slowness grid for this branch
   *
   * @return An array of doubles containing the slowness grid for this branch
   */
  public double[] getSlownessGrid() {
    return slownessGrid;
  }

  /**
   * Function to return the tau for each point in the slowness grid
   *
   * @return An array of doubles containing the tau for each point in the slowness grid
   */
  public double[] getTauGrid() {
    return tauGrid;
  }

  /**
   * Function to return the basis function coefficients for each point in the slowness grid
   *
   * @return A two-dimensional array of doubles containing the basis function coefficients for each
   *     point in the slowness grid
   */
  public double[][] getBasisCoefficients() {
    return basisCoefficients;
  }

  /**
   * BranchDataReference constructor, loads data from the FORTRAN file reader for one branch. The
   * file data should have already been loaded from the *.hed and *.tbl files.
   *
   * @param inputBranchData A ReadTau object holding the FORTRAN branch input data source.
   * @param branchIndex An integer containing the FORTRAN branch index
   * @param segmentIndex An integer containing the FORTRAN segment index
   * @param segmentPhaseCode A String containing the segment phase code for this branch
   * @param extraPhaseList An ExtraPhases object holding the list of extra phases
   * @param auxTTReference An AuxiliaryTTReference object containing auxiliary data augmenting the
   *     basic travel-times.
   */
  public BranchDataReference(
      ReadTau inputBranchData,
      int branchIndex,
      int segmentIndex,
      String segmentPhaseCode,
      ExtraPhases extraPhaseList,
      AuxiliaryTTReference auxTTReference) {

    // Do phase code.
    branchPhaseCode = inputBranchData.phaseCode[branchIndex];

    // Do segment summary information.
    genericPhaseCode = segmentPhaseCode;
    if (inputBranchData.typeSeg[segmentIndex][1] <= 0) {
      isBranchUpGoing = true;
    } else {
      isBranchUpGoing = false;
    }

    // The three types are: 1) initial, 2) down-going, and 3) up-coming.
    // For example, sP would be S, P, P, while ScP would be S, S, P.
    correctionPhaseType = new char[3];
    if (!isBranchUpGoing) {
      // For the normal case, set all three flags.
      for (int j = 0; j < 3; j++) {
        if (Math.abs(inputBranchData.typeSeg[segmentIndex][j]) == 1) {
          correctionPhaseType[j] = 'P';
        } else if (Math.abs(inputBranchData.typeSeg[segmentIndex][j]) == 2) {
          correctionPhaseType[j] = 'S';
        } else {
          correctionPhaseType[j] = ' ';
        }
      }
    } else {
      // For up-going phases, it's convenient to make all three flags
      // the same.
      if (Math.abs(inputBranchData.typeSeg[segmentIndex][0]) == 1) {
        correctionPhaseType[0] = 'P';
      } else {
        correctionPhaseType[0] = 'S';
      }

      correctionPhaseType[1] = correctionPhaseType[0];
      correctionPhaseType[2] = correctionPhaseType[0];
    }

    // We need to know whether to add or subtract the up-going correction.
    // For example, the up-going correction would be subtracted for P, but
    // added for pP.
    if (inputBranchData.typeSeg[segmentIndex][0] > 0) {
      upGoingCorrectionSign = 1;
    } else {
      upGoingCorrectionSign = -1;
    }

    // We might need to add or subtract the up-going correction more than
    // once.
    numMantleTraversals = (int) Math.round(inputBranchData.countSeg[segmentIndex][0]);

    // Do branch summary information.
    slownessRange = new double[2];
    distanceRange = new double[2];
    for (int j = 0; j < 2; j++) {
      slownessRange[j] = inputBranchData.pBrn[branchIndex][j];
      distanceRange[j] = inputBranchData.xBrn[branchIndex][j];
    }

    // Set up the branch specification.
    int start = inputBranchData.indexBrn[branchIndex][0] - 1;
    int end = inputBranchData.indexBrn[branchIndex][1];
    slownessGrid = Arrays.copyOfRange(inputBranchData.pSpec, start, end);
    tauGrid = Arrays.copyOfRange(inputBranchData.tauSpec, start, end);
    basisCoefficients = new double[5][];

    for (int k = 0; k < 5; k++) {
      basisCoefficients[k] = Arrays.copyOfRange(inputBranchData.basisSpec[k], start, end);
    }

    /*
     * This section draws only on preset information within the
     * Java code (i.e., not the model).  Although it is the same for
     * any constructor, it must be duplicated because the variables
     * being set are final.
     */

    // Set up the unique code (for plotting).
    uniquePhaseCodes = new String[2];
    uniquePhaseCodes[0] = TauUtilities.makeUniquePhaseCode(branchPhaseCode);
    if (branchPhaseCode.contains("ab")) {
      uniquePhaseCodes[1] = uniquePhaseCodes[0].replace("ab", "bc");
    } else {
      uniquePhaseCodes[1] = null;
    }

    // Set the useless phase flag.
    isPhaseUseless = auxTTReference.isUselessPhase(branchPhaseCode);

    if (branchPhaseCode != null) {
      if (branchPhaseCode.equals("PnPn")) {
        System.out.println("Got PnPn (BranchDataReference 1)!");
        System.out.println();
      }
    }

    // Set up diffracted and add-on phases.
    if (!isBranchUpGoing) {
      branchHasDiffraction = extraPhaseList.branchHasDiffraction(branchPhaseCode);
      branchHasAddOn = extraPhaseList.createAddOnPhase(branchPhaseCode, distanceRange[1]);
    } else {
      branchHasDiffraction = false;
      branchHasAddOn = false;
    }

    // Handle a diffracted branch.
    if (branchHasDiffraction) {
      diffractedPhaseCode = extraPhaseList.getDiffractedPhaseCode();
      maxDiffractedDistance = extraPhaseList.getDiffractedPhaseLimit();
    } else {
      diffractedPhaseCode = "";
      maxDiffractedDistance = 0d;
    }

    // Handle an add-on phase.
    if (branchHasAddOn) {
      addOnPhaseCode = extraPhaseList.getAddOnPhaseCode();
      // Add-on flags can be different than the base phase.
    } else {
      addOnPhaseCode = "";
    }

    // Set up the type of surface reflection, if any.
    if (upGoingCorrectionSign > 0 && !isBranchUpGoing) {
      if (correctionPhaseType[0] == 'P') {
        if (correctionPhaseType[1] == 'P') {
          reflectionPhaseCode = "pP";
        } else {
          reflectionPhaseCode = "pS";
        }
      } else {
        if (correctionPhaseType[1] == 'P') {
          reflectionPhaseCode = "sP";
        } else {
          reflectionPhaseCode = "sS";
        }
      }

      convertedPhaseCode = reflectionPhaseCode.toUpperCase();
    } else if (numMantleTraversals > 1) {
      if (correctionPhaseType[1] == 'P') {
        if (correctionPhaseType[2] == 'P') {
          reflectionPhaseCode = "PP";
        } else {
          reflectionPhaseCode = "PS";
        }
      } else {
        if (correctionPhaseType[2] == 'P') {
          reflectionPhaseCode = "SP";
        } else {
          reflectionPhaseCode = "SS";
        }
      }

      convertedPhaseCode = reflectionPhaseCode.toUpperCase();
    } else {
      reflectionPhaseCode = null;
      convertedPhaseCode = null;
    }

    // We don't get shell information from the Fortran files.
    turningModelShellName = null;
    turningRadiusRange = null;
  }

  /**
   * BranchDataReference constructor, load data from the tau-p table generation branch data into
   * this class supporting the actual travel-time generation.
   *
   * @param inputBranchData A BranchData object containing travel-time table generation branch data
   * @param branchIndex An integer containing the FORTRAN branch index
   * @param extraPhaseListAn An ExtraPhases object holding the list of extra phases
   * @param auxTTReference An AuxiliaryTTReference object containing auxiliary data augmenting the
   *     basic travel-times.
   */
  public BranchDataReference(
      BranchData inputBranchData,
      int branchIndex,
      ExtraPhases extraPhaseList,
      AuxiliaryTTReference auxTTReference) {

    // Do phase code.
    branchPhaseCode = inputBranchData.getPhaseCode();

    // Do segment summary information.
    genericPhaseCode = inputBranchData.getPhaseSegmentCode();
    isBranchUpGoing = inputBranchData.getIsBranchUpGoing();

    // The three types are: 1) initial, 2) down-going, and 3) up-coming.
    // For example, sP would be S, P, P, while ScP would be S, S, P.
    correctionPhaseType = Arrays.copyOf(inputBranchData.getRaySegmentPhaseTypes(), 3);

    // We need to know whether to add or subtract the up-going correction.
    // For example, the up-going correction would be subtracted for P, but
    // added for pP.
    upGoingCorrectionSign = inputBranchData.getUpGoingDepthCorrectionSign();

    // We might need to add or subtract the up-going correction more than
    // once.
    numMantleTraversals = inputBranchData.getNumMantleTraversals();

    // Do branch summary information.
    slownessRange = Arrays.copyOf(inputBranchData.getSlownessRange(), 2);
    distanceRange = Arrays.copyOf(inputBranchData.getDistanceRange(), 2);

    // Set up the branch specification.
    slownessGrid =
        Arrays.copyOf(
            inputBranchData.getRayParameters(), inputBranchData.getRayParameters().length);
    tauGrid = Arrays.copyOf(inputBranchData.getTauValues(), inputBranchData.getTauValues().length);
    basisCoefficients = new double[5][];
    for (int k = 0; k < 5; k++) {
      basisCoefficients[k] =
          Arrays.copyOf(
              inputBranchData.getBasisCoefficientRow(k),
              inputBranchData.getBasisCoefficientRow(k).length);
    }

    /*
     * This section draws only on preset information within the
     * Java code (i.e., not the model).  Although it is the same for
     * any constructor, it must be duplicated because the variables
     * being set are final.
     */

    // Set up the unique code (for plotting).
    uniquePhaseCodes = new String[2];
    uniquePhaseCodes[0] = TauUtilities.makeUniquePhaseCode(branchPhaseCode);
    if (branchPhaseCode.contains("ab")) {
      uniquePhaseCodes[1] = uniquePhaseCodes[0].replace("ab", "bc");
    } else {
      uniquePhaseCodes[1] = null;
    }

    // Set the useless phase flag.
    isPhaseUseless = auxTTReference.isUselessPhase(branchPhaseCode);

    if (branchPhaseCode != null) {
      if (branchPhaseCode.equals("PnPn")) {
        System.out.println("Got PnPn (BranchDataReference 2)!");
        System.out.println();
      }
    }

    // Set up diffracted and add-on phases.
    if (!isBranchUpGoing) {
      branchHasDiffraction = extraPhaseList.branchHasDiffraction(branchPhaseCode);
      branchHasAddOn = extraPhaseList.createAddOnPhase(branchPhaseCode, distanceRange[1]);
    } else {
      branchHasDiffraction = false;
      branchHasAddOn = false;
    }

    // Handle a diffracted branch.
    if (branchHasDiffraction) {
      diffractedPhaseCode = extraPhaseList.getDiffractedPhaseCode();
      maxDiffractedDistance = extraPhaseList.getDiffractedPhaseLimit();
    } else {
      diffractedPhaseCode = "";
      maxDiffractedDistance = 0d;
    }

    // Handle an add-on phase.
    if (branchHasAddOn) {
      addOnPhaseCode = extraPhaseList.getAddOnPhaseCode();
      // Add-on flags can be different than the base phase.
    } else {
      addOnPhaseCode = "";
    }

    // Set up the type of surface reflection, if any.
    if (upGoingCorrectionSign > 0 && !isBranchUpGoing) {
      if (correctionPhaseType[0] == 'P') {
        if (correctionPhaseType[1] == 'P') {
          reflectionPhaseCode = "pP";
        } else {
          reflectionPhaseCode = "pS";
        }
      } else {
        if (correctionPhaseType[1] == 'P') {
          reflectionPhaseCode = "sP";
        } else {
          reflectionPhaseCode = "sS";
        }
      }

      convertedPhaseCode = reflectionPhaseCode.toUpperCase();
    } else if (numMantleTraversals > 1) {
      if (correctionPhaseType[1] == 'P') {
        if (correctionPhaseType[2] == 'P') {
          reflectionPhaseCode = "PP";
        } else {
          reflectionPhaseCode = "PS";
        }
      } else {
        if (correctionPhaseType[2] == 'P') {
          reflectionPhaseCode = "SP";
        } else {
          reflectionPhaseCode = "SS";
        }
      }

      convertedPhaseCode = reflectionPhaseCode.toUpperCase();
    } else {
      reflectionPhaseCode = null;
      convertedPhaseCode = null;
    }

    // Set up the shell information.  Note that this the shell
    // information can be handy for debugging new Earth models.
    turningModelShellName = inputBranchData.getTurnShellName();
    double[] temp = inputBranchData.getRadiusTurningRange();
    if (temp != null) {
      turningRadiusRange = Arrays.copyOf(temp, 2);
    } else {
      turningRadiusRange = null;
    }
  }

  /**
   * Function to get the type of the phase arriving at the station.
   *
   * @return 'P' or 'S' depending on the type of the phase when it arrives at the station.
   */
  public char getArrivalType() {
    if (isBranchUpGoing) {
      return correctionPhaseType[0];
    } else {
      return correctionPhaseType[2];
    }
  }

  /**
   * Function to [rint out branch information for debugging purposes.
   *
   * @param full A boolean flag, if true print the detailed branch specification as well
   */
  public void dumpBrn(boolean full) {
    if (isBranchUpGoing) {
      System.out.format("\n          phase = %s up  %s  ", branchPhaseCode, uniquePhaseCodes[0]);

      if (branchHasDiffraction) {
        System.out.format("diff = %s  ", diffractedPhaseCode);
      }

      if (branchHasAddOn) {
        System.out.format("add-on = %s  ", addOnPhaseCode);
      }

      System.out.format("isPhaseUsisUselesseless = %b\n", isPhaseUseless);
      System.out.format(
          "Segment: code = %s  type = %c        sign = %2d" + "  count = %d\n",
          genericPhaseCode, correctionPhaseType[0], upGoingCorrectionSign, numMantleTraversals);
    } else {
      System.out.format("\n          phase = %s  %s  ", branchPhaseCode, uniquePhaseCodes[0]);

      if (branchHasDiffraction) {
        System.out.format("diff = %s  ", diffractedPhaseCode);
      }

      if (branchHasAddOn) {
        System.out.format("add-on = %s  ", addOnPhaseCode);
      }

      System.out.format("isUseless = %b\n", isPhaseUseless);
      System.out.format(
          "Segment: code = %s  type = %c, %c, %c  " + "sign = %2d  count = %d",
          genericPhaseCode,
          correctionPhaseType[0],
          correctionPhaseType[1],
          correctionPhaseType[2],
          upGoingCorrectionSign,
          numMantleTraversals);

      if (reflectionPhaseCode == null) {
        System.out.println();
      } else {
        System.out.println("  refl = " + reflectionPhaseCode + " " + convertedPhaseCode);
      }
    }

    System.out.format(
        "Branch: pRange = %8.6f - %8.6f  xRange = %6.2f - %6.2f\n",
        slownessRange[0],
        slownessRange[1],
        Math.toDegrees(distanceRange[0]),
        Math.toDegrees(distanceRange[1]));

    if (branchHasDiffraction)
      System.out.format(
          "        maxDiffractedDistance = %6.2f\n", Math.toDegrees(maxDiffractedDistance));

    if (turningModelShellName != null) {
      System.out.format(
          "Shell: %7.2f-%7.2f %s\n",
          turningRadiusRange[0], turningRadiusRange[1], turningModelShellName);
    }

    if (full) {
      System.out.println(
          "\n         p        tau                 " + "basis function coefficients");

      for (int j = 0; j < slownessGrid.length; j++) {
        System.out.format(
            "%3d: %8.6f  %8.6f  %9.2e  %9.2e  %9.2e  %9.2e  " + "%9.2e\n",
            j,
            slownessGrid[j],
            tauGrid[j],
            basisCoefficients[0][j],
            basisCoefficients[1][j],
            basisCoefficients[2][j],
            basisCoefficients[3][j],
            basisCoefficients[4][j]);
      }
    }
  }
}
