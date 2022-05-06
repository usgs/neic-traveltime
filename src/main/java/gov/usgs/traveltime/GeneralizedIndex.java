package gov.usgs.traveltime;

/**
 * The GeneralizedIndex class is an interface allows computed sample points to be used like they
 * were actual arrays for bilinear interpolation.
 *
 * @author Ray Buland
 */
public interface GeneralizedIndex {

  /**
   * Function to get the array index from a value.
   *
   * @param value A double containing a generic value
   * @return An integer containing the index of the array from a value
   */
  int getIndex(double value);

  /**
   * Function to get the array value from an index.
   *
   * @param index An integer containing the array index
   * @return A double containing the value at that array index
   */
  double getValue(int index);
}
