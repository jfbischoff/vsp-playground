/* *********************************************************************** *
 * project: org.matsim.*
 * ModeData													   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.agarwalamit.fundamentalDiagrams.core;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.IntStream;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.vehicles.VehicleType;
import playground.agarwalamit.utils.ListUtils;

/**
 * @author amit after ssix
 *
 * A class intended to contain "static" mode-dependent data (vehicle type, speed etc.)
 * as well as dynamic data used in the mobsim (speed, flow of the mode)
 * as well as methods to store and update this data.
 */

public final class TravelModesFlowDynamicsUpdator {

	private final int NUMBER_OF_MEMORIZED_FLOWS = 10;
	private Id<VehicleType> modeId;
	private VehicleType vehicleType=null;//      Maybe keeping global data in the EventHandler can be smart (ssix, 25.09.13)
	//	     So far programmed to contain also global data, i.e. data without a specific vehicleType (ssix, 30.09.13)
	private int numberOfAgents;
	//private int numberOfDrivingAgents;//dynamic variable counting live agents on the track
	private double permanentDensity;
	private double permanentAverageVelocity;
	private double permanentFlow;

	private Map<Id<Person>, Double> lastSeenOnStudiedLinkEnter;//records last entry time for every person, but also useful for getting actual number of people in the simulation
	private int speedTableSize;
	private List<Double> speedTable;
	private Double flowTime;
	private List<Double> flowTable15Min;
	private List<Double> lastXHourlyFlows;//recording a number of flows to ensure stability
	private boolean speedStability;
	private boolean flowStability;
	private final Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();

	private final int noOfModes;
	private final double lengthOfTrack;
	private final Id<Link> startOfTheLink;

	TravelModesFlowDynamicsUpdator(final VehicleType vT, Scenario scenario, FDNetworkGenerator fdNetworkGenerator){
		this.vehicleType = vT;
		if(this.vehicleType != null) this.modeId = this.vehicleType.getId();

		this.noOfModes = scenario.getConfig().qsim().getMainModes().size();
		this.lengthOfTrack = fdNetworkGenerator.getLengthOfTrack();
		this.startOfTheLink = fdNetworkGenerator.getFirstLinkIdOfTrack();
	}

	void handle(LinkEnterEvent event){
		if (event.getLinkId().equals(startOfTheLink)){
			Id<Person> personId = this.delegate.getDriverOfVehicle(event.getVehicleId());
			double nowTime = event.getTime();

			this.updateFlow15Min(nowTime, this.vehicleType.getPcuEquivalents());
//			this.updateFlow15Min(nowTime, (double) ((AttributableVehicle)scenario.getVehicles().getVehicles().get(event.getVehicleId())).getAttributes().getAttribute("vehicle_pcu"));
			this.updateSpeedTable(nowTime, personId);

			//Checking for stability
			//Making sure all agents are on the track before testing stability
			//Also waiting half an hour to let the database build itself.

			if ((this.getNumberOfDrivingAgents() == this.numberOfAgents) && (nowTime > FDModule.MAX_ACT_END_TIME * 2)){
				if (!(this.speedStability)){
					this.checkSpeedStability();
				}
				if (!(this.flowStability)){
					this.checkFlowStability15Min();
				}
			}
		}
	}

	public void handle(VehicleEntersTrafficEvent event) {
		this.delegate.handleEvent(event);
	}

	public void handle(VehicleLeavesTrafficEvent event) {
		this.delegate.handleEvent(event);
	}

	void updateFlow15Min(double nowTime, double pcuVehicle){
		if (nowTime == this.flowTime){//Still measuring the flow of the same second
			Double nowFlow = this.flowTable15Min.get(0);
			this.flowTable15Min.set(0, nowFlow +pcuVehicle);
		} else {//Need to offset the new flow table from existing flow table.
			int timeDifference = (int) (nowTime- this.flowTime);
			if (timeDifference<900){
				for (int i=899-timeDifference; i>=0; i--){
					this.flowTable15Min.set(i+timeDifference, this.flowTable15Min.get(i));
				}
				if (timeDifference > 1){
					for (int i = 1; i<timeDifference; i++){
						this.flowTable15Min.set(i, 0.);
					}
				}
				this.flowTable15Min.set(0, pcuVehicle);
			} else {
				this.flowTable15Min = new LinkedList<>(Collections.nCopies(900,0.));
			}
			this.flowTime = nowTime;
		}
		updateLastXFlows900();
	}

	private void updateLastXFlows900(){
		Double nowFlow = this.getCurrentHourlyFlow();
		for (int i=NUMBER_OF_MEMORIZED_FLOWS-2; i>=0; i--){
			this.lastXHourlyFlows.set(i+1, this.lastXHourlyFlows.get(i));
		}
		this.lastXHourlyFlows.set(0, nowFlow);
	}

	void updateSpeedTable(double nowTime, Id<Person> personId){
		if (this.lastSeenOnStudiedLinkEnter.containsKey(personId)){
			double lastSeenTime = lastSeenOnStudiedLinkEnter.get(personId);
			double speed = this.lengthOfTrack / (nowTime-lastSeenTime);//in m/s!!
			for (int i=speedTableSize-2; i>=0; i--){
				this.speedTable.set(i+1, this.speedTable.get(i));
			}
			this.speedTable.set(0, speed);

			this.lastSeenOnStudiedLinkEnter.put(personId,nowTime);
		} else {
			this.lastSeenOnStudiedLinkEnter.put(personId, nowTime);
		}
	}

	private void checkSpeedStability(){
		double averageSpeed = ListUtils.doubleMean(this.speedTable);
		double relativeDeviances = IntStream.range(0, this.speedTableSize)
											.mapToDouble(i -> Math.pow((this.speedTable.get(i) - averageSpeed) / averageSpeed,
													2))
											.sum();
		relativeDeviances /= this.noOfModes;//taking dependence on number of modes away
		if (relativeDeviances < 0.0005){
			this.speedStability = true;
			FDModule.LOG.info("========== Reaching a certain speed stability in mode: "+modeId);
		} else {
			this.speedStability = false;
		}
	}

	void checkFlowStability15Min(){
//		double relativeDeviances = 0.;
//		double avgFlow = ListUtils.doubleMean(this.lastXHourlyFlows);
//		for(int i=0;i<this.NUMBER_OF_MEMORIZED_FLOWS;i++){
//			relativeDeviances  += Math.pow( (this.lastXHourlyFlows.get(i).doubleValue() - avgFlow)/avgFlow , 2);
//		}
//		relativeDeviances /= this.NUMBER_OF_MEMORIZED_FLOWS;

		double absoluteDeviances = this.lastXHourlyFlows.get(this.lastXHourlyFlows.size()-1) - this.lastXHourlyFlows.get(0);
		if (Math.abs(absoluteDeviances) < 1 ){
			// probably, one could make it dependent on PCU of vehicle so that effectively,
			// it is stability of flow in #vehicles/h rather than stability of flow in PCU/h.
			// for e.g., "if (Math.abs(absoluteDeviances) < 1 / (this.vehicleType != null ? this.vehicleType.getPcuEquivalents() :1 ) )" Amit Apr'18

//		if(relativeDeviances < 0.05){
			this.flowStability = true;
			if(modeId==null) FDModule.LOG.info("========== Reaching a certain flow stability for global flow.");
			else FDModule.LOG.info("========== Reaching a certain flow stability in mode: "+modeId.toString());
		} else {
			this.flowStability = false;
		}
	}
	
	void resetBins() {
		//numberOfAgents for each mode should be initialized at this point
		this.decideSpeedTableSize();
		this.speedTable = new LinkedList<>(Collections.nCopies(this.speedTableSize, 0.));

		this.flowTime = 0.;
		this.flowTable15Min = new LinkedList<>(Collections.nCopies(900,0.));

		this.lastXHourlyFlows = new LinkedList<>(Collections.nCopies(this.NUMBER_OF_MEMORIZED_FLOWS, 0.));

		this.speedStability = false;
		this.flowStability = false;
		this.lastSeenOnStudiedLinkEnter = new TreeMap<>();
		this.permanentDensity = 0.;
		this.permanentAverageVelocity =0.;
		this.permanentFlow = 0.;
	}

	private void decideSpeedTableSize() {
		//Ensures a significant speed sampling for every mode size
		//Is pretty empirical and can be changed if necessary (ssix, 16.10.13)
		if (this.numberOfAgents >= 500) this.speedTableSize = 50;
		else if (this.numberOfAgents >= 100) this.speedTableSize = 20;
		else if (this.numberOfAgents >= 10) this.speedTableSize = 10;
		else if (this.numberOfAgents >  0) this.speedTableSize = this.numberOfAgents;
		else { //case no agents in mode
			this.speedTableSize = 1;
		}
	}

	void saveDynamicVariables(){
		this.permanentDensity = this.numberOfAgents / (lengthOfTrack) *1000 * this.vehicleType.getPcuEquivalents();
		this.permanentAverageVelocity = this.getActualAverageVelocity();
		FDModule.LOG.info("Calculated permanent Speed from "+modeId+"'s lastXSpeeds : "+speedTable+"\nResult is : "+this.permanentAverageVelocity);
		this.permanentFlow = this.getSlidingAverageOfLastXHourlyFlows();
		FDModule.LOG.info("Calculated permanent Flow from "+modeId+"'s lastXFlows900 : "+lastXHourlyFlows+"\nResult is :"+this.permanentFlow);
	}

	VehicleType getVehicleType(){
		return this.vehicleType;
	}

	Id<VehicleType> getModeId(){
		return this.modeId;
	}

	double getActualAverageVelocity(){
		return ListUtils.doubleMean(this.speedTable);
	}

	double getCurrentHourlyFlow(){
		return this.flowTable15Min.stream().mapToDouble(i->i).sum() * 4;
	}

	private double getSlidingAverageOfLastXHourlyFlows(){
		return this.lastXHourlyFlows.stream().mapToDouble(i->i).average().orElse(0.);
	}

	boolean isSpeedStable(){
		return this.speedStability;
	}

	boolean isFlowStable(){
		return this.flowStability;
	}

	public int getNumberOfAgents(){
		return this.numberOfAgents ;
	}

	public void setnumberOfAgents(int n){
		this.numberOfAgents = n;
	}

	public double getPermanentDensity(){
		return this.permanentDensity;
	}

	void setPermanentDensity(double permanentDensity) {
		this.permanentDensity = permanentDensity;
	}

	public double getPermanentAverageVelocity(){
		return this.permanentAverageVelocity;
	}

	void setPermanentAverageVelocity(double permanentAverageVelocity) {
		this.permanentAverageVelocity = permanentAverageVelocity;
	}

	public double getPermanentFlow(){
		return this.permanentFlow;
	}

	void setPermanentFlow(double permanentFlow) {
		this.permanentFlow = permanentFlow;
	}

	int getNumberOfDrivingAgents() {
		return this.lastSeenOnStudiedLinkEnter.size();
	}

	public int getSpeedTableSize() {
		return speedTableSize;
	}
}