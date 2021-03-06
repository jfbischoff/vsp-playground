/* *********************************************************************** *
 * project: org.matsim.*
 * RunEmissionToolOffline.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.jbischoff.taxi.emissions;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetReader;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.fleet.FleetSpecificationImpl;
import org.matsim.contrib.emissions.HbefaVehicleAttributes;
import org.matsim.contrib.emissions.HbefaVehicleCategory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

/**
 * @author jbischoff
 * Creates a emissions vehicle file for a taxi vehicles file
 */
public class TaxiToEmissionVehicleConverter {
	public static void main(String[] args) {
		
		String dir = "C:/Users/Joschka/Documents/shared-svn/projects/sustainability-w-michal-and-dlr/data/";
        Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(dir+"scenarios/2014_10_basic_scenario_v4/berlin_brb.xml");
		FleetSpecificationImpl vrpData = new FleetSpecificationImpl();
		new FleetReader(vrpData).readFile(dir + "/scenarios/2014_10_basic_scenario_v4/taxis4to4_EV0.0.xml");
        new TaxiToEmissionVehicleConverter().convert(vrpData,dir+"/scenarios/2014_10_basic_scenario_v4/+emissionVehicles.xml"); 

}

	public void convert(FleetSpecification vrpData, String emissionVehicleFile) {
		Vehicles outputVehicles = VehicleUtils.createVehiclesContainer();

		HbefaVehicleCategory vehicleCategory;
		HbefaVehicleAttributes vehicleAttributes;
		//We are using passenger car fleet average for Germany. May or may not true for taxis, but considering HBEFA has neither hybrids nor natural gas vehicles, 
		//it might be the way to go 
		for (DvrpVehicleSpecification vrpVeh : vrpData.getVehicleSpecifications().values()) {

			Id<DvrpVehicle> personId = vrpVeh.getId();
		
			vehicleCategory = HbefaVehicleCategory.PASSENGER_CAR;
			
			vehicleAttributes = new HbefaVehicleAttributes();

			Id<VehicleType> vehTypeId = Id.create(vehicleCategory + ";" + 
					vehicleAttributes.getHbefaTechnology() + ";" + 
					vehicleAttributes.getHbefaSizeClass() + ";" + 
					vehicleAttributes.getHbefaEmConcept(),VehicleType.class);
			VehicleType vehicleType = VehicleUtils.getFactory().createVehicleType(vehTypeId);

			// either set following or use switch in EmissionConfigGroup to use vehicle id for vehicle description. Amit sep 2016
			vehicleType.setDescription(vehTypeId.toString());

			if(!(outputVehicles.getVehicleTypes().containsKey(vehTypeId))){//getVehicles().containsKey(vehTypeId))){
				outputVehicles.addVehicleType(vehicleType);
			} else {
				// do nothing
			}

			Vehicle vehicle = VehicleUtils.getFactory().createVehicle(Id.create(personId, Vehicle.class), vehicleType);
			outputVehicles.addVehicle(vehicle);//getVehicles().put(vehicle.getId(), vehicle);
		}

		VehicleWriterV1 vehicleWriter = new VehicleWriterV1(outputVehicles);
		vehicleWriter.writeFile(emissionVehicleFile);
		System.out.println("Writing emission Vehicles files is finished.");
		
	}
	
}
