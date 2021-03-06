/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.michalm.drt.run;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.av.maxspeed.DvrpTravelTimeWithMaxSpeedLimitModule;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

/**
 * @author michalm
 */
public class RunOneSharedTaxiWithMaxSpeedExample {
	private static final String CONFIG_FILE = "one_shared_taxi/one_shared_taxi_config.xml";

	public static void run(boolean otfvis, int lastIteration) {
		Config config = ConfigUtils.loadConfig(CONFIG_FILE, new DrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());
		config.controler().setLastIteration(lastIteration);
		config.controler().setWriteEventsInterval(lastIteration);
		Controler controler = DrtControlerCreator.createControlerWithSingleModeDrt(config, otfvis);
		VehicleType vehicleType = new VehicleTypeImpl(Id.create("autonomousVehicleType", VehicleType.class));
		vehicleType.setMaximumVelocity(1);
		controler.addOverridingModule(new DvrpTravelTimeWithMaxSpeedLimitModule(vehicleType));
		controler.run();
	}

	public static void main(String[] args) {
		run(true, 2); // switch to 'true' to turn on visualisation
	}
}
