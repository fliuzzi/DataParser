package com.where.data.parsers.citysearch;

import java.io.Serializable;

import org.json.JSONObject;

public class RatingStat implements Serializable {
	private static final long serialVersionUID = -5278439922671940876L;
	
	private int starRating;
	private int count;
	private int percentage;
	
	public RatingStat(int starRating) {
		this.starRating = starRating;
	}
	
	public int getStarRating() {
		return starRating;
	}
	public void setStarRating(int starRating) {
		this.starRating = starRating;
	}
	public int getCount() {
		return count;
	}
	public void setCount(int count) {
		this.count = count;
	}
	public int getPercentage() {
		return percentage;
	}
	public void setPercentage(int percentage) {
		this.percentage = percentage;
	}
	
	public JSONObject toJSON() {
		try {
			JSONObject json = new JSONObject();
			json.put("stars", starRating);
			json.put("count", count);
			json.put("percentage", percentage);
			return json;
		}
		catch(Exception ex) {
			throw new IllegalStateException(ex);
		}
	}
	
	public void fromJSON(JSONObject json) {
		try {
			starRating = json.getInt("stars");
			count = json.getInt("count");
			percentage = json.getInt("percentage");
		}
		catch(Exception ex) {
			throw new IllegalStateException(ex);
		}
	}
}
