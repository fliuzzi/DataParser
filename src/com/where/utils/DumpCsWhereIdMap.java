package com.where.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.lucene.document.Document;

/**
 * Dump the CS listing IDs and corresponding Where IDs to a map file 
 * @author ajay Feb 2, 2011
 */
public class DumpCsWhereIdMap extends IndexProcessor {
    
    private BufferedWriter writer;
    
    public DumpCsWhereIdMap(String indexPath, String outfile) throws IOException {
        super(indexPath);
        writer = new BufferedWriter(new FileWriter(outfile));
    }
    
    public void processDocument(Document doc) throws Exception {
    	writer.write(doc.get("listingid") + "|" + doc.get("whereid"));
    }
    
    public void finishProcessing() throws Exception{
        writer.close();
    }
    
    public static void main(String args[]) throws Exception {
        if (args.length != 3) {
        	System.out.println("usage: com.where.utils.DumpCsWhereIdMap <lucene_index_dir> <output_map_file>");
        	System.exit(1);
        }
        new DumpCsWhereIdMap(args[0],args[1]).readDocuments();
    }
}
