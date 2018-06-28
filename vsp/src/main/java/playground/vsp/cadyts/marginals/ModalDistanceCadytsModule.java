/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package playground.vsp.cadyts.marginals;

import org.matsim.core.controler.AbstractModule;
import playground.vsp.cadyts.marginals.prep.DistanceDistribution;

/**
 * Created by amit on 21.02.18.
 */

public class ModalDistanceCadytsModule extends AbstractModule{

    private final DistanceDistribution inputDistanceDistrbution;

    public ModalDistanceCadytsModule(DistanceDistribution inputDistanceDistrbution){
        this.inputDistanceDistrbution = inputDistanceDistrbution;
    }

    @Override
    public void install() {
        bind(DistanceDistribution.class).toInstance(inputDistanceDistrbution);

        bind(ModalDistanceCadytsContext.class).asEagerSingleton();
        addControlerListenerBinding().to(ModalDistanceCadytsContext.class);

        bind(EventsToBeelinDistanceRange.class).asEagerSingleton(); // this is not an event handler

        bind(BeelineDistanceCollector.class);
        bind(BeelineDistancePlansTranslatorBasedOnEvents.class).asEagerSingleton();

        addControlerListenerBinding().to(ModalDistanceDistributionControlerListener.class);
    }
}
