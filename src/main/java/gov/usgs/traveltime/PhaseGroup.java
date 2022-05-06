package gov.usgs.traveltime;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * The PhaseGroup class holds a single phase group. Each phase group is comprised of a generic group
 * name and a set of related phases. Phase groups are primarily used in the Locator to facilitate
 * identification when phases come and go (as the location changes changing the depth and distance
 * to each station). Although there are some special purpose phase groups, they generally come in
 * pairs: a crust-mantle group and a core group. For example, the P group (Pg, Pb, Pn, P and Pdif)
 * are complemented by the PKP group (PKPdf, PKPab, PKPbc, and PKPdif). All possible Locator phases
 * much be in one and only one general group (although they will be duplicated in the special
 * groups).
 *
 * @author Ray Buland
 */
public class PhaseGroup implements Serializable {
  /** A long containing the version id used in serialization */
  private static final long serialVersionUID = 1L;

  /** A string containing the name of the phase group */
  private String groupName;

  /** An ArrayList of Strings containing the list of phase codes in the phase group */
  private ArrayList<String> phaseList;

  /**
   * Get the name of the phase group
   *
   * @return A String containing the name of the phase group
   */
  public String getGroupName() {
    return groupName;
  }

  /**
   * Get the name of the phase group
   *
   * @return An ArrayList of Strings containing the list of phase codes in the phase group
   */
  public ArrayList<String> getPhaseList() {
    return phaseList;
  }

  /**
   * PhaseGroup Constructor, initializes the phase group.
   *
   * @param groupName A string containing the name of the phase group
   */
  protected PhaseGroup(String groupName) {
    this.groupName = groupName;
    phaseList = new ArrayList<String>();
  }

  /**
   * Function to add one phase to the group.
   *
   * @param phase A string containing the phase code to be added
   */
  protected void addPhase(String phase) {
    phaseList.add(phase);
  }

  /** Function to print the contents of this phase group. */
  protected void dumpGroup() {
    System.out.print(groupName + ": ");

    for (int j = 0; j < phaseList.size(); j++) {
      if (j > 0 && j % 15 == 0) System.out.println();
      System.out.print(phaseList.get(j) + " ");
    }

    System.out.println();
  }
}
