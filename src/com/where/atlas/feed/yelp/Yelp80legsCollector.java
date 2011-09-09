package com.where.atlas.feed.yelp;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicLong;

import org.json.JSONException;
import org.json.JSONObject;

import com.where.atlas.feed.PlaceCollector;
import com.where.place.Place;
import com.where.place.YelpPlace;

public class Yelp80legsCollector implements PlaceCollector{
	static AtomicLong atomicN = new AtomicLong();
	HashSet<String> cache;
	MessageDigest m;
	public Yelp80legsCollector() throws NoSuchAlgorithmException
	{
		cache = new HashSet<String>();
		m=MessageDigest.getInstance("MD5");
	}
	

	@Override
	public void collect(Place place) {
		YelpPlace ypplace = (YelpPlace)place;
		try {
			writeEntry(ypplace.toJSON(true)); //.toString()+"\n");
			
			
		} catch (IOException e) {
			System.err.println("error writing:"+ypplace+"\t"+e.getMessage());
		}
		
	}

	@Override
	public void collectBadInput(Object input, Exception reason) {
		//not implemented right now
	}
	
	private synchronized void writeEntry(JSONObject entry_json) throws IOException
	{
		String entry = entry_json.toString()+"\n";
		m.update(entry.getBytes(),0,entry.length());
		
		if(cache.add(new BigInteger(1,m.digest()).toString(16))){
			try {
				addId(entry_json);
			} catch (JSONException e) {
				System.out.println("error occurs when addId to Json"); 
				e.printStackTrace();
			}
			Yelp80legsParser.getWriter().write(entry_json.toString()+"\n");
			Yelp80legsParser.getWriter().flush();
		}
		else{
			System.out.println("DUPLICATE DETECTED (and left out)");
		}
	}

	private static void addId(JSONObject json) throws JSONException{
		json.put("_id", atomicN.getAndIncrement());		
	}
	
}
