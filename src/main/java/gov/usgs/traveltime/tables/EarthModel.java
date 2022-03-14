package gov.usgs.traveltime.tables;

import gov.usgs.traveltime.ModConvert;
import gov.usgs.traveltime.TauUtil;
import gov.usgs.traveltime.TtStatus;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * The EarthModel class takes care of everything related to one Earth model.
 *
 * @author Ray Buland
 */
public class EarthModel {
  /** A String holding the name of this earth model */
  private String earthModelName;

  /** A double containing the inner core boundary radius */
  private double innerCoreRadius = ShellName.INNER_CORE.getDefaultRadius();

  /** A double containing the outer core boundary radius */
  private double outerCoreRadius = ShellName.OUTER_CORE.getDefaultRadius();

  /** A double containing the upper mantle discontinuity radius */
  private double upperMantleRadius = ShellName.LOWER_MANTLE.getDefaultRadius();

  /** A double containing the radius of the Moho discontinuity */
  private double mohoRadius = ShellName.UPPER_MANTLE.getDefaultRadius();

  /** A double containing the radius of the Conrad discontinuity */
  private double conradRadius = ShellName.LOWER_CRUST.getDefaultRadius();

  /** A double containg the free surface radius */
  private double surfaceRadius = ShellName.UPPER_CRUST.getDefaultRadius();

  /** A ModelSample object containing the model at the inner core boundary */
  private ModelSample innerCoreModel;

  /** A ModelSample object containing the model at the outer core boundary */
  private ModelSample outerCoreModel;

  /** A ModelSample object containing the model at the upper mantle discontinuity */
  private ModelSample upperMantleModel;

  /** A ModelSample object containing the model at the Moho discontinuity */
  private ModelSample mohoModel;

  /** A ModelSample object containing the model at the Conrad discontinuity */
  private ModelSample conradModel;

  /** A ModelSample object containing the model at the free surfacety */
  private ModelSample surfaceModel;

  /** An array list of ModelSample objects containing the model */
  private ArrayList<ModelSample> model;

  /** An array list of ModelShell objects containing the model shell parameters */
  private ArrayList<ModelShell> shells;

  /** An array list of CriticalSlowness objects containing the model critical slownesses */
  private ArrayList<CriticalSlowness> criticalSlownesses;

  /** An EarthModel object containing the reference earth model information */
  private EarthModel referenceModel;

  /** A ModelInterpolation object used for interpolations */
  private ModelInterpolation ModelInterpolationolations;

  /** A ModConvert object containing model dependent constants and conversions */
  ModConvert modelConversions;

  /**
   * Function to return the name of this earth model.
   *
   * @return A String containing the name of this earth model.
   */
  public String getEarthModelName() {
    return earthModelName;
  }

  /**
   * Function to return the inner core earth model.
   *
   * @return A ModelSample containing the model at the inner core boundary.
   */
  public ModelSample getInnerCoreModel() {
    return innerCoreModel;
  }

  /**
   * Function to return the outer core earth model.
   *
   * @return A ModelSample containing the model at the outer core boundary.
   */
  public ModelSample getOuterCoreModel() {
    return outerCoreModel;
  }

  /**
   * Function to return the model
   *
   * @return An array list of ModelSample objects containing the model.
   */
  public ArrayList<ModelSample> getModel() {
    return model;
  }

  /**
   * Function to return the model shells compiled by the reference Earth model processing.
   *
   * @return An array list of ModelShell objects containing the model.
   */
  public ArrayList<ModelShell> getShells() {
    return shells;
  }

  /**
   * Getter for the list of critical slownesses compiled by the reference Earth model processing.
   *
   * @return An array list of CriticalSlowness objects containing the critical slownesses
   */
  public ArrayList<CriticalSlowness> getCriticalSlownesses() {
    return criticalSlownesses;
  }

  /**
   * Gett the model dependent constants and conversions
   *
   * @return A ModConvert object containing model dependent constants and conversions
   */
  public ModConvert getModelConversions() {
    return modelConversions;
  }

  /**
   * Function to return the reference earth model
   *
   * @return An EarthModel object containing the reference earth model.
   */
  public EarthModel getReferenceModel() {
    return referenceModel;
  }

  /**
   * EarthModel constructor, in this version, we're going to read in a reference Earth model from a
   * file.
   *
   * @param earthModelName A string containing the name of the Earth model
   * @param isCubic A boolean flag, True if cubic spline interpolation is to be used
   */
  public EarthModel(String earthModelName, boolean isCubic) {
    this.earthModelName = earthModelName;

    model = new ArrayList<ModelSample>();
    shells = new ArrayList<ModelShell>();
    ModelInterpolationolations = new ModelInterpolation(model, shells, isCubic);
  }

  /**
   * EarthModel constructor, in this version, we already have a reference Earth model and we're
   * going to re-interpolate it.
   *
   * @param referenceModel An EarthModel file containing the reference Earth model information
   * @param modelConversions A ModConvert object containing the model dependent constants
   */
  public EarthModel(EarthModel referenceModel, ModConvert modelConversions) {
    this.referenceModel = referenceModel;
    this.modelConversions = modelConversions;
    this.earthModelName = referenceModel.getEarthModelName();

    model = new ArrayList<ModelSample>();
    shells = new ArrayList<ModelShell>();
    criticalSlownesses = new ArrayList<CriticalSlowness>();
  }

  /**
   * Function to read the Earth model file ffrom disk, set up shells, refine internal boundaries,
   * and initialize critical points.
   *
   * @param modelFile A String containing the path to the Earth model file
   * @return Return a TtStatus object containing the travel-time status
   */
  public TtStatus readModelFile(String modelFile) {
    /*
     * We have to read everything in, but we don't need density,
     * anisotropy, or attenuation for the travel-times.
     */
    @SuppressWarnings("unused")
    double r, rho, vpv, vph, vsv, vsh, eta, qMu, qKappa, rLast = 0d;

    // Open and read the phase groups file.
    BufferedInputStream inModel;
    try {
      inModel = new BufferedInputStream(new FileInputStream(modelFile));
    } catch (FileNotFoundException e) {
      return TtStatus.BAD_MODEL_READ;
    }

    Scanner scan = new Scanner(inModel);

    // Read the header.
    String modelCheck = scan.next();
    if (!modelCheck.equals(earthModelName)) {
      System.out.println(
          "\n***** Error: model name mismatch (earthModelName:"
              + earthModelName
              + " != modelHeader:"
              + modelCheck
              + ") *****\n");
      scan.close();
      return TtStatus.BAD_MODEL_FILE;
    }

    int n = scan.nextInt();
    if (!scan.hasNextInt()) {
      surfaceRadius = scan.nextDouble();
      upperMantleRadius = scan.nextDouble();
      mohoRadius = scan.nextDouble();
      conradRadius = scan.nextDouble();
    }

    // Read the model points.
    int last = 0;
    int i = 0;
    while (scan.hasNextInt()) {
      i = scan.nextInt();
      if (i != ++last) {
        System.out.format("\n***** Warning: sample %d found, %d " + "expected *****\n\n", last, i);
        last = i;
      }

      r = scan.nextDouble();
      if (r < rLast) {
        System.out.format("\n***** Error: radius %7.2f out of order " + "*****\n\n", r);
        scan.close();
        return TtStatus.BAD_MODEL_FILE;
      }

      rho = scan.nextDouble();
      vpv = scan.nextDouble();
      vph = scan.nextDouble();
      vsv = scan.nextDouble();
      vsh = scan.nextDouble();
      eta = scan.nextDouble();
      qMu = scan.nextDouble();
      qKappa = scan.nextDouble();
      model.add(new ModelSample(r, vpv, vph, vsv, vsh, eta));

      // Trap discontinuities.
      if (r == rLast) {
        if (model.size() > 1) {
          shells.get(shells.size() - 1).addTop(model.size() - 2, r);
          shells.add(new ModelShell(model.size() - 2, model.size() - 1, r));
          bridgeVelocity(model.size() - 1);
        }

        shells.add(new ModelShell(model.size() - 1, r));
      }

      rLast = r;
    }

    // Done, finalize the outermost shell.
    shells.get(shells.size() - 1).addTop(model.size() - 1, rLast);

    // Do some crude checks.
    if (i != n) {
      System.out.format("\n***** Warning: %d points found, %d " + "expected *****\n\n", n, i);
    }

    if (rLast != surfaceRadius) {
      System.out.format(
          "\n***** Warning: radius of the model is not the "
              + "same as the radius of the Earth (%7.2f != %7.2f)\n",
          rLast, surfaceRadius);
    }
    // OK. We're good (probably).
    scan.close();

    // Set the S velocity to the P velocity in the inner core.
    eliminatePKJKP();

    // Interpolate velocity.
    ModelInterpolationolations.interpVel();

    // Find important internal boundaries.
    refineBoundaries();

    // Initialize the model specific conversion constants.
    modelConversions =
        new ModConvert(
            upperMantleModel.getRadius(),
            mohoModel.getRadius(),
            conradModel.getRadius(),
            surfaceModel.getRadius(),
            surfaceModel.getIsotropicSVelocity());

    // Do the Earth flattening transformation.
    flattenModel();
    referenceModel = this;
    return TtStatus.SUCCESS;
  }

  /** Function to interpolate the reference Earth model at a standard sampling. */
  public void interpolate() {
    // Loop over the reference Earth model shells.
    double r1 = 0d;
    for (int i = 0; i < referenceModel.shells.size(); i++) {
      ModelShell referenceShell = referenceModel.shells.get(i);

      // Initialize the interpolated model shell.
      ModelShell newShell;
      if (!referenceShell.getIsDiscontinuity()) {
        newShell = new ModelShell(referenceShell, model.size());
        model.add(new ModelSample(referenceModel.model.get(referenceShell.getBottomSampleIndex())));
        bridgeVelocity(model.size() - 1);

        // Figure how many samples we'll need.
        double r0 = r1;
        r1 = referenceShell.getTopSampleRadius();
        int numSamples = (int) ((r1 - r0) / TablesUtil.RESAMPLE - 0.5d);
        double dr = (r1 - r0) / (numSamples + 1);

        // Fill in the interpolated model.
        for (int j = 1; j <= numSamples; j++) {
          double r = r0 + j * dr;
          model.add(
              new ModelSample(
                  r, referenceModel.getVelocity('P', i, r), referenceModel.getVelocity('S', i, r)));
        }

        // Add the top of the shell.
        newShell.addTop(model.size(), r1);
        model.add(new ModelSample(referenceModel.model.get(referenceShell.getTopSampleIndex())));
      } else {
        newShell = new ModelShell(referenceShell, model.size() - 1);
        newShell.addTop(model.size(), r1);
      }

      shells.add(newShell);
    }

    // Apply the Earth flattening transformation.
    flattenModel();

    // Find critical slownesses (ends of travel-time branches).
    findCriticalPoints();
  }

  /**
   * Function to interpolate the model to find velocity (V(r)).
   *
   * @param waveType A char containing the wave type ('P' = compressional, 'S' = shear)
   * @param radius A double containing the radius in kilometers
   * @return A double containing the velocity in kilometers/second at the given radius
   */
  public double getVelocity(char waveType, double radius) {
    if (waveType == 'P') {
      return ModelInterpolationolations.getCompressionalVelocity(radius);
    } else {
      return ModelInterpolationolations.getSheerVelocity(radius);
    }
  }

  /**
   * Function to interpolate the model to find velocity (V(r)) in a particular shell.
   *
   * @param waveType A char containing the wave type ('P' = compressional, 'S' = shear)
   * @param shellIndex An integer containing the shell index number
   * @param radius A double containing the radius in kilometers
   * @return A double containing the compressional velocity in kilometers/second at the given radius
   */
  public double getVelocity(char waveType, int shellIndex, double radius) {
    if (waveType == 'P') {
      return ModelInterpolationolations.getCompressionalVelocity(shellIndex, radius);
    } else {
      return ModelInterpolationolations.getSheerVelocity(shellIndex, radius);
    }
  }

  /**
   * Function to get the model depth at a given index.
   *
   * @param index An integer containing the sample index to use
   * @return A double containing the non-dimensional Earth flattened depth
   */
  public double getDepth(int index) {
    return model.get(index).getDepth();
  }

  /**
   * Function to get the model radius at a given index.
   *
   * @param index An integer containing the sample index to use
   * @return A double containing the dimensional Earth radius in kilometers
   */
  public double getRadius(int index) {
    return model.get(index).getRadius();
  }

  /**
   * Function to get the model slowness at a given index.
   *
   * @param waveType A char containing the wave type ('P' = compressional, 'S' = shear)
   * @param index An integer containing the sample index to use
   * @return A double containing the non-dimensional P-wave slowness
   */
  public double getSlowness(char waveType, int index) {
    if (waveType == 'P') {
      return model.get(index).getCompressionalWaveSlowness();
    } else {
      return model.get(index).getShearWaveSlowness();
    }
  }

  /**
   * Function to get the size of the model.
   *
   * @return An integer containing the the number of samples in this Earth model.
   */
  public int size() {
    return model.size();
  }

  /**
   * Function to apply the Earth flattening transformation to the model and make all flattened
   * parameters non-dimensional at the same time.
   */
  private void flattenModel() {
    for (int j = 0; j < model.size(); j++) {
      model.get(j).flatten(modelConversions);
    }
  }

  /**
   * Function to eliminate the poorly observed PKJKP phase in the inner core by replacing the S
   * velocity with the P velocity.
   */
  private void eliminatePKJKP() {
    ModelShell shell = shells.get(0);
    for (int j = shell.getBottomSampleIndex(); j <= shell.getTopSampleIndex(); j++) {
      model.get(j).eliminatePKJKP();
    }
  }

  /**
   * Function match the key radii with model discontinuities. This is necessary to figure out the
   * phase codes.
   */
  private void refineBoundaries() {
    double tempIC = TauUtil.DMAX,
        tempOC = TauUtil.DMAX,
        tempUM = TauUtil.DMAX,
        tempM = TauUtil.DMAX,
        tempC = TauUtil.DMAX,
        tempFS = TauUtil.DMAX;

    // Find the closest boundary to target boundaries.
    for (int j = 0; j < shells.size(); j++) {
      double rDisc = shells.get(j).getTopSampleRadius();
      int iTop = shells.get(j).getTopSampleIndex();

      if (Math.abs(rDisc - innerCoreRadius) < Math.abs(tempIC - innerCoreRadius)) {
        tempIC = rDisc;
        innerCoreModel = model.get(iTop);
      }

      if (Math.abs(rDisc - outerCoreRadius) < Math.abs(tempOC - outerCoreRadius)) {
        tempOC = rDisc;
        outerCoreModel = model.get(iTop);
      }

      if (Math.abs(rDisc - upperMantleRadius) < Math.abs(tempUM - upperMantleRadius)) {
        tempUM = rDisc;
        upperMantleModel = model.get(iTop);
      }

      if (Math.abs(rDisc - mohoRadius) < Math.abs(tempM - mohoRadius)) {
        tempM = rDisc;
        mohoModel = model.get(iTop);
      }

      if (Math.abs(rDisc - conradRadius) < Math.abs(tempC - conradRadius)) {
        tempC = rDisc;
        conradModel = model.get(iTop);
      }

      if (Math.abs(rDisc - surfaceRadius) < Math.abs(tempFS - surfaceRadius)) {
        tempFS = rDisc;
        surfaceModel = model.get(iTop);
      }
    }

    // Set the radii.
    innerCoreRadius = innerCoreModel.getRadius();
    outerCoreRadius = outerCoreModel.getRadius();
    upperMantleRadius = upperMantleModel.getRadius();
    mohoRadius = mohoModel.getRadius();
    conradRadius = conradModel.getRadius();
    surfaceRadius = surfaceModel.getRadius();

    // Go around again setting up the shells.
    for (int j = 0; j < shells.size(); j++) {
      ModelShell shell = shells.get(j);
      if (!shell.getIsDiscontinuity()) {
        double rDisc = shell.getTopSampleRadius();

        if (rDisc <= innerCoreRadius) {
          shell.addName(ShellName.INNER_CORE, Double.NaN, TablesUtil.DELX[0]);
        } else if (rDisc <= outerCoreRadius) {
          shell.addName(ShellName.OUTER_CORE, Double.NaN, TablesUtil.DELX[1]);
        } else if (rDisc <= upperMantleRadius) {
          shell.addName(ShellName.LOWER_MANTLE, Double.NaN, TablesUtil.DELX[2]);
        } else if (rDisc <= mohoRadius) {
          shell.addName(ShellName.UPPER_MANTLE, Double.NaN, TablesUtil.DELX[3]);
        } else if (rDisc <= conradRadius) {
          shell.addName(ShellName.LOWER_CRUST, Double.NaN, TablesUtil.DELX[4]);
        } else {
          shell.addName(ShellName.UPPER_CRUST, Double.NaN, TablesUtil.DELX[5]);
        }
      }
    }

    // Go around yet again setting up the discontinuities.
    for (int j = 0; j < shells.size(); j++) {
      ModelShell shell = shells.get(j);
      if (shell.getIsDiscontinuity()) {
        double rDisc = shell.getTopSampleRadius();

        if (rDisc == innerCoreRadius) {
          shell.addName(ShellName.INNER_CORE_BOUNDARY, Double.NaN, TablesUtil.DELX[1]);
        } else if (rDisc == outerCoreRadius) {
          shell.addName(ShellName.CORE_MANTLE_BOUNDARY, Double.NaN, TablesUtil.DELX[2]);
        } else if (rDisc == mohoRadius) {
          shell.addName(ShellName.MOHO_DISCONTINUITY, Double.NaN, TablesUtil.DELX[4]);
        } else if (rDisc == conradRadius) {
          shell.addName(ShellName.CONRAD_DISCONTINUITY, surfaceRadius - rDisc, TablesUtil.DELX[5]);
        } else {
          if (rDisc < upperMantleRadius) {
            shell.addName(null, surfaceRadius - rDisc, TablesUtil.DELX[2]);
          } else if (rDisc < mohoRadius) {
            shell.addName(null, surfaceRadius - rDisc, TablesUtil.DELX[3]);
          } else if (rDisc < conradRadius) {
            shell.addName(null, surfaceRadius - rDisc, TablesUtil.DELX[4]);
          } else {
            shell.addName(null, surfaceRadius - rDisc, TablesUtil.DELX[5]);
          }
        }
      }
    }
  }

  /**
   * Function to bridge shell velocities. If velocity is nearly continuous across shell boundaries,
   * assume that it should be exactly continuous and make it so.
   *
   * @param index An integer containing the index of the model sample at the bottom of the new shell
   */
  private void bridgeVelocity(int index) {
    if (index > 0) {
      if (Math.abs(
              model.get(index).getIsotropicPVelocity()
                  - model.get(index - 1).getIsotropicPVelocity())
          <= TablesUtil.VELOCITYTOL * model.get(index).getIsotropicPVelocity()) {
        model
            .get(index)
            .setIsotropicPVelocity(
                0.5d
                    * (model.get(index).getIsotropicPVelocity()
                        + model.get(index - 1).getIsotropicPVelocity()));
        model.get(index - 1).setIsotropicPVelocity(model.get(index).getIsotropicPVelocity());
      }

      if (Math.abs(
              model.get(index).getIsotropicSVelocity()
                  - model.get(index - 1).getIsotropicSVelocity())
          <= TablesUtil.VELOCITYTOL * model.get(index).getIsotropicSVelocity()) {
        model
            .get(index)
            .setIsotropicSVelocity(
                0.5d
                    * (model.get(index).getIsotropicSVelocity()
                        + model.get(index - 1).getIsotropicSVelocity()));
        model.get(index - 1).setIsotropicSVelocity(model.get(index).getIsotropicSVelocity());
      }
    }
  }

  /**
   * Funtion to collect the critical points. A critical point is a slowness that must be sampled
   * exactly because it will be the end of a branch for a surface focus event.
   */
  private void findCriticalPoints() {
    // The slownesses above and below each discontinuity in the model
    // will be a branch end point.
    ModelShell shell = null;
    for (int j = 0; j < shells.size(); j++) {
      shell = shells.get(j);
      criticalSlownesses.add(
          new CriticalSlowness(
              'P',
              j,
              (shell.getIsDiscontinuity()
                  ? ShellSlownessLocation.BOUNDARY
                  : ShellSlownessLocation.SHELL),
              model.get(shell.getBottomSampleIndex()).getCompressionalWaveSlowness()));

      if (model.get(shell.getBottomSampleIndex()).getCompressionalWaveSlowness()
          != model.get(shell.getBottomSampleIndex()).getShearWaveSlowness()) {
        criticalSlownesses.add(
            new CriticalSlowness(
                'S',
                j,
                (shell.getIsDiscontinuity()
                    ? ShellSlownessLocation.BOUNDARY
                    : ShellSlownessLocation.SHELL),
                model.get(shell.getBottomSampleIndex()).getShearWaveSlowness()));
      }
    }

    criticalSlownesses.add(
        new CriticalSlowness(
            'P',
            shells.size() - 1,
            ShellSlownessLocation.SHELL,
            model.get(shell.getTopSampleIndex()).getCompressionalWaveSlowness()));

    criticalSlownesses.add(
        new CriticalSlowness(
            'S',
            shells.size() - 1,
            ShellSlownessLocation.SHELL,
            model.get(shell.getTopSampleIndex()).getShearWaveSlowness()));

    /*
     * Now look for high slowness zones.  Note that this is not quite the
     * same as low velocity zones because of the definition of slowness in
     * the spherical earth.  First do the P-wave slowness.
     */
    boolean inLVZ = false;
    for (int i = 0; i < shells.size(); i++) {
      shell = shells.get(i);
      ModelSample sample = model.get(shell.getBottomSampleIndex());

      for (int j = shell.getBottomSampleIndex() + 1; j <= shell.getTopSampleIndex(); j++) {
        ModelSample lastSample = sample;
        sample = model.get(j);

        if (!inLVZ) {
          if (sample.getCompressionalWaveSlowness() <= lastSample.getCompressionalWaveSlowness()) {
            inLVZ = true;
            criticalSlownesses.add(
                new CriticalSlowness(
                    'P',
                    i,
                    ShellSlownessLocation.SHELL,
                    lastSample.getCompressionalWaveSlowness()));
            shell.setHasLowVelocityZone(true);
          }
        } else {
          if (sample.getCompressionalWaveSlowness() >= lastSample.getCompressionalWaveSlowness()) {
            inLVZ = false;
            criticalSlownesses.add(
                new CriticalSlowness(
                    'P',
                    i,
                    ShellSlownessLocation.SHELL,
                    lastSample.getCompressionalWaveSlowness()));
          }
        }
      }
    }

    // Now do the S-wave slowness.
    inLVZ = false;

    for (int i = 0; i < shells.size(); i++) {
      shell = shells.get(i);
      ModelSample sample = model.get(shell.getBottomSampleIndex());

      for (int j = shell.getBottomSampleIndex() + 1; j <= shell.getTopSampleIndex(); j++) {
        ModelSample lastSample = sample;
        sample = model.get(j);

        if (!inLVZ) {
          if (sample.getShearWaveSlowness() <= lastSample.getShearWaveSlowness()) {
            inLVZ = true;
            criticalSlownesses.add(
                new CriticalSlowness(
                    'S', i, ShellSlownessLocation.SHELL, lastSample.getShearWaveSlowness()));
            shell.setHasLowVelocityZone(true);
          }
        } else {
          if (sample.getShearWaveSlowness() >= lastSample.getShearWaveSlowness()) {
            inLVZ = false;
            criticalSlownesses.add(
                new CriticalSlowness(
                    'S', i, ShellSlownessLocation.SHELL, lastSample.getShearWaveSlowness()));
          }
        }
      }
    }

    // Add the missing shells.
    fixShells();

    // Sort the critical slownesses into order.
    criticalSlownesses.sort(null);

    // And remove duplicates.
    for (int j = 1; j < criticalSlownesses.size(); j++) {
      if (criticalSlownesses.get(j).isSame(criticalSlownesses.get(j - 1))) {
        criticalSlownesses.remove(j);
        j--;
      }
    }
  }

  /**
   * Function to add the shell indices for the model velocities for which the critical slownesses
   * aren't actually critical. For example, if a critical slowness corresponds to the end of a
   * P-wave branch, we will need to add the S-wave shell that the slowness falls into. At the same
   * time fix any out of order critical points (typically due to low velocity zones).
   */
  private void fixShells() {
    for (int i = 0; i < criticalSlownesses.size(); i++) {
      CriticalSlowness crit = criticalSlownesses.get(i);

      // Add or check/fix P slowness shells.
      for (int j = shells.size() - 1; j >= 0; j--) {
        ModelShell shell = shells.get(j);

        if (crit.getSlowness()
            >= model.get(shell.getBottomSampleIndex()).getCompressionalWaveSlowness()) {
          crit.setShellIndex('P', j);
          break;
        }
      }
      // Add or check/fix S slowness shells.
      for (int j = shells.size() - 1; j >= 0; j--) {
        ModelShell shell = shells.get(j);

        if (crit.getSlowness() >= model.get(shell.getBottomSampleIndex()).getShearWaveSlowness()) {
          crit.setShellIndex('S', j);
          break;
        }
      }
    }
  }

  /** Print the model. */
  public void printModel() {
    System.out.format(
        "\n%s %d %7.2f %7.2f %7.2f %7.2f %7.2f %7.2f\n",
        earthModelName,
        model.size(),
        innerCoreModel.getRadius(),
        outerCoreModel.getRadius(),
        upperMantleModel.getRadius(),
        mohoModel.getRadius(),
        conradModel.getRadius(),
        surfaceModel.getRadius());

    for (int j = 0; j < model.size(); j++) {
      System.out.format("\t%3d: %s\n", j, model.get(j).printSample(false, null));
    }
  }

  /**
   * Function to print the model to the screen.
   *
   * @param flat If true print the Earth flattened parameters
   * @param nice If true print dimensional depths
   */
  public void printModel(boolean flat, boolean nice) {

    if (flat) {
      if (nice) {
        System.out.format(
            "\n%s %d %7.2f %7.2f %7.2f %7.2f %7.2f %7.2f\n",
            referenceModel.getEarthModelName(),
            model.size(),
            modelConversions.realZ(referenceModel.surfaceModel.getDepth()),
            modelConversions.realZ(referenceModel.conradModel.getDepth()),
            modelConversions.realZ(referenceModel.mohoModel.getDepth()),
            modelConversions.realZ(referenceModel.upperMantleModel.getDepth()),
            modelConversions.realZ(referenceModel.outerCoreModel.getDepth()),
            modelConversions.realZ(referenceModel.innerCoreModel.getDepth()));
        System.out.println("\t        R         Z    slowP    slowS");
      } else {
        System.out.format(
            "\n%s %d %7.4f %7.4f %7.4f %7.4f %7.4f %7.4f\n",
            referenceModel.getEarthModelName(),
            model.size(),
            referenceModel.surfaceModel.getDepth(),
            referenceModel.conradModel.getDepth(),
            referenceModel.mohoModel.getDepth(),
            referenceModel.upperMantleModel.getDepth(),
            referenceModel.outerCoreModel.getDepth(),
            referenceModel.innerCoreModel.getDepth());
        System.out.println("\t        R         Z    slowP    slowS");
      }

      int n = model.size() - 1;
      for (int j = n; j >= 0; j--) {
        if (nice) {
          System.out.format("\t%3d: %s\n", n - j, model.get(j).printSample(true, modelConversions));
        } else {
          System.out.format("\t%3d: %s\n", n - j, model.get(j).printSample(true, null));
        }
      }
    } else {
      System.out.format(
          "\n%s %d %7.2f %7.2f %7.2f %7.2f %7.2f %7.2f\n",
          referenceModel.getEarthModelName(),
          model.size(),
          referenceModel.innerCoreModel.getRadius(),
          referenceModel.outerCoreModel.getRadius(),
          referenceModel.upperMantleModel.getRadius(),
          referenceModel.mohoModel.getRadius(),
          referenceModel.conradModel.getRadius(),
          referenceModel.surfaceModel.getRadius());
      System.out.println("                   R     Vp      Vs");

      for (int j = 0; j < model.size(); j++) {
        System.out.format("\t%3d: %s\n", j, model.get(j).printSample(false, null));
      }
    }
  }

  /** Function to print the shell limits to the screen. */
  public void printShells() {
    System.out.println("\n\t\tShells:");

    for (int j = 0; j < shells.size(); j++) {
      System.out.format("%3d   %s\n", j, shells.get(j).toString());
    }
  }

  /** Function to print the (potentially) critical points. */
  public void printCriticalPoints() {
    System.out.println("\n\t\t  Critical points:");
    int n = criticalSlownesses.size() - 1;

    for (int j = n; j >= 0; j--) {
      System.out.format("\t  %3d %s\n", n - j, criticalSlownesses.get(j));
    }
  }
}
