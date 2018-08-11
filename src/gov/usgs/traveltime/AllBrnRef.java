package gov.usgs.traveltime;

import java.util.ArrayList;

import gov.usgs.traveltime.tables.BrnData;
import gov.usgs.traveltime.tables.TauModel;

/**
 * Umbrella storage for all non-volatile travel-time data.  
 * Currently, the data is loaded into appropriate classes 
 * from the FORTRAN-ish ReadTau class in the constructor.  
 * However, in the future, it will read files written by 
 * other Java classes.  Note that after the data is loaded, 
 * ReadTau and all it's storage may be sent to the garbage 
 * collector.
 * 
 * @author Ray Buland
 *
 */
public class AllBrnRef {
	final String modelName;							// Earth model name
	final ModDataRef pModel, sModel;		// Earth model data
	final BrnDataRef[] branches;				// Surface focus branch data
	final UpDataRef pUp, sUp;						// Up-going branch data
	final ModConvert cvt;								// Model dependent conversions
	final AuxTtRef auxtt;								// Model independent auxiliary data
	
	/**
	 * Load all data from TauRead into more convenient Java classes.
	 * 
	 * @param in The TauRead data source
	 * @param auxtt The auxiliary data source
	 */
	public AllBrnRef(ReadTau in, AuxTtRef auxtt) {
		String[] segCode;
		
		this.modelName = in.modelName;
		
		// Remember the auxiliary data.
		this.auxtt = auxtt;
		
		// Set up the conversion constants, etc.
		cvt = new ModConvert(in);
		
		// Set up the Earth model.
		pModel = new ModDataRef(in, cvt, 'P');
		sModel = new ModDataRef(in, cvt, 'S');
		
		// Set up the segment codes first.
		segCode = new String[in.numSeg];
		int i = -1;
		int endSeg = 0;
		for(int j=0; j<in.numBrn; j++) {
			// Look for a new segment.
			if(in.indexBrn[j][0] > endSeg) {
				endSeg = in.indexSeg[++i][1];
			}
			// Set the segment code.
			segCode[i] = TauUtil.phSeg(in.phCode[j]);
		}
		
		// Load the branch data.
		branches = new BrnDataRef[in.numBrn];
		ExtraPhases extra = new ExtraPhases(auxtt);
		i = -1;
		endSeg = 0;
		// Loop over branches setting them up.
		for(int j=0; j<in.numBrn; j++) {
			// Look for a new segment.
			if(in.indexBrn[j][0] > endSeg) {
				endSeg = in.indexSeg[++i][1];
			}
			// Load the branch data.
			branches[j] = new BrnDataRef(in, j, i, segCode[i], extra, auxtt);
		}
		
		// Set up the up-going branch data.
		pUp = new UpDataRef(in, 'P');
		sUp = new UpDataRef(in, 'S');
	}
	
	/**
	 * Load data from the tau-p table generation classes into the classes 
	 * supporting the actual travel-time generation.  Note that this two 
	 * step process provides the flexibility to get the data from various 
	 * places and also allows the reference classes to make all the data 
	 * final.
	 * 
	 * @param finModel Travel-time table generation final tau model
	 * @param brnData Travel-time table generation branch data
	 * @param auxtt The auxiliary data source
	 */
	public AllBrnRef(TauModel finModel, ArrayList<BrnData> brnData, 
			AuxTtRef auxtt) {
		// Remember the input data.
		this.modelName = finModel.getModelName();
		this.auxtt = auxtt;
		
		// Set up the conversion constants, etc.
		cvt = finModel.getConvert();
		
		// Set up the Earth model.
		pModel = new ModDataRef(finModel, cvt, 'P');
		sModel = new ModDataRef(finModel, cvt, 'S');
		
		// Load the branch data.
		branches = new BrnDataRef[brnData.size()];
		ExtraPhases extra = new ExtraPhases(auxtt);
		// Loop over branches setting them up.
		for(int j=0; j<branches.length; j++) {
			branches[j] = new BrnDataRef(brnData.get(j), j, extra, auxtt);
		}
		
		// Set up the up-going branch data.
		pUp = new UpDataRef(finModel, 'P');
		sUp = new UpDataRef(finModel, 'S');
	}

	/**
	 * Get the number of travel-time branches loaded.
	 * 
	 * @return Number of travel-time branches loaded.
	 */
	public int getNoBranches() {
		return branches.length;
	}
	
	/**
	 * Print global or header data for debugging purposes.
	 */
	public void dumpHead() {
		System.out.println("\n     "+modelName);
		System.out.format("Normalization: xNorm =%11.4e  vNorm =%11.4e  "+
				"tNorm =%11.4e\n", cvt.xNorm, cvt.vNorm, cvt.tNorm);
		System.out.format("Boundaries: zUpperMantle =%7.1f  zMoho =%7.1f  "+
				"zConrad =%7.1f\n", cvt.zUpperMantle, cvt.zMoho, cvt.zConrad);
		System.out.format("Derived: rSurface =%8.1f  zNewUp = %7.1f  "+
				"dTdDel2P =%11.4e\n", cvt.rSurface, cvt.zNewUp, cvt.dTdDelta);
	}
	
	/**
	 * Print model parameters for debugging purposes.
	 * 
	 * @param typeMod Wave type ('P' or 'S')
	 * @param nice If true print the model in dimensional units
	 */
	public void dumpMod(char typeMod, boolean nice) {
		if(typeMod == 'P') {
			pModel.dumpMod(nice);
		} else if(typeMod == 'S') {
			sModel.dumpMod(nice);
		}
	}
	
	/**
	 * Print data for one travel-time branch for debugging purposes.  
	 * 
	 * @param iBrn Branch number
	 * @param full If true print the detailed branch specification as well
	 */
	public void dumpBrn(int iBrn, boolean full) {
		branches[iBrn].dumpBrn(full);
	}
	
	/**
	 * Print data for one travel-time segment for debugging purposes.
	 * 
	 * @param seg Segment phase code
	 * @param full If true, print the detailed specification for each branch
	 * as well
	 */
	public void dumpBrn(String seg, boolean full) {
		for(int j=0; j<branches.length; j++) {
			if(branches[j].getPhSeg().equals(seg)) 
				branches[j].dumpBrn(full);
		}
	}
	
	/**
	 * Print data for all travel-time segments for debugging purposes.
	 * 
	 * @param full If true, print the detailed specification for each branch
	 * as well
	 */
	public void dumpBrn(boolean full) {
		for(int j=0; j<branches.length; j++) {
			branches[j].dumpBrn(full);
		}
	}
	
	/**
	 * Print data for one up-going branch for debugging purposes.
	 * 
	 * @param typeUp Wave type ('P' or 'S')
	 * @param iUp Depth index
	 */
	public void dumpUp(char typeUp, int iUp) {
		if(typeUp == 'P') {
			pUp.dumpUp(iUp);
		} else if(typeUp == 'S') {
			sUp.dumpUp(iUp);
		}
	}
	
	/**
	 * Print data for all up-going branches for debugging purposes.
	 * 
	 * @param typeUp Wave type ('P' or 'S')
	 */
	public void dumpUp(char typeUp) {
		if(typeUp == 'P') {
			pUp.dumpUp(pModel, cvt);
		} else if(typeUp == 'S') {
			sUp.dumpUp(sModel, cvt);
		}
	}
}
