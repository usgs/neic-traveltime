package gov.usgs.traveltimeservice;

import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(
    info =
        @Info(
            title = "traveltime service",
            version = "0.0",
            description = "traveltime service description"))
public class Application {

  public static void main(String[] args) {
    Micronaut.run(Application.class);
  }
}
