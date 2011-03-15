package com.where.atlas.feed.cslists;

import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.NIOFSDirectory;

import com.where.commons.feed.citysearch.search.query.Profile;

public class CSListParseAndIndex
{
    public static void main(String args[])
    {
        if(args.length != 5)
        {
            System.err.println("Usage: CSListIndexer");
            System.err.println("\t Input: existing cslists index dir");
            System.err.println("\t Input: existing ad index dir");
            System.err.println("\t Input: CSLists JSON file (to be indexed)");
            System.err.println("\t Output: index dir");
            System.err.println("\t Output: DYM .txt file");
            return;
        }
        
        Profile listprofile = new Profile();
        
        try{
        	IndexSearcher smbsearcher = new IndexSearcher(new NIOFSDirectory(new File("/idx/smb")));
            Profile.setSmbSearcher(smbsearcher, new ReentrantReadWriteLock());
            listprofile.setIndexPath(args[0]);
            listprofile.setAdIndexPath(args[1]);
            listprofile.setLocalezeIndexPath("/idx/localeze");
        	
        	
            CSListParserUtils parserutils = 
                    new CSListParserUtils(listprofile,args[2],args[3],args[4]);
            CSListCollectAndIndex collector = new CSListCollectAndIndex(parserutils);
            
            new CSListParser(parserutils).parse(collector,new FileInputStream(new File(args[2])));
            
            parserutils.finishProcessing();
            collector.closeWriters();
            System.out.println("Bad " + collector.getNumBadCounted());
            System.out.println("Indexed "+collector.getNumGoodCounted()+" lists.");
        }
        catch(Exception e){
            System.err.println("EXCEPTION\n");
            e.printStackTrace();
        }
              
   }
}
