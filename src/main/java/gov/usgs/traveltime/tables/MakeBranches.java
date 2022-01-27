package gov.usgs.traveltime.tables;

import gov.usgs.traveltime.ModConvert;
import gov.usgs.traveltime.Spline;
import gov.usgs.traveltime.TtStatus;
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
 * Make the various desired travel-time branches by adding and subtracting bits and pieces of tau
 * and range in the major shells of the Earth (mantle, outer core, and inner core). Note that the
 * behavior of the up-going segment is coded, but not added in since it depends on source depth.
 *
 * @author Ray Buland
 */
public class MakeBranches {
  /**
   * This is awkward, but it turns out that the merged slownesses are ordered from the surface to
   * the center while the tau-range integrals are ordered from the center to the surface.
   * Unfortunately, the indices in the layers are for the merged slownesses and, of course, we now
   * need them for the tau-range integrals. This offset connects the two.
   */
  int slowOffset;

  double[] p, tau, x;
  double[][] basis;
  ArrayList<String> phases = null;
  ArrayList<BranchData> branches;
  TauModel finModel;
  ModConvert convert;
  DecimateTTBranch decimate;
  Spline spline;

  /**
   * Get the pieces we'll need in the following.
   *
   * @param finModel Final tau model of the Earth
   * @param decimate Branch decimation class
   */
  public MakeBranches(TauModel finModel, DecimateTTBranch decimate) {
    this.finModel = finModel;
    convert = finModel.convert;
    slowOffset = finModel.sPieces.getRayParameters().length - 1;
    this.decimate = decimate;
    spline = new Spline();
  }

  /**
   * Read in a list of desired phases.
   *
   * @param phaseFile Name of the file of desired phases
   * @return Travel-time status.
   */
  public TtStatus readPhases(String phaseFile) {
    BufferedInputStream inPhases = null;
    Scanner scan;

    // Open and read the desired phases.
    try {
      inPhases = new BufferedInputStream(new FileInputStream(phaseFile));
    } catch (FileNotFoundException e) {
      return TtStatus.BAD_PHASE_LIST;
    }

    // Read the desired phases.
    phases = new ArrayList<String>();
    scan = new Scanner(inPhases);
    while (scan.hasNext()) {
      phases.add(scan.next());
    }
    scan.close();
    try {
      inPhases.close();
    } catch (IOException e) {
      return TtStatus.BAD_PHASE_LIST;
    }
    if (phases.size() == 0) return TtStatus.BAD_PHASE_LIST;
    else {
      doBranches();
      return TtStatus.SUCCESS;
    }
  }

  /**
   * Alternatively, we can take a phase list generated externally.
   *
   * @param phases List of desired phases
   * @return Travel-time status
   */
  public TtStatus getPhases(ArrayList<String> phases) {
    if (phases == null) return TtStatus.BAD_PHASE_LIST;
    if (phases.size() == 0) return TtStatus.BAD_PHASE_LIST;
    this.phases = phases;
    doBranches();
    return TtStatus.SUCCESS;
  }

  /** Use the phase code to figure out what sort of branch to set up. */
  private void doBranches() {
    String phCode; // Branch phase code
    char[] code;
    int begShell, endShell;

    branches = new ArrayList<BranchData>();
    if (TablesUtil.deBugLevel > 0) {
      System.out.println();
    }

    for (int j = 0; j < phases.size(); j++) {
      phCode = phases.get(j);
      code = phCode.toCharArray();
      /** Direct branches (P and S). */
      if (code.length == 1) {
        if (code[0] == 'P' || code[0] == 'S') {
          // Add an up-going branch stub for the direct branches.
          upGoing(code[0]);
          // Direct branches ray parameters go from the surface to the center.
          endShell = finModel.getShellIndex(code[0], ShellName.SURFACE);
          refracted(
              phCode,
              wrapTypes(code[0], code[0], code[0]),
              wrapCounts(1, 1, 1),
              endShell,
              finModel.getShell(code[0], endShell).iTop);
        } else {
          System.out.println("\n***** Unknown phase code type (" + phCode + ") *****\n");
        }
        /** Surface reflected and converted phases. */
      } else if (code.length == 2) {
        /** Surface reflected phases (pP, sP, pS, and sS). */
        if (code[0] == 'p' || code[0] == 's') {
          if (code[1] == 'P' || code[1] == 'S') {
            // Surface reflection ray parameters go from the surface to the
            // center.
            if (code[0] == 'p' || code[1] == 'P') {
              // If any part of the phase is a P, it restricts the ray
              // parameter range.
              endShell = finModel.getShellIndex('P', ShellName.SURFACE);
              refracted(
                  phCode,
                  wrapTypes(code[0], code[1], code[1]),
                  wrapCounts(1, 1, 1),
                  endShell,
                  finModel.getShell('P', endShell).iTop);
            } else {
              // If the phase is all S, we use all ray parameters.
              endShell = finModel.getShellIndex('S', ShellName.SURFACE);
              refracted(
                  phCode,
                  wrapTypes(code[0], code[1], code[1]),
                  wrapCounts(1, 1, 1),
                  endShell,
                  finModel.getShell('S', endShell).iTop);
            }
          } else {
            System.out.println("\n***** Unknown phase code type (" + phCode + ") *****\n");
          }
          /** Surface converted phases (PP, SS, SP, and PS). */
        } else {
          if ((code[0] == 'P' || code[0] == 'S') && (code[1] == 'P' || code[1] == 'S')) {
            if (code[0] == code[1]) {
              // For PP and SS, use ray parameters from the surface to the center.
              endShell = finModel.getShellIndex(code[0], ShellName.SURFACE);
              refracted(
                  phCode,
                  wrapTypes(code[0], code[0], code[1]),
                  wrapCounts(2, 2, 2),
                  endShell,
                  finModel.getShell(code[0], endShell).iTop);
            } else {
              // For SP and PS, just use ray parameters from the surface to the
              // core.
              begShell = finModel.getShellIndex('S', ShellName.MANTLE_BOTTOM);
              endShell = finModel.getShellIndex('P', ShellName.SURFACE);
              converted(
                  phCode,
                  wrapTypes(code[0], code[0], code[1]),
                  wrapCounts(2, 0, 0),
                  begShell,
                  endShell,
                  finModel.getShell('S', begShell).iTop,
                  finModel.getShell('P', endShell).iTop);
            }
          } else {
            System.out.println("\n***** Unknown phase code type (" + phCode + ") *****\n");
          }
        }
        /** Outer core reflections (PcP, ScS, ScP, and PcS). */
      } else if (phCode.contains("c")) {
        if (phCode.length() == 3
            && (code[0] == 'P' || code[0] == 'S')
            && (code[2] == 'P' || code[2] == 'S')) {
          // These phases have ray parameters that go from the surface to the
          // core.
          if (code[0] == 'P' || code[2] == 'P') {
            // If any part of the phase is a P, it restricts the ray
            // parameter range.
            endShell = finModel.getShellIndex('P', ShellName.MANTLE_BOTTOM);
            reflected(
                phCode,
                wrapTypes(code[0], code[0], code[2]),
                wrapCounts(1, 0, 0),
                endShell,
                finModel.getShell('P', endShell).iTop);
          } else {
            // If the phase is all S, we use all ray parameters.
            endShell = finModel.getShellIndex('S', ShellName.MANTLE_BOTTOM);
            reflected(
                phCode,
                wrapTypes(code[0], code[0], code[2]),
                wrapCounts(1, 0, 0),
                endShell,
                finModel.getShell('S', endShell).iTop);
          }
        } else {
          System.out.println("\n***** Unknown phase code type (" + phCode + ") *****\n");
        }
        /** Surface reflected and direct inner core reflections. */
      } else if (phCode.contains("KiK")) {
        /**
         * Surface reflected inner core reflections (pPKiKP, sPKiKP, pSKiKS, sSKiKS, pSKiKP, sSKiKP,
         * pPKiKS, and sPKiKS).
         */
        if (code[0] == 'p' || code[0] == 's') {
          // These phases are restricted to ray parameters that can reach
          // the inner core.
          if (phCode.length() == 6
              && (code[1] == 'P' || code[1] == 'S')
              && (code[5] == 'P' || code[5] == 'S')) {
            endShell = finModel.getShellIndex('P', ShellName.INNER_CORE_BOUNDARY);
            reflected(
                phCode,
                wrapTypes(code[0], code[1], code[5]),
                wrapCounts(1, 1, 0),
                endShell,
                finModel.getShell('P', endShell).iTop);
          } else {
            System.out.println("\n***** Unknown phase code type (" + phCode + ") *****\n");
          }
          /** Direct inner core reflections (PKiKP, SKiKS, SKiKP, and PKiKS). */
        } else {
          // These phases are restricted to ray parameters that can reach
          // the inner core.
          if (phCode.length() == 5
              && (code[0] == 'P' || code[0] == 'S')
              && (code[4] == 'P' || code[4] == 'S')) {
            endShell = finModel.getShellIndex('P', ShellName.INNER_CORE_BOUNDARY);
            reflected(
                phCode,
                wrapTypes(code[0], code[0], code[4]),
                wrapCounts(1, 1, 0),
                endShell,
                finModel.getShell('P', endShell).iTop);
          } else {
            System.out.println("\n***** Unknown phase code type (" + phCode + ") *****\n");
          }
        }
        /**
         * Reflections from the under side of the core-mantle boundary (PKKP, SKKS, SKKP, and PKKS).
         */
      } else if (phCode.contains("KK")) {
        // These phases have to reach the core, but the nature of the core-
        // mantle boundary complicates things.
        if (phCode.length() == 4
            && (code[0] == 'P' || code[0] == 'S')
            && (code[3] == 'P' || code[3] == 'S')) {
          if (code[0] == 'P' || code[3] == 'P') {
            // If any part of the phase is a P, it restricts the ray
            // parameter range.
            endShell = finModel.getShellIndex('P', ShellName.CORE_TOP);
            refracted(
                phCode,
                wrapTypes(code[0], code[0], code[3]),
                wrapCounts(1, 2, 2),
                endShell,
                finModel.getShell('P', endShell).iTop);
          } else {
            // If the phase is all S in the mantle, it still changes things.
            endShell = finModel.getShellIndex('S', ShellName.CORE_TOP);
            refracted(
                phCode,
                wrapTypes(code[0], code[0], code[3]),
                wrapCounts(1, 2, 2),
                endShell,
                finModel.getShell('S', endShell).iTop);
          }
        } else {
          System.out.println("\n***** Unknown phase code type (" + phCode + ") *****\n");
        }
        /**
         * Core-mantle boundary conversions (SKP and PKS). Note that direct core phases (PKP and
         * SKS) are included with P and S.
         */
      } else if (phCode.contains("K")) {
        if (phCode.length() == 3
            && (code[0] == 'P' || code[0] == 'S')
            && (code[2] == 'P' || code[2] == 'S')) {
          // These phases have to reach the core.
          endShell = finModel.getShellIndex('P', ShellName.CORE_TOP);
          refracted(
              phCode,
              wrapTypes(code[0], code[0], code[2]),
              wrapCounts(1, 1, 1),
              endShell,
              finModel.getShell('P', endShell).iTop);
        } else {
          System.out.println("\n***** Unknown phase code type (" + phCode + ") *****\n");
        }
        /** We have either a bad or unimplemented phase code. */
      } else {
        System.out.println("\n***** Unknown phase code type (" + phCode + ") *****\n");
      }
    }
  }

  /**
   * Build a list of branch end ray parameter values. The final list will be sorted into ascending
   * order and each value will be unique.
   *
   * @return List of branch end ray parameters.
   */
  public TreeSet<Double> getBranchEnds() {
    TreeSet<Double> ends;

    ends = new TreeSet<Double>();
    for (int j = 0; j < branches.size(); j++) {
      ends.add(branches.get(j).getSlownessRange()[0]);
      ends.add(branches.get(j).getSlownessRange()[1]);
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
   * Get the travel-time branches we just constructed.
   *
   * @return Travel-time branch data
   */
  public ArrayList<BranchData> getBranches() {
    return branches;
  }

  /**
   * Create a stub for an up-going branch.
   *
   * @param upType Phase type ('P' or 'S')
   */
  private void upGoing(char upType) {
    branches.add(newBranch(upType));
  }

  /**
   * Create a refracted branch. Note that little p and s phases are included here even though they
   * are surface reflections and perhaps even converted. It can also include compound phases such as
   * PP and PKKP. It is assumed that all refracted phases will include ray parameters all the way to
   * the center of the Earth (e.g., P includes PKP).
   *
   * @param phCode Phase code
   * @param upType Type of up-going phase, if any
   * @param downType Type of down-going phase
   * @param retType Type of the phase coming back up
   * @param mCount Number of mantle traversals
   * @param ocCount Number of outer core traversals
   * @param icCount Number of inner core traversals
   * @param endShell Ending shell index
   * @param endP Ending slowness index
   */
  private void refracted(
      String phCode, char[] typeSeg, double[] shellCounts, int endShell, int endP) {
    int minBrnP = 0, maxBrnP;
    double xTarget, xFactor;
    BranchData branch;
    ModelShell shell;

    // Do some setup.
    endP = slowOffset - endP;
    xFactor = decFactor(shellCounts, false);

    // Create the branch.
    for (int shIndex = 0; shIndex <= endShell; shIndex++) {
      shell = finModel.getShell(typeSeg[1], shIndex);
      // Figure the index of the maximum ray parameter for this branch.
      maxBrnP = Math.min(slowOffset - shell.iTop, endP);
      if (shell.getCode(typeSeg[1]).charAt(0) != 'r' && minBrnP < maxBrnP) {
        // Initialize the branch.
        branch = buildBranch(phCode, typeSeg, shellCounts, minBrnP, maxBrnP, shell);
        // Deal with low velocity zones at discontinuities.
        fixLVZ(typeSeg, shellCounts, minBrnP);
        // Do the decimation.
        xTarget =
            xFactor
                * Math.max(
                    finModel.getDelX(typeSeg[1], shIndex), finModel.getDelX(typeSeg[2], shIndex));
        decimate.downGoingDecimation(branch, xTarget, minBrnP);
        // Create the interpolation basis functions.
        spline.basisSet(branch.getRayParameters(), branch.getBasisCoefficients());
        // We need to name each sub-branch.
        branch.setPhaseCode(
            makePhCode(
                shellCounts, shell.getCode(typeSeg[1]), shell.getCode(typeSeg[2]), typeSeg[0]));
        if (TablesUtil.deBugLevel > 0) {
          System.out.format(
              "     %2d %-8s %3d %3d %3.0f\n",
              branches.size(), branch.getPhaseCode(), minBrnP, maxBrnP, convert.dimR(xTarget));
        }
        // OK.  Add it to the branches list.
        branches.add(branch);
      }
      minBrnP = maxBrnP;
    }
  }

  /**
   * Create a reflected branch. Note that little p and s phases are included here as well as
   * conversions at the reflector (generally the outer or inner core).
   *
   * @param phCode Phase code
   * @param upType Type of up-going phase, if any
   * @param downType Type of down-going phase
   * @param retType Type of the phase coming back up
   * @param mCount Number of mantle traversals
   * @param ocCount Number of outer core traversals
   * @param icCount Number of inner core traversals
   * @param endShell Ending shell index
   * @param endP Ending slowness index
   */
  private void reflected(
      String phCode, char[] typeSeg, double[] shellCounts, int endShell, int endP) {
    double xTarget;
    BranchData branch;

    // Create the branch.
    endP = slowOffset - endP;
    branch = buildBranch(phCode, typeSeg, shellCounts, 0, endP, null);
    // Decimate the branch.
    xTarget =
        decFactor(shellCounts, true)
            * Math.max(
                finModel.getNextDelX(typeSeg[1], endShell),
                finModel.getNextDelX(typeSeg[2], endShell));
    decimate.downGoingDecimation(branch, xTarget, 0);
    // Create the interpolation basis functions.
    spline.basisSet(branch.getRayParameters(), branch.getBasisCoefficients());
    if (TablesUtil.deBugLevel > 0) {
      System.out.format(
          "     %2d %-8s %3d %3d %3.0f\n",
          branches.size(), branch.getPhaseCode(), 0, endP, convert.dimR(xTarget));
    }
    // Add it to the branch list.
    branches.add(branch);
  }

  /**
   * Create a surface converted branch. This is a special case and only includes two compound phase:
   * a P or S down-going from the source to the surface and then converted to an S or P and down-
   * going again to the surface. Because P'S' and S'P' aren't very useful phases, this case has only
   * been tested for mantle phases.
   *
   * @param phCode Phase code
   * @param upType Type of up-going phase, if any
   * @param downType Type of down-going phase
   * @param retType Type of the phase coming back up
   * @param mCount Number of mantle traversals
   * @param ocCount Number of outer core traversals
   * @param icCount Number of inner core traversals
   * @param begShell Beginning shell index
   * @param endShell Ending shell index
   * @param begP Beginning slowness index
   * @param endP Ending slowness index
   */
  private void converted(
      String phCode,
      char[] typeSeg,
      double[] shellCounts,
      int begShell,
      int endShell,
      int begP,
      int endP) {
    boolean useShell2 = false;
    int minBrnP, maxBrnP1, maxBrnP2, maxBrnP, shIndex2;
    double xTarget, xFactor;
    BranchData branch;
    ModelShell shell1, shell2;

    // Do some setup.
    begP = slowOffset - begP;
    endP = slowOffset - endP;
    minBrnP = begP;
    shIndex2 = begShell;
    xFactor = decFactor(shellCounts, false);

    /*
     * Create the branch.  This logic is really convoluted because the two possible
     * converted branches (SP and PS) contain a complete down-going S (or P) converted
     * at the surface and followed by a complete down-going P (or S).  This results in
     * lots of the apparently possible phases being geometrically impossible.
     */
    for (int shIndex1 = begShell; shIndex1 <= endShell; shIndex1++) {
      shell1 = finModel.getShell(typeSeg[1], shIndex1);
      // Figure the index of the maximum ray parameter for the first ray type.
      maxBrnP1 = Math.min(slowOffset - shell1.iTop, endP);
      if (shell1.getCode(typeSeg[1]).charAt(0) != 'r' && minBrnP < maxBrnP1) {
        // Now find an index of the maximum ray parameter for the second ray
        // type that works.
        do {
          shell2 = finModel.getShell(typeSeg[2], shIndex2);
          maxBrnP2 = Math.min(slowOffset - shell2.iTop, endP);
          if ((shell2.getCode(typeSeg[2]).charAt(0) != 'r' && minBrnP < maxBrnP2)
              || shIndex2 == finModel.shellSize(typeSeg[2]) - 1) {
            if (slowOffset - shell1.iTop <= slowOffset - shell2.iTop) {
              useShell2 = false;
              maxBrnP = maxBrnP1;
              if (TablesUtil.deBugLevel > 1) {
                System.out.format(
                    "nph: nph in j l code = %c %d %3d %3d\n",
                    typeSeg[1], shIndex1, minBrnP, maxBrnP);
              }
            } else {
              useShell2 = true;
              maxBrnP = maxBrnP2;
              if (TablesUtil.deBugLevel > 1) {
                System.out.format(
                    "kph: kph ik j l code = %c %d %3d %3d\n",
                    typeSeg[2], shIndex2, minBrnP, maxBrnP);
              }
            }

            // Initialize the branch.
            if (!useShell2) {
              branch = buildBranch(phCode, typeSeg, shellCounts, minBrnP, maxBrnP, shell1);
            } else {
              branch = buildBranch(phCode, typeSeg, shellCounts, minBrnP, maxBrnP, shell2);
            }
            // Deal with low velocity zones at discontinuities.
            fixLVZ(typeSeg, shellCounts, minBrnP);
            // Do the decimation.
            xTarget =
                xFactor
                    * Math.max(
                        finModel.getDelX(typeSeg[1], shIndex1),
                        finModel.getDelX(typeSeg[2], shIndex2));
            decimate.downGoingDecimation(branch, xTarget, minBrnP);
            // Create the interpolation basis functions.
            spline.basisSet(branch.getRayParameters(), branch.getBasisCoefficients());
            // We need to name each sub-branch.
            branch.setPhaseCode(
                makePhCode(
                    shellCounts,
                    shell1.getCode(typeSeg[1]),
                    shell2.getCode(typeSeg[2]),
                    typeSeg[0]));
            if (TablesUtil.deBugLevel > 0) {
              if (TablesUtil.deBugLevel > 1) {
                System.out.format("shells: %2d %2d\n", shIndex1, shIndex2);
              }
              System.out.format(
                  "     %2d %-8s %3d %3d %3.0f %5b\n",
                  branches.size(),
                  branch.getPhaseCode(),
                  minBrnP,
                  maxBrnP,
                  convert.dimR(xTarget),
                  useShell2);
            }
            // OK.  Add it to the branches list.
            branches.add(branch);
          } else {
            useShell2 = true;
          }
          // Update the start of the next branch.
          if (!useShell2) {
            // We used the outer shell loop.
            minBrnP = Math.max(minBrnP, maxBrnP1);
          } else {
            // We used the inner shell loop.
            shIndex2++;
            minBrnP = Math.max(minBrnP, maxBrnP2);
          }
          // see if we're still in the inner shell loop.
        } while (useShell2 && minBrnP < maxBrnP1);
      } else {
        minBrnP = Math.max(minBrnP, maxBrnP1);
      }
    }
  }

  /**
   * For convenience, wrap the traversal counts for the mantle, outer core, and inner core into a
   * double array.
   *
   * @param mCount Number of mantle traversals
   * @param ocCount Number of outer core traversals
   * @param icCount Number of inner core traversals
   * @return Traversal counts wrapped into a double array
   */
  private double[] wrapCounts(int mCount, int ocCount, int icCount) {
    double[] shellCounts;

    shellCounts = new double[3];
    shellCounts[0] = mCount;
    shellCounts[1] = ocCount;
    shellCounts[2] = icCount;
    return shellCounts;
  }

  /**
   * For convenience, wrap the ray types for the up-going, down-going, and return paths.
   *
   * @param upType Type of up-going phase, if any
   * @param downType Type of down-going phase
   * @param retType Type of the phase coming back up
   * @return Ray path types wrapped into a char array
   */
  private char[] wrapTypes(char upType, char downType, char retType) {
    char[] typeSeg;

    typeSeg = new char[3];
    typeSeg[0] = upType;
    typeSeg[1] = downType;
    typeSeg[2] = retType;
    return typeSeg;
  }

  /**
   * Create an up-going branch.
   *
   * @param upType Type of up-going phase
   * @return Branch data
   */
  private BranchData newBranch(char upType) {
    BranchData branch;

    // Create the branch.
    branch = new BranchData(upType);

    // Set up the ray parameter arrays.
    if (upType == 'P') {
      branch.setRayParameters(finModel.pPieces.getProxyRayParameters());
    } else {
      branch.setRayParameters(finModel.sPieces.getProxyRayParameters());
    }
    branch.setBasisCoefficients(new double[5][branch.getRayParameters().length]);
    spline.basisSet(branch.getRayParameters(), branch.getBasisCoefficients());
    branch.update();
    if (TablesUtil.deBugLevel > 0) {
      System.out.format(
          "     %2d %s up     %3d %3d\n",
          branches.size(), branch.getPhaseCode(), 0, branch.getRayParameters().length - 1);
    }
    return branch;
  }

  /**
   * Create a down-going branch.
   *
   * @param phCode Phase code
   * @param typeSeg Types of up-going phase, down-going phase, and phase coming back up
   * @param countSeg Number of mantle traversals
   * @param n Number of ray parameter samples
   * @param shell Model shell where rays in this branch turn
   * @return Branch data
   */
  private BranchData newBranch(
      String phCode, char[] typeSeg, int countSeg, int n, ModelShell shell) {
    BranchData branch;

    branch = new BranchData(phCode, typeSeg, countSeg, shell);
    // Allocate arrays.
    branch.setRayParameters(new double[n]);
    branch.setTauValues(new double[n]);
    branch.setRayTravelDistances(new double[n]);
    branch.setBasisCoefficients(new double[5][n]);
    // Make the branch data arrays local for convenience.
    p = branch.getRayParameters();
    tau = branch.getTauValues();
    x = branch.getRayTravelDistances();
    basis = branch.getBasisCoefficients();
    // Initialize them.
    Arrays.fill(tau, 0d);
    Arrays.fill(x, 0d);
    return branch;
  }

  /**
   * Create a down-going branch and populate the ray parameter, tau, and range arrays.
   *
   * @param phCode Phase code
   * @param typeSeg Types of up-going phase, down-going phase, and phase coming back up
   * @param shellCounts The number of traversals of the mantle, outer core, and inner core
   * @param minBrnP Beginning ray parameter index
   * @param maxBrnP Ending ray parameter index
   * @param shell Model shell where rays in this branch turn
   * @return Branch data
   */
  private BranchData buildBranch(
      String phCode,
      char[] typeSeg,
      double[] shellCounts,
      int minBrnP,
      int maxBrnP,
      ModelShell shell) {
    BranchData branch;

    // Initialize the branch.
    branch = newBranch(phCode, typeSeg, (int) shellCounts[0], maxBrnP - minBrnP + 1, shell);
    // Add up the branch data.
    for (int i = minBrnP, k = 0; i <= maxBrnP; i++, k++) {
      p[k] = finModel.getP(i);
      for (int j = 0; j < shellCounts.length; j++) {
        tau[k] +=
            shellCounts[j]
                * (finModel.getTauSpec(typeSeg[1], i, j) + finModel.getTauSpec(typeSeg[2], i, j));
        x[k] +=
            shellCounts[j]
                * (finModel.getXspec(typeSeg[1], i, j) + finModel.getXspec(typeSeg[2], i, j));
      }
    }
    return branch;
  }

  /**
   * If this branch begins just under a low velocity zone (high slowness zone), we need to correct
   * tau and range.
   *
   * @param typeSeg Types of up-going phase, down-going phase, and phase coming back up
   * @param shellCounts The number of traversals of the mantle, outer core, and inner core
   * @param minBrnP Beginning ray parameter index
   */
  private void fixLVZ(char[] typeSeg, double[] shellCounts, int minBrnP) {
    int lvzIndex;

    lvzIndex = finModel.getIndex(typeSeg[1], p[0]);
    if (lvzIndex >= 0) {
      if (finModel.getLvz(typeSeg[1], lvzIndex)) {
        // We have a low velocity zone on the down-going ray.
        for (int j = 0; j < shellCounts.length; j++) {
          tau[0] -= shellCounts[j] * finModel.getTauSpec(typeSeg[1], minBrnP, j);
          x[0] -= shellCounts[j] * finModel.getXspec(typeSeg[1], minBrnP, j);
        }
        tau[0] += shellCounts[0] * finModel.getTauInt(typeSeg[1], lvzIndex)[minBrnP];
        x[0] += shellCounts[0] * finModel.getXInt(typeSeg[1], lvzIndex)[minBrnP];
      }
    }
    lvzIndex = finModel.getIndex(typeSeg[2], p[0]);
    if (lvzIndex >= 0) {
      if (finModel.getLvz(typeSeg[2], lvzIndex)) {
        // We have a low velocity zone on the returning ray.
        for (int j = 0; j < shellCounts.length; j++) {
          tau[0] -= shellCounts[j] * finModel.getTauSpec(typeSeg[2], minBrnP, j);
          x[0] -= shellCounts[j] * finModel.getXspec(typeSeg[2], minBrnP, j);
        }
        tau[0] += shellCounts[0] * finModel.getTauInt(typeSeg[2], lvzIndex)[minBrnP];
        x[0] += shellCounts[0] * finModel.getXInt(typeSeg[2], lvzIndex)[minBrnP];
      }
    }
  }

  /**
   * The decimation factor is partly based on the number of traversals of the major model shells
   * except for reflected phases. Note that this is purely based on experience and guess work.
   *
   * @param shellCounts The number of traversals of the mantle, outer core, and inner core
   * @param reflected True if this is a reflected phase.
   * @return Decimation factor
   */
  private double decFactor(double[] shellCounts, boolean reflected) {
    double xFactor;
    if (!reflected) {
      xFactor =
          Math.max(
              0.75d * (double) Math.max(shellCounts[0], Math.max(shellCounts[1], shellCounts[2])),
              1d);
    } else {
      if (shellCounts[1] > 0) {
        xFactor = 1.5d;
      } else {
        xFactor = 1d;
      }
    }
    return xFactor;
  }

  /**
   * We have a phase code for each branch already, but since one tau-p logical branch (e.g., P) can
   * generate lots of seismologically distinct sub-branches (e.g., Pg, Pb, Pn, P, PKP, and PKIKP),
   * we need a way of generating the sub-branch names from the temporary phase codes associated with
   * each model shell.
   *
   * @param shellCounts The number of traversals of the mantle, outer core, and inner core
   * @param downCode The temporary phase code for the down-going ray
   * @param retCode The temporary phase code for the returning ray
   * @param upType The phase type of the initial ray (lower case for up-going, upper case for
   *     down-going)
   * @return Sub-branch phase code
   */
  private String makePhCode(double[] shellCounts, String downCode, String retCode, char upType) {
    String newCode;

    // Set a default to work with.
    newCode = downCode.substring(1, 2) + retCode.substring(2);
    // See what we really have.
    if (shellCounts[0] < 2d) {
      // The phase is direct in the mantle (e.g. P).
      if (shellCounts[1] > 1d) {
        // The phase is reflected from the under side of the core-mantle
        // boundary.  Set a new default.
        newCode =
            downCode.substring(1, 2)
                + "KKKKKKKK".substring(0, (int) shellCounts[1] - 1)
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

  /** Print a list of the desired phases. */
  public void printPhases() {
    System.out.println("\nPhases:");
    for (int j = 0; j < phases.size(); j++) {
      System.out.println("  " + phases.get(j));
    }
  }

  /** Print a list of branch headers. */
  public void printBranches() {
    System.out.println("\n\tBranches");
    for (int j = 0; j < branches.size(); j++) {
      System.out.format("%3d %s\n", j, branches.get(j));
    }
  }

  /**
   * Print a list of branch headers. This option allows for more flexible printing.
   *
   * @param full If true, print the branch data as well
   * @param nice If true, convert range to degrees
   */
  public void printBranches(boolean full, boolean nice) {
    System.out.println("\n\tBranches");

    for (int j = 0; j < branches.size(); j++) {
      branches.get(j).printBranch(full, nice);
    }
  }
}
