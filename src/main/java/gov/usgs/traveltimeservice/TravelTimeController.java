package gov.usgs.traveltimeservice;

import gov.usgs.processingformats.TravelTimeException;
import gov.usgs.processingformats.TravelTimePlotRequest;
import gov.usgs.processingformats.TravelTimeRequest;
import gov.usgs.traveltime.TravelTimeService;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.swagger.v3.oas.annotations.Hidden;
import java.net.URI;

/**
 * The TravelTimeController is a required Micronaut class defining the desired uri's and endpoints
 * for travel time webservices, and linking those endpoints with the corresponding TravelTimeService
 * methods.
 *
 * @author John Patton
 */
@Controller("/traveltimeservices")
public class TravelTimeController {

  /**
   * A string containing the path to the travel time models, automatically populated by Micronaut
   * from the traveltime.model.path environment varible, defaulting to ./build/models/ if the
   * environment varible is not present.
   */
  @Value("${traveltime.model.path:./build/models/}")
  protected String modelPath;

  /**
   * A string containing the path to the travel serialized files, automatically populated by
   * Micronaut from the traveltime.serialized.path environment varible, defaulting to
   * ./build/models/ if the environment varible is not present.
   */
  @Value("${traveltime.serialized.path:./build/models/}")
  protected String serializedPath;

  /**
   * Function to setup the default root endpoint, pointing to index.html
   *
   * @return returns a HttpResponse containing index.html
   */
  @Get(uri = "/", produces = MediaType.TEXT_HTML)
  @Hidden
  public HttpResponse getIndex() {
    return HttpResponse.redirect(URI.create("/traveltimeservices/index.html"));
  }

  /**
   * Function to setup the travel time endpoint.
   *
   * @param request a final TravelTimeRequest containing the TravelTime request
   * @return A TravelTimeRequest with the response section containing the resulting TravelTime data
   * @throws gov.usgs.processingformats.TravelTimeException Throws a TravelTimeException upon
   *     certain severe errors.
   */
  @Post(uri = "/traveltime", consumes = MediaType.APPLICATION_JSON)
  public TravelTimeRequest getTravelTime(@Body TravelTimeRequest request)
      throws TravelTimeException {
    TravelTimeService service = new TravelTimeService(modelPath, serializedPath);
    return service.getTravelTimes(request);
  }

  /**
   * Function to setup the travel time plot endpoint.
   *
   * @param request a final TravelTimePlotRequest containing the TravelTime plot request
   * @return A TravelTimePlotRequest with the response section containing the resulting TravelTime
   *     plot data
   * @throws gov.usgs.processingformats.TravelTimeException Throws a TravelTimeException upon
   *     certain severe errors.
   */
  @Post(uri = "/traveltimeplot", consumes = MediaType.APPLICATION_JSON)
  public TravelTimePlotRequest getTravelTimePlot(@Body TravelTimePlotRequest request)
      throws TravelTimeException {
    TravelTimeService service = new TravelTimeService(modelPath, serializedPath);
    return service.getTravelTimePlot(request);
  }
}
