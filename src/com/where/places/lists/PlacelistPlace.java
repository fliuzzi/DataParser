package com.where.places.lists;

import java.io.Serializable;

import org.json.JSONObject;

import com.where.commons.feed.citysearch.CSListing;

public class PlacelistPlace implements Serializable {
	private static final long serialVersionUID = 2281331432885082245L;
	
	private String listingid;
	private String note;
	private String type;
	
	private CSListing poi;
	
	public PlacelistPlace(String listingid, String note) {
		this.listingid  = listingid;
		this.note = note;
	}
	
	public PlacelistPlace(String listingid, String note, String type) {
		this.listingid  = listingid;
		this.note = note;
		this.type = type;
	}
	
	public String getType() { 
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public String getListingid() {
		return listingid;
	}
	public void setListingid(String listingid) {
		this.listingid = listingid;
	}
	public String getNote() {
		return note;
	}
	public void setNote(String note) {
		this.note = note;
	}
	
	public String toString() {
		return listingid + "\n" + note;
	}
	
	public CSListing getPoi() {
		return poi;
	}

	public void setPoi(CSListing poi) {
		this.poi = poi;
	}

	public JSONObject toJSON() {
		return toJSON(false);
	}
	
	public JSONObject toJSON(boolean full) {
		try {
			JSONObject json = new JSONObject();
			json.put("poi", poi.toJSONSnippet());
			if(note != null) json.put("note", note);
			if(type != null) json.put("type", type);
			return json;
		}
		catch(Exception ex) {
			throw new IllegalStateException(ex);
		}
	}
	
	public static PlacelistPlace fromJSON(JSONObject json) {
		try {
			String listingid = json.getString("listingid");
			String note = json.optString("note", null);
			String type = json.optString("type", null);
			return new PlacelistPlace(listingid, note, type);
		}
		catch(Exception ex) {
			throw new IllegalStateException(ex);
		}
	}
}