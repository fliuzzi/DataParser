package com.where.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

import com.where.atlas.feed.yelp.YelpRawDataParser;

public class CitySubset
{
    public static void main(String[] args)
    {
        if(args.length != 3)return;
        //ARG0: input: city csid list
        //ARG1: input: neighborhoodset list
        //ARG2: output:  subset list
        
        try{
            
            
            BufferedWriter writer = new BufferedWriter(new FileWriter(args[2]));
            
            Set<String> neighborSet = YelpRawDataParser.populateMapFromTxt(new Scanner(new File(args[1])));
            
            Set<String> citySet = YelpRawDataParser.populateMapFromTxt(new Scanner(new File(args[0])));
            System.out.println("Neighbor Set: " + neighborSet.size() + "\nCity Set: "+citySet.size());
            
            citySet.retainAll(neighborSet);
            System.out.print("size of subset: "+citySet.size()+"  --writing...");
            
            Iterator<String> cityiterator = citySet.iterator();
            
            while(cityiterator.hasNext())
            {
                writer.write(cityiterator.next());
                writer.newLine();
            }
            System.out.println("Done.");
            writer.close();
            
        }
        catch(Exception e)
        {
            System.out.println(e.getMessage());
        }
            
    }

}
