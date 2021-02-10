package gov.usgs.traveltime;

import gov.usgs.traveltime.tables.TauIntegralException;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Umbrella storage for all volatile branch level travel-time data.
 *
 * @author Ray Buland
 */
public class AllBrnVol {

  BrnDataVol[] branches; // Volatile branch data
  ModDataVol pModel, sModel; // Volatile model data
  UpDataVol pUp, sUp; // Volatile up-going branch data
  double dSource; // Dimensional source depth in kilometers
  double zSource; // Flat Earth source depth
  double dTdDepth; // Derivative of travel time with respect to depth
  double eqLat; // Geographical earthquake latitude in degrees
  double eqLon; // Earthquake longitude in degrees
  double eqDepth; // Earthquake depth in kilometers
  double staDelta; // Source-receiver distance in degrees
  double staAzim; // Receiver azimuth at the source in degrees
  boolean complexSession; // True if this is a "complex" session
  boolean complexRequest; // True if this is a "complex" request
  boolean badDepth; // True if the depth is out of range
  boolean badDelta; // True if the distance is out of range
  boolean returnAllPhases; // Do all phases (including useless ones)
  boolean returnBackBranches; // Return back branches as well as primary arrivals
  boolean tectonic; // True if the source is in a tectonic province
  boolean rstt; // Use RSTT for local phases
  double lastDepth = Double.NaN; // Last depth computed in kilometers
  AllBrnRef ref;
  ModConvert cvt;
  TtFlags flags;
  Spline spline;
  ArrayList<String> expList;
  int lastBrn = -1, upBrnP = -1, upBrnS = -1;

  /** Private logging object. */
  private static final Logger LOGGER = Logger.getLogger(AllBrnVol.class.getName());

  @Override
  public String toString() {
    return ref.modelName
        + " d="
        + lastDepth
        + " cmplx="
        + (complexSession ? "T" : "F")
        + " eqcoord="
        + eqLat
        + " "
        + eqLon
        + " del="
        + staDelta
        + " flgs="
        + (!returnAllPhases ? "U" : "u")
        + (!returnBackBranches ? "b" : "B")
        + (tectonic ? "T" : "t")
        + (rstt ? "R" : "r");
  }

  /**
   * Set up volatile copies of data that changes with depth.
   *
   * @param ref The reference data source
   */
  public AllBrnVol(AllBrnRef ref) {
    this.ref = ref;
    this.cvt = ref.cvt;

    // Set up the volatile piece of the model.
    pModel = new ModDataVol(ref.pModel, cvt);
    sModel = new ModDataVol(ref.sModel, cvt);
    // Set up the up-going branch data.
    pUp = new UpDataVol(ref.pUp, pModel, sModel, cvt);
    sUp = new UpDataVol(ref.sUp, sModel, pModel, cvt);

    // Set up the branch data.
    branches = new BrnDataVol[ref.branches.length];
    spline = new Spline();
    for (int j = 0; j < branches.length; j++) {
      branches[j] = new BrnDataVol(ref.branches[j], pUp, sUp, cvt, ref.auxtt, spline);
    }
  }

  /**
   * Get the earth model name.
   *
   * @return Earth model name
   */
  public String getEarthModel() {
    return ref.modelName;
  }

  /**
   * Return a pointer to the reference portion of all branches.
   *
   * @return The reference
   */
  public AllBrnRef getAllBrnRef() {
    return ref;
  }

  /**
   * Set up a new session. Note that this sets up the complex session parameters of use to the
   * travel-time package.
   *
   * @param latitude Source geographical latitude in degrees
   * @param longitude Source longitude in degrees
   * @param depth Source depth in kilometers
   * @param phList Array of phase use commands
   * @param returnAllPhases If false, only provide "useful" crustal phases
   * @param returnBackBranches If false, suppress back branches
   * @param tectonic If true, map Pb and Sb onto Pg and Sg
   * @param rstt If true, use RSTT crustal phases
   * @throws BadDepthException If the depth is out of range
   * @throws TauIntegralException If the tau integral doesn't make sense
   */
  public void newSession(
      double latitude,
      double longitude,
      double depth,
      String[] phList,
      boolean returnAllPhases,
      boolean returnBackBranches,
      boolean tectonic,
      boolean rstt)
      throws BadDepthException, TauIntegralException {
    // See if the epicenter makes sense.
    if (!Double.isNaN(latitude)
        && latitude >= -90d
        && latitude <= 90d
        && !Double.isNaN(longitude)
        && longitude >= -180d
        && longitude <= 180d) {
      complexSession = true;
      eqLat = latitude;
      eqLon = longitude;
      // If not, this can only be a simple request.
    } else {
      complexSession = false;
      eqLat = Double.NaN;
      eqLon = Double.NaN;
    }
    setSession(depth, phList, returnAllPhases, returnBackBranches, tectonic, rstt);
  }

  /**
   * Set up a new session. Note that this just sets up the simple session parameters of use to the
   * travel-time package.
   *
   * @param depth Source depth in kilometers
   * @param phList Array of phase use commands
   * @param returnAllPhases If false, only provide "useful" crustal phases
   * @param returnBackBranches If false, suppress back branches
   * @param tectonic If true, map Pb and Sb onto Pg and Sg
   * @param rstt If true, use RSTT crustal phases
   * @throws BadDepthException If the depth is out of range
   * @throws TauIntegralException If the tau integral doesn't make sense
   */
  public void newSession(
      double depth,
      String[] phList,
      boolean returnAllPhases,
      boolean returnBackBranches,
      boolean tectonic,
      boolean rstt)
      throws BadDepthException, TauIntegralException {
    complexSession = false;
    eqLat = Double.NaN;
    eqLon = Double.NaN;
    setSession(depth, phList, returnAllPhases, returnBackBranches, tectonic, rstt);
  }

  /**
   * Set up a new session. Note that this just sets up the simple session parameters of use to the
   * travel-time package.
   *
   * @param depth Source depth in kilometers
   * @param phList Array of phase use commands
   * @param returnAllPhases If false, only provide "useful" crustal phases
   * @param returnBackBranches If false, suppress back branches
   * @param tectonic If true, map Pb and Sb onto Pg and Sg
   * @param rstt If true, use RSTT crustal phases
   * @throws BadDepthException If the depth is out of range
   * @throws TauIntegralException If the tau integral doesn't make sense
   */
  private void setSession(
      double depth,
      String[] phList,
      boolean returnAllPhases,
      boolean returnBackBranches,
      boolean tectonic,
      boolean rstt)
      throws BadDepthException, TauIntegralException {
    char tagBrn;
    double xMin;
    boolean all;

    // Make sure the depth is in range.
    if (depth >= 0d && depth <= TauUtil.MAXDEPTH) {
      badDepth = false;
      // Remember the session control flags.
      this.returnAllPhases = returnAllPhases;
      this.returnBackBranches = returnBackBranches;
      this.tectonic = tectonic;
      this.rstt = rstt;

      if (depth != lastDepth) {
        // Set up the new source depth.
        dSource = Math.max(depth, 0.011d);
        // The interpolation gets squirrelly for very shallow sources.
        if (depth < 0.011d) {
          // Note that a source depth of zero is intrinsically different
          // from any positive depth (i.e., no up-going phases).
          zSource = 0d;
          dTdDepth = 1d / cvt.vNorm;
        } else {
          zSource = Math.min(Math.log(Math.max(1d - dSource * cvt.xNorm, TauUtil.DMIN)), 0d);
          dTdDepth = 1d / (cvt.vNorm * (1d - dSource * cvt.xNorm));
        }

        // See if we want all phases.
        all = false;
        if (phList != null) {
          if (phList.length > 0) {
            // Check for the "all" keyword (overrides everything else).
            for (int j = 0; j < phList.length; j++) {
              if (phList[j].toLowerCase().equals("all")) {
                all = true;
                break;
              }
            }
          } else all = true; // The phase list is empty.
        } else all = true; // The phase list is null.

        // Initialize the branch compute flags.
        if (all) {
          // Turn everything on except possibly useless phases.
          for (int j = 0; j < branches.length; j++) {
            // If we only want useful phases and this one is useless, just
            // turn it off.
            if (!returnAllPhases && ref.branches[j].isUseless) {
              branches[j].setCompute(false);
            }
            // Otherwise, we're good to go.
            else {
              branches[j].setCompute(true);
            }
          }
        } else {
          // Compare branch phase codes against the phase list.
          expandList(phList);
          for (int j = 0; j < branches.length; j++) {
            // See if this phase is selected (unless it's NEIC-useless).
            if (returnAllPhases || !ref.branches[j].isUseless) {
              branches[j].setCompute(testList(ref.branches[j].phCode));
            } else {
              branches[j].setCompute(false);
            }
          }
        }

        // Correct the up-going branch data.
        pUp.newDepth(zSource);
        sUp.newDepth(zSource);

        // To correct each branch we need a few depth dependent pieces.
        xMin = cvt.xNorm * Math.min(Math.max(2d * dSource, 2d), 25d);
        if (dSource <= cvt.zConrad) {
          tagBrn = 'g';
        } else if (dSource <= cvt.zMoho) {
          tagBrn = 'b';
        } else if (dSource <= cvt.zUpperMantle) {
          tagBrn = 'n';
        } else {
          tagBrn = ' ';
        }
        // Now correct each branch.
        for (int j = 0; j < branches.length; j++) {
          branches[j].depthCorr(zSource, dTdDepth, xMin, tagBrn);
        }
        lastDepth = depth;
      }
      //		dumpBrn("pS", true, true, false);
    } else {
      badDepth = true;
    }
  }

  /**
   * Get the arrival times from all branches for a "complex" request (i.e., including latitude and
   * longitude). This will include the ellipticity and bounce point corrections as well as the
   * elevation correction.
   *
   * @param staLat Receiver geographic latitude in degrees
   * @param staLon Receiver longitude in degrees
   * @param elev Station elevation in kilometers
   * @param delta Source receiver distance desired in degrees
   * @param azimuth Receiver azimuth at the source in degrees
   * @return An array list of travel times
   */
  public TTime getTT(double staLat, double staLon, double elev, double delta, double azimuth) {

    // If this is a simple session, the request can only be simple.
    if (complexSession) {
      complexRequest = true;
    } else {
      complexRequest = false;
    }

    // See if the distance makes sense.
    if ((!Double.isNaN(delta)) && (delta >= 0d) && (delta <= 180d)) {
      badDelta = false;
      staDelta = delta;

      if (complexRequest) {
        // OK, how about the azimuth.
        if ((!Double.isNaN(azimuth)) && (azimuth >= 0d) && (azimuth <= 360d)) {
          staAzim = azimuth;
        } else {
          // If the station coordinates makes sense, we can fix it.
          if ((!Double.isNaN(staLat))
              && (staLat >= -90d)
              && (staLat <= 90d)
              && (!Double.isNaN(staLon))
              && (staLon >= -180d)
              && (staLon <= 180d)) {
            staDelta = TauUtil.delAz(eqLat, eqLon, staLat, staLon);
            staAzim = TauUtil.azimuth;
          } else {
            complexRequest = false;
            staAzim = Double.NaN;
          }
        }
      } else {
        staAzim = Double.NaN;
      }

      // The distance is bad, see if we can fix it.
    } else {
      // See if the station coordinates make sense.
      if ((!Double.isNaN(staLat))
          && (staLat >= -90d)
          && (staLat <= 90d)
          && (!Double.isNaN(staLon))
          && (staLon >= -180d)
          && (staLon <= 180d)) {
        badDelta = false;
        staDelta = TauUtil.delAz(eqLat, eqLon, staLat, staLon);
        staAzim = TauUtil.azimuth;
      } else {
        badDelta = true;
      }
    }

    // If the elevation doesn't make sense, just set it to zero.
    if ((!Double.isNaN(elev)) && (elev >= TauUtil.MINELEV) && (elev <= TauUtil.MAXELEV)) {
      return doTT(elev);
    } else {
      return doTT(0d);
    }
  }

  /**
   * Get the arrival times from all branches for a "simple" request (i.e., without latitude and
   * longitude. This will only include the elevation correction.
   *
   * @param elev Station elevation in kilometers
   * @param delta Source receiver distance desired in degrees
   * @return An array list of travel times
   */
  public TTime getTT(double elev, double delta) {
    complexRequest = false;
    // See if the distance makes sense.
    if (!Double.isNaN(delta) && delta >= 0d && delta <= 180d) {
      badDelta = false;
      staDelta = delta;
      staAzim = Double.NaN;
    } else {
      badDelta = true;
    }
    // If the elevation doesn't make sense, just set it to zero.
    if (!Double.isNaN(elev) && elev >= TauUtil.MINELEV && elev <= TauUtil.MAXELEV) {
      return doTT(elev);
    } else {
      return doTT(0d);
    }
  }

  /**
   * Get the arrival times from all branches including all applicable travel-time corrections.
   *
   * @param elev Station elevation in kilometers
   * @return An array list of travel times
   */
  private TTime doTT(double elev) {
    boolean upGoing;
    int lastTT, delTT = -1;
    double delCorUp, delCorDn, retDelta = Double.NaN, retAzim = Double.NaN, reflCorr = Double.NaN;
    double[] xs;
    String tmpCode;
    TTime ttList;
    //	TTime rsttList;
    TTimeData tTime;

    if (badDepth || badDelta) return null;

    ttList = new TTime(dSource, staDelta);
    lastTT = 0;
    // The desired distance might translate to up to three
    // different distances (as the phases wrap around the Earth).
    xs = new double[3];
    xs[0] = Math.abs(Math.toRadians(staDelta)) % (2d * Math.PI);
    if (xs[0] > Math.PI) {
      xs[0] = 2d * Math.PI - xs[0];
    }
    xs[1] = 2d * Math.PI - xs[0];
    xs[2] = xs[0] + 2d * Math.PI;
    if (Math.abs(xs[0]) <= TauUtil.DTOL) {
      xs[0] = TauUtil.DTOL;
      xs[2] = -10d;
    }
    if (Math.abs(xs[0] - Math.PI) <= TauUtil.DTOL) {
      xs[0] = Math.PI - TauUtil.DTOL;
      xs[1] = -10d;
    }

    // Set up the correction to surface focus.
    findUpBrn();
    // Set up distance and azimuth for retrograde phases.
    if (complexRequest) {
      retDelta = 360d - staDelta;
      retAzim = 180d + staAzim;
      if (retAzim > 360d) {
        retAzim -= 360d;
      }
    }

    // Go get the arrivals.  This is a little convoluted because
    // of the correction to surface focus needed for the statistics.
    for (int j = 0; j < branches.length; j++) {
      // Loop over possible distances.
      for (int i = 0; i < 3; i++) {
        branches[j].getTT(i, xs[i], dSource, returnAllPhases, ttList);
        // We have to add the phase statistics and other corrections at this level.
        if (ttList.getNumPhases() > lastTT) {
          for (int k = lastTT; k < ttList.getNumPhases(); k++) {
            tTime = ttList.getPhase(k);
            flags = ref.auxtt.findFlags(tTime.phCode);
            if (tTime.phCode.equals("pwP")) delTT = k;
            // There's a special case for up-going P and S.
            if (tTime.dTdZ > 0d && (flags.phGroup.equals("P") || flags.phGroup.equals("S"))) {
              upGoing = true;
            } else {
              upGoing = false;
            }
            // Set the correction to surface focus.
            if (tTime.phCode.charAt(0) == 'L') {
              // Travel-time corrections and correction to surface focus
              // don't make sense for surface waves.
              delCorUp = 0d;
            } else {
              // Get the correction.
              try {
                delCorUp = upRay(tTime.phCode, tTime.dTdD);
              } catch (PhaseNotFoundException e) {
                // This should never happen.
                System.out.println(e.toString());
                delCorUp = 0d;
              }
              // This is the normal case.  Do various travel-time corrections.
              if (!TauUtil.noCorr) {
                tTime.tt += elevCorr(tTime.phCode, elev, tTime.dTdD, rstt);
                // If this was a complex request, do the ellipticity and bounce
                // point corrections.
                if (complexRequest) {
                  // The ellipticity correction is straightforward.
                  tTime.tt += ellipCorr(eqLat, dSource, staDelta, staAzim, upGoing);

                  // The bounce point correction is not.  See if there is a bounce.
                  if (branches[j].ref.phRefl != null) {
                    if (ref.auxtt.topoMap != null) {
                      // If so, we may need to do some preliminary work.
                      if (branches[j].ref.phRefl.equals("SP")) {
                        tmpCode = "S";
                      } else if (branches[j].ref.phRefl.equals("PS")) {
                        tmpCode = tTime.phCode.substring(0, tTime.phCode.indexOf('S'));
                      } else {
                        tmpCode = null;
                      }
                      // If we had an SP or PS, get the distance of the first part.
                      if (tmpCode != null) {
                        try {
                          delCorDn = oneRay(tmpCode, tTime.dTdD);
                        } catch (PhaseNotFoundException e) {
                          // This should never happen.
                          System.out.println(e.toString());
                          //				System.out.format("Phase = %-8s delta = %6.2f\n", tTime.phCode,
                          // staDelta);
                          delCorDn = 0.5d * staDelta;
                        }
                      } else {
                        delCorDn = Double.NaN;
                      }
                      // Finally, we can do the bounce point correction.
                      if (tTime.dTdD > 0d) {
                        reflCorr =
                            reflCorr(
                                tTime.phCode,
                                branches[j].ref,
                                eqLat,
                                eqLon,
                                staDelta,
                                staAzim,
                                tTime.dTdD,
                                delCorUp,
                                delCorDn);
                      } else {
                        reflCorr =
                            reflCorr(
                                tTime.phCode,
                                branches[j].ref,
                                eqLat,
                                eqLon,
                                retDelta,
                                retAzim,
                                tTime.dTdD,
                                delCorUp,
                                delCorDn);
                      }
                    } else {
                      reflCorr = Double.NaN;
                    }
                    if (!Double.isNaN(reflCorr)) {
                      tTime.tt += reflCorr;
                      if (tTime.phCode.equals("pwP")) delTT = -1;
                    }
                  }
                }
              }
            }
            // Add auxiliary information.
            addAux(tTime.phCode, xs[i], delCorUp, tTime, upGoing);
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

    // Rough in RSTT.
    /*	if(rstt) {
    	rsttList = addRSTT();
    	if(rsttList != null) {
    		for(int j=0; j<rsttList.size(); j++) {
    			tTime = ttList.get(j);
    			flags = ref.auxtt.findFlags(tTime.phCode);
    			// There's a special case for up-going P and S.
    			if(tTime.dTdZ > 0d && (flags.phGroup.equals("P") ||
    					flags.phGroup.equals("S"))) upGoing = true;
    			else upGoing = false;
    			// Set the correction to surface focus.
    			if(tTime.phCode.charAt(0) == 'L') {
    				// Travel-time corrections and correction to surface focus
    				// don't make sense for surface waves.
    				delCorUp = 0d;
    			} else {
    				// Get the correction.
    				try {
    					delCorUp = upRay(tTime.phCode, tTime.dTdD);
    				} catch (PhaseNotFoundException e) {
    					// This should never happen.
    					e.printStackTrace();
    					delCorUp = 0d;
    				}
    			}
    			// Add auxiliary information.
    			addAux(tTime.phCode, xs[0], delCorUp, tTime, upGoing);
    		}
    		rsttMerge(ttList, rsttList);
    	}
    } */
    // Sort the arrivals into increasing time order, filter, etc.
    ttList.finalizeTravelTimes(tectonic, returnBackBranches);
    return ttList;
  }

  /**
   * Compute the elevation correction for one phase.
   *
   * @param phCode Phase code
   * @param elev Elevation in kilometers
   * @param dTdD Ray parameter in seconds/degree
   * @param rstt True if RSTT is being used for crustal phases
   * @return Elevation correction in seconds
   */
  public double elevCorr(String phCode, double elev, double dTdD, boolean rstt) {
    char type;

    // We don't want any corrections if RSTT is used for regional phases.
    if (rstt && ref.auxtt.phFlags.get(phCode).isRegional) {
      return 0d;
    }
    // Don't do an elevation correction for surface waves.
    if (phCode.startsWith("L")) {
      return 0d;
    }

    // Otherwise, the correction depends on the phase type at the station.
    type = TauUtil.arrivalType(phCode);
    if (type == 'P') {
      return TauUtil.topoCorr(elev, TauUtil.DEFVP, dTdD / cvt.deg2km);
    } else if (type == 'S') {
      return TauUtil.topoCorr(elev, TauUtil.DEFVS, dTdD / cvt.deg2km);
    } else {
      return 0d; // This should never happen
    }
  }

  /**
   * Compute the ellipticity correction for one phase.
   *
   * @param eqLat Hypocenter geographic latitude in degrees
   * @param depth Hypocenter depth in kilometers
   * @param delta Source-receiver distance in degrees
   * @param azimuth Receiver azimuth from the source in degrees
   * @param upGoing True if the phase is an up-going P or S
   * @return Ellipticity correction in seconds
   */
  public double ellipCorr(
      double eqLat, double depth, double delta, double azimuth, boolean upGoing) {
    // Do the interpolation.
    if (flags.ellip == null) {
      return 0d;
    } else {
      // There's a special case for the ellipticity of up-going P
      // and S waves.
      if (upGoing) {
        return flags.upEllip.getEllipCorr(eqLat, depth, delta, azimuth);
        // Otherwise, it's business as usual.
      } else {
        return flags.ellip.getEllipCorr(eqLat, depth, delta, azimuth);
      }
    }
  }

  /**
   * Compute the bounce point correction due to the reflection occurring at some elevation other
   * than zero. This will provide a positive correction for bounces under mountains and a negative
   * correction for bounces at the bottom of the ocean. Note that pwP is a very special case as it
   * will only exist if pP bounces at the bottom of the ocean.
   *
   * @param phCode Phase code
   * @param brnRef Branch data reference object
   * @param eqLat Geographic source latitude in degrees
   * @param eqLon Source longitude in degrees
   * @param delta Source-receiver distance in degrees
   * @param azimuth Receiver azimuth from the source in degrees
   * @param dTdD Ray parameter in seconds per degree
   * @param delCorUp Source-bounce point distance in degrees for an up-going ray
   * @param delCorDn Source-bounce point distance in degrees for a down-going ray
   * @return Bounce point correction in seconds
   */
  public double reflCorr(
      String phCode,
      BrnDataRef brnRef,
      double eqLat,
      double eqLon,
      double delta,
      double azimuth,
      double dTdD,
      double delCorUp,
      double delCorDn) {
    double elev = 0d, refDelta, refLat, refLon;

    // Get the distance to the bounce point by type.
    switch (brnRef.phRefl) {
        // For pP etc., we just need to trace the initial up-going ray.
      case "pP":
      case "sP":
      case "pS":
      case "sS":
        refDelta = delCorUp;
        break;
        // For PP etc., we need to tract the initial down-going ray.
      case "PP":
      case "SS":
        refDelta = 0.5d * (delta - delCorUp);
        break;
        // For converted phases, we need to track the initial down-going
        // ray
      case "SP":
      case "PS":
        refDelta = delCorDn;
        break;
        // Hopefully, this can never happen.
      default:
        System.out.println("Unknown bounce type!");
        return Double.NaN;
    }

    // Project the initial part of the ray to the bounce point.
    refLat = TauUtil.projLat(eqLat, eqLon, refDelta, azimuth);
    refLon = TauUtil.projLon;
    // Get the elevation at that point.
    elev = ref.auxtt.topoMap.getElev(refLat, refLon);

    // Do the correction.
    switch (brnRef.convRefl) {
        // Handle all reflecting P phases.
      case "PP":
        // pwP is a very special case.
        if (phCode.equals("pwP")) {
          if (elev <= -1.5d) {
            /* Like pP, we need a negative correction for bouncing at the
             * bottom of the ocean , but also a positive correction for
             * the two way transit of the water layer.  The 4.67 seconds
             * at the end compensates for the default delay of pwP behind
             * pP. */
            return 2d
                    * (TauUtil.topoCorr(elev, TauUtil.DEFVP, dTdD / cvt.deg2km)
                        - TauUtil.topoCorr(elev, TauUtil.DEFVW, dTdD / cvt.deg2km))
                - 4.67d;
          } else {
            // If we're not under water, there is no pwP.
            return Double.NaN;
          }
          // This is the normal case.
        } else {
          return 2d * TauUtil.topoCorr(elev, TauUtil.DEFVP, dTdD / cvt.deg2km);
        }
        // Handle all converted phases.
      case "PS":
      case "SP":
        return TauUtil.topoCorr(elev, TauUtil.DEFVP, dTdD / cvt.deg2km)
            + TauUtil.topoCorr(elev, TauUtil.DEFVS, dTdD / cvt.deg2km);
        // Handle all reflecting S phases.
      case "SS":
        return 2d * TauUtil.topoCorr(elev, TauUtil.DEFVS, dTdD / cvt.deg2km);
        // Again, this should never happen.
      default:
        System.out.println("Impossible phase conversion: " + brnRef.convRefl);
        return Double.NaN;
    }
  }

  /**
   * Add the phase statistics and phase flags.
   *
   * @param phCode Phase code
   * @param xs Source-receiver distance in radians
   * @param delCorUp Surface focus correction in degrees
   * @param tTime Travel-time object to update
   * @param upGoing True if the phase is an up-going P or S
   */
  protected void addAux(
      String phCode, double xs, double delCorUp, TTimeData tTime, boolean upGoing) {
    double del, spd, obs, dSdD;

    // Add the phase statistics.  First, convert distance to degrees
    del = Math.toDegrees(xs);
    // Apply the surface focus correction.
    if (phCode.charAt(0) == 'p' || phCode.charAt(0) == 's') {
      // For surface reflections, just subtract the up-going distance.
      del = Math.max(del - delCorUp, 0.01d);
    } else if (phCode.charAt(0) != 'L') {
      // For a down-going ray.
      del = del + delCorUp;
    }

    // No statistics for phase that wrap around the Earth.
    if (del >= 360d || flags.ttStat == null) {
      spd = TauUtil.DEFSPREAD;
      obs = TauUtil.DEFOBSERV;
      dSdD = 0d;
    } else {
      // Wrap distances greater than 180 degrees.
      if (del > 180d) {
        del = 360d - del;
      }
      // Do the interpolation.
      if (tTime.corrTt) {
        tTime.tt += flags.ttStat.getBias(del);
      }
      spd = flags.ttStat.getSpread(del, upGoing);
      obs = flags.ttStat.getObserv(del, upGoing);
      dSdD = flags.ttStat.getSpreadDerivative(del, upGoing);
    }
    // Add statistics.
    tTime.addStats(spd, obs, dSdD);
    // Add flags.
    tTime.addFlags(
        flags.phGroup, flags.auxGroup, flags.isRegional, flags.isDepth, flags.canUse, flags.dis);
  }

  /**
   * Compute the distance and travel-time for one surface focus ray.
   *
   * @param phCode Phase code for the desired branch
   * @param dTdD Desired ray parameter in seconds/degree
   * @return Source-receiver distance in degrees
   * @throws PhaseNotFoundException If the desired arrival doesn't exist
   */
  public double oneRay(String phCode, double dTdD) throws PhaseNotFoundException {
    String tmpCode;
    double tcorr;

    if (phCode.contains("bc")) {
      tmpCode = TauUtil.phSeg(phCode) + "ab";
    } else {
      tmpCode = phCode;
    }
    for (lastBrn = 0; lastBrn < branches.length; lastBrn++) {
      if (tmpCode.equals(branches[lastBrn].phCode)) {
        tcorr = branches[lastBrn].oneRay(dTdD);
        if (!Double.isNaN(tcorr)) {
          return tcorr;
        }
      }
    }
    throw new PhaseNotFoundException(tmpCode);
  }

  /**
   * Compute the distance and travel-time for the portion of a surface focus ray cut off by the
   * source depth. This provides the distance needed to correct a down-going ray to surface focus.
   *
   * @param phCode Phase code of the phase being corrected
   * @param dTdD Desired ray parameter in seconds/degree
   * @return Distance cut off in degrees
   * @throws PhaseNotFoundException If the up-going arrival doesn't exist
   */
  public double upRay(String phCode, double dTdD) throws PhaseNotFoundException {
    char type = phCode.charAt(0);
    if (type == 'p' || type == 'P') {
      lastBrn = upBrnP;
    } else if (type == 's' || type == 'S') {
      lastBrn = upBrnS;
    } else {
      throw new PhaseNotFoundException(phCode);
    }
    if (lastBrn < 0) {
      return 0d;
    } else {
      return branches[lastBrn].oneRay(dTdD);
    }
  }

  /**
   * Getter for the time correction computed by the last call to oneRay or upRay.
   *
   * @return travel-time in seconds
   */
  public double getTimeCorr() {
    return branches[lastBrn].getTimeCorr();
  }

  /** Find the indices of the up-going P and S phases. */
  private void findUpBrn() {
    // Initialize the pointers just in case.
    upBrnP = -1;
    upBrnS = -1;
    // Find the up-going P type branch.
    for (int j = 0; j < branches.length; j++) {
      if (branches[j].exists && branches[j].ref.isUpGoing && branches[j].ref.typeSeg[0] == 'P') {
        upBrnP = j;
        break;
      }
    }
    // Find the up-going S type branch.
    for (int j = 0; j < branches.length; j++) {
      if (branches[j].exists && branches[j].ref.isUpGoing && branches[j].ref.typeSeg[0] == 'S') {
        upBrnS = j;
        break;
      }
    }
  }

  /**
   * Expand the list of desired phases using group codes. Note that the output list can be a
   * combination of phase codes, group codes, and special keywords.
   *
   * @param phList Input phase list
   */
  private void expandList(String[] phList) {
    String phGroup, auxGroup;

    // Start over with a new keyword list.
    if (expList != null) expList.clear();
    else expList = new ArrayList<String>();

    // Loop over the phase list entries expanding into a list of keywords.
    for (int j = 0; j < phList.length; j++) {
      phGroup = ref.auxtt.findGroup(phList[j]);
      // If the phase and group are the same, the list is generic.
      if (phGroup.equals(phList[j])) {
        expList.add(phGroup);
        if (ref.auxtt.isPrimary()) {
          auxGroup = ref.auxtt.compGroup(phGroup);
          if (auxGroup != null) expList.add(auxGroup);
        }
        // Otherwise, just keep the phase code or special keyword.
      } else {
        expList.add(phList[j]);
      }
    }
  }

  /**
   * Test a phase code against phase list keywords. Note that this implementation is somewhat
   * different from the Fortran Brnset, but follows the same ideas and is somewhat more precise.
   * This is because the Fortran implementation predated the phase groups and the phase groups were
   * developed and integrated with the Locator rather than the travel-time package.
   *
   * @param phCode Phase code
   * @return True if the phase code matches any of the phase list keywords
   */
  private boolean testList(String phCode) {
    String keyword;

    for (int j = 0; j < expList.size(); j++) {
      keyword = expList.get(j);
      // Local phases.
      if (keyword.toLowerCase().equals("ploc")) {
        if (ref.auxtt.isRegional(phCode)) {
          return true;
        }
        // Depth phases.
      } else if (keyword.toLowerCase().equals("pdep")) {
        if (ref.auxtt.isDepthPh(phCode)) {
          return true;
        }
        // Basic phases (everything that can be used in a location).
      } else if (keyword.toLowerCase().equals("basic")) {
        if (ref.auxtt.canUse(phCode)) {
          return true;
        }
        // Otherwise, see if we want this specific (or generic) phase.
      } else {
        if (keyword.equals(phCode) || keyword.equals(ref.auxtt.findGroup(phCode))) {
          return true;
        }
      }
    }
    return false;
  }

  /** Print global or header data for debugging purposes. */
  public void dumpHead() {
    String headerString = "\n     " + ref.modelName;
    headerString +=
        String.format(
            "Normalization: xNorm =%11.4e  vNorm =%11.4e  " + "tNorm =%11.4e\n",
            cvt.xNorm, cvt.vNorm, cvt.tNorm);
    headerString +=
        String.format(
            "Boundaries: zUpperMantle =%7.1f  zMoho =%7.1f  " + "zConrad =%7.1f\n",
            cvt.zUpperMantle, cvt.zMoho, cvt.zConrad);
    headerString +=
        String.format(
            "Derived: rSurface =%8.1f  zNewUp = %7.1f  " + "dTdDel2P =%11.4e  dTdDepth = %11.4e\n",
            cvt.rSurface, cvt.zNewUp, cvt.dTdDelta, dTdDepth);
    LOGGER.fine(headerString);
  }

  /**
   * Print a summary table of branch information similar to the FORTRAN Ttim range list.
   *
   * @param useful If true, only print "useful" crustal phases
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

  public int getBranchCount(boolean useful) {
    if (useful == false) {
      return (branches.length);
    } else {
      int count = 0;
      for (int j = 0; j < branches.length; j++) {
        if (branches[j].getIsUseless() == false) {
          count++;
        }
      }
      return (count);
    }
  }

  /**
   * Print model parameters for debugging purposes.
   *
   * @param typeMod Wave type ('P' or 'S')
   * @param nice If true print the model in dimensional units
   */
  public void dumpMod(char typeMod, boolean nice) {
    if (typeMod == 'P') {
      ref.pModel.dumpMod(nice);
    } else if (typeMod == 'S') {
      ref.sModel.dumpMod(nice);
    }
  }

  /**
   * Print data for one travel-time branch for debugging purposes.
   *
   * @param iBrn Branch number
   * @param full If true, print the detailed branch specification as well
   * @param all If true print even more specifications
   * @param sci if true, print in scientific notation
   */
  public void dumpBrn(int iBrn, boolean full, boolean all, boolean sci) {
    branches[iBrn].dumpBrn(full, all, sci, true, false);
  }

  /**
   * Print data for one travel-time phase code for debugging purposes.
   *
   * @param phCode Phase code
   * @param full If true, print the detailed branch specification as well
   * @param all If true print even more specifications
   * @param sci if true, print in scientific notation
   */
  public void dumpBrn(String phCode, boolean full, boolean all, boolean sci) {
    for (int j = 0; j < branches.length; j++) {
      if (branches[j].phCode.equals(phCode)) branches[j].dumpBrn(full, all, sci, true, false);
    }
  }

  /**
   * Print data for all travel-time branches for debugging purposes.
   *
   * @param full If true, print the detailed branch specification as well
   * @param all If true print even more specifications
   * @param sci if true, print in scientific notation
   * @param returnAllPhases If false, only print "useful" crustal phases
   */
  public void dumpBrn(boolean full, boolean all, boolean sci, boolean returnAllPhases) {
    for (int j = 0; j < branches.length; j++) {
      branches[j].dumpBrn(full, all, sci, returnAllPhases, false);
    }
  }

  /**
   * Print data for all travel-time branches that have at least one caustic. This turns out to be
   * particularly useful for finding instabilities in the interpolation.
   *
   * @param full If true, print the detailed branch specification as well
   * @param all If true print even more specifications
   * @param sci if true, print in scientific notation
   * @param returnAllPhases If false, only print "useful" crustal phases
   */
  public void dumpCaustics(boolean full, boolean all, boolean sci, boolean returnAllPhases) {
    for (int j = 0; j < branches.length; j++) {
      branches[j].dumpBrn(full, all, sci, returnAllPhases, true);
    }
  }

  /**
   * Print data for one travel-time segment for debugging purposes.
   *
   * @param seg Segment phase code
   * @param full If true, print the detailed branch specification as well
   * @param all If true print even more specifications
   * @param sci if true, print in scientific notation
   * @param returnAllPhases If false, only print "useful" crustal phases
   */
  public void dumpSeg(String seg, boolean full, boolean all, boolean sci, boolean returnAllPhases) {
    for (int j = 0; j < branches.length; j++) {
      if (branches[j].getPhSeg().equals(seg))
        branches[j].dumpBrn(full, all, sci, returnAllPhases, false);
    }
  }

  /**
   * Print data for one corrected up-going branch for debugging purposes.
   *
   * @param typeUp Wave type ('P' or 'S')
   * @param full If true print the up-going tau values as well
   */
  public void dumpCorrUp(char typeUp, boolean full) {
    if (typeUp == 'P') {
      pUp.dumpCorrUp(full);
    } else if (typeUp == 'S') {
      sUp.dumpCorrUp(full);
    }
  }

  /**
   * Print data for one decimated up-going branch for debugging purposes.
   *
   * @param typeUp Wave type ('P' or 'S')
   * @param full If true print the up-going tau values as well
   */
  public void dumpDecUp(char typeUp, boolean full) {
    if (typeUp == 'P') {
      pUp.dumpDecUp(full);
    } else if (typeUp == 'S') {
      sUp.dumpDecUp(full);
    }
  }
}
