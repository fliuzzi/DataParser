package com.where.atlas.feed;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Scanner;

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
    
    YelpParserUtils parser;
    public final float withPhoneThreshold_ = .65f;
    public final float withoutPhoneThreshold_ = .9f;
    BufferedWriter bufferedWriter;
    IndexSearcher searcher;
    
    public YelpRawDataParser(YelpParserUtils Yparser)
    {
        parser=Yparser;
        try{
        bufferedWriter = new BufferedWriter(new FileWriter(parser.getTargetPath()));
        searcher = new IndexSearcher(new NIOFSDirectory(new File(parser.getOldIndexPath())));
        
        }
        catch(Throwable t){
            System.err.print("Error writing output");
        }
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
                        
                        poi = new Place();
                        location = new Address();
                        
                        poi.setName(listingElement.getAttribute("name"));//NAME
                        location.setZip(listingElement.getAttribute("postal_code"));//addy.ZIP
                        poi.setPhone(stripPhone(listingElement.getAttribute("phone")));//PHONE
                        location.setCity(listingElement.getAttribute("locality"));//addy.CITY
                        location.setState(listingElement.getAttribute("region"));
                        
                        //System.out.println(listingElement.getAttribute("region"));
                        
                        
                        if(listingElement.getAttribute("lat").length() > 1 && listingElement.getAttribute("lon").length() > 1){
                            location.setLat(Double.parseDouble(listingElement.getAttribute("lat")));//LATITUDE
                            location.setLng(Double.parseDouble(listingElement.getAttribute("lon")));//LONGITUDE
                        }
                        
                        poi.setAddress(location);
                        
                        
                        //TODO:IF THIS IS A BOSTON LOCATION.....(de-dupe)
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
                                        
                                        //TODO: QUERY INDEX FOR POI
                                        
                                        
                                        
                                        GeoHashCache geoCache = new GeoHashCache(CSListingDocumentFactory.LATITUDE_RANGE, CSListingDocumentFactory.LONGITUDE_RANGE,
                                                CSListingDocumentFactory.LISTING_ID, searcher.getIndexReader());
                                        
                                        JaroWinklerDistance jw = new JaroWinklerDistance();
                                        
                                        
                                        
                                        //START QUERY
                                        
                                        BooleanQuery bq = new BooleanQuery();        
                                        Analyzer analyzer = CSListingDocumentFactory.getAnalyzerWrapper();
                                        addLatLongQueryFromCriteria(bq, poi.getAddress().getLat(),poi.getAddress().getLng());
                                        BooleanQuery mainQuery = new BooleanQuery();
                                        HashMap<String, Float> boosts = new HashMap<String, Float>();
                                        boosts.put(CSListingDocumentFactory.NAME,1.0f);
                                        String[] fields = { CSListingDocumentFactory.NAME };
                                        mainQuery.add(new TermQuery(new Term(
                                               CSListingDocumentFactory.PHONE,
                                               poi.getPhone())),
                                               Occur.SHOULD);

                                        MultiFieldQueryParser qp = new MultiFieldQueryParser(
                                                Version.LUCENE_30, fields, analyzer, boosts);
                                        qp.setDefaultOperator(Operator.OR);
                                        
                                        try
                                        {
                                            mainQuery.add(qp.parse(poi.getName()), Occur.SHOULD);
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
                                                if(poi.getPhone() != null)
                                                   samePhone = phone.equals(poi.getPhone()); //have the same phone?
                                                
                                                float dist = jw.getDistance(left, right);
                                                
                                                String csId = d.get(CSListingDocumentFactory.LISTING_ID).trim();
                                                
                                                Scanner in = new Scanner(new File("/home/fliuzzi/data/bostoncityCSIDS.txt"));
                                                String line = null;
                                                boolean isInBostonMarket = false;
                                                while(in.hasNextLine())
                                                {
                                                    line = in.nextLine().trim();
                                                    if(line.equals(csId))
                                                    {
                                                        isInBostonMarket = true;
                                                        break;
                                                    }
                                                }
                                                in.close();
                                                
                                                boolean hit = (samePhone && dist > withPhoneThreshold_ && isInBostonMarket)
                                                                    || (dist > withoutPhoneThreshold_ && isInBostonMarket);
                                                
                                                if(hit)
                                                {
                                                    System.out.println("DING!");
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
        catch(Exception e){e.printStackTrace();}
    }
}

