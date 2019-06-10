package gov.usgs.traveltime.tables;

import gov.usgs.traveltime.ModConvert;
import gov.usgs.traveltime.TauUtil;
import java.util.Arrays;

/**
 * In order to sample the Earth and the ranges for both P and S waves adequately on one set of
 * slownesses, the slowness sampling will be very inhomogeneous. For any particular phase, this
 * sampling translates to a ray parameter sampling that can be so variable as to threaten the
 * stability of the tau(p) interpolation. This is addressed by decimating the ray parameter sampling
 * such that the corresponding ranges (ray travel distances) are approximately evenly spaced.
 *
 * @author Ray Buland
 */
public class DecTTbranch {
  Decimate dec;
  TauModel finModel;
  ModConvert convert;

  /**
   * Instantiate the general decimation class.
   *
   * @param finModel Final model
   * @param convert Model dependent conversions
   */
  public DecTTbranch(TauModel finModel, ModConvert convert) {
    this.finModel = finModel;
    this.convert = convert;
    dec = new Decimate();
  }

  /**
   * Figure the decimation for a proxy for the up-going branches range spacing. Note that the
   * eventual over all decimation will be a logical AND of the decimation figured here and the
   * decimations for all the other branches.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   */
  public void upGoingDec(char type) {
    int k = -1, jLast = 0, iMin;
    double pLim, pTarget, pDiff;
    boolean[] keep, upKeep;
    double[] pOld, xOld, pNew, xNew;
    IntPieces piece;

    // Run the decimation algorithm.
    piece = finModel.getPiece(type);
    keep = piece.keep;
    pOld = piece.proxyP;
    xOld = piece.proxyX;
    upKeep = dec.slowDecimation(xOld, convert.normR(TablesUtil.DELXUP));

    // Do some setup.
    pLim = TablesUtil.PLIM * pOld[pOld.length - 1];
    pNew = new double[pOld.length];
    xNew = new double[xOld.length];

    // Actually do the decimation.
    for (int j = 0; j < xOld.length; j++) {
      if (upKeep[j]) {
        if (pOld[j] < pLim || pOld[j] - pNew[k] < TablesUtil.PTOL) {
          // Most of the time, we just keep this sample.
          jLast = j;
          keep[j] = true;
          pNew[++k] = pOld[j];
          xNew[k] = xOld[j];
        } else {
          // For shallow rays, we may want to keep an additional
          // sample.
          pTarget = pNew[k] + 0.75d * (pOld[j] - pNew[k]);
          iMin = 0;
          pDiff = TauUtil.DMAX;
          for (int i = jLast; i <= j; i++) {
            if (Math.abs(pOld[i] - pTarget) < pDiff) {
              iMin = i;
              pDiff = Math.abs(pOld[i] - pTarget);
            }
          }
          if (iMin == jLast || iMin == j) {
            // We didn't find another sample to add.
            jLast = j;
            keep[j] = true;
            pNew[++k] = pOld[j];
            xNew[k] = xOld[j];
          } else {
            // Add the rescued sample plus the current one.
            keep[iMin] = true;
            pNew[++k] = pOld[iMin];
            xNew[k] = xOld[iMin];
            jLast = j;
            keep[j] = true;
            pNew[++k] = pOld[j];
            xNew[k] = xOld[j];
          }
        }
      }
    }
    piece.update(k + 1, pNew, xNew);
  }

  /**
   * Figure the decimation for a down-going branch range spacing. Note that the eventual over all
   * decimation will be a logical AND of the decimation figured here and the decimations for all the
   * other branches.
   *
   * @param branch Branch data
   * @param xTarget Non-dimensional range spacing target
   * @param pOffset Base slowness index in the merged slowness array
   */
  public void downGoingDec(BranchData branch, double xTarget, int pOffset) {
    int beg = 0, k = -1;
    boolean[] keep, downKeep;
    double[] pOld, tauOld, xOld, pNew, tauNew, xNew;
    IntPieces piece;

    // Set up.
    piece = finModel.getPiece(branch.getRaySegmentPhaseTypes()[0]);
    keep = piece.keep;
    pOld = branch.getRayParameters();
    tauOld = branch.getTauValues();
    xOld = branch.getRayTravelDistances();
    pNew = new double[pOld.length];
    tauNew = new double[tauOld.length];
    xNew = new double[xOld.length];

    // Look for caustics.
    for (int i = 1; i < xOld.length - 1; i++) {
      if ((xOld[i + 1] - xOld[i]) * (xOld[i] - xOld[i - 1]) <= 0d) {
        // Got one.  Decimate the branch up to the caustic.
        if (i - 2 > beg) {
          downKeep = dec.slowDecimation(Arrays.copyOfRange(xOld, beg, i - 1), xTarget);
          for (int j = beg, l = 0; j < i - 1; j++, l++) {
            if (downKeep[l]) {
              pNew[++k] = pOld[j];
              tauNew[k] = tauOld[j];
              xNew[k] = xOld[j];
              keep[j + pOffset] = true;
            }
          }
          // Add in the few points we're going to take no matter what.
          beg = Math.min(i + 2, xOld.length);
          for (int j = i - 1; j < beg; j++) {
            pNew[++k] = pOld[j];
            tauNew[k] = tauOld[j];
            xNew[k] = xOld[j];
            keep[j + pOffset] = true;
          }
          i = beg;
        }
      }
    }
    // Decimate after the last caustic (or the whole branch if there
    // are no caustics.
    downKeep = dec.slowDecimation(Arrays.copyOfRange(xOld, beg, xOld.length), xTarget);
    for (int j = beg, l = 0; j < xOld.length; j++, l++) {
      if (downKeep[l]) {
        pNew[++k] = pOld[j];
        tauNew[k] = tauOld[j];
        xNew[k] = xOld[j];
        keep[j + pOffset] = true;
      }
    }
    // Update the branch data with the decimated versions.
    branch.update(k + 1, pNew, tauNew, xNew);
  }
}
