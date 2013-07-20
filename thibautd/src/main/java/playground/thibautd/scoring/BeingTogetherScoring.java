/* *********************************************************************** *
 * project: org.matsim.*
 * BeingTogetherScoring.java
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
package playground.thibautd.scoring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ObjectUtils;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.PersonLeavesVehicleEvent;
import org.matsim.core.scoring.ScoringFunctionAccumulator.ArbitraryEventScoring;

import playground.thibautd.utils.MapUtils;
import playground.thibautd.utils.MapUtils.Factory;

/**
 * @author thibautd
 */
public class BeingTogetherScoring implements ArbitraryEventScoring {
	private final double marginalUtilityOfTime;
	private final Id ego;
	private final Set<Id> alters;

	private final Factory<IntervalsAtLocation> locatedIntervalsFactory =
		new Factory<IntervalsAtLocation>() {
			@Override
			public IntervalsAtLocation create() {
				return new IntervalsAtLocation();
			}
		};
	private final IntervalsAtLocation intervalsForEgo = new IntervalsAtLocation();
	private final Map<Id, IntervalsAtLocation> intervalsPerAlter = new HashMap<Id, IntervalsAtLocation>();

	private boolean wasReset = false;

	public BeingTogetherScoring(
			final double marginalUtilityOfTime,
			final Id ego,
			final Collection<Id> alters) {
		this.marginalUtilityOfTime = marginalUtilityOfTime;
		this.ego = ego;
		this.alters = Collections.unmodifiableSet( new HashSet<Id>( alters ) );
	}

	// /////////////////////////////////////////////////////////////////////////
	// basic scoring
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public void finish() {}

	@Override
	public double getScore() {
		if ( wasReset ) throw new IllegalArgumentException();
	
		double accumulatedTimePassedTogether = 0;
		for ( Map.Entry<Location, WrappedAroundIntervalSequence> e : intervalsForEgo.map.entrySet() ) {
			final Location location = e.getKey();
			final List<Interval> egoIntervals = e.getValue().getWrappedAroundSequence();

			for ( IntervalsAtLocation locatedAlterIntervals : intervalsPerAlter.values() ) {
				final WrappedAroundIntervalSequence seq = locatedAlterIntervals.map.get( location );
				if ( seq == null ) continue;
				final List<Interval> alterIntervals = seq.getWrappedAroundSequence();
				accumulatedTimePassedTogether += calcOverlap( egoIntervals , alterIntervals );
			}
		}

		return accumulatedTimePassedTogether * marginalUtilityOfTime;
	}

	private static double calcOverlap(
			final List<Interval> egoIntervals,
			final List<Interval> alterIntervals) {
		double sum = 0;
		for ( Interval ego : egoIntervals ) {
			for ( Interval alter : alterIntervals ) {
				sum += measureOverlap( ego , alter );
			}
		}
		return sum;
	}

	private static double measureOverlap(
			final Interval i1,
			final Interval i2) {
		final double startOverlap = Math.max( i1.start , i2.start );
		final double endOverlap = Math.min( i1.end , i2.end );
		return Math.max( endOverlap - startOverlap , 0 );
	}

	@Override
	public void reset() {
		wasReset = true;
	}

	// /////////////////////////////////////////////////////////////////////////
	// event handling
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public void handleEvent(final Event event) {
		if (event instanceof ActivityStartEvent) startAct( (ActivityStartEvent) event );
		if (event instanceof ActivityEndEvent) endAct( (ActivityEndEvent) event );
		if (event instanceof PersonEntersVehicleEvent) enterVehicle( (PersonEntersVehicleEvent) event );
		if (event instanceof PersonLeavesVehicleEvent) leaveVehicle( (PersonLeavesVehicleEvent) event );
	}

	private void enterVehicle(final PersonEntersVehicleEvent event) {
		if ( !isRelevant( event.getPersonId() ) ) return;
		final IntervalsAtLocation intervals =
			event.getPersonId().equals( ego ) ?
			intervalsForEgo :
			MapUtils.getArbitraryObject(
					event.getPersonId(),
					intervalsPerAlter,
					locatedIntervalsFactory);
		intervals.startInterval(
				new Location( event.getVehicleId() ),
				event.getTime() );
	}

	private void leaveVehicle(final PersonLeavesVehicleEvent event) {
		if ( !isRelevant( event.getPersonId() ) ) return;
		final IntervalsAtLocation intervals =
			event.getPersonId().equals( ego ) ?
			intervalsForEgo :
			MapUtils.getArbitraryObject(
					event.getPersonId(),
					intervalsPerAlter,
					locatedIntervalsFactory);
		intervals.endInterval(
				new Location( event.getVehicleId() ),
				event.getTime() );	
	}

	private void startAct(final ActivityStartEvent event) {
		if ( !isRelevant( event.getPersonId() ) ) return;
		final IntervalsAtLocation intervals =
			event.getPersonId().equals( ego ) ?
			intervalsForEgo :
			MapUtils.getArbitraryObject(
					event.getPersonId(),
					intervalsPerAlter,
					locatedIntervalsFactory);
		intervals.startInterval(
				new Location( event.getLinkId() , event.getFacilityId() , event.getActType() ),
				event.getTime() );		
	}

	private void endAct(final ActivityEndEvent event) {
		if ( !isRelevant( event.getPersonId() ) ) return;
		final IntervalsAtLocation intervals =
			event.getPersonId().equals( ego ) ?
			intervalsForEgo :
			MapUtils.getArbitraryObject(
					event.getPersonId(),
					intervalsPerAlter,
					locatedIntervalsFactory);
		intervals.endInterval(
				new Location( event.getLinkId() , event.getFacilityId() , event.getActType() ),
				event.getTime() );	
	}

	private boolean isRelevant(final Id personId) {
		return ego.equals( personId ) || alters.contains( personId );
	}

	// /////////////////////////////////////////////////////////////////////////
	// classes
	// /////////////////////////////////////////////////////////////////////////
	private static class IntervalsAtLocation {
		private final Factory<WrappedAroundIntervalSequence> seqFactory =
			new Factory<WrappedAroundIntervalSequence>() {
				@Override
				public WrappedAroundIntervalSequence create() {
					return new WrappedAroundIntervalSequence();
				}
			};
		private final Map<Location, WrappedAroundIntervalSequence> map =
			new HashMap<Location,WrappedAroundIntervalSequence>();

		public void startInterval(
				final Location location,
				final double time) {
			final WrappedAroundIntervalSequence seq =
				MapUtils.getArbitraryObject(
						location,
						map,
						seqFactory);
			seq.startInterval( time );
		}

		public void endInterval(
				final Location location,
				final double time) {
			final WrappedAroundIntervalSequence seq =
				MapUtils.getArbitraryObject(
						location,
						map,
						seqFactory);
			seq.endInterval( time );
		}
	}

	private static class Location {
		private final Id vehId;
		private final Id linkId;
		private final Id facilityId;
		private final String activityType;

		public Location(final Id vehicleId) {
			this.vehId = vehicleId;
			this.linkId = null;
			this.facilityId = null;
			this.activityType = null;
		}

		public Location(
				final Id linkId,
				final Id facilityId,
				final String actType) {
			this.vehId = null;
			this.linkId = linkId;
			this.facilityId = facilityId;
			this.activityType = actType;
		}

		@Override
		public boolean equals( final Object o ) {
			return o instanceof Location &&
				ObjectUtils.equals( ((Location) o).vehId , vehId ) &&
				ObjectUtils.equals( ((Location) o).linkId , linkId ) &&
				ObjectUtils.equals( ((Location) o).facilityId , facilityId ) &&
				ObjectUtils.equals( ((Location) o).activityType , activityType );
		}

		@Override
		public int hashCode() {
			return (vehId == null ? 0 : vehId.hashCode()) +
				(linkId == null ? 0 : linkId.hashCode()) +
				(facilityId == null ? 0 : facilityId.hashCode()) +
				(activityType == null ? 0 : activityType.hashCode());
		}
	}

	private static class WrappedAroundIntervalSequence {
		private Interval first = null;
		private final List<Interval> between = new ArrayList<Interval>();
		private Interval last = null;

		public void startInterval(double time) {
			if (last != null) throw new IllegalStateException( "must close interval before starting new one" );
			last = new Interval();
			last.start = time;
		}

		public void endInterval(double time) {
			if ( last == null ) {
				assert between.isEmpty();
				assert first == null;
				first = new Interval();
				first.end = time;
			}
			else {
				last.end = time;
				between.add( last );
				last = null;
			}
		}

		public List<Interval> getWrappedAroundSequence() {
			final List<Interval> seq = new ArrayList<Interval>( between );
			if ( first != null && last != null ) {
				assert Double.isNaN( first.start );
				assert Double.isNaN( last.end );
				final Interval wrap = new Interval();
				wrap.start = last.start;
				wrap.end = first.end + 24 * 3600;
				if ( wrap.start <= wrap.end ) {
					// if time inconsistent, just do not add an interval
					// (the agent is "less than not here")
					seq.add( wrap );
				}
			}
			// XXX probably a better way
			return fitIn24Hours( seq );
		}

		private static List<Interval> fitIn24Hours(final List<Interval> seq) {
			final List<Interval> newList = new ArrayList<Interval>();

			for ( Interval old : seq ) {
				newList.addAll( splitIn24Hours( old ) );
			}

			return newList;
		}

		private static Collection<Interval> splitIn24Hours(final Interval old) {
			if ( old.start >= 0 && old.end <= 24 * 3600 ) return Collections.singleton( old );

			if ( old.start < 0 ) throw new IllegalArgumentException( ""+old.start );
			if ( old.end > 2 * 24 * 3600 ) throw new IllegalArgumentException( ""+old.start );

			if ( old.start > 24 * 3600 ) {
				final Interval newi = new Interval();
				newi.start = old.start - 24 * 3600;
				newi.end = old.end - 24 * 3600;
				return Collections.singleton( newi );
			}

			final List<Interval> split = new ArrayList<Interval>();
			split.add( new Interval( old.start , 24 * 3600 ) );
			split.add( new Interval( 0 , old.end - 24 * 3600 ) );

			return split;
		}
	}

	private static class Interval {
		private double start = Double.NaN;
		private double end = Double.NaN;

		public Interval() {}
		public Interval(final double start, final double end) {
			this.start = start;
			this.end = end;
		}
	}
}

