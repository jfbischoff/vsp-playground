/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationQSimFactory.java
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

package playground.christoph.evacuation.mobsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.SynchronizedEventsManagerImpl;
import org.matsim.core.events.parallelEventsHandler.SimStepParallelEventsManagerImpl;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.ExperimentalBasicWithindayAgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.multimodalsimengine.MultiModalDepartureHandler;
import org.matsim.core.mobsim.qsim.multimodalsimengine.MultiModalSimEngine;
import org.matsim.core.mobsim.qsim.multimodalsimengine.MultiModalSimEngineFactory;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.MultiModalTravelTimeWrapperFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.DefaultQSimEngineFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.ParallelQNetsimEngineFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineFactory;

/**
 * @author cdobler
 */
public class EvacuationQSimFactory implements MobsimFactory {

    private final static Logger log = Logger.getLogger(EvacuationQSimFactory.class);
    
    private MultiModalTravelTimeWrapperFactory timeFactory;
    
    public EvacuationQSimFactory() {
    }
    
    public EvacuationQSimFactory(MultiModalTravelTimeWrapperFactory timeFactory) {
    	this.timeFactory = timeFactory;
    }
    
    @Override
    public Netsim createMobsim(Scenario sc, EventsManager eventsManager) {

        QSimConfigGroup conf = sc.getConfig().getQSimConfigGroup();
        if (conf == null) {
            throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
        }

        // Get number of parallel Threads
        int numOfThreads = conf.getNumberOfThreads();
        QNetsimEngineFactory netsimEngFactory;
        if (numOfThreads > 1) {
        	if (!(eventsManager instanceof SimStepParallelEventsManagerImpl)) {
        		eventsManager = new SynchronizedEventsManagerImpl(eventsManager);        		
        	}
            netsimEngFactory = new ParallelQNetsimEngineFactory();
            log.info("Using parallel QSim with " + numOfThreads + " threads.");
        } else {
            netsimEngFactory = new DefaultQSimEngineFactory();
        }
        QSim qSim = QSim.createQSimWithDefaultEngines(sc, eventsManager, netsimEngFactory);
        AgentFactory agentFactory = new ExperimentalBasicWithindayAgentFactory(qSim);
        AgentSource agentSource = new EvacuationPopulationAgentSource(sc, agentFactory, qSim);
        qSim.addAgentSource(agentSource);
        
        if (sc.getConfig().multiModal().isMultiModalSimulationEnabled()) {
        	if (timeFactory == null) timeFactory = new MultiModalTravelTimeWrapperFactory();
        	
        	// create MultiModalSimEngine
        	MultiModalSimEngine multiModalEngine = new MultiModalSimEngineFactory().createMultiModalSimEngine(qSim, timeFactory);
        	
        	// add MultiModalSimEngine to qSim
        	qSim.addMobsimEngine(multiModalEngine);
        	
        	// add MultiModalDepartureHandler
        	qSim.addDepartureHandler(new MultiModalDepartureHandler(qSim, multiModalEngine, sc.getConfig().multiModal()));        	
        }
        
        return qSim;

    }

}
