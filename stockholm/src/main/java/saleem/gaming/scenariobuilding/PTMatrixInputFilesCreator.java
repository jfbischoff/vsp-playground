/*
 * Copyright 2018 Mohammad Saleem
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: salee@kth.se
 *
 */ 
package saleem.gaming.scenariobuilding;


import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.utils.CreatePseudoNetwork;

import saleem.stockholmmodel.modelbuilding.PTCapacityAdjusmentPerSample;


/**
 * A class to create input files for ThreadedMatrixCreator class.
 * 
 * @author dziemke, Mohammad Saleem
 */
public class PTMatrixInputFilesCreator {
	private static final Logger log = Logger.getLogger(PTMatrixInputFilesCreator.class);

	public static void main(String[] args) {
		String path = "./ihop2/matsim-input/config.xml";
	    Config config = ConfigUtils.loadConfig(path);
	    final Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
	    double samplesize = config.qsim().getStorageCapFactor();
	    String outputRoot = "./ihop2/matsim-output/output";
//		LogToOutputSaver.setOutputDirectory(outputRoot);

		double departureTime = 8. * 60 * 60;
		
		// Changing vehicle and road capacity according to sample size
		PTCapacityAdjusmentPerSample capadjuster = new PTCapacityAdjusmentPerSample();
		capadjuster.adjustStoarageAndFlowCapacity(scenario, samplesize);
		
//		controler.getConfig().qsim().setInflowConstraint(InflowConstraint.maxflowFromFdiag);
//		controler.getConfig().qsim().setTrafficDynamics(TrafficDynamics.withHoles);

		Network network = scenario.getNetwork();
		TransitSchedule schedule = scenario.getTransitSchedule();
		new CreatePseudoNetwork(schedule, network, "tr_").createNetwork();

//		controler.addControlerListener(new FareControlListener());
//		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
//			@Inject CharyparNagelScoringParametersForPerson parameters;
//			@Override
//			public ScoringFunction createNewScoringFunction(Person person) {
//				final CharyparNagelScoringParameters params = parameters.getScoringParameters( person );
//				SumScoringFunction sumScoringFunction = new SumScoringFunction();
//				sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring( params ));
//				sumScoringFunction.addScoringFunction(new StockholmLegScoring( person, params , scenario.getNetwork()));
//				sumScoringFunction.addScoringFunction(new StockholmMoneyScoring(person, params ));
//				sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring( params ));
//				return sumScoringFunction;			}
//		});
//		controler.addControlerListener(new FareControlListener());
	
		controler.run();
		
		TransitRouter trouter = controler.getInjector().getBinding(TransitRouter.class).getProvider().get();
		createStopsFile(scenario.getTransitSchedule().getFacilities(), outputRoot + "ptStops.csv", ",");
//		
//		// The locationFacilitiesMap is passed twice: Once for origins and once for destinations.
//		// In other uses the two maps may be different -- thus the duplication here.
		new ThreadedMatrixCreator(scenario, scenario.getTransitSchedule().getFacilities(), scenario.getTransitSchedule().getFacilities(), departureTime, outputRoot, " ", 1, trouter);		
	}
	
	
	/**
	 * Creates a csv file containing the public transport stops or measure points
	 */
	public static void createStopsFile(Map<? extends Id,? extends Facility> locationFacilitiesMap, String outputFileStops, String separator) {
		final CSVFileWriter stopsWriter = new CSVFileWriter(outputFileStops, separator);
		
		stopsWriter.writeField("id");
		stopsWriter.writeField("x");
		stopsWriter.writeField("y");
		stopsWriter.writeNewLine();

		for (Facility fac : locationFacilitiesMap.values() ) {
			if ( fac instanceof Identifiable ) {
				stopsWriter.writeField( ((Identifiable)fac).getId() );
			} else {
				throw new RuntimeException( Facility.FACILITY_NO_LONGER_IDENTIFIABLE ) ;
			}
			stopsWriter.writeField(fac.getCoord().getX());
			stopsWriter.writeField(fac.getCoord().getY());
			stopsWriter.writeNewLine();
		}
		
		stopsWriter.close();
		log.info("Stops file based on schedule written.");
	}
}
