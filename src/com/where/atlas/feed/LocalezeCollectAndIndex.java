package com.where.atlas.feed;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;

import com.where.atlas.Place;

public class LocalezeCollectAndIndex implements PlaceCollector
{
    private LocalezeParserUtils parserutils;
    private BufferedWriter err;
    
    private String idRaw;
    private File index;
    private Directory directory;
    private IndexWriter writer;
    private IndexSearcher searcher;
    
    LocalezeCollectAndIndex(LocalezeParserUtils Lparserutils)
    {
        parserutils = Lparserutils;
        
        String logFile = parserutils.getCompanyFilesPath() + "/errs.txt";
        
        try{
        err = new BufferedWriter(new FileWriter(logFile));
        }
        catch(Exception e){
            System.err.println("Error setting up logFile");
        }
        
        try{
            idRaw = parserutils.getIndexPath();
            index = new File(idRaw);
            index.mkdir();
            directory = new NIOFSDirectory(index);
            writer = new IndexWriter(directory, new SimpleAnalyzer(), true, MaxFieldLength.UNLIMITED);
            writer.setMergeFactor(100000);
            writer.setMaxMergeDocs(Integer.MAX_VALUE);        
            
            searcher = new IndexSearcher(new NIOFSDirectory(new File(parserutils.getIndexPath() + "/cmpcat")));
        }
        catch(Exception e)
        {
            System.err.println("Error setting up main index");
        }
    }
    
    //************************************************
    //    close writers!  (static called from main)
    //************************************************
    
    /**
     * Collect a new place
     * @param place - place to collect
     */
    public void collect(Place place)
    {
        System.out.println(place);
    }

    /**
     * Log bad input to err.txt 
     * @param input - bad input 
     * @param reason - exception that caused it
     */
    public void collectBadInput(Object input, Exception reason) 
    {
        try{
            err.newLine();                
            err.write(input + " reason: " + reason.getMessage());
            err.newLine(); 
            }
            catch(Exception e){
                System.out.println("Error writing logFile entry.");
            }
    }
    
    

}
