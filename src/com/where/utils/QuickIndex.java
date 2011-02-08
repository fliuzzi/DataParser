package com.where.utils;

import java.io.File;

import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;

public class QuickIndex
{
    private IndexWriter writer;
    private int counter;
    private int max;
    
    public QuickIndex(String toreadpath, String towritepath, String maxcount){
        
        counter = 0;
        max = Integer.parseInt(maxcount);
        
        //make the writer
        try{
            Directory directory = new NIOFSDirectory(new File(towritepath));
            writer = new IndexWriter(directory, new SimpleAnalyzer(), true, MaxFieldLength.UNLIMITED);
            writer.setMergeFactor(100000);
            writer.setMaxMergeDocs(Integer.MAX_VALUE);
            
            System.out.println("Starting...");
            read(toreadpath,max);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

    }

    
    public void processDocument(Document doc) throws Exception{
        String prefixed = doc.getField("rawname").stringValue();
        
        //add prefix xYz_
        prefixed = "XxYz__" + prefixed;
        
        doc.removeFields("rawname");
        doc.removeFields("companyname");
        
        doc.add(new Field("rawname", prefixed, Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field("companyname", prefixed, Field.Store.YES, Field.Index.ANALYZED_NO_NORMS));
        
        
        writeDoc(doc);
    }
    
    private void writeDoc(Document doc)
    {
        try{
            writer.addDocument(doc);
            counter++;
        }
        catch(Exception e)
        {
            System.err.println("Error writing doc");
            e.printStackTrace();
        }
    }
    
    public void finishProcessing() throws Exception{
        
        System.out.println("Wrote "+counter+" documents to index.");
        writer.close();
    }
    
    
    public static void main(String args[])
    {
        //args: indextoread, indextowrite, document count
        
        try{
            
            if(args.length != 3) return;
            
            
            new QuickIndex(args[0],args[1],args[2]);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
    
    private void read(String path,int maxcount) throws Exception{
        IndexReader reader = IndexReader.open(new NIOFSDirectory(new File(path)));
        int max = maxcount;
        for (int i = 0; i < max ; i++)
        {
            Document doc = reader.document(i,null);
            processDocument(doc);
        }
        reader.close();
        finishProcessing();
    }

}
