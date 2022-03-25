package gov.usgs.traveltime.tables;

import gov.usgs.traveltime.ModConvert;
import java.util.ArrayList;
import org.apache.commons.math3.analysis.solvers.PegasusSolver;
import org.apache.commons.math3.exception.NoBracketingException;

/**
 * The SampleSlowness class contains the slowness sampling The slowness sampling is much different
 * from the velocity sampling in the Earth model. In the Earth model, the sampling just needs to be
 * good enough for the spline fits to be reasonable. The slowness sampling is complex for a variety
 * of reasons. In addition to needing to sample the travel times (tau-p) adequately in distance, the
 * Earth model sampling in radius should be sampled finely enough to not miss any important detail
 * and the sampling in slowness must be regular enough to avoid stability problems in the
 * interpolation. To make it even more difficult, all desired phases are generated on the same
 * slowness sampling for production performance reasons. This means that the sampling of P, for
 * example, needs to be fine enough that the sampling of P'P' is also adequate even though the ray
 * travel distances are much larger for the same slowness grid. Note that the over sampling of
 * phases like P are then compensated by decimating the sampling at a later stage. Of course, the
 * decimated P sampling will be a subset of the P'P' sampling, which simplifies computation when
 * compensating for source depth.
 *
 * @author Ray Buland
 */
public class SampleSlowness {
  /** An integer containing the index of the deepest Earth model sample for this shell */
  private int deepestSampleIndex;

  /** A double containing the non-dimensional step length in range in kilometers */
  private double rangeStepLength;

  /** A double containing not quite the last radius in kilometers */
  private double lastRadius;

  /** An EarthModel object containing the reference earth model. */
  private EarthModel referenceModel;

  /** An EarthModel object containing the re-sampled Earth model */
  private EarthModel resampledModel;

  /** A TauModel object containing the tau-p model */
  private TauModel tauModel;

  /** A TauModel object containing the tau-p slowness depth model */
  private TauModel tauDepthModel;

  /** A ModConvert object containing the model dependant conversions */
  private ModConvert modelConversions;

  /** A TauInt object containing the Tau-X integration logic */
  private TauIntegrate tauInt;

  /** An array list of ModelSample objects containing the model */
  private ArrayList<ModelSample> model;

  /** An array list of ModelShell objects containing the model shell parameters */
  private ArrayList<ModelShell> shells;

  /**
   * An array list of CriticalSlowness objects containing the model criticalSlownesses slownesses
   */
  private ArrayList<CriticalSlowness> criticalSlownesses;

  /** An array list of TauSample objects containing the temporary model */
  private ArrayList<TauSample> temporaryModel;

  /**
   * An apache PegasusSolver object implementing the Pegasus method for root-finding (approximating
   * a zero of a univariate real function)
   */
  private PegasusSolver rootSolver;

  /**
   * A FindCaustic object used to calcuate the non-dimensional derivative of range (ray travel
   * distance) as a function of ray parameter.
   */
  private FindCaustic findCaustic;

  /**
   * A FindRange object used to calcuate the difference between a non-dimensional range (ray travel
   * distance) and a target range as a function of ray parameter.
   */
  private FindRange findRange;

  /**
   * A FindRange object used to calcuate the difference between a non-dimensional slowness and a
   * target slowness as a function of radius.
   */
  private FindRadius findRadius;

  /**
   * Get the slowness depth mode
   *
   * @return A TauModel object containing the slowness depth model
   */
  public TauModel getTauDepthModel() {
    return tauDepthModel;
  }

  /**
   * SampleSlowness Constructor The tau model will contain the output of the slowness sampling
   * process. Note that this goes in several steps. First, an adequate sampling is created for the
   * P- and S-wave slownesses independently. Then the two slowness samplings are merged. Finally,
   * the P- and S-wave slowness models are recreated using the merged sampling. The point of all
   * this is to allow converted phases using a common depth correction (i.e., the up-going branches
   * used to correct depth are on the same sampling for all possible phases).
   *
   * @param resampledModel An EarthModel object containing the re-sampled Earth model
   */
  public SampleSlowness(EarthModel resampledModel) {
    this.resampledModel = resampledModel;

    referenceModel = resampledModel.getReferenceModel();
    modelConversions = resampledModel.getModelConversions();
    model = resampledModel.getModel();
    shells = resampledModel.getShells();
    criticalSlownesses = resampledModel.getCriticalSlownesses();

    tauInt = new TauIntegrate(resampledModel, modelConversions);
    tauModel = new TauModel(referenceModel, modelConversions);
    tauDepthModel = new TauModel(referenceModel, modelConversions);
    temporaryModel = new ArrayList<TauSample>();

    // Create a root solver with default accuracy (1e-6).
    rootSolver = new PegasusSolver();
    findCaustic = new FindCaustic(tauInt);
    findRange = new FindRange(tauInt);
    findRadius = new FindRadius(referenceModel, modelConversions);
  }

  /**
   * Function to perform the sampling for both P and S velocities. Note that they will be merged at
   * a later stage.
   *
   * @param waveType A char containing the velocity/slowness wave type (P = P-wave, S = S-wave)
   * @throws Exception On an illegal integration interval
   */
  public void sample(char waveType) throws Exception {
    // Initialize temporary variables.
    int shellIndex = shells.size() - 1;
    ModelShell shell = shells.get(shellIndex);

    /*
     * Loop over criticalSlownesses points.  Because the criticalSlownesses points are branch
     * ends, this strategy guarantees that all possible branches are
     * sampled exactly at their ends.
     */
    double slowTop = model.get(shell.getTopSampleIndex()).getSlowness(waveType);
    for (int iCrit = criticalSlownesses.size() - 2; iCrit >= 0; iCrit--) {
      CriticalSlowness crit = criticalSlownesses.get(iCrit);

      if (crit.getSlowness() < slowTop) {
        // Set up limits for this shell.
        shellIndex = crit.getShellIndex(waveType);
        shell = shells.get(shellIndex);
        rangeStepLength = modelConversions.normR(shell.getRangeIncrementTarget());
        deepestSampleIndex = shell.getBottomSampleIndex();

        if (TablesUtil.deBugLevel > 0) {
          System.out.format(
              "\nShell: %3d %3d %6.2f %s\n",
              shellIndex, deepestSampleIndex, shell.getRangeIncrementTarget(), shell.getName());
        }

        double slowBot = crit.getSlowness();

        // Sample the top and bottom of this layer.
        temporaryModel.clear();
        tauInt.integrateDist(waveType, slowTop, deepestSampleIndex);
        double xTop = tauInt.getSummaryIntDist();
        double rTop = tauInt.getBottomingRadius();
        lastRadius = rTop;

        // We can save this one now.
        temporaryModel.add(new TauSample(rTop, slowTop, xTop));
        tauInt.integrateDist(waveType, slowBot, deepestSampleIndex);
        double xBot = tauInt.getSummaryIntDist();
        double rBot = tauInt.getBottomingRadius();

        // Figure out the initial sampling.
        int nSamp = Math.max((int) (Math.abs(xBot - xTop) / rangeStepLength + 0.8), 1);
        double dSlow = (slowTop - slowBot) / (nSamp * nSamp);
        double slowMin = (slowTop - slowBot) / nSamp;

        if (TablesUtil.deBugLevel > 1) {
          System.out.format("Samp: %3d " + "%10.4e %10.4e\n", nSamp, dSlow, slowMin);
        }

        for (int k = 1; k < nSamp; k++) {
          double slow = slowTop - Math.max(k * k * dSlow, k * slowMin);
          tauInt.integrateDist(waveType, slow, deepestSampleIndex);
          temporaryModel.add(
              new TauSample(tauInt.getBottomingRadius(), slow, tauInt.getSummaryIntDist()));
        }

        // Now save the bottom.
        temporaryModel.add(new TauSample(rBot, slowBot, xBot));

        // Check for hidden caustics.
        testSampling(waveType, dSlow);

        // Print it out for testing purposes.
        if (TablesUtil.deBugLevel > 0) {
          System.out.println("Temporary " + waveType + " slowness model:");

          for (int j = 0; j < temporaryModel.size(); j++) {
            System.out.format("%3d %s\n", j, temporaryModel.get(j));
          }
        }

        /*
         * Look for caustics.  Using dX/dp = 0 is slick for refining the
         * location of caustics, but it has problems.  First, it's infinite
         * at the top of every shell, which may lead to missing small
         * triplications.  Second, it has backwards compatibility problems
         * making testing against the Fortran version more difficult.  The
         * hybrid approach below isn't elegant, but resolves both issues.
         */
        int topSampleIndex = 0;
        TauSample sample1 = temporaryModel.get(0);
        TauSample sample2 = temporaryModel.get(1);
        for (int bottomSampleIndex = 1;
            bottomSampleIndex < temporaryModel.size() - 1;
            bottomSampleIndex++) {
          TauSample sample0 = sample1;
          sample1 = sample2;
          sample2 = temporaryModel.get(bottomSampleIndex + 1);

          if ((sample1.getRayTravelDistance() - sample0.getRayTravelDistance())
                  * (sample2.getRayTravelDistance() - sample1.getRayTravelDistance())
              <= 0d) {
            double pCaustic =
                getCaustic(
                    waveType, sample2.getSlowness(), sample0.getSlowness(), deepestSampleIndex);

            if (!Double.isNaN(pCaustic)) {
              tauInt.integrateDist(waveType, pCaustic, deepestSampleIndex);
              sample1 =
                  new TauSample(tauInt.getBottomingRadius(), pCaustic, tauInt.getSummaryIntDist());
              temporaryModel.set(bottomSampleIndex, sample1);

              if (TablesUtil.deBugLevel > 0) {
                System.out.format("\n Caustic: " + "%3d %s\n", bottomSampleIndex, sample1);
              }

              refineSampling(waveType, topSampleIndex, bottomSampleIndex);
              if (TablesUtil.deBugLevel > 0) {
                System.out.println();
              }

              topSampleIndex = bottomSampleIndex;
            } else {
              if (TablesUtil.deBugLevel > 0) {
                System.out.format("\n Warning: " + "caustic not found: %s\n", sample1);
              }
            }
          }
        }

        refineSampling(waveType, topSampleIndex, temporaryModel.size() - 1);
        slowTop = slowBot;
      }
    }
  }

  /**
   * Function to test sampling. If a shell only has two points, make sure there isn't a caustic
   * hiding inside. This is pretty crude, but seems to be adequate for the current range of models.
   *
   * @param waveType A char containing the velocity/slowness wave type (P = P-wave, S = S-wave)
   * @param slownessIncrement A double containing the Non-dimensional slowness increment
   * @throws Exception
   */
  private void testSampling(char waveType, double slownessIncrement) throws Exception {
    if (temporaryModel.size() == 2) {
      double slow = temporaryModel.get(0).getSlowness() - 0.25d * slownessIncrement;

      tauInt.integrateDist(waveType, slow, deepestSampleIndex);
      double x = tauInt.getSummaryIntDist();

      if (TablesUtil.deBugLevel > 0) {
        System.out.format("    extra = %8.6f " + "%8.6f\n", slow, x);
      }

      if ((x - temporaryModel.get(0).getRayTravelDistance())
              * (temporaryModel.get(1).getRayTravelDistance() - x)
          <= 0d) {
        temporaryModel.add(temporaryModel.get(1));
        temporaryModel.set(1, new TauSample(tauInt.getBottomingRadius(), slow, x));
      }
    }
  }

  /**
   * Function to find a caustic that has been bracketed. This works by finding a zero in the
   * derivative of distance with ray parameter. Note that ray parameter and slowness may appear to
   * be used interchangeably. Conceptually, we're shooting rays into the Earth at different angles
   * (summarized in the ray parameter) looking for the caustic. This ties into the model because we
   * want a model sample at the slowness where the caustic ray bottoms (i.e., where the slowness and
   * ray parameter are equal).
   *
   * @param waveType A char containing the velocity/slowness wave type (P = P-wave, S = S-wave)
   * @param minRayParam A double containing the non-dimensional ray parameter smaller than the
   *     caustic ray parameter
   * @param maxRayParam A double containing the non-dimensional ray parameter larger than the
   *     caustic ray parameter
   * @param deepestSampleIndex An integer holding the index of the deepest layer of the model to
   *     integrate to
   * @return A double containing the non-dimensional ray parameter at the caustic
   */
  private double getCaustic(
      char waveType, double minRayParam, double maxRayParam, int deepestSampleIndex) {
    double pCaustic;

    findCaustic.setUp(waveType, deepestSampleIndex);
    try {
      pCaustic = rootSolver.solve(TablesUtil.MAXEVAL, findCaustic, minRayParam, maxRayParam);

      if (TablesUtil.deBugLevel > 2) {
        System.out.format(
            "\tCaustic: %8.6f " + "[%8.6f,%8.6f] %2d\n",
            pCaustic, minRayParam, maxRayParam, rootSolver.getEvaluations());
      }
    } catch (NoBracketingException e) {
      pCaustic = Double.NaN;
    }

    return pCaustic;
  }

  /**
   * Function to refine the sampling. Based on the rough sample created above, refine the sampling
   * so that the model is reasonably sampled in range (ray travel distance) and radius.
   *
   * @param waveType A char containing the velocity/slowness wave type (P = P-wave, S = S-wave)
   * @param topSampleIndex An integer containing the starting temporary tau model sample index
   * @param bottomSampleIndex An integer containing the ending temporary tau model sample index
   * @throws Exception On an illegal integration interval
   */
  private void refineSampling(char waveType, int topSampleIndex, int bottomSampleIndex)
      throws Exception {

    TauSample sample0 = null;
    TauSample sample1;

    if (tauModel.size(waveType) == 0) {
      tauModel.add(waveType, temporaryModel.get(topSampleIndex));
    }

    // Figure out the initial sampling.
    double targetRange = temporaryModel.get(topSampleIndex).getRayTravelDistance();
    double xBot = temporaryModel.get(bottomSampleIndex).getRayTravelDistance();
    double pBot = temporaryModel.get(bottomSampleIndex).getSlowness();
    int nSamp = Math.max((int) (Math.abs(xBot - targetRange) / rangeStepLength + 0.8), 1);
    double dX = (xBot - targetRange) / nSamp;

    if (TablesUtil.deBugLevel > 1) {
      System.out.format("Samp: %2d %10.4e " + "%10.4e\n", nSamp, targetRange, dX);
    }

    /*
     *  Ambitious loop trying to optimize sampling in range, slowness,
     *  and radius.
     */
    int iTmp = topSampleIndex + 1;
    do {
      // Make a step.
      targetRange = targetRange + dX;

      if (TablesUtil.deBugLevel > 1)
        System.out.format("\txTarget dX: %8.6f " + "%10.4e\n", targetRange, dX);

      // Set the next range.
      if (Math.abs(targetRange - xBot) > TablesUtil.XTOL) {
        // Bracket the range.
        sample1 = temporaryModel.get(iTmp - 1);

        for (; iTmp <= bottomSampleIndex; iTmp++) {
          sample0 = sample1;
          sample1 = temporaryModel.get(iTmp);
          if ((targetRange - sample1.getRayTravelDistance())
                  * (targetRange - sample0.getRayTravelDistance())
              <= 0d) {
            break;
          }
        }

        // Test for some sort of odd failure.
        if (iTmp > bottomSampleIndex) {
          iTmp = bottomSampleIndex;
          System.out.format(
              "====> Off-the-end: %3d %8.6f %8.6f %8.6f\n",
              iTmp, targetRange, sample1.getRayTravelDistance(), sample0.getRayTravelDistance());
        }

        // Find the slowness that gives the desired range.
        double targetRayParam =
            getRange(
                waveType,
                sample1.getSlowness(),
                sample0.getSlowness(),
                targetRange,
                deepestSampleIndex);
        tauInt.integrateDist(waveType, targetRayParam, deepestSampleIndex);
        tauModel.add(
            waveType,
            new TauSample(tauInt.getBottomingRadius(), targetRayParam, tauInt.getSummaryIntDist()));

        if (TablesUtil.deBugLevel > 0) {
          System.out.format(
              "sol     %3d %s\n", tauModel.size(waveType) - 1, tauModel.getLast(waveType));
        }
      } else {
        // Add the last sample, though this may not be the end...
        tauModel.add(waveType, temporaryModel.get(bottomSampleIndex));
      }

      sample0 = tauModel.getSample(waveType, tauModel.size(waveType) - 2);
      sample1 = tauModel.getLast(waveType);

      // Make sure our slowness sampling is OK.
      if (Math.abs(sample1.getSlowness() - sample0.getSlowness()) > TablesUtil.DELPMAX) {
        // Oops!  Fix the last sample.
        nSamp =
            Math.max(
                (int) (Math.abs(pBot - sample0.getSlowness()) / TablesUtil.DELPMAX + 0.99d), 1);
        double targetRayParam = sample0.getSlowness() + (pBot - sample0.getSlowness()) / nSamp;
        tauInt.integrateDist(waveType, targetRayParam, deepestSampleIndex);
        sample1.update(tauInt.getBottomingRadius(), targetRayParam, tauInt.getSummaryIntDist());

        if (TablesUtil.deBugLevel > 0) {
          System.out.format(" dpmax  %3d %s\n", tauModel.size(waveType) - 1, sample1);
        }

        // Reset the range sampling.
        targetRange = sample1.getRayTravelDistance();
        nSamp = Math.max((int) (Math.abs(xBot - targetRange) / rangeStepLength + 0.8), 1);
        dX = (xBot - targetRange) / nSamp;

        if (TablesUtil.deBugLevel > 1) {
          System.out.format("Samp: %2d %10.4e " + "%10.4e\n", nSamp, targetRange, dX);
        }

        iTmp = topSampleIndex + 1;
      }

      // Make sure our radius sampling is OK too.
      if (Math.abs(sample1.getRadius() - lastRadius) > TablesUtil.DELRMAX) {
        // Oops!  Fix the last sample.
        double rTarget = sample0.getRadius() - TablesUtil.DELRMAX;
        int iRadius = tauInt.getBottomIndex();

        while (rTarget > resampledModel.getRadius(iRadius)) {
          iRadius++;
        }

        // Turn the target radius into a slowness increment.
        ModelSample model0 = resampledModel.getModel().get(iRadius);
        ModelSample model1 = resampledModel.getModel().get(iRadius - 1);
        double dSlow =
            Math.abs(
                sample0.getSlowness()
                    - model0.getSlowness(waveType)
                        * Math.pow(
                            rTarget / model0.getRadius(),
                            Math.log(model1.getSlowness(waveType) / model0.getSlowness(waveType))
                                / Math.log(model1.getRadius() / model0.getRadius())));

        // Do the fixing.
        nSamp = Math.max((int) (Math.abs(pBot - sample0.getSlowness()) / dSlow + 0.99d), 1);
        double targetRayParam = sample0.getSlowness() + (pBot - sample0.getSlowness()) / nSamp;
        tauInt.integrateDist(waveType, targetRayParam, deepestSampleIndex);
        sample1.update(tauInt.getBottomingRadius(), targetRayParam, tauInt.getSummaryIntDist());

        if (TablesUtil.deBugLevel > 0) {
          System.out.format(
              " drmax  %3d %s\n", tauModel.size(waveType) - 1, tauModel.getLast(waveType));
        }

        // Reset the range sampling.
        targetRange = sample1.getRayTravelDistance();
        nSamp = Math.max((int) (Math.abs(xBot - targetRange) / rangeStepLength + 0.8), 1);
        dX = (xBot - targetRange) / nSamp;

        if (TablesUtil.deBugLevel > 1) {
          System.out.format("Samp: %2d %10.4e +" + "%10.4e\n", nSamp, targetRange, dX);
        }

        iTmp = topSampleIndex + 1;
      }

      lastRadius = sample1.getRadius();
    } while (Math.abs(targetRange - xBot) > TablesUtil.XTOL);

    if (TablesUtil.deBugLevel > 0) {
      System.out.format(
          "end     %3d %s\n", tauModel.size(waveType) - 1, tauModel.getLast(waveType));
    }
  }

  /**
   * Function to find the non-dimensional ray parameter that results in a target non- dimensional
   * ray travel distance or range on the surface.
   *
   * @param waveType A char containing the velocity/slowness wave type (P = P-wave, S = S-wave)
   * @param minRayParam A double containing the non-dimensional ray parameter smaller than the ray
   *     parameter for the target range
   * @param maxRayParam A double containing the non-dimensional ray parameter larger than the ray
   *     parameter for the target range
   * @param targetRange A double containing the non-dimensional target range
   * @param deepestSampleIndex An integer holding the index of the deepest layer of the model to
   *     integrate to
   * @return A double containing the non-dimensional ray parameter of the ray with the target range
   */
  private double getRange(
      char waveType,
      double minRayParam,
      double maxRayParam,
      double targetRange,
      int deepestSampleIndex) {
    findRange.setUp(waveType, targetRange, deepestSampleIndex);
    double pRange = rootSolver.solve(TablesUtil.MAXEVAL, findRange, minRayParam, maxRayParam);

    if (TablesUtil.deBugLevel > 2) {
      System.out.format(
          "\tRange: %8.6f " + "[%8.6f,%8.6f] %2d\n",
          pRange, minRayParam, maxRayParam, rootSolver.getEvaluations());
    }

    return pRange;
  }

  /**
   * Function to create the depth model. Make yet another version of the model. In this case, we
   * don't care about range, but we want to sample the reference Earth model finely enough to make
   * the tau/x integrals accurate. This requires some chicanery in the upper part of the outer core
   * in order to sample the Earth model P-wave structure adequately. This is because the tau model
   * we just constructed misses this region (the first P-wave to penetrate the outer core bottoms in
   * the middle of the outer core because of the velocity drop). While building this model, we make
   * an effort to refine the radius associated with each slowness by going back to the reference
   * Earth model. This is because the radii we have so far depend on a power law interpolation of
   * slowness that isn't very realistic, but makes the tau/x integrals closed form.
   *
   * @param waveType A char containing the velocity/slowness wave type (P = P-wave, S = S-wave)
   * @throws Exception On an illegal integration interval
   */
  public void depthModel(char waveType) throws Exception {
    int iBeg, iEnd, i;
    ModelShell shell = null;

    // Initialize temporary variables.
    ArrayList<Double> slowness = tauModel.getSlowness();
    int iCur = shells.get(shells.size() - 1).getTopSampleIndex();
    double slowMax = resampledModel.getSlowness(waveType, iCur);

    // Find the starting slowness.
    for (iBeg = 0; iBeg < slowness.size(); iBeg++) {
      if (slowness.get(iBeg).equals(slowMax)) {
        break;
      }
    }

    // Add the top point of the model.
    tauDepthModel.add(waveType, resampledModel.getRadius(iCur), slowMax, iBeg);
    iBeg++;

    if (TablesUtil.deBugLevel > 0) {
      System.out.println();
    }

    /*
     * Loop over shells.
     */
    for (int shellIndex = shells.size() - 1; shellIndex >= 0; shellIndex--) {
      shell = shells.get(shellIndex);
      double slowTop = resampledModel.getSlowness(waveType, shell.getTopSampleIndex());
      double slowBot = resampledModel.getSlowness(waveType, shell.getBottomSampleIndex());

      // Only do shells that really have a slowness gradient.
      if (slowBot != slowTop) {
        if (slowBot < slowTop) {
          // Find the bottom of this shell in the merged slownesses.
          for (iEnd = iBeg; iEnd < slowness.size(); iEnd++) {
            if (slowness.get(iEnd).equals(slowBot)) {
              break;
            }
          }
        } else {
          // Find the bottom of this shell in the merged slownesses.
          for (iEnd = iBeg; iEnd >= 0; iEnd--) {
            if (slowness.get(iEnd).equals(slowBot)) {
              break;
            }
          }
        }

        if (TablesUtil.deBugLevel > 0) {
          System.out.format(
              "Merged indices: %3d %3d %8.6f %8.6f %3d %s\n",
              iBeg, iEnd, slowness.get(iBeg), slowness.get(iEnd), shellIndex, shell.getName());
        }

        // Fill in a new version of the model.
        if (!shell.getIsDiscontinuity()) {
          // This is messy for normal model shells.
          iCur = shell.getTopSampleIndex() - 1;
          double locSlow0 = resampledModel.getSlowness(waveType, shell.getTopSampleIndex());
          double locSlow1 = resampledModel.getSlowness(waveType, iCur);

          // Loop over the merged slownesses.
          i = iBeg;
          while (i != iEnd) {
            if (locSlow1 > locSlow0) {
              // This is a high slowness zone.
              i--;

              if (i == iEnd) {
                break;
              }

              double mergeSlow = slowness.get(i);

              if (mergeSlow == locSlow0) {
                mergeSlow = slowness.get(--i);
              }

              while (mergeSlow > locSlow1) {
                locSlow0 = locSlow1;
                locSlow1 = resampledModel.getSlowness(waveType, --iCur);
              }

              if (TablesUtil.deBugLevel > 2) {
                System.out.format(
                    "\tLVZ: locSlow = " + "%8.6f %8.6f mergeSlow = %8.6f\n",
                    locSlow0, locSlow1, mergeSlow);
              }

              double rFound =
                  getRadius(
                      waveType,
                      shellIndex,
                      resampledModel.getRadius(iCur),
                      resampledModel.getRadius(iCur + 1),
                      mergeSlow);
              tauDepthModel.add(waveType, rFound, mergeSlow, i);

              if (TablesUtil.deBugLevel > 1) {
                System.out.format("\tAdd: %3d " + "%7.2f %8.6f\n", i, rFound, slowness.get(i));
              }

              if (mergeSlow == locSlow1) {
                locSlow0 = locSlow1;
                locSlow1 = resampledModel.getSlowness(waveType, --iCur);

                if (locSlow1 <= locSlow0) {
                  i++;
                }
              }
            } else {
              // Slowness is decreasing normally.
              double mergeSlow = slowness.get(i);

              while (mergeSlow < locSlow1) {
                locSlow0 = locSlow1;
                locSlow1 = resampledModel.getSlowness(waveType, --iCur);
              }

              if (TablesUtil.deBugLevel > 2) {
                System.out.format(
                    "\t     locSlow = " + "%8.6f %8.6f mergeSlow = %8.6f\n",
                    locSlow0, locSlow1, mergeSlow);
              }

              double rFound =
                  getRadius(
                      waveType,
                      shellIndex,
                      resampledModel.getRadius(iCur),
                      resampledModel.getRadius(iCur + 1),
                      mergeSlow);

              tauDepthModel.add(waveType, rFound, mergeSlow, i);

              if (TablesUtil.deBugLevel > 1) {
                System.out.format("\tAdd: %3d " + "%7.2f %8.6f\n", i, rFound, slowness.get(i));
              }

              if (mergeSlow == locSlow1) {
                locSlow0 = locSlow1;
                locSlow1 = resampledModel.getSlowness(waveType, --iCur);
              }

              i++;
            }
          }

          // The last point is the bottom of the shell.
          iCur = shell.getBottomSampleIndex();
          tauDepthModel.add(waveType, resampledModel.getRadius(iCur), slowness.get(iEnd), iEnd);

          if (TablesUtil.deBugLevel > 1) {
            System.out.format(
                "\tAdd: %3d %7.2f " + "%8.6f\n",
                iEnd, resampledModel.getRadius(iCur), slowness.get(iEnd));
          }
        } else {
          // It's a lot easier for discontinuities.
          iCur = shell.getBottomSampleIndex();

          if (iEnd >= iBeg) {
            for (int j = iBeg; j <= iEnd; j++) {
              tauDepthModel.add(waveType, resampledModel.getRadius(iCur), slowness.get(j), j);

              if (TablesUtil.deBugLevel > 1) {
                System.out.format(
                    "\tAdd: %3d " + "%7.2f %8.6f\n",
                    j, resampledModel.getRadius(iCur), slowness.get(j));
              }
            }
          } else {
            for (int j = iBeg - 2; j >= iEnd; j--) {
              tauDepthModel.add(waveType, resampledModel.getRadius(iCur), slowness.get(j), j);

              if (TablesUtil.deBugLevel > 1) {
                System.out.format(
                    "\tAdd: %3d " + "%7.2f %8.6f\n",
                    j, resampledModel.getRadius(iCur), slowness.get(j));
              }
            }
          }
        }

        iBeg = ++iEnd;
      }
    }

    tauDepthModel.putSlowness(tauModel.getSlowness());
    tauDepthModel.makeDepthShells(waveType);
  }

  /**
   * Function to compute the Earth radius in kilometers that corresponds to the bottoming depth of a
   * ray with a desired target non-dimensional ray parameter. Note that we already have an Earth
   * radius corresponding to each slowness sample. However, this was computed from the internal
   * model by backing radius out from the flattened Earth model. Going back to the cubic spline
   * interpolation of the reference Earth model refines these values, which will be used for
   * correcting travel times for source depth.
   *
   * @param waveType A char containing the velocity/slowness wave type (P = P-wave, S = S-wave)
   * @param shellIndex An integer containing the shell index
   * @param minRadius A double containing the radius in kilometers corresponding to a slowness less
   *     than the target ray parameter
   * @param maxRadius A double containing the radius in kilometers corresponding to a slowness
   *     greater than the target ray parameter
   * @param targetRayParam A double holding the target ray parameter
   * @return A double holding the radius in kilometers where the reference Earth model slowness
   *     matches the desired ray parameter
   */
  private double getRadius(
      char waveType, int shellIndex, double minRadius, double maxRadius, double targetRayParam) {
    findRadius.setUp(waveType, shellIndex, targetRayParam);
    double radius = rootSolver.solve(TablesUtil.MAXEVAL, findRadius, minRadius, maxRadius);

    if (TablesUtil.deBugLevel > 2) {
      System.out.format(
          "\tRadius: %7.2f " + "[%7.2f,%7.2f] %2d\n",
          radius, minRadius, maxRadius, rootSolver.getEvaluations());
    }

    return radius;
  }

  /**
   * Function to merge the slowness samplings for P- and S-wave slowness Models into a single
   * sampling.
   */
  public void merge() {
    tauModel.merge(resampledModel);
  }

  /**
   * Function to print the slowness Earth model.
   *
   * @param waveType A char containing the velocity/slowness wave type (P = P-wave, S = S-wave)
   * @param version A String containing the model version ("Tau", "Depth", or "Final")
   */
  public void printModel(char waveType, String version) {
    if (version.equals("Depth")) {
      tauDepthModel.printModel(waveType, version);
    } else {
      tauModel.printModel(waveType, version);
    }
  }

  /** Function to print the merged slowness Earth model sampling. */
  public void printMergedSlownesses() {
    tauModel.printMergedSlownesses();
  }

  /**
   * Function to print the wave type specific shells for the depth model.
   *
   * @param A char containing the velocity/slowness wave type (P = P-wave, S = S-wave)
   */
  public void printShells(char waveType) {
    tauDepthModel.printDepthShells(waveType);
  }
}
