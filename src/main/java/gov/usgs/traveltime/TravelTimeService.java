package gov.usgs.traveltime;

import gov.usgs.processingformats.TravelTimeException;
import gov.usgs.processingformats.TravelTimeRequest;
import gov.usgs.processingformats.TravelTimeData;
import gov.usgs.processingformats.TravelTimeService;
import gov.usgs.processingformats.Utility;
import gov.usgs.traveltime.TTSessionLocal;
import java.io.IOException;

public class TravelTimeService implements TravelTimeService {
  /** A String containing the earth model path for the locator, null to use default. */
  private String modelPath = null;

  /**
   * The LocService constructor. Sets up the earth model path.
   *
   * @param modelPath A String containing the earth model path to use
   */
  public TravelTimeService(String modelPath) {
    this.modelPath = modelPath;
  }

/**
   * Function to get a location using the provided input, implementing the location service
   * interface.
   *
   * @param request a Final LocationRequest containing the location request
   * @return A LocationResult containing the resulting location
   * @throws gov.usgs.processingformats.LocationException Throws a LocationException upon certain
   *     severe errors.
   */
  @Override
  public TravelTimeData getTravelTimes(final TravelTimeRequest request) throws TravelTimeException {
    if (request == null) {
      LOGGER.severe("Null request.");
      throw new TravelTimeException("Null request");
    }

    return null;
  }