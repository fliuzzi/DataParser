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
    BufferedWriter csidWriter,zipWriter;
    
    
    public CityCSID(String indexPath, String query, String csidoutput,String zipOutput) throws IOException
    {
        super(indexPath);
        lisIndexPath = indexPath;
        querystr = query;
        targetFilePath = csidoutput;
        csidlist = new ArrayList<String>();
        counter=0;
        
        csidWriter = new BufferedWriter(new FileWriter(csidoutput));
        zipWriter = new BufferedWriter(new FileWriter(zipOutput));
    }
    
    public void processDocument(Document doc) throws Exception{
        //grab pid and whereids from doc
        String city = doc.get("market");
        
        if(city != null && city.indexOf("boston ma metro") > -1)
        {
            //csid file
            csidWriter.write(doc.get("listingid"));
            csidWriter.newLine();
            
            //zip code file
            zipWriter.write(doc.get("zip"));
            zipWriter.newLine();
            
            counter++;
        }
    }
    
    public void finishProcessing() throws Exception{
        csidWriter.close();
        zipWriter.close();
        System.out.println("Wrote "+counter+" CSIDS in the city: "+querystr);
    }
    
    public static void main(String args[])
    {
        if (args.length!=4) return;
        
        try{
            new CityCSID(args[0],args[1],args[2],args[3]).readDocuments();
        }
        catch(Exception e){
            System.err.println("IO ERROR\n");
            e.printStackTrace();
        }
    }
}
