package playground.dziemke.analysis.general.srv;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PersonUtils;
import playground.dziemke.analysis.general.Trip;
import playground.dziemke.analysis.general.TripFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * @author gthunig on 04.04.2017.
 */
public class SrvTripFilterImpl implements TripFilter {
    public static final Logger log = Logger.getLogger(SrvTripFilterImpl.class);

    // Parameters
    private boolean onlyAnalyzeTripsWithMode;
    private List<String> modes = new ArrayList<>();

    private boolean onlyAnalyzeTripInteriorOfArea; // formerly results labelled as "int"
    private boolean onlyAnalyzeTripsStartingOrEndingInArea; // formerly results labelled as "ber" (Berlin-based) <----------
    private String[] areaIds;

    private boolean onlyAnalyzeTripsInDistanceRange; // "dist"; usually varied for analysis // <----------
    private double minDistance_km = -1;
    private double maxDistance_km = -1;

    private boolean onlyAnalyzeTripsWithActivityTypeBeforeTrip;
    private String activityTypeBeforeTrip;
    private boolean onlyAnalyzeTripsWithActivityTypeAfterTrip;
    private String activityTypeAfterTrip;

    private boolean onlyAnalyzeTripsDoneByPeopleInAgeRange; // "age"; this requires setting a CEMDAP file
    private Population population;
    private int minAge = -1; // typically "x0"
    private int maxAge = -1; // typically "x9"; highest number usually chosen is 119
    
    private boolean onlyAnalyzeTripsInDepartureTimeWindow;
    private double minDepartureTime_s;
    private double maxDepartureTime_s;

    public void activateMode(String mode) {
        onlyAnalyzeTripsWithMode = true;
        this.modes.add(mode);
    }

    public void activateInt(String... areIds) {
        this.onlyAnalyzeTripInteriorOfArea = true;
        this.areaIds = areIds;
    }

    public void activateSOE(String... areaIds) {
        onlyAnalyzeTripsStartingOrEndingInArea = true;
        this.areaIds = areaIds;
    }

    public void activateDist(double minDistance_km, double maxDistance_km) {
        onlyAnalyzeTripsInDistanceRange = true;
        this.minDistance_km = minDistance_km;
        this.maxDistance_km = maxDistance_km;
    }

    public void activateCertainActBefore(String activityTypeBeforeTrip) {
        onlyAnalyzeTripsWithActivityTypeBeforeTrip = true;
        this.activityTypeBeforeTrip = activityTypeBeforeTrip;
    }

    public void activateCertainActAfter(String activityTypeAfterTrip) {
        onlyAnalyzeTripsWithActivityTypeAfterTrip = true;
        this.activityTypeAfterTrip = activityTypeAfterTrip;
    }

    public void activateAge(Population population, int minAge, int maxAge) {
        onlyAnalyzeTripsDoneByPeopleInAgeRange = true;
        this.population = population;
        this.minAge = minAge;
        this.maxAge = maxAge;
    }
    
    public void activateDepartureTimeRange(double minDepartureTime_s, double maxDepartureTime_s) {
    	onlyAnalyzeTripsInDepartureTimeWindow = true;
        this.minDepartureTime_s = minDepartureTime_s;
        this.maxDepartureTime_s = maxDepartureTime_s;
    }

    public List<? extends Trip> filter(List<? extends Trip> inputTrips) {
        log.info("Unfiltered trips size: " + inputTrips.size());
        List<SrvTrip> filteredTrips = new LinkedList<>();
        boolean printedWarn1 = false;
        boolean printedWarn2 = false;

        for (Trip currentTrip : inputTrips) {
            SrvTrip trip = (SrvTrip)currentTrip;
            // Choose if trip will be considered
            if (onlyAnalyzeTripInteriorOfArea || onlyAnalyzeTripsStartingOrEndingInArea) {
                boolean startingInArea = Arrays.asList(areaIds).contains(trip.getDepartureZoneId().toString());
                boolean endingInArea = Arrays.asList(areaIds).contains(trip.getArrivalZoneId().toString());
                if (onlyAnalyzeTripsStartingOrEndingInArea) {
                    if (!startingInArea && !endingInArea)
                        continue;
                }
                if (onlyAnalyzeTripInteriorOfArea) {
                    if (onlyAnalyzeTripsStartingOrEndingInArea && !printedWarn1) {
                        log.warn("onlyAnalyzeTripInteriorOfArea and onlyAnalyzeTripsStartingOrEndingInArea activated at the same time!");
                        printedWarn1 = true;
                    }
                    if (!startingInArea || !endingInArea)
                        continue;
                }
            }

            if (onlyAnalyzeTripsWithMode) {
                if (!modes.contains(trip.getLegMode())) {
                    continue;
                }
            }

            if (onlyAnalyzeTripsInDistanceRange && (trip.getDistanceBeeline_m() / 1000.) > maxDistance_km) {
                continue;
            }
            if (onlyAnalyzeTripsInDistanceRange && (trip.getDistanceBeeline_m() / 1000.) < minDistance_km) {
                continue;
            }

            if (onlyAnalyzeTripsWithActivityTypeBeforeTrip && onlyAnalyzeTripsWithActivityTypeAfterTrip && !printedWarn2) {
                log.warn("onlyAnalyzeTripsWithActivityTypeBeforeTrip and onlyAnalyzeTripsWithActivityTypeAfterTrip activated at the same time."
                        + "This may lead to results that are hard to interpret: rather not use these options simultaneously.");
                printedWarn2 = true;
            }

            if (onlyAnalyzeTripsWithActivityTypeBeforeTrip) {
                if (!trip.getActivityTypeBeforeTrip().equals(activityTypeBeforeTrip)) {
                    continue;
                }
            }

            if (onlyAnalyzeTripsWithActivityTypeAfterTrip) {
                if (!trip.getActivityTypeAfterTrip().equals(activityTypeAfterTrip)) {
                    continue;
                }
            }

            if (onlyAnalyzeTripsDoneByPeopleInAgeRange) {
                Person person = population.getPersons().get(trip.getPersonId());
                int age = PersonUtils.getAge(person);
                if (age < minAge || age > maxAge)
                    continue;
            }

            if (onlyAnalyzeTripsInDepartureTimeWindow && (trip.getDepartureTime_s()) > maxDepartureTime_s) {
                continue;
            }
            if (onlyAnalyzeTripsInDepartureTimeWindow && (trip.getDepartureTime_s()) < minDepartureTime_s) {
                continue;
            }

            // activity times and durations
            if ((trip.getArrivalTime_s() < 0) || (trip.getDepartureTime_s() < 0) || (trip.getDuration_s() < 0) ) {
                continue;
            }

			/* Only filteredTrips that fullfill all checked criteria are added; otherwise that loop would have been "continued" already */
            filteredTrips.add(trip);
        }

        log.info("Filtered trips size: " + filteredTrips.size());
        return filteredTrips;
    }

    public String adaptOutputDirectory(String outputDirectory) {
        if (onlyAnalyzeTripsWithMode) {
            for (String mode : modes) {
                outputDirectory = outputDirectory + "_" + mode;
            }
        }
        if (onlyAnalyzeTripInteriorOfArea) {
            outputDirectory = outputDirectory + "_inside-" + areaIds[0];
        }
        if (onlyAnalyzeTripsStartingOrEndingInArea) {
            outputDirectory = outputDirectory + "_soe-in-" + areaIds[0];
        }
        if (onlyAnalyzeTripsInDistanceRange) {
            outputDirectory = outputDirectory + "_dist-" + minDistance_km + "-" + maxDistance_km;
        }
        if (onlyAnalyzeTripsWithActivityTypeBeforeTrip) {
            outputDirectory = outputDirectory + "_act-bef-" + activityTypeBeforeTrip;
        }
        if (onlyAnalyzeTripsWithActivityTypeAfterTrip) {
            outputDirectory = outputDirectory + "_act-aft-" + activityTypeAfterTrip;
        }
        if (onlyAnalyzeTripsDoneByPeopleInAgeRange) {
            outputDirectory = outputDirectory + "_age-" + minAge + "-" + maxAge;
        }
        if (onlyAnalyzeTripsInDepartureTimeWindow) {
            outputDirectory = outputDirectory + "_dep-time-" + (minDepartureTime_s / 3600.) + "-" + (maxDepartureTime_s / 3600.);
        }
        return outputDirectory;
    }

}
