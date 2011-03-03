package com.where.data.parsers.cslists;
//generates the josn
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class CSListWriter {
	private CSListWriter() {}
	
	private static Map<String, Integer> authormap = new HashMap<String, Integer>();
	
	private static JSONObject readCSListsFromFile(String path) throws Exception {
		BufferedReader reader = new BufferedReader(new FileReader(path));
		String line = null;
		StringBuffer buffer = new StringBuffer();
		while((line = reader.readLine()) != null) {
			buffer.append(line);
		}
		reader.close();
		
		return new JSONObject(buffer.toString());
	}
	
	private static void loadAllCSLists(String path) throws Exception {
		Integer one = new Integer(1);
		
		JSONObject all = new JSONObject();
		JSONArray alllists = new JSONArray();
		for(int i = 0, n = 19; i < n; i++) {
			String file = path + "/list" + i + ".json";
			JSONObject json = readCSListsFromFile(file);
			JSONArray lists = json.getJSONArray("lists");
			for(int j = 0, k = lists.length(); j < k; j++) {
				JSONObject list = lists.getJSONObject(j);
				JSONObject author = list.getJSONObject("author");
				String authorid = author.getString("id");
				Integer count = authormap.get(authorid);
				if(count == null) count = one;
				else count = new Integer(count.intValue()+1);
				authormap.put(authorid, count);
				alllists.put(list);
			}
		}
		
		for(int i = 0, n = alllists.length(); i < n; i++) {
			JSONObject list = alllists.getJSONObject(i);
			JSONObject author = list.getJSONObject("author");
			String authorid = author.getString("id");
			Integer count = authormap.get(authorid);
			author.put("lists", count.intValue());
		}
		
		all.put("lists", alllists);
		
		System.out.println(alllists.length() + " lists");
		
		PrintWriter writer = new PrintWriter(new FileWriter(path + "/cslists.json"));
		writer.println(all.toString());
		writer.close();
	}
	
	public static void main(String[] args) throws Exception {
		String path = args[0];
		loadAllCSLists(path);
//		BufferedReader reader = new BufferedReader(new FileReader(new java.io.File("/Users/imitrovic/Desktop/cslists.json")));
//		StringBuffer buffer = new StringBuffer();
//		String line = null;
//		while((line = reader.readLine()) != null) {
//			buffer.append(line);
//		}
//		reader.close();
//		
//		JSONObject l = new JSONObject(buffer.toString());
//		JSONArray lists = l.getJSONArray("lists");
//		
//		reader = new BufferedReader(new FileReader(new java.io.File("/Users/imitrovic/Desktop/lists.json")));
//		buffer = new StringBuffer();
//		line = null;
//		while((line = reader.readLine()) != null) {
//			buffer.append(line);
//		}
//		reader.close();
//		
//		JSONObject l1 = new JSONObject(buffer.toString());
//		JSONArray lists1 = l1.getJSONArray("lists");
//		
//		for(int i = 0, n = lists1.length(); i < n; i++) {
//			JSONObject j = lists1.getJSONObject(i);
//			lists.put(j);
//		}
//		
//		PrintWriter writer = new PrintWriter(new FileWriter("/Users/imitrovic/Desktop/cslists1.json"));
//		writer.println(l.toString());
//		writer.close();
//		
//		reader = new BufferedReader(new FileReader(new java.io.File("/Users/imitrovic/Desktop/cslists1.json")));
//		buffer = new StringBuffer();
//		line = null;
//		while((line = reader.readLine()) != null) {
//			buffer.append(line);
//		}
//		reader.close();
//		
//		JSONObject l2 = new JSONObject(buffer.toString());
//		JSONArray lists2 = l2.getJSONArray("lists");
//		
//		System.out.println(lists2.get(0));
//		System.out.println(lists2.get(lists2.length()-1));
	}
}
