package gov.usgs.traveltime.tables;

import gov.usgs.traveltime.ModConvert;

/**
 * Put together the bits and pieces we'll need to make the travel-time 
 * branch layout and decimation come out right.
 * 
 * @author Ray Buland
 *
 */
public class TablePieces {
	IntPieces pPieces, sPieces;
	ModConvert convert;

	/**
	 * Initialize bits and pieces for each phase type.
	 * 
	 * @param finModel Final tau model
	 */
	public TablePieces(TauModel finModel) {
		convert = finModel.convert;
		pPieces = new IntPieces('P', finModel);
		sPieces = new IntPieces('S', finModel);
	}
	
	/**
	 * Print out the proxy ranges.
	 */
	public void printProxy() {
		int nP, nS;
		
		System.out.println("\n\tProxy Ranges");
		nP = pPieces.proxyX.length-1;
		nS = sPieces.proxyX.length-1;
		for(int j=0; j<nP; j++) {
			System.out.format("%3d %8.6f %8.2f %8.2f %8.6f %8.2f %8.2f\n", 
					j, pPieces.proxyP[j], convert.dimR(pPieces.proxyX[j]), 
					convert.dimR(pPieces.proxyX[j+1]-pPieces.proxyX[j]), 
					sPieces.proxyP[j], convert.dimR(sPieces.proxyX[j]), 
					convert.dimR(sPieces.proxyX[j+1]-sPieces.proxyX[j]));
		}
		System.out.format("%3d %8.6f %8.2f          %8.6f %8.2f %8.2f\n", 
				nP, pPieces.proxyP[nP], convert.dimR(pPieces.proxyX[nP]), 
				sPieces.proxyP[nP], convert.dimR(sPieces.proxyX[nP]), 
				convert.dimR(sPieces.proxyX[nP+1]-sPieces.proxyX[nP]));
		for(int j=nP+1; j<nS; j++) {
			System.out.format("%3d                            "+
					"%8.6f %8.2f %8.2f\n", j, 
					sPieces.proxyP[j], convert.dimR(sPieces.proxyX[j]), 
					convert.dimR(sPieces.proxyX[j+1]-sPieces.proxyX[j]));
		}
		System.out.format("%3d                            "+
				"%8.6f %8.2f\n", nS, 
				sPieces.proxyP[nS], convert.dimR(sPieces.proxyX[nS]));
	}
}
