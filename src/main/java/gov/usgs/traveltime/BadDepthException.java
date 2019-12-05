package gov.usgs.traveltime;

/**
 * Exception handler for Travel-time depth out of range.
 * 
 * @author Ray Buland
 *
 */
public class BadDepthException extends Exception {
	private static final long serialVersionUID = 1L;
	String message;
	
	public BadDepthException(String message) {
		super(message);
		this.message = message;
	}
	
	public String toString() {
		return "Bad tau integral (" + message + ")";
	}
}
