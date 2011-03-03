package com.where.data.parsers.citysearch;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Placelist implements Serializable {
	private static final long serialVersionUID = -1012494606105927332L;

	private String name;
	private String description;
	private String listUrl;
	private int placeCount;
		
	private List<CSListing> list = new ArrayList<CSListing>();
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getListUrl() {
		return listUrl;
	}
	public void setListUrl(String listUrl) {
		this.listUrl = listUrl;
	}
	
	public void addAll(List<CSListing> list) {
		this.list.addAll(list);
	}
	public void add(CSListing poi) {
		list.add(poi);
	}
	
	public List<CSListing> places() {
		return list;
	}
	public int getPlaceCount() {
		return placeCount;
	}
	public void setPlaceCount(int placeCount) {
		this.placeCount = placeCount;
	}
	@Override
	public int hashCode() {
		return listUrl.hashCode();
	}
	@Override
	public boolean equals(Object obj) {
		if(obj == null || !(obj instanceof Placelist)) return false;
		
		return listUrl.equals(((Placelist)obj).listUrl);
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		
		json.put("name", name);
		
		if(description != null) {
			json.put("description", description);
		}
		
		json.put("url", listUrl);
		
		JSONArray pois = new JSONArray();
		for(CSListing poi:list) {
			pois.add(poi.toJSONSnippet());
		}
		json.put("places", pois);
		
		json.put("count", placeCount);
		
		return json;
	}
}
