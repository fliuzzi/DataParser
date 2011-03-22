package com.where.atlas.feed.yellowpages;

import java.io.BufferedWriter;
import java.io.File;
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
    int parseingType;

    
    public YPRawDataParser(YPParserUtils Yparser)
    {
        parser=Yparser;
        try{
        	
        	File file = new File(parser.getTargetPath());
        	file.createNewFile();
            bufferedWriter = new BufferedWriter(new FileWriter(parser.getTargetPath()));
            
            //Start the JSON Array
            bufferedWriter.write("[");
        }
        catch(Exception e){
            System.err.print("Error Loading!"+e.getMessage());
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
    
    private void parseListings(PlaceCollector collector,NodeList listings)
    {
    	Node listingNode = null;
        YPPlace poi = null;
        Address location = null;
        //// <LISTING>
        for(int i = 0; i < listings.getLength();i++)
        {   
            listingNode = listings.item(i);
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
    	
    }
    
    
    
    private void parseDetails(PlaceCollector collector,NodeList details,NodeList reviews)
    {
    	Node listingNode = null;
        YPPlace poi = null;
        Address location = null;
        //// <BUSINESS_DETAILS>
        for(int i = 0; i < details.getLength();i++)
        {
        	listingNode = details.item(i);
            if(listingNode.getNodeType() == Node.ELEMENT_NODE){
                Element listingElement = (Element)listingNode.getFirstChild();
                
                NodeList URL = listingElement.getElementsByTagName("URL");
                Element ypurl = (Element) URL.item(0);
                NodeList url = ypurl.getChildNodes();
                String strurl = ((Node) url.item(0)).getNodeValue();
                
                poi.setYPurl(strurl);
                
                poi = new YPPlace();
                location = new Address();
                
                
                
        	
            }
        }
        
        //// <BUSINESS_REVIEWS>
        for(int i = 0; i < details.getLength();i++)
        {   
        	listingNode = details.item(i);
            if(listingNode.getNodeType() == Node.ELEMENT_NODE){
                Element listingElement = (Element)listingNode;
                
                
                poi = new YPPlace();
                location = new Address();
                
                poi.setName(listingElement.getAttribute("name"));//NAME
        	
            }
        }
    	
    }
    
    
    
    public void parse(PlaceCollector collector, InputStream ins) throws IOException {
        try{
                DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
                Document doc = docBuilder.parse(ins);
                
                doc.getDocumentElement().normalize();
                
                
                if(parser.getParserType() == 1){
                	NodeList listOfListings = doc.getElementsByTagName("listing");
                	parseListings(collector, listOfListings);
                }
                else if(parser.getParserType() == 2)
                {
                	NodeList listOfDetails = doc.getElementsByTagName("business_details");
                    NodeList listOfReviews = doc.getElementsByTagName("business_reviews");
                	parseDetails(collector,listOfDetails,listOfReviews);
                }
                else
                {
                	System.err.println("INVALID PARSEING TYPE\n restart and choose: 1) <listing>   2) <details>");
                	return;
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