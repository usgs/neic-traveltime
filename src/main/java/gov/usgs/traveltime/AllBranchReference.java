package gov.usgs.traveltime;

import gov.usgs.traveltime.tables.BranchData;
import gov.usgs.traveltime.tables.TauModel;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.channels.FileLock;
import java.util.ArrayList;

/**
 * The AllBranchReference class is the umbrella storage for all non-volatile travel-time data.
 * Currently, the data is loaded into appropriate classes from the FORTRAN-ish ReadTau class
 * travelTimeInformation the constructor. However, travelTimeInformation the future, it will read
 * files written by other Java classes. Note that after the data is loaded, ReadTau and all it's
 * storage may be sent to the garbage collector.
 *
 * @author Ray Buland
 */
public class AllBranchReference {
  /** A String containing the earth model name */
  private final String earthModelName;

  /** A ModelDataReference object holding the P wave earth mode data */
  private final ModelDataReference earthModelP;

  /** A ModelDataReference object holding the S wave earth mode data */
  private final ModelDataReference earthModels;

  /** An array of BranchDataReference objects containing the surface focus branch data */
  private final BranchDataReference[] surfaceBranches;

  /** An UpGoingDataReference object containing the up-going P branch data */
  private final UpGoingDataReference upgoingPBranchData;

  /** An UpGoingDataReference object containing the up-going S branch data */
  private final UpGoingDataReference upgoingSBranchData;

  /** A ModelConversions object containing model dependent constants and conversions */
  private final ModelConversions modelConversions;

  /** A AuxiliaryTTReference object holding the model independent travel time auxiliary data */
  private final AuxiliaryTTReference auxTTReference;

  /**
   * Function to return the model name
   *
   * @return A String containing the model name
   */
  public String getEarthModelName() {
    return earthModelName;
  }

  /**
   * Get the model dependent constants and conversions
   *
   * @return A ModelConversions object containing model dependent constants and conversions
   */
  public ModelConversions getModelConversions() {
    return modelConversions;
  }

  /**
   * Get the P wave earth mode data
   *
   * @return A ModelDataReference object holding the P wave earth mode data
   */
  public ModelDataReference getEarthModelP() {
    return earthModelP;
  }

  /**
   * Get the S wave earth mode data
   *
   * @return A ModelDataReference object holding the S wave earth mode data
   */
  public ModelDataReference getEarthModelS() {
    return earthModels;
  }

  /**
   * Get the surface focus branch data
   *
   * @return An array of BranchDataReference objects containing the surface focus branch data
   */
  public BranchDataReference[] getSurfaceBranches() {
    return surfaceBranches;
  }

  /**
   * Get the up-going P branch data
   *
   * @return An UpGoingDataReference object containing the up-going P branch data
   */
  public UpGoingDataReference getUpgoingPBranchData() {
    return upgoingPBranchData;
  }

  /**
   * Get the up-going S branch data
   *
   * @return An UpGoingDataReference object containing the up-going S branch data
   */
  public UpGoingDataReference getUpgoingSBranchData() {
    return upgoingSBranchData;
  }

  /**
   * Get the model independent auxiliary data
   *
   * @return A AuxiliaryTTReference object holding the model independent auxiliary data
   */
  public AuxiliaryTTReference getAuxTTData() {
    return auxTTReference;
  }

  /**
   * AllBranchReference constuctor, load all data from the legacy FORTRAN generated format for
   * travel-time header and table information into more convenient Java classes.
   *
   * @param serializationFileName A string containing the path and name of the model serialization
   *     file
   * @param travelTimeInformation A ReadTau object containing the travel-time header and table
   *     information read from the legacy FORTRAN generated format
   * @param auxTTReference An AuxiliaryTTReference object holding the model independent auxiliary
   *     data
   * @throws IOException If serialization file write fails
   */
  public AllBranchReference(
      String serializationFileName,
      ReadTau travelTimeInformation,
      AuxiliaryTTReference auxTTReference)
      throws IOException {
    // Remember the input data.
    this.earthModelName = travelTimeInformation.modelName;
    this.auxTTReference = auxTTReference;

    // Set up the conversion constants, etc.
    modelConversions = new ModelConversions(travelTimeInformation);

    // Set up the Earth model.
    earthModelP = new ModelDataReference(travelTimeInformation, modelConversions, 'P');
    earthModels = new ModelDataReference(travelTimeInformation, modelConversions, 'S');

    // Set up the segment codes first.
    String[] segmentCodes = new String[travelTimeInformation.numSeg];
    int i = -1;
    int endSegmentIndex = 0;

    // loop over surfaceBranches looking for segment codes
    for (int j = 0; j < travelTimeInformation.numBrn; j++) {
      // Look for a new segment.
      if (travelTimeInformation.indexBrn[j][0] > endSegmentIndex) {
        endSegmentIndex = travelTimeInformation.indexSeg[++i][1];
      }

      // Set the segment code.
      segmentCodes[i] = TauUtilities.phSeg(travelTimeInformation.phaseCode[j]);
    }

    // Load the branch data.
    surfaceBranches = new BranchDataReference[travelTimeInformation.numBrn];
    ExtraPhases extra = new ExtraPhases(auxTTReference);
    i = -1;
    endSegmentIndex = 0;

    // Loop over surfaceBranches setting them up.
    for (int j = 0; j < travelTimeInformation.numBrn; j++) {
      // Look for a new segment.
      if (travelTimeInformation.indexBrn[j][0] > endSegmentIndex) {
        endSegmentIndex = travelTimeInformation.indexSeg[++i][1];
      }

      // Load the branch data.
      surfaceBranches[j] =
          new BranchDataReference(
              travelTimeInformation, j, i, segmentCodes[i], extra, auxTTReference);
    }

    // Set up the up-going branch data.
    upgoingPBranchData = new UpGoingDataReference(travelTimeInformation, 'P');
    upgoingSBranchData = new UpGoingDataReference(travelTimeInformation, 'S');

    // Serialize the model out.
    if (serializationFileName != null) {
      serializeModelToFile(serializationFileName);
    }
  }

  /**
   * AllBranchReference constuctor, Load data from the tau-p table generation classes into the
   * classes supporting the actual travel-time generation. Note that this two step process provides
   * the flexibility to get the data from various places and also allows the reference classes to
   * make all the data final.
   *
   * @param serializationFileName A string containing the path and name of the model serialization
   *     file
   * @param finalTauModel A TauModel object containing the travel-time table generation final tau
   *     model
   * @param branchData An ArrayList of BranchData objects holding the travel-time table generation
   *     branch data
   * @param auxTTReference An AuxiliaryTTReference object holding the model independent auxiliary
   *     data
   * @throws IOException If serialization file write fails
   */
  public AllBranchReference(
      String serializationFileName,
      TauModel finalTauModel,
      ArrayList<BranchData> branchData,
      AuxiliaryTTReference auxTTReference)
      throws IOException {

    // Remember the input data.
    this.earthModelName = finalTauModel.getReferenceEarthModelName();
    this.auxTTReference = auxTTReference;

    // Set up the conversion constants, etc.
    modelConversions = finalTauModel.getModelConversions();

    // Set up the Earth model.
    earthModelP = new ModelDataReference(finalTauModel, modelConversions, 'P');
    earthModels = new ModelDataReference(finalTauModel, modelConversions, 'S');

    // Load the branch data.
    surfaceBranches = new BranchDataReference[branchData.size()];
    ExtraPhases extra = new ExtraPhases(auxTTReference);

    // Loop over surfaceBranches setting them up.
    for (int j = 0; j < surfaceBranches.length; j++) {
      surfaceBranches[j] = new BranchDataReference(branchData.get(j), j, extra, auxTTReference);
    }

    // Set up the up-going branch data.
    upgoingPBranchData = new UpGoingDataReference(finalTauModel, 'P');
    upgoingSBranchData = new UpGoingDataReference(finalTauModel, 'S');

    // Serialize the model out.
    if (serializationFileName != null) {
      serializeModelToFile(serializationFileName);
    }
  }

  /**
   * AllBranchReference, load data from the model serialization file.
   *
   * @param serializationFileName A string containing the path and name of the model serialization
   *     file
   * @param earthModelName A string containing the earth model name
   * @param auxTTDataAn AuxiliaryTTReference object holding the model independent auxiliary data
   * @throws IOException If serialization read fails
   * @throws ClassNotFoundException Serialization object mismatch
   */
  public AllBranchReference(
      String serializationFileName, String earthModelName, AuxiliaryTTReference auxTTReference)
      throws IOException, ClassNotFoundException {
    // Remember the input data.
    this.earthModelName = earthModelName;
    this.auxTTReference = auxTTReference;

    // Read the model.
    FileInputStream serIn = new FileInputStream(serializationFileName);
    ObjectInputStream objIn = new ObjectInputStream(serIn);

    // Wait for a shared lock for reading.
    FileLock lock = serIn.getChannel().lock(0, Long.MAX_VALUE, true);

    //	System.out.println("AllBrnRef read lock: valid = "+lock.isValid()+
    //			" shared = "+lock.isShared());

    // read in the model conversions
    modelConversions = (ModelConversions) objIn.readObject();

    // read in the model data
    earthModelP = (ModelDataReference) objIn.readObject();
    earthModels = (ModelDataReference) objIn.readObject();

    // read in the branches
    surfaceBranches = (BranchDataReference[]) objIn.readObject();
    upgoingPBranchData = (UpGoingDataReference) objIn.readObject();
    upgoingSBranchData = (UpGoingDataReference) objIn.readObject();

    // release the lock
    if (lock.isValid()) {
      lock.release();
    }

    // close the files
    objIn.close();
    serIn.close();
  }

  /**
   * Function to get the number of travel-time surface branches loaded.
   *
   * @return Number of travel-time surface branches loaded.
   */
  public int getNumberOfBranches() {
    return surfaceBranches.length;
  }

  /**
   * Function to Serialize the model classes out to a file.
   *
   * @param serializationFileName A string containing the path and name of the model serialization
   *     file
   * @throws IOException On any serialization IO error
   */
  private void serializeModelToFile(String serializationFileName) throws IOException {
    // Write out the serialized file.
    FileOutputStream serOut = new FileOutputStream(serializationFileName);
    ObjectOutputStream objOut = new ObjectOutputStream(serOut);

    // Wait for an exclusive lock for writing.
    FileLock lock = serOut.getChannel().lock();

    //	System.out.println("AllBrnRef write lock: valid = "+lock.isValid()+
    //			" shared = "+lock.isShared());

    /*
     * The Earth model data can be read and written very quickly, so for persistent
     * applications such as the travel time or location server, serialization is
     * not necessary.  However, if the travel times are needed for applications
     * that start and stop frequently, the serialization should save some set up
     * time.
     */

    // write out the model conversions
    objOut.writeObject(modelConversions);

    // write the model data
    objOut.writeObject(earthModelP);
    objOut.writeObject(earthModels);

    // write the branch data
    objOut.writeObject(surfaceBranches);
    objOut.writeObject(upgoingPBranchData);
    objOut.writeObject(upgoingSBranchData);

    // release the lock
    if (lock.isValid()) {
      lock.release();
    }

    // close the files
    objOut.close();
    serOut.close();
  }

  /** Function to print global or header data for debugging purposes. */
  public void dumpHeaderData() {
    System.out.println("\n     " + earthModelName);
    System.out.format(
        "Normalization: xNorm =%11.4e  vNorm =%11.4e  " + "tNorm =%11.4e\n",
        modelConversions.getDistanceNormalization(),
        modelConversions.getVelocityNormalization(),
        modelConversions.getTauTTNormalization());
    System.out.format(
        "Boundaries: zUpperMantle =%7.1f  zMoho =%7.1f  " + "zConrad =%7.1f\n",
        modelConversions.getUpperMantleDepth(),
        modelConversions.getMohoDepth(),
        modelConversions.getConradDepth());
    System.out.format(
        "Derived: rSurface =%8.1f  zNewUp = %7.1f  " + "dTdDel2P =%11.4e\n",
        modelConversions.getSurfaceRadius(),
        modelConversions.getUpGoingReplacementDepth(),
        modelConversions.get_dTdDelta());
  }

  /**
   * Function to print model parameters for debugging purposes.
   *
   * @param waveType A char containing the wave type ('P' = compressional, 'S' = shear)
   * @param nice A boolean flag indicating whether to print the model travelTimeInformation
   *     dimensional units
   */
  public void dumpModelParams(char waveType, boolean nice) {
    if (waveType == 'P') {
      earthModelP.dumpModel(nice);
    } else if (waveType == 'S') {
      earthModels.dumpModel(nice);
    }
  }

  /**
   * Function to print surface branch data for one travel-time branch for debugging purposes.
   *
   * @param branchIndex An integer containing the branch index number
   * @param full A boolean flag indicating whether to print the detailed branch specification as
   *     well
   */
  public void dumpBranchData(int branchIndex, boolean full) {
    surfaceBranches[branchIndex].dumpBrn(full);
  }

  /**
   * Function to print surface branch data for one travel-time phase code for debugging purposes.
   *
   * @param phaseCode A string containing the desired phase code
   * @param full A boolean flag indicating whether to print the detailed branch specification as
   *     well
   */
  public void dumpBranchData(String phaseCode, boolean full) {
    for (int j = 0; j < surfaceBranches.length; j++) {
      if (surfaceBranches[j].getBranchPhaseCode().equals(phaseCode)) {
        surfaceBranches[j].dumpBrn(full);
      }
    }
  }

  /**
   * Function to print data for all surface branches for debugging purposes.
   *
   * @param full A boolean flag indicating whether to print the detailed branch specification as
   *     well
   */
  public void dumpBranchData(boolean full) {
    for (int j = 0; j < surfaceBranches.length; j++) {
      surfaceBranches[j].dumpBrn(full);
    }
  }

  /**
   * Function to print data for one travel-time segment for debugging purposes.
   *
   * @param segmentPhaseCode A String containing the segment phase code
   * @param full A boolean flag indicating whether to print the detailed branch specification as
   *     well
   */
  public void dumpSegment(String segmentPhaseCode, boolean full) {
    for (int j = 0; j < surfaceBranches.length; j++) {
      if (surfaceBranches[j].getGenericPhaseCode().equals(segmentPhaseCode)) {
        surfaceBranches[j].dumpBrn(full);
      }
    }
  }

  /**
   * Function to print data for one up-going branch for debugging purposes.
   *
   * @param waveType A char containing the wave type ('P' = compressional, 'S' = shear)
   * @param depthIndex An integer contaiing the depth index
   */
  public void dumpUpGoingData(char waveType, int depthIndex) {
    if (waveType == 'P') {
      upgoingPBranchData.dumpUp(depthIndex);
    } else if (waveType == 'S') {
      upgoingSBranchData.dumpUp(depthIndex);
    }
  }

  /**
   * Function to print data for all up-going branches for debugging purposes.
   *
   * @param waveType A char containing the wave type ('P' = compressional, 'S' = shear)
   */
  public void dumpUpGoingData(char waveType) {
    if (waveType == 'P') {
      upgoingPBranchData.dumpUp(earthModelP, modelConversions);
    } else if (waveType == 'S') {
      upgoingSBranchData.dumpUp(earthModels, modelConversions);
    }
  }
}
