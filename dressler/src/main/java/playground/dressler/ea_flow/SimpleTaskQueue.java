/* *********************************************************************** *
 * project: org.matsim.*
 * SimpleTaskQueue.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.dressler.ea_flow;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import playground.dressler.network.IndexedNodeI;

public class SimpleTaskQueue implements TaskQueue {
	private int depth = 0;
	private IndexedNodeI origin = null;
	private LinkedList<BFTask> _list;
	
	public SimpleTaskQueue(){
		_list= new LinkedList<BFTask>();
	}
	
	@Override
	public boolean addAll(Collection<? extends BFTask> c) {
		//return _list.addAll(c);
		Boolean result = false;
		for(BFTask task: c){
			task.depth = this.depth;
			if (task.origin == null) task.origin = this.origin;
			result = _list.add(task) || result; // never want a shortcut!		
		}
		return result;
	}

	@Override
	public Iterator<BFTask> iterator() {
		return _list.iterator();
	}

	@Override
	public boolean add(BFTask task) {
		task.depth = this.depth;
		if (task.origin == null) task.origin = this.origin;
		return _list.add(task);
	}

	@Override
	public boolean addAll(TaskQueue tasks) {		
		boolean result = false;
		
		for(BFTask task: tasks){
			task.depth = this.depth;
			if (task.origin == null) task.origin = this.origin;
			result = _list.add(task) || result; // never want a shortcut!
		}
		return result;
	}

	@Override
	public BFTask poll() {
		return _list.poll();
	}

	

}
