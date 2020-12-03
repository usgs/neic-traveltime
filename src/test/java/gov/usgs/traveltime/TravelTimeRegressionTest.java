package gov.usgs.traveltime;

import gov.usgs.traveltime.session.*;
import java.io.IOException;
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
  @Test
  public void testTravelTimes() {
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
    TTSessionLocal ttLocal = null;

    try {
      // Initialize the local travel-time manager.
      ttLocal = new TTSessionLocal(true, true, true, modelPath);
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
    TTime ttList1 = ttLocal.getTT(elev, delta1);

    // check number of ttimes
    Assertions.assertEquals(20, ttList1.getNumPhases(), "tTimes Size Check:");

    // check the first ttime
    TTimeData firstTTime = ttList1.getPhase(0);
    Assertions.assertEquals("Pg", firstTTime.getPhCode(), "tTime first phase code:");
    Assertions.assertEquals(19.234, firstTTime.getTT(), 0.001, "tTime first arrival time:");
    Assertions.assertEquals(
        19.070, firstTTime.getDTdD(), 0.001, "tTime first tangential derivative time:");
    Assertions.assertEquals(
        0.0148, firstTTime.getDTdZ(), 0.0001, "tTime first vertical derivative time:");
    Assertions.assertEquals(
        0.0012, firstTTime.getDXdP(), 0.0001, "tTime first ray parameter derivative time:");
    Assertions.assertEquals(0.666, firstTTime.getSpread(), 0.001, "tTime first spread:");
    Assertions.assertEquals(14873.999, firstTTime.getObserv(), 0.001, "tTime first observability:");
    Assertions.assertEquals(5.0, firstTTime.getWindow(), 0.001, "tTime first association window:");
    Assertions.assertEquals("P", firstTTime.getPhGroup(), "tTime first phase group:");
    Assertions.assertEquals("PKP", firstTTime.getAuxGroup(), "tTime first auxiliary phase group:");
    Assertions.assertEquals(true, firstTTime.isRegional(), "tTime first regional flag:");
    Assertions.assertEquals(false, firstTTime.isDepth(), "tTime first depth flag:");
    Assertions.assertEquals(true, firstTTime.canUse(), "tTime first phase use flag:");
    Assertions.assertEquals(false, firstTTime.getDis(), "tTime first disrespect flag:");

    // check the travel time package at distance of 90 degrees
    TTime ttList2 = ttLocal.getTT(elev, delta2);

    // check number of ttimes
    Assertions.assertEquals(32, ttList2.getNumPhases(), "tTimes Size Check:");

    // check the first ttime
    firstTTime = ttList2.getPhase(0);
    Assertions.assertEquals("P", firstTTime.getPhCode(), "tTime first phase code:");
    Assertions.assertEquals(779.729, firstTTime.getTT(), 0.001, "tTime first arrival time:");
    Assertions.assertEquals(
        4.655, firstTTime.getDTdD(), 0.001, "tTime first tangential derivative time:");
    Assertions.assertEquals(
        -0.1672, firstTTime.getDTdZ(), 0.0001, "tTime first vertical derivative time:");
    Assertions.assertEquals(
        -0.0088, firstTTime.getDXdP(), 0.0001, "tTime first ray parameter derivative time:");
    Assertions.assertEquals(1.102, firstTTime.getSpread(), 0.001, "tTime first spread:");
    Assertions.assertEquals(12898.048, firstTTime.getObserv(), 0.001, "tTime first observability:");
    Assertions.assertEquals(
        7.715, firstTTime.getWindow(), 0.001, "tTime first association window:");
    Assertions.assertEquals("P", firstTTime.getPhGroup(), "tTime first phase group:");
    Assertions.assertEquals("PKP", firstTTime.getAuxGroup(), "tTime first auxiliary phase group:");
    Assertions.assertEquals(false, firstTTime.isRegional(), "tTime first regional flag:");
    Assertions.assertEquals(false, firstTTime.isDepth(), "tTime first depth flag:");
    Assertions.assertEquals(true, firstTTime.canUse(), "tTime first phase use flag:");
    Assertions.assertEquals(false, firstTTime.getDis(), "tTime first disrespect flag:");

    // check the travel time package at distance of 180 degrees
    TTime ttList3 = ttLocal.getTT(elev, delta3);

    // check number of ttimes
    Assertions.assertEquals(12, ttList3.getNumPhases(), "tTimes Size Check:");

    // check the first ttime
    firstTTime = ttList3.getPhase(0);
    Assertions.assertEquals("PKPdf", firstTTime.getPhCode(), "tTime first phase code:");
    Assertions.assertEquals(1210.790, firstTTime.getTT(), 0.001, "tTime first arrival time:");
    Assertions.assertEquals(
        3.315E-9, firstTTime.getDTdD(), 0.001, "tTime first tangential derivative time:");
    Assertions.assertEquals(
        -0.1724, firstTTime.getDTdZ(), 0.0001, "tTime first vertical derivative time:");
    Assertions.assertEquals(
        -0.0052, firstTTime.getDXdP(), 0.0001, "tTime first ray parameter derivative time:");
    Assertions.assertEquals(1.468, firstTTime.getSpread(), 0.001, "tTime first spread:");
    Assertions.assertEquals(3.733, firstTTime.getObserv(), 0.001, "tTime first observability:");
    Assertions.assertEquals(
        10.278, firstTTime.getWindow(), 0.001, "tTime first association window:");
    Assertions.assertEquals("PKP", firstTTime.getPhGroup(), "tTime first phase group:");
    Assertions.assertEquals("P", firstTTime.getAuxGroup(), "tTime first auxiliary phase group:");
    Assertions.assertEquals(false, firstTTime.isRegional(), "tTime first regional flag:");
    Assertions.assertEquals(false, firstTTime.isDepth(), "tTime first depth flag:");
    Assertions.assertEquals(true, firstTTime.canUse(), "tTime first phase use flag:");
    Assertions.assertEquals(false, firstTTime.getDis(), "tTime first disrespect flag:");
  }
}
