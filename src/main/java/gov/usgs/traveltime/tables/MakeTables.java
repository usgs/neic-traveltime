package gov.usgs.traveltime.tables;

import gov.usgs.traveltime.AllBrnRef;
import gov.usgs.traveltime.AuxTtRef;
import gov.usgs.traveltime.ModConvert;
import gov.usgs.traveltime.TtStatus;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Travel-time table generation driver.
 *
 * @author Ray Buland
 */
public class MakeTables {
  EarthModel refModel;
  TauModel finModel;
  ArrayList<BranchData> brnData;

  /**
   * Initialize the reference Earth model. This needs to happen in the constructor to ensure that
   * the travel-time properties file has been read.
   *
   * @param earthModel Name of the Earth model
   */
  public MakeTables(String earthModel) {
    refModel = new EarthModel(earthModel, true);
  }

  /**
   * Create the travel-time tables out of whole cloth.
   *
   * @param modelFile Name of the Earth model file
   * @param phaseFile Name of the file of desired phases
   * @return Model read status
   * @throws Exception If an integration interval is illegal
   */
  public TtStatus buildModel(String modelFile, String phaseFile) throws Exception {
    EarthModel locModel;
    ModConvert convert;
    TauModel depModel;
    SampleSlowness sample;
    Integrate integrate;
    DecimateTTBranch decimate;
    MakeBranches layout;
    TtStatus status;

    // Read the model.
    status = refModel.readModelFile(modelFile);

    // If it read OK, process it.
    if (status == TtStatus.SUCCESS) {
      if (TablesUtil.deBugLevel > 2) {
        // Print the shell summaries.
        refModel.printShells();
        // Print out the radial version.
        refModel.printModel();
      }
      // Interpolate the model.
      convert = refModel.getModelConvesions();
      locModel = new EarthModel(refModel, convert);
      locModel.interpolate();
      if (TablesUtil.deBugLevel > 0) {
        // Print the shell summaries.
        locModel.printShells();
        if (TablesUtil.deBugLevel > 2) {
          // Print out the radial version.
          locModel.printModel(false, false);
        }
        // Print out the Earth flattened version.
        locModel.printModel(true, true);
        // Critical points are model slownesses that need to be sampled exactly.
        locModel.printCriticalPoints();
      }

      // Make the initial slowness sampling.
      sample = new SampleSlowness(locModel);
      sample.sample('P');
      if (TablesUtil.deBugLevel > 0) {
        sample.printModel('P', "Tau");
      }
      sample.sample('S');
      if (TablesUtil.deBugLevel > 0) {
        sample.printModel('S', "Tau");
      }
      // We need a merged set of slownesses for converted branches (e.g., ScP).
      sample.merge();
      if (TablesUtil.deBugLevel > 0) {
        sample.printMerge();
      }
      // Fiddle with the sampling so that low velocity zones are
      // better sampled.
      sample.depthModel('P');
      if (TablesUtil.deBugLevel > 0) {
        sample.depModel.printDepShells('P');
        sample.printModel('P', "Depth");
      }
      sample.depthModel('S');
      if (TablesUtil.deBugLevel > 0) {
        sample.depModel.printDepShells('S');
        sample.printModel('S', "Depth");
      }
      depModel = sample.getDepthModel();
      if (TablesUtil.deBugLevel > 2) {
        depModel.printDepShells('P');
        depModel.printDepShells('S');
      }

      // Do the integrals.
      integrate = new Integrate(depModel);
      integrate.doTauIntegrals('P');
      integrate.doTauIntegrals('S');
      // The final model only includes depth samples that will be
      // of interest for earthquake location.
      finModel = integrate.getFinalModel();
      if (TablesUtil.deBugLevel > 1) {
        finModel.printShellInts('P');
        finModel.printShellInts('S');
      }
      // Reorganize the integral data.
      finModel.makePieces();
      if (TablesUtil.deBugLevel > 0) {
        // These final shells control making the branches.
        finModel.printShellSpec('P');
        finModel.printShellSpec('S');
        if (TablesUtil.deBugLevel > 2) {
          // Proxy depth sampling before decimation.
          finModel.printProxy();
        }
      }
      // Decimate the default sampling for the up-going branches.
      decimate = new DecimateTTBranch(finModel, convert);
      decimate.upGoingDecimation('P');
      decimate.upGoingDecimation('S');
      if (TablesUtil.deBugLevel > 0) {
        if (TablesUtil.deBugLevel > 2) {
          finModel.pPieces.printDec();
          finModel.sPieces.printDec();
        }
        // Proxy depth sampling after decimation.
        finModel.printProxy();
      }

      // Make the branches.
      if (TablesUtil.deBugLevel > 0) {
        finModel.printDepShells('P');
        finModel.printDepShells('S');
      }
      layout = new MakeBranches(finModel, decimate);
      layout.readPhases(phaseFile); // Read the desired phases from a file
      if (TablesUtil.deBugLevel > 0) {
        if (TablesUtil.deBugLevel > 1) {
          layout.printPhases();
        }
        layout.printBranches(false, true);
      }
      brnData = layout.getBranches();
      // Do the final decimation.
      finModel.decimateP();
      if (TablesUtil.deBugLevel > 0) {
        finModel.printP();
      }
      finModel.decimateTauX('P');
      finModel.decimateTauX('S');
      // Print the final branches.
      if (TablesUtil.deBugLevel > 2) {
        layout.printBranches(true, true);
      }
      // Build the branch end ranges.
      finModel.setEnds(layout.getBranchEnds());
    } else {
      System.out.println("failed to read model: " + status);
    }
    return status;
  }

  /**
   * Fill in all the reference data needed to calculate travel times from the table generation.
   *
   * @param serName Name of the model serialization file
   * @param auxTT Auxiliary travel-time data
   * @return The reference data for all branches
   * @throws IOException If serialization file write fails
   */
  public AllBrnRef fillAllBrnRef(String serName, AuxTtRef auxTT) throws IOException {
    return new AllBrnRef(serName, finModel, brnData, auxTT);
  }
}
