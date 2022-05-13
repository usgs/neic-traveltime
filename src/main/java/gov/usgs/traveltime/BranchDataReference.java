package gov.usgs.traveltime;

import gov.usgs.traveltime.tables.BranchData;
import java.io.Serializable;
import java.util.Arrays;

/**
 * Store all non-volatile information associated with one travel-time branch. Note that all data are
 * normalized and transformed for internal use.
 *
 * @author Ray Buland
 */
public class BranchDataReference implements Serializable {
  private static final long serialVersionUID = 1L;
  final String phCode; // Branch phase code
  final String[] uniqueCode; // Unique phase codes (oxymoron?)
  final String phSeg; // Generic phase code for all branches in this segment
  final String phDiff; // Phase code of an associated diffracted phase
  final String phAddOn; // Phase code of an associated add-on phase
  final String phRefl; // Type of bounce point
  final String convRefl; // Reflection or conversion on reflection
  final String turnShell; // Name of the model shell where the rays turn
  final boolean isUpGoing; // True if this is an up-going branch
  final boolean hasDiff; // True if this branch is also diffracted
  final boolean hasAddOn; // True if this branch has an associated add-on phase
  final boolean isUseless; // True if this phase is always in the coda of another phase
  /*
   * Originally, the useless flag was hard coded and putting it in the reference section
   * made sense.  Now, the useless flag is set via the groups.txt file (chaff section)
   * in the auxiliary data.  The reference useless flag has been retained to avoid
   * messing with the Earth model files.  However, only the volatile useless flag is still
   * used.
   */
  final char[] typeSeg; // Phase type for correction, descending, ascending
  final int signSeg; // Sign of the up-going correction
  final int countSeg; // Number of mantle traversals
  final double[] pRange; // Slowness range for this branch
  final double[] xRange; // Distance range for this branch
  final double[] rRange; // Radius range where the rays turn
  final double xDiff; // Maximum distance of an associated diffracted phase
  final double[] pBrn; // Slowness grid for this branch
  final double[] tauBrn; // Tau for each grid point
  final double[][] basis; // Basis function coefficients for each grid point

  /**
   * Load data from the FORTRAN file reader for one branch. The file data should have already been
   * loaded from the *.hed and *.tbl files.
   *
   * @param in Branch input data source.
   * @param indexBrn FORTRAN branch index
   * @param indexSeg FORTRAN segment index
   * @param segCode Segment code for this branch
   * @param extra List of extra phases
   * @param auxtt Auxiliary data source
   */
  public BranchDataReference(
      ReadTau in,
      int indexBrn,
      int indexSeg,
      String segCode,
      ExtraPhases extra,
      AuxiliaryTTReference auxtt) {

    // Do phase code.
    phCode = in.phCode[indexBrn];

    // Do segment summary information.
    phSeg = segCode;
    if (in.typeSeg[indexSeg][1] <= 0) isUpGoing = true;
    else isUpGoing = false;
    // The three types are: 1) initial, 2) down-going, and 3) up-coming.
    // For example, sP would be S, P, P, while ScP would be S, S, P.
    typeSeg = new char[3];
    if (!isUpGoing) {
      // For the normal case, set all three flags.
      for (int j = 0; j < 3; j++) {
        if (Math.abs(in.typeSeg[indexSeg][j]) == 1) typeSeg[j] = 'P';
        else if (Math.abs(in.typeSeg[indexSeg][j]) == 2) typeSeg[j] = 'S';
        else typeSeg[j] = ' ';
      }
    } else {
      // For up-going phases, it's convenient to make all three flags
      // the same.
      if (Math.abs(in.typeSeg[indexSeg][0]) == 1) typeSeg[0] = 'P';
      else typeSeg[0] = 'S';
      typeSeg[1] = typeSeg[0];
      typeSeg[2] = typeSeg[0];
    }
    // We need to know whether to add or subtract the up-going correction.
    // For example, the up-going correction would be subtracted for P, but
    // added for pP.
    if (in.typeSeg[indexSeg][0] > 0) signSeg = 1;
    else signSeg = -1;
    // We might need to add or subtract the up-going correction more than
    // once.
    countSeg = (int) Math.round(in.countSeg[indexSeg][0]);

    // Do branch summary information.
    pRange = new double[2];
    xRange = new double[2];
    for (int j = 0; j < 2; j++) {
      pRange[j] = in.pBrn[indexBrn][j];
      xRange[j] = in.xBrn[indexBrn][j];
    }

    // Set up the branch specification.
    int start = in.indexBrn[indexBrn][0] - 1;
    int end = in.indexBrn[indexBrn][1];
    pBrn = Arrays.copyOfRange(in.pSpec, start, end);
    tauBrn = Arrays.copyOfRange(in.tauSpec, start, end);
    basis = new double[5][];
    for (int k = 0; k < 5; k++) {
      basis[k] = Arrays.copyOfRange(in.basisSpec[k], start, end);
    }

    /*
     * This section draws only on preset information within the
     * Java code (i.e., not the model).  Although it is the same for
     * any constructor, it must be duplicated because the variables
     * being set are final.
     */

    // Set up the unique code (for plotting).
    uniqueCode = new String[2];
    uniqueCode[0] = TauUtilities.uniqueCode(phCode);
    if (phCode.contains("ab")) {
      uniqueCode[1] = uniqueCode[0].replace("ab", "bc");
    } else {
      uniqueCode[1] = null;
    }

    // Set the useless phase flag.
    isUseless = auxtt.isChaff(phCode);

    if (phCode != null) {
      if (phCode.equals("PnPn")) {
        System.out.println("Got PnPn (BranchDataReference 1)!");
        System.out.println();
      }
    }

    // Set up diffracted and add-on phases.
    if (!isUpGoing) {
      hasDiff = extra.hasDiff(phCode);
      hasAddOn = extra.hasAddOn(phCode, xRange[1]);
    } else {
      hasDiff = false;
      hasAddOn = false;
    }

    // Handle a diffracted branch.
    if (hasDiff) {
      phDiff = extra.getPhDiff();
      xDiff = extra.getPhLim();
    } else {
      phDiff = "";
      xDiff = 0d;
    }

    // Handle an add-on phase.
    if (hasAddOn) {
      phAddOn = extra.getPhAddOn();
      // Add-on flags can be different than the base phase.
    } else {
      phAddOn = "";
    }

    // Set up the type of surface reflection, if any.
    if (signSeg > 0 && !isUpGoing) {
      if (typeSeg[0] == 'P') {
        if (typeSeg[1] == 'P') phRefl = "pP";
        else phRefl = "pS";
      } else {
        if (typeSeg[1] == 'P') phRefl = "sP";
        else phRefl = "sS";
      }
      convRefl = phRefl.toUpperCase();
    } else if (countSeg > 1) {
      if (typeSeg[1] == 'P') {
        if (typeSeg[2] == 'P') phRefl = "PP";
        else phRefl = "PS";
      } else {
        if (typeSeg[2] == 'P') phRefl = "SP";
        else phRefl = "SS";
      }
      convRefl = phRefl.toUpperCase();
    } else {
      phRefl = null;
      convRefl = null;
    }

    // We don't get shell information from the Fortran files.
    turnShell = null;
    rRange = null;
  }

  /**
   * Load data from the tau-p table generation branch data into this class supporting the actual
   * travel-time generation.
   *
   * @param brnData Travel-time table generation branch data
   * @param indexBrn FORTRAN branch index
   * @param extra List of extra phases
   * @param auxtt Auxiliary data source
   */
  public BranchDataReference(
      BranchData brnData, int indexBrn, ExtraPhases extra, AuxiliaryTTReference auxtt) {

    // Do phase code.
    phCode = brnData.getPhaseCode();

    // Do segment summary information.
    phSeg = brnData.getPhaseSegmentCode();
    isUpGoing = brnData.getIsBranchUpGoing();
    // The three types are: 1) initial, 2) down-going, and 3) up-coming.
    // For example, sP would be S, P, P, while ScP would be S, S, P.
    typeSeg = Arrays.copyOf(brnData.getRaySegmentPhaseTypes(), 3);
    // We need to know whether to add or subtract the up-going correction.
    // For example, the up-going correction would be subtracted for P, but
    // added for pP.
    signSeg = brnData.getUpGoingDepthCorrectionSign();
    // We might need to add or subtract the up-going correction more than
    // once.
    countSeg = brnData.getNumMantleTraversals();

    // Do branch summary information.
    pRange = Arrays.copyOf(brnData.getSlownessRange(), 2);
    xRange = Arrays.copyOf(brnData.getDistanceRange(), 2);

    // Set up the branch specification.
    pBrn = Arrays.copyOf(brnData.getRayParameters(), brnData.getRayParameters().length);
    tauBrn = Arrays.copyOf(brnData.getTauValues(), brnData.getTauValues().length);
    basis = new double[5][];
    for (int k = 0; k < 5; k++) {
      basis[k] =
          Arrays.copyOf(
              brnData.getBasisCoefficientRow(k), brnData.getBasisCoefficientRow(k).length);
    }

    /*
     * This section draws only on preset information within the
     * Java code (i.e., not the model).  Although it is the same for
     * any constructor, it must be duplicated because the variables
     * being set are final.
     */

    // Set up the unique code (for plotting).
    uniqueCode = new String[2];
    uniqueCode[0] = TauUtilities.uniqueCode(phCode);
    if (phCode.contains("ab")) {
      uniqueCode[1] = uniqueCode[0].replace("ab", "bc");
    } else {
      uniqueCode[1] = null;
    }

    // Set the useless phase flag.
    isUseless = auxtt.isChaff(phCode);

    if (phCode != null) {
      if (phCode.equals("PnPn")) {
        System.out.println("Got PnPn (BranchDataReference 2)!");
        System.out.println();
      }
    }

    // Set up diffracted and add-on phases.
    if (!isUpGoing) {
      hasDiff = extra.hasDiff(phCode);
      hasAddOn = extra.hasAddOn(phCode, xRange[1]);
    } else {
      hasDiff = false;
      hasAddOn = false;
    }

    // Handle a diffracted branch.
    if (hasDiff) {
      phDiff = extra.getPhDiff();
      xDiff = extra.getPhLim();
    } else {
      phDiff = "";
      xDiff = 0d;
    }

    // Handle an add-on phase.
    if (hasAddOn) {
      phAddOn = extra.getPhAddOn();
      // Add-on flags can be different than the base phase.
    } else {
      phAddOn = "";
    }

    // Set up the type of surface reflection, if any.
    if (signSeg > 0 && !isUpGoing) {
      if (typeSeg[0] == 'P') {
        if (typeSeg[1] == 'P') phRefl = "pP";
        else phRefl = "pS";
      } else {
        if (typeSeg[1] == 'P') phRefl = "sP";
        else phRefl = "sS";
      }
      convRefl = phRefl.toUpperCase();
    } else if (countSeg > 1) {
      if (typeSeg[1] == 'P') {
        if (typeSeg[2] == 'P') phRefl = "PP";
        else phRefl = "PS";
      } else {
        if (typeSeg[2] == 'P') phRefl = "SP";
        else phRefl = "SS";
      }
      convRefl = phRefl.toUpperCase();
    } else {
      phRefl = null;
      convRefl = null;
    }

    // Set up the shell information.  Note that this the shell
    // information can be handy for debugging new Earth models.
    turnShell = brnData.getTurnShellName();
    double[] temp;
    temp = brnData.getRadiusTurningRange();
    if (temp != null) {
      rRange = Arrays.copyOf(temp, 2);
    } else {
      rRange = null;
    }
  }

  /**
   * get the branch segment code.
   *
   * @return Branch segment code
   */
  public String getPhSeg() {
    return phSeg;
  }

  /**
   * Get the type of the phase arriving at the station.
   *
   * @return 'P' or 'S' depending on the type of the phase when it arrives at the station.
   */
  public char getArrivalType() {
    if (isUpGoing) return typeSeg[0];
    else return typeSeg[2];
  }

  /**
   * Print out branch information for debugging purposes.
   *
   * @param full If true print the detailed branch specification as well
   */
  public void dumpBrn(boolean full) {
    if (isUpGoing) {
      System.out.format("\n          phase = %s up  %s  ", phCode, uniqueCode[0]);
      if (hasDiff) System.out.format("diff = %s  ", phDiff);
      if (hasAddOn) System.out.format("add-on = %s  ", phAddOn);
      System.out.format("isUseless = %b\n", isUseless);
      System.out.format(
          "Segment: code = %s  type = %c        sign = %2d" + "  count = %d\n",
          phSeg, typeSeg[0], signSeg, countSeg);
    } else {
      System.out.format("\n          phase = %s  %s  ", phCode, uniqueCode[0]);
      if (hasDiff) System.out.format("diff = %s  ", phDiff);
      if (hasAddOn) System.out.format("add-on = %s  ", phAddOn);
      System.out.format("isUseless = %b\n", isUseless);
      System.out.format(
          "Segment: code = %s  type = %c, %c, %c  " + "sign = %2d  count = %d",
          phSeg, typeSeg[0], typeSeg[1], typeSeg[2], signSeg, countSeg);
      if (phRefl == null) System.out.println();
      else System.out.println("  refl = " + phRefl + " " + convRefl);
    }
    System.out.format(
        "Branch: pRange = %8.6f - %8.6f  xRange = %6.2f - %6.2f\n",
        pRange[0], pRange[1], Math.toDegrees(xRange[0]), Math.toDegrees(xRange[1]));
    if (hasDiff) System.out.format("        xDiff = %6.2f\n", Math.toDegrees(xDiff));
    if (turnShell != null) {
      System.out.format("Shell: %7.2f-%7.2f %s\n", rRange[0], rRange[1], turnShell);
    }

    if (full) {
      System.out.println(
          "\n         p        tau                 " + "basis function coefficients");
      for (int j = 0; j < pBrn.length; j++) {
        System.out.format(
            "%3d: %8.6f  %8.6f  %9.2e  %9.2e  %9.2e  %9.2e  " + "%9.2e\n",
            j, pBrn[j], tauBrn[j], basis[0][j], basis[1][j], basis[2][j], basis[3][j], basis[4][j]);
      }
    }
  }
}
