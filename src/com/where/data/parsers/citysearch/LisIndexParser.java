package com.where.data.parsers.citysearch;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.NIOFSDirectory;

import com.where.commons.feed.citysearch.CSListing;
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
    
    public static void processReviews(String input, String output) throws IOException
    {
        NIOFSDirectory dir = new NIOFSDirectory(new File(input));
        
        IndexSearcher searcher = new IndexSearcher(dir);
        
        TermQuery query = new TermQuery(new Term("market", "boston ma metro"));
        TopDocs td = searcher.search(query, Integer.MAX_VALUE);
        ScoreDoc [] sdocs = td.scoreDocs;
        BufferedWriter bw = new BufferedWriter(new FileWriter(new File(output)));
        bw.newLine();
        for(int i = 0, max = sdocs.length; i < max; i++)
        {
        	Document doc = searcher.getIndexReader().document(sdocs[i].doc);
            String rawCsId = doc.get("listingid");
            StringBuilder sb = new StringBuilder();
            byte[] bytes = doc.getBinaryValue("poi");
			if(bytes != null) {			
				try {
					ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
					CSListing poi = (CSListing)in.readObject();
					in.close();
					
					List<com.where.commons.feed.citysearch.Review> editorials = poi.editorials();
					if(editorials != null && !editorials.isEmpty()) {
						sb.append(rawCsId+"\t");
						boolean has = false;
						for(com.where.commons.feed.citysearch.Review review: editorials) {
							String rev = review.getReview();
							if(has) sb.append(",");
							if(rev != null) {
								rev = rev.replaceAll(",", " ");
								rev = rev.replaceAll("\t", " ");
								rev = rev.replaceAll("\n", " ");
								sb.append(rev);
								has = true;
							}
						}
			           if(has) {
			        	   bw.write(sb.toString());
			        	   bw.newLine();
			           }
					}
				} catch (Throwable t) {
					System.err.println("*** ERR: "+rawCsId);
					t.printStackTrace();
				}
			}
        }
        bw.close();
        searcher.close();
    }
    
    
    public static void main(String [] args) throws IOException
    {
    	if(args == null || (args.length != 2 && args.length != 3)) {
    		System.out.println("Usage: LisIndexParser <input file> <output file> <optional: true - parse for reviews / false or none process ids");
    		return;
    	}
        String inputFile = args[0];
        String outputFile = args[1];
        
        if(args.length == 2) {
        	processIndex(inputFile, outputFile);
        } else {
        	processReviews(inputFile, outputFile);
        }
    }
}
