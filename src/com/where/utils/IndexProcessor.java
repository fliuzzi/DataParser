package com.where.utils;

import java.io.File;

import org.apache.lucene.document.Document;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.index.IndexReader;

public abstract class IndexProcessor
{
    private String index_;
    
    public IndexProcessor(String index){
        index_ = index;
    }

    public abstract void processDocument(Document doc) throws Exception;
    public abstract void finishProcessing() throws Exception;
    
    public void readDocuments() throws Exception{
        IndexReader reader = IndexReader.open(new NIOFSDirectory(new File(index_)));
        int max = reader.maxDoc();
        for (int i = 0; i < max ; i++)
        {
            Document doc = reader.document(i,null);
            processDocument(doc);
        }
        reader.close();
        finishProcessing();
    }
}