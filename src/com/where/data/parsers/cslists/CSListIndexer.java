//CSListIndexer.java
//indexes the cslist json

package com.where.data.parsers.cslists;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.spatial.geohash.GeoHashUtils;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.json.JSONArray;
import org.json.JSONObject;

import com.where.commons.feed.citysearch.CSListing;
import com.where.commons.feed.citysearch.search.Analyzer;
import com.where.commons.feed.citysearch.search.query.Profile;
import com.where.commons.util.LocationUtil;
import com.where.places.lists.GroupOfPlaces;
import com.where.places.lists.CSListPlace;
import com.where.places.lists.PlacelistPlace;
import com.where.utils.CSListingUtil;
import com.where.utils.Utils;


/*
 * CSListIndexer indexes JSON -> CSLists Index & Places Index
 *                          CSLists Index
 *                              Fields:
 *                                      id - hashed from pl URL
 *                                      lat and lat-range
 *                                      long and long-range
 *                                      PlaceList object
 *                          Places Index
 *                              Fields:
 *                                      id - CS listing id
 *                                      placelists - Arraylist of the placelist hashes
 */

public class CSListIndexer {
	private static Profile profile = new Profile();
	
	public static final String PLACELIST = "placelist";
	public static final String ID = "id";
	public static final String META = "meta";
	public static final String LATITUDE = "latitude";
	public static final String LONGITUDE = "longitude";
    public static final String LATITUDE_RANGE = "latitude_range";
    public static final String LONGITUDE_RANGE= "longitude_range";

	public static final String GEOHASH = "geohash";
	public static final String TIMESTAMP = "timestamp";
	
	public static final String PLACE_ID = "id";
	public static final String PLACELISTS = "placelists";
	
	private static Map<String, List<String>> badpois = new HashMap<String, List<String>>();
	
	//temporary
	private static Set<String> indexed = new HashSet<String>();
	
	private static Map<String, Set<CSListPlace>> inversed = new HashMap<String, Set<CSListPlace>>();
	
	private CSListIndexer() {}
	
	private static void indexCSLists(String cslistFile, String indexPath, String dymPath) throws Exception {
		IndexWriter writer = newIndexWriter(indexPath);
		indexPlacelists(cslistFile, writer, dymPath);
		writer.optimize();
		writer.close();
		
		
		
		// write the bad POIs to a txt...
		PrintWriter out = new PrintWriter(new FileWriter(new File(new File(indexPath).getParent(), "badpois.txt")));
		for(Map.Entry<String, List<String>> entry:badpois.entrySet()) {
			out.println(entry.getKey());
			for(String s:entry.getValue()) {
				out.println(s);
			}
		}
		out.close();
		
		indexPlaceLookups(indexPath);
		
		System.out.println("Bad " + badpois.size());
	}
	
	
	//indexes the cslists/places Index
	private static void indexPlaceLookups(String indexPath) throws Exception {
		IndexWriter writer = newIndexWriter(indexPath + "/places");
		for(Map.Entry<String, Set<CSListPlace>> entry:inversed.entrySet()) {
		    
		    
		    //placeID = key of entry map
			String placeid = entry.getKey();
			
			
			Set<CSListPlace> lists = entry.getValue();
			Document document = new Document();
			document.add(new Field(PLACE_ID, placeid, Field.Store.YES, Field.Index.NOT_ANALYZED));
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			ObjectOutputStream oout = new ObjectOutputStream(bout);
			List<CSListPlace> lists1 = new ArrayList<CSListPlace>(lists);
			Collections.sort(lists1, new Comparator<CSListPlace>() {
				@Override
				public int compare(CSListPlace o1, CSListPlace o2) {
					long diff = o2.getCreated() - o1.getCreated();
					
					if(diff > 0) return 1;
					if(diff == 0) return 0;
					return -1;
				}
			});
			List<String> ids = new ArrayList<String>();
			for(CSListPlace l:lists1) {
				ids.add(l.getId());
			}
			oout.writeObject(ids);
			oout.close();
			byte[] bytes = bout.toByteArray();
			document.add(new Field(PLACELISTS, bytes, 0, bytes.length, Field.Store.YES));
			writer.addDocument(document);
		}
		writer.optimize();
		writer.close();
	}
	
	private static List<Document> newPlacelistDocuments(CSListPlace placelist) throws Exception {
		List<Document> docs = new ArrayList<Document>();
		
		//already indexed, to remove this
		String lid = placelist.getSourceUrl().substring(placelist.getSourceUrl().lastIndexOf("/"));
		if(indexed.contains(lid)) return docs;
		
		indexed.add(lid);
		
		//POI lookup failed
		if(!setPOIs(placelist)) return docs;
		
//		for(int i = 0, n = placelist.groupsSize(); i < n; i++) {
//			GroupOfPlaces group = placelist.group(i);
//			for(PlacelistPlace place:group.entries()) {
//				docs.add(newPlacelistDocument(placelist, lat, lng));
//			}
//		}
		
		//just for now although we'll cluster for national lists that span multiple cities
		CSListing poi = placelist.group(0).entries().get(0).getPoi();
		docs.add(newPlacelistDocument(placelist, poi.getAddress().getLat(), poi.getAddress().getLng()));
		
		addInverseLookup(placelist);

		return docs;
	}
	
	
	
	private static void addInverseLookup(CSListPlace placelist) {
		for(int i = 0, n = placelist.groupsSize(); i < n; i++) {
			GroupOfPlaces group = placelist.group(i);
			List<PlacelistPlace> places = group.entries();
			for(PlacelistPlace place:places) {
				Set<CSListPlace> lookupLists = inversed.get(place.getListingid());
				if(lookupLists == null) {
					lookupLists = new HashSet<CSListPlace>();
					inversed.put(place.getListingid(), lookupLists);
				}
				lookupLists.add(placelist);
			}
		}
	}
	
	private static Document newPlacelistDocument(CSListPlace placelist, double lat, double lng) throws Exception {
		Document document = new Document();
		
		document.add(new Field(ID, placelist.getId(), Field.Store.YES, Field.Index.NOT_ANALYZED));
		
		//Is this the best way or should be broken into fields I.M.
		document.add(new Field(META, placelist.toTokens(), Field.Store.NO, Field.Index.ANALYZED));
		//add location (city, state, neighborhood etc. from pois that make list)
		addPlacelistDocument(placelist, document);
		
		NumericField timestamp = new NumericField(TIMESTAMP);
		timestamp.setLongValue(placelist.getCreated());
		document.add(timestamp);
		
		document.add(new Field(LATITUDE, LocationUtil.formatCoord(lat, 1), Field.Store.NO, Field.Index.ANALYZED));
		document.add(new Field(LONGITUDE, LocationUtil.formatCoord(lng, 2), Field.Store.NO, Field.Index.ANALYZED));
        document.add(new NumericField(LATITUDE_RANGE,  Store.YES,true).setDoubleValue(lat));  
        document.add(new NumericField(LONGITUDE_RANGE, Store.YES,true).setDoubleValue(lng));
		
		String geohash = GeoHashUtils.encode(lat, lng);
		document.add(new Field(GEOHASH, geohash, Field.Store.NO, Field.Index.NOT_ANALYZED));
		
		return document;
	}
	
	private static void addPlacelistDocument(CSListPlace placelist, Document document) {
		try {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			ObjectOutputStream oout = new ObjectOutputStream(bout);
			oout.writeObject(placelist);
			oout.close();
			
			byte[] bytes = bout.toByteArray();
			document.add(new Field(PLACELIST, bytes, 0, bytes.length, Field.Store.YES));
		}
		catch(Exception ex) {
			throw new IllegalArgumentException(ex);
		}
	}
	
	private static boolean setPOIs(CSListPlace placelist) {
		boolean poisFound = true;
		List<String> ids = badpois.get(placelist.getSourceUrl());
		if(ids == null) ids = new ArrayList<String>();
		for(int i = 0, n = placelist.groupsSize(); i < n; i++) {
			GroupOfPlaces g = placelist.group(i);
			List<String>  badgroupplaces = new ArrayList<String>();
			for(PlacelistPlace place:g.entries()) {
				CSListing poi = CSListingUtil.loadProfile(profile, place.getListingid()); 
				if(poi == null) {
					//System.out.println("Bad POI lookup " + place.getListingid() + " " + placelist.getSourceUrl());
					ids.add(place.getListingid());
					badgroupplaces.add(place.getListingid());
					poisFound = false;
				}
				place.setPoi(poi);
			}
			
			//remove all "bad" places from this group
			if(!badgroupplaces.isEmpty()) {
				g.removePlaces(badgroupplaces);
			}
		}
		
		//try to salvage Placelist
		placelist.removeEmptyGroups();
		if(placelist.groupsSize() > 0) poisFound = true;
		
		if(!ids.isEmpty()) {
			badpois.put(placelist.getSourceUrl(), ids);
		}
		
		return poisFound;
	}
	
	private static void indexPlacelists(String csListFile, IndexWriter writer, String dymPath) throws Exception {
	    
	    //line-parse cslist.json file and store all non-null to StringBuffer buffer
		BufferedReader reader = new BufferedReader(new FileReader(csListFile));
		String line = null;
		StringBuffer buffer = new StringBuffer();
		while((line = reader.readLine()) != null) {
			buffer.append(line);
		}
		reader.close();
		
		
		
		PrintWriter dymwriter = new PrintWriter(new FileWriter(dymPath));
		
		int count = 0;
		JSONObject json = new JSONObject(buffer.toString());
		JSONArray lists = json.getJSONArray("lists");
		System.out.println(lists.length() + " lists in JSON");
		for(int i = 0, n = lists.length(); i < n; i++) {
			CSListPlace pl = CSListPlace.fromJSON(lists.getJSONObject(i));
			
			//set the hashed placelistID
			pl.setId(Utils.hash(pl.getSourceUrl()));
			
			//writes the dym.txt file by passing the writer and a placeList
			writeDym(dymwriter, pl);
			
			
			List<Document> docs = newPlacelistDocuments(pl);
			for(Document doc:docs) {	
				count++;
				writer.addDocument(doc);
			}
		}
		
		dymwriter.close();
		
		System.out.println("Indexed " + count + " lists");
	}
	
	//TODO keep this but add new function for typeahead restricted by location
	//writeDym: writes the dym.txt file
	private static void writeDym(PrintWriter writer, CSListPlace pl) throws Exception {
		writer.println(pl.getName().trim().toLowerCase());
		String[] split = pl.getName().split(" ");
		for(String s:split) {
			writer.println(s.trim().toLowerCase());
		}
		for(int i = 0, n = pl.groupsSize(); i < n; i++) {
			GroupOfPlaces group = pl.group(i);
			if(!group.getName().equals(CSListPlace.DEFAULT_GROUP_NAME)) {
				writer.println(group.getName().trim().toLowerCase());
				split = group.getName().split(" ");
				for(String s:split) {
					writer.println(s.trim().toLowerCase());
				}
			}
		}
	}
	
    private static IndexWriter newIndexWriter(String indexPath) throws IOException {
        Directory directory = new NIOFSDirectory(new File(indexPath));
        IndexWriter writer = new IndexWriter(directory, new Analyzer(), true, MaxFieldLength.UNLIMITED);
        writer.setMergeFactor(100000);
        writer.setMaxMergeDocs(Integer.MAX_VALUE);
        return writer;
	}
	
	public static void main(String[] args) throws Exception {
		if(args.length < 5)
		{
		    System.out.println("Usage: CSListIndexer");
            System.out.println("\t Input: existing cslists index dir");
            System.out.println("\t Input: existing ad index dir");
            System.out.println("\t Input: CSLists JSON file (to be indexed)");
            System.out.println("\t Output: index dir");
            System.out.println("\t Output: DYM .txt file");
		
		}
	    String indexPath = args[0];
        String adIndexPath = args[1];
        String csListFile = args[2];
        String outputDir = args[3];
        String dym = args[4];
        
		CSListIndexer.profile.setIndexPath(indexPath);
		CSListIndexer.profile.setAdIndexPath(adIndexPath);
        CSListIndexer.profile.setLocalezeIndexPath("/idx/localeze");
		
		indexCSLists(csListFile, outputDir, dym);
	}
}
