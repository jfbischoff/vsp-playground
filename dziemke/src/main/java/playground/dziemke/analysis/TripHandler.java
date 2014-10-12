package playground.dziemke.analysis;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

public class TripHandler implements ActivityEndEventHandler, ActivityStartEventHandler, LinkLeaveEventHandler {
	private Map<Id<Trip>, Trip> trips = new HashMap<>();
	
	private Map<Id, Integer> activityEndCount = new HashMap <Id, Integer>();
	private Map<Id, Integer> activityStartCount = new HashMap <Id, Integer>();
	
	int noPreviousEndOfActivityCounter = 0;
	
	
	@Override
	public void handleEvent(ActivityEndEvent event) {
		// store information from event to variables and print the information on console
		//String eventType = event.getEventType();
		Id<Link> linkId = event.getLinkId();
		//String linkShortened = linkId.toString().substring(0, 10) + "...";
		Id<Person> personId = event.getPersonId();
		double time = event.getTime();
		String actType = event.getActType();
		//Id facilityId =	event.getFacilityId();
		//System.out.println("Type: " + eventType + " - LinkId: " + linkShortened + " - PersonId: " + personId.toString()
		//		+ " - Time: " + time/60/60 + " - ActType: " + actType + " - FacilityId: " + facilityId);
		
		
		// count number of activity ends for every agent and store these numbers in a map
		if (!activityEndCount.containsKey(personId)) {
			activityEndCount.put(personId, 1);
		} else {
			int numberOfCompletedDepartures = activityEndCount.get(personId);
			activityEndCount.put(personId, numberOfCompletedDepartures + 1);
		}
		//System.out.println("Agent " + personId + " has " + activityEndCount.get(personId) + " activity ends.");
		
		
		// create an instance of the object "Trip"
		//Trip trip = new Trip(personId);
		Trip trip = new Trip();
		Id<Trip> tripId = Id.create(personId + "_" + activityEndCount.get(personId), Trip.class);
		trip.setTripId(tripId);
		trip.setPersonId(personId);
		trip.setDepartureLinkId(linkId);
		trip.setDepartureTime(time);
		//trip.setDepartureLegMode(legMode);
		trip.setActivityEndActType(actType);
		trips.put(tripId, trip);
		
		
		// check if activity end link is the same as previous activity start link
		if (activityEndCount.get(personId) >= 2) {
			int numberOfLastArrival = activityStartCount.get(personId);
			Id<Trip> lastTripId = Id.create(personId + "_" + numberOfLastArrival, Trip.class);
			if (!trips.get(tripId).getDepartureLinkId().equals(trips.get(lastTripId).getArrivalLinkId())) {
				System.err.println("Activity end link differs from previous activity start link.");
			} 
		}
		
		
		// check if type of ending activity is the same as type of previously started activity
		if (activityEndCount.get(personId) >= 2) {
			int numberOfLastArrival = activityStartCount.get(personId);
			Id<Trip> lastTripId = Id.create(personId + "_" + numberOfLastArrival, Trip.class);
			if (!trips.get(tripId).getActivityEndActType().equals(trips.get(lastTripId).getActivityStartActType())) {
				System.err.println("Type of ending activity is not the same as type of previously started activity.");
			} 
		}
	}

	
	@Override
	public void handleEvent(ActivityStartEvent event) {
		// store information from event to variables and print the information on console
		//String eventType = event.getEventType();
		Id<Link> linkId = event.getLinkId();
		//String linkShortened = linkId.toString().substring(0, 10) + "...";
		Id<Person> personId = event.getPersonId();
		double time = event.getTime();
		String actType = event.getActType();
		//Id facilityId =	event.getFacilityId();
		//System.out.println("Type: " + eventType + " - LinkId: " + linkShortened + " - PersonId: " + personId.toString()
		//		+ " - Time: " + time/60/60 + " - ActType: " + actType + " - FacilityId: " + facilityId);
		
		
		// count number of activity starts for every agent and store these numbers in a map
		if (!activityStartCount.containsKey(personId)) {
			activityStartCount.put(personId, 1);
		} else {
			int numberOfCompletedDepartures = activityStartCount.get(personId);
			activityStartCount.put(personId, numberOfCompletedDepartures + 1);
		}
		//System.out.println("Agent " + personId + " has " + activityEndCount.get(personId) + " activity ends and " + activityStartCount.get(personId) + " activity starts.");
		
		
		// add information to the object "Trip"
		Id<Trip> tripId = Id.create(personId + "_" + activityStartCount.get(personId), Trip.class);
		if (trips.get(tripId) != null) {
			trips.get(tripId).setArrivalLinkId(linkId);
			trips.get(tripId).setArrivalTime(time);
			//trips.get(tripId).setArrivalLegMode(legMode);
			trips.get(tripId).setActivityStartActType(actType);
			trips.get(tripId).setTripComplete(true);
		} else {
			this.noPreviousEndOfActivityCounter++;
		}
		
		
		// check if number of activity ends and number of activity starts are the same
		if(activityStartCount.get(personId) != activityEndCount.get(personId)) {
			System.err.println("Activity start count differs from activity end count.");
		}
		
		
		// checking leg modes is not applicable here
	}
	
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		// store information from event to variables
		//String eventType = event.getEventType();
		Id<Link> linkId = event.getLinkId();
		//String linkShortened = linkId.toString().substring(0, 10) + "...";
		Id<Person> personId = event.getPersonId();
		//double time = event.getTime();
		//Id vehicleId = event.getVehicleId();
		
		
		// add information to the object "Trip"
		Id<Trip> tripId = Id.create(personId + "_" + activityEndCount.get(personId), Trip.class);
		//if (trips2.get(tripId) != null) {
		if (trips.get(tripId).getLinks().isEmpty()) {
			if (trips.get(tripId).getDepartureLinkId().equals(linkId)) {
				trips.get(tripId).getLinks().add(linkId);
				//System.out.println("Added first link to trip " + tripId);
			} else {
				System.err.println("First route link different from departure link!");
			}
		} else {
			trips.get(tripId).getLinks().add(linkId);
			//System.out.println("Added another link to trip " + tripId);
			//System.out.println("List of trip " + tripId + " has now " + trips2.get(tripId).getLinks().size() + " elements");
		}
	}
	
	
	@Override
	public void reset(int iteration) {
	}
	
	
	public Map<Id<Trip>, Trip> getTrips() {
		return this.trips;
	}
	
	
	public int getNoPreviousEndOfActivityCounter() {
		return this.noPreviousEndOfActivityCounter;
	}
}
