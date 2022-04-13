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
 * Currently, the data is loaded into appropriate classes from the FORTRAN-ish ReadTau class in the
 * constructor. However, in the future, it will read files written by other Java classes. Note that
 * after the data is loaded, ReadTau and all it's storage may be sent to the garbage collector.
 *
 * @author Ray Buland
 */
public class AllBranchReference {
  /** A String containing the earth model name */
  private final String earthModelName;

  /** A ModelDataReference object holding the P wave earth mode data */
  private final ModelDataReference EarthModelP;

  /** A ModelDataReference object holding the S wave earth mode data */
  private final ModelDataReference EarthModelS;

  /** An array of */
  final BranchDataReference[] branches; // Surface focus branch data

  final UpGoingDataReference pUp, sUp; // Up-going branch data

  /** A ModelConversions object containing model dependent constants and conversions */
  private final ModelConversions modelConversions;

  final AuxiliaryTTReference auxtt; // Model independent auxiliary data

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
    return EarthModelP;
  }

  /**
   * Get the S wave earth mode data
   *
   * @return A ModelDataReference object holding the S wave earth mode data
   */
  public ModelDataReference getEarthModelS() {
    return EarthModelS;
  }

  /**
   * Load all data from TauRead into more convenient Java classes.
   *
   * @param serName Name of the model serialization file
   * @param in The TauRead data source
   * @param auxtt The auxiliary data source
   * @throws IOException If serialization file write fails
   */
  public AllBranchReference(String serName, ReadTau in, AuxiliaryTTReference auxtt)
      throws IOException {
    String[] segCode;

    // Remember the input data.
    this.earthModelName = in.modelName;
    this.auxtt = auxtt;

    // Set up the conversion constants, etc.
    modelConversions = new ModelConversions(in);

    // Set up the Earth model.
    EarthModelP = new ModelDataReference(in, modelConversions, 'P');
    EarthModelS = new ModelDataReference(in, modelConversions, 'S');

    // Set up the segment codes first.
    segCode = new String[in.numSeg];
    int i = -1;
    int endSeg = 0;
    for (int j = 0; j < in.numBrn; j++) {
      // Look for a new segment.
      if (in.indexBrn[j][0] > endSeg) {
        endSeg = in.indexSeg[++i][1];
      }
      // Set the segment code.
      segCode[i] = TauUtilities.phSeg(in.phCode[j]);
    }

    // Load the branch data.
    branches = new BranchDataReference[in.numBrn];
    ExtraPhases extra = new ExtraPhases(auxtt);
    i = -1;
    endSeg = 0;
    // Loop over branches setting them up.
    for (int j = 0; j < in.numBrn; j++) {
      // Look for a new segment.
      if (in.indexBrn[j][0] > endSeg) {
        endSeg = in.indexSeg[++i][1];
      }
      // Load the branch data.
      branches[j] = new BranchDataReference(in, j, i, segCode[i], extra, auxtt);
    }

    // Set up the up-going branch data.
    pUp = new UpGoingDataReference(in, 'P');
    sUp = new UpGoingDataReference(in, 'S');

    // Serialize the model out.
    if (serName != null) {
      serialOut(serName);
    }
  }

  /**
   * Load data from the tau-p table generation classes into the classes supporting the actual
   * travel-time generation. Note that this two step process provides the flexibility to get the
   * data from various places and also allows the reference classes to make all the data final.
   *
   * @param serName Name of the model serialization file
   * @param finModel Travel-time table generation final tau model
   * @param brnData Travel-time table generation branch data
   * @param auxtt The auxiliary data source
   * @throws IOException If serialization file write fails
   */
  public AllBranchReference(
      String serName, TauModel finModel, ArrayList<BranchData> brnData, AuxiliaryTTReference auxtt)
      throws IOException {

    // Remember the input data.
    this.earthModelName = finModel.getReferenceEarthModelName();
    this.auxtt = auxtt;

    // Set up the conversion constants, etc.
    modelConversions = finModel.getModelConversions();

    // Set up the Earth model.
    EarthModelP = new ModelDataReference(finModel, modelConversions, 'P');
    EarthModelS = new ModelDataReference(finModel, modelConversions, 'S');

    // Load the branch data.
    branches = new BranchDataReference[brnData.size()];
    ExtraPhases extra = new ExtraPhases(auxtt);
    // Loop over branches setting them up.
    for (int j = 0; j < branches.length; j++) {
      branches[j] = new BranchDataReference(brnData.get(j), j, extra, auxtt);
    }

    // Set up the up-going branch data.
    pUp = new UpGoingDataReference(finModel, 'P');
    sUp = new UpGoingDataReference(finModel, 'S');

    // Serialize the model out.
    if (serName != null) {
      serialOut(serName);
    }
  }

  /**
   * Load data from the model serialization file.
   *
   * @param serName Name of the model serialization file
   * @param earthModelName Earth model name
   * @param auxtt The auxiliary data source
   * @throws IOException If serialization read fails
   * @throws ClassNotFoundException Serialization object mismatch
   */
  public AllBranchReference(String serName, String earthModelName, AuxiliaryTTReference auxtt)
      throws IOException, ClassNotFoundException {
    FileInputStream serIn;
    ObjectInputStream objIn;
    FileLock lock;

    // Remember the input data.
    this.earthModelName = earthModelName;
    this.auxtt = auxtt;

    // Read the model.
    serIn = new FileInputStream(serName);
    objIn = new ObjectInputStream(serIn);
    // Wait for a shared lock for reading.
    lock = serIn.getChannel().lock(0, Long.MAX_VALUE, true);
    //	System.out.println("AllBrnRef read lock: valid = "+lock.isValid()+
    //			" shared = "+lock.isShared());
    modelConversions = (ModelConversions) objIn.readObject();
    EarthModelP = (ModelDataReference) objIn.readObject();
    EarthModelS = (ModelDataReference) objIn.readObject();
    branches = (BranchDataReference[]) objIn.readObject();
    pUp = (UpGoingDataReference) objIn.readObject();
    sUp = (UpGoingDataReference) objIn.readObject();
    if (lock.isValid()) lock.release();
    objIn.close();
    serIn.close();
  }

  /**
   * Get the number of travel-time branches loaded.
   *
   * @return Number of travel-time branches loaded.
   */
  public int getNoBranches() {
    return branches.length;
  }

  /**
   * Serialize the model classes out to a file.
   *
   * @param serName Name of the model serialization file
   * @throws IOException On any serialization IO error
   */
  private void serialOut(String serName) throws IOException {
    FileOutputStream serOut;
    ObjectOutputStream objOut;
    FileLock lock;

    // Write out the serialized file.
    serOut = new FileOutputStream(serName);
    objOut = new ObjectOutputStream(serOut);
    // Wait for an exclusive lock for writing.
    lock = serOut.getChannel().lock();
    //	System.out.println("AllBrnRef write lock: valid = "+lock.isValid()+
    //			" shared = "+lock.isShared());
    /*
     * The Earth model data can be read and written very quickly, so for persistent
     * applications such as the travel time or location server, serialization is
     * not necessary.  However, if the travel times are needed for applications
     * that start and stop frequently, the serialization should save some set up
     * time.
     */
    objOut.writeObject(modelConversions);
    objOut.writeObject(EarthModelP);
    objOut.writeObject(EarthModelS);
    objOut.writeObject(branches);
    objOut.writeObject(pUp);
    objOut.writeObject(sUp);
    if (lock.isValid()) lock.release();
    objOut.close();
    serOut.close();
  }

  /** Print global or header data for debugging purposes. */
  public void dumpHead() {
    System.out.println("\n     " + earthModelName);
    System.out.format(
        "Normalization: xNorm =%11.4e  vNorm =%11.4e  " + "tNorm =%11.4e\n",
        modelConversions.xNorm, modelConversions.vNorm, modelConversions.tNorm);
    System.out.format(
        "Boundaries: zUpperMantle =%7.1f  zMoho =%7.1f  " + "zConrad =%7.1f\n",
        modelConversions.zUpperMantle, modelConversions.zMoho, modelConversions.zConrad);
    System.out.format(
        "Derived: rSurface =%8.1f  zNewUp = %7.1f  " + "dTdDel2P =%11.4e\n",
        modelConversions.rSurface, modelConversions.zNewUp, modelConversions.dTdDelta);
  }

  /**
   * Print model parameters for debugging purposes.
   *
   * @param typeMod Wave type ('P' or 'S')
   * @param nice If true print the model in dimensional units
   */
  public void dumpMod(char typeMod, boolean nice) {
    if (typeMod == 'P') {
      EarthModelP.dumpMod(nice);
    } else if (typeMod == 'S') {
      EarthModelS.dumpMod(nice);
    }
  }

  /**
   * Print data for one travel-time branch for debugging purposes.
   *
   * @param iBrn Branch number
   * @param full If true print the detailed branch specification as well
   */
  public void dumpBrn(int iBrn, boolean full) {
    branches[iBrn].dumpBrn(full);
  }

  /**
   * Print data for one travel-time phase code for debugging purposes.
   *
   * @param phCode Phase code
   * @param full If true, print the detailed specification for each branch as well
   */
  public void dumpBrn(String phCode, boolean full) {
    for (int j = 0; j < branches.length; j++) {
      if (branches[j].phCode.equals(phCode)) branches[j].dumpBrn(full);
    }
  }

  /**
   * Print data for all travel-time segments for debugging purposes.
   *
   * @param full If true, print the detailed specification for each branch as well
   */
  public void dumpBrn(boolean full) {
    for (int j = 0; j < branches.length; j++) {
      branches[j].dumpBrn(full);
    }
  }

  /**
   * Print data for one travel-time segment for debugging purposes.
   *
   * @param seg Segment phase code
   * @param full If true, print the detailed specification for each branch as well
   */
  public void dumpSeg(String seg, boolean full) {
    for (int j = 0; j < branches.length; j++) {
      if (branches[j].getPhSeg().equals(seg)) branches[j].dumpBrn(full);
    }
  }

  /**
   * Print data for one up-going branch for debugging purposes.
   *
   * @param typeUp Wave type ('P' or 'S')
   * @param iUp Depth index
   */
  public void dumpUp(char typeUp, int iUp) {
    if (typeUp == 'P') {
      pUp.dumpUp(iUp);
    } else if (typeUp == 'S') {
      sUp.dumpUp(iUp);
    }
  }

  /**
   * Print data for all up-going branches for debugging purposes.
   *
   * @param typeUp Wave type ('P' or 'S')
   */
  public void dumpUp(char typeUp) {
    if (typeUp == 'P') {
      pUp.dumpUp(EarthModelP, modelConversions);
    } else if (typeUp == 'S') {
      sUp.dumpUp(EarthModelS, modelConversions);
    }
  }
}
