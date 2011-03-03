package com.where.atlas.feed.cslists;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.apache.lucene.document.Document;

import com.where.atlas.feed.PlaceCollector;
import com.where.place.CSListPlace;
import com.where.place.Place;

public class CSListCollectAndIndex implements PlaceCollector
{
    
    CSListParserUtils parserutils;
    PrintWriter dymwriter;
    int goodCounter;
    int badCounter;
    PrintWriter out;


    
    public CSListCollectAndIndex(CSListParserUtils csparserutils) throws IOException
    {
        parserutils = csparserutils;
        dymwriter = new PrintWriter(new FileWriter(parserutils.getDymFilePath()));
        goodCounter=0;
        out = new PrintWriter(new FileWriter(new File(new File(parserutils.getOutputDirPath()).getParent(), "badpois.txt")));
    }
    
    /**
     * Collect a new place
     * @param place - place to collect
     */
    public void collect(Place place)
    {
        try{
            
            //writes the dym.txt file by passing the writer and a placeList
            CSListParserUtils.writeDym(dymwriter, ((CSListPlace)place).toPlacelist());
            
            
            List<Document> docs = CSListParserUtils.newPlacelistDocuments(((CSListPlace)place).toPlacelist());
            
          
            for(Document doc:docs) {    
                parserutils.getWriter().addDocument(doc);
                
                
                if(++goodCounter%200 == 0) System.out.print("+");
                if(goodCounter%1000 == 0) System.out.println();
                //if(goodCounter%6000 == 0)
                   // parserutils.getWriter().optimize();
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
        badCounter++;
        out.println(input + "Reason: "+reason+"\n");
    }
    
    public int getNumGoodCounted()
    {
        return goodCounter;
    }
    
    public int getNumBadCounted()
    {
        return badCounter;
    }
    
    public void closeWriters()
    {
        dymwriter.close();
        out.close();
    }
}
