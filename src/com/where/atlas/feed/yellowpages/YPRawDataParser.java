package com.where.atlas.feed.yellowpages;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.QueryParser;
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
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.Version;
import org.json.JSONObject;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.where.atlas.feed.FeedParser;
import com.where.atlas.feed.PlaceCollector;
import com.where.atlas.feed.citysearch.CSListingDocumentFactory;
import com.where.atlas.feed.yelp.YelpRawDataParseAndDeDupe.WriterThread;
import com.where.place.Address;
import com.where.place.Place;
import com.where.place.YPPlace;
import com.where.util.lucene.NullSafeGeoFilter;
import com.where.util.lucene.NullSafeGeoFilter.GeoHashCache;


public class YPRawDataParser implements FeedParser { 
    
    
    protected YPParserUtils parser;
    protected final float withPhoneThreshold_ = .65f;
    protected final float withoutPhoneThreshold_ = .9f;
    private static BufferedWriter bufferedWriter;
    protected IndexSearcher searcher;
    protected Set<String> cityMap,zipMap;
    protected Map<Place,String> searchedPlaces; 
    protected GeoHashCache geoCache;
    protected JaroWinklerDistance jw;
    protected Analyzer analyzer;
    protected HashMap<String,Float> boosts;
    protected MultiFieldQueryParser qp;
    protected static AtomicInteger counter;
    protected WriterThread writerThread;
    private static final String del = "\t";
    
    public YPRawDataParser(YPParserUtils Yparser)
    {
        parser=Yparser;
        BooleanQuery.setMaxClauseCount(10000);
        try{
            
            
            searchedPlaces = new ConcurrentHashMap<Place,String>();
            
            bufferedWriter = new BufferedWriter(new FileWriter(parser.getTargetPath()));
            
            
          //this excepts 
            writerThread = new WriterThread(bufferedWriter);
            
            searcher = new IndexSearcher(new NIOFSDirectory(new File(parser.getOldIndexPath())));
            
            zipMap = populateMapFromTxt(new Scanner(new File("/home/fliuzzi/data/bostonMarketZipCodes.txt")));
            System.out.println("Loaded zipcode map: " + zipMap.size() + " entries.");
            cityMap = populateMapFromTxt(new Scanner(new File("/home/fliuzzi/data/citySUBSET.txt")));
            System.out.println("Loaded city-map: " + cityMap.size() + " entries.");
            counter=new AtomicInteger(0);
            
            geoCache = new GeoHashCache(CSListingDocumentFactory.LATITUDE_RANGE, CSListingDocumentFactory.LONGITUDE_RANGE,
                    CSListingDocumentFactory.LISTING_ID, searcher.getIndexReader());
            jw = new JaroWinklerDistance();
            analyzer = CSListingDocumentFactory.getAnalyzerWrapper();
            boosts = new HashMap<String, Float>();
            boosts.put(CSListingDocumentFactory.NAME,1.0f);
        }
        catch(Throwable t){
            System.err.print("Error Loading!");
        }
    }
    
    protected static BufferedWriter bufferedWriter()
    {
        return bufferedWriter;
    }
    
    protected void resetCounter()
    {
        counter.set(0);
    }
    
    private void writeEntry(StringBuilder strBuilder)
    {
        if(counter.incrementAndGet() % 500 == 0)
            System.out.print("+");
        if(counter.get() % 20000 == 0)
            System.out.println();
        
        try{
            writerThread.addTask(strBuilder.toString());
        }
        catch(Throwable t){
            System.err.println("Error writing entry: "+strBuilder);
        }
    }
    
    public static Set<String> populateMapFromTxt(Scanner in)
    {
        Set<String> txtSet = new HashSet<String>();
        
        
        while(in.hasNextLine())
        {
            txtSet.add(in.nextLine().trim());
        }
        in.close();
        return txtSet;
    }
    
    private String cleanReview(String review)
    {
        review.replace("\\n","");
        review.replace("\\t", "");
        return review;
    }
    
    public void closeWriter()
    {
        try{
            writerThread.finish();
            bufferedWriter.close();
            System.out.println("Finished Writing");
        }
        catch(Exception e)
        {
            System.err.println("err closing output file");
        }
    }
    
    //Makes sure the first character of line is not a * or ?
    //                                     (query safe)
    protected String cleanForQuery(String line)
    {
        if(line == null || line.trim().length() == 0){return line;}
        
        char firstLetter = line.charAt(0);
        
        if(firstLetter =='*' || firstLetter == '?')
            return cleanForQuery(line.substring(1));
        else
        {
            //get rid of unsafe characters
            line.replace("&#39;", "'");
            line.replace("&amp;", "&");
            
            return QueryParser.escape(line);
        }
    }
    
        
    public static String stripPhone(String phone) {
        if(phone == null || phone.trim().length() == 0){return phone;}
        StringBuffer buffer = new StringBuffer();
        char[] chrs = phone.toCharArray();
        for(int i = 0, n = chrs.length; i < n; i++) {
            if(Character.isDigit(chrs[i])) {
                buffer.append(chrs[i]);
            }
        }
        String tel = buffer.toString();
        if(tel.length() > 10) tel = tel.substring(0, 9);
        
        return tel;
    }
    
    public BooleanQuery getLatLongQueryFromCriteria(double lat, double lng)
    {
        BooleanQuery rawQuery = new BooleanQuery();
        Query latQ = NumericRangeQuery.newDoubleRange(
                CSListingDocumentFactory.LATITUDE_RANGE,
                lat-.05, lat+.05,
                true, true);

        Query longQ = NumericRangeQuery.newDoubleRange(
                CSListingDocumentFactory.LONGITUDE_RANGE,
                lng-.05, lng+.05,
                true, true);

        rawQuery.add(latQ, Occur.MUST);
        rawQuery.add(longQ, Occur.MUST);
        
        return rawQuery;
    }
    
    public void parse(PlaceCollector collector, InputStream ins) throws IOException {
        try{
                DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
                Document doc = docBuilder.parse(ins);
                
                doc.getDocumentElement().normalize();
                
                NodeList listOfListings = doc.getElementsByTagName("listing");
                Node listingNode = null;
                YPPlace poi = null;
                Address location = null;
                
                for(int i = 0; i < listOfListings.getLength();i++)
                {   
                    listingNode = listOfListings.item(i);
                    if(listingNode.getNodeType() == Node.ELEMENT_NODE){
                        Element listingElement = (Element)listingNode;
                        
                        
                        poi = new YPPlace();
                        location = new Address();
                        
                        poi.setName(listingElement.getAttribute("name"));//NAME
                        location.setZip(listingElement.getAttribute("postal_code"));//addy.ZIP
                        poi.setPhone(stripPhone(listingElement.getAttribute("phone")));//PHONE
                        poi.setHours(listingElement.getAttribute("hours"));
                        poi.setBizURL(listingElement.getAttribute("biz_url"));
                        location.setCity(listingElement.getAttribute("locality"));//addy.CITY
                        location.setState(listingElement.getAttribute("region"));
                        location.setAddress1(listingElement.getAttribute("address"));
                        
                        
                        if(listingElement.getAttribute("lat").length() > 1 && listingElement.getAttribute("lon").length() > 1){
                            location.setLat(Double.parseDouble(listingElement.getAttribute("lat")));	//LATITUDE
                            location.setLng(Double.parseDouble(listingElement.getAttribute("lon")));	//LONGITUDE
                        }
                        
                        poi.setAddress(location);
                        
                        //sub-nodes
                        NodeList categories = listingElement.getElementsByTagName("category");
                        fillCategories(categories,poi);
                        NodeList reviews = listingElement.getElementsByTagName("review");
                        fillReviews(reviews,poi);
                        
                        NodeList URLlist = listingElement.getElementsByTagName("URL");
                        Element URL = (Element) URLlist.item(0);
                        
                        
                        poi.setYPurl(URL.getTextContent());
                        	
                        if(poi.getName().length() <= 0)
                    		collector.collectBadInput(poi, new Exception("Nullname"));
                        else
                        	collector.collect(poi);
                    }
                }
        }
        catch(Exception e){
            System.err.println(e.getMessage());
            }
    }
    
    private void fillReviews(NodeList reviewList,YPPlace poi)
    {
    	for(int i = 0;i < reviewList.getLength();i++)
        {
    		Node reviewNode = reviewList.item(i);
        	if(reviewNode.getNodeType() == Node.ELEMENT_NODE){
                Element review = (Element)reviewNode;
                JSONObject json = new JSONObject();
                
                try{
	                json.put("user", review.getAttribute("user_name"));
	                json.put("rating", review.getAttribute("rating"));
	                json.put("date", review.getAttribute("date"));
	                json.put("text", review.getAttribute("text"));
                }
                catch(Exception e)
                {
                	System.out.println("Error constructing review json: "+e.getMessage());
                }
                
                poi.addReview(json);
                
        	}
        }
    }
    
    private void fillCategories(NodeList categoryList,YPPlace poi)
    {
    	for(int i = 0;i < categoryList.getLength();i++)
        {
    		Node categoryNode = categoryList.item(i);
        	if(categoryNode.getNodeType() == Node.ELEMENT_NODE){
                Element category = (Element)categoryNode;
                
                if(category.getAttribute("name") != null)
                	poi.addCategory(category.getAttribute("name"));
        	}
        }
    }
    
    
    
    
}