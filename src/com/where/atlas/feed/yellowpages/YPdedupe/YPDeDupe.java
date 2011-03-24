package com.where.atlas.feed.yellowpages.YPdedupe;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.List;


import org.apache.lucene.search.spell.JaroWinklerDistance;
import org.json.JSONException;
import org.json.JSONObject;


import com.where.commons.feed.citysearch.CSListing;
import com.where.commons.feed.citysearch.search.query.Search;
import com.where.commons.feed.citysearch.search.query.SearchResult;
import com.where.commons.feed.citysearch.search.query.Search.SearchCriteria;

public class YPDeDupe {
	private static final float THRESHOLD 		= 0.88f;
	BufferedReader reader;
	BufferedWriter writer;
	Search searchob;
	
	public YPDeDupe(BufferedReader reader, BufferedWriter writer)
	{
		this.reader = reader;
		this.writer = writer;
		
		searchob = new Search();
		searchob.setIndexPath("/idx/lis");
	}
	
	public BufferedReader getReader()
	{
		return reader;
	}
	
	public BufferedWriter getWriter()
	{
		return writer;
	}
	
	public List<CSListing> searchByLocation(double lat, double lon, String keywords, String addressdata) {
		//System.out.println("** Search Criteria: "+lat+"/"+lon+" : "+keywords+" : "+addressdata);
		
		//TODO: this is where a funky query will throw out data
		//keywords = QueryParser.escape(keywords);
		//addressdata = QueryParser.escape(addressdata);
		
		SearchCriteria criteria = new SearchCriteria();
		criteria.setLat(lat);
		criteria.setLng(lon);
		criteria.setMiles(0.5);
		criteria.setItemsPerPage(10);
		criteria.setKeywords(keywords.replaceAll("[\\p{Punct}]", ""));
		if(addressdata != null && !addressdata.equals("")) 
			criteria.setLocation(addressdata.replaceAll("[\\p{Punct}]", ""));
		criteria.setSortByRelevance(true);
		SearchResult result = null;
		
		
		try {
			result = searchob.geoSearch(criteria);
		} catch (Throwable t) {
			System.err.println("**** "+ (new Date()).toString());
			System.err.println("KEYWORDS: "+keywords+"\nADDRESSDATA: "+addressdata);
			t.printStackTrace();
		}
		return (result != null ? result.pois() : null);
	}
	
	public void dedupe() throws IOException, JSONException
	{
		String line = null;
		long count = 0;
		
        while((line = reader.readLine()) != null) {
        	
        	JSONObject listing;
			try {
				listing = new JSONObject(line);
			} catch (JSONException e) 
			{     
				//if thats a bad json listing, continue to the next
				continue;
			}
			
        	String[] ids = findWhereId(listing);
        	if(ids != null)
        	{
        		count++;
        		listing.put("whereid",ids[0]);
        		listing.put("csid", ids[1]);
        		writer.write(listing.toString());
        		writer.newLine();
        	}
        	
        	
        }
        System.out.println("Done.  Wrote "+count+" de-duped listings.");
	}
	
	
	public String[] findWhereId(JSONObject json) {
		double[] geo = new double[2];
		geo[0] = json.optJSONObject("location").optDouble("lat");
		geo[1] = json.optJSONObject("location").optDouble("lng");
		String address = null;
		//if we dont have geo data, we cant use it
		if(geo[0] != 0 && geo[1] != 0){
			if(!(json.optJSONObject("location").optString("address1").equals("")))
					address = json.optJSONObject("location").optString("address1");
			
			List<CSListing> possiblePois = searchByLocation(geo[0], geo[1], json.optString("name"), address);
			if(possiblePois == null || possiblePois.isEmpty()) {  // no location with address. so try with just lat lon
				possiblePois = searchByLocation(geo[0], geo[1], json.optString("name"), null);
			}
			
			if(possiblePois != null && !possiblePois.isEmpty()) {
				//System.out.println("*** Results ("+possiblePois.size()+") for: "+json.optLong(ID)+" "+json.optJSONObject(PLACE).optString(NAME));
				CSListingHolder holder = null;
				JaroWinklerDistance jd = new JaroWinklerDistance();
				String placename = json.optString("name").toLowerCase().replace("the ","");
				String smushedname = placename.replace(" ","");
				for(CSListing l: possiblePois) {
					float distance = jd.getDistance(smushedname, l.getName().toLowerCase().replaceAll(" ", ""));
					//System.out.println("  * "+l.getWhereId()+" "+ l.getName()+" "+distance);
					if(holder == null || holder.distance < distance) holder = new CSListingHolder(l, distance);
					if(holder.distance == 1) break;
				}
				
				if(holder != null) {
					if(holder.distance > THRESHOLD) {
						String[] ids = {holder.poi.getWhereId(), holder.poi.getListingId()};
						return ids;
					}
				}
			}
		}
		
		return null;
	}
	
	private static class CSListingHolder {
		public CSListing poi;
		public float distance = 0;
		
		public CSListingHolder(CSListing l, float d) {
			poi = l;
			distance = d;
		}
	}
	

	public static void main(String[] args) throws IOException {
		if (args.length != 2)
		{
			System.err.println("usage: takes in a newline delimeted list of YP jsons," +
					"de-dupes them against our places, and outputs the jsons with whereid and csid keys");
			System.err.println("arg0: YP file\targ1: output file");
		}
		
		YPDeDupe main = new YPDeDupe(new BufferedReader(new FileReader(args[0])), 
				new BufferedWriter(new FileWriter(args[1])));	
		
		System.out.println("Beginning de-dupe");
		try {
			main.dedupe();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		
		
	}
}