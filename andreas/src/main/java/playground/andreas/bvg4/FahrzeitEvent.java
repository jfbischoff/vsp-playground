/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.andreas.bvg4;

import org.matsim.api.core.v01.Id;

/**
 * Corresponds to Golden Spool File fahrzeit_ist
 * 
 * @author aneumann
 *
 */
public class FahrzeitEvent {
	
	// Tagged VERSION
	private final int rblDate;
	
	// Tagged LSK
	private final int kurs;
	
	// Tagged IST_ABFAHRT
	private final String departureDateIst;
	private final double departureTimeIst;
	
	// Tagged ZEITBASIS
	private final String zeitBasis;
	
	// Tagged FAHRZEUGNUMMER
	private final Id vehId;
	
	// Tagged LFD_NR
	private final int runningNumber;
	
	// Tagged PUNKT_NR
	private final Id stopId;
	
	// Tagged KUERZEL
	private final String stopNameShort;
	
	// Tagged NAME
	private final String stopName;
	
	// Tagged IST_ABFAHRT_AM_PUNKT
	private final String departureDateIstAtStop;
	private final double departureTimeIstAtStop;
	
	// Tagged IST_ABFAHRT
	private final String arrivalDateIstAtStop;
	private final double arrivalTimeIstAtStop;
	
	// Tagged DISTANZ_STRECKE_IST
	private final int distanceStreckeIst;
	
	// Tagged TUERSTATUS
	private final boolean statusOfDoor;
	
	// Tagged STATUS_LOKALISIERUNG
	private final boolean statusLokalisierung;
	
	public FahrzeitEvent(int rblDate, int kurs, String departureDateIst, double departureTimeIst,
			String zeitBasis, Id vehId, int runningNumber, Id stopId, String stopNameShort,
			String stopName, String departureDateIstAtStop, double departureTimeIstAtStop,
			String arrivalDateIstAtStop, double arrivalTimeIstAtStop, int distanceStreckeIst,
			boolean statusOfDoor, boolean statusLokalisierung){
		
		this.rblDate = rblDate;
		this.kurs = kurs;
		this.departureDateIst = departureDateIst;
		this.departureTimeIst = departureTimeIst;
		this.zeitBasis = zeitBasis;
		this.vehId = vehId;
		this.runningNumber = runningNumber;
		this.stopId = stopId;
		this.stopNameShort = stopNameShort;
		this.stopName = stopName;
		this.departureDateIstAtStop = departureDateIstAtStop;
		this.departureTimeIstAtStop = departureTimeIstAtStop;
		this.arrivalDateIstAtStop = arrivalDateIstAtStop;
		this.arrivalTimeIstAtStop = arrivalTimeIstAtStop;
		this.distanceStreckeIst = distanceStreckeIst;
		this.statusOfDoor = statusOfDoor;
		this.statusLokalisierung = statusLokalisierung;
	}


	public int getRblDate() {
		return rblDate;
	}

	public int getKurs() {
		return kurs;
	}

	public String getDepartureDateIst() {
		return departureDateIst;
	}

	public double getDepartureTimeIst() {
		return departureTimeIst;
	}

	public String getZeitBasis() {
		return zeitBasis;
	}

	public Id getVehId() {
		return vehId;
	}

	public int getRunningNumber() {
		return runningNumber;
	}

	public Id getStopId() {
		return stopId;
	}

	public String getStopNameShort() {
		return stopNameShort;
	}

	public String getStopName() {
		return stopName;
	}

	public String getDepartureDateIstAtStop() {
		return departureDateIstAtStop;
	}

	public double getDepartureTimeIstAtStop() {
		return departureTimeIstAtStop;
	}

	public String getArrivalDateIstAtStop() {
		return arrivalDateIstAtStop;
	}

	public double getArrivalTimeIstAtStop() {
		return arrivalTimeIstAtStop;
	}

	public int getDistanceStreckeIst() {
		return distanceStreckeIst;
	}

	public boolean isStatusOfDoor() {
		return statusOfDoor;
	}

	public boolean isStatusLokalisierung() {
		return statusLokalisierung;
	}

	@Override
	public String toString() {
		StringBuffer strB = new StringBuffer();
		strB.append(this.rblDate); strB.append(", ");
		strB.append(this.kurs); strB.append(", ");
		strB.append(this.departureDateIst); strB.append(", ");
		strB.append(this.departureTimeIst); strB.append(", ");
		strB.append(this.zeitBasis); strB.append(", ");
		strB.append(this.vehId); strB.append(", ");
		strB.append(this.runningNumber); strB.append(", ");
		strB.append(this.stopId); strB.append(", ");
		strB.append(this.stopNameShort); strB.append(", ");
		strB.append(this.stopName); strB.append(", ");
		strB.append(this.departureDateIstAtStop); strB.append(", ");
		strB.append(this.departureTimeIstAtStop); strB.append(", ");
		strB.append(this.arrivalDateIstAtStop); strB.append(", ");
		strB.append(this.arrivalTimeIstAtStop); strB.append(", ");
		strB.append(this.distanceStreckeIst); strB.append(", ");
		strB.append(this.statusOfDoor); strB.append(", ");
		strB.append(this.statusLokalisierung); strB.append(", ");
		return strB.toString();
	}
}
