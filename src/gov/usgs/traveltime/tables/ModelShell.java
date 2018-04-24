package gov.usgs.traveltime.tables;

/**
 * Keep track of one range between model discontinuities 
 * (i.e., one shell of the Earth model).
 * 
 * @author Ray Buland
 *
 */
public class ModelShell {
	int[] indices;
	double[] rRange;
	
	/**
	 * Initialize the shell with the parameters at the deep end.
	 * 
	 * @param index Model sample index
	 * @param r Model sample radius in kilometers
	 */
	public ModelShell(int index, double r) {
		indices = new int[2];
		rRange = new double[2];
		indices[0] = index;
		rRange[0] = r;
	}
	
	/**
	 * Add the parameters at the shallow end of the shell.
	 * 
	 * @param index Model sample index
	 * @param r Model sample radius in kilometers
	 */
	public void addEnd(int index, double r) {
		indices[1] = index;
		rRange[1] = r;
	}
	
	/**
	 * Determine if a sample radius is in this shell.
	 * 
	 * @param r Sample radius in kilometers
	 * @return True if the sample falls inside this shell
	 */
	public boolean isInShell(double r) {
		if(r >= rRange[0] && r <= rRange[1]) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Get the model shell indices.
	 * 
	 * @return Model shell indices
	 */
	public int[] getIndices() {
		return indices;
	}
	
	/**
	 * Print out the shell limits.
	 * 
	 * @param j Shell index
	 */
	public void printShell(int j) {
		System.out.format("%3d:   %3d - %3d range: %7.2f - %7.2f\n", j, indices[0], 
				indices[1], rRange[0], rRange[1]);
	}
}
