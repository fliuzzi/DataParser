package com.where.places.lists;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.where.commons.feed.citysearch.CSListing;
import com.where.commons.feed.citysearch.search.query.Search;
//import com.where.data.MemcacheUtil;
import com.where.places.lists.PlacelistSearch.SearchCriteria;
import com.where.places.lists.PlacelistSearch.SearchResult;

@Controller
public class ListsAPI {
	private static Log logger = LogFactory.getLog(ListsAPI.class);
	
	@Autowired
	private PlacelistSearch placelistSearch;
	
	@Autowired
	private Search search;
	
	@RequestMapping("/lists/nearby")
	public ModelAndView nearby(HttpServletRequest request, HttpServletResponse response) {
		JSONObject json = new JSONObject();
		try {
			SearchResult result = search(request);
			
			if(result != null) json = result.toJSON();
			
			writeJSON(json, response);
		}
		catch(Exception ex) {
			logger.error("Error while loading nearby lists", ex);
			try {
				writeJSON(json, response);
			}
			catch(Exception ignored) {}
		}
		return null;
	}
	
	public SearchResult search(HttpServletRequest request) {
		SearchCriteria criteria = new SearchCriteria();
		
		if(request.getParameter("lat") != null) {
			criteria.setLat(Double.parseDouble(request.getParameter("lat")));
			criteria.setLng(Double.parseDouble(request.getParameter("lng")));
			if(request.getParameter("radius") != null) {
				criteria.setMiles(Double.parseDouble(request.getParameter("radius")));
			}
		}
		// for now
		else {
			CSListing poi = (CSListing)request.getAttribute("poi");
			if(poi != null) {
				criteria.setLat(poi.getAddress().getLat());
				criteria.setLng(poi.getAddress().getLng());
				criteria.setMiles(15);
			}
			else {
				//String location = request.getParameter("location");
				SearchResult result = new SearchResult();
				result.setPage(0);
				result.setItemsPerPage(0);
				result.setTotalItems(0);
				return result;
			}
		}
		
		criteria.setQuery(request.getParameter("query"));
		
		String count = request.getParameter("count") != null ? request.getParameter("count") : request.getParameter("lcount");
		if(count != null) {
			criteria.setItemsPerPage(Integer.parseInt(count));
		}
		
		String page = request.getParameter("page") != null ? request.getParameter("page") : request.getParameter("lpage");
		if(page != null) {
			criteria.setPage(Integer.parseInt(page));
		}
		
		SearchResult result = placelistSearch.nearbyLists(criteria);
		
		if(criteria.getPage() == 0 && criteria.getQuery() != null && criteria.getQuery().trim().length() > 0) {
			List<String> dym = placelistSearch.didYouMean(criteria.getQuery(), 5);
			if(!dym.isEmpty()) {
				String respell = dym.get(0);
				if(respell.equals(criteria.getQuery().trim().toLowerCase())) {
					if(dym.size() > 1) {
						dym = dym.subList(1, dym.size());
					}
					else dym = new ArrayList<String>();
				}
			}
			result.setDidYouMean(dym);
		}		
		
		return result;
	}
	
	@RequestMapping("/lists/parentsof/{type}/{placeid}")
	public ModelAndView parentLists(@PathVariable String type, @PathVariable String placeid, HttpServletRequest request, HttpServletResponse response) {
		JSONObject json = new JSONObject();
		try {			
			List<Placelist> lists = placelistSearch.loadParentLists(type, placeid);
			if(lists != null && !lists.isEmpty()) {
				SearchResult result = new SearchResult();
				result.setItemsPerPage(lists.size());
				result.setTotalItems(result.getItemsPerPage());
				result.setLists(lists);
				result.setPage(0);
				json = result.toJSON();
			}
			
			writeJSON(json, response);
		}
		catch(Exception ex) {
			logger.error("Error while loading parent lists for " + type + " "+ placeid, ex);
			try {
				writeJSON(json, response);
			}
			catch(Exception ignored) {}
		}
		return null;
	}
	
	@RequestMapping("/lists/suggest")
	public ModelAndView suggest(HttpServletRequest request, HttpServletResponse response) {
		JSONObject json = new JSONObject();
		try {
			if(!search.hasCache()) search.setCache(MemcacheUtil.getCache());
			String query = request.getParameter("query");
			List<String> suggest = null;
			String lat = request.getParameter("lat");
			com.where.commons.feed.citysearch.search.query.Search.SearchCriteria criteria = new com.where.commons.feed.citysearch.search.query.Search.SearchCriteria();
			criteria.setKeywords(query);
			if(lat != null) {
				criteria.setMiles(Double.parseDouble(request.getParameter("radius")));
				criteria.setLat(Double.parseDouble(lat));
				criteria.setLng(Double.parseDouble(request.getParameter("lng")));
				criteria.setItemsPerPage(10);
				suggest = search.typeAheadPlaceList(criteria);
			}
			else if(request.getParameter("location") != null) {
				criteria.setLocation(request.getParameter("location"));
				suggest = search.typeAheadPlaceList(criteria);
			}
			else suggest = search.typeAheadPlaceList(query, 10);
			
			JSONArray a = new JSONArray();
			for(String s:suggest) {
				a.put(s);
			}
			json.put("suggest", a);
		}
		catch(Exception ex) {
			logger.error("Error while suggesting", ex);
		}
		
		writeJSON(json, response);
		
		return null;
	}	
	
	private void writeJSON(JSONObject json, HttpServletResponse response) {
		try {
			response.setContentType("application/json");
			response.getWriter().write(json.toString());
			response.getWriter().close();
		}
		catch(Exception ignored) {}
	}
}
