package gov.usgs.traveltime.tables;

import gov.usgs.traveltime.AllBranchReference;
import gov.usgs.traveltime.AuxiliaryTTReference;
import gov.usgs.traveltime.ModelConversions;
import gov.usgs.traveltime.TravelTimeStatus;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Travel-time table generation driver.
 *
 * @author Ray Buland
 */
public class MakeTables {
  /** An EarthModel object containing the reference earth model */
  private EarthModel referenceEarthModel;

  /** A TauModel object containing the final model */
  private TauModel finalTTModel;

  /** An ArrayList of BranchData objects containing the branch data tables */
  private ArrayList<BranchData> branchDataTables;

  /**
   * MakeTables constructor, initializes the reference Earth model. This needs to happen in the
   * constructor to ensure that the travel-time properties file has been read.
   *
   * @param earthModel A String containing the name of the Earth model to use
   */
  public MakeTables(String earthModel) {
    referenceEarthModel = new EarthModel(earthModel, true);
  }

  /**
   * Create the travel-time tables out of whole cloth.
   *
   * @param earthModelFile A String containing the path to the the Earth model file
   * @param phaseListFile A string containing the path to the the file of containing the desired
   *     phases
   * @return A TravelTimeStatus object containing the model read status
   * @throws Exception If an integration interval is illegal
   */
  public TravelTimeStatus buildModel(String earthModelFile, String phaseListFile) throws Exception {
    // Read the model.
    TravelTimeStatus status = referenceEarthModel.readModelFile(earthModelFile);

    // If it read OK, process it.
    if (status == TravelTimeStatus.SUCCESS) {
      if (TablesUtil.deBugLevel > 2) {
        // Print the shell summaries.
        referenceEarthModel.printShells();

        // Print out the radial version.
        referenceEarthModel.printModel();
      }

      // Interpolate the model.
      ModelConversions modelConversions = referenceEarthModel.getModelConversions();
      EarthModel localModel = new EarthModel(referenceEarthModel, modelConversions);
      localModel.interpolate();

      if (TablesUtil.deBugLevel > 0) {
        // Print the shell summaries.
        localModel.printShells();
        if (TablesUtil.deBugLevel > 2) {
          // Print out the radial version.
          localModel.printModel(false, false);
        }
        // Print out the Earth flattened version.
        localModel.printModel(true, true);
        // Critical points are model slownesses that need to be sampled exactly.
        localModel.printCriticalPoints();
      }

      // Make the initial slowness sampling.
      SampleSlowness slownessSampling = new SampleSlowness(localModel);
      slownessSampling.sample('P');
      if (TablesUtil.deBugLevel > 0) {
        slownessSampling.printModel('P', "Tau");
      }

      slownessSampling.sample('S');
      if (TablesUtil.deBugLevel > 0) {
        slownessSampling.printModel('S', "Tau");
      }

      // We need a merged set of slownesses for converted branches (e.g., ScP).
      slownessSampling.merge();
      if (TablesUtil.deBugLevel > 0) {
        slownessSampling.printMergedSlownesses();
      }

      // Fiddle with the sampling so that low velocity zones are
      // better sampled.
      slownessSampling.depthModel('P');
      if (TablesUtil.deBugLevel > 0) {
        slownessSampling.getTauDepthModel().printDepthShells('P');
        slownessSampling.printModel('P', "Depth");
      }

      slownessSampling.depthModel('S');
      if (TablesUtil.deBugLevel > 0) {
        slownessSampling.getTauDepthModel().printDepthShells('S');
        slownessSampling.printModel('S', "Depth");
      }

      TauModel depthModel = slownessSampling.getTauDepthModel();
      if (TablesUtil.deBugLevel > 2) {
        depthModel.printDepthShells('P');
        depthModel.printDepthShells('S');
      }

      // Do the integrals.
      Integrate integrate = new Integrate(depthModel);
      integrate.doTauIntegrals('P');
      integrate.doTauIntegrals('S');

      // The final model only includes depth samples that will be
      // of interest for earthquake location.
      finalTTModel = integrate.getFinalModel();
      if (TablesUtil.deBugLevel > 1) {
        finalTTModel.printShellIntegrals('P');
        finalTTModel.printShellIntegrals('S');
      }

      // Reorganize the integral data.
      finalTTModel.makePieces();
      if (TablesUtil.deBugLevel > 0) {
        // These final shells control making the branches.
        finalTTModel.printSpecialTauIntegrals('P');
        finalTTModel.printSpecialTauIntegrals('S');
        if (TablesUtil.deBugLevel > 2) {
          // Proxy depth sampling before decimation.
          finalTTModel.printProxyRanges();
        }
      }

      // Decimate the default sampling for the up-going branches.
      DecimateTravelTimeBranch decimate =
          new DecimateTravelTimeBranch(finalTTModel, modelConversions);
      decimate.upGoingDecimation('P');
      decimate.upGoingDecimation('S');

      if (TablesUtil.deBugLevel > 0) {
        if (TablesUtil.deBugLevel > 2) {
          finalTTModel.getIntPiecesP().printDec();
          finalTTModel.getIntPiecesS().printDec();
        }

        // Proxy depth sampling after decimation.
        finalTTModel.printProxyRanges();
      }

      // Make the branches.
      if (TablesUtil.deBugLevel > 0) {
        finalTTModel.printDepthShells('P');
        finalTTModel.printDepthShells('S');
      }

      MakeBranches layout = new MakeBranches(finalTTModel, decimate);
      layout.readPhases(phaseListFile); // Read the desired phases from a file

      if (TablesUtil.deBugLevel > 0) {
        if (TablesUtil.deBugLevel > 1) {
          layout.printPhases();
        }
        layout.printBranches(false, true);
      }

      branchDataTables = layout.getBranchList();

      // Do the final decimation.
      finalTTModel.decimateRayParameters();
      if (TablesUtil.deBugLevel > 0) {
        finalTTModel.printRayParameters();
      }
      finalTTModel.decimateTauRange('P');
      finalTTModel.decimateTauRange('S');

      // Print the final branches.
      if (TablesUtil.deBugLevel > 2) {
        layout.printBranches(true, true);
      }

      // Build the branch end ranges.
      finalTTModel.setEnds(layout.getBranchEnds());
    } else {
      System.out.println("failed to read model: " + status);
    }

    return status;
  }

  /**
   * Function to fill in all the reference data needed to calculate travel times from the table
   * generation.
   *
   * @param modelSerializationFile A string containing the name of the model serialization file
   * @param AuxTravelTimeData An AuxiliaryTTReference object containing the auxiliary travel-time
   *     data
   * @return An AllBrnRef object containing the the reference data for all branches
   * @throws IOException If the writing of the serialization fails
   */
  public AllBranchReference fillInBranchReferenceData(
      String modelSerializationFile, AuxiliaryTTReference AuxTravelTimeData) throws IOException {
    return new AllBranchReference(
        modelSerializationFile, finalTTModel, branchDataTables, AuxTravelTimeData);
  }
}
