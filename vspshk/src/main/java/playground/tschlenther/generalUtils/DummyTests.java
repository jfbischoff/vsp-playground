package playground.tschlenther.generalUtils;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.Tuple;

import playground.tschlenther.parkingSearch.utils.ParkingTuple;
import playground.tschlenther.processing.testapplet;

public class DummyTests {
	
	public static void main(String[] argS){
//		testListIndex();
//		testDecimalFormat();
//		testDateFormat();
//		testCollectionModification();
//		testGregorianCalendar();
//		System.out.println(" ü ü ü");
//		testLocalDate();
		testStringSplit();
	}
	
	private static void testDateFormat(){
		String now = new SimpleDateFormat("ddMM").format(new Date());
		System.out.println(now);
		now = new SimpleDateFormat("ddMMyy").format(new Date());
		System.out.println(now);
		now = new SimpleDateFormat("ddMMyy_HH.mm").format(new Date());
		System.out.println(now);
	}
	
	private static void testListIndex(){
		List<String> ll = new ArrayList<String>();
		ll.add("A");
		ll.add("B");
		ll.add("C");
		ll.add("D");
		ll.add("E");
		
		System.out.println("size of list: " +  ll.size() + "\n last index of B: " + ll.lastIndexOf("B") );
		System.out.println("last index of E: " + ll.lastIndexOf("E") + "\n \n" + ll.toString());
		
		
		HashSet set = new HashSet();
		
		System.out.println("---- now testing HashSet----");
		System.out.println("set size should be 0 and is : " + set.size());
		set.add("a");
		System.out.println("set size should be 1 and is : " + set.size());
		set.add("b");
		System.out.println("set size should be 2 and is : " + set.size());
		
		
		String pathToZoneFile = "blablabla/basjfkj/runs/meineZone.txt";
		String zone = pathToZoneFile.substring(pathToZoneFile.lastIndexOf("/")+1, pathToZoneFile.lastIndexOf("."));
		System.out.println(zone);
		
		System.out.println("--");
	
		System.out.println("Viertelstunde:" + (int) (39551/900) );
		System.out.println("halbe stunde:" + (int) (39551/1800) );
		System.out.println("Stunde:" + (int) (39551/3600) );
		
		
		
		
	}

	private static void testArray(){
		double[] arr1 = new double[3];
		
		arr1[0] = 1;
		arr1[1] = 2;
		arr1[2] = 3;
		System.out.println("\n ARRAY \n" + printarray(arr1));
		
		System.out.println("länge des arrays= " + arr1.length);
		System.out.println("letztes element= " + arr1[arr1.length-1]);

		double[] arr2 = new double[1];
		System.out.println("\n ARRAY \n" + printarray(arr2));
		System.out.println("länge des arrays= " + arr2.length);
		System.out.println("letztes element= " + arr2[arr2.length-1]);

		arr2[0] = 1;
		
		System.out.println("\n ARRAY \n" + printarray(arr2));
		
		
	}
	
	private static  void testDecimalFormat(){
		double x = 389;
		double y = 0.8298;
		double z = 23.61599;
		
		DecimalFormat df = new DecimalFormat("##.##");
		
		System.out.println("x = " + df.format(x) + "\t y= " + df.format(y) + "\t z= " + df.format(z) );
	}
	
	private static void testTreeSet(){
		System.out.println("--------------------TREEEEESEEEEEET------------------");
		
		TreeSet treeSet = new TreeSet();
		treeSet.add(1.0d);
		treeSet.add(4.3d);
		treeSet.add(4.1d);
		treeSet.add(5.0d);
		
		System.out.println("Treeset:\n" + treeSet.toString());
		TreeSet pSet = new TreeSet<ParkingTuple>();
		pSet.add(new ParkingTuple(1000,0));
		pSet.add(new ParkingTuple(500,2));
		pSet.add(new ParkingTuple(10000,1));
		pSet.add(new ParkingTuple(3000,0.6));
		
		System.out.println("\n\n pSet :\n" + pSet.toString());

	}
	
	private static String printarray(double[] arr){
		String str = "";
		for(int i = 0; i< arr.length; i++){
			str += "" + i + ":" + arr[i] + "\n";
		}
		return str;
	}
	
	private static void testCollectionModification(){
		List<Tuple<Coord,Double>> freeSlots = new ArrayList<Tuple<Coord,Double>>();

		freeSlots.add(new Tuple<Coord, Double>(new Coord(0, 0), 1.0));
		freeSlots.add(new Tuple<Coord, Double>(new Coord(1, 0), 2.0));
		freeSlots.add(new Tuple<Coord, Double>(new Coord(0, 1), 3.0));
		
		System.out.println(freeSlots.toString());
		List<Tuple<Coord,Double>> nList = new ArrayList<Tuple<Coord,Double>>();
		for (Tuple<Coord, Double> t : freeSlots){ 
			Coord c = t.getFirst();
			nList.add(new Tuple<Coord, Double>(c,0.0));
		}
		System.out.println(freeSlots.toString());
		System.out.println(nList.toString());
		
		Map<String,Double> map = new HashMap<String,Double>();
		map.put("eins", 1.0);
		map.put("zwei", 2.0);
		map.put("drei", 3.0);
		map.put("vier", 4.0);
		
		
		for(String key: map.keySet()){
			System.out.println("Vorher");
			Double d = map.get(key);
			System.out.println(key + ":" + d);
			map.put(key, d-1);
		}
		
		for(String key: map.keySet()){
			System.out.println("Nachher");
			Double d = map.get(key);
			System.out.println(key + ":" + d);
		}
		
	}
	
	private static void testGregorianCalendar(){
		SimpleDateFormat f  = new SimpleDateFormat("yyMMdd");
		String tag = "170805";
		
		Calendar cl = Calendar.getInstance();
		cl.clear();
		try {
			cl.setTime(f.parse(tag));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		cl.set(Calendar.DAY_OF_WEEK, cl.getFirstDayOfWeek());
		
		System.out.println("Jahr: " + cl.get(Calendar.YEAR));
		System.out.println("Monat: " + cl.get(Calendar.MONTH));
		System.out.println("Tag: " + cl.get(Calendar.DAY_OF_MONTH));
		System.out.println("Wochentag: " + cl.get(Calendar.DAY_OF_WEEK));
		System.out.println("Monatszahl Januar: " + cl.get(Calendar.JANUARY));
		System.out.println("first weekday: " + cl.get(cl.getFirstDayOfWeek()));
		System.out.println("Montags zahl: " + Calendar.MONDAY);
	}
	
	private static void testLocalDate(){
		System.out.println(" -- testing LocalDate -- ");
		
		LocalDate date = LocalDate.of(2014, 01, 06);
		System.out.println("Datum: " + date.toString());
		System.out.println("Wochentag: " + date.getDayOfWeek().getValue());
		System.out.println("Jahr: " + date.getYear());
	}
			

	private static void testStringSplit(){
		String str = "141101i01:00  285-   37-   76-    0-   97-    0-  344-   22-    0-  239-    9-    0-    0-    4-    8-   25-    0-    0-   72-    3-    0-    0-    0-    0-    0-    1-    0-   94-    3-    0-    0-    0-    0-    0-    0-    0-  300-   17-    1-    1-    2-    7-   12-    4-";
		System.out.println("char at index 6: " + str.charAt(6));
		if(str.charAt(6) == 'i'){
			System.out.println("statuskennung ist i");
		} else{
			System.out.println("statuskennung ist cool");
		}
		String[] tokens = str.split("\\s+");
	
		String str2 = "141101 15:00  833-   22-  960-    7-  603-    1-  381-    0- 1131-    8- 1290-   13-    3-  752-   48-    6-    3-   11-    2-    6-    2-    6-  911-   31-    3-    3-    4-    0-    0-    2-    1-  557-   30-    1-    0-    1-    0-    0-   13-    3-  373-    1-    0-    0-    0-    0-    0-    4-    8- 1077-   33-    3-    1-    5-    0-    2-    2-   15- 1213-   42-    5-    0-    8-    0-    5-    2-";
		String[] tokens2 = str.split("\\s+");
		int i = 0;
		for(String s : tokens){
			System.out.println("" + i + ": " + s);
			i++;
		}
		
	}
}
