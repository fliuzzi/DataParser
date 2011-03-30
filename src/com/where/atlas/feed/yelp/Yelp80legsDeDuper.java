package com.where.atlas.feed.yelp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.lucene.search.spell.JaroWinklerDistance;
import org.json.JSONException;
import org.json.JSONObject;

import com.where.commons.feed.citysearch.CSListing;
import com.where.commons.feed.citysearch.search.query.Search;
import com.where.commons.feed.citysearch.search.query.SearchResult;
import com.where.commons.feed.citysearch.search.query.Search.SearchCriteria;

/**
 * @author fliuzzi
 */
public class Yelp80legsDeDuper {

	
	private static final float THRESHOLD 		= 0.88f;
	protected BufferedReader reader;
	protected BufferedWriter writer;
	protected Search searchob;
	protected AtomicLong goodCount;
	protected long totalCount;
	
	public Yelp80legsDeDuper(BufferedReader reader, BufferedWriter writer)
	{
		this.reader = reader;
		this.writer = writer;
		goodCount = new AtomicLong(0);
		totalCount = 0;
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
	
	private String fixQuery(String query)
	{
		if(query != null)
			return query.replace(",","").replace(".","").replace("AND","").replace("OR", "").trim();
		else 
			return query;
	}
	
	public List<CSListing> searchByLocation(double lat, double lon, String keywords, String addressdata) {
		
		//TODO: temporary fix for oregon listings being disjuncted
		
		keywords = StringEscapeUtils.unescapeHtml(keywords);
		addressdata = StringEscapeUtils.unescapeHtml(addressdata);
		
		keywords = fixQuery(keywords);
		addressdata = fixQuery(addressdata);
		
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
	
	private synchronized void collect(String str) throws IOException
	{
		writer.write(str);
		writer.newLine();
	}
	
	public void analyzeAndCollect(JSONObject listing) throws JSONException, IOException
	{
    	String[] ids = findWhereId(listing);
    	if(ids != null)
    	{
    		listing.put("whereid",ids[0]);
    		listing.put("csid", ids[1]);
    		collect(listing.toString());
    		goodCount.incrementAndGet();
    	}
	}
	
	public void dedupe() throws IOException, JSONException, InterruptedException
	{
		String line = null;
		
        ExecutorService thePool = Executors.newFixedThreadPool(2);
        
		
        while((line = reader.readLine()) != null) {
        	totalCount++;
        	final String line_ = line;
				
			@SuppressWarnings("unused")
			Future<?> fut = thePool.submit(
                    new Runnable(){ public void run(){
                    	try{
	                    	analyzeAndCollect(new JSONObject(line_));
                    	}
                    	catch(JSONException e)
                    	{
                    		System.out.println(e.getMessage());
                    	} catch (IOException e) {
                    		System.out.println(e.getMessage());
						}
	                    }});
			
        }
       
        System.out.println(totalCount+" total listings...awaiting analyzation and collection...\n" +
        											"(this may take a bit)");
        thePool.shutdown();
        thePool.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);
        
        System.out.println("Done.  De-duped to "+goodCount.get()+" listings. ~" + ((goodCount.get()/(double)totalCount)*100)+"%");
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
				CSListingHolder holder = null;
				JaroWinklerDistance jd = new JaroWinklerDistance();
				String placename = json.optString("name").toLowerCase().replace("the ","");
				String smushedname = placename.replace(" ","");
				for(CSListing l: possiblePois) {
					float distance = jd.getDistance(smushedname, l.getName().toLowerCase().replaceAll(" ", ""));
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
	

	public static void main(String[] args) throws IOException, InterruptedException {
		if (args.length != 2)
		{
			System.err.println("usage: takes in a newline delimeted Yelp json," +
					"de-dupes them against our places, and outputs the jsons with whereid and csid keys");
			System.err.println("arg0: Yelp file\targ1: output file");
		}
		
		InputStream fileStream = new FileInputStream(args[0]);
		InputStream gzipStream = new GZIPInputStream(fileStream);
		BufferedReader reader = new BufferedReader(new InputStreamReader(gzipStream));
		
		
        	
		
		Yelp80legsDeDuper main = new Yelp80legsDeDuper(reader, 
				new BufferedWriter(new FileWriter(args[1])));	
		
		System.out.println("Beginning de-dupe");
		try {
			main.dedupe();
		} catch (JSONException je) {
			je.printStackTrace();
		}
		
		main.getWriter().close();
		
	}


}
