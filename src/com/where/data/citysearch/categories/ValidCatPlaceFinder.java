package com.where.data.citysearch.categories;

import gnu.trove.TLongHashSet;
import gnu.trove.TLongIntHashMap;
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
    public static void main(String [] args) throws IOException
    {
        String method = args[0];
        
        if(method.equals("ratings"))
        {
            generateRatingsFile(args);
        }
        else if(method.equals("places"))
        {
            generatePoisFile(args);
        }
    }
    
    private static int numPois = 0;
    //ratings /home/michael/viz/category/valid_placeids.txt 
    // /h/csdata/experimental/input/merged_12_23.txt  /h/csdata/experimental/input/merged_12_23_filtered_by_category.txt
    public static void generateRatingsFile(String[] args) throws IOException
    {
        String validPOIsFile = args[1];
        String ratingsInput  = args[2];
        String ratingsOutput= args[3];
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
    // places /home/michael/viz/category/playcatcount.txt 
    // /h/csdata/experimental/input/merged_12_23_filtered_by_play_category_sorted.txt 
    ///idx/fixed_cat_lis  /home/michael/viz/category/playcat_places.txt 20 2    
    public static void generatePoisFile(String[] args) throws IOException
    {
        String catFile    = args[1];
        String oldRatings = args[2];
        String lisFile    = args[3];        
        String outputFile = args[4];
        int minHits = Integer.parseInt(args[5]);
        int minRatingsPerson = Integer.parseInt(args[6]);
        
        HashSet<String> cats = loadValidCats(catFile, minHits);
        TLongHashSet validPOIs = loadPOIs(lisFile, cats);
        validPOIs = verifyRecCount(oldRatings, validPOIs, minRatingsPerson);
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
    
    private static TLongHashSet verifyRecCount(String catFile, TLongHashSet candidates, int minCount) 
        throws NumberFormatException, IOException
    {
        TLongHashSet output = new TLongHashSet();
        TLongIntHashMap cntMap = new TLongIntHashMap();
        BufferedReader reader = new BufferedReader(new FileReader(new File(catFile)));        
        String line;
        while((line = reader.readLine()) != null)
        {
            String [] vals = line.split(",");
            long placeId = Long.parseLong(vals[1]);
            if(candidates.contains(placeId))
            {
                cntMap.adjustOrPutValue(placeId, 1, 1);
            }
        }        
        TLongIterator iter = candidates.iterator();
        while(iter.hasNext())
        {
            long val = iter.next();
            int cnt = cntMap.get(val);
            if(cnt > minCount)
            {
                output.add(val);
            }
        }

        return output;
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
