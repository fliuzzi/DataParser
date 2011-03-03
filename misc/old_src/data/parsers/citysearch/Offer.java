package com.where.data.parsers.citysearch;

import java.io.Serializable;

import org.json.simple.JSONObject;

public class Offer implements Serializable {
	private static final long serialVersionUID = -9120769100071348691L;
	
	private String name;
	private String text;
	private String description;
	private String url;
	private String expirationDate;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getExpirationDate() {
		return expirationDate;
	}
	public void setExpirationDate(String expirationDate) {
		this.expirationDate = expirationDate;
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		
		if(name != null) {
			json.put("name", name);
		}
		
		if(text != null) {
			json.put("text", text);
		}
		
		if(description != null) {
			json.put("description", description);
		}
		
		if(expirationDate != null) {
			json.put("expirationDate", expirationDate);
		}
		
		if(url != null) {
			json.put("url", url);
		}
		
		return json;
	}
}
