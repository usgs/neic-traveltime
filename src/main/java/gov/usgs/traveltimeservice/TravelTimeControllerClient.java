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

  @Post("/traveltime")
  public TravelTimeRequest getTravelTime(@NotBlank TravelTimeRequest request)
      throws TravelTimeException;

  @Post("/traveltimeplot")
  public TravelTimePlotRequest getTravelTimePlot(@NotBlank TravelTimePlotRequest request)
      throws TravelTimeException;
}
