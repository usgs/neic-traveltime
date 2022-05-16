package gov.usgs.traveltime.tables;

/**
 * Enumeration class defining the location types of a critical slowness within a shell.
 *
 * @author Ray Buland
 */
public enum ShellSlownessLocation {
  /** The critical slowness is within a shell. */
  SHELL,
  /** The critical slowness is between shells. */
  BOUNDARY;
}
