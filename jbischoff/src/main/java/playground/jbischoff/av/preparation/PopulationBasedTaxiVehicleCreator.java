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
import java.util.Map;
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
import org.matsim.contrib.util.random.WeightedRandomSelection;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

import playground.jbischoff.taxi.berlin.demand.TaxiDemandWriter;
import playground.jbischoff.utils.JbUtils;

/**
 * @author  jbischoff
 *
 */
public class PopulationBasedTaxiVehicleCreator
	{


//	private String networkFile = "../../../shared-svn/projects/vw_rufbus/av_simulation/demand/zones/network_noptvw.xml";
//	private String shapeFile = "../../../shared-svn/projects/vw_rufbus/av_simulation/demand/zones/zones_via.shp";
//	private String vehiclesFilePrefix = "../../../shared-svn/projects/vw_rufbus/av_simulation/vehicles/v";
//	private String populationData = "../../../shared-svn/projects/vw_rufbus/av_simulation/demand/zones/pop.csv";
//	
    
//	private String networkFile = "../../../shared-svn/studies/jbischoff/multimodal/berlin/input/10pct/network.final10pct_car.xml.gz";
	private String networkFile = "/Users/ihab/Desktop/ils4/kaddoura/optAV/input/be_251.output_network.xml.gz";
	private String shapeFile = "/Users/ihab/Documents/workspace/shared-svn/projects/audi_av/shp/Planungsraum.shp";
	private String vehiclesFilePrefix = "/Users/ihab/Documents/workspace/shared-svn/studies/jbischoff/berlin-internalisation/vehicles_be_251/v";
	private String populationData = "/Users/ihab/Documents/workspace/shared-svn/projects/audi_av/shp/bevoelkerung.txt";
	
	
	private Scenario scenario ;
	Map<String,Geometry> geometry;
	private Random random = MatsimRandom.getRandom();
		private List<DvrpVehicleSpecification> vehicles = new ArrayList<>();
    private final WeightedRandomSelection<String> wrs;
    CoordinateTransformation ct; 

	
	public static void main(String[] args) {
		for (int i = 2000; i<=21000 ; i=i+1000 ){
			PopulationBasedTaxiVehicleCreator tvc = new PopulationBasedTaxiVehicleCreator();
			System.out.println(i);
			tvc.run(i);
		}
}

	public PopulationBasedTaxiVehicleCreator() {
				
		this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
		this.geometry = JbUtils.readShapeFileAndExtractGeometry(shapeFile, "SCHLUESSEL");	
//		this.geometry = JbUtils.readShapeFileAndExtractGeometry(shapeFile, "ID"); //wolfsburg	
		this.wrs = new WeightedRandomSelection<>();
		this.ct = TransformationFactory.getCoordinateTransformation("EPSG:25833", TransformationFactory.DHDN_GK4);
        readPopulationData();
	}
	
	private void readPopulationData() {
		
		TabularFileParserConfig config = new TabularFileParserConfig();
        config.setDelimiterTags(new String[] {"\t"}); //berlin
//        config.setDelimiterTags(new String[] {","}); //wolfsburg
        config.setFileName(populationData);
        config.setCommentTags(new String[] { "#" });
        new TabularFileParser().parse(config, new TabularFileHandler() {
			
			@Override
			public void startRow(String[] row) {

				wrs.add(row[0], Double.parseDouble(row[2]));
			}
		});
        
		
	}

	private void run(int amount) {
	    
		for (int i = 0 ; i< amount; i++){
			Link link ;
		Point p = TaxiDemandWriter.getRandomPointInFeature(random, geometry.get(wrs.select()));
		link = NetworkUtils.getNearestLinkExactly(((Network) scenario.getNetwork()),ct.transform( MGC.point2Coord(p)));

			DvrpVehicleSpecification v = ImmutableDvrpVehicleSpecification.newBuilder()
					.id(Id.create(Id.create("rt" + i, DvrpVehicle.class), DvrpVehicle.class))
					.startLinkId(link.getId())
					.capacity(5)
					.serviceBeginTime((double)Math.round(1))
					.serviceEndTime((double)Math.round(30 * 3600))
					.build();
        vehicles.add(v);

		}
		new FleetWriter(vehicles.stream()).write(vehiclesFilePrefix + amount + ".xml.gz");
	}
	


	
}
