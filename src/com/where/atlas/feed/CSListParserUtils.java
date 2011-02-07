package com.where.atlas.feed;

import java.io.ByteArrayOutputStream;
import java.io.File;
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

import com.where.commons.feed.citysearch.CSListing;
import com.where.commons.feed.citysearch.search.Analyzer;
import com.where.commons.feed.citysearch.search.query.Profile;
import com.where.commons.util.LocationUtil;
import com.where.place.CSListPlace;
import com.where.places.lists.GroupOfPlaces;
import com.where.places.lists.PlacelistPlace;
import com.where.utils.CSListingUtil;

public class CSListParserUtils
{
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
    
    public static Profile profile;
    private String csListFilePath;
    private String outputDirPath;
    private String dymFilePath;
    private IndexWriter writer;
    private static Set<String> indexed;
    private static Map<String, Set<CSListPlace>> inversed;
    public static Map<String, List<String>> badpois;

    
    
    

    
    public CSListParserUtils(Profile profile, String csListFilePath, String outputDirPath, String dymFilePath)throws IOException 
    {
        CSListParserUtils.profile=profile;
        this.csListFilePath=csListFilePath;
        this.outputDirPath=outputDirPath;
        this.dymFilePath=dymFilePath;
        
        writer = newIndexWriter(outputDirPath);
        indexed = new HashSet<String>();
        
        inversed = new HashMap<String, Set<CSListPlace>>();
        badpois = new HashMap<String, List<String>>();

    }
    
    //TODO keep this but add new function for typeahead restricted by location
    //writeDym: writes the dym.txt file
    public static void writeDym(PrintWriter writer, CSListPlace place) throws Exception {
        
        
        
        writer.println(place.getName().trim().toLowerCase());
        String[] split = place.getName().split(" ");
        for(String s:split) {
            writer.println(s.trim().toLowerCase());
        }
        for(int i = 0, n = place.groupsSize(); i < n; i++) {
            GroupOfPlaces group = place.group(i);
            if(!group.getName().equals(CSListPlace.DEFAULT_GROUP_NAME)) {
                writer.println(group.getName().trim().toLowerCase());
                split = group.getName().split(" ");
                for(String s:split) {
                    writer.println(s.trim().toLowerCase());
                }
            }
        }
    }
    
    public static List<Document> newPlacelistDocuments(CSListPlace placelist) throws Exception {
        List<Document> docs = new ArrayList<Document>();
        
        //already indexed, to remove this
        String lid = placelist.getSourceUrl().substring(placelist.getSourceUrl().lastIndexOf("/"));
        if(indexed.contains(lid)) return docs;
        
        indexed.add(lid);
        
        //POI lookup failed
        if(!setPOIs(placelist)) return docs;
                
        //just for now although we'll cluster for national lists that span multiple cities
        CSListing poi = placelist.group(0).entries().get(0).getPoi();
        docs.add(newPlacelistDocument(placelist, poi.getAddress().getLat(), poi.getAddress().getLng()));
        
        addInverseLookup(placelist);

        return docs;
    }
    
    public static void addInverseLookup(CSListPlace placelist) {
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
    
    public static Document newPlacelistDocument(CSListPlace placelist, double lat, double lng) throws Exception {
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
    
    public static boolean setPOIs(CSListPlace placelist) {
        boolean poisFound = true;
        List<String> ids = badpois.get(placelist.getSourceUrl());
        if(ids == null) ids = new ArrayList<String>();
        for(int i = 0, n = placelist.groupsSize(); i < n; i++) {
            GroupOfPlaces g = placelist.group(i);
            List<String>  badgroupplaces = new ArrayList<String>();
            for(PlacelistPlace place:g.entries()) {
                CSListing poi = CSListingUtil.loadProfile(CSListParserUtils.profile, place.getListingid()); 
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

    

    
    public Profile getProfile()
    {
        return profile;
    }
    public String getCSListFilePath()
    {
        return csListFilePath;
    }
    public String getOutputDirPath()
    {
        return outputDirPath;
    }
    public String getDymFilePath()
    {
        return dymFilePath;
    }
    public IndexWriter getWriter()
    {
        return writer;
    }
    
    private static IndexWriter newIndexWriter(String indexPath) throws IOException {
        Directory directory = new NIOFSDirectory(new File(indexPath));
        IndexWriter writer = new IndexWriter(directory, new Analyzer(), true, MaxFieldLength.UNLIMITED);
        writer.setMergeFactor(100000);
        writer.setMaxMergeDocs(Integer.MAX_VALUE);
        return writer;
    }
    
    public void finishProcessing() throws Exception
    {
        writer.optimize();
        writer.close();
    
        
        indexPlaceLookups(outputDirPath);
    }
    
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
}
