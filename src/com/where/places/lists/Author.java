package com.where.places.lists;

import java.io.Serializable;

import org.json.JSONObject;

public class Author implements Serializable {
	private static final long serialVersionUID = 1632208808573486710L;
	
	private String id;
	private String name;
	private String photo;
	
	private int listCount;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPhoto() {
		return photo;
	}
	public void setPhoto(String photo) {
		this.photo = photo;
	}
	public int getListCount() {
		return listCount;
	}
	public void setListCount(int listCount) {
		this.listCount = listCount;
	}
	
	public JSONObject toJSON() {
		try {
			JSONObject json = new JSONObject();
			json.put("id", id);
			json.put("name", name);
			if(photo != null) json.put("photo", photo);
			if(listCount > 0) json.put("lists", listCount);
			
			return json;
		}
		catch(Exception ex) {
			throw new IllegalStateException(ex);
		}
	}
	
	public static Author fromJSON(JSONObject json) {
		try {
			Author author = new Author();
			author.setId(json.getString("id"));
			author.setName(json.getString("name"));
			author.setPhoto(json.optString("photo", null));
			author.setListCount(json.optInt("lists", 0));
			return author;
		}
		catch(Exception ex) {
			throw new IllegalStateException(ex);
		}
	}
	
	public String toString() {
		return name;
	}
}
