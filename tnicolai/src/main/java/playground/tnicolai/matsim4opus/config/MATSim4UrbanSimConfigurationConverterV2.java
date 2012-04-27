/* *********************************************************************** *
 * project: org.matsim.*
 * MATSimConfigObject.java
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

/**
 * 
 */
package playground.tnicolai.matsim4opus.config;

import java.io.File;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.Module;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.SimulationConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.tnicolai.matsim4opus.constants.Constants;
import playground.tnicolai.matsim4opus.matsim4urbansim.jaxbconfig2.ConfigType;
import playground.tnicolai.matsim4opus.matsim4urbansim.jaxbconfig2.Matsim4UrbansimType;
import playground.tnicolai.matsim4opus.matsim4urbansim.jaxbconfig2.MatsimConfigType;
import playground.tnicolai.matsim4opus.utils.ids.IdFactory;
import playground.tnicolai.matsim4opus.utils.io.Paths;

/**
 * @author thomas
 * 
 * improvements dec'11:
 * - adjusting flow- and storage capacities to population sample rate. The
 * storage capacity includes a fetch factor to avoid backlogs and network breakdown
 * for small sample rates.
 * 
 * improvements jan'12:
 * - initGlobalSettings sets the number of available processors in the 
 * 	GlobalConfigGroup to speed up MATSim computations. Before that only
 * 	2 processors were used even if there are more.
 * 
 * improvements feb'12:
 * - setting mutationrange = 2h for TimeAllocationMutator (this seems to 
 * shift the depature times ???)
 * 
 * improvements march'12:
 * - extended the MATSim4UrbanSim configuration, e.g. a standard MATSim config can be loaded
 *
 */
public class MATSim4UrbanSimConfigurationConverterV2 {
	
	// logger
	private static final Logger log = Logger.getLogger(MATSim4UrbanSimConfigurationConverterV2.class);
	
	// MATSim scenario
	private ScenarioImpl scenario 	= null;
	// JAXB representation of matsim4urbansim config
	private MatsimConfigType matsimConfig = null;
	
	/**
	 * constructor
	 * 
	 * @param scenario stores MATSim parameters
	 * @param matsimConfig stores all parameters from matsim4urbansim config ( generated by UrbanSim )
	 */
	public MATSim4UrbanSimConfigurationConverterV2(final MatsimConfigType matsimConfig){
		this.scenario = null;
		this.matsimConfig = matsimConfig;	
	}
	
	/**
	 * constructor
	 * 
	 * @param scenario stores MATSim parameters
	 * @param matsimConfiFile path to matsim config file
	 */
	public MATSim4UrbanSimConfigurationConverterV2(final String matsimConfiFile){
		
		this.scenario = null;
		this.matsimConfig = unmarschal(matsimConfiFile); // loading and initializing MATSim config		
	}
	
	/**
	 * loading, validating and initializing MATSim config.
	 */
	MatsimConfigType unmarschal(String matsimConfigFile){
		
		// JAXBUnmaschal reads the UrbanSim generated MATSim config, validates it against
		// the current xsd (checks e.g. the presents and data type of parameter) and generates
		// an Java object representing the config file.
		JAXBUnmaschalV2 unmarschal = new JAXBUnmaschalV2( matsimConfigFile );
		
		MatsimConfigType matsimConfig = null;
		
		// binding the parameter from the MATSim Config into the JAXB data structure
		if( (matsimConfig = unmarschal.unmaschalMATSimConfig()) == null){
			log.error("Unmarschalling failed. SHUTDOWN MATSim!");
			System.exit(Constants.UNMARSCHALLING_FAILED);
		}
		return matsimConfig;
	}
	
	/**
	 * Transferring all parameter from matsim4urbansim config to internal MATSim config/scenario
	 * @return boolean true if initialization successful
	 */
	public boolean init(){
		
		try{
			// get root elements from JAXB matsim4urbansim config object
			ConfigType matsimParameter = matsimConfig.getConfig();
			Matsim4UrbansimType matsim4UrbanSimParameter = matsimConfig.getMatsim4Urbansim();
			
			// init standard MATSim config first, this may be overwritten from MATSim4UrbanSim config
			initStandradMATSimConfig(matsimParameter);
			
			// MATSim4UrbanSim config initiation
			initGlobalSettings();
			initMATSim4UrbanSimParameter(matsim4UrbanSimParameter);
			initNetwork(matsimParameter);
			initInputPlansFile(matsimParameter);
			initControler(matsimParameter);
			initPlanCalcScore(matsimParameter);
			initSimulation();
			initStrategy(matsimParameter);
			initPlanCalcRoute();
			
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	private void initStandradMATSimConfig(ConfigType matsimParameter){
		String standardMATSimConfig = matsimParameter.getMatsimConfig().getInputFile();
		if(standardMATSimConfig != null && Paths.pathExsits(standardMATSimConfig)){
			log.info("Initializing MATSim from standard MATSim config file: " + standardMATSimConfig);
			scenario = (ScenarioImpl)ScenarioUtils.createScenario(ConfigUtils.loadConfig( standardMATSimConfig.trim() ));
		}
		else{
			log.info("Creating an empty MATSim scenario.");
			scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		}
		log.info("...done!");
	}
	
	/**
	 * Determines and sets available processors into MATSim config
	 */
	private void initGlobalSettings(){
		log.info("Setting GlobalConfigGroup to config...");
		GlobalConfigGroup globalCG = (GlobalConfigGroup) scenario.getConfig().getModule(GlobalConfigGroup.GROUP_NAME);
		globalCG.setNumberOfThreads(Runtime.getRuntime().availableProcessors());
		log.info("GlobalConfigGroup settings:");
		log.info("Number of Threads: " + Runtime.getRuntime().availableProcessors() + " ...");
		log.info("... done!");
	}
	
	/**
	 * store matsim4urbansim parameter in MATSim config.Param()
	 * 
	 * @param matsim4UrbanSimParameter
	 */
	private void initMATSim4UrbanSimParameter(Matsim4UrbansimType matsim4UrbanSimParameter){
		log.info("Setting MATSim4UrbanSim to config...");
		
		initUrbanSimParameter(matsim4UrbanSimParameter);
		initMATSim4UrbanSimControler(matsim4UrbanSimParameter);
		initAccessibilityParameter(matsim4UrbanSimParameter);
		
		log.info("... done!");
	}
	
	/**
	 * store UrbanSimParameter
	 * 
	 * @param matsim4UrbanSimParameter
	 */
	private void initUrbanSimParameter(Matsim4UrbansimType matsim4UrbanSimParameter){
		
		// get every single matsim4urbansim/urbansimParameter
		double populationSamplingRate = matsim4UrbanSimParameter.getUrbansimParameter().getPopulationSamplingRate();
		double randomLocationDistributionRadiusForUrbanSimZone = matsim4UrbanSimParameter.getUrbansimParameter().getRandomLocationDistributionRadiusForUrbanSimZone();
		int year 				= matsim4UrbanSimParameter.getUrbansimParameter().getYear().intValue();
		String opusHome 		= Paths.checkPathEnding( matsim4UrbanSimParameter.getUrbansimParameter().getOpusHome() );
		String opusDataPath 	= Paths.checkPathEnding( matsim4UrbanSimParameter.getUrbansimParameter().getOpusDataPath() );
		String matsim4Opus 		= Paths.checkPathEnding( matsim4UrbanSimParameter.getUrbansimParameter().getMatsim4Opus() );
		String matsim4OpusConfig = Paths.checkPathEnding( matsim4UrbanSimParameter.getUrbansimParameter().getMatsim4OpusConfig() );
		String matsim4OpusOutput = Paths.checkPathEnding( matsim4UrbanSimParameter.getUrbansimParameter().getMatsim4OpusOutput() );
		String matsim4OpusTemp 	= Paths.checkPathEnding( matsim4UrbanSimParameter.getUrbansimParameter().getMatsim4OpusTemp() );
		String matsim4OpusBackup = Paths.checkPathEnding( matsim4UrbanSimParameter.getUrbansimParameter().getMatsim4Opus() ) + Paths.checkPathEnding( "backup" );
		boolean isTestRun 		= matsim4UrbanSimParameter.getUrbansimParameter().isIsTestRun();
		boolean backupRunData 	= matsim4UrbanSimParameter.getUrbansimParameter().isBackupRunData();
		String testParameter 	= matsim4UrbanSimParameter.getUrbansimParameter().getTestParameter();
		
		UrbanSimParameterConfigModule module = this.getUrbanSimParameterConfig();
		module.setPopulationSampleRate(populationSamplingRate);
		module.setYear(year);
		module.setOpusHome(opusHome);
		module.setOpusDataPath(opusDataPath);
		module.setMATSim4Opus(matsim4Opus);
		module.setMATSim4OpusConfig(matsim4OpusConfig);
		module.setMATSim4OpusOutput(matsim4OpusOutput);
		module.setMATSim4OpusTemp(matsim4OpusTemp);
		module.setMATSim4OpusBackup(matsim4OpusBackup);
		module.setTestParameter(testParameter);
		module.setRandomLocationDistributionRadiusForUrbanSimZone(randomLocationDistributionRadiusForUrbanSimZone);
		module.setBackup(backupRunData);
		module.setTestRun(isTestRun);	
		
		// setting paths into constants structure
		Constants.OPUS_HOME = module.getOpusHome();
		Constants.OPUS_DATA_PATH = module.getOpusDataPath();
		Constants.MATSIM_4_OPUS = module.getMATSim4Opus();
		Constants.MATSIM_4_OPUS_CONFIG = module.getMATSim4OpusConfig();
		Constants.MATSIM_4_OPUS_OUTPUT = module.getMATSim4OpusOutput();
		Constants.MATSIM_4_OPUS_TEMP = module.getMATSim4OpusTemp();
		Constants.MATSIM_4_OPUS_BACKUP = module.getMATSim4OpusBackup();
		
		log.info("UrbanSimParameter settings:");
		log.info("PopulationSamplingRate: " + module.getPopulationSampleRate() );
		log.info("Year: " + module.getYear() ); 
		log.info("OPUS_HOME: " + Constants.OPUS_HOME );
		log.info("OPUS_DATA_PATH: " + Constants.OPUS_DATA_PATH );
		log.info("MATSIM_4_OPUS: " + Constants.MATSIM_4_OPUS );
		log.info("MATSIM_4_OPUS_CONIG: " + Constants.MATSIM_4_OPUS_CONFIG );
		log.info("MATSIM_4_OPUS_OUTPUT: " + Constants.MATSIM_4_OPUS_OUTPUT );
		log.info("MATSIM_4_OPUS_TEMP: " + Constants.MATSIM_4_OPUS_TEMP ); 
		log.info("MATSIM_4_OPUS_BACKUP: " + Constants.MATSIM_4_OPUS_BACKUP );
		log.info("(Custom) Test Parameter: " + module.getTestParameter() );
		log.info("RandomLocationDistributionRadiusForUrbanSimZone:" + module.getRandomLocationDistributionRadiusForUrbanSimZone());
		log.info("Backing Up Run Data: " + module.isBackup() );
		log.info("Is Test Run: " + module.isTestRun() );
	}
	
	/**
	 * Setting 
	 * 
	 * @param matsim4UrbanSimParameter
	 */
	private void initMATSim4UrbanSimControler(Matsim4UrbansimType matsim4UrbanSimParameter){
		
		// get every single matsim4urbansim/matsim4urbansimContoler parameter
		int cellSize 							= matsim4UrbanSimParameter.getMatsim4UrbansimContoler().getCellSizeCellBasedAccessibility().intValue();
		boolean useCustomBoundingBox			= matsim4UrbanSimParameter.getMatsim4UrbansimContoler().isUseCustomBoundingBox();
		double boundingBoxLeft					= matsim4UrbanSimParameter.getMatsim4UrbansimContoler().getBoundingBoxLeft();
		double boundingBoxBottom				= matsim4UrbanSimParameter.getMatsim4UrbansimContoler().getBoundingBoxBottom();
		double boundingBoxRight					= matsim4UrbanSimParameter.getMatsim4UrbansimContoler().getBoundingBoxRight();
		double boundingBoxTop					= matsim4UrbanSimParameter.getMatsim4UrbansimContoler().getBoundingBoxTop();
		String shapeFile						= matsim4UrbanSimParameter.getMatsim4UrbansimContoler().getShapeFileCellBasedAccessibility().getInputFile();
		if(!Paths.pathExsits(shapeFile)){
			log.warn("Shape-file " + shapeFile + " not found!");
			shapeFile = null;
		}
		
		boolean computeZone2ZoneImpedance 		= matsim4UrbanSimParameter.getMatsim4UrbansimContoler().isZone2ZoneImpedance();
		boolean computeAgentPerformanceFeedback	= matsim4UrbanSimParameter.getMatsim4UrbansimContoler().isAgentPerformance();
		boolean computeZoneBasedAccessibility 	= matsim4UrbanSimParameter.getMatsim4UrbansimContoler().isZoneBasedAccessibility();
		boolean computeCellBasedAccessibility	= matsim4UrbanSimParameter.getMatsim4UrbansimContoler().isCellBasedAccessibility();
		boolean computeCellBasedAccessibilityNetwork   = false;
		boolean computeCellbasedAccessibilityShapeFile = false;
		// if cell-based accessibility is enabled, check whether a shapefile is given 
		// (cell-based shape file computation enabled) or not (cell-based network computation enabled) 
		if(computeCellBasedAccessibility){ 
			if(shapeFile == null)
				computeCellBasedAccessibilityNetwork   = true;
			else
				computeCellbasedAccessibilityShapeFile = true;
		}

		MATSim4UrbanSimControlerConfigModule module = getMATSim4UrbaSimControlerConfig();
		module.setAgentPerformance(computeAgentPerformanceFeedback);
		module.setZone2ZoneImpedance(computeZone2ZoneImpedance);
		module.setZoneBasedAccessibility(computeZoneBasedAccessibility);
		module.setCellBasedAccessibility(computeCellBasedAccessibility);
		module.setCellSizeCellBasedAccessibility(cellSize);
		module.setCellBasedAccessibilityShapeFile(computeCellbasedAccessibilityShapeFile);
		module.setCellBasedAccessibilityNetwork(computeCellBasedAccessibilityNetwork);
		module.setShapeFileCellBasedAccessibility(shapeFile);
		module.setUseCustomBoundingBox(useCustomBoundingBox);
		module.setBoundingBoxLeft(boundingBoxLeft);
		module.setBoundingBoxBottom(boundingBoxBottom);
		module.setBoundingBoxRight(boundingBoxRight);
		module.setBoundingBoxTop(boundingBoxTop);
		
		// view results
		log.info("MATSim4UrbanSimControler settings:");
		log.info("Compute Agent-performance: " + module.isAgentPerformance() );
		log.info("Compute Zone2Zone Impedance Matrix: " + module.isZone2ZoneImpedance() ); 
		log.info("Compute Zone-Based Accessibilities: " + module.isZoneBasedAccessibility() );
		log.info("Compute Cell-Based Accessibilities (using ShapeFile): " + module.isCellBasedAccessibilityShapeFile() ); 
		log.info("Compute Cell-Based Accessibilities (using Network Boundaries): " + module.isCellBasedAccessibilityNetwork() );
		log.info("Cell Size: " + module.getCellSizeCellBasedAccessibility() );
		log.info("Use (Custom) Network Boundaries: " + module.isUseCustomBoundingBox() );
		log.info("Network Boundary (Top): " + module.getBoundingBoxTop() ); 
		log.info("Network Boundary (Left): " + module.getBoundingBoxLeft() ); 
		log.info("Network Boundary (Right): " + module.getBoundingBoxRight() ); 
		log.info("Network Boundary (Bottom): " + module.getBoundingBoxBottom() ); 
		log.info("Shape File: " + module.getShapeFileCellBasedAccessibility() );
	}
	
	private void initAccessibilityParameter(Matsim4UrbansimType matsim4UrbanSimParameter){
		
		// these are all parameter for the accessibility computation
		double logitScaleParameter,
		betaCarTT, betaCarTTPower, betaCarLnTT,
		betaCarTD, betaCarTDPower, betaCarLnTD,
		betaCarTC, betaCarTCPower, betaCarLnTC,
		betaWalkTT, betaWalkTTPower, betaWalkLnTT,
		betaWalkTD, betaWalkTDPower, betaWalkLnTD,
		betaWalkTC, betaWalkTCPower, betaWalkLnTC;
		
		PlanCalcScoreConfigGroup cnScoringGroup = scenario.getConfig().planCalcScore();
		
		double accessibilityDestinationSamplingRate = matsim4UrbanSimParameter.getAccessibilityParameter().getAccessibilityDestinationSamplingRate();
		// these parameter define if the beta or logit_scale parameter are taken from MATSim or the config file
		boolean useMATSimLogitScaleParameter 	= matsim4UrbanSimParameter.getAccessibilityParameter().isUseLogitScaleParameterFromMATSim();
		boolean useMATSimCarParameter			= matsim4UrbanSimParameter.getAccessibilityParameter().isUseCarParameterFromMATSim();
		boolean useMATSimWalkParameter			= matsim4UrbanSimParameter.getAccessibilityParameter().isUseWalkParameterFromMATSim();
		boolean useRawSum						= matsim4UrbanSimParameter.getAccessibilityParameter().isUseRawSumsWithoutLn();
		
		if(useMATSimLogitScaleParameter)
			logitScaleParameter = scenario.getConfig().planCalcScore().getBrainExpBeta();
		else
			logitScaleParameter = matsim4UrbanSimParameter.getAccessibilityParameter().getLogitScaleParameter();
		
		if(useMATSimCarParameter){
			// usually travelling_utils are negative
			betaCarTT 	   	= cnScoringGroup.getTraveling_utils_hr() - cnScoringGroup.getPerforming_utils_hr(); // [utils/h]
			betaCarTTPower	= 0.;
			betaCarLnTT		= 0.;
			betaCarTD		= cnScoringGroup.getMarginalUtilityOfMoney() * cnScoringGroup.getMonetaryDistanceCostRateCar(); // this is [utils/money * money/meter] = [utils/meter]
			betaCarTDPower	= 0.;																							// useful setting for MonetaryDistanceCostRateCar: 10cent/km (only fuel) or 
			betaCarLnTD		= 0.;																							// 80cent/km (including taxes, insurance ...)
			betaCarTC		= cnScoringGroup.getMarginalUtilityOfMoney(); // [utils/money]
			betaCarTCPower	= 0.;
			betaCarLnTC		= 0.;
		}
		else{
			betaCarTT 	   	= matsim4UrbanSimParameter.getAccessibilityParameter().getBetaCarTravelTime();
			betaCarTTPower	= matsim4UrbanSimParameter.getAccessibilityParameter().getBetaCarTravelTimePower2();
			betaCarLnTT		= matsim4UrbanSimParameter.getAccessibilityParameter().getBetaCarLnTravelTime();
			betaCarTD		= matsim4UrbanSimParameter.getAccessibilityParameter().getBetaCarTravelDistance();
			betaCarTDPower	= matsim4UrbanSimParameter.getAccessibilityParameter().getBetaCarTravelDistancePower2();
			betaCarLnTD		= matsim4UrbanSimParameter.getAccessibilityParameter().getBetaCarLnTravelDistance();
			betaCarTC		= matsim4UrbanSimParameter.getAccessibilityParameter().getBetaCarTravelCost();
			betaCarTCPower	= matsim4UrbanSimParameter.getAccessibilityParameter().getBetaCarTravelCostPower2();
			betaCarLnTC		= matsim4UrbanSimParameter.getAccessibilityParameter().getBetaCarLnTravelCost();
		}
		
		if(useMATSimWalkParameter){
			// usually travelling_utils are negative
			betaWalkTT		= cnScoringGroup.getTravelingWalk_utils_hr() - cnScoringGroup.getPerforming_utils_hr(); // [utils/h]
			betaWalkTTPower	= 0.;
			betaWalkLnTT	= 0.;
			betaWalkTD		= 0.; // getMonetaryDistanceCostRateWalk doesn't exist thus set to 0.0: [utils/money * money/meter] = [utils/meter]
			betaWalkTDPower	= 0.;												
			betaWalkLnTD	= 0.;
			betaWalkTC		= 0.; // [utils/money]
			betaWalkTCPower	= 0.;
			betaWalkLnTC	= 0.;
		}
		else{
			betaWalkTT		= matsim4UrbanSimParameter.getAccessibilityParameter().getBetaWalkTravelTime();
			betaWalkTTPower	= matsim4UrbanSimParameter.getAccessibilityParameter().getBetaWalkTravelTimePower2();
			betaWalkLnTT	= matsim4UrbanSimParameter.getAccessibilityParameter().getBetaWalkLnTravelTime();
			betaWalkTD		= matsim4UrbanSimParameter.getAccessibilityParameter().getBetaWalkTravelDistance();
			betaWalkTDPower	= matsim4UrbanSimParameter.getAccessibilityParameter().getBetaWalkTravelDistancePower2();
			betaWalkLnTD	= matsim4UrbanSimParameter.getAccessibilityParameter().getBetaWalkLnTravelDistance();
			betaWalkTC		= matsim4UrbanSimParameter.getAccessibilityParameter().getBetaWalkTravelCost();
			betaWalkTCPower	= matsim4UrbanSimParameter.getAccessibilityParameter().getBetaWalkTravelCostPower2();
			betaWalkLnTC	= matsim4UrbanSimParameter.getAccessibilityParameter().getBetaWalkLnTravelCost();
		}
		
		AccessibilityParameterConfigModule module = getAccessibilityParameterConfig();
		module.setAccessibilityDestinationSamplingRate(accessibilityDestinationSamplingRate);
		module.setUseLogitScaleParameterFromMATSim(useMATSimLogitScaleParameter);
		module.setUseRawSumsWithoutLn(useRawSum);
		module.setUseCarParameterFromMATSim(useMATSimCarParameter);
		module.setUseWalkParameterFromMATSim(useMATSimWalkParameter);
		module.setLogitScaleParameter(logitScaleParameter);
		module.setBetaCarTravelTime(betaCarTT);
		module.setBetaCarTravelTimePower2(betaCarTTPower);
		module.setBetaCarLnTravelTime(betaCarLnTT);
		module.setBetaCarTravelDistance(betaCarTD);
		module.setBetaCarTravelDistancePower2(betaCarTDPower);
		module.setBetaCarLnTravelDistance(betaCarLnTD);
		module.setBetaCarTravelCost(betaCarTC);
		module.setBetaCarTravelCostPower2(betaCarTCPower);
		module.setBetaCarLnTravelCost(betaCarLnTC);
		module.setBetaWalkTravelTime(betaWalkTT);
		module.setBetaWalkTravelTimePower2(betaWalkTTPower);
		module.setBetaWalkLnTravelTime(betaWalkLnTT);
		module.setBetaWalkTravelDistance(betaWalkTD);
		module.setBetaWalkTravelDistancePower2(betaWalkTDPower);
		module.setBetaWalkLnTravelDistance(betaWalkLnTD);
		module.setBetaWalkTravelCost(betaWalkTC);
		module.setBetaWalkTravelCostPower2(betaWalkTCPower);
		module.setBetaWalkLnTravelCost(betaWalkLnTC);
		
		
		// view results
		log.info("AccessibilityParameter settings:");
		
		log.info("AccessibilityDestinationSamplingRate: " + module.getAccessibilityDestinationSamplingRate());
		log.info("Compute raw sum (not logsum): " + module.isUseRawSumsWithoutLn() );
		log.info("Logit Scale Parameter: " + module.isUseLogitScaleParameterFromMATSim() ); 
		
		log.info("BETA_CAR_TRAVEL_TIMES: " + module.getBetaCarTravelTime() );
		log.info("BETA_CAR_TRAVEL_TIMES_POWER: " + module.getBetaCarTravelTimePower2() );
		log.info("BETA_CAR_LN_TRAVEL_TIMES: " + module.getBetaCarLnTravelTime());
		log.info("BETA_CAR_TRAVEL_DISTANCE: " + module.getBetaCarTravelDistance() );
		log.info("BETA_CAR_TRAVEL_DISTANCE_POWER: " + module.getBetaCarTravelDistancePower2() );
		log.info("BETA_CAR_LN_TRAVEL_DISTANCE: " + module.getBetaCarLnTravelDistance() );
		log.info("BETA_CAR_TRAVEL_COSTS: " + module.getBetaCarTravelCost() );
		log.info("BETA_CAR_TRAVEL_COSTS_POWER: " + module.getBetaCarTravelCostPower2() );
		log.info("BETA_CAR_LN_TRAVEL_COSTS: " + module.getBetaCarLnTravelCost());
		
		log.info("BETA_WALK_TRAVEL_TIMES: " + module.getBetaWalkTravelTime()  );
		log.info("BETA_WALK_TRAVEL_TIMES_POWER: " + module.getBetaWalkTravelTimePower2() );
		log.info("BETA_WALK_LN_TRAVEL_TIMES: " + module.getBetaWalkLnTravelTime() );
		log.info("BETA_WALK_TRAVEL_DISTANCE: " + module.getBetaWalkTravelDistance() );
		log.info("BETA_WALK_TRAVEL_DISTANCE_POWER: " + module.getBetaWalkTravelDistancePower2() );
		log.info("BETA_WALK_LN_TRAVEL_DISTANCE: " + module.getBetaWalkLnTravelDistance() );
		log.info("BETA_WALK_TRAVEL_COSTS: " + module.getBetaWalkTravelCost() );
		log.info("BETA_WALK_TRAVEL_COSTS_POWER: " + module.getBetaWalkTravelCostPower2() );
		log.info("BETA_WALK_LN_TRAVEL_COSTS: " + module.getBetaWalkLnTravelCost() );
	}
	/**
	 * setting MATSim network
	 * 
	 * @param matsimParameter
	 */
	private void initNetwork(ConfigType matsimParameter){
		log.info("Setting NetworkConfigGroup to config...");
		String networkFile = matsimParameter.getNetwork().getInputFile();
		NetworkConfigGroup networkCG = (NetworkConfigGroup) scenario.getConfig().getModule(NetworkConfigGroup.GROUP_NAME);
		// set network
		networkCG.setInputFile( networkFile );
		
		log.info("NetworkConfigGroup settings:");
		log.info("Network: " + networkCG.getInputFile());
		log.info("... done!");
	}
	
	/**
	 * setting input plans file (for warm/hot start)
	 * 
	 * @param matsimParameter
	 */
	private void initInputPlansFile(ConfigType matsimParameter){
		log.info("Looking for warm or hot start...");
		// get plans file for hot start
		String hotStart = matsimParameter.getHotStartPlansFile().getInputFile();
		// get plans file for warm start 
		String warmStart = matsimParameter.getInputPlansFile().getInputFile();
		
		MATSim4UrbanSimControlerConfigModule module = getMATSim4UrbaSimControlerConfig();
		
		// setting plans file as input
		if( !hotStart.equals("") &&
		  (new File(hotStart)).exists() ){
			log.info("Hot Start detcted!");
			setPlansFile( hotStart );
			module.setHotStart(true);
		}
		else if( !warmStart.equals("") ){
			log.info("Warm Start detcted!");
			setPlansFile( warmStart );
			module.setWarmStart(true);
		}
		else{
			log.info("Cold Start (no plans file) detected!");
			module.setColdStart(true);
		}
		
		// setting target location for hot start plans file
		if(!hotStart.equals("")){
			log.info("Storing plans file from current run. This enables hot start for next MATSim run.");
			module.setHotStartTargetLocation(hotStart);
		}
		else
			module.setHotStartTargetLocation("");
	}

	/**
	 * sets (either a "warm" or "hot" start) a plans file, see above.
	 */
	private void setPlansFile(String plansFile) {
		log.info("Setting PlansConfigGroup to config...");
		PlansConfigGroup plansCG = (PlansConfigGroup) scenario.getConfig().getModule(PlansConfigGroup.GROUP_NAME);
		// set input plans file
		plansCG.setInputFile( plansFile );
		
		log.info("PlansConfigGroup setting:");
		log.info("Input plans file set to: " + plansCG.getInputFile());
		log.info("... done!");
	}
	
	/**
	 * setting controler parameter
	 * 
	 * @param matsimParameter
	 */
	private void initControler(ConfigType matsimParameter){
		log.info("Setting ControlerConfigGroup to config...");
		int firstIteration = matsimParameter.getControler().getFirstIteration().intValue();
		int lastIteration = matsimParameter.getControler().getLastIteration().intValue();
		ControlerConfigGroup controlerCG = (ControlerConfigGroup) scenario.getConfig().getModule(ControlerConfigGroup.GROUP_NAME);
		// set values
		controlerCG.setFirstIteration( firstIteration );
		controlerCG.setLastIteration( lastIteration);
		controlerCG.setOutputDirectory( Constants.MATSIM_4_OPUS_OUTPUT );
		
//		HashSet<String> hs = new HashSet<String>();
//		hs.add("otfvis");
//		controlerCG.setSnapshotFormat(Collections.unmodifiableSet(hs));
		controlerCG.setSnapshotFormat(Arrays.asList("otfvis")); // otfvis dosn't work ???
		controlerCG.setWriteSnapshotsInterval( 0 ); // disabling snapshots
		
		log.info("ControlerConfigGroup settings:");
		log.info("FirstIteration: " + controlerCG.getFirstIteration());
		log.info("LastIteration: " + controlerCG.getLastIteration());
		log.info("MATSim output directory: " +  controlerCG.getOutputDirectory());
		log.info("... done!");
	}
	
	/**
	 * setting planCalcScore parameter
	 * 
	 * @param matsimParameter
	 */
	private void initPlanCalcScore(ConfigType matsimParameter){
		log.info("Setting PlanCalcScore to config...");
		String activityType_0 = matsimParameter.getPlanCalcScore().getActivityType0();
		String activityType_1 = matsimParameter.getPlanCalcScore().getActivityType1();
		
		ActivityParams homeActivity = new ActivityParams(activityType_0);
		homeActivity.setTypicalDuration( matsimParameter.getPlanCalcScore().getHomeActivityTypicalDuration().intValue() ); 	// should be something like 12*60*60
		
		ActivityParams workActivity = new ActivityParams(activityType_1);
		workActivity.setTypicalDuration( matsimParameter.getPlanCalcScore().getWorkActivityTypicalDuration().intValue() );	// should be something like 8*60*60
		workActivity.setOpeningTime( matsimParameter.getPlanCalcScore().getWorkActivityOpeningTime().intValue() );			// should be something like 7*60*60
		workActivity.setLatestStartTime( matsimParameter.getPlanCalcScore().getWorkActivityLatestStartTime().intValue() );	// should be something like 9*60*60
		scenario.getConfig().planCalcScore().addActivityParams( homeActivity );
		scenario.getConfig().planCalcScore().addActivityParams( workActivity );

		log.info("PlanCalcScore settings:");
		log.info("Activity_Type_0: " + homeActivity.getType() + " Typical Duration Activity_Type_0: " + homeActivity.getTypicalDuration());
		log.info("Activity_Type_1: " + workActivity.getType() + " Typical Duration Activity_Type_1: " + workActivity.getTypicalDuration());
		log.info("Opening Time Activity_Type_1: " + workActivity.getOpeningTime()); 
		log.info("Latest Start Time Activity_Type_1: " + workActivity.getLatestStartTime());
		log.info("... done!");
	}
	
	/**
	 * setting simulation
	 */
	private void initSimulation(){
		log.info("Setting SimulationConfigGroup to config...");
		
		
		SimulationConfigGroup simulation = scenario.getConfig().simulation();
		if( simulation == null){		
			simulation = new SimulationConfigGroup();
			scenario.getConfig().addSimulationConfigGroup( simulation );
		}
		
		double popSampling = this.matsimConfig.getMatsim4Urbansim().getUrbansimParameter().getPopulationSamplingRate();
		
		log.warn("FlowCapFactor and StorageCapFactor are adapted to the population sampling rate (sampling rate = " + popSampling + ").");
		
		// setting FlowCapFactor == population sampling rate (no correction factor needed here)
		simulation.setFlowCapFactor( popSampling );	
		
		// Adapting the storageCapFactor has the following reason:
		// Too low SorageCapacities especially with small sampling 
		// rates can (eg 1%) lead to strong backlogs on the traffic network. 
		// This leads to an unstable behavior of the simulation (by breakdowns 
		// during the learning progress).
		// The correction fetch factor introduced here raises the 
		// storage capacity at low sampling rates and becomes flatten 
		// with increasing sampling rates (at a 100% sample, the 
		// storage capacity == 1).			tnicolai nov'11
		if(popSampling <= 0.){
			popSampling = 0.01;
			log.warn("Raised popSampling rate to " + popSampling + " to to avoid erros while calulating the correction fetch factor ...");
		}
		// tnicolai dec'11
		double fetchFactor = Math.pow(popSampling, -0.25);	// same as: / Math.sqrt(Math.sqrt(sample))
		double storageCap = popSampling * fetchFactor;
		
		// setting StorageCapFactor
		simulation.setStorageCapFactor( storageCap );	
		
		boolean removeStuckVehicles = false;
		simulation.setRemoveStuckVehicles( removeStuckVehicles );
		simulation.setStuckTime(10.);
		
		log.info("SimulationConfigGroup settings:");
		log.info("FlowCapFactor (= population sampling rate): "+ scenario.getConfig().simulation().getFlowCapFactor());
		log.warn("StorageCapFactor: " + scenario.getConfig().simulation().getStorageCapFactor() + " (with fetch factor = " + fetchFactor + ")" );
		log.info("RemoveStuckVehicles: " + (removeStuckVehicles?"True":"False") );
		log.info("StuckTime: " + scenario.getConfig().simulation().getStuckTime());
		log.info("... done!");
	}
	
	/**
	 * setting strategy
	 */
	private void initStrategy(ConfigType matsimParameter){
		log.info("Setting StrategyConfigGroup to config...");
		
		// some modules are disables after 80% of overall iterations, 
		// last iteration for them determined here tnicolai feb'12
		int disableStrategyAfterIteration = (int) Math.ceil(scenario.getConfig().controler().getLastIteration() * 0.8);
		
		// configure strategies for re-planning (should be something like 5)
		scenario.getConfig().strategy().setMaxAgentPlanMemorySize( matsimParameter.getStrategy().getMaxAgentPlanMemorySize().intValue() );
		
		// clear if any strategy is defined
		if(!scenario.getConfig().strategy().getStrategySettings().isEmpty())
			scenario.getConfig().strategy().getStrategySettings().clear();
		
		StrategyConfigGroup.StrategySettings timeAlocationMutator = new StrategyConfigGroup.StrategySettings(IdFactory.get(1));
		timeAlocationMutator.setModuleName("TimeAllocationMutator");
		timeAlocationMutator.setProbability( matsimParameter.getStrategy().getTimeAllocationMutatorProbability() ); // should be something like 0.1
		timeAlocationMutator.setDisableAfter(disableStrategyAfterIteration);
		scenario.getConfig().strategy().addStrategySettings(timeAlocationMutator);
		// change mutation range to 2h. tnicolai feb'12
		scenario.getConfig().setParam("TimeAllocationMutator", "mutationRange", "7200"); 
		
		StrategyConfigGroup.StrategySettings changeExpBeta = new StrategyConfigGroup.StrategySettings(IdFactory.get(2));
		changeExpBeta.setModuleName("ChangeExpBeta");
		changeExpBeta.setProbability( matsimParameter.getStrategy().getChangeExpBetaProbability() ); // should be something like 0.9
		scenario.getConfig().strategy().addStrategySettings(changeExpBeta);
		
		StrategyConfigGroup.StrategySettings reroute = new StrategyConfigGroup.StrategySettings(IdFactory.get(3));
		reroute.setModuleName("ReRoute_Dijkstra");
		reroute.setProbability( matsimParameter.getStrategy().getReRouteDijkstraProbability() ); // should be something like 0.1
		reroute.setDisableAfter(disableStrategyAfterIteration);
		scenario.getConfig().strategy().addStrategySettings(reroute);
		
		log.info("StrategyConfigGroup settings:");
		log.info("Strategy_1: " + timeAlocationMutator.getModuleName() + " Probability: " + timeAlocationMutator.getProbability() + " Disable After Itereation: " + timeAlocationMutator.getDisableAfter()); 
		log.info("Strategy_2: " + changeExpBeta.getModuleName() + " Probability: " + changeExpBeta.getProbability());
		log.info("Strategy_3: " + reroute.getModuleName() + " Probability: " + reroute.getProbability() + " Disable After Itereation: " + reroute.getDisableAfter() );
		log.info("... done!");
	}
	
	/**
	 * setting walk speed in plancalcroute
	 */
	private void initPlanCalcRoute(){
		log.info("Setting PlanCalcRouteGroup to config...");
		scenario.getConfig().plansCalcRoute().setWalkSpeed(1.38888889); // 1.38888889m/s corresponds to 5km/h -- and -- 0.833333333333333m/s corresponds to 3km/h
		log.info("PlanCalcRouteGroup settings:");
		log.info("WalkSpeed: " + scenario.getConfig().plansCalcRoute().getWalkSpeed() );
		log.info("...done!");
	}
	
	public ScenarioImpl getScenario(){
			return scenario;
	}
	
	public AccessibilityParameterConfigModule getAccessibilityParameterConfig() {
		Module m = this.scenario.getConfig().getModule(AccessibilityParameterConfigModule.GROUP_NAME);
		if (m instanceof AccessibilityParameterConfigModule) {
			return (AccessibilityParameterConfigModule) m;
		}
		AccessibilityParameterConfigModule apcm = new AccessibilityParameterConfigModule(AccessibilityParameterConfigModule.GROUP_NAME);
		this.scenario.getConfig().getModules().put(AccessibilityParameterConfigModule.GROUP_NAME, apcm);
		return apcm;
	}
	
	public MATSim4UrbanSimControlerConfigModule getMATSim4UrbaSimControlerConfig() {
		Module m = this.scenario.getConfig().getModule(MATSim4UrbanSimControlerConfigModule.GROUP_NAME);
		if (m instanceof MATSim4UrbanSimControlerConfigModule) {
			return (MATSim4UrbanSimControlerConfigModule) m;
		}
		MATSim4UrbanSimControlerConfigModule mccm = new MATSim4UrbanSimControlerConfigModule(MATSim4UrbanSimControlerConfigModule.GROUP_NAME);
		this.scenario.getConfig().getModules().put(MATSim4UrbanSimControlerConfigModule.GROUP_NAME, mccm);
		return mccm;
	}
	
	public UrbanSimParameterConfigModule getUrbanSimParameterConfig() {
		Module m = this.scenario.getConfig().getModule(UrbanSimParameterConfigModule.GROUP_NAME);
		if (m instanceof UrbanSimParameterConfigModule) {
			return (UrbanSimParameterConfigModule) m;
		}
		UrbanSimParameterConfigModule upcm = new UrbanSimParameterConfigModule(UrbanSimParameterConfigModule.GROUP_NAME);
		this.scenario.getConfig().getModules().put(UrbanSimParameterConfigModule.GROUP_NAME, upcm);
		return upcm;
	}
	
	// Testing fetch  factor calculation for storageCap 
	public static void main(String[] args) {
		// testing calculation of storage capacity fetch factor
		for(double sample = 0.01; sample <=1.; sample += 0.01){
			
			double factor = Math.pow(sample, -0.25); // same as: 1. / Math.sqrt(Math.sqrt(sample))
			double storageCap = sample * factor;
			
			System.out.println("Sample rate " + sample + " leads to a fetch fector of: " + factor + " and a StroraceCapacity of: " + storageCap );
		}
		
		for(int i = 0; i <= 100; i++){
			System.out.println("i = " + i + " disable int = " + (int) Math.ceil(i * 0.8)+ " disable double = " + i * 0.8);			
		}
	}
}

