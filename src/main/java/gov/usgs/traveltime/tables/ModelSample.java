package gov.usgs.traveltime.tables;

import gov.usgs.traveltime.ModConvert;

/**
 * ModelSample class contains all model parameters at one radius.
 *
 * @author Ray Buland
 */
public class ModelSample {
  /**
   * The "standard" Earth model representation that emerged after PREM (1981) and before AK135
   * (1997) includes density, anisotropy, and attenuation, none of which are needed for the tau-p
   * travel times. Just to be on the safe side, it makes sense to ensure that we make an isotropic
   * model out of a potentially anisotropic model. In this case, vertical polarization should be
   * understood as radial and horizontal as tangential in the idealized spherically symmetric Earth
   * model. The spherical approximation works for seismology because the oblateness is small enough
   * (~1/300) to be considered a perturbation.
   */

  /** A double value containing the radius in kilometers */
  private double radius;

  /** A double value containing the isotropic P velocity in kilometers/second */
  private double isotropicPVelocity;

  /** A double value containing the isotropic S velocity in kilometers/second */
  private double isotropicSVelocity;

  /** A double value containing the non-dimensional Earth flattened depth in kilometers */
  private double depth;

  /** A double value containing the non-dimensional compressional wave slowness */
  private double compressionalWaveSlowness;

  /** A double value containing the non-dimensional shear wave slowness */
  private double shearWaveSlowness;

  /**
   * Get the model sample radius in kilometers
   *
   * @return A double holding the model sample radius in kilometers
   */
  public double getRadius() {
    return radius;
  }

  /**
   * Get the model sample isotropic P velocity in kilometers/second
   *
   * @return A double holding the model isotropic P velocity in kilometers/second
   */
  public double getIsotropicPVelocity() {
    return isotropicPVelocity;
  }

  /**
   * Set the model sample isotropic P velocity in kilometers/second
   *
   * @param isotropicPVelocity A double holding the model isotropic P velocity in kilometers/second
   */
  public void setIsotropicPVelocity(double isotropicPVelocity) {
    this.isotropicPVelocity = isotropicPVelocity;
  }

  /**
   * Get the model sample isotropic S velocity in kilometers/second
   *
   * @return A double holding the model isotropic S velocity in kilometers/second
   */
  public double getIsotropicSVelocity() {
    return isotropicSVelocity;
  }

  /**
   * Set the model sample isotropic S velocity in kilometers/second
   *
   * @param isotropicSVelocity A double holding the model isotropic S velocity in kilometers/second
   */
  public void setIsotropicSVelocity(double isotropicSVelocity) {
    this.isotropicSVelocity = isotropicSVelocity;
  }

  /**
   * Get the non-dimensional Earth flattened depth
   *
   * @return A double holding the non-dimensional Earth flattened depth
   */
  public double getDepth() {
    return depth;
  }

  /**
   * Get the non-dimensional compressional wave slowness
   *
   * @return A double holding the non-dimensional compressional wave slowness
   */
  public double getCompressionalWaveSlowness() {
    return compressionalWaveSlowness;
  }

  /**
   * Get the non-dimensional shear wave slowness
   *
   * @return A double holding the non-dimensional shear wave slowness
   */
  public double getShearWaveSlowness() {
    return shearWaveSlowness;
  }

  /**
   * Function to create a (possibly) anisotropic model sample and it's isotropic equivalent.
   *
   * @param radius A double containing the radius in kilometers
   * @param vertPolarizedPVelocity A double containing the vertically polarized P velocity in
   *     kilometers/second
   * @param horizPolarizedPVelocity A double containng the horizontally polarized P velocity in
   *     kilometers/second
   * @param vertPolarizedSVelocity A double containing the vertically polarized S velocity in
   *     kilometers/second
   * @param horizPolarizedSVelocity A double containing the horizontally polarized S velocity in
   *     kilometers/second
   * @param eta A double containing the anisotropy parameter
   */
  public ModelSample(
      double radius,
      double vertPolarizedPVelocity,
      double horizPolarizedPVelocity,
      double vertPolarizedSVelocity,
      double horizPolarizedSVelocity,
      double anisotropyParam) {
    this.radius = radius;

    // Create the isotropic version.
    if ((anisotropyParam != 1d)
        || (vertPolarizedPVelocity != horizPolarizedPVelocity)
        || (vertPolarizedSVelocity != horizPolarizedSVelocity)) {
      isotropicSVelocity =
          Math.sqrt(
              0.0666666666666667d
                  * ((1d - 2d * anisotropyParam) * Math.pow(horizPolarizedPVelocity, 2d)
                      + Math.pow(vertPolarizedPVelocity, 2d)
                      + 5d * Math.pow(horizPolarizedSVelocity, 2d)
                      + (6d + 4d * anisotropyParam) * Math.pow(vertPolarizedSVelocity, 2d)));

      isotropicPVelocity =
          Math.sqrt(
              0.0666666666666667d
                  * ((8d + 4d * anisotropyParam) * Math.pow(horizPolarizedPVelocity, 2d)
                      + 3d * Math.pow(vertPolarizedPVelocity, 2d)
                      + (8d - 8d * anisotropyParam) * Math.pow(vertPolarizedSVelocity, 2d)));
    } else {
      isotropicPVelocity = vertPolarizedPVelocity;
      isotropicSVelocity = vertPolarizedSVelocity;
    }

    // Mask fluid areas.
    if (isotropicSVelocity == 0d) {
      isotropicSVelocity = isotropicPVelocity;
    }
  }

  /**
   * Function to create an isotropic model sample.
   *
   * @param radius A double containing the radius in kilometers
   * @param isotropicPVelocity A double containing the isotropic P velocity in kilometers/second
   * @param isotropicSVelocity A double containing the isotropic S velocity in kilometers/second
   */
  public ModelSample(double radius, double isotropicPVelocity, double isotropicSVelocity) {

    this.radius = radius;
    this.isotropicPVelocity = isotropicPVelocity;
    this.isotropicSVelocity = isotropicSVelocity;

    // Mask fluid areas.
    if (isotropicSVelocity == 0d) {
      isotropicSVelocity = isotropicPVelocity;
    }
  }

  /**
   * ModelSample copy constructor, creates a model sample by copying from another model sample.
   *
   * @param sample A ModelSample object containing the reference model sample to copy from
   */
  public ModelSample(ModelSample sample) {
    radius = sample.getRadius();
    isotropicPVelocity = sample.getIsotropicPVelocity();
    isotropicSVelocity = sample.getIsotropicSVelocity();
    depth = sample.getDepth();
    compressionalWaveSlowness = sample.getCompressionalWaveSlowness();
    shearWaveSlowness = sample.getShearWaveSlowness();
  }

  /**
   * Method to eliminate the poorly observed PKJKP phase by replacing the S velocity in the inner
   * core with the P velocity.
   */
  protected void eliminatePKJKP() {
    isotropicSVelocity = isotropicPVelocity;
  }

  /**
   * Function to non-dimensionalize the sample and apply the Earth flattening transformation.
   *
   * @param convert A ModConvert object containing the model sensitive conversion constants
   */
  public void flatten(ModConvert convert) {
    depth = convert.flatZ(radius);
    compressionalWaveSlowness = convert.flatP(isotropicPVelocity, radius);
    shearWaveSlowness = convert.flatP(isotropicSVelocity, radius);
  }

  /**
   * Getter for slowness.
   *
   * @param waveType A char containing the wave type ('P' = compressional, 'S' = shear)
   * @return Non-dimensional Earth flattened slowness
   */
  public double getSlowness(char waveType) {
    if (waveType == 'P') {
      return compressionalWaveSlowness;
    } else {
      return shearWaveSlowness;
    }
  }

  /**
   * Function to create a string representing the model sample.
   *
   * @param flat A boolean flag, if true print the Earth flattened parameters
   * @param convert A ModConvert object containing the model sensitive conversion constants, if not
   *     null, convert to dimensional depth
   * @return String describing this model sample
   */
  public String printSample(boolean flat, ModConvert convert) {

    if (flat) {
      if (convert == null) {
        return String.format(
            "%7.2f %9.4f %8.6f %8.6f", radius, depth, compressionalWaveSlowness, shearWaveSlowness);
      } else {
        return String.format(
            "%8.2f %7.2f %8.6f %8.6f",
            radius, convert.realZ(depth), compressionalWaveSlowness, shearWaveSlowness);
      }
    } else {
      return String.format("%9.2f %7.4f %7.4f", radius, isotropicPVelocity, isotropicSVelocity);
    }
  }
}
