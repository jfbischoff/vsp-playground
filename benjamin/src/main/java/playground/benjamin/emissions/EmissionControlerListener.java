/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionControlerListener.java
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
package playground.benjamin.emissions;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.algorithms.EventWriterXML;

/**
 * @author benjamin
 *
 */
public class EmissionControlerListener implements StartupListener, IterationStartsListener, ShutdownListener {
	private static final Logger logger = Logger.getLogger(EmissionControlerListener.class);
	
	Controler controler;
	String emissionEventOutputFile;
	Integer lastIteration;
	EmissionHandler emissionHandler;

	public EmissionControlerListener() {
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		controler = event.getControler();
		lastIteration = controler.getLastIteration();
		logger.info("emissions will be calculated for iteration " + lastIteration);
		emissionHandler = new EmissionHandler();
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		Integer iteration = event.getIteration();
		
		if(lastIteration.equals(iteration)){
			computeEmissions(iteration);
		}
	}

	private void computeEmissions(Integer iteration) {
		logger.info("entering computeEmissions ...") ;
		
		EventsManager eventsManager = controler.getEvents();
		Scenario scenario = controler.getScenario() ;
		emissionEventOutputFile = controler.getControlerIO().getIterationFilename(iteration, "emission.events.xml.gz");
		
		emissionHandler.installEmissionEventHandler(scenario, eventsManager, emissionEventOutputFile);

		logger.info("leaving computeEmissions ...") ;
}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		EventWriterXML emissionEventWriter = emissionHandler.getEmissionEventWriter();
		emissionEventWriter.closeFile();
		logger.info("Vehicle-specific warm emission calculation was not possible in " + WarmEmissionAnalysisModule.getVehInfoWarnCnt() + " cases.");
		logger.info("Emission calculation terminated. Output can be found in " + emissionEventOutputFile);
	}
}
