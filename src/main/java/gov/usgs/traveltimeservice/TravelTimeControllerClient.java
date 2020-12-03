package gov.usgs.traveltimeservice;

import gov.usgs.processingformats.TravelTimeException;
import gov.usgs.processingformats.TravelTimeRequest;
import gov.usgs.processingformats.TravelTimeData;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;
import javax.validation.constraints.NotBlank;

@Client("${traveltime.controller.path:/traveltime}")
public interface TravelTimeControllerClient {

  @Post("/traveltime")
  public TravelTimeData getLocation(@NotBlank TravelTimeRequest request) throws TravelTimeException;
}
