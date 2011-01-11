package com.where.data.parsers.citysearch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Field;
import org.apache.lucene.spatial.geohash.GeoHashUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.where.commons.feed.citysearch.CSListing;
import com.where.commons.feed.citysearch.ParseUtils;
import com.where.commons.feed.citysearch.Placelist;
import com.where.commons.feed.citysearch.search.CSListingIndexer;
import com.where.commons.feed.citysearch.Category;
import com.where.commons.util.LocationUtil;

public class CitySearchListsParser {
	public static final String NAME = "name";
	public static final String DESCRIPTION = "desc";
	public static final String POI_COUNT = "count";
	public static final String LISTING_IDS = "listingids";
	public static final String URL = "url";
	public static final String LATITUDE = "lat";
	public static final String LONGITUDE = "lng";
	public static final String META = "meta";
	public static final String GEOHASH = "geohash";
	
	private static PrintWriter writer;
	
	private static Map<Placelist, PlacelistMeta> pmap = new HashMap<Placelist, PlacelistMeta>();
	
	protected CitySearchListsParser() {}
	
	private static final Log logger = LogFactory.getLog(CitySearchListsParser.class);
	
	public static void parseListings(String zipPath, String indexPath) {
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
							
							List<CSListing> pois = parse("<locations>" + buffer.toString());
							toListMeta(pois);
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
					List<CSListing> pois = parse("<locations>" + buffer.toString());
					toListMeta(pois);
				}
			
				zis.closeEntry();
			}
			zis.close();
			
			String parent = new File(indexPath).getParent();
			writer = new PrintWriter(new File(parent, "listurls.txt"));
			int count = 0;
			CSListingIndexer indexer = CSListingIndexer.newInstance(indexPath);
			for(Map.Entry<Placelist, PlacelistMeta> entry:pmap.entrySet()) {
				count++;
				indexer.index(toDocument(entry.getKey(), entry.getValue()));
			}
			indexer.close();
			writer.close();
			
			logger.info("Done. Extarcted and Indexed " + count + " lists");
		} 
		catch(Exception ex) {
			logger.error("Error parsing out cs enhanced listing data " + (zipEntry != null ? zipEntry.getName() : ""), ex);
			
			throw new IllegalStateException(ex);
		}
	}
	
	private static org.apache.lucene.document.Document toDocument(Placelist list, PlacelistMeta meta) {
		org.apache.lucene.document.Document doc = new org.apache.lucene.document.Document();
		
		BigDecimal lat = meta.lat.divide(new BigDecimal(String.valueOf(meta.listingids.size())), 7, BigDecimal.ROUND_HALF_UP);
		BigDecimal lng = meta.lng.divide(new BigDecimal(String.valueOf(meta.listingids.size())), 7, BigDecimal.ROUND_HALF_UP);
		
		doc.add(new Field(NAME, list.getName(), Field.Store.YES, Field.Index.ANALYZED));
		if(list.getDescription() != null) {
			doc.add(new Field(DESCRIPTION, list.getDescription(), Field.Store.YES, Field.Index.NOT_ANALYZED));
		}
		doc.add(new Field(URL, list.getListUrl(), Field.Store.YES, Field.Index.NOT_ANALYZED));
		writer.println(list.getListUrl());
		
		doc.add(new Field(POI_COUNT, String.valueOf(meta.listingids.size()), Field.Store.YES, Field.Index.NOT_ANALYZED));
		
		StringBuffer lbuffer = new StringBuffer();
		for(String listingid:meta.listingids) {
			lbuffer.append(listingid);
			lbuffer.append(" ");
		}
		doc.add(new Field(LISTING_IDS, lbuffer.toString().trim(), Field.Store.YES, Field.Index.ANALYZED));
		
		//doc.add(new Field(LATITUDE, lat.toString(), Field.Store.NO, Field.Index.NOT_ANALYZED));
		//doc.add(new Field(LONGITUDE, lng.toString(), Field.Store.NO, Field.Index.NOT_ANALYZED));
		
		doc.add(new Field(LATITUDE, LocationUtil.formatCoord(lat.doubleValue(), 1), Field.Store.NO, Field.Index.ANALYZED));
		doc.add(new Field(LONGITUDE, LocationUtil.formatCoord(lng.doubleValue(), 2), Field.Store.NO, Field.Index.ANALYZED));
		
		String geohash = GeoHashUtils.encode(lat.doubleValue(), lng.doubleValue());
		doc.add(new Field(GEOHASH, geohash, Field.Store.NO, Field.Index.NOT_ANALYZED));
		
		StringBuffer buffer = new StringBuffer();
		boost(list.getName(), buffer, 3);
		if(list.getDescription() != null) {
			boost(list.getDescription(), buffer, 1);
		}
		boost(meta.categories.toString(), buffer, 2);
		//boost(meta.cities.toString(), buffer, 1);
		//boost(meta.zips.toString(), buffer, 1);
		//boost(meta.neighborhoods.toString(), buffer, 1);
		
		doc.add(new Field(META, buffer.toString(), Field.Store.NO, Field.Index.ANALYZED));
		
		if(list.getDescription() != null && meta.listingids.size() > 5) {
			doc.setBoost(1.4f);
		}
		else {
			if(list.getDescription() != null) doc.setBoost(1.2f);
			if(meta.listingids.size() > 5) doc.setBoost(doc.getBoost()*1.4f);
		}
		
		return doc;
	}
	
	private static void boost(String value, StringBuffer collector, int boost) {
		if(value == null || value.trim().length() == 0) return;
		
		for(int i = 0; i < boost; i++) {
			collector.append(value);
			collector.append(" ");
		}
	}	
	
	private static void toListMeta(List<CSListing> pois) {
		for(CSListing poi:pois) {
			List<Placelist> lists = poi.lists();
			for(Placelist list:lists) {
				PlacelistMeta meta = pmap.get(list);
				if(meta == null) {
					meta = new PlacelistMeta();
				}
				meta.listingids.add(poi.getListingId());
				meta.categories.append(poi.getCategory());
				meta.categories.append(" ");
				//meta.cities.append(poi.getAddress().getCity() + poi.getAddress().getState());
				//meta.cities.append(" ");
				//if(poi.getAddress().getZip() != null) {
				//	meta.zips.append(poi.getAddress().getZip());
				//	meta.zips.append(" ");
				//}
				//if(poi.getNeighborhood() != null) {
				//	meta.neighborhoods.append(poi.getNeighborhood());
				//	meta.neighborhoods.append(" ");
				//}		
				meta.lat = meta.lat.add(new BigDecimal(poi.getAddress().getLat()));
				meta.lng = meta.lng.add(new BigDecimal(poi.getAddress().getLng()));
				
				//System.out.println(meta.lat + " " + meta.lng + " " + meta.poiCount);
				
				pmap.put(list, meta);
			}
		}
	}
	
	private static List<CSListing> parse(String text) throws Exception {
		DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = db.parse(new InputSource(new StringReader(text)));
		doc.getDocumentElement().normalize();
		
		return parse(doc);
	}
	
	private static List<CSListing> parse(Document doc) {
		List<CSListing> poilist = new ArrayList<CSListing>();
		
		NodeList list = doc.getDocumentElement().getElementsByTagName("location");
		if(list == null || list.getLength() == 0) return poilist;
		
		for(int i = 0, n = list.getLength(); i < n; i++) {
			Element location = (Element)list.item(i);
			CSListing poi = populateDetail(location);
			if(!poi.lists().isEmpty()) {
				poilist.add(poi);
			}
		}
		
		return poilist;
	}
	
	public static CSListing populateDetail(Element location) {	
		CSListing poi = new CSListing();
	
		poi.setListingId(ParseUtils.getChildValueByName(location, "id"));
				
		Element address = ParseUtils.getChildByName(location, "address");
		if(address != null) {
			poi.getAddress().setAddress1(ParseUtils.getChildValueByName(address, "street"));
			poi.getAddress().setCity(ParseUtils.getChildValueByName(address, "city"));
			poi.getAddress().setState(ParseUtils.getChildValueByName(address, "state"));
			poi.getAddress().setZip(ParseUtils.getChildValueByName(address, "postal_code"));
			
			poi.getAddress().setLat(Double.parseDouble(ParseUtils.getChildValueByName(address, "latitude")));
			poi.getAddress().setLng(Double.parseDouble(ParseUtils.getChildValueByName(address, "longitude")));
			
			poi.setSubtitle(poi.getAddress().getAddress1() + "\n" + poi.getAddress().getCity() + ", " + poi.getAddress().getState());
		}
		
		Element elists = ParseUtils.getChildByName(location, "lists");
		if(elists != null) {
			NodeList lists = elists.getElementsByTagName("list");
			if(lists != null) {
				for(int i = 0, n = lists.getLength(); i < n; i++) {
					Element list = (Element)lists.item(i);
					Placelist plist = new Placelist();
					plist.setName(ParseUtils.getChildValueByName(list, "list_title"));
					plist.setDescription(ParseUtils.getChildValueByName(list, "list_description"));
					plist.setListUrl(ParseUtils.getChildValueByName(list, "list_url"));
					poi.lists().add(plist);
				}
			}
		}		
		
		Element neighborhoods = ParseUtils.getChildByName(location, "neighborhoods");
		if(neighborhoods != null) {		
			StringBuffer buffer = new StringBuffer();
			
			NodeList nhoods = neighborhoods.getElementsByTagName("neighborhood");
			if(nhoods != null && nhoods.getLength() > 0) {
				for(int i = 0, n = nhoods.getLength(); i < n; i++) {
					try {
						Element e = (Element)nhoods.item(i);
						buffer.append(e.getFirstChild().getNodeValue());
						buffer.append(", ");
					}
					catch(Exception ignored) {}
				}
			}
			
			String n = buffer.toString().trim();
			if(n.length() > 0) n = n.substring(0, n.length()-1).trim();
			
			poi.setNeighborhood(n);
		}
		
		Element categories = ParseUtils.getChildByName(location, "categories");
		if(categories != null) {
			StringBuffer buffer = new StringBuffer();
			
			NodeList category = categories.getChildNodes();
			if(category != null && category.getLength() > 0) {
				for(int i = 0, n = category.getLength(); i < n && i < 4; i++) {
					Element cat = (Element)category.item(i);
					String name = cat.getAttribute("name");
					String id = cat.getAttribute("nameid");
					
					if(!CitySearchParser.isPaymentMethod(name)) {
						buffer.append(name);
						buffer.append(", ");
						buffer.append("category" + id);
						buffer.append(", ");
					}
					
					Category c = new Category(id, name);
				
					String parentname = cat.getAttribute("parent");
					String parentid = cat.getAttribute("parentid");
					
					if(parentname != null && !CitySearchParser.isPaymentMethod(parentname)) {
						buffer.append(parentname);
						buffer.append(", ");
						buffer.append("parentcategory" + parentid);
						buffer.append(", ");
					}
					
					if(parentname != null) {
						Category p = new Category(parentid, parentname);
						c.setParent(p);
					}
					
					Element group = ParseUtils.getChildByName(cat, "group");
					if(group != null) {
						String gname = ParseUtils.getAttributeValue(group, "name");
						String gid = ParseUtils.getAttributeValue(group, "groupid");
						Category.Group g = new Category.Group(gid, gname);
						c.setGroup(g);
						
						if(!CitySearchParser.isPaymentMethod(gname)) {
							buffer.append("group" + gid);
							buffer.append(", ");
						}
					}
					
					poi.addCategory(c);
				}
				
				String n = buffer.toString().trim();
				if(n.length() > 0) n = n.substring(0, n.length()-1).trim();
				
				poi.setCategory(n);
			}
		}
		
		return poi;
	}		
	
	private static class PlacelistMeta {
		private List<String> listingids = new ArrayList<String>();
		private BigDecimal lat = new BigDecimal(0);
		private BigDecimal lng = new BigDecimal(0);
		private StringBuffer categories = new StringBuffer();
		//private StringBuffer cities = new StringBuffer();
		//private StringBuffer zips = new StringBuffer();
		//private StringBuffer neighborhoods = new StringBuffer();
	}
	
	public static void main(String[] args) throws Exception {
		CitySearchListsParser.parseListings(args[0], args[1]);
	}	
}