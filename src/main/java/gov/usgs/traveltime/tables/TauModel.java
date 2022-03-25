package gov.usgs.traveltime.tables;

import gov.usgs.traveltime.ModConvert;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * The TauModel class contains an alternative version of the earth model with sampling suitable for
 * the tau-p travel-time calculation. Unlike the Earth models, the tau models are in depth order
 * (i.e., the first point is at the free surface). This class is used multiple times for various
 * views of the model. The final model also includes the tau and range integrals.
 *
 * @author Ray Buland
 */
public class TauModel {
  /** An array of doubles containing the ray parameter branch ends */
  private double[] rayParamBranchEnds;

  /** A EarthModel object containing the reference earth model */
  private EarthModel referenceModel;

  /** An ArrayList of TauSample objects containing the P slowness model */
  private ArrayList<TauSample> slownessModelP;

  /** An ArrayList of TauSample objects containing the S slowness model */
  private ArrayList<TauSample> slownessModelS;

  /** An ArrayList of double values containing the merged P- and S-wave slownesses */
  private ArrayList<Double> slowness;

  /** An ArrayList of ModelShell objects containing the P shells */
  private ArrayList<ModelShell> shellModelP = null;

  /** An ArrayList of ModelShell objects containing the S shells */
  private ArrayList<ModelShell> shellModelS = null;

  /** An ArrayList of TauXsample objects containing the P integrals */
  private ArrayList<TauXsample> integralsP = null;

  /** An ArrayList of TauXsample objects containing the S integrals */
  private ArrayList<TauXsample> integralsS = null;

  /** An BranchIntegrals objects containing the P integral branch pieces */
  private BranchIntegrals intPiecesP;

  /** An BranchIntegrals objects containing the S integral branch pieces */
  private BranchIntegrals intPiecesS;

  /** A ModConvert object containing model dependent constants and conversions */
  private ModConvert modelConversions;

  /**
   * Get the reference earth model.
   *
   * @return An EarthModel object holding the reference earth model
   */
  public EarthModel getReferenceModel() {
    return referenceModel;
  }

  /**
   * Get the model dependant constants and conversions.
   *
   * @return A ModConvert object holding the model dependant constants and conversions
   */
  public ModConvert getModelConversions() {
    return modelConversions;
  }

  /**
   * Get the merged P- and S-wave slownesses
   *
   * @return An ArrayList of Double objects holding the merged P- and S-wave slownesses
   */
  public ArrayList<Double> getSlowness() {
    return slowness;
  }

  /**
   * Get the P-wave shells
   *
   * @return An ArrayList of ModelShell objects containing the P shells
   */
  public ArrayList<ModelShell> getShellModelP() {
    return shellModelP;
  }

  /**
   * Get the S-wave shels
   *
   * @return An ArrayList of ModelShell objects containing the S shells
   */
  public ArrayList<ModelShell> getShellModelS() {
    return shellModelS;
  }

  /**
   * Get the P-wave integrals
   *
   * @return An ArrayList of TauXsample objects containing the P integrals
   */
  public ArrayList<TauXsample> getIntegralsP() {
    return integralsP;
  }

  /**
   * Get the S-wave integrals
   *
   * @return An ArrayList of TauXsample objects containing the S integrals
   */
  public ArrayList<TauXsample> getIntegralsS() {
    return integralsS;
  }

  /**
   * Get the P integral branch pieces
   *
   * @return A BranchIntegrals object containing the P integral branch pieces
   */
  public BranchIntegrals getIntPiecesP() {
    return intPiecesP;
  }

  /**
   * Get the S integral branch pieces
   *
   * @return A BranchIntegrals object containing the S integral branch pieces
   */
  public BranchIntegrals getIntPiecesS() {
    return intPiecesS;
  }

  /**
   * Get the Earth model name.
   *
   * @return A string containing the the Earth model name
   */
  public String getReferenceEarthModelName() {
    return referenceModel.getEarthModelName();
  }

  /**
   * Function to get the ray parameters associated with the branch ends.
   *
   * @return Array of branch end ray parameters.
   */
  public double[] getRayParamBranchEnds() {
    return rayParamBranchEnds;
  }

  /**
   * TauModel constructor, allocates lists for independent P and S models.
   *
   * @param referenceModel An EarthModel object containing the reference Earth model
   * @param modelConversions A ModConvert object holding the model dependent conversions
   */
  public TauModel(EarthModel referenceModel, ModConvert modelConversions) {
    this.referenceModel = referenceModel;
    this.modelConversions = modelConversions;
    slownessModelP = new ArrayList<TauSample>();
    slownessModelS = new ArrayList<TauSample>();
  }

  /**
   * Function to initilize the integrals. Only the final model actually has integrals, so initialize
   * them separately.
   */
  public void initIntegrals() {
    integralsP = new ArrayList<TauXsample>();
    integralsS = new ArrayList<TauXsample>();
  }

  /**
   * Function to add a sample to the model by ray travel distance.
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   * @param earthRadius A double holding the dimensional Earth radius in kilometers
   * @param slowness A double containing the non-dimensional slowness
   * @param rayTravelDist A double containing the non-dimensional ray travel distance (range)
   */
  public void add(char modelType, double earthRadius, double slowness, double rayTravelDist) {
    if (modelType == 'P') {
      slownessModelP.add(new TauSample(earthRadius, slowness, rayTravelDist));
    } else {
      slownessModelS.add(new TauSample(earthRadius, slowness, rayTravelDist));
    }
  }

  /**
   * Function to add a sample to the model by index.
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   * @param earthRadius A double holding the dimensional Earth radius in kilometers
   * @param slowness A double containing the non-dimensional slowness
   * @param index An integer containing the index of the merged slownesses
   */
  public void add(char modelType, double earthRadius, double slowness, int index) {
    if (modelType == 'P') {
      slownessModelP.add(new TauSample(earthRadius, slowness, index, modelConversions));
    } else {
      slownessModelS.add(new TauSample(earthRadius, slowness, index, modelConversions));
    }
  }

  /**
   * Function to add a sample to the model.
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   * @param sample A TauSample object holding a complete sample created externally
   */
  public void add(char modelType, TauSample sample) {
    if (modelType == 'P') {
      slownessModelP.add(new TauSample(sample));
    } else {
      slownessModelS.add(new TauSample(sample));
    }
  }

  /**
   * Function to add a sample with an index to the model.
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   * @param sample A TauSample object holding a complete sample created externally
   * @param index An integer containing the desired index of the sample
   */
  public void add(char modelType, TauSample sample, int index) {
    if (modelType == 'P') {
      slownessModelP.add(new TauSample(sample, index));
    } else {
      slownessModelS.add(new TauSample(sample, index));
    }
  }

  /**
   * Function to add a sample with an index to the model and at the same time store tau and range
   * integrals for all ray parameters down to this bottoming depth for the specified phase type.
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   * @param sample A TauSample object holding a complete sample created externally
   * @param index An integer containing the desired index of the sample
   * @param tauRangeInt A TauXsample object containing the set of tau and range integrals
   */
  public void add(char modelType, TauSample sample, int index, TauXsample tauRangeInt) {
    if (modelType == 'P') {
      for (int j = integralsP.size(); j < slownessModelP.size(); j++) {
        integralsP.add(null);
      }

      slownessModelP.add(new TauSample(sample, index));
      integralsP.add(tauRangeInt);
    } else {
      for (int j = integralsS.size(); j < slownessModelS.size(); j++) {
        integralsS.add(null);
      }

      slownessModelS.add(new TauSample(sample, index));
      integralsS.add(tauRangeInt);
    }
  }

  /**
   * Function to get a tau model sample.
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   * @param index An integer containing the desired model index
   * @return A TauSample object containing the tau model sample
   */
  public TauSample getSample(char modelType, int index) {
    if (modelType == 'P') {
      return slownessModelP.get(index);
    } else {
      return slownessModelS.get(index);
    }
  }

  /**
   * Function to retrieve the last tau model sample.
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   * @return A TauSample object containing the last tau model sample
   */
  public TauSample getLast(char modelType) {
    if (modelType == 'P') {
      return slownessModelP.get(slownessModelP.size() - 1);
    } else {
      return slownessModelS.get(slownessModelS.size() - 1);
    }
  }

  /**
   * Function to get get the index of the model sample with the specified slowness.
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   * @param slowness A double containing the non-dimensional slowness
   * @return An integer containing the model index associated with the given non-dimensional
   *     slowness
   */
  public int getIndex(char modelType, double slowness) {
    if (modelType == 'P') {
      for (int j = 0; j < slownessModelP.size(); j++) {
        if (slownessModelP.get(j).slow == slowness) {
          return j;
        }
      }

      return -1;
    } else {
      for (int j = 0; j < slownessModelS.size(); j++) {
        if (slownessModelS.get(j).slow == slowness) {
          return j;
        }
      }

      return -1;
    }
  }

  /**
   * Function to set the last sample in the model.
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   * @param sample A TauSample object holding a complete sample created externally
   */
  public void setLast(char modelType, TauSample sample) {
    if (modelType == 'P') {
      slownessModelP.set(slownessModelP.size() - 1, new TauSample(sample));
    } else {
      slownessModelS.set(slownessModelS.size() - 1, new TauSample(sample));
    }
  }

  /**
   * Function to get the number of model samples.
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   * @return An integer containing the number of model samples
   */
  public int size(char modelType) {
    if (modelType == 'P') {
      return slownessModelP.size();
    } else {
      return slownessModelS.size();
    }
  }

  /**
   * Function to merge the P- and S-wave slownesses into a single list. To avoid making the spacing
   * very non-uniform, the merge is done between critical slownesses. For each interval, the
   * sampling that seems most complete (judging by the number of samples) is used.
   *
   * @param localModel An EarthModel object holding the earth model to use for merging
   */
  public void merge(EarthModel localModel) {
    ArrayList<CriticalSlowness> critical = localModel.getCriticalSlownesses();
    slowness = new ArrayList<Double>();
    CriticalSlowness crit1 = critical.get(critical.size() - 1);
    int beginIndexP = 1;
    int beginIndexS = 0;

    for (int iCrit = critical.size() - 2; iCrit >= 0; iCrit--) {
      CriticalSlowness crit0 = crit1;
      crit1 = critical.get(iCrit);
      int endIndexP;
      int endIndexS;

      if (TablesUtil.deBugLevel > 1) {
        System.out.format(
            "\tInterval: " + "%8.6f %8.6f\n", crit0.getSlowness(), crit1.getSlowness());
      }

      if (crit0.getSlowness() <= slownessModelP.get(beginIndexP - 1).slow) {
        for (endIndexP = beginIndexP; endIndexP < slownessModelP.size(); endIndexP++) {
          if (crit1.getSlowness() == slownessModelP.get(endIndexP).slow) {
            break;
          }
        }
      } else {
        endIndexP = 0;
      }

      for (endIndexS = beginIndexS; endIndexS < slownessModelS.size(); endIndexS++) {
        if (crit1.getSlowness() == slownessModelS.get(endIndexS).slow) {
          break;
        }
      }

      if (TablesUtil.deBugLevel > 1) {
        System.out.format(
            "\tIndices: " + "P: %d %d S: %d %d\n", beginIndexP, endIndexP, beginIndexS, endIndexS);
      }

      if (endIndexP - beginIndexP > endIndexS - beginIndexS) {
        for (int j = beginIndexP; j <= endIndexP; j++) {
          slowness.add(slownessModelP.get(j).slow);
        }
      } else {
        for (int j = beginIndexS; j <= endIndexS; j++) {
          slowness.add(slownessModelS.get(j).slow);
        }
      }

      beginIndexP = ++endIndexP;
      beginIndexS = ++endIndexS;
    }
  }

  /**
   * Function to put the slowness into the depth model. The merged slownesses are created in tau
   * model, but are needed in depth model as well.
   *
   * @param slowness An ArrayList of Double values containing the merged list of non-dimensional
   *     slownesses
   */
  public void putSlowness(ArrayList<Double> slowness) {
    this.slowness = slowness;
  }

  /**
   * Function to create a set of depth shells. This time, the shell indices are into the merged
   * slownesses, which will be the basis for the tau and range integrals. Note that these shells
   * will directly drive the construction and naming of the final phases.
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   */
  public void makeDepthShells(char modelType) {
    ModelShell lastShell = null;

    if (modelType == 'P') {
      shellModelP = new ArrayList<ModelShell>();
      int iEnd = slownessModelP.size() - 1;

      for (int i = 0; i < referenceModel.getShells().size(); i++) {
        ModelShell refShell = referenceModel.getShells().get(i);
        double slowTop = referenceModel.getSlowness(modelType, refShell.getTopSampleIndex());
        int iBeg = iEnd;

        for (iEnd = iBeg; iEnd >= 0; iEnd--) {
          if (slownessModelP.get(iEnd).slow == slowTop) {
            break;
          }
        }

        if (TablesUtil.deBugLevel > 1) {
          System.out.format(
              "MakeDepShells: " + "%c %3d %3d %8.6f\n", modelType, iBeg, iEnd, slowTop);
        }
        ModelShell newShell = new ModelShell(refShell, slownessModelP.get(iBeg).index);
        newShell.addTop(slownessModelP.get(iEnd).index, refShell.getTopSampleRadius());

        if (slowTop > referenceModel.getSlowness(modelType, refShell.getBottomSampleIndex())) {
          if (lastShell != null) {
            if (!lastShell.getTempPCode().equals(newShell.getTempPCode())) {
              // Make sure we have continuity.
              lastShell.setTopSampleIndex(newShell.getBottomSampleIndex());
              shellModelP.add(newShell);
              lastShell = newShell;
            } else {
              // Merge the two shells unless the last shell is an LVZ.
              if (lastShell.getBottomSampleIndex() > newShell.getTopSampleIndex()) {
                lastShell.setTopSampleIndex(newShell.getTopSampleIndex());
                lastShell.setTopSampleRadius(newShell.getTopSampleRadius());
              } else {
                shellModelP.add(newShell);
                lastShell = newShell;
              }
            }
          } else {
            shellModelP.add(newShell);
            lastShell = newShell;
          }
        }
      }

      fixLvzShells('P', shellModelP);
    } else {
      shellModelS = new ArrayList<ModelShell>();
      int iEnd = slownessModelS.size() - 1;

      for (int i = 0; i < referenceModel.getShells().size(); i++) {
        ModelShell refShell = referenceModel.getShells().get(i);
        double slowTop = referenceModel.getSlowness(modelType, refShell.getTopSampleIndex());
        int iBeg = iEnd;

        for (iEnd = iBeg; iEnd >= 0; iEnd--) {
          if (slownessModelS.get(iEnd).slow == slowTop) {
            break;
          }
        }
        if (TablesUtil.deBugLevel > 1) {
          System.out.format(
              "MakeDepShells: " + "%c %3d %3d %8.6f\n", modelType, iBeg, iEnd, slowTop);
        }

        ModelShell newShell = new ModelShell(refShell, slownessModelS.get(iBeg).index);
        newShell.addTop(slownessModelS.get(iEnd).index, refShell.getTopSampleRadius());

        if (slowTop > referenceModel.getSlowness(modelType, refShell.getBottomSampleIndex())) {
          if (lastShell != null) {
            if (!lastShell.getTempSCode().equals(newShell.getTempSCode())) {
              // Make sure we have continuity.
              lastShell.setTopSampleIndex(newShell.getBottomSampleIndex());
              shellModelS.add(newShell);
              lastShell = newShell;
            } else {
              // Merge the two shells unless the last shell is an LVZ.
              if (lastShell.getBottomSampleIndex() > newShell.getTopSampleIndex()) {
                lastShell.setTopSampleIndex(newShell.getTopSampleIndex());
                lastShell.setTopSampleRadius(newShell.getTopSampleRadius());
              } else {
                shellModelS.add(newShell);
                lastShell = newShell;
              }
            }
          } else {
            shellModelS.add(newShell);
            lastShell = newShell;
          }
        }
      }

      fixLvzShells('S', shellModelS);
    }
  }

  /**
   * Function to filter out shells that are entirely inside a high slowness zone (low velocity
   * zone).
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   * @param shells An ArrayList of ModelShell objects containing the model shells to filter
   */
  private void fixLvzShells(char modelType, ArrayList<ModelShell> shells) {
    int n = shells.get(shells.size() - 1).getBottomSampleIndex();

    for (int j = shells.size() - 2; j >= 0; j--) {
      if (shells.get(j).getBottomSampleIndex() < n) {
        shells.remove(j);
      } else {
        shells.get(j).setTopSampleIndex(n);
        n = shells.get(j).getBottomSampleIndex();
      }
    }
  }

  /**
   * Function to put the shells into the final model. The final shells are created in depth model,
   * but are needed in final model as well.
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   * @param shells An ArrayList of ModelShell objects containing the model shells to put
   */
  public void putShells(char modelType, ArrayList<ModelShell> shells) {
    if (modelType == 'P') {
      shellModelP = shells;
    } else {
      shellModelS = shells;
    }
  }

  /**
   * Function to get a depth model shell by index.
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   * @param index An integer containing the desired depth model shell index
   * @return A ModelShell object holding the depth model shell
   */
  public ModelShell getShell(char modelType, int index) {
    if (modelType == 'P') {
      return shellModelP.get(index);
    } else {
      return shellModelS.get(index);
    }
  }

  /**
   * Function to get the last depth model shell (i.e., the one starting at the surface).
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   * @return A ModelShell object holding the depth model shell
   */
  public ModelShell getLastShell(char modelType) {
    if (modelType == 'P') {
      return shellModelP.get(shellModelP.size() - 1);
    } else {
      return shellModelS.get(shellModelS.size() - 1);
    }
  }

  /**
   * Function to get a depth model shell index by name. If the name is not found, return -1;
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   * @param shellName A ShellName object containing the depth model shell name
   * @return An integer holding the depth model shell index
   */
  public int getShell(char modelType, ShellName shellName) {
    if (modelType == 'P') {
      for (int j = 0; j < shellModelP.size(); j++) {
        if (shellModelP.get(j).getName().equals(shellName.name())) {
          return j;
        }
      }
    } else {
      for (int j = 0; j < shellModelS.size(); j++) {
        if (shellModelS.get(j).getName().equals(shellName.name())) {
          return j;
        }
      }
    }

    return -1;
  }

  /**
   * Function to get a depth model shell index by name. If the name is not found return -1;
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   * @param shellName A ShellName object containing the depth model shell name
   * @return An integer holding the depth model shell index
   */
  public int getShellIndex(char modelType, ShellName shellName) {
    // First try some special names needed to make branches.
    if (shellName == ShellName.SURFACE) {
      return shellSize(modelType) - 1;
    } else if (shellName == ShellName.MANTLE_BOTTOM) {
      int index = getShell(modelType, ShellName.CORE_MANTLE_BOUNDARY);

      if (index < 0) {
        index = getShell(modelType, ShellName.OUTER_CORE);
      }

      return index;
    } else if (shellName == ShellName.CORE_TOP) {
      return getShell(modelType, ShellName.OUTER_CORE);
    } else {
      // If that fails, just try the name.
      return getShell(modelType, shellName);
    }
  }

  /**
   * Function to get the number of shells.
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   * @return An integer containing the number of shells for the provided model type
   */
  public int shellSize(char modelType) {
    if (modelType == 'P') {
      return shellModelP.size();
    } else {
      return shellModelS.size();
    }
  }

  /** Function to make integral pieces from the raw integrals in integralsP and integralsS. */
  public void makePieces() {
    intPiecesP = new BranchIntegrals('P', this);
    intPiecesS = new BranchIntegrals('S', this);
  }

  /**
   * Function to get the integral pieces for one phase type.
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   * @return A BranchIntegrals object containing the integral pieces for the given model type
   */
  public BranchIntegrals getPiece(char modelType) {
    if (modelType == 'P') {
      return intPiecesP;
    } else {
      return intPiecesS;
    }
  }

  /**
   * Function to get the master (possibly decimated) ray parameter array. Note that the S-wave ray
   * parameters are a super set the P-wave ray parameters.
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   * @return An array of doubles containing the ray parameter array
   */
  public double[] getRayParameters(char modelType) {
    if (modelType == 'P') {
      return intPiecesP.getRayParameters();
    } else {
      return intPiecesS.getRayParameters();
    }
  }

  /**
   * Function to retrieve a ray parameter by index from the master (possibly decimated) array.
   *
   * @param index Ray parameter index
   * @return Ray parameter
   */
  public double getRayParameters(int index) {
    return intPiecesS.getRayParameters()[index];
  }

  /**
   * Function to retrieve the master decimation array.
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   * @return An array of boolean flags containining the master decimation array
   */
  public boolean[] getDecimation(char modelType) {
    if (modelType == 'P') {
      return intPiecesP.getDecimationKeep();
    } else {
      return intPiecesS.getDecimationKeep();
    }
  }

  /** Decimate the ray parameter arrays for both the P and S models. */
  public void decimateRayParameters() {
    intPiecesP.decimateRayParameters();
    intPiecesS.decimateRayParameters();
  }

  /**
   * A function to decimate the up-going tau and range arrays in parallel with decimating the master
   * ray parameter arrays in the integral pieces. Note that there are two sets of up-going branches
   * with different sampling. The up-going branches here are used to correct all the other branches
   * for source depth. The up-going branch sampling that will be used to actually generate the
   * up-going branch travel times was done with the proxy sampling. These branches are stubs in the
   * sense that they have a sampling, but tau and range are zero as is appropriate for a surface
   * focus source.
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   */
  public void decimateTauRange(char modelType) {
    double[] decimateTau = null;
    double[] decimateRange = null;
    boolean[] keep = getDecimation(modelType);

    // Loop over the up-going branches.
    for (int i = 0; i < numIntegrals(modelType) - 3; i++) {
      // Some of the integrals don't exist.
      double[] tau = getTauIntegrals(modelType, i);

      if (tau != null) {
        double[] x = getRangeIntegrals(modelType, i);

        // Allocate temporary arrays on the shallowest integrals
        // because they are the longest.
        if (decimateTau == null) {
          decimateTau = new double[tau.length];
          decimateRange = new double[x.length];
        }

        // Do the decimation.
        int k = 0;
        for (int j = 0; j < tau.length; j++) {
          if (keep[j]) {
            decimateTau[k] = tau[j];
            decimateRange[k++] = x[j];
          }
        }

        // Update the integral arrays with the decimated versions.
        update(modelType, i, k, decimateTau, decimateRange);
      }
    }
  }

  /**
   * Function to set the low velocity zone (really high slowness zone) flag.
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   */
  public void setLowVelocityZone(char modelType) {
    if (modelType == 'P') {
      integralsP.get(integralsP.size() - 1).lvz = true;
    } else {
      integralsS.get(integralsS.size() - 1).lvz = true;
    }
  }

  /**
   * Function to get the low velocity zone (really high slowness zone) flag. Note that this is done
   * by slowness rather than index because the final model is so fragmentary. It is assumed that
   * there are no low velocity zones where the final model is missing samples (i.e., the lower
   * mantle). The core-mantle boundary is, of course, a special case.
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   * @param modelIndex An integer containing the model index
   * @return A boolean flag, true if this level corresponds to a low velocity zone
   */
  public boolean getLowVelocityZone(char modelType, int modelIndex) {
    if (modelType == 'P') {
      if (integralsP.get(modelIndex) != null) {
        return integralsP.get(modelIndex).lvz;
      } else {
        return false;
      }
    } else {
      if (integralsS.get(modelIndex) != null) {
        return integralsS.get(modelIndex).lvz;
      } else {
        return false;
      }
    }
  }

  /**
   * Function to get one of the integral sets by index.
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   * @param depthIndex An integer containing the depth index
   * @return An array of doubles containig the tau integrals for all ray parameters
   */
  public double[] getTauIntegrals(char modelType, int depthIndex) {
    if (modelType == 'P') {
      if (integralsP.get(depthIndex) != null) {
        return integralsP.get(depthIndex).tau;
      } else {
        return null;
      }
    } else {
      if (integralsS.get(depthIndex) != null) {
        return integralsS.get(depthIndex).tau;
      } else {
        return null;
      }
    }
  }

  /**
   * Function to get one of the special integral sets by name.
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   * @param name A ShellName object containig the integral set name
   * @return An array of doubles containig the tau integrals for all ray parameters
   */
  public double[] getTauIntegrals(char modelType, ShellName name) {
    int n;

    if (modelType == 'P') {
      n = integralsP.size();
      for (int j = n - 3; j < n; j++) {
        if (name == integralsP.get(j).name) {
          return integralsP.get(j).tau;
        }
      }
    } else {
      n = integralsS.size();
      for (int j = n - 3; j < n; j++) {
        if (name == integralsS.get(j).name) {
          return integralsS.get(j).tau;
        }
      }
    }
    return null;
  }

  /**
   * Function to get one of the range integral sets by index.
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   * @param depthIndex An integer containing the depth index
   * @return An array of doubles containig the tau integrals for all ray parameters
   */
  public double[] getRangeIntegrals(char modelType, int depthIndex) {
    if (modelType == 'P') {
      if (integralsP.get(depthIndex) != null) {
        return integralsP.get(depthIndex).x;
      } else {
        return null;
      }
    } else {
      if (integralsS.get(depthIndex) != null) {
        return integralsS.get(depthIndex).x;
      } else {
        return null;
      }
    }
  }

  /**
   * Function to get one of the range integral sets by name.
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   * @param name A ShellName object containig the integral set name
   * @return An array of doubles containig the tau integrals for all ray parameters
   */
  public double[] getRangeIntegrals(char modelType, ShellName name) {
    if (modelType == 'P') {
      int n = integralsP.size();

      for (int j = n - 3; j < n; j++) {
        if (name == integralsP.get(j).name) {
          return integralsP.get(j).x;
        }
      }
    } else {
      int n = integralsS.size();

      for (int j = n - 3; j < n; j++) {
        if (name == integralsS.get(j).name) {
          return integralsS.get(j).x;
        }
      }
    }

    return null;
  }

  /**
   * Get a major shell tau value by index. Note that because these values are drawn from the pieces,
   * the integrals are specific to a region rather than cumulative as for getTauIntegrals.
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   * @param index An integer contaiing the tau index
   * @param shellIndex An integer containin the major shell index (0 = mantle, 1 = outer core, 2 =
   *     inner core)
   * @return A double ctonaining the tau value
   */
  public double getSpecialTauIntegrals(char modelType, int index, int shellIndex) {
    switch (shellIndex) {
      case 0:
        if (modelType == 'P') {
          return intPiecesP.getMantleTauIntegrals()[index];
        } else {
          return intPiecesS.getMantleTauIntegrals()[index];
        }
      case 1:
        if (modelType == 'P') {
          return intPiecesP.getOuterCoreTauIntegrals()[index];
        } else {
          return intPiecesS.getOuterCoreTauIntegrals()[index];
        }
      case 2:
        if (modelType == 'P') {
          return intPiecesP.getInnerCoreTauIntegrals()[index];
        } else {
          return intPiecesS.getInnerCoreTauIntegrals()[index];
        }
      default:
        return Double.NaN;
    }
  }

  /**
   * Function to Get a major shell range value by index. Note that because these values are drawn
   * from the pieces, the integrals are specific to a region rather than cumulative as for
   * getTauIntegrals.
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   * @param index An integer holding the Range index
   * @param shellIndex An integer containin the major shell index (0 = mantle, 1 = outer core, 2 =
   *     inner core)
   * @return A double containign the range value
   */
  public double getSpecialRangeIntegrals(char modelType, int index, int shellIndex) {
    switch (shellIndex) {
      case 0:
        if (modelType == 'P') {
          return intPiecesP.getMantleRangeIntegrals()[index];
        } else {
          return intPiecesS.getMantleRangeIntegrals()[index];
        }
      case 1:
        if (modelType == 'P') {
          return intPiecesP.getOuterCoreRangeIntegrals()[index];
        } else {
          return intPiecesS.getOuterCoreRangeIntegrals()[index];
        }
      case 2:
        if (modelType == 'P') {
          return intPiecesP.getInnerCoreRangeIntegrals()[index];
        } else {
          return intPiecesS.getInnerCoreRangeIntegrals()[index];
        }
      default:
        return Double.NaN;
    }
  }

  /**
   * Function to update tau and range arrays with their decimated versions.
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   * @param index An integer containing the integral index
   * @param length An integer containing the length of the decimated arrays
   * @param newTau An array of doubles containing the decimated tau array
   * @param newRange An array of doubles containing the decimated range array
   */
  public void update(char modelType, int index, int length, double[] newTau, double[] newRange) {
    if (modelType == 'P') {
      integralsP.get(index).update(length, newTau, newRange);
    } else {
      integralsS.get(index).update(length, newTau, newRange);
    }
  }

  /**
   * Function to get the radial sampling interval associated with a shell by index.
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   * @param index An integer containing the shell index
   * @return A double containing the non-dimensional radial sampling
   */
  public double getRangeIncrementTarget(char modelType, int index) {
    if (modelType == 'P') {
      return modelConversions.normR(shellModelP.get(index).getRangeIncrementTarget());
    } else {
      return modelConversions.normR(shellModelS.get(index).getRangeIncrementTarget());
    }
  }

  /**
   * Function to retrieve the radial sampling interval associated with the shell above the current
   * one.
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   * @param index An integer containing the shell index
   * @return A double containing the non-dimensional radial sampling
   */
  public double getNextRangeIncrementTarget(char modelType, int index) {
    if (modelType == 'P') {
      for (int j = index + 1; j < shellModelP.size(); j++) {
        if (!shellModelP.get(j).getIsDiscontinuity()) {
          return modelConversions.normR(shellModelP.get(j).getRangeIncrementTarget());
        }
      }
    } else {
      for (int j = index + 1; j < shellModelS.size(); j++) {
        if (!shellModelS.get(j).getIsDiscontinuity()) {
          return modelConversions.normR(shellModelS.get(j).getRangeIncrementTarget());
        }
      }
    }

    return Double.NaN;
  }

  /**
   * Get the size of the integral lists.
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   * @return The size of the integral list
   */
  public int numIntegrals(char modelType) {
    if (modelType == 'P') {
      return integralsP.size();
    } else {
      return integralsS.size();
    }
  }

  /**
   * Function to get the size of the integrals list, counting only the non-null, upper mantle
   * integrals. These are the integrals used for depth correction.
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   * @return An integer containing the non-null number of upper mantle integrals
   */
  public int numUpperMantleInts(char modelType) {
    int size = 0;

    if (modelType == 'P') {
      for (int j = 0; j < integralsP.size() - 3; j++) {
        if (integralsP.get(j) != null) {
          size++;
        }
      }
    } else {
      for (int j = 0; j < integralsS.size() - 3; j++) {
        if (integralsS.get(j) != null) {
          size++;
        }
      }
    }

    return size;
  }

  /**
   * Function to copy the list of ray parameters associated with branch ends to an internal array.
   *
   * @param ends A treeset of Double objects contaiing the list of non-dimensional ray parameters
   *     associated with branch ends
   */
  public void setEnds(TreeSet<Double> ends) {
    int j = 0;

    rayParamBranchEnds = new double[ends.size()];
    Iterator<Double> iter = ends.iterator();

    while (iter.hasNext()) {
      rayParamBranchEnds[j++] = iter.next();
    }
  }

  /**
   * Get the ray travel distances (ranges) at the branch ends.
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   * @param index Depth index
   * @return Array of branch end ranges
   */
  public ArrayList<Double> getXUp(char modelType, int index) {
    int i = 0;
    double[] p, x;
    ArrayList<Double> xUp;

    xUp = new ArrayList<Double>();
    p = getRayParameters(modelType);
    if (modelType == 'P') {
      if (integralsP.get(index) != null) {
        x = integralsP.get(index).x;
        for (int j = 0; j < x.length; j++) {
          if (rayParamBranchEnds[i] == p[j]) {
            xUp.add(x[j]);
            i++;
          }
        }
      } else {
        return null;
      }
    } else {
      if (integralsS.get(index) != null) {
        x = integralsS.get(index).x;
        for (int j = 0; j < x.length; j++) {
          if (rayParamBranchEnds[i] == p[j]) {
            xUp.add(x[j]);
            i++;
          }
        }
      } else {
        return null;
      }
    }
    return xUp;
  }

  /**
   * Function to print out the slowness model.
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   * @param version A string containing the model version ("Tau", "Depth", or "Final")
   */
  public void printModel(char modelType, String version) {
    if (version.equals("Depth")) {
      System.out.println("\n     Depth model for " + modelType + " slowness");
      System.out.println("      R     slowness      Z");
    } else if (version.equals("Final")) {
      System.out.println("\n     Final model for " + modelType + " slowness");
      System.out.println("      R     slowness      Z       length");
    } else {
      System.out.println("\n   Tau model for " + modelType + " slowness");
      System.out.println("      R     slowness    X");
    }

    if (modelType == 'P') {
      if (!version.equals("Final")) {
        for (int j = 0; j < slownessModelP.size(); j++) {
          System.out.format("%3d %s\n", j, slownessModelP.get(j));
        }
      } else {
        for (int j = 0; j < slownessModelP.size(); j++) {
          if (integralsP.get(j) != null) {
            System.out.format(
                "%3d %s %3d %s\n",
                j, slownessModelP.get(j), integralsP.get(j).tau.length, integralsP.get(j).name);
          } else {
            System.out.format("%3d %s null\n", j, slownessModelP.get(j));
          }
        }
      }
    } else {
      if (!version.equals("Final")) {
        for (int j = 0; j < slownessModelS.size(); j++) {
          System.out.format("%3d %s\n", j, slownessModelS.get(j));
        }
      } else {
        for (int j = 0; j < slownessModelS.size(); j++) {
          if (integralsS.get(j) != null) {
            System.out.format(
                "%3d %s %3d %s\n",
                j, slownessModelS.get(j), integralsS.get(j).tau.length, integralsS.get(j).name);
          } else {
            System.out.format("%3d %s null\n", j, slownessModelS.get(j));
          }
        }
      }
    }
  }

  /**
   * Print out the slowness model.
   *
   * @param version A string containing the model version ("Tau", "Depth", or "Final")
   */
  public void printModel(String version) {
    if (version.equals("Depth")) {
      System.out.println("\n     Depth model");
      System.out.println("      R     slowness      Z     slowness" + "      Z");
    } else if (version.equals("Final")) {
      System.out.println("\n     Final model");
      System.out.println(
          "      R     slowness      Z       length" + "     slowness      Z       length");
    } else {
      System.out.println("\n   Tau model");
      System.out.println("      R     slowness    X     slowness    X");
    }

    if (!version.equals("Final")) {
      for (int j = 0; j < slownessModelP.size(); j++) {
        System.out.format("%3d %s %s\n", j, slownessModelP.get(j), slownessModelS.get(j));
      }
      if (!version.equals("Depth")) {
        for (int j = slownessModelP.size(); j < slownessModelS.size(); j++) {
          System.out.format(
              "%3d                           %s\n",
              j, slownessModelP.get(j), slownessModelS.get(j));
        }
      } else {
        for (int j = slownessModelP.size(); j < slownessModelS.size(); j++) {
          System.out.format(
              "%3d                               %s\n",
              j, slownessModelP.get(j), slownessModelS.get(j));
        }
      }
    } else {
      for (int j = 0; j < slownessModelP.size(); j++) {
        if (integralsP.get(j) != null) {
          if (integralsS.get(j) != null) {
            System.out.format(
                "%3d %s  %3d %s  %3d\n",
                j,
                slownessModelP.get(j),
                integralsP.get(j).tau.length,
                slownessModelS.get(j),
                integralsS.get(j).tau.length);
          } else {
            System.out.format(
                "%3d %s  %3d %s null\n",
                j, slownessModelP.get(j), integralsP.get(j).tau.length, slownessModelS.get(j));
          }
        } else {
          if (integralsS.get(j) != null) {
            System.out.format(
                "%3d %s null %s  %3d\n",
                j, slownessModelP.get(j), slownessModelS.get(j), integralsS.get(j).tau.length);
          } else {
            System.out.format(
                "%3d %s null %s null\n", j, slownessModelP.get(j), slownessModelS.get(j));
          }
        }
      }

      for (int j = slownessModelP.size(); j < slownessModelS.size(); j++) {
        if (integralsS.get(j) != null) {
          System.out.format(
              "%3d                                    %s  %3d\n",
              j, slownessModelS.get(j), integralsS.get(j).tau.length);
        } else {
          System.out.format(
              "%3d                                    %s null\n", j, slownessModelS.get(j));
        }
      }
    }
  }

  /** Function to print out the merged slownesses. */
  public void printMergedSlownesses() {
    System.out.println("\n\tMerged slownesses");

    for (int j = 0; j < slowness.size(); j++) {
      System.out.format("\t%3d %8.6f\n", j, slowness.get(j));
    }
  }

  /**
   * Function to print the wave type specific shells for the depth model.
   *
   * @param waveType Wave type (P = compressional, S = shear)
   */
  public void printDepthShells(char waveType) {
    System.out.println("\n\t" + waveType + " Model Shells:");
    if (waveType == 'P') {
      for (int j = 0; j < shellModelP.size(); j++) {
        String shellString = shellModelP.get(j).printShell(waveType);

        if (shellString != null) {
          System.out.format("%3d   %s\n", j, shellString);
        }
      }
    } else {
      for (int j = 0; j < shellModelS.size(); j++) {
        String shellString = shellModelS.get(j).printShell(waveType);

        if (shellString != null) {
          System.out.format("%3d   %s\n", j, shellString);
        }
      }
    }
  }

  /**
   * Function to get the last entry of the tau-range integrals for the desired phase type as a
   * string.
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   * @return A String summarizing the last tau-range integral sample
   */
  public String stringLastIntegral(char modelType) {
    if (modelType == 'P') {
      int n = slownessModelP.size() - 1;

      return String.format(
          "%3d %9.6f %8.6f",
          integralsP.get(n).tau.length, slownessModelP.get(n).z, slownessModelP.get(n).slow);
    } else {
      int n = slownessModelS.size() - 1;

      return String.format(
          "%3d %9.6f %8.6f",
          integralsS.get(n).tau.length, slownessModelS.get(n).z, slownessModelS.get(n).slow);
    }
  }

  /**
   * Function to print a summary of all the tau-range integrals for the desired phase type.
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   */
  public void printIntegrals(char modelType) {
    if (modelType == 'P') {
      int n = integralsP.size();

      for (int j = 0; j < n - 3; j++) {
        if (integralsP.get(j) != null) {
          System.out.format(
              "Lev1 %3d %3d %9.6f %8.6f\n",
              j, integralsP.get(j).tau.length, slownessModelP.get(j).z, slownessModelP.get(j).slow);
        }
      }

      for (int j = n - 3; j < n - 1; j++) {
        System.out.format(
            "Lev2 %3d %3d %9.6f %8.6f\n",
            j, integralsP.get(j).tau.length, slownessModelP.get(j).z, slownessModelP.get(j).slow);
      }

      System.out.format(
          "Lev3 %3d %3d %9.6f %8.6f\n",
          n - 1,
          integralsP.get(n - 1).tau.length,
          slownessModelP.get(n - 1).z,
          slownessModelP.get(n - 1).slow);
    } else {
      int n = integralsS.size();

      for (int j = 0; j < n - 3; j++) {
        if (integralsS.get(j) != null) {
          System.out.format(
              "Lev1 %3d %3d %9.6f %8.6f\n",
              j, integralsS.get(j).tau.length, slownessModelS.get(j).z, slownessModelS.get(j).slow);
        }
      }

      for (int j = n - 3; j < n - 1; j++) {
        System.out.format(
            "Lev2 %3d %3d %9.6f %8.6f\n",
            j, integralsS.get(j).tau.length, slownessModelS.get(j).z, slownessModelS.get(j).slow);
      }

      System.out.format(
          "Lev3 %3d %3d %9.6f %8.6f\n",
          n - 1,
          integralsS.get(n - 1).tau.length,
          slownessModelS.get(n - 1).z,
          slownessModelS.get(n - 1).slow);
    }
  }

  /**
   * Function to print out the shell integrals. Note that these are the raw cumulative integrals.
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   */
  public void printShellIntegrals(char modelType) {
    System.out.format("\n\t\tShell Integrals for %c-waves\n", modelType);
    System.out.println("                        Tau                    " + "   X");
    System.out.println("        p     Mantle     OC       IC     Mantle" + "   OC     IC");

    if (modelType == 'P') {
      int n = integralsP.size();
      TauXsample mantle = integralsP.get(n - 3);
      TauXsample outerCore = integralsP.get(n - 2);
      TauXsample innerCore = integralsP.get(n - 1);

      for (int j = 0, k = slowness.size() - 1; j < mantle.tau.length; j++, k--) {
        System.out.format(
            "%3d %8.6f %8.6f %8.6f %8.6f %6.2f %6.2f %6.2f\n",
            j,
            slowness.get(k),
            mantle.tau[j],
            outerCore.tau[j],
            innerCore.tau[j],
            Math.toDegrees(mantle.x[j]),
            Math.toDegrees(outerCore.x[j]),
            Math.toDegrees(innerCore.x[j]));
      }
    } else {
      int n = integralsS.size();
      TauXsample mantle = integralsS.get(n - 3);
      TauXsample outerCore = integralsS.get(n - 2);
      TauXsample innerCore = integralsS.get(n - 1);
      n = mantle.tau.length - 1;

      for (int j = n, k = slowness.size() - 1; j >= 0; j--, k--) {
        System.out.format(
            "%3d %8.6f %8.6f %8.6f %8.6f %6.2f %6.2f %6.2f\n",
            n - j,
            slowness.get(k),
            mantle.tau[j],
            outerCore.tau[j],
            innerCore.tau[j],
            Math.toDegrees(mantle.x[j]),
            Math.toDegrees(outerCore.x[j]),
            Math.toDegrees(innerCore.x[j]));
      }
    }
  }

  /** Function to print out the proxy ranges. */
  public void printProxyRanges() {
    System.out.println("\n\t\t\tProxy Ranges");
    System.out.println("                  P                            S");
    System.out.println("    slowness      X       delX   slowness" + "      X       delX");

    int nP = intPiecesP.getProxyRanges().length;
    int nS = intPiecesS.getProxyRanges().length;

    System.out.format(
        "%3d %8.6f %8.2f            %8.6f %8.2f\n",
        0,
        intPiecesP.getProxyRayParameters()[0],
        modelConversions.dimR(intPiecesP.getProxyRanges()[0]),
        intPiecesS.getProxyRayParameters()[0],
        modelConversions.dimR(intPiecesS.getProxyRanges()[0]));

    if (nS >= nP) {
      for (int j = 1; j < nP; j++) {
        System.out.format(
            "%3d %8.6f %8.2f %8.2f   %8.6f %8.2f %8.2f\n",
            j,
            intPiecesP.getProxyRayParameters()[j],
            modelConversions.dimR(intPiecesP.getProxyRanges()[j]),
            modelConversions.dimR(
                intPiecesP.getProxyRanges()[j] - intPiecesP.getProxyRanges()[j - 1]),
            intPiecesS.getProxyRayParameters()[j],
            modelConversions.dimR(intPiecesS.getProxyRanges()[j]),
            modelConversions.dimR(
                intPiecesS.getProxyRanges()[j] - intPiecesS.getProxyRanges()[j - 1]));
      }

      for (int j = nP; j < nS; j++) {
        System.out.format(
            "%3d                              " + "%8.6f %8.2f %8.2f\n",
            j,
            intPiecesS.getProxyRayParameters()[j],
            modelConversions.dimR(intPiecesS.getProxyRanges()[j]),
            modelConversions.dimR(
                intPiecesS.getProxyRanges()[j] - intPiecesS.getProxyRanges()[j - 1]));
      }
    } else {
      for (int j = 1; j < nS; j++) {
        System.out.format(
            "%3d %8.6f %8.2f %8.2f   %8.6f %8.2f %8.2f\n",
            j,
            intPiecesP.getProxyRayParameters()[j],
            modelConversions.dimR(intPiecesP.getProxyRanges()[j]),
            modelConversions.dimR(
                intPiecesP.getProxyRanges()[j] - intPiecesP.getProxyRanges()[j - 1]),
            intPiecesS.getProxyRayParameters()[j],
            modelConversions.dimR(intPiecesS.getProxyRanges()[j]),
            modelConversions.dimR(
                intPiecesS.getProxyRanges()[j] - intPiecesS.getProxyRanges()[j - 1]));
      }

      for (int j = nS; j < nP; j++) {
        System.out.format(
            "%3d %8.6f %8.2f %8.2f\n",
            j,
            intPiecesP.getProxyRayParameters()[j],
            modelConversions.dimR(intPiecesP.getProxyRanges()[j]),
            modelConversions.dimR(
                intPiecesP.getProxyRanges()[j] - intPiecesP.getProxyRanges()[j - 1]));
      }
    }
  }

  /**
   * Print the integrals for the whole mantle, outer core, and inner core. Note that these are the
   * partial integrals specific to a major shell of the Earth.
   *
   * @param modelType A char containing the desired model type (P = P slowness, S = S slowness)
   */
  public void printSpecialTauIntegrals(char modelType) {
    if (modelType == 'P') {
      intPiecesP.printShellIntegrals();
    } else {
      intPiecesS.printShellIntegrals();
    }
  }

  /** Function to print the ray parameter arrays for both the P and S branches. */
  public void printRayParameters() {
    System.out.println("\nMaster Ray Parameters");
    System.out.println("       P        S");

    for (int j = 0; j < intPiecesP.getRayParameters().length; j++) {
      System.out.format(
          "%3d %8.6f %8.6f\n",
          j, intPiecesP.getRayParameters()[j], intPiecesS.getRayParameters()[j]);
    }

    for (int j = intPiecesP.getRayParameters().length;
        j < intPiecesS.getRayParameters().length;
        j++) {
      System.out.format("%3d          %8.6f\n", j, intPiecesS.getRayParameters()[j]);
    }
  }
}
