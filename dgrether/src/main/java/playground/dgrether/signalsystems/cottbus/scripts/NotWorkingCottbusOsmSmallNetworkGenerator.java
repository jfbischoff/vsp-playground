/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.dgrether.signalsystems.cottbus.scripts;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.core.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.DgPaths;
import playground.dgrether.signalsystems.utils.DgSignalizedLinks2Shape;
import playground.dgrether.utils.DgNet2Shape;


public class NotWorkingCottbusOsmSmallNetworkGenerator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Network network = NetworkUtils.createNetwork();
		OsmNetworkReader osmReader = new OsmNetworkReader(network,
				TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,TransformationFactory.WGS84_UTM33N),  false);
		osmReader.setKeepPaths(false);
		osmReader.setScaleMaxSpeed(true);
		String osmFile = DgPaths.REPOS + "shared-svn/studies/dgrether/cottbus/Cottbus-BA/brandenburg_tagged.osm";
		String output = DgPaths.REPOS +  "shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/network_small/network";

//		 set osmReader useHighwayDefaults false
//		 Autobahn
		osmReader.setHighwayDefaults(1, "motorway",      2, 120.0/3.6, 1.0, 2000, true);
		osmReader.setHighwayDefaults(1, "motorway_link", 1,  80.0/3.6, 1.0, 1500, true);
//		 Bundesstrasse?
		osmReader.setHighwayDefaults(1, "trunk",         1,  80.0/3.6, 1.0, 2000);
		osmReader.setHighwayDefaults(1, "trunk_link",    1,  50.0/3.6, 1.0, 1500);
//		 Durchgangsstrassen
		osmReader.setHighwayDefaults(1, "primary",       1,  80.0/3.6, 1.0, 1500);
		osmReader.setHighwayDefaults(1, "primary_link",  1,  60.0/3.6, 1.0, 1500);
//		 Hauptstrassen
		osmReader.setHighwayDefaults(1, "secondary",     1,  60.0/3.6, 1.0, 1000);
//		 mehr Hauptstrassen
		osmReader.setHighwayDefaults(1, "tertiary",      1,  45.0/3.6, 1.0,  600);
//	 Nebenstrassen
		osmReader.setHighwayDefaults(1, "minor",         1,  45.0/3.6, 1.0,  600);
//	 diverse
		osmReader.setHighwayDefaults(1, "unclassified",  1,  45.0/3.6, 1.0,  600);
//	 Wohngebiete
		osmReader.setHighwayDefaults(1, "residential",   1,  30.0/3.6, 1.0,  600);

		
		
		//cottbus innenstadt
		osmReader.setHierarchyLayer(51.820578,14.247866, 51.684789,14.507332, 1);

		osmReader.parse(osmFile);

//		new org.matsim.core.network.algorithms.NetworkCleaner().run(network);
		
		// Write network to file
		String networkFile = output + ".xml.gz";
		new NetworkWriter(network).write(networkFile);
		System.out.println("Done! Unprocessed MATSim Network saved as " + output + ".xml.gz");
		// Clean network
		String networkCleanedFile = output + "_cl.xml.gz";
//		new NetworkCleaner().run(new String[] {output + ".xml.gz", networkCleanedFile});
//		System.out.println("NetworkCleaner done! Network saved as " + output + "_cl.xml.gz");
//		
		//postprocess junctions in the network
		String networkWoJunctionsFile = output + "_cl_wo_junctions.xml.gz";
		String lanesFile = DgPaths.REPOS + "shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/lanes.xml";
		String lanesOutFile = output + "_cl_wo_junctions.xml";
//		new DgOsmJunctionsPostprocessing().postprocessJunctions(osmFile, networkCleanedFile, networkWoJunctionsFile, lanesFile, lanesOutFile);
		
		networkWoJunctionsFile = networkFile;
		//write output 
		Config c1 = ConfigUtils.createConfig();
		c1.network().setInputFile(networkWoJunctionsFile);
		String signalsSystems = DgPaths.REPOS +  "shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/signal_systems.xml";
		ConfigUtils.addOrGetModule(c1, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setUseSignalSystems(true);
		ConfigUtils.addOrGetModule(c1, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setSignalSystemFile(signalsSystems);
		Scenario scenario = ScenarioUtils.loadScenario(c1);
		
		CoordinateReferenceSystem crs = MGC.getCRS(TransformationFactory.WGS84_UTM33N);
		new DgNet2Shape().write(scenario.getNetwork(), output + ".shp", crs);

		String signalsShapeFile = DgPaths.REPOS + "shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/network_small/signalized_links.shp";
		new DgSignalizedLinks2Shape().getSignalizedLinksAndWrite2Shape(scenario, signalsShapeFile);
		

	}

}
