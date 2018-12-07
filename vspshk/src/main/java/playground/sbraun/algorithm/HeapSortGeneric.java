package playground.sbraun.algorithm;

/**
 * @author sbraun
 *
 */


//T should implement the interface Comparable
public class HeapSortGeneric <T extends Comparable<T>> {
	private T[] nodes;
	
	public HeapSortGeneric(T[] nodes){
		this.nodes = nodes;
	}
	
	public void buildHeap(){
		for (int i=nodes.length/2;i>=0;i--){
			heapify(i,nodes.length);
		}
	}
	
	private void heapify(int curIdx, int endIdx){
		int left = curIdx*2+1;
		int right = curIdx*2+2;
		int groesstes =curIdx;
		if (left< endIdx && nodes[groesstes].compareTo(nodes[left])<0) groesstes=left;
		if (right< endIdx && nodes[groesstes].compareTo(nodes[right])<0) groesstes=right;
		if (curIdx != groesstes){
			T tmp = nodes[curIdx];
			nodes[curIdx] = nodes[groesstes];
			nodes[groesstes] = tmp;
			heapify(groesstes,endIdx);
		}
	}
	
	
	
	public void heapSort(){
		int sorted = 0;
		int endIdx = nodes.length;
		while (sorted<nodes.length){
			T tmp = nodes[nodes.length-sorted-1];
			nodes[nodes.length-sorted-1] = nodes[0];
			nodes[0] = tmp;
			
			endIdx--;
			sorted++;
			heapify(0, endIdx);
		}
	}
}
