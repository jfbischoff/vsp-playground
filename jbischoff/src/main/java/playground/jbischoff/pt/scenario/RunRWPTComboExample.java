/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

/**
 *
 */
package playground.jbischoff.pt.scenario;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.av.intermodal.router.VariableAccessTransitRouterModule;
import org.matsim.contrib.av.intermodal.router.config.VariableAccessConfigGroup;
import org.matsim.contrib.av.intermodal.router.config.VariableAccessModeConfigGroup;
import org.matsim.contrib.av.robotaxi.fares.taxi.TaxiFareHandler;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author jbischoff
 */
public class RunRWPTComboExample {
	public static void main(String[] args) {

		Config config = ConfigUtils.loadConfig(
				"C:/Users/Joschka/Documents/shared-svn/studies/jbischoff/multimodal/berlin/input/10pct/config.xml",
				new TaxiConfigGroup(), new DvrpConfigGroup());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		VariableAccessModeConfigGroup walk = new VariableAccessModeConfigGroup();
		walk.setDistance(1000);
		walk.setTeleported(true);
		walk.setMode("walk");

		config.global().setNumberOfThreads(4);

		//		VariableAccessModeConfigGroup bike = new VariableAccessModeConfigGroup();
		//		bike.setDistance(1100);
		//		bike.setTeleported(true);
		//		bike.setMode("bike");

		VariableAccessModeConfigGroup taxi = new VariableAccessModeConfigGroup();
		taxi.setDistance(200000);
		taxi.setTeleported(false);
		taxi.setMode("taxi");
		VariableAccessConfigGroup vacfg = new VariableAccessConfigGroup();
		vacfg.setAccessModeGroup(taxi);
		//		vacfg.setAccessModeGroup(bike);
		vacfg.setAccessModeGroup(walk);

		config.addModule(vacfg);
		config.transitRouter().setSearchRadius(3000);
		config.transitRouter().setExtensionRadius(0);

		String mode = TaxiConfigGroup.get(config).getMode();

		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new DvrpModule());
		controler.configureQSimComponents(DvrpQSimComponents.activateModes(mode));

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().to(TaxiFareHandler.class).asEagerSingleton();
			}
		});

		controler.addOverridingModule(new TaxiModule());
		controler.addOverridingModule(new VariableAccessTransitRouterModule());
		//       controler.addOverridingModule(new TripHistogramModule());
		//       controler.addOverridingModule(new OTFVisLiveModule());

		controler.run();

	}
}
