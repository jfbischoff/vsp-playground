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

package playground.jbischoff.taxi.berlin.supply;

import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.util.random.WeightedRandomSelection;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.jbischoff.taxi.berlin.demand.TaxiDemandWriter;
import playground.michalm.zone.Zone;

import com.vividsolutions.jts.geom.Point;


public class BerlinTaxiCreator
    implements VehicleCreator
{
    private static final Logger log = Logger.getLogger(BerlinTaxiCreator.class);
    private static final Random RND = new Random(42);
    private static final double PAXPERCAR = 4;

    private final CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(
            "EPSG:25833", TransformationFactory.DHDN_GK4);

    private final Scenario scenario;
    private final Map<Id<Zone>, Zone> zones;
    private final NetworkImpl network;
    private final WeightedRandomSelection<Id<Zone>> lorSelection;
    private final double evShare;

    private int currentVehicleId = 0;


    public BerlinTaxiCreator(Scenario scenario, Map<Id<Zone>, Zone> zones,
            WeightedRandomSelection<Id<Zone>> lorSelection, double evShare)
    {
        this.scenario = scenario;
        this.zones = zones;
        this.lorSelection = lorSelection;
        this.evShare = evShare;

        network = (NetworkImpl)scenario.getNetwork();
    }


    @Override
    public Vehicle createVehicle(double t0, double t1)
    {
        Id<Zone> lorId = lorSelection.select();
        String vehIdString = "t_" + lorId + "_" + (t0 / 3600) + "_" + currentVehicleId;
        if (RND.nextDouble() < evShare) {
            vehIdString = "e" + vehIdString;
        }
        Id<Vehicle> vehId = Id.create(vehIdString, Vehicle.class);

        Link link = getRandomLinkInLor(lorId);
        Vehicle v = new VehicleImpl(vehId, link, PAXPERCAR, Math.round(t0), Math.round(t1));
        currentVehicleId++;
        return v;
    }


    private Link getRandomLinkInLor(Id<Zone> lorId)
    {
//        log.info(lorId);
        Id<Zone> id = lorId;
        if (lorId.toString().length() == 7)
            id = Id.create("0" + lorId.toString(), Zone.class);
//        log.info(id);
        Point p = TaxiDemandWriter.getRandomPointInFeature(RND, this.zones.get(id)
                .getMultiPolygon());
        Coord coord = ct.transform(new CoordImpl(p.getX(), p.getY()));
        Link link = network.getNearestLinkExactly(coord);

        return link;
    }
}