/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.ikaddoura.berlin;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import playground.ikaddoura.analysis.IKAnalysisRun;
import playground.ikaddoura.analysis.modalSplitUserType.AgentAnalysisFilter;
import playground.ikaddoura.durationBasedTimeAllocationMutator.DurationBasedTimeAllocationPlanStrategyProvider;

/**
* @author ikaddoura
*/

public class RunMATSim7 {

	private static final Logger log = Logger.getLogger(RunMATSim7.class);

	private static String configFile;
	private static String outputDirectory;
	private static String runId;
	private static String visualizationScriptInputDirectory;
	
	private static double ascCar;
	private static double ascPt;
	private static double ascTransitWalk;
	private static double ascWalk;
	private static double ascBicycle;
	private static double ascRide;	
	
	private static double marginalUtilityTravelingCar = Double.POSITIVE_INFINITY;
	private static double marginalUtilityTravelingPt = Double.POSITIVE_INFINITY;
	private static double marginalUtilityTravelingTransitWalk = Double.POSITIVE_INFINITY;
	private static double marginalUtilityTravelingWalk = Double.POSITIVE_INFINITY;
	private static double marginalUtilityTravelingBicycle = Double.POSITIVE_INFINITY;
	private static double marginalUtilityTravelingRide = Double.POSITIVE_INFINITY;
	
	private static boolean useCongestedCarRouterForRide;
	private static double probaForRandomSingleTripMode;
	
	public static void main(String[] args) {
		if (args.length > 0) {
			
			configFile = args[0];		
			log.info("configFile: "+ configFile);
			
			outputDirectory = args[1];
			log.info("outputDirectory: "+ outputDirectory);
			
			runId = args[2];
			log.info("runId: "+ runId);
			
			visualizationScriptInputDirectory = args[3];
			log.info("visualizationScriptInputDirectory: "+ visualizationScriptInputDirectory);
			
			ascCar = Double.parseDouble(args[4]);
			log.info("ascCar: "+ ascCar);

			ascPt = Double.parseDouble(args[5]);
			log.info("ascPt: "+ ascPt);
			
			ascTransitWalk = Double.parseDouble(args[6]);
			log.info("ascTransitWalk: "+ ascTransitWalk);

			ascWalk = Double.parseDouble(args[7]);
			log.info("ascWalk: "+ ascWalk);

			ascBicycle = Double.parseDouble(args[8]);
			log.info("ascBicycle: "+ ascBicycle);
			
			ascRide = Double.parseDouble(args[9]);
			log.info("ascRide: "+ ascRide);
			
			marginalUtilityTravelingCar = Double.parseDouble(args[10]);
			log.info("marginalUtilityTravelingCar: "+ marginalUtilityTravelingCar);
			
			marginalUtilityTravelingPt = Double.parseDouble(args[11]);
			log.info("marginalUtilityTravelingPt: "+ marginalUtilityTravelingPt);
			
			marginalUtilityTravelingTransitWalk = Double.parseDouble(args[12]);
			log.info("marginalUtilityTravelingTransitWalk: "+ marginalUtilityTravelingTransitWalk);
			
			marginalUtilityTravelingWalk = Double.parseDouble(args[13]);
			log.info("marginalUtilityTravelingWalk: "+ marginalUtilityTravelingWalk);
			
			marginalUtilityTravelingBicycle = Double.parseDouble(args[14]);
			log.info("marginalUtilityTravelingBicycle: "+ marginalUtilityTravelingBicycle);
			
			marginalUtilityTravelingRide = Double.parseDouble(args[15]);
			log.info("marginalUtilityTravelingRide: "+ marginalUtilityTravelingRide);
			
			useCongestedCarRouterForRide = Boolean.parseBoolean(args[16]);
			log.info("useCongestedCarRouterForRide: "+ useCongestedCarRouterForRide);
			
			probaForRandomSingleTripMode = Double.parseDouble(args[17]);
			log.info("probaForRandomSingleTripMode: "+ probaForRandomSingleTripMode);

		} else {
			
			configFile = "/Users/ihab/Desktop/ils4a/ziemke/open_berlin_scenario/input/be_3_ik/config_be_300_mode-choice_test.xml";
			outputDirectory = "/Users/ihab/Documents/workspace/runs-svn/open_berlin_scenario/be_300_test_7/";
			runId = "test-run";
			ascCar = -1.;
			ascPt = -1.;
			ascWalk = 0.;
			ascBicycle = -1.;
			ascRide = -1.;
		}
		
		RunMATSim7 runner = new RunMATSim7();
		runner.run(configFile, outputDirectory, runId);
	}

	public void run(String configFile, String outputDirectory, String runId) {
		
		Config config = ConfigUtils.loadConfig(configFile);
		
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.failIfDirectoryExists);
		config.controler().setOutputDirectory(outputDirectory);
		config.controler().setRunId(runId);
		
		config.planCalcScore().getModes().get(TransportMode.car).setConstant(ascCar);
		config.planCalcScore().getModes().get(TransportMode.pt).setConstant(ascPt);
		config.planCalcScore().getModes().get(TransportMode.transit_walk).setConstant(ascTransitWalk);
		config.planCalcScore().getModes().get(TransportMode.walk).setConstant(ascWalk);
		config.planCalcScore().getModes().get("bicycle").setConstant(ascBicycle);
		config.planCalcScore().getModes().get(TransportMode.ride).setConstant(ascRide);

		config.planCalcScore().getModes().get(TransportMode.car).setMarginalUtilityOfTraveling(marginalUtilityTravelingCar);
		config.planCalcScore().getModes().get(TransportMode.pt).setMarginalUtilityOfTraveling(marginalUtilityTravelingPt);
		config.planCalcScore().getModes().get(TransportMode.transit_walk).setMarginalUtilityOfTraveling(marginalUtilityTravelingTransitWalk);
		config.planCalcScore().getModes().get(TransportMode.walk).setMarginalUtilityOfTraveling(marginalUtilityTravelingWalk);
		config.planCalcScore().getModes().get("bicycle").setMarginalUtilityOfTraveling(marginalUtilityTravelingBicycle);
		config.planCalcScore().getModes().get(TransportMode.ride).setMarginalUtilityOfTraveling(marginalUtilityTravelingRide);

		// own time allocation mutator strategy
		final String STRATEGY_NAME = "durationBasedTimeMutator";

		StrategySettings stratSets = new StrategySettings();
		stratSets.setStrategyName(STRATEGY_NAME);
		stratSets.setWeight(0.1);
		stratSets.setSubpopulation("person");
		config.strategy().addStrategySettings(stratSets);
		
		config.subtourModeChoice().setProbaForRandomSingleTripMode(probaForRandomSingleTripMode);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		
		// add own time allocation mutator strategy
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addPlanStrategyBinding(STRATEGY_NAME).toProvider(DurationBasedTimeAllocationPlanStrategyProvider.class);
			}
		});
		
		// use the sbb pt raptor router
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				install(new SwissRailRaptorModule());
			}
		});
		
		if (useCongestedCarRouterForRide) {
			// use the congested car router for the teleported ride mode
			controler.addOverridingModule(new AbstractModule(){
				@Override
				public void install() {
					addTravelTimeBinding(TransportMode.ride).to(networkTravelTime());
					addTravelDisutilityFactoryBinding(TransportMode.ride).to(carTravelDisutilityFactoryKey());        }
		    });
		}
				
		controler.run();
		
		log.info("Running analysis...");
				
		final String scenarioCRS = TransformationFactory.DHDN_GK4;	
		final String shapeFileZones = null;
		final String zonesCRS = null;
		final String homeActivity = "home";
		final int scalingFactor = 10;
		
		List<AgentAnalysisFilter> filters = new ArrayList<>();

		AgentAnalysisFilter filter1 = new AgentAnalysisFilter(scenario);
		filter1.setSubpopulation("person");
		filter1.setPersonAttribute("berlin");
		filter1.setPersonAttributeName("home-activity-zone");
		filter1.preProcess(scenario);
		filters.add(filter1);
		
		AgentAnalysisFilter filter2 = new AgentAnalysisFilter(scenario);
		filter2.preProcess(scenario);
		filters.add(filter2);
		
		AgentAnalysisFilter filter3 = new AgentAnalysisFilter(scenario);
		filter3.setSubpopulation("person");
		filter3.setPersonAttribute("brandenburg");
		filter3.setPersonAttributeName("home-activity-zone");
		filter3.preProcess(scenario);
		filter3.preProcess(scenario);
		filters.add(filter3);

		IKAnalysisRun analysis = new IKAnalysisRun(
				scenario,
				null,
				visualizationScriptInputDirectory,
				scenarioCRS,
				shapeFileZones,
				zonesCRS,
				homeActivity,
				scalingFactor,
				filters,
				null);
		analysis.run();
	
		log.info("Done.");
		
	}

}

