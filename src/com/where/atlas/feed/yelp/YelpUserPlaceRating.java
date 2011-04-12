package com.where.atlas.feed.yelp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * 
 * @author fliuzzi
 *
 *	YelpUserPlaceRating.java
 *		 Usage: takes in a de-duped yelp file 
 *				and old merger file and outputs the merger file 
 *					arg0:old merger file 
 *					arg1: Deduped file 
 *					arg2: output file
 *
 *			Note:  1) if a user rates a place twice, nothing is done as of yet.
 *				   2) the program reads in the old merger so that it can predict new user long values
 *								(it adds TWO million after finding the max to be SURE
 *															because yellowpages adds one mil)
 *
 */
public class YelpUserPlaceRating {

	protected BufferedReader reader;
	protected BufferedWriter writer;
	protected HashMap<String,String> userMap;
	protected long userID;
	protected long count;
	
	public YelpUserPlaceRating(BufferedReader mergerbr, BufferedReader br,BufferedWriter writer) throws IOException
	{
		count=0;
		this.reader = br;
		this.writer = writer;
		userMap = new HashMap<String,String>();
		userID = findMaxUserID(mergerbr);
		collect();
	}
	
	protected void writeUserPlaceRating(String user,String csid, String rating) throws IOException
	{
		count++;
		writer.write(user+","+csid+","+rating);
		writer.newLine();
	}
	
	protected long getCount()
	{
		return count;
	}
	
	protected long getUserCount()
	{
		return userMap.size();
	}
	
	protected String findUserID(String username)
	{
		if(isNull(username))
			return "";
		
		if(userMap.containsKey(username))
			return userMap.get(username);
		else{
			
			userMap.put(username, String.valueOf(++userID));
			return String.valueOf(userID);
		}
		
	}
	
	protected boolean isNull(String str)
	{
		if(str == null || str.equals(""))
			return true;
		return false;
	}
	
	protected void collectReviews(JSONArray reviews,String csid) throws JSONException, IOException
	{
		for(int i = 0;i<reviews.length();i++)
		{
			JSONObject review = reviews.getJSONObject(i);
			String userid = findUserID(review.optString("user"));
			String rating = review.optString("rating");
			
			if(isNull(rating) || isNull(userid) || isNull(csid))
				continue;
			else
				writeUserPlaceRating(userid, csid, rating);
		}
	}

	protected void collect() throws IOException
	{
		String line = null;
		while((line = reader.readLine()) != null) {
			try{
				JSONObject json = new JSONObject(line);
				String csid = json.optString("csid");
				JSONArray jarray = json.optJSONArray("reviews");
				if(jarray != null && !(csid.equals("")))
				{
					collectReviews(jarray,csid);
				}
			}
			catch(Exception e)
			{
				continue;
			}
		}
		
		writer.close();
	}
	
	//parses through the merger file and finds the greatest userid....then adds two hundred million
	protected long findMaxUserID(BufferedReader reader) throws IOException
	{
		String line = null;
		long id = 0;
		long max = 0;
		
		while((line = reader.readLine()) != null) {
			id = Long.parseLong(line.substring(0, line.indexOf(",")));
			if(id > max)
				max=id;
		}
		///two!
		return max + 200000000;
	}
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		if(args.length != 3)
		{
			System.err.println("Usage: takes in a de-duped Yelp file, and old merger file " +
					"and outputs the new merger file");
			System.err.println("arg0:old merger file \t arg1: Deduped file \t arg2: output file");
			return;
		}
		
		
		YelpUserPlaceRating main = new YelpUserPlaceRating(new BufferedReader(new FileReader(args[0])),
														new BufferedReader(new FileReader(args[1])), 
														new BufferedWriter(new FileWriter(args[2])));	
		
		
		
		
		
		System.out.println("Done. Wrote "+main.getCount()+" ratings amongst "+main.getUserCount()+" unique Yelp users.");
		
	}


}
