package ch.sbb.matsim.mobsim.qsim;

import org.apache.log4j.Logger;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.pt.ComplexTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.TransitStopHandlerFactory;

import ch.sbb.matsim.config.SBBTransitConfigGroup;
import ch.sbb.matsim.mobsim.qsim.pt.SBBTransitEngineQSimModule;

/**
 * @author Sebastian HÃ¶rl / ETHZ
 */
public class SBBTransitModule extends AbstractModule {
	@Override
	public void install() {
		
		Logger.getLogger(SBBTransitModule.class).warn("COMMENTED THIS OUT: bind(TransitStopHandlerFactory.class).to(ComplexTransitStopHandlerFactory.class).asEagerSingleton();");
		// bind(TransitStopHandlerFactory.class).to(ComplexTransitStopHandlerFactory.class).asEagerSingleton();

		installQSimModule(new SBBTransitEngineQSimModule());
		
        // make sure the config is registered before the simulation starts
        // https://github.com/SchweizerischeBundesbahnen/matsim-sbb-extensions/issues/3
        ConfigUtils.addOrGetModule(getConfig(), SBBTransitConfigGroup.class);
	}
}
