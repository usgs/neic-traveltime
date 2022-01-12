package gov.usgs.traveltime;

import gov.usgs.traveltime.tables.TauIntegralException;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Generate all volatile information associated with one travel-time branch.
 *
 * @author Ray Buland
 */
public class BrnDataVol {
  boolean compute; // True if travel times should be computed
  boolean exists; // True if the corrected branch still exists
  boolean isUseless; // True if the phase just gets in the way
  String phCode; // Corrected phase code
  String[] uniqueCode; // Local storage for diffracted and add on phases
  double[] pRange; // Corrected slowness range for this branch
  double[] xRange; // Corrected distance range for this branch
  double[] xDiff; // Corrected distance range for a diffracted branch
  double pCaustic; // Corrected slowness of a caustic, if any
  double[] pBrn; // Updated ray parameter grid
  double[] tauBrn; // Corrected tau values
  double[] xBrn; // Interpolated distance values
  double[][] poly = null; // Interpolation polynomial for tau(p)
  double[][] xLim; // Distance limits for each ray parameter interval
  int iMin; // Count of caustic minimums in this branch
  int iMax; // Count of caustic maximums in this branch
  String[] flag; // Min/max flag highlighting caustics for printing
  int[] xTries; // Number of valid distance ranges for travel times
  double dTdDepth; // Normalization for the depth derivative
  BrnDataRef ref; // Link to non-volatile branch data
  UpDataVol pUp, sUp; // Up-going P and S data for correcting the depth
  ModConvert cvt; // Model specific conversions
  AuxTtRef auxtt; // Auxiliary travel-time data
  TtFlags flags; // Flags, etc. by phase code
  TtStat ttStat; // Local copy of the phase statistics
  Ellip ellip; // Local copy of the ellipticity correction
  Spline spline; // Spline code
  double tCorr,
      xSign = Double.NaN,
      zSign = Double.NaN,
      pSourceSq = Double.NaN,
      pEnd = Double.NaN; // Some variables need to be remembered between calls
  // to the travel-time routine.

  /** Private logging object. */
  private static final Logger LOGGER = Logger.getLogger(AllBrnVol.class.getName());

  /**
   * Set up volatile copies of data that changes with depth
   *
   * @param ref The branch reference data source
   * @param pUp The corrected P up-going branch source
   * @param sUp The corrected S up-going branch source
   * @param cvt The conversion factor object
   * @param auxtt Auxiliary travel-time data
   * @param spline The spline object
   */
  public BrnDataVol(
      BrnDataRef ref, UpDataVol pUp, UpDataVol sUp, ModConvert cvt, AuxTtRef auxtt, Spline spline) {
    this.ref = ref;
    this.pUp = pUp;
    this.sUp = sUp;
    this.cvt = cvt;
    this.auxtt = auxtt;
    this.spline = spline;

    // Do branch summary information.
    isUseless = auxtt.isChaff(ref.phCode);
    compute = true;
    exists = true;
    xDiff = new double[2];
  }

  /**
   * Correct this branch for source depth using the corrected up-going branch ray parameters and tau
   * values.
   *
   * @param zSource Earth flattened, normalized source depth
   * @param dTdDepth Correction factor for dT/dDepth
   * @param xMin The minimum source-receiver distance desired between ray parameter samples
   * @param tagBrn The P or S up-going branch suffix
   * @throws TauIntegralException If the tau integral fails
   */
  public void depthCorr(double zSource, double dTdDepth, double xMin, char tagBrn)
      throws TauIntegralException {
    int i, len = 0;
    double pMax;
    double[][] basisTmp;

    this.dTdDepth = dTdDepth;

    // Skip branches we aren't computing.
    if (compute) {
      // A surface source is a special case (i.e., no up-going phases).
      if (-zSource <= TauUtil.DTOL) {
        if (ref.signSeg < 0) {
          // This branch starts with a down-going ray.
          exists = true;

          // Do things common to all branches.
          phCode = ref.phCode;
          pRange = Arrays.copyOf(ref.pRange, ref.pRange.length);
          xRange = Arrays.copyOf(ref.xRange, ref.xRange.length);
          pCaustic = pRange[1];
          pEnd = Double.NaN;
          flags = auxtt.findFlags(phCode);

          // Make a local copy of the reference p and tau.
          len = ref.pBrn.length;
          pBrn = Arrays.copyOf(ref.pBrn, len);
          tauBrn = Arrays.copyOf(ref.tauBrn, len);

          // Set up the diffracted range.
          if (ref.hasDiff) {
            xDiff[0] = Math.max(xRange[0], xRange[1]);
            xDiff[1] = ref.xDiff;
          }

          // Spline it.
          poly = new double[4][len];
          xBrn = new double[len];
          spline.tauSpline(tauBrn, xRange, ref.basis, poly, xBrn);
        } else {
          // This branch starts with an up-going ray.
          exists = false;
          return;
        }

        // Otherwise, we need to correct the surface focus data for depth.
      } else {
        // Assume the branch exists until proven otherwise.
        exists = true;

        // Do things common to all branches.
        phCode = ref.phCode;
        pRange = Arrays.copyOf(ref.pRange, ref.pRange.length);
        xRange = Arrays.copyOf(ref.xRange, ref.xRange.length);
        pCaustic = pRange[1];
        pEnd = Double.NaN;
        flags = auxtt.findFlags(phCode);

        // Do phases that start as P.
        if (ref.typeSeg[0] == 'P') {
          // Correct ray parameter range.
          pMax = Math.min(pRange[1], pUp.pMax);

          // Screen phases that don't exist.
          if (pRange[0] >= pMax) {
            exists = false;
            return;
          }
          pRange[1] = pMax;

          /* See how long we need the corrected arrays to be.  This is
           * awkward and not very Java-like, but the gain in performance
           * seemed worthwhile in this case.*/
          for (int j = 0; j < ref.pBrn.length; j++) {
            // See if we need this point.
            if (ref.pBrn[j] < pMax + TauUtil.DTOL) {
              len++;

              // If this point is equal to pMax, we're done.
              if (Math.abs(ref.pBrn[j] - pMax) <= TauUtil.DTOL) {
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
            exists = false;
            return;
          }

          // Otherwise, allocate the arrays.
          pBrn = new double[len];
          tauBrn = new double[len];

          // Correct an up-going branch separately.
          if (ref.isUpGoing) {
            // Correct the distance.
            xRange[1] = pUp.xEndUp;

            // Correct tau for the up-going branch.
            i = 0;
            for (int j = 0; j < ref.pBrn.length; j++) {
              // See if we need to correct this point.
              // Make sure we do not loop past the end of pUp.pUp
              if ((ref.pBrn[j] < pMax + TauUtil.DTOL) && (i < pUp.pUp.length)) {
                // pTauUp is a superset of pBrn so we need to sync them.
                // advance through the pUp.pUp array until we find the index
                // of pUp.pUp that matches the value in ref.pBrn[j].
                // To make sure don't loop past the end of pUp.pUp, we check
                // length-1 because of how the while loop is structured
                while ((Math.abs(ref.pBrn[j] - pUp.pUp[i]) > TauUtil.DTOL)
                    && (i < pUp.pUp.length - 1)) {
                  i++;
                }

                // Correct the tau and x values.
                pBrn[j] = ref.pBrn[j];
                tauBrn[j] = pUp.tauUp[i];

                // If this point is equal to pMax, we're done.
                if (Math.abs(ref.pBrn[j] - pMax) <= TauUtil.DTOL) {
                  break;
                }
              } else {
                // Otherwise, (we've hit the max, or run out of
                // elements in pTauUp)
                // we add one more point and quit.
                pBrn[j] = pMax;
                tauBrn[j] = pUp.tauEndUp;

                break;
              }
            }

            // Decimate the up-going branch.
            pBrn = pUp.realUp(pBrn, tauBrn, xRange, xMin);
            tauBrn = pUp.getDecTau();

            // Spline it.
            len = pBrn.length;
            basisTmp = new double[5][len];
            spline.basisSet(pBrn, basisTmp);
            poly = new double[4][len];
            xBrn = new double[len];
            spline.tauSpline(tauBrn, xRange, basisTmp, poly, xBrn);

            // Otherwise, correct a down-going branch.
          } else {
            // Correct distance.
            int m = 0;
            for (i = 0; i < xRange.length; i++) {
              for (; m < pUp.ref.pXUp.length; m++) {
                if (Math.abs(pRange[i] - pUp.ref.pXUp[m]) <= TauUtil.DTOL) {
                  if (m >= pUp.xUp.length) {
                    exists = false;
                    return;
                  }

                  xRange[i] += ref.signSeg * pUp.xUp[m];
                  break;
                }
              }

              if (m >= pUp.ref.pXUp.length) {
                xRange[i] = lastX();
              }
            }

            // Set up the diffracted branch distance range.
            if (ref.hasDiff) {
              xDiff[0] = xRange[0];
              xDiff[1] = ref.xDiff;
            }

            // Correct tau for down-going branches.
            i = 0;
            for (int j = 0; j < ref.pBrn.length; j++) {
              // See if we need to correct this point.
              // Make sure we do not loop past the end of pUp.pUp
              if ((ref.pBrn[j] < pMax + TauUtil.DTOL) && (i < pUp.pUp.length)) {
                // pTauUp is a superset of pBrn so we need to sync them.
                // advance through the pUp.pUp array until we find the index
                // of pUp.pUp that matches the value in ref.pBrn[j].
                // To make sure don't loop past the end of pUp.pUp, we check
                // length-1 because of how the while loop is structured
                while ((Math.abs(ref.pBrn[j] - pUp.pUp[i]) > TauUtil.DTOL)
                    && (i < pUp.pUp.length - 1)) {
                  i++;
                }

                // Correct the tau and x values.
                pBrn[j] = ref.pBrn[j];
                tauBrn[j] = ref.tauBrn[j] + ref.signSeg * pUp.tauUp[i];

                // If this point is equal to pMax, we're done.
                if (Math.abs(ref.pBrn[j] - pMax) <= TauUtil.DTOL) {
                  break;
                }
              } else {
                // Otherwise, (we've hit the max, or run out of
                // elements in pTauUp)
                // we add one more point and quit.
                pBrn[j] = pMax;
                tauBrn[j] = lastTau();

                break;
              }
            }

            // Spline it.
            poly = new double[4][len];
            xBrn = new double[len];
            if (Math.abs(pRange[1] - ref.pRange[1]) <= TauUtil.DTOL) {
              spline.tauSpline(tauBrn, xRange, ref.basis, poly, xBrn);
            } else {
              basisTmp = new double[5][len];
              spline.basisSet(pBrn, basisTmp);
              spline.tauSpline(tauBrn, xRange, basisTmp, poly, xBrn);
            }
          }
          // Do phases that start as S.
        } else {
          // Correct ray parameter range.
          pMax = Math.min(pRange[1], sUp.pMax);

          // Screen phases that don't exist.
          if (pRange[0] >= pMax) {
            exists = false;
            return;
          }
          pRange[1] = pMax;

          /* See how long we need the corrected arrays to be.  This is
           * awkward and not very Java-like, but the gain in performance
           * seemed worthwhile in this case.*/
          for (int j = 0; j < ref.pBrn.length; j++) {
            // See if we need this point.
            if (ref.pBrn[j] < pMax + TauUtil.DTOL) {
              len++;

              // If this point is equal to pMax, we're done.
              if (Math.abs(ref.pBrn[j] - pMax) <= TauUtil.DTOL) {
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
            exists = false;
            return;
          }
          // Otherwise, allocate the arrays.
          pBrn = new double[len];
          tauBrn = new double[len];

          // Correct an up-going branch separately.
          if (ref.isUpGoing) {
            // Correct the distance.
            xRange[1] = sUp.xEndUp;

            // Correct tau for the up-going branch.
            i = 0;
            for (int j = 0; j < ref.pBrn.length; j++) {
              // See if we need to correct this point.
              // Make sure we do not loop past the end of sUp.pUp
              if ((ref.pBrn[j] < pMax + TauUtil.DTOL) && (i < sUp.pUp.length)) {
                // pTauUp is a superset of pBrn so we need to sync them.
                // advance through the sUp.pUp array until we find the index
                // of sUp.pUp that matches the value in ref.pBrn[j].
                // To make sure don't loop past the end of sUp.pUp, we check
                // length-1 because of how the while loop is structured
                while ((Math.abs(ref.pBrn[j] - sUp.pUp[i]) > TauUtil.DTOL)
                    && (i < sUp.pUp.length - 1)) {
                  i++;
                }

                // Correct the tau and x values.
                pBrn[j] = ref.pBrn[j];
                tauBrn[j] = sUp.tauUp[i];

                // If this point is equal to pMax, we're done.
                if (Math.abs(ref.pBrn[j] - pMax) <= TauUtil.DTOL) {
                  break;
                }
              } else {
                // Otherwise, (we've hit the max, or run out of
                // elements in pTauUp)
                // we add one more point and quit.
                pBrn[j] = pMax;
                tauBrn[j] = sUp.tauEndUp;

                break;
              }
            }

            // Decimate the up-going branch.
            pBrn = sUp.realUp(pBrn, tauBrn, xRange, xMin);
            tauBrn = sUp.getDecTau();

            // Spline it.
            len = pBrn.length;
            basisTmp = new double[5][len];
            spline.basisSet(pBrn, basisTmp);
            poly = new double[4][len];
            xBrn = new double[len];
            spline.tauSpline(tauBrn, xRange, basisTmp, poly, xBrn);

            // Otherwise, correct a down-going branch.
          } else {
            // Correct distance.
            int m = 0;
            for (i = 0; i < xRange.length; i++) {
              for (; m < sUp.ref.pXUp.length; m++) {
                if (Math.abs(pRange[i] - sUp.ref.pXUp[m]) <= TauUtil.DTOL) {
                  if (m >= sUp.xUp.length) {
                    exists = false;
                    return;
                  }

                  xRange[i] += ref.signSeg * sUp.xUp[m];
                  break;
                }
              }
              if (m >= sUp.ref.pXUp.length) {
                xRange[i] = lastX();
              }
            }
            // Set up the diffracted branch distance range.
            if (ref.hasDiff) {
              xDiff[0] = xRange[0];
              xDiff[1] = ref.xDiff;
            }

            // Correct tau for down-going branches.
            i = 0;
            for (int j = 0; j < ref.pBrn.length; j++) {
              // See if we need to correct this point.
              // Make sure we do not loop past the end of sUp.pUp
              if ((ref.pBrn[j] < pMax + TauUtil.DTOL) && (i < sUp.pUp.length)) {
                // pTauUp is a superset of pBrn so we need to sync them.
                // advance through the sUp.pUp array until we find the index
                // of sUp.pUp that matches the value in ref.pBrn[j].
                // To make sure don't loop past the end of sUp.pUp, we check
                // length-1 because of how the while loop is structured
                while ((Math.abs(ref.pBrn[j] - sUp.pUp[i]) > TauUtil.DTOL)
                    && (i < sUp.pUp.length - 1)) {
                  i++;
                }

                // Correct the tau and x values.
                pBrn[j] = ref.pBrn[j];
                tauBrn[j] = ref.tauBrn[j] + ref.signSeg * sUp.tauUp[i];

                // If this point is equal to pMax, we're done.
                if (Math.abs(ref.pBrn[j] - pMax) <= TauUtil.DTOL) {
                  break;
                }
              } else {
                // Otherwise, (we've hit the max, or run out of
                // elements in pTauUp)
                // we add one more point and quit.
                pBrn[j] = pMax;
                tauBrn[j] = lastTau();

                break;
              }
            }

            // Spline it.
            poly = new double[4][len];
            xBrn = new double[len];
            if (Math.abs(pRange[1] - ref.pRange[1]) <= TauUtil.DTOL) {
              spline.tauSpline(tauBrn, xRange, ref.basis, poly, xBrn);
            } else {
              basisTmp = new double[5][len];
              spline.basisSet(pBrn, basisTmp);
              spline.tauSpline(tauBrn, xRange, basisTmp, poly, xBrn);
            }
          }
        }
      }

      // Complete everything we'll need to compute a travel time.
      xLim = new double[2][len - 1];
      flag = new String[len - 1];
      tauPoly(tagBrn);

      // Now that we know what type of phase we have, select the right
      // statistics and ellipticity correction.
      if (ref.isUpGoing) {
        ttStat = auxtt.findStats(phCode);
        ellip = auxtt.findEllip(flags.phGroup + "up");
      } else {
        ttStat = flags.ttStat;
        ellip = flags.ellip;
      }

      // Un-computed phases might as well not exist.
    } else {
      exists = false;
    }
  }

  /**
   * The tau value corresponding to the largest ray parameter (usually the slowness at the source)
   * is computed from the end integrals computed as part of the up-going branch corrections.
   *
   * @return Normalized tau for the maximum ray parameter for this branch
   */
  private double lastTau() {
    double tau;

    if (ref.typeSeg[0] == 'P') {
      // Add or subtract the up-going piece.  For a surface reflection
      // it would be added.  For a down-going branch it would be
      // subtracted (because that part of the branch is cut off by the
      // source depth).
      tau = ref.signSeg * pUp.tauEndUp;
      // Add the down-going part, which may not be the same as the
      // up-going piece (e.g., sP).
      if (ref.typeSeg[1] == 'P') {
        tau += ref.countSeg * (pUp.tauEndUp + pUp.tauEndLvz);
      } else {
        tau += ref.countSeg * (pUp.tauEndCnv);
      }
      // Add the coming-back-up part, which may not be the same as the
      // down-going piece (e.g., ScP).
      if (ref.typeSeg[2] == 'P') {
        tau += ref.countSeg * (pUp.tauEndUp + pUp.tauEndLvz);
      } else {
        tau += ref.countSeg * (pUp.tauEndCnv);
      }
    } else {
      // Add or subtract the up-going piece.  For a surface reflection
      // it would be added.  For a down-going branch it would be
      // subtracted (because that part of the branch is cut off by the
      // source depth).
      tau = ref.signSeg * sUp.tauEndUp;
      // Add the down-going part, which may not be the same as the
      // up-going piece (e.g., sP).
      if (ref.typeSeg[1] == 'S') {
        tau += ref.countSeg * (sUp.tauEndUp + sUp.tauEndLvz);
      } else {
        tau += ref.countSeg * (sUp.tauEndCnv);
      }
      // Add the coming-back-up part, which may not be the same as the
      // down-going piece (e.g., ScP).
      if (ref.typeSeg[2] == 'S') {
        tau += ref.countSeg * (sUp.tauEndUp + sUp.tauEndLvz);
      } else {
        tau += ref.countSeg * (sUp.tauEndCnv);
      }
    }
    return tau;
  }

  /**
   * The distance value corresponding to the largest ray parameter (usually the slowness at the
   * source) is computed from the end integrals computed as part of the up-going branch corrections.
   *
   * @return Normalized distance for the maximum ray parameter for this branch
   */
  private double lastX() {
    double x;

    if (ref.typeSeg[0] == 'P') {
      // Add or subtract the up-going piece.  For a surface reflection
      // it would be added.  For a down-going branch it would be
      // subtracted (because that part of the branch is cut off by the
      // source depth).
      x = ref.signSeg * pUp.xEndUp;
      // Add the down-going part, which may not be the same as the
      // up-going piece (e.g., sP).
      if (ref.typeSeg[1] == 'P') {
        x += ref.countSeg * (pUp.xEndUp + pUp.xEndLvz);
      } else {
        x += ref.countSeg * (pUp.xEndCnv);
      }
      // Add the coming-back-up part, which may not be the same as the
      // down-going piece (e.g., ScP).
      if (ref.typeSeg[2] == 'P') {
        x += ref.countSeg * (pUp.xEndUp + pUp.xEndLvz);
      } else {
        x += ref.countSeg * (pUp.xEndCnv);
      }
    } else {
      // Add or subtract the up-going piece.  For a surface reflection
      // it would be added.  For a down-going branch it would be
      // subtracted (because that part of the branch is cut off by the
      // source depth).
      x = ref.signSeg * sUp.xEndUp;
      // Add the down-going part, which may not be the same as the
      // up-going piece (e.g., sP).
      if (ref.typeSeg[1] == 'S') {
        x += ref.countSeg * (sUp.xEndUp + sUp.xEndLvz);
      } else {
        x += ref.countSeg * (sUp.xEndCnv);
      }
      // Add the coming-back-up part, which may not be the same as the
      // down-going piece (e.g., ScP).
      if (ref.typeSeg[2] == 'S') {
        x += ref.countSeg * (sUp.xEndUp + sUp.xEndLvz);
      } else {
        x += ref.countSeg * (sUp.xEndCnv);
      }
    }
    return x;
  }

  /**
   * Using tau and distance (from tauSpline), compute the final spline interpolation polynomial.
   *
   * @param tagBrn Up-going P and S branch suffix
   */
  private void tauPoly(char tagBrn) {
    int n;
    double pEnd, dp, dtau, xMin, xMax, pExt, sqrtPext, xCaustic;
    double[] dpe, sqrtDp, sqrt3Dp;

    // Fill in the rest of the interpolation polynomial.  Note that
    // distance will be overwritten with the linear polynomial
    // coefficient.
    n = pBrn.length;
    pEnd = pBrn[n - 1];
    dpe = new double[2];
    sqrtDp = new double[2];
    sqrt3Dp = new double[2];
    dpe[1] = pEnd - pBrn[0];
    sqrtDp[1] = Math.sqrt(dpe[1]);
    sqrt3Dp[1] = dpe[1] * sqrtDp[1];
    // Set up variables for tracking caustics.
    iMin = 0;
    iMax = 0;
    pCaustic = pBrn[n - 1];
    xMin = xRange[0];
    xMax = xMin;

    // Loop over ray parameter intervals.
    for (int j = 0; j < n - 1; j++) {
      // Complete the interpolation polynomial.
      dpe[0] = dpe[1];
      sqrtDp[0] = sqrtDp[1];
      sqrt3Dp[0] = sqrt3Dp[1];
      dpe[1] = pEnd - pBrn[j + 1];
      sqrtDp[1] = Math.sqrt(dpe[1]);
      sqrt3Dp[1] = dpe[1] * sqrtDp[1];
      dp = pBrn[j] - pBrn[j + 1];
      dtau = tauBrn[j + 1] - tauBrn[j];
      poly[3][j] =
          (2d * dtau - dp * (xBrn[j + 1] + xBrn[j]))
              / (0.5d * (sqrt3Dp[1] - sqrt3Dp[0])
                  - 1.5d * sqrtDp[1] * sqrtDp[0] * (sqrtDp[1] - sqrtDp[0]));
      poly[2][j] =
          (dtau
                  - dp * xBrn[j]
                  - (sqrt3Dp[1] + 0.5d * sqrt3Dp[0] - 1.5d * dpe[1] * sqrtDp[0]) * poly[3][j])
              / Math.pow(dp, 2d);
      poly[1][j] =
          (dtau
                  - (Math.pow(dpe[1], 2d) - Math.pow(dpe[0], 2d)) * poly[2][j]
                  - (sqrt3Dp[1] - sqrt3Dp[0]) * poly[3][j])
              / dp;
      poly[0][j] =
          tauBrn[j] - sqrt3Dp[0] * poly[3][j] - dpe[0] * (dpe[0] * poly[2][j] + poly[1][j]);

      // Set up the distance limits.
      xLim[0][j] = Math.min(xBrn[j], xBrn[j + 1]);
      xLim[1][j] = Math.max(xBrn[j], xBrn[j + 1]);
      if (xLim[0][j] < xMin) {
        xMin = xLim[0][j];
        if (xBrn[j] <= xBrn[j + 1]) pCaustic = pBrn[j];
        else pCaustic = pBrn[j + 1];
      }
      // See if there's a caustic in this interval.
      flag[j] = "";
      if (Math.abs(poly[2][j]) > TauUtil.DMIN) {
        sqrtPext = -0.375d * poly[3][j] / poly[2][j];
        pExt = Math.pow(sqrtPext, 2d);
        if (sqrtPext > 0d && pExt > dpe[1] && pExt < dpe[0]) {
          xCaustic = poly[1][j] + sqrtPext * (2d * sqrtPext * poly[2][j] + 1.5d * poly[3][j]);
          xLim[0][j] = Math.min(xLim[0][j], xCaustic);
          xLim[1][j] = Math.max(xLim[1][j], xCaustic);
          if (xCaustic < xMin) {
            xMin = xCaustic;
            pCaustic = pEnd - pExt;
          }
          if (poly[3][j] < 0d) {
            flag[j] = "min";
            iMin++;
          } else {
            flag[j] = "max";
            iMax++;
          }
        }
      }
      xMax = Math.max(xMax, xLim[1][j]);
    }

    // Fix ranges.
    xRange[0] = xMin;
    xRange[1] = xMax;
    // Set the distances to try (see findTt for details).
    xTries = new int[2];
    for (int j = 0; j < 2; j++) {
      if (xRange[j] <= Math.PI) xTries[j] = 0;
      else if (xRange[j] <= 2d * Math.PI) xTries[j] = 1;
      else xTries[j] = 2;
    }
    // Fix the phase code for the up-going branch.
    if (ref.isUpGoing && tagBrn != ' ') phCode = "" + phCode.charAt(0) + tagBrn;
  }

  /**
   * Get the travel times for this branch. Three different distances must to be processed, but this
   * needs to be driven from AllBrnVol so that the surface focus distance correction can be applied
   * for the auxiliary data, hence the need for depIndex.
   *
   * @param depIndex Depth index (there are three possible depths)
   * @param xs Desired distance in radians
   * @param dSource Source depth in kilometers
   * @param returnAllPhases If false only return potentially useful phases
   * @param ttList A list of travel times to be filled in
   */
  public void getTT(
      int depIndex, double xs, double dSource, boolean returnAllPhases, TTime ttList) {
    String tmpCode;
    boolean found = false;
    double pTol, dps, dp, ps, del;
    TTimeData tTime;
    TtFlags addFlags;

    // Skip non-existent and useless phases (if requested).
    if (!exists || (!returnAllPhases && isUseless)) return;
    // On the first index, set up the conversion for dT/dDelta.
    if (depIndex == 0) xSign = cvt.dTdDelta * Math.pow(-1d, xTries[0] + 1);

    // Loop over possible distances.
    if (depIndex >= xTries[0] && depIndex <= xTries[1]) {
      xSign = -xSign;
      // See if we have an arrival at this distance.
      if (xs >= xRange[0] && xs <= xRange[1]) {
        // Set up some useful variables.
        if (Double.isNaN(pEnd)) {
          pEnd = pBrn[pBrn.length - 1];
          zSign = dTdDepth * ref.signSeg;
          if (ref.typeSeg[0] == 'P') pSourceSq = Math.pow(pUp.pSource, 2d);
          else pSourceSq = Math.pow(sUp.pSource, 2d);
        }
        // Loop over ray parameter intervals looking for arrivals.
        for (int j = 0; j < xLim[0].length; j++) {
          if (xs > xLim[0][j] && xs <= xLim[1][j]) {
            // pTol is a totally empirically tolerance.
            pTol = Math.max(3e-6d * (pBrn[j + 1] - pBrn[j]), 1e-4d);

            // This is the general case.
            if (Math.abs(poly[2][j]) > TauUtil.DMIN) {
              // There should be two solutions.
              dps =
                  -(3d * poly[3][j]
                          + Math.copySign(
                              Math.sqrt(
                                  Math.abs(
                                      9d * Math.pow(poly[3][j], 2d)
                                          + 32d * poly[2][j] * (xs - poly[1][j]))),
                              poly[3][j]))
                      / (8d * poly[2][j]);
              for (int k = 0; k < 2; k++) {
                if (k > 0) dps = (poly[1][j] - xs) / (2d * poly[2][j] * dps);
                dp = Math.copySign(Math.pow(dps, 2d), dps);
                // Arrivals outside the interval aren't real.
                if (dp >= Math.max(pEnd - pBrn[j + 1] - pTol, 0d) && dp <= pEnd - pBrn[j] + pTol) {
                  // Add the arrival.
                  found = true;
                  ps = pEnd - dp;
                  // Fiddle the phase code for bc branches.
                  if (phCode.contains("ab") && ps <= pCaustic)
                    tmpCode = TauUtil.phSeg(phCode) + "bc";
                  else tmpCode = phCode;
                  // Add it.
                  ttList.addPhase(
                      tmpCode,
                      ref.uniqueCode,
                      cvt.tNorm
                          * (poly[0][j]
                              + dp * (poly[1][j] + dp * poly[2][j] + dps * poly[3][j])
                              + ps * xs),
                      xSign * ps,
                      zSign * Math.sqrt(Math.abs(pSourceSq - Math.pow(ps, 2d))),
                      -(2d * poly[2][j]
                              + 0.75d * poly[3][j] / Math.max(Math.abs(dps), TauUtil.DTOL))
                          / cvt.tNorm,
                      false);
                }
              }
              // We have to be careful if the quadratic term is zero.
            } else {
              // On the plus side, there's only one solution.
              dps = (xs - poly[1][j]) / (1.5d * poly[3][j]);
              dp = Math.copySign(Math.pow(dps, 2d), dps);
              //	System.out.println("Sol spec: "+(float)dp);
              // Arrivals outside the interval aren't real.
              if (dp < pEnd - pBrn[j + 1] - pTol || dp > pEnd - pBrn[j] + pTol) break;
              // Add the arrival.
              found = true;
              ps = pEnd - dp;
              // Fiddle the phase code for bc branches.
              if (phCode.contains("ab") && ps <= pCaustic) tmpCode = TauUtil.phSeg(phCode) + "bc";
              else tmpCode = phCode;
              // add it.
              ttList.addPhase(
                  tmpCode,
                  ref.uniqueCode,
                  cvt.tNorm * (poly[0][j] + dp * (poly[1][j] + dps * poly[3][j]) + ps * xs),
                  xSign * ps,
                  zSign * Math.sqrt(Math.abs(pSourceSq - Math.pow(ps, 2d))),
                  -(0.75d * poly[3][j] / Math.max(Math.abs(dps), TauUtil.DTOL)) / cvt.tNorm,
                  false);
            }
          }
        }
      }

      // See if we have a diffracted arrival.
      if (ref.hasDiff) {
        if (xs > xDiff[0] && xs <= xDiff[1]) {
          // This would have gotten missed as it's off the end of the branch.
          if (Double.isNaN(pEnd)) {
            pEnd = pBrn[pBrn.length - 1];
            zSign = dTdDepth * ref.signSeg;
            if (ref.typeSeg[0] == 'P') pSourceSq = Math.pow(pUp.pSource, 2d);
            else pSourceSq = Math.pow(sUp.pSource, 2d);
          }
          dp = pRange[1] - pRange[0];
          dps = Math.sqrt(Math.abs(dp));
          // Fiddle the uniqueCode.
          if (uniqueCode == null) uniqueCode = new String[2];
          uniqueCode[0] = ref.phDiff + 0;
          uniqueCode[1] = null;
          // Add it.
          ttList.addPhase(
              ref.phDiff,
              uniqueCode,
              cvt.tNorm
                  * (poly[0][0]
                      + dp * (poly[1][0] + dp * poly[2][0] + dps * poly[3][0])
                      + pRange[0] * xs),
              xSign * pRange[0],
              zSign * Math.sqrt(Math.abs(pSourceSq - Math.pow(pRange[0], 2d))),
              -(2d * poly[2][0] + 0.75d * poly[3][0] / Math.max(Math.abs(dps), TauUtil.DTOL))
                  / cvt.tNorm,
              false);
        }
      }

      // See if we have an add-on phase.
      if (ref.hasAddOn && found) {
        addFlags = auxtt.findFlags(ref.phAddOn);
        if (addFlags.ttStat != null) {
          del = Math.toDegrees(xs);
          if (del >= addFlags.ttStat.minDelta && del <= addFlags.ttStat.maxDelta) {
            // Fiddle the uniqueCode.
            if (uniqueCode == null) uniqueCode = new String[2];
            uniqueCode[0] = ref.phAddOn + 0;
            uniqueCode[1] = null;
            // See what we've got.
            if (ref.phAddOn.equals("Lg")) {
              // Make sure we have a valid depth.
              if (dSource <= TauUtil.LGDEPMAX) {
                ttList.addPhase(ref.phAddOn, uniqueCode, 0d, cvt.dTdDLg, 0d, 0d, true);
              }
            } else if (ref.phAddOn.equals("LR")) {
              // Make sure we have a valid depth and distance.
              if (dSource <= TauUtil.LRDEPMAX && xs <= TauUtil.LRDELMAX) {
                ttList.addPhase(ref.phAddOn, uniqueCode, 0d, cvt.dTdDLR, 0d, 0d, true);
              }
            } else if (ref.phAddOn.equals("pwP") || ref.phAddOn.equals("PKPpre")) {
              tTime = ttList.getPhase(ttList.getNumPhases() - 1);
              ttList.addPhase(
                  ref.phAddOn, uniqueCode, tTime.tt, tTime.dTdD, tTime.dTdZ, tTime.dXdP, true);
            }
          }
        }
      }
    }
  }

  /**
   * Compute the surface focus source-receiver distance and travel time. Note that for up-going
   * branches the calculation is from the source depth.
   *
   * @param dTdD Desired ray parameter in seconds/degree
   * @return Source-receiver distance in degrees
   */
  public double oneRay(double dTdD) {
    double xCorr;
    double ps = Math.abs(dTdD) / cvt.dTdDelta;

    // Check validity.
    if (!exists || ps < pRange[0] || ps > pRange[1]) {
      return Double.NaN;
    }

    for (int j = 1; j < pBrn.length; j++) {
      if (ps <= pBrn[j]) {
        double dp = pBrn[pBrn.length - 1] - ps;
        double dps = Math.sqrt(Math.abs(dp));
        xCorr = poly[1][j - 1] + 2d * dp * poly[2][j - 1] + 1.5d * dps * poly[3][j - 1];
        tCorr =
            cvt.tNorm
                * (poly[0][j - 1]
                    + dp * (poly[1][j - 1] + dp * poly[2][j - 1] + dps * poly[3][j - 1])
                    + ps * xCorr);
        return Math.toDegrees(xCorr);
      }
    }
    xCorr = poly[1][pBrn.length - 1];
    tCorr = cvt.tNorm * (poly[0][pBrn.length - 1] + ps * xCorr);
    return Math.toDegrees(xCorr);
  }

  /**
   * Getter for the travel time computed by the last call to oneRay.
   *
   * @return Travel-time in seconds
   */
  public double getTimeCorr() {
    return tCorr;
  }

  /**
   * Setter for the branch compute flag. Branches can be requested through the desired phase list.
   * The branch flags will need to be reset for each new session.
   *
   * @param compute If true compute travel times from this branch
   */
  public void setCompute(boolean compute) {
    this.compute = compute;
  }

  /**
   * Get the branch segment code.
   *
   * @return Branch segment code
   */
  public String getPhSeg() {
    return ref.phSeg;
  }

  /**
   * Print out branch information for debugging purposes. Note that this partly duplicates the print
   * function in AllBrnRef, but includes volatile data as well.
   *
   * @param full If true, print the branch specification as well
   * @param all If true, print even more specifications
   * @param sci If true, print using scientific notation
   * @param returnAllPhases If false, omit "useless" crustal phases
   * @param caustics If true only print branches with caustics
   */
  public void dumpBrn(
      boolean full, boolean all, boolean sci, boolean returnAllPhases, boolean caustics) {
    String branchString = "";

    if (!caustics || iMin + iMax > 0) {
      if (exists) {
        if (returnAllPhases || !isUseless) {
          if (ref.isUpGoing) {
            branchString += String.format("\n         phase = %2s up  ", phCode);

            if (ref.hasDiff) {
              branchString += String.format("diff = %s  ", ref.phDiff);
            }

            if (ref.hasAddOn) {
              branchString += String.format("add-on = %s  ", ref.phAddOn);
            }

            branchString +=
                String.format(
                    "\nSegment: code = %s  type = %c        sign = %2d" + "  count = %d\n",
                    ref.phSeg, ref.typeSeg[0], ref.signSeg, ref.countSeg);
          } else {
            branchString += String.format("\n         phase = %s  ", phCode);

            if (ref.hasDiff) {
              branchString += String.format("diff = %s  ", ref.phDiff);
            }

            if (ref.hasAddOn) {
              branchString += String.format("add-on = %s  ", ref.phAddOn);
            }

            branchString +=
                String.format(
                    "\nSegment: code = %s  type = %c, %c, %c  " + "sign = %2d  count = %d\n",
                    ref.phSeg,
                    ref.typeSeg[0],
                    ref.typeSeg[1],
                    ref.typeSeg[2],
                    ref.signSeg,
                    ref.countSeg);
          }

          branchString +=
              String.format(
                  "Branch:  pRange = %8.6f - %8.6f  xRange = %6.2f - " + "%6.2f ",
                  pRange[0], pRange[1], Math.toDegrees(xRange[0]), Math.toDegrees(xRange[1]));

          if (ref.hasDiff) {
            branchString +=
                String.format(
                    "pCaustic = %8.6f  xDiff = " + "%6.2f - %6.2f\n",
                    pCaustic, Math.toDegrees(xDiff[0]), Math.toDegrees(xDiff[1]));
          } else {
            branchString += String.format("pCaustic = %8.6f\n", pCaustic);
          }

          if (ref.turnShell != null) {
            if (iMin + iMax == 1) {
              branchString +=
                  String.format(
                      "Shell: %7.2f-%7.2f (%7.2f-%7.2f) %s (1 caustic)\n",
                      ref.rRange[0],
                      ref.rRange[1],
                      cvt.rSurface - ref.rRange[1],
                      cvt.rSurface - ref.rRange[0],
                      ref.turnShell);
            } else if (iMin + iMax > 1) {
              branchString +=
                  String.format(
                      "Shell: %7.2f-%7.2f (%7.2f-%7.2f) %s (%d caustics)\n",
                      ref.rRange[0],
                      ref.rRange[1],
                      cvt.rSurface - ref.rRange[1],
                      cvt.rSurface - ref.rRange[0],
                      ref.turnShell,
                      iMin + iMax);
            } else {
              branchString +=
                  String.format(
                      "Shell: %7.2f-%7.2f (%7.2f-%7.2f) %s\n",
                      ref.rRange[0],
                      ref.rRange[1],
                      cvt.rSurface - ref.rRange[1],
                      cvt.rSurface - ref.rRange[0],
                      ref.turnShell);
            }
          }

          //	branchString += String.format("Flags: group = %s %s  flags = %b %b %b %b\n",
          // ref.phGroup,
          //			ref.auxGroup, ref.isRegional, ref.isDepth, ref.canUse, ref.dis);

          if (full) {
            int n = pBrn.length;

            if (all && poly != null) {
              if (sci) {
                System.out.println(
                    "\n               p            tau         x"
                        + "                 basis function coefficients                    xLim");

                for (int j = 0; j < n - 1; j++) {
                  branchString +=
                      String.format(
                          "%3d: %3s %13.6e %13.6e %6.2f %13.6e %13.6e "
                              + "%13.6e %13.6e %6.2f %6.2f\n",
                          j,
                          flag[j],
                          pBrn[j],
                          tauBrn[j],
                          Math.toDegrees(xBrn[j]),
                          poly[0][j],
                          poly[1][j],
                          poly[2][j],
                          poly[3][j],
                          Math.toDegrees(xLim[0][j]),
                          Math.toDegrees(xLim[1][j]));
                }

                branchString +=
                    String.format(
                        "%3d:     %13.6e %13.6e %6.2f\n",
                        n - 1, pBrn[n - 1], tauBrn[n - 1], Math.toDegrees(xBrn[n - 1]));
              } else {
                System.out.println(
                    "\n             p      tau       x            "
                        + "basis function coefficients             xLim");

                for (int j = 0; j < n - 1; j++) {
                  branchString +=
                      String.format(
                          "%3d: %3s %8.6f %8.6f %6.2f  %9.2e  %9.2e  "
                              + "%9.2e  %9.2e %6.2f %6.2f\n",
                          j,
                          flag[j],
                          pBrn[j],
                          tauBrn[j],
                          Math.toDegrees(xBrn[j]),
                          poly[0][j],
                          poly[1][j],
                          poly[2][j],
                          poly[3][j],
                          Math.toDegrees(xLim[0][j]),
                          Math.toDegrees(xLim[1][j]));
                }

                branchString +=
                    String.format(
                        "%3d:     %8.6f %8.6f %6.2f\n",
                        n - 1, pBrn[n - 1], tauBrn[n - 1], Math.toDegrees(xBrn[n - 1]));
              }
            } else {
              if (sci) {
                System.out.println("\n               p            tau         x        " + "xLim");

                if (poly != null) {
                  for (int j = 0; j < n - 1; j++) {
                    branchString +=
                        String.format(
                            "%3d: %3s %13.6e %13.6e %6.2f %6.2f %6.2f\n",
                            j,
                            flag[j],
                            pBrn[j],
                            tauBrn[j],
                            Math.toDegrees(xBrn[j]),
                            Math.toDegrees(xLim[0][j]),
                            Math.toDegrees(xLim[1][j]));
                  }

                  branchString +=
                      String.format(
                          "%3d:     %13.6e %13.6e %6.2f\n",
                          n - 1, pBrn[n - 1], tauBrn[n - 1], Math.toDegrees(xBrn[n - 1]));
                } else {
                  branchString +=
                      String.format(
                          "%3d:     %13.6e %13.6e %6.2f\n",
                          0, pBrn[0], tauBrn[0], Math.toDegrees(xRange[0]));

                  for (int j = 1; j < n - 1; j++) {
                    branchString +=
                        String.format("%3d:     %13.6e %13.6e\n", j, pBrn[j], tauBrn[j]);
                  }

                  branchString +=
                      String.format(
                          "%3d:     %13.6e %13.6e %6.2f\n",
                          n - 1, pBrn[n - 1], tauBrn[n - 1], Math.toDegrees(xRange[1]));
                }
              } else {
                System.out.println("\n             p      tau       x        " + "xLim");

                if (poly != null) {
                  for (int j = 0; j < n - 1; j++) {
                    branchString +=
                        String.format(
                            "%3d: %3s %8.6f %8.6f %6.2f %6.2f %6.2f\n",
                            j,
                            flag[j],
                            pBrn[j],
                            tauBrn[j],
                            Math.toDegrees(xBrn[j]),
                            Math.toDegrees(xLim[0][j]),
                            Math.toDegrees(xLim[1][j]));
                  }

                  branchString +=
                      String.format(
                          "%3d:     %8.6f %8.6f %6.2f\n",
                          n - 1, pBrn[n - 1], tauBrn[n - 1], Math.toDegrees(xBrn[n - 1]));
                } else {
                  branchString +=
                      String.format(
                          "%3d:     %8.6f  %8.6f  %6.2f\n",
                          0, pBrn[0], tauBrn[0], Math.toDegrees(xRange[0]));

                  for (int j = 1; j < n - 1; j++) {
                    branchString += String.format("%3d:     %8.6f  %8.6f\n", j, pBrn[j], tauBrn[j]);
                  }

                  branchString +=
                      String.format(
                          "%3d:     %8.6f  %8.6f  %6.2f\n",
                          n - 1, pBrn[n - 1], tauBrn[n - 1], Math.toDegrees(xRange[1]));
                }
              }
            }
          }
        } else {
          branchString += String.format("\n          phase = %s is useless\n", ref.phCode);
        }
      } else {
        if (ref.isUpGoing) {
          branchString += String.format("\n          phase = %s up doesn't exist\n", ref.phCode);
        } else {
          branchString += String.format("\n          phase = %s doesn't exist\n", ref.phCode);
        }
      }
    }

    LOGGER.fine(branchString);
  }

  public boolean getIsUseless() {
    return (isUseless);
  }

  /**
   * Generate one line of a branch summary table.
   *
   * @param returnAllPhases If true, omit "useless" crustal phases
   * @return a String containing one line of a branch summary table
   */
  public String forTable(boolean returnAllPhases) {
    if (!exists || (!returnAllPhases && isUseless)) {
      return "";
    }
    if (ref.isUpGoing) {
      return String.format(
          "%-2s up    %7.4f %7.4f %7.2f %7.2f %7.4f" + "          %c %c %c %2d %d\n",
          phCode,
          pRange[0],
          pRange[1],
          Math.toDegrees(xRange[0]),
          Math.toDegrees(xRange[1]),
          pCaustic,
          ref.typeSeg[0],
          ref.typeSeg[1],
          ref.typeSeg[2],
          ref.signSeg,
          ref.countSeg);
    } else if (ref.hasDiff) {
      return String.format(
          "%-8s %7.4f %7.4f %7.2f %7.2f %7.4f %7.2f" + "  %c %c %c %2d %d\n",
          phCode,
          pRange[0],
          pRange[1],
          Math.toDegrees(xRange[0]),
          Math.toDegrees(xRange[1]),
          pCaustic,
          Math.toDegrees(ref.xDiff),
          ref.typeSeg[0],
          ref.typeSeg[1],
          ref.typeSeg[2],
          ref.signSeg,
          ref.countSeg);
    } else {
      return String.format(
          "%-8s %7.4f %7.4f %7.2f %7.2f %7.4f" + "          %c %c %c %2d %d\n",
          phCode,
          pRange[0],
          pRange[1],
          Math.toDegrees(xRange[0]),
          Math.toDegrees(xRange[1]),
          pCaustic,
          ref.typeSeg[0],
          ref.typeSeg[1],
          ref.typeSeg[2],
          ref.signSeg,
          ref.countSeg);
    }
  }
}
