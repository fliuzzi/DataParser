package com.where.atlas.feed.yelp;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;

import com.where.atlas.feed.PlaceCollector;
import com.where.place.Place;
import com.where.place.YelpPlace;

public class Yelp80legsCollector implements PlaceCollector{

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
			writeEntry(ypplace.toJSON().toString()+"\n");
			
			
		} catch (IOException e) {
			System.err.println("error writing:"+ypplace+"\t"+e.getMessage());
		}
		
	}

	@Override
	public void collectBadInput(Object input, Exception reason) {
		//not implemented right now
	}
	
	private synchronized void writeEntry(String entry) throws IOException
	{
		m.update(entry.getBytes(),0,entry.length());
		
		if(cache.add(new BigInteger(1,m.digest()).toString(16)))
			Yelp80legsParser.getWriter().write(entry);
		else
			System.out.println("DUPLICATE DETECTED (and left out)");
	}

	
	
}
