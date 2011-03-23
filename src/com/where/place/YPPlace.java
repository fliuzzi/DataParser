package com.where.place;


import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.where.atlas.feed.yellowpages.YPRawDataParser;

public class YPPlace extends Place {

	String pid;
	String hours;
	String YPurl;
	String biz_url;
	ArrayList<String> categories;
	ArrayList<JSONObject> reviews;
	
	String accreditations;
	String brands;
	String payment_types_accepted;
	String biz_since;
	String aka;
	String avg_rating;



	String languages;
	
	
	public YPPlace(){
		setSource(Source.YP);
		categories = new ArrayList<String>();
		reviews = new ArrayList<JSONObject>();
	}
	
	public void setPID(String pid)
	{
		this.pid=pid;
	}
	
	public String getPID()
	{
		return pid;
	}
	
	private String parseYPURLtoPID(String url)
    {
    	if(url.length() > 0)
    		return url.substring(url.indexOf("lid=")+4);
    	else
    		return null;
    }
	
	public void addReview(JSONObject rev) throws JSONException
	{
		reviews.add(new JSONObject(YPRawDataParser.cleanReview(rev.toString())));
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
		setPID(parseYPURLtoPID(url));
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
	
	
	public JSONObject toJSON()
	{
		return toJSON(true);
	}
	
	public String getAccreditations() {
		return accreditations;
	}

	public void setAccreditations(String accreditations) {
		this.accreditations = accreditations;
	}

	public String getBrands() {
		return brands;
	}

	public void setBrands(String brands) {
		this.brands = brands;
	}

	public String getPayment_types_accepted() {
		return payment_types_accepted;
	}

	public void setPayment_types_accepted(String payment_types_accepted) {
		this.payment_types_accepted = payment_types_accepted;
	}

	public String getBiz_since() {
		return biz_since;
	}

	public void setBiz_since(String biz_since) {
		this.biz_since = biz_since;
	}

	public String getAka() {
		return aka;
	}

	public void setAka(String aka) {
		this.aka = aka;
	}

	public String getAvg_rating() {
		return avg_rating;
	}

	public void setAvg_rating(String avg_rating) {
		this.avg_rating = avg_rating;
	}

	public String getLanguages() {
		return languages;
	}

	public void setLanguages(String languages) {
		this.languages = languages;
	}
	
    public JSONObject toJSON(boolean includeReviews) {
    	JSONObject json = new JSONObject();
    	try{
    		if(categories.size() > 0)
	    	{
	    		JSONArray jarray = new JSONArray();
	    		for(String str:categories)
	    		{
	    			jarray.put(str);
	    		}
	    		json.put("categories", jarray);
	    	}
	    	if(pid != null && pid.length() > 0)
	    		json.put("pid", pid);
	    	if(getName() != null)
	    		json.put("name", getName());
    		
	    	
	    	if(getAccreditations() != null && getAccreditations().length() > 0)
	    		json.put("accreditations", getAccreditations());
	    	if(getBrands() != null && getBrands().length() > 0)
	    		json.put("brands", getBrands());
	    	if(getPayment_types_accepted() != null && getPayment_types_accepted().length() > 0)
	    		json.put("payment", getPayment_types_accepted());
	    	if(getAka() != null && getAka().length() > 0)
	    		json.put("aka", getAka());
	    	if(getAvg_rating() != null && getAvg_rating().length() > 0)
	    		json.put("avg_rating", getAvg_rating());
	    	if(getBiz_since() != null && getBiz_since().length() > 0)
	    		json.put("in_biz_since", getBiz_since());
	    	if(getLanguages() != null && getLanguages().length() > 0)
	    		json.put("languages", getLanguages());
	    	
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
	    	
	    	if(hours != null && hours.length() > 0)
	    		json.put("hours", hours);
	    	if(YPurl != null)
	    		json.put("ypurl",YPurl);
	    	if(biz_url != null && biz_url.length() > 0)
	    		json.put("biz_url",biz_url);
	    	
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
    
//    public static YPPlace fromJSON(JSONObject json) {
//        try {
//        	
//        	YPPlace poi =  new YPPlace();
//        	
//        	
//        	poi.setName(json.optString("name"));
//        	poi.setPID(json.optString("pid"));
//        	poi.setAka(json.optString("aka"));
//        	poi.setBiz_since(json.optString("in_biz_since"));
//        	poi.setBrands(json.optString("brands"));
//        	poi.setAvg_rating(json.optString("avg_rating"));
//        	poi.setYPurl(json.optString("ypurl"));
//        	poi.setAccreditations(json.optString("accreditations"));
//        	
//        	
//        	
//        }
//        catch(Exception e)
//        {
//        	throw new IllegalStateException(e);
//        }
//    }
	
	
	public String toString()
	{
		//return super.toString()+"\nBizurl:"+biz_url+"\nReviews:"+reviews+"\nCategories:"+categories+"\n";
		return toJSON().toString();
	}
}
