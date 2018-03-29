/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.michalm.taxi.ev;

import org.matsim.vsp.ev.charging.ChargingListener;
import org.matsim.vsp.ev.data.ElectricVehicle;

import playground.michalm.taxi.data.EvrpVehicle.Ev;

/**
 * @author michalm
 */
public class ETaxiChargingListener implements ChargingListener {
	@Override
	public void notifyVehicleQueued(ElectricVehicle ev, double now) {
		((Ev)ev).getAtChargerActivity().vehicleQueued(now);
	}

	@Override
	public void notifyChargingStarted(ElectricVehicle ev, double now) {
		((Ev)ev).getAtChargerActivity().chargingStarted(now);
	}

	@Override
	public void notifyChargingEnded(ElectricVehicle ev, double now) {
		((Ev)ev).getAtChargerActivity().chargingEnded(now);
	}
}
