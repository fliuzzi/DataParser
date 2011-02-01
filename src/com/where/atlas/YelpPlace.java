package com.where.atlas;

import java.util.ArrayList;

import com.where.atlas.feed.YelpParserUtils.InnerRating;

public class YelpPlace extends Place
{
    String yelpName;
    ArrayList<InnerRating> ratings;
    boolean phoneMatch;
    
    public YelpPlace()
    {
        setSource(Source.YELP);    
        phoneMatch=false;
        ratings = new ArrayList<InnerRating>();
    }
    
    public String getYelpName()
    {
        return yelpName;
    }
    public void setYelpName(String name)
    {
        yelpName = name;
    }
    public void setPhoneMatch(boolean bool)
    {
        phoneMatch=bool;
    }
    public boolean getPhoneMatch()
    {
        return phoneMatch;
    }
    public void setRatings(ArrayList<InnerRating> Yratings)
    {
        ratings = Yratings;
    }
    public ArrayList<InnerRating> getRatings()
    {
        return ratings;
    }
    
    public String toString()
    {
        return "Yelp Place:Name: " + getYelpName() + "\nCS Name" + getName() + "\ncsid:"+getNativeId()+"  Phone:"+getPhone();
    }
}
