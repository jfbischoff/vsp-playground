/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractModeHistogram
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
package air.analysis.categoryhistogram;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Class that helps to create histograms over time for certain categories.
 * @author dgrether
 *
 */
public final class CategoryHistogram {

	private Map<String, CategoryHistogramData> data = new TreeMap<String, CategoryHistogramData>();
	private int iteration = 0;
	private int binSizeSeconds;
	private Integer firstIndex;
	private Integer lastIndex;

	public CategoryHistogram(int binSizeSeconds) {
		this.binSizeSeconds = binSizeSeconds;
		this.reset(0);
	}

	public void reset(final int iteration) {
		this.iteration = iteration;
		this.getCategoryData().clear();
		this.firstIndex = null;
		this.lastIndex = null;
	}
	
	public void increase(double time_seconds, int count, String category){
		int index = getBinIndex(time_seconds);
		CategoryHistogramData categoryData = getDataForCategory(category);
		CategoryHistogramUtils.add2MapEntry(categoryData.departuresByBin, index, count);
	}
	
	int getDepartures(String category, int index){
		return CategoryHistogramUtils.getNotNullInteger(this.getCategoryData().get(category).departuresByBin,
				index);
	}

	int getArrivals(String category, int index){
		return CategoryHistogramUtils.getNotNullInteger(this.getCategoryData().get(category).arrivalsByBin,
				index);
	}
	
	int getAbort(String category, int index){
		return CategoryHistogramUtils.getNotNullInteger(this.getCategoryData().get(category).abortByBin,
				index);
	}

	public void decrease(double time_seconds, int count, String category){
		int index = this.getBinIndex(time_seconds);
		CategoryHistogramData categoryData = getDataForCategory(category);
		CategoryHistogramUtils.add2MapEntry(categoryData.arrivalsByBin, index, count);
	}
	
	public void abort(double time_seconds, int count, String category){
		int index = this.getBinIndex(time_seconds);
		CategoryHistogramData categoryData = getDataForCategory(category);
		CategoryHistogramUtils.add2MapEntry(categoryData.abortByBin, count, index);
	}
	
	
	/**
	 * @return Set of all transportation categorys data is available for
	 */
	public Set<String> getLegModes() {
		return this.getCategoryData().keySet();
	}



	private int getBinIndex(final double time) {
		int bin = (int)(time / this.getBinSizeSeconds());
		if (this.getFirstIndex() == null){
			this.firstIndex = bin;
		}
		this.lastIndex = bin;
		return bin;
	}

	private CategoryHistogramData getDataForCategory(final String legMode) {
		CategoryHistogramData categoryData = this.data.get(legMode);
		if (categoryData == null) {
			categoryData = new CategoryHistogramData(); // +1 for all times out of our range
			this.getCategoryData().put(legMode, categoryData);
		}
		return categoryData;
	}

	Map<String, CategoryHistogramData> getCategoryData() {
		return data;
	}

	int getIteration() {
		return iteration;
	}

	int getBinSizeSeconds() {
		return binSizeSeconds;
	}

	Integer getFirstIndex() {
		return firstIndex;
	}


	Integer getLastIndex() {
		return lastIndex;
	}


}