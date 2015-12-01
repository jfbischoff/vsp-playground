/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop.analysis;

import gnu.trove.map.hash.TDoubleDoubleHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.common.stats.StatsWriter;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Person;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * @author johannes
 *
 */
public class ActivityChainTask extends AnalyzerTask {

	public static final String KEY = "n.act";
	
	@Override
	public void analyze(Collection<? extends Person> persons, Map<String, DescriptiveStatistics> results) {
		TObjectDoubleHashMap<String> chains = new TObjectDoubleHashMap<String>();
		
		TObjectIntHashMap<String> typeCount = new TObjectIntHashMap<String>();
		
		TDoubleDoubleHashMap tripCounts = new TDoubleDoubleHashMap();
		
		for(Person person : persons) {
			Episode plan = person.getEpisodes().get(0);
			
			StringBuilder builder = new StringBuilder();
			for(int i = 0; i < plan.getActivities().size(); i++) {
				String type = (String) plan.getActivities().get(i).getAttribute(CommonKeys.ACTIVITY_TYPE);
				
				typeCount.adjustOrPutValue(type, 1, 1);
				
				builder.append(type);
				builder.append("-");
			}
			
			String chain = builder.toString();
			chains.adjustOrPutValue(chain, 1, 1);
			
			tripCounts.adjustOrPutValue(plan.getLegs().size(), 1, 1);
		}
		
		for(Object key : typeCount.keys()) {
			DescriptiveStatistics stats = new DescriptiveStatistics();
			stats.addValue(typeCount.get((String) key));
			results.put(String.format("%s.%s", KEY, key), stats);
		}

		if (outputDirectoryNotNull()) {
			try {
				StatsWriter.writeLabeledHistogram(chains, "chain", "n", getOutputDirectory() + "/actchains.txt", true);
				
				StatsWriter.writeHistogram(tripCounts, "nTrips", "n", getOutputDirectory() + "/tripcounts.txt", true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
