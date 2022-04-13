package gov.usgs.traveltime;

import gov.usgs.processingformats.Utility;
import gov.usgs.traveltime.session.*;
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
   * This test is designed as a overall regression test for the traveltime package. If the behavior
   * of the traveltime package is changed in such a that the travel time results are expected to
   * change, this test will need to be updated
   */
  /*  @Test
  public void testAK135TravelTimes() {
    String modelPath = "build/models/";
    String earthModel = "ak135";
    double sourceDepth = 10.0;
    boolean returnAllPhases = false;
    boolean returnBackBranches = false;
    boolean rstt = false;
    boolean tectonic = false;
    String[] phList = null;
    double delta1 = 1.0;
    double delta2 = 90.0;
    double delta3 = 180.0;
    double elev = 0.0d;
    TravelTimeLocalSession ttLocal = null;

    try {
      // Initialize the local travel-time manager.
      ttLocal = new TravelTimeLocalSession(true, true, true, modelPath, modelPath);
    } catch (IOException | ClassNotFoundException e) {
      System.out.println("Unable to read travel-time auxiliary data.");
      Assertions.fail("Unable to read travel-time auxiliary data");
    }

    // Set up a simple session.
    try {
      ttLocal.newSession(
          earthModel, sourceDepth, phList, returnAllPhases, returnBackBranches, tectonic, rstt);

    } catch (Exception e) {
      System.out.println("Session setup failed");
      Assertions.fail("Session setup failed");
    }

    // check supported modles
    String[] models = ttLocal.getAvailModels();
    Assertions.assertEquals(5, models.length, "Model Count Check:");

    // check size of branch list
    Assertions.assertEquals(86, ttLocal.getBranchCount(returnAllPhases), "BranchList Size Check:");

    // check the travel time package at distance of 1 degree
    TravelTime ttList1 = ttLocal.getTT(elev, delta1);

    // check number of TravelTimes
    Assertions.assertEquals(20, ttList1.getNumPhases(), "TravelTimes Size Check:");

    // check the first TravelTime
    TravelTimeData firstTravelTime = ttList1.getPhase(0);
    Assertions.assertEquals("Pg", firstTravelTime.getphaseCode(), "TravelTime first phase code:");
    Assertions.assertEquals(
        19.234, firstTravelTime.getTT(), 0.001, "TravelTime first arrival time:");
    Assertions.assertEquals(
        19.070, firstTravelTime.getDTdD(), 0.001, "TravelTime first tangential derivative time:");
    Assertions.assertEquals(
        0.0148, firstTravelTime.getDTdZ(), 0.0001, "TravelTime first vertical derivative time:");
    Assertions.assertEquals(
        0.0012,
        firstTravelTime.getDXdP(),
        0.0001,
        "TravelTime first ray parameter derivative time:");
    Assertions.assertEquals(0.666, firstTravelTime.getSpread(), 0.001, "TravelTime first spread:");
    Assertions.assertEquals(
        14873.999, firstTravelTime.getObserv(), 0.001, "TravelTime first observability:");
    Assertions.assertEquals(
        5.0, firstTravelTime.getWindow(), 0.001, "TravelTime first association window:");
    Assertions.assertEquals("P", firstTravelTime.getPhaseGroup(), "TravelTime first phase group:");
    Assertions.assertEquals(
        "PKP", firstTravelTime.getAuxGroup(), "TravelTime first auxiliary phase group:");
    Assertions.assertEquals(true, firstTravelTime.isRegional(), "TravelTime first regional flag:");
    Assertions.assertEquals(false, firstTravelTime.isDepth(), "TravelTime first depth flag:");
    Assertions.assertEquals(true, firstTravelTime.canUse(), "TravelTime first phase use flag:");
    Assertions.assertEquals(false, firstTravelTime.getDis(), "TravelTime first disrespect flag:");

    // check the travel time package at distance of 90 degrees
    TravelTime ttList2 = ttLocal.getTT(elev, delta2);

    // check number of TravelTimes
    Assertions.assertEquals(32, ttList2.getNumPhases(), "TravelTimes Size Check:");

    // check the first TravelTime
    firstTravelTime = ttList2.getPhase(0);
    Assertions.assertEquals("P", firstTravelTime.getphaseCode(), "TravelTime first phase code:");
    Assertions.assertEquals(
        779.729, firstTravelTime.getTT(), 0.001, "TravelTime first arrival time:");
    Assertions.assertEquals(
        4.655, firstTravelTime.getDTdD(), 0.001, "TravelTime first tangential derivative time:");
    Assertions.assertEquals(
        -0.1672, firstTravelTime.getDTdZ(), 0.0001, "TravelTime first vertical derivative time:");
    Assertions.assertEquals(
        -0.0088,
        firstTravelTime.getDXdP(),
        0.0001,
        "TravelTime first ray parameter derivative time:");
    Assertions.assertEquals(1.102, firstTravelTime.getSpread(), 0.001, "TravelTime first spread:");
    Assertions.assertEquals(
        12898.048, firstTravelTime.getObserv(), 0.001, "TravelTime first observability:");
    Assertions.assertEquals(
        7.715, firstTravelTime.getWindow(), 0.001, "TravelTime first association window:");
    Assertions.assertEquals("P", firstTravelTime.getPhaseGroup(), "TravelTime first phase group:");
    Assertions.assertEquals(
        "PKP", firstTravelTime.getAuxGroup(), "TravelTime first auxiliary phase group:");
    Assertions.assertEquals(false, firstTravelTime.isRegional(), "TravelTime first regional flag:");
    Assertions.assertEquals(false, firstTravelTime.isDepth(), "TravelTime first depth flag:");
    Assertions.assertEquals(true, firstTravelTime.canUse(), "TravelTime first phase use flag:");
    Assertions.assertEquals(false, firstTravelTime.getDis(), "TravelTime first disrespect flag:");

    // check the travel time package at distance of 180 degrees
    TravelTime ttList3 = ttLocal.getTT(elev, delta3);

    // check number of TravelTimes
    Assertions.assertEquals(12, ttList3.getNumPhases(), "TravelTimes Size Check:");

    // check the first TravelTime
    firstTravelTime = ttList3.getPhase(0);
    Assertions.assertEquals(
        "PKPdf", firstTravelTime.getphaseCode(), "TravelTime first phase code:");
    Assertions.assertEquals(
        1210.790, firstTravelTime.getTT(), 0.001, "TravelTime first arrival time:");
    Assertions.assertEquals(
        3.315E-9, firstTravelTime.getDTdD(), 0.001, "TravelTime first tangential derivative time:");
    Assertions.assertEquals(
        -0.1724, firstTravelTime.getDTdZ(), 0.0001, "TravelTime first vertical derivative time:");
    Assertions.assertEquals(
        10.278, firstTTime.getWindow(), 0.001, "tTime first association window:");
    Assertions.assertEquals("PKP", firstTTime.getPhGroup(), "tTime first phase group:");
    Assertions.assertEquals("P", firstTTime.getAuxGroup(), "tTime first auxiliary phase group:");
    Assertions.assertEquals(false, firstTTime.isRegional(), "tTime first regional flag:");
    Assertions.assertEquals(false, firstTTime.isDepth(), "tTime first depth flag:");
    Assertions.assertEquals(true, firstTTime.canUse(), "tTime first phase use flag:");
    Assertions.assertEquals(false, firstTTime.getDis(), "tTime first disrespect flag:");
  }*/

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

  public void runModelTest(String verificationFile, String modelPath) throws Exception {
    System.out.println("runModelTest: " + verificationFile);

    // note that these values are expected to match what was set in GenerateTravelTimeValidationData
    boolean readStats = true;
    boolean readEllip = true;
    boolean readTopo = true;
    boolean returnAllPhases = true;
    boolean returnBackBranches = true;
    boolean rstt = false;
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
    TTSessionLocal ttLocal = null;

    try {
      ttLocal = new TTSessionLocal(readStats, readEllip, readTopo, modelPath, modelPath);
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
      ttLocal.newSession(
          modelName, depth, phList, returnAllPhases, returnBackBranches, tectonic, rstt);

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
        TTime ttList = ttLocal.getTT(elevation, distance);

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
          TTimeData ttData = ttList.getPhase(l);
          Assertions.assertEquals(expectedPhase, ttData.getPhCode(), "phase code:");
          Assertions.assertEquals(expectedTravelTime, ttData.getTT(), 0.001, "travel time:");
          Assertions.assertEquals(
              expectedDistanceDerivative, ttData.getDTdD(), 0.001, "distance derivative:");
          Assertions.assertEquals(
              expectedDepthDerivative, ttData.getDTdZ(), 0.0001, "depth derivative:");
          Assertions.assertEquals(
              expectedRayDerivative, ttData.getDXdP(), 0.0001, "ray parameter derivative:");
          Assertions.assertEquals(
              expectedStatisticalSpread, ttData.getSpread(), 0.001, "statistical spread:");
          Assertions.assertEquals(
              expectedObservability, ttData.getObserv(), 0.001, "observability:");
          Assertions.assertEquals(
              expectedAssociationWindow, ttData.getWindow(), 0.001, "association window:");
          Assertions.assertEquals(
              expectedTeleseismicPhaseGroup, ttData.getPhGroup(), "phase group:");
          Assertions.assertEquals(
              expectedAuxiliaryPhaseGroup, ttData.getAuxGroup(), "auxiliary phase group:");
          Assertions.assertEquals(expectedRegionalFlag, ttData.isRegional(), "regional phase:");
          Assertions.assertEquals(expectedDepthFlag, ttData.isDepth(), "depth phase :");
          Assertions.assertEquals(expectedLocationUseFlag, ttData.canUse(), "location use:");
          Assertions.assertEquals(
              expectedAssociationWeightFlag, ttData.getDis(), "association weight:");

          System.out.println("Done with phase " + expectedPhase);
        }
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
