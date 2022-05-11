package gov.usgs.traveltime;

import gov.usgs.traveltime.tables.TauModel;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * The UpGoingDataReference class stores non-volatile up-going branch data for one wave type. Note
 * that all data have been normalized.
 *
 * @author Ray Buland
 */
public class UpGoingDataReference implements Serializable {
  /** A long containing the version id used in serialization */
  private static final long serialVersionUID = 1L;

  /** A char containing the wave type ('P' or 'S') of up-going branches */
  private final char waveType;

  /** An array of double values containing the slowness grid for this branch */
  private final double[] slownessGrid;

  /** A two dimensional array of double values containing the tau for up-going branches by depth */
  private final double[][] upGoingTauValues;

  /** An array of doubles containing the slownesses for branch end points */
  private final double[] branchEndpointSlownesses;

  /**
   * A two dimensional array of double values containing the distances for up-going branches by
   * depth
   */
  private final double[][] upGoingDistanceValues;

  /**
   * Get the wave type ('P' or 'S') of up-going branches
   *
   * @return A char containing the wave type ('P' or 'S') of up-going branches
   */
  public char getWaveType() {
    return waveType;
  }

  /**
   * Get the slowness grid for this branch
   *
   * @return An array of double values containing the slowness grid for this branch
   */
  public double[] getSlownessGrid() {
    return slownessGrid;
  }

  /**
   * Get the tau for up-going branches by depth
   *
   * @return A two dimensional array of double values containing the tau for up-going branches by
   *     depth
   */
  public double[][] getUpGoingTauValues() {
    return upGoingTauValues;
  }

  /**
   * Get the slownesses for branch end points
   *
   * @return An array of doubles containing the slownesses for branch end points
   */
  public double[] getBranchEndpointSlownesses() {
    return branchEndpointSlownesses;
  }

  /**
   * Get the distances for up-going branches by depth
   *
   * @return A two dimensional array of double values containing the distances for up-going branches
   *     by depth
   */
  public double[][] getUpGoingDistanceValues() {
    return upGoingDistanceValues;
  }

  /**
   * UpGoingDataReference constructor, loads from the FORTRAN file reader up-going branchs of one
   * type. The file data should have already been loaded from the *.hed and *.tbl files.
   *
   * @param in Branch input data source.
   * @param waveType A char containing the wave type ('P' or 'S') of up-going branches
   */
  public UpGoingDataReference(ReadTau in, char waveType) {
    this.waveType = waveType;

    // Set the FORTRAN type index.
    int i = -1;
    if (waveType == 'P') {
      i = 0;
    } else {
      i = 1;
    }

    // Copy the slowness grids.
    slownessGrid = Arrays.copyOf(in.pTauUp[i], in.pTauUp[i].length);
    branchEndpointSlownesses = Arrays.copyOf(in.pXUp[i], in.pXUp[i].length);

    // The ray parameter for the up-going branches should be truncated
    // at the source slowness, but are not due to the way FORTRAN arrays
    // are dimensioned.
    upGoingTauValues = new double[in.numRec[i]][];
    upGoingDistanceValues = new double[in.numRec[i]][];

    for (int j = 0; j < in.numRec[i]; j++) {
      int k;
      for (k = 1; k < in.tauUp[i][j].length; k++) {
        if (in.tauUp[i][j][k] == 0d) {
          break;
        }
      }

      upGoingTauValues[j] = Arrays.copyOf(in.tauUp[i][j], k);
      for (k = 1; k < in.xUp[i][j].length; k++) {
        if (in.xUp[i][j][k] == 0d) {
          break;
        }
      }

      upGoingDistanceValues[j] = Arrays.copyOf(in.xUp[i][j], k);
    }
  }

  /**
   * UpGoingDataReference constructor, loads from the tau-p table generation branch data into this
   * class supporting the actual travel-time generation.
   *
   * @param finModel Travel-time branch input data source
   * @param waveType A char containing the wave type ('P' or 'S') of up-going branches
   */
  public UpGoingDataReference(TauModel finModel, char waveType) {
    this.waveType = waveType;

    // Set up the ray parameter sampling.  This is common to all depths.
    slownessGrid =
        Arrays.copyOf(
            finModel.getRayParameters(waveType), finModel.getTauIntegrals(waveType, 1).length);
    branchEndpointSlownesses =
        Arrays.copyOf(finModel.getRayParamBranchEnds(), finModel.getRayParamBranchEnds().length);

    // Set the outer dimension.
    int n = finModel.numUpperMantleInts(waveType);
    upGoingTauValues = new double[n][];
    upGoingDistanceValues = new double[n][];

    // Fill in the arrays.
    int k = -1;
    for (int i = 0; i < finModel.numIntegrals(waveType) - 3; i++) {
      if (finModel.getTauIntegrals(waveType, i) != null) {
        // Tau is easy.
        n = finModel.getTauIntegrals(waveType, i).length;
        upGoingTauValues[++k] = Arrays.copyOf(finModel.getTauIntegrals(waveType, i), n);

        // We have to do this the hard way since we can't use toArray to go
        // from Double to double.
        ArrayList<Double> xUpTmp = finModel.getXUp(waveType, i);
        upGoingDistanceValues[k] = new double[xUpTmp.size()];

        for (int j = 0; j < xUpTmp.size(); j++) {
          upGoingDistanceValues[k][j] = xUpTmp.get(j);
        }
      }
    }
  }

  /**
   * Function to print out the up-going branch data for one depth.
   *
   * @param depthRecordIndex Depth record number
   */
  public void dumpUp(int depthRecordIndex) {
    System.out.println("\n     Up-going " + waveType + " record " + depthRecordIndex);
    System.out.println("          p        tau        p           X");

    for (int k = 0; k < upGoingDistanceValues[depthRecordIndex].length; k++) {
      System.out.format(
          "%3d  %8.6f  %8.6f  %8.6f  %9.6f\n",
          k,
          slownessGrid[k],
          upGoingTauValues[depthRecordIndex][k],
          branchEndpointSlownesses[k],
          upGoingDistanceValues[depthRecordIndex][k]);
    }

    for (int k = upGoingDistanceValues[depthRecordIndex].length;
        k < upGoingTauValues[depthRecordIndex].length;
        k++) {
      System.out.format(
          "%3d  %8.6f  %8.6f\n", k, slownessGrid[k], upGoingTauValues[depthRecordIndex][k]);
    }
  }

  /**
   * Function to print out the up-going branch data for all depths.
   *
   * @param model Earth model corresponding to the up-going branches
   * @param convert Model dependent constants and conversions
   */
  public void dumpUp(ModelDataReference model, ModelConversions convert) {
    for (int depthRecordIndex = 0; depthRecordIndex < upGoingTauValues.length; depthRecordIndex++) {
      System.out.format(
          "\n     Up-going %c record %2d at depth %6.2f\n",
          waveType,
          depthRecordIndex,
          convert.computeDimensionalDepth(model.getUpGoingDepth(depthRecordIndex)));
      System.out.println("          p        tau        p           X");

      for (int k = 0; k < upGoingDistanceValues[depthRecordIndex].length; k++) {
        System.out.format(
            "%3d  %8.6f  %8.6f  %8.6f  %9.6f\n",
            k,
            slownessGrid[k],
            upGoingTauValues[depthRecordIndex][k],
            branchEndpointSlownesses[k],
            upGoingDistanceValues[depthRecordIndex][k]);
      }

      for (int k = upGoingDistanceValues[depthRecordIndex].length;
          k < upGoingTauValues[depthRecordIndex].length;
          k++) {
        System.out.format(
            "%3d  %8.6f  %8.6f\n", k, slownessGrid[k], upGoingTauValues[depthRecordIndex][k]);
      }
    }
  }
}
