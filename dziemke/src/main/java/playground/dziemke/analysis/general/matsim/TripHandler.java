package playground.dziemke.analysis.general.matsim;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.events.handler.EventHandler;
import playground.dziemke.analysis.general.Trip;

/**
 * @author on 04.04.2017.
 */
public class TripHandler implements ActivityEndEventHandler, ActivityStartEventHandler, LinkLeaveEventHandler,
        PersonArrivalEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler,
        PersonStuckEventHandler, EventHandler {
    public static final Logger log = Logger.getLogger(TripHandler.class);
    private static boolean tripmodeWarn = true;

    private Map<Id<Trip>, MatsimTrip> trips = new HashMap<>();

    private Map<Id<Person>, Integer> activityEndCount = new HashMap <>();
    private Map<Id<Person>, Integer> activityStartCount = new HashMap <>();

    private int noPreviousEndOfActivityCounter = 0;
    private int personStuckCounter = 0;

    private Vehicle2DriverEventHandler vehicle2driver = new Vehicle2DriverEventHandler();

    private static final String PT_INTERACTION = "pt interaction";

    private boolean aggregateActivityByMainType = false;

    public void setAggregateActivityByMainType(boolean aggregateActivityByMainType) {
        this.aggregateActivityByMainType = aggregateActivityByMainType;
    }

    private String aggregateActivityByMainType(String activity) {
        return activity.split("_")[0];
    }

    @Override
    public void handleEvent(ActivityEndEvent event) {
        //if its pt interaction, skip it
        if (event.getActType().equals(PT_INTERACTION)) return;

        // store information from event to variables and print the information on console
        //String eventType = event.getEventType();
        Id<Link> linkId = event.getLinkId();
        //String linkShortened = linkId.toString().substring(0, 10) + "...";
        Id<Person> personId = event.getPersonId();
        double time_s = event.getTime();
        String actType = event.getActType();
        if (aggregateActivityByMainType) actType = aggregateActivityByMainType(actType);
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
        MatsimTrip trip = new MatsimTrip();
        Id<Trip> tripId = Id.create(personId + "_" + activityEndCount.get(personId), Trip.class);
        trip.setTripId(tripId);
        trip.setPersonId(personId);
        trip.setDepartureLinkId(linkId);
        trip.setDepartureTime_s(time_s);
        //trip.setDepartureLegMode(legMode);
        trip.setActivityTypeBeforeTrip(actType);
        trips.put(tripId, trip);


        // check if activity end link is the same as previous activity start link
        if (activityEndCount.get(personId) >= 2) {
            int numberOfLastArrival = activityStartCount.get(personId);
            Id<Trip> lastTripId = Id.create(personId + "_" + numberOfLastArrival, Trip.class);
            if (!trips.get(tripId).getDepartureLinkId().equals(trips.get(lastTripId).getArrivalLinkId())) {
                //System.err.println("Activity end link differs from previous activity start link.");
                throw new RuntimeException("Activity end link differs from previous activity start link.");
            }
        }


        // check if type of ending activity is the same as type of previously started activity
        if (activityEndCount.get(personId) >= 2) {
            int numberOfLastArrival = activityStartCount.get(personId);
            Id<Trip> lastTripId = Id.create(personId + "_" + numberOfLastArrival, Trip.class);
            if (!trips.get(tripId).getActivityTypeBeforeTrip().equals(trips.get(lastTripId).getActivityTypeAfterTrip())) {
                //System.err.println("Type of ending activity is not the same as type of previously started activity.");
                throw new RuntimeException("Type of ending activity is not the same as type of previously started activity.");
            }
        }
    }


    @Override
    public void handleEvent(ActivityStartEvent event) {
        //if its pt interaction, skip it
        if (event.getActType().equals(PT_INTERACTION)) {

            Id<Person> personId = event.getPersonId();
            Id<Trip> tripId = Id.create(personId + "_" + activityEndCount.get(personId), Trip.class);
            MatsimTrip matsimTrip = trips.get(tripId);
            if (matsimTrip != null) {
                matsimTrip.setLegMode(TransportMode.pt);
                matsimTrip.setLegModeLock(true);
            }
            return;
        }

        // store information from event to variables and print the information on console
        //String eventType = event.getEventType();
        Id<Link> linkId = event.getLinkId();
        //String linkShortened = linkId.toString().substring(0, 10) + "...";
        Id<Person> personId = event.getPersonId();
        double time_s = event.getTime();
        String actType = event.getActType();
        if (aggregateActivityByMainType) actType = aggregateActivityByMainType(actType);
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
        MatsimTrip matsimTrip = trips.get(tripId);
        if (matsimTrip != null) {
            matsimTrip.setArrivalLinkId(linkId);
            matsimTrip.setArrivalTime_s(time_s);
            //trips.get(tripId).setArrivalLegMode(legMode);
            matsimTrip.setActivityTypeAfterTrip(actType);
        } else {
            log.warn("No previous end of activity!");
            this.noPreviousEndOfActivityCounter++;
        }


        // check if number of activity ends and number of activity starts are the same
        if(!Objects.equals(activityStartCount.get(personId), activityEndCount.get(personId))) {
            //System.err.println("Activity start count differs from activity end count.");
            throw new RuntimeException("Activity start count differs from activity end count.");
        }


        // checking leg modes is not applicable here
    }


    @Override
    public void handleEvent(LinkLeaveEvent event) {
        // store information from event to variables
        //String eventType = event.getEventType();
        Id<Link> linkId = event.getLinkId();
        //String linkShortened = linkId.toString().substring(0, 10) + "...";
//		Id<Person> personId = event.getDriverId();
        Id<Person> personId = vehicle2driver.getDriverOfVehicle(event.getVehicleId());
        //double time = event.getTime();
        //Id vehicleId = event.getVehicleId();


        // add information concerning passed links to the object "Trip"
        Id<Trip> tripId = Id.create(personId + "_" + activityEndCount.get(personId), Trip.class);
        MatsimTrip fromMatsimTrip = trips.get(tripId);
        if (fromMatsimTrip != null) {
        // without trip there was no activity that ended before, ergo this person is not of interest for the analysis
            if (fromMatsimTrip.getLinks().isEmpty()) {
                if (fromMatsimTrip.getDepartureLinkId().equals(linkId)) {
                    fromMatsimTrip.getLinks().add(linkId);
                    //System.out.println("Added first link to trip " + tripId);
                } else {
                    //System.err.println("First route link different from departure link!");
                    throw new RuntimeException("First route link different from departure link!");
                }
            } else {
                fromMatsimTrip.getLinks().add(linkId);
//			System.out.println("Added another link to trip " + tripId);
//			System.out.println("List of trip " + tripId + " has now " + trips.get(tripId).getLinks().size() + " elements");
            }
        }
    }


    //	// --------------------------------------------------------------------------------------------------
    public void handleEvent(PersonArrivalEvent event) {
        // store information from event to variable
        String legMode = event.getLegMode();
        //System.out.println("Mode of current trip is " + legModeString);
        Id<Person> personId = event.getPersonId();
        // other information not needed

        // add information concerning leg mode to the object "Trip"
        Id<Trip> tripId = Id.create(personId + "_" + activityEndCount.get(personId), Trip.class);
        MatsimTrip fromMatsimTrip = trips.get(tripId);
        if (fromMatsimTrip != null) {
            // without trip there was no activity that ended before, ergo this person is not of interest for the analysis
            if (legMode.equals(TransportMode.transit_walk) || legMode.equals(TransportMode.egress_walk) || legMode.equals(TransportMode.access_walk)) {
                legMode = TransportMode.walk;
            }
            fromMatsimTrip.setLegMode(legMode);
            if (tripmodeWarn) {
                log.warn("Trip mode = Arrival leg mode; assumed that every leg has the same legMode");
                tripmodeWarn = false;
            }
        }

    }

    @Override
    public void handleEvent(PersonStuckEvent event) {

        Id<Person> personId = event.getPersonId();

        //person stuck, trip is not useful, delete
        Id<Trip> tripId = Id.create(personId + "_" + activityEndCount.get(personId), Trip.class);
        trips.remove(tripId);
        personStuckCounter++;
    }

// --------------------------------------------------------------------------------------------------

    @Override
    public void reset(int iteration) {
    }

    public Map<Id<Trip>, MatsimTrip> getTrips() {
        cropTripsWithoutArrival();
        return this.trips;
    }

    private void cropTripsWithoutArrival() {
        trips.entrySet().removeIf(e -> e.getValue().getArrivalLinkId() == null );
    }

    public int getNoPreviousEndOfActivityCounter() {
        return this.noPreviousEndOfActivityCounter;
    }

    public int getPersonStuckCounter() {
        return this.personStuckCounter;
    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        vehicle2driver.handleEvent(event);
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
        vehicle2driver.handleEvent(event);
    }
}
