package com.where.data.parsers.citysearch;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.NIOFSDirectory;

import com.where.util.common.StringUtil;

public class LisIndexParser
{
    public static void processIndex(String input, String output) throws IOException
    {
        NIOFSDirectory dir = new NIOFSDirectory(new File(input));
        
        IndexSearcher searcher = new IndexSearcher(dir);
        
        TermQuery query = new TermQuery(new Term("market", "boston ma metro"));
        TopDocs td = searcher.search(query, Integer.MAX_VALUE);
        ScoreDoc [] sdocs = td.scoreDocs;
        BufferedWriter bw = new BufferedWriter(new FileWriter(new File(output)));
        bw.write("csid,name,neighborhood,lat, lng");
        bw.newLine();
        for(int i = 0, max = sdocs.length; i < max; i++)
        {
            Document doc = searcher.getIndexReader().document(sdocs[i].doc);
            String rawCsId = doc.get("listingid");
            String slat = doc.get("latitude_range");
            String slong= doc.get("longitude_range");
            if(StringUtil.isEmpty(rawCsId) || StringUtil.isEmpty(slong) || StringUtil.isEmpty(slat))
            {
                continue;
            }
            String name    = doc.get("raw_name");
            name = name.replace(",", " ");
            String neighborhood = doc.get("nbh_city");
            if(neighborhood == null)
            {
                neighborhood  = "";                   
            }
            if(!StringUtil.isEmpty(neighborhood))
            {
                neighborhood = neighborhood.replace(",", " ");                
            }
            double lat = Double.parseDouble(slat);
            double lng = Double.parseDouble(slong);
            
            bw.write(rawCsId+ "," + name + "," + lat + "," + lng);
            bw.newLine();
        }
        bw.close();
        searcher.close();
    }
    
    
    public static void main(String [] args) throws IOException
    {
        String inputFile = args[0];
        String outputFile = args[1];
        
        processIndex(inputFile, outputFile);
    }
}
