package gov.usgs.traveltime;

/**
 * Status and exit conditions.
 * 
 * @author Ray Buland
 *
 */
public enum TtStatus {
	// Travel-time status:
	SUCCESS (0),							// Success
	BAD_MODEL_READ (202),			// Model read failed
	BAD_MODEL_FILE (203);			// Model file is badly formed
	
	private final int status;	// Exit flag
	
	/**
	 * The constructor sets up the exit values.
	 * 
	 * @param status Exit value
	 */
	TtStatus(int status) {
		this.status = status;
	}
	
	/**
	 * Get the exit value.
	 * 
	 * @return Exit value
	 */
	int status() {
		return status;
	}
}
