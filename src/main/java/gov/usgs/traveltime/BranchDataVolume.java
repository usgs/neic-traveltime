package gov.usgs.traveltime;

import gov.usgs.traveltime.tables.TauIntegralException;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Generate all volatile information associated with one travel-time branch.
 *
 * @author Ray Buland
 */
public class BranchDataVolume {
  /** A boolean causticFlags indicating whether travel times should be computed */
  private boolean shouldComputeTT;

  /** A boolean causticFlags indicating if the corrected branch still exists */
  private boolean correctedBranchExists;

  /**
   * A boolean causticFlags indicating whether this phase is useless (phase is always in the coda of
   * another phase)
   */
  boolean isPhaseUseless;

  /** A String containing the corrected phase code */
  private String correctedPhaseCode;

  /** A string containing the unique phase code, used to store diffracted and add on phases */
  private String[] uniquePhaseCode;

  /** An array of doubles containing the corrected slowness range for this branch */
  private double[] correctedSlownessRange;

  /** An array of doubles containing the corrected distance range for this branch */
  private double[] correctedDistanceRange;

  /** An array of doubles containing the corrected distance range for a diffracted branch */
  private double[] diffractedDistanceRange;

  /** A double containing the corrected slowness of a caustic, if any */
  private double correctedCausticSlowness;

  /** An array of doubles containing the updated ray parameter grid */
  private double[] updatedRayParameters;

  /** An array of doubles containing the corrected tau values */
  private double[] correctedTauValues;

  /** An array of doubles containing the interpolated distance values */
  private double[] interpolatedDistanceValues;

  /**
   * A two dimensional array of doubles containing the interpolation polynomials for tau(p) for each
   * ray parameter interval
   */
  private double[][] interpolationPolynomials = null;

  /**
   * A two dimensional array of doubles containing the distance limits for each ray parameter
   * interval
   */
  private double[][] distanceLimits;

  /** An integer containing the count of caustic minimums in this branch */
  private int minimumCausticCount;

  /** An integer containing the count of caustic maximums in this branch */
  private int maximumCausticCount;

  /** An array of Strings containing the min/max causticFlags highlighting caustics for printing */
  private String[] causticFlags;

  /** An array of integers containing the number of valid distance ranges for travel times */
  private int[] numTTDistanceRanges;

  /** A double containing the normalization for the depth derivative */
  private double depthDerivativeNorm;

  /** A BranchDataReference object containing the non-volatile branch data reference object */
  private BranchDataReference branchReference;

  /** An UpGoingDataVolume containing the P data for correcting the depth */
  private UpGoingDataVolume upgoingPBranch;

  /** An UpGoingDataVolume containing the S data for correcting the depth */
  UpGoingDataVolume upgoingSBranch;

  /** A ModelConversions object containing model dependent constants and conversions */
  private final ModelConversions modelConversions;

  /** A AuxiliaryTTReference object holding the travel time auxiliary data */
  private AuxiliaryTTReference auxTTReference;

  /**
   * A TravelTimeFlags object containing a local copy of the auxiliary travel-time flags,
   * statistics, and corrections by phase (loaded from AuxiliaryTTReference)
   */
  private TravelTimeFlags ttFlags;

  /**
   * A TravelTimeStatistics object containing a local copy of the phase statistics (loaded from
   * AuxiliaryTTReference)
   */
  private TravelTimeStatistics ttStatistics;

  /**
   * An Ellipticity object containing a local copy of the ellipticity correction (loaded from
   * AuxiliaryTTReference)
   */
  private Ellipticity ellipticityCorrections;

  /**
   * A Spline object holding the spline interpolation routines needed for the computation of travel
   * times.
   */
  private Spline splineRoutines;

  /**
   * A double containing the current travel time correction computed by the last call to
   * computeOneRay
   */
  private double correctedTravelTime;

  /**
   * A double containing the current computed distance sign that needs to be remembered between
   * calls
   */
  private double distanceSign = Double.NaN;

  /**
   * A double containing the current computed depth sign that needs to be remembered between calls
   */
  private double depthSign = Double.NaN;

  /** A double containing the squared up going P/S slowness used for the corecting the depth */
  private double slownessUpSquared = Double.NaN;

  /**
   * A double containing the ending ray parameter value that needs to be remembered between calls
   */
  private double rayParameterEndValue = Double.NaN;

  /** Private logging object. */
  private static final Logger LOGGER = Logger.getLogger(AllBranchVolume.class.getName());

  /**
   * Function to return whether the corrected branch still exists
   *
   * @return A boolean causticFlags indicating if the corrected branch still exists
   */
  public boolean getCorrectedBranchExists() {
    return correctedBranchExists;
  }

  /**
   * Function to return whether this phase is useless
   *
   * @return A boolean causticFlags indicating whether this phase is useless (phase is always in the
   *     coda of another phase)
   */
  public boolean getIsPhaseUseless() {
    return (isPhaseUseless);
  }

  /**
   * Function to return the branch segment code.
   *
   * @return A String containing the branch segment phase code
   */
  public String getGenericPhaseCode() {
    return branchReference.getGenericPhaseCode();
  }

  /**
   * Function to return the corrected phase code
   *
   * @return A String containing the corrected phase code
   */
  public String getCorrectedPhaseCode() {
    return correctedPhaseCode;
  }

  /**
   * Get the non-volatile branch data reference object
   *
   * @return A BranchDataReference object containing the non-volatile branch data reference object
   */
  public BranchDataReference getBranchReference() {
    return branchReference;
  }

  /**
   * Get the corrected travel time computed by the last call to computeOneRay.
   *
   * @return A double containing the corrected travel-time in seconds
   */
  public double getCorrectedTravelTime() {
    return correctedTravelTime;
  }

  /**
   * Function to set the branch compute causticFlags. Branches can be requested through the desired
   * phase list. The branch flags will need to be reset for each new session.
   *
   * @param shouldComputeTT A boolean causticFlags, if true compute travel times from this branch
   */
  public void setChouldComputeTT(boolean shouldComputeTT) {
    this.shouldComputeTT = shouldComputeTT;
  }

  /**
   * Set up volatile copies of data that changes with depth
   *
   * @param branchReference A BranchDataReference object containing the branch data reference object
   * @param upgoingPBranch An UpGoingDataVolume object holding the corrected P up-going branch
   *     source
   * @param upgoingSBranch An UpGoingDataVolume object holding the corrected S up-going branch
   *     source
   * @param modelConversions A ModelConversions object holding the model conversion factors and
   *     functions
   * @param auxTTReference An AuxiliaryTTReference containing the auxiliary travel-time data
   * @param splineRoutines A Spline object containing the spline routines
   */
  public BranchDataVolume(
      BranchDataReference branchReference,
      UpGoingDataVolume upgoingPBranch,
      UpGoingDataVolume upgoingSBranch,
      ModelConversions modelConversions,
      AuxiliaryTTReference auxTTReference,
      Spline splineRoutines) {
    this.branchReference = branchReference;
    this.upgoingPBranch = upgoingPBranch;
    this.upgoingSBranch = upgoingSBranch;
    this.modelConversions = modelConversions;
    this.auxTTReference = auxTTReference;
    this.splineRoutines = splineRoutines;

    // Do branch summary information.
    isPhaseUseless = auxTTReference.isUselessPhase(branchReference.getBranchPhaseCode());
    shouldComputeTT = true;
    correctedBranchExists = true;
    diffractedDistanceRange = new double[2];
  }

  /**
   * Function to correct this branch for source depth using the corrected up-going branch ray
   * parameters and tau values.
   *
   * @param sourceDepth A double containing the earth flattened, normalized source depth in
   *     kilometers
   * @param depthDerivativeNorm A double containing the correction factor for dT/dDepth
   * @param minimumDistance A double containing minimum source-receiver distance desired between ray
   *     parameter samples
   * @param branchSuffix A char holding the P or S up-going branch suffix
   * @throws TauIntegralException If the tau integral fails
   */
  public void correctDepth(
      double sourceDepth, double depthDerivativeNorm, double minimumDistance, char branchSuffix)
      throws TauIntegralException {
    int i;
    int len = 0;

    this.depthDerivativeNorm = depthDerivativeNorm;

    // Skip branches we aren't computing.
    if (shouldComputeTT) {
      // A surface source is a special case (i.e., no up-going phases).
      if (-sourceDepth <= TauUtilities.DOUBLETOLERANCE) {
        if (branchReference.getUpGoingCorrectionSign() < 0) {
          // This branch starts with a down-going ray.
          correctedBranchExists = true;

          // Do things common to all branches.
          correctedPhaseCode = branchReference.getBranchPhaseCode();
          correctedSlownessRange =
              Arrays.copyOf(
                  branchReference.getSlownessRange(), branchReference.getSlownessRange().length);
          correctedDistanceRange =
              Arrays.copyOf(
                  branchReference.getDistanceRange(), branchReference.getDistanceRange().length);
          correctedCausticSlowness = correctedSlownessRange[1];
          rayParameterEndValue = Double.NaN;
          ttFlags = auxTTReference.findPhaseFlags(correctedPhaseCode);

          // Make a local copy of the reference p and tau.
          len = branchReference.getSlownessGrid().length;
          updatedRayParameters = Arrays.copyOf(branchReference.getSlownessGrid(), len);
          correctedTauValues = Arrays.copyOf(branchReference.getTauGrid(), len);

          // Set up the diffracted range.
          if (branchReference.getBranchHasDiffraction()) {
            diffractedDistanceRange[0] =
                Math.max(correctedDistanceRange[0], correctedDistanceRange[1]);
            diffractedDistanceRange[1] = branchReference.getMaxDiffractedDistance();
          }

          // Spline it.
          interpolationPolynomials = new double[4][len];
          interpolatedDistanceValues = new double[len];

          splineRoutines.computeTauSpline(
              correctedTauValues,
              correctedDistanceRange,
              branchReference.getBasisCoefficients(),
              interpolationPolynomials,
              interpolatedDistanceValues);
        } else {
          // This branch starts with an up-going ray.
          correctedBranchExists = false;
          return;
        }

        // Otherwise, we need to correct the surface focus data for depth.
      } else {
        // Assume the branch exists until proven otherwise.
        correctedBranchExists = true;

        // Do things common to all branches.
        correctedPhaseCode = branchReference.getBranchPhaseCode();
        correctedSlownessRange =
            Arrays.copyOf(
                branchReference.getSlownessRange(), branchReference.getSlownessRange().length);
        correctedDistanceRange =
            Arrays.copyOf(
                branchReference.getDistanceRange(), branchReference.getDistanceRange().length);
        correctedCausticSlowness = correctedSlownessRange[1];
        rayParameterEndValue = Double.NaN;
        ttFlags = auxTTReference.findPhaseFlags(correctedPhaseCode);

        // Do phases that start as P.
        if (branchReference.getCorrectionPhaseType()[0] == 'P') {
          // Correct ray parameter range.
          double pMax = Math.min(correctedSlownessRange[1], upgoingPBranch.pMax);

          // Screen phases that don't exist.
          if (correctedSlownessRange[0] >= pMax) {
            correctedBranchExists = false;
            return;
          }

          correctedSlownessRange[1] = pMax;

          /* See how long we need the corrected arrays to be.  This is
           * awkward and not very Java-like, but the gain in performance
           * seemed worthwhile in this case.*/
          for (int j = 0; j < branchReference.getSlownessGrid().length; j++) {
            // See if we need this point.
            if (branchReference.getSlownessGrid()[j] < pMax + TauUtilities.DOUBLETOLERANCE) {
              len++;

              // If this point is equal to pMax, we're done.
              if (Math.abs(branchReference.getSlownessGrid()[j] - pMax)
                  <= TauUtilities.DOUBLETOLERANCE) {
                break;
              }
              // Otherwise, add one more point and quit.
            } else {
              len++;

              break;
            }
          }

          // If the branch is empty, it doesn't exist for this source
          // depth.
          if (len == 0) {
            correctedBranchExists = false;
            return;
          }

          // Otherwise, allocate the arrays.
          updatedRayParameters = new double[len];
          correctedTauValues = new double[len];

          // Correct an up-going branch separately.
          if (branchReference.getIsBranchUpGoing()) {
            // Correct the distance.
            correctedDistanceRange[1] = upgoingPBranch.xEndUp;

            // Correct tau for the up-going branch.
            // We assume that branchReference.getSlownessGrid() is a subset of upgoingPBranch.pUp
            // We assume that TauUtilities.DOUBLETOLERANCE is being used as a
            // float epsilon for floating point comparisions
            // We assume upgoingPBranch.pUp is the same lenght as upgoingPBranch.tauUp
            // We assume that branchReference.getSlownessGrid() and upgoingPBranch.pUp are sorted in
            // increasing order JMP 1/13/2022
            i = 0;
            for (int j = 0; j < branchReference.getSlownessGrid().length; j++) {
              // See if we need to correct this point.
              // Make sure we do not loop past the end of upgoingPBranch.pUp
              if ((branchReference.getSlownessGrid()[j] < pMax + TauUtilities.DOUBLETOLERANCE)
                  && (i < upgoingPBranch.pUp.length)) {
                // pTauUp is a superset of updatedRayParameters so we need to sync them.
                // advance through the upgoingPBranch.pUp array until we find the index
                // of upgoingPBranch.pUp that matches the value in
                // branchReference.getSlownessGrid()[j].
                // To make sure don't loop past the end of upgoingPBranch.pUp, we check
                // length-1 because of how the while loop is structured
                while ((Math.abs(branchReference.getSlownessGrid()[j] - upgoingPBranch.pUp[i])
                        > TauUtilities.DOUBLETOLERANCE)
                    && (i < upgoingPBranch.pUp.length - 1)) {
                  i++;
                }

                // Correct the tau and x values.
                updatedRayParameters[j] = branchReference.getSlownessGrid()[j];
                correctedTauValues[j] = upgoingPBranch.tauUp[i];

                // If this point is equal to pMax, we're done.
                if (Math.abs(branchReference.getSlownessGrid()[j] - pMax)
                    <= TauUtilities.DOUBLETOLERANCE) {
                  break;
                }
              } else {
                // Otherwise, (we've hit the max, or run out of
                // elements in pTauUp)
                // we add one more point and quit.
                updatedRayParameters[j] = pMax;
                correctedTauValues[j] = upgoingPBranch.tauEndUp;

                break;
              }
            }

            // Decimate the up-going branch.
            updatedRayParameters =
                upgoingPBranch.realUp(
                    updatedRayParameters,
                    correctedTauValues,
                    correctedDistanceRange,
                    minimumDistance);
            correctedTauValues = upgoingPBranch.getDecTau();

            // Spline it.
            len = updatedRayParameters.length;
            double[][] basisTmp = new double[5][len];
            splineRoutines.constuctBasisFunctions(updatedRayParameters, basisTmp);
            interpolationPolynomials = new double[4][len];
            interpolatedDistanceValues = new double[len];

            splineRoutines.computeTauSpline(
                correctedTauValues,
                correctedDistanceRange,
                basisTmp,
                interpolationPolynomials,
                interpolatedDistanceValues);

            // Otherwise, correct a down-going branch.
          } else {
            // Correct distance.
            int m = 0;
            for (i = 0; i < correctedDistanceRange.length; i++) {
              for (; m < upgoingPBranch.ref.pXUp.length; m++) {
                if (Math.abs(correctedSlownessRange[i] - upgoingPBranch.ref.pXUp[m])
                    <= TauUtilities.DOUBLETOLERANCE) {
                  if (m >= upgoingPBranch.xUp.length) {
                    correctedBranchExists = false;
                    return;
                  }

                  correctedDistanceRange[i] +=
                      branchReference.getUpGoingCorrectionSign() * upgoingPBranch.xUp[m];
                  break;
                }
              }

              if (m >= upgoingPBranch.ref.pXUp.length) {
                correctedDistanceRange[i] = computeLastDistance();
              }
            }

            // Set up the diffracted branch distance range.
            if (branchReference.getBranchHasDiffraction()) {
              diffractedDistanceRange[0] = correctedDistanceRange[0];
              diffractedDistanceRange[1] = branchReference.getMaxDiffractedDistance();
            }

            // Correct tau for down-going branches.
            // We assume that branchReference.getSlownessGrid() is a subset of upgoingPBranch.pUp
            // We assume that TauUtilities.DOUBLETOLERANCE is being used as a
            // float epsilon for floating point comparisions
            // We assume upgoingPBranch.pUp is the same lenght as upgoingPBranch.tauUp
            // We assume that branchReference.getSlownessGrid() and upgoingPBranch.pUp are sorted in
            // increasing order JMP 1/13/2022
            i = 0;
            for (int j = 0; j < branchReference.getSlownessGrid().length; j++) {
              // See if we need to correct this point.
              // Make sure we do not loop past the end of upgoingPBranch.pUp
              if ((branchReference.getSlownessGrid()[j] < pMax + TauUtilities.DOUBLETOLERANCE)
                  && (i < upgoingPBranch.pUp.length)) {
                // pTauUp is a superset of updatedRayParameters so we need to sync them.
                // advance through the upgoingPBranch.pUp array until we find the index
                // of upgoingPBranch.pUp that matches the value in
                // branchReference.getSlownessGrid()[j].
                // To make sure don't loop past the end of upgoingPBranch.pUp, we check
                // length-1 because of how the while loop is structured
                while ((Math.abs(branchReference.getSlownessGrid()[j] - upgoingPBranch.pUp[i])
                        > TauUtilities.DOUBLETOLERANCE)
                    && (i < upgoingPBranch.pUp.length - 1)) {
                  i++;
                }

                // Correct the tau and x values.
                updatedRayParameters[j] = branchReference.getSlownessGrid()[j];
                correctedTauValues[j] =
                    branchReference.getTauGrid()[j]
                        + branchReference.getUpGoingCorrectionSign() * upgoingPBranch.tauUp[i];

                // If this point is equal to pMax, we're done.
                if (Math.abs(branchReference.getSlownessGrid()[j] - pMax)
                    <= TauUtilities.DOUBLETOLERANCE) {
                  break;
                }
              } else {
                // Otherwise, (we've hit the max, or run out of
                // elements in pTauUp)
                // we add one more point and quit.
                updatedRayParameters[j] = pMax;
                correctedTauValues[j] = computeLastTau();

                break;
              }
            }

            // Spline it.
            interpolationPolynomials = new double[4][len];
            interpolatedDistanceValues = new double[len];

            if (Math.abs(correctedSlownessRange[1] - branchReference.getSlownessRange()[1])
                <= TauUtilities.DOUBLETOLERANCE) {
              splineRoutines.computeTauSpline(
                  correctedTauValues,
                  correctedDistanceRange,
                  branchReference.getBasisCoefficients(),
                  interpolationPolynomials,
                  interpolatedDistanceValues);
            } else {
              double[][] basisTmp = new double[5][len];
              splineRoutines.constuctBasisFunctions(updatedRayParameters, basisTmp);
              splineRoutines.computeTauSpline(
                  correctedTauValues,
                  correctedDistanceRange,
                  basisTmp,
                  interpolationPolynomials,
                  interpolatedDistanceValues);
            }
          }
          // Do phases that start as S.
        } else {
          // Correct ray parameter range.
          double pMax = Math.min(correctedSlownessRange[1], upgoingSBranch.pMax);

          // Screen phases that don't exist.
          if (correctedSlownessRange[0] >= pMax) {
            correctedBranchExists = false;
            return;
          }

          correctedSlownessRange[1] = pMax;

          /* See how long we need the corrected arrays to be.  This is
           * awkward and not very Java-like, but the gain in performance
           * seemed worthwhile in this case.*/
          for (int j = 0; j < branchReference.getSlownessGrid().length; j++) {
            // See if we need this point.
            if (branchReference.getSlownessGrid()[j] < pMax + TauUtilities.DOUBLETOLERANCE) {
              len++;

              // If this point is equal to pMax, we're done.
              if (Math.abs(branchReference.getSlownessGrid()[j] - pMax)
                  <= TauUtilities.DOUBLETOLERANCE) {
                break;
              }
              // Otherwise, add one more point and quit.
            } else {
              len++;
              break;
            }
          }

          // If the branch is empty, it doesn't exist for this source
          // depth.
          if (len == 0) {
            correctedBranchExists = false;
            return;
          }

          // Otherwise, allocate the arrays.
          updatedRayParameters = new double[len];
          correctedTauValues = new double[len];

          // Correct an up-going branch separately.
          if (branchReference.getIsBranchUpGoing()) {
            // Correct the distance.
            correctedDistanceRange[1] = upgoingSBranch.xEndUp;

            // Correct tau for the up-going branch.
            // We assume that branchReference.getSlownessGrid() is a subset of upgoingSBranch.pUp
            // We assume that TauUtilities.DOUBLETOLERANCE is being used as a
            // float epsilon for floating point comparisions
            // We assume upgoingSBranch.pUp is the same lenght as upgoingSBranch.tauUp
            // We assume that branchReference.getSlownessGrid() and upgoingSBranch.pUp are sorted in
            // increasing order JMP 1/13/2022
            i = 0;
            for (int j = 0; j < branchReference.getSlownessGrid().length; j++) {
              // See if we need to correct this point.
              // Make sure we do not loop past the end of upgoingSBranch.pUp
              if ((branchReference.getSlownessGrid()[j] < pMax + TauUtilities.DOUBLETOLERANCE)
                  && (i < upgoingSBranch.pUp.length)) {
                // pTauUp is a superset of updatedRayParameters so we need to sync them.
                // advance through the upgoingSBranch.pUp array until we find the index
                // of upgoingSBranch.pUp that matches the value in
                // branchReference.getSlownessGrid()[j].
                // To make sure don't loop past the end of upgoingSBranch.pUp, we check
                // length-1 because of how the while loop is structured
                while ((Math.abs(branchReference.getSlownessGrid()[j] - upgoingSBranch.pUp[i])
                        > TauUtilities.DOUBLETOLERANCE)
                    && (i < upgoingSBranch.pUp.length - 1)) {
                  i++;
                }

                // Correct the tau and x values.
                updatedRayParameters[j] = branchReference.getSlownessGrid()[j];
                correctedTauValues[j] = upgoingSBranch.tauUp[i];

                // If this point is equal to pMax, we're done.
                if (Math.abs(branchReference.getSlownessGrid()[j] - pMax)
                    <= TauUtilities.DOUBLETOLERANCE) {
                  break;
                }
              } else {
                // Otherwise, (we've hit the max, or run out of
                // elements in pTauUp)
                // we add one more point and quit.
                updatedRayParameters[j] = pMax;
                correctedTauValues[j] = upgoingSBranch.tauEndUp;

                break;
              }
            }

            // Decimate the up-going branch.
            updatedRayParameters =
                upgoingSBranch.realUp(
                    updatedRayParameters,
                    correctedTauValues,
                    correctedDistanceRange,
                    minimumDistance);
            correctedTauValues = upgoingSBranch.getDecTau();

            // Spline it.
            len = updatedRayParameters.length;
            double[][] basisTmp = new double[5][len];
            splineRoutines.constuctBasisFunctions(updatedRayParameters, basisTmp);
            interpolationPolynomials = new double[4][len];
            interpolatedDistanceValues = new double[len];

            splineRoutines.computeTauSpline(
                correctedTauValues,
                correctedDistanceRange,
                basisTmp,
                interpolationPolynomials,
                interpolatedDistanceValues);

            // Otherwise, correct a down-going branch.
          } else {
            // Correct distance.
            int m = 0;
            for (i = 0; i < correctedDistanceRange.length; i++) {
              for (; m < upgoingSBranch.ref.pXUp.length; m++) {
                if (Math.abs(correctedSlownessRange[i] - upgoingSBranch.ref.pXUp[m])
                    <= TauUtilities.DOUBLETOLERANCE) {
                  if (m >= upgoingSBranch.xUp.length) {
                    correctedBranchExists = false;
                    return;
                  }

                  correctedDistanceRange[i] +=
                      branchReference.getUpGoingCorrectionSign() * upgoingSBranch.xUp[m];
                  break;
                }
              }
              if (m >= upgoingSBranch.ref.pXUp.length) {
                correctedDistanceRange[i] = computeLastDistance();
              }
            }

            // Set up the diffracted branch distance range.
            if (branchReference.getBranchHasDiffraction()) {
              diffractedDistanceRange[0] = correctedDistanceRange[0];
              diffractedDistanceRange[1] = branchReference.getMaxDiffractedDistance();
            }

            // Correct tau for down-going branches.
            // We assume that branchReference.getSlownessGrid() is a subset of upgoingSBranch.pUp
            // We assume that TauUtilities.DOUBLETOLERANCE is being used as a
            // float epsilon for floating point comparisions
            // We assume upgoingSBranch.pUp is the same lenght as upgoingSBranch.tauUp
            // We assume that branchReference.getSlownessGrid() and upgoingSBranch.pUp are sorted in
            // increasing order JMP 1/13/2022
            i = 0;
            for (int j = 0; j < branchReference.getSlownessGrid().length; j++) {
              // See if we need to correct this point.
              // Make sure we do not loop past the end of upgoingSBranch.pUp
              if ((branchReference.getSlownessGrid()[j] < pMax + TauUtilities.DOUBLETOLERANCE)
                  && (i < upgoingSBranch.pUp.length)) {
                // pTauUp is a superset of updatedRayParameters so we need to sync them.
                // advance through the upgoingSBranch.pUp array until we find the index
                // of upgoingSBranch.pUp that matches the value in
                // branchReference.getSlownessGrid()[j].
                // To make sure don't loop past the end of upgoingSBranch.pUp, we check
                // length-1 because of how the while loop is structured
                while ((Math.abs(branchReference.getSlownessGrid()[j] - upgoingSBranch.pUp[i])
                        > TauUtilities.DOUBLETOLERANCE)
                    && (i < upgoingSBranch.pUp.length - 1)) {
                  i++;
                }

                // Correct the tau and x values.
                updatedRayParameters[j] = branchReference.getSlownessGrid()[j];
                correctedTauValues[j] =
                    branchReference.getTauGrid()[j]
                        + branchReference.getUpGoingCorrectionSign() * upgoingSBranch.tauUp[i];

                // If this point is equal to pMax, we're done.
                if (Math.abs(branchReference.getSlownessGrid()[j] - pMax)
                    <= TauUtilities.DOUBLETOLERANCE) {
                  break;
                }
              } else {
                // Otherwise, (we've hit the max, or run out of
                // elements in pTauUp)
                // we add one more point and quit.
                updatedRayParameters[j] = pMax;
                correctedTauValues[j] = computeLastTau();

                break;
              }
            }

            // Spline it.
            interpolationPolynomials = new double[4][len];
            interpolatedDistanceValues = new double[len];

            if (Math.abs(correctedSlownessRange[1] - branchReference.getSlownessRange()[1])
                <= TauUtilities.DOUBLETOLERANCE) {
              splineRoutines.computeTauSpline(
                  correctedTauValues,
                  correctedDistanceRange,
                  branchReference.getBasisCoefficients(),
                  interpolationPolynomials,
                  interpolatedDistanceValues);
            } else {
              double[][] basisTmp = new double[5][len];
              splineRoutines.constuctBasisFunctions(updatedRayParameters, basisTmp);
              splineRoutines.computeTauSpline(
                  correctedTauValues,
                  correctedDistanceRange,
                  basisTmp,
                  interpolationPolynomials,
                  interpolatedDistanceValues);
            }
          }
        }
      }

      // Complete everything we'll need to compute a travel time.
      distanceLimits = new double[2][len - 1];
      causticFlags = new String[len - 1];
      computeTauPolynomial(branchSuffix);

      // Now that we know what type of phase we have, select the right
      // statistics and ellipticity correction.
      if (branchReference.getIsBranchUpGoing()) {
        ttStatistics = auxTTReference.findPhaseStatistics(correctedPhaseCode);
        ellipticityCorrections = auxTTReference.findEllipticity(ttFlags.getGroupPhaseCode() + "up");
      } else {
        ttStatistics = ttFlags.getPhaseStatistics();
        ellipticityCorrections = ttFlags.getEllipticityCorrections();
      }

      // Un-computed phases might as well not exist.
    } else {
      correctedBranchExists = false;
    }
  }

  /**
   * Function to compute tau value corresponding to the largest ray parameter (usually the slowness
   * at the source) it is computed from the end integrals computed as part of the up-going branch
   * corrections.
   *
   * @return Normalized tau for the maximum ray parameter for this branch
   */
  private double computeLastTau() {
    double tau;

    if (branchReference.getCorrectionPhaseType()[0] == 'P') {
      // Add or subtract the up-going piece.  For a surface reflection
      // it would be added.  For a down-going branch it would be
      // subtracted (because that part of the branch is cut off by the
      // source depth).
      tau = branchReference.getUpGoingCorrectionSign() * upgoingPBranch.tauEndUp;

      // Add the down-going part, which may not be the same as the
      // up-going piece (e.g., sP).
      if (branchReference.getCorrectionPhaseType()[1] == 'P') {
        tau +=
            branchReference.getNumMantleTraversals()
                * (upgoingPBranch.tauEndUp + upgoingPBranch.tauEndLvz);
      } else {
        tau += branchReference.getNumMantleTraversals() * (upgoingPBranch.tauEndCnv);
      }

      // Add the coming-back-up part, which may not be the same as the
      // down-going piece (e.g., ScP).
      if (branchReference.getCorrectionPhaseType()[2] == 'P') {
        tau +=
            branchReference.getNumMantleTraversals()
                * (upgoingPBranch.tauEndUp + upgoingPBranch.tauEndLvz);
      } else {
        tau += branchReference.getNumMantleTraversals() * (upgoingPBranch.tauEndCnv);
      }
    } else {
      // Add or subtract the up-going piece.  For a surface reflection
      // it would be added.  For a down-going branch it would be
      // subtracted (because that part of the branch is cut off by the
      // source depth).
      tau = branchReference.getUpGoingCorrectionSign() * upgoingSBranch.tauEndUp;

      // Add the down-going part, which may not be the same as the
      // up-going piece (e.g., sP).
      if (branchReference.getCorrectionPhaseType()[1] == 'S') {
        tau +=
            branchReference.getNumMantleTraversals()
                * (upgoingSBranch.tauEndUp + upgoingSBranch.tauEndLvz);
      } else {
        tau += branchReference.getNumMantleTraversals() * (upgoingSBranch.tauEndCnv);
      }

      // Add the coming-back-up part, which may not be the same as the
      // down-going piece (e.g., ScP).
      if (branchReference.getCorrectionPhaseType()[2] == 'S') {
        tau +=
            branchReference.getNumMantleTraversals()
                * (upgoingSBranch.tauEndUp + upgoingSBranch.tauEndLvz);
      } else {
        tau += branchReference.getNumMantleTraversals() * (upgoingSBranch.tauEndCnv);
      }
    }

    return tau;
  }

  /**
   * Function to compute the distance value corresponding to the largest ray parameter (usually the
   * slowness at the source) it is computed from the end integrals computed as part of the up-going
   * branch corrections.
   *
   * @return Normalized distance for the maximum ray parameter for this branch
   */
  private double computeLastDistance() {
    double distance;

    if (branchReference.getCorrectionPhaseType()[0] == 'P') {
      // Add or subtract the up-going piece.  For a surface reflection
      // it would be added.  For a down-going branch it would be
      // subtracted (because that part of the branch is cut off by the
      // source depth).
      distance = branchReference.getUpGoingCorrectionSign() * upgoingPBranch.xEndUp;

      // Add the down-going part, which may not be the same as the
      // up-going piece (e.g., sP).
      if (branchReference.getCorrectionPhaseType()[1] == 'P') {
        distance +=
            branchReference.getNumMantleTraversals()
                * (upgoingPBranch.xEndUp + upgoingPBranch.xEndLvz);
      } else {
        distance += branchReference.getNumMantleTraversals() * (upgoingPBranch.xEndCnv);
      }

      // Add the coming-back-up part, which may not be the same as the
      // down-going piece (e.g., ScP).
      if (branchReference.getCorrectionPhaseType()[2] == 'P') {
        distance +=
            branchReference.getNumMantleTraversals()
                * (upgoingPBranch.xEndUp + upgoingPBranch.xEndLvz);
      } else {
        distance += branchReference.getNumMantleTraversals() * (upgoingPBranch.xEndCnv);
      }
    } else {
      // Add or subtract the up-going piece.  For a surface reflection
      // it would be added.  For a down-going branch it would be
      // subtracted (because that part of the branch is cut off by the
      // source depth).
      distance = branchReference.getUpGoingCorrectionSign() * upgoingSBranch.xEndUp;

      // Add the down-going part, which may not be the same as the
      // up-going piece (e.g., sP).
      if (branchReference.getCorrectionPhaseType()[1] == 'S') {
        distance +=
            branchReference.getNumMantleTraversals()
                * (upgoingSBranch.xEndUp + upgoingSBranch.xEndLvz);
      } else {
        distance += branchReference.getNumMantleTraversals() * (upgoingSBranch.xEndCnv);
      }

      // Add the coming-back-up part, which may not be the same as the
      // down-going piece (e.g., ScP).
      if (branchReference.getCorrectionPhaseType()[2] == 'S') {
        distance +=
            branchReference.getNumMantleTraversals()
                * (upgoingSBranch.xEndUp + upgoingSBranch.xEndLvz);
      } else {
        distance += branchReference.getNumMantleTraversals() * (upgoingSBranch.xEndCnv);
      }
    }

    return distance;
  }

  /**
   * Function that computes the final spline interpolation polynomial using tau and distance (from
   * tauSpline),
   *
   * @param branchSuffix A char indicating the up-going P and S branch suffix
   */
  private void computeTauPolynomial(char branchSuffix) {
    // Fill in the rest of the interpolation polynomial.  Note that
    // distance will be overwritten with the linear polynomial
    // coefficient.
    int n = updatedRayParameters.length;
    double rayParameterEndValue = updatedRayParameters[n - 1];
    double[] dpe = new double[2];
    double[] sqrtDp = new double[2];
    double[] sqrt3Dp = new double[2];
    dpe[1] = rayParameterEndValue - updatedRayParameters[0];
    sqrtDp[1] = Math.sqrt(dpe[1]);
    sqrt3Dp[1] = dpe[1] * sqrtDp[1];

    // Set up variables for tracking caustics.
    minimumCausticCount = 0;
    maximumCausticCount = 0;
    correctedCausticSlowness = updatedRayParameters[n - 1];
    double minimumDistance = correctedDistanceRange[0];
    double maximumDistance = minimumDistance;

    // Loop over ray parameter intervals.
    for (int j = 0; j < n - 1; j++) {
      // Complete the interpolation polynomial.
      dpe[0] = dpe[1];
      sqrtDp[0] = sqrtDp[1];
      sqrt3Dp[0] = sqrt3Dp[1];
      dpe[1] = rayParameterEndValue - updatedRayParameters[j + 1];
      sqrtDp[1] = Math.sqrt(dpe[1]);
      sqrt3Dp[1] = dpe[1] * sqrtDp[1];
      double dp = updatedRayParameters[j] - updatedRayParameters[j + 1];
      double dtau = correctedTauValues[j + 1] - correctedTauValues[j];
      interpolationPolynomials[3][j] =
          (2d * dtau - dp * (interpolatedDistanceValues[j + 1] + interpolatedDistanceValues[j]))
              / (0.5d * (sqrt3Dp[1] - sqrt3Dp[0])
                  - 1.5d * sqrtDp[1] * sqrtDp[0] * (sqrtDp[1] - sqrtDp[0]));
      interpolationPolynomials[2][j] =
          (dtau
                  - dp * interpolatedDistanceValues[j]
                  - (sqrt3Dp[1] + 0.5d * sqrt3Dp[0] - 1.5d * dpe[1] * sqrtDp[0])
                      * interpolationPolynomials[3][j])
              / Math.pow(dp, 2d);
      interpolationPolynomials[1][j] =
          (dtau
                  - (Math.pow(dpe[1], 2d) - Math.pow(dpe[0], 2d)) * interpolationPolynomials[2][j]
                  - (sqrt3Dp[1] - sqrt3Dp[0]) * interpolationPolynomials[3][j])
              / dp;
      interpolationPolynomials[0][j] =
          correctedTauValues[j]
              - sqrt3Dp[0] * interpolationPolynomials[3][j]
              - dpe[0] * (dpe[0] * interpolationPolynomials[2][j] + interpolationPolynomials[1][j]);

      // Set up the distance limits.
      distanceLimits[0][j] =
          Math.min(interpolatedDistanceValues[j], interpolatedDistanceValues[j + 1]);
      distanceLimits[1][j] =
          Math.max(interpolatedDistanceValues[j], interpolatedDistanceValues[j + 1]);
      if (distanceLimits[0][j] < minimumDistance) {
        minimumDistance = distanceLimits[0][j];
        if (interpolatedDistanceValues[j] <= interpolatedDistanceValues[j + 1]) {
          correctedCausticSlowness = updatedRayParameters[j];
        } else {
          correctedCausticSlowness = updatedRayParameters[j + 1];
        }
      }

      // See if there's a caustic in this interval.
      causticFlags[j] = "";
      if (Math.abs(interpolationPolynomials[2][j]) > TauUtilities.MINIMUMDOUBLE) {
        double sqrtPext = -0.375d * interpolationPolynomials[3][j] / interpolationPolynomials[2][j];
        double pExt = Math.pow(sqrtPext, 2d);

        if (sqrtPext > 0d && pExt > dpe[1] && pExt < dpe[0]) {
          double xCaustic =
              interpolationPolynomials[1][j]
                  + sqrtPext
                      * (2d * sqrtPext * interpolationPolynomials[2][j]
                          + 1.5d * interpolationPolynomials[3][j]);
          distanceLimits[0][j] = Math.min(distanceLimits[0][j], xCaustic);
          distanceLimits[1][j] = Math.max(distanceLimits[1][j], xCaustic);

          if (xCaustic < minimumDistance) {
            minimumDistance = xCaustic;
            correctedCausticSlowness = rayParameterEndValue - pExt;
          }

          if (interpolationPolynomials[3][j] < 0d) {
            causticFlags[j] = "min";
            minimumCausticCount++;
          } else {
            causticFlags[j] = "max";
            maximumCausticCount++;
          }
        }
      }

      maximumDistance = Math.max(maximumDistance, distanceLimits[1][j]);
    }

    // Fix ranges.
    correctedDistanceRange[0] = minimumDistance;
    correctedDistanceRange[1] = maximumDistance;

    // Set the distances to try (see findTt for details).
    numTTDistanceRanges = new int[2];
    for (int j = 0; j < 2; j++) {
      if (correctedDistanceRange[j] <= Math.PI) {
        numTTDistanceRanges[j] = 0;
      } else if (correctedDistanceRange[j] <= 2d * Math.PI) {
        numTTDistanceRanges[j] = 1;
      } else {
        numTTDistanceRanges[j] = 2;
      }
    }

    // Fix the phase code for the up-going branch.
    if (branchReference.getIsBranchUpGoing() && branchSuffix != ' ') {
      correctedPhaseCode = "" + correctedPhaseCode.charAt(0) + branchSuffix;
    }
  }

  /**
   * Function to get the travel times for this branch. Three different distances must to be
   * processed, but this needs to be driven from AllBranchVolume so that the surface focus distance
   * correction can be applied for the auxiliary data, hence the need for depthIndex.
   *
   * @param depthIndex A double containing the depth index (there are three possible depths)
   * @param desiredDistance A double containing the desired distance in radians
   * @param sourceDepth A double containing the source depth in kilometers
   * @param returnAllPhases A boolean flag, if false only return potentially useful phases
   * @param travelTimeList A TravelTime object containing the list of travel times to be filled in
   */
  public void getTravelTimes(
      int depthIndex,
      double desiredDistance,
      double sourceDepth,
      boolean returnAllPhases,
      TravelTime travelTimeList) {
    boolean found = false;

    // Skip non-existent and useless phases (if requested).
    if (!correctedBranchExists || (!returnAllPhases && isPhaseUseless)) {
      return;
    }

    // On the first index, set up the conversion for dT/dDelta.
    if (depthIndex == 0)
      distanceSign = modelConversions.get_dTdDelta() * Math.pow(-1d, numTTDistanceRanges[0] + 1);

    // Loop over possible distances.
    if (depthIndex >= numTTDistanceRanges[0] && depthIndex <= numTTDistanceRanges[1]) {
      distanceSign = -distanceSign;

      // See if we have an arrival at this distance.
      if (desiredDistance >= correctedDistanceRange[0]
          && desiredDistance <= correctedDistanceRange[1]) {
        // Set up some useful variables.
        if (Double.isNaN(rayParameterEndValue)) {
          rayParameterEndValue = updatedRayParameters[updatedRayParameters.length - 1];
          depthSign = depthDerivativeNorm * branchReference.getUpGoingCorrectionSign();

          if (branchReference.getCorrectionPhaseType()[0] == 'P') {
            slownessUpSquared = Math.pow(upgoingPBranch.pSource, 2d);
          } else {
            slownessUpSquared = Math.pow(upgoingSBranch.pSource, 2d);
          }
        }

        // Loop over ray parameter intervals looking for arrivals.
        for (int j = 0; j < distanceLimits[0].length; j++) {
          if (desiredDistance > distanceLimits[0][j] && desiredDistance <= distanceLimits[1][j]) {
            // pTol is a totally empirically tolerance.
            double pTol =
                Math.max(3e-6d * (updatedRayParameters[j + 1] - updatedRayParameters[j]), 1e-4d);

            // This is the general case.
            if (Math.abs(interpolationPolynomials[2][j]) > TauUtilities.MINIMUMDOUBLE) {
              // There should be two solutions.
              double dps =
                  -(3d * interpolationPolynomials[3][j]
                          + Math.copySign(
                              Math.sqrt(
                                  Math.abs(
                                      9d * Math.pow(interpolationPolynomials[3][j], 2d)
                                          + 32d
                                              * interpolationPolynomials[2][j]
                                              * (desiredDistance
                                                  - interpolationPolynomials[1][j]))),
                              interpolationPolynomials[3][j]))
                      / (8d * interpolationPolynomials[2][j]);

              for (int k = 0; k < 2; k++) {
                if (k > 0) {
                  dps =
                      (interpolationPolynomials[1][j] - desiredDistance)
                          / (2d * interpolationPolynomials[2][j] * dps);
                }

                double dp = Math.copySign(Math.pow(dps, 2d), dps);

                // Arrivals outside the interval aren't real.
                if (dp >= Math.max(rayParameterEndValue - updatedRayParameters[j + 1] - pTol, 0d)
                    && dp <= rayParameterEndValue - updatedRayParameters[j] + pTol) {
                  // Add the arrival.
                  found = true;
                  double ps = rayParameterEndValue - dp;

                  // Fiddle the phase code for bc branches.
                  String tmpCode;
                  if (correctedPhaseCode.contains("ab") && ps <= correctedCausticSlowness) {
                    tmpCode = TauUtilities.createSegmentCode(correctedPhaseCode) + "bc";
                  } else {
                    tmpCode = correctedPhaseCode;
                  }

                  // Add it.
                  travelTimeList.addPhase(
                      tmpCode,
                      branchReference.getUniquePhaseCodes(),
                      modelConversions.getTauTTNormalization()
                          * (interpolationPolynomials[0][j]
                              + dp
                                  * (interpolationPolynomials[1][j]
                                      + dp * interpolationPolynomials[2][j]
                                      + dps * interpolationPolynomials[3][j])
                              + ps * desiredDistance),
                      distanceSign * ps,
                      depthSign * Math.sqrt(Math.abs(slownessUpSquared - Math.pow(ps, 2d))),
                      -(2d * interpolationPolynomials[2][j]
                              + 0.75d
                                  * interpolationPolynomials[3][j]
                                  / Math.max(Math.abs(dps), TauUtilities.DOUBLETOLERANCE))
                          / modelConversions.getTauTTNormalization(),
                      false);
                }
              }
              // We have to be careful if the quadratic term is zero.
            } else {
              // On the plus side, there's only one solution.
              double dps =
                  (desiredDistance - interpolationPolynomials[1][j])
                      / (1.5d * interpolationPolynomials[3][j]);
              double dp = Math.copySign(Math.pow(dps, 2d), dps);

              //	System.out.println("Sol spec: "+(float)dp);
              // Arrivals outside the interval aren't real.
              if (dp < rayParameterEndValue - updatedRayParameters[j + 1] - pTol
                  || dp > rayParameterEndValue - updatedRayParameters[j] + pTol) {
                break;
              }

              // Add the arrival.
              found = true;
              double ps = rayParameterEndValue - dp;

              // Fiddle the phase code for bc branches.
              String tmpCode;
              if (correctedPhaseCode.contains("ab") && ps <= correctedCausticSlowness) {
                tmpCode = TauUtilities.createSegmentCode(correctedPhaseCode) + "bc";
              } else {
                tmpCode = correctedPhaseCode;
              }

              // add it.
              travelTimeList.addPhase(
                  tmpCode,
                  branchReference.getUniquePhaseCodes(),
                  modelConversions.getTauTTNormalization()
                      * (interpolationPolynomials[0][j]
                          + dp
                              * (interpolationPolynomials[1][j]
                                  + dps * interpolationPolynomials[3][j])
                          + ps * desiredDistance),
                  distanceSign * ps,
                  depthSign * Math.sqrt(Math.abs(slownessUpSquared - Math.pow(ps, 2d))),
                  -(0.75d
                          * interpolationPolynomials[3][j]
                          / Math.max(Math.abs(dps), TauUtilities.DOUBLETOLERANCE))
                      / modelConversions.getTauTTNormalization(),
                  false);
            }
          }
        }
      }

      // See if we have a diffracted arrival.
      if (branchReference.getBranchHasDiffraction()) {
        if (desiredDistance > diffractedDistanceRange[0]
            && desiredDistance <= diffractedDistanceRange[1]) {
          // This would have gotten missed as it's off the end of the branch.
          if (Double.isNaN(rayParameterEndValue)) {
            rayParameterEndValue = updatedRayParameters[updatedRayParameters.length - 1];
            depthSign = depthDerivativeNorm * branchReference.getUpGoingCorrectionSign();
            if (branchReference.getCorrectionPhaseType()[0] == 'P') {
              slownessUpSquared = Math.pow(upgoingPBranch.pSource, 2d);
            } else {
              slownessUpSquared = Math.pow(upgoingSBranch.pSource, 2d);
            }
          }

          double dp = correctedSlownessRange[1] - correctedSlownessRange[0];
          double dps = Math.sqrt(Math.abs(dp));

          // Fiddle the unique code.
          if (uniquePhaseCode == null) {
            uniquePhaseCode = new String[2];
          }

          uniquePhaseCode[0] = branchReference.getDiffractedPhaseCode() + 0;
          uniquePhaseCode[1] = null;

          // Add it.
          travelTimeList.addPhase(
              branchReference.getDiffractedPhaseCode(),
              uniquePhaseCode,
              modelConversions.getTauTTNormalization()
                  * (interpolationPolynomials[0][0]
                      + dp
                          * (interpolationPolynomials[1][0]
                              + dp * interpolationPolynomials[2][0]
                              + dps * interpolationPolynomials[3][0])
                      + correctedSlownessRange[0] * desiredDistance),
              distanceSign * correctedSlownessRange[0],
              depthSign
                  * Math.sqrt(
                      Math.abs(slownessUpSquared - Math.pow(correctedSlownessRange[0], 2d))),
              -(2d * interpolationPolynomials[2][0]
                      + 0.75d
                          * interpolationPolynomials[3][0]
                          / Math.max(Math.abs(dps), TauUtilities.DOUBLETOLERANCE))
                  / modelConversions.getTauTTNormalization(),
              false);
        }
      }

      // See if we have an add-on phase.
      if (branchReference.getBranchHasAddOn() && found) {
        TravelTimeFlags addFlags =
            auxTTReference.findPhaseFlags(branchReference.getAddOnPhaseCode());

        if (addFlags.getPhaseStatistics() != null) {
          double distance = Math.toDegrees(desiredDistance);

          if (distance >= addFlags.getPhaseStatistics().minDelta
              && distance <= addFlags.getPhaseStatistics().maxDelta) {
            // Fiddle the unique code.
            if (uniquePhaseCode == null) {
              uniquePhaseCode = new String[2];
            }

            uniquePhaseCode[0] = branchReference.getAddOnPhaseCode() + 0;
            uniquePhaseCode[1] = null;

            // See what we've got.
            if (branchReference.getAddOnPhaseCode().equals("Lg")) {
              // Make sure we have a valid depth.
              if (sourceDepth <= TauUtilities.LGMAXIMUMDEPTH) {
                travelTimeList.addPhase(
                    branchReference.getAddOnPhaseCode(),
                    uniquePhaseCode,
                    0d,
                    modelConversions.get_dTdDelta_Lg(),
                    0d,
                    0d,
                    true);
              }
            } else if (branchReference.getAddOnPhaseCode().equals("LR")) {
              // Make sure we have a valid depth and distance.
              if (sourceDepth <= TauUtilities.LRMAXIMUMDEPTH
                  && desiredDistance <= TauUtilities.LRMAXIMUMDISTANCE) {
                travelTimeList.addPhase(
                    branchReference.getAddOnPhaseCode(),
                    uniquePhaseCode,
                    0d,
                    modelConversions.get_dTdDelta_LR(),
                    0d,
                    0d,
                    true);
              }
            } else if (branchReference.getAddOnPhaseCode().equals("pwP")
                || branchReference.getAddOnPhaseCode().equals("PKPpre")) {
              TravelTimeData travelTime =
                  travelTimeList.getPhase(travelTimeList.getNumPhases() - 1);
              travelTimeList.addPhase(
                  branchReference.getAddOnPhaseCode(),
                  uniquePhaseCode,
                  travelTime.getTravelTime(),
                  travelTime.getDistanceDerivitive(),
                  travelTime.getDepthDerivitive(),
                  travelTime.getRayDerivative(),
                  true);
            }
          }
        }
      }
    }
  }

  /**
   * Function to compute the surface focus source-receiver distance and travel time. Note that for
   * up-going branches the calculation is from the source depth.
   *
   * @param desiredRayParam A double containing the desired ray parameter in seconds/degree
   * @return A double containing the source-receiver distance in degrees
   */
  public double computeOneRay(double desiredRayParam) {
    double xCorr;
    double ps = Math.abs(desiredRayParam) / modelConversions.get_dTdDelta();

    // Check validity.
    if (!correctedBranchExists
        || ps < correctedSlownessRange[0]
        || ps > correctedSlownessRange[1]) {
      return Double.NaN;
    }

    for (int j = 1; j < updatedRayParameters.length; j++) {
      if (ps <= updatedRayParameters[j]) {
        double dp = updatedRayParameters[updatedRayParameters.length - 1] - ps;
        double dps = Math.sqrt(Math.abs(dp));
        xCorr =
            interpolationPolynomials[1][j - 1]
                + 2d * dp * interpolationPolynomials[2][j - 1]
                + 1.5d * dps * interpolationPolynomials[3][j - 1];
        correctedTravelTime =
            modelConversions.getTauTTNormalization()
                * (interpolationPolynomials[0][j - 1]
                    + dp
                        * (interpolationPolynomials[1][j - 1]
                            + dp * interpolationPolynomials[2][j - 1]
                            + dps * interpolationPolynomials[3][j - 1])
                    + ps * xCorr);
        return Math.toDegrees(xCorr);
      }
    }

    xCorr = interpolationPolynomials[1][updatedRayParameters.length - 1];
    correctedTravelTime =
        modelConversions.getTauTTNormalization()
            * (interpolationPolynomials[0][updatedRayParameters.length - 1] + ps * xCorr);

    return Math.toDegrees(xCorr);
  }

  /**
   * Function to print out branch information for debugging purposes. Note that this partly
   * duplicates the print function in AllBrnRef, but includes volatile data as well.
   *
   * @param full A boolean flag, if true, print the branch specification as well
   * @param all A boolean flag, if true, print even more specifications
   * @param scientificNotation A boolean flag, if true, print using scientific notation
   * @param returnAllPhases A boolean flag, if false, omit "useless" crustal phases
   * @param caustics A boolean flag, if true only print branches with caustics
   */
  public void dumpBranchInformation(
      boolean full,
      boolean all,
      boolean scientificNotation,
      boolean returnAllPhases,
      boolean caustics) {
    String branchString = "";

    if (!caustics || minimumCausticCount + maximumCausticCount > 0) {
      if (correctedBranchExists) {
        if (returnAllPhases || !isPhaseUseless) {
          if (branchReference.getIsBranchUpGoing()) {
            branchString += String.format("\n         phase = %2s up  ", correctedPhaseCode);

            if (branchReference.getBranchHasDiffraction()) {
              branchString +=
                  String.format("diff = %s  ", branchReference.getDiffractedPhaseCode());
            }

            if (branchReference.getBranchHasAddOn()) {
              branchString += String.format("add-on = %s  ", branchReference.getAddOnPhaseCode());
            }

            branchString +=
                String.format(
                    "\nSegment: code = %s  type = %c        sign = %2d" + "  count = %d\n",
                    branchReference.getGenericPhaseCode(),
                    branchReference.getCorrectionPhaseType()[0],
                    branchReference.getUpGoingCorrectionSign(),
                    branchReference.getNumMantleTraversals());
          } else {
            branchString += String.format("\n         phase = %s  ", correctedPhaseCode);

            if (branchReference.getBranchHasDiffraction()) {
              branchString +=
                  String.format("diff = %s  ", branchReference.getDiffractedPhaseCode());
            }

            if (branchReference.getBranchHasAddOn()) {
              branchString += String.format("add-on = %s  ", branchReference.getAddOnPhaseCode());
            }

            branchString +=
                String.format(
                    "\nSegment: code = %s  type = %c, %c, %c  " + "sign = %2d  count = %d\n",
                    branchReference.getGenericPhaseCode(),
                    branchReference.getCorrectionPhaseType()[0],
                    branchReference.getCorrectionPhaseType()[1],
                    branchReference.getCorrectionPhaseType()[2],
                    branchReference.getUpGoingCorrectionSign(),
                    branchReference.getNumMantleTraversals());
          }

          branchString +=
              String.format(
                  "Branch:  correctedSlownessRange = %8.6f - %8.6f  correctedDistanceRange = %6.2f - "
                      + "%6.2f ",
                  correctedSlownessRange[0],
                  correctedSlownessRange[1],
                  Math.toDegrees(correctedDistanceRange[0]),
                  Math.toDegrees(correctedDistanceRange[1]));

          if (branchReference.getBranchHasDiffraction()) {
            branchString +=
                String.format(
                    "correctedCausticSlowness = %8.6f  diffractedDistanceRange = "
                        + "%6.2f - %6.2f\n",
                    correctedCausticSlowness,
                    Math.toDegrees(diffractedDistanceRange[0]),
                    Math.toDegrees(diffractedDistanceRange[1]));
          } else {
            branchString +=
                String.format("correctedCausticSlowness = %8.6f\n", correctedCausticSlowness);
          }

          if (branchReference.getTurningModelShellName() != null) {
            if (minimumCausticCount + maximumCausticCount == 1) {
              branchString +=
                  String.format(
                      "Shell: %7.2f-%7.2f (%7.2f-%7.2f) %s (1 caustic)\n",
                      branchReference.getTurningRadiusRange()[0],
                      branchReference.getTurningRadiusRange()[1],
                      modelConversions.getSurfaceRadius()
                          - branchReference.getTurningRadiusRange()[1],
                      modelConversions.getSurfaceRadius()
                          - branchReference.getTurningRadiusRange()[0],
                      branchReference.getTurningModelShellName());
            } else if (minimumCausticCount + maximumCausticCount > 1) {
              branchString +=
                  String.format(
                      "Shell: %7.2f-%7.2f (%7.2f-%7.2f) %s (%d caustics)\n",
                      branchReference.getTurningRadiusRange()[0],
                      branchReference.getTurningRadiusRange()[1],
                      modelConversions.getSurfaceRadius()
                          - branchReference.getTurningRadiusRange()[1],
                      modelConversions.getSurfaceRadius()
                          - branchReference.getTurningRadiusRange()[0],
                      branchReference.getTurningModelShellName(),
                      minimumCausticCount + maximumCausticCount);
            } else {
              branchString +=
                  String.format(
                      "Shell: %7.2f-%7.2f (%7.2f-%7.2f) %s\n",
                      branchReference.getTurningRadiusRange()[0],
                      branchReference.getTurningRadiusRange()[1],
                      modelConversions.getSurfaceRadius()
                          - branchReference.getTurningRadiusRange()[1],
                      modelConversions.getSurfaceRadius()
                          - branchReference.getTurningRadiusRange()[0],
                      branchReference.getTurningModelShellName());
            }
          }

          //	branchString += String.format("Flags: group = %s %s  flags = %b %b %b %b\n",
          // branchReference.PhaseGroup,
          //			branchReference.auxGroup, branchReference.isRegionalPhase, branchReference.isDepth,
          // branchReference.canUse, branchReference.dis);

          if (full) {
            int n = updatedRayParameters.length;

            if (all && interpolationPolynomials != null) {
              if (scientificNotation) {
                System.out.println(
                    "\n               p            tau         x"
                        + "                 basis function coefficients                    distanceLimits");

                for (int j = 0; j < n - 1; j++) {
                  branchString +=
                      String.format(
                          "%3d: %3s %13.6e %13.6e %6.2f %13.6e %13.6e "
                              + "%13.6e %13.6e %6.2f %6.2f\n",
                          j,
                          causticFlags[j],
                          updatedRayParameters[j],
                          correctedTauValues[j],
                          Math.toDegrees(interpolatedDistanceValues[j]),
                          interpolationPolynomials[0][j],
                          interpolationPolynomials[1][j],
                          interpolationPolynomials[2][j],
                          interpolationPolynomials[3][j],
                          Math.toDegrees(distanceLimits[0][j]),
                          Math.toDegrees(distanceLimits[1][j]));
                }

                branchString +=
                    String.format(
                        "%3d:     %13.6e %13.6e %6.2f\n",
                        n - 1,
                        updatedRayParameters[n - 1],
                        correctedTauValues[n - 1],
                        Math.toDegrees(interpolatedDistanceValues[n - 1]));
              } else {
                System.out.println(
                    "\n             p      tau       x            "
                        + "basis function coefficients             distanceLimits");

                for (int j = 0; j < n - 1; j++) {
                  branchString +=
                      String.format(
                          "%3d: %3s %8.6f %8.6f %6.2f  %9.2e  %9.2e  "
                              + "%9.2e  %9.2e %6.2f %6.2f\n",
                          j,
                          causticFlags[j],
                          updatedRayParameters[j],
                          correctedTauValues[j],
                          Math.toDegrees(interpolatedDistanceValues[j]),
                          interpolationPolynomials[0][j],
                          interpolationPolynomials[1][j],
                          interpolationPolynomials[2][j],
                          interpolationPolynomials[3][j],
                          Math.toDegrees(distanceLimits[0][j]),
                          Math.toDegrees(distanceLimits[1][j]));
                }

                branchString +=
                    String.format(
                        "%3d:     %8.6f %8.6f %6.2f\n",
                        n - 1,
                        updatedRayParameters[n - 1],
                        correctedTauValues[n - 1],
                        Math.toDegrees(interpolatedDistanceValues[n - 1]));
              }
            } else {
              if (scientificNotation) {
                System.out.println(
                    "\n               p            tau         x        " + "distanceLimits");

                if (interpolationPolynomials != null) {
                  for (int j = 0; j < n - 1; j++) {
                    branchString +=
                        String.format(
                            "%3d: %3s %13.6e %13.6e %6.2f %6.2f %6.2f\n",
                            j,
                            causticFlags[j],
                            updatedRayParameters[j],
                            correctedTauValues[j],
                            Math.toDegrees(interpolatedDistanceValues[j]),
                            Math.toDegrees(distanceLimits[0][j]),
                            Math.toDegrees(distanceLimits[1][j]));
                  }

                  branchString +=
                      String.format(
                          "%3d:     %13.6e %13.6e %6.2f\n",
                          n - 1,
                          updatedRayParameters[n - 1],
                          correctedTauValues[n - 1],
                          Math.toDegrees(interpolatedDistanceValues[n - 1]));
                } else {
                  branchString +=
                      String.format(
                          "%3d:     %13.6e %13.6e %6.2f\n",
                          0,
                          updatedRayParameters[0],
                          correctedTauValues[0],
                          Math.toDegrees(correctedDistanceRange[0]));

                  for (int j = 1; j < n - 1; j++) {
                    branchString +=
                        String.format(
                            "%3d:     %13.6e %13.6e\n",
                            j, updatedRayParameters[j], correctedTauValues[j]);
                  }

                  branchString +=
                      String.format(
                          "%3d:     %13.6e %13.6e %6.2f\n",
                          n - 1,
                          updatedRayParameters[n - 1],
                          correctedTauValues[n - 1],
                          Math.toDegrees(correctedDistanceRange[1]));
                }
              } else {
                System.out.println("\n             p      tau       x        " + "distanceLimits");

                if (interpolationPolynomials != null) {
                  for (int j = 0; j < n - 1; j++) {
                    branchString +=
                        String.format(
                            "%3d: %3s %8.6f %8.6f %6.2f %6.2f %6.2f\n",
                            j,
                            causticFlags[j],
                            updatedRayParameters[j],
                            correctedTauValues[j],
                            Math.toDegrees(interpolatedDistanceValues[j]),
                            Math.toDegrees(distanceLimits[0][j]),
                            Math.toDegrees(distanceLimits[1][j]));
                  }

                  branchString +=
                      String.format(
                          "%3d:     %8.6f %8.6f %6.2f\n",
                          n - 1,
                          updatedRayParameters[n - 1],
                          correctedTauValues[n - 1],
                          Math.toDegrees(interpolatedDistanceValues[n - 1]));
                } else {
                  branchString +=
                      String.format(
                          "%3d:     %8.6f  %8.6f  %6.2f\n",
                          0,
                          updatedRayParameters[0],
                          correctedTauValues[0],
                          Math.toDegrees(correctedDistanceRange[0]));

                  for (int j = 1; j < n - 1; j++) {
                    branchString +=
                        String.format(
                            "%3d:     %8.6f  %8.6f\n",
                            j, updatedRayParameters[j], correctedTauValues[j]);
                  }

                  branchString +=
                      String.format(
                          "%3d:     %8.6f  %8.6f  %6.2f\n",
                          n - 1,
                          updatedRayParameters[n - 1],
                          correctedTauValues[n - 1],
                          Math.toDegrees(correctedDistanceRange[1]));
                }
              }
            }
          }
        } else {
          branchString +=
              String.format(
                  "\n          phase = %s is useless\n", branchReference.getBranchPhaseCode());
        }
      } else {
        if (branchReference.getIsBranchUpGoing()) {
          branchString +=
              String.format(
                  "\n          phase = %s up doesn't exist\n",
                  branchReference.getBranchPhaseCode());
        } else {
          branchString +=
              String.format(
                  "\n          phase = %s doesn't exist\n", branchReference.getBranchPhaseCode());
        }
      }
    }

    LOGGER.fine(branchString);
  }

  /**
   * Function to generate one line of a branch summary table.
   *
   * @param returnAllPhases A boolean flag, if true, omit "useless" crustal phases
   * @return aA String containing one line of a branch summary table
   */
  public String forTable(boolean returnAllPhases) {
    if (!correctedBranchExists || (!returnAllPhases && isPhaseUseless)) {
      return "";
    }

    if (branchReference.getIsBranchUpGoing()) {
      return String.format(
          "%-2s up    %7.4f %7.4f %7.2f %7.2f %7.4f" + "          %c %c %c %2d %d\n",
          correctedPhaseCode,
          correctedSlownessRange[0],
          correctedSlownessRange[1],
          Math.toDegrees(correctedDistanceRange[0]),
          Math.toDegrees(correctedDistanceRange[1]),
          correctedCausticSlowness,
          branchReference.getCorrectionPhaseType()[0],
          branchReference.getCorrectionPhaseType()[1],
          branchReference.getCorrectionPhaseType()[2],
          branchReference.getUpGoingCorrectionSign(),
          branchReference.getNumMantleTraversals());
    } else if (branchReference.getBranchHasDiffraction()) {
      return String.format(
          "%-8s %7.4f %7.4f %7.2f %7.2f %7.4f %7.2f" + "  %c %c %c %2d %d\n",
          correctedPhaseCode,
          correctedSlownessRange[0],
          correctedSlownessRange[1],
          Math.toDegrees(correctedDistanceRange[0]),
          Math.toDegrees(correctedDistanceRange[1]),
          correctedCausticSlowness,
          Math.toDegrees(branchReference.getMaxDiffractedDistance()),
          branchReference.getCorrectionPhaseType()[0],
          branchReference.getCorrectionPhaseType()[1],
          branchReference.getCorrectionPhaseType()[2],
          branchReference.getUpGoingCorrectionSign(),
          branchReference.getNumMantleTraversals());
    } else {
      return String.format(
          "%-8s %7.4f %7.4f %7.2f %7.2f %7.4f" + "          %c %c %c %2d %d\n",
          correctedPhaseCode,
          correctedSlownessRange[0],
          correctedSlownessRange[1],
          Math.toDegrees(correctedDistanceRange[0]),
          Math.toDegrees(correctedDistanceRange[1]),
          correctedCausticSlowness,
          branchReference.getCorrectionPhaseType()[0],
          branchReference.getCorrectionPhaseType()[1],
          branchReference.getCorrectionPhaseType()[2],
          branchReference.getUpGoingCorrectionSign(),
          branchReference.getNumMantleTraversals());
    }
  }
}
