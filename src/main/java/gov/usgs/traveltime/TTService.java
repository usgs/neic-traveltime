package gov.usgs.traveltime;

import gov.usgs.processingformats.TravelTimeData;
import gov.usgs.processingformats.TravelTimeException;
import gov.usgs.processingformats.TravelTimeReceiver;
import gov.usgs.processingformats.TravelTimeRequest;
import gov.usgs.processingformats.TravelTimeService;
import gov.usgs.traveltime.tables.TauIntegralException;
import java.io.IOException;
import java.util.ArrayList;

public class TTService implements TravelTimeService {
  /** A String containing the earth model path for the locator, null to use default. */
  private String modelPath = null;

  /** A String containing the serialized path for the locator, null to use default. */
  private String serializedPath = null;

  /**
   * The TTService constructor. Sets up the earth model path.
   *
   * @param modelPath A String containing the earth model path to use
   */
  public TTService(String modelPath, String serializedPath) {
    this.modelPath = modelPath;
    this.serializedPath = serializedPath;
  }

  /**
   * Function to get a travel time using the provided input, implementing the TravelTime service
   * interface.
   *
   * @param request a Final TravelTimeRequest containing the TravelTime request
   * @return A TravelTimeResult containing the resulting TravelTime
   * @throws gov.usgs.processingformats.TravelTimeException Throws a TravelTimeException upon
   *     certain severe errors.
   */
  @Override
  public TravelTimeRequest getTravelTimes(final TravelTimeRequest request)
      throws TravelTimeException {
    if (request == null) {
      // LOGGER.severe("Null request.");
      throw new TravelTimeException("Null request");
    }
    boolean readStats = true;
    boolean readEllip = true;
    boolean readTopo = true;

    ArrayList<TravelTimeReceiver> responseList = null;

    try {
      String phases[] = request.PhaseTypes.toArray(new String[request.PhaseTypes.size()]);

      // setup new session
      TTSessionLocal ttLocal =
          new TTSessionLocal(readStats, readEllip, readTopo, modelPath, serializedPath);
      ttLocal.newSession(
          request.EarthModel,
          request.Source.Depth,
          phases,
          request.Source.Latitude,
          request.Source.Longitude,
          request.ReturnAllPhases,
          request.ReturnBackBranches,
          request.ConvertTectonic,
          false);

      // for each Receiver in the request
      responseList = new ArrayList<TravelTimeReceiver>();
      for (TravelTimeReceiver receiver : request.Receivers) {

        // get the traveltime for this source/receiver pair
        // based on what we got
        double lat = Double.NaN;
        if (receiver.Latitude != null) {
          lat = receiver.Latitude;
        }

        double lon = Double.NaN;
        if (receiver.Longitude != null) {
          lon = receiver.Longitude;
        }

        double elev = Double.NaN;
        if (receiver.Elevation != null) {
          elev = receiver.Elevation;
        }

        double delta = Double.NaN;
        if (receiver.Distance != null) {
          delta = receiver.Distance;
        }

        double azimuth = Double.NaN;

        TTime travelTime = ttLocal.getTT(lat, lon, elev, delta, azimuth);

        if (travelTime == null) {
          continue;
        }

        // create response
        TravelTimeReceiver response = new TravelTimeReceiver(receiver);
        response.Branches = new ArrayList<TravelTimeData>();

        // add traveltimes to response
        for (int i = 0; i < travelTime.getNumPhases(); i++) {
          TTimeData phase = travelTime.getPhase(i);

          // build travel time data
          TravelTimeData data = new TravelTimeData();
          data.Phase = phase.phCode;
          data.TravelTime = phase.tt;
          data.DistanceDerivative = phase.dTdD;
          data.DepthDerivative = phase.dTdZ;
          data.RayDerivative = phase.dXdP;
          data.StatisticalSpread = phase.spread;
          data.Observability = phase.observ;
          data.TeleseismicPhaseGroup = phase.phGroup;
          data.AuxiliaryPhaseGroup = phase.auxGroup;
          data.LocationUseFlag = phase.canUse;
          data.AssociationWeightFlag = phase.dis;

          // add to list
          response.Branches.add(data);
        }

        // add response to response list
        responseList.add(response);
      }
    } catch (BadDepthException e) {
      throw new TravelTimeException("Source depth out of range");
    } catch (TauIntegralException e) {
      throw new TravelTimeException("Bad Tau Integral");
    } catch (IOException e) {
      throw new TravelTimeException("Read Error");
    } catch (ClassNotFoundException e) {
      throw new TravelTimeException("Input serialization");
    }

    request.Response = responseList;

    return (request);
  }
}
