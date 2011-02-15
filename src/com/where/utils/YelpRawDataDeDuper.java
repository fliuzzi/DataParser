package com.where.utils;

import java.io.File;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;

public class YelpRawDataDeDuper
{
    
    
    public static void main(String[] args)
    {
        if(args.length != 1) return;
        
        Set<String>reviewSet = new HashSet<String>();
        try{
            Scanner reader = new Scanner(new File(args[0])).useDelimiter("\\n");
            
            
            for(int i =0 ; i<10;i++)
            {
                StringTokenizer tokenizer = new StringTokenizer(reader.next(),"\t");
                
                while(tokenizer.hasMoreTokens())
                    System.out.println(tokenizer.nextToken());
                
                System.out.println();
            }
        }
        catch(Exception e)
        {
            e.getMessage();
        }
    }

    
    
    public Set<String> populateMapFromTxt(Scanner in)
    {
        String line = null;
        Set<String> txtSet = new HashSet<String>();
        
        
        while(in.hasNextLine())
        {
            txtSet.add(in.nextLine().trim());
        }
        in.close();
        return txtSet;
    }
}
