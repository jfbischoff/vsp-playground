/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.santiago.run;

import java.text.ParseException;
import java.util.Collections;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.VehicleGenerator;
import org.matsim.contrib.dvrp.data.VehicleImpl;
import org.matsim.contrib.dvrp.data.file.VehicleWriter;
import org.matsim.contrib.util.random.RandomUtils;
import org.matsim.contrib.util.random.UniformRandom;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author michalm
 */
public class AVGenerator {
	private static class AVCreator implements VehicleGenerator.VehicleCreator {
		private static final int PAX_PER_CAR = 4;

		// 1 : 3 is the proportion in density between bigger and smaller rectangle
		private static final double DENSITY_RELATION = 1. / 4;

		private static final Coord MIN_COORD_SMALLER_RECTANGLE = new Coord(342295.1142950431, 6291210.245397029);
		private static final Coord MAX_COORD_SMALLER_RECTANGLE = new Coord(351912.2889312578, 6301887.03896847);

		private static final Coord MIN_COORD_BIGGER_RECTANGLE = new Coord(335093.5800615201, 6282523.057405231);
		private static final Coord MAX_COORD_BIGGER_RECTANGLE = new Coord(359383.2200004731, 6306617.889938615);

		private final Network network;
		private int currentVehicleId = 0;

		public AVCreator(Scenario scenario) {
			network = NetworkUtils.createNetwork();
			new TransportModeNetworkFilter(scenario.getNetwork()).filter(network, Collections.singleton("car"));
		}

		@Override
		public VehicleImpl createVehicle(double t0, double t1) {
			Id<Vehicle> vehId = Id.create("taxi" + currentVehicleId++, Vehicle.class);

			Coord coord = RandomUtils.getGlobalGenerator().nextDouble() < DENSITY_RELATION
					? randomCoordInBiggerRectangle() : randomCoordInSmallerRectangle();

			Link link = NetworkUtils.getNearestLinkExactly(network, coord);
			return new VehicleImpl(vehId, link, PAX_PER_CAR, Math.round(t0), Math.round(t1));
		}

		private Coord randomCoordInSmallerRectangle() {
			UniformRandom uniform = RandomUtils.getGlobalUniform();
			double x = uniform.nextDouble(MIN_COORD_SMALLER_RECTANGLE.getX(), MAX_COORD_SMALLER_RECTANGLE.getX());
			double y = uniform.nextDouble(MIN_COORD_SMALLER_RECTANGLE.getY(), MAX_COORD_SMALLER_RECTANGLE.getY());
			return new Coord(x, y);
		}

		private Coord randomCoordInBiggerRectangle() {
			UniformRandom uniform = RandomUtils.getGlobalUniform();
			for (;;) {
				double x = uniform.nextDouble(MIN_COORD_BIGGER_RECTANGLE.getX(), MAX_COORD_BIGGER_RECTANGLE.getX());
				double y = uniform.nextDouble(MIN_COORD_BIGGER_RECTANGLE.getY(), MAX_COORD_BIGGER_RECTANGLE.getY());
				if (x < MIN_COORD_SMALLER_RECTANGLE.getX() || x > MAX_COORD_SMALLER_RECTANGLE.getX()
						|| y < MIN_COORD_SMALLER_RECTANGLE.getY() || y > MAX_COORD_SMALLER_RECTANGLE.getY()) {
					return new Coord(x, y);
				}
			}
		}

	}

	public static void main(String[] args) throws ParseException {
		String dir = "../../runs-svn/santiago/v2a/";
		String networkFile = dir + "input/network_merged_cl.xml.gz";
		String taxisFilePrefix = dir + "taxis_";

		// we start at 4:30 with vehicles, and at 5:00 with requests
		double startTime = 0;
		double workTime = 30 * 3600;

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);

		for (int i = 200; i <= 1600; i += 200) {
			AVCreator avc = new AVCreator(scenario);
			VehicleGenerator vg = new VehicleGenerator(workTime, workTime, avc);
			vg.generateVehicles(new double[] { i, i }, startTime, 30 * 3600);
			new VehicleWriter(vg.getVehicles()).write(taxisFilePrefix + i + ".xml");
		}
	}
}
