package gov.usgs.traveltime.tables;

import java.util.Arrays;

import gov.usgs.traveltime.TauUtil;

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
	 * it.  This "slow" method iterates for as long as it takes to minimize 
	 * the variance between the final grid and the ideal grid.
	 * 
	 * @param x Array to be decimated
	 * @param xTarget Target difference between successive elements of 
	 * x after decimation
	 * @return keep An array of booleans, one for each element of x--if an 
	 * element is true, keep the corresponding element of x
	 */
	public boolean[] slowDecimation(double[] x, double xTarget) {
		int k0, k1, k2, kb = 0, nch, m1, m2, pass;
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
			if(TablesUtil.deBugLevel > 2) {
				System.out.format("\nInit: %9.3e %9.3e\n", var/m, doVar(keep));
			}
			
			// second pass.
			if(m > 1) {
				pass = 1;
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
									if(TablesUtil.deBugLevel > 2) {
										System.out.format("Var1: %9.3e %9.3e %d\n", var/m, 
												doVar(keep), pass);
									}
								} else if(var1/m1 > var2/m2) {
									keep[++k1] = true;
									var = var2;
									m = m2;
									if(TablesUtil.deBugLevel > 2) {
										System.out.format("Var2: %9.3e %9.3e %d\n", var/m, 
												doVar(keep), pass);
									}
								} else {
									// If the variances are equal, keep the smallest 
									// number of data.
									if(m1 <= m2) {
										keep[--k1] = true;
										var = var1;
										m = m1;
										if(TablesUtil.deBugLevel > 2) {
											System.out.format("M1:   %9.3e %9.3e %d\n", var/m, 
													doVar(keep), pass);
										}
									} else {
										keep[++k1] = true;
										var = var2;
										m = m2;
										if(TablesUtil.deBugLevel > 2) {
											System.out.format("M2:   %9.3e %9.3e %d\n", var/m, 
													doVar(keep), pass);
										}
									}
								}
							}
							if(k0 == 0) kb = k1;
						}
					}
					pass++;
				} while(nch > 0 && m > 1);
			}
		}
		return keep;
	}
	
	/**
	 * The "fast" method was used in the FORTRAN real-time travel-time 
	 * calculation for the up-going branches only to save time.  It has the 
	 * advantage of being one pass.  There are several differences related 
	 * to the architecture of the travel-time computation.  First, 
	 * although the decimation is designed to make the spacing in ray 
	 * travel distance, the distance isn't actually kept and must be 
	 * estimated from tau.  Second, for performance reasons, the algorithm 
	 * seeks to enforce a minimum distance spacing rather than a uniform 
	 * target spacing.
	 * 
	 * @param p Normalized ray parameter grid
	 * @param tau Normalized tau on the same grid
	 * @param xRange Normalized distance at the branch end points
	 * @param xMin Normalized minimum distance interval desired
	 * @return Decimated, normalized ray parameter grid
	 */
	public boolean[] fastDecimation(double[] p, double[] tau, 
			double[] xRange, double xMin) {
		boolean[] keep;
		int n, iBeg, iEnd;
		double xCur, xLast, dx, dx2, sgn, rnd, xTarget, xLeast;
		
		// Scan the current sampling to see if it is already OK.
		xCur = xRange[1];
		for(int i=p.length-2; i>=0; i--) {
			xLast = xCur;
			xCur = calcX(p, tau, xRange, i);
			if(Math.abs(xCur-xLast) <= xMin) {
				
				// It's not OK.  Set up the flag array.
				keep = new boolean[p.length];
				Arrays.fill(keep, true);
				// Set up the decimation algorithm.
				if(Math.abs(xCur-xLast) <= 0.75d*xMin) {
					xCur = xLast;
					i++;
				}
				n = Math.max((int)(Math.abs(xCur-xRange[0])/xMin+0.8d), 1);
				dx = (xCur-xRange[0])/n;
				dx2 = Math.abs(dx/2d);
				if(dx >= 0d) {
					sgn = 1d;
					rnd = 1d;
				} else {
					sgn = -1d;
					rnd = 0d;
				}
				xTarget = xRange[0]+dx;
				iBeg = 1;
				iEnd = 0;
				xLeast = TauUtil.DMAX;
				
				// Scan the ray parameter grid looking for points to kill.
				for(int j=1; j<=i; j++) {
					xCur = calcX(p, tau, xRange, j);
					if(sgn*(xCur-xTarget) > dx2) {
						// This point looks OK.  See if we have points to kill.
						if(iEnd >= iBeg) {
							for(int k=iBeg; k<=iEnd; k++) keep[k] = false;
						}
						// Reset the kill pointers.
						iBeg = iEnd+2;
						iEnd = j-1;
						xLeast = TauUtil.DMAX;
						xTarget += (int)((xCur-xTarget-dx2)/dx+rnd)*dx;
					}
					// Look for the best points to kill.
					if(Math.abs(xCur-xTarget) < xLeast) {
						xLeast = Math.abs(xCur-xTarget);
						iEnd = j-1;
					}
				}
				// See if there's one more range to kill.
				if(iEnd >= iBeg) {
					for(int k=iBeg; k<=iEnd; k++) keep[k] = false;
				}
				return keep;
			}
		}
		return null;
	}
	
	/**
	 * Return distance as a function of tau (distance is minus the derivative 
	 * of tau).  The method uses a simple three point approximation of the 
	 * derivative except at the end points where distance is already known.
	 * 
	 * @param p Normalized ray parameter grid
	 * @param tau Normalized tau on the same grid
	 * @param xRange Normalized distance at the branch end points
	 * @param i Grid point where the derivative is required
	 * @return Distance corresponding to tau(p_i)
	 */
	private double calcX(double[] p, double[] tau, double[] xRange, int i) {
		double h1, h2, hh;
		
		if(i == 0) return xRange[0];
		else if(i == p.length-1) return xRange[1];
		else {
			h1 = p[i-1]-p[i];
			h2 = p[i+1]-p[i];
			hh = h1*h2*(p[i-1]-p[i+1]);
			h1 = Math.pow(h1, 2d);
			h2 = -Math.pow(h2, 2d);
			return -(h2*tau[i-1]-(h2+h1)*tau[i]+h1*tau[i+1])/hh;
		}
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
		newVar = var-(Math.pow(dx1, 2d)+Math.pow(dx2, 2d));
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
	
	/**
	 * Compute the variance from scratch for testing purposes.
	 * 
	 * @param keep For each element, if true, keep the corresponding 
	 * x value
	 * @return Variance of absolute differences between kept x values 
	 * minus the target difference
	 */
	private double doVar(boolean[] keep) {
		int i = 0, m = 0;
		double var = 0d;
		
		for(int j=1; j<x.length; j++) {
			if(keep[j]) {
				var += Math.pow(Math.abs(x[j]-x[i])-xTarget, 2d);
				i = j;
				m++;
			}
		}
		return var/m;
	}
}
