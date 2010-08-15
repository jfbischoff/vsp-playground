package playground.mzilske.neo;

import java.io.File;

import playground.mzilske.vis.DirectoryUtils;

public class ScenarioXML2Neo {

	public static void main(String[] args) {
		String networkFileName = "../../run951/951.output_network.xml.gz";
		String populationFileName = "../../run951/951.output_plans.xml.gz";
	
		
//		String networkFileName = "../../matsim/output/example5/output_network.xml.gz";
//		String populationFileName = "../../matsim/output/example5/wurst.xml";
		
		String directory = "output/neo";
		DirectoryUtils.deleteDirectory(new File(directory));
		NeoBatchScenario scenario = new NeoBatchScenario(directory);
		try {
				new ApiNetworkReader(scenario).readFile(networkFileName);
				scenario.vacuum();
				new ApiPopulationReader(scenario).readFile(populationFileName);
		} finally {
			scenario.shutdown();
		}
	}

}
