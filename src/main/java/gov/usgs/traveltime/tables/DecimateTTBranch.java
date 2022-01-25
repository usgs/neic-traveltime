package gov.usgs.traveltime.tables;

import gov.usgs.traveltime.ModConvert;
import gov.usgs.traveltime.TauUtil;
import java.util.Arrays;

/**
 * Tne DecimateTTBranch class decimates the travel time branch in order to sample the Earth and the
 * ranges for both P and S waves adequately on one set of slownesses, the slowness sampling will be
 * very inhomogeneous. For any particular phase, this sampling translates to a ray parameter
 * sampling that can be so variable as to threaten the stability of the tau(p) interpolation. This
 * is addressed by decimating the ray parameter sampling such that the corresponding ranges (ray
 * travel distances) are approximately evenly spaced.
 *
 * @author Ray Buland
 */
public class DecimateTTBranch {
  /** The Decimate object used for decimation */
  private Decimate dec;

  /** A TauModel containing the final model */
  private TauModel finalTTModel;

  /** A ModConvert object containing the model dependant conversions */
  private ModConvert modelConversions;

  /**
   * Constructor function to instantiate the general decimation class.
   *
   * @param finalTTModel A TauModel containing the final model
   * @param modelConversionsA ModConvert object containing the model dependant conversions
   */
  public DecimateTTBranch(TauModel finalTTModel, ModConvert modelConversions) {
    this.finalTTModel = finalTTModel;
    this.modelConversions = modelConversions;
    this.dec = new Decimate();
  }

  /**
   * Function to figure the decimation for a proxy for the up-going branches range spacing. Note
   * that the eventual over all decimation will be a logical AND of the decimation figured here and
   * the decimations for all the other branches.
   *
   * @param type A char containing the Model type ('P' = P slowness, 'S' = S slowness)
   */
  public void upGoingDecimation(char type) {
    int k = -1, jLast = 0;

    // Run the decimation algorithm.
    IntPieces piece = finalTTModel.getPiece(type);
    boolean[] keep = piece.keep;
    double[] pOld = piece.proxyP;
    double[] xOld = piece.proxyX;
    boolean[] upKeep = dec.slowDecimation(xOld, modelConversions.normR(TablesUtil.DELXUP));

    // Do some setup.
    double pLim = TablesUtil.PLIM * pOld[pOld.length - 1];
    double[] pNew = new double[pOld.length];
    double[] xNew = new double[xOld.length];

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
          double pTarget = pNew[k] + 0.75d * (pOld[j] - pNew[k]);
          int iMin = 0;
          double pDiff = TauUtil.DMAX;

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
   * Function to figure the decimation for a down-going branch range spacing. Note that the eventual
   * over all decimation will be a logical AND of the decimation figured here and the decimations
   * for all the other branches.
   *
   * @param branch A BranchData object containing the Branch data to decimate
   * @param rangeSpacingTarget A double containing the non-dimensional range spacing target
   * @param baseSlownessIndex An integer containign the base slowness index in the merged slowness
   *     array
   */
  public void downGoingDecimation(
      BranchData branch, double rangeSpacingTarget, int baseSlownessIndex) {
    // Set up.
    int beg = 0, k = -1;
    boolean[] downKeep;
    IntPieces piece = finalTTModel.getPiece(branch.getRaySegmentPhaseTypes()[0]);
    boolean[] keep = piece.keep;
    double[] pOld = branch.getRayParameters();
    double[] tauOld = branch.getTauValues();
    double[] xOld = branch.getRayTravelDistances();
    double[] pNew = new double[pOld.length];
    double[] tauNew = new double[tauOld.length];
    double[] xNew = new double[xOld.length];

    // Look for caustics.
    for (int i = 1; i < xOld.length - 1; i++) {
      if ((xOld[i + 1] - xOld[i]) * (xOld[i] - xOld[i - 1]) <= 0d) {
        // Got one.  Decimate the branch up to the caustic.
        if (i - 2 > beg) {
          downKeep = dec.slowDecimation(Arrays.copyOfRange(xOld, beg, i - 1), rangeSpacingTarget);

          for (int j = beg, l = 0; j < i - 1; j++, l++) {
            if (downKeep[l]) {
              pNew[++k] = pOld[j];
              tauNew[k] = tauOld[j];
              xNew[k] = xOld[j];
              keep[j + baseSlownessIndex] = true;
            }
          }
          // Add in the few points we're going to take no matter what.
          beg = Math.min(i + 2, xOld.length);

          for (int j = i - 1; j < beg; j++) {
            pNew[++k] = pOld[j];
            tauNew[k] = tauOld[j];
            xNew[k] = xOld[j];
            keep[j + baseSlownessIndex] = true;
          }

          i = beg;
        }
      }
    }

    // Decimate after the last caustic (or the whole branch if there
    // are no caustics.
    downKeep = dec.slowDecimation(Arrays.copyOfRange(xOld, beg, xOld.length), rangeSpacingTarget);
    for (int j = beg, l = 0; j < xOld.length; j++, l++) {
      if (downKeep[l]) {
        pNew[++k] = pOld[j];
        tauNew[k] = tauOld[j];
        xNew[k] = xOld[j];
        keep[j + baseSlownessIndex] = true;
      }
    }

    // Update the branch data with the decimated versions.
    branch.update(k + 1, pNew, tauNew, xNew);
  }
}
