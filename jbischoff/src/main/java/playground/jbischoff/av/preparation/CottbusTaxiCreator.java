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

package playground.jbischoff.av.preparation;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetWriter;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

/**
 * @author  jbischoff
 *
 */
public class CottbusTaxiCreator {

	public static void main(String[] args) {
		List<DvrpVehicleSpecification> taxis = new ArrayList<>();
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile("C:/Users/Joschka/Desktop/av4cottbus/cb02/output_network.xml.gz");
		for (int i = 0; i<1000 ;i++){

			DvrpVehicleSpecification v = ImmutableDvrpVehicleSpecification.newBuilder()
					.id(Id.create(Id.create("taxi_" + i, DvrpVehicle.class), DvrpVehicle.class))
					.startLinkId(network.getLinks().get(Id.createLinkId(10617)).getId())
					.capacity(4)
					.serviceBeginTime(0.0)
					.serviceEndTime((double)(25 * 3600))
					.build();
			taxis.add(v);
		}
		new FleetWriter(taxis.stream()).write("C:/Users/Joschka/Desktop/av4cottbus/taxis.xml");
		
	}

}
