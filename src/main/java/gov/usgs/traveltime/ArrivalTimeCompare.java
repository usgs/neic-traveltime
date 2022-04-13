package gov.usgs.traveltime;

import java.util.Comparator;

/**
 * Comparator to sort travel-time results into ascending arrival time order
 *
 * @author Ray Buland
 */
public class ArrivalTimeCompare implements Comparator<TravelTimeData> {
  @Override
  /**
   * Compare the arrival time fields of two arrival results.
   *
   * @param arr1 Travel-time data for the first phase
   * @param arr2 Travel-time data for the second phase
   */
  public int compare(TravelTimeData arr1, TravelTimeData arr2) {
    return (int) Math.signum(arr1.tt - arr2.tt);
  }
}
