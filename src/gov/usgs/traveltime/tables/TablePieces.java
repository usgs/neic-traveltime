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
		
		System.out.println("\n\t\t\tProxy Ranges");
		System.out.println("                  P                            S");
		System.out.println("    slowness      X       delX   slowness"+
				"      X       delX");
		nP = pPieces.proxyX.length;
		nS = sPieces.proxyX.length;
		System.out.format("%3d %8.6f %8.2f            %8.6f %8.2f\n", 
				0, pPieces.proxyP[0], convert.dimR(pPieces.proxyX[0]), 
				sPieces.proxyP[0], convert.dimR(sPieces.proxyX[0]));
		for(int j=1; j<nP; j++) {
			System.out.format("%3d %8.6f %8.2f %8.2f   %8.6f %8.2f %8.2f\n", 
					j, pPieces.proxyP[j], convert.dimR(pPieces.proxyX[j]), 
					convert.dimR(pPieces.proxyX[j]-pPieces.proxyX[j-1]), 
					sPieces.proxyP[j], convert.dimR(sPieces.proxyX[j]), 
					convert.dimR(sPieces.proxyX[j+1]-sPieces.proxyX[j]));
		}
		for(int j=nP; j<nS; j++) {
			System.out.format("%3d                              "+
					"%8.6f %8.2f %8.2f\n", j, 
					sPieces.proxyP[j], convert.dimR(sPieces.proxyX[j]), 
					convert.dimR(sPieces.proxyX[j]-sPieces.proxyX[j-1]));
		}
	}
	
	/**
	 * Print the integrals for the whole mantle, outer core, and inner core.
	 * 
	 * @param type Model type (P = P slowness, S = S slowness)
	 */
	public void printShellInts(char type) {
		if(type == 'P') {
			pPieces.printShellInts();
		} else {
			sPieces.printShellInts();
		}
	}
}
