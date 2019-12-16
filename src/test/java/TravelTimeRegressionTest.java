package gov.usgs.traveltime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import gov.usgs.traveltime.session.*;
import java.io.IOException;
import org.junit.Test;

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
    } catch (IOException e) {
      System.out.println("Unable to read travel-time auxiliary data.");
      fail("Unable to read travel-time auxiliary data");
    }

    // Set up a simple session.
    try {
      ttLocal.newSession(
          earthModel, sourceDepth, phList, returnAllPhases, returnBackBranches, tectonic, rstt);

    } catch (Exception e) {
      System.out.println("Session setup failed");
      fail("Session setup failed");
    }

    // check supported modles
    String[] models = ttLocal.getAvailModels();
    assertEquals("Model Count Check:", 5, models.length);

    // check size of branch list
    assertEquals("BranchList Size Check:", 86, ttLocal.getBranchCount(returnAllPhases));

    // check the travel time package at distance of 1 degree
    TTime ttList1 = ttLocal.getTT(elev, delta1);

    // check number of ttimes
    assertEquals("tTimes Size Check:", 20, ttList1.getNumPhases());

    // check the first ttime
    TTimeData firstTTime = ttList1.getPhase(0);
    assertEquals("tTime first phase code:", "Pg", firstTTime.getPhCode());
    assertEquals("tTime first arrival time:", 19.234, firstTTime.getTT(), 0.001);
    assertEquals("tTime first tangential derivative time:", 19.070, firstTTime.getDTdD(), 0.001);
    assertEquals("tTime first vertical derivative time:", 0.0148, firstTTime.getDTdZ(), 0.0001);
    assertEquals(
        "tTime first ray parameter derivative time:", 0.0012, firstTTime.getDXdP(), 0.0001);
    assertEquals("tTime first spread:", 0.666, firstTTime.getSpread(), 0.001);
    assertEquals("tTime first observability:", 14873.999, firstTTime.getObserv(), 0.001);
    assertEquals("tTime first association window:", 5.0, firstTTime.getWindow(), 0.001);
    assertEquals("tTime first phase group:", "P", firstTTime.getPhGroup());
    assertEquals("tTime first auxiliary phase group:", "PKP", firstTTime.getAuxGroup());
    assertEquals("tTime first regional flag:", true, firstTTime.isRegional());
    assertEquals("tTime first depth flag:", false, firstTTime.isDepth());
    assertEquals("tTime first phase use flag:", true, firstTTime.canUse());
    assertEquals("tTime first disrespect flag:", false, firstTTime.getDis());

    // check the travel time package at distance of 90 degrees
    TTime ttList2 = ttLocal.getTT(elev, delta2);

    // check number of ttimes
    assertEquals("tTimes Size Check:", 32, ttList2.getNumPhases());

    // check the first ttime
    firstTTime = ttList2.getPhase(0);
    assertEquals("tTime first phase code:", "P", firstTTime.getPhCode());
    assertEquals("tTime first arrival time:", 779.729, firstTTime.getTT(), 0.001);
    assertEquals("tTime first tangential derivative time:", 4.655, firstTTime.getDTdD(), 0.001);
    assertEquals("tTime first vertical derivative time:", -0.1672, firstTTime.getDTdZ(), 0.0001);
    assertEquals(
        "tTime first ray parameter derivative time:", -0.0088, firstTTime.getDXdP(), 0.0001);
    assertEquals("tTime first spread:", 1.102, firstTTime.getSpread(), 0.001);
    assertEquals("tTime first observability:", 12898.048, firstTTime.getObserv(), 0.001);
    assertEquals("tTime first association window:", 7.715, firstTTime.getWindow(), 0.001);
    assertEquals("tTime first phase group:", "P", firstTTime.getPhGroup());
    assertEquals("tTime first auxiliary phase group:", "PKP", firstTTime.getAuxGroup());
    assertEquals("tTime first regional flag:", false, firstTTime.isRegional());
    assertEquals("tTime first depth flag:", false, firstTTime.isDepth());
    assertEquals("tTime first phase use flag:", true, firstTTime.canUse());
    assertEquals("tTime first disrespect flag:", false, firstTTime.getDis());

    // check the travel time package at distance of 180 degrees
    TTime ttList3 = ttLocal.getTT(elev, delta3);

    // check number of ttimes
    assertEquals("tTimes Size Check:", 12, ttList3.getNumPhases());

    // check the first ttime
    firstTTime = ttList3.getPhase(0);
    assertEquals("tTime first phase code:", "PKPdf", firstTTime.getPhCode());
    assertEquals("tTime first arrival time:", 1210.790, firstTTime.getTT(), 0.001);
    assertEquals("tTime first tangential derivative time:", 3.315E-9, firstTTime.getDTdD(), 0.001);
    assertEquals("tTime first vertical derivative time:", -0.1724, firstTTime.getDTdZ(), 0.0001);
    assertEquals(
        "tTime first ray parameter derivative time:", -0.0052, firstTTime.getDXdP(), 0.0001);
    assertEquals("tTime first spread:", 1.468, firstTTime.getSpread(), 0.001);
    assertEquals("tTime first observability:", 3.733, firstTTime.getObserv(), 0.001);
    assertEquals("tTime first association window:", 10.278, firstTTime.getWindow(), 0.001);
    assertEquals("tTime first phase group:", "PKP", firstTTime.getPhGroup());
    assertEquals("tTime first auxiliary phase group:", "P", firstTTime.getAuxGroup());
    assertEquals("tTime first regional flag:", false, firstTTime.isRegional());
    assertEquals("tTime first depth flag:", false, firstTTime.isDepth());
    assertEquals("tTime first phase use flag:", true, firstTTime.canUse());
    assertEquals("tTime first disrespect flag:", false, firstTTime.getDis());
  }
}
