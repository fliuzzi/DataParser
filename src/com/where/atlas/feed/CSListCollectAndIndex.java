package com.where.atlas.feed;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;

import com.where.atlas.CSListPlace;
import com.where.atlas.Place;

public class CSListCollectAndIndex implements PlaceCollector
{
    
    CSListParserUtils parserutils;
    PrintWriter dymwriter;

    
    public CSListCollectAndIndex(CSListParserUtils csparserutils) throws IOException
    {
        parserutils = csparserutils;
        dymwriter = new PrintWriter(new FileWriter(parserutils.getDymFilePath()));
    }
    
    /**
     * Collect a new place
     * @param place - place to collect
     */
    public void collect(Place place)
    {
        try{
            System.out.println("Collecting Place:\n"+(CSListPlace)place+"\n");
            
            //writes the dym.txt file by passing the writer and a placeList
            CSListParserUtils.writeDym(dymwriter, (CSListPlace)place);
            
            
            List<Document> docs = CSListParserUtils.newPlacelistDocuments((CSListPlace)place);
            
          
            int count=0;
            for(Document doc:docs) {    
                parserutils.getWriter().addDocument(doc);
                if(count%10000 == 0){
                    parserutils.getWriter().optimize();
                }
            }    
        }
        catch(Exception e){
            System.err.println("Error collecting place" + ((CSListPlace)place));
            e.printStackTrace();
        }
    }

    /**
     * Log bad input. 
     * @param input - bad input 
     * @param reason - exception that caused it
     */
    public void collectBadInput(Object input, Exception reason)
    {
        
    }
    
    public PrintWriter getDYMWriter()
    {
        return dymwriter;
    }
}
