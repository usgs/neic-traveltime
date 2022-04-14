package gov.usgs.traveltime.tables;

import gov.usgs.traveltime.ModelConversions;
import gov.usgs.traveltime.ModelDataVolume;
import gov.usgs.traveltime.TauUtilities;

/**
 * The TauIntegrate class integrates tau and distance, x, along a ray path through the normalized,
 * flattened Earth model. Note that this extends and replaces the FORTRAN routine of the same name.
 *
 * @author Ray Buland
 */
public class TauIntegrate {
  /** An integer containing the index for the model sample above the bottoming radius. */
  private int bottomIndex = -1;

  /** A double containing the integrated distance for one layer */
  private double layerIntDist = 0d;

  /** A double containing the integrated distance for a range of layers. */
  private double summaryIntDist = 0d;

  /** A double containign the bottoming radius for a range of layers. */
  private double bottomingRadius = Double.NaN;

  /** A ModelDataVolume object containing the tau model data */
  private ModelDataVolume tauModel = null;

  /** An EarthModel object containing the earth model data */
  private EarthModel earthModel = null;

  /** A ModelConversions object containing model dependent constants and conversions */
  private ModelConversions modelConversions = null;

  /**
   * Get the integrated distance for one layer.
   *
   * @return A double containing the normalized distance for one layer
   */
  public double getLayerIntDist() {
    return layerIntDist;
  }

  /**
   * Get the integrated distance, X, for a range of layers.
   *
   * @return The normalized distance for a range of layers
   */
  public double getSummaryIntDist() {
    return summaryIntDist;
  }

  /**
   * Get the bottoming radius, R, for a range of layers.
   *
   * @return The dimensional bottoming Earth radius in kilometers
   */
  public double getBottomingRadius() {
    return bottomingRadius;
  }

  /**
   * Get the index for the model sample above the bottoming radius.
   *
   * @return Bottoming index
   */
  public int getBottomIndex() {
    return bottomIndex;
  }

  /**
   * TauIntegrate constructor. This form of the tau-range integration only allows direct calls to
   * integrateLayer and integrateDerivative. This is used in table generation for creating the raw
   * tau and x integrals.
   */
  public TauIntegrate() {}

  /**
   * TauIntegrate constructor, this constructor remembers the model data output from the table
   * generation. Note that this implies a separate tauInt for each model wave type. This form allows
   * range integration used in the travel-time computation for depth correction.
   *
   * @param tauModel Model data
   */
  public TauIntegrate(ModelDataVolume tauModel) {
    this.tauModel = tauModel;
  }

  /**
   * TauIntegrate constructor, this constructor remembers the model data input to the table
   * generation. The internal model knows both velocity models. This form allows integrateDist and
   * integrateTauDist used in table generation for constructing the slowness sampling.
   *
   * @param earthModel Model data
   * @param modelConversions Model dependent constants
   */
  public TauIntegrate(EarthModel earthModel, ModelConversions modelConversions) {
    this.earthModel = earthModel;
    this.modelConversions = modelConversions;
  }

  /**
   * Function to integrate tau and distance over a range of model layers from the output of the
   * model generation.
   *
   * @param rayParameter A double containing the normalized ray parameter
   * @param startLayerIndex An integer containing the starting model layer index
   * @param endLayerIndex An integer containing the ending model layer index
   * @return A double containing the normalized integrated tau
   * @throws TauIntegralException If tau or x is negative in any layer
   */
  public double integrateRange(double rayParameter, int startLayerIndex, int endLayerIndex)
      throws TauIntegralException {
    double tauSum = 0d;
    summaryIntDist = 0d;

    // Loop over grid points accumulating the integrals.
    for (int j = startLayerIndex; j < endLayerIndex; j++) {
      tauSum =
          tauSum
              + integrateLayer(
                  rayParameter,
                  tauModel.getP(j),
                  tauModel.getP(j + 1),
                  tauModel.getDepth(j),
                  tauModel.getDepth(j + 1));
      summaryIntDist += getLayerIntDist();
    }

    return tauSum;
  }

  /**
   * Function to integrate tau and distance over a range of model layers plus an additional
   * increment at the end from the output of the model generation.
   *
   * @param rayParameter A double containing the normalized ray parameter
   * @param startLayerIndex An integer containing the starting model layer index
   * @param endLayerIndex An integer containing the ending model layer index
   * @param lastSlowness A double containing the last normalized slowness
   * @param lastDepth A double containing the last normalized depth
   * @return A double containing the normalized integrated tau
   * @throws TauIntegralException If tau or x is negative in any layer
   */
  public double integrateRange(
      double rayParameter,
      int startLayerIndex,
      int endLayerIndex,
      double lastSlowness,
      double lastDepth)
      throws TauIntegralException {
    double tauSum = 0d;
    summaryIntDist = 0d;

    // Loop over grid points accumulating the integrals.
    for (int j = startLayerIndex; j < endLayerIndex; j++) {
      tauSum +=
          integrateLayer(
              rayParameter,
              tauModel.getP(j),
              tauModel.getP(j + 1),
              tauModel.getDepth(j),
              tauModel.getDepth(j + 1));
      summaryIntDist += getLayerIntDist();
    }

    // Add an increment at the end that's between grid points.
    tauSum +=
        integrateLayer(
            rayParameter,
            tauModel.getP(endLayerIndex),
            lastSlowness,
            tauModel.getDepth(endLayerIndex),
            lastDepth);
    summaryIntDist += getLayerIntDist();

    return tauSum;
  }

  /**
   * Function to integrate tau and distance over a range of model layers plus an additional
   * increment at the beginning and the end from the output of the model generation.
   *
   * @param rayParameter A double containing the normalized ray parameter
   * @param startLayerIndex An integer containing the starting model layer index
   * @param endLayerIndex An integer containing the ending model layer index
   * @param firstSlowness A double containing the first normalized slowness
   * @param firstDepth A double containing the first normalized depth
   * @param lastSlowness A double containing the last normalized slowness
   * @param lastDepth A double containing the last normalized depth
   * @return A double containing the normalized integrated tau
   * @throws TauIntegralException If tau or x is negative in any layer
   */
  public double integrateRange(
      double rayParameter,
      int startLayerIndex,
      int endLayerIndex,
      double firstSlowness,
      double firstDepth,
      double lastSlowness,
      double lastDepth)
      throws TauIntegralException {
    // Start with an increment at the beginning that's between grid
    // points.
    double tauSum =
        integrateLayer(
            rayParameter,
            firstSlowness,
            tauModel.getP(startLayerIndex),
            firstDepth,
            tauModel.getDepth(startLayerIndex));

    // Loop over grid points accumulating the integrals.
    for (int j = startLayerIndex; j < endLayerIndex; j++) {
      tauSum +=
          integrateLayer(
              rayParameter,
              tauModel.getP(j),
              tauModel.getP(j + 1),
              tauModel.getDepth(j),
              tauModel.getDepth(j + 1));
      summaryIntDist += getLayerIntDist();
    }

    // Add an increment at the end that's between grid points.
    tauSum +=
        integrateLayer(
            rayParameter,
            tauModel.getP(endLayerIndex),
            lastSlowness,
            tauModel.getDepth(endLayerIndex),
            lastDepth);
    summaryIntDist += getLayerIntDist();

    return tauSum;
  }

  /**
   * Function to integrate tau and distance over a range of model layers plus an additional
   * increment at the end from the input to the model generation.
   *
   * @param phaseType A char containing the model phase type (P = P slowness, S = S slowness)
   * @param rayParameter A double containing the normalized ray parameter
   * @param limitIndex An integer containing the index of the deepest layer of the model to
   *     integrate to
   * @return A double containing the normalized integrated tau
   * @throws TauIntegralException If tau or x is negative in any layer
   */
  public double integrateDist(char phaseType, double rayParameter, int limitIndex)
      throws TauIntegralException {
    int j;

    double tauSum = 0d;
    summaryIntDist = 0d;

    // Loop over grid points accumulating the P slowness integrals.
    for (j = earthModel.size() - 1; j > limitIndex; j--) {
      if (rayParameter > earthModel.getSlowness(phaseType, j - 1)) break;
      tauSum +=
          integrateLayer(
              rayParameter,
              earthModel.getSlowness(phaseType, j),
              earthModel.getSlowness(phaseType, j - 1),
              earthModel.getDepth(j),
              earthModel.getDepth(j - 1));
      summaryIntDist += getLayerIntDist();
      //		System.out.format("GetX:  %8.6f %8.6f %8.6f %9.6f %9.6f %9.6f\n", rayParameter,
      //				earthModel.getSlowness(phaseType, j), earthModel.getSlowness(phaseType, j-1),
      // earthModel.getDepth(j),
      //				earthModel.getDepth(j-1), getLayerIntDist());
    }

    // Handle the last bit.
    if (rayParameter > 0d) {
      bottomIndex = j;

      if (rayParameter < earthModel.getSlowness(phaseType, j)) {
        // Add an increment at the end that's between grid points.
        if (earthModel.getRadius(j - 1) > 0d) {
          bottomingRadius =
              earthModel.getRadius(j)
                  * Math.pow(
                      rayParameter / earthModel.getSlowness(phaseType, j),
                      Math.log(earthModel.getRadius(j - 1) / earthModel.getRadius(j))
                          / Math.log(
                              earthModel.getSlowness(phaseType, j - 1)
                                  / earthModel.getSlowness(phaseType, j)));
        } else {
          bottomingRadius =
              earthModel.getRadius(j) * (rayParameter / earthModel.getSlowness(phaseType, j));
        }

        double lastDepth = Math.log(modelConversions.xNorm * bottomingRadius);

        tauSum +=
            integrateLayer(
                rayParameter,
                earthModel.getSlowness(phaseType, j),
                rayParameter,
                earthModel.getDepth(j),
                lastDepth);
        summaryIntDist += getLayerIntDist();
        //			System.out.format("GetX:  %8.6f %8.6f %8.6f %9.6f %9.6f %9.6f\n", rayParameter,
        //					earthModel.getSlowness(phaseType, j), rayParameter, earthModel.getDepth(j),
        // lastDepth,
        // getLayerIntDist());
      } else {
        // We ended on a model sample.
        bottomingRadius = earthModel.getRadius(j);
      }
    } else {
      // Finish the straight through ray.
      bottomIndex = earthModel.size() - 1;
      bottomingRadius = 0d;
      summaryIntDist = getLayerIntDist(); // In this case, we get all of X here.
    }

    summaryIntDist *= 2d;

    return 2d * tauSum;
  }

  /**
   * Function to integrate tau and distance over a range of model layers plus an additional
   * increment at the end from the input to the model generation.
   *
   * @param phaseType A char containing the model phase type (P = P slowness, S = S slowness)
   * @param rayParameter A double containing the normalized ray parameter
   * @param limitIndex An integer containing the index of the deepest layer of the model to
   *     integrate to
   * @return A double containing the normalized integrated tau
   */
  public double integrateTauDist(char phaseType, double rayParameter, int limitIndex) {
    double x = 0d;
    double dXdPsum = 0d;
    int j;

    // Loop over grid points accumulating the P slowness integrals.
    for (j = earthModel.size() - 1; j > limitIndex; j--) {
      if (rayParameter > earthModel.getSlowness(phaseType, j - 1)) {
        break;
      }

      x =
          integrateDerivative(
              rayParameter,
              earthModel.getSlowness(phaseType, j),
              earthModel.getSlowness(phaseType, j - 1),
              earthModel.getDepth(j),
              earthModel.getDepth(j - 1));
      dXdPsum += x;
      //		System.out.format("GetDx: %8.6f %8.6f %8.6f %9.6f %9.6f %9.6f\n", rayParameter,
      //				earthModel.getSlowness(phaseType, j), earthModel.getSlowness(phaseType, j-1),
      // earthModel.getDepth(j),
      //				earthModel.getDepth(j-1), x);
    }

    // Handle the last bit.
    if (rayParameter < earthModel.getSlowness(phaseType, j)) {
      // Add an increment at the end that's between grid points.
      bottomingRadius =
          earthModel.getRadius(j)
              * Math.pow(
                  rayParameter / earthModel.getSlowness(phaseType, j),
                  Math.log(earthModel.getRadius(j - 1) / earthModel.getRadius(j))
                      / Math.log(
                          earthModel.getSlowness(phaseType, j - 1)
                              / earthModel.getSlowness(phaseType, j)));

      double lastDepth = Math.log(modelConversions.xNorm * bottomingRadius);

      x =
          integrateDerivative(
              rayParameter,
              earthModel.getSlowness(phaseType, j),
              rayParameter,
              earthModel.getDepth(j),
              lastDepth);
      dXdPsum += x;
      //		System.out.format("GetDx: %8.6f %8.6f %8.6f %9.6f %9.6f %9.6f\n", rayParameter,
      //				earthModel.getSlowness(phaseType, j), rayParameter, earthModel.getDepth(j), lastDepth,
      // x);
    } else {
      // We ended on a model sample.
      bottomingRadius = earthModel.getRadius(j);
    }

    return 2d * dXdPsum;
  }

  /**
   * Function to integrate tau and distance over one layer. Note that the plethora of special cases
   * arose from years of bitter experience.
   *
   * @param rayParameter A double containing the normalized ray parameter
   * @param topSlowness A double containing the normalized slowness at the top of the layer
   * @param bottomSlowness A double containing the normalized slowness at the bottom of the layer
   * @param topDepth A double containing the normalized depth at the top of the layer
   * @param bottomDepth A double containing the normalized depth at the bottom of the layer
   * @return A double containing the normalized tau
   * @throws TauIntegralException If tau or x is negative
   */
  public double integrateLayer(
      double rayParameter,
      double topSlowness,
      double bottomSlowness,
      double topDepth,
      double bottomDepth)
      throws TauIntegralException {

    // Handle a zero thickness layer (discontinuity).
    if (Math.abs(topDepth - bottomDepth) <= TauUtilities.DTOL) {
      layerIntDist = 0d;
      return 0d;
    }

    // Handle a constant slowness layer.
    if (Math.abs(topSlowness - bottomSlowness) <= TauUtilities.DTOL) {
      if (Math.abs(rayParameter - topSlowness) <= TauUtilities.DTOL) {
        layerIntDist = 0d;
        return 0d;
      } else {
        double b = Math.abs(topDepth - bottomDepth);
        double pTop2 =
            Math.sqrt(Math.abs(Math.pow(topSlowness, 2d) - Math.pow(bottomSlowness, 2d)));
        layerIntDist = b * rayParameter / pTop2;
        return b * pTop2;
      }
    }

    // Handle the straight through ray at the center.
    if (rayParameter <= TauUtilities.DTOL && bottomSlowness <= TauUtilities.DTOL) {
      layerIntDist = Math.PI / 2d; // Accumulate all of x in the last layer.
      return topSlowness;
    }

    double b =
        topSlowness - (bottomSlowness - topSlowness) / (Math.exp(bottomDepth - topDepth) - 1d);

    if (TablesUtil.deBugLevel > 2) {
      System.out.println(
          "b: "
              + topSlowness
              + " "
              + bottomSlowness
              + " "
              + (float) (bottomSlowness - topSlowness)
              + " "
              + (float) (bottomDepth - topDepth)
              + " "
              + (float) (Math.exp(bottomDepth - topDepth) - 1d)
              + " "
              + (float) b);
    }

    // Handle the straight through ray elsewhere.
    if (rayParameter <= TauUtilities.DTOL) {
      double tau =
          -(bottomSlowness
              - topSlowness
              + b * Math.log(bottomSlowness / topSlowness)
              - b
                  * Math.log(
                      Math.max(
                          (topSlowness - b) * bottomSlowness / ((bottomSlowness - b) * topSlowness),
                          TauUtilities.DMIN)));

      layerIntDist = 0d;
      validateTau(rayParameter, topSlowness, bottomSlowness, topDepth, bottomDepth, tau);

      return tau;
    }

    // The ray parameter is equal to the layer bottom slowness.
    if (rayParameter == bottomSlowness) {
      double p2 = Math.pow(rayParameter, 2d);
      double pTop2 = Math.sqrt(Math.abs(Math.pow(topSlowness, 2d) - p2));
      double b2 = Math.sqrt(Math.abs(Math.pow(b, 2d) - p2));
      double xInt;

      if (Math.pow(b, 2d) >= p2) {
        xInt =
            Math.log(
                Math.max(
                    (topSlowness - b)
                        * (b * bottomSlowness - p2)
                        / ((bottomSlowness - b) * (b2 * pTop2 + b * topSlowness - p2)),
                    TauUtilities.DMIN));
        layerIntDist = bottomSlowness * xInt / b2;
      } else {
        xInt =
            Math.copySign(Math.PI / 2d, b - bottomSlowness)
                - Math.asin(
                    Math.max(
                        Math.min(
                            (b * topSlowness - p2) / (bottomSlowness * Math.abs(topSlowness - b)),
                            1d),
                        -1d));
        layerIntDist = -bottomSlowness * xInt / b2;
      }

      double tau = -(b * Math.log(bottomSlowness / (topSlowness + pTop2)) - pTop2 - b2 * xInt);
      validateTau(rayParameter, topSlowness, bottomSlowness, topDepth, bottomDepth, tau);

      return tau;

      // The ray parameter is equal to the layer top slowness.
    } else if (rayParameter == topSlowness) {
      double p2 = Math.pow(rayParameter, 2d);
      double pBot2 = Math.sqrt(Math.abs(Math.pow(bottomSlowness, 2d) - p2));
      double b2 = Math.sqrt(Math.abs(Math.pow(b, 2d) - p2));
      double xInt;

      if (Math.pow(b, 2d) >= p2) {
        xInt =
            Math.log(
                Math.max(
                    (topSlowness - b)
                        * (b2 * pBot2 + b * bottomSlowness - p2)
                        / ((bottomSlowness - b) * (b * topSlowness - p2)),
                    TauUtilities.DMIN));
        layerIntDist = topSlowness * xInt / b2;
      } else {
        xInt =
            Math.asin(
                    Math.max(
                        Math.min(
                            (b * bottomSlowness - p2)
                                / (topSlowness * Math.abs(bottomSlowness - b)),
                            1d),
                        -1d))
                - Math.copySign(Math.PI / 2d, b - topSlowness);
        layerIntDist = -topSlowness * xInt / b2;
      }

      double tau = -(b * Math.log((bottomSlowness + pBot2) / topSlowness) + pBot2 - b2 * xInt);
      validateTau(rayParameter, topSlowness, bottomSlowness, topDepth, bottomDepth, tau);

      return tau;
    }

    // Finally, handle the general case.
    double p2 = Math.pow(rayParameter, 2d);
    double pBot2 = Math.sqrt(Math.abs(Math.pow(bottomSlowness, 2d) - p2));
    double pTop2 = Math.sqrt(Math.abs(Math.pow(topSlowness, 2d) - p2));
    double bSq = Math.pow(b, 2d);
    double b2 = Math.sqrt(Math.abs(bSq - p2));
    double xInt;

    if (TablesUtil.deBugLevel > 2) {
      System.out.println(
          "b p2 pBot2 pTop2 b2 = "
              + (float) b
              + " "
              + (float) p2
              + " "
              + (float) pBot2
              + " "
              + (float) pTop2
              + " "
              + (float) b2);
    }

    if (b2 <= TauUtilities.DMIN) {
      xInt = 0d;
      layerIntDist =
          rayParameter
              * (Math.sqrt(Math.abs((bottomSlowness + b) / (bottomSlowness - b)))
                  - Math.sqrt(Math.abs((topSlowness + b) / (topSlowness - b))))
              / b;
    } else if (bSq >= p2) {
      xInt =
          Math.log(
              Math.max(
                  (topSlowness - b)
                      * (b2 * pBot2 + b * bottomSlowness - p2)
                      / ((bottomSlowness - b) * (b2 * pTop2 + b * topSlowness - p2)),
                  TauUtilities.DMIN));

      if (TablesUtil.deBugLevel > 2) {
        System.out.println(
            "bSq >= p2: "
                + (float) ((topSlowness - b) * (b2 * pBot2 + b * bottomSlowness - p2))
                + " "
                + (float) ((bottomSlowness - b) * (b2 * pTop2 + b * topSlowness - p2))
                + " "
                + (float)
                    Math.log(
                        (topSlowness - b)
                            * (b2 * pBot2 + b * bottomSlowness - p2)
                            / ((bottomSlowness - b) * (b2 * pTop2 + b * topSlowness - p2))));
      }

      layerIntDist = rayParameter * xInt / b2;
    } else {
      xInt =
          Math.asin(
                  Math.max(
                      Math.min(
                          (b * bottomSlowness - p2) / (rayParameter * Math.abs(bottomSlowness - b)),
                          1d),
                      -1d))
              - Math.asin(
                  Math.max(
                      Math.min(
                          (b * topSlowness - p2) / (rayParameter * Math.abs(topSlowness - b)), 1d),
                      -1d));
      if (TablesUtil.deBugLevel > 2) {
        System.out.println(
            "Bot: "
                + (float) (b * bottomSlowness - p2)
                + " "
                + (float) (rayParameter * Math.abs(bottomSlowness - b))
                + " "
                + (float) Math.asin(b * bottomSlowness - p2)
                    / (rayParameter * Math.abs(bottomSlowness - b)));

        System.out.println(
            "Top: "
                + (float) (b * topSlowness - p2)
                + " "
                + (float) (rayParameter * Math.abs(topSlowness - b))
                + " "
                + (float) Math.asin(b * topSlowness - p2)
                    / (rayParameter * Math.abs(topSlowness - b)));
      }

      layerIntDist = -rayParameter * xInt / b2;
    }

    double tau =
        -(pBot2
            - pTop2
            + b * Math.log((bottomSlowness + pBot2) / (topSlowness + pTop2))
            - b2 * xInt);

    if (TablesUtil.deBugLevel > 2) {
      System.out.println(
          "tau xInt layerIntDist = "
              + (float) tau
              + " "
              + (float) xInt
              + " "
              + (float) layerIntDist);
    }

    validateTau(rayParameter, topSlowness, bottomSlowness, topDepth, bottomDepth, tau);

    return tau;
  }

  /**
   * Function to compute the derivative of ray travel distance with respect to ray parameter for one
   * mode layer.
   *
   * @param rayParameter A double containing the normalized ray parameter
   * @param topSlowness A double containing the normalized slowness at the top of the layer
   * @param bottomSlowness A double containing the normalized slowness at the bottom of the layer
   * @param topDepth A double containing the normalized depth at the top of the layer
   * @param bottomDepth A double containing the normalized depth at the bottom of the layer
   * @return A double containing the normalized dX/dp
   */
  public double integrateDerivative(
      double rayParameter,
      double topSlowness,
      double bottomSlowness,
      double topDepth,
      double bottomDepth) {
    // Handle a zero thickness layer (discontinuity) or a constant
    // slowness layer.
    if (Math.abs(topDepth - bottomDepth) <= TauUtilities.DTOL
        || Math.abs(topSlowness - bottomSlowness) <= TauUtilities.DTOL) {
      return 0d;
    }

    double p2 = Math.pow(rayParameter, 2d);
    double pTop2 = Math.pow(topSlowness, 2d);

    // Handle a bottoming ray.
    if (Math.abs(rayParameter - bottomSlowness) <= TauUtilities.DTOL) {
      return (bottomDepth - topDepth)
          * (1d / Math.sqrt(Math.abs(pTop2 - p2)))
          / Math.log(topSlowness / bottomSlowness);
    }

    // Do the general case.
    double pBot2 = Math.pow(bottomSlowness, 2d);
    return (bottomDepth - topDepth)
        * (1d / Math.sqrt(Math.abs(pTop2 - p2)) - 1d / Math.sqrt(Math.abs(pBot2 - p2)))
        / Math.log(topSlowness / bottomSlowness);
  }

  /**
   * Function to validate the tau and distance layer integral.
   *
   * @param rayParameter A double containing the normalized ray parameter
   * @param topSlowness A double containing the normalized slowness at the top of the layer
   * @param bottomSlowness A double containing the normalized slowness at the bottom of the layer
   * @param topDepth A double containing the normalized depth at the top of the layer
   * @param bottomDepth A double containing the normalized depth at the bottom of the layer
   * @param tau A double containing the normalized tau
   * @throws TauIntegralException If tau or x is negative
   */
  private void validateTau(
      double rayParameter,
      double topSlowness,
      double bottomSlowness,
      double topDepth,
      double bottomDepth,
      double tau)
      throws TauIntegralException {
    if (tau < -TauUtilities.TAUINTTOL) {
      System.out.format(
          "***** Bad tau: rayParameter = %8.6f, topSlowness = %8.6f, "
              + "bottomSlowness = %8.6f, topDepth = %9.6f, bottomDepth = %9.6f, tau = %11.4e, "
              + "TAUINTTOL=%11.4e, x = %11.4e\n",
          rayParameter,
          topSlowness,
          bottomSlowness,
          topDepth,
          bottomDepth,
          tau,
          -TauUtilities.TAUINTTOL,
          layerIntDist);

      if (rayParameter > topSlowness) {
        System.out.println("***** rayParameter too big to penetrate to this layer!");
      }

      throw new TauIntegralException("Partial integrals cannot be negative");
      /*	} else if(layerIntDist < -TauUtilities.DMIN) {
      System.out.format("***** Bad x: rayParameter = %8.6f, topSlowness = %8.6f, "+
      		"bottomSlowness = %8.6f, topDepth = %9.6f, bottomDepth = %9.6f, tau = %11.4e, "+
      		"x = %11.4e\n", rayParameter, topSlowness, bottomSlowness, topDepth, bottomDepth, tau, layerIntDist);
      throw new Exception(); */
    }
  }
}
