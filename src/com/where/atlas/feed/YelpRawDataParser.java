package com.where.atlas.feed;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
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
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.Version;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.where.place.Address;
import com.where.place.Place;
import com.where.util.lucene.NullSafeGeoFilter;
import com.where.util.lucene.NullSafeGeoFilter.GeoHashCache;


public class YelpRawDataParser implements FeedParser { 
    
    protected YelpParserUtils parser;
    protected final float withPhoneThreshold_ = .65f;
    protected final float withoutPhoneThreshold_ = .9f;
    protected BufferedWriter bufferedWriter;
    protected IndexSearcher searcher;
    protected Set<String> cityMap;
    protected GeoHashCache geoCache;
    protected JaroWinklerDistance jw;
    protected BooleanQuery bq,mainQuery,rawQuery;
    protected Analyzer analyzer;
    protected HashMap<String,Float> boosts;
    protected MultiFieldQueryParser qp;
    protected int counter;
    protected static String currentState;
    
    
    public YelpRawDataParser(YelpParserUtils Yparser)
    {
        parser=Yparser;
        BooleanQuery.setMaxClauseCount(10000);
        try{
        bufferedWriter = new BufferedWriter(new FileWriter(parser.getTargetPath()));
        searcher = new IndexSearcher(new NIOFSDirectory(new File(parser.getOldIndexPath())));
        
        cityMap = createCityMap(new Scanner(new File("/home/fliuzzi/data/bostoncityCSIDS.txt")));
        System.out.println("Loaded city-map into memory.\nSearching State:");
        counter=0;
        
        geoCache = new GeoHashCache(CSListingDocumentFactory.LATITUDE_RANGE, CSListingDocumentFactory.LONGITUDE_RANGE,
                CSListingDocumentFactory.LISTING_ID, searcher.getIndexReader());
        jw = new JaroWinklerDistance();
        bq = new BooleanQuery();
        mainQuery = new BooleanQuery();
        analyzer = CSListingDocumentFactory.getAnalyzerWrapper();
        boosts = new HashMap<String, Float>();
        boosts.put(CSListingDocumentFactory.NAME,1.0f);
        String[] fields = { CSListingDocumentFactory.NAME };
        qp = new MultiFieldQueryParser(
                Version.LUCENE_30, fields, analyzer, boosts);
        qp.setDefaultOperator(Operator.OR);
        
        }
        catch(Throwable t){
            System.err.print("Error Loading!");
        }
    }
    
    public Set<String> createCityMap(Scanner in)
    {
        String line = null;
        Set<String> citySet = new HashSet<String>();
        
        
        while(in.hasNextLine())
        {
            citySet.add(in.nextLine().trim());
        }
        in.close();
        return citySet;
    }
    
    public void closeWriter()
    {
        try{
            bufferedWriter.close();
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
            return line;
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
        rawQuery = new BooleanQuery();
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
        
        return rawQuery;
    }
    
    public void parse(PlaceCollector collector, InputStream ins) throws IOException {
        try{             
                int placeCounter=0;
                long reviewCounter=0;
            
            
                DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
                Document doc = docBuilder.parse(ins);
                
                doc.getDocumentElement().normalize();
                
                NodeList listOfListings = doc.getElementsByTagName("listing");
                Node listingNode = null;
                Node reviewNode = null;
                Place poi = null;
                Address location = null;
                
                for(int i = 0; i < listOfListings.getLength();i++)
                {
                    placeCounter++;
                    
                    listingNode = listOfListings.item(i);
                    if(listingNode.getNodeType() == Node.ELEMENT_NODE){
                        Element listingElement = (Element)listingNode;
                        
                        currentState = listingElement.getAttribute("region");
                        
                        
                        
                        poi = new Place();
                        location = new Address();
                        
                        poi.setName(listingElement.getAttribute("name"));//NAME
                        location.setZip(listingElement.getAttribute("postal_code"));//addy.ZIP
                        poi.setPhone(stripPhone(listingElement.getAttribute("phone")));//PHONE
                        location.setCity(listingElement.getAttribute("locality"));//addy.CITY
                        location.setState(currentState);
                        
                        //System.out.println(listingElement.getAttribute("region"));
                        
                        
                        if(listingElement.getAttribute("lat").length() > 1 && listingElement.getAttribute("lon").length() > 1){
                            location.setLat(Double.parseDouble(listingElement.getAttribute("lat")));//LATITUDE
                            location.setLng(Double.parseDouble(listingElement.getAttribute("lon")));//LONGITUDE
                        }
                        
                        poi.setAddress(location);
                        
                        if(poi.getAddress().getState().equalsIgnoreCase("MA"))
                        {
                            NodeList reviewNodes = listingElement.getElementsByTagName("reviews");
                                for(int x = 0;x < reviewNodes.getLength();x++)
                                {
                                    
                                    reviewCounter++;
                                    reviewNode = reviewNodes.item(x);
                                    if(reviewNode.getNodeType() == Node.ELEMENT_NODE){
                                        Element reviewElement = (Element) reviewNode;
                                        String date = reviewElement.getAttribute("date");
                                        String reviewHash = reviewElement.getAttribute("id");//TODO: de-dupe reviews
                                        String userIDHash = reviewElement.getAttribute("user_id");
                                        String rating = reviewElement.getAttribute("rating");
                                        String reviewText = reviewElement.getAttribute("text");
                                        
                                        
                                        bq = getLatLongQueryFromCriteria(poi.getAddress().getLat(),poi.getAddress().getLng());
                                        
                                        mainQuery.add(new TermQuery(new Term(
                                               CSListingDocumentFactory.PHONE,
                                               poi.getPhone())),
                                               Occur.SHOULD);
                                        
                                        try
                                        {
                                            mainQuery.add(qp.parse(cleanForQuery(poi.getName())), Occur.SHOULD);
                                            bq.add(mainQuery, Occur.MUST);
                                            Filter wrapper = new QueryWrapperFilter(bq);
                                            
                                            Filter distanceFilter = 
                                                new NullSafeGeoFilter(wrapper, poi.getAddress().getLat(),poi.getAddress().getLng(), 1, geoCache, CSListingDocumentFactory.GEOHASH);                     
                                            TopDocs td = searcher.search(bq, distanceFilter, 10);
                                            ScoreDoc [] sds = td.scoreDocs;
                                            for(ScoreDoc sd : sds)
                                            {
                                                org.apache.lucene.document.Document d = searcher.getIndexReader().document(sd.doc);
                                                String name = d.get(CSListingDocumentFactory.NAME).toLowerCase().trim();
                                                String city = d.get(CSListingDocumentFactory.CITY).toLowerCase().trim();
                                                String left = name + " " + city;
                                                
                                                String csName = left;
                                                
                                                String right = poi.getName() + " " + poi.getAddress().getCity();
                                                right = right.toLowerCase().trim();
                                                String phone = d.get(CSListingDocumentFactory.PHONE);
                                                
                                                
                                                boolean samePhone=false;
                                                if(poi.getPhone() != null && phone != null)
                                                   samePhone = phone.equals(poi.getPhone()); //have the same phone?
                                                
                                                float dist = jw.getDistance(left, right);
                                                
                                                String csId = d.get(CSListingDocumentFactory.LISTING_ID).trim();
                                                

                                                boolean isInBostonMarket = cityMap.contains(csId);
                                                
                                                boolean hit = (samePhone && dist > withPhoneThreshold_ && isInBostonMarket)
                                                                    || (dist > withoutPhoneThreshold_ && isInBostonMarket);
                                                
                                                if(hit)
                                                {
                                                    counter++;
                                                    System.out.println("-"+counter+"-");
                                                    String del = "\t";
                                                    bufferedWriter.write(csId+del+rating+del+date+del+userIDHash+del+reviewText);
                                                    bufferedWriter.newLine();
                                                }
                                            }
                                        }
                                        catch (Exception e)
                                        {
                                            
                                            e.printStackTrace();
                                        }
                                        
                                    }
                                }
                        }
                    }
                }
        }
        catch(Exception e){
            System.err.println("Parsing error.");
            e.printStackTrace();
            }
    }
}

