/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.michalm.taxi.optimizer.assignment;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.vsp.ev.data.Battery;
import org.matsim.vsp.ev.data.EvData;
import org.matsim.contrib.locationchoice.router.BackwardFastMultiNodeDijkstraFactory;
import org.matsim.contrib.locationchoice.router.BackwardMultiNodePathCalculator;
import org.matsim.contrib.taxi.optimizer.BestDispatchFinder.Dispatch;
import org.matsim.contrib.taxi.optimizer.VehicleData;
import org.matsim.contrib.taxi.optimizer.assignment.AssignmentDestinationData;
import org.matsim.contrib.taxi.optimizer.assignment.AssignmentRequestInserter;
import org.matsim.contrib.taxi.optimizer.assignment.AssignmentTaxiOptimizer;
import org.matsim.contrib.taxi.optimizer.assignment.VehicleAssignmentProblem;
import org.matsim.contrib.taxi.optimizer.assignment.VehicleAssignmentProblem.AssignmentCost;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.scheduler.TaxiScheduleInquiry;
import org.matsim.contrib.util.PartialSort;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.router.FastAStarEuclideanFactory;
import org.matsim.core.router.FastMultiNodeDijkstraFactory;
import org.matsim.core.router.MultiNodePathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.common.collect.Maps;

import playground.michalm.taxi.data.EvrpVehicle;
import playground.michalm.taxi.optimizer.assignment.AssignmentChargerPlugData.ChargerPlug;
import playground.michalm.taxi.schedule.ETaxiChargingTask;
import playground.michalm.taxi.scheduler.ETaxiScheduler;

/**
 * Main assumptions:
 * <ul>
 * <li>no diversion and destination unknown
 * <li>charging scheduling has higher priority than request scheduling
 * <li>charging scheduling is triggered less frequently than request scheduling
 * </ul>
 * To avoid race conditions / oscillations:
 * <ul>
 * <li>charging scheduling can override planned request-related assignments and planned charging assignments
 * <li>request scheduling can override planned request-related assignments
 * <li>currently executed assignments cannot be interrupted (i.e. diversion is off)
 * <li>since the destination remains unknown till the end of pickup, all schedules end with STAY or PICKUP tasks
 * </ul>
 */
public class AssignmentETaxiOptimizer extends AssignmentTaxiOptimizer {
	public static AssignmentETaxiOptimizer create(TaxiConfigGroup taxiCfg, Fleet fleet, Network network,
			MobsimTimer timer, TravelTime travelTime, TravelDisutility travelDisutility, ETaxiScheduler eScheduler,
			EvData evData, AssignmentETaxiOptimizerParams params) {
		MultiNodePathCalculator multiNodeRouter = (MultiNodePathCalculator)new FastMultiNodeDijkstraFactory(true)
				.createPathCalculator(network, travelDisutility, travelTime);
		BackwardMultiNodePathCalculator backwardMultiNodeRouter = (BackwardMultiNodePathCalculator)new BackwardFastMultiNodeDijkstraFactory(
				true).createPathCalculator(network, travelDisutility, travelTime);
		LeastCostPathCalculator router = new FastAStarEuclideanFactory().createPathCalculator(network, travelDisutility,
				travelTime);
		return new AssignmentETaxiOptimizer(taxiCfg, fleet, timer, travelTime, eScheduler, evData, params,
				multiNodeRouter, backwardMultiNodeRouter, router);
	}

	private final AssignmentETaxiOptimizerParams params;
	private final EvData evData;
	private final ETaxiToPlugAssignmentCostProvider eAssignmentCostProvider;
	private final VehicleAssignmentProblem<ChargerPlug> eAssignmentProblem;
	private final ETaxiScheduler eScheduler;
	private final Fleet fleet;
	private final MobsimTimer timer;

	private final Map<Id<Vehicle>, Vehicle> scheduledForCharging;

	public AssignmentETaxiOptimizer(TaxiConfigGroup taxiCfg, Fleet fleet, MobsimTimer timer, TravelTime travelTime,
			ETaxiScheduler eScheduler, EvData evData, AssignmentETaxiOptimizerParams params,
			MultiNodePathCalculator multiNodeRouter, BackwardMultiNodePathCalculator backwardMultiNodeRouter,
			LeastCostPathCalculator router) {
		super(taxiCfg, fleet, eScheduler, params, new AssignmentRequestInserter(fleet, timer, travelTime, eScheduler,
				params, multiNodeRouter, backwardMultiNodeRouter, router));
		this.params = params;
		this.evData = evData;
		this.eScheduler = eScheduler;
		this.fleet = fleet;
		this.timer = timer;

		if (taxiCfg.isVehicleDiversion() && taxiCfg.isDestinationKnown()) {
			throw new IllegalArgumentException("Unsupported");
		}

		if (params.socCheckTimeStep % params.reoptimizationTimeStep != 0) {
			throw new RuntimeException("charge-scheduling must be followed up by req-scheduling");
		}

		eAssignmentProblem = new VehicleAssignmentProblem<>(travelTime, multiNodeRouter, backwardMultiNodeRouter);

		eAssignmentCostProvider = new ETaxiToPlugAssignmentCostProvider(params);

		int plugsCount = evData.getChargers().size() * 2;// TODO
		scheduledForCharging = Maps.newHashMapWithExpectedSize(plugsCount * 2);
	}

	private final boolean chargingTaskRemovalEnabled = true;

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {
		if (isNewDecisionEpoch(e, params.socCheckTimeStep)) {
			if (chargingTaskRemovalEnabled) {
				unscheduleAwaitingRequestsAndCharging();
			} else {
				unscheduleAwaitingRequests();
			}
			scheduleCharging();
			setRequiresReoptimization(true);
		}

		super.notifyMobsimBeforeSimStep(e);
	}

	private void unscheduleAwaitingRequestsAndCharging() {
		eScheduler.beginChargingTaskRemoval();
		unscheduleAwaitingRequests();// and charging
		List<Vehicle> vehiclesWithChargingTasksRemoved = eScheduler.endChargingTaskRemoval();
		for (Vehicle v : vehiclesWithChargingTasksRemoved) {
			if (scheduledForCharging.remove(v.getId()) == null) {
				throw new RuntimeException();
			}
		}
	}

	// if socCheckTimeStep is too small --> small number of idle plugs --> poorer assignments
	protected void scheduleCharging() {
		AssignmentDestinationData<ChargerPlug> pData = AssignmentChargerPlugData.create(timer.getTimeOfDay(),
				evData.getChargers().values());
		if (pData.getSize() == 0) {
			return;
		}

		VehicleData vData = initVehicleDataForCharging(pData);
		if (vData.getSize() == 0) {
			return;
		}

		AssignmentCost<ChargerPlug> cost = eAssignmentCostProvider.getCost(pData, vData);
		List<Dispatch<ChargerPlug>> assignments = eAssignmentProblem.findAssignments(vData, pData, cost);

		for (Dispatch<ChargerPlug> a : assignments) {
			eScheduler.scheduleCharging((EvrpVehicle)a.vehicle, a.destination.charger, a.path);
			if (scheduledForCharging.put(a.vehicle.getId(), a.vehicle) != null) {
				throw new IllegalStateException();
			}
		}
	}

	@Override
	public void nextTask(Vehicle vehicle) {
		Schedule schedule = vehicle.getSchedule();
		if (schedule.getStatus() == ScheduleStatus.STARTED) {
			if (schedule.getCurrentTask() instanceof ETaxiChargingTask) {
				if (scheduledForCharging.remove(vehicle.getId()) == null) {
					throw new IllegalStateException();
				}
			}
		}

		super.nextTask(vehicle);
	}

	private VehicleData initVehicleDataForCharging(AssignmentDestinationData<ChargerPlug> pData) {
		// XXX if chargers are heavily used then shorten the planning horizon;
		// (like with undersupply of taxis)
		double chargingPlanningHorizon = 10 * 60;// 10 minutes (should be longer than socCheckTimeStep)
		double maxDepartureTime = timer.getTimeOfDay() + chargingPlanningHorizon;
		Stream<? extends Vehicle> vehiclesBelowMinSocLevel = fleet.getVehicles().values().stream()
				.filter(v -> isChargingSchedulable(v, eScheduler, maxDepartureTime));

		// filter least charged vehicles
		// assumption: all b.capacities are equal
		List<? extends Vehicle> leastChargedVehicles = PartialSort.kSmallestElements(
				pData.getSize(), vehiclesBelowMinSocLevel, v -> ((EvrpVehicle)v).getEv().getBattery().getSoc());

		return new VehicleData(timer.getTimeOfDay(), eScheduler, leastChargedVehicles.stream());
	}

	// TODO MIN_RELATIVE_SOC should depend on %idle
	private boolean isChargingSchedulable(Vehicle v, TaxiScheduleInquiry scheduleInquiry, double maxDepartureTime) {
		Battery b = ((EvrpVehicle)v).getEv().getBattery();
		boolean undercharged = b.getSoc() < params.minRelativeSoc * b.getCapacity();
		if (!undercharged || !scheduledForCharging.containsKey(v.getId())) {
			return false;// not needed or already planned
		}

		LinkTimePair departure = scheduleInquiry.getImmediateDiversionOrEarliestIdleness(v);
		return departure != null && departure.time <= maxDepartureTime;// schedulable within the time horizon
	}
}
