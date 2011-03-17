package com.where.place;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.where.commons.feed.citysearch.CSListing;
import com.where.commons.feed.citysearch.Category;
import com.where.commons.feed.citysearch.RatingStat;
import com.where.commons.feed.citysearch.Tip;

public class YPPlace extends Place {

	String YPName;
	String hours;
	String YPurl;
	String biz_url;
	ArrayList<String> categories;
	ArrayList<JSONObject> reviews;
	
	public YPPlace(){
		setSource(Source.YP);
		categories = new ArrayList<String>();
		reviews = new ArrayList<JSONObject>();
	}
	
	public void addReview(JSONObject rev)
	{
		reviews.add(rev);
	}
	
	public ArrayList<JSONObject> getReviews()
	{
		return reviews;
	}
	
	public void addCategory(String cat)
	{
		categories.add(cat);
	}
	
	public ArrayList<String> getCategories()
	{
		return categories;
	}
	
	public void setHours(String hrs)
	{
		hours = hrs;
	}
	
	public String getHours()
	{
		return hours;
	}
	
	public void setYPurl(String url)
	{
		YPurl = url;
	}
	
	public String getYPurl()
	{
		return YPurl;
	}
	
	public void setBizURL(String url)
	{
		biz_url = url;
	}
	
	public String getBizURL()
	{
		return biz_url;
	}
	
	public void setYPName(String name)
	{
		YPName = name;
	}
	
	public String getYPName()
	{
		return YPName;
	}
	
	public JSONObject toJSON()
	{
		return toJSON(true);
	}
	
//	String YPName;
//	String hours;
//	String YPurl;
//	String biz_url;
//	ArrayList<String> categories;
//	ArrayList<JSONObject> reviews;
	//TODO: finish this
    public JSONObject toJSON(boolean includeReviews) {
    	JSONObject json = new JSONObject();
    	try{
	    	if(getWhereId() != null)
	    		json.put("whereid", getWhereId());
	    	if(getAddress() != null)
	    	{
	    		Address addy = getAddress();
	    		json.put("location", addy.toJSON());
	    	}
	    	
	    	if(getLatlng() != null){
	    		json.put("lat", getLatlng()[0]);
	    		json.put("long", getLatlng()[1]);
	    	}
	    	if(YPName != null)
	    		json.put("ypname", YPName);
	    	if(hours != null)
	    		json.put("hours", hours);
	    	if(YPurl != null)
	    		json.put("ypurl",YPurl);
	    	if(biz_url != null)
	    		json.put("biz_url",biz_url);
	    	if(categories.size() > 0)
	    	{
	    		JSONArray jarray = new JSONArray();
	    		for(String str:categories)
	    		{
	    			jarray.put(str);
	    		}
	    		json.put("categories", jarray);
	    	}
	    	
	    	if(includeReviews)
		    	if(reviews.size() > 0)
		    	{
		    		JSONArray jarray = new JSONArray();
		    		for(JSONObject jobj:reviews)
		    		{
		    			jarray.put(jobj);
		    		}
		    		json.put("reviews", jarray);
		    	}
	    	
	    	
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    	}
    	return json;
    }
	
	
	public String toString()
	{
		//return super.toString()+"\nBizurl:"+biz_url+"\nReviews:"+reviews+"\nCategories:"+categories+"\n";
		return toJSON().toString();
	}
}
