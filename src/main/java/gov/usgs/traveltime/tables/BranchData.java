package gov.usgs.traveltime.tables;

import java.util.Arrays;

/**
 * The BranchData class contains all information needed to set up a branch.
 *
 * @author Ray Buland
 */
public class BranchData {
  /** A String containing the branch phase code */
  private String phaseCode;

  /** A String containing the branch phase segment code */
  private String phaseSegmentCode;

  /** A String containing the Name of the shell where the rays turn */
  private String turnShellName;

  /** A boolean flag indicating whether this branch is up-going or not */
  private boolean isBranchUpGoing;

  /**
   * A char array containing the phase types of up-going, down-going, and coming back up ray
   * segments
   */
  private char[] raySegmentPhaseTypes;

  /** An int containing the sign of the up-going depthcorrection */
  private int upGoingDepthCorrectionSign;

  /** An int containing the number mantle traversals */
  private int numMantleTraversals;

  /** A double array containing the slowness rayTravelDistances for this branch */
  private double[] slownessRange;

  /** A double array containing the slowness rayTravelDistances for this branch */
  private double[] distanceRange;

  /**
   * A double array containing the radius rayTravelDistances of turning shell in kilometer for this
   * branch
   */
  private double[] radiusTurningRange;

  /** A double array containing the non-dimensional ray parameters */
  private double[] rayParameters = null;

  /** A double array containing the non-dimensional tau values */
  private double[] tauValues = null;

  /** A double array containing the non-dimensional ray travel distances */
  private double[] rayTravelDistances = null;

  /**
   * A two dimensional double array containing the non-dimensional interpolation basis coefficients
   */
  private double[][] basisCoefficients;

  /**
   * Set up an up-going branch.
   *
   * @param upType Phase type ('P' or 'S')
   */
  public BranchData(char upType) {
    phaseCode = "" + upType;
    phaseSegmentCode = phaseCode;
    isBranchUpGoing = true;
    raySegmentPhaseTypes = new char[3];
    upGoingDepthCorrectionSign = 1;
    Arrays.fill(raySegmentPhaseTypes, upType);
    numMantleTraversals = 0;
  }

  /**
   * Set up a down-going branch.
   *
   * @param phaseCode Phase code
   * @param raySegmentPhaseTypes A char array containing the phase types of up-going, down-going,
   *     and coming back up ray segments
   * @param numMantleTraversals Number of mantle traversals
   * @param shell Model shell where the rays in this branch turn
   */
  public BranchData(
      String phaseCode, char[] raySegmentPhaseTypes, int numMantleTraversals, ModelShell shell) {
    this.phaseCode = phaseCode;
    phaseSegmentCode = phaseCode;
    isBranchUpGoing = false;

    this.raySegmentPhaseTypes = Arrays.copyOf(raySegmentPhaseTypes, raySegmentPhaseTypes.length);

    if (raySegmentPhaseTypes[0] == 'p' || raySegmentPhaseTypes[0] == 's') {
      upGoingDepthCorrectionSign = 1;

      if (raySegmentPhaseTypes[0] == 'p') {
        this.raySegmentPhaseTypes[0] = 'P';
      } else {
        this.raySegmentPhaseTypes[0] = 'S';
      }
    } else {
      upGoingDepthCorrectionSign = -1;
    }
    this.numMantleTraversals = numMantleTraversals;

    if (shell != null) {
      turnShellName = shell.name;
      radiusTurningRange = new double[2];
      radiusTurningRange[0] = shell.rBot;
      radiusTurningRange[1] = shell.rTop;
    } else {
      turnShellName = null;
      radiusTurningRange = null;
    }
  }

  /**
   * Function to return the phase code for this branch.
   *
   * @return A String containing the phase code
   */
  public String getPhaseCode() {
    return phaseCode;
  }

  /**
   * Function to return the ray segment phase type array.
   *
   * @return A char array containing the phase types of up-going, down-going, and coming back up ray
   *     segments
   */
  public char[] getRaySegmentPhaseTypes() {
    return raySegmentPhaseTypes;
  }

  /**
   * Function to return the phase segment code.
   *
   * @return A String containing the phase segment code
   */
  public String getPhaseSegmentCode() {
    return phaseSegmentCode;
  }

  /**
   * Function to return the turning shell name.
   *
   * @return A String containing the turning shell name
   */
  public String getTurnShellName() {
    return turnShellName;
  }

  /**
   * Function to return whether the branch is up-going
   *
   * @return A boolean containing the the up-going branch flag
   */
  public boolean getIsBranchUpGoing() {
    return isBranchUpGoing;
  }

  /**
   * Function to return the sign of the up-going depth correction
   *
   * @return An int containing the sign of the up-going depth correction
   */
  public int getUpGoingDepthCorrectionSign() {
    return upGoingDepthCorrectionSign;
  }

  /**
   * Function to return the number of the up-going depth correction
   *
   * @return An int containing the count of the up-going depth correction
   */
  public int getNumMantleTraversals() {
    return numMantleTraversals;
  }

  /**
   * Function to return the summary non-dimensional slowness rayTravelDistances
   *
   * @return A double array containing the summary non-dimensional slowness rayTravelDistances
   */
  public double[] getSlownessRange() {
    return slownessRange;
  }

  /**
   * Function to return the summary non-dimensional distance rayTravelDistances
   *
   * @return A double array containing the summary non-dimensional distance rayTravelDistances
   */
  public double[] getDistanceRange() {
    return distanceRange;
  }

  /**
   * Function to return the radius rayTravelDistances of the turning shell
   *
   * @return double array containing the radius rayTravelDistances of the turning shell
   */
  public double[] getRadiusTurningRange() {
    return radiusTurningRange;
  }

  /**
   * Function to return the non-dimensional ray parameter sampling
   *
   * @return double array containing the non-dimensional ray parameter sampling
   */
  public double[] getRayParameters() {
    return rayParameters;
  }

  /**
   * Function to return the non-dimensional tauValues values
   *
   * @return double array containing the non-dimensional tauValues values
   */
  public double[] getTauValues() {
    return tauValues;
  }

  /**
   * Function to return the ray travel distances
   *
   * @return double array containing the ray travel distances
   */
  public double[] getRayTravelDistances() {
    return rayTravelDistances;
  }

  /**
   * Function to get the interpolation basis function coefficients
   *
   * @return A two dimensional double array containing the interpolation basis function coefficients
   */
  public double[][] getBasisCoefficients() {
    return basisCoefficients;
  }

  /**
   * Function to return basis function for a given row
   *
   * @param rowIndex An int identifying the index of the basis function row to return
   * @return double array containing the row of the interpolation basis functions
   */
  public double[] getBasisCoefficientRow(int rowIndex) {
    return basisCoefficients[rowIndex];
  }

  /**
   * Function to set the phase code for this branch.
   *
   * @param phaseCode A String containing the phase code
   */
  public void setPhaseCode(String phaseCode) {
    this.phaseCode = phaseCode;
  }

  /**
   * Function to set the non-dimensional ray parameter sampling
   *
   * @param rayParameters A double array containing the non-dimensional ray parameter sampling
   */
  public void setRayParameters(double[] rayParameters) {
    this.rayParameters = rayParameters;
  }

  /**
   * Function to set the non-dimensional tauValues values
   *
   * @param tauValues A double array containing the non-dimensional tauValues values
   */
  public void setTauValues(double[] tauValues) {
    this.tauValues = tauValues;
  }

  /**
   * Function to set the ray travel distances
   *
   * @param rayTravelDistances A double array containing the ray travel distances
   */
  public void setRayTravelDistances(double[] rayTravelDistances) {
    this.rayTravelDistances = rayTravelDistances;
  }

  /**
   * Function to set the interpolation basis function coefficients
   *
   * @param basisCoefficients A two dimensional double array containing the interpolation basis
   *     function coefficients
   */
  public void setBasisCoefficients(double[][] basisCoefficients) {
    this.basisCoefficients = basisCoefficients;
  }

  /** Function to do a partial update for the up-going branch. */
  public void update() {
    tauValues = new double[rayParameters.length];
    rayTravelDistances = new double[rayParameters.length];
    Arrays.fill(tauValues, 0d);
    Arrays.fill(rayTravelDistances, 0d);
    slownessRange = new double[2];
    slownessRange[0] = rayParameters[0];
    slownessRange[1] = rayParameters[rayParameters.length - 1];
    distanceRange = new double[2];
    distanceRange[0] = 0d;
    distanceRange[1] = 0d;
  }

  /**
   * Function to update the data arrays with the decimated versions.
   *
   * @param length A double containing the new length
   * @param decRayParams A double array containing the decimated slowness sampling
   * @param decTauValues A double array containing the decimated tau values
   * @param decRayTrav A double array containing the decimated ray travel distances
   */
  public void update(
      int length, double[] decRayParams, double[] decTauValues, double[] decRayTrav) {
    rayParameters = Arrays.copyOf(decRayParams, length);
    tauValues = Arrays.copyOf(decTauValues, length);
    rayTravelDistances = Arrays.copyOf(decRayTrav, length);

    // Figure ranges.
    slownessRange = new double[2];
    slownessRange[0] = rayParameters[0];
    slownessRange[1] = rayParameters[rayParameters.length - 1];
    distanceRange = new double[2];
    distanceRange[0] = rayTravelDistances[0];
    distanceRange[1] = rayTravelDistances[rayTravelDistances.length - 1];
  }

  /**
   * Function to print this branch to screen.
   *
   * @param full A boolean flag, if true, print the branch data as well as the header
   * @param nice A boolean flag, If true, convert ray travel distances to degrees
   */
  public void printBranch(boolean full, boolean nice) {
    if (rayParameters == null) {
      System.out.format(
          "%-8s %2d %c %c %c %1d %5b\n",
          phaseCode,
          upGoingDepthCorrectionSign,
          raySegmentPhaseTypes[0],
          raySegmentPhaseTypes[1],
          raySegmentPhaseTypes[2],
          numMantleTraversals,
          isBranchUpGoing);
    } else {
      if (full) {
        System.out.println();
      }

      if (!nice) {
        System.out.format(
            "%-8s %2d %c %c %c %1d %5b %8.6f %8.6f %8.6f %8.6f %3d\n",
            phaseCode,
            upGoingDepthCorrectionSign,
            raySegmentPhaseTypes[0],
            raySegmentPhaseTypes[1],
            raySegmentPhaseTypes[2],
            numMantleTraversals,
            isBranchUpGoing,
            slownessRange[0],
            slownessRange[1],
            distanceRange[0],
            distanceRange[1],
            rayParameters.length);

        if (turnShellName != null) {
          System.out.format(
              "     shell: %7.2f %7.2f %s\n",
              radiusTurningRange[0], radiusTurningRange[1], turnShellName);
        }

        if (full) {
          System.out.println(
              "         p       tauValues      X                " + "basis function coefficients");

          if (rayTravelDistances != null) {
            for (int j = 0; j < rayParameters.length; j++) {
              System.out.format(
                  "%3d: %8.6f %8.6f %8.6f %9.2e %9.2e %9.2e %9.2e " + "%9.2e\n",
                  j,
                  rayParameters[j],
                  tauValues[j],
                  rayTravelDistances[j],
                  basisCoefficients[0][j],
                  basisCoefficients[1][j],
                  basisCoefficients[2][j],
                  basisCoefficients[3][j],
                  basisCoefficients[4][j]);
            }
          } else {
            for (int j = 0; j < rayParameters.length; j++) {
              System.out.format(
                  "%3d: %8.6f %8.6f %8.6f %9.2e %9.2e %9.2e %9.2e " + "%9.2e\n",
                  j,
                  rayParameters[j],
                  0d,
                  0d,
                  basisCoefficients[0][j],
                  basisCoefficients[1][j],
                  basisCoefficients[2][j],
                  basisCoefficients[3][j],
                  basisCoefficients[4][j]);
            }
          }
        }
      } else {
        System.out.format(
            "%-8s %2d %c %c %c %1d %5b %8.6f %8.6f %6.2f %6.2f %3d\n",
            phaseCode,
            upGoingDepthCorrectionSign,
            raySegmentPhaseTypes[0],
            raySegmentPhaseTypes[1],
            raySegmentPhaseTypes[2],
            numMantleTraversals,
            isBranchUpGoing,
            slownessRange[0],
            slownessRange[1],
            Math.toDegrees(distanceRange[0]),
            Math.toDegrees(distanceRange[1]),
            rayParameters.length);

        if (turnShellName != null) {
          System.out.format(
              "     shell: %7.2f-%7.2f %s\n",
              radiusTurningRange[0], radiusTurningRange[1], turnShellName);
        }

        if (full) {
          System.out.println(
              "         p       tauValues      X                " + "basis function coefficients");

          if (rayTravelDistances != null) {
            for (int j = 0; j < rayParameters.length; j++) {
              System.out.format(
                  "%3d: %8.6f %8.6f %6.2f %9.2e %9.2e %9.2e %9.2e " + "%9.2e\n",
                  j,
                  rayParameters[j],
                  tauValues[j],
                  Math.toDegrees(rayTravelDistances[j]),
                  basisCoefficients[0][j],
                  basisCoefficients[1][j],
                  basisCoefficients[2][j],
                  basisCoefficients[3][j],
                  basisCoefficients[4][j]);
            }
          } else {
            for (int j = 0; j < rayParameters.length; j++) {
              System.out.format(
                  "%3d: %8.6f %8.6f %6.2f %9.2e %9.2e %9.2e %9.2e " + "%9.2e\n",
                  j,
                  rayParameters[j],
                  0d,
                  0d,
                  basisCoefficients[0][j],
                  basisCoefficients[1][j],
                  basisCoefficients[2][j],
                  basisCoefficients[3][j],
                  basisCoefficients[4][j]);
            }
          }
        }
      }
    }
  }

  /**
   * Function to convert this branch to a string.
   *
   * @return a String containing the branch.
   */
  public String toString() {
    if (rayParameters == null) {
      return String.format(
          "%-8s %2d %c %c %c %1d %5b",
          phaseCode,
          upGoingDepthCorrectionSign,
          raySegmentPhaseTypes[0],
          raySegmentPhaseTypes[1],
          raySegmentPhaseTypes[2],
          numMantleTraversals,
          isBranchUpGoing);
    } else {
      return String.format(
          "%-8s %2d %c %c %c %1d %5b %8.6f %8.6f %8.6f %8.6f %3d",
          phaseCode,
          upGoingDepthCorrectionSign,
          raySegmentPhaseTypes[0],
          raySegmentPhaseTypes[1],
          raySegmentPhaseTypes[2],
          numMantleTraversals,
          isBranchUpGoing,
          slownessRange[0],
          slownessRange[1],
          distanceRange[0],
          distanceRange[1],
          rayParameters.length);
    }
  }
}
