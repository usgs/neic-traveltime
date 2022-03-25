package gov.usgs.traveltime.tables;

import gov.usgs.traveltime.ModConvert;
import gov.usgs.traveltime.TauUtil;
import java.util.ArrayList;

/**
 * The Integrate class computes tau and range integrals for all merged slowness values. Combining
 * these raw materials for P and S will allow the construction of all required phases as well as the
 * up-going branches for depth correcting all phases.
 *
 * @author Ray Buland
 */
public class Integrate {
  /** An integer containing the number of samples */
  private int numSamples = 0;

  /** A double containing non-dimensional flattened maximum earthquake depth */
  private double maximumDepth;

  /** A double containing the non-dimensional flattened outer core depth */
  private double outerCoreDepth;

  /** A double containing the non-dimensional flattened inner core depth */
  private double innerCoreDepth;

  /** An EarthModel object containing the reference earth model information */
  private EarthModel referenceModel;

  /** A TauModel object containing the slowness depth model */
  private TauModel tauDepthModel;

  /** A TauModel object containing the final model */
  private TauModel tauFinalModel;

  /** A TauInt object containing the Tau-X integration logic */
  private TauIntegrate tauInt;

  /** A ModConvert object containing model dependent constants and conversions */
  private ModConvert modelConversions;

  /** An ArrayList of doubles containing the slowness values */
  private ArrayList<Double> slowness;

  /**
   * Get the final model (pared down and with integrals).
   *
   * @return A TauModel object containing the final tau model
   */
  public TauModel getFinalModel() {
    return tauFinalModel;
  }

  /**
   * Integrate constructor Remember various incarnations of the model.
   *
   * @param tauDepthModel A TauModel object containing the slowness depth model
   */
  public Integrate(TauModel tauDepthModel) {
    this.tauDepthModel = tauDepthModel;
    referenceModel = tauDepthModel.getReferenceModel();
    modelConversions = tauDepthModel.getModelConversions();
    tauFinalModel = new TauModel(referenceModel, modelConversions);
    tauFinalModel.initIntegrals();
    tauInt = new TauIntegrate();
    slowness = tauDepthModel.getSlowness();
    tauFinalModel.putSlowness(slowness);
    tauFinalModel.putShells('P', tauDepthModel.getShellModelP());
    tauFinalModel.putShells('S', tauDepthModel.getShellModelS());
    maximumDepth = modelConversions.flatZ(modelConversions.rSurface - TauUtil.MAXDEPTH);
    outerCoreDepth = referenceModel.getOuterCoreModel().getDepth();
    innerCoreDepth = referenceModel.getInnerCoreModel().getDepth();

    if (TablesUtil.deBugLevel > 0) {
      System.out.format(
          "\n\tzMax zOC zIC %8.6f %8.6f %8.6f\n", maximumDepth, outerCoreDepth, innerCoreDepth);
    }
  }

  /**
   * Function to compute all the tau and range integrals we'll need later.
   *
   * @param waveType A char containing the wave waveType ('P' = P slowness, 'S' = S slowness)
   * @throws Exception If the tau integration interval is invalid
   */
  public void doTauIntegrals(char waveType) throws Exception {
    int iRay = 0;
    double zLim = modelDepth(waveType);
    int n1 =
        tauDepthModel.getShell(waveType, 0).getBottomSampleIndex()
            - tauDepthModel
                .getShell(waveType, tauDepthModel.shellSize(waveType) - 1)
                .getTopSampleIndex()
            + 1;

    if (TablesUtil.deBugLevel > 0) {
      System.out.format("\nmm = %d n1 = %d\n\n", tauDepthModel.size(waveType), n1);
    }

    double[] tau = new double[n1];
    double[] x = new double[n1];
    int n = n1;
    int nP = slowness.size() - 1;

    TauSample sample1 = tauDepthModel.getSample(waveType, 0);
    tauFinalModel.add(waveType, sample1, -1);
    double zLast = sample1.z;

    // Loop over depth intervals.
    boolean disc = false;
    for (int i = 1; i < tauDepthModel.size(waveType); i++) {
      TauSample sample0 = sample1;
      sample1 = tauDepthModel.getSample(waveType, i);

      if (sample0.z != sample1.z) {
        // Normal interval: do integrals for all ray parameters.
        if (disc) {
          disc = false;
          tauFinalModel.add(waveType, sample0, numSamples);
        }

        /**
         * This loop is central to everything as this is where the integrals are finally done. It is
         * confusing though because instead of integrating for each ray parameter from the surface
         * to the turning point, the integrals are done for all legal ray parameters through each
         * layer of the model (i.e., the radial interval between two slowness samples). This
         * organization allows the separation of tau-x contributions from the mantle, outer core,
         * and inner core needed to construct phases like PcP and PKKP. Note that the limit n, while
         * it may seem superfluous is actually critical as it limits the ray penetration into low
         * velocity zones beneath discontinuities (as Dr. Who said, "Ponder that").
         */
        iRay = 0;
        for (int j = nP; j >= 0 && iRay < n; j--, iRay++) {
          if (sample1.slow < slowness.get(j)) {
            n = iRay;
            break;
          }

          tau[iRay] +=
              tauInt.integrateLayer(
                  slowness.get(j), sample0.slow, sample1.slow, sample0.z, sample1.z);
          x[iRay] += tauInt.getLayerIntDist();
        }

        if (sample0.z >= zLim) {
          if (sample0.z >= maximumDepth) {
            numSamples++;
            tauFinalModel.add(
                waveType,
                sample1,
                numSamples,
                new TauXsample(iRay, tau, x, ShellName.UPPER_MANTLE));

            if (TablesUtil.deBugLevel > 0) {
              System.out.format(
                  "lev1 %c %3d %s\n",
                  waveType,
                  tauFinalModel.size(waveType) - 1,
                  tauFinalModel.stringLastIntegral(waveType));
            }
          } else {
            tauFinalModel.add(waveType, sample1, numSamples);
          }
        }
        zLast = sample0.z;
      } else {
        // We're in a discontinuity.
        if (sample0.z != zLast) {
          // Save the integrals at the bottom of the mantle and outer core.
          if (sample0.z == outerCoreDepth || sample0.z == innerCoreDepth) {
            numSamples++;

            if (sample0.z == outerCoreDepth) {
              tauFinalModel.add(
                  waveType,
                  sample0,
                  numSamples,
                  new TauXsample(n1, tau, x, ShellName.CORE_MANTLE_BOUNDARY));
            } else {
              tauFinalModel.add(
                  waveType,
                  sample0,
                  numSamples,
                  new TauXsample(n1, tau, x, ShellName.INNER_CORE_BOUNDARY));
            }

            if (TablesUtil.deBugLevel > 0) {
              System.out.format(
                  "lev2 %c %3d %3d %9.6f %8.6f\n",
                  waveType, tauFinalModel.size(waveType) - 1, iRay, sample0.z, sample0.slow);
            }
          } else {
            disc = true;
          }

          // Flag high slowness zones below discontinuities.
          if (sample1.slow > sample0.slow) {
            tauFinalModel.setLowVelocityZone(waveType);

            if (TablesUtil.deBugLevel > 0) {
              System.out.format(
                  "lvz  %c %3d %8.6f %8.6f\n", waveType, iRay, tau[iRay - 1], x[iRay - 1]);
            }
          }

          zLast = sample0.z;
        }
      }
    }

    // Save the integrals down to the center of the Earth.
    numSamples++;
    tauFinalModel.add(waveType, sample1, numSamples, new TauXsample(n1, tau, x, ShellName.CENTER));

    if (TablesUtil.deBugLevel > 0) {
      System.out.format(
          "lev3 %c %3d %3d %9.6f %8.6f\n",
          waveType, tauFinalModel.size(waveType) - 1, iRay, sample1.z, sample1.slow);
      tauFinalModel.printModel(waveType, "Final");
    }

    // We'll still need access to the merged slownesses.
    tauFinalModel.putSlowness(tauDepthModel.getSlowness());
  }

  /**
   * Function to find the bottoming depth of converted mantle phases. Because the P velocity is
   * higher than the S velocity, phases from the deepest source depth can't bottom any deeper.
   * However, P to S conversions can go much deeper (nearly to the core).
   *
   * @param waveType A char containing the wave waveType ('P' = P slowness, 'S' = S slowness)
   * @return The deepest depth that needs to be remembered for the travel-time computation
   */
  private double modelDepth(char waveType) {
    if (waveType == 'P') {
      return maximumDepth;
    } else {
      int j;
      for (j = 0; j < tauDepthModel.size('P'); j++) {
        if (tauDepthModel.getSample('P', j).z < maximumDepth) {
          break;
        }
      }

      double pLim = tauDepthModel.getSample('P', j).slow;
      if (TablesUtil.deBugLevel > 0) {
        System.out.format(
            "\ni maximumDepth pLim zm = %3d %9.6f %8.6f %9.6f\n",
            j, maximumDepth, pLim, tauDepthModel.getSample('P', j).z);
      }

      for (j = 0; j < tauDepthModel.size('S'); j++) {
        if (tauDepthModel.getSample('S', j).slow <= pLim) {
          break;
        }
      }

      if (TablesUtil.deBugLevel > 0) {
        System.out.format(
            "i pLim zLim = %3d %8.6f %9.6f\n", j, pLim, tauDepthModel.getSample('S', j).z);
      }

      return tauDepthModel.getSample('S', j).z;
    }
  }
}
