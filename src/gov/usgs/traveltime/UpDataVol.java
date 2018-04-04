package gov.usgs.traveltime;

import java.util.Arrays;

/**
 * Store volatile up-going branch data for one wave type.  Note that all 
 * data have been normalized.
 * 
 * @author Ray Buland
 *
 */
public class UpDataVol {
	int iSrc;							// Source depth model index
	double zSource;				// Normalized source depth
	double pSource;				// Slowness at the source depth
	double pMax;					// Lid slowness if source is in a low velocity zone
	double[] pUp;					// Corrected up-going branch ray parameters
	double[] tauUp;				// Corrected up-going branch tau
	double[] xUp;					// Corrected up-going branch distance
	double tauEndUp;			// Tau integral from surface to LVZ for this wave type
	double tauEndLvz;			// Tau integral from LVZ to source for this wave type
	double tauEndCnv;			// Tau integral from surface to source for other wave type
	double xEndUp;				// Distance integral from surface to LVZ for this wave type
	double xEndLvz;				// Distance integral from LVZ to source for this wave type
	double xEndCnv;				// Distance integral from surface to source for other wave type
	int lenNew;						// Length of the decimated up-going branch
	double[] pDec;				// Decimated up-going branch ray parameters
	double[] tauDec;			// Decimated up-going branch tau
	UpDataRef ref;
	ModDataVol modPri, modSec;
	ModConvert cvt;
	TauInt intPri, intSec;
	Decimate dec;

	/**
	 * Set up volatile copies of data that changes with depth.  Note 
	 * that both P and S models are needed. If this is handling the 
	 * up-going data for P, the primary model would be for P and the 
	 * secondary model would be for S.
	 * 
	 * @param ref The up-going reference data source
	 * @param modPri The primary Earth model data source
	 * @param modSec the secondary Earth model data source
	 * @param cvt Model specific conversions
	 */
	public UpDataVol(UpDataRef ref, ModDataVol modPri, ModDataVol modSec, ModConvert cvt) {
		this.ref = ref;
		this.modPri = modPri;
		this.modSec = modSec;
		this.cvt = cvt;
		
		// Set up the integration routines.
		intPri = new TauInt(modPri);
		intSec = new TauInt(modSec);
		// Set up the decimation.
		dec = new Decimate();
	}
		
	/**
	 * Correct up-going tau to the desired source depth.  The up-going 
	 * branches are used to correct tau for all ray parameters for all 
	 * travel-time branches.  At the same time, integrals are computed 
	 * for the largest ray parameter (usually equal to the source depth 
	 * slowness) needed to correct tau for the largest ray parameter for 
	 * all branches.
	 * 
	 * @param depth Normalized source depth
	 * @throws Exception If the source depth is too deep
	 */
	public void newDepth(double depth) throws Exception {
		int i;
		double xInt;
		boolean corrTau;			// True if tauUp needs correcting
		int iUp;							// Up-going branch index
		int iBot;							// Bottoming depth model index
		double zMax;					// Depth of pMax below a low velocity zone
		
		// Initialize.
		zSource = depth;
		tauEndUp = 0d;
		tauEndLvz = 0d;
		tauEndCnv = 0d;
		xEndUp = 0d;
		xEndLvz = 0d;
		xEndCnv = 0d;
		
		// Get the source slowness.
		pSource = modPri.findP(zSource);
		iSrc = modPri.iSource;
		pMax = modPri.findMaxP();
//	modPri.printFind(false);
		
		// If the source is at the surface, we're already done.
		if(-zSource <= TauUtil.DTOL) return;
		// Otherwise, copy the desired data into temporary storage.
		iUp = modPri.ref.indexUp[iSrc];
//	System.out.println("\t\t\tiUp = "+iUp);
		pUp = Arrays.copyOf(ref.pTauUp, ref.tauUp[iUp].length);
		tauUp = Arrays.copyOf(ref.tauUp[iUp], pUp.length);
		xUp = Arrays.copyOf(ref.xUp[iUp], ref.xUp[iUp].length);
		
		// See if we need to correct tauUp.
		if(Math.abs(ref.pTauUp[iUp]-pMax) <= TauUtil.DTOL) corrTau = false;
		else corrTau = true;		
		
		pMax = pSource;
		// Correct the up-going tau values to the exact source depth.
/*	System.out.println("Partial integrals: "+(float)pSource+" - "+
				(float)modPri.ref.pMod[iSrc]+"  "+(float)zSource+" - "+
				(float)modPri.ref.zMod[iSrc]); */
		i=0;
		for(int j=0; j<tauUp.length; j++) {
			if(ref.pTauUp[j] <= pMax) {
				if(corrTau) {
		//		System.out.println("j  p tau (before): "+(j+1)+" "+
		//				(float)ref.pTauUp[j]+" "+(float)tauUp[j]);
					tauUp[j] -= intPri.intLayer(ref.pTauUp[j], pSource, 
							modPri.ref.pMod[iSrc], zSource, modPri.ref.zMod[iSrc]);
		//		System.out.println("     tau (after): "+(float)tauUp[j]+" "+
		//				(float)ref.pXUp[i]);
					
					// See if we need to correct an end point distance as well.
					if(Math.abs(ref.pTauUp[j]-ref.pXUp[i]) <= TauUtil.DTOL) {
					xInt = intPri.getXLayer();
						xUp[i++] -= xInt;
		//			System.out.println("i  x (after) dx = "+i+" "+
		//					(float)xUp[i-1]+" "+(float)xInt);
					}
				}
			} else break;
		}
		
		/**
		 * Compute tau and distance for the ray parameter equal to the 
		 * source slowness (i.e., horizontal take-off angle from the source).
		 */
//	System.out.println("\nEnd integral: "+(float)pMax+" "+iSrc+" "+
//			(float)pSource+" "+(float)zSource);
		tauEndUp = intPri.intRange(pMax, 0, iSrc-1, pSource, 
				zSource);
		xEndUp = intPri.getXSum();
//	System.out.println("tau x = "+(float)tauEndUp+" "+xEndUp);
		
		/**
		 * If the source depth is in a low velocity zone, we need to 
		 * compute tau and distance down to the shallowest turning ray 
		 * (the horizontal ray is trapped).
		 */
		if(pMax > pSource ) {
			zMax = modPri.findZ(pMax, false);
			iBot = modPri.iSource;
	//	System.out.println("\nLVZ integral: "+(float)pMax+" "+iSrc+" "+
	//			iBot+" "+(float)pSource+" "+(float)zSource+" "+(float)pMax+
	//			" "+(float)zMax);
			tauEndLvz = intPri.intRange(pMax, iSrc, iBot, pSource, 
					zSource, pMax, zMax);
			xEndLvz = intPri.getXSum();
	//	System.out.println("tau x = "+(float)tauEndLvz+" "+xEndLvz);
		} else {
			tauEndLvz = 0d;
			xEndLvz = 0d;
		}
		
		/**
		 * Compute tau and distance for the other wave type for the ray 
		 * parameter equal to the source slowness.
		 */
		try{
			zMax = modSec.findZ(pMax, true);
			iBot = modSec.iSource;
	//	System.out.println("\nCnv integral: "+(float)pMax+" "+iBot+" "+
	//			(float)pMax+" "+(float)zMax);
			tauEndCnv = intSec.intRange(pMax, 0, iBot-1, pMax, zMax);
			xEndCnv = intSec.getXSum();
	//	System.out.println("tau x = "+(float)tauEndCnv+" "+xEndCnv);
		} catch(Exception e) {
			tauEndCnv = 0d;
			xEndCnv = 0d;
	//	System.out.println("\nNo Cnv correction needed");
		}
	}
	
	/**
	 * Generate the up-going branch that will be used to compute travel 
	 * times.  The stored up-going branches must be complete in ray 
	 * parameter samples in order to correct all other travel-time branches 
	 * to the desired source depth.  However, due to the irregular spacing 
	 * of the ray parameter grid, the interpolation will be unstable.  
	 * Therefore, the up-going branch must be decimated to be useful later.  
	 * For very shallow sources, even the decimated grid will be unstable 
	 * and must be completely replaced.
	 * 
	 * @param pBrn Normalized raw ray parameter grid
	 * @param tauBrn Normalized raw tau grid
	 * @param xRange Normalized distance range
	 * @param xMin Normalized minimum distance interval desired
	 * @return A new grid of ray parameter values for the up-going branch
	 * @throws Exception If the tau integration fails
	 */
	public double[] realUp(double pBrn[], double tauBrn[], double[] xRange, 
			double xMin) throws Exception {
		int power;
		double depth, dp;
		
		depth = cvt.realZ(zSource);
		if(depth <= cvt.zNewUp) {
			// For shallow sources, recompute tau on a more stable ray 
			// parameter grid.  The parameters are depth dependent.
			if(depth < 1.5) {
				lenNew = 5;
				power = 6;
			} else if(depth < 10.5) {
				lenNew = 6;
				power = 6;
			} else {
				lenNew = 6;
				power = 7;
			}
			// Allocate some space.
			pDec = new double[lenNew];
			tauDec = new double[lenNew];
			
			// Create the up-going branch.
			pDec[0] = pBrn[0];
			tauDec[0] = tauBrn[0];
			dp = 0.75d*pMax/Math.pow(lenNew-2, power);
			for(int j=1; j<lenNew-1; j++) {
					pDec[j] = pMax-dp*Math.pow(lenNew-j-1, power--);
					tauDec[j] = intPri.intRange(pDec[j], 0, iSrc-1, pSource, 
							zSource);
			}
			pDec[lenNew-1] = pMax;
			tauDec[lenNew-1] = tauEndUp;
		} else {
			// For deeper sources, it is enough to decimate the ray 
			// parameter grid we already have.
			pDec = dec.decXFast(pBrn, tauBrn, xRange, xMin);
			tauDec = dec.getDecTau();
		}
		return pDec;
	}
	
	/**
	 * Get the decimated tau values associated with the decimated ray
	 * parameter grid.
	 * 
	 * @return Decimated, normalized tau on the decimated ray parameter 
	 * grid
	 */
	public double[] getDecTau() {
		return tauDec;
	}
	
	/**
	 * Print out the up-going branch data corrected for the source depth.
	 * 
	 * @param full If true print the corrected tau array as well.
	 */
	public void dumpCorrUp(boolean full) {
		System.out.println("\n     Up-going "+ref.typeUp+" corrected");
		System.out.format("TauEnd: %8.6f %8.6f %8.6f  XEnd: %8.6f %8.6f %8.6f\n", 
				tauEndUp, tauEndLvz, tauEndCnv, xEndUp, xEndLvz, xEndCnv);
		if(full) {
			System.out.println("          p        tau");
			for(int k=0; k<tauUp.length; k++) {
				System.out.format("%3d  %8.6f %11.4e\n",k,pUp[k],tauUp[k]);
			}
	/*	if(brnLen > tauUp.length) {
				System.out.format("%3d  %8.6f  %8.6f\n",brnLen-1,pUp[brnLen-1],
						tauEndUp+tauEndLvz);
			} */
		}
	}
	
	/**
	 * Print out the decimated up-going branch data corrected for the source depth.
	 * 
	 * @param full If true print the corrected tau array as well.
	 */
	public void dumpDecUp(boolean full) {
		System.out.println("\n     Up-going "+ref.typeUp+" decimated");
		System.out.format("TauEnd: %8.6f %8.6f %8.6f  XEnd: %8.6f %8.6f %8.6f\n", 
				tauEndUp, tauEndLvz, tauEndCnv, xEndUp, xEndLvz, xEndCnv);
		if(full) {
			System.out.println("          p        tau");
			for(int k=0; k<tauDec.length; k++) {
				System.out.format("%3d  %8.6f %11.4e\n",k,pDec[k],tauDec[k]);
			}
		}
	}
}
