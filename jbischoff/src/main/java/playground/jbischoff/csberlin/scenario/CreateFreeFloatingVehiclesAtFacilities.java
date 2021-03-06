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
package playground.jbischoff.csberlin.scenario;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.mutable.MutableInt;
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
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

import playground.jbischoff.taxi.berlin.demand.TaxiDemandWriter;
import playground.jbischoff.utils.JbUtils;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class CreateFreeFloatingVehiclesAtFacilities {

	private String networkFile = "C:/Users/Joschka/Documents/shared-svn/projects/bmw_carsharing/data/scenario/network-car.xml.gz";
	private String shapeFile = "C:/Users/Joschka/Documents/shared-svn/projects/bmw_carsharing/data/scenario/gis/ff_current_business_area.shp";
	private String vehiclesFilePrefix = "C:/Users/Joschka/Documents/shared-svn/projects/bmw_carsharing/data/scenario/ff_";
	
	private final Scenario scenario ;
	private Geometry geometry;
	private Random random = MatsimRandom.getRandom();
	private List<DvrpVehicleSpecification> vehicles = new ArrayList<>();

	
	public static void main(String[] args) {
		CreateFreeFloatingVehiclesAtFacilities tvc = new CreateFreeFloatingVehiclesAtFacilities();
		tvc.run(5000);
}

	public CreateFreeFloatingVehiclesAtFacilities() {
				
		this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
		this.geometry = JbUtils.readShapeFileAndExtractGeometry(shapeFile, "ID").get("0");	
	}
	private void run(int amount) {
		final MutableInt number = new MutableInt(0);
		TabularFileParserConfig config = new TabularFileParserConfig();
		config.setDelimiterTags(new String[] { "\t" });
		config.setFileName("../../../shared-svn/projects/bmw_carsharing/data/parkplaetze50cs.txt");
		config.setCommentTags(new String[] { "#" });
		new TabularFileParser().parse(config, new TabularFileHandler() {

			@Override
			public void startRow(String[] row) {
				int ii = Integer.parseInt(row[2]);
				for (int i = 0;i<ii;i++){
					number.increment();
					Link link = scenario.getNetwork().getLinks().get(Id.createLinkId(row[0]));
					DvrpVehicleSpecification v = ImmutableDvrpVehicleSpecification.newBuilder()
							.id(Id.create(Id.create("ff" + number.intValue(), DvrpVehicle.class), DvrpVehicle.class))
							.startLinkId(link.getId())
							.capacity(5)
							.serviceBeginTime((double)Math.round(1))
							.serviceEndTime((double)Math.round(30 * 3600))
							.build();
			        vehicles.add(v);
				}
	
			}

		});
		
		
		for (int i = number.intValue() ; i< amount; i++){
		Point p = TaxiDemandWriter.getRandomPointInFeature(random, geometry);
		Link link = NetworkUtils.getNearestLinkExactly(((Network) scenario.getNetwork()),MGC.point2Coord(p));
			DvrpVehicleSpecification v = ImmutableDvrpVehicleSpecification.newBuilder()
					.id(Id.create(Id.create("ff" + i, DvrpVehicle.class), DvrpVehicle.class))
					.startLinkId(link.getId())
					.capacity(5)
					.serviceBeginTime((double)Math.round(1))
					.serviceEndTime((double)Math.round(30 * 3600))
					.build();
        vehicles.add(v);

		}
		new FleetWriter(vehicles.stream()).write(vehiclesFilePrefix + amount + ".xml");
	}

}
