package gov.usgs.traveltime;

import java.io.FileWriter;
import java.io.PrintWriter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * The GenerateTravelTimeValidationData class is a utility class that generates a json file of
 * validation data for use in regression unit tests.
 *
 * @author Ray Buland
 */
public class GenerateTravelTimeValidationData {

  /** A boolean flag, if true, read the phase statistics */
  private boolean readStatistics = true;

  /** A boolean flag, if true, read the Ellipticity corrections */
  private boolean readEllipiticity = true;

  /** A boolean flag, if true, read the topography file */
  private boolean readTopography = true;

  /** A boolean flag, if false, only provide "useful" crustal phases */
  private boolean returnAllPhases = true;

  /** A boolean flag, if false, suppress back branches */
  private boolean returnBackBranches = true;

  /** A boolean flag, if true, map Pb and Sb onto Pg and Sg */
  private boolean isTectonicSource = true;

  /** A String, if not null, contains the path to model files */
  private String modelPath = null;

  /** A String, if not null, contains the path to serialized files */
  private String serializedPath = null;

  /** An array of strings containing the phase use commands */
  private String[] phasesToUse = null;

  /** An array of Strings containing the list of models to generate */
  private String[] modelList = {"ak135", "cia", "cus", "ogs", "wus"};

  /** An array of double values to holding the depths to generate */
  private double[] depthList = {1d, 10d, 35d, 100d, 200d, 300d, 500d, 600d, 750d};

  /** An array of double values to holding the distances to generate */
  private double[] distanceList = {
    1d, 2d, 3d, 4d, 5d, 10d, 20d, 30d, 40d, 50d, 60d, 75d, 90d, 110d, 130d, 150d, 180d
  };

  /** A double containing the elevation to generate */
  private double elevation = 0.0d;

  /** A double containing the source latitude to to use for generating a complex test */
  double sourceLatitude = 54.480;

  /** A double containing the source longitude to to use for generating a complex test */
  double sourceLongitude = -159.766;

  /** A double containing the source depth to use for generating a complex test */
  double sourceDepth = 21.27;

  /**
   * An array of double values to holding the reciever latitudes to use for generating a complex
   * test
   */
  double[] latitudeList = {
    56.301, 54.831, 54.971, 55.149, 55.106, 55.144, 55.317, 54.724, 57.782, 60.413, 59.923, 45.905,
    39.690, 28.388, 33.576, -33.914, 35.098
  };

  /**
   * An array of double values to holding the reciever longitudes to use for generating a complex
   * test
   */
  double[] longitudeList = {
    -158.414, -159.589, -162.324, -161.864, -162.283, -162.25, -161.904, -163.712, -152.583,
    -163.350, -161.685, -112.777, -105.161, -80.675, -7.622, 18.375, 135.578
  };

  /**
   * An array of double values to holding the reciever elevations to use for generating a complex
   * test
   */
  double[] elevationList = {
    0.017, 0.071, 0.392, 0.425, 0.198, 0.396, 0.516, 0.620, 0.152, 0.009, 0.021, 1.609, 1.682,
    0.003, 0.027, 0.018, 0.971
  };

  /**
   * Function to generate travel time validation data.
   *
   * @param modelPath A string containing the model path to use
   * @param serializedPath A string containing the serialization path to use
   * @throws Exception If the travel-time setup fails
   */
  public void generate(String modelPath, String serializedPath) throws Exception {
    generateSimple(modelPath, serializedPath);
    generateComplex(modelPath, serializedPath);
  }

  /**
   * Function to generate simple session travel time validation data.
   *
   * @param modelPath A string containing the model path to use
   * @param serializedPath A string containing the serialization path to use
   * @throws Exception If the travel-time setup fails
   */
  public void generateSimple(String modelPath, String serializedPath) throws Exception {
    // create session
    TravelTimeSession ttLocal =
        new TravelTimeSession(
            readStatistics, readEllipiticity, readTopography, modelPath, serializedPath);

    // for each model
    for (int i = 0; i < modelList.length; i++) {
      String earthModel = modelList[i];

      JSONObject modelObject = new JSONObject();
      modelObject.put("EarthModel", earthModel);

      // for each depth
      JSONArray depthArray = new JSONArray();
      for (int j = 0; j < depthList.length; j++) {
        double sourceDepth = depthList[j];

        JSONObject depthObject = new JSONObject();
        depthObject.put("Depth", sourceDepth);

        // setup new session
        ttLocal.newSession(
            earthModel,
            sourceDepth,
            phasesToUse,
            returnAllPhases,
            returnBackBranches,
            isTectonicSource);

        // for each distance
        JSONArray distanceArray = new JSONArray();
        for (int k = 0; k < distanceList.length; k++) {
          double distance = distanceList[k];

          TravelTime ttList = ttLocal.getTravelTimes(elevation, distance);

          JSONObject distanceObject = new JSONObject();
          distanceObject.put("Distance", distance);

          // for each phase
          JSONArray phaseArray = new JSONArray();
          for (int l = 0; l < ttList.getNumPhases(); l++) {
            TravelTimeData phase = ttList.getPhase(l);
            JSONObject phaseObject = new JSONObject();

            phaseObject.put("Phase", phase.getPhaseCode());
            phaseObject.put("TravelTime", phase.getTravelTime());
            phaseObject.put("DistanceDerivative", phase.getDistanceDerivitive());
            phaseObject.put("DepthDerivative", phase.getDepthDerivitive());
            phaseObject.put("RayDerivative", phase.getRayDerivative());
            phaseObject.put("StatisticalSpread", phase.getStatisticalSpread());
            phaseObject.put("Observability", phase.getObservability());
            phaseObject.put("AssociationWindow", phase.getAssocWindow());
            phaseObject.put("TeleseismicPhaseGroup", phase.getGroupPhaseCode());
            phaseObject.put("AuxiliaryPhaseGroup", phase.getAuxiliaryGroupPhaseCode());
            phaseObject.put("LocationUseFlag", phase.getLocationCanUse());
            phaseObject.put("AssociationWeightFlag", phase.getAssocDownWeight());
            phaseObject.put("DepthFlag", phase.getIsDepthSensitive());
            phaseObject.put("RegionalFlag", phase.getIsRegional());

            // add the phase to the phase list
            phaseArray.add(phaseObject);
          }

          // add the phase list to the distance object
          distanceObject.put("Phases", phaseArray);

          // add the distance to the distance list
          distanceArray.add(distanceObject);
        }

        // add the distance list to the depth object
        depthObject.put("Distances", distanceArray);

        // add the depth to the depth list
        depthArray.add(depthObject);
      }

      // add the depths list to the model object
      modelObject.put("Depths", depthArray);

      // convert to string
      String validationString = modelObject.toJSONString();

      // write this model to file
      try {
        FileWriter fileWriter = new FileWriter("./" + earthModel + "ValidationData.json", false);
        PrintWriter printWriter = new PrintWriter(fileWriter);
        printWriter.println(validationString);
        printWriter.close();
      } catch (Exception e) {
        System.out.println(e.toString());
      }
    }
  }

  /**
   * Function to generate complex session travel time validation data.
   *
   * @param modelPath A string containing the model path to use
   * @param serializedPath A string containing the serialization path to use
   * @throws Exception If the travel-time setup fails
   */
  public void generateComplex(String modelPath, String serializedPath) throws Exception {
    // create session
    TravelTimeSession ttLocal =
        new TravelTimeSession(
            readStatistics, readEllipiticity, readTopography, modelPath, serializedPath);

    // setup new complex session
    ttLocal.newSession(
        "ak135",
        sourceDepth,
        phasesToUse,
        sourceLatitude,
        sourceLongitude,
        returnAllPhases,
        returnBackBranches,
        isTectonicSource);

    JSONObject complexObject = new JSONObject();
    complexObject.put("SourceLatitude", sourceLatitude);
    complexObject.put("SourceLongitude", sourceLongitude);
    complexObject.put("SourceDepth", sourceDepth);

    // for each lat/lon point
    JSONArray pointArray = new JSONArray();
    for (int i = 0; i < latitudeList.length; i++) {

      double recieverLatitude = latitudeList[i];
      double receiverLongitude = longitudeList[i];
      double receiverElevation = elevationList[i];
      double receiverDistance = Double.NaN;
      double recieverAzimuth = Double.NaN;

      TravelTime ttList =
          ttLocal.getTravelTimes(
              recieverLatitude,
              receiverLongitude,
              receiverElevation,
              receiverDistance,
              recieverAzimuth);

      JSONObject pointObject = new JSONObject();
      pointObject.put("RecieverLatitude", recieverLatitude);
      pointObject.put("ReceiverLongitude", receiverLongitude);
      pointObject.put("ReceiverElevation", receiverElevation);

      // for each phase
      JSONArray phaseArray = new JSONArray();
      for (int j = 0; j < ttList.getNumPhases(); j++) {
        TravelTimeData phase = ttList.getPhase(j);
        JSONObject phaseObject = new JSONObject();

        phaseObject.put("Phase", phase.getPhaseCode());
        phaseObject.put("TravelTime", phase.getTravelTime());
        phaseObject.put("DistanceDerivative", phase.getDistanceDerivitive());
        phaseObject.put("DepthDerivative", phase.getDepthDerivitive());
        phaseObject.put("RayDerivative", phase.getRayDerivative());
        phaseObject.put("StatisticalSpread", phase.getStatisticalSpread());
        phaseObject.put("Observability", phase.getObservability());
        phaseObject.put("AssociationWindow", phase.getAssocWindow());
        phaseObject.put("TeleseismicPhaseGroup", phase.getGroupPhaseCode());
        phaseObject.put("AuxiliaryPhaseGroup", phase.getAuxiliaryGroupPhaseCode());
        phaseObject.put("LocationUseFlag", phase.getLocationCanUse());
        phaseObject.put("AssociationWeightFlag", phase.getAssocDownWeight());
        phaseObject.put("DepthFlag", phase.getIsDepthSensitive());
        phaseObject.put("RegionalFlag", phase.getIsRegional());

        // add the phase to the phase list
        phaseArray.add(phaseObject);
      }

      // add the phase list to the distance object
      pointObject.put("Phases", phaseArray);

      // add the distance to the distance list
      pointArray.add(pointObject);
    }

    // add the points list to the model object
    complexObject.put("Points", pointArray);

    // convert to string
    String validationString = complexObject.toJSONString();

    // write this model to file
    try {
      FileWriter fileWriter = new FileWriter("./complexValidationData.json", false);
      PrintWriter printWriter = new PrintWriter(fileWriter);
      printWriter.println(validationString);
      printWriter.close();
    } catch (Exception e) {
      System.out.println(e.toString());
    }
  }
}
