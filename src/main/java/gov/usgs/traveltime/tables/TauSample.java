package gov.usgs.traveltime.tables;

import gov.usgs.traveltime.ModelConversions;

/**
 * The TauSample class encapsulates one sample in the alternative view of the Earth model suitable
 * for the tau-p travel-time calculation.
 *
 * @author Ray Buland
 */
public class TauSample {
  /** An integer containing the depth index of this tau sample */
  private int index;

  /** A double containing the dimensional earth radius in kilometers */
  private double radius;

  /** A double containing the non-dimensional Earth flattened depth in kilometers */
  private double depth;

  /** A double containing the non-dimensionall model slowness */
  private double slowness;

  /** A double containing the non-dimensional ray travel distance (range) in kilometers */
  private double rayTravelDistance;

  /**
   * Get the index of this tau sample. This used to be a direct access file record number. Now it is
   * used to access the correct up-going tau and range for depth correction.
   *
   * @return An Integer holding the depth index of this tau sample
   */
  public int getIndex() {
    return index;
  }

  /**
   * Get the dimensional earth radius in kilometers.
   *
   * @return A double holding the dimensional earth radius in kilometers
   */
  public double getRadius() {
    return radius;
  }

  /**
   * Get the model depth.
   *
   * @return Non-dimensional Earth flattened depth in kilometers
   */
  public double getDepth() {
    return depth;
  }

  /**
   * Get the model slowness.
   *
   * @return A double containing the non-dimensional slowness
   */
  public double getSlowness() {
    return slowness;
  }

  /**
   * Get the non-dimensional ray travel distance (range)
   *
   * @return A double containing the non-dimensional ray travel distance (range) in kilometers
   */
  public double getRayTravelDistance() {
    return rayTravelDistance;
  }

  /**
   * TauSample constructor, initialize this sample.
   *
   * @param radius A double containing the dimensional Earth radius in kilometers
   * @param slowness A double containing the non-dimensional slowness
   * @param rayTravelDistance A double containing the non-dimensional ray travel distance (range) in
   *     kilometers
   */
  public TauSample(double radius, double slowness, double rayTravelDistance) {
    this.radius = radius;
    this.slowness = slowness;
    this.rayTravelDistance = rayTravelDistance;
    depth = Double.NaN;
  }

  /**
   * TauSample constructor, initialize this sample. For some purposes, we don't need range.
   *
   * @param radius A double containing the dimensional Earth radius in kilometers
   * @param slowness A double containing the non-dimensional slowness
   * @param index Index into the merged slownesses
   * @param convert A ModelConversions object containing the model dependant conversions
   */
  public TauSample(double radius, double slowness, int index, ModelConversions convert) {
    this.radius = radius;
    this.slowness = slowness;
    this.index = index;
    rayTravelDistance = Double.NaN;
    depth = convert.computeFlatDepth(radius);
  }

  /**
   * TauSample copy constructor, initialize this sample from another sample.
   *
   * @param sample A TauSample object containing an existing tau sample
   */
  public TauSample(TauSample sample) {
    this.radius = sample.getRadius();
    this.slowness = sample.getSlowness();
    this.rayTravelDistance = sample.getRayTravelDistance();
    this.depth = sample.getDepth();
  }

  /**
   * TauSample constructor, initialize this sample from another sample and add an index.
   *
   * @param sample A TauSample object containing an existing tau sample
   * @param index An integer containing the desired index
   */
  public TauSample(TauSample sample, int index) {
    this.radius = sample.getRadius();
    this.slowness = sample.getSlowness();
    this.rayTravelDistance = sample.getRayTravelDistance();
    this.depth = sample.getDepth();
    this.index = index;
  }

  /**
   * Function to update this sample.
   *
   * @param radius A double containing the dimensional Earth radius in kilometers
   * @param slowness A double containing the non-dimensional slowness
   * @param rayTravelDistance A double containing the non-dimensional ray travel distance (range) in
   *     kilometers
   */
  public void update(double radius, double slowness, double rayTravelDistance) {
    this.radius = radius;
    this.slowness = slowness;
    this.rayTravelDistance = rayTravelDistance;
  }

  /**
   * Function to convert this TauSample into a string.
   *
   * @return A String containing the string representation of this TauSample
   */
  @Override
  public String toString() {
    if (!Double.isNaN(rayTravelDistance)) {
      return String.format("%7.2f %8.6f %8.6f", radius, slowness, rayTravelDistance);
    } else if (!Double.isNaN(depth)) {
      return String.format("%7.2f %8.6f %8.6f %3d", radius, slowness, depth, index);
    } else {
      return String.format("%7.2f %8.6f   NaN    %3d", radius, slowness, index);
    }
  }
}
