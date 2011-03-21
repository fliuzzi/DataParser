package com.where.data.citysearch;

import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntIterator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.NIOFSDirectory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class CatCounter
{

    /**
     * @param args
     * @throws IOException 
     * @throws CorruptIndexException 
     */
    public static void main(String[] args) throws CorruptIndexException, IOException
    {
        String fname = args[0];
        String output = args[1];
        IndexReader reader = IndexReader.open(new NIOFSDirectory(new File(fname)));
        TObjectIntHashMap<String> map = new TObjectIntHashMap<String>();
        for(int i = 0, max = reader.maxDoc();i < max; i++){
            String tmp = reader.document(i).get("category");
            if(!StringUtils.isEmpty(tmp))
            {
                String [] cats = tmp.split("\\|");
                for(String cat : cats)
                {
                    cat = cat.trim();
                    map.adjustOrPutValue(cat, 1, 1);
                }
            }
        }        
        reader.close();
        
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(output)));
        TObjectIntIterator<String> iter = map.iterator();
        while(iter.hasNext())
        {
            iter.advance();
            writer.write(iter.key() + "\\t" + iter.value());
            writer.newLine();
        }
        
        writer.close();
    }

}
