package com.where.atlas.feed.yelp;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.where.atlas.feed.FeedParser;
import com.where.atlas.feed.PlaceCollector;
import com.where.atlas.feed.yellowpages.YPRawDataParser;
import com.where.place.Address;
import com.where.place.YelpPlace;

public class Yelp80legsParser implements FeedParser{

	private static Boolean WANT_REVIEW_TEXT;
	private static BufferedWriter bufferedWriter;

	public Yelp80legsParser(String writePath) throws IOException
	{
		bufferedWriter = new BufferedWriter(new FileWriter(writePath));
	}

	public void setFlag(String flg){
		if(flg.equals("true"))
			WANT_REVIEW_TEXT = true;
		else if(flg.equals("false")){
			WANT_REVIEW_TEXT = false;
		}
		else{

		}
	}

	public static BufferedWriter getWriter()
	{
		return bufferedWriter;
	}

	@Override
	public void parse(PlaceCollector collector, InputStream ins)
	throws Exception {
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
		Document doc = docBuilder.parse(ins);

		doc.getDocumentElement().normalize();


		NodeList listOfListings = doc.getElementsByTagName("listing");
		parseListings(collector, listOfListings);

	}


	private void parseListings(PlaceCollector collector,NodeList listings)
	{
		Node listingNode = null;
		YelpPlace poi = null;
		Address location = null;
		//// <LISTING>
		for(int i = 0; i < listings.getLength();i++)
		{   

			listingNode = listings.item(i);
			if(listingNode.getNodeType() == Node.ELEMENT_NODE){
				Element listingElement = (Element)listingNode;


				poi = new YelpPlace();
				location = new Address();


				poi.setName(StringEscapeUtils.unescapeHtml(listingElement.getAttribute("name")));//NAME
				poi.setPhone(YPRawDataParser.stripPhone(listingElement.getAttribute("phone")));//PHONE
				poi.setBiz_url(StringEscapeUtils.unescapeHtml(listingElement.getAttribute("biz_url")));
				poi.setAvg_rating(StringEscapeUtils.unescapeHtml(listingElement.getAttribute("overall_rating")));
				poi.setWheelchair(StringEscapeUtils.unescapeHtml(listingElement.getAttribute("wheelchair")));
				poi.setParking(StringEscapeUtils.unescapeHtml(listingElement.getAttribute("parking")));
				poi.setPrice(StringEscapeUtils.unescapeHtml(listingElement.getAttribute("price")));
				poi.setType(StringEscapeUtils.unescapeHtml(listingElement.getAttribute("type")));



				location.setZip(StringEscapeUtils.unescapeHtml(listingElement.getAttribute("postal_code")));//addy.ZIP
				location.setCity(StringEscapeUtils.unescapeHtml(listingElement.getAttribute("locality")));//addy.CITY
				location.setState(StringEscapeUtils.unescapeHtml(listingElement.getAttribute("region")));
				location.setAddress1(StringEscapeUtils.unescapeHtml(listingElement.getAttribute("address")));

				if(listingElement.getAttribute("lat").length() > 1 && listingElement.getAttribute("lon").length() > 1){
					location.setLat(Double.parseDouble(listingElement.getAttribute("lat")));	//LATITUDE
					location.setLng(Double.parseDouble(listingElement.getAttribute("lon")));	//LONGITUDE
				}

				poi.setAddress(location);

				//sub-nodes
				NodeList categories = listingElement.getElementsByTagName("category");
				try {
					fillCategories(categories,poi);
				} catch (Exception e) {
					e.printStackTrace();
				}
				NodeList reviews = listingElement.getElementsByTagName("reviews");
				fillReviews(reviews,poi);

				NodeList URLlist = listingElement.getElementsByTagName("URL");
				Element URL = (Element) URLlist.item(0);

				try{
					poi.setYelp_url(URL.getTextContent());
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

	private static String removeNonUtf8CompliantCharacters( final String inString ) {
		if (null == inString ) return null;
		byte[] byteArr = inString.getBytes();
		for ( int i=0; i < byteArr.length; i++ ) {
			byte ch= byteArr[i];
			// remove any characters outside the valid UTF-8 range as well as all control characters
			// except tabs and new lines
			if ( !( (ch > 31 && ch < 253 ) || ch == '\t' || ch == '\n' || ch == '\r') ) {
				byteArr[i]=' ';
			}
		}
		return new String( byteArr );
	}

	private void fillReviews(NodeList reviewList,YelpPlace poi)
	{
		for(int i = 0;i < reviewList.getLength();i++)
		{
			Node reviewNode = reviewList.item(i);
			if(reviewNode.getNodeType() == Node.ELEMENT_NODE){
				Element review = (Element)reviewNode;
				JSONObject json = new JSONObject();

				try{
					json.put("user", StringEscapeUtils.unescapeHtml(review.getAttribute("user_name")));
					json.put("user_id",StringEscapeUtils.unescapeHtml(review.getAttribute("user_id")));
					json.put("rating", StringEscapeUtils.unescapeHtml(review.getAttribute("rating")));
					json.put("date", StringEscapeUtils.unescapeHtml(review.getAttribute("date")));
					if(WANT_REVIEW_TEXT){
						json.put("text", removeNonUtf8CompliantCharacters(review.getAttribute("text")));
					}
					if(review.getAttribute("useful").length() > 0)
						json.put("useful", review.getAttribute("useful"));
					if(review.getAttribute("funny").length() > 0)
						json.put("funny", review.getAttribute("funny"));
					if(review.getAttribute("cool").length() > 0)
						json.put("cool",review.getAttribute("cool"));

					poi.addReview(json);
				}
				catch(Exception e)
				{
					System.out.println("Error constructing review json: "+e.getMessage());
				}



			}
		}
	}

	private void fillCategories(NodeList categoryList,YelpPlace poi)
	{
		for(int i = 0;i < categoryList.getLength();i++)
		{
			Node categoryNode = categoryList.item(i);
			if(categoryNode.getNodeType() == Node.ELEMENT_NODE){
				Element category = (Element)categoryNode;

				if(category.getAttribute("name") != null)
					try {
						poi.addCategory(jsonStringDecode(StringEscapeUtils.unescapeHtml(category.getAttribute("name"))));
					} catch (JSONException e) {
						e.printStackTrace();
					}
			}
		}
	}

	private String jsonStringDecode(String string) throws JSONException { 
		return ((String) new JSONTokener('"' + string + '"').nextValue()); 
	}

}
