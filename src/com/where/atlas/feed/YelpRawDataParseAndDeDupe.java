package com.where.atlas.feed;

import java.io.File;
import java.io.FileInputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class YelpRawDataParseAndDeDupe
{

    
    public static void main(String[] args)
    {
        if(args.length != 3)
            {
            System.err.println("USAGE: program will go through a directory of yelp raw data zips");
            System.err.println("ARG1: directory of raw data .zips");
            System.err.println("ARG2: CS 'lis' index");
            System.err.println("ARG3: Output file target path");
            return;
            }
        try{
            File zipDirectory = new File(args[0]);
            File[] zipFiles = zipDirectory.listFiles();
            
            if(zipFiles == null){
                System.err.println("Directory Error");
                return;
            }
            else{
                YelpRawDataParser parser = new YelpRawDataParser(new YelpParserUtils(args[1],args[2]));
                
                for(int x = 0; x < zipFiles.length; x++)
                {
                    System.out.println("Zip File #"+(x+1)+" : " + zipFiles[x].getName());
                    ZipFile zipFile = new ZipFile(zipFiles[x]);
                    
                    String state = "";
                    
                    for (Enumeration<? extends ZipEntry> e = zipFile.entries(); e.hasMoreElements();) {
                        
                        ZipEntry entry = (ZipEntry) e.nextElement();
                        parser.parse(new ConsoleOutputCollector(), zipFile.getInputStream(entry));
        
                        String currentState = YelpRawDataParser.currentState;
                        if(currentState != null && !currentState.equals(state))
                        {
                            System.out.println(YelpRawDataParser.currentState);
                            state = YelpRawDataParser.currentState;
                        }
                    }
                    zipFile.close();
                }
                parser.closeWriter();
                
            }
        }
        catch(Throwable e){
            e.printStackTrace();
            }
        

    }

}
