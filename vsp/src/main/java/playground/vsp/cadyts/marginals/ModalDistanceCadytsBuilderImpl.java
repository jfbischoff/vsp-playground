/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.vsp.cadyts.marginals;

import cadyts.calibrators.analytical.AnalyticalCalibrator;
import cadyts.measurements.SingleLinkMeasurement;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.LookUpItemFromId;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.misc.Time;
import playground.vsp.cadyts.marginals.prep.DistanceBin;

import java.util.Map;

/**
 * @author nagel
 * @author mrieser
 */
public final class ModalDistanceCadytsBuilderImpl {

	public static final String MARGINALS = "_marginals";

	private static Logger log = Logger.getLogger( ModalDistanceCadytsBuilderImpl.class ) ;

	public static <T> AnalyticalCalibrator<T> buildCalibratorAndAddMeasurements(final Config config, final Map<Id<T>, DistanceBin> inputDistanceDistribution,
																				LookUpItemFromId<T> lookUp, Class<T> idType) {

		if (inputDistanceDistribution.isEmpty()) {
			log.warn("Distance distribution container is empty.");
		}

		CadytsConfigGroup cadytsConfig = ConfigUtils.addOrGetModule(config, CadytsConfigGroup.GROUP_NAME, CadytsConfigGroup.class);
		// set the variance scale to a bigger value, so that corrections don't get cut of at '15.0'. The counts weight
		// must be set accordingly
		cadytsConfig.setVarianceScale(100);

		AnalyticalCalibrator<T> matsimCalibrator = ModalDistanceCadytsBuilderImpl.buildCalibrator(config);

		//add counts data into calibrator
		int numberOfAddedMeasurements = 0 ;
		for(Map.Entry< Id<T>, DistanceBin> entry : inputDistanceDistribution.entrySet()){
			// (loop over all counting "items" (usually locations/stations)

			T item = lookUp.getItem(Id.create(entry.getKey(), idType)) ;

			if ( item==null ) {
				throw new RuntimeException("item is null; entry=" + entry + " idType=" + idType ) ;
			}
			DistanceBin bin = entry.getValue();
			//only one measurement per day.
			// set the standard deviation for marginals to a 'big' value to avoid the cadyts correction being cut of at
			// '15.0' for all bins. When all bins pull with a correction of '15.0' all corrections cancel out each other.
			// The correction factor is calculated as followed: (expectedValue - simulatedValue) / stddev^2
			// the stddev can also be set globally for cadyts but then it would also affect the counts calibration
			matsimCalibrator.addMeasurement(item, 0, 86400, bin.getCount(), bin.getStandardDeviation(), SingleLinkMeasurement.TYPE.COUNT_VEH);
			numberOfAddedMeasurements++;
		}

        if ( numberOfAddedMeasurements==0 ) {
			log.warn("No measurements were added.");
        }
		
        if ( matsimCalibrator.getProportionalAssignment() ) {
        	throw new RuntimeException("Gunnar says that this may not work so do not set to true. kai, sep'14") ;
        }
		return matsimCalibrator;
	}

	public static <T> AnalyticalCalibrator<T> buildCalibrator(final Config config) {
		CadytsConfigGroup cadytsConfig = ConfigUtils.addOrGetModule(config, CadytsConfigGroup.GROUP_NAME, CadytsConfigGroup.class ) ;

		//get timeBinSize_s and validate it
		if ((Time.MIDNIGHT % cadytsConfig.getTimeBinSize())!= 0 ){
			throw new RuntimeException("Cadyts requires a divisor of 86400 as time bin size value .");
		}
		if ( (cadytsConfig.getTimeBinSize() % 3600) != 0 ) {
			throw new RuntimeException("At this point, time bin sizes need to be multiples of 3600.  This is not a restriction " +
					"of Cadyts, but of the counts file format, which only allows for hourly inputs") ;
		}


		AnalyticalCalibrator<T> matsimCalibrator = new AnalyticalCalibrator<>(
				config.controler().getOutputDirectory() + "/cadyts"+MARGINALS+".log",
				MatsimRandom.getLocalInstance().nextLong(),cadytsConfig.getTimeBinSize()
		) ;

		matsimCalibrator.setRegressionInertia(cadytsConfig.getRegressionInertia()) ;
		matsimCalibrator.setMinStddev(cadytsConfig.getMinFlowStddev_vehPerHour(), SingleLinkMeasurement.TYPE.FLOW_VEH_H);
		matsimCalibrator.setMinStddev(cadytsConfig.getMinFlowStddev_vehPerHour(), SingleLinkMeasurement.TYPE.COUNT_VEH);
		matsimCalibrator.setFreezeIteration(cadytsConfig.getFreezeIteration());
		matsimCalibrator.setPreparatoryIterations(cadytsConfig.getPreparatoryIterations());
		matsimCalibrator.setVarianceScale(cadytsConfig.getVarianceScale());

		matsimCalibrator.setBruteForce(cadytsConfig.useBruteForce());
		// I don't think this has an influence on any of the variants we are using. (Has an influence only when plan choice is left
		// completely to cadyts, rather than just taking the score offsets.) kai, dec'13
		// More formally, one would need to use the selectPlan() method of AnalyticalCalibrator which we are, however, not using. kai, mar'14
		if ( matsimCalibrator.getBruteForce() ) {
			log.warn("setting bruteForce==true for calibrator, but this won't do anything in the way the cadyts matsim integration is set up. kai, mar'14") ;
		}

		matsimCalibrator.setStatisticsFile(config.controler().getOutputDirectory() + "/calibration-stats"+MARGINALS+".txt");
		return matsimCalibrator;
	}
}