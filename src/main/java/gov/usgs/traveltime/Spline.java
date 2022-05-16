package gov.usgs.traveltime;

import java.util.Arrays;

/**
 * The Spline class is a collection of spline interpolation routines needed for the computation of
 * travel times.
 *
 * @author Ray Buland
 */
public class Spline {

  /**
   * Function to construct custom spline interpolation basis functions. These basis functions depend
   * only on the ray parameter grid. This is a straight port of FORTRAN routine Tauspl. If
   * rayParameterGrid has dimension N, basisFunctions must have dimension N X 5.
   *
   * @param rayParameterGrid An array of double values containig the normalized ray parameter grid
   * @param basisFunctions A two dimensional array of double values used to return the calculated
   *     basis function coefficients
   */
  public void constuctBasisFunctions(double[] rayParameterGrid, double[][] basisFunctions) {
    int i = 0;

    // Trap a one point series.
    if (rayParameterGrid.length == 1) {
      return;
    }

    // Initialize scratch arrays.
    double[] dp = new double[5];
    double[] sqrtDp = new double[5];
    double[] sqrt3Dp = new double[5];
    double[] invDp = new double[5];
    double[] dSqrtDp = new double[4];
    double[] dSqrt3Dp = new double[4];
    double[] dInvDp = new double[4];
    double[] d = new double[4];

    // Start the process.
    double pEnd = rayParameterGrid[rayParameterGrid.length - 1];
    dp[1] = pEnd - rayParameterGrid[0] + 3d * (rayParameterGrid[1] - rayParameterGrid[0]);
    sqrtDp[1] = Math.sqrt(Math.abs(dp[1]));
    sqrt3Dp[1] = dp[1] * sqrtDp[1];
    invDp[1] = 1d / sqrtDp[1];

    for (int k = 2; k < 5; k++) {
      dp[k] = pEnd - rayParameterGrid[0] + (4 - k) * (rayParameterGrid[1] - rayParameterGrid[0]);
      sqrtDp[k] = Math.sqrt(Math.abs(dp[k]));
      sqrt3Dp[k] = dp[k] * sqrtDp[k];
      invDp[k] = 1d / sqrtDp[k];
      dSqrtDp[k - 1] = sqrtDp[k] - sqrtDp[k - 1];
      dSqrt3Dp[k - 1] = sqrt3Dp[k] - sqrt3Dp[k - 1];
      dInvDp[k - 1] = invDp[k] - invDp[k - 1];
    }

    // Main loop.
    if (rayParameterGrid.length > 2) {
      // Loop over the ray parameter array.
      for (i = 0; i < rayParameterGrid.length - 2; i++) {
        // Update the temporary variables.
        for (int k = 1; k < 5; k++) {
          dp[k - 1] = dp[k];
          sqrtDp[k - 1] = sqrtDp[k];
          sqrt3Dp[k - 1] = sqrt3Dp[k];
          invDp[k - 1] = invDp[k];

          if (k < 4) {
            dSqrtDp[k - 1] = dSqrtDp[k];
            dSqrt3Dp[k - 1] = dSqrt3Dp[k];
            dInvDp[k - 1] = dInvDp[k];
          }
        }

        dp[4] = pEnd - rayParameterGrid[i + 1];
        sqrtDp[4] = Math.sqrt(Math.abs(dp[4]));
        sqrt3Dp[4] = dp[4] * sqrtDp[4];
        invDp[4] = 1d / sqrtDp[4];
        dSqrtDp[3] = sqrtDp[4] - sqrtDp[3];
        dSqrt3Dp[3] = sqrt3Dp[4] - sqrt3Dp[3];
        dInvDp[3] = invDp[4] - invDp[3];

        // Construct G;i-1.
        double ali =
            1d / (0.125d * dSqrt3Dp[0] - (0.75d * dSqrtDp[0] + 0.375d * dInvDp[0] * dp[2]) * dp[2]);
        double alr =
            ali
                * (0.125d * sqrt3Dp[1]
                    - (0.75d * sqrtDp[1] + 0.375d * dp[2] * invDp[1] - sqrtDp[2]) * dp[2]);
        double b1h = dSqrtDp[1] + alr * dSqrtDp[0];
        double b3h = dSqrt3Dp[1] + alr * dSqrt3Dp[0];
        double bih = dInvDp[1] + alr * dInvDp[0];
        double th0p = dSqrtDp[0] * b3h - dSqrt3Dp[0] * b1h;
        double th2p = dSqrtDp[2] * b3h - dSqrt3Dp[2] * b1h;
        double th3p = dSqrtDp[3] * b3h - dSqrt3Dp[3] * b1h;
        double th2m = dInvDp[2] * b3h - dSqrt3Dp[2] * bih;

        // The d;i's completely define G;i-1.
        d[3] =
            ali
                * ((dInvDp[0] * b3h - dSqrt3Dp[0] * bih) * th2p - th2m * th0p)
                / ((dInvDp[3] * b3h - dSqrt3Dp[3] * bih) * th2p - th2m * th3p);
        d[2] = (th0p * ali - th3p * d[3]) / th2p;
        d[1] = (dSqrt3Dp[0] * ali - dSqrt3Dp[2] * d[2] - dSqrt3Dp[3] * d[3]) / b3h;
        d[0] = alr * d[1] - ali;

        // Construct the contributions G;i-1(rayParameterGrid;i-2) and
        // G;i-1(rayParameterGrid;i).  G;i-1(rayParameterGrid;i-1) is normalized to unity.
        basisFunctions[0][i] =
            (0.125d * sqrt3Dp[4]
                    - (0.75d * sqrtDp[4] + 0.375d * dp[3] * invDp[4] - sqrtDp[3]) * dp[3])
                * d[3];

        if (i >= 2) {
          basisFunctions[1][i - 2] =
              (0.125d * sqrt3Dp[0]
                      - (0.75d * sqrtDp[0] + 0.375d * dp[1] * invDp[0] - sqrtDp[1]) * dp[1])
                  * d[0];
        }

        // Construct the contributions -dG;i-1(rayParameterGrid)/dp for rayParameterGrid;i-2,
        // rayParameterGrid;i-1, and rayParameterGrid;i.
        basisFunctions[2][i] = -0.75d * (sqrtDp[4] + dp[3] * invDp[4] - 2d * sqrtDp[3]) * d[3];

        if (i >= 1) {
          basisFunctions[3][i - 1] =
              -0.75d
                  * ((sqrtDp[1] + dp[2] * invDp[1] - 2d * sqrtDp[2]) * d[1]
                      - (dSqrtDp[0] + dInvDp[0] * dp[2]) * d[0]);
        }

        if (i >= 2) {
          basisFunctions[4][i - 2] =
              -0.75d * (sqrtDp[0] + dp[1] * invDp[0] - 2d * sqrtDp[1]) * d[0];
        }
      }
    }

    for (int j = 0; j < 4; j++) {
      for (int k = 1; k < 5; k++) {
        dp[k - 1] = dp[k];
        sqrtDp[k - 1] = sqrtDp[k];
        sqrt3Dp[k - 1] = sqrt3Dp[k];
        invDp[k - 1] = invDp[k];

        if (k < 4) {
          dSqrtDp[k - 1] = dSqrtDp[k];
          dSqrt3Dp[k - 1] = dSqrt3Dp[k];
          dInvDp[k - 1] = dInvDp[k];
        }
      }

      dp[4] = 0d;
      sqrtDp[4] = 0d;
      invDp[4] = 0d;

      // Construction of the d;i's is different for each case.
      // In cases G;i, i=n-1,n,n+1, G;i is truncated at rayParameterGrid;n to
      // avoid patching across the singularity in the second
      // derivative.
      if (j == 3) {
        // For G;n+1 constrain G;n+1(rayParameterGrid;n) to be .25.
        d[0] = 2d / (dp[0] * sqrtDp[0]);
      } else {
        // For G;i, i=n-2,n-1,n, the condition dG;i(rayParameterGrid)/dp|rayParameterGrid;i = 0
        // has been substituted for the second derivative
        // continuity condition that can no longer be satisfied.
        double alr =
            (sqrtDp[1] + dp[2] * invDp[1] - 2d * sqrtDp[2]) / (dSqrtDp[0] + dInvDp[0] * dp[2]);
        d[1] =
            1d
                / (0.125d * sqrt3Dp[1]
                    - (0.75d * sqrtDp[1] + 0.375d * dp[2] * invDp[1] - sqrtDp[2]) * dp[2]
                    - (0.125d * dSqrt3Dp[0]
                            - (0.75d * dSqrtDp[0] + 0.375d * dInvDp[0] * dp[2]) * dp[2])
                        * alr);
        d[0] = alr * d[1];

        if (j == 1) {
          // For G;n-1 constrain G;n-1(rayParameterGrid;n) to be .25.
          d[2] = (2d + dSqrt3Dp[1] * d[1] + dSqrt3Dp[0] * d[0]) / (sqrt3Dp[2]);
        } else if (j == 0) {
          // No additional constraints are required for G;n-2.
          d[2] =
              -((dSqrt3Dp[1] - dSqrtDp[1] * dp[3]) * d[1]
                      + (dSqrt3Dp[0] - dSqrtDp[0] * dp[3]) * d[0])
                  / (dSqrt3Dp[2] - dSqrtDp[2] * dp[3]);
          d[3] = (dSqrt3Dp[2] * d[2] + dSqrt3Dp[1] * d[1] + dSqrt3Dp[0] * d[0]) / (sqrt3Dp[3]);
        }
      }

      // Construct the contributions G;i-1(rayParameterGrid;i-2) and
      // G;i-1(rayParameterGrid;i).
      if (j <= 1) {
        basisFunctions[0][i] =
            (0.125d * sqrt3Dp[2]
                        - (0.75d * sqrtDp[2] + 0.375d * dp[3] * invDp[2] - sqrtDp[3]) * dp[3])
                    * d[2]
                - (0.125d * dSqrt3Dp[1] - (0.75d * dSqrtDp[1] + 0.375d * dInvDp[1] * dp[3]) * dp[3])
                    * d[1]
                - (0.125d * dSqrt3Dp[0] - (0.75d * dSqrtDp[0] + 0.375d * dInvDp[0] * dp[3]) * dp[3])
                    * d[0];
      }

      if (i > 1) {
        basisFunctions[1][i - 2] =
            (0.125d * sqrt3Dp[0]
                    - (0.75d * sqrtDp[0] + 0.375d * dp[1] * invDp[0] - sqrtDp[1]) * dp[1])
                * d[0];
      }

      // Construct the contributions -dG;i-1(rayParameterGrid)/dp | rayParameterGrid;i-2,
      // rayParameterGrid;i-1, and rayParameterGrid;i.
      if (j <= 1) {
        basisFunctions[2][i] =
            -0.75d
                * ((sqrtDp[2] + dp[3] * invDp[2] - 2d * sqrtDp[3]) * d[2]
                    - (dSqrtDp[1] + dInvDp[1] * dp[3]) * d[1]
                    - (dSqrtDp[0] + dInvDp[0] * dp[3]) * d[0]);
      }

      if (j <= 2 && i > 0) {
        basisFunctions[3][i - 1] = 0d;
      }

      if (i > 1) {
        basisFunctions[4][i - 2] = -0.75d * (sqrtDp[0] + dp[1] * invDp[0] - 2d * sqrtDp[1]) * d[0];
      }

      i++;
    }
  }

  /**
   * Function that uses the custom spline basis functions to build an interpolation for distance.
   * Note that the interpolation depends of tau at each ray parameter, but only on distance at the
   * end points. When finished the tau values will have been copied into the first row of poly and
   * the interpolated distance values will be in the second row. This is a straight port of FORTRAN
   * routine Fitspl.
   *
   * @param tauGrid An array of doubles containing the normalized tau at each ray parameter grid
   *     point
   * @param distanceRange An array of doubles holding the normalized distance at each end of the ray
   *     parameter grid
   * @param basisFunctions A two dimensional array of doubles containing the ray parameter grid
   *     basis functions as computed in function constuctBasisFunctions
   * @param interpolationPolynomials A two dimensional array of doubles containing the scratch array
   *     dimensioned [3][rayParameterGrid.length]
   * @param interpolatedDistances An array of doubles containing the normalized, interpolated
   *     distance values to return
   */
  public void computeTauSpline(
      double[] tauGrid,
      double[] distanceRange,
      double[][] basisFunctions,
      double[][] interpolationPolynomials,
      double[] interpolatedDistances) {
    int n = tauGrid.length;

    // Make sure we have a reasonable length branch.
    if (n == 1) {
      interpolatedDistances[0] = distanceRange[0];
      return;
    }

    // Set up the working arrays.  In the FORTRAN routine Fitspl,
    // two temporary arrays were used, a(n,2) and b(n).  Since poly
    // happens to be available, I've just used poly[0][n] to store
    // b and poly[1][n] and poly[2][n] to store a.
    interpolationPolynomials[0] = Arrays.copyOf(tauGrid, n);
    interpolationPolynomials[1] = Arrays.copyOf(basisFunctions[0], n);
    interpolationPolynomials[2] = Arrays.copyOf(basisFunctions[1], n);
    double[] ap = new double[3];

    for (int j = 0; j < 3; j++) {
      ap[j] = basisFunctions[j + 2][n - 1];
    }

    // Arrays ap(*,1), a, and ap(*,2) comprise n+2 x n+2 penta-
    // diagonal symmetric matrix A.  Let x1, tauGrid, and xn comprise
    // corresponding n+2 vector b.  Then, A * g = b, may be solved
    // for n+2 vector g such that interpolation I is given by
    // I(rayParameterGrid) = sum(i=0,n+1) g;i * G;i(rayParameterGrid).

    // First, eliminate the lower triangular portion of A to form A'.
    double alr = interpolationPolynomials[1][0] / basisFunctions[2][0];
    interpolationPolynomials[1][0] = 1d - basisFunctions[3][0] * alr;
    interpolationPolynomials[2][0] -= basisFunctions[4][0] * alr;
    interpolationPolynomials[0][0] -= distanceRange[0] * alr;

    for (int j = 1; j < n; j++) {
      alr = interpolationPolynomials[1][j] / interpolationPolynomials[1][j - 1];
      interpolationPolynomials[1][j] = 1d - interpolationPolynomials[2][j - 1] * alr;
      interpolationPolynomials[0][j] -= interpolationPolynomials[0][j - 1] * alr;
    }

    alr = ap[0] / interpolationPolynomials[1][n - 2];
    ap[1] -= interpolationPolynomials[2][n - 2] * alr;
    double gn = distanceRange[1] - interpolationPolynomials[0][n - 2] * alr;

    // Back solve the upper triangular portion of A' for
    // coefficients g;i.
    alr = ap[1] / interpolationPolynomials[1][n - 1];
    gn =
        (gn - interpolationPolynomials[0][n - 1] * alr)
            / (ap[2] - interpolationPolynomials[2][n - 1] * alr);
    interpolationPolynomials[0][n - 1] =
        (interpolationPolynomials[0][n - 1] - gn * interpolationPolynomials[2][n - 1])
            / interpolationPolynomials[1][n - 1];

    for (int j = n - 2; j >= 0; j--) {
      interpolationPolynomials[0][j] =
          (interpolationPolynomials[0][j]
                  - interpolationPolynomials[0][j + 1] * interpolationPolynomials[2][j])
              / interpolationPolynomials[1][j];
    }

    // Fill in the interpolated distances.
    interpolatedDistances[0] = distanceRange[0];
    for (int j = 1; j < n - 1; j++) {
      interpolatedDistances[j] =
          basisFunctions[2][j] * interpolationPolynomials[0][j - 1]
              + basisFunctions[3][j] * interpolationPolynomials[0][j]
              + basisFunctions[4][j] * interpolationPolynomials[0][j + 1];
    }

    interpolatedDistances[n - 1] = distanceRange[1];

    /*	System.out.println("\nFitspl: tau x b a");
    for(int j=0; j<n; j++) {
    	System.out.format("%13.6e %13.6e %13.6e %13.6e %13.6e\n",
    			tauGrid[j], interpolatedDistances[j], interpolationPolynomials[0][j], interpolationPolynomials[1][j], interpolationPolynomials[2][j]);
    } */
  }
}
