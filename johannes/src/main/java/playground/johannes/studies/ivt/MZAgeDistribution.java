/* *********************************************************************** *
 * project: org.matsim.*
 * MZAgeDistribution.java
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
package playground.johannes.studies.ivt;

import java.io.IOException;

import playground.johannes.sna.gis.CRSUtils;
import playground.johannes.sna.graph.analysis.GraphAnalyzer;
import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.GravityCostFunction;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.analysis.AgeAccessibilityTask;
import playground.johannes.socialnetworks.graph.social.analysis.AgeTask;
import playground.johannes.socialnetworks.graph.social.io.Population2SocialGraph;
import playground.johannes.socialnetworks.graph.spatial.analysis.Accessibility;

/**
 * @author illenberger
 *
 */
public class MZAgeDistribution {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Population2SocialGraph reader = new Population2SocialGraph();
		SocialGraph graph = reader.read("/Users/jillenberger/Work/socialnets/data/schweiz/complete/plans/plans.0.005.xml", CRSUtils.getCRS(21781));
		
		GraphAnalyzer.analyze(graph, new AgeAccessibilityTask(new Accessibility(new GravityCostFunction(1.2, 0, new CartesianDistanceCalculator()))), "/Users/jillenberger/Work/phd/doc/tex/ch3/fig/data/");
	}

}
