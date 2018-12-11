package gov.usgs.traveltime.tables;

/**
 * Location of a critical slowness within a shell.
 * 
 * @author Ray Buland
 *
 */
public enum ShellLoc {
	/**
	 * The critical slowness is within a shell.
	 */
	SHELL,
	/**
	 * The critical slowness is between shells. 
	 */
	BOUNDARY;
}
