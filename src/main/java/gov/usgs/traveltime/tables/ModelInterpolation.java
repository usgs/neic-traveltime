package gov.usgs.traveltime.tables;

import java.util.ArrayList;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.exception.OutOfRangeException;

/**
 * The ModelInterpolation class interpolates the earth model. This encapsulation is necessary
 * because the Apache Commons library routines don't handle the zero length intervals used in the
 * Earth models to flag discontinuities in velocity.
 *
 * @author Ray Buland
 */
public class ModelInterpolation {
  /** A boolean flag, if true use cubic spline interpolation, if false use linear interpolation */
  private boolean useCubicInterpolation;

  /** An integer containing the number of the last shell found */
  private int lastShellNumber = -1;

  /** A double containing the radius of the last shell found */
  private double lastRadius = Double.NaN;

  /** An ArrayList of ModelSample objects containing the earth model data */
  private ArrayList<ModelSample> earthModelData;

  /** An ArrayList of ModelShell objects containing the earth model shells */
  private ArrayList<ModelShell> earthModelShells;

  /**
   * An array of PolynomialSplineFunction objects containing the interpolated compressional
   * velocities
   */
  private PolynomialSplineFunction interpolatedCompVelocities[];

  /** An array of PolynomialSplineFunction objects containing the interpolated sheer velocities */
  private PolynomialSplineFunction interpolatedSheerVelocities[];

  /**
   * ModelInterpolation constructor, store the earth model data, etc.
   *
   * @param earthModelData An ArrayList of ModelSample objects containing the earth model data
   * @param earthModelShells An ArrayList of ModelShell objects containing the earth model shells
   * @param useCubicInterpolation A boolean flag, if true use cubic spline interpolation, if false
   *     use linear interpolation
   */
  public ModelInterpolation(
      ArrayList<ModelSample> earthModelData,
      ArrayList<ModelShell> earthModelShells,
      boolean useCubicInterpolation) {
    this.earthModelData = earthModelData;
    this.earthModelShells = earthModelShells;
    this.useCubicInterpolation = useCubicInterpolation;
  }

  /** Function to interpolate the velocity model values. */
  public void interpVel() {
    LinearInterpolator linear = null;
    SplineInterpolator cubic = null;

    // Initialize the arrays of splines.
    interpolatedCompVelocities = new PolynomialSplineFunction[earthModelShells.size()];
    interpolatedSheerVelocities = new PolynomialSplineFunction[earthModelShells.size()];

    // Loop over shells doing the interpolation.
    for (int i = 0; i < earthModelShells.size(); i++) {
      if (!earthModelShells.get(i).isDisc) {
        // Allocate some temporary storage.
        int iBot = earthModelShells.get(i).iBot;
        int iTop = earthModelShells.get(i).iTop;
        int n = iTop - iBot + 1;
        double[] r = new double[n];
        double[] v = new double[n];

        // The cubic interpolation only works if there are more than 2 points.
        if (useCubicInterpolation && n > 2) {
          // Use cubic splines.
          if (cubic == null) {
            cubic = new SplineInterpolator();
          }

          // Interpolate Vp.
          for (int j = iBot; j <= iTop; j++) {
            r[j - iBot] = earthModelData.get(j).getRadius();
            v[j - iBot] = earthModelData.get(j).getIsotropicPVelocity();
          }
          interpolatedCompVelocities[i] = cubic.interpolate(r, v);

          // Interpolate Vs.
          for (int j = iBot; j <= iTop; j++) {
            v[j - iBot] = earthModelData.get(j).getIsotropicSVelocity();
          }
          interpolatedSheerVelocities[i] = cubic.interpolate(r, v);
        } else {
          // The alternative is linear interpolation.
          if (linear == null) {
            linear = new LinearInterpolator();
          }

          // Interpolate Vp.
          for (int j = iBot; j <= iTop; j++) {
            r[j - iBot] = earthModelData.get(j).getRadius();
            v[j - iBot] = earthModelData.get(j).getIsotropicPVelocity();
          }
          interpolatedCompVelocities[i] = linear.interpolate(r, v);

          // Interpolate Vs.
          for (int j = iBot; j <= iTop; j++) {
            v[j - iBot] = earthModelData.get(j).getIsotropicSVelocity();
          }
          interpolatedSheerVelocities[i] = linear.interpolate(r, v);
        }
      }
    }
  }

  /**
   * Function to interpolate to find the compressional velocity Vp.
   *
   * @param radius A double containing the radius in kilometers
   * @return A double containing the compressional velocity in kilometers/second at given radius
   */
  public double getCompressionalVelocity(double radius) {
    int shell = getShell(radius);

    if (shell >= 0) {
      try {
        return interpolatedCompVelocities[shell].value(radius);
      } catch (OutOfRangeException e) {
        return Double.NaN;
      }
    } else {
      return Double.NaN;
    }
  }

  /**
   * Function to interpolate to find Compressional velocity Vp in a particular shell.
   *
   * @param shell An integer containing the shell number
   * @param radius A double containing the radius in kilometers
   * @return A double containing the compressional velocity in kilometers/second at given radius
   */
  public double getCompressionalVelocity(int shell, double radius) {
    if (shell >= 0 && shell < earthModelShells.size()) {
      try {
        return interpolatedCompVelocities[shell].value(radius);
      } catch (OutOfRangeException e) {
        return Double.NaN;
      }
    } else {
      return Double.NaN;
    }
  }

  /**
   * Function to interpolate to find the shear velocity Vs.
   *
   * @param radius A double containing the radius in kilometers
   * @return A double containing the shear velocity in kilometers/second at the given radius
   */
  public double getSheerVelocity(double radius) {
    int shell = getShell(radius);

    if (shell >= 0) {
      try {
        return interpolatedSheerVelocities[shell].value(radius);
      } catch (OutOfRangeException e) {
        return Double.NaN;
      }
    } else {
      return Double.NaN;
    }
  }

  /**
   * Function to interpolate to find the shear velocity Vs in a particular shell.
   *
   * @param shell An integer containing the shell number
   * @param radius A double containing the radius in kilometers
   * @return A double containing the shear velocity in kilometers/second at given radius
   */
  public double getSheerVelocity(int shell, double radius) {
    if (shell >= 0 && shell < earthModelShells.size()) {
      try {
        return interpolatedSheerVelocities[shell].value(radius);
      } catch (OutOfRangeException e) {
        return Double.NaN;
      }
    } else {
      return Double.NaN;
    }
  }

  /**
   * Function to find the shell containing the given radius.
   *
   * @param radius A double value containing the radius in kilometers
   * @return An integer containing the shell number
   */
  private int getShell(double radius) {
    if (radius == lastRadius) {
      return lastShellNumber;
    } else {
      for (int j = 0; j < earthModelShells.size(); j++) {
        if (earthModelShells.get(j).isInShell(radius)) {
          lastRadius = radius;
          lastShellNumber = j;

          return j;
        }
      }
    }

    return -1;
  }
}
