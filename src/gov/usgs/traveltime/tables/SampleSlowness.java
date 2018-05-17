package gov.usgs.traveltime.tables;

import java.util.ArrayList;

import org.apache.commons.math3.analysis.solvers.PegasusSolver;

import gov.usgs.traveltime.ModConvert;

/**
 * The slowness sampling is much different from the velocity sampling 
 * in the Earth model.  In the Earth model, the sampling just needs 
 * to be good enough for the spline fits to be reasonable.  The slowness 
 * sampling is complex for a variety of reasons.  In addition to needing 
 * to sample the travel times (tau-p) adequately in distance, the Earth 
 * model sampling in radius should be sampled finely enough to not miss 
 * any important detail and the sampling in slowness must be regular 
 * enough to avoid stability problems in the interpolation.  To make it 
 * even more difficult, all desired phases are generated on the same 
 * slowness sampling for production performance reasons.  This means that 
 * the sampling of P, for example, needs to be fine enough that the 
 * sampling of P'P' is also adequate even though the ray travel distances 
 * are much larger for the same slowness grid.  Note that the over sampling 
 * of phases like P are then compensated by decimating the sampling at a 
 * later stage.  Of course, the decimated P sampling will be a subset of the 
 * P'P' sampling, which simplifies computation when compensating for source 
 * depth.
 * 
 * @author Ray Buland
 *
 */
public class SampleSlowness {
	int limit;			// Index of the deepest Earth model sample for this shell
	double delX;		// Non-dimensional step length in range
	EarthModel refModel;
	InternalModel locModel;
	TauModel tauModel;
	ModConvert convert;
	TauInt tauInt;
	ArrayList<ModelSample> model;
	ArrayList<ModelShell> shells;
	ArrayList<CritSlowness> critical;
	ArrayList<TauSample> tmpModel;
	PegasusSolver solver;
	FindCaustic findCaustic;
	FindRange findRange;
	FindRadius findRadius;

	/**
	 * The tau model will contain the output of the slowness sampling 
	 * process.
	 * 
	 * @param locModel Re-sampled Earth model
	 * @param tauModel Slowness versus depth Earth model view
	 * @param tauInt Integration logic
	 */
	public SampleSlowness(InternalModel locModel, TauModel tauModel, 
			TauInt tauInt) {
		this.locModel = locModel;
		refModel = locModel.refModel;
		convert = locModel.convert;
		model = locModel.model;
		shells = locModel.shells;
		critical = locModel.getCritical();
		this.tauModel = tauModel;
		this.tauInt = tauInt;
		tmpModel = new ArrayList<TauSample>();
		solver = new PegasusSolver();
		findCaustic = new FindCaustic(tauInt);
		findRange = new FindRange(tauInt);
		findRadius = new FindRadius(refModel, convert);
	}
	
	/**
	 * Do the sampling for both P and S velocities.  Note that they will be 
	 * merged at a later stage.
	 * 
	 * @param type Velocity/slowness type (P = P-wave, S = S-wave)
	 * @throws Exception On an illegal integration interval
	 */
	public void sample(char type) throws Exception {
		int iShell, iCrit, nSamp, iTop;
		double slowTop, xTop, rTop, slowBot, xBot, rBot, dSlow, slowMin, slow, 
			pCaustic;
		ModelShell shell;
		CritSlowness crit;
		TauSample sample0, sample1, sample2;
		
		// Initialize temporary variables.
		iShell = shells.size()-1;
		shell = shells.get(iShell);
		delX = shell.delX;
		limit = shell.iBot;
		/* 
		 * Loop over critical points.  Because the critical points are branch 
		 * ends, this strategy guarantees that all possible branches are 
		 * sampled exactly at their ends.
		 */
		slowTop = model.get(shell.iTop).getSlow(type);
		for(iCrit=critical.size()-2; iCrit>=0; iCrit--) {
			crit = critical.get(iCrit);
			if(crit.slowness < slowTop) {
				iShell = crit.getShell(type);
				shell = shells.get(iShell);
				delX = shell.delX;
				limit = shell.iBot;
				if(shell.name == null) {
					System.out.format("\nShell: %3d %3d %6.2f %s\n", iShell, limit, 
							delX/convert.xNorm, shell.altName);
				} else {
					System.out.format("\nShell: %3d %3d %6.2f %s\n", iShell, limit, 
							delX/convert.xNorm, shell.name);
				}
				slowBot = crit.slowness;
				// Sample the top and bottom of this layer.
				tmpModel.clear();
				tauInt.intX(type, slowTop, limit);
				xTop = tauInt.getXSum();
				rTop = tauInt.getRbottom();
				// We can save this one now.
				tmpModel.add(new TauSample(rTop, slowTop, xTop));
				tauInt.intX(type, slowBot, limit);
				xBot = tauInt.getXSum();
				rBot = tauInt.getRbottom();
				// Figure out the initial sampling.
				nSamp = Math.max((int) (Math.abs(xBot-xTop)/delX+0.8), 1);
				dSlow = (slowTop-slowBot)/(nSamp*nSamp);
				slowMin = (slowTop-slowBot)/nSamp;
				System.out.format("Samp: %3d %10.4e %10.4e\n", nSamp, dSlow, slowMin);
				for(int k=1; k<nSamp; k++) {
					slow = slowTop - Math.max(k*k*dSlow, k*slowMin);
					tauInt.intX(type, slow, limit);
					tmpModel.add(new TauSample(tauInt.getRbottom(), slow, 
							tauInt.getXSum()));
				}
				// Now save the bottom.
				tmpModel.add(new TauSample(rBot, slowBot, xBot));
				// Check for hidden caustics.
				testSampling(type, dSlow);
				// Print it out for testing purposes.
				System.out.println("Temporary "+type+" slowness model:");
				for(int j=0; j<tmpModel.size(); j++) {
					System.out.format("%3d %s\n", j, tmpModel.get(j));
				}
				
				/* 
				 * Look for caustics.  Using dX/dp = 0 is slick for refining the 
				 * location of caustics, but it has problems.  First, it's infinite 
				 * at the top of every shell, which may lead to missing small 
				 * triplications.  Second, it has backwards compatibility problems 
				 * making testing against the Fortran version more difficult.  The 
				 * hybrid approach below isn't elegant, but resolves both issues.
				 */
				iTop = 0;
				sample1 = tmpModel.get(0);
				sample2 = tmpModel.get(1);
				for(int iBot=1; iBot<tmpModel.size()-1; iBot++) {
					sample0 = sample1;
					sample1 = sample2;
					sample2 = tmpModel.get(iBot+1);
					if((sample1.x-sample0.x)*(sample2.x-sample1.x) <= 0d) {
						pCaustic = getCaustic(type, sample2.slow, sample0.slow, limit);
						tauInt.intX(type, pCaustic, limit);
						sample1 = new TauSample(tauInt.getRbottom(), pCaustic, 
								tauInt.getXSum());
						tmpModel.set(iBot, sample1);
						System.out.format("\n Caustic: %3d %s\n", iBot, sample1);
						refineSampling(type, iTop, iBot);
						iTop = iBot;
					}
				}
				refineSampling(type, iTop, tmpModel.size()-1);
				slowTop = slowBot;
			}
		}
		refineRadius(type);
	}
	
	/**
	 * If a shell only has two points, make sure there isn't a caustic hiding 
	 * inside.  This is pretty crude, but seems to be adequate for the current 
	 * range of models.
	 * 
	 * @param type Velocity/slowness type (P = P-wave, S = S-wave)
	 * @param dSlow Non-dimensional slowness increment
	 * @throws Exception 
	 */
	private void testSampling(char type, double dSlow) throws Exception {
		double slow, x;
		
		if(tmpModel.size() == 2) {
			slow = tmpModel.get(0).slow-0.25d*dSlow;
			tauInt.intX(type, slow, limit);
			x = tauInt.getXSum();
			System.out.format("    extra = %8.6f %8.6f\n", slow, x);
			if((x-tmpModel.get(0).x)*(tmpModel.get(1).x-x) <= 0d) {
				tmpModel.add(tmpModel.get(1));
				tmpModel.set(1, new TauSample(tauInt.getRbottom(), slow, x));
			}
		}
	}
	
	/**
	 * Based on the rough sample created above, refine the sampling so that 
	 * the model is reasonably sampled in range (ray travel distance) and 
	 * radius.
	 * 
	 * @param type Velocity/slowness type (P = P-wave, S = S-wave)
	 * @param iTop Starting temporary tau model sample index
	 * @param iBot Ending temporary tau model sample index
	 * @throws Exception On an illegal integration interval
	 */
	private void refineSampling(char type, int iTop, int iBot) throws Exception {
		int nSamp, iTmp, iTau, iRadius;
		double xTarget, dX, xBot, pBot, pTarget, rTarget, dSlow;
		ModelSample model0, model1;
		TauSample sample0 = null, sample1;
		
		if(tauModel.size(type) == 0) {
			tauModel.add(type, tmpModel.get(iTop));
		}
		// Figure out the initial sampling.
		xTarget = tmpModel.get(iTop).x;
		xBot = tmpModel.get(iBot).x;
		pBot = tmpModel.get(iBot).slow;
		nSamp = Math.max((int) (Math.abs(xBot-xTarget)/delX+0.8), 1);
		dX = (xBot-xTarget)/nSamp;
		System.out.format("Samp: %2d %10.4e %10.4e\n", nSamp, xTarget, dX);
		
		/*
		 *  Ambitious loop trying to optimize sampling in range, slowness, 
		 *  and radius.
		 */
		iTmp = iTop+1;
		do {
			// Make a step.
			xTarget = xTarget+dX;
			System.out.format("\txTarget dX: %8.6f %10.4e\n", xTarget, dX);
			
			// Set the next range.
			if(Math.abs(xTarget-xBot) > TablesUtil.XTOL) {
				// Bracket the range.
				sample1 = tmpModel.get(iTmp-1);
				for(; iTmp<=iBot; iTmp++) {
					sample0 = sample1;
					sample1 = tmpModel.get(iTmp);
					if((xTarget-sample1.x)*(xTarget-sample0.x) <= 0d) break;
				}
				// Test for some sort of odd failure.
				if(iTmp > iBot) {
					iTmp = iBot;
					System.out.format("====> Off-the-end: %3d %8.6f %8.6f %8.6f\n", 
							iTmp, xTarget, sample1.x, sample0.x);
				}
				// Find the slowness that gives the desired range.
				pTarget = getRange(type, sample1.slow, sample0.slow, xTarget, limit);
				tauInt.intX(type, pTarget, limit);
				tauModel.add(type, new TauSample(tauInt.getRbottom(), pTarget, 
						tauInt.getXSum()));
				System.out.format("sol     %3d %s\n", tauModel.size(type)-1, 
						tauModel.getLast(type));
			} else {
				// Add the last sample, though this may not be the end...
				tauModel.add(type, tmpModel.get(iBot));
			}
			
			iTau = tauModel.size(type)-1;
			sample0=tauModel.getSample(type, iTau-1);
			// Make sure our slowness sampling is OK.
			if(Math.abs(tauModel.getSample(type, iTau).slow-sample0.slow) > 
					TablesUtil.DELPMAX) {
				// Oops!  Fix the last sample.
				nSamp = Math.max((int) (Math.abs(pBot-sample0.slow)/
						TablesUtil.DELPMAX+0.99d), 1);
				pTarget = sample0.slow+(pBot-sample0.slow)/nSamp;
				tauInt.intX(type, pTarget, limit);
				tauModel.setLast(type, new TauSample(tauInt.getRbottom(), pTarget, 
						tauInt.getXSum()));
				System.out.format(" dpmax  %3d %s\n", tauModel.size(type)-1, 
						tauModel.getLast(type));
				// Reset the range sampling.
				xTarget = tauModel.getLast(type).x;
				nSamp = Math.max((int) (Math.abs(xBot-xTarget)/delX+0.8), 1);
				dX = (xBot-xTarget)/nSamp;
//			System.out.format("Samp: %2d %10.4e %10.4e\n", nSamp, xTarget, dX);
				iTmp = iTop+1;
			}
			
			// Make sure our radius sampling is OK too.
			if(Math.abs(tauModel.getSample(type, iTau).r-sample0.r) > 
					TablesUtil.DELRMAX) {
				// Oops!  Fix the last sample.
				rTarget = sample0.r-TablesUtil.DELRMAX;
				iRadius = tauInt.getIbottom();
				while(rTarget > locModel.getR(iRadius)) {
					iRadius--;
				}
				// Turn the target radius into a slowness increment.
				model0 = locModel.model.get(iRadius);
				model1 = locModel.model.get(iRadius+1);
				dSlow = Math.abs(sample0.slow-model0.getSlow(type)*
						Math.pow(rTarget/model0.r, Math.log(model1.getSlow(type)/
						model0.getSlow(type))/Math.log(model1.r/model0.r)));
				// Do the fixing.
				nSamp = Math.max((int) (Math.abs(pBot-sample0.slow)/dSlow+0.99d), 
						1);
				pTarget = sample0.slow+(pBot-sample0.slow)/nSamp;
				tauInt.intX(type, pTarget, limit);
				tauModel.setLast(type, new TauSample(tauInt.getRbottom(), pTarget, 
						tauInt.getXSum()));
				System.out.format(" drmax  %3d %s\n", tauModel.size(type)-1, 
						tauModel.getLast(type));
				// Reset the range sampling.
				xTarget = tauModel.getLast(type).x;
				nSamp = Math.max((int) (Math.abs(xBot-xTarget)/delX+0.8), 1);
				dX = (xBot-xTarget)/nSamp;
				System.out.format("Samp: %2d %10.4e %10.4e\n", nSamp, xTarget, dX);
				iTmp = iTop+1;
			}
		} while(Math.abs(xTarget-xBot) > TablesUtil.XTOL);
		System.out.format("end     %3d %s\n", tauModel.size(type)-1, 
				tauModel.getLast(type));
	}
	
	/**
	 * Refine the Earth radii in kilometers corresponding to the slowness 
	 * samples by going back to the reference Earth model.
	 * 
	 * @param type Slowness type (P = P slowness, S = S slowness)
	 */
	private void refineRadius(char type) {
		TauSample sample;
		
		for(int j=0; j<tauModel.size(type); j++) {
			sample = tauModel.getSample(type, j);
			sample.r = getRadius(type, sample.r, sample.slow);
		}
	}
	
	/**
	 * Find a caustic that has been bracketed.  This works by finding a zero in 
	 * the derivative of distance with ray parameter.  Note that ray parameter 
	 * and slowness may appear to be used interchangeably.  Conceptually, we're 
	 * shooting rays into the Earth at different angles (summarized in the ray 
	 * parameter) looking for the caustic.  This ties into the model because we 
	 * want a model sample at the slowness where the caustic ray bottoms (i.e., 
	 * where the slowness and ray parameter are equal).
	 * 
	 * @param type Slowness type (P = P slowness, S = S slowness)
	 * @param minP Non-dimensional ray parameter smaller than the caustic ray 
	 * parameter
	 * @param maxP Non-dimensional ray parameter larger than the caustic ray 
	 * parameter
	 * @param limit Index of the deepest layer of the model to integrate to
	 * @return Non-dimensional ray parameter at the caustic
	 */
	private double getCaustic(char type, double minP, double maxP, int limit) {
		double pCaustic;
		
		findCaustic.setUp(type, limit);
		pCaustic = solver.solve(TablesUtil.MAXEVAL, findCaustic, minP, maxP);
//	System.out.format("\tCaustic: %8.6f [%8.6f,%8.6f] %2d\n", pCaustic, 
//			minP, maxP, solver.getEvaluations());
		return pCaustic;
	}
	
	/**
	 * Find the non-dimensional ray parameter that results in a target non-
	 * dimensional ray travel distance or range on the surface.
	 * 
	 * @param type Slowness type (P = P slowness, S = S slowness)
	 * @param minP Non-dimensional ray parameter smaller than the ray  
	 * parameter for the target range
	 * @param maxP Non-dimensional ray parameter larger than the ray 
	 * parameter for the target range
	 * @param xTarget Non-dimensional target range
	 * @param limit Index of the deepest layer of the model to integrate to
	 * @return Non-dimensional ray parameter of the ray with the target range
	 */
	private double getRange(char type, double minP, double maxP, double xTarget, 
			int limit) {
		double pRange;
		
		findRange.setUp(type, xTarget, limit);
		pRange = solver.solve(TablesUtil.MAXEVAL, findRange, minP, maxP);
//	System.out.format("\tRange: %8.6f [%8.6f,%8.6f] %2d\n", pRange, 
//			minP, maxP, solver.getEvaluations());
		return pRange;
	}
	
	/**
	 * Find the Earth radius in kilometers that corresponds to the bottoming 
	 * depth of a ray with a desired target non-dimensional ray parameter.  Note 
	 * that we already have an Earth radius corresponding to each slowness 
	 * sample.  However, this was computed from the internal model by backing 
	 * radius out from the flattened Earth model.  Going back to the cubic 
	 * spline interpolation of the reference Earth model refines these values, 
	 * which will be used for correcting travel times for source depth.
	 * 
	 * @param type Slowness type (P = P slowness, S = S slowness)
	 * @param oldR Radius in kilometers corresponding to the target slowness
	 * @param pTarget Target ray parameter
	 * @return Radius in kilometers where the reference Earth model slowness 
	 * matches the desired ray parameter
	 */
	private double getRadius(char type, double oldR, double pTarget) {
		double minR, maxR, radius;
		
		findRadius.setUp(type, pTarget);
		minR = oldR;
		maxR = minR;
		if(oldR*convert.tNorm/refModel.getVel(type, oldR) >= pTarget) {
			do {
				maxR += 1d;
			} while(maxR*convert.tNorm/refModel.getVel(type, maxR) <= pTarget);
		} else {
			do {
				minR -= 1d;
			} while(minR*convert.tNorm/refModel.getVel(type, minR) >= pTarget);
		}
		radius = solver.solve(TablesUtil.MAXEVAL, findRadius, minR, maxR);
		System.out.format("\tRadius: %7.2f [%7.2f,%7.2f] %2d\n", radius, 
				minR, maxR, solver.getEvaluations());
		return radius;
	}
}
