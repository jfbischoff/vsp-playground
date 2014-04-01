/* *********************************************************************** *
 * project: org.matsim.*
 * StrategyAnalysisConfigGroup.java
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
package playground.thibautd.socnetsim.run;

import org.matsim.core.config.experimental.ReflectiveModule;

/**
 * @author thibautd
 */
public class StrategyAnalysisConfigGroup extends ReflectiveModule {

	public static final String GROUP_NAME = "strategyanalysis";

	private boolean dumpGroupSizes = false;

	public StrategyAnalysisConfigGroup() {
		super( GROUP_NAME );
	}

	@StringGetter( "dumpGroupSizes" )
	public boolean isDumpGroupSizes() {
		return this.dumpGroupSizes;
	}

	@StringSetter( "dumpGroupSizes" )
	public void setDumpGroupSizes( final boolean dumpGroupSizes ) {
		this.dumpGroupSizes = dumpGroupSizes;
	}
}

