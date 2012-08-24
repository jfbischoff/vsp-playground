/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.droeder.southAfrica.testScenario;

import org.apache.log4j.Logger;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.PtConstants;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

import playground.andreas.P2.helper.PConfigGroup;
import playground.andreas.P2.helper.PScenarioImpl;
import playground.andreas.P2.schedule.PTransitRouterImplFactory;
import playground.droeder.southAfrica.PtSubModeControler;
import playground.droeder.southAfrica.qSimHook.TransitSubModeQSimFactory;

/**
 * @author droeder
 *
 */
public class RunRsaMultiModalTest {
	
	
	private final static Logger log = Logger.getLogger(RunRsaMultiModalTest.class);

	public static void main(final String[] args) {
		
		if(args.length == 0){
			log.info("Arg 1: config.xml");
			System.exit(1);
		}
		sim(args[0]);
		
	}

	
	private static void sim(String conf) {
		Config config = new Config();
		config.addModule(PConfigGroup.GROUP_NAME, new PConfigGroup());
		ConfigUtils.loadConfig(config, conf);
		
		PScenarioImpl scenario = new PScenarioImpl(config);
		ScenarioUtils.loadScenario(scenario);
		
//		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(conf));
	
		Controler controler = new PtSubModeControler(scenario, true);
		controler.setOverwriteFiles(true);
		controler.setCreateGraphs(true);
		
		// manipulate config
		// add "pt interaction" cause controler.init() is called too late and in a protected way
		ActivityParams transitActivityParams = new ActivityParams(PtConstants.TRANSIT_ACTIVITY_TYPE);
		transitActivityParams.setTypicalDuration(120.0);
		scenario.getConfig().planCalcScore().addActivityParams(transitActivityParams);
		
		// TODO[dr] this is very confusing, but the PTransitRouterFactory needs to be registered as controlerListener, because it provides
		//some essential functionality to the P-module
		PTransitRouterImplFactory pFact = new PTransitRouterImplFactory(controler);
//		PtSubModeDependRouterFactory pFact = new PtSubModeDependRouterFactory(controler, true);
		controler.addControlerListener(pFact);		
//		controler.setTransitRouterFactory(pFact);
		//set mobsimFactory
		controler.setMobsimFactory(new TransitSubModeQSimFactory());
		controler.addControlerListener(new WriteSelectedPlansAfterIteration());
		controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());

		controler.run();
		OTFVis.playMVI(controler.getControlerIO().getIterationFilename(controler.getLastIteration(), "otfvis.mvi"));
	}
}
