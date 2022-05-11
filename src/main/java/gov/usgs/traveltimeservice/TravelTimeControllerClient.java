package gov.usgs.traveltimeservice;

import gov.usgs.processingformats.TravelTimeException;
import gov.usgs.processingformats.TravelTimePlotRequest;
import gov.usgs.processingformats.TravelTimeRequest;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;
import javax.validation.constraints.NotBlank;

/**
 * The TravelTimeController is a required Micronaut interface defining the desired endpoints and
 * functions for travel time webservices.
 *
 * @author John Patton
 */
@Client("${traveltime.controller.path:/traveltime}")
public interface TravelTimeControllerClient {

  /**
   * Interface for the travel time endpoint.
   *
   * @param request a final TravelTimeRequest containing the TravelTime request
   * @return A TravelTimeRequest with the response section containing the resulting TravelTime data
   * @throws gov.usgs.processingformats.TravelTimeException Throws a TravelTimeException upon
   *     certain severe errors.
   */    
  @Post("/traveltime")
  public TravelTimeRequest getTravelTime(@NotBlank TravelTimeRequest request)
      throws TravelTimeException;

  /**
   * Interface for the travel time plot endpoint.
   *
   * @param request a final TravelTimePlotRequest containing the TravelTime plot request
   * @return A TravelTimePlotRequest with the response section containing the resulting TravelTime
   *     plot data
   * @throws gov.usgs.processingformats.TravelTimeException Throws a TravelTimeException upon
   *     certain severe errors.
   */      
  @Post("/traveltimeplot")
  public TravelTimePlotRequest getTravelTimePlot(@NotBlank TravelTimePlotRequest request)
      throws TravelTimeException;
}
