package gov.usgs.traveltime;

import java.util.ArrayList;
import java.util.Arrays;

import gov.usgs.traveltime.tables.TauModel;

/**
 * Store non-volatile up-going branch data for one wave type.  Note that all 
 * data have been normalized.
 * 
 * @author Ray Buland
 *
 */
public class UpDataRef {
	final char typeUp;							// Type of up-going branches
	final double[] pTauUp;					// Slowness grid for this branch
	final double[][] tauUp;					// Tau for up-going branches by depth
	final double[] pXUp;						// Slownesses for branch end points
	final double[][] xUp;						// Distances for branch ends by depth

	/**
	 * Load data from the FORTRAN file reader up-going branchs of one type.  
	 * The file data should have already been loaded from the *.hed and 
	 * *.tbl files.
	 * 
	 * @param in Branch input data source.
	 * @param typeUp Wave type ('P' or 'S')
	 */
	public UpDataRef(ReadTau in, char typeUp) {
		this.typeUp = typeUp;
		int i =-1, k;
		
		// Set the FORTRAN type index.
		if(typeUp == 'P') i = 0;
		else i = 1;
		
		// Copy the slowness grids.
		pTauUp = Arrays.copyOf(in.pTauUp[i], in.pTauUp[i].length);
		pXUp = Arrays.copyOf(in.pXUp[i], in.pXUp[i].length);
		
		// The ray parameter for the up-going branches should be truncated 
		// at the source slowness, but are not due to the way FORTRAN arrays 
		// are dimensioned.
		tauUp = new double[in.numRec[i]][];
		xUp = new double[in.numRec[i]][];
		for(int j=0; j<in.numRec[i]; j++) {
			for(k=1; k<in.tauUp[i][j].length; k++) {
				if(in.tauUp[i][j][k] == 0d) {
					break;
				}
			}
			tauUp[j] = Arrays.copyOf(in.tauUp[i][j], k);
			for(k=1; k<in.xUp[i][j].length; k++) {
				if(in.xUp[i][j][k] == 0d) {
					break;
				}
			}
			xUp[j] = Arrays.copyOf(in.xUp[i][j], k);
		}
	}
	
	/**
	 * Load data from the tau-p table generation branch data into this 
	 * class supporting the actual travel-time generation.
	 * 
	 * @param finModel Travel-time branch input data source
	 * @param typeUp Wave type ('P' or 'S')
	 */
	public UpDataRef(TauModel finModel, char typeUp) {
		int n, k = -1;
		ArrayList<Double> xUpTmp;
		
		this.typeUp = typeUp;
		
		// Set up the ray parameter sampling.  This is common to all depths.
		pTauUp = Arrays.copyOf(finModel.getP(typeUp), finModel.getTauInt(typeUp, 1).length);
		pXUp = Arrays.copyOf(finModel.getPxUp(), finModel.getPxUp().length);
		
		// Set the outer dimension.
		n = finModel.intsRealSize(typeUp);
		tauUp = new double[n][];
		xUp = new double[n][];
		
		// Fill in the arrays.
		for(int i=0; i<finModel.intsSize(typeUp)-3; i++) {
			if(finModel.getTauInt(typeUp, i) != null) {
				// Tau is easy.
				n = finModel.getTauInt(typeUp, i).length;
				tauUp[++k] = Arrays.copyOf(finModel.getTauInt(typeUp, i), n);
				// We have to do this the hard way since we can't use toArray to go 
				// from Double to double.
				xUpTmp = finModel.getXUp(typeUp, i);
				xUp[k] = new double[xUpTmp.size()];
				for(int j=0; j<xUpTmp.size(); j++) {
					xUp[k][j] = xUpTmp.get(j);
				}
			}
 		}
	}
	
	/**
	 * Print out the up-going branch data for one depth.
	 * 
	 * @param rec Depth record number
	 */
	public void dumpUp(int rec) {
		System.out.println("\n     Up-going "+typeUp+" record "+rec);
		System.out.println("          p        tau        p           X");
		for(int k=0; k<xUp[rec].length; k++) {
			System.out.format("%3d  %8.6f  %8.6f  %8.6f  %9.6f\n",k,
					pTauUp[k],tauUp[rec][k],pXUp[k],xUp[rec][k]);
		}
		for(int k=xUp[rec].length; k<tauUp[rec].length; k++) {
			System.out.format("%3d  %8.6f  %8.6f\n",k,pTauUp[k],tauUp[rec][k]);
		}
	}
	
	/**
	 * Print out the up-going branch data for all depths.
	 * 
	 * @param model Earth model corresponding to the up-going branches
	 * @param convert Model dependent constants and conversions
	 */
	public void dumpUp(ModDataRef model, ModConvert convert) {
		for(int rec=0; rec<tauUp.length; rec++) {
			System.out.format("\n     Up-going %c record %2d at depth %6.2f\n", 
					typeUp, rec, convert.realZ(model.getDepth(rec)));
			System.out.println("          p        tau        p           X");
			for(int k=0; k<xUp[rec].length; k++) {
				System.out.format("%3d  %8.6f  %8.6f  %8.6f  %9.6f\n",k,
						pTauUp[k],tauUp[rec][k],pXUp[k],xUp[rec][k]);
			}
			for(int k=xUp[rec].length; k<tauUp[rec].length; k++) {
				System.out.format("%3d  %8.6f  %8.6f\n",k,pTauUp[k],tauUp[rec][k]);
			}
		}
	}
}
