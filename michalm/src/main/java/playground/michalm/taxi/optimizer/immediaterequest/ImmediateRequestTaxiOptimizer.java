/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.michalm.taxi.optimizer.immediaterequest;

import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizerWithOnlineTracking;
import org.matsim.contrib.dvrp.router.*;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.schedule.Task.TaskType;
import org.matsim.contrib.dvrp.util.LinkTimePair;

import playground.michalm.taxi.model.TaxiRequest;
import playground.michalm.taxi.optimizer.*;
import playground.michalm.taxi.schedule.*;
import playground.michalm.taxi.schedule.TaxiTask.TaxiTaskType;


/**
 * The main assumption: Requests are scheduled as the last request in the schedule (of a given
 * vehicle)
 * <p>
 * However, different strategies/policies may be used for:
 * <li>vehicle selection set (only idle, idle+delivering, all)
 * <li>rescheduling on/off (a reaction t changes in a schedule after updating it)
 * 
 * @author michalm
 */
public abstract class ImmediateRequestTaxiOptimizer
    extends AbstractTaxiOptimizer
    implements VrpOptimizerWithOnlineTracking
{
    protected static class VehiclePath
    {
        public static final VehiclePath NO_VEHICLE_PATH_FOUND = new VehiclePath(null,
                new VrpPathImpl(Integer.MAX_VALUE / 2, Integer.MAX_VALUE / 2, Double.MAX_VALUE,
                        new Link[0], new double[0]));

        private final Vehicle vehicle;
        private final VrpPathWithTravelData path;


        protected VehiclePath(Vehicle vehicle, VrpPathWithTravelData path)
        {
            this.vehicle = vehicle;
            this.path = path;
        }
    }


    public static class Params
    {
        public final boolean destinationKnown;
        public final boolean minimizePickupTripTime;
        public final double pickupDuration;
        public final double dropoffDuration;


        public Params(boolean destinationKnown, boolean minimizePickupTripTime,
                double pickupDuration, double dropoffDuration)
        {
            super();
            this.destinationKnown = destinationKnown;
            this.minimizePickupTripTime = minimizePickupTripTime;
            this.pickupDuration = pickupDuration;
            this.dropoffDuration = dropoffDuration;
        }
    }


    private final VrpPathCalculator calculator;
    private final Params params;

    private TaxiDelaySpeedupStats delaySpeedupStats;


    public ImmediateRequestTaxiOptimizer(VrpData data, VrpPathCalculator calculator, Params params)
    {
        super(data);
        this.calculator = calculator;
        this.params = params;
    }


    public void setDelaySpeedupStats(TaxiDelaySpeedupStats delaySpeedupStats)
    {
        this.delaySpeedupStats = delaySpeedupStats;
    }


    @Override
    protected void scheduleRequest(TaxiRequest request)
    {
        VehiclePath bestVehicle = findBestVehicle(request, data.getVehicles());

        if (bestVehicle != VehiclePath.NO_VEHICLE_PATH_FOUND) {
            scheduleRequestImpl(bestVehicle, request);
        }
    }


    protected VehiclePath findBestVehicle(TaxiRequest req, List<Vehicle> vehicles)
    {
        double currentTime = data.getTime();
        VehiclePath best = VehiclePath.NO_VEHICLE_PATH_FOUND;

        for (Vehicle veh : vehicles) {
            Schedule<TaxiTask> schedule = TaxiSchedules.getSchedule(veh);

            // COMPLETED or STARTED but delayed (time window T1 exceeded)
            if (schedule.getStatus() == ScheduleStatus.COMPLETED
                    || currentTime >= veh.getT1()) {
                // skip this vehicle
                continue;
            }

            // status = UNPLANNED/PLANNED/STARTED
            LinkTimePair departure = calculateDeparture(schedule, currentTime);

            if (departure == null) {
                continue;
            }

            VrpPathWithTravelData path = calculator.calcPath(departure.link, req.getFromLink(),
                    departure.time);

            if (params.minimizePickupTripTime) {
                if (path.getTravelTime() < best.path.getTravelTime()) {
                    // TODO: in the future: add a check if the taxi time windows are satisfied
                    best = new VehiclePath(veh, path);
                }
            }
            else {
                if (path.getArrivalTime() < best.path.getArrivalTime()) {
                    // TODO: in the future: add a check if the taxi time windows are satisfied
                    best = new VehiclePath(veh, path);
                }
            }
        }

        return best;
    }


    protected LinkTimePair calculateDeparture(Schedule<TaxiTask> schedule, double currentTime)
    {
        Link link;
        double time;

        switch (schedule.getStatus()) {
            case UNPLANNED:
                Vehicle vehicle = schedule.getVehicle();
                link = vehicle.getStartLink();
                time = Math.max(vehicle.getT0(), currentTime);
                return new LinkTimePair(link, time);

            case PLANNED:
            case STARTED:
                TaxiTask lastTask = Schedules.getLastTask(schedule);

                switch (lastTask.getTaxiTaskType()) {
                    case WAIT_STAY:
                        link = ((StayTask)lastTask).getLink();
                        time = Math.max(lastTask.getBeginTime(), currentTime);
                        return new LinkTimePair(link, time);

                    case PICKUP_STAY:
                        if (!params.destinationKnown) {
                            return null;
                        }

                    default:
                        throw new IllegalStateException();
                }

            case COMPLETED:
            default:
                throw new IllegalStateException();

        }
    }


    protected void scheduleRequestImpl(VehiclePath best, TaxiRequest req)
    {
        Schedule<TaxiTask> bestSched = TaxiSchedules.getSchedule(best.vehicle);

        if (bestSched.getStatus() != ScheduleStatus.UNPLANNED) {// PLANNED or STARTED
            TaxiWaitStayTask lastTask = (TaxiWaitStayTask)Schedules.getLastTask(bestSched);// only WAIT

            switch (lastTask.getStatus()) {
                case PLANNED:
                    if (lastTask.getBeginTime() == best.path.getDepartureTime()) { // waiting for 0 seconds!!!
                        bestSched.removeLastTask();// remove WaitTask
                    }
                    else {
                        // TODO actually this WAIT task will not be performed
                        // so maybe we can remove it right now?

                        lastTask.setEndTime(best.path.getDepartureTime());// shortening the WAIT task
                    }
                    break;

                case STARTED:
                    lastTask.setEndTime(best.path.getDepartureTime());// shortening the WAIT task
                    break;

                case PERFORMED:
                default:
                    throw new IllegalStateException();
            }
        }

        bestSched.addTask(new TaxiPickupDriveTask(best.path, req));

        double t3 = best.path.getArrivalTime() + params.pickupDuration;
        bestSched.addTask(new TaxiPickupStayTask(best.path.getArrivalTime(), t3, req));

        if (params.destinationKnown) {
            appendDropoffAfterPickup(bestSched);
            appendWaitAfterDropoff(bestSched);
        }
    }


    @Override
    protected boolean updateBeforeNextTask(Schedule<TaxiTask> schedule)
    {
        double time = data.getTime();
        TaxiTask currentTask = schedule.getCurrentTask();

        double plannedEndTime;

        if (currentTask.getType() == TaskType.DRIVE) {
            plannedEndTime = ((DriveTask)currentTask).getVehicleTracker().getPlannedEndTime();
        }
        else {
            plannedEndTime = currentTask.getEndTime();
        }

        double delay = time - plannedEndTime;

        if (delay != 0) {
            if (delaySpeedupStats != null) {// optionally, one may record delays
                delaySpeedupStats.updateStats(currentTask, delay);
            }
        }

        updateCurrentAndPlannedTasks(schedule, time);

        if (!params.destinationKnown) {
            if (currentTask.getTaxiTaskType() == TaxiTaskType.PICKUP_STAY) {
                appendDropoffAfterPickup(schedule);
                appendWaitAfterDropoff(schedule);
                return true;
            }
        }

        // return delay != 0;//works only for offline vehicle tracking

        //since we can change currentTask.endTime continuously, it is hard to determine
        //what endTime was at the moment of last reoptimization (triggered by other vehicles or
        //requests)
        return true;
    }


    protected void appendDropoffAfterPickup(Schedule<TaxiTask> schedule)
    {
        TaxiPickupStayTask pickupStayTask = (TaxiPickupStayTask)Schedules.getLastTask(schedule);

        // add DELIVERY after SERVE
        TaxiRequest req = ((TaxiPickupStayTask)pickupStayTask).getRequest();
        Link reqFromLink = req.getFromLink();
        Link reqToLink = req.getToLink();
        double t3 = pickupStayTask.getEndTime();

        VrpPathWithTravelData path = calculator.calcPath(reqFromLink, reqToLink, t3);
        schedule.addTask(new TaxiDropoffDriveTask(path, req));

        double t4 = path.getArrivalTime();
        double t5 = t4 + params.dropoffDuration;
        schedule.addTask(new TaxiDropoffStayTask(t4, t5, req));
    }


    protected void appendWaitAfterDropoff(Schedule<TaxiTask> schedule)
    {
        TaxiDropoffStayTask dropoffStayTask = (TaxiDropoffStayTask)Schedules.getLastTask(schedule);

        // addWaitTime at the end (even 0-second WAIT)
        double t5 = dropoffStayTask.getEndTime();
        double tEnd = Math.max(t5, schedule.getVehicle().getT1());
        Link link = dropoffStayTask.getLink();

        schedule.addTask(new TaxiWaitStayTask(t5, tEnd, link));
    }


    /**
     * @param schedule
     */
    protected void updateCurrentAndPlannedTasks(Schedule<TaxiTask> schedule,
            double currentTaskEndTime)
    {
        Task currentTask = schedule.getCurrentTask();

        if (currentTask.getEndTime() == currentTaskEndTime) {
            return;
        }

        currentTask.setEndTime(currentTaskEndTime);

        List<TaxiTask> tasks = schedule.getTasks();

        int startIdx = currentTask.getTaskIdx() + 1;
        double t = currentTaskEndTime;

        for (int i = startIdx; i < tasks.size(); i++) {
            TaxiTask task = tasks.get(i);

            switch (task.getTaxiTaskType()) {
                case WAIT_STAY: {
                    if (i == tasks.size() - 1) {// last task
                        task.setBeginTime(t);

                        if (task.getEndTime() < t) {// may happen if a previous task was delayed
                            // I used to remove this WAIT_TASK, but now I keep it in the schedule:
                            // schedule.removePlannedTask(task.getTaskIdx());
                            task.setEndTime(t);
                        }
                    }
                    else {
                        // if this is not the last task then some other task must have been added
                        // at time <= t
                        // THEREFORE: task.endTime() <= t, and so it can be removed

                        TaxiTask nextTask = tasks.get(i + 1);
                        switch (nextTask.getTaxiTaskType()) {
                            case PICKUP_DRIVE:

                                TaxiRequest req = ((TaxiPickupDriveTask)nextTask).getRequest();

                                if (req.getT0() > req.getSubmissionTime()) {//advance requests
                                    //currently no support
                                    throw new RuntimeException();
                                }
                                else {//immediate requests
                                    schedule.removeTask(task);
                                    i--;
                                }

                                break;

                            default:
                                //maybe in the future: WAIT+CHARGE or WAIT+CRUISE would make sense
                                //but currently it is not supported
                                throw new RuntimeException();
                        }
                    }

                    break;
                }

                case PICKUP_DRIVE:
                case DROPOFF_DRIVE:
                case CRUISE_DRIVE: {
                    // cannot be shortened/lengthen, therefore must be moved forward/backward
                    task.setBeginTime(t);
                    VrpPathWithTravelData path = (VrpPathWithTravelData) ((DriveTask)task)
                            .getPath();
                    t += path.getTravelTime(); //TODO one may consider recalculation of SP!!!!
                    task.setEndTime(t);

                    break;
                }
                case PICKUP_STAY: {
                    task.setBeginTime(t);// t == taxi's arrival time
                    double t0 = ((TaxiPickupStayTask)task).getRequest().getT0();// t0 == passenger's departure time
                    t = Math.max(t, t0) + params.pickupDuration; // the true pickup starts at max(t, t0)
                    task.setEndTime(t);

                    break;
                }
                case DROPOFF_STAY: {
                    // cannot be shortened/lengthen, therefore must be moved forward/backward
                    task.setBeginTime(t);
                    t += params.dropoffDuration;
                    task.setEndTime(t);

                    break;
                }

                default:
                    throw new IllegalStateException();
            }
        }
    }


    @Override
    public void nextLinkEntered(DriveTask driveTask)
    {
        @SuppressWarnings("unchecked")
        Schedule<TaxiTask> schedule = (Schedule<TaxiTask>)driveTask.getSchedule();

        double predictedEndTime = driveTask.getVehicleTracker().predictEndTime(data.getTime());
        updateCurrentAndPlannedTasks(schedule, predictedEndTime);
    }
}
