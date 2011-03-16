package com.where.atlas.feed.yellowpages;

import gnu.trove.TLongHashSet;
import gnu.trove.TObjectLongHashMap;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

import com.where.atlas.feed.citysearch.CSListingDocumentFactory;
import com.where.place.Address;
import com.where.place.YelpPlace;
import com.where.util.lucene.NullSafeGeoFilter;
import com.where.util.lucene.NullSafeGeoFilter.GeoHashCache;

public class YPParserUtils
{
    
    private String oldIndexPath_;
    private String targetPath_;
    private File[] ratings;
    private GeoHashCache geoCache_;
    private IndexSearcher oldSearchIndexSearcher_;
    public final float withPhoneThreshold_ = .65f;
    public final float withoutPhoneThreshold_ = .9f;
    private JaroWinklerDistance jw_;
    private FileChannel outputChannel_;
    private File outputFile_;
    private FileOutputStream outputStream_;
    private String outputFileName_;
    private static String newline = System.getProperty("line.separator"); 
    private TObjectLongHashMap<String> seenUserIds_ = new TObjectLongHashMap<String>();
    private long maxUserId_;
    
    private Set<String> seenRecIds = new HashSet<String>();
    private Map<String, TLongHashSet> seenPairs_ = new HashMap<String, TLongHashSet>();
    
    
    public static class InnerRating
    {
        public long userId_;
        public int rating_;
    }
    

    public static class Listing
    {
        public long csId_;
        public String name_;
        public String csName_ = "";
        public boolean phoneMatch_ = false;
        public double lat_;
        public double lng_;
        public String geoHash_;
        public String street_;
        public String city_;
        public String state_;
        public int zip_;
        public String phone_;
        public ArrayList<InnerRating> ratings_ = new ArrayList<InnerRating>();
        
        
        //converts a YelpListing to a YelpPlace object
        public YelpPlace toPlace()
        {
            YelpPlace place = new YelpPlace();
            Address address = new Address();
            double latlng[] = {lat_,lng_};  
            
            
            
            place.setNativeId(new Long(csId_).toString());
            place.setYelpName(name_);
            place.setName(csName_);
            place.setPhoneMatch(phoneMatch_);
            place.setLatlng(latlng);
            place.setGeohash(geoHash_);
            place.setPhone(phone_);
            //place.setRatings(ratings_);
            
            address.setAddress1(street_);
            address.setCity(city_);
            address.setState(state_);
            address.setZip(new Integer(zip_).toString());
           
            place.setAddress(address);
            
            return place;
        }
        
    }
    
    public YPParserUtils(String oldIndexPath, String ratingsPath, String targetPath) throws IOException,CorruptIndexException
    {
        
        oldIndexPath_ = oldIndexPath;
        targetPath_ = targetPath;
        
        maxUserId_ = 45000000;
        
        ratings = getFiles(ratingsPath);
        
        
        jw_ = new JaroWinklerDistance();
        oldSearchIndexSearcher_ = new IndexSearcher(new NIOFSDirectory(new File(oldIndexPath)));
        geoCache_ = new GeoHashCache(CSListingDocumentFactory.LATITUDE_RANGE, CSListingDocumentFactory.LONGITUDE_RANGE,
                CSListingDocumentFactory.LISTING_ID, oldSearchIndexSearcher_.getIndexReader());
        outputFileName_ = targetPath;
        outputFile_ = new File(outputFileName_);
        outputFile_.createNewFile();
        outputStream_ = new FileOutputStream(outputFile_);
        outputChannel_ = outputStream_.getChannel();
    }
    
    public YPParserUtils(String CSIndex,String targetFile)
    {
        oldIndexPath_=CSIndex;
        targetPath_ = targetFile;
    }
    
    // returns a File array of all files in path 'dirName' that end with .rating
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
    
    public String slurpFileStream(FileInputStream stream) throws IOException
    {
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
    
    public boolean canUseListing(Listing l)
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
                    
    public void storeListing(Listing l) {
        long placeId = l.csId_;
        
        for(InnerRating ir : l.ratings_)
        {
            
            try
            {
                
                outputChannel_.write(ByteBuffer.wrap((ir.userId_ + "," + placeId + "," + ir.rating_ + newline).getBytes()));                
            }
            catch(Exception e){
                
                e.printStackTrace();}
        }
    }
    
    public void addLatLongQueryFromCriteria(BooleanQuery rawQuery, double lat, double lng)
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

                
                if(!seenRecIds.contains(recId))
                {
                    seenRecIds.add(recId);
                }
                else
                {
                    continue;                    
                }       
                String userIDraw = pieces [1];
                long userId = getUserId(userIDraw);
                TLongHashSet tmp = null;
                if(!seenPairs_.containsKey(recId))
                {
                    tmp = new TLongHashSet();
                    seenPairs_.put(recId, tmp);
                }        
                if(!tmp.contains(userId))
                {
                    tmp.add(userId);
                    InnerRating ir = new InnerRating();
                    ir.userId_ = userId;
                    ir.rating_ = Integer.parseInt(pieces[2]);                
                    l.ratings_.add(ir);  
                   
                }         
            }
            catch(Exception e)
            {
                throw(new RuntimeException(e));
            }            
        }
    }
    
    public long getUserId(String lookup)
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
    
    public long incrementAndGetMaxUserId()
    {
        return maxUserId_++;
    }
    
    public File[] getRatings()
    {
        return ratings;
    }
    
    public String getTargetPath()
    {
        return targetPath_;
    }
    
    public String getOldIndexPath()
    {
        return oldIndexPath_;
    }
}
