package com.where.places.lists;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.where.utils.Utils;

public final class Placelist implements Serializable {
	private static final long serialVersionUID = -3076392235360782173L;

	public static final String DEFAULT_GROUP_NAME = "default";
	
	private final String id;
	private final String name;
	private final Author author;
	private final String description;
	private final long created;
	private final long modified;
	private final boolean ispublic;
	
	private final String source;
	private final String sourceUrl;
	
	private final List<GroupOfPlaces> groups;
	
	public Placelist(Builder builder)
	{
		this.id = builder.id;
		this.name = builder.name;
		this.author = builder.author;
		this.description = builder.description;
		this.created = builder.created;
		this.modified = builder.modified;
		this.ispublic = builder.ispublic;
		this.source = builder.source;
		this.sourceUrl = builder.sourceUrl;
		
		this.groups = builder.groups;
		
	}
	
	public static class Builder extends com.where.util.common.AbstractBuilder<Placelist>
	{
		private String id;
		private String name;
		private Author author;
		private String description;
		private Long created;
		private Long modified;
		private boolean ispublic = true;
		
		private String source;
		private String sourceUrl;
		
		private List<GroupOfPlaces> groups = new ArrayList<GroupOfPlaces>();
		
		public Builder()
		{
			super(Placelist.class);
		}
		public Builder(Placelist list)
		{
			super(Placelist.class);
			this.id= list.id;
			this.name = list.name;
			this.author = list.author;
			this.description = list.description;
			this.created = list.created;
			this.modified = list.modified;
			this.source = list.source;
			this.sourceUrl = list.sourceUrl;
			this.ispublic = list.ispublic;
		}
		
		@Override
		public Placelist build()
		{
			if(created == null || created == 0) created = System.currentTimeMillis();
			if(modified == null || modified == 0) modified = created;
			
			return new Placelist(this);
		}
		
		public Builder ispublic(boolean ispublic) { this.ispublic = ispublic; return this; }
		public Builder id(String id) { this.id = id; return this; }
		public Builder name(String name) { this.name = name; return this; }
		public Builder author(Author author) { this.author = author; return this; }
		public Builder description(String description) { this.description = description; return this; }
		public Builder created(long created) { this.created = created; return this; }
		public Builder modified(long modified) { this.modified = modified; return this; }
		
		public Builder source(String source) { this.source = source; return this; }
		public Builder sourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; return this; }
		
		public Builder groups(List<GroupOfPlaces> groups) { this.groups = groups; return this; } 	
	
		public void removeEmptyGroups() {
			for(Iterator<GroupOfPlaces> i = groups.iterator(); i.hasNext();) {
				GroupOfPlaces g = i.next();
				if(g.entries().isEmpty()) i.remove();
			}
		}
		
		public void addToGroup(String groupName, String listingid, String note) {
			addToGroup(groupName, listingid, note, null);
		}
		
		public void addToGroup(String groupName, String listingid, String note, String type) {
			if(groupName == null) groupName = DEFAULT_GROUP_NAME;
			
			GroupOfPlaces group = new GroupOfPlaces(groupName);
			int index = groups.indexOf(group);
			if(index > -1) group = groups.get(index);
			else groups.add(group);
			
			group.addPlacelistEntry(listingid, note);
		}
		
		public void addToGroup(String groupName, PlacelistPlace entry) {
			if(groupName == null) groupName = DEFAULT_GROUP_NAME;
			
			GroupOfPlaces group = new GroupOfPlaces(groupName);
			int index = groups.indexOf(group);
			if(index > -1) group = groups.get(index);
			else groups.add(group);
			
			group.addPlacelistEntry(entry);
		}
		
		public void addGroup(GroupOfPlaces group) {
			groups.add(group);
		}
		
		public void addGroup(int index, GroupOfPlaces group) {
			groups.add(index, group);
		}	

		public String id() { return this.id; }
		public String name() { return this.name; }
		public Author author() { return this.author; }
		public String sourceUrl() { return this.sourceUrl; }
		public boolean isPublic() { return this.ispublic; }
	}
	
	public boolean isPublic() {
		return ispublic;
	}

	public String getId() {
		return id;
	}
	public String getName() {
		return name;
	}
	public Author getAuthor() {
		return author;
	}
	public String getDescription() {
		return description;
	}
	public long getCreated() {
		return created;
	}
	public long getModified() {
		return modified;
	}
	public String getSource() {
		return source;
	}
	public String getSourceUrl() {
		return sourceUrl;
	}
	
	public List<PlacelistPlace> places() 
	{
		List<PlacelistPlace> lPlaces = new LinkedList<PlacelistPlace>();
		for(GroupOfPlaces g : groups)
		{
			lPlaces.addAll(g.entries());
		}
		return lPlaces;
	}
	
	public int getPlacesCount()
	{
		return calculateGroupsSize();
	}
	
	public List<GroupOfPlaces> groups() { return groups; }
	
	public int groupsSize() {
		return groups.size();
	}
	
	public GroupOfPlaces group(int index) {
		return groups.get(index);
	}
	
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		if(!groups.isEmpty()) {
			for(GroupOfPlaces group:groups) {
				buffer.append("\n");
				buffer.append(group.getName() + " " + group.entries().size());
				buffer.append("\n");
				buffer.append("=======");
				buffer.append("\n");
				for(PlacelistPlace listentry:group.entries()) {
					buffer.append(listentry);
					buffer.append("\n");
				}
			}
		}
		return id + " " + name + " by " + author + " on " + created + "\n" + description + "\n" + buffer.toString();
	}
	
	public JSONObject toJSON() {
		return toJSON(false);
	}
	
	public JSONObject toJSON(boolean full) {
		return toJSON(full, true);
	}
	
	public JSONObject toJSON(boolean full, boolean groupshow) {
		try {
			JSONObject json = new JSONObject();
			if(id != null) json.put("id", id);
			json.put("name", name);
			if(author != null) json.put("author", author.toJSON());
			json.put("created", created);
			json.put("modified", modified);
			if(groupshow) {
				int size = 0;
				for(GroupOfPlaces group:groups) {
					size+=group.entries().size();
				}
				json.put("places", size);
			}
			json.put("ispublic", ispublic);
			
			if(description != null) json.put("description", description);
			
			if(source != null) {
				JSONObject s = new JSONObject();
				s.put("name", source);
				if(sourceUrl != null) s.put("url", sourceUrl);
				json.put("source", s);
			}
			
			if(groupshow) {
				JSONArray e = new JSONArray();
				for(GroupOfPlaces group:groups) {
					e.put(group.toJSON(full));
				}
				
				json.put("groups", e);
			}
			
			return json;
		}
		catch(Exception ex) {
			throw new IllegalStateException(ex);
		}
	}
	public static Placelist fromJSON(JSONObject json) {
		return fromJSON(json, false);
	}
	public static Placelist fromJSON(JSONObject json, boolean idFromSource) {
		try {
			Placelist.Builder list = new Placelist.Builder()
				.name(json.optString("name", ""))
				.modified(json.optLong("modified", 0))
				.description(json.optString("description", null));
			
			list.ispublic(json.optBoolean("ispublic", true));
			
			if(json.has("author")) {
				list.author(Author.fromJSON(json.getJSONObject("author")));
			}
			
			if(json.has("created")) {
				list.created(json.optLong("created"));
			}
			else { 
				list.created(json.optLong("date"));
			}
			JSONObject source = json.optJSONObject("source");
			if(source != null) {
				list.source(source.getString("name"));
				list.sourceUrl(source.optString("url", null));
			}
			
			JSONArray groups = json.optJSONArray("groups");
			if(groups != null) {
				for(int i = 0, n = groups.length(); i < n; i++) {
					list.addGroup(GroupOfPlaces.fromJSON(groups.getJSONObject(i), list.name()));
				}
			}

			list.id(idFromSource ? Utils.hash(list.sourceUrl()) : json.optString("id", null));
			
			return list.build();
		}
		catch(Exception ex) {
			throw new IllegalStateException(ex);
		}
	}
	
	public String toTokens() {
		StringBuffer buffer = new StringBuffer();
		for(GroupOfPlaces group:groups) {
			if(!group.getName().equals(DEFAULT_GROUP_NAME)) {
				buffer.append(group.getName().trim());
				buffer.append(" ");
			}
		}
		return name.trim() + " " + buffer.toString();
	}
	
	public void removeEmptyGroups() {
		for(Iterator<GroupOfPlaces> i = groups.iterator(); i.hasNext();) {
			GroupOfPlaces g = i.next();
			if(g.entries().isEmpty()) i.remove();
		}
	}
	
	public void addToGroup(String groupName, String listingid, String note) {
		addToGroup(groupName, listingid, note, null);
	}
	
	public void addToGroup(String groupName, String listingid, String note, String type) {
		if(groupName == null) groupName = DEFAULT_GROUP_NAME;
		
		GroupOfPlaces group = new GroupOfPlaces(groupName);
		int index = groups.indexOf(group);
		if(index > -1) group = groups.get(index);
		else groups.add(group);
		
		group.addPlacelistEntry(listingid, note, type);
	}
	
	public void addToGroup(String groupName, PlacelistPlace entry) {
		if(groupName == null) groupName = DEFAULT_GROUP_NAME;
		
		GroupOfPlaces group = new GroupOfPlaces(groupName);
		int index = groups.indexOf(group);
		if(index > -1) group = groups.get(index);
		else groups.add(group);
		
		group.addPlacelistEntry(entry);
	}
	
	public void addGroup(GroupOfPlaces group) {
		groups.add(group);
	}
	
	public void addGroup(int index, GroupOfPlaces group) {
		groups.add(index, group);
	}	
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null || !(obj instanceof Placelist)) return false;
		
		Placelist p = (Placelist)obj;
		return p.id.equals(id);
	}
	
	@Override
	public int hashCode() {
		return 17+37*id.hashCode();
	}
	
	private static void run(String command) throws Exception {
		Runtime runtime = Runtime.getRuntime();
	    Process process = runtime.exec(command);
	    java.io.InputStream is = process.getInputStream();
	    java.io.InputStreamReader isr = new java.io.InputStreamReader(is);
	    java.io.BufferedReader br = new java.io.BufferedReader(isr);
	       String line;

	       while ((line = br.readLine()) != null) {
	         System.out.println(line);
	       }
	       br.close();
	       process.waitFor();
	}
	
	public static void main(String[] args) throws Exception {
		String command = "java -jar /Users/imitrovic/Documents/workspace/80legsResultDownloader/80legsResultDownloader.jar -token 4daa797db523087a3b491180c67eb2a3 -analysisOnly 1 -unzip 1 -downloadLocation /Users/imitrovic/Documents/workspace/80legsResultDownloader/download -cpId 16 -jobId ";
		long[] jobs = new long[] {72443,72470,72483,72498,72526,72555,72573,72590,72605,72632,72661,72688,72713,72753,72780,72821,72859,72898,72949,73022,73043,73081,73122,73154,73181,73216,73253,73286,73311,73373,73415,73466,73545,73870,73889,73905,73972,74055,74091,74123,74144,74157,74171,74197};
		for(long job:jobs) {
			run(command+job);
		}
	}
	
	public int calculateGroupsSize() {
		int size = groupsSize();
		int count = 0;
		for(int i = 0; i < size; i++) {
			count += group(i).entries().size();
		}
		return count;
	}
}