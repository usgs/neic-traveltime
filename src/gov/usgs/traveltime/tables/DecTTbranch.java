package gov.usgs.traveltime.tables;

import gov.usgs.traveltime.ModConvert;
import gov.usgs.traveltime.TauUtil;

/**
 * In order to sample the Earth and the ranges for both P 
 * and S waves adequately on one set of slownesses, the 
 * slowness sampling will be very inhomogeneous.  For any 
 * particular phase, this sampling translates to a ray 
 * parameter sampling that can be so variable as to threaten 
 * the stability of the tau(p) interpolation.  This is 
 * addressed by decimating the ray parameter sampling such 
 * that the corresponding ranges (ray travel distances) are 
 * approximately evenly spaced.
 * 
 * @author Ray Buland
 *
 */
public class DecTTbranch {
	Decimate dec;
	ModConvert convert;
	
	/**
	 * Instantiate the general decimation class.
	 * 
	 * @param convert Model dependent conversions
	 */
	public DecTTbranch(ModConvert convert) {
		this.convert = convert;
		dec = new Decimate();
	}
	
	/**
	 * Figure the decimation for a proxy for the up-going 
	 * branches range spacing.  Note that the eventual up-going 
	 * decimation will be an and of the decimation figured here 
	 * and the decimations for all the other branches.
	 * 
	 * @param piece Proxy range sampling
	 */
	public void upGoingDec(IntPieces piece) {
		int k = -1, jLast = 0, iMin;
		double pLim, pTarget, pDiff;
		boolean[] keep, upKeep;
		double[] pOld, xOld, pNew, xNew;
		
		// Run the decimation algorithm.
		keep = piece.keep;
		pOld = piece.proxyP;
		xOld = piece.proxyX;
		upKeep = dec.decEst(xOld, convert.normR(TablesUtil.DELXUP));
		
		// Do some setup.
		pLim = TablesUtil.PLIM*pOld[pOld.length-1];
		pNew = new double[pOld.length];
		xNew = new double[xOld.length];
		
		// Actually do the decimation.
		for(int j=0; j<xOld.length; j++) {
			if(upKeep[j]) {
				if(pOld[j] < pLim || pOld[j]-pNew[k] < TablesUtil.PTOL) {
					// Most of the time, we just keep this sample.
					jLast = j;
					keep[j] = true;
					pNew[++k] = pOld[j];
					xNew[k] = xOld[j];
				} else {
					// For shallow rays, we may want to keep an additional 
					// sample.
					pTarget = pNew[k]+0.75d*(pOld[j]-pNew[k]);
					iMin = 0;
					pDiff = TauUtil.DMAX;
					for(int i=jLast; i<=j; i++) {
						if(Math.abs(pOld[i]-pTarget) < pDiff) {
							iMin = i;
							pDiff = Math.abs(pOld[i]-pTarget);
						}
					}
					if(iMin == jLast || iMin == j) {
						// We didn't find another sample to add.
						jLast = j;
						keep[j] = true;
						pNew[++k] = pOld[j];
						xNew[k] = xOld[j];
					} else {
						// Add the rescued sample plus the current one.
						keep[iMin] = true;
						pNew[++k] = pOld[iMin];
//					xNew[k] = 0d;
						xNew[k] = xOld[iMin];
						jLast = j;
						keep[j] = true;
						pNew[++k] = pOld[j];
						xNew[k] = xOld[j];
					}
				}
			}
		}
		piece.update(k+1, pNew, xNew);
	}
}
