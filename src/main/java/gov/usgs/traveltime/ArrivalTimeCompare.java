package gov.usgs.traveltime;

import java.util.Comparator;

/**
 * ArrivalTimeCompare is a comparator to sort travel-time results into ascending arrival time order
 *
 * @author Ray Buland
 */
public class ArrivalTimeCompare implements Comparator<TravelTimeData> {
  @Override
  /**
   * Function to compare the arrival time fields of two arrival results.
   *
   * @param arrival1 A TravelTimeData object containing the first phase
   * @param arrival2 A TravelTimeData object containing the second phase
   * @return an integer representing the comparison result
   */
  public int compare(TravelTimeData arrival1, TravelTimeData arrival2) {
    return (int) Math.signum(arrival1.getTravelTime() - arrival2.getTravelTime());
  }
}
