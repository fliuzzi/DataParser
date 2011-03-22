package com.where.data.citysearch.categories;

import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntIterator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.NIOFSDirectory;

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
        String goodCatFile = args[1];
        HashSet<String> goodCats = new HashSet<String>();
        BufferedReader br = new BufferedReader(new FileReader(new File(goodCatFile)));
        String line = "";
        while((line = br.readLine()) != null)
        {
            line = line.trim();
            goodCats.add(line);
        }
        String output = args[2];
        IndexReader reader = IndexReader.open(new NIOFSDirectory(new File(fname)));
        TObjectIntHashMap<String> map = new TObjectIntHashMap<String>();
        for(int i = 0, max = reader.maxDoc();i < max; i++){
            String tmp = reader.document(i).get("catnames");
            if(!StringUtils.isEmpty(tmp))
            {
                String [] cats = tmp.split("\\|");
                for(String cat : cats)
                {
                    cat = cat.trim();
                    if(goodCats.contains(cat))
                    {
                        map.adjustOrPutValue(cat, 1, 1);
                    }
                }
            }
        }        
        reader.close();
        
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(output)));
        TObjectIntIterator<String> iter = map.iterator();
        while(iter.hasNext())
        {
            iter.advance();
            writer.write(iter.key() + "\t" + iter.value());
            writer.newLine();
        }
        
        writer.close();
    }

}
