package com.where.places.lists;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.spell.LevensteinDistance;
import org.apache.lucene.search.spell.SpellChecker;
//import org.apache.lucene.spatial.geohash.GeoHashDistanceFilter;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.Version;
import org.json.JSONArray;
import org.json.JSONObject;

import com.where.data.cslists.CSListIndexer;
import com.where.util.lucene.ListGeoFilter;

public class PlacelistSearch {
	private static Log logger = LogFactory.getLog(PlacelistSearch.class);
	
	private static final SearchResult EMPTY = SearchResult.nullResult();
	private static final List<CSListPlace> EMPTY_LISTS = new ArrayList<CSListPlace>();
	private static final List<String> EMPTY_RESPELL = new ArrayList<String>();
	private ListGeoFilter.ListGeoHashCache geoHashCache_ = null;
	private static final int MAX = 1000;
	
	static {
	    BooleanQuery.setMaxClauseCount(Integer.MAX_VALUE);
	}	
	
	private IndexSearcher searcher;
	private IndexSearcher placesSearcher;
	private SpellChecker spell;
	
	public void setIndexPath(String indexPath) {
		try {
		    searcher = new IndexSearcher(new NIOFSDirectory(new File(indexPath)));
			geoHashCache_ = new ListGeoFilter.ListGeoHashCache(CSListIndexer.GEOHASH, searcher.getIndexReader());
		}
		catch(Exception ex) {
			throw new IllegalArgumentException("Error while creating searcher for " + indexPath, ex);
		}
		try {
			placesSearcher = new IndexSearcher(new NIOFSDirectory(new File(indexPath+"/places")));
		}
		catch(Exception ex) {
			throw new IllegalArgumentException("Error while creating searcher for " + indexPath, ex);
		}
	}
	
	public void setDymIndexPath(String dymIndexPath) {
		try {
			spell = new SpellChecker(FSDirectory.open(new File(dymIndexPath)));
			spell.setStringDistance(new LevensteinDistance());
		}
		catch(Exception ex) {
			throw new IllegalArgumentException("Error while creating searcher for " + dymIndexPath, ex);
		}
	}
	
	public SearchResult nearbyLists(SearchCriteria criteria) {
		try {
			Analyzer analyzer = new com.where.commons.feed.citysearch.search.Analyzer();
			BooleanQuery query = new BooleanQuery();
			
			if(criteria.query != null && criteria.query.trim().length() > 0) {
				try {
					query.add(new QueryParser(Version.LUCENE_30, CSListIndexer.META, analyzer).parse(criteria.query), Occur.MUST);
				}
				catch(Throwable ex) {
					logger.warn("Couldn't parse " + criteria.query + " in nearbyLists");
					return EMPTY;
				}
			}
			
			String latTxt = String.valueOf(criteria.lat);
			latTxt = latTxt.substring(0, latTxt.indexOf(".")).replace('-', 'm');
			query.add(new QueryParser(Version.LUCENE_30, CSListIndexer.LATITUDE, analyzer).parse(latTxt), Occur.MUST);
			
			String lngTxt = String.valueOf(criteria.lng);
			lngTxt = lngTxt.substring(0, lngTxt.indexOf(".")).replace('-', 'm');
			query.add(new QueryParser(Version.LUCENE_30, CSListIndexer.LONGITUDE, analyzer).parse(lngTxt), Occur.MUST);
		
			QueryWrapperFilter filter = new QueryWrapperFilter(query);
			
			ListGeoFilter distanceFilter = new ListGeoFilter(filter, 
			        criteria.lat, criteria.lng, criteria.miles, geoHashCache_, CSListIndexer.GEOHASH);
	
			Sort sort = new Sort(new SortField(CSListIndexer.TIMESTAMP, SortField.LONG, true));
			int max = criteria.page*criteria.itemsPerPage + criteria.itemsPerPage;
			TopDocs docs = searcher.search(query, distanceFilter, max, sort);
			
			if(docs == null || docs.totalHits == 0) {
				return EMPTY;
			}
			
			SearchResult result = new SearchResult();
			result.setPage(criteria.page);
			result.setItemsPerPage(criteria.itemsPerPage);
			result.setTotalItems(docs.totalHits);
			result.setLists(collectLists(docs, criteria, searcher));
			
			return result;
		}
		catch(Exception ex) {
			logger.error("Error while loading nearby lists " + criteria, ex);
			return EMPTY;
		}
	}
	
	public CSListPlace loadList(String listid) {	
		List<CSListPlace> lists = loadLists(Collections.singletonList(listid));
		if(lists.isEmpty()) return null;
		
		return lists.get(0);
	}
	
	public List<CSListPlace> loadLists(List<String> listids) {		
		try {
			//Analyzer analyzer = new com.where.commons.feed.citysearch.search.Analyzer();
			Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_30);

			BooleanQuery query = new BooleanQuery();
			for(String id:listids) {
				query.add(new QueryParser(Version.LUCENE_30, CSListIndexer.ID, analyzer).parse(id), Occur.SHOULD);
			}
		
			TopDocs docs = searcher.search(query, MAX);
			
			if(docs == null || docs.totalHits == 0) {
				return EMPTY_LISTS;
			}
			
			ScoreDoc[] sdocs = docs.scoreDocs;
			if(sdocs.length == 0) return null;
			
			List<CSListPlace> result = new ArrayList<CSListPlace>();
			for(int i = 0, n = sdocs.length; i < n; i++) {
				CSListPlace list = getPlacelistFromCache(sdocs[i].doc);
				if(list == null) {
					Document document = searcher.doc(sdocs[i].doc);
					list = loadPlacelist(document, sdocs[i].doc);
				}
				if(list != null) result.add(list);
			}
			
			return result;
		}
		catch(Exception ex) {
			logger.error("Error while loading lists", ex);
			return EMPTY_LISTS;
		}
	}
	
	public List<CSListPlace> loadParentLists(String type, String placeid) {
		try {
			Analyzer analyzer = new com.where.commons.feed.citysearch.search.Analyzer();

			Query query = new QueryParser(Version.LUCENE_30, CSListIndexer.ID, analyzer).parse(placeid);
			
			int max = 1;
			TopDocs docs = placesSearcher.search(query, max);
			
			if(docs == null || docs.totalHits == 0) {
				return EMPTY_LISTS;
			}
			
			ScoreDoc[] sdocs = docs.scoreDocs;
			if(sdocs.length == 0) return EMPTY_LISTS;
			
			List<CSListPlace> lists = getParentPlacelistsFromCache(sdocs[0].doc);
			if(lists == null) {
				Document document = placesSearcher.doc(sdocs[0].doc);
				lists = loadParentLists(document, sdocs[0].doc);
			}
			return lists;
		}
		catch(Exception ex) {
			logger.error("Error while loading parent lists " + type + " " + placeid, ex);
			return EMPTY_LISTS;
		}
	}
	
	private List<CSListPlace> collectLists(TopDocs docs, SearchCriteria criteria, IndexSearcher searcher) throws Exception {
		List<CSListPlace> lists = new ArrayList<CSListPlace>();
		ScoreDoc[] sdocs = docs.scoreDocs;
		int start = criteria.page*criteria.itemsPerPage;
		int counter = 0;
		for(ScoreDoc sdoc:sdocs) {
			counter++;
			if(counter > start) {
				CSListPlace list = getPlacelistFromCache(sdoc.doc);
				if(list == null) {
					Document document = searcher.doc(sdoc.doc);
					list = loadPlacelist(document, sdoc.doc);
				}
				if(list != null) lists.add(list);
			}
		}
		return lists;
	}
	
	public static CSListPlace getPlacelistFromCache(int docid) {
		Object pl = MemcacheUtil.get(""+docid, "PlaceListSearch");
		if(pl != null) return (CSListPlace) pl;
		return null;
	}
	
	public static CSListPlace loadPlacelist(Document document, int docid) {
		try {
			byte[] bytes = document.getBinaryValue(CSListIndexer.PLACELIST);
			ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
			CSListPlace list = (CSListPlace)in.readObject();
			//System.out.println("LIST " + list.getId() + " " + document.getField(CSListIndexer.ID));
			in.close();
			MemcacheUtil.set(""+docid, list, "PlaceListSearch");
			return list;
		}
		catch(Exception ex) {
			throw new IllegalArgumentException(ex);
		}
	}
	
	@SuppressWarnings("unchecked")
	public List<CSListPlace> getParentPlacelistsFromCache(int docid) {
		Object pl = MemcacheUtil.get(""+docid, "ParentPlacelistsSearch");
		if(pl != null) return (List<CSListPlace>) pl;
		return null;
	}
	
	public List<CSListPlace> loadParentLists(Document document, int docid) {
		try {
			byte[] bytes = document.getBinaryValue(CSListIndexer.PLACELISTS);
			ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
			@SuppressWarnings("unchecked")
			List<String> ids = (List<String>)in.readObject();
			in.close();
			List<CSListPlace> lists = loadLists(ids);
			MemcacheUtil.set(""+docid, lists, "ParentPlacelistsSearch");
			return lists;
		}
		catch(Exception ex) {
			throw new IllegalArgumentException(ex);
		}
	}
	
	public List<String> didYouMean(String word, int howMany) {
		try {
			return didYouMean(word, howMany, spell);
		}
		catch(Exception ex) {
			return EMPTY_RESPELL;
		}
	}
	
	@SuppressWarnings("unchecked")
	public List<String> didYouMean(String word, int howMany, SpellChecker sp) {
		if(word == null) return EMPTY_RESPELL;
		
		Object dym = MemcacheUtil.get(word+"_"+howMany, "PlacelistSearchDYM");
		if(dym != null) return (List<String>) dym;
		
		word = word.toLowerCase();
		
		try {
			String[] respell = sp.suggestSimilar(word, howMany);

			if(respell == null || respell.length == 0) {
				MemcacheUtil.set(word+"_"+howMany, EMPTY_RESPELL, "PlacelistSearchDYM");
				return EMPTY_RESPELL;
			}
			
			if(respell.length > howMany) {
				respell = Arrays.copyOf(respell, howMany);
			}
			
			List<String> results = Arrays.asList(respell);
			if(results != null) MemcacheUtil.set(word+"_"+howMany, results, "PlacelistSearchDYM");
			return results;
		}
		catch(Exception ignored) {
			return EMPTY_RESPELL;
		}
	}
	
	public static class SearchResult implements Serializable {
		private static final long serialVersionUID = 1090042242289431503L;
		
		private List<CSListPlace> lists;
		private int page;
		private int itemsPerPage;
		private int totalItems;
		
		private List<String> didYouMean;

		public List<CSListPlace> getLists() {
			
			return lists;
		}

		public void setLists(List<CSListPlace> lists) {
			this.lists = lists;
		}

		public int getPage() {
			return page;
		}

		public void setPage(int page) {
			this.page = page;
		}

		public int getItemsPerPage() {
			return itemsPerPage;
		}

		public void setItemsPerPage(int itemsPerPage) {
			this.itemsPerPage = itemsPerPage;
		}

		public int getTotalItems() {
			return totalItems;
		}

		public List<String> getDidYouMean() {
			return didYouMean;
		}

		public void setDidYouMean(List<String> didYouMean) {
			this.didYouMean = didYouMean;
		}

		public void setTotalItems(int totalItems) {
			this.totalItems = totalItems;
		}
		
		public JSONObject toJSON() {
			try {
				JSONObject json = new JSONObject();
				json.put("page", page);
				json.put("listsPerPage", itemsPerPage);
				json.put("totalLists", totalItems);
				
				if(didYouMean != null) {
					JSONArray dym = new JSONArray();
					for(String d:didYouMean) {
						dym.put(d);
					}
					json.put("dym", dym);
				}
				
				if(lists == null) lists = EMPTY_LISTS;
				
				JSONArray a = new JSONArray();
				for(CSListPlace placelist:lists) {
					a.put(placelist.toJSON());
				}
				json.put("lists", a);
				
				return json;
			}
			catch(Exception ex) {
				throw new IllegalStateException(ex);
			}
		}

		private static SearchResult nullResult() {
			SearchResult result = new SearchResult();
			result.setLists(new ArrayList<CSListPlace>());
			return result;
		}
	}
	
	public static class SearchCriteria implements Serializable {
		private static final long serialVersionUID = -4777871182281220771L;
		
		private double lat;
		private double lng;
		private double miles;
		private String query;
		private String authorid;
		
		private int page;
		private int itemsPerPage = 15;
		
		public double getLat() {
			return lat;
		}
		public void setLat(double lat) {
			this.lat = lat;
		}
		public double getLng() {
			return lng;
		}
		public void setLng(double lng) {
			this.lng = lng;
		}
		public double getMiles() {
			return miles;
		}
		public void setMiles(double miles) {
			this.miles = miles;
		}
		public String getQuery() {
			return query;
		}
		public void setQuery(String query) {
			this.query = query;
		}
		public String getAuthorid() {
			return authorid;
		}
		public void setAuthorid(String authorid) {
			this.authorid = authorid;
		}
		public int getPage() {
			return page;
		}
		public void setPage(int page) {
			this.page = page;
		}
		public int getItemsPerPage() {
			return itemsPerPage;
		}
		public void setItemsPerPage(int itemsPerPage) {
			this.itemsPerPage = itemsPerPage;
		}
		
		public String toString() {
			return lat + " " + lng + " " + miles + " " + query + " " + authorid;
		}
	}
	
	public static void main(String[] args) throws Exception {
		SearchCriteria criteria = new SearchCriteria();
		criteria.lat= 42.365188;
		criteria.lng=-71.058243;
		criteria.miles=1;
		//criteria.query="hotel";
		
		PlacelistSearch search = new PlacelistSearch();
		search.setIndexPath(args[0]);
		SearchResult result = search.nearbyLists(criteria);
		System.out.println("Found " + result.totalItems + " lists.");
		for(CSListPlace list:result.lists) {
			System.out.println(list.getName() + " " + list.getSourceUrl());
		}
		System.out.println(result.lists.get(0).toJSON());
		System.out.println(result.lists.get(1).toJSON());
	}
}
