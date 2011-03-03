package com.where.data.parsers.localeze;

import java.io.Serializable;

import org.json.JSONObject;

public class AttributeType implements Serializable {
	private static final long serialVersionUID = 3594881778580054923L;
	
	private Long id;
	private String name;
	
	public AttributeType(Long id, String name) {
		this.id = id;
		this.name = name;
	}
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public int hashCode() {
		return id.intValue() + 17*name.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null || !(obj instanceof AttributeType)) return false;
		
		return ((AttributeType)obj).id.equals(id);
	}
	
	@Override
	public String toString() {
		return "AttributeType: " + id + "," + name;
	}
	
	public JSONObject toJSON() {
		try {
			JSONObject json = new JSONObject();
			json.put("id", id.longValue());
			json.put("name", name);
			return json;
		}
		catch(Exception ex) {
			throw new IllegalStateException(ex);
		}
	}
	
	public void fromJSON(JSONObject json) {
		try {
			id = new Long(json.getLong("id"));
			name = json.getString("name");
		}
		catch(Exception ex) {
			throw new IllegalStateException(ex);
		}	
	}
}