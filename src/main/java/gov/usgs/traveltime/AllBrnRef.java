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
 * Umbrella storage for all non-volatile travel-time data. Currently, the data is loaded into
 * appropriate classes from the FORTRAN-ish ReadTau class in the constructor. However, in the
 * future, it will read files written by other Java classes. Note that after the data is loaded,
 * ReadTau and all it's storage may be sent to the garbage collector.
 *
 * @author Ray Buland
 */
public class AllBrnRef {
  final String modelName; // Earth model name
  final ModDataRef pModel, sModel; // Earth model data
  final BrnDataRef[] branches; // Surface focus branch data
  final UpDataRef pUp, sUp; // Up-going branch data
  final ModConvert cvt; // Model dependent conversions
  final AuxTtRef auxtt; // Model independent auxiliary data

  /**
   * Load all data from TauRead into more convenient Java classes.
   *
   * @param serName Name of the model serialization file
   * @param in The TauRead data source
   * @param auxtt The auxiliary data source
   * @throws IOException If serialization file write fails
   */
  public AllBrnRef(String serName, ReadTau in, AuxTtRef auxtt) throws IOException {
    String[] segCode;

    // Remember the input data.
    this.modelName = in.modelName;
    this.auxtt = auxtt;

    // Set up the conversion constants, etc.
    cvt = new ModConvert(in);

    // Set up the Earth model.
    pModel = new ModDataRef(in, cvt, 'P');
    sModel = new ModDataRef(in, cvt, 'S');

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
      segCode[i] = TauUtil.phSeg(in.phCode[j]);
    }

    // Load the branch data.
    branches = new BrnDataRef[in.numBrn];
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
      branches[j] = new BrnDataRef(in, j, i, segCode[i], extra, auxtt);
    }

    // Set up the up-going branch data.
    pUp = new UpDataRef(in, 'P');
    sUp = new UpDataRef(in, 'S');

    // Serialize the model out.
    if (serName != null) serialOut(serName);
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
  public AllBrnRef(String serName, TauModel finModel, ArrayList<BranchData> brnData, AuxTtRef auxtt)
      throws IOException {

    // Remember the input data.
    this.modelName = finModel.getModelName();
    this.auxtt = auxtt;

    // Set up the conversion constants, etc.
    cvt = finModel.getConvert();

    // Set up the Earth model.
    pModel = new ModDataRef(finModel, cvt, 'P');
    sModel = new ModDataRef(finModel, cvt, 'S');

    // Load the branch data.
    branches = new BrnDataRef[brnData.size()];
    ExtraPhases extra = new ExtraPhases(auxtt);
    // Loop over branches setting them up.
    for (int j = 0; j < branches.length; j++) {
      branches[j] = new BrnDataRef(brnData.get(j), j, extra, auxtt);
    }

    // Set up the up-going branch data.
    pUp = new UpDataRef(finModel, 'P');
    sUp = new UpDataRef(finModel, 'S');

    // Serialize the model out.
    if (serName != null) serialOut(serName);
  }

  /**
   * Load data from the model serialization file.
   *
   * @param serName Name of the model serialization file
   * @param modelName Earth model name
   * @param auxtt The auxiliary data source
   * @throws IOException If serialization read fails
   * @throws ClassNotFoundException Serialization object mismatch
   */
  public AllBrnRef(String serName, String modelName, AuxTtRef auxtt)
      throws IOException, ClassNotFoundException {
    FileInputStream serIn;
    ObjectInputStream objIn;
    FileLock lock;

    // Remember the input data.
    this.modelName = modelName;
    this.auxtt = auxtt;

    // Read the model.
    serIn = new FileInputStream(serName);
    objIn = new ObjectInputStream(serIn);
    // Wait for a shared lock for reading.
    lock = serIn.getChannel().lock(0, Long.MAX_VALUE, true);
    //	System.out.println("AllBrnRef read lock: valid = "+lock.isValid()+
    //			" shared = "+lock.isShared());
    cvt = (ModConvert) objIn.readObject();
    pModel = (ModDataRef) objIn.readObject();
    sModel = (ModDataRef) objIn.readObject();
    branches = (BrnDataRef[]) objIn.readObject();
    pUp = (UpDataRef) objIn.readObject();
    sUp = (UpDataRef) objIn.readObject();
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
    objOut.writeObject(cvt);
    objOut.writeObject(pModel);
    objOut.writeObject(sModel);
    objOut.writeObject(branches);
    objOut.writeObject(pUp);
    objOut.writeObject(sUp);
    if (lock.isValid()) lock.release();
    objOut.close();
    serOut.close();
  }

  /** Print global or header data for debugging purposes. */
  public void dumpHead() {
    System.out.println("\n     " + modelName);
    System.out.format(
        "Normalization: xNorm =%11.4e  vNorm =%11.4e  " + "tNorm =%11.4e\n",
        cvt.xNorm, cvt.vNorm, cvt.tNorm);
    System.out.format(
        "Boundaries: zUpperMantle =%7.1f  zMoho =%7.1f  " + "zConrad =%7.1f\n",
        cvt.zUpperMantle, cvt.zMoho, cvt.zConrad);
    System.out.format(
        "Derived: rSurface =%8.1f  zNewUp = %7.1f  " + "dTdDel2P =%11.4e\n",
        cvt.rSurface, cvt.zNewUp, cvt.dTdDelta);
  }

  /**
   * Print model parameters for debugging purposes.
   *
   * @param typeMod Wave type ('P' or 'S')
   * @param nice If true print the model in dimensional units
   */
  public void dumpMod(char typeMod, boolean nice) {
    if (typeMod == 'P') {
      pModel.dumpMod(nice);
    } else if (typeMod == 'S') {
      sModel.dumpMod(nice);
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
      pUp.dumpUp(pModel, cvt);
    } else if (typeUp == 'S') {
      sUp.dumpUp(sModel, cvt);
    }
  }
}
