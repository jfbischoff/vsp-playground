/* *********************************************************************** *
 * project: org.matsim.*
 * PassengerRoutingModuleFactory.java
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
package playground.thibautd.jointtrips.router;

import org.matsim.api.core.v01.population.PopulationFactory;

import playground.thibautd.router.RoutingModule;
import playground.thibautd.router.RoutingModuleFactory;
import playground.thibautd.router.TripRouterFactory;

/**
 * @author thibautd
 */
public class PassengerRoutingModuleFactory implements RoutingModuleFactory {
	private final PopulationFactory popFactory;

	public PassengerRoutingModuleFactory(
			final PopulationFactory popFactory) {
		this.popFactory = popFactory;
	}


	@Override
	public RoutingModule createModule(
			final String mainMode,
			final TripRouterFactory factory) {
		return new PassengerRoutingModule(
			mainMode,
			popFactory,
			factory.getModeRouteFactory());
	}
}

