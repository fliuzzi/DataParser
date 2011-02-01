package com.where.atlas.feed;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;

public class YelpParseAndIndex
{
    public static void main(String[] args) throws IOException{
        if(args.length != 3)
        {
            System.err.println("USAGE:");
            System.err.println("old index file: path to old index to de-dupe against");
            System.err.println("new data input directory: new directory to merge in");
            System.err.println("new data output file : directory to write output file");
            return;
        }
        
        YelpParserUtils parserutils = new YelpParserUtils(args[0], args[1], args[2]);
        
        //loop through file array passing each file input stream to YelpParser.parse()
        
        File[] ratingsFiles = parserutils.getRatings();
        int numFiles = ratingsFiles.length;
        
        YelpParser yelpparser = new YelpParser(parserutils);
        FileInputStream stream;
        File file;
        
        
        try{
           
            for(int i = 0; i < numFiles; i++)
            {
                file = ratingsFiles[i];
                stream = new FileInputStream(file);
                yelpparser.parse(new ConsoleOutputCollector(), stream);
            }
        }
        catch(Exception e)
        {
            System.err.println("IOError - ratings files!");
            e.printStackTrace();
        }
            
        
        
        //close streams
    }
}
