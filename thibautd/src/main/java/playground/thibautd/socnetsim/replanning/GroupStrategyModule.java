/* *********************************************************************** *
 * project: org.matsim.*
 * GroupStrategyModule.java
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
package playground.thibautd.socnetsim.replanning;

import org.matsim.api.core.v01.replanning.PlanStrategyModule;

import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;

/**
 * Generalizes {@link PlanStrategyModule} to group-level plans.
 * @author thibautd
 */
public interface GroupStrategyModule extends GenericStrategyModule<GroupPlans> {
}

