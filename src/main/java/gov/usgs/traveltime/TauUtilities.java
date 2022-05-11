package gov.usgs.traveltime;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.TreeMap;

/**
 * Utility methods for the travel-time package.
 *
 * @author Ray Buland
 */
public class TauUtilities {
  /** A double constant containing the global tolerance value. */
  public static final double DOUBLETOLERANCE = 1e-9d;

  /** A double constant containing the global minimum positive value. */
  public static final double MINIMUMDOUBLE = 1e-30d;

  /** A double constant containing the global maximum positive value. */
  public static final double MAXIMUMDOUBLE = 1e30d;

  /** A double constant containing the maximum depth supported by the travel-time tables. */
  public static final double MAXIMUMDEPTH = 800d;

  /**
   * A double constant holding the crude minimum elevation in kilometers (would include a station at
   * the bottom of the Mariana Trench).
   */
  public static final double MINIMUMELEVATION = -11d;

  /**
   * A double constant holding the crude maximum elevation in kilometers (would include a station at
   * the top of Mount Everest).
   */
  public static final double MAXIUMUMELEVATION = 9d;

  /**
   * A double constant containing the global default shallow crustal P velocity in km/s (from
   * ak135).
   */
  public static final double DEFAULTPVELOCITY = 5.80d;

  /**
   * A double constant containing the global default shallow crustal S velocity in km/s (from
   * ak135).
   */
  public static final double DEFAULTSVELOCITY = 3.46d;

  /** A double constant containing the global default water P velocity in km/s. */
  public static final double DEFAULTWATERPVELOCITY = 1.50d;

  /** A double constant containing the Lg group velocity in kilometers/second. */
  public static final double LGGROUPVELOCITY = 3.4d;

  /** A double constant containing the LR group velocity in kilometers/second. */
  public static final double LRGROUPVELOCITY = 3.5d;

  /**
   * A double constant containing the maximum credible Pn source-receiver distance in degrees.
   * Because of the nature of the algorithm, there are no inherent distance limits for any of the
   * phases.
   */
  public static final double PNMAXIMUMDISTANCE = 21.5d;

  /**
   * A double constant containing the maximum credible Sn source-receiver distance in degrees. Note
   * that there is a short Sn branch in the WUS model at more than 40 degrees that appears to be an
   * artifact of layering the WUS crust on top of the AK135 mantle.
   */
  public static final double SNMAXIMUMDISTANCE = 30.0d;

  /**
   * A double constant containing the minimum distance in radians for an Sn branch to proxy for Lg.
   */
  protected static final double SNMINIMUMDISTANCE = 0.035d;

  /** A double constant containing the maximum credible Pg source-receiver distance in degrees. */
  public static final double PGMAXIMUMDISTANCE = 8.5d;

  /** A double constant containing the maximum credible Lg source-receiver distance in degrees. */
  public static final double LGMAXIMUMDISTANCE = 30.0d;

  /** A double constant containing the maximum depth in kilometers for which Lg will be added. */
  protected static final double LGMAXIMUMDEPTH = 35d;

  /** A double constant containing the maximum distance in radians for which LR will be added. */
  protected static final double LRMAXIMUMDISTANCE = 0.698d;

  /** A double constant containing the maximum depth in kilometers for which LR will be added. */
  protected static final double LRMAXIMUMDEPTH = 55d;

  /** A double constant containing the global default travel-time statistical bias in seconds. */
  public static final double DEFAULTTTBIAS = 0d;

  /** A double constant containing the global default travel-time statistical spread in seconds. */
  public static final double DEFAULTTTSPREAD = 12d;

  /**
   * A double constant containing the global default travel-time statistical relative observability.
   */
  public static final double DEFAULTTTOBSERVABILITY = 0d;

  /**
   * A double constant containing the association window for theoretical phases will be the
   * association factor times the spread.
   */
  public static final double ASSOCWINDOWFACTOR = 7d;

  /**
   * A double constant containing the association window size. A larger window is needed when the
   * location is poor.
   */
  public static final double ASSOCWINDOWMINIMUM = 5d;

  /** A double constant containing the distance increment in degrees for travel-time plots. */
  public static final double PLOTDISTANCEINCREMENT = 1d;

  /**
   * A double constant containing the minimum time in seconds that phases with the same name should
   * be separated.
   */
  protected static final double SAMEPHASESEPERATION = 0.005d;

  /**
   * A double constant containing the time interval following a phase where it's nearly impossible
   * to pick another phase.
   */
  protected static final double OBSERVABILITYSHADOW = 3d;

  /**
   * A double constant containing the observability threshold to ensure observability doesn't go to
   * zero.
   */
  protected static final double MINIMUMOBSERVABILITY = 1d;

  /** A double constant containing the frequency for which OBSERVABILITYSHADOW is half a cycle. */
  protected static final double OBSERVABILITYFREQUENCY = Math.PI / OBSERVABILITYSHADOW;

  /**
   * A double constant containing the Ellipticity factor needed to compute geocentric co-latitude.
   */
  protected static final double ELLIPTICITYFACTOR = 0.993305521d;

  /** A double constant containing the tau comparision tolerance */
  public static final double TAUCOMPARISONTOLERANCE = 1e-11d;

  /**
   * A boolean flag, if true read read model data from the input files generated by the old Fortran
   * Remodl and Setbrn programs (primarily for testing purposes). By default generate the model data
   * from a model definition file.
   */
  public static boolean useFortranFiles = false;

  /**
   * A boolean flag, if true, suppress all travel-time corrections for debugging purposes and cases
   * where the corrections aren't needed.
   */
  public static boolean suppressCorrections = false;

  /** A boolean flag, if true, apply miscellaneous filters to get rid of extraneous phases. */
  public static boolean useMiscFilter = true;

  /**
   * A double containing the receiver azimuth relative to the source in degrees clockwise from north
   * as computed by computeDistAzm.
   */
  private static double recieverAzimuth = Double.NaN;

  /**
   * A double containing the longitude in degrees projected from an epicenter by a distance and
   * azimuth as computed by projectLatitude
   */
  private static double projectedLongitude = Double.NaN;

  /** A TreeMap containing the storage for unique phase codes. */
  private static TreeMap<String, Integer> uniquePhaseCodes;

  /** A String containing the default path for model files. */
  private static String defaultModelPath = "./models/";

  /** A String containing the default path for serialized files. */
  public static String defaultSerializedPath = "./models/";

  /**
   * Function to return the receiver azimuth relative to the source in degrees clockwise from north
   * as computed by computeDistAzm.
   *
   * @return A double containing the receiver azimuth relative to the source in degrees clockwise
   *     from north as computed by computeDistAzm.
   */
  public static double getRecieverAzimuth() {
    return recieverAzimuth;
  }

  /**
   * Function to return the longitude in degrees projected from an epicenter by a distance and
   * azimuth as computed by projectLatitude
   *
   * @return A double containing the longitude in degrees projected from an epicenter by a distance
   *     and azimuth as computed by projectLatitude
   */
  public static double getProjectedLongitude() {
    return projectedLongitude;
  }

  /**
   * Function to set the default model path
   *
   * @param newModelPath A string containing the new default model path
   */
  public static void setDefaultModelPath(String newModelPath) {
    defaultModelPath = newModelPath;
  }

  /**
   * Function to set the default serialized path
   *
   * @param newSerializedPath A string containing the new default serialized path
   */
  public static void setDefaulSerializedPath(String newSerializedPath) {
    defaultSerializedPath = newSerializedPath;
  }

  /**
   * Function to build a path to a given model file using the defaultModelPath.
   *
   * @param modelFile A String holding the model file name
   * @return A string containing the file path to the model file.
   */
  public static String getModelPath(String modelFile) {
    return defaultModelPath + modelFile;
  }

  /**
   * Function to build a path to a given serialized file using the defaultModelPath.
   *
   * @param serializedFile A String holding the serialized file name
   * @return A string containing the file path to the serialized file.
   */
  public static String getSerializedPath(String serializedFile) {
    return defaultSerializedPath + serializedFile;
  }

  /**
   * Function to make a list of available Earth models.
   *
   * @return An array of Strings containing the list of available Earth models.
   */
  public static String[] getAvailableModels() {
    FilenameFilter filter =
        new FilenameFilter() {
          @Override
          public boolean accept(File dir, String name) {
            if (name.startsWith("m") && name.endsWith(".mod")) {
              return true;
            } else {
              return false;
            }
          }
        };

    File folder = new File(defaultModelPath);
    String[] modelList = folder.list(filter);

    for (int j = 0; j < modelList.length; j++) {
      modelList[j] = modelList[j].substring(1, modelList[j].indexOf('.'));
    }

    return modelList;
  }

  /**
   * Function to create a segment code by stripping a phase code of unnecessary frippery.
   *
   * @param phaseCode A String containing the phase code to strip
   * @return A String containing the segment code
   */
  public static String createSegmentCode(String phaseCode) {
    int index;

    if ((index = phaseCode.indexOf("df")) >= 0) {
      return phaseCode.substring(0, index);
    }

    if ((index = phaseCode.indexOf("ab")) >= 0) {
      return phaseCode.substring(0, index);
    }

    if ((index = phaseCode.indexOf("ac")) >= 0) {
      return phaseCode.substring(0, index);
    }

    if ((index = phaseCode.indexOf("g")) >= 0) {
      String generatedPhaseCode =
          phaseCode.substring(0, index) + phaseCode.substring(index + 1, phaseCode.length());

      if ((index = generatedPhaseCode.indexOf("g")) >= 0) {
        return generatedPhaseCode.substring(0, index);
      } else {
        return generatedPhaseCode;
      }
    }

    if ((index = phaseCode.indexOf("b")) >= 0) {
      String generatedPhaseCode =
          phaseCode.substring(0, index) + phaseCode.substring(index + 1, phaseCode.length());

      if ((index = generatedPhaseCode.indexOf("b")) >= 0) {
        return generatedPhaseCode.substring(0, index);
      } else {
        return generatedPhaseCode;
      }
    }

    if ((index = phaseCode.indexOf("n")) >= 0) {
      String generatedPhaseCode =
          phaseCode.substring(0, index) + phaseCode.substring(index + 1, phaseCode.length());

      if ((index = generatedPhaseCode.indexOf("n")) >= 0) {
        return generatedPhaseCode.substring(0, index);
      } else {
        return generatedPhaseCode;
      }
    }

    return phaseCode;
  }

  /**
   * Function to make phase codes unique by appending a reference number. This is needed to keep
   * branches straight in the plot data.
   *
   * @param phaseCode A String containing the phase code
   * @return A String containing the unique phase code
   */
  public static String makeUniquePhaseCode(String phaseCode) {
    if (uniquePhaseCodes == null) {
      uniquePhaseCodes = new TreeMap<String, Integer>();
    }

    Integer no = uniquePhaseCodes.get(phaseCode);

    if (no != null) {
      uniquePhaseCodes.replace(phaseCode, ++no);
    } else {
      no = 0;
      uniquePhaseCodes.put(phaseCode, no);
    }

    return phaseCode + no;
  }

  /**
   * Function to classify seismic phases according to their wave type at the receiver.
   *
   * @param phaseCode A String containing the phase code to classify
   * @return A char containing the classification, 'P' for a p-wave, 'S' for an s-wave, 'L' for an
   *     Lg, and 'R' for an LR.
   */
  public static char classifyPhaseWaveType(String phaseCode) {
    // Try the common cases first.
    for (int j = phaseCode.length() - 1; j >= 0; j--) {
      if (phaseCode.charAt(j) == 'P') {
        return 'P';
      } else if (phaseCode.charAt(j) == 'S') {
        return 'S';
      }
    }

    // Then do the special cases.
    if (phaseCode.equals("Lg")) {
      return 'L';
    } else if (phaseCode.equals("LR")) {
      return 'R';
    } else {
      // This should never happen.
      return ' ';
    }
  }

  /**
   * Function to determine if a phase has been converted at the Earth's surface.
   *
   * @param phaseCode A String containing the phase code to check
   * @return A boolean flag, true if the phase has been converted at the surface
   */
  public static boolean isPhaseConverted(String phaseCode) {
    if ((phaseCode.contains("p") || phaseCode.contains("P"))
        && (phaseCode.contains("s") || phaseCode.contains("S"))) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Function to filter out phases of the same name that are so close in time as to be useless. This
   * is particularly useful when there are small instabilities in the tau interpolation.
   *
   * @param travelTimeList An ArrayList of TravelTimeData objects to filter
   */
  public static void filterClosePhases(ArrayList<TravelTimeData> travelTimeList) {
    for (int j = 1; j < travelTimeList.size(); j++) {
      if (travelTimeList.get(j).getPhaseCode().equals(travelTimeList.get(j - 1).getPhaseCode())
          && travelTimeList.get(j).getTravelTime() - travelTimeList.get(j - 1).getTravelTime()
              <= SAMEPHASESEPERATION) {
        travelTimeList.remove(j--);
      }
    }
  }

  /**
   * Function to modify the observability of phases closely following other phases (in time). This
   * is a real seat-of-the-pants hack. The problem is that the proximity of phases in time depends
   * on depth, but the statistics include all depths (corrected to surface focus).
   *
   * @param travelTimeList An ArrayList of TravelTimeData objects to filter
   */
  public static void modifyCloseObservability(ArrayList<TravelTimeData> travelTimeList) {
    // Loop backwards over the phases.
    for (int j = travelTimeList.size() - 2; j >= 0; j--) {
      TravelTimeData travelTimeJ = travelTimeList.get(j);

      // Loop over the phases later than the current phase.
      for (int i = j + 1; i < travelTimeList.size(); i++) {
        TravelTimeData travelTimeI = travelTimeList.get(i);

        // If the phases are close together, modify the later phase.
        if (travelTimeI.getTravelTime() - travelTimeJ.getTravelTime() < OBSERVABILITYSHADOW) {
          // If the later phase has some observability, make sure it
          // might still be used.
          if (travelTimeI.getObservability() >= MINIMUMOBSERVABILITY) {
            travelTimeI.setObservability(
                Math.max(
                    travelTimeI.getObservability()
                        - 0.5d
                            * travelTimeJ.getObservability()
                            * (Math.cos(
                                    OBSERVABILITYFREQUENCY
                                        * (travelTimeI.getTravelTime()
                                            - travelTimeJ.getTravelTime()))
                                + 1d),
                    MINIMUMOBSERVABILITY));
            // Otherwise, let the later phase observability go to zero.
          } else {
            travelTimeI.setObservability(
                Math.max(
                    travelTimeI.getObservability()
                        - 0.5d
                            * travelTimeJ.getObservability()
                            * (Math.cos(
                                    OBSERVABILITYFREQUENCY
                                        * (travelTimeI.getTravelTime()
                                            - travelTimeJ.getTravelTime()))
                                + 1d),
                    0d));
          }
        } else {
          break;
        }
      }
    }
  }

  /**
   * Function to filter out all seismic back branches. Triplication back branches confuse the
   * Locator, so this filter removes all secondary phases of the same name no matter how big the
   * arrival time differences.
   *
   * @param travelTimeList An ArrayList of TravelTimeData objects to filter
   */
  public static void filterBackBranches(ArrayList<TravelTimeData> travelTimeList) {
    for (int j = 0; j < travelTimeList.size() - 1; j++) {
      for (int i = j + 1; i < travelTimeList.size(); i++) {
        if (travelTimeList.get(j).getPhaseCode().equals(travelTimeList.get(i).getPhaseCode())) {
          travelTimeList.remove(i--);
        }
      }
    }
  }

  /**
   * Function to filter out all tectonic phases. \Pb and Sb can be renamed Pg and Sg respectively.
   * By default, the travel-time calculations are done for continental cratons where there may be
   * clear Pb (P*) and Sb (S*) phases. However, these just annoy the seismic analysts in tectonic
   * areas where they are rarely if ever observed. The crude hack implemented here is to rename Pb
   * and Sb to Pg and Sg respectively. This has the effect of pretending that the modeled Conrad
   * discontinuity is a smooth transition and that Pg and Sg may turn all the way to the Moho. Note
   * that this may create new back branches, so it should be run before the close or back branch
   * filters.
   *
   * @param travelTimeList An ArrayList of TravelTimeData objects to filter
   */
  public static void filterTectonicPhases(ArrayList<TravelTimeData> travelTimeList) {
    for (int j = 0; j < travelTimeList.size(); j++) {
      // Turn Pbs into Pgs.
      if (travelTimeList.get(j).getPhaseCode().contains("Pb")
          && !travelTimeList.get(j).getPhaseCode().contains("K")) {
        travelTimeList.get(j).replacePhaseCode("Pb", "Pg");
      }

      // Turn Sbs into Sgs.
      if (travelTimeList.get(j).getPhaseCode().contains("Sb")
          && !travelTimeList.get(j).getPhaseCode().contains("K")) {
        travelTimeList.get(j).replacePhaseCode("Sb", "Sg");
      }
    }
  }

  /**
   * Function to modify the location can use flag. By default, locationCanUse only reflects phase
   * types that make sense to use in an earthquake location. However, phases can't be used if they
   * have no statistics either. Setting locationCanUse to reflect both conditions makes it easier
   * for the Locator.
   *
   * @param travelTimeList An ArrayList of TravelTimeData objects to filter
   */
  public static void modifyCanUse(ArrayList<TravelTimeData> travelTimeList) {
    for (int j = 0; j < travelTimeList.size(); j++) {
      if (travelTimeList.get(j).getStatisticalSpread() >= DEFAULTTTSPREAD
          || travelTimeList.get(j).getObservability() <= DEFAULTTTOBSERVABILITY)
        travelTimeList.get(j).setLocationCanUse(false);

      /* This is the way the old Locator worked.  It is less correct
      and it should make little difference, but it makes side-by-side
      comparison difficult.
      	if(travelTimeList.get(j).getStatisticalSpread() >= DEFAULTTTSPREAD) travelTimeList.get(j).setLocationCanUse(false);
      */
    }
  }

  /**
   * Funtion that appliesy miscellaneous filters to get rid of extraneous phases.
   *
   * @param travelTimeList An ArrayList of TravelTimeData objects to filter
   * @param distance Source-receiver distance in degrees
   */
  public static void applyMiscFilters(ArrayList<TravelTimeData> travelTimeList, double distance) {
    if (useMiscFilter) {
      for (int j = 0; j < travelTimeList.size(); j++) {
        if (distance > SNMAXIMUMDISTANCE) {
          // Filter Sn, pSn, and sSn at large distances.
          if (travelTimeList.get(j).getPhaseCode().contains("Sn")
              && travelTimeList.get(j).getPhaseCode().length() < 4) {
            travelTimeList.remove(j);
            break;
          }
        }
      }
    }
  }

  /**
   * Function to compute the geocentric co-latitude.
   *
   * @param latitude A double contining the geographical latitude in degrees
   * @return A double containing thr geocentric co-latitude in degrees
   */
  public static double computeGeocentricColatitude(double latitude) {
    if (Math.abs(90d - latitude) < TauUtilities.DOUBLETOLERANCE) {
      return 0d;
    } else if (Math.abs(90d + latitude) < TauUtilities.DOUBLETOLERANCE) {
      return 180d;
    } else {
      return 90d
          - Math.toDegrees(
              Math.atan(
                  ELLIPTICITYFACTOR
                      * Math.sin(Math.toRadians(latitude))
                      / Math.cos(Math.toRadians(latitude))));
    }
  }

  /**
   * Function to compute the geographic latitude.
   *
   * @param coLatitude A double contining the geocentric co-latitude in degrees
   * @return A double contining the geographic latitude in degrees
   */
  public static double computeGeographicLatitude(double coLatitude) {
    return Math.toDegrees(
        Math.atan(
            Math.cos(Math.toRadians(coLatitude))
                / (ELLIPTICITYFACTOR
                    * Math.max(
                        Math.sin(Math.toRadians(coLatitude)), TauUtilities.DOUBLETOLERANCE))));
  }

  /**
   * Function to compute the distance and azimuth between a source and a reciever. A historically
   * significant subroutine from deep time (1962)! This routine was written by Bob Engdahl in
   * Fortran (actually in the days before subroutines) and beaten into it's current Fortran form by
   * Ray Buland in the early 1980s. It's optimized with respect to computing sines and cosines
   * (probably still worthwhile) and it computes exactly what's needed--no more, no less. This (much
   * more horrible) alternate form to the computeDistAzm in LocUtil is much closer to Engdahl's
   * original. It is needed to avoid a build path cycle. Note that the azimuth is returned in static
   * variable azimuth. Note that the projected longitude is returned in static variable
   * recieverAzimuth.
   *
   * @param sourceLatitude A double containing the geographic source latitude in degrees
   * @param sourceLongitude A double containing the source longitude in degrees
   * @param recieverLatitude A double containing the geographic reciever (station) latitude in
   *     degrees
   * @param recieverLongitude A double containing the reciever (station) longitude in degrees
   * @return A double containing the distance in degrees. Note that the azimuth is accessed via the
   *     getRecieverAzimuth function after this call.
   */
  public static double computeDistAzm(
      double eqLat, double sourceLongitude, double recieverLatitude, double recieverLongitude) {
    // Get the hypocenter geocentric co-latitude.
    double coLatitude = computeGeocentricColatitude(eqLat);

    // Hypocenter sines and cosines.
    double eqSinLat = Math.sin(Math.toRadians(coLatitude));
    double eqCosLat = Math.cos(Math.toRadians(coLatitude));
    double eqSinLon = Math.sin(Math.toRadians(sourceLongitude));
    double eqCosLon = Math.cos(Math.toRadians(sourceLongitude));

    // Get the station geocentric co-latitude.
    coLatitude = computeGeocentricColatitude(recieverLatitude);

    // Station sines and cosines.
    double staSinLat = Math.sin(Math.toRadians(coLatitude));
    double staCosLat = Math.cos(Math.toRadians(coLatitude));
    double staSinLon = Math.sin(Math.toRadians(recieverLongitude));
    double staCosLon = Math.cos(Math.toRadians(recieverLongitude));

    // South Pole:
    if (staSinLat <= TauUtilities.DOUBLETOLERANCE) {
      recieverAzimuth = 180d;
      return Math.toDegrees(Math.PI - Math.acos(eqCosLat));
    }

    // Compute some intermediate variables.
    // Use Bob Engdahl's variable names
    double cosdel =
        eqSinLat * staSinLat * (staCosLon * eqCosLon + staSinLon * eqSinLon) + eqCosLat * staCosLat;
    double tm1 = staSinLat * (staSinLon * eqCosLon - staCosLon * eqSinLon);
    double tm2 =
        eqSinLat * staCosLat - eqCosLat * staSinLat * (staCosLon * eqCosLon + staSinLon * eqSinLon);
    double sindel = Math.sqrt(Math.pow(tm1, 2d) + Math.pow(tm2, 2d));

    // Do the azimuth.
    if (Math.abs(tm1) <= TauUtilities.DOUBLETOLERANCE
        && Math.abs(tm2) <= TauUtilities.DOUBLETOLERANCE) {
      recieverAzimuth = 0d; // North Pole.
    } else {
      recieverAzimuth = Math.toDegrees(Math.atan2(tm1, tm2));
      if (recieverAzimuth < 0d) recieverAzimuth += 360;
    }

    // Do distance.
    if (sindel <= TauUtilities.DOUBLETOLERANCE
        && Math.abs(cosdel) <= TauUtilities.DOUBLETOLERANCE) {
      return 0d;
    } else {
      return Math.toDegrees(Math.atan2(sindel, cosdel));
    }
  }

  /**
   * Function to project an epicenter using distance and azimuth. Used in finding the bounce point
   * for a surface reflected seismic phase. Note that the projected longitude is returned in static
   * variable projectedLongitude.
   *
   * @param latitude A double containing the geographic epicenter latitude in degrees
   * @param longitude A double containing the longitude in degrees
   * @param distance A double containing the distance to project in degrees
   * @param azimuth A double containing the azimuth to project in degrees
   * @return A double containing the projected geographic latitude in degrees, note that the
   *     projected longitude is accessed via the getProjectedLongitude() function after this call.
   */
  public static double projectLatitude(
      double latitude, double longitude, double distance, double azimuth) {
    double coLatitude = TauUtilities.computeGeocentricColatitude(latitude);

    if (longitude < 0d) {
      longitude += 360d;
    }

    double sinLat = Math.sin(Math.toRadians(coLatitude));
    double cosLat = Math.cos(Math.toRadians(coLatitude));
    double sinDel = Math.sin(Math.toRadians(distance));
    double cosDel = Math.cos(Math.toRadians(distance));
    double sinAzim = Math.sin(Math.toRadians(azimuth));
    double cosAzim = Math.cos(Math.toRadians(azimuth));

    double cTheta = sinDel * sinLat * cosAzim + cosLat * cosDel;
    double projectedLatitude = Math.acos(cTheta);
    double sinNewLat = Math.sin(projectedLatitude);

    if (coLatitude == 0d) {
      projectedLongitude = azimuth;
    } else if (projectedLatitude == 0d) {
      projectedLongitude = 0d;
    } else {
      projectedLongitude =
          longitude
              + Math.toDegrees(
                  Math.atan2(
                      sinDel * sinAzim / sinNewLat,
                      (cosDel - cosLat * cTheta) / (sinLat * sinNewLat)));
    }

    if (projectedLongitude > 360d) {
      projectedLongitude -= 360d;
    }

    if (projectedLongitude > 180d) {
      projectedLongitude -= 360d;
    }

    return computeGeographicLatitude(Math.toDegrees(projectedLatitude));
  }

  /**
   * Function to compute the elevation correction.
   *
   * @param elevation A double containing the elevation in kilometers
   * @param velocity A double containing the velocity in kilometers/second
   * @param rayParameter A double containing the ray parameter in seconds/kilometers
   * @return A double containing the elevation correction in seconds
   */
  public static double compElevationCorrection(
      double elevation, double velocity, double rayParameter) {
    return (elevation / velocity)
        * Math.sqrt(Math.abs(1. - Math.min(Math.pow(velocity * rayParameter, 2d), 1d)));
  }

  /**
   * Function to perform a bilinear interpolation. The indices are such that val[ind] &lt; var &lt;=
   * val[ind+1]. The two dimensional grid of values to be interpolated has values value0 associated
   * with it's first index and values value1 associated with it's second index. This function
   * differes from the other biLinearInterpolation function by taking as input a grid of double
   * values instead of a grid of short values.
   *
   * @param variable0 A double containing the first variable
   * @param variable1 A double containing the second variable
   * @param value0 A GeneralizedIndex object containing the value array for the first grid index
   * @param value1 A GeneralizedIndex object containing the value array for the second grid index
   * @param grid A two dimensional array of double values to be interpolated
   * @return A double containing the interpolated value
   */
  public static double biLinearInterpolation(
      double variable0,
      double variable1,
      GeneralizedIndex value0,
      GeneralizedIndex value1,
      double[][] grid) {
    // Use the virtual arrays to get the interpolation indices.
    int ind0 = value0.getIndex(variable0);
    int ind1 = value1.getIndex(variable1);

    // Interpolate the first variable at it's lower index.
    double lin00 =
        grid[ind0][ind1]
            + (grid[ind0 + 1][ind1] - grid[ind0][ind1])
                * (variable0 - value0.getValue(ind0))
                / (value0.getValue(ind0 + 1) - value0.getValue(ind0));

    // Interpolate the first variable at it's upper index.
    double lin01 =
        grid[ind0][ind1 + 1]
            + (grid[ind0 + 1][ind1 + 1] - grid[ind0][ind1 + 1])
                * (variable0 - value0.getValue(ind0))
                / (value0.getValue(ind0 + 1) - value0.getValue(ind0));

    // Interpolate the second variable.
    return lin00
        + (lin01 - lin00)
            * (variable1 - value1.getValue(ind1))
            / (value1.getValue(ind1 + 1) - value1.getValue(ind1));
  }

  /**
   * Function to perform a bilinear interpolation. The indices are such that val[ind] &lt; var &lt;=
   * val[ind+1]. The two dimensional grid of values to be interpolated has values value0 associated
   * with it's first index and values value1 associated with it's second index. This function
   * differes from the other biLinearInterpolation function by taking as input a grid of short
   * values instead of a grid of double values.
   *
   * @param variable0 A double containing the first variable
   * @param variable1 A double containing the second variable
   * @param value0 A GeneralizedIndex object containing the value array for the first grid index
   * @param value1 A GeneralizedIndex object containing the value array for the second grid index
   * @param grid A two dimensional array of short values to be interpolated
   * @return A double containing the interpolated value
   */
  public static double biLinearInterpolation(
      double variable0,
      double variable1,
      GeneralizedIndex value0,
      GeneralizedIndex value1,
      short[][] grid) {
    // Use the virtual arrays to get the interpolation indices.
    int ind0 = value0.getIndex(variable0);
    int ind1 = value1.getIndex(variable1);

    // Interpolate the first variable at it's lower index.
    double lin00 =
        (double) grid[ind0][ind1]
            + ((double) grid[ind0 + 1][ind1] - (double) grid[ind0][ind1])
                * (variable0 - value0.getValue(ind0))
                / (value0.getValue(ind0 + 1) - value0.getValue(ind0));

    // Interpolate the first variable at it's upper index.
    double lin01 =
        (double) grid[ind0][ind1 + 1]
            + ((double) grid[ind0 + 1][ind1 + 1] - (double) grid[ind0][ind1 + 1])
                * (variable0 - value0.getValue(ind0))
                / (value0.getValue(ind0 + 1) - value0.getValue(ind0));

    // Interpolate the second variable.
    return lin00
        + (lin01 - lin00)
            * (variable1 - value1.getValue(ind1))
            / (value1.getValue(ind1 + 1) - value1.getValue(ind1));
  }
}
