package gov.usgs.traveltimeservice;

import gov.usgs.processingformats.TravelTimeData;
import gov.usgs.processingformats.TravelTimeException;
import gov.usgs.processingformats.TravelTimeRequest;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;
import javax.validation.constraints.NotBlank;

@Client("${traveltime.controller.path:/traveltime}")
public interface TravelTimeControllerClient {

  @Post("/traveltime")
  public TravelTimeData getTravelTime(@NotBlank TravelTimeRequest request)
      throws TravelTimeException;
}
