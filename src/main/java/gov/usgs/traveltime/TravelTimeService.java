package gov.usgs.traveltime;

import gov.usgs.processingformats.TravelTimeData;
import gov.usgs.processingformats.TravelTimeException;
import gov.usgs.processingformats.TravelTimePlotDataBranch;
import gov.usgs.processingformats.TravelTimePlotDataSample;
import gov.usgs.processingformats.TravelTimePlotRequest;
import gov.usgs.processingformats.TravelTimeReceiver;
import gov.usgs.processingformats.TravelTimeRequest;
import gov.usgs.traveltime.tables.TauIntegralException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.NavigableMap;

/**
 * The TravelTimeService class contains the interfaces between the travel time package and service
 * code, i.e. webservices.
 *
 * @author John Patton
 */
public class TravelTimeService implements gov.usgs.processingformats.TravelTimeService {
  /** A String containing the earth model path for the locator, null to use default. */
  private String modelPath = null;

  /** A String containing the serialized path for the locator, null to use default. */
  private String serializedPath = null;

  /**
   * The TravelTimeService constructor. Sets up the earth model path.
   *
   * @param modelPath A String containing the earth model path to use
   * @param serializedPath A String containing the serialized file path to use
   */
  public TravelTimeService(String modelPath, String serializedPath) {
    this.modelPath = modelPath;
    this.serializedPath = serializedPath;
  }

  /**
   * Function to get a set of travel times using the provided input, i.e a series of stations
   * (recievers) relative to an event (source), implementing the TravelTime service interface.
   *
   * @param request a final TravelTimeRequest containing the TravelTime request
   * @return A TravelTimeRequest with the response section containing the resulting TravelTime data
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
    boolean readEllipticity = true;
    boolean readTopo = true;

    ArrayList<TravelTimeReceiver> responseList = null;

    try {
      String phases[] = request.PhaseTypes.toArray(new String[request.PhaseTypes.size()]);

      // setup new session
      TravelTimeLocalSession ttLocal =
          new TravelTimeLocalSession(
              readStats, readEllipticity, readTopo, modelPath, serializedPath);
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

        TravelTime travelTime = ttLocal.getTT(lat, lon, elev, delta, azimuth);

        if (travelTime == null) {
          continue;
        }

        // create response
        TravelTimeReceiver response = new TravelTimeReceiver(receiver);
        response.Branches = new ArrayList<TravelTimeData>();

        // add traveltimes to response
        for (int i = 0; i < travelTime.getNumPhases(); i++) {
          gov.usgs.traveltime.TravelTimeData phase = travelTime.getPhase(i);

          // build travel time data
          gov.usgs.processingformats.TravelTimeData data =
              new gov.usgs.processingformats.TravelTimeData();
          data.Phase = phase.phCode;
          data.TravelTime = phase.tt;
          data.DistanceDerivative = phase.dTdD;
          data.DepthDerivative = phase.dTdZ;
          data.RayDerivative = phase.dXdP;
          data.StatisticalSpread = phase.spread;
          data.Observability = phase.observ;
          data.TeleseismicPhaseGroup = phase.PhaseGroup;
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

  /**
   * Function to get travel time plot data using the provided input, implementing the TravelTime
   * service interface.
   *
   * @param request a final TravelTimePlotRequest containing the travel time plot data request
   * @return A TravelTimePlotRequest with the response section containing the resulting travel time
   *     plot data
   * @throws gov.usgs.processingformats.TravelTimeException Throws a TravelTimeException upon
   *     certain severe errors.
   */
  @Override
  public TravelTimePlotRequest getTravelTimePlot(final TravelTimePlotRequest request)
      throws TravelTimeException {
    if (request == null) {
      // LOGGER.severe("Null request.");
      throw new TravelTimeException("Null request");
    }
    boolean readStats = true;
    boolean readEllipticity = true;
    boolean readTopo = true;

    // optional defaults
    String phases[] = null;
    boolean returnAllPhases = true;
    boolean returnBackBranches = true;
    boolean convertTectonic = true;
    double maxDistance = 180.0;
    double maxTime = 3600.0;
    double deltaStep = -1.0;
    ArrayList<TravelTimePlotDataBranch> branchList = null;

    try {
      // handle optional values
      if (request.PhaseTypes != null) {
        phases = request.PhaseTypes.toArray(new String[request.PhaseTypes.size()]);
      }
      if (request.ReturnAllPhases != null) {
        returnAllPhases = request.ReturnAllPhases;
      }
      if (request.ReturnBackBranches != null) {
        returnBackBranches = request.ReturnBackBranches;
      }
      if (request.ConvertTectonic != null) {
        convertTectonic = request.ConvertTectonic;
      }
      if ((request.MaximumDistance != null) && (request.MaximumDistance > 0)) {
        maxDistance = request.MaximumDistance;
      }
      if ((request.MaximumTravelTime != null) && (request.MaximumTravelTime > 0)) {
        maxTime = request.MaximumTravelTime;
      }
      if ((request.DistanceStep != null) && (request.DistanceStep > 0)) {
        deltaStep = request.DistanceStep;
      }

      // setup new session
      TravelTimeLocalSession ttLocal =
          new TravelTimeLocalSession(
              readStats, readEllipticity, readTopo, modelPath, serializedPath);
      ttLocal.newSession(
          request.EarthModel,
          request.Source.Depth,
          phases,
          request.Source.Latitude,
          request.Source.Longitude,
          returnAllPhases,
          returnBackBranches,
          convertTectonic,
          false);

      // allocate response
      branchList = new ArrayList<TravelTimePlotDataBranch>();

      // get plotting information
      TravelTimePlot plot =
          ttLocal.getPlot(
              request.EarthModel,
              request.Source.Depth,
              phases,
              returnAllPhases,
              returnBackBranches,
              convertTectonic,
              maxDistance,
              maxTime,
              deltaStep);

      // add traveltimes to response
      NavigableMap<String, TravelTimeBranch> map = plot.branches.headMap("~", true);
      for (@SuppressWarnings("rawtypes") Map.Entry entry : map.entrySet()) {
        TravelTimeBranch branch = (TravelTimeBranch) entry.getValue();

        // build travel time branch
        TravelTimePlotDataBranch dataBranch = new TravelTimePlotDataBranch();
        dataBranch.Phase = branch.phCode;

        ArrayList<TravelTimePlotDataSample> dataSamples = new ArrayList<TravelTimePlotDataSample>();
        for (int i = 0; i < branch.branch.size(); i++) {
          TravelTimePlotPoint point = branch.branch.get(i);

          TravelTimePlotDataSample dataPoint = new TravelTimePlotDataSample();
          dataPoint.Distance = point.delta;
          dataPoint.TravelTime = point.tt;
          dataPoint.StatisticalSpread = point.spread;
          dataPoint.Observability = point.observ;
          dataPoint.RayParameter = point.dTdD;

          // add point to list
          dataSamples.add(dataPoint);
        }

        // add sample points to branch
        dataBranch.Samples = dataSamples;

        // add branch to branchList
        branchList.add(dataBranch);
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

    request.Response = branchList;

    return (request);
  }
}
