package gov.usgs.traveltime.tables;

import java.util.ArrayList;

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
	ArrayList<ModelShell> pShells, sShells;
	ModConvert convert;

	/**
	 * Initialize bits and pieces for each phase type.
	 * 
	 * @param finModel Final tau model
	 */
	public TablePieces(TauModel finModel) {
		convert = finModel.convert;
		pShells = finModel.pShells;
		sShells = finModel.sShells;
		pPieces = new IntPieces('P', finModel);
		sPieces = new IntPieces('S', finModel);
	}
	
	/**
	 * Decimate the ray parameter arrays for both the P and S 
	 * models.
	 */
	public void decimateP() {
		pPieces.decimateP();
		sPieces.decimateP();
	}
	
	/**
	 * Get the integral pieces for one phase type.
	 * 
	 * @param type Model type (P = P slowness, S = S slowness)
	 * @return Integral pieces
	 */
	public IntPieces getPiece(char type) {
		if(type == 'P') {
			return pPieces;
		} else {
			return sPieces;
		}
	}
	
	/**
	 * Get a ray parameter by index.  Note that the S-wave ray 
	 * parameters include the P-wave ray parameters.
	 * 
	 * @param index Ray parameter index
	 * @return Ray parameter
	 */
	public double getP(int index) {
		return sPieces.p[index];
	}
	
	/**
	 * Get all the ray parameters.
	 * 
	 * @return Ray parameter array
	 */
	public double[] getP() {
		return sPieces.p;
	}
	
	/**
	 * Get a major shell tau value by index.
	 * 
	 * @param type Model type (P = P slowness, S = S slowness)
	 * @param index Tau index
	 * @param shell Major shell index (0 = mantle, 1 = outer 
	 * core, 2 = inner core)
	 * @return Tau value
	 */
	public double getTau(char type, int index, int shell) {
		switch (shell) {
		case 0:
			if(type == 'P') {
				return pPieces.mantleTau[index];
			} else {
				return sPieces.mantleTau[index];
			}
		case 1:
			if(type == 'P') {
				return pPieces.outerCoreTau[index];
			} else {
				return sPieces.outerCoreTau[index];
			}
		case 2:
			if(type == 'P') {
				return pPieces.innerCoreTau[index];
			} else {
				return sPieces.innerCoreTau[index];
			}
		default:
			return Double.NaN;
		}
	}
	
	/**
	 * Get a major shell range value by index.
	 * 
	 * @param type Model type (P = P slowness, S = S slowness)
	 * @param index Range index
	 * @param shell Major shell index (0 = mantle, 1 = outer 
	 * core, 2 = inner core)
	 * @return Range value
	 */
	public double getX(char type, int index, int shell) {
		switch (shell) {
		case 0:
			if(type == 'P') {
				return pPieces.mantleX[index];
			} else {
				return sPieces.mantleX[index];
			}
		case 1:
			if(type == 'P') {
				return pPieces.outerCoreX[index];
			} else {
				return sPieces.outerCoreX[index];
			}
		case 2:
			if(type == 'P') {
				return pPieces.innerCoreX[index];
			} else {
				return sPieces.innerCoreX[index];
			}
		default:
			return Double.NaN;
		}
	}
	
	/**
	 * Get the radial sampling interval associated with a shell by 
	 * index.
	 * 
	 * @param type Model type (P = P slowness, S = S slowness)
	 * @param index Shell index
	 * @return Non-dimensional radial sampling
	 */
	public double getDelX(char type, int index) {
		if(type == 'P') {
			return convert.normR(pShells.get(index).delX);
		} else {
			return convert.normR(sShells.get(index).delX);
		}
	}
	
	/**
	 * Get the radial sampling interval associated with the shell 
	 * above the current one.
	 * 
	 * @param type Model type (P = P slowness, S = S slowness)
	 * @param index Shell index
	 * @return Non-dimensional radial sampling
	 */
	public double getNextDelX(char type, int index) {
		if(type == 'P') {
			for(int j=index+1; j<pShells.size(); j++) {
				if(!pShells.get(j).isDisc) {
					return convert.normR(pShells.get(j).delX);
				}
			}
		} else {
			for(int j=index+1; j<sShells.size(); j++) {
				if(!sShells.get(j).isDisc) {
					return convert.normR(sShells.get(j).delX);
				}
			}
		}
		return Double.NaN;
	}
	
	/**
	 * Get the master decimation array.
	 * 
	 * @param type Model type (P = P slowness, S = S slowness)
	 * @return The master decimation array
	 */
	public boolean[] getDecimation(char type) {
		if(type == 'P') {
			return pPieces.keep;
		} else {
			return sPieces.keep;
		}
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
	
	/**
	 * Print the ray parameter arrays for both the P and S branches.
	 */
	public void printP() {
		System.out.println("\nMaster Ray Parameters");
		System.out.println("       P        S");
		for(int j=0; j<pPieces.p.length; j++) {
			System.out.format("%3d %8.6f %8.6f\n", j, pPieces.p[j], sPieces.p[j]);
		}
		for(int j=pPieces.p.length; j<sPieces.p.length; j++) {
			System.out.format("%3d          %8.6f\n", j, sPieces.p[j]);
		}
	}
}
