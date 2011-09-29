package freight.vrp;

import java.util.Collection;

import org.matsim.api.core.v01.network.Network;

import playground.mzilske.freight.CarrierShipment;
import playground.mzilske.freight.CarrierVehicle;

public interface VRPSolverFactory {
	
	public abstract VRPSolver createSolver(Collection<CarrierShipment> shipments, Collection<CarrierVehicle> carrierVehicles, Network network);

}
