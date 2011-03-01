package com.where.places.lists;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class CSListPlace implements Serializable {
	private static final long serialVersionUID = -3076392235360782173L;

	public static final String DEFAULT_GROUP_NAME = "default";
	
	private String id;
	private String name;
	private Author author;
	private String description;
	private long created;
	private long modified;
	
	private String source;
	private String sourceUrl;
	
	private List<GroupOfPlaces> groups = new ArrayList<GroupOfPlaces>();

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
	public Author getAuthor() {
		return author;
	}
	public void setAuthor(Author author) {
		this.author = author;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public long getCreated() {
		return created;
	}
	public void setCreated(long created) {
		this.created = created;
	}
	public long getModified() {
		return modified;
	}
	public void setModified(long modified) {
		this.modified = modified;
	}
	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source;
	}
	public String getSourceUrl() {
		return sourceUrl;
	}
	public void setSourceUrl(String sourceUrl) {
		this.sourceUrl = sourceUrl;
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
			if(modified == 0) modified = created;
			json.put("modified", modified);
			if(groupshow) {
				int size = 0;
				for(GroupOfPlaces group:groups) {
					size+=group.entries().size();
				}
				json.put("places", size);
			}
			
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
	
	public static CSListPlace fromJSON(String jsonstr) {
		try { return fromJSON(new JSONObject(jsonstr)); } catch (Exception e) { throw new IllegalStateException(e); }
	}
	
	public static CSListPlace fromJSON(JSONObject json) {
		try {
			CSListPlace list = new CSListPlace();
			list.setId(json.optString("id", null));
			list.setName(json.optString("name", null));
			if(json.has("created")) {
				list.setCreated(json.getLong("created"));
			} else list.setCreated(json.getLong("date"));
			list.setModified(json.optLong("modified"));
			if(json.has("author")) list.setAuthor(Author.fromJSON(json.getJSONObject("author")));
			list.setDescription(json.optString("description", null));
			
			JSONObject source = json.optJSONObject("source");
			if(source != null) {
				list.setSource(source.getString("name"));
				list.setSourceUrl(source.optString("url", null));
			}
			
			JSONArray groups = json.optJSONArray("groups");
			if(groups != null) {
				for(int i = 0, n = groups.length(); i < n; i++) {
					list.addGroup(GroupOfPlaces.fromJSON(groups.getJSONObject(i), list.getName()));
				}
			}
			
			return list;
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
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null || !(obj instanceof CSListPlace)) return false;
		
		CSListPlace p = (CSListPlace)obj;
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
}