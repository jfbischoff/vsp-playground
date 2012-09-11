/* *********************************************************************** *
 * project: org.matsim.*
 * WalkSpeedComparator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.withinday.replanning.utils;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.internal.MatsimComparator;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.util.TravelTime;

import playground.christoph.evacuation.core.utils.geometry.Coord3dImpl;
import playground.christoph.evacuation.trafficmonitoring.WalkTravelTimeFactory;

/*
 * Compares walk speeds of people respecting their age, gender, etc.
 */
public class WalkSpeedComparator implements Comparator<Id>, Serializable, MatsimComparator {

	private static final long serialVersionUID = 1L;
	
	private final TravelTime travelTime;
	private final Link link; 
	private final Map<Id, Double> travelTimesMap;
	
	public WalkSpeedComparator() {
		travelTime = new WalkTravelTimeFactory(new PlansCalcRouteConfigGroup()).createTravelTime();
		
		NetworkFactory factory = new NetworkFactoryImpl(NetworkImpl.createNetwork());
		Node startNode = factory.createNode(new IdImpl("startNode"), new Coord3dImpl(0, 0, 0));
		Node endNode = factory.createNode(new IdImpl("endNode"), new Coord3dImpl(1, 0, 0));
		link = factory.createLink(new IdImpl("link"), startNode, endNode);
		link.setLength(1.0);
		
		travelTimesMap = new HashMap<Id, Double>();
	}
	
	public void calcTravelTimes(Population population) {
		travelTimesMap.clear();
		
		for (Person person : population.getPersons().values()) {
			double tt = travelTime.getLinkTravelTime(link, 0.0, person, null);
			travelTimesMap.put(person.getId(), tt);
		}
	}
	
	@Override
	public int compare(Id id1, Id id2) {
		double tt1 = travelTimesMap.get(id1);
		double tt2 = travelTimesMap.get(id2);
		
		/*
		 * Invert the return value since people with long travel times should be
		 * at the front end of the queue.
		 */
		return -Double.compare(tt1, tt2);
	}
	
}
