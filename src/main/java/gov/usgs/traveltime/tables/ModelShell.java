package gov.usgs.traveltime.tables;

import java.util.ArrayList;

/**
 * The ModelShell class is used to keep track of one range between model discontinuities (i.e., one
 * shell of the Earth model).
 *
 * @author Ray Buland
 */
public class ModelShell {
  /** A string containing the model shell name (typical Earth model nomenclature) */
  private String name;

  /** A string containing the temporary P-wave phase code */
  private String tempPCode;

  /** A string containing the temporary S-wave phase code */
  private String tempSCode; // Temporary S-wave phase code

  /** A boolean flag indicating this shell is a discontinuity */
  private boolean isDiscontinuity; // True if this is a discontinuity

  /** A boolean flag indicating if this shell contains a high slowness zone */
  boolean hasLowVelocityZone;

  /** An integer containing the deepest sample in this shell */
  private int bottomSampleIndex;

  /** An integer containing the shallowest sample in this shell */
  private int topSampleIndex;

  /** A double containing the radius in kilometers of the deepest sample in this shell */
  private double bottomSampleRadius;

  /** A double containing the radius in kilometers of the shallowest sample in this shell */
  private double topSampleRadius;

  /** A double containing the range increment target for this shell in kilometers */
  private double rangeIncrementTarget;

  /**
   * Function to return the name of this model shell.
   *
   * @return A String containing the name of this model shell.
   */
  public String getName() {
    return name;
  }

  /**
   * Function to return the temporary P-wave phase code
   *
   * @return A String containing the temporary P-wave phase code
   */
  public String getTempPCode() {
    return tempPCode;
  }

  /**
   * Function to return the temporary S-wave phase code
   *
   * @return A String containing the temporary S-wave phase code
   */
  public String getTempSCode() {
    return tempSCode;
  }

  /**
   * Function to get the temporary phase code associated with this shell.
   *
   * @param waveType A char containing the desired wave type (P = compressional, S = shear)
   * @return A string containing the temporary phase code
   */
  public String getTempCode(char waveType) {
    if (waveType == 'P') {
      return tempPCode;
    } else {
      return tempSCode;
    }
  }

  /**
   * Function to return whether this shell is a discontinuity
   *
   * @return A boolean flag, true indicating whether this shell is a discontinuity
   */
  public boolean getIsDiscontinuity() {
    return isDiscontinuity;
  }

  /**
   * Function to return whether this shell contains a high slowness zone
   *
   * @return A boolean flag, true if this shell contains a high slowness zone
   */
  public boolean getHasLowVelocityZone() {
    return hasLowVelocityZone;
  }

  /**
   * Function to return the index of the bottom (deepest) sample in this shell
   *
   * @return An integer containing the index of the bottom (deepest) sample in this shell
   */
  public int getBottomSampleIndex() {
    return bottomSampleIndex;
  }

  /**
   * Function to return the index of the top (shallowest) sample in this shell
   *
   * @return An integer containing the index of the top (shallowest) sample in this shell
   */
  public int getTopSampleIndex() {
    return topSampleIndex;
  }

  /**
   * Function to set the index of the top (shallowest) sample in this shell
   *
   * @param topSampleIndex An integer containing the index of the top (shallowest) sample in this
   *     shell
   */
  public void setTopSampleIndex(int topSampleIndex) {
    this.topSampleIndex = topSampleIndex;
  }

  /**
   * Function to return the radius in kilometers of the bottom (deepest) sample in this shell
   *
   * @return A double containing the adius in kilometers of the bottom (deepest) sample in this
   *     shell
   */
  public double getBottomSampleRadius() {
    return bottomSampleRadius;
  }

  /**
   * Function to return the radius in kilometers of the top (shallowest) sample in this shell
   *
   * @return A double containing the radius in kilometers of the top (shallowest) sample in this
   *     shell
   */
  public double getTopSampleRadius() {
    return topSampleRadius;
  }

  /**
   * Function to set the radius in kilometers of the top (shallowest) sample in this shell
   *
   * @param topSampleRadius A double containing the radius in kilometers of the top (shallowest)
   *     sample in this shell
   */
  public void setTopSampleRadius(double topSampleRadius) {
    this.topSampleRadius = topSampleRadius;
  }

  /**
   * Function to return the range increment target in kilometers for this shell
   *
   * @return A double containing the range increment target in kilometers for this shell
   */
  public double getRangeIncrementTarget() {
    return rangeIncrementTarget;
  }

  /**
   * Function to set the range increment target in kilometers for this shell
   *
   * @param rangeIncrementTarget A double containing the range increment target in kilometers for
   *     this shell
   */
  public void setRangeIncrementTarget(double rangeIncrementTarget) {
    this.rangeIncrementTarget = rangeIncrementTarget;
  }

  /**
   * Function to set the flag saying this shell has an embedded high slowness zone.
   *
   * @param hasLowVelocityZone A boolean flag indicating that this shell has an embedded high
   *     slowness zone.
   */
  public void setHasLowVelocityZone(boolean hasLowVelocityZone) {
    this.hasLowVelocityZone = hasLowVelocityZone;
  }

  /**
   * Function to initialize the shell with the parameters at the deep end.
   *
   * @param index An integer containing the bottom (deepest) model sample index
   * @param radius A double containing the bottom (deepest) model sample radius in kilometers
   */
  public ModelShell(int index, double radius) {
    isDiscontinuity = false;
    hasLowVelocityZone = false;
    bottomSampleIndex = index;
    bottomSampleRadius = radius;
    name = null;
    tempPCode = null;
    tempSCode = null;
  }

  /**
   * Function to create a discontinuity shell.
   *
   * @param bottomSampleIndex An integer containing the bottom (deepest) model sample index
   * @param topSampleIndex An integer containing the top (shallowest) model sample index
   * @param radius A double containing the model sample radius (deepest and shallowest) in
   *     kilometers
   */
  public ModelShell(int bottomSampleIndex, int topSampleIndex, double radius) {
    isDiscontinuity = true;
    hasLowVelocityZone = false;
    this.bottomSampleIndex = bottomSampleIndex;
    this.topSampleIndex = topSampleIndex;
    bottomSampleRadius = radius;
    topSampleRadius = radius;
    name = null;
    tempPCode = null;
    tempSCode = null;
  }

  /**
   * Function to initialize the shell by copying from another shell.
   *
   * @param shell A ModelShell object containing the reference shell to copy from
   * @param bottomSampleIndex An integer containing the bottom (deepest) model sample index
   */
  public ModelShell(ModelShell shell, int bottomSampleIndex) {
    this.bottomSampleIndex = bottomSampleIndex;
    isDiscontinuity = shell.getIsDiscontinuity();
    hasLowVelocityZone = shell.getHasLowVelocityZone();
    bottomSampleRadius = shell.getBottomSampleRadius();
    rangeIncrementTarget = shell.getRangeIncrementTarget();
    name = shell.getName();
    tempPCode = shell.getTempPCode();
    tempSCode = shell.getTempSCode();
  }

  /**
   * Function to add the parameters at the top (shallow) end of the shell.
   *
   * @param topSampleIndex An integer containing the top (shallowest) model sample index
   * @param topSampleRadius A double containing the top model sample radius (shallowest) in
   *     kilometers
   */
  public void addTop(int topSampleIndex, double topSampleRadius) {
    this.topSampleIndex = topSampleIndex;
    this.topSampleRadius = topSampleRadius;
  }

  /**
   * Function to add a convenience name to the shell and set the target range increment.
   *
   * @param shellName A ShellName enumeration containing the shell name
   * @param depthTop A double containing the depth of the top of the shell in kilometers
   * @param rangeIncrementTarget Target range increment for this shell in kilometers
   */
  public void addName(ShellName shellName, double depthTop, double rangeIncrementTarget) {
    if (shellName != null) {
      this.name = shellName.toString();
      tempPCode = shellName.getTempPCode();
      tempSCode = shellName.getTempSCode();

      if (tempPCode == null && tempSCode == null) {
        tempPCode = String.format("rPd%dP", (int) (depthTop + .5d));
        tempSCode = String.format("rSd%dS", (int) (depthTop + .5d));
      }
    } else {
      this.name = String.format("%d km discontinuity", (int) (depthTop + .5d));
      tempPCode = String.format("rPd%dP", (int) (depthTop + .5d));
      tempSCode = String.format("rSd%dS", (int) (depthTop + .5d));
    }

    this.rangeIncrementTarget = rangeIncrementTarget;
  }

  /**
   * Function to determine if a given sample radius is in this shell.
   *
   * @param radius A double containig the sample radius in kilometers to check
   * @return A boolean flag, true if the radius is inside this shell
   */
  public boolean isInShell(double radius) {
    if (radius >= bottomSampleRadius && radius <= topSampleRadius) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Function to determine if a given sample slowness is in this shell.
   *
   * @param type A char containing the desired wave type (P = compressional, S = shear)
   * @param slowness A double containing the non-dimensional slowness
   * @param model An ArrayList of ModelSample objects containing the Earth model
   * @return A boolean flag, true if the slowness is inside this shell
   */
  public boolean isInShell(char type, double slowness, ArrayList<ModelSample> model) {
    if (type == 'P') {
      if ((slowness >= model.get(bottomSampleIndex).getCompressionalWaveSlowness())
          && (slowness <= model.get(topSampleIndex).getCompressionalWaveSlowness())) {
        return true;
      } else {
        return false;
      }
    } else {
      if ((slowness >= model.get(bottomSampleIndex).getShearWaveSlowness())
          && (slowness <= model.get(topSampleIndex).getShearWaveSlowness())) {
        return true;
      } else {
        return false;
      }
    }
  }

  /**
   * Function to print this wave type specific shell for the depth model.
   *
   * @param type A char containing the desired wave type (P = compressional, S = shear)
   * @return A String representing this shell
   */
  public String printShell(char type) {
    if (type == 'P') {
      if (tempPCode != null) {
        return String.format(
            "%3d - %3d range: %7.2f - %7.2f %5b delX: %6.2f %-8s %s",
            bottomSampleIndex,
            topSampleIndex,
            bottomSampleRadius,
            topSampleRadius,
            isDiscontinuity,
            rangeIncrementTarget,
            tempPCode,
            name);
      } else {
        return null;
      }
    } else {
      if (tempSCode != null) {
        return String.format(
            "%3d - %3d range: %7.2f - %7.2f %5b delX: %6.2f %-8s %s",
            bottomSampleIndex,
            topSampleIndex,
            bottomSampleRadius,
            topSampleRadius,
            isDiscontinuity,
            rangeIncrementTarget,
            tempSCode,
            name);
      } else {
        return null;
      }
    }
  }

  /**
   * Function to convert this shell to a string for printing
   *
   * @return A String representing this shell
   */
  @Override
  public String toString() {
    return String.format(
        "%3d - %3d range: %7.2f - %7.2f %5b delX: %6.2f %-8s %-8s %s",
        bottomSampleIndex,
        topSampleIndex,
        bottomSampleRadius,
        topSampleRadius,
        isDiscontinuity,
        rangeIncrementTarget,
        tempPCode,
        tempSCode,
        name);
  }
}
