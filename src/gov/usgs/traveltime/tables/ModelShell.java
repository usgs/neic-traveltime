package gov.usgs.traveltime.tables;

/**
 * Keep track of one range between model discontinuities 
 * (i.e., one shell of the Earth model).
 * 
 * @author Ray Buland
 *
 */
public class ModelShell {
	int iBot, iTop;
	double rBot, rTop;
	double delX;			// Non-dimensional ray distance increment target for this layer
	
	/**
	 * Initialize the shell with the parameters at the deep end.
	 * 
	 * @param index Model sample index
	 * @param r Model sample radius in kilometers
	 */
	public ModelShell(int index, double r) {
		iBot = index;
		rBot = r;
	}
	
	/**
	 * Initialize the shell by copying from another shell.
	 * 
	 * @param shell Reference shell
	 * @param index Sample index for the bottom of the model shell
	 */
	public ModelShell(ModelShell shell, int index) {
		iBot = index;
		rBot = shell.rBot;
		delX = shell.delX;
		
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
	 * @return True if the sample falls inside this shell
	 */
	public boolean isInShell(double r) {
		if(r >= rBot && r <= rTop) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Get the model shell indices.
	 * 
	 * @return Model shell indices
	 *
	public int[] getIndices() {
		return indices;
	} */
	
	/**
	 * Print out the shell limits.
	 * 
	 * @param j Shell index
	 */
	public void printShell(int j) {
		System.out.format("%3d:   %3d - %3d range: %7.2f - %7.2f delX: %9.6f\n", 
				j, iBot, iTop, rBot, rTop, delX);
	}
}
