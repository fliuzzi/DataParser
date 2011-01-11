package com.where.data.parsers.localeze;


import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class TestJaroWinklerQuery
{
    public void testQuery() throws IOException
    {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File indexDir= new File(tempDir, "test.jarowinlerquery." + System.currentTimeMillis());
        if ( indexDir.exists() ) { indexDir.delete(); }
        Directory dir =FSDirectory.open(indexDir);

        IndexWriter writer = null;
        Analyzer an = new StandardAnalyzer(org.apache.lucene.util.Version.LUCENE_30);        
        writer = new IndexWriter(dir, an, IndexWriter.MaxFieldLength.LIMITED);

        Document d1 = new Document();
        d1.add(new Field("NAME", "poodle", Field.Store.YES, Field.Index.ANALYZED));
        //d1.add(new Field("LOCATION", "south boston", Field.Store.YES, Field.Index.NOT_ANALYZED));
        writer.addDocument(d1);

        Document d2 = new Document();
        d2.add(new Field("NAME", "foodle", Field.Store.YES, Field.Index.ANALYZED));
        //d2.add(new Field("LOCATION", "94 marion st", Field.Store.YES, Field.Index.NOT_ANALYZED));
        writer.addDocument(d2);        

        Document d3 = new Document();
        d3.add(new Field("NAME", "ppodle", Field.Store.YES, Field.Index.ANALYZED));
        writer.addDocument(d3);        
        
        Document d4 = new Document();
        d4.add(new Field("NAME", "poodlk", Field.Store.YES, Field.Index.ANALYZED));
        //d4.add(new Field("LOCATION", "asdasds", Field.Store.YES, Field.Index.NOT_ANALYZED));
        writer.addDocument(d4);        
        
        d4 = new Document();
        d4.add(new Field("NAME", "schnauzer", Field.Store.YES, Field.Index.ANALYZED));
        //d4.add(new Field("LOCATION", "asdasds", Field.Store.YES, Field.Index.NOT_ANALYZED));
        writer.addDocument(d4);        

        d4 = new Document();
        d4.add(new Field("NAME", "mauzer", Field.Store.YES, Field.Index.ANALYZED));
        //d4.add(new Field("LOCATION", "asdasds", Field.Store.YES, Field.Index.NOT_ANALYZED));
        writer.addDocument(d4);        

        writer.commit();
        writer.close();
        IndexReader reader = IndexReader.open(dir);
        
        IndexSearcher searcher = new IndexSearcher(reader);

        float minsim = .9f;
        Term poodleTerm = new Term("NAME", "poodle");

        JaroWinklerQuery jwq = new JaroWinklerQuery(poodleTerm, minsim);
        @SuppressWarnings("unused")
        TopDocs td2 = jwq.search(searcher, 1000);
    }
}
