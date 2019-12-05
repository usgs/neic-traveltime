package gov.usgs.traveltime.tables;

import gov.usgs.traveltime.ModConvert;
import gov.usgs.traveltime.ModDataVol;
import gov.usgs.traveltime.TauUtil;

/**
 * Integrate tau and distance, x, along a ray path through the normalized, flattened Earth model.
 * Note that this extends and replaces the FORTRAN routine of the same name.
 *
 * @author Ray Buland
 */
public class TauInt {
  int iBottom = -1;
  double xLayer = 0d, xSum = 0d, rBottom = Double.NaN;
  ModDataVol tauModel = null;
  EarthModel tabModel = null;
  ModConvert convert = null;

  /**
   * This form of the tau-range integration only allows direct calls to intLayer and intDeriv. This
   * is used in table generation for creating the raw tau and x integrals.
   */
  public TauInt() {}

  /**
   * The constructor remembers the model data output from the table generation. Note that this
   * implies a separate tauInt for each model wave type. This form allows range integration used in
   * the travel-time computation for depth correction.
   *
   * @param tauModel Model data
   */
  public TauInt(ModDataVol tauModel) {
    this.tauModel = tauModel;
  }

  /**
   * The constructor remembers the model data input to the table generation. The internal model
   * knows both velocity models. This form allows intX and intDxDp used in table generation for
   * constructing the slowness sampling.
   *
   * @param tabModel Model data
   * @param convert Model dependent constants
   */
  public TauInt(EarthModel tabModel, ModConvert convert) {
    this.tabModel = tabModel;
    this.convert = convert;
  }

  /**
   * Integrate tau and distance over a range of model layers from the output of the model
   * generation.
   *
   * @param p Normalized ray parameter
   * @param start Starting model layer index
   * @param end Ending model layer index
   * @return Normalized integrated tau
   * @throws TauIntegralException If tau or x is negative in any layer
   */
  public double intRange(double p, int start, int end) throws TauIntegralException {
    double tauSum;

    tauSum = 0d;
    xSum = 0d;
    // Loop over grid points accumulating the integrals.
    for (int j = start; j < end; j++) {
      tauSum =
          tauSum
              + intLayer(
                  p,
                  tauModel.getP(j),
                  tauModel.getP(j + 1),
                  tauModel.getZ(j),
                  tauModel.getZ(j + 1));
      xSum += getXLayer();
    }
    return tauSum;
  }

  /**
   * Integrate tau and distance over a range of model layers plus an additional increment at the end
   * from the output of the model generation.
   *
   * @param p Normalized ray parameter
   * @param start Starting model layer index
   * @param end Ending model layer index
   * @param pLast Last normalized slowness
   * @param zLast Last normalized depth
   * @return Normalized integrated tau
   * @throws TauIntegralException If tau or x is negative in any layer
   */
  public double intRange(double p, int start, int end, double pLast, double zLast)
      throws TauIntegralException {
    double tauSum;

    tauSum = 0d;
    xSum = 0d;
    // Loop over grid points accumulating the integrals.
    for (int j = start; j < end; j++) {
      tauSum +=
          intLayer(
              p, tauModel.getP(j), tauModel.getP(j + 1), tauModel.getZ(j), tauModel.getZ(j + 1));
      xSum += getXLayer();
    }
    // Add an increment at the end that's between grid points.
    tauSum += intLayer(p, tauModel.getP(end), pLast, tauModel.getZ(end), zLast);
    xSum += getXLayer();
    return tauSum;
  }

  /**
   * Integrate tau and distance over a range of model layers plus an additional increment at the
   * beginning and the end from the output of the model generation.
   *
   * @param p Normalized ray parameter
   * @param start Starting model layer index
   * @param end Ending model layer index
   * @param pFirst First normalized slowness
   * @param zFirst First normalized depth
   * @param pLast Last normalized slowness
   * @param zLast Last normalized depth
   * @return Normalized integrated tau
   * @throws TauIntegralException If tau or x is negative in any layer
   */
  public double intRange(
      double p, int start, int end, double pFirst, double zFirst, double pLast, double zLast)
      throws TauIntegralException {
    double tauSum;

    // Start with an increment at the beginning that's between grid
    // points.
    tauSum = intLayer(p, pFirst, tauModel.getP(start), zFirst, tauModel.getZ(start));
    // Loop over grid points accumulating the integrals.
    for (int j = start; j < end; j++) {
      tauSum +=
          intLayer(
              p, tauModel.getP(j), tauModel.getP(j + 1), tauModel.getZ(j), tauModel.getZ(j + 1));
      xSum += getXLayer();
    }
    // Add an increment at the end that's between grid points.
    tauSum += intLayer(p, tauModel.getP(end), pLast, tauModel.getZ(end), zLast);
    xSum += getXLayer();
    return tauSum;
  }

  /**
   * Integrate tau and distance over a range of model layers plus an additional increment at the end
   * from the input to the model generation.
   *
   * @param type Slowness type (P = P slowness, S = S slowness)
   * @param p Normalized ray parameter
   * @param limit Index of the deepest layer of the model to integrate to
   * @return Normalized integrated tau
   * @throws TauIntegralException If tau or x is negative in any layer
   */
  public double intX(char type, double p, int limit) throws TauIntegralException {
    double tauSum, zLast;
    int j;

    tauSum = 0d;
    xSum = 0d;
    // Loop over grid points accumulating the P slowness integrals.
    for (j = tabModel.size() - 1; j > limit; j--) {
      if (p > tabModel.getSlow(type, j - 1)) break;
      tauSum +=
          intLayer(
              p,
              tabModel.getSlow(type, j),
              tabModel.getSlow(type, j - 1),
              tabModel.getZ(j),
              tabModel.getZ(j - 1));
      xSum += getXLayer();
      //		System.out.format("GetX:  %8.6f %8.6f %8.6f %9.6f %9.6f %9.6f\n", p,
      //				tabModel.getSlow(type, j), tabModel.getSlow(type, j-1), tabModel.getZ(j),
      //				tabModel.getZ(j-1), getXLayer());
    }
    // Handle the last bit.
    if (p > 0d) {
      iBottom = j;
      if (p < tabModel.getSlow(type, j)) {
        // Add an increment at the end that's between grid points.
        if (tabModel.getR(j - 1) > 0d) {
          rBottom =
              tabModel.getR(j)
                  * Math.pow(
                      p / tabModel.getSlow(type, j),
                      Math.log(tabModel.getR(j - 1) / tabModel.getR(j))
                          / Math.log(tabModel.getSlow(type, j - 1) / tabModel.getSlow(type, j)));
        } else {
          rBottom = tabModel.getR(j) * (p / tabModel.getSlow(type, j));
        }
        zLast = Math.log(convert.xNorm * rBottom);
        tauSum += intLayer(p, tabModel.getSlow(type, j), p, tabModel.getZ(j), zLast);
        xSum += getXLayer();
        //			System.out.format("GetX:  %8.6f %8.6f %8.6f %9.6f %9.6f %9.6f\n", p,
        //					tabModel.getSlow(type, j), p, tabModel.getZ(j), zLast, getXLayer());
      } else {
        // We ended on a model sample.
        rBottom = tabModel.getR(j);
      }
    } else {
      // Finish the straight through ray.
      iBottom = tabModel.size() - 1;
      rBottom = 0d;
      xSum = getXLayer(); // In this case, we get all of X here.
    }
    xSum *= 2d;
    return 2d * tauSum;
  }

  /**
   * Integrate tau and distance over a range of model layers plus an additional increment at the end
   * from the input to the model generation.
   *
   * @param type Slowness type (P = P slowness, S = S slowness)
   * @param p Normalized ray parameter
   * @param limit Index of the deepest layer of the model to integrate to
   * @return Normalized integrated tau
   */
  public double intDxDp(char type, double p, int limit) {
    double zLast, x, dXdPsum = 0d;
    int j;

    // Loop over grid points accumulating the P slowness integrals.
    for (j = tabModel.size() - 1; j > limit; j--) {
      if (p > tabModel.getSlow(type, j - 1)) break;
      x =
          intDeriv(
              p,
              tabModel.getSlow(type, j),
              tabModel.getSlow(type, j - 1),
              tabModel.getZ(j),
              tabModel.getZ(j - 1));
      dXdPsum += x;
      //		System.out.format("GetDx: %8.6f %8.6f %8.6f %9.6f %9.6f %9.6f\n", p,
      //				tabModel.getSlow(type, j), tabModel.getSlow(type, j-1), tabModel.getZ(j),
      //				tabModel.getZ(j-1), x);
    }
    // Handle the last bit.
    if (p < tabModel.getSlow(type, j)) {
      // Add an increment at the end that's between grid points.
      rBottom =
          tabModel.getR(j)
              * Math.pow(
                  p / tabModel.getSlow(type, j),
                  Math.log(tabModel.getR(j - 1) / tabModel.getR(j))
                      / Math.log(tabModel.getSlow(type, j - 1) / tabModel.getSlow(type, j)));
      zLast = Math.log(convert.xNorm * rBottom);
      x = intDeriv(p, tabModel.getSlow(type, j), p, tabModel.getZ(j), zLast);
      dXdPsum += x;
      //		System.out.format("GetDx: %8.6f %8.6f %8.6f %9.6f %9.6f %9.6f\n", p,
      //				tabModel.getSlow(type, j), p, tabModel.getZ(j), zLast, x);
    } else {
      // We ended on a model sample.
      rBottom = tabModel.getR(j);
    }
    return 2d * dXdPsum;
  }

  /**
   * Integrate tau and distance over one layer. Note that the plethora of special cases arose from
   * years of bitter experience.
   *
   * @param p Normalized ray parameter
   * @param pTop Normalized slowness at the top of the layer
   * @param pBot Normalized slowness at the bottom of the layer
   * @param zTop Normalized depth at the top of the layer
   * @param zBot Normalized depth at the bottom of the layer
   * @return Normalized tau
   * @throws TauIntegralException If tau or x is negative
   */
  public double intLayer(double p, double pTop, double pBot, double zTop, double zBot)
      throws TauIntegralException {
    double tau, b, p2, pTop2, pBot2, bSq, b2, xInt;

    // Handle a zero thickness layer (discontinuity).
    if (Math.abs(zTop - zBot) <= TauUtil.DTOL) {
      xLayer = 0d;
      return 0d;
    }

    // Handle a constant slowness layer.
    if (Math.abs(pTop - pBot) <= TauUtil.DTOL) {
      if (Math.abs(p - pTop) <= TauUtil.DTOL) {
        xLayer = 0d;
        return 0d;
      } else {
        b = Math.abs(zTop - zBot);
        pTop2 = Math.sqrt(Math.abs(Math.pow(pTop, 2d) - Math.pow(pBot, 2d)));
        xLayer = b * p / pTop2;
        return b * pTop2;
      }
    }

    // Handle the straight through ray at the center.
    if (p <= TauUtil.DTOL && pBot <= TauUtil.DTOL) {
      xLayer = Math.PI / 2d; // Accumulate all of x in the last layer.
      return pTop;
    }
    b = pTop - (pBot - pTop) / (Math.exp(zBot - zTop) - 1d);
    if (TablesUtil.deBugLevel > 2)
      System.out.println(
          "b: "
              + pTop
              + " "
              + pBot
              + " "
              + (float) (pBot - pTop)
              + " "
              + (float) (zBot - zTop)
              + " "
              + (float) (Math.exp(zBot - zTop) - 1d)
              + " "
              + (float) b);
    // Handle the straight through ray elsewhere.
    if (p <= TauUtil.DTOL) {
      tau =
          -(pBot
              - pTop
              + b * Math.log(pBot / pTop)
              - b * Math.log(Math.max((pTop - b) * pBot / ((pBot - b) * pTop), TauUtil.DMIN)));
      xLayer = 0d;
      tauTest(p, pTop, pBot, zTop, zBot, tau);
      return tau;
    }

    // The ray parameter is equal to the layer bottom slowness.
    if (p == pBot) {
      p2 = Math.pow(p, 2d);
      pTop2 = Math.sqrt(Math.abs(Math.pow(pTop, 2d) - p2));
      b2 = Math.sqrt(Math.abs(Math.pow(b, 2d) - p2));
      if (Math.pow(b, 2d) >= p2) {
        xInt =
            Math.log(
                Math.max(
                    (pTop - b) * (b * pBot - p2) / ((pBot - b) * (b2 * pTop2 + b * pTop - p2)),
                    TauUtil.DMIN));
        xLayer = pBot * xInt / b2;
      } else {
        xInt =
            Math.copySign(Math.PI / 2d, b - pBot)
                - Math.asin(
                    Math.max(Math.min((b * pTop - p2) / (pBot * Math.abs(pTop - b)), 1d), -1d));
        xLayer = -pBot * xInt / b2;
      }
      tau = -(b * Math.log(pBot / (pTop + pTop2)) - pTop2 - b2 * xInt);
      tauTest(p, pTop, pBot, zTop, zBot, tau);
      return tau;

      // The ray parameter is equal to the layer top slowness.
    } else if (p == pTop) {
      p2 = Math.pow(p, 2d);
      pBot2 = Math.sqrt(Math.abs(Math.pow(pBot, 2d) - p2));
      b2 = Math.sqrt(Math.abs(Math.pow(b, 2d) - p2));
      if (Math.pow(b, 2d) >= p2) {
        xInt =
            Math.log(
                Math.max(
                    (pTop - b) * (b2 * pBot2 + b * pBot - p2) / ((pBot - b) * (b * pTop - p2)),
                    TauUtil.DMIN));
        xLayer = pTop * xInt / b2;
      } else {
        xInt =
            Math.asin(Math.max(Math.min((b * pBot - p2) / (pTop * Math.abs(pBot - b)), 1d), -1d))
                - Math.copySign(Math.PI / 2d, b - pTop);
        xLayer = -pTop * xInt / b2;
      }
      tau = -(b * Math.log((pBot + pBot2) / pTop) + pBot2 - b2 * xInt);
      tauTest(p, pTop, pBot, zTop, zBot, tau);
      return tau;
    }

    // Finally, handle the general case.
    p2 = Math.pow(p, 2d);
    pBot2 = Math.sqrt(Math.abs(Math.pow(pBot, 2d) - p2));
    pTop2 = Math.sqrt(Math.abs(Math.pow(pTop, 2d) - p2));
    bSq = Math.pow(b, 2d);
    b2 = Math.sqrt(Math.abs(bSq - p2));
    if (TablesUtil.deBugLevel > 2)
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
    if (b2 <= TauUtil.DMIN) {
      xInt = 0d;
      xLayer =
          p
              * (Math.sqrt(Math.abs((pBot + b) / (pBot - b)))
                  - Math.sqrt(Math.abs((pTop + b) / (pTop - b))))
              / b;
    } else if (bSq >= p2) {
      xInt =
          Math.log(
              Math.max(
                  (pTop - b)
                      * (b2 * pBot2 + b * pBot - p2)
                      / ((pBot - b) * (b2 * pTop2 + b * pTop - p2)),
                  TauUtil.DMIN));
      if (TablesUtil.deBugLevel > 2)
        System.out.println(
            "bSq >= p2: "
                + (float) ((pTop - b) * (b2 * pBot2 + b * pBot - p2))
                + " "
                + (float) ((pBot - b) * (b2 * pTop2 + b * pTop - p2))
                + " "
                + (float)
                    Math.log(
                        (pTop - b)
                            * (b2 * pBot2 + b * pBot - p2)
                            / ((pBot - b) * (b2 * pTop2 + b * pTop - p2))));
      xLayer = p * xInt / b2;
    } else {
      xInt =
          Math.asin(Math.max(Math.min((b * pBot - p2) / (p * Math.abs(pBot - b)), 1d), -1d))
              - Math.asin(Math.max(Math.min((b * pTop - p2) / (p * Math.abs(pTop - b)), 1d), -1d));
      if (TablesUtil.deBugLevel > 2) {
        System.out.println(
            "Bot: "
                + (float) (b * pBot - p2)
                + " "
                + (float) (p * Math.abs(pBot - b))
                + " "
                + (float) Math.asin(b * pBot - p2) / (p * Math.abs(pBot - b)));
        System.out.println(
            "Top: "
                + (float) (b * pTop - p2)
                + " "
                + (float) (p * Math.abs(pTop - b))
                + " "
                + (float) Math.asin(b * pTop - p2) / (p * Math.abs(pTop - b)));
      }
      xLayer = -p * xInt / b2;
    }
    tau = -(pBot2 - pTop2 + b * Math.log((pBot + pBot2) / (pTop + pTop2)) - b2 * xInt);
    if (TablesUtil.deBugLevel > 2)
      System.out.println(
          "tau xInt xLayer = " + (float) tau + " " + (float) xInt + " " + (float) xLayer);
    tauTest(p, pTop, pBot, zTop, zBot, tau);
    return tau;
  }

  /**
   * Compute the derivative of ray travel distance with respect to ray parameter for one mode layer.
   *
   * @param p Normalized ray parameter
   * @param pTop Normalized slowness at the top of the layer
   * @param pBot Normalized slowness at the bottom of the layer
   * @param zTop Normalized depth at the top of the layer
   * @param zBot Normalized depth at the bottom of the layer
   * @return Normalized dX/dp
   */
  public double intDeriv(double p, double pTop, double pBot, double zTop, double zBot) {
    double p2, pTop2, pBot2;

    // Handle a zero thickness layer (discontinuity) or a constant
    // slowness layer.
    if (Math.abs(zTop - zBot) <= TauUtil.DTOL || Math.abs(pTop - pBot) <= TauUtil.DTOL) {
      return 0d;
    }

    p2 = Math.pow(p, 2d);
    pTop2 = Math.pow(pTop, 2d);
    // Handle a bottoming ray.
    if (Math.abs(p - pBot) <= TauUtil.DTOL) {
      return (zBot - zTop) * (1d / Math.sqrt(Math.abs(pTop2 - p2))) / Math.log(pTop / pBot);
    }
    // Do the general case.
    pBot2 = Math.pow(pBot, 2d);
    return (zBot - zTop)
        * (1d / Math.sqrt(Math.abs(pTop2 - p2)) - 1d / Math.sqrt(Math.abs(pBot2 - p2)))
        / Math.log(pTop / pBot);
  }

  /**
   * Get the integrated distance, X, for one layer.
   *
   * @return The normalized distance for one layer
   */
  public double getXLayer() {
    return xLayer;
  }

  /**
   * Get the integrated distance, X, for a range of layers.
   *
   * @return The normalized distance for a range of layers
   */
  public double getXSum() {
    return xSum;
  }

  /**
   * Get the bottoming radius, R, for a range of layers.
   *
   * @return The dimensional bottoming Earth radius in kilometers
   */
  public double getRbottom() {
    return rBottom;
  }

  /**
   * Get the index for the model sample above the bottoming radius.
   *
   * @return Bottoming index
   */
  public int getIbottom() {
    return iBottom;
  }

  /**
   * Validate the tau and distance layer integral.
   *
   * @param p Normalized ray parameter
   * @param pTop Normalized slowness at the top of the layer
   * @param pBot Normalized slowness at the bottom of the layer
   * @param zTop Normalized depth at the top of the layer
   * @param zBot Normalized depth at the bottom of the layer
   * @param tau Normalized tau
   * @throws TauIntegralException If tau or x is negative
   */
  private void tauTest(double p, double pTop, double pBot, double zTop, double zBot, double tau)
      throws TauIntegralException {
    if (tau < -TauUtil.DMIN) {
      System.out.format(
          "***** Bad tau: p = %8.6f, pTop = %8.6f, "
              + "pBot = %8.6f, zTop = %9.6f, zBot = %9.6f, tau = %11.4e, "
              + "x = %11.4e\n",
          p, pTop, pBot, zTop, zBot, tau, xLayer);
      if (p > pTop) System.out.println("***** p too big to penetrate to this layer!");
      throw new TauIntegralException("Partial integrals cannot be negative");
      /*	} else if(xLayer < -TauUtil.DMIN) {
      System.out.format("***** Bad x: p = %8.6f, pTop = %8.6f, "+
      		"pBot = %8.6f, zTop = %9.6f, zBot = %9.6f, tau = %11.4e, "+
      		"x = %11.4e\n", p, pTop, pBot, zTop, zBot, tau, xLayer);
      throw new Exception(); */
    }
  }
}
