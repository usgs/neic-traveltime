package gov.usgs.traveltime;

import gov.usgs.traveltime.tables.TauIntegralException;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * The AllBranchVolume class is the umbrella storage for all volatile (changes with depth) branch
 * level travel-time data.
 *
 * @author Ray Buland
 */
public class AllBranchVolume {
  /** An array of BranchDataVolume objects containing volatile branch data */
  private BranchDataVolume[] branches; // volatile branch data

  /** A ModelDataVolume object containing the volatile P earth model data */
  private ModelDataVolume earthModelP;

  /** A ModelDataVolume object containing the volatile S earth model data */
  private ModelDataVolume earthModelS;

  /** An UpGoingDataVolume object holding the volatile up-going P branch data */
  private UpGoingDataVolume upgoingPBranch; // volatile up-going branch data

  /** An UpGoingDataVolume object holding the volatile up-going P branch data */
  private UpGoingDataVolume upgoingSBranch;

  /** A double holding the derivative of travel time with respect to depth */
  private double ttDepthDerivative;

  /** A double containing the geographic source (earthquake) latitude in degrees */
  private double sourceLatitude;

  /** A double containing the source (earthquake) longitude in degrees */
  private double sourceLongitude;

  /** A double containing the dimensional source depth in kilometers */
  private double sourceDimensionalDepth;

  /** A double containing the flat earth source depth in kilometers */
  private double sourceFlatDepth;

  /** A double containing the source (earthquake) reciever (station) distance in degrees */
  private double receiverDistance;

  /** A double containing the receiver (station) azimuth at the source (earthquake) in degrees */
  private double receiverAzimuth;

  /** A boolean flag, true indicates that this is a "complex" session */
  private boolean isComplexSession;

  /** A boolean flag, true indicates that this is a "complex" request */
  private boolean isComplexRequest;

  /** A boolean flag, true indicates that the depth is out of range */
  private boolean depthIsOutOfRange;

  /** A boolean flag, true indicates that the distance is out of range */
  private boolean distanceIsOutOfRange;

  /** A boolean flag, true indicates to do all phases (including useless ones) */
  private boolean returnAllPhases;

  /** A boolean flag, true indicates to return back branches as well as primary arrivals */
  private boolean returnBackBranches;

  /** A boolean flag, true indicates the source is in a tectonic province */
  private boolean isTectonicSource;

  /** A double containing the last depth computed in kilometers */
  private double lastComputedDepth = Double.NaN;

  /** An AllBranchReference object containing the non-volatile travel-time branch data */
  AllBranchReference allBranchReference;

  /** A ModelConversions object containing model dependent constants and conversions */
  private ModelConversions modelConversions;

  /** A TravelTimeFlags object containing the auxiliary travel-time information */
  private TravelTimeFlags flags;

  /**
   * A Spline object holding the spline interpolation routines needed for the computation of travel
   * times.
   */
  private Spline splineRoutines;

  /* An ArrayList of Strings containing the list of desired phases using group codes. Note that the list can be a
   * combination of phase codes, group codes, and special keywords. */
  private ArrayList<String> phaseList;

  /** An integer containing the last branch index used in calculations */
  private int lastBranchIndex = -1;

  /** An integer containing the last up going branch index for P used in calculations */
  private int lastUpgoingPBranchIndex = -1;

  /** An integer containing the last up going branch index for S used in calculations */
  private int lastUpgoingSBranchIndex = -1;

  /** Private logging object. */
  private static final Logger LOGGER = Logger.getLogger(AllBranchVolume.class.getName());

  /**
   * Get the non-volatile travel-time branch data
   *
   * @return An AllBranchReference object containing the non-volatile travel-time branch data
   */
  public AllBranchReference getAllBranchReference() {
    return allBranchReference;
  }

  /**
   * Function to convert this AllBranchVolume into a string.
   *
   * @return A String containing the string representation of this AllBranchVolume
   */
  @Override
  public String toString() {
    return allBranchReference.getEarthModelName()
        + " d="
        + lastComputedDepth
        + " cmplx="
        + (isComplexSession ? "T" : "F")
        + " eqcoord="
        + sourceLatitude
        + " "
        + sourceLongitude
        + " distance="
        + receiverDistance
        + " flgs="
        + (!returnAllPhases ? "U" : "u")
        + (!returnBackBranches ? "b" : "B")
        + (isTectonicSource ? "T" : "t");
  }

  /**
   * AllBranchVolume constructor, sets up volatile copies of data that changes with depth.
   *
   * @param allBranchReference The reference data source
   */
  public AllBranchVolume(AllBranchReference allBranchReference) {
    this.allBranchReference = allBranchReference;
    this.modelConversions = allBranchReference.getModelConversions();

    // Set up the volatile piece of the model.
    earthModelP = new ModelDataVolume(allBranchReference.getEarthModelP(), modelConversions);
    earthModelS = new ModelDataVolume(allBranchReference.getEarthModelS(), modelConversions);

    // Set up the up-going branch data.
    upgoingPBranch =
        new UpGoingDataVolume(
            allBranchReference.getUpgoingPBranchData(), earthModelP, earthModelS, modelConversions);
    upgoingSBranch =
        new UpGoingDataVolume(
            allBranchReference.getUpgoingSBranchData(), earthModelS, earthModelP, modelConversions);

    // Set up the branch data.
    branches = new BranchDataVolume[allBranchReference.getSurfaceBranches().length];
    splineRoutines = new Spline();

    for (int j = 0; j < branches.length; j++) {
      branches[j] =
          new BranchDataVolume(
              allBranchReference.getSurfaceBranches()[j],
              upgoingPBranch,
              upgoingSBranch,
              modelConversions,
              allBranchReference.getAuxTTData(),
              splineRoutines);
    }
  }

  /**
   * Function to set up a new session. Note that this sets up the complex session parameters of use
   * to the travel-time package.
   *
   * @param latitude A double containing the source (earthquake) geographical latitude in degrees
   * @param longitude A double containing the source (earthquake) longitude in degrees
   * @param depth A double containing the source (earthquake) depth in kilometers
   * @param phasesToUse An Array strings containing the the phase use commands
   * @param returnAllPhases A boolean flag, if false, only provide "useful" crustal phases
   * @param returnBackBranches A boolean flag, if false, suppress back branches
   * @param isTectonicSource A boolean flag, if true, map Pb and Sb onto Pg and Sg
   * @throws BadDepthException If the depth is out of range
   * @throws TauIntegralException If the tau integral doesn't make sense
   */
  public void newSession(
      double latitude,
      double longitude,
      double depth,
      String[] phasesToUse,
      boolean returnAllPhases,
      boolean returnBackBranches,
      boolean isTectonicSource)
      throws BadDepthException, TauIntegralException {
    // See if the epicenter makes sense.
    if (!Double.isNaN(latitude)
        && latitude >= -90d
        && latitude <= 90d
        && !Double.isNaN(longitude)
        && longitude >= -180d
        && longitude <= 180d) {
      isComplexSession = true;
      sourceLatitude = latitude;
      sourceLongitude = longitude;
      // If not, this can only be a simple request.
    } else {
      isComplexSession = false;
      sourceLatitude = Double.NaN;
      sourceLongitude = Double.NaN;
    }

    setSession(depth, phasesToUse, returnAllPhases, returnBackBranches, isTectonicSource);
  }

  /**
   * Function to set up a new session. Note that this just sets up the simple session parameters of
   * use to the travel-time package.
   *
   * @param depth A double containing the source depth in kilometers
   * @param phasesToUse An array of strings containing the phase use commands
   * @param returnAllPhases A boolean flag, if false, only provide "useful" crustal phases
   * @param returnBackBranches A boolean flag, if false, suppress back branches
   * @param isTectonicSource A boolean flag, if true, map Pb and Sb onto Pg and Sg
   * @throws BadDepthException If the depth is out of range
   * @throws TauIntegralException If the tau integral doesn't make sense
   */
  public void newSession(
      double depth,
      String[] phasesToUse,
      boolean returnAllPhases,
      boolean returnBackBranches,
      boolean isTectonicSource)
      throws BadDepthException, TauIntegralException {
    isComplexSession = false;
    sourceLatitude = Double.NaN;
    sourceLongitude = Double.NaN;

    setSession(depth, phasesToUse, returnAllPhases, returnBackBranches, isTectonicSource);
  }

  /**
   * Function to set up a new session. Note that this just sets up the simple session parameters of
   * use to the travel-time package.
   *
   * @param depth A double containing the source depth in kilometers
   * @param phasesToUse An array of strings containing the phase use commands
   * @param returnAllPhases A boolean flag, if false, only provide "useful" crustal phases
   * @param returnBackBranches A boolean flag, if false, suppress back branches
   * @param isTectonicSource A boolean flag, if true, map Pb and Sb onto Pg and Sg
   * @throws BadDepthException If the depth is out of range
   * @throws TauIntegralException If the tau integral doesn't make sense
   */
  private void setSession(
      double depth,
      String[] phasesToUse,
      boolean returnAllPhases,
      boolean returnBackBranches,
      boolean isTectonicSource)
      throws BadDepthException, TauIntegralException {

    // Make sure the depth is in range.
    if (depth >= 0d && depth <= TauUtilities.MAXDEPTH) {
      depthIsOutOfRange = false;

      // Remember the session control flags.
      this.returnAllPhases = returnAllPhases;
      this.returnBackBranches = returnBackBranches;

      if (depth != lastComputedDepth) {
        // Set up the new source depth.
        sourceDimensionalDepth = Math.max(depth, 0.011d);

        // The interpolation gets squirrelly for very shallow sources.
        if (depth < 0.011d) {
          // Note that a source depth of zero is intrinsically different
          // from any positive depth (i.e., no up-going phases).
          sourceFlatDepth = 0d;
          ttDepthDerivative = 1d / modelConversions.getVelocityNormalization();
        } else {
          sourceFlatDepth =
              Math.min(
                  Math.log(
                      Math.max(
                          1d - sourceDimensionalDepth * modelConversions.getDistanceNormalization(),
                          TauUtilities.DMIN)),
                  0d);
          ttDepthDerivative =
              1d
                  / (modelConversions.getVelocityNormalization()
                      * (1d
                          - sourceDimensionalDepth * modelConversions.getDistanceNormalization()));
        }

        // See if we want all phases.
        boolean all = false;
        if (phasesToUse != null) {
          if (phasesToUse.length > 0) {
            // Check for the "all" keyword (overrides everything else).
            for (int j = 0; j < phasesToUse.length; j++) {
              if (phasesToUse[j].toLowerCase().equals("all")) {
                all = true;
                break;
              }
            }
          } else {
            all = true; // The phase list is empty.
          }
        } else {
          all = true; // The phase list is null.
        }

        // Initialize the branch compute flags.
        if (all) {
          // Turn everything on except possibly useless phases.
          for (int j = 0; j < branches.length; j++) {
            // If we only want useful phases and this one is useless, just
            // turn it off.
            if (!returnAllPhases && branches[j].getIsPhaseUseless()) {
              branches[j].setChouldComputeTT(false);
            } else {
              // Otherwise, we're good to go.
              branches[j].setChouldComputeTT(true);
            }
          }
        } else {
          // Compare branch phase codes against the phase list.
          expandPhaseList(phasesToUse);

          for (int j = 0; j < branches.length; j++) {
            // See if this phase is selected (unless it's NEIC-useless).
            if (returnAllPhases || !branches[j].getIsPhaseUseless()) {
              branches[j].setChouldComputeTT(
                  isPhaseInList(allBranchReference.getSurfaceBranches()[j].getBranchPhaseCode()));
            } else {
              branches[j].setChouldComputeTT(false);
            }
          }
        }

        // Correct the up-going branch data.
        upgoingPBranch.newDepth(sourceFlatDepth);
        upgoingSBranch.newDepth(sourceFlatDepth);

        // To correct each branch we need a few depth dependent pieces.
        char branchSuffix;
        double xMin =
            modelConversions.getDistanceNormalization()
                * Math.min(Math.max(2d * sourceDimensionalDepth, 2d), 25d);
        if (sourceDimensionalDepth <= modelConversions.getConradDepth()) {
          branchSuffix = 'g';
        } else if (sourceDimensionalDepth <= modelConversions.getMohoDepth()) {
          branchSuffix = 'b';
        } else if (sourceDimensionalDepth <= modelConversions.getUpperMantleDepth()) {
          branchSuffix = 'n';
        } else {
          branchSuffix = ' ';
        }

        // Now correct each branch.
        for (int j = 0; j < branches.length; j++) {
          branches[j].correctDepth(sourceFlatDepth, ttDepthDerivative, xMin, branchSuffix);
        }

        lastComputedDepth = depth;
      }
    } else {
      depthIsOutOfRange = true;
    }
  }

  /**
   * Function to get the arrival times from all branches for a "complex" request (i.e., including
   * latitude and longitude). This will include the Ellipticity and bounce point corrections as well
   * as the elevation correction.
   *
   * @param recieverLatitude A double containing the receiver geographic latitude in degrees
   * @param recieverLongitude A double containing the receiver longitude in degrees
   * @param recieverElevation A double containing the receiver elevation in kilometers
   * @param recieverDistance A double containing the source receiver distance desired in degrees
   * @param recieverAzimuth A double containing the receiver azimuth at the source in degrees
   * @return A TravelTime object containing the travel times
   */
  public TravelTime getTravelTime(
      double recieverLatitude,
      double recieverLongitude,
      double recieverElevation,
      double recieverDistance,
      double recieverAzimuth) {

    // If this is a simple session, the request can only be simple.
    if (isComplexSession) {
      isComplexRequest = true;
    } else {
      isComplexRequest = false;
    }

    // See if the distance makes sense.
    if ((!Double.isNaN(recieverDistance))
        && (recieverDistance >= 0d)
        && (recieverDistance <= 180d)) {
      distanceIsOutOfRange = false;
      this.receiverDistance = recieverDistance;

      if (isComplexRequest) {
        // OK, how about the azimuth.
        if ((!Double.isNaN(recieverAzimuth))
            && (recieverAzimuth >= 0d)
            && (recieverAzimuth <= 360d)) {
          this.receiverAzimuth = recieverAzimuth;
        } else {
          // If the station coordinates makes sense, we can fix it.
          if ((!Double.isNaN(recieverLatitude))
              && (recieverLatitude >= -90d)
              && (recieverLatitude <= 90d)
              && (!Double.isNaN(recieverLongitude))
              && (recieverLongitude >= -180d)
              && (recieverLongitude <= 180d)) {
            this.receiverDistance =
                TauUtilities.delAz(
                    sourceLatitude, sourceLongitude, recieverLatitude, recieverLongitude);
            this.receiverAzimuth = TauUtilities.recieverAzimuth;
          } else {
            isComplexRequest = false;
            this.receiverAzimuth = Double.NaN;
          }
        }
      } else {
        this.receiverAzimuth = Double.NaN;
      }

      // The distance is bad, see if we can fix it.
    } else {
      // See if the station coordinates make sense.
      if ((!Double.isNaN(recieverLatitude))
          && (recieverLatitude >= -90d)
          && (recieverLatitude <= 90d)
          && (!Double.isNaN(recieverLongitude))
          && (recieverLongitude >= -180d)
          && (recieverLongitude <= 180d)) {
        distanceIsOutOfRange = false;
        this.receiverDistance =
            TauUtilities.delAz(
                sourceLatitude, sourceLongitude, recieverLatitude, recieverLongitude);
        this.receiverAzimuth = TauUtilities.recieverAzimuth;
      } else {
        distanceIsOutOfRange = true;
      }
    }

    // If the elevation doesn't make sense, just set it to zero.
    if ((!Double.isNaN(recieverElevation))
        && (recieverElevation >= TauUtilities.MINELEV)
        && (recieverElevation <= TauUtilities.MAXELEV)) {
      return computeTravelTime(recieverElevation);
    } else {
      return computeTravelTime(0d);
    }
  }

  /**
   * Get the arrival times from all branches for a "simple" request (i.e., without latitude and
   * longitude. This will only include the elevation correction.
   *
   * @param recieverElevation Station elevation in kilometers
   * @param recieverDistance Source receiver distance desired in degrees
   * @return An array list of travel times
   */
  public TravelTime getTravelTime(double recieverElevation, double recieverDistance) {
    isComplexRequest = false;
    // See if the distance makes sense.
    if (!Double.isNaN(recieverDistance) && recieverDistance >= 0d && recieverDistance <= 180d) {
      distanceIsOutOfRange = false;
      this.receiverDistance = recieverDistance;
      this.receiverAzimuth = Double.NaN;
    } else {
      distanceIsOutOfRange = true;
    }
    // If the elevation doesn't make sense, just set it to zero.
    if (!Double.isNaN(recieverElevation)
        && recieverElevation >= TauUtilities.MINELEV
        && recieverElevation <= TauUtilities.MAXELEV) {
      return computeTravelTime(recieverElevation);
    } else {
      return computeTravelTime(0d);
    }
  }

  /**
   * Funtion to get the arrival times from all branches including all applicable travel-time
   * corrections.
   *
   * @param recieverElevation Station elevation in kilometers
   * @return An array list of travel times
   */
  private TravelTime computeTravelTime(double recieverElevation) {

    if (depthIsOutOfRange || distanceIsOutOfRange) {
      return null;
    }

    TravelTime ttList = new TravelTime(sourceDimensionalDepth, receiverDistance);
    int lastTT = 0;

    // The desired distance might translate to up to three
    // different distances (as the phases wrap around the Earth).
    double[] sourceRecieverDistances = new double[3];
    sourceRecieverDistances[0] = Math.abs(Math.toRadians(receiverDistance)) % (2d * Math.PI);
    if (sourceRecieverDistances[0] > Math.PI) {
      sourceRecieverDistances[0] = 2d * Math.PI - sourceRecieverDistances[0];
    }

    sourceRecieverDistances[1] = 2d * Math.PI - sourceRecieverDistances[0];
    sourceRecieverDistances[2] = sourceRecieverDistances[0] + 2d * Math.PI;
    if (Math.abs(sourceRecieverDistances[0]) <= TauUtilities.DTOL) {
      sourceRecieverDistances[0] = TauUtilities.DTOL;
      sourceRecieverDistances[2] = -10d;
    }

    if (Math.abs(sourceRecieverDistances[0] - Math.PI) <= TauUtilities.DTOL) {
      sourceRecieverDistances[0] = Math.PI - TauUtilities.DTOL;
      sourceRecieverDistances[1] = -10d;
    }

    // Set up the correction to surface focus.
    findUpBranchIndices();

    // Set up distance and azimuth for retrograde phases.
    double retrogradeDistance = Double.NaN;
    double retrogradeAzimuth = Double.NaN;
    if (isComplexRequest) {
      retrogradeDistance = 360d - receiverDistance;
      retrogradeAzimuth = 180d + receiverAzimuth;
      if (retrogradeAzimuth > 360d) {
        retrogradeAzimuth -= 360d;
      }
    }

    // Go get the arrivals.  This is a little convoluted because
    // of the correction to surface focus needed for the statistics.
    for (int j = 0; j < branches.length; j++) {
      // Loop over possible distances.
      for (int i = 0; i < 3; i++) {
        branches[j].getTravelTimes(
            i, sourceRecieverDistances[i], sourceDimensionalDepth, returnAllPhases, ttList);

        // We have to add the phase statistics and other corrections at this level.
        if (ttList.getNumPhases() > lastTT) {
          boolean upGoing;
          int delTT = -1;

          for (int k = lastTT; k < ttList.getNumPhases(); k++) {
            double upGoingBounceDistance;
            TravelTimeData TravelTime = ttList.getPhase(k);
            flags = allBranchReference.getAuxTTData().findPhaseFlags(TravelTime.phaseCode);

            if (TravelTime.phaseCode.equals("pwP")) {
              delTT = k;
            }

            // There's a special case for up-going P and S.
            if (TravelTime.dTdZ > 0d
                && (flags.PhaseGroup.equals("P") || flags.PhaseGroup.equals("S"))) {
              upGoing = true;
            } else {
              upGoing = false;
            }

            // Set the correction to surface focus.
            if (TravelTime.phaseCode.charAt(0) == 'L') {
              // Travel-time corrections and correction to surface focus
              // don't make sense for surface waves.
              upGoingBounceDistance = 0d;
            } else {
              // Get the correction.
              try {
                upGoingBounceDistance = computeUpRayCutoff(TravelTime.phaseCode, TravelTime.dTdD);
              } catch (PhaseNotFoundException e) {
                // This should never happen.
                System.out.println(e.toString());
                upGoingBounceDistance = 0d;
              }

              // This is the normal case.  Do various travel-time corrections.
              if (!TauUtilities.noCorr) {
                TravelTime.tt +=
                    elevationCorrection(TravelTime.phaseCode, recieverElevation, TravelTime.dTdD);
                // If this was a complex request, do the Ellipticity and bounce
                // point corrections.
                if (isComplexRequest) {
                  // The Ellipticity correction is straightforward.
                  TravelTime.tt +=
                      ellipticityCorrection(
                          sourceLatitude,
                          sourceDimensionalDepth,
                          receiverDistance,
                          receiverAzimuth,
                          upGoing);

                  // The bounce point correction is not.  See if there is a bounce.
                  if (branches[j].getBranchReference().getReflectionPhaseCode() != null) {
                    double reflectionCorrection = Double.NaN;

                    if (allBranchReference.getAuxTTData().getBouncePointTopography() != null) {
                      // If so, we may need to do some preliminary work.
                      String tmpCode;
                      if (branches[j].getBranchReference().getReflectionPhaseCode().equals("SP")) {
                        tmpCode = "S";
                      } else if (branches[j]
                          .getBranchReference()
                          .getReflectionPhaseCode()
                          .equals("PS")) {
                        tmpCode =
                            TravelTime.phaseCode.substring(0, TravelTime.phaseCode.indexOf('S'));
                      } else {
                        tmpCode = null;
                      }

                      // If we had an SP or PS, get the distance of the first part.
                      double downGoingBounceDistance;
                      if (tmpCode != null) {
                        try {
                          downGoingBounceDistance =
                              computeSurfaceRayDistance(tmpCode, TravelTime.dTdD);
                        } catch (PhaseNotFoundException e) {
                          // This should never happen.
                          System.out.println(e.toString());
                          //				System.out.format("Phase = %-8s distance = %6.2f\n",
                          // TravelTime.phaseCode,
                          // receiverDistance);
                          downGoingBounceDistance = 0.5d * receiverDistance;
                        }
                      } else {
                        downGoingBounceDistance = Double.NaN;
                      }

                      // Finally, we can do the bounce point correction.
                      if (TravelTime.dTdD > 0d) {
                        reflectionCorrection =
                            reflectionCorrection(
                                TravelTime.phaseCode,
                                branches[j].getBranchReference(),
                                sourceLatitude,
                                sourceLongitude,
                                receiverDistance,
                                receiverAzimuth,
                                TravelTime.dTdD,
                                upGoingBounceDistance,
                                downGoingBounceDistance);
                      } else {
                        reflectionCorrection =
                            reflectionCorrection(
                                TravelTime.phaseCode,
                                branches[j].getBranchReference(),
                                sourceLatitude,
                                sourceLongitude,
                                retrogradeDistance,
                                retrogradeAzimuth,
                                TravelTime.dTdD,
                                upGoingBounceDistance,
                                downGoingBounceDistance);
                      }
                    } else {
                      reflectionCorrection = Double.NaN;
                    }

                    if (!Double.isNaN(reflectionCorrection)) {
                      TravelTime.tt += reflectionCorrection;
                      if (TravelTime.phaseCode.equals("pwP")) {
                        delTT = -1;
                      }
                    }
                  }
                }
              }
            }

            // Add auxiliary information.
            addPhaseStatistics(
                TravelTime.phaseCode,
                sourceRecieverDistances[i],
                upGoingBounceDistance,
                TravelTime,
                upGoing);
          }

          // If there was no pwP, get rid of the estimate.
          if (delTT >= 0) {
            ttList.removePhase(delTT);
            delTT = -1;
          }

          lastTT = ttList.getNumPhases();
        }
      }
    }

    // Sort the arrivals into increasing time order, filter, etc.
    ttList.finalizeTravelTimes(isTectonicSource, returnBackBranches);
    return ttList;
  }

  /**
   * Function to compute the elevation correction for one phase.
   *
   * @param phaseCode A String containing the phase code
   * @param recieverElevation A double containing the reciever elevation in kilometers
   * @param rayParameter A double containing the ray parameter in seconds/degree
   * @return A double containing the elevation correction in seconds
   */
  public double elevationCorrection(
      String phaseCode, double recieverElevation, double rayParameter) {
    // Don't do an elevation correction for surface waves.
    if (phaseCode.startsWith("L")) {
      return 0d;
    }

    // Otherwise, the correction depends on the phase type at the station.
    char type = TauUtilities.arrivalType(phaseCode);
    if (type == 'P') {
      return TauUtilities.elevationCorrection(
          recieverElevation,
          TauUtilities.DEFVP,
          rayParameter / modelConversions.getDegreesToKilometers());
    } else if (type == 'S') {
      return TauUtilities.elevationCorrection(
          recieverElevation,
          TauUtilities.DEFVS,
          rayParameter / modelConversions.getDegreesToKilometers());
    } else {
      return 0d; // This should never happen
    }
  }

  /**
   * Function to compute the ellipticity correction for one phase.
   *
   * @param sourceLatitude A double containing the source (earthquake hypocenter) geographic
   *     latitude in degrees
   * @param sourceDepth A double containing the source (earthquake hypocenter) depth in kilometers
   * @param recieverDistance A double containing the source-receiver distance in degrees
   * @param recieverAzimuth A double containing the receiver azimuth from the source in degrees
   * @param isUpGoing A boolean flag, true if the phase is an up-going P or S
   * @return A double containing the ellipticity correction in seconds
   */
  public double ellipticityCorrection(
      double sourceLatitude,
      double sourceDepth,
      double recieverDistance,
      double recieverAzimuth,
      boolean isUpGoing) {
    // Do the interpolation.
    if (flags.getEllipticityCorrections() == null) {
      return 0d;
    } else {
      // There's a special case for the Ellipticity of up-going P
      // and S waves.
      if (isUpGoing) {
        return flags.upEllipticity.getEllipticityCorrection(
            sourceLatitude, sourceDepth, recieverDistance, recieverAzimuth);
        // Otherwise, it's business as usual.
      } else {
        return flags
            .getEllipticityCorrections()
            .getEllipticityCorrection(
                sourceLatitude, sourceDepth, recieverDistance, recieverAzimuth);
      }
    }
  }

  /**
   * Function to compute the bounce point correction due to the reflection occurring at some
   * elevation other than zero. This will provide a positive correction for bounces under mountains
   * and a negative correction for bounces at the bottom of the ocean. Note that pwP is a very
   * special case as it will only exist if pP bounces at the bottom of the ocean.
   *
   * @param phaseCode A string containing the phase code
   * @param branchReference A BranchDataReference object containing the branch data reference object
   * @param sourceLatitude A double containign the geographic source latitude in degrees
   * @param sourceLongitude A double containign the source longitude in degrees
   * @param recieverDistance A double containing the source-receiver distance in degrees
   * @param recieverAzimuth A double containing the receiver azimuth from the source in degrees
   * @param rayParameter A double containing the ray parameter in seconds per degree
   * @param upGoingBounceDistance A double containing the source-bounce point distance in degrees
   *     for an up-going ray
   * @param downGoingBounceDistance A double containing the source-bounce point distance in degrees
   *     for a down-going ray
   * @return A double containing the bounce point correction in seconds
   */
  public double reflectionCorrection(
      String phaseCode,
      BranchDataReference branchReference,
      double sourceLatitude,
      double sourceLongitude,
      double recieverDistance,
      double recieverAzimuth,
      double rayParameter,
      double upGoingBounceDistance,
      double downGoingBounceDistance) {

    // Get the distance to the bounce point by type.
    double reflectionDistance;
    switch (branchReference.getReflectionPhaseCode()) {
        // For pP etc., we just need to trace the initial up-going ray.
      case "pP":
      case "sP":
      case "pS":
      case "sS":
        reflectionDistance = upGoingBounceDistance;
        break;
        // For PP etc., we need to tract the initial down-going ray.
      case "PP":
      case "SS":
        reflectionDistance = 0.5d * (recieverDistance - upGoingBounceDistance);
        break;
        // For converted phases, we need to track the initial down-going
        // ray
      case "SP":
      case "PS":
        reflectionDistance = downGoingBounceDistance;
        break;
        // Hopefully, this can never happen.
      default:
        System.out.println("Unknown bounce type!");
        return Double.NaN;
    }

    // Project the initial part of the ray to the bounce point.
    double reflectionLatitude =
        TauUtilities.projectLatitude(
            sourceLatitude, sourceLongitude, reflectionDistance, recieverAzimuth);
    double reflectionLongitude = TauUtilities.projLon;

    // Get the elevation at that point.
    double recieverElevation =
        allBranchReference
            .getAuxTTData()
            .getBouncePointTopography()
            .getElev(reflectionLatitude, reflectionLongitude);

    // Do the correction.
    switch (branchReference.getConvertedPhaseCode()) {
        // Handle all reflecting P phases.
      case "PP":
        // pwP is a very special case.
        if (phaseCode.equals("pwP")) {
          if (recieverElevation <= -1.5d) {
            /* Like pP, we need a negative correction for bouncing at the
             * bottom of the ocean , but also a positive correction for
             * the two way transit of the water layer.  The 4.67 seconds
             * constant at the end compensates for the default delay of pwP behind
             * pP. */
            return 2d
                    * (TauUtilities.elevationCorrection(
                            recieverElevation,
                            TauUtilities.DEFVP,
                            rayParameter / modelConversions.getDegreesToKilometers())
                        - TauUtilities.elevationCorrection(
                            recieverElevation,
                            TauUtilities.DEFVW,
                            rayParameter / modelConversions.getDegreesToKilometers()))
                - 4.67d;
          } else {
            // If we're not under water, there is no pwP.
            return Double.NaN;
          }
          // This is the normal case.
        } else {
          return 2d
              * TauUtilities.elevationCorrection(
                  recieverElevation,
                  TauUtilities.DEFVP,
                  rayParameter / modelConversions.getDegreesToKilometers());
        }
        // Handle all converted phases.
      case "PS":
      case "SP":
        return TauUtilities.elevationCorrection(
                recieverElevation,
                TauUtilities.DEFVP,
                rayParameter / modelConversions.getDegreesToKilometers())
            + TauUtilities.elevationCorrection(
                recieverElevation,
                TauUtilities.DEFVS,
                rayParameter / modelConversions.getDegreesToKilometers());
        // Handle all reflecting S phases.
      case "SS":
        return 2d
            * TauUtilities.elevationCorrection(
                recieverElevation,
                TauUtilities.DEFVS,
                rayParameter / modelConversions.getDegreesToKilometers());
        // Again, this should never happen.
      default:
        System.out.println(
            "Impossible phase conversion: " + branchReference.getConvertedPhaseCode());
        return Double.NaN;
    }
  }

  /**
   * Function to add the phase statistics and phase flags.
   *
   * @param phaseCode A string containing the phase code
   * @param sourceRecieverDistance A double containing the source-receiver distance in radians
   * @param upGoingBounceDistance A double containing the surface focus correction in degrees
   * @param TravelTime A TravelTimeData object to update
   * @param upGoing A boolean flag, true if the phase is an up-going P or S
   */
  protected void addPhaseStatistics(
      String phaseCode,
      double sourceRecieverDistance,
      double upGoingBounceDistance,
      TravelTimeData TravelTime,
      boolean upGoing) {

    // Add the phase statistics.  First, convert distance to degrees
    double distance = Math.toDegrees(sourceRecieverDistance);

    // Apply the surface focus correction.
    if (phaseCode.charAt(0) == 'p' || phaseCode.charAt(0) == 's') {
      // For surface reflections, just subtract the up-going distance.
      distance = Math.max(distance - upGoingBounceDistance, 0.01d);
    } else if (phaseCode.charAt(0) != 'L') {
      // For a down-going ray.
      distance = distance + upGoingBounceDistance;
    }

    // No statistics for phase that wrap around the Earth.
    double spread;
    double observability;
    double spreadDerivative;
    if (distance >= 360d || flags.getPhaseStatistics() == null) {
      spread = TauUtilities.DEFSPREAD;
      observability = TauUtilities.DEFOBSERV;
      spreadDerivative = 0d;
    } else {
      // Wrap distances greater than 180 degrees.
      if (distance > 180d) {
        distance = 360d - distance;
      }
      // Do the interpolation.
      if (TravelTime.corrTt) {
        TravelTime.tt += flags.getPhaseStatistics().getPhaseBias(distance);
      }
      spread = flags.getPhaseStatistics().getPhaseSpread(distance, upGoing);
      observability = flags.getPhaseStatistics().getPhaseObservability(distance, upGoing);
      spreadDerivative = flags.getPhaseStatistics().getSpreadDerivative(distance, upGoing);
    }

    // Add statistics.
    TravelTime.addStats(spread, observability, spreadDerivative);

    // Add flags.
    TravelTime.addFlags(
        flags.PhaseGroup,
        flags.auxGroup,
        flags.isRegionalPhase,
        flags.isDepth,
        flags.canUse,
        flags.dis);
  }

  /**
   * Function to compute the distance and travel-time for one surface focus ray.
   *
   * <p>Note that the travel time is retrieved by the getCorrectedTravelTime() function
   *
   * @param phaseCode A string containing the phase code
   * @param rayParameter A double containing the desired ray parameter in seconds per degree
   * @return A double containing the source-receiver distance in degrees
   * @throws PhaseNotFoundException If the desired arrival doesn't exist
   */
  public double computeSurfaceRayDistance(String phaseCode, double rayParameter)
      throws PhaseNotFoundException {
    String tmpCode;
    if (phaseCode.contains("bc")) {
      tmpCode = TauUtilities.phSeg(phaseCode) + "ab";
    } else {
      tmpCode = phaseCode;
    }

    for (lastBranchIndex = 0; lastBranchIndex < branches.length; lastBranchIndex++) {
      if (tmpCode.equals(branches[lastBranchIndex].getCorrectedPhaseCode())) {
        double tcorr = branches[lastBranchIndex].computeOneRay(rayParameter);

        if (!Double.isNaN(tcorr)) {
          return tcorr;
        }
      }
    }

    throw new PhaseNotFoundException(tmpCode);
  }

  /**
   * Function to compute the distance and travel-time for the portion of a surface focus ray cut off
   * by the source depth. This provides the distance needed to correct a down-going ray to surface
   * focus.
   *
   * <p>Note that the travel time is retrieved by the getCorrectedTravelTime() function
   *
   * @param phaseCode A string containing the phase code
   * @param rayParameter A double containing the desired ray parameter in seconds per degree
   * @return A double containing the source-reciever distance cut off in degrees
   * @throws PhaseNotFoundException If the up-going arrival doesn't exist
   */
  public double computeUpRayCutoff(String phaseCode, double rayParameter)
      throws PhaseNotFoundException {
    char type = phaseCode.charAt(0);
    if (type == 'p' || type == 'P') {
      lastBranchIndex = lastUpgoingPBranchIndex;
    } else if (type == 's' || type == 'S') {
      lastBranchIndex = lastUpgoingSBranchIndex;
    } else {
      throw new PhaseNotFoundException(phaseCode);
    }

    if (lastBranchIndex < 0) {
      return 0d;
    } else {
      return branches[lastBranchIndex].computeOneRay(rayParameter);
    }
  }

  /**
   * Function to get the corrected travel-time computed by the last call to
   * computeSurfaceRayDistance or computeUpRayCutoff.
   *
   * @return A double containing the corrected travel-time in seconds
   */
  public double getCorrectedTravelTime() {
    return branches[lastBranchIndex].getCorrectedTravelTime();
  }

  /** Function to find the indices of the up-going P and S phases. */
  private void findUpBranchIndices() {
    // Initialize the pointers just in case.
    lastUpgoingPBranchIndex = -1;
    lastUpgoingSBranchIndex = -1;

    // Find the up-going P type branch.
    for (int j = 0; j < branches.length; j++) {
      if (branches[j].getCorrectedBranchExists()
          && branches[j].getBranchReference().getIsBranchUpGoing()
          && branches[j].getBranchReference().getCorrectionPhaseType()[0] == 'P') {
        lastUpgoingPBranchIndex = j;
        break;
      }
    }

    // Find the up-going S type branch.
    for (int j = 0; j < branches.length; j++) {
      if (branches[j].getCorrectedBranchExists()
          && branches[j].getBranchReference().getIsBranchUpGoing()
          && branches[j].getBranchReference().getCorrectionPhaseType()[0] == 'S') {
        lastUpgoingSBranchIndex = j;
        break;
      }
    }
  }

  /**
   * Function to expand the list of desired phases using group codes. Note that the output list can
   * be a combination of phase codes, group codes, and special keywords.
   *
   * @param phasesToUse An array of strings containing the input phase list
   */
  private void expandPhaseList(String[] phasesToUse) {
    // Start over with a new keyword list.
    if (phaseList != null) {
      phaseList.clear();
    } else {
      phaseList = new ArrayList<String>();
    }

    // Loop over the phase list entries expanding into a list of keywords.
    for (int j = 0; j < phasesToUse.length; j++) {
      String phaseGroup = allBranchReference.getAuxTTData().findGroup(phasesToUse[j]);

      // If the phase and group are the same, the list is generic.
      if (phaseGroup.equals(phasesToUse[j])) {
        phaseList.add(phaseGroup);

        if (allBranchReference.getAuxTTData().getIsPrimaryGroup()) {
          String auxGroup = allBranchReference.getAuxTTData().findCompGroup(phaseGroup);

          if (auxGroup != null) {
            phaseList.add(auxGroup);
          }
        }
        // Otherwise, just keep the phase code or special keyword.
      } else {
        phaseList.add(phasesToUse[j]);
      }
    }
  }

  /**
   * Function to test a phase code against phase list keywords. Note that this implementation is
   * somewhat different from the Fortran Brnset, but follows the same ideas and is somewhat more
   * precise. This is because the Fortran implementation predated the phase groups and the phase
   * groups were developed and integrated with the Locator rather than the travel-time package.
   *
   * @param phaseCode A string containing the phase code
   * @return True if the phase code matches any of the phase list keywords
   */
  private boolean isPhaseInList(String phaseCode) {
    for (int j = 0; j < phaseList.size(); j++) {
      String keyword = phaseList.get(j);

      // Local phases.
      if (keyword.toLowerCase().equals("ploc")) {
        if (allBranchReference.getAuxTTData().isRegionalPhase(phaseCode)) {
          return true;
        }
        // Depth phases.
      } else if (keyword.toLowerCase().equals("pdep")) {
        if (allBranchReference.getAuxTTData().isDepthPhase(phaseCode)) {
          return true;
        }
        // Basic phases (everything that can be used in a location).
      } else if (keyword.toLowerCase().equals("basic")) {
        if (allBranchReference.getAuxTTData().usePhaseForLocation(phaseCode)) {
          return true;
        }
        // Otherwise, see if we want this specific (or generic) phase.
      } else {
        if (keyword.equals(phaseCode)
            || keyword.equals(allBranchReference.getAuxTTData().findGroup(phaseCode))) {
          return true;
        }
      }
    }

    return false;
  }

  /** Function to print global or header data for debugging purposes. */
  public void dumpHead() {
    String headerString = "\n     " + allBranchReference.getEarthModelName();

    headerString +=
        String.format(
            "Normalization: xNorm =%11.4e  vNorm =%11.4e  " + "tNorm =%11.4e\n",
            modelConversions.getDistanceNormalization(),
            modelConversions.getVelocityNormalization(),
            modelConversions.getTauTTNormalization());

    headerString +=
        String.format(
            "Boundaries: zUpperMantle =%7.1f  zMoho =%7.1f  " + "zConrad =%7.1f\n",
            modelConversions.getUpperMantleDepth(),
            modelConversions.getMohoDepth(),
            modelConversions.getConradDepth());

    headerString +=
        String.format(
            "Derived: rSurface =%8.1f  zNewUp = %7.1f  "
                + "dTdDel2P =%11.4e  ttDepthDerivative = %11.4e\n",
            modelConversions.getSurfaceRadius(),
            modelConversions.getUpGoingReplacementDepth(),
            modelConversions.get_dTdDelta(),
            ttDepthDerivative);

    LOGGER.fine(headerString);
  }

  /**
   * Function to print a summary table of branch information similar to the FORTRAN Ttim range list.
   *
   * @param useful A boolean flag, if true, only print "useful" crustal phases
   */
  public void logTable(boolean useful) {
    String tableString =
        String.format(
            "Summary Branch Table: \nPhase          pRange          xRange    "
                + "pCaustic difLim    Flags\n");

    for (int j = 0; j < branches.length; j++) {
      tableString += branches[j].forTable(useful);
    }

    LOGGER.fine(tableString);
  }

  /**
   * Function to count the number of branches
   *
   * @param useful A boolean flag, if true, only count "useful" crustal phases
   * @return An integer containing the number of branches
   */
  public int getBranchCount(boolean useful) {
    if (useful == false) {
      return (branches.length);
    } else {
      int count = 0;

      for (int j = 0; j < branches.length; j++) {
        if (branches[j].getIsPhaseUseless() == false) {
          count++;
        }
      }

      return (count);
    }
  }

  /**
   * Function to print model parameters for debugging purposes.
   *
   * @param waveType A char containing the wave type ('P' = compressional, 'S' = shear)
   * @param nice A boolean flag, if true print the model in dimensional units
   */
  public void dumpMod(char waveType, boolean nice) {
    if (waveType == 'P') {
      allBranchReference.getEarthModelP().dumpMod(nice);
    } else if (waveType == 'S') {
      allBranchReference.getEarthModelS().dumpMod(nice);
    }
  }

  /**
   * Function to print data for one travel-time branch for debugging purposes.
   *
   * @param branchIndex An integer containing the branch number index
   * @param full A boolean flag,iif true, print the detailed branch specification as well
   * @param all A boolean flag, If true print even more specifications
   * @param scientificNotation A boolean flag, if true, print in scientific notation
   */
  public void dumpBranchInformation(
      int branchIndex, boolean full, boolean all, boolean scientificNotation) {
    branches[branchIndex].dumpBranchInformation(full, all, scientificNotation, true, false);
  }

  /**
   * Function to print data for one travel-time phase code for debugging purposes.
   *
   * @param phaseCode A string containing the phase code
   * @param full A boolean flag, if true, print the detailed branch specification as well
   * @param all A boolean flag, If true print even more specifications
   * @param scientificNotation A boolean flag, if true, print in scientific notation
   */
  public void dumpBranchInformation(
      String phaseCode, boolean full, boolean all, boolean scientificNotation) {
    for (int j = 0; j < branches.length; j++) {
      if (branches[j].getCorrectedPhaseCode().equals(phaseCode)) {
        branches[j].dumpBranchInformation(full, all, scientificNotation, true, false);
      }
    }
  }

  /**
   * Function to print data for all travel-time branches for debugging purposes.
   *
   * @param full A boolean flag, if true, print the detailed branch specification as well
   * @param all A boolean flag, If true print even more specifications
   * @param scientificNotation A boolean flag, if true, print in scientific notation
   * @param returnAllPhases If false, only print "useful" crustal phases
   */
  public void dumpBranchInformation(
      boolean full, boolean all, boolean scientificNotation, boolean returnAllPhases) {
    for (int j = 0; j < branches.length; j++) {
      branches[j].dumpBranchInformation(full, all, scientificNotation, returnAllPhases, false);
    }
  }

  /**
   * Function to print data for all travel-time branches that have at least one caustic. This turns
   * out to be particularly useful for finding instabilities in the interpolation.
   *
   * @param full A boolean flag, if true, print the detailed branch specification as well
   * @param all A boolean flag, If true print even more specifications
   * @param scientificNotation A boolean flag, if true, print in scientific notation
   * @param returnAllPhases A boolean flag, if false, only print "useful" crustal phases
   */
  public void dumpCaustics(
      boolean full, boolean all, boolean scientificNotation, boolean returnAllPhases) {
    for (int j = 0; j < branches.length; j++) {
      branches[j].dumpBranchInformation(full, all, scientificNotation, returnAllPhases, true);
    }
  }

  /**
   * Function to print data for one travel-time segment for debugging purposes.
   *
   * @param segmentPhase A string containing the generic segment phase code
   * @param full A boolean flag, if true, print the detailed branch specification as well
   * @param all A boolean flag, If true print even more specifications
   * @param scientificNotation A boolean flag, if true, print in scientific notation
   * @param returnAllPhases A boolean flag, if false, only print "useful" crustal phases
   */
  public void dumpSegmentInformation(
      String segmentPhase,
      boolean full,
      boolean all,
      boolean scientificNotation,
      boolean returnAllPhases) {
    for (int j = 0; j < branches.length; j++) {
      if (branches[j].getGenericPhaseCode().equals(segmentPhase))
        branches[j].dumpBranchInformation(full, all, scientificNotation, returnAllPhases, false);
    }
  }

  /**
   * Function to print data for one corrected up-going branch for debugging purposes.
   *
   * @param waveType A char containing the wave type ('P' = compressional, 'S' = shear)
   * @param full A boolean flag, if true, print the detailed branch specification as well
   */
  public void dumpCorrUp(char waveType, boolean full) {
    if (waveType == 'P') {
      upgoingPBranch.dumpCorrUp(full);
    } else if (waveType == 'S') {
      upgoingSBranch.dumpCorrUp(full);
    }
  }

  /**
   * Function to print data for one decimated up-going branch for debugging purposes.
   *
   * @param waveType A char containing the wave type ('P' = compressional, 'S' = shear)
   * @param full A boolean flag, if true, print the detailed branch specification as well
   */
  public void dumpDecUp(char waveType, boolean full) {
    if (waveType == 'P') {
      upgoingPBranch.dumpDecUp(full);
    } else if (waveType == 'S') {
      upgoingSBranch.dumpDecUp(full);
    }
  }
}
