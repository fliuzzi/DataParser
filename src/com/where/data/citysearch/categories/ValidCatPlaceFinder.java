package com.where.data.citysearch.categories;

import gnu.trove.TLongHashSet;
import gnu.trove.TLongIterator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.NIOFSDirectory;

public class ValidCatPlaceFinder
{    
    private static int numPois = 0;
    // /home/michael/viz/category/valid_placeids.txt 
    // /h/csdata/experimental/input/merged_12_23.txt  /h/csdata/experimental/input/merged_12_23_filtered_by_category.txt
    public static void main(String[] args) throws IOException
    {
        String validPOIsFile = args[0];
        String ratingsInput  = args[1];
        String ratingsOutput= args[2];
        TLongHashSet validPOIs = loadPOIs(validPOIsFile);
        
        writeNewRatingsFile(ratingsInput, ratingsOutput, validPOIs);
    }
    
    private static TLongHashSet loadPOIs(String file) throws IOException
    {
        TLongHashSet retval = new TLongHashSet();
        BufferedReader reader = new BufferedReader(new FileReader(new File(file)));
        String line;
        while((line = reader.readLine()) != null)
        {
            line = line.trim();
            long placeId = Long.parseLong(line);
            retval.add(placeId);
        }
        return retval;
    }
    
    /**
     * @param args
     * @throws IOException 
     */
    
    // /home/michael/viz/category/catcount_sorted.txt 
    ///idx/fixed_cat_lis /home/michael/viz/category/valid_placeids.txt 20 
    // /h/csdata/experimental/input/merged_12_23.txt  /h/csdata/experimental/input/merged_12_23_filtered_by_category.txt
    public static void main1(String[] args) throws IOException
    {
        String catFile = args[0];
        String lisFile = args[1];
        String outputFile = args[2];
        int minHits = Integer.parseInt(args[3]);
        HashSet<String> cats = loadValidCats(catFile, minHits);
        TLongHashSet validPOIs = loadPOIs(lisFile, cats);
        System.out.print("Got " + validPOIs.size() + " valid POIs out of a possible " + numPois);
        TLongIterator iter = validPOIs.iterator();
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(outputFile)));
        while(iter.hasNext())
        {
            long nextVal = iter.next();
            writer.write(Long.toString(nextVal));
            writer.newLine();
        }
    }
    
    private static void writeNewRatingsFile(String ratingsInput, String ratingsOutput, TLongHashSet validPOIs) throws IOException
    {
        BufferedReader reader = new BufferedReader(new FileReader(new File(ratingsInput)));
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(ratingsOutput)));
        
        String line;
        while((line = reader.readLine()) != null)
        {
            String [] vals = line.split(",");
            long placeId = Long.parseLong(vals[1]);
            if(validPOIs.contains(placeId))
            {
                writer.write(line);
                writer.newLine();
            }
        }
        
        reader.close();
        writer.close();
    }
    
    
    private static HashSet<String> loadValidCats(String file, int minHits) throws IOException
    {        
        BufferedReader reader = new BufferedReader(new FileReader(new File(file)));
        HashSet<String> output =  new HashSet<String>();
        String line = "";
        while((line = reader.readLine()) != null)
        {
            line = line.trim();
            String [] vals = line.split("\t");
            int cnt = Integer.parseInt(vals[1]);
            if(cnt >= 20)
            {
                output.add(vals[0]);
            }
        }
        reader.close();
        return output;
    }

    private static TLongHashSet loadPOIs(String lisFile, HashSet<String> validCats) throws CorruptIndexException, IOException
    {
        TLongHashSet output = new TLongHashSet();
        IndexReader reader = IndexReader.open(new NIOFSDirectory(new File(lisFile)));
        numPois = reader.maxDoc();
        for(int i = 0; i < numPois; i++)
        {
            Document doc = reader.document(i);
            String catnames = doc.get("catnames");
            if(!StringUtils.isEmpty(catnames))
            {
                String[] catlist = catnames.split("\\|");
                for(String cat : catlist)
                {
                    if(validCats.contains(cat))
                    {
                        long id = Long.parseLong(doc.get("listingid"));
                        output.add(id);
                        break;
                    }
                }                
            }
        }
        reader.close();
        return output;
    }
}
