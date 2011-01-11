package com.where.data.parsers.yelp;

import gnu.trove.TLongHashSet;
import gnu.trove.TObjectLongHashMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.QueryParser.Operator;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.spell.JaroWinklerDistance;
import org.apache.lucene.spatial.geohash.GeoHashUtils;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.Version;

import com.where.commons.feed.citysearch.CSListingDocumentFactory;
import com.where.util.lucene.NullSafeGeoFilter;
import com.where.util.lucene.NullSafeGeoFilter.GeoHashCache;

public class Parser
{
    public final float withPhoneThreshold_ = .65f;
    public final float withoutPhoneThreshold_ = .9f;
    
    private GeoHashCache geoCache_;

    private IndexSearcher oldSearchIndexSearcher_;
    private String outputFileName_;
    private JaroWinklerDistance jw_ = new JaroWinklerDistance();
    private static String newline = System.getProperty("line.separator"); 
   
    private AtomicLong maxUserId_;
    private Object suIdMutex = new String();
    private TObjectLongHashMap<String> seenUserIds_ = new TObjectLongHashMap<String>();
    private Object srIdsMutex = new String();
    private Set<String> seenRecIds = new HashSet<String>();
    private Object spMutex = new String();
    private Map<String, TLongHashSet> seenPairs_ = new HashMap<String, TLongHashSet>();
    private File outputFile_;
    private FileOutputStream outputStream_;
    private FileChannel outputChannel_;
    private String newDataDir_;
    
    public Parser(String oldIndexPath, String newDataDir, String newIndexPath) throws CorruptIndexException, IOException
    {
        oldSearchIndexSearcher_ = new IndexSearcher(new NIOFSDirectory(new File(oldIndexPath)));
        geoCache_ = new GeoHashCache(CSListingDocumentFactory.LATITUDE_RANGE, CSListingDocumentFactory.LONGITUDE_RANGE,
                CSListingDocumentFactory.LISTING_ID, oldSearchIndexSearcher_.getIndexReader());
        outputFileName_ = newIndexPath;
        outputFile_ = new File(outputFileName_);
        outputStream_ = new FileOutputStream(outputFile_);
        outputChannel_ = outputStream_.getChannel();
        newDataDir_ = newDataDir;
    }

    protected ExecutorService thePool_ = Executors.newFixedThreadPool(4);
    
    public void runData() throws IOException, InterruptedException, ExecutionException
    {        
        processInputFiles(newDataDir_);
        outputStream_.close();
    }
        
    public void close() throws IOException
    {
        oldSearchIndexSearcher_.close();
        thePool_.shutdown();
    }
    
    public long getMaxUserId(String id2ItemMapFile)
    {
        return maxUserId_.longValue();
    }
    
    public void setMaxUserId(long id)
    {
        maxUserId_ = new AtomicLong(id);
    }
    
    public long incrementAndGetMaxUserId()
    {
        return maxUserId_.incrementAndGet();
    }
    
    public long getMaxId(String inputFile) throws IOException
    {
        long maxId =0;
        BufferedReader ir = new BufferedReader(new FileReader(new File(inputFile)));
        String line; 
        int cnt = 0;
        while((line = ir.readLine()) != null)
        {
            cnt++;
            line = line.trim();
            if(line.length() == 0){continue;}
            String [] splits = line.split("[\\s,]");
            try
            {
                long num =  Long.parseLong(splits[0].trim());
                if(num > maxId)
                {
                    maxId = num;
                }                
            }
            catch(Exception e)
            {
                System.out.println("bad user id: " + splits[0].trim() + " at line " + cnt);
            }
            
        }
        ir.close();
        return maxId;
    }

    long getUserId(String lookup)
    {
        synchronized(suIdMutex)
        {
            if(seenUserIds_.containsKey(lookup))
            {
                return seenUserIds_.get(lookup);            
            }
            else
            {
               long retval = incrementAndGetMaxUserId();
               seenUserIds_.put(lookup, retval);
               return retval;
            }            
        }
    }
    
    public static enum STATE {FRESH, HASNAME, HASLOC, HASSTREET, 
                              HASCITY, HASSTATE, HASZIP, HASPHONE, HASRATINGS};
    
    public static class InnerRating
    {
        public long userId_;
        public int rating_;
    }
                              
    public static class Listing
    {
        long csId_;
        String name_;
        String csName_ = "";
        boolean phoneMatch_ = false;
        double lat_;
        double lng_;
        String geoHash_;
        String street_;
        String city_;
        String state_;
        int zip_;
        String phone_;
        ArrayList<InnerRating> ratings_ = new ArrayList<InnerRating>();
    }
    
    int normalizePhone(String ph)
    {
        ph = ph.replaceAll("[\\-\\.\\(\\\\s]", "");
        int retval = -1;        
        try{retval = Integer.parseInt(ph);}
        catch(Exception e){}
        return retval;
    }
    
    public void setGeoInfo(Listing l, String s)
    {
        String [] lr = s.split(",");
        l.lat_ = Double.parseDouble(lr[0]);
        l.lng_ = Double.parseDouble(lr[1]);
        l.geoHash_ = GeoHashUtils.encode(l.lat_, l.lng_);
    }
    
    public void processRatings(Listing l, String s)
    {
        if(l == null) return;
        String [] all = s.split("\\|");
        for(String rec : all)
        {
            try
            {
                String [] pieces = rec.split(",");
                String recId = pieces[0];
                synchronized(srIdsMutex)
                {
                    if(!seenRecIds.contains(recId))
                    {
                        seenRecIds.add(recId);                    
                    }
                    else
                    {
                        continue;                    
                    }                    
                }
                String userIDraw = pieces [1];
                long userId = getUserId(userIDraw);
                TLongHashSet tmp = null;
                synchronized(spMutex)
                {
                    if(!seenPairs_.containsKey(recId))
                    {
                        tmp = new TLongHashSet();
                        seenPairs_.put(recId, tmp);
                    }                    
                }
                synchronized(tmp)
                {
                    if(!tmp.contains(userId))
                    {
                        tmp.add(userId);
                        InnerRating ir = new InnerRating();
                        ir.userId_ = userId;
                        ir.rating_ = Integer.parseInt(pieces[2]);                
                        l.ratings_.add(ir);                    
                    }                    
                }
            }
            catch(Exception e)
            {
                throw(new RuntimeException(e));
            }            
        }
    }
    
    protected boolean canUseListing(Listing l)
    {
        BooleanQuery bq = new BooleanQuery();        
        Analyzer analyzer = CSListingDocumentFactory.getAnalyzerWrapper();
        addLatLongQueryFromCriteria(bq, l.lat_, l.lng_);
        BooleanQuery mainQuery = new BooleanQuery();
        HashMap<String, Float> boosts = new HashMap<String, Float>();
        boosts.put(CSListingDocumentFactory.NAME,1.0f);
        String[] fields = { CSListingDocumentFactory.NAME };
        mainQuery.add(new TermQuery(new Term(
               CSListingDocumentFactory.PHONE,
               l.phone_)),
               Occur.SHOULD);

        MultiFieldQueryParser qp = new MultiFieldQueryParser(
                Version.LUCENE_30, fields, analyzer, boosts);
        qp.setDefaultOperator(Operator.OR);
        try
        {
            mainQuery.add(qp.parse(l.name_), Occur.SHOULD);
            bq.add(mainQuery, Occur.MUST);
            Filter wrapper = new QueryWrapperFilter(bq);
            Filter distanceFilter = 
                new NullSafeGeoFilter(wrapper, l.lat_, l.lng_, 1, geoCache_, CSListingDocumentFactory.GEOHASH);                     
            TopDocs td = oldSearchIndexSearcher_.search(bq, distanceFilter, 10);
            ScoreDoc [] sds = td.scoreDocs;
            for(ScoreDoc sd : sds)
            {
                Document d = oldSearchIndexSearcher_.getIndexReader().document(sd.doc);
                String name = d.get(CSListingDocumentFactory.NAME).toLowerCase().trim();
                String city = d.get(CSListingDocumentFactory.CITY).toLowerCase().trim();
                String left = name + " " + city;
                l.csName_ = left;
                String right = l.name_ + " " + l.city_;
                right = right.toLowerCase().trim();
                String phone = d.get(CSListingDocumentFactory.PHONE);
                boolean samePhone = phone.equals(l.phone_);
                l.phoneMatch_ = samePhone;
                float dist = jw_.getDistance(left, right);
                long csId = Long.parseLong(d.get(CSListingDocumentFactory.LISTING_ID));
                if(samePhone && dist > withPhoneThreshold_)
                {
                    l.csId_ = csId;
                    return true;
                }
                else if(dist > withoutPhoneThreshold_)
                {
                    l.csId_ = csId;
                    return true;                    
                }
            }
        } 
        catch (Exception e)
        {
            return false;
        }

        return false;
    }
                    
    protected void storeListing(Listing l) {
    	long placeId = l.csId_;
    	for(InnerRating ir : l.ratings_)
        {
            try
            {
                outputChannel_.write(ByteBuffer.wrap((ir.userId_ + "," + placeId + "," + ir.rating_ + newline).getBytes()));                
            }
            catch(Exception e){}
        }
    }
    
    protected void addLatLongQueryFromCriteria(BooleanQuery rawQuery, double lat, double lng)
    {
        Query latQ = NumericRangeQuery.newDoubleRange(
                CSListingDocumentFactory.LATITUDE_RANGE,
                lat-.5, lat+.5,
                true, true);

        Query longQ = NumericRangeQuery.newDoubleRange(
                CSListingDocumentFactory.LONGITUDE_RANGE,
                lng-.5, lng+.5,
                true, true);

        rawQuery.add(latQ, Occur.MUST);
        rawQuery.add(longQ, Occur.MUST);
    }
    
    public static class ListingChecker implements Runnable
    {
        Parser p_;
        Listing l_;
        public ListingChecker(Parser p, Listing l)
        {
            p_ = p;
            l_ = l;
        }
        @Override
        public void run()
        {
            if(p_.canUseListing(l_))
            {
            	p_.storeListing(l_);
            }
            
        }
    };

    
    public static ByteBuffer makeByteBuffer(String input)
    {        
        return ByteBuffer.wrap(input.getBytes());
    }
    
    public static String slurpFile(File file) throws IOException
    {
        FileInputStream stream = new FileInputStream(file);
        try
        {
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            return Charset.defaultCharset().decode(bb).toString();            
        }
        finally
        {
            stream.close();
        }
    }
    
    public class FileProcessor implements Runnable
    {
        public final File file_;
        public final Parser parser_;
        public FileProcessor(File f, Parser p)
        {
            file_ = f;
            parser_ = p;
        }
        public void run()
        {
            try
            {
                parser_.processInputFile(file_);                
            }
            catch(Exception e)
            {
                throw(new RuntimeException(e));
            }
        }
    }
    
    public void processInputFile(File file) throws IOException, InterruptedException
    {
        Listing curListing = null;
        STATE state = STATE.FRESH;
        String line = null;
        int cnt = 0;        
        BufferedReader inputReader = new BufferedReader(new StringReader(slurpFile(file)));
        while((line = inputReader.readLine()) != null)
        {
        	try {
	            line = line.trim(); 
	            switch(state)
	            {
	                case  HASRATINGS :
	                {
	                    if(line.indexOf("name:") == 0)
	                    {
	                        try
	                        {
	                            if(curListing != null)
	                            {
	                                ListingChecker lc = new ListingChecker(this, curListing);
	                                lc.run();
	                            }
	                            curListing = new Listing();
	                            curListing.name_= line.substring("name:".length());
	                            state = STATE.HASNAME;
	                            break;                            
	                        }
	                        catch(Exception e)
	                        {
	                            state = STATE.FRESH;
	                            break;                            
	                        }
	                    }
	                    state = STATE.FRESH;
	                    break;
	                }
	                case HASPHONE :
	                {
	                    if(line.indexOf("ratings:") == 0)
	                    {
	                        try
	                        {
	                            processRatings(curListing, line.substring("ratings:".length()));
	                            state = STATE.HASRATINGS;
	                            break;                            
	                        }
	                        catch(Exception e)
	                        {
	                            state = STATE.FRESH;
	                            break;                                                
	                        }
	                    }
	                    state = STATE.FRESH;
	                    break;                    
	                }
	                case HASZIP :
	                {
	                    if(line.indexOf("phone:") == 0 &&curListing != null)
	                    {
	                        try
	                        {
	                            curListing.phone_ =  CSListingDocumentFactory.cleanPhone(line.substring("phone:".length()));
	                            state = STATE.HASPHONE;
	                            break;                                                        
	                        }
	                        catch(Exception e)
	                        {
	                            curListing = null;
	                            state = STATE.FRESH;
	                            break;
	                        }
	                    }
	                    state = STATE.FRESH;
	                    break;                                        
	                }
	                case HASSTATE:
	                {
	                    if(line.indexOf("zip:") == 0 &&curListing != null)
	                    {
	                        try
	                        {
	                            curListing.zip_= Integer.parseInt(line.substring("zip:".length()));
	                            state = STATE.HASZIP;
	                            break;                                                        
	                        }
	                        catch(Exception e)
	                        {
	                            curListing = null;
	                            state = STATE.FRESH;
	                            break;                                                                                        
	                        }
	                    }
	                    state = STATE.FRESH;
	                    break;                                                            
	                }
	                case HASCITY:
	                {
	                    if(line.indexOf("state:") == 0 &&curListing != null)
	                    {
	                        try
	                        {
	                            curListing.state_= line.substring("state:".length());
	                            state = STATE.HASSTATE;
	                            break;                                                        
	                        }
	                        catch(Exception e)
	                        {
	                            curListing = null;
	                            state = STATE.FRESH;
	                            break;                                                                                        
	                        }
	                    }                    
	                    state = STATE.FRESH;
	                    break;                                        
	                }
	                case HASSTREET:
	                {
	                    if(line.indexOf("city:") == 0 &&curListing != null)
	                    {
	                        try
	                        {
	                            curListing.city_ = line.substring("city:".length());
	                            state = STATE.HASCITY;
	                            break;                                                        
	                        }
	                        catch(Exception e)
	                        {
	                            state = STATE.FRESH;
	                            break;                                                                    
	                        }
	                    }                    
	                    state = STATE.FRESH;
	                    break;                                        
	                }
	                case HASLOC:
	                {
	                    if(line.indexOf("address:") == 0 &&curListing != null)
	                    {
	                        try
	                        {
	                            curListing.street_ = line.substring("address:".length());
	                            state = STATE.HASSTREET;
	                            break;                                                        
	                        }
	                        catch(Exception e)
	                        {
	                            state = STATE.FRESH;
	                            break;                                                                    
	                        }
	                    }
	                    state = STATE.FRESH;
	                    break;                                        
	                }
	                case HASNAME:
	                {
	                    if(line.indexOf("loc:") == 0 &&curListing != null)
	                    {
	                        try
	                        {
	                            setGeoInfo(curListing, line.substring("loc:".length()));
	                            state = STATE.HASLOC;
	                            break;                                                        
	                        }
	                        catch(Exception e)
	                        {
	                            state = STATE.FRESH;
	                            break;                                                                    
	                        }
	                    }
	                    state = STATE.FRESH;
	                    break;                                        
	                }
	                case FRESH:
	                {
	                    if(line.indexOf("name:") == 0)
	                    {
	                        try
	                        {
	                            if(curListing == null) {curListing = new Listing();}
	                            curListing.name_ = line.substring("name:".length());
	                            state = STATE.HASNAME;
	                            break;                                                        
	                        }
	                        catch(Exception e)
	                        {
	                            state = STATE.FRESH;
	                            break;                                                                    
	                        }
	                    }
	                    state = STATE.FRESH;
	                    break;                                        
	                }
	                default: 
	                    break;
	            }
        	} catch (Throwable t) {
	        	System.out.println("BAD LINE: "+cnt+"  :   "+line);
	        	t.printStackTrace();
	        }
        }         
    }
    
    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        if(args.length < 3) 
        {
            System.out.println("USAGE:");
            System.out.println("old index file: path to old index to dedup against");
            System.out.println("new data input directory: new directory to merge in");
            System.out.println("new data output file : directory to write output file");
        }
        String oldIndex = args[0];
        String newDataDir = args[1];
        String newDataIndexDir = args[2];
        if(args.length > 3)
        {
            batchSize = Integer.parseInt(args[3].trim());
        }
        Parser p = new Parser(oldIndex, newDataDir, newDataIndexDir);
        p.setMaxUserId(45000000);
        p.processInputFiles(newDataDir);
        p.close();
    }
    
    public static int batchSize = 8;

    public void processInputFiles(String dirName) throws IOException, InterruptedException, ExecutionException
    {
       File [] ratingsFiles = getFiles(dirName);
       int numBatches = ratingsFiles.length/batchSize;
       int i = 0;
       for(i = 0; i < numBatches; i++)
       {
           int startIndex = i * batchSize;
           
           processInputFileBatch(startIndex, batchSize, ratingsFiles);
           System.out.println("Finished processing batch number " + i);
       }
       int startIndex = i * batchSize;
       if(startIndex < ratingsFiles.length);
       {
           processInputFileBatch(startIndex, ratingsFiles.length-startIndex, ratingsFiles);           
           System.out.println("Finished processing leftovers");
       }   

       outputStream_.close();
    }
    
        
    protected void processInputFileBatch(int startIndex, int batchSize, File [] ratingsFiles) throws IOException, InterruptedException, ExecutionException
    {
        assert(startIndex + batchSize < ratingsFiles.length);
        int i = startIndex;
        ArrayList<FutureTask<FileProcessor>> fpList = 
            new ArrayList<FutureTask<FileProcessor>>();
        
        while(i < startIndex + batchSize)
        {
            File f = ratingsFiles[i];
            FutureTask<FileProcessor> ft = new FutureTask<FileProcessor>(new FileProcessor(f, this), null);
            fpList.add(ft);
            thePool_.execute(ft);
            i++;
        }
        for(FutureTask<FileProcessor> ft : fpList)
        {
            ft.get();
        }
        outputStream_.flush();        
    }
    
    public static File [] getFiles(String dirName)
    {
        File dir = new File(dirName);

        return dir.listFiles(
                new FileFilter() {
                    public boolean accept(File file) {
                        return file.isFile() && file.getName().endsWith(".rating");
                    }
                });        
    }
}
