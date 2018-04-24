package gov.usgs.traveltime.tables;

import java.util.ArrayList;

import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.exception.OutOfRangeException;

/**
 * Interpolate the model.  This encapsulation is necessary 
 * because the Apache Commons library routines don't handle the 
 * zero length intervals used in the Earth models to flag 
 * discontinuities in velocity.
 * 
 * @author Ray Buland
 *
 */
public class ModelInterp {
	boolean isCubic;			// If true use cubic spline interpolation
	int shellLast = -1;
	double rLast = Double.NaN;
	ArrayList<ModelSample> model;
	ArrayList<ModelShell> shells;
	PolynomialSplineFunction interpVp[], interpVs[];

	/**
	 * Remember the model, etc.
	 * 
	 * @param model Earth model data
	 * @param shells Earth model shell limits
	 * @param isCubic True for cubic splines, false for linear interpolation
	 */
	public ModelInterp(ArrayList<ModelSample> model, ArrayList<ModelShell> shells, 
			boolean isCubic) {
		this.model = model;
		this.shells = shells;
		this.isCubic = isCubic;
	}
	
	/**
	 * Interpolate the velocity model values.
	 */
	public void interpVel() {
		int[] indices;
		int n;
		double[] r, v;
		LinearInterpolator linear = null;
		SplineInterpolator cubic = null;
		
		// Initialize the arrays of splines.
		interpVp = new PolynomialSplineFunction[shells.size()];
		interpVs = new PolynomialSplineFunction[shells.size()];
		
		// Loop over shells doing the interpolation.
		for(int i=0; i<shells.size(); i++) {
			indices = shells.get(i).getIndices();
			// Allocate some temporary storage.
			n = indices[1]-indices[0]+1;
			r = new double[n];
			v = new double[n];
			// The cubic interpolation only works if there are more than 2 points.
			if(isCubic && n > 2) {
				// Use cubic splines.
				if(cubic == null) cubic = new SplineInterpolator();
				// Interpolate Vp.
				for(int j=indices[0]; j<=indices[1]; j++) {
					r[j-indices[0]] = model.get(j).r;
					v[j-indices[0]] = model.get(j).vp;
				}
				interpVp[i] = cubic.interpolate(r, v);
				// Interpolate Vs.
				for(int j=indices[0]; j<=indices[1]; j++) {
					v[j-indices[0]] = model.get(j).vs;
				}
				interpVs[i] = cubic.interpolate(r, v);
			} else {
				// The alternative is linear interpolation.
				if(linear == null) linear = new LinearInterpolator();
				// Interpolate Vp.
				for(int j=indices[0]; j<=indices[1]; j++) {
					r[j-indices[0]] = model.get(j).r;
					v[j-indices[0]] = model.get(j).vp;
				}
				interpVp[i] = linear.interpolate(r, v);
				// Interpolate Vs.
				for(int j=indices[0]; j<=indices[1]; j++) {
					v[j-indices[0]] = model.get(j).vs;
				}
				interpVs[i] = linear.interpolate(r, v);
			}
		}
	}
	
	/**
	 * Interpolate to find Vp(r).
	 * 
	 * @param r Radius in kilometers
	 * @return Compressional velocity in kilometers/second at radius r
	 */
	public double getVp(double r) {
		int shell;
		
		shell = getShell(r);
		if(shell >= 0) {
			try {
				return interpVp[shell].value(r);
			} catch(OutOfRangeException e) {
				return Double.NaN;
			}
		} else {
			return Double.NaN;
		}
	}
	
	/**
	 * Interpolate to find Vp(r) in a particular shell.
	 * 
	 * @param shell Shell number
	 * @param r Radius in kilometers
	 * @return Compressional velocity in kilometers/second at radius r
	 */
	public double getVp(int shell, double r) {
		
		if(shell >= 0 && shell < shells.size()) {
			try {
				return interpVp[shell].value(r);
			} catch(OutOfRangeException e) {
				return Double.NaN;
			}
		} else {
			return Double.NaN;
		}
	}
	
	/**
	 * Interpolate to find Vs(r).
	 * 
	 * @param r Radius in kilometers
	 * @return Shear velocity in kilometers/second at radius r
	 */
	public double getVs(double r) {
		int shell;
		
		shell = getShell(r);
		if(shell >= 0) {
			try {
				return interpVp[shell].value(r);
			} catch(OutOfRangeException e) {
				return Double.NaN;
			}
		} else {
			return Double.NaN;
		}
	}
	
	/**
	 * Interpolate to find Vs(r) in a particular shell.
	 * 
	 * @param shell Shell number
	 * @param r Radius in kilometers
	 * @return Shear velocity in kilometers/second at radius r
	 */
	public double getVs(int shell, double r) {
		
		if(shell >= 0 && shell < shells.size()) {
			try {
				return interpVp[shell].value(r);
			} catch(OutOfRangeException e) {
				return Double.NaN;
			}
		} else {
			return Double.NaN;
		}
	}
	
	/**
	 * Find the shell containing radius r.
	 * 
	 * @param r Radius in kilometers
	 * @return Shell number
	 */
	private int getShell(double r) {
		if(r == rLast) {
			return shellLast;
		} else {
			for(int j=0; j<shells.size(); j++) {
				if(shells.get(j).isInShell(r)) {
					rLast = r;
					shellLast = j;
					return j;
				}
			}
		}
		return -1;
	}
}
