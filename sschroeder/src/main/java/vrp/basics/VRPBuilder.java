package vrp.basics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;

import vrp.api.Constraints;
import vrp.api.Costs;
import vrp.api.Customer;
import vrp.api.Node;
import vrp.api.VRP;

public class VRPBuilder {
	
	private Constraints constraints = new Constraints(){

		@Override
		public boolean judge(Tour tour) {
			return true;
		}

		@Override
		public boolean judge(Tour tour, Vehicle vehicle) {
			return true;
		}
		
	};
	
	private Costs costs = new CrowFlyDistance();
	
	private List<Id> depots = new ArrayList<Id>();
	
	private Collection<Customer> customers = new ArrayList<Customer>();
	
	private Map<Id,VehicleType> types = new HashMap<Id, VehicleType>();

	public Customer createAndAddCustomer(Id id, Node node, int demand, double start, double end, double serviceTime, boolean isDepot){
		Customer customer = createCustomer(id, node, demand, start, end, serviceTime);
		addCustomer(customer,isDepot);
		return customer;
	}
	
	public void addCustomer(Customer customer, boolean isDepot){
		customers.add(customer);
		if(isDepot){
			depots.add(customer.getId());
		}
	}
	
	public void assignVehicleType(Id depotId, VehicleType vehicleType){
		types.put(depotId, vehicleType);
	}
	
	private Customer createCustomer(Id id, Node node, int demand, double start, double end, double serviceTime){
		Customer customer = new CustomerImpl(id, node);
		customer.setDemand(demand);
		customer.setServiceTime(serviceTime);
		customer.setTheoreticalTimeWindow(start, end);
		return customer;
	}
	
	public void setConstraints(Constraints constraints){
		this.constraints = constraints;
	}
	
	public void setCosts(Costs costs){
		this.costs = costs;
	}
	
	public VRP buildVRP(){
		verify();
		VRPWithMultipleDepotsAndVehiclesImpl vrp = new VRPWithMultipleDepotsAndVehiclesImpl(depots, customers, costs, constraints);
		for(Id id : types.keySet()){
			vrp.assignVehicleType(id, types.get(id));
		}
		assertEachDepotHasVehicleType(vrp);
		return vrp;
	}
	
	private void assertEachDepotHasVehicleType(VRP vrp) {
		for(Id id : depots){
			VehicleType type = vrp.getVehicleType(id);
			if(type == null){
				throw new IllegalStateException("each depot must have one vehicleType. Depot " + id + " does not have!");
			}
		}
		
	}

	private void verify() {
		if(depots.isEmpty()){
			throw new IllegalStateException("at least one depot must be set");
		}
	}
}
