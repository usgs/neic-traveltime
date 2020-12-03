package gov.usgs.traveltime;

import gov.usgs.processingformats.TravelTimeData;
import gov.usgs.processingformats.TravelTimeException;
import gov.usgs.processingformats.TravelTimeRequest;
import gov.usgs.processingformats.TravelTimeService;

public class TTService implements TravelTimeService {
  /** A String containing the earth model path for the locator, null to use default. */
  private String modelPath = null;

  /**
   * The TTService constructor. Sets up the earth model path.
   *
   * @param modelPath A String containing the earth model path to use
   */
  public TTService(String modelPath) {
    this.modelPath = modelPath;
  }

  /**
   * Function to get a travel time using the provided input, implementing the TravelTime service
   * interface.
   *
   * @param request a Final TravelTimeRequest containing the TravelTime request
   * @return A TravelTimeResult containing the resulting TravelTime
   * @throws gov.usgs.processingformats.TravelTimeException Throws a TravelTimeException upon
   *     certain severe errors.
   */
  @Override
  public TravelTimeData getTravelTimes(final TravelTimeRequest request) throws TravelTimeException {
    if (request == null) {
      // LOGGER.severe("Null request.");
      throw new TravelTimeException("Null request");
    }

    return null;
  }
}
