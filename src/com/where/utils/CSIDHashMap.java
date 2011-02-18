package com.where.utils;

import gnu.trove.TIntIntHashMap;
import java.io.IOException;

import org.apache.lucene.document.Document;

public class CSIDHashMap extends IndexProcessor
{
    TIntIntHashMap idmap;
    
    
    public CSIDHashMap(String indexPath) throws IOException
    {
        super(indexPath);
        idmap = new TIntIntHashMap();
        System.out.print("Generating WhereID Map.\t");
        
    }
    
    public void processDocument(Document doc) throws Exception{
        
        String pid = doc.get("listingid");
        String whereid = doc.get("whereid");
        
        if(pid != null)
        {
            idmap.put(Integer.parseInt(pid), Integer.parseInt(whereid));
        }
    }
    
    public void finishProcessing() throws Exception{
        System.out.println("Done.");
    }
    

    public TIntIntHashMap getIdMap()
    {
        return idmap;
    }
}