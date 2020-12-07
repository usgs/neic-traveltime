package gov.usgs.traveltimeservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.usgs.processingformats.TravelTimeData;
import gov.usgs.processingformats.TravelTimeRequest;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.annotation.MicronautTest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@MicronautTest
public class TravelTimeControllerTest {

  @Inject private EmbeddedServer server;

  @Test
  public void exampleRequest() throws Exception {
    // read request from json file.
    TravelTimeRequest request = readRequestJson(Paths.get("src/test/resources/request.json"));

    TravelTimeController traveltimes =
        server.getApplicationContext().createBean(TravelTimeController.class, server.getURL());
    TravelTimeRequest response = traveltimes.getTravelTime(request);
    TravelTimeData responseData = response.Response.get(0).Branches.get(0);

    TravelTimeRequest validation = readRequestJson(Paths.get("src/test/resources/response.json"));
    TravelTimeData validationData = validation.Response.get(0).Branches.get(0);

    Assertions.assertEquals(validationData.Phase, responseData.Phase, "Phase");
    Assertions.assertEquals(validationData.TravelTime, responseData.TravelTime, 1e-4, "TravelTime");
    Assertions.assertEquals(
        validationData.Observability, responseData.Observability, 1e-4, "Observability");
    Assertions.assertEquals(
        validationData.LocationUseFlag, responseData.LocationUseFlag, "LocationUseFlag");
  }

  public TravelTimeRequest readRequestJson(final Path path) throws Exception {
    byte[] requestBytes = Files.readAllBytes(path);
    ObjectMapper mapper = new ObjectMapper();
    try {
      TravelTimeRequest request = mapper.readValue(requestBytes, TravelTimeRequest.class);
      return request;
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }
}
