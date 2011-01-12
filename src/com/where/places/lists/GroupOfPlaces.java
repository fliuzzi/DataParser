package com.where.places.lists;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class GroupOfPlaces implements Serializable {
	private static final long serialVersionUID = 2804176994321339547L;
	
	private static final String DEFAULT_GROUP_NAME = "My picks";
	
	private String groupName;
	private List<PlacelistPlace> entries = new ArrayList<PlacelistPlace>();
	
	public GroupOfPlaces(String groupName) {
		this.groupName = groupName;
	}
	
	public void addPlacelistEntry(String listingid, String note) {
		addPlacelistEntry(listingid, note, null);
	}
	
	public void addPlacelistEntry(String listingid, String note, String type) {
		PlacelistPlace entry = new PlacelistPlace(listingid, note, type);
		entries.add(entry);
	}
	
	public void addPlacelistEntry(PlacelistPlace entry) {
		entries.add(entry);
	}
	
	public String getName() {
		return groupName;
	}
	
	public List<PlacelistPlace> entries() {
		return entries;
	}
	
	public void removePlaces(List<String> listingids) {
		for(Iterator<PlacelistPlace> i = entries.iterator();i.hasNext();) {
			PlacelistPlace p = i.next();
			if(listingids.contains(p.getListingid())) {
				i.remove();
			}
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null || !(obj instanceof GroupOfPlaces)) return false;
		
		return ((GroupOfPlaces)obj).groupName.equals(groupName);
	}
	
	@Override
	public int hashCode() {
		return 17+37*groupName.hashCode();
	}
	
	public JSONObject toJSON(boolean full) {
		try {
			JSONObject json = new JSONObject();
			json.put("name", groupName);
			json.put("places", entries.size());
			
			if(full) {
				JSONArray e = new JSONArray();
				for(PlacelistPlace entry:entries) {
					e.put(entry.toJSON());
				}
				
				json.put("listings", e);
			}
			
			return json;
		}
		catch(Exception ex) {
			throw new IllegalStateException(ex);
		}
	}
	
	public static GroupOfPlaces fromJSON(JSONObject json, String listName) {
		try {
			String groupName = json.optString("name");
			if(groupName != null) groupName = groupName.trim();
			else groupName = DEFAULT_GROUP_NAME;
			
			if(groupName.toLowerCase().startsWith(listName.trim().toLowerCase())) {
				groupName = groupName.substring(listName.trim().length());
				//CS thing
				if(groupName.startsWith(":")) groupName = groupName.substring(1);
			}
			
			GroupOfPlaces group = new GroupOfPlaces(groupName);
			
			JSONArray listings = json.getJSONArray("listings");
			for(int i = 0, n = listings.length(); i < n; i++) {
				group.addPlacelistEntry(PlacelistPlace.fromJSON(listings.getJSONObject(i)));
			}
			return group;
		}
		catch(Exception ex) {
			throw new IllegalStateException(ex);
		}
	}
}
