package com.where.atlas.feed.citysearch.citysearchJSONdetails;

import com.where.atlas.feed.PlaceCollector;
import com.where.atlas.feed.citysearch.citysearchJSONdetails.CSListingIndexer;
import com.where.place.CSPlace;
import com.where.place.Place;

public class CSCollectAndIndex implements PlaceCollector
{
    
    private CSListingIndexer indexer;
    private long counter;
    private boolean isAdvertiser;
    private CSListingIndexer spAltCategoryIndexer;
    
    
    public CSCollectAndIndex(CSParserUtils csparserutil)
    {
        indexer = csparserutil.getIndexer();
        isAdvertiser = csparserutil.isAdvertiserFeed();
        spAltCategoryIndexer = csparserutil.getAdvertiserIndexer();
        counter = 0;
    }
    
    /**
     * Collect a new place
     * @param place - place to collect
     */
    public void collect(Place place)
    {
        displayProgress(++counter);
        
        
        
        if(isAdvertiser)
        {
            org.apache.lucene.document.Document doc = CSListingDocumentFactory.createCategoryDocument((CSPlace)place);
            if(doc == null) return;
            
            spAltCategoryIndexer.index(doc);
        }
        
        
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
