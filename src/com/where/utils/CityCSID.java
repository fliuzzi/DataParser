package com.where.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.lucene.document.Document;

public class CityCSID extends IndexProcessor
{
    
    String lisIndexPath;
    String targetFilePath;
    String querystr;
    long counter;
    
    ArrayList<String> csidlist;
    BufferedWriter bufferedWriter;
    
    public CityCSID(String indexPath, String query, String output) throws IOException
    {
        super(indexPath);
        lisIndexPath = indexPath;
        querystr = query;
        targetFilePath = output;
        csidlist = new ArrayList<String>();
        counter=0;
        
        bufferedWriter = new BufferedWriter(new FileWriter(output));
    }
    
    public void processDocument(Document doc) throws Exception{
        //grab pid and whereids from doc
        String city = doc.get("city");
        
        if(city != null && city.indexOf("boston") > -1)
        {
            bufferedWriter.write(doc.get("listingid"));
            bufferedWriter.newLine();
            counter++;
        }
    }
    
    public void finishProcessing() throws Exception{
        bufferedWriter.close();
        System.out.println("Wrote "+counter+" CSIDS in the city: "+querystr);
    }
    
    public static void main(String args[])
    {
        if (args.length!=3) return;
        
        try{
            new CityCSID(args[0],args[1],args[2]).readDocuments();
        }
        catch(Exception e){
            System.err.println("IO ERROR\n");
            e.printStackTrace();
        }
        
        
    }
}
