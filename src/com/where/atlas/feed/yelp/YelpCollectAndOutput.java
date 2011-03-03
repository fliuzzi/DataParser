package com.where.atlas.feed.yelp;

import com.where.atlas.feed.PlaceCollector;
import com.where.place.Place;
import com.where.place.YelpPlace;


//collects YelpPlace objects and outputs to a file in the format:
//                  userid,CSid,rating
public class YelpCollectAndOutput implements PlaceCollector
{
    private YelpParserUtils parserutils;
    
    public YelpCollectAndOutput(YelpParserUtils Yparserutils)
    {
        parserutils = Yparserutils;
    }
    
    public void collect(Place place)
    {
        parserutils.storeListing(((YelpPlace)place).toListing());
    }

    public void collectBadInput(Object input, Exception reason)
    {
        System.err.println("Cannot use listing:\n"+input);
    }
}
