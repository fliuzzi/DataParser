package com.where.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Set;

import org.apache.lucene.document.Document;

import com.where.atlas.feed.YelpRawDataParser;

public class CityCSID extends IndexProcessor
{
    
    String lisIndexPath;
    String targetFilePath;
    String querystr;
    Set<String> citysubset;
    long counter;
    private final String del = "\t";
    
    ArrayList<String> csidlist;
    BufferedWriter csidWriter,zipWriter;
    
    public static String clean(String name)
    {
        return name.replace("\t","").replace("\n", "");
    }
    
    
    public CityCSID(String indexPath, String query, String csidoutput,String zipOutput) throws IOException
    {
        super(indexPath);
        lisIndexPath = indexPath;
        querystr = query;
        targetFilePath = csidoutput;
        csidlist = new ArrayList<String>();
        counter=0;
        
        citysubset = YelpRawDataParser.populateMapFromTxt(new Scanner(new File("/home/fliuzzi/data/finalyelprawdata/citySUBSET.txt")));
        
        csidWriter = new BufferedWriter(new FileWriter(csidoutput));
        zipWriter = new BufferedWriter(new FileWriter(zipOutput));
    }
    
    public void processDocument(Document doc) throws Exception{
        //grab pid and whereids from doc
        String listingid = doc.get("listingid");
        
        if(listingid != null && citysubset.contains(listingid))
        {
            //csid file
            csidWriter.write(listingid+del+CityCSID.clean(doc.get("name"))+del
                    +doc.get("latitude_range")+del+doc.get("longitude_range"));
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
