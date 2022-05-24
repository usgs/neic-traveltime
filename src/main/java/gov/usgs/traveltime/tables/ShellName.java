package gov.usgs.traveltime.tables;

/**
 * Enumeration providing names for Earth model shells is a convenience, but turns out to be very
 * handy for understanding the voluminous output.
 *
 * @author Ray Buland
 */
public enum ShellName {
  // Shell names:
  /** Placeholder for integrals bottoming at all source depths. */
  CENTER(0d, "", ""),
  /** Default radius of the inner core in kilometers. */
  INNER_CORE(1217d, "tPKPdf", "tSKSdf"),
  /** Default radius of the outer core in kilometers. */
  OUTER_CORE(3482d, "tPKPab", "tSKSab"),

  /**
   * Default radius of the lower mantle in kilometers (i.e., to the 410 kilometer discontinuity).
   */
  LOWER_MANTLE(5961d, "tP", "tS"),
  /** Default radius of the upper mantle in kilometers (i.e., to the Moho discontinuity). */
  UPPER_MANTLE(6336d, "tPn", "tSn"),
  /** Default radius of the lower crust in kilometers (i.e., to the Conrad discontinuity). */
  LOWER_CRUST(6351d, "tPb", "tSb"),

  /**
   * Default radius of the upper crust in kilometers (i.e., to the free surface). This is, of
   * course, the mean radius of the Earth.
   */
  UPPER_CRUST(6371d, "tPg", "tSg"),

  // Discontinuity names:
  /** Inner core-outer core boundary radius in kilometers. */
  INNER_CORE_BOUNDARY(1217d, "rPKiKP", "rSKiKS"),
  /** Core-mantle boundary radius in kilometers. */
  CORE_MANTLE_BOUNDARY(3482d, "", "rScS"),
  /** Moho radius in kilometers. */
  MOHO_DISCONTINUITY(6336d, "rPmP", "rSmS"),
  /** Conrad radius in kilometers. */
  CONRAD_DISCONTINUITY(6351d, null, null),
  /** Place holder for the free surface. */
  SURFACE(6371d, "", ""),

  // Handy handles.
  /**
   * Associate with the maximum slowness associated with the core. This is trickier than it might
   * seem because of the velocity drop in P and the velocity increase in S.
   */
  CORE_TOP(3482d, "", ""),
  /**
   * Associate with the minimum slowness at the base of the mantle. Again, we have to be careful
   * because of the velocity drop in P.
   */
  MANTLE_BOTTOM(3482d, "", "");

  /** A double containing the default radius of the top discontinuity of the shell in kilometers */
  private final double defaultRadius;

  /** A String containing the temporary P-wave phase code */
  private final String tempPCode;

  /** A String containing the temporary S-wave phase code */
  private final String tempSCode;

  /**
   * The ShellName constructor sets up the default radius.
   *
   * @param defaultRadius A double containing the default radius of the top discontinuity of the
   *     shell in kilometers
   * @param tempPCode A String containing the temporary P-wave phase code
   * @param tempSCode A String containing the temporary S-wave phase code
   */
  ShellName(double defaultRadius, String tempPCode, String tempSCode) {
    this.defaultRadius = defaultRadius;
    this.tempPCode = tempPCode;
    this.tempSCode = tempSCode;
  }

  /**
   * Function to get the default radius of the top discontinuity of the shell in kilometers
   *
   * @return A double containing thedefault radius of the top discontinuity of the shell in
   *     kilometers
   */
  public double getDefaultRadius() {
    return defaultRadius;
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
}
