package com.where.data.parsers.citysearch;

import java.io.Serializable;

import org.json.JSONObject;

public class Location implements Serializable {
	private static final long serialVersionUID = 2777101785260916166L;

	private String address1;
	private String city;
	private String state;
	private String zip;
	
	private double lat = Double.NaN;
	private double lng = Double.NaN;
	
	public String getAddress1() {
		return address1;
	}
	public void setAddress1(String address1) {
		this.address1 = address1;
	}
	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
	}
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	public String getZip() {
		return zip;
	}
	public void setZip(String zip) {
		this.zip = zip;
	}
	public double getLat() {
		return lat;
	}
	public void setLat(double lat) {
		this.lat = lat;
	}
	public double getLng() {
		return lng;
	}
	public void setLng(double lng) {
		this.lng = lng;
	}
	
	public JSONObject toJSON() {
		try {
			JSONObject location = new JSONObject();
			
			if(getAddress1() != null) {
				location.put("street", getAddress1());
			}
			if(getCity() != null) {
				location.put("city", getCity());
			}
			if(getState() != null) {
				location.put("state", getState());
			}
			if(getZip() != null) {
				location.put("zip", getZip());
			}
			location.put("lat", getLat());
			location.put("lng", getLng());
			
			return location;
		}
		catch(Exception ex) {
			throw new IllegalStateException(ex);
		}
	}
}
