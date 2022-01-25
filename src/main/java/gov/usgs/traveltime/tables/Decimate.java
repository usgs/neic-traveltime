package gov.usgs.traveltime.tables;

import gov.usgs.traveltime.TauUtil;
import java.util.Arrays;

/**
 * The Decimate class computes a decimation that makes the samples of an array approximately evenly
 * spaced (at a predefined spacing).
 *
 * @author Ray Buland
 */
public class Decimate {
  /** An integer containing the number of data in the array to keep */
  private int numDataToKeep;

  /** An integer containing the trial number of data in the array to keep */
  private int trialNumDataToKeep;

  /** A double containing the desired spacing of array values */
  private double desiredSpacing;

  /** A double containing the current variance of residuals */
  private double currentVariance;

  /** An array of double values containing the array to be decimated */
  private double[] arrayToDecimate;

  /**
   * Function to calculate a decimation for the array arrayToDecimate such that the differences
   * between the remaining terms is as close to desiredSpacing as possible. Note that the first and
   * last elements of arrayToDecimate are always kept. This method figures out the decimation, but
   * doesn't actually implement it. This "slow" method iterates for as long as it takes to minimize
   * the variance between the final grid and the ideal grid.
   *
   * @param arrayToDecimate An array of doubles containing the array to be decimated
   * @param desiredSpacing A double containing the target difference between successive elements of
   *     arrayToDecimate after decimation
   * @return keep A boolean array, one for each element of arrayToDecimate, if an element is true,
   *     keep the corresponding element of arrayToDecimate
   */
  public boolean[] slowDecimation(double[] arrayToDecimate, double desiredSpacing) {
    int kb = 0;

    this.arrayToDecimate = arrayToDecimate;
    this.desiredSpacing = desiredSpacing;
    boolean[] keep = new boolean[arrayToDecimate.length]; // True if this array element will be kept
    Arrays.fill(keep, true);

    if (arrayToDecimate.length > 2) {
      // First pass.
      int k1 = 0;
      this.currentVariance = 0d;
      this.numDataToKeep = 0;

      double dx1, dx2;
      for (int j = 1; j < arrayToDecimate.length - 1; j++) {
        dx1 = Math.abs(arrayToDecimate[k1] - arrayToDecimate[j]) - desiredSpacing;
        dx2 = Math.abs(arrayToDecimate[k1] - arrayToDecimate[j + 1]) - desiredSpacing;

        if (Math.abs(dx2) < Math.abs(dx1)) {
          keep[j] = false;
        } else {
          if (k1 == 0) {
            kb = j;
          }

          k1 = j;
          currentVariance += Math.pow(dx1, 2d);
          numDataToKeep++;
        }
      }

      // Add the last point.
      dx1 =
          Math.abs(arrayToDecimate[k1] - arrayToDecimate[arrayToDecimate.length - 1])
              - desiredSpacing;
      currentVariance += Math.pow(dx1, 2d);
      numDataToKeep++;

      if (TablesUtil.deBugLevel > 2) {
        System.out.format("\nInit: %9.3e %9.3e\n", currentVariance / numDataToKeep, doVar(keep));
      }

      // second pass.
      if (numDataToKeep > 1) {
        int pass = 1;
        int nch;
        do {
          k1 = 0;
          int k2 = kb;
          nch = 0;
          for (int j = kb + 1; j < arrayToDecimate.length; j++) {
            if (keep[j]) {
              int k0 = k1;
              k1 = k2;
              k2 = j;
              double var1 = variance(k0, k1, k2, k1 - 1);
              int m1 = trialNumDataToKeep;
              double var2 = variance(k0, k1, k2, k1 + 1);
              int m2 = trialNumDataToKeep;

              if (Math.min(var1 / m1, var2 / m2) < currentVariance / numDataToKeep) {
                // We've reduced the variance.  Decide what to do.
                nch++;
                keep[k1] = !keep[k1];

                // Keep the smallest variance.
                if (var1 / m1 < var2 / m2) {
                  keep[--k1] = true;
                  currentVariance = var1;
                  numDataToKeep = m1;

                  if (TablesUtil.deBugLevel > 2) {
                    System.out.format(
                        "Var1: %9.3e %9.3e %d\n",
                        currentVariance / numDataToKeep, doVar(keep), pass);
                  }
                } else if (var1 / m1 > var2 / m2) {
                  keep[++k1] = true;
                  currentVariance = var2;
                  numDataToKeep = m2;

                  if (TablesUtil.deBugLevel > 2) {
                    System.out.format(
                        "Var2: %9.3e %9.3e %d\n",
                        currentVariance / numDataToKeep, doVar(keep), pass);
                  }
                } else {
                  // If the variances are equal, keep the smallest
                  // number of data.
                  if (m1 <= m2) {
                    keep[--k1] = true;
                    currentVariance = var1;
                    numDataToKeep = m1;

                    if (TablesUtil.deBugLevel > 2) {
                      System.out.format(
                          "M1:   %9.3e %9.3e %d\n",
                          currentVariance / numDataToKeep, doVar(keep), pass);
                    }
                  } else {
                    keep[++k1] = true;
                    currentVariance = var2;
                    numDataToKeep = m2;

                    if (TablesUtil.deBugLevel > 2) {
                      System.out.format(
                          "M2:   %9.3e %9.3e %d\n",
                          currentVariance / numDataToKeep, doVar(keep), pass);
                    }
                  }
                }
              }

              if (k0 == 0) {
                kb = k1;
              }
            }
          }

          pass++;
        } while (nch > 0 && numDataToKeep > 1);
      }
    }

    return keep;
  }

  /**
   * Function to perform a "fast" decimation. The "fast" method was used in the FORTRAN real-time
   * travel-time calculation for the up-going branches only to save time. It has the advantage of
   * being one pass. There are several differences related to the architecture of the travel-time
   * computation. First, although the decimation is designed to make the spacing in ray travel
   * distance, the distance isn't actually kept and must be estimated from normTauGrid. Second, for
   * performance reasons, the algorithm seeks to enforce a minimum distance spacing rather than a
   * uniform target spacing.
   *
   * @param normRayParamGrid An array of doubles containing the normalized ray parameter grid
   * @param normTauGrid An array of doubles containing the normalized tau on the same grid
   * @param normDistanceGrid An array of doubles containing the normalized distance at the branch
   *     end points
   * @param minDistInterval A double containing the normalized minimum distance interval desired
   * @return keep A boolean array, containing the flags specifiying the decimated, normalized ray
   *     parameter grid
   */
  public boolean[] fastDecimation(
      double[] normRayParamGrid,
      double[] normTauGrid,
      double[] normDistanceGrid,
      double minDistInterval) {
    // Scan the current sampling to see if it is already OK.
    boolean[] keep;
    double xCur = normDistanceGrid[1];

    for (int i = normRayParamGrid.length - 2; i >= 0; i--) {
      double xLast = xCur;
      xCur = calcX(normRayParamGrid, normTauGrid, normDistanceGrid, i);

      if (Math.abs(xCur - xLast) <= minDistInterval) {
        // It's not OK.  Set up the flag array.
        keep = new boolean[normRayParamGrid.length];
        Arrays.fill(keep, true);

        // Set up the decimation algorithm.
        if (Math.abs(xCur - xLast) <= 0.75d * minDistInterval) {
          xCur = xLast;
          i++;
        }

        int n = Math.max((int) (Math.abs(xCur - normDistanceGrid[0]) / minDistInterval + 0.8d), 1);
        double dx = (xCur - normDistanceGrid[0]) / n;
        double dx2 = Math.abs(dx / 2d);

        double sgn, rnd;
        if (dx >= 0d) {
          sgn = 1d;
          rnd = 1d;
        } else {
          sgn = -1d;
          rnd = 0d;
        }

        double desiredSpacing = normDistanceGrid[0] + dx;
        int iBeg = 1;
        int iEnd = 0;
        double xLeast = TauUtil.DMAX;

        // Scan the ray parameter grid looking for points to kill.
        for (int j = 1; j <= i; j++) {
          xCur = calcX(normRayParamGrid, normTauGrid, normDistanceGrid, j);
          if (sgn * (xCur - desiredSpacing) > dx2) {
            // This point looks OK.  See if we have points to kill.
            if (iEnd >= iBeg) {
              for (int k = iBeg; k <= iEnd; k++) {
                keep[k] = false;
              }
            }

            // Reset the kill pointers.
            iBeg = iEnd + 2;
            iEnd = j - 1;
            xLeast = TauUtil.DMAX;
            desiredSpacing += (int) ((xCur - desiredSpacing - dx2) / dx + rnd) * dx;
          }

          // Look for the best points to kill.
          if (Math.abs(xCur - desiredSpacing) < xLeast) {
            xLeast = Math.abs(xCur - desiredSpacing);
            iEnd = j - 1;
          }
        }

        // See if there's one more range to kill.
        if (iEnd >= iBeg) {
          for (int k = iBeg; k <= iEnd; k++) {
            keep[k] = false;
          }
        }

        return keep;
      }
    }

    return null;
  }

  /**
   * Function to return distance as a function of tau (distance is minus the derivative of tau). The
   * method uses a simple three point approximation of the derivative except at the end points where
   * distance is already known.
   *
   * @param normRayParamGrid An array of doubles containing the normalized ray parameter grid
   * @param normTauGrid An array of doubles containing the normalized tau on the same grid
   * @param normDistanceGrid An array of doubles containing the normalized distance at the branch
   *     end points
   * @param gridPoint An integer containing the grid point where the derivative is required
   * @return A double containing the distance corresponding to tau(p_i)
   */
  private double calcX(
      double[] normRayParamGrid, double[] normTauGrid, double[] normDistanceGrid, int gridPoint) {
    if (gridPoint == 0) {
      return normDistanceGrid[0];
    } else if (gridPoint == normRayParamGrid.length - 1) {
      return normDistanceGrid[1];
    } else {
      double h1 = normRayParamGrid[gridPoint - 1] - normRayParamGrid[gridPoint];
      double h2 = normRayParamGrid[gridPoint + 1] - normRayParamGrid[gridPoint];
      double hh = h1 * h2 * (normRayParamGrid[gridPoint - 1] - normRayParamGrid[gridPoint + 1]);
      h1 = Math.pow(h1, 2d);
      h2 = -Math.pow(h2, 2d);

      return -(h2 * normTauGrid[gridPoint - 1]
              - (h2 + h1) * normTauGrid[gridPoint]
              + h1 * normTauGrid[gridPoint + 1])
          / hh;
    }
  }

  /**
   * Function to compute the variance of various possible values to keep.
   *
   * @param firstTrialIndex An int containing the first trial arrayToDecimate array index
   * @param secondTrialIndex An int containing the second trial arrayToDecimate array index
   * @param thirdTrialIndex An int containing the third trial arrayToDecimate array index
   * @param alternateSecondTrialIndex An int containing the alternate second trial arrayToDecimate
   *     array index
   * @return A double containing the new trial variance of residuals
   */
  private double variance(
      int firstTrialIndex,
      int secondTrialIndex,
      int thirdTrialIndex,
      int alternateSecondTrialIndex) {
    double dx1 =
        Math.abs(arrayToDecimate[firstTrialIndex] - arrayToDecimate[secondTrialIndex])
            - desiredSpacing;
    double dx2 =
        Math.abs(arrayToDecimate[secondTrialIndex] - arrayToDecimate[thirdTrialIndex])
            - desiredSpacing;
    double newVar = currentVariance - (Math.pow(dx1, 2d) + Math.pow(dx2, 2d));

    if (alternateSecondTrialIndex > firstTrialIndex
        && alternateSecondTrialIndex < thirdTrialIndex) {
      dx1 =
          Math.abs(arrayToDecimate[firstTrialIndex] - arrayToDecimate[alternateSecondTrialIndex])
              - desiredSpacing;
      dx2 =
          Math.abs(arrayToDecimate[alternateSecondTrialIndex] - arrayToDecimate[thirdTrialIndex])
              - desiredSpacing;
      newVar += Math.pow(dx1, 2d) + Math.pow(dx2, 2d);
      trialNumDataToKeep = numDataToKeep;
    } else {
      dx1 =
          Math.abs(arrayToDecimate[firstTrialIndex] - arrayToDecimate[thirdTrialIndex])
              - desiredSpacing;
      newVar += Math.pow(dx1, 2d);
      trialNumDataToKeep = numDataToKeep - 1;
    }

    return newVar;
  }

  /**
   * Function to compute the variance from scratch for testing purposes.
   *
   * @param keep An array of booleans, for each element, if true, keep the corresponding
   *     arrayToDecimate value
   * @return A double containing the fariance of absolute differences between kept values minus the
   *     target difference
   */
  private double doVar(boolean[] keep) {
    int i = 0, numDataToKeep = 0;
    double currentVariance = 0d;

    for (int j = 1; j < arrayToDecimate.length; j++) {
      if (keep[j]) {
        currentVariance +=
            Math.pow(Math.abs(arrayToDecimate[j] - arrayToDecimate[i]) - desiredSpacing, 2d);
        i = j;
        numDataToKeep++;
      }
    }

    return currentVariance / numDataToKeep;
  }
}
