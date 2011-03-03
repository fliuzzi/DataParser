package com.where.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;

import com.where.atlas.feed.yelp.YelpRawDataParser;

public class YelpRawDataDeDuper
{
    public static void main(String[] args)
    {
        final String del = "\t";
        
        if(args.length != 3) return;

        int stringCount=0;
        int uniqueCount=0;
        int dupeCount=0;
        Set<String> uniqueCitySet = new HashSet<String>();
        try{
            Set<Integer>reviewSet = new HashSet<Integer>();
            BufferedWriter writer = new BufferedWriter(new FileWriter(args[1]));
        
            Scanner reader = new Scanner(new File(args[0])).useDelimiter("\\n");
            StringBuilder strBuilder = new StringBuilder();
            
            Set<String> neighborSet = YelpRawDataParser.populateMapFromTxt(new Scanner(new File(args[2])));
            
            
            while(reader.hasNext())
            {
                StringTokenizer tokenizer = new StringTokenizer(reader.next(),"\t");
                
                if (strBuilder != null && strBuilder.length() > 0)
                    strBuilder = strBuilder.delete(0, strBuilder.length());
                
                stringCount=0;
                while(tokenizer.hasMoreTokens())
                {
                    strBuilder.append(tokenizer.nextToken()+del);
                    stringCount++;
                }
                strBuilder.delete(strBuilder.lastIndexOf(del),strBuilder.length());
                        
                    if(reviewSet.add(strBuilder.toString().hashCode()) && 
                                neighborSet.contains(strBuilder.substring(0, strBuilder.indexOf("\t"))))
                    {
                        uniqueCitySet.add(strBuilder.substring(0,strBuilder.indexOf("\t")));
                        writer.write(strBuilder.toString());
                        writer.newLine();
                        uniqueCount++;
                    }
                    else
                        dupeCount++;
            }
        }
        catch(Exception e)
        {
            System.out.println(e.getMessage());
        }
        
        System.out.println("Wrote "+uniqueCount+" uniques and de-duped "+dupeCount);
        System.out.println("amongst "+uniqueCitySet.size()+" unique pois");
    }

    
    
    
    
    public Set<String> populateMapFromTxt(Scanner in)
    {
        Set<String> txtSet = new HashSet<String>();
        
        
        while(in.hasNextLine())
        {
            txtSet.add(in.nextLine().trim());
        }
        in.close();
        return txtSet;
    }
}
