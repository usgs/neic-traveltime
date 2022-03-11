package gov.usgs.traveltime.tables;

import gov.usgs.traveltime.ModConvert;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * An alternative version of the earth model with sampling suitable for the tau-p travel-time
 * calculation. Unlike the Earth models, the tau models are in depth order (i.e., the first point is
 * at the free surface). This class is used multiple times for various views of the model. The final
 * model also includes the tau and range integrals.
 *
 * @author Ray Buland
 */
public class TauModel {
  double[] pEnds;
  EarthModel refModel;
  ArrayList<TauSample> pModel, sModel;
  ArrayList<Double> slowness;
  ArrayList<ModelShell> pShells = null, sShells = null;
  ArrayList<TauXsample> pInts = null, sInts = null;
  BranchIntegrals pPieces, sPieces;
  ModConvert convert;

  /**
   * Get the model dependant conversions.
   *
   * @return A ModConvert object holding the model dependant conversions
   */
  public ModConvert getModelConversions() {
    return convert;
  }

  /**
   * Allocate lists for independent P and S models.
   *
   * @param refModel Reference Earth model
   * @param convert Model dependent conversions
   */
  public TauModel(EarthModel refModel, ModConvert convert) {
    this.refModel = refModel;
    this.convert = convert;
    pModel = new ArrayList<TauSample>();
    sModel = new ArrayList<TauSample>();
  }

  /** Only the final model actually has integrals, so initialize them separately. */
  public void initIntegrals() {
    pInts = new ArrayList<TauXsample>();
    sInts = new ArrayList<TauXsample>();
  }

  /**
   * Add a sample to the model.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @param r Dimensional Earth radius in kilometers
   * @param slow Non-dimensional slowness
   * @param x Non-dimensional ray travel distance (range)
   */
  public void add(char type, double r, double slow, double x) {
    if (type == 'P') {
      pModel.add(new TauSample(r, slow, x));
    } else {
      sModel.add(new TauSample(r, slow, x));
    }
  }

  /**
   * Add a sample to the model.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @param r Dimensional Earth radius in kilometers
   * @param slow Non-dimensional slowness
   * @param index Index into the merged slownesses
   */
  public void add(char type, double r, double slow, int index) {
    if (type == 'P') {
      pModel.add(new TauSample(r, slow, index, convert));
    } else {
      sModel.add(new TauSample(r, slow, index, convert));
    }
  }

  /**
   * Add a sample to the model.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @param sample Complete sample created externally
   */
  public void add(char type, TauSample sample) {
    if (type == 'P') {
      pModel.add(new TauSample(sample));
    } else {
      sModel.add(new TauSample(sample));
    }
  }

  /**
   * Add a sample with an index to the model.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @param sample Complete sample created externally
   * @param index Index
   */
  public void add(char type, TauSample sample, int index) {
    if (type == 'P') {
      pModel.add(new TauSample(sample, index));
    } else {
      sModel.add(new TauSample(sample, index));
    }
  }

  /**
   * Add a sample with an index to the model and at the same time store tau and range integrals for
   * all ray parameters down to this bottoming depth for the specified phase type.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @param sample Complete sample created externally
   * @param index Index
   * @param tauX Set of tau and range integrals
   */
  public void add(char type, TauSample sample, int index, TauXsample tauX) {
    if (type == 'P') {
      for (int j = pInts.size(); j < pModel.size(); j++) {
        pInts.add(null);
      }
      pModel.add(new TauSample(sample, index));
      pInts.add(tauX);
    } else {
      for (int j = sInts.size(); j < sModel.size(); j++) {
        sInts.add(null);
      }
      sModel.add(new TauSample(sample, index));
      sInts.add(tauX);
    }
  }

  /**
   * Get the Earth model name.
   *
   * @return The Earth model name
   */
  public String getModelName() {
    return refModel.getEarthModelName();
  }

  /**
   * Get the model dependent conversions.
   *
   * @return A pointer to the model dependent conversion class
   */
  public ModConvert getConvert() {
    return convert;
  }

  /**
   * Get a tau model sample.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @param index Model index
   * @return Model sample
   */
  public TauSample getSample(char type, int index) {
    if (type == 'P') {
      return pModel.get(index);
    } else {
      return sModel.get(index);
    }
  }

  /**
   * Get the last model sample added.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @return Last model sample
   */
  public TauSample getLast(char type) {
    if (type == 'P') {
      return pModel.get(pModel.size() - 1);
    } else {
      return sModel.get(sModel.size() - 1);
    }
  }

  /**
   * Get the index of the model sample with the specified slowness.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @param slow Non-dimensional slowness
   * @return The model index associated with slow
   */
  public int getIndex(char type, double slow) {
    if (type == 'P') {
      for (int j = 0; j < pModel.size(); j++) {
        if (pModel.get(j).slow == slow) {
          return j;
        }
      }
      return -1;
    } else {
      for (int j = 0; j < sModel.size(); j++) {
        if (sModel.get(j).slow == slow) {
          return j;
        }
      }
      return -1;
    }
  }

  /**
   * Replace the last sample in the model.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @param sample New model sample
   */
  public void setLast(char type, TauSample sample) {
    if (type == 'P') {
      pModel.set(pModel.size() - 1, new TauSample(sample));
    } else {
      sModel.set(sModel.size() - 1, new TauSample(sample));
    }
  }

  /**
   * Get the number of model samples.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @return Number of model samples
   */
  public int size(char type) {
    if (type == 'P') {
      return pModel.size();
    } else {
      return sModel.size();
    }
  }

  /**
   * Merge the P- and S-wave slownesses into a single list. To avoid making the spacing very
   * non-uniform, the merge is done between critical slownesses. For each interval, the sampling
   * that seems most complete (judging by the number of samples) is used.
   *
   * @param locModel Internal Earth model
   */
  public void merge(EarthModel locModel) {
    int pBeg = 1, pEnd, sBeg = 0, sEnd;
    ArrayList<CriticalSlowness> critical;
    CriticalSlowness crit0, crit1;

    critical = locModel.getCriticalSlownesses();
    slowness = new ArrayList<Double>();
    crit1 = critical.get(critical.size() - 1);
    for (int iCrit = critical.size() - 2; iCrit >= 0; iCrit--) {
      crit0 = crit1;
      crit1 = critical.get(iCrit);
      if (TablesUtil.deBugLevel > 1)
        System.out.format(
            "\tInterval: " + "%8.6f %8.6f\n", crit0.getSlowness(), crit1.getSlowness());
      if (crit0.getSlowness() <= pModel.get(pBeg - 1).slow) {
        for (pEnd = pBeg; pEnd < pModel.size(); pEnd++) {
          if (crit1.getSlowness() == pModel.get(pEnd).slow) break;
        }
      } else {
        pEnd = 0;
      }
      for (sEnd = sBeg; sEnd < sModel.size(); sEnd++) {
        if (crit1.getSlowness() == sModel.get(sEnd).slow) break;
      }

      if (TablesUtil.deBugLevel > 1)
        System.out.format("\tIndices: " + "P: %d %d S: %d %d\n", pBeg, pEnd, sBeg, sEnd);
      if (pEnd - pBeg > sEnd - sBeg) {
        for (int j = pBeg; j <= pEnd; j++) {
          slowness.add(pModel.get(j).slow);
        }
      } else {
        for (int j = sBeg; j <= sEnd; j++) {
          slowness.add(sModel.get(j).slow);
        }
      }
      pBeg = ++pEnd;
      sBeg = ++sEnd;
    }
  }

  /**
   * The merged slownesses are created in tau model, but are needed in depth model as well.
   *
   * @param slowness Merged list of non-dimensional slownesses
   */
  public void putSlowness(ArrayList<Double> slowness) {
    this.slowness = slowness;
  }

  /**
   * Make yet another set of shells. This time, the shell indices are into the merged slownesses,
   * which will be the basis for the tau and range integrals. Note that these shells will directly
   * drive the construction and naming of the final phases.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   */
  public void makeDepShells(char type) {
    int iBeg, iEnd;
    double slowTop;
    ModelShell refShell, newShell, lastShell = null;

    if (type == 'P') {
      pShells = new ArrayList<ModelShell>();
      iEnd = pModel.size() - 1;
      for (int i = 0; i < refModel.getShells().size(); i++) {
        refShell = refModel.getShells().get(i);
        slowTop = refModel.getSlowness(type, refShell.getTopSampleIndex());
        iBeg = iEnd;
        for (iEnd = iBeg; iEnd >= 0; iEnd--) {
          if (pModel.get(iEnd).slow == slowTop) break;
        }
        if (TablesUtil.deBugLevel > 1)
          System.out.format("MakeDepShells: " + "%c %3d %3d %8.6f\n", type, iBeg, iEnd, slowTop);
        newShell = new ModelShell(refShell, pModel.get(iBeg).index);
        newShell.addTop(pModel.get(iEnd).index, refShell.getTopSampleRadius());
        if (slowTop > refModel.getSlowness(type, refShell.getBottomSampleIndex())) {
          if (lastShell != null) {
            if (!lastShell.getTempPCode().equals(newShell.getTempPCode())) {
              // Make sure we have continuity.
              lastShell.setTopSampleIndex(newShell.getBottomSampleIndex());
              pShells.add(newShell);
              lastShell = newShell;
            } else {
              // Merge the two shells unless the last shell is an LVZ.
              if (lastShell.getBottomSampleIndex() > newShell.getTopSampleIndex()) {
                lastShell.setTopSampleIndex(newShell.getTopSampleIndex());
                lastShell.setTopSampleRadius(newShell.getTopSampleRadius());
              } else {
                pShells.add(newShell);
                lastShell = newShell;
              }
            }
          } else {
            pShells.add(newShell);
            lastShell = newShell;
          }
        }
      }
      fixLvzShells('P', pShells);
    } else {
      sShells = new ArrayList<ModelShell>();
      iEnd = sModel.size() - 1;
      for (int i = 0; i < refModel.getShells().size(); i++) {
        refShell = refModel.getShells().get(i);
        slowTop = refModel.getSlowness(type, refShell.getTopSampleIndex());
        iBeg = iEnd;
        for (iEnd = iBeg; iEnd >= 0; iEnd--) {
          if (sModel.get(iEnd).slow == slowTop) break;
        }
        if (TablesUtil.deBugLevel > 1)
          System.out.format("MakeDepShells: " + "%c %3d %3d %8.6f\n", type, iBeg, iEnd, slowTop);
        newShell = new ModelShell(refShell, sModel.get(iBeg).index);
        newShell.addTop(sModel.get(iEnd).index, refShell.getTopSampleRadius());
        if (slowTop > refModel.getSlowness(type, refShell.getBottomSampleIndex())) {
          if (lastShell != null) {
            if (!lastShell.getTempSCode().equals(newShell.getTempSCode())) {
              // Make sure we have continuity.
              lastShell.setTopSampleIndex(newShell.getBottomSampleIndex());
              sShells.add(newShell);
              lastShell = newShell;
            } else {
              // Merge the two shells unless the last shell is an LVZ.
              if (lastShell.getBottomSampleIndex() > newShell.getTopSampleIndex()) {
                lastShell.setTopSampleIndex(newShell.getTopSampleIndex());
                lastShell.setTopSampleRadius(newShell.getTopSampleRadius());
              } else {
                sShells.add(newShell);
                lastShell = newShell;
              }
            }
          } else {
            sShells.add(newShell);
            lastShell = newShell;
          }
        }
      }
      fixLvzShells('S', sShells);
    }
  }

  /**
   * We need to filter out shells that are entirely inside a high slowness zone (low velocity zone).
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @param shells Model shells
   */
  private void fixLvzShells(char type, ArrayList<ModelShell> shells) {
    int n;

    n = shells.get(shells.size() - 1).getBottomSampleIndex();
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
   * The final shells are created in depth model, but are needed in final model as well.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @param shells Model shells
   */
  public void putShells(char type, ArrayList<ModelShell> shells) {
    if (type == 'P') {
      pShells = shells;
    } else {
      sShells = shells;
    }
  }

  /**
   * Get a depth model shell by index.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @param index Depth model shell index
   * @return Depth model shell
   */
  public ModelShell getShell(char type, int index) {
    if (type == 'P') {
      return pShells.get(index);
    } else {
      return sShells.get(index);
    }
  }

  /**
   * Get the last depth model shell (i.e., the one starting at the surface).
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @return Depth model shell
   */
  public ModelShell getLastShell(char type) {
    if (type == 'P') {
      return pShells.get(pShells.size() - 1);
    } else {
      return sShells.get(sShells.size() - 1);
    }
  }

  /**
   * Get a depth model shell index by name. If the name is not found, return -1;
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @param name Depth model shell name
   * @return Depth model shell index
   */
  public int getShell(char type, ShellName name) {
    if (type == 'P') {
      for (int j = 0; j < pShells.size(); j++) {
        if (pShells.get(j).getName().equals(name.name())) return j;
      }
    } else {
      for (int j = 0; j < sShells.size(); j++) {
        if (sShells.get(j).getName().equals(name.name())) return j;
      }
    }
    return -1;
  }

  /**
   * Get a depth model shell index by name. If the name is not found return -1;
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @param name Generic shell name
   * @return Depth model shell index
   */
  public int getShellIndex(char type, ShellName name) {
    int index;

    // First try some special names needed to make branches.
    if (name == ShellName.SURFACE) {
      return shellSize(type) - 1;
    } else if (name == ShellName.MANTLE_BOTTOM) {
      index = getShell(type, ShellName.CORE_MANTLE_BOUNDARY);
      if (index < 0) {
        index = getShell(type, ShellName.OUTER_CORE);
      }
      return index;
    } else if (name == ShellName.CORE_TOP) {
      return getShell(type, ShellName.OUTER_CORE);
    } else {
      // If that fails, just try the name.
      return getShell(type, name);
    }
  }

  /**
   * Get the number of shells.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @return Number of shells
   */
  public int shellSize(char type) {
    if (type == 'P') {
      return pShells.size();
    } else {
      return sShells.size();
    }
  }

  /** Make integral pieces from the raw integrals in pInts and sInts. */
  public void makePieces() {
    pPieces = new BranchIntegrals('P', this);
    sPieces = new BranchIntegrals('S', this);
  }

  /**
   * Get the integral pieces for one phase type.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @return Integral pieces
   */
  public BranchIntegrals getPiece(char type) {
    if (type == 'P') {
      return pPieces;
    } else {
      return sPieces;
    }
  }

  /**
   * Get the master (possibly decimated) ray parameter array. Note that the S-wave ray parameters
   * are a super set the P-wave ray parameters.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @return Ray parameter array
   */
  public double[] getP(char type) {
    if (type == 'P') {
      return pPieces.getRayParameters();
    } else {
      return sPieces.getRayParameters();
    }
  }

  /**
   * Get a ray parameter by index from the master (possibly decimated) array.
   *
   * @param index Ray parameter index
   * @return Ray parameter
   */
  public double getP(int index) {
    return sPieces.getRayParameters()[index];
  }

  /**
   * Get the master decimation array.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @return The master decimation array
   */
  public boolean[] getDecimation(char type) {
    if (type == 'P') {
      return pPieces.getDecimationKeep();
    } else {
      return sPieces.getDecimationKeep();
    }
  }

  /** Decimate the ray parameter arrays for both the P and S models. */
  public void decimateRayParameters() {
    pPieces.decimateRayParameters();
    sPieces.decimateRayParameters();
  }

  /**
   * Decimate the up-going tau and range arrays in parallel with decimating the master ray parameter
   * arrays in the integral pieces. Note that there are two sets of up-going branches with different
   * sampling. The up-going branches here are used to correct all the other branches for source
   * depth. The up-going branch sampling that will be used to actually generate the up-going branch
   * travel times was done with the proxy sampling. These branches are stubs in the sense that they
   * have a sampling, but tau and range are zero as is appropriate for a surface focus source.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   */
  public void decimateTauX(char type) {
    int k;
    boolean[] keep;
    double[] tau, x, decTau = null, decX = null;

    keep = getDecimation(type);

    // Loop over the up-going branches.
    for (int i = 0; i < intsSize(type) - 3; i++) {
      // Some of the integrals don't exist.
      tau = getTauInt(type, i);
      if (tau != null) {
        x = getXInt(type, i);
        // Allocate temporary arrays on the shallowest integrals
        // because they are the longest.
        if (decTau == null) {
          decTau = new double[tau.length];
          decX = new double[x.length];
        }
        // Do the decimation.
        k = 0;
        for (int j = 0; j < tau.length; j++) {
          if (keep[j]) {
            decTau[k] = tau[j];
            decX[k++] = x[j];
          }
        }
        // Update the integral arrays with the decimated versions.
        update(type, i, k, decTau, decX);
      }
    }
  }

  /**
   * Set the low velocity zone (really high slowness zone) flag.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   */
  public void setLvz(char type) {
    if (type == 'P') {
      pInts.get(pInts.size() - 1).lvz = true;
    } else {
      sInts.get(sInts.size() - 1).lvz = true;
    }
  }

  /**
   * Get the low velocity zone (really high slowness zone) flag. Note that this is done by slowness
   * rather than index because the final model is so fragmentary. It is assumed that there are no
   * low velocity zones where the final model is missing samples (i.e., the lower mantle). The
   * core-mantle boundary is, of course, a special case.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @param index Model index
   * @return True if this level corresponds to a low velocity zone
   */
  public boolean getLvz(char type, int index) {
    if (type == 'P') {
      if (pInts.get(index) != null) {
        return pInts.get(index).lvz;
      } else {
        return false;
      }
    } else {
      if (sInts.get(index) != null) {
        return sInts.get(index).lvz;
      } else {
        return false;
      }
    }
  }

  /**
   * Get one of the integral sets by index.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @param index Depth index
   * @return Tau integrals for all ray parameters
   */
  public double[] getTauInt(char type, int index) {
    if (type == 'P') {
      if (pInts.get(index) != null) {
        return pInts.get(index).tau;
      } else {
        return null;
      }
    } else {
      if (sInts.get(index) != null) {
        return sInts.get(index).tau;
      } else {
        return null;
      }
    }
  }

  /**
   * Get one of the special integral sets by name.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @param name Special integral set name
   * @return Tau integrals for all ray parameters
   */
  public double[] getTauInt(char type, ShellName name) {
    int n;

    if (type == 'P') {
      n = pInts.size();
      for (int j = n - 3; j < n; j++) {
        if (name == pInts.get(j).name) {
          return pInts.get(j).tau;
        }
      }
    } else {
      n = sInts.size();
      for (int j = n - 3; j < n; j++) {
        if (name == sInts.get(j).name) {
          return sInts.get(j).tau;
        }
      }
    }
    return null;
  }

  /**
   * Get one of the integral sets by index.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @param index Depth index
   * @return Tau integrals for all ray parameters
   */
  public double[] getXInt(char type, int index) {
    if (type == 'P') {
      if (pInts.get(index) != null) {
        return pInts.get(index).x;
      } else {
        return null;
      }
    } else {
      if (sInts.get(index) != null) {
        return sInts.get(index).x;
      } else {
        return null;
      }
    }
  }

  /**
   * Get one of the special integral sets by name.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @param name Special integral set name
   * @return Range integrals for all ray parameters
   */
  public double[] getXInt(char type, ShellName name) {
    int n;

    if (type == 'P') {
      n = pInts.size();
      for (int j = n - 3; j < n; j++) {
        if (name == pInts.get(j).name) {
          return pInts.get(j).x;
        }
      }
    } else {
      n = sInts.size();
      for (int j = n - 3; j < n; j++) {
        if (name == sInts.get(j).name) {
          return sInts.get(j).x;
        }
      }
    }
    return null;
  }

  /**
   * Get a major shell tau value by index. Note that because these values are drawn from the pieces,
   * the integrals are specific to a region rather than cumulative as for getTauInt.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @param index Tau index
   * @param shell Major shell index (0 = mantle, 1 = outer core, 2 = inner core)
   * @return Tau value
   */
  public double getTauSpec(char type, int index, int shell) {
    switch (shell) {
      case 0:
        if (type == 'P') {
          return pPieces.getMantleTauIntegrals()[index];
        } else {
          return sPieces.getMantleTauIntegrals()[index];
        }
      case 1:
        if (type == 'P') {
          return pPieces.getOuterCoreTauIntegrals()[index];
        } else {
          return sPieces.getOuterCoreTauIntegrals()[index];
        }
      case 2:
        if (type == 'P') {
          return pPieces.getInnerCoreTauIntegrals()[index];
        } else {
          return sPieces.getInnerCoreTauIntegrals()[index];
        }
      default:
        return Double.NaN;
    }
  }

  /**
   * Get a major shell range value by index. Note that because these values are drawn from the
   * pieces, the integrals are specific to a region rather than cumulative as for getTauInt.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @param index Range index
   * @param shell Major shell index (0 = mantle, 1 = outer core, 2 = inner core)
   * @return Range value
   */
  public double getXspec(char type, int index, int shell) {
    switch (shell) {
      case 0:
        if (type == 'P') {
          return pPieces.getMantleRangeIntegrals()[index];
        } else {
          return sPieces.getMantleRangeIntegrals()[index];
        }
      case 1:
        if (type == 'P') {
          return pPieces.getOuterCoreRangeIntegrals()[index];
        } else {
          return sPieces.getOuterCoreRangeIntegrals()[index];
        }
      case 2:
        if (type == 'P') {
          return pPieces.getInnerCoreRangeIntegrals()[index];
        } else {
          return sPieces.getInnerCoreRangeIntegrals()[index];
        }
      default:
        return Double.NaN;
    }
  }

  /**
   * Update tau and range arrays with their decimated versions.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @param index Integral index
   * @param len Length of the decimated arrays
   * @param newTau Decimated tau array
   * @param newX Decimated range array
   */
  public void update(char type, int index, int len, double[] newTau, double[] newX) {
    if (type == 'P') {
      pInts.get(index).update(len, newTau, newX);
    } else {
      sInts.get(index).update(len, newTau, newX);
    }
  }

  /**
   * Get the radial sampling interval associated with a shell by index.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @param index Shell index
   * @return Non-dimensional radial sampling
   */
  public double getDelX(char type, int index) {
    if (type == 'P') {
      return convert.normR(pShells.get(index).getRangeIncrementTarget());
    } else {
      return convert.normR(sShells.get(index).getRangeIncrementTarget());
    }
  }

  /**
   * Get the radial sampling interval associated with the shell above the current one.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @param index Shell index
   * @return Non-dimensional radial sampling
   */
  public double getNextDelX(char type, int index) {
    if (type == 'P') {
      for (int j = index + 1; j < pShells.size(); j++) {
        if (!pShells.get(j).getIsDiscontinuity()) {
          return convert.normR(pShells.get(j).getRangeIncrementTarget());
        }
      }
    } else {
      for (int j = index + 1; j < sShells.size(); j++) {
        if (!sShells.get(j).getIsDiscontinuity()) {
          return convert.normR(sShells.get(j).getRangeIncrementTarget());
        }
      }
    }
    return Double.NaN;
  }

  /**
   * Get the size of the integral lists.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @return The size of the integral list
   */
  public int intsSize(char type) {
    if (type == 'P') {
      return pInts.size();
    } else {
      return sInts.size();
    }
  }

  /**
   * The the size of the integrals list, counting only the non-null, upper mantle integrals. These
   * are the integrals used for depth correction.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @return The non-null number of upper mantle integrals
   */
  public int intsRealSize(char type) {
    int size = 0;

    if (type == 'P') {
      for (int j = 0; j < pInts.size() - 3; j++) {
        if (pInts.get(j) != null) size++;
      }
    } else {
      for (int j = 0; j < sInts.size() - 3; j++) {
        if (sInts.get(j) != null) size++;
      }
    }
    return size;
  }

  /**
   * Copy the list of ray parameters associated with branch ends to an internal array.
   *
   * @param ends List of non-dimensional ray parameters associated with branch ends
   */
  public void setEnds(TreeSet<Double> ends) {
    int j = 0;
    Iterator<Double> iter;

    pEnds = new double[ends.size()];
    iter = ends.iterator();

    while (iter.hasNext()) {
      pEnds[j++] = iter.next();
    }
  }

  /**
   * Get the ray parameters associated with the branch ends.
   *
   * @return Array of branch end ray parameters.
   */
  public double[] getPxUp() {
    return pEnds;
  }

  /**
   * Get the ray travel distances (ranges) at the branch ends.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @param index Depth index
   * @return Array of branch end ranges
   */
  public ArrayList<Double> getXUp(char type, int index) {
    int i = 0;
    double[] p, x;
    ArrayList<Double> xUp;

    xUp = new ArrayList<Double>();
    p = getP(type);
    if (type == 'P') {
      if (pInts.get(index) != null) {
        x = pInts.get(index).x;
        for (int j = 0; j < x.length; j++) {
          if (pEnds[i] == p[j]) {
            xUp.add(x[j]);
            i++;
          }
        }
      } else {
        return null;
      }
    } else {
      if (sInts.get(index) != null) {
        x = sInts.get(index).x;
        for (int j = 0; j < x.length; j++) {
          if (pEnds[i] == p[j]) {
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
   * Print out the slowness model.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @param ver Model version ("Tau", "Depth", or "Final")
   */
  public void printModel(char type, String ver) {
    if (ver.equals("Depth")) {
      System.out.println("\n     Depth model for " + type + " slowness");
      System.out.println("      R     slowness      Z");
    } else if (ver.equals("Final")) {
      System.out.println("\n     Final model for " + type + " slowness");
      System.out.println("      R     slowness      Z       length");
    } else {
      System.out.println("\n   Tau model for " + type + " slowness");
      System.out.println("      R     slowness    X");
    }
    if (type == 'P') {
      if (!ver.equals("Final")) {
        for (int j = 0; j < pModel.size(); j++) {
          System.out.format("%3d %s\n", j, pModel.get(j));
        }
      } else {
        for (int j = 0; j < pModel.size(); j++) {
          if (pInts.get(j) != null) {
            System.out.format(
                "%3d %s %3d %s\n", j, pModel.get(j), pInts.get(j).tau.length, pInts.get(j).name);
          } else {
            System.out.format("%3d %s null\n", j, pModel.get(j));
          }
        }
      }
    } else {
      if (!ver.equals("Final")) {
        for (int j = 0; j < sModel.size(); j++) {
          System.out.format("%3d %s\n", j, sModel.get(j));
        }
      } else {
        for (int j = 0; j < sModel.size(); j++) {
          if (sInts.get(j) != null) {
            System.out.format(
                "%3d %s %3d %s\n", j, sModel.get(j), sInts.get(j).tau.length, sInts.get(j).name);
          } else {
            System.out.format("%3d %s null\n", j, sModel.get(j));
          }
        }
      }
    }
  }

  /**
   * Print out the slowness model.
   *
   * @param ver Model version ("Tau", "Depth", or "Final")
   */
  public void printModel(String ver) {
    if (ver.equals("Depth")) {
      System.out.println("\n     Depth model");
      System.out.println("      R     slowness      Z     slowness" + "      Z");
    } else if (ver.equals("Final")) {
      System.out.println("\n     Final model");
      System.out.println(
          "      R     slowness      Z       length" + "     slowness      Z       length");
    } else {
      System.out.println("\n   Tau model");
      System.out.println("      R     slowness    X     slowness    X");
    }
    if (!ver.equals("Final")) {
      for (int j = 0; j < pModel.size(); j++) {
        System.out.format("%3d %s %s\n", j, pModel.get(j), sModel.get(j));
      }
      if (!ver.equals("Depth")) {
        for (int j = pModel.size(); j < sModel.size(); j++) {
          System.out.format("%3d                           %s\n", j, pModel.get(j), sModel.get(j));
        }
      } else {
        for (int j = pModel.size(); j < sModel.size(); j++) {
          System.out.format(
              "%3d                               %s\n", j, pModel.get(j), sModel.get(j));
        }
      }
    } else {
      for (int j = 0; j < pModel.size(); j++) {
        if (pInts.get(j) != null) {
          if (sInts.get(j) != null) {
            System.out.format(
                "%3d %s  %3d %s  %3d\n",
                j, pModel.get(j), pInts.get(j).tau.length, sModel.get(j), sInts.get(j).tau.length);
          } else {
            System.out.format(
                "%3d %s  %3d %s null\n", j, pModel.get(j), pInts.get(j).tau.length, sModel.get(j));
          }
        } else {
          if (sInts.get(j) != null) {
            System.out.format(
                "%3d %s null %s  %3d\n", j, pModel.get(j), sModel.get(j), sInts.get(j).tau.length);
          } else {
            System.out.format("%3d %s null %s null\n", j, pModel.get(j), sModel.get(j));
          }
        }
      }
      for (int j = pModel.size(); j < sModel.size(); j++) {
        if (sInts.get(j) != null) {
          System.out.format(
              "%3d                                    %s  %3d\n",
              j, sModel.get(j), sInts.get(j).tau.length);
        } else {
          System.out.format("%3d                                    %s null\n", j, sModel.get(j));
        }
      }
    }
  }

  /** Print out the merged slownesses. */
  public void printMerge() {
    System.out.println("\n\tMerged slownesses");
    for (int j = 0; j < slowness.size(); j++) {
      System.out.format("\t%3d %8.6f\n", j, slowness.get(j));
    }
  }

  /**
   * Print the wave type specific shells for the depth model.
   *
   * @param type Wave type (P = compressional, S = shear)
   */
  public void printDepShells(char type) {
    String shellString;

    System.out.println("\n\t" + type + " Model Shells:");
    if (type == 'P') {
      for (int j = 0; j < pShells.size(); j++) {
        shellString = pShells.get(j).printShell(type);
        if (shellString != null) System.out.format("%3d   %s\n", j, shellString);
      }
    } else {
      for (int j = 0; j < sShells.size(); j++) {
        shellString = sShells.get(j).printShell(type);
        if (shellString != null) System.out.format("%3d   %s\n", j, shellString);
      }
    }
  }

  /**
   * Access the toString for the last entry of the tau-range integrals for the desired phase type.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   * @return String summarizing the last tau-range integral sample
   */
  public String stringLast(char type) {
    int n;

    if (type == 'P') {
      n = pModel.size() - 1;
      return String.format(
          "%3d %9.6f %8.6f", pInts.get(n).tau.length, pModel.get(n).z, pModel.get(n).slow);
    } else {
      n = sModel.size() - 1;
      return String.format(
          "%3d %9.6f %8.6f", sInts.get(n).tau.length, sModel.get(n).z, sModel.get(n).slow);
    }
  }

  /**
   * Print a summary of all the tau-range integrals for the desired phase type.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   */
  public void printInt(char type) {
    int n;

    if (type == 'P') {
      n = pInts.size();
      for (int j = 0; j < n - 3; j++) {
        if (pInts.get(j) != null) {
          System.out.format(
              "Lev1 %3d %3d %9.6f %8.6f\n",
              j, pInts.get(j).tau.length, pModel.get(j).z, pModel.get(j).slow);
        }
      }
      for (int j = n - 3; j < n - 1; j++) {
        System.out.format(
            "Lev2 %3d %3d %9.6f %8.6f\n",
            j, pInts.get(j).tau.length, pModel.get(j).z, pModel.get(j).slow);
      }
      System.out.format(
          "Lev3 %3d %3d %9.6f %8.6f\n",
          n - 1, pInts.get(n - 1).tau.length, pModel.get(n - 1).z, pModel.get(n - 1).slow);
    } else {
      n = sInts.size();
      for (int j = 0; j < n - 3; j++) {
        if (sInts.get(j) != null) {
          System.out.format(
              "Lev1 %3d %3d %9.6f %8.6f\n",
              j, sInts.get(j).tau.length, sModel.get(j).z, sModel.get(j).slow);
        }
      }
      for (int j = n - 3; j < n - 1; j++) {
        System.out.format(
            "Lev2 %3d %3d %9.6f %8.6f\n",
            j, sInts.get(j).tau.length, sModel.get(j).z, sModel.get(j).slow);
      }
      System.out.format(
          "Lev3 %3d %3d %9.6f %8.6f\n",
          n - 1, sInts.get(n - 1).tau.length, sModel.get(n - 1).z, sModel.get(n - 1).slow);
    }
  }

  /**
   * Print out the shell integrals. Note that these are the raw cumulative integrals.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   */
  public void printShellInts(char type) {
    int n;
    TauXsample mantle, outerCore, innerCore;

    System.out.format("\n\t\tShell Integrals for %c-waves\n", type);
    System.out.println("                        Tau                    " + "   X");
    System.out.println("        p     Mantle     OC       IC     Mantle" + "   OC     IC");
    if (type == 'P') {
      n = pInts.size();
      mantle = pInts.get(n - 3);
      outerCore = pInts.get(n - 2);
      innerCore = pInts.get(n - 1);
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
      n = sInts.size();
      mantle = sInts.get(n - 3);
      outerCore = sInts.get(n - 2);
      innerCore = sInts.get(n - 1);
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

  /** Print out the proxy ranges. */
  public void printProxy() {
    int nP, nS;

    System.out.println("\n\t\t\tProxy Ranges");
    System.out.println("                  P                            S");
    System.out.println("    slowness      X       delX   slowness" + "      X       delX");
    nP = pPieces.getProxyRanges().length;
    nS = sPieces.getProxyRanges().length;
    System.out.format(
        "%3d %8.6f %8.2f            %8.6f %8.2f\n",
        0,
        pPieces.getProxyRayParameters()[0],
        convert.dimR(pPieces.getProxyRanges()[0]),
        sPieces.getProxyRayParameters()[0],
        convert.dimR(sPieces.getProxyRanges()[0]));
    if (nS >= nP) {
      for (int j = 1; j < nP; j++) {
        System.out.format(
            "%3d %8.6f %8.2f %8.2f   %8.6f %8.2f %8.2f\n",
            j,
            pPieces.getProxyRayParameters()[j],
            convert.dimR(pPieces.getProxyRanges()[j]),
            convert.dimR(pPieces.getProxyRanges()[j] - pPieces.getProxyRanges()[j - 1]),
            sPieces.getProxyRayParameters()[j],
            convert.dimR(sPieces.getProxyRanges()[j]),
            convert.dimR(sPieces.getProxyRanges()[j] - sPieces.getProxyRanges()[j - 1]));
      }
      for (int j = nP; j < nS; j++) {
        System.out.format(
            "%3d                              " + "%8.6f %8.2f %8.2f\n",
            j,
            sPieces.getProxyRayParameters()[j],
            convert.dimR(sPieces.getProxyRanges()[j]),
            convert.dimR(sPieces.getProxyRanges()[j] - sPieces.getProxyRanges()[j - 1]));
      }
    } else {
      for (int j = 1; j < nS; j++) {
        System.out.format(
            "%3d %8.6f %8.2f %8.2f   %8.6f %8.2f %8.2f\n",
            j,
            pPieces.getProxyRayParameters()[j],
            convert.dimR(pPieces.getProxyRanges()[j]),
            convert.dimR(pPieces.getProxyRanges()[j] - pPieces.getProxyRanges()[j - 1]),
            sPieces.getProxyRayParameters()[j],
            convert.dimR(sPieces.getProxyRanges()[j]),
            convert.dimR(sPieces.getProxyRanges()[j] - sPieces.getProxyRanges()[j - 1]));
      }
      for (int j = nS; j < nP; j++) {
        System.out.format(
            "%3d %8.6f %8.2f %8.2f\n",
            j,
            pPieces.getProxyRayParameters()[j],
            convert.dimR(pPieces.getProxyRanges()[j]),
            convert.dimR(pPieces.getProxyRanges()[j] - pPieces.getProxyRanges()[j - 1]));
      }
    }
  }

  /**
   * Print the integrals for the whole mantle, outer core, and inner core. Note that these are the
   * partial integrals specific to a major shell of the Earth.
   *
   * @param type Model type (P = P slowness, S = S slowness)
   */
  public void printShellSpec(char type) {
    if (type == 'P') {
      pPieces.printShellInts();
    } else {
      sPieces.printShellInts();
    }
  }

  /** Print the ray parameter arrays for both the P and S branches. */
  public void printP() {
    System.out.println("\nMaster Ray Parameters");
    System.out.println("       P        S");
    for (int j = 0; j < pPieces.getRayParameters().length; j++) {
      System.out.format(
          "%3d %8.6f %8.6f\n", j, pPieces.getRayParameters()[j], sPieces.getRayParameters()[j]);
    }
    for (int j = pPieces.getRayParameters().length; j < sPieces.getRayParameters().length; j++) {
      System.out.format("%3d          %8.6f\n", j, sPieces.getRayParameters()[j]);
    }
  }
}
