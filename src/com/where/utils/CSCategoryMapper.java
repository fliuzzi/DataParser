package com.where.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.NIOFSDirectory;
import org.json.JSONObject;

import com.where.atlas.feed.yelp.YelpRawDataParser;

public class CSCategoryMapper extends IndexProcessor{

	static NIOFSDirectory dir;
	static IndexSearcher searcher;
	TermQuery query;
	BufferedWriter bw;
	JSONObject towrite;
	Set<String> citysubset;
	
	public CSCategoryMapper(String toWritePath) throws IOException
	{
		super("/idx/lis");
		bw = new BufferedWriter(new FileWriter(toWritePath));
		towrite = new JSONObject();
        citysubset = YelpRawDataParser.populateMapFromTxt(new Scanner(new File("/home/fliuzzi/data/citySUBSET.txt")));
	}
	
	public void processDocument(Document doc) throws Exception
	{
		
		String csid = doc.get("listingid");
		if(csid != null && citysubset.contains(csid))
        {
			//csid
			
			String category = doc.get("category");
			String thumbURL = doc.get("thumburl");
			String catname = new String();
			
			
			query = new TermQuery(new Term("catid", category));
	        TopDocs td = searcher.search(query, 1);
	        ScoreDoc [] sdocs = td.scoreDocs;
	        for(ScoreDoc sd : sdocs)
	        {
	        	Document d = searcher.getIndexReader().document(sd.doc);
	        	catname = d.get("catname");
	        }
			
	        
	        towrite.append(csid, thumbURL);
	        towrite.accumulate(csid, catname);
	        
        }
	}
    public void finishProcessing() throws Exception
    {
    	System.out.println("Starting write");
    	bw.write(towrite.toString());
    	bw.close();
    	System.out.println("Done.");
    }
    
	public static void main(String[] args) {
		if(args.length!=1)return;
		
		try{
			//path of index
			dir = new NIOFSDirectory(new File("/idx/lis/cat_index"));
	        
	        searcher = new IndexSearcher(dir);
	        new CSCategoryMapper(args[0]).readDocuments();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
        
	}

}
