package gov.usgs.traveltime.tables;

import java.util.ArrayList;

/**
 * Keep track of one range between model discontinuities 
 * (i.e., one shell of the Earth model).
 * 
 * @author Ray Buland
 *
 */
public class ModelShell {
	String name;			// Alternate name for unnamed discontinuities
	String pCode;			// Temporary P-wave phase code
	String sCode;			// Temporary S-wave phase code
	boolean isDisc;		// True if this is a discontinuity
	int iBot;					// Index of the deepest sample in this region 
	int iTop;					// Index of the shallowest sample in this region
	double rBot;			// Radius of the deepest sample in this region in kilometers
	double rTop;			// Radius of the shallowest sample in this region in kilometers
	double delX;			// Range increment target for this layer in kilometers
	
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
		name = null;
		pCode = null;
		sCode = null;
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
		name = null;
		pCode = null;
		sCode = null;
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
		pCode = shell.pCode;
		sCode = shell.sCode;
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
	 * Add a convenience name to the shell and keep track of the target 
	 * range increment.
	 * 
	 * @param name Shell name enumeration
	 * @param depth Depth of the top of the shell in kilometers
	 * @param delX Target range increment for this shell
	 */
	public void addName(ShellName name, double depth, double delX) {
		if(name != null) {
			this.name = name.toString();
			pCode = name.tmpPcode();
			sCode = name.tmpScode();
			if(pCode == null && sCode == null) {
				pCode = String.format("rPd%dP", (int) (depth+.5d));
				sCode = String.format("rSd%dS", (int) (depth+.5d));
			}
		} else {
			this.name = String.format("%d km discontinuity", (int) (depth+.5d));
			pCode = String.format("rPd%dP", (int) (depth+.5d));
			sCode = String.format("rSd%dS", (int) (depth+.5d));
		}
		this.delX = delX;
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
	 * Print this wave type specific shell for the depth model.
	 * 
	 * @param type Wave type (P = compressional, S = shear)
	 * @return String representing this shell
	 */
	public String printTau(char type) {
		if(type == 'P') {
			if(pCode != null) {
				return String.format("%3d - %3d range: %7.2f - %7.2f delX: %6.2f %-8s %s", 
						iBot, iTop, rBot, rTop, delX, pCode, name);
			} else {
				return null;
			}
		} else {
			if(sCode != null) {
				return String.format("%3d - %3d range: %7.2f - %7.2f delX: %6.2f %-8s %s", 
						iBot, iTop, rBot, rTop, delX, sCode, name);
			} else {
				return null;
			}
		}
	}
	
@Override
	public String toString() {
		return String.format("%3d - %3d range: %7.2f - %7.2f delX: %6.2f %-8s %-8s %s", 
				iBot, iTop, rBot, rTop, delX, pCode, sCode, name);
	}
}
