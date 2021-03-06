/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.jbischoff.av.preparation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetWriter;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;

import playground.jbischoff.taxi.berlin.demand.TaxiDemandWriter;

/**
 * @author jbischoff
 */
public class TaxiVehicleCreator {

	private String networkFile = "C:/Users/Joschka/Documents/shared-svn/projects/audi_av/scenario/networkc.xml.gz";
	private String shapeFile = "C:/Users/Joschka/Documents/shared-svn/projects/audi_av/shp/untersuchungsraumAll.shp";
	private String vehiclesFilePrefix = "C:/Users/Joschka/Documents/shared-svn/projects/audi_av/scenario/taxi_vehicles_";

	private Scenario scenario;
	private Geometry geometry;
	private Random random = MatsimRandom.getRandom();
	private List<DvrpVehicleSpecification> vehicles = new ArrayList<>();

	public static void main(String[] args) {
		TaxiVehicleCreator tvc = new TaxiVehicleCreator();
		for (int i = 10000; i < 25100; i = i + 1000) {
			System.out.println(i);
			tvc.run(i);
		}
	}

	public TaxiVehicleCreator() {

		this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
		this.geometry = ScenarioPreparator.readShapeFileAndExtractGeometry(shapeFile);
	}

	private void run(int amount) {

		for (int i = 0; i < amount; i++) {
			Point p = TaxiDemandWriter.getRandomPointInFeature(random, geometry);
			Link link = NetworkUtils.getNearestLinkExactly(((Network)scenario.getNetwork()), MGC.point2Coord(p));
			DvrpVehicleSpecification v = ImmutableDvrpVehicleSpecification.newBuilder()
					.id(Id.create(Id.create("rt" + i, DvrpVehicle.class), DvrpVehicle.class))
					.startLinkId(link.getId())
					.capacity(5)
					.serviceBeginTime((double)Math.round(1))
					.serviceEndTime((double)Math.round(48 * 3600))
					.build();
			vehicles.add(v);

		}
		new FleetWriter(vehicles.stream()).write(vehiclesFilePrefix + amount + ".xml.gz");
	}

}
