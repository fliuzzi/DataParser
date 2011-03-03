package com.where.data.parsers.citysearch;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class CitySearchAuthorsParser {
	private static long index;
	private static long ratingCount;
	
	private static PrintWriter writer;
	private static Map<String, Long> authors = new HashMap<String, Long>();
	
	protected CitySearchAuthorsParser() {}
	
	private static final Log logger = LogFactory.getLog(CitySearchAuthorsParser.class);
	
	public static void parseListings(String path) {
		try {
			writer = new PrintWriter(new FileWriter("as.csv"));
		}
		catch(Exception ignored) {}
		
		parseListingsZip(path);
	}
	
	private static void parseListingsZip(String zipPath) {
		int count = 0;
		ZipEntry zipEntry = null;
		try {
			ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath));
			
			while((zipEntry = zis.getNextEntry()) != null) {
				boolean startLocationsFound = false;
				
				BufferedReader reader = new BufferedReader(new InputStreamReader(zis));
				StringBuffer buffer = new StringBuffer();
				String line = null;
				int locationCounter = 0;
				while((line = reader.readLine()) != null) {
					if(!startLocationsFound) {
						int locationsIndex = line.indexOf("<locations ");
						if(locationsIndex > -1) {
							line = line.substring(line.indexOf(">", locationsIndex) + 1);
							startLocationsFound = true;
						}
					}
					
					int locationsIndex = line.indexOf("</locations>");
					if(locationsIndex > -1) {
						line = line.substring(0, locationsIndex);
					}
					
					int locationEnd = line.indexOf("</location>");
					if(locationEnd > -1) {
						locationCounter++;
						if(locationCounter > 2000) {
							buffer.append(line.substring(0, locationEnd+11));
							buffer.append("</locations>");
							count+=parse("<locations>" + buffer.toString());
							
							buffer.delete(0, buffer.length());

							if(line.length() > locationEnd+11) {
								buffer.append(line.substring(locationEnd+11));
							}
							locationCounter = 0;
						}
						else buffer.append(line);
					}
					else buffer.append(line);
				}
				
				if(buffer.length() > -1 && buffer.toString().indexOf("<location ") > -1) {
					buffer.append("</locations>");
					count+=parse("<locations>" + buffer.toString());
				}
			
				zis.closeEntry();
			}
			zis.close();
			
			writer.close();
			
			System.out.println("Done. Processed " + count + " CS Listings. Extracted " + authors.size() + " authors and " + ratingCount + " posts.");
		} 
		catch(Exception ex) {
			logger.error("Error parsing out cs enhanced listing data " + (zipEntry != null ? zipEntry.getName() : ""), ex);
			
			throw new IllegalStateException(ex);
		}
	}
	
	private static int parse(String text) throws Exception {
		DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = db.parse(new InputSource(new StringReader(text)));
		doc.getDocumentElement().normalize();
		
		return parse(doc);
	}	
	
	private static int parse(Document doc) {
		NodeList list = doc.getDocumentElement().getElementsByTagName("location");
		if(list == null || list.getLength() == 0) return 0;
		
		for(int i = 0, n = list.getLength(); i < n; i++) {
			Element location = (Element)list.item(i);
			extractRating(location);
		}
		
		return list.getLength();
	}
	
	public static void extractRating(Element location) {	
		String placeid = ParseUtils.getChildValueByName(location, "id");
		
		Element reviews = ParseUtils.getChildByName(location, "reviews");
		if(reviews != null) {				
			NodeList reviewlist = reviews.getElementsByTagName("review");
			if(reviewlist != null && reviewlist.getLength() > 0) {
				for(int i = 0, n = reviewlist.getLength(); i < n; i++) {
					Review review = new Review();
					Element r = (Element)reviewlist.item(i);
					review.setAuthor(ParseUtils.getChildValueByName(r, "review_author"));
					review.setRating(ParseUtils.getChildValueByName(r, "review_rating"));

					//Long authorid = getAuthorId(review.getAuthor());
					//if(authorid.longValue() != 1 && review.getRating() > 0) {
						ratingCount++;
						writer.println(review.getAuthor() + "," + placeid + "," + review.getRating());
					///}
				}
			}
		}	
	}
	
	@SuppressWarnings("unused")
	private static Long getAuthorId(String author) {
		Long id = authors.get(author);
		
		if(id != null) return id;
		
		index++;
		id = new Long(index);
		authors.put(author, id);
		
		return id;
	}
	
	public static void main(String[] args) throws Exception {
		parseListings(args[0]);
//		java.util.Set<String> bad = new java.util.HashSet<String>();
//		java.util.Set<String> all = new java.util.HashSet<String>();
//		
//		BufferedReader reader = new BufferedReader(new FileReader("/Users/imitrovic/downloads/ratings1.csv"));
//		PrintWriter w = new PrintWriter(new FileWriter("/Users/imitrovic/downloads/ratings2.csv"));
//		String line = null;
//		long c=0;
//		while((line = reader.readLine()) != null) {
//			//if(all.contains(line)) bad.add(line);
//			all.add(line);
//		}
//		reader.close();
//		
//		for(String l:all) {
//			w.println(l);
//		}
//		w.close();
//		
//		System.out.println(bad.size() + ", " + all.size());
	}	
}