package gov.usgs.traveltime.tables;

import gov.usgs.traveltime.ModelConversions;
import gov.usgs.traveltime.Spline;
import gov.usgs.traveltime.TravelTimeStatus;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Scanner;
import java.util.TreeSet;

/**
 * MakeBranches is a class that makes the various desired travel-time branches by adding and
 * subtracting bits and pieces of tau and range in the major shells of the Earth (mantle, outer
 * core, and inner core). Note that the behavior of the up-going segment is coded, but not added in
 * since it depends on source depth.
 *
 * @author Ray Buland
 */
public class MakeBranches {
  /**
   * A double containing the slowness integral offest. This is awkward, but it turns out that the
   * merged slownesses are ordered from the surface to the center while the tau-range integrals are
   * ordered from the center to the surface. Unfortunately, the indices in the layers are for the
   * merged slownesses and, of course, we now need them for the tau-range integrals. This offset
   * connects the two.
   */
  private int slownessIntegralOffset;

  /** A double array containing the non-dimensional ray parameters */
  private double[] rayParameters;

  /** A double array containing the non-dimensional tau values */
  private double[] tauValues;

  /** A double array containing the non-dimensional ray travel distances */
  private double[] rayTravelDistances;

  /**
   * A two dimensional double array containing the non-dimensional interpolation basis coefficients
   */
  private double[][] basisCoefficients;

  /** An ArrayList of strings containing the desired phases to make branches with */
  private ArrayList<String> phaseList = null;

  /** An ArrayList of branches containing the branches we've made */
  private ArrayList<BranchData> branchList;

  /** A TauModel object containing the final model */
  private TauModel finalTTModel;

  /** A ModelConversions object containing the model dependant conversions */
  private ModelConversions modelConversions;

  /** A DecimateTravelTimeBranch object containing the Branch decimation class */
  private DecimateTravelTimeBranch branchDecimator;

  /** A Spline object holding the spline interpolation routines */
  private Spline spline;

  /**
   * Get the travel-time branches we have constructed.
   *
   * @return An array list of BranchData objects containing the Travel-time branch data
   */
  public ArrayList<BranchData> getBranchList() {
    return branchList;
  }

  /**
   * MakeBranches Constructor, set the pieces we'll need.
   *
   * @param finalTTModel A TauModel object containing the final model
   * @param branchDecimator A DecimateTravelTimeBranch containing the Branch decimation class
   */
  public MakeBranches(TauModel finalTTModel, DecimateTravelTimeBranch branchDecimator) {
    this.finalTTModel = finalTTModel;
    modelConversions = finalTTModel.getModelConversions();
    slownessIntegralOffset = finalTTModel.getIntPiecesS().getRayParameters().length - 1;
    this.branchDecimator = branchDecimator;
    spline = new Spline();
  }

  /**
   * Function to read in a list of desired phases for the branches from a file.
   *
   * @param phaseFile A string containing the path to the the file containing the desired phases
   * @return A TravelTimeStatus object reporting the travel-time status.
   */
  public TravelTimeStatus readPhases(String phaseFile) {
    BufferedInputStream inPhases = null;
    Scanner scan;

    // Open and read the desired phases.
    try {
      inPhases = new BufferedInputStream(new FileInputStream(phaseFile));
    } catch (FileNotFoundException e) {
      return TravelTimeStatus.BAD_PHASE_LIST;
    }

    // Read the desired phases.
    phaseList = new ArrayList<String>();
    scan = new Scanner(inPhases);

    while (scan.hasNext()) {
      phaseList.add(scan.next());
    }
    scan.close();

    try {
      inPhases.close();
    } catch (IOException e) {
      return TravelTimeStatus.BAD_PHASE_LIST;
    }

    if (phaseList.size() == 0) {
      return TravelTimeStatus.BAD_PHASE_LIST;
    } else {
      doBranches();
      return TravelTimeStatus.SUCCESS;
    }
  }

  /**
   * Function to read in a list of desired phases for the branches from a list.
   *
   * @param phaseList An arraylist of strings containing the desired phases
   * @return A TravelTimeStatus object reporting the travel-time status.
   */
  public TravelTimeStatus getPhases(ArrayList<String> phaseList) {
    if (phaseList == null) {
      return TravelTimeStatus.BAD_PHASE_LIST;
    }

    if (phaseList.size() == 0) {
      return TravelTimeStatus.BAD_PHASE_LIST;
    }

    this.phaseList = phaseList;
    doBranches();

    return TravelTimeStatus.SUCCESS;
  }

  /** Function that uses the phase codes to figure out what sort of branchs to set up. */
  private void doBranches() {
    String phaseCode; // Branch phase code
    char[] code;
    int beginShellIndex, endShellIndex;

    branchList = new ArrayList<BranchData>();
    if (TablesUtil.deBugLevel > 0) {
      System.out.println();
    }

    for (int j = 0; j < phaseList.size(); j++) {
      phaseCode = phaseList.get(j);
      code = phaseCode.toCharArray();

      /** Direct branches (P and S). */
      if (code.length == 1) {
        if (code[0] == 'P' || code[0] == 'S') {
          // Add an up-going branch stub for the direct branches.
          upGoing(code[0]);

          // Direct branches ray parameters go from the surface to the center.
          endShellIndex = finalTTModel.getShellIndex(code[0], ShellName.SURFACE);

          refracted(
              phaseCode,
              wrapTypes(code[0], code[0], code[0]),
              wrapCounts(1, 1, 1),
              endShellIndex,
              finalTTModel.getShell(code[0], endShellIndex).getTopSampleIndex());
        } else {
          System.out.println("\n***** Unknown phase code type (" + phaseCode + ") *****\n");
        }
        /** Surface reflected and converted phaseList. */
      } else if (code.length == 2) {
        /** Surface reflected phases (pP, sP, pS, and sS). */
        if (code[0] == 'p' || code[0] == 's') {
          if (code[1] == 'P' || code[1] == 'S') {
            // Surface reflection ray parameters go from the surface to the
            // center.
            if (code[0] == 'p' || code[1] == 'P') {
              // If any part of the phase is a P, it restricts the ray
              // parameter range.
              endShellIndex = finalTTModel.getShellIndex('P', ShellName.SURFACE);

              refracted(
                  phaseCode,
                  wrapTypes(code[0], code[1], code[1]),
                  wrapCounts(1, 1, 1),
                  endShellIndex,
                  finalTTModel.getShell('P', endShellIndex).getTopSampleIndex());
            } else {
              // If the phase is all S, we use all ray parameters.
              endShellIndex = finalTTModel.getShellIndex('S', ShellName.SURFACE);

              refracted(
                  phaseCode,
                  wrapTypes(code[0], code[1], code[1]),
                  wrapCounts(1, 1, 1),
                  endShellIndex,
                  finalTTModel.getShell('S', endShellIndex).getTopSampleIndex());
            }
          } else {
            System.out.println("\n***** Unknown phase code type (" + phaseCode + ") *****\n");
          }
          /** Surface converted phases (PP, SS, SP, and PS). */
        } else {
          if ((code[0] == 'P' || code[0] == 'S') && (code[1] == 'P' || code[1] == 'S')) {
            if (code[0] == code[1]) {
              // For PP and SS, use ray parameters from the surface to the center.
              endShellIndex = finalTTModel.getShellIndex(code[0], ShellName.SURFACE);

              refracted(
                  phaseCode,
                  wrapTypes(code[0], code[0], code[1]),
                  wrapCounts(2, 2, 2),
                  endShellIndex,
                  finalTTModel.getShell(code[0], endShellIndex).getTopSampleIndex());
            } else {
              // For SP and PS, just use ray parameters from the surface to the
              // core.
              beginShellIndex = finalTTModel.getShellIndex('S', ShellName.MANTLE_BOTTOM);
              endShellIndex = finalTTModel.getShellIndex('P', ShellName.SURFACE);

              converted(
                  phaseCode,
                  wrapTypes(code[0], code[0], code[1]),
                  wrapCounts(2, 0, 0),
                  beginShellIndex,
                  endShellIndex,
                  finalTTModel.getShell('S', beginShellIndex).getTopSampleIndex(),
                  finalTTModel.getShell('P', endShellIndex).getTopSampleIndex());
            }
          } else {
            System.out.println("\n***** Unknown phase code type (" + phaseCode + ") *****\n");
          }
        }
        /** Outer core reflections (PcP, ScS, ScP, and PcS). */
      } else if (phaseCode.contains("c")) {
        if (phaseCode.length() == 3
            && (code[0] == 'P' || code[0] == 'S')
            && (code[2] == 'P' || code[2] == 'S')) {
          // These phases have ray parameters that go from the surface to the
          // core.
          if (code[0] == 'P' || code[2] == 'P') {
            // If any part of the phase is a P, it restricts the ray
            // parameter range.
            endShellIndex = finalTTModel.getShellIndex('P', ShellName.MANTLE_BOTTOM);

            reflected(
                phaseCode,
                wrapTypes(code[0], code[0], code[2]),
                wrapCounts(1, 0, 0),
                endShellIndex,
                finalTTModel.getShell('P', endShellIndex).getTopSampleIndex());
          } else {
            // If the phase is all S, we use all ray parameters.
            endShellIndex = finalTTModel.getShellIndex('S', ShellName.MANTLE_BOTTOM);

            reflected(
                phaseCode,
                wrapTypes(code[0], code[0], code[2]),
                wrapCounts(1, 0, 0),
                endShellIndex,
                finalTTModel.getShell('S', endShellIndex).getTopSampleIndex());
          }
        } else {
          System.out.println("\n***** Unknown phase code type (" + phaseCode + ") *****\n");
        }
        /** Surface reflected and direct inner core reflections. */
      } else if (phaseCode.contains("KiK")) {
        /**
         * Surface reflected inner core reflections (pPKiKP, sPKiKP, pSKiKS, sSKiKS, pSKiKP, sSKiKP,
         * pPKiKS, and sPKiKS).
         */
        if (code[0] == 'p' || code[0] == 's') {
          // These phases are restricted to ray parameters that can reach
          // the inner core.
          if (phaseCode.length() == 6
              && (code[1] == 'P' || code[1] == 'S')
              && (code[5] == 'P' || code[5] == 'S')) {
            endShellIndex = finalTTModel.getShellIndex('P', ShellName.INNER_CORE_BOUNDARY);

            reflected(
                phaseCode,
                wrapTypes(code[0], code[1], code[5]),
                wrapCounts(1, 1, 0),
                endShellIndex,
                finalTTModel.getShell('P', endShellIndex).getTopSampleIndex());
          } else {
            System.out.println("\n***** Unknown phase code type (" + phaseCode + ") *****\n");
          }
          /** Direct inner core reflections (PKiKP, SKiKS, SKiKP, and PKiKS). */
        } else {
          // These phases are restricted to ray parameters that can reach
          // the inner core.
          if (phaseCode.length() == 5
              && (code[0] == 'P' || code[0] == 'S')
              && (code[4] == 'P' || code[4] == 'S')) {
            endShellIndex = finalTTModel.getShellIndex('P', ShellName.INNER_CORE_BOUNDARY);

            reflected(
                phaseCode,
                wrapTypes(code[0], code[0], code[4]),
                wrapCounts(1, 1, 0),
                endShellIndex,
                finalTTModel.getShell('P', endShellIndex).getTopSampleIndex());
          } else {
            System.out.println("\n***** Unknown phase code type (" + phaseCode + ") *****\n");
          }
        }
        /**
         * Reflections from the under side of the core-mantle boundary (PKKP, SKKS, SKKP, and PKKS).
         */
      } else if (phaseCode.contains("KK")) {
        // These phases have to reach the core, but the nature of the core-
        // mantle boundary complicates things.
        if (phaseCode.length() == 4
            && (code[0] == 'P' || code[0] == 'S')
            && (code[3] == 'P' || code[3] == 'S')) {
          if (code[0] == 'P' || code[3] == 'P') {
            // If any part of the phase is a P, it restricts the ray
            // parameter range.
            endShellIndex = finalTTModel.getShellIndex('P', ShellName.CORE_TOP);

            refracted(
                phaseCode,
                wrapTypes(code[0], code[0], code[3]),
                wrapCounts(1, 2, 2),
                endShellIndex,
                finalTTModel.getShell('P', endShellIndex).getTopSampleIndex());
          } else {
            // If the phase is all S in the mantle, it still changes things.
            endShellIndex = finalTTModel.getShellIndex('S', ShellName.CORE_TOP);

            refracted(
                phaseCode,
                wrapTypes(code[0], code[0], code[3]),
                wrapCounts(1, 2, 2),
                endShellIndex,
                finalTTModel.getShell('S', endShellIndex).getTopSampleIndex());
          }
        } else {
          System.out.println("\n***** Unknown phase code type (" + phaseCode + ") *****\n");
        }
        /**
         * Core-mantle boundary conversions (SKP and PKS). Note that direct core phases (PKP and
         * SKS) are included with P and S.
         */
      } else if (phaseCode.contains("K")) {
        if (phaseCode.length() == 3
            && (code[0] == 'P' || code[0] == 'S')
            && (code[2] == 'P' || code[2] == 'S')) {
          // These phases have to reach the core.
          endShellIndex = finalTTModel.getShellIndex('P', ShellName.CORE_TOP);

          refracted(
              phaseCode,
              wrapTypes(code[0], code[0], code[2]),
              wrapCounts(1, 1, 1),
              endShellIndex,
              finalTTModel.getShell('P', endShellIndex).getTopSampleIndex());
        } else {
          System.out.println("\n***** Unknown phase code type (" + phaseCode + ") *****\n");
        }
        /** We have either a bad or unimplemented phase code. */
      } else {
        System.out.println("\n***** Unknown phase code type (" + phaseCode + ") *****\n");
      }
    }
  }

  /**
   * Function to compute a list of branch end ray parameter values. The final list will be sorted
   * into ascending order and each value will be unique.
   *
   * @return A TreeSet of double values containing the list of branch end ray parameters.
   */
  public TreeSet<Double> getBranchEnds() {
    TreeSet<Double> ends;

    ends = new TreeSet<Double>();
    for (int j = 0; j < branchList.size(); j++) {
      ends.add(branchList.get(j).getSlownessRange()[0]);
      ends.add(branchList.get(j).getSlownessRange()[1]);
    }

    if (TablesUtil.deBugLevel > 1) {
      System.out.println("\nBranch End Ray Parameters:");
      Iterator<Double> iter = ends.iterator();

      while (iter.hasNext()) {
        System.out.format("     %8.6f\n", iter.next());
      }
    }
    return ends;
  }

  /**
   * Function to create a stub for an up-going branch.
   *
   * @param upType A character containing the phase type ('P' or 'S')
   */
  private void upGoing(char upType) {
    branchList.add(newUpBranch(upType));
  }

  /**
   * Function to create a refracted branch. Note that little p and s phases are included here even
   * though they are surface reflections and perhaps even converted. It can also include compound
   * phases such as PP and PKKP. It is assumed that all refracted phases will include ray parameters
   * all the way to the center of the Earth (e.g., P includes PKP).
   *
   * @param phaseCode A String containing the desired phase code
   * @param rayTypes An array of char values containing the the ray types for the up-going,
   *     down-going, and return paths
   * @param shellTravCounts An array of double values holding the number of traversals for the
   *     mantle, outer core, and inner core
   * @param endShellIndex An integer containing the ending shell index
   * @param endSlownessIndex An integer containing the ending slowness index
   */
  private void refracted(
      String phaseCode,
      char[] rayTypes,
      double[] shellTravCounts,
      int endShellIndex,
      int endSlownessIndex) {

    // Do some setup.
    int minRayParamIndex = 0;
    endSlownessIndex = slownessIntegralOffset - endSlownessIndex;
    double decimationFactor = compDecimationFactor(shellTravCounts, false);

    // Create the branch.
    for (int shIndex = 0; shIndex <= endShellIndex; shIndex++) {
      ModelShell shell = finalTTModel.getShell(rayTypes[1], shIndex);

      // Figure the index of the maximum ray parameter for this branch.
      int maxRayParamIndex =
          Math.min(slownessIntegralOffset - shell.getTopSampleIndex(), endSlownessIndex);
      if (shell.getTempCode(rayTypes[1]).charAt(0) != 'r' && minRayParamIndex < maxRayParamIndex) {
        // Initialize the branch.
        BranchData branch =
            buildBranch(
                phaseCode, rayTypes, shellTravCounts, minRayParamIndex, maxRayParamIndex, shell);

        // Deal with low velocity zones at discontinuities.
        fixLowVelZone(rayTypes, shellTravCounts, minRayParamIndex);

        // Do the decimation.
        double xTarget =
            decimationFactor
                * Math.max(
                    finalTTModel.getRangeIncrementTarget(rayTypes[1], shIndex),
                    finalTTModel.getRangeIncrementTarget(rayTypes[2], shIndex));
        branchDecimator.downGoingDecimation(branch, xTarget, minRayParamIndex);

        // Create the interpolation basis functions.
        spline.basisSet(branch.getRayParameters(), branch.getBasisCoefficients());

        // We need to name each sub-branch.
        branch.setPhaseCode(
            makePhCode(
                shellTravCounts,
                shell.getTempCode(rayTypes[1]),
                shell.getTempCode(rayTypes[2]),
                rayTypes[0]));

        if (TablesUtil.deBugLevel > 0) {
          System.out.format(
              "     %2d %-8s %3d %3d %3.0f\n",
              branchList.size(),
              branch.getPhaseCode(),
              minRayParamIndex,
              maxRayParamIndex,
              modelConversions.dimR(xTarget));
        }

        // OK.  Add it to the branches list.
        branchList.add(branch);
      }

      minRayParamIndex = maxRayParamIndex;
    }
  }

  /**
   * Ffnction to create a reflected branch. Note that little p and s phases are included here as
   * well as conversions at the reflector (generally the outer or inner core).
   *
   * @param phaseCode A String containing the desired phase code
   * @param rayTypes An array of char values containing the the ray types for the up-going,
   *     down-going, and return paths
   * @param shellTravCounts An array of double values holding the number of traversals for the
   *     mantle, outer core, and inner core
   * @param endShellIndex An integer containing the ending shell index
   * @param endSlownessIndex An integer containing the ending slowness index
   */
  private void reflected(
      String phaseCode,
      char[] rayTypes,
      double[] shellTravCounts,
      int endShellIndex,
      int endSlownessIndex) {

    // Create the branch.
    endSlownessIndex = slownessIntegralOffset - endSlownessIndex;
    BranchData branch =
        buildBranch(phaseCode, rayTypes, shellTravCounts, 0, endSlownessIndex, null);

    // Decimate the branch.
    double xTarget =
        compDecimationFactor(shellTravCounts, true)
            * Math.max(
                finalTTModel.getNextRangeIncrementTarget(rayTypes[1], endShellIndex),
                finalTTModel.getNextRangeIncrementTarget(rayTypes[2], endShellIndex));
    branchDecimator.downGoingDecimation(branch, xTarget, 0);

    // Create the interpolation basis functions.
    spline.basisSet(branch.getRayParameters(), branch.getBasisCoefficients());
    if (TablesUtil.deBugLevel > 0) {
      System.out.format(
          "     %2d %-8s %3d %3d %3.0f\n",
          branchList.size(),
          branch.getPhaseCode(),
          0,
          endSlownessIndex,
          modelConversions.dimR(xTarget));
    }

    // Add it to the branch list.
    branchList.add(branch);
  }

  /**
   * Function to create a surface converted branch. This is a special case and only includes two
   * compound phase: a P or S down-going from the source to the surface and then converted to an S
   * or P and down- going again to the surface. Because P'S' and S'P' aren't very useful phases,
   * this case has only been tested for mantle phases.
   *
   * @param phaseCode A String containing the desired phase code
   * @param rayTypes An array of char values containing the the ray types for the up-going,
   *     down-going, and return paths
   * @param shellTravCounts An array of double values holding the number of traversals for the
   *     mantle, outer core, and inner core
   * @param beginShellIndex An integer containing the beginning shell index
   * @param endShellIndex An integer containing the ending shell index
   * @param beginSlownessIndex An integer containing the beginning slowness index
   * @param endSlownessIndex An integer containing the ending slowness index
   */
  private void converted(
      String phaseCode,
      char[] rayTypes,
      double[] shellTravCounts,
      int beginShellIndex,
      int endShellIndex,
      int beginSlownessIndex,
      int endSlownessIndex) {
    // Do some setup.
    boolean useShell2 = false;
    beginSlownessIndex = slownessIntegralOffset - beginSlownessIndex;
    endSlownessIndex = slownessIntegralOffset - endSlownessIndex;
    int minRayParamIndex = beginSlownessIndex;
    int shIndex2 = beginShellIndex;
    double decimationFactor = compDecimationFactor(shellTravCounts, false);

    /*
     * Create the branch.  This logic is really convoluted because the two possible
     * converted branches (SP and PS) contain a complete down-going S (or P) converted
     * at the surface and followed by a complete down-going P (or S).  This results in
     * lots of the apparently possible phases being geometrically impossible.
     */
    for (int shIndex1 = beginShellIndex; shIndex1 <= endShellIndex; shIndex1++) {
      ModelShell shell1 = finalTTModel.getShell(rayTypes[1], shIndex1);

      // Figure the index of the maximum ray parameter for the first ray type.
      int maxBrnP1 =
          Math.min(slownessIntegralOffset - shell1.getTopSampleIndex(), endSlownessIndex);

      if (shell1.getTempCode(rayTypes[1]).charAt(0) != 'r' && minRayParamIndex < maxBrnP1) {
        // Now find an index of the maximum ray parameter for the second ray
        // type that works.
        do {
          ModelShell shell2 = finalTTModel.getShell(rayTypes[2], shIndex2);
          int maxBrnP2 =
              Math.min(slownessIntegralOffset - shell2.getTopSampleIndex(), endSlownessIndex);
          int maxRayParamIndex;

          if ((shell2.getTempCode(rayTypes[2]).charAt(0) != 'r' && minRayParamIndex < maxBrnP2)
              || shIndex2 == finalTTModel.shellSize(rayTypes[2]) - 1) {
            if (slownessIntegralOffset - shell1.getTopSampleIndex()
                <= slownessIntegralOffset - shell2.getTopSampleIndex()) {
              useShell2 = false;
              maxRayParamIndex = maxBrnP1;

              if (TablesUtil.deBugLevel > 1) {
                System.out.format(
                    "nph: nph in j l code = %c %d %3d %3d\n",
                    rayTypes[1], shIndex1, minRayParamIndex, maxRayParamIndex);
              }
            } else {
              useShell2 = true;
              maxRayParamIndex = maxBrnP2;

              if (TablesUtil.deBugLevel > 1) {
                System.out.format(
                    "kph: kph ik j l code = %c %d %3d %3d\n",
                    rayTypes[2], shIndex2, minRayParamIndex, maxRayParamIndex);
              }
            }

            // Initialize the branch.
            BranchData branch;
            if (!useShell2) {
              branch =
                  buildBranch(
                      phaseCode,
                      rayTypes,
                      shellTravCounts,
                      minRayParamIndex,
                      maxRayParamIndex,
                      shell1);
            } else {
              branch =
                  buildBranch(
                      phaseCode,
                      rayTypes,
                      shellTravCounts,
                      minRayParamIndex,
                      maxRayParamIndex,
                      shell2);
            }

            // Deal with low velocity zones at discontinuities.
            fixLowVelZone(rayTypes, shellTravCounts, minRayParamIndex);

            // Do the decimation.
            double xTarget =
                decimationFactor
                    * Math.max(
                        finalTTModel.getRangeIncrementTarget(rayTypes[1], shIndex1),
                        finalTTModel.getRangeIncrementTarget(rayTypes[2], shIndex2));
            branchDecimator.downGoingDecimation(branch, xTarget, minRayParamIndex);

            // Create the interpolation basis functions.
            spline.basisSet(branch.getRayParameters(), branch.getBasisCoefficients());

            // We need to name each sub-branch.
            branch.setPhaseCode(
                makePhCode(
                    shellTravCounts,
                    shell1.getTempCode(rayTypes[1]),
                    shell2.getTempCode(rayTypes[2]),
                    rayTypes[0]));
            if (TablesUtil.deBugLevel > 0) {
              if (TablesUtil.deBugLevel > 1) {
                System.out.format("shells: %2d %2d\n", shIndex1, shIndex2);
              }

              System.out.format(
                  "     %2d %-8s %3d %3d %3.0f %5b\n",
                  branchList.size(),
                  branch.getPhaseCode(),
                  minRayParamIndex,
                  maxRayParamIndex,
                  modelConversions.dimR(xTarget),
                  useShell2);
            }

            // OK.  Add it to the branches list.
            branchList.add(branch);
          } else {
            useShell2 = true;
          }

          // Update the start of the next branch.
          if (!useShell2) {
            // We used the outer shell loop.
            minRayParamIndex = Math.max(minRayParamIndex, maxBrnP1);
          } else {
            // We used the inner shell loop.
            shIndex2++;
            minRayParamIndex = Math.max(minRayParamIndex, maxBrnP2);
          }

          // see if we're still in the inner shell loop.
        } while (useShell2 && minRayParamIndex < maxBrnP1);
      } else {
        minRayParamIndex = Math.max(minRayParamIndex, maxBrnP1);
      }
    }
  }

  /**
   * Convenience function that wraps the traversal counts for the mantle, outer core, and inner core
   * into a double array.
   *
   * @param mCount An integer containing the number of mantle traversals
   * @param ocCount An integer containing the number of outer core traversals
   * @param icCount An integer containing the number of inner core traversals
   * @return An array of doubles containing the traversal counts
   */
  private double[] wrapCounts(int mCount, int ocCount, int icCount) {
    double[] shellTravCounts = new double[3];

    shellTravCounts[0] = mCount;
    shellTravCounts[1] = ocCount;
    shellTravCounts[2] = icCount;

    return shellTravCounts;
  }

  /**
   * Convenience function that wraps the ray types for the up-going, down-going, and return paths
   * into a single object.
   *
   * @param upType A char containing the type of the up-going phase, if any
   * @param downType A char containing the type of the of down-going phase
   * @param retType A char containing the type of the of the phase coming back up
   * @return An array of char varibles containing the ray path types
   */
  private char[] wrapTypes(char upType, char downType, char retType) {
    char[] rayTypes = new char[3];

    rayTypes[0] = upType;
    rayTypes[1] = downType;
    rayTypes[2] = retType;

    return rayTypes;
  }

  /**
   * Function to create an up-going branch.
   *
   * @param upType A char containing the type of up-going phase
   * @return A BranchData object containing the branch data
   */
  private BranchData newUpBranch(char upType) {
    // Create the branch.
    BranchData branch = new BranchData(upType);

    // Set up the ray parameter arrays.
    if (upType == 'P') {
      branch.setRayParameters(finalTTModel.getIntPiecesP().getProxyRayParameters());
    } else {
      branch.setRayParameters(finalTTModel.getIntPiecesS().getProxyRayParameters());
    }

    branch.setBasisCoefficients(new double[5][branch.getRayParameters().length]);
    spline.basisSet(branch.getRayParameters(), branch.getBasisCoefficients());
    branch.update();

    if (TablesUtil.deBugLevel > 0) {
      System.out.format(
          "     %2d %s up     %3d %3d\n",
          branchList.size(), branch.getPhaseCode(), 0, branch.getRayParameters().length - 1);
    }

    return branch;
  }

  /**
   * Function to create a down-going branch.
   *
   * @param phaseCode A string containing the branch phase code
   * @param rayTypes An array of char values containing the the ray types for the up-going,
   *     down-going, and return paths
   * @param numMantleTrav An integer holding the number of mantle traversals
   * @param numRayParams An integer containing the number of ray parameter samples
   * @param shell ModelShell object containing the model shell where rays in this branch turn
   * @return A BranchData object containing the branch data
   */
  private BranchData newDownBranch(
      String phaseCode, char[] rayTypes, int numMantleTrav, int numRayParams, ModelShell shell) {
    BranchData branch = new BranchData(phaseCode, rayTypes, numMantleTrav, shell);

    // Allocate arrays.
    branch.setRayParameters(new double[numRayParams]);
    branch.setTauValues(new double[numRayParams]);
    branch.setRayTravelDistances(new double[numRayParams]);
    branch.setBasisCoefficients(new double[5][numRayParams]);

    // Make the branch data arrays local for convenience.
    rayParameters = branch.getRayParameters();
    tauValues = branch.getTauValues();
    rayTravelDistances = branch.getRayTravelDistances();
    basisCoefficients = branch.getBasisCoefficients();

    // Initialize them.
    Arrays.fill(tauValues, 0d);
    Arrays.fill(rayTravelDistances, 0d);

    return branch;
  }

  /**
   * Function to create a down-going branch and populate the ray parameter, tau, and range arrays.
   *
   * @param phaseCode A string containing the branch phase code
   * @param rayTypes An array of char values containing the the ray types for the up-going,
   *     down-going, and return paths
   * @param shellTravCounts An array of double values holding the number of traversals for the
   *     mantle, outer core, and inner core
   * @param minRayParamIndex An integer containing the beginning ray parameter index
   * @param maxRayParamIndex An integer containing the ending ray parameter index
   * @param shell ModelShell object containing the model shell where rays in this branch turn
   * @return A BranchData object containing the branch data
   */
  private BranchData buildBranch(
      String phaseCode,
      char[] rayTypes,
      double[] shellTravCounts,
      int minRayParamIndex,
      int maxRayParamIndex,
      ModelShell shell) {
    // Initialize the branch.
    BranchData branch =
        newDownBranch(
            phaseCode,
            rayTypes,
            (int) shellTravCounts[0],
            maxRayParamIndex - minRayParamIndex + 1,
            shell);

    // Add up the branch data.
    for (int i = minRayParamIndex, k = 0; i <= maxRayParamIndex; i++, k++) {
      rayParameters[k] = finalTTModel.getRayParameters(i);

      for (int j = 0; j < shellTravCounts.length; j++) {
        tauValues[k] +=
            shellTravCounts[j]
                * (finalTTModel.getSpecialTauIntegrals(rayTypes[1], i, j)
                    + finalTTModel.getSpecialTauIntegrals(rayTypes[2], i, j));
        rayTravelDistances[k] +=
            shellTravCounts[j]
                * (finalTTModel.getSpecialRangeIntegrals(rayTypes[1], i, j)
                    + finalTTModel.getSpecialRangeIntegrals(rayTypes[2], i, j));
      }
    }

    return branch;
  }

  /**
   * Function to correct tau and range. If this branch begins just under a low velocity zone (high
   * slowness zone)
   *
   * @param rayTypes An array of char values containing the the ray types for the up-going,
   *     down-going, and return paths
   * @param shellTravCounts An array of double values holding the number of traversals for the
   *     mantle, outer core, and inner core
   * @param minRayParamIndex An integer containing the beginning ray parameter index
   */
  private void fixLowVelZone(char[] rayTypes, double[] shellTravCounts, int minRayParamIndex) {
    int lvzIndex = finalTTModel.getIndex(rayTypes[1], rayParameters[0]);

    if (lvzIndex >= 0) {
      if (finalTTModel.getLowVelocityZone(rayTypes[1], lvzIndex)) {
        // We have a low velocity zone on the down-going ray.
        for (int j = 0; j < shellTravCounts.length; j++) {
          tauValues[0] -=
              shellTravCounts[j]
                  * finalTTModel.getSpecialTauIntegrals(rayTypes[1], minRayParamIndex, j);
          rayTravelDistances[0] -=
              shellTravCounts[j]
                  * finalTTModel.getSpecialRangeIntegrals(rayTypes[1], minRayParamIndex, j);
        }

        tauValues[0] +=
            shellTravCounts[0]
                * finalTTModel.getTauIntegrals(rayTypes[1], lvzIndex)[minRayParamIndex];
        rayTravelDistances[0] +=
            shellTravCounts[0]
                * finalTTModel.getRangeIntegrals(rayTypes[1], lvzIndex)[minRayParamIndex];
      }
    }

    lvzIndex = finalTTModel.getIndex(rayTypes[2], rayParameters[0]);

    if (lvzIndex >= 0) {
      if (finalTTModel.getLowVelocityZone(rayTypes[2], lvzIndex)) {
        // We have a low velocity zone on the returning ray.
        for (int j = 0; j < shellTravCounts.length; j++) {
          tauValues[0] -=
              shellTravCounts[j]
                  * finalTTModel.getSpecialTauIntegrals(rayTypes[2], minRayParamIndex, j);
          rayTravelDistances[0] -=
              shellTravCounts[j]
                  * finalTTModel.getSpecialRangeIntegrals(rayTypes[2], minRayParamIndex, j);
        }

        tauValues[0] +=
            shellTravCounts[0]
                * finalTTModel.getTauIntegrals(rayTypes[2], lvzIndex)[minRayParamIndex];
        rayTravelDistances[0] +=
            shellTravCounts[0]
                * finalTTModel.getRangeIntegrals(rayTypes[2], lvzIndex)[minRayParamIndex];
      }
    }
  }

  /**
   * Function to compute the decimation factor. The decimation factor is partly based on the number
   * of traversals of the major model shells except for reflected phases. Note that this is purely
   * based on experience and guess work.
   *
   * @param shellTravCounts An array of double values holding the number of traversals for the
   *     mantle, outer core, and inner core
   * @param reflected A boolean holding the reflected flag, true if this is a reflected phase.
   * @return A double containing the decimation factor
   */
  private double compDecimationFactor(double[] shellTravCounts, boolean reflected) {
    double decimationFactor;

    if (!reflected) {
      decimationFactor =
          Math.max(
              0.75d
                  * (double)
                      Math.max(
                          shellTravCounts[0], Math.max(shellTravCounts[1], shellTravCounts[2])),
              1d);
    } else {
      if (shellTravCounts[1] > 0) {
        decimationFactor = 1.5d;
      } else {
        decimationFactor = 1d;
      }
    }

    return decimationFactor;
  }

  /**
   * Generate the phase codes for this branch. While we have a phase code for each branch already,
   * but since one tau-p logical branch (e.g., P) can generate lots of seismologically distinct
   * sub-branches (e.g., Pg, Pb, Pn, P, PKP, and PKIKP), we need a way of generating the sub-branch
   * names from the temporary phase codes associated with each model shell.
   *
   * @param shellTravCounts An array of double values holding the number of traversals for the
   *     mantle, outer core, and inner core
   * @param downCode A string containing the the temporary phase code for the down-going ray
   * @param retCode A string containing the temporary phase code for the returning ray
   * @param upType A string containing the phase type of the initial ray (lower case for up-going,
   *     upper case for down-going)
   * @return A string containing the sub-branch phase code
   */
  private String makePhCode(
      double[] shellTravCounts, String downCode, String retCode, char upType) {
    // Set a default to work with.
    String newCode = downCode.substring(1, 2) + retCode.substring(2);

    // See what we really have.
    if (shellTravCounts[0] < 2d) {
      // The phase is direct in the mantle (e.g. P).
      if (shellTravCounts[1] > 1d) {
        // The phase is reflected from the under side of the core-mantle
        // boundary.  Set a new default.
        newCode =
            downCode.substring(1, 2)
                + "KKKKKKKK".substring(0, (int) shellTravCounts[1] - 1)
                + retCode.substring(2);
      }
    } else {
      // The phase is compound in the mantle (e.g., PP).
      if (downCode.length() < 3) {
        newCode = downCode.substring(1, 2) + retCode.substring(1);
      } else {
        if (downCode.charAt(2) != 'K') {
          newCode = downCode.substring(1, 3) + retCode.substring(1);
        } else {
          newCode =
              downCode.substring(1, 2) + "'" + retCode.substring(1, 2) + "'" + retCode.substring(4);
        }
      }
    }

    // See if we need to turn an "ab" branch into an "ac" branch.
    if (newCode.charAt(0) == 'S' && (newCode.contains("KSab") || newCode.contains("S'ab"))) {
      newCode = newCode.replace("ab", "ac");
    }

    // Add in little "p" or "s" if necessary.
    if (upType == 'p' || upType == 's') {
      newCode = upType + newCode;
    }

    return newCode;
  }

  /** Function to print a list of the desired phases. */
  public void printPhases() {
    System.out.println("\nPhases:");

    for (int j = 0; j < phaseList.size(); j++) {
      System.out.println("  " + phaseList.get(j));
    }
  }

  /** Function to print a list of branch headers. */
  public void printBranches() {
    System.out.println("\n\tBranches");

    for (int j = 0; j < branchList.size(); j++) {
      System.out.format("%3d %s\n", j, branchList.get(j));
    }
  }

  /**
   * Function to print a list of branch headers. This option allows for more flexible printing.
   *
   * @param full A boolean flag, if true, print the branch data as well
   * @param nice A boolean flag, if true, modelConversions range to degrees
   */
  public void printBranches(boolean full, boolean nice) {
    System.out.println("\n\tBranches");

    for (int j = 0; j < branchList.size(); j++) {
      branchList.get(j).printBranch(full, nice);
    }
  }
}
