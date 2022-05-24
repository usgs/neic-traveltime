package gov.usgs.traveltimeservice;

import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(
    info =
        @Info(
            title = "USGS Travel Time webservice",
            version = "0.2.1",
            description =
                "Webservice used by the USGS NEIC to request seismic travel times and travel time plot information."))
public class Application {

  /**
   * Main program for the travel-time web service.
   *
   * @param args An array of Strings containing the command line arguments (not used)
   */
  public static void main(String[] args) {
    Micronaut.run(Application.class);
  }
}
