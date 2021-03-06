/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.jbischoff.wobscenario.cemdap;

import java.util.Map;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.opengis.feature.simple.SimpleFeature;

import playground.vsp.corineLandcover.CorineLandCoverData;
import playground.vsp.corineLandcover.LandCoverUtils;
import playground.vsp.openberlinscenario.cemdap.output.ActivityTypes;
import playground.vsp.openberlinscenario.cemdap.output.Cemdap2MatsimUtils;
import playground.vsp.openberlinscenario.cemdap.output.CemdapStopsParser;

/**
 * @author dziemke
 */
public class WobFeature2Coord {
	private final static Logger LOG = Logger.getLogger(WobFeature2Coord.class);
	private int to = 0;
	public WobFeature2Coord() {
	}

	public final void assignCoords(Population population, int planNumber, ObjectAttributes personZoneAttributes, Map<String, SimpleFeature> zones,
			Map<Id<Person>, Coord> homeZones, boolean allowVariousWorkAndEducationLocations,  CorineLandCoverData corineLandCoverData ) {
		int counter = 0;
		LOG.info("Start assigning (non-home) coordinates. Plan number is " + planNumber +".");
		for (Person person : population.getPersons().values()) {
			counter++;
			if (counter % 10000 == 0) {
				LOG.info(counter + " persons assigned with (non-home) coordinates so far.");
				Gbl.printMemoryUsage();
			}
			
			int activityIndex = 0;
			Coord workCoord = null;
			Coord zoneCoord = null;
			Coord educCoord = null;

			for (PlanElement planElement : person.getPlans().get(planNumber).getPlanElements()) {
				if (planElement instanceof Activity) {
					Activity activity = (Activity) planElement;
					String zoneId = (String) personZoneAttributes.getAttribute(person.getId().toString(), CemdapStopsParser.ZONE + activityIndex);
					if (zoneId == null) {
						LOG.error("Person with ID " + person.getId() + ": Object attribute '" + CemdapStopsParser.ZONE + activityIndex + "' not found.");
					}
					SimpleFeature zone = zones.get(zoneId);
					if (zone == null) {
						if (zoneId.equals("9999990")) zoneCoord = new Coord(618108.4459077471,5811466.460424992); 
						if (zoneId.equals("9999991")) zoneCoord = new Coord(618450.1692363387,5810991.123045962); 
						if (zoneId.equals("9999992")) zoneCoord = new Coord(620470.8428419383,5810124.78878703); 
						if (zoneId.equals("9999993")) zoneCoord = new Coord(619775.265360059,5810238.353786824); 
						if (zoneId.equals("9999994")) zoneCoord = new Coord(622221.8065909432,5810804.264158037); 
						if (zoneId.equals("9999995")) zoneCoord = new Coord(621452.6153294491,5811715.06310022); 
						if (zoneId.equals("9999996")) zoneCoord = new Coord(621119.738008176, 5812300.314582227); 
						if (zoneCoord == null) {
							throw new RuntimeException("Zone "+zoneId + " not found.");
						}
					}
					if (allowVariousWorkAndEducationLocations) {
						if (activity.getType().equals(ActivityTypes.HOME)) {
							((Activity)activity).setCoord(homeZones.get(person.getId()));
						} else {
							if (zoneCoord != null){
								((Activity)activity).setCoord(zoneCoord);

							} else {
							Coord coord = getCoord(corineLandCoverData, zone, "other");
							((Activity)activity).setCoord(coord);
							}
						}
					} else {
						if (activity.getType().equals(ActivityTypes.HOME)) {
							((Activity)activity).setCoord(homeZones.get(person.getId()));
						} else if (activity.getType().equals(ActivityTypes.WORK)) {
							if (zoneCoord != null){
								workCoord = zoneCoord;

							} else {
							if (workCoord == null) {
								workCoord = getCoord(corineLandCoverData, zone, "other");
							}
							((Activity)activity).setCoord(workCoord);
							}
							} else if (activity.getType().equals(ActivityTypes.EDUCATION)) {
								if (zoneCoord != null){
									educCoord = zoneCoord;

								}
							if (educCoord == null) {
								educCoord = getCoord(corineLandCoverData, zone, "other");
							}
							((Activity)activity).setCoord(educCoord);
						} else {
							Coord coord;
							if (zoneCoord != null) {coord = zoneCoord;
							} else{
							coord = getCoord(corineLandCoverData, zone, "other");
							}
							((Activity)activity).setCoord(coord);
						}
					}
					activityIndex++;
				}
			}
		}
		LOG.info("Finished assigning non-home coordinates.");
	}

	private Coord getCoord (CorineLandCoverData corineLandCoverData, SimpleFeature feature, String activityType) {
		Coord coord ;
		if (corineLandCoverData==null) {
			coord = Cemdap2MatsimUtils.getRandomCoordinate(feature);
		} else {
			coord = corineLandCoverData.getRandomCoord(feature,activityType.equals("home")? LandCoverUtils.LandCoverActivityType.home : LandCoverUtils.LandCoverActivityType.other );
			
			

		}
		return coord;
	}

	
	public final void assignHomeCoords(Population population, ObjectAttributes personZoneAttributes, Map<String, SimpleFeature> zones, Map<Id<Person>, Coord> homeZones, CorineLandCoverData corineLandCoverData) {
		int counter = 0;
		LOG.info("Start assigning home coordinates.");
		for (Person person : population.getPersons().values()) {
			counter++;
			if (counter % 20000 == 0) {
				LOG.info(counter + " persons assigned with home coordinates so far.");
				Gbl.printMemoryUsage();
			}
					
			int activityIndex = 0;

			for (PlanElement planElement : person.getPlans().get(0).getPlanElements()) {
				if (planElement instanceof Activity) {
					Activity activity = (Activity) planElement;
					String zoneId = (String) personZoneAttributes.getAttribute(person.getId().toString(), CemdapStopsParser.ZONE + activityIndex);
					Id<Person> personId = person.getId();
					if (zoneId == null) {
						
						
						LOG.error("Person with ID " + person.getId() + ": Object attribute '" + CemdapStopsParser.ZONE + activityIndex + "' not found.");
					}
					SimpleFeature zone = zones.get(zoneId);
					Coord homeCoord = null;
					if (zone == null) {
						if (zoneId.equals("9999990")) homeCoord = new Coord(618108.4459077471,5811466.460424992); 
						if (zoneId.equals("9999991")) homeCoord = new Coord(618450.1692363387,5810991.123045962); 
						if (zoneId.equals("9999992")) homeCoord = new Coord(620470.8428419383,5810124.78878703); 
						if (zoneId.equals("9999993")) homeCoord = new Coord(619775.265360059,5810238.353786824); 
						if (zoneId.equals("9999994")) homeCoord = new Coord(622221.8065909432,5810804.264158037); 
						if (zoneId.equals("9999995")) homeCoord = new Coord(621452.6153294491,5811715.06310022); 
						if (zoneId.equals("9999996")) homeCoord = new Coord(621119.738008176, 5812300.314582227); 
//						LOG.error("Number of people living at VW: " + vwcount);
						if (homeCoord == null) {
						throw new RuntimeException("Zone "+zoneId + " not found.");}
						
					}
					if (activity.getType().equals(ActivityTypes.HOME)) {
						if (homeCoord==null){
						homeCoord = getCoord(corineLandCoverData, zone, "home");}
						}
						homeZones.put(personId, homeCoord);
					activityIndex++;
				}
			}
		}
		LOG.info("Finished assigning home coordinates.");
	}
}