package gov.usgs.traveltimeservice;

import gov.usgs.processingformats.TravelTimeException;
import gov.usgs.processingformats.TravelTimePlotRequest;
import gov.usgs.processingformats.TravelTimeRequest;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;
import javax.validation.constraints.NotBlank;

@Client("${traveltime.controller.path:/traveltime}")
public interface TravelTimeControllerClient {

  @Post("/traveltime")
  public TravelTimeRequest getTravelTime(@NotBlank TravelTimeRequest request)
      throws TravelTimeException;

  @Post("/traveltimeplot")
  public TravelTimePlotRequest getTravelTime(@NotBlank TravelTimePlotRequest request)
      throws TravelTimeException;
}