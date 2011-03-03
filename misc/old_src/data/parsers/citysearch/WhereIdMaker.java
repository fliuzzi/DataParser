package com.where.data.parsers.citysearch;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.NIOFSDirectory;

public class WhereIdMaker {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws CorruptIndexException 
	 */
	public static void main(String[] args) throws CorruptIndexException, IOException 
	{
		String indexdir = "/h/csdata/latest_idxbuild";//args[0];
		String outputFile = "/h/csdata/csid2whereid.txt";//args[1];
		IndexReader reader = IndexReader.open(new NIOFSDirectory(new File(indexdir)));
		int maxDoc = reader.maxDoc();
		PrintWriter ow = new PrintWriter(new FileWriter(outputFile));
		ow.println("csid\twhereid");
		for(int docId = 0; docId < maxDoc; ++docId)
		{
			if(docId %1000 == 0){System.out.print("=");}
			if(docId %80000 == 0){System.out.println("");}
			
			Document doc = reader.document(docId);
			int csId = Integer.parseInt(doc.get("listingid"));
			int whereId = docId+1;
			ow.println(csId + "\t" + whereId);
		}
		ow.close();
		reader.close();
	}

}
