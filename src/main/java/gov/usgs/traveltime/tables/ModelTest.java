package gov.usgs.traveltime.tables;

import gov.usgs.traveltime.AllBranchReference;
import gov.usgs.traveltime.AuxiliaryTTReference;
import gov.usgs.traveltime.TauUtilities;
import gov.usgs.traveltime.TravelTimeStatus;

/**
 * Class to test main program for travel-time table generation.
 *
 * @author Ray Buland
 */
public class ModelTest {

  /**
   * Test driver for the model generation (replacing Fortran programs Remodl and Setbrn generating
   * *.hed and *.tlb files). Note that in the long run, the Java version will directly populate the
   * travel-time reference classes so that the model tables, etc. can be generated on the fly.
   *
   * @param args Command line arguments
   * @throws Exception On an illegal integration interval
   */
  public static void main(String[] args) throws Exception {
    double sysTime;
    String earthModel = "ak135";
    MakeTables make;
    AuxiliaryTTReference auxTTReference = null;
    AllBranchReference allRef;
    TravelTimeStatus status;

    TablesUtil.deBugLevel = 1;
    sysTime = System.currentTimeMillis();
    make = new MakeTables(earthModel);
    status =
        make.buildModel(
            TauUtilities.getModelPath("m" + earthModel + ".mod"),
            TauUtilities.getModelPath("phases.txt"));

    if (status == TravelTimeStatus.SUCCESS) {
      // Build the branch reference classes.
      // NOTE assumes default model path for now, need to figure out
      // where to get this path. Cmd line arg?
      auxTTReference = new AuxiliaryTTReference(true, false, false, null, null);
      allRef = make.fillInBranchReferenceData(null, auxTTReference);

      System.out.format(
          "\n***** Table generation time: %5.3f *****\n",
          0.001 * (System.currentTimeMillis() - sysTime));

      // allRef.dumpHead();
      // allRef.dumpMod('P', true);
      // allRef.dumpMod('S', true);
      allRef.dumpBranchData(false);
      // allRef.dumpUp('P');
      // allRef.dumpUp('S');
    } else {
      System.out.println("Read status = " + status);
    }
  }
}
