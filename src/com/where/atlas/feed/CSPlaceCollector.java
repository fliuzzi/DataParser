package com.where.atlas.feed;

import com.where.atlas.CSPlace;
import com.where.atlas.Place;

public class CSPlaceCollector implements PlaceCollector
{
    private CSListingIndexer indexer;
    private long counter;
    
    
    public CSPlaceCollector(CSParserUtils csparserutil)
    {
        indexer = csparserutil.getIndexer();
        counter = 0;
    }
    
    /**
     * Collect a new place
     * @param place - place to collect
     */
    public void collect(Place place)
    {
        displayProgress(++counter);
        
        CSParser.index((CSPlace)place, indexer);
    }
    
    private void displayProgress(long counter)
    {
        //approx 3.5 million CS places Jan 26 2011
        // 175,000 = 5%
        
        if(counter % 5000 == 0)
            System.out.print("+");
        if(counter % 175000 == 0)
            System.out.println("   ~"+(counter/175000)*5+"%");
    }

    /**
     * Log bad input. 
     * @param input - bad input 
     * @param reason - exception that caused it
     */
    public void collectBadInput(Object input, Exception reason)
    {
        //not supported as of yet
    }
}
