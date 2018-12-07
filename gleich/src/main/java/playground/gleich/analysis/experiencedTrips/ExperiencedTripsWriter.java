/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.gleich.analysis.experiencedTrips;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class ExperiencedTripsWriter {
	private Map<Id<Person>, List<ExperiencedTrip>> agent2trips;
	private Set<String> monitoredModes;
	// first level separator
	private String sep = ";";
	// second level separator
	private String sep2 = ",";
	
	private Counter counterTrips = new Counter("[" + this.getClass().getSimpleName() + "] wrote ExperiencedTrip # ", "", 10);
	private Counter counterLegs = new Counter("[" + this.getClass().getSimpleName() + "] wrote ExperiencedLeg # ", "", 10);
	
	public ExperiencedTripsWriter(Map<Id<Person>, List<ExperiencedTrip>> agent2trips, 
			Set<String> monitoredModes){
		this.agent2trips = agent2trips;
		this.monitoredModes = monitoredModes;
	}
	
	private BufferedWriter initialize(String file) throws IOException {
		BufferedWriter bw = IOUtils.getBufferedWriter(file);
		// write header
		bw.write("tripId" + sep + "agent" + sep + "tripNumber" + sep + "activityBefore" + sep +
				"activityAfter" + sep + "fromLinkId" + sep + "toLinkId" + sep +
				"startTime" + sep + "endTime" + sep + "totalTravelTime" + sep + 
				"numberOfLegs" + sep + "transitStopsVisited");
		for(String mode: monitoredModes){
			bw.write(sep + mode + ".InVehicleTime");
			bw.write(sep + mode + ".Distance");
			bw.write(sep + mode + ".WaitTime");
			bw.write(sep + mode + ".maxPerLegWaitTime");
			bw.write(sep + mode + ".NumberOfLegs");
		}
		bw.write(sep + "Other" + ".InVehicleTime");
		bw.write(sep + "Other" + ".Distance");
		bw.write(sep + "Other" + ".WaitTime");
		bw.write(sep + "Other" + ".maxPerLegWaitTime");
		bw.write(sep + "Other" + ".NumberOfLegs");
		
		return bw;
	}
	
	public void writeExperiencedTrips(String file) {
		try {
			BufferedWriter bw = initialize(file);
			
			bw.newLine();
			for(List<ExperiencedTrip> tripList: agent2trips.values()){
				for(ExperiencedTrip trip: tripList){
					writeExperiencedTrip(bw, trip);
					bw.newLine();
					counterTrips.incCounter();
				}
			}
			bw.close();
			counterTrips.printCounter();
			counterTrips.reset();
		} catch (IOException e1) {
			e1.printStackTrace();
			throw new RuntimeException("could not write");
		}
	}
	
	public void writeExperiencedLegs(String file) {
		try {
			BufferedWriter bw = initialize(file);
			
			// add header for leg
			bw.write(sep + "legNr" + sep + "legFromLinkId" + sep + "legToLinkId" + sep +
					"legStartTime" + sep + "legEndTime" + sep +
					"legMode" + sep + "legWaitTime" + sep + "legGrossWaitTime" + sep +
					"legInVehicleTime" + sep + "legDistance" + sep + "legTransitRouteId" + sep +
					"legPtFromStop" + sep + "legPtToStop");
			bw.newLine();
			for(List<ExperiencedTrip> tripList: agent2trips.values()){
				for(ExperiencedTrip trip: tripList){
					for(int i = 0; i < trip.getLegs().size(); i++) {
						ExperiencedLeg leg = trip.getLegs().get(i);
						writeExperiencedTrip(bw, trip);
						writeExperiencedLeg(bw, leg, i);
						bw.newLine();
						counterLegs.incCounter();
					}
				}
			}
			bw.close();
			counterLegs.printCounter();
			counterLegs.reset();
		} catch (IOException e1) {
			e1.printStackTrace();
			throw new RuntimeException("could not write");
		}
	}

	private void writeExperiencedTrip(BufferedWriter bw, ExperiencedTrip trip) {
		try {
			bw.write(trip.getId() + sep + trip.getAgent() + sep + trip.getTripNumber() + sep +
					trip.getActivityBefore() + sep + trip.getActivityAfter() + sep + 
					trip.getFromLinkId() + sep + trip.getToLinkId() + sep + 
					convertSecondsToTimeString(trip.getStartTime()) + sep + 
					convertSecondsToTimeString(trip.getEndTime()) + sep + 
					trip.getTotalTravelTime() + sep + trip.getLegs().size());
			if (trip.getTransitStopsVisited().size() < 1) {
				bw.write(sep + "no pt");
			} else {
				Iterator<Id<TransitStopFacility>> stopIterator = trip.getTransitStopsVisited().iterator();
				bw.write(sep + stopIterator.next());
				while (stopIterator.hasNext()) {
					bw.write(sep2 + stopIterator.next());
				}
			}
			for (String mode: monitoredModes){
				try{
					bw.write(sep + trip.getMode2inVehicleOrMoveTime().get(mode) + sep + 
							trip.getMode2inVehicleOrMoveDistance().get(mode) + sep +
							trip.getMode2waitTime().get(mode) + sep + 
							trip.getMode2maxPerLegWaitTime().get(mode) + sep +
							trip.getMode2numberOfLegs().get(mode));
				} catch (NullPointerException e){
					e.printStackTrace();
					throw new RuntimeException("monitored mode " + mode +
							" not found in ExperiencedTrip " + trip.getId());
				}
			}
			bw.write(sep + trip.getMode2inVehicleOrMoveTime().get("Other") + sep + 
					trip.getMode2inVehicleOrMoveDistance().get("Other") + sep +
					trip.getMode2waitTime().get("Other") + sep +
					trip.getMode2maxPerLegWaitTime().get("Other") + sep +
					trip.getMode2numberOfLegs().get("Other"));
			
		} catch (IOException e1) {
			e1.printStackTrace();
			throw new RuntimeException("could not write");
		}
	}
	
	private void writeExperiencedLeg(BufferedWriter bw, ExperiencedLeg leg, int legNr) {
		try {
			bw.write(sep + Integer.toString(legNr+1) + sep + leg.getFromLinkId() + sep + leg.getToLinkId() + sep + 
					convertSecondsToTimeString(leg.getStartTime()) + sep + 
					convertSecondsToTimeString(leg.getEndTime()) + sep + 
					leg.getMode() + sep + leg.getWaitTime() + sep + leg.getGrossWaitTime() + sep +
					leg.getInVehicleTime() + sep + leg.getDistance() + sep + String.valueOf(leg.getTransitRouteId()));
			if (leg.getMode().equals(TransportMode.pt)) {
				bw.write( sep + String.valueOf(leg.getPtFromStop()) + sep + String.valueOf(leg.getPtToStop()));
			} else {
				bw.write( sep + "no pt" + sep + "no pt");
			}
		} catch (IOException e1) {
			e1.printStackTrace();
			throw new RuntimeException("could not write");
		}
	}
	
	public static String convertSecondsToTimeString(double seconds) {
		return Time.writeTime(seconds);

	}
}
