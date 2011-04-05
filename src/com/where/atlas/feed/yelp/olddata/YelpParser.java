package com.where.atlas.feed.yelp.olddata;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringReader;

import com.where.atlas.feed.FeedParser;
import com.where.atlas.feed.PlaceCollector;
import com.where.atlas.feed.citysearch.CSListingDocumentFactory;
import com.where.atlas.feed.yelp.olddata.YelpParserUtils.Listing;

public class YelpParser implements FeedParser{
    
    public static enum STATE {FRESH, HASNAME, HASLOC, HASSTREET, 
        HASCITY, HASSTATE, HASZIP, HASPHONE, HASRATINGS};
        
    private YelpParserUtils parserutils;
        
    public YelpParser(YelpParserUtils Yparserutils)
    {
        parserutils = Yparserutils;
    }
    
    /**
     * Parse an input stream and send places to a collector
     * @param collector - collector to use
     * @param ins - input stream
     * @throws Exception on any errors
     */
    public void parse(PlaceCollector collector, InputStream ins) throws Exception
    {
        BufferedReader inputReader = new BufferedReader(
                            new StringReader(parserutils.slurpFileStream((FileInputStream)ins)));
        
        Listing curListing = null;
        STATE state = STATE.FRESH;
        String line = null;
        int cnt = 0;
        
        while((line = inputReader.readLine()) != null)
        {
            try {
                line = line.trim(); 
                switch(state)
                {
                    case  HASRATINGS :
                    {
                        if(line.indexOf("name:") == 0)
                        {
                            try
                            {
                                if(curListing != null)
                                {
                                    if(parserutils.canUseListing(curListing))
                                    {
                                        collector.collect(curListing.toPlace());
                                    }
                                    else
                                        collector.collectBadInput(curListing.toPlace(),new Exception());
                                }
                                curListing = new YelpParserUtils.Listing();
                                curListing.name_= line.substring("name:".length());
                                state = STATE.HASNAME;
                                break;                            
                            }
                            catch(Exception e)
                            {
                                state = STATE.FRESH;
                                break;                            
                            }
                        }
                        state = STATE.FRESH;
                        break;
                    }
                    case HASPHONE :
                    {
                        if(line.indexOf("ratings:") == 0)
                        {
                            try
                            {
                                
                                parserutils.processRatings(curListing, line.substring("ratings:".length()));
                                state = STATE.HASRATINGS;
                                break;                            
                            }
                            catch(Exception e)
                            {
                                state = STATE.FRESH;
                                break;                                                
                            }
                        }
                        state = STATE.FRESH;
                        break;                    
                    }
                    case HASZIP :
                    {
                        if(line.indexOf("phone:") == 0 &&curListing != null)
                        {
                            try
                            {
                                curListing.phone_ =  CSListingDocumentFactory.cleanPhone(line.substring("phone:".length()));
                                state = STATE.HASPHONE;
                                break;                                                        
                            }
                            catch(Exception e)
                            {
                                curListing = null;
                                state = STATE.FRESH;
                                break;
                            }
                        }
                        state = STATE.FRESH;
                        break;                                        
                    }
                    case HASSTATE:
                    {
                        if(line.indexOf("zip:") == 0 &&curListing != null)
                        {
                            try
                            {
                                curListing.zip_= Integer.parseInt(line.substring("zip:".length()));
                                state = STATE.HASZIP;
                                break;                                                        
                            }
                            catch(Exception e)
                            {
                                curListing = null;
                                state = STATE.FRESH;
                                break;                                                                                        
                            }
                        }
                        state = STATE.FRESH;
                        break;                                                            
                    }
                    case HASCITY:
                    {
                        if(line.indexOf("state:") == 0 &&curListing != null)
                        {
                            try
                            {
                                curListing.state_= line.substring("state:".length());
                                state = STATE.HASSTATE;
                                break;                                                        
                            }
                            catch(Exception e)
                            {
                                curListing = null;
                                state = STATE.FRESH;
                                break;                                                                                        
                            }
                        }                    
                        state = STATE.FRESH;
                        break;                                        
                    }
                    case HASSTREET:
                    {
                        if(line.indexOf("city:") == 0 &&curListing != null)
                        {
                            try
                            {
                                curListing.city_ = line.substring("city:".length());
                                state = STATE.HASCITY;
                                break;                                                        
                            }
                            catch(Exception e)
                            {
                                state = STATE.FRESH;
                                break;                                                                    
                            }
                        }                    
                        state = STATE.FRESH;
                        break;                                        
                    }
                    case HASLOC:
                    {
                        if(line.indexOf("address:") == 0 &&curListing != null)
                        {
                            try
                            {
                                curListing.street_ = line.substring("address:".length());
                                state = STATE.HASSTREET;
                                break;                                                        
                            }
                            catch(Exception e)
                            {
                                state = STATE.FRESH;
                                break;                                                                    
                            }
                        }
                        state = STATE.FRESH;
                        break;                                        
                    }
                    case HASNAME:
                    {
                        if(line.indexOf("loc:") == 0 &&curListing != null)
                        {
                            try
                            {
                                parserutils.setGeoInfo(curListing, line.substring("loc:".length()));
                                state = STATE.HASLOC;
                                break;                                                        
                            }
                            catch(Exception e)
                            {
                                state = STATE.FRESH;
                                break;                                                                    
                            }
                        }
                        state = STATE.FRESH;
                        break;                                        
                    }
                    case FRESH:
                    {
                        if(line.indexOf("name:") == 0)
                        {
                            try
                            {
                                if(curListing == null) {curListing = new Listing();}
                                curListing.name_ = line.substring("name:".length());
                                state = STATE.HASNAME;
                                break;                                                        
                            }
                            catch(Exception e)
                            {
                                state = STATE.FRESH;
                                break;                                                                    
                            }
                        }
                        state = STATE.FRESH;
                        break;                                        
                    }
                    default: 
                        break;
                }
            } catch (Throwable t) {
                System.out.println("BAD LINE: "+cnt+"  :   "+line);
                t.printStackTrace();
            }
        }
    }
    
}
