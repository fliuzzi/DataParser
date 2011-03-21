package com.where.atlas.feed.yellowpages;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.where.atlas.feed.FeedParser;
import com.where.atlas.feed.PlaceCollector;
import com.where.place.Address;
import com.where.place.YPPlace;


public class YPRawDataParser implements FeedParser { 
    
    
    protected YPParserUtils parser;
    private static BufferedWriter bufferedWriter;

    
    public YPRawDataParser(YPParserUtils Yparser)
    {
        parser=Yparser;
        try{
            bufferedWriter = new BufferedWriter(new FileWriter(parser.getTargetPath()));
            
            //Start the JSON Array
            bufferedWriter.write("[");
        }
        catch(Throwable t){
            System.err.print("Error Loading!");
        }
    }
    
    protected static BufferedWriter bufferedWriter()
    {
        return bufferedWriter;
    }
    
    public static String cleanReview(String review)
    {
        review.replace("\\n","");
        review.replace("\\t", "");
        return review;
    }
    
    public void closeWriter() throws IOException
    {
    	//close the array and close the writer
    	bufferedWriter.write("]");
        bufferedWriter.close();
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
    
    
    public void parse(PlaceCollector collector, InputStream ins) throws IOException {
        try{
                DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
                Document doc = docBuilder.parse(ins);
                
                doc.getDocumentElement().normalize();
                
                NodeList listOfListings = doc.getElementsByTagName("listing");
                //NodeList listOfDetails = doc.getElementsByTagName("business_details");
                //NodeList listOfReviews = doc.getElementsByTagName("business_reviews");
                
                Node listingNode = null;
                YPPlace poi = null;
                Address location = null;
                
                
                //// <LISTING>
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
                        
                        try{
                        	poi.setYPurl(URL.getTextContent());
                        }
                        catch(NullPointerException np)
                        {
                        	
                        }
                        	
                        if(poi.getName().length() <= 0)
                    		collector.collectBadInput(poi, new Exception("Nullname"));
                        else
                        	collector.collect(poi);
                    }
                }
                
                
//                //// <BUSINESS_DETAILS>
//                for(int i = 0; i < listOfDetails.getLength();i++)
//                {
//                    listingNode = listOfDetails.item(i);
//                    if(listingNode.getNodeType() == Node.ELEMENT_NODE){
//                        Element detailElement = (Element)listingNode;
//                        
//                        poi = new YPPlace();
//                        location = new Address();
//                        
//                        NodeList detailElement
//                        }}
                	
                
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
	                
	                poi.addReview(json);
                }
                catch(Exception e)
                {
                	System.out.println("Error constructing review json: "+e.getMessage());
                }
                
                
                
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