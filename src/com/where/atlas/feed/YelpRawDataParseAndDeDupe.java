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
            System.err.println("USAGE:");
            System.err.println("ARG1: zip file");
            System.err.println("ARG2: CS 'lis' index");
            System.err.println("ARG3: Output file target path");
            return;
            }
        try{
            
            ZipFile zipFile = new ZipFile(new File(args[0]));
            YelpRawDataParser parser = new YelpRawDataParser(new YelpParserUtils(args[1],args[2]));
            String state = "";
            
            for (Enumeration<? extends ZipEntry> e = zipFile.entries(); e.hasMoreElements();) {
                
                ZipEntry entry = (ZipEntry) e.nextElement();
                parser.parse(new ConsoleOutputCollector(), zipFile.getInputStream(entry));

                if(!YelpRawDataParser.currentState.equals(state))
                {
                    System.out.println(YelpRawDataParser.currentState);
                    state = YelpRawDataParser.currentState;
                }
            }
            
            parser.closeWriter();
        }
        catch(Exception e){e.printStackTrace();}
        

    }

}
