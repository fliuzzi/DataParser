package com.where.place;

import java.util.ArrayList;

import com.where.atlas.feed.yelp.YelpParserUtils.InnerRating;
import com.where.atlas.feed.yelp.YelpParserUtils.Listing;

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
        return "Yelp Place:Name: " + getYelpName() + "\nCS Name: " + getName() 
                            + "\ncsid: "+getNativeId()+"  Phone: "+getPhone() + "\nRatings:\t" + getRatings();
    }
    
    public Listing toListing()
    {
        Listing listing = new Listing();
        Address address = getAddress();
        double[] latlng = getLatlng();
        
        
        listing.name_=getYelpName();
        listing.csId_ = Long.parseLong(getNativeId());
        listing.csName_ = getName();
        listing.phoneMatch_ = getPhoneMatch();
        listing.lat_ = latlng[0];
        listing.lng_ = latlng[1];
        listing.geoHash_ = getGeohash();
        listing.street_ = address.getAddress1();
        listing.city_ = address.getCity();
        listing.state_ = address.getState();
        listing.zip_ = Integer.parseInt(address.getZip());
        listing.phone_ = getPhone();
        listing.ratings_ = getRatings();
        
        return listing;
    }
}
