package gov.usgs.traveltimeservice;

import gov.usgs.processingformats.TravelTimeData;
import gov.usgs.processingformats.TravelTimeException;
import gov.usgs.processingformats.TravelTimeRequest;
import gov.usgs.traveltime.TTService;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.Hidden;
import java.net.URI;

@Controller("/ws/traveltime")
public class TravelTimeController {

  @Value("${traveltime.model.path:./build/models/}")
  protected String modelPath;

  @Get(uri = "/", produces = MediaType.TEXT_HTML)
  @Hidden
  public HttpResponse getIndex() {
    return HttpResponse.redirect(URI.create("/ws/traveltime/index.html"));
  }

  @Post(uri = "/traveltime", consumes = MediaType.APPLICATION_JSON)
  public TravelTimeData getTravelTime(@Body TravelTimeRequest request) throws TravelTimeException {
    TTService service = new TTService(modelPath);
    return service.getTravelTimes(request);
  }
}
