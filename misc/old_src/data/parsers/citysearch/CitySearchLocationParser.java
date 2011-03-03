package com.where.data.parsers.citysearch;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;
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

import com.where.commons.feed.citysearch.search.CSListingIndexer;

public class CitySearchLocationParser {	
	protected CitySearchLocationParser() {}
	
	private static final Log logger = LogFactory.getLog(CitySearchLocationParser.class);
	
	private static final Set<String> CITYSTATE = new HashSet<String>();
	
	public static void parseListings(String path, String indexPath) {
		parseListingsZip(path, indexPath);
	}
	
	private static void parseListingsZip(String zipPath, String indexPath) {
		int count = 0;
		ZipEntry zipEntry = null;
		try {
			CSListingIndexer indexer = CSListingIndexer.newInstance(indexPath);
			
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
							count+=parse("<locations>" + buffer.toString(), indexer);
							
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
					count+=parse("<locations>" + buffer.toString(), indexer);
				}
			
				zis.closeEntry();
			}
			zis.close();
			
			indexer.close();
			
			logger.info("Done. Extarcted and Indexed " + count + " CS Listings");
			logger.info("Writing categories");
		} 
		catch(Exception ex) {
			logger.error("Error parsing out cs enhanced listing data " + (zipEntry != null ? zipEntry.getName() : ""), ex);
			
			throw new IllegalStateException(ex);
		}
	}
	
	private static int parse(String text, CSListingIndexer indexer) throws Exception {
		DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = db.parse(new InputSource(new StringReader(text)));
		doc.getDocumentElement().normalize();
		
		return parse(doc, indexer);
	}
	
	private static int parse(Document doc, CSListingIndexer indexer) {
		NodeList list = doc.getDocumentElement().getElementsByTagName("location");
		if(list == null || list.getLength() == 0) return 0;
		
		for(int i = 0, n = list.getLength(); i < n; i++) {
			Element location = (Element)list.item(i);
			CSListing poi = populateDetail(location);
			String key = poi.getAddress().getCity() + "_" + poi.getAddress().getState();
			key = key.trim().toLowerCase();
			if(!CITYSTATE.contains(key)) {
				CITYSTATE.add(key);
				index(poi, indexer);
			}
		}
		
		return list.getLength();
	}
	
	private static void index(CSListing poi, CSListingIndexer indexer) {
		indexer.index(CSListingDocumentFactory.createLocationDocument(poi));
	}
	
	public static CSListing populateDetail(Element location) {	
		CSListing poi = new CSListing();
	
		populateBasic(poi, location);
		
		return poi;
	}
	
	private static void populateBasic(CSListing poi, Element location) {
		Element address = ParseUtils.getChildByName(location, "address");
		if(address != null) {
			poi.getAddress().setCity(ParseUtils.getChildValueByName(address, "city"));
			poi.getAddress().setState(ParseUtils.getChildValueByName(address, "state"));
		}
	}	
	
	public static void main(String[] args) throws Exception {
		CitySearchLocationParser.parseListings(args[0], args[1]);
	}	
}