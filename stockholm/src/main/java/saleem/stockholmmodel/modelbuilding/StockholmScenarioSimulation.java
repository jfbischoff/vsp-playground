/*
 * Copyright 2018 Mohammad Saleem
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: salee@kth.se
 *
 */
package saleem.stockholmmodel.modelbuilding;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.utils.CreatePseudoNetwork;

import ch.sbb.matsim.mobsim.qsim.SBBQSimModule;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

/**
 * Execution class for StockholmPTCar model. Before running the simulation, the
 * main function reads the Config file, gets the storage capacity factor to
 * calculate the sample size, and then sets the sitting capacity, standing
 * capacity and passenger car equivalents of the vehicle types based on the
 * sample size. This is neccessary if a smaller than 100% demand sample is
 * executed, in the same way as setting storage capacity and flow capacity (in
 * the Config file) of the network links.
 * 
 * @author Mohammad Saleem
 */
public class StockholmScenarioSimulation {
	public static void main(String[] args) {

//		System.out.println("System.exit(0);");
//		System.exit(0);
		
		String path = args[0];

		Config config = ConfigUtils.loadConfig(path);
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		// If due to network cleanup, the network has to be scaled up, then set sample
		// size here manually, according to population sample size
		double samplesize = config.qsim().getStorageCapFactor(); // Changing vehicle and road capacity according to
																	// sample size
		PTCapacityAdjusmentPerSample capadjuster = new PTCapacityAdjusmentPerSample();
		capadjuster.adjustStoarageAndFlowCapacity(scenario, samplesize);

		Network network = scenario.getNetwork();
		TransitSchedule schedule = scenario.getTransitSchedule();
		new CreatePseudoNetwork(schedule, network, "tr_").createNetwork();

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				install(new SwissRailRaptorModule());
			}
		});
		controler.run();
	}
}
