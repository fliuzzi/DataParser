package com.where.data.parsers.localeze;

import java.io.Serializable;

import org.json.JSONObject;

public class Category implements Serializable {
	private static final long serialVersionUID = -4280490216689190223L;
	
	private static final long SUB_SUBCATEGORY_STEM = 1000;
	private static final long SUBCATEGORY_STEM = 1000000;
	
	private Long id;
	private String name;
	
	private Category parent;
	
	public Category(Long id, String name) {
		this.id = id;
		this.name = name;
	}
	
	protected Category() {}
	
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
	
	public void setParent(Category parent) {
		this.parent = parent;
	}
	
	public Category getParent() {
		return parent;
	}
	
	protected Long stemKey() {
		return new Long((id.longValue()/SUBCATEGORY_STEM)*SUBCATEGORY_STEM);
	}
	
	protected Long subStemKey() {
		return new Long((id.longValue()/SUB_SUBCATEGORY_STEM)*SUB_SUBCATEGORY_STEM);
	}
	
	public boolean isParent(Category cat) {
		if(equals(cat)) return false;
		
		long stem = (id.longValue()/SUBCATEGORY_STEM)*SUBCATEGORY_STEM;
		
		return stem == cat.id.longValue();
	}
	
	public boolean isChild(Category cat) {
		return cat.isParent(this);
	}
	
	@Override
	public int hashCode() {
		return id.intValue() + 17*name.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null || !(obj instanceof Category)) return false;
		
		return ((Category)obj).id.equals(id);
	}
	
	@Override
	public String toString() {
		return "Category: " + id + "," + name + (parent != null ? " Parent: " + parent.toString() : "");
	}
	
	public JSONObject toJSON() {
		try {
			JSONObject json = new JSONObject();
			json.put("id", id.longValue());
			json.put("name", name);
			if(parent != null) {
				json.put("parent", parent.toJSON());
			}
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
			
			JSONObject jsonparent = json.optJSONObject("parent");
			if(jsonparent != null) {
				parent = new Category();
				parent.fromJSON(jsonparent);
			}
		}
		catch(Exception ex) {
			throw new IllegalStateException(ex);
		}	
	}
}