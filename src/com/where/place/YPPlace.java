package com.where.place;

import java.util.ArrayList;

import org.json.JSONObject;

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
	
	public String toString()
	{
		return super.toString()+"\nYPurl:"+YPurl+"\nReviews:"+reviews+"\nCategories:"+categories+"\n";
	}
}
