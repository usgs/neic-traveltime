package gov.usgs.traveltime.tables;

import java.util.ArrayList;

import gov.usgs.traveltime.ModConvert;

/**
 * Keep track of one range between model discontinuities 
 * (i.e., one shell of the Earth model).
 * 
 * @author Ray Buland
 *
 */
public class ModelShell {
	ShellName name;
	String altName;
	boolean isDisc;
	int iBot, iTop;
	double rBot, rTop;
	double delX;			// Non-dimensional range increment target for this layer
	
	/**
	 * Initialize the shell with the parameters at the deep end.
	 * 
	 * @param index Model sample index
	 * @param r Model sample radius in kilometers
	 */
	public ModelShell(int index, double r) {
		isDisc = false;
		iBot = index;
		rBot = r;
	}
	
	/**
	 * Create a discontinuity.
	 * 
	 * @param iBot Model sample index for the bottom
	 * @param iTop Model sample index for the top
	 * @param r Model sample radius in kilometers
	 */
	public ModelShell(int iBot, int iTop, double r) {
		isDisc = true;
		this.iBot = iBot;
		this.iTop = iTop;
		rBot = r;
		rTop = r;
	}
	
	/**
	 * Initialize the shell by copying from another shell.
	 * 
	 * @param shell Reference shell
	 * @param index Sample index for the bottom of the model shell
	 */
	public ModelShell(ModelShell shell, int index) {
		iBot = index;
		isDisc = shell.isDisc;
		rBot = shell.rBot;
		delX = shell.delX;
		name = shell.name;
		altName = shell.altName;
	}
	
	/**
	 * Add the parameters at the shallow end of the shell.
	 * 
	 * @param index Model sample index
	 * @param r Model sample radius in kilometers
	 */
	public void addEnd(int index, double r) {
		iTop = index;
		rTop = r;
	}
	
	/**
	 * Determine if a sample radius is in this shell.
	 * 
	 * @param r Sample radius in kilometers
	 * @return True if the radius is inside this shell
	 */
	public boolean isInShell(double r) {
		if(r >= rBot && r <= rTop) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Determine if a sample slowness is in this shell.
	 * 
	 * @param type Wave type (P = compressional, S = shear)
	 * @param slow Non-dimensional slowness
	 * @param model Earth model
	 * @return True if the slowness is in this shell
	 */
	public boolean isInShell(char type, double slow, 
			ArrayList<ModelSample> model) {
		if(type == 'P') {
			if(slow >= model.get(iBot).slowP && slow <= 
					model.get(iTop).slowP) {
				return true;
			}
			else {
				return false;
			}
		} else {
			if(slow >= model.get(iBot).slowS && slow <= 
					model.get(iTop).slowS) {
				return true;
			}
			else {
				return false;
			}
		}
	}
	
	/**
	 * Format the information in this shell for printing.
	 * 
	 * @param convert Model dependent conversions
	 * @return String describing this shell
	 */
	public String printShell(ModConvert convert) {
		
		if(name == null) {
			return String.format("%3d - %3d range: %7.2f - %7.2f delX: %6.2f %s", 
					iBot, iTop, rBot, rTop, convert.dimR(delX), altName);
		} else {
			return String.format("%3d - %3d range: %7.2f - %7.2f delX: %6.2f %s", 
					iBot, iTop, rBot, rTop, convert.dimR(delX), name);
		}
	}
}
