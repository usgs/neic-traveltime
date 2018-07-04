package gov.usgs.traveltime.tables;

import java.util.ArrayList;
import java.util.Arrays;

import gov.usgs.traveltime.ModConvert;

/**
 * Put together the bits and pieces we'll need to make the travel-time 
 * branch layout and decimation come out right by phase type.
 * 
 * @author Ray Buland
 *
 */
public class IntPieces {
	char type;
	boolean[] keep;
	double[] p;													// Ray parameters
	double[] mantleTau, mantleX;				// Integrals through the mantle
	double[] outerCoreTau, outerCoreX;	// Integrals through the outer core
	double[] innerCoreTau, innerCoreX;	// Integrals through the inner core
	double[] ocCumTau, ocCumX, icCumTau, icCumX;
	double[] proxyX, proxyP;
	TauModel finModel;
	ModConvert convert;
	ArrayList<TauXsample> ints;

	/**
	 * We need the model to get started.
	 * 
	 * @param type Model type (P = P slowness, S = S slowness)
	 * @param finModel Final tau model
	 */
	public IntPieces(char type, TauModel finModel) {
		this.finModel = finModel;
		convert = finModel.convert;
		if(type == 'P') {
			ints = finModel.pInts;
		} else {
			ints = finModel.sInts;
		}
	}
	
	/**
	 * Create the tau and range partial integrals by major shells rather 
	 * than the cumulative integrals computed in Integrate.
	 */
	public void setShellInts() {
		mantleTau = finModel.getTauInt(type, ShellName.CORE_MANTLE_BOUNDARY);
		mantleX = finModel.getXInt(type, ShellName.CORE_MANTLE_BOUNDARY);
		ocCumTau = finModel.getTauInt(type, ShellName.INNER_CORE_BOUNDARY);
		ocCumX = finModel.getXInt(type, ShellName.INNER_CORE_BOUNDARY);
		icCumTau = finModel.getTauInt(type, ShellName.CENTER);
		icCumX = finModel.getXInt(type, ShellName.CENTER);
		for(int j=0; j<mantleTau.length; j++) {
			innerCoreTau[j] = icCumTau[j]-ocCumTau[j];
			innerCoreX[j] = icCumX[j]-ocCumX[j];
			outerCoreTau[j] = ocCumTau[j]-mantleTau[j];
			outerCoreX[j] = ocCumX[j]-mantleX[j];
		}
	}
	
	/**
	 * To decimate the up-going branches, we need a proxy for the range 
	 * spacing at all source depths so that the ray parameter spacing is 
	 * common.
	 */
	public void initDecimation() {
		int n;
		double[] x;
		ArrayList<Double> slowness;
		
		/**
		 * The master keep list will be an or of branch keeps.
		 */
		keep = new boolean[mantleTau.length];
		Arrays.fill(keep, false);
		/**
		 * Create the proxy ranges.
		 */
		proxyX = new double[mantleX.length];
		Arrays.fill(proxyX, 0d);
		n = finModel.size(type);
		// Put together a list of maximum range differences.
		for(int i=0; i<n-3; i++) {
			x = finModel.getXInt(type, i);
			for(int j=1; j<x.length; j++) {
				proxyX[j] = Math.max(proxyX[j], Math.abs(x[j-1]-x[j]));
			}
	/*	if(x.length == n-1) {
				proxyX[n-1] = x[x.length-1];
			} */
		}
		// Now put the range differences back together to sort of look 
		// like a range.
		slowness = finModel.slowness;
		for(int j=1; j<proxyX.length; j++) {
			p[j] = slowness.get(j);
			proxyP[j] = slowness.get(j);
			proxyX[j] = proxyX[j-1]+proxyX[j];
		}
	}
	
	/**
	 * Print out the proxy ranges.
	 */
	public void printProxy() {
		int n;
		
		System.out.println("\nProxy Ranges");
		n = proxyX.length;
		for(int j=0; j<n-1; j++) {
			System.out.format("%3d %8.6f %8.2f %8.2f\n", j, proxyP[j], 
					convert.dimR(proxyX[j]), convert.dimR(proxyX[j+1]-proxyX[j]));
		}
		System.out.format("%3d %8.6f %8.2f\n", n, proxyP[n], 
				convert.dimR(proxyX[n]));
	}
	
	/**
	 * Print out the shell integrals.
	 */
	public void printShellInts() {
		System.out.println("\n\tShell Integrals");
		for(int j=0; j<mantleTau.length; j++) {
			System.out.format("%3d %8.6f %8.6f %8.6f %8.6f %6.2f %6.2f %6.2f\n", 
					j, p[j], mantleTau[j], outerCoreTau[j], innerCoreTau[j], 
					Math.toDegrees(mantleX[j]), Math.toDegrees(outerCoreX[j]), 
					Math.toDegrees(innerCoreX[j]));
		}
	}
}
