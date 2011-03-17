package com.where.data.parsers.citysearch;

import java.io.Serializable;

import org.json.JSONObject;

public class Category implements Serializable {
	private static final long serialVersionUID = 4974348956014571136L;
	
	public static final Group GROUP_PAYMENT_METHODS = new Group("152", "Payment Methods");

	private String name;
	private String id;
	
	private Category parent;
	private Group group;
	
	public Category(String id, String name) {
		this.id = id;
		this.name = name;
	}
	
	public Category getParent() {
		return parent;
	}

	public void setParent(Category parent) {
		this.parent = parent;
	}

	public Group getGroup() {
		return group;
	}

	public void setGroup(Group group) {
		this.group = group;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public boolean isPayment() {
		return getGroup() != null && getGroup().equals(GROUP_PAYMENT_METHODS);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null || !(obj instanceof Category)) return false;
		
		return ((Category)obj).id.equals(id);
	}
	
	@Override
	public String toString() {
		return id + "," + name + (parent != null ? "," + parent.id + "," + parent.name : ",,") + (group != null ? "," + group.toString() : ",,");
	}	
	
	@SuppressWarnings("unchecked")
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("id", id);
		json.put("name", name);
		if(group != null) {
			json.put("group", group.toJSON());
		}
		if(parent != null) {
			json.put("parent", parent.toJSON());
		}
		return json;
	}
	
	public static class Group implements Serializable {
		private static final long serialVersionUID = 9016475676535591923L;
		
		private String name;
		private String id;
		
		public Group(String id, String name) {
			this.id = id;
			this.name = name;
		}
		
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		@Override
		public int hashCode() {
			return id.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			if(obj == null || !(obj instanceof Group)) return false;
			
			return ((Group)obj).id.equals(id);
		}
		
		@Override
		public String toString() {
			return id + "," + name;
		}
		
		@SuppressWarnings("unchecked")
		public JSONObject toJSON() {
			JSONObject json = new JSONObject();
			json.put("id", id);
			json.put("name", name);
			return json;
		}
	}
}
