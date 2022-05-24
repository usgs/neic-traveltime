package gov.usgs.traveltime;

import gov.usgs.processingformats.Utility;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Regression Test driver for the travel time package.
 *
 * @author John Patton
 */
public class TravelTimeRegressionTest {
  /**
   * These tests are designed as a overall regression tests for the travel time package. If the
   * behavior of the travel time package is changed in such a that the results are expected to
   * change, these tests (specifically the *verification.json files) will need to be updated
   */
  @Test
  public void runAK135Test() {
    System.out.println("runAK135Test:");

    try {
      runModelTest("build/resources/test/ak135ValidationData.json", "build/models/");
    } catch (Exception e) {
      // failure
      Assertions.fail(e.toString());
      return;
    }
  }

  @Test
  public void runCIATest() {
    System.out.println("runCIATest:");

    try {
      runModelTest("build/resources/test/ciaValidationData.json", "build/models/");
    } catch (Exception e) {
      // failure
      Assertions.fail(e.toString());
      return;
    }
  }

  @Test
  public void runCUSTest() {
    System.out.println("runCUSTest:");

    try {
      runModelTest("build/resources/test/cusValidationData.json", "build/models/");
    } catch (Exception e) {
      // failure
      Assertions.fail(e.toString());
      return;
    }
  }

  @Test
  public void runOGSTest() {
    System.out.println("runOGSTest:");

    try {
      runModelTest("build/resources/test/ogsValidationData.json", "build/models/");
    } catch (Exception e) {
      // failure
      Assertions.fail(e.toString());
      return;
    }
  }

  @Test
  public void runWUSTest() {
    System.out.println("runWUSTest:");

    try {
      runModelTest("build/resources/test/wusValidationData.json", "build/models/");
    } catch (Exception e) {
      // failure
      Assertions.fail(e.toString());
      return;
    }
  }

  @Test
  public void runComplexTest() {
    System.out.println("runComplexTest:");

    try {
      runComplexTest("build/resources/test/complexValidationData.json", "build/models/");
    } catch (Exception e) {
      // failure
      Assertions.fail(e.toString());
      return;
    }
  }

  public void runModelTest(String verificationFile, String modelPath) throws Exception {
    System.out.println("runModelTest: " + verificationFile);

    // note that these values are expected to match what was set in GenerateTravelTimeValidationData
    boolean readStats = true;
    boolean readEllip = true;
    boolean readTopo = true;
    boolean returnAllPhases = true;
    boolean returnBackBranches = true;
    boolean tectonic = true;
    String[] phList = null;
    double elevation = 0.0d;

    // load from file
    String verificationString = loadFromFile(verificationFile);

    // parse input string into json
    JSONObject verificationObject;
    try {
      verificationObject = Utility.fromJSONString(verificationString);
    } catch (ParseException e) {
      // parse failure
      Assertions.fail(e.toString());
      return;
    }

    // Initialize the local travel-time manager.
    TravelTimeSession ttLocal = null;

    try {
      ttLocal = new TravelTimeSession(readStats, readEllip, readTopo, modelPath, modelPath);
    } catch (IOException | ClassNotFoundException e) {
      System.out.println("Unable to read travel-time auxiliary data.");
      Assertions.fail("Unable to read travel-time auxiliary data");
      return;
    }

    System.out.println("set up session:");

    // get the model name
    String modelName = verificationObject.get("EarthModel").toString();
    System.out.println(modelName);

    // get the depths
    JSONArray depthArray = (JSONArray) verificationObject.get("Depths");

    // go through all depths
    for (int j = 0; j < depthArray.size(); j++) {
      // get the depth object
      JSONObject depthObject = (JSONObject) depthArray.get(j);

      // get the current depth
      double depth = (double) depthObject.get("Depth");
      System.out.println("Depth " + String.valueOf(depth));

      // setup new session
      ttLocal.newSession(modelName, depth, phList, returnAllPhases, returnBackBranches, tectonic);

      // get the distances
      JSONArray distanceArray = (JSONArray) depthObject.get("Distances");

      // go through all distances
      for (int k = 0; k < distanceArray.size(); k++) {
        // get the distance object
        JSONObject distanceObject = (JSONObject) distanceArray.get(k);

        // get the current distance
        double distance = (double) distanceObject.get("Distance");
        System.out.println("Distance " + String.valueOf(distance));

        // get the expected travel tmes
        JSONArray phaseArray = (JSONArray) distanceObject.get("Phases");

        // get the travel times
        TravelTime ttList = ttLocal.getTravelTimes(elevation, distance);

        // check the size of the tt list
        Assertions.assertEquals(phaseArray.size(), ttList.getNumPhases(), "tt list size:");

        // go through all phases
        for (int l = 0; l < phaseArray.size(); l++) {
          // get the expected data
          JSONObject phaseObject = (JSONObject) phaseArray.get(l);

          String expectedPhase = phaseObject.get("Phase").toString();
          double expectedTravelTime = (double) phaseObject.get("TravelTime");
          double expectedDistanceDerivative = (double) phaseObject.get("DistanceDerivative");
          double expectedDepthDerivative = (double) phaseObject.get("DepthDerivative");
          double expectedRayDerivative = (double) phaseObject.get("RayDerivative");

          double expectedStatisticalSpread = Double.NaN;
          try {
            expectedStatisticalSpread = (double) phaseObject.get("StatisticalSpread");
          } catch (NullPointerException e) {
            System.out.println(
                "Warning: Model:"
                    + modelName
                    + " Distance: "
                    + String.valueOf(distance)
                    + " Phase: "
                    + expectedPhase
                    + " StatisticalSpread is null in validation data set");
            expectedStatisticalSpread = Double.NaN;
          }

          double expectedObservability = Double.NaN;
          try {
            expectedObservability = (double) phaseObject.get("Observability");
          } catch (NullPointerException e) {
            System.out.println(
                "Warning: Model:"
                    + modelName
                    + " Distance: "
                    + String.valueOf(distance)
                    + " Phase: "
                    + expectedPhase
                    + " Observability is null in validation data set");
            expectedObservability = Double.NaN;
          }

          double expectedAssociationWindow = Double.NaN;
          try {
            expectedAssociationWindow = (double) phaseObject.get("AssociationWindow");
          } catch (NullPointerException e) {
            System.out.println(
                "Warning: Model:"
                    + modelName
                    + " Distance: "
                    + String.valueOf(distance)
                    + " Phase: "
                    + expectedPhase
                    + " AssociationWindow is null in validation data set");
            expectedAssociationWindow = Double.NaN;
          }

          String expectedTeleseismicPhaseGroup =
              phaseObject.get("TeleseismicPhaseGroup").toString();

          String expectedAuxiliaryPhaseGroup = null;
          try {
            expectedAuxiliaryPhaseGroup = phaseObject.get("AuxiliaryPhaseGroup").toString();
          } catch (NullPointerException e) {
            // we expect this but toString doesn't like it
            expectedAuxiliaryPhaseGroup = null;
          }

          boolean expectedLocationUseFlag = (boolean) phaseObject.get("LocationUseFlag");
          boolean expectedAssociationWeightFlag =
              (boolean) phaseObject.get("AssociationWeightFlag");
          boolean expectedDepthFlag = (boolean) phaseObject.get("DepthFlag");
          boolean expectedRegionalFlag = (boolean) phaseObject.get("RegionalFlag");

          // check the phase data against the tt data
          TravelTimeData ttData = ttList.getPhase(l);
          Assertions.assertEquals(expectedPhase, ttData.getPhaseCode(), "phase code:");
          Assertions.assertEquals(
              expectedTravelTime, ttData.getTravelTime(), 0.001, "travel time:");
          Assertions.assertEquals(
              expectedDistanceDerivative,
              ttData.getDistanceDerivitive(),
              0.001,
              "distance derivative:");
          Assertions.assertEquals(
              expectedDepthDerivative, ttData.getDepthDerivitive(), 0.0001, "depth derivative:");
          Assertions.assertEquals(
              expectedRayDerivative,
              ttData.getRayDerivative(),
              0.0001,
              "ray parameter derivative:");
          Assertions.assertEquals(
              expectedStatisticalSpread,
              ttData.getStatisticalSpread(),
              0.001,
              "statistical spread:");
          Assertions.assertEquals(
              expectedObservability, ttData.getObservability(), 0.001, "observability:");
          Assertions.assertEquals(
              expectedAssociationWindow, ttData.getAssocWindow(), 0.001, "association window:");
          Assertions.assertEquals(
              expectedTeleseismicPhaseGroup, ttData.getGroupPhaseCode(), "phase group:");
          Assertions.assertEquals(
              expectedAuxiliaryPhaseGroup,
              ttData.getAuxiliaryGroupPhaseCode(),
              "auxiliary phase group:");
          Assertions.assertEquals(expectedRegionalFlag, ttData.getIsRegional(), "regional phase:");
          Assertions.assertEquals(expectedDepthFlag, ttData.getIsDepthSensitive(), "depth phase :");
          Assertions.assertEquals(
              expectedLocationUseFlag, ttData.getLocationCanUse(), "location use:");
          Assertions.assertEquals(
              expectedAssociationWeightFlag, ttData.getAssocDownWeight(), "association weight:");

          System.out.println("Done with phase " + expectedPhase);
        }
      }
    }
  }

  public void runComplexTest(String verificationFile, String modelPath) throws Exception {
    System.out.println("runComplexTest: " + verificationFile);

    // note that these values are expected to match what was set in GenerateTravelTimeValidationData
    boolean readStats = true;
    boolean readEllip = true;
    boolean readTopo = true;
    boolean returnAllPhases = true;
    boolean returnBackBranches = true;
    boolean rstt = false;
    boolean tectonic = true;
    String[] phList = null;

    // load from file
    String verificationString = loadFromFile(verificationFile);

    // parse input string into json
    JSONObject verificationObject;
    try {
      verificationObject = Utility.fromJSONString(verificationString);
    } catch (ParseException e) {
      // parse failure
      Assertions.fail(e.toString());
      return;
    }

    // Initialize the local travel-time manager.
    TravelTimeSession ttLocal = null;

    try {
      ttLocal = new TravelTimeSession(readStats, readEllip, readTopo, modelPath, modelPath);
    } catch (IOException | ClassNotFoundException e) {
      System.out.println("Unable to read travel-time auxiliary data.");
      Assertions.fail("Unable to read travel-time auxiliary data");
      return;
    }

    System.out.println("set up session:");

    // get the source values
    double sourceLatitude = (double) verificationObject.get("SourceLatitude");
    double sourceLongitude = (double) verificationObject.get("SourceLongitude");
    double sourceDepth = (double) verificationObject.get("SourceDepth");

    // setup new complex session
    ttLocal.newSession(
        "ak135",
        sourceDepth,
        phList,
        sourceLatitude,
        sourceLongitude,
        returnAllPhases,
        returnBackBranches,
        tectonic);

    // get the points
    JSONArray pointArray = (JSONArray) verificationObject.get("Points");

    // go through all Points
    for (int j = 0; j < pointArray.size(); j++) {
      // get the point object
      JSONObject pointObject = (JSONObject) pointArray.get(j);

      // get the current receiver
      double recieverLatitude = (double) pointObject.get("RecieverLatitude");
      double receiverLongitude = (double) pointObject.get("ReceiverLongitude");
      double receiverElevation = (double) pointObject.get("ReceiverElevation");
      double receiverDistance = Double.NaN;
      double recieverAzimuth = Double.NaN;

      // get the expected travel tmes
      JSONArray phaseArray = (JSONArray) pointObject.get("Phases");

      // get the travel times (complex)
      TravelTime ttList =
          ttLocal.getTravelTimes(
              recieverLatitude,
              receiverLongitude,
              receiverElevation,
              receiverDistance,
              recieverAzimuth);

      // check the size of the tt list
      Assertions.assertEquals(phaseArray.size(), ttList.getNumPhases(), "tt list size:");

      // go through all phases
      for (int l = 0; l < phaseArray.size(); l++) {
        // get the expected data
        JSONObject phaseObject = (JSONObject) phaseArray.get(l);

        String expectedPhase = phaseObject.get("Phase").toString();
        double expectedTravelTime = (double) phaseObject.get("TravelTime");
        double expectedDistanceDerivative = (double) phaseObject.get("DistanceDerivative");
        double expectedDepthDerivative = (double) phaseObject.get("DepthDerivative");
        double expectedRayDerivative = (double) phaseObject.get("RayDerivative");

        double expectedStatisticalSpread = Double.NaN;
        try {
          expectedStatisticalSpread = (double) phaseObject.get("StatisticalSpread");
        } catch (NullPointerException e) {
          System.out.println(
              "Warning: Complex Request:"
                  + " Point: "
                  + String.valueOf(recieverLatitude)
                  + ", "
                  + String.valueOf(receiverLongitude)
                  + " Phase: "
                  + expectedPhase
                  + " StatisticalSpread is null in validation data set");
          expectedStatisticalSpread = Double.NaN;
        }

        double expectedObservability = Double.NaN;
        try {
          expectedObservability = (double) phaseObject.get("Observability");
        } catch (NullPointerException e) {
          System.out.println(
              "Warning: Complex Request:"
                  + " Point: "
                  + String.valueOf(recieverLatitude)
                  + ", "
                  + String.valueOf(receiverLongitude)
                  + " Phase: "
                  + expectedPhase
                  + " Observability is null in validation data set");
          expectedObservability = Double.NaN;
        }

        double expectedAssociationWindow = Double.NaN;
        try {
          expectedAssociationWindow = (double) phaseObject.get("AssociationWindow");
        } catch (NullPointerException e) {
          System.out.println(
              "Warning: Complex Request:"
                  + " Point: "
                  + String.valueOf(recieverLatitude)
                  + ", "
                  + String.valueOf(receiverLongitude)
                  + " Phase: "
                  + expectedPhase
                  + " AssociationWindow is null in validation data set");
          expectedAssociationWindow = Double.NaN;
        }

        String expectedTeleseismicPhaseGroup = phaseObject.get("TeleseismicPhaseGroup").toString();

        String expectedAuxiliaryPhaseGroup = null;
        try {
          expectedAuxiliaryPhaseGroup = phaseObject.get("AuxiliaryPhaseGroup").toString();
        } catch (NullPointerException e) {
          // we expect this but toString doesn't like it
          expectedAuxiliaryPhaseGroup = null;
        }

        boolean expectedLocationUseFlag = (boolean) phaseObject.get("LocationUseFlag");
        boolean expectedAssociationWeightFlag = (boolean) phaseObject.get("AssociationWeightFlag");
        boolean expectedDepthFlag = (boolean) phaseObject.get("DepthFlag");
        boolean expectedRegionalFlag = (boolean) phaseObject.get("RegionalFlag");

        // check the phase data against the tt data
        TravelTimeData ttData = ttList.getPhase(l);
        Assertions.assertEquals(expectedPhase, ttData.getPhaseCode(), "phase code:");
        Assertions.assertEquals(expectedTravelTime, ttData.getTravelTime(), 0.001, "travel time:");
        Assertions.assertEquals(
            expectedDistanceDerivative,
            ttData.getDistanceDerivitive(),
            0.001,
            "distance derivative:");
        Assertions.assertEquals(
            expectedDepthDerivative, ttData.getDepthDerivitive(), 0.0001, "depth derivative:");
        Assertions.assertEquals(
            expectedRayDerivative, ttData.getRayDerivative(), 0.0001, "ray parameter derivative:");
        Assertions.assertEquals(
            expectedStatisticalSpread, ttData.getStatisticalSpread(), 0.001, "statistical spread:");
        Assertions.assertEquals(
            expectedObservability, ttData.getObservability(), 0.001, "observability:");
        Assertions.assertEquals(
            expectedAssociationWindow, ttData.getAssocWindow(), 0.001, "association window:");
        Assertions.assertEquals(
            expectedTeleseismicPhaseGroup, ttData.getGroupPhaseCode(), "phase group:");
        Assertions.assertEquals(
            expectedAuxiliaryPhaseGroup,
            ttData.getAuxiliaryGroupPhaseCode(),
            "auxiliary phase group:");
        Assertions.assertEquals(expectedRegionalFlag, ttData.getIsRegional(), "regional phase:");
        Assertions.assertEquals(expectedDepthFlag, ttData.getIsDepthSensitive(), "depth phase :");
        Assertions.assertEquals(
            expectedLocationUseFlag, ttData.getLocationCanUse(), "location use:");
        Assertions.assertEquals(
            expectedAssociationWeightFlag, ttData.getAssocDownWeight(), "association weight:");

        System.out.println("Done with phase " + expectedPhase);
      }
    }
  }

  /**
   * This function loads the data in the given file path as a string.
   *
   * @param filePath A String containing the path to the file to load
   * @return A String containing the data in the file
   */
  private String loadFromFile(String filePath) {
    System.out.println("loadFromFile: " + filePath);

    // read the input
    BufferedReader inputReader = null;
    String fileString = "";
    try {
      inputReader = new BufferedReader(new FileReader(filePath));
      String text = null;
      // int line = 0;

      // each line is assumed to be part of the input
      while ((text = inputReader.readLine()) != null) {
        fileString += text;
        // line++;
        // System.out.println("loadFromFile: line " + String.valueOf(line));
      }
    } catch (FileNotFoundException e) {
      // no file
      System.out.println("File not found");
      return "";
    } catch (IOException e) {
      // problem reading
      return "";
    } finally {
      try {
        if (inputReader != null) {
          inputReader.close();
        }
      } catch (IOException e) {
        // can't close
      }
    }

    System.out.println("loadFromFile complete:");

    return fileString;
  }
}
