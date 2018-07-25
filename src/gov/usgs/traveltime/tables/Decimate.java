package gov.usgs.traveltime.tables;

import java.util.Arrays;

/**
 * Compute a decimation that makes the samples of an array approximately 
 * evenly spaced (at a predefined spacing).
 * 
 * @author Ray Buland
 *
 */
public class Decimate {
	int m;						// Current number of data to keep
	int newM;					// Trial number of data to keep
	double xTarget;		// Desired spacing of x array values
	double var;				// Current variance of residuals
	double[] x;				// Array to be decimated
	
	/**
	 * Calculate a decimation for the array x such that the differences 
	 * between the remaining terms is as close to xTarget as possible.  
	 * Note that the first and last elements of x are always kept.  This 
	 * method figures out the decimation, but doesn't actually implement 
	 * it.
	 * 
	 * @param x Array to be decimated
	 * @param xTarget Target difference between successive elements of 
	 * x after decimation
	 * @return keep An array of booleans, one for each element of x--if an 
	 * element is true, keep the corresponding element of x
	 */
	public boolean[] figureDecimation(double[] x, double xTarget) {
		int k0, k1, k2, kb = 0, nch, m1, m2;
		double dx1, dx2, var1, var2;
		boolean[] keep;		// True if this x element will be kept
		
		this.x = x;
		this.xTarget = xTarget;
		keep = new boolean[x.length];
		Arrays.fill(keep, true);
		
		if(x.length > 2) {
			// First pass.
			k1 = 0;
			var = 0d;
			m = 0;
			for(int j=1; j<x.length-1; j++) {
				dx1 = Math.abs(x[k1]-x[j])-xTarget;
				dx2 = Math.abs(x[k1]-x[j+1])-xTarget;
				if(Math.abs(dx2) < Math.abs(dx1)) {
					keep[j] = false;
				} else {
					if(k1 == 0) kb = j;
					k1 = j;
					var += Math.pow(dx1, 2d);
					m++;
				}
			}
			// Add the last point.
			dx1 = Math.abs(x[k1]-x[x.length-1])-xTarget;
			var += Math.pow(dx1, 2d);
			m++;
			
			// second pass.
			if(m > 1) {
				do {
					k1 = 0;
					k2 = kb;
					nch = 0;
					for(int j=kb+1; j<x.length; j++) {
						if(keep[j]) {
							k0 = k1;
							k1 = k2;
							k2 = j;
							var1 = variance(k0, k1, k2, k1-1);
							m1 = newM;
							var2 = variance(k0, k1, k2, k1+1);
							m2 = newM;
							if(Math.min(var1/m1, var2/m2) < var/m) {
								// We've reduced the variance.  Decide what to do.
								nch++;
								keep[k1] = !keep[k1];
								// Keep the smallest variance.
								if(var1/m1 < var2/m2) {
									keep[--k1] = true;
									var = var1;
									m = m1;
								} else if(var1/m1 > var2/m2) {
									keep[++k1] = true;
									var = var2;
									m = m2;
								} else {
									// If the variances are equal, keep the smallest 
									// number of data.
									if(m1 <= m2) {
										keep[--k1] = true;
										var = var1;
										m = m1;
									} else {
										keep[++k1] = true;
										var = var2;
										m = m2;
									}
								}
							}
							if(k0 == 0) kb = k1;
						}
					}
				} while(nch > 0 && m > 1);
			}
		}
		return keep;
	}
	
	/**
	 * Compute the variance of various possible values to keep.
	 * 
	 * @param k0 First trial x array index
	 * @param k1 Second trial x array index
	 * @param k2 Third trial x array index
	 * @param kt Alternate second trial x array index
	 * @return New trial variance of residuals
	 */
	private double variance(int k0, int k1, int k2, int kt) {
		double dx1, dx2, newVar;
		
		dx1 = Math.abs(x[k0]-x[k1])-xTarget;
		dx2 = Math.abs(x[k1]-x[k2])-xTarget;
		newVar = var-(Math.pow(dx1, 2d)-Math.pow(dx2, 2d));
		if(kt > k0 && kt < k2) {
			dx1 = Math.abs(x[k0]-x[kt])-xTarget;
			dx2 = Math.abs(x[kt]-x[k2])-xTarget;
			newVar += Math.pow(dx1, 2d)+Math.pow(dx2, 2d);
			newM = m;
		} else {
			dx1 = Math.abs(x[k0]-x[k2])-xTarget;
			newVar += Math.pow(dx1, 2d);
			newM = m-1;
		}
		return newVar;
	}
}
