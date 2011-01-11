package com.where.data.parsers.localeze;


import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.spell.JaroWinklerDistance;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.NIOFSDirectory;

import com.where.commons.feed.citysearch.CSListing;
import com.where.commons.feed.citysearch.search.query.Search;
import com.where.commons.feed.citysearch.search.query.SearchResult;
import com.where.commons.feed.citysearch.search.query.Search.SearchCriteria;

public class LocalEzeCitySearchStep1Merger
{
    private static Log logger = LogFactory.getLog(LocalEzeCitySearchStep1Merger.class.getName());
    private Search citySearcher_;

    private String citySearchIndexDir_;
    private String localEzeCitySearchDir_;
    private String outputDir_;
    private String outputDupDir_;
    private IndexReader csReader_;
    private IndexSearcher csSearcher_;
    private IndexReader leReader_;
    private IndexWriter outputWriter_;
    private IndexWriter dupWriter_;
    public final static String PHONE = "phone";
    public final static String ADDRESS = "address";
    public final static String NAME = "companyname";
    public final static String STREET_ADDRESS = "street_address";
    JaroWinklerDistance dist_ = new JaroWinklerDistance();

    public LocalEzeCitySearchStep1Merger(String citySearchIndexDir, String localEzeCitySearchDir, String outputDir, String outputDupDir)
    {
        citySearchIndexDir_ = citySearchIndexDir;
        citySearcher_ = new Search();
        citySearcher_.setIndexPath(citySearchIndexDir_);

        localEzeCitySearchDir_ = localEzeCitySearchDir;
        outputDir_ = outputDir;
        outputDupDir_ = outputDupDir;
    }
    
    public void initializeReaders() throws CorruptIndexException, IOException
    {
        csSearcher_ = new IndexSearcher(new NIOFSDirectory(new File(citySearchIndexDir_)));
        csReader_= csSearcher_.getIndexReader();
        leReader_ = IndexReader.open(new NIOFSDirectory(new File(localEzeCitySearchDir_)));        
    }
    
    public void initializeWriters() throws CorruptIndexException, LockObtainFailedException, IOException
    {
        Analyzer an = new StandardAnalyzer(org.apache.lucene.util.Version.LUCENE_30);
        outputWriter_ = new IndexWriter(FSDirectory.open(new File(outputDir_)), an, IndexWriter.MaxFieldLength.LIMITED);
        dupWriter_ = new IndexWriter(FSDirectory.open(new File(outputDupDir_)), an, IndexWriter.MaxFieldLength.LIMITED);        
    }
    
    public void initialize() throws CorruptIndexException, IOException
    {
        logger.info("starting initialization");
        initializeReaders();
        initializeWriters();
        logger.info("finished initialization");
    }
    
    @Override
    public void finalize()
    {
        try
        {
            csReader_.close();
            leReader_.close();
            outputWriter_.close();
            dupWriter_.close();
        } 
        catch (IOException e)
        {
            logger.error(e.getStackTrace());
        }        
    }
        
    public final void addOutputDoc(Document doc) throws CorruptIndexException, IOException
    {
        outputWriter_.addDocument(doc);                        
    }

    public final void addDupDoc(Document doc) throws CorruptIndexException, IOException
    {
        dupWriter_.addDocument(doc);                        
    }

    
    protected List<CSListing> searchByLocation(double lat, double lon, String name, String addressdata) 
    {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setLat(lat);
        criteria.setLng(lon);
        criteria.setMiles(0.5);
        criteria.setItemsPerPage(10);        
        if(!isEmpty(addressdata)) criteria.setLocation(addressdata);
        criteria.setSortByRelevance(true);
        SearchResult result = null;
        try 
        {
            result = citySearcher_.geoSearch(criteria);
        } 
        catch (Throwable t) {
            logger.error(t);
        }
        return (result != null ? result.pois() : null);
    }

    protected double minNameDistance = .88;
    protected double minAddressDistance = .88;

    protected boolean isMatchingListing(String name, String address, String phone, CSListing listing, JaroWinklerDistance distance)
    {
        if(distance.getDistance(name, listing.getName().toLowerCase().trim()) < minNameDistance)
        {
            return false;
        }
        if(phone.equals(listing.getPhone().toLowerCase().trim())){return true;}
        if(distance.getDistance(address, listing.getName().toLowerCase().trim()) >= minAddressDistance)
        {
            return true;
        }
        return false;
    }
    private static int numProcessingThreads = 12;
    
    public void processIndices() throws CorruptIndexException, IOException, InterruptedException, ExecutionException
    {
        final JaroWinklerDistance jwd = new JaroWinklerDistance();
        logger.info("Started duplicates processing");
        int cnt = 0;
        ExecutorService thePool = Executors.newFixedThreadPool(numProcessingThreads);
        ArrayList<Future<?>> jobs = new ArrayList<Future<?>>();
        for(int i = 0, maxDoc = leReader_.maxDoc(); i < maxDoc; i++)
        {
            cnt++;
            final int idx =  cnt;
            final Document testDoc = leReader_.document(i);

            jobs.add(thePool.submit(new Runnable(){
                public void run(){
                    try
                    {
                        double lat = Double.parseDouble(testDoc.get("latitude_range"));                    
                        double lng = Double.parseDouble(testDoc.get("longitude_range"));
                        String name = testDoc.get("rawname");
                        String address = testDoc.get("address");
                        if(!isEmpty(address)){address = address.trim();}
                        if(isEmpty(address)) {address = null;}
                        String phone = testDoc.get("phone");
                        String pid = testDoc.get("pid");
                        List<CSListing> listings = null;
                        try
                        {
                            listings = searchByLocation(lat, lng, name, address);                
                        }
                        catch(Exception e)
                        {
                            logger.warn(e);
                            Document errDoc = new Document();
                            errDoc.add(new Field("localeze_docid", pid, Field.Store.YES, Field.Index.NOT_ANALYZED));
                            errDoc.add(new Field("err", "error thrown in parsing", Field.Store.YES, Field.Index.NOT_ANALYZED));
                            try
                            {
                                dupWriter_.addDocument(errDoc);
                            } 
                            catch (Exception e1)
                            {
                                logger.error(e1);
                            }                    
                            return;
                        }
                      
                        boolean foundmatch = false;
                        for(CSListing listing: listings)
                        {
                            if(isMatchingListing(name, address, phone, listing, jwd))
                            {
                                foundmatch = true;
                                Document errDoc = new Document();
                                errDoc.add(new Field("localeze_docid", pid, Field.Store.YES, Field.Index.NOT_ANALYZED));
                                errDoc.add(new Field("dup_csid", listing.getListingId(), Field.Store.YES, Field.Index.NOT_ANALYZED));
                                try
                                {
                                    dupWriter_.addDocument(errDoc);
                                } catch (Exception e)
                                {
                                    logger.error(e);
                                }                    
                                break;
                            }   
                        }
                        if(!foundmatch)
                        {
                            try
                            {
                                outputWriter_.addDocument(testDoc);                            
                            }
                            catch(Exception e)
                            {
                                logger.error(e);
                            }
                        }
                        
                    }
                    finally
                    {
                        if(idx% 250 == 0){logger.info("count: " + idx);}                        
                    }
                }
            }));
            
            if(cnt%2000 == 0)
            {
                for(Future<?> job : jobs)
                {
                    job.get();
                }
                jobs.clear();
            }
        }
        
        logger.info("Finished duplicates processing");        
        csReader_.close();
        leReader_.close();
        outputWriter_.close();
        dupWriter_.close();
        logger.info("Closed all files and threads");       
    }
    
    public static boolean isEmpty(String s)
    {
        return s== null?true: s.isEmpty();
    }
    
    float nameThreshold = .88f;
    float addressThreshold = .88f;
    
    protected String isDuplicateDocument(Document doc1, Document doc2) throws IOException
    {
        String srcName = doc1.get(NAME).trim().toLowerCase();
        String tgtName = doc2.get("raw_name").trim().toLowerCase();
        boolean sameName = false;
        if(isEmpty(srcName))
        {
            return "No name!";
        }
        if(!isEmpty(tgtName))
        {
            //float dist = dist_.getDistance(srcName, tgtName);
            //if(dist > nameThreshold) {sameName = true;}
            if(srcName.equals(tgtName)) {sameName = true;}
            else{return null;}
        }
        else{return null;}
        
        boolean samePhone = false;
        String phone = doc1.get(PHONE);
        String phone2 = doc2.get(PHONE);
        if(phone != null && phone2 != null);
        {
            samePhone = phone.equals(phone2);            
        }
        
        String address = doc1.get(ADDRESS);
        String address2 = doc2.get(STREET_ADDRESS);
        boolean sameAddress = false;
        if(isEmpty(address))
        {
            return "No address!";
        }
        if(!samePhone && isEmpty(address2))
        {
            return null;
        }
        if(!isEmpty(address2))
        {
            //float dist = dist_.getDistance(address, address2);
            //if(dist > addressThreshold) {sameAddress = true;}
            if(address.equals(address2)){sameAddress = true;}
        }
        
        StringBuilder buf = new StringBuilder();

        if(samePhone && sameName)
        {
            buf.append(" docId " + "-phone ");
        }
        if(sameAddress && sameName)
        {
                buf.append(" docId " + "-address");
        }            
        if(buf.length() == 0){return null;}
        return buf.toString().trim();
    }
    
    public static void main(String[] args) throws Exception 
    {
        String citySearchIndexDir = args[0];
        String localEzeCitySearchDir = args[1];
        String outputDir = args[2];
        String outputDupDir = args[3];

        LocalEzeCitySearchStep1Merger merger = 
            new LocalEzeCitySearchStep1Merger(citySearchIndexDir, 
                                              localEzeCitySearchDir, 
                                              outputDir, 
                                              outputDupDir);
        merger.initialize();
        merger.processIndices();
    }
}
