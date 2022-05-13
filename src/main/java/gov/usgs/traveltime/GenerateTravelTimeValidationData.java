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

  /**
   * Function to generate travel time validation data.
   *
   * @param args Command line arguments (not used)
   * @throws Exception If the travel-time setup fails
   */
  public void generate(String modelPath, String serializedPath) throws Exception {
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
}
