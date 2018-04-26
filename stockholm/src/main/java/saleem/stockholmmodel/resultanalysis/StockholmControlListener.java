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
package saleem.stockholmmodel.resultanalysis;

import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
/**
 * A control listener for stuck vehicles and persons.
 * 
 * @author Mohammad Saleem
 *
 */
public class StockholmControlListener implements StartupListener, IterationEndsListener{
	HandleStuckVehicles handler;
	/**
	 * Notifies all observers of the Controler that an iteration is finished
	 * @param event
	 */
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		handler.printStuckPersonsAndVehicles();
		// TODO Auto-generated method stub
		
	}
	/**
	 * Notifies all observers that the controler is initialized and they should do the same
	 *
	 * @param event
	 */
	@Override
	public void notifyStartup(StartupEvent event) {
		// TODO Auto-generated method stub
		handler = new HandleStuckVehicles();
		event.getServices().getEvents().addHandler(handler);
		
	}

}
