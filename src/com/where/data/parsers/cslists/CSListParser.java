//CSListParser.java
//***********
// Will probably not be used again, since data has already been collected.
//**********

package com.where.data.parsers.cslists;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.DefaultHtmlMapper;
import org.apache.tika.parser.html.HtmlMapper;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.sax.BodyContentHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import org.xml.sax.Attributes;

import com.where.places.lists.Author;
import com.where.places.lists.Placelist;
import com.where.utils.HTTPUtils;

public class CSListParser {
	private CSListParser() {}
	
	private static final Integer ONE = new Integer(1);
	
	private static final String DATE_TOKEN = "From the archives:";
	private static final String DATE_TOKEN1 = "Updated:";
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMMMM dd, yyyy");
	
	private static Map<String, Integer> listCount = new HashMap<String, Integer>();
	
	private static Set<String> badlists = new HashSet<String>();
	
	private static Placelist parseCSList(String url) throws Exception {
		//long t1 = System.currentTimeMillis();
		
		HtmlParser p = new HtmlParser();
		
		Metadata m = new Metadata();
				
		ParseContext c = new ParseContext();
		c.set(HtmlMapper.class, new DefaultHtmlMapper() {
	        public String mapSafeElement(String name) {
	            if("DIV".equals(name)) return "div";
	            if("SPAN".equals(name)) return "span";
	            return super.mapSafeElement(name);
	        }
	        
	        public String mapSafeAttribute(String elementName, String attributeName) { 
	        	if("DIV".equalsIgnoreCase(elementName) && "ID".equalsIgnoreCase(attributeName)) {
	        		return attributeName;
	        	}
	        	else if("P".equalsIgnoreCase(elementName) && "CLASS".equalsIgnoreCase(attributeName)) {
	        		return attributeName;
	        	}        	
	        	else if("SPAN".equalsIgnoreCase(elementName) && "CLASS".equalsIgnoreCase(attributeName)) {
	        		return attributeName;
	        	}
	        	return super.mapSafeAttribute(elementName, attributeName);
	        }
	    });
		
		CSListContentHandler handler = new CSListContentHandler();
		
		try {
			InputStream in = HTTPUtils.openInputStream(url, true).getInputStream();
			p.parse(in, handler, m, c);
			in.close();
		}
		catch(Exception ex) {
			ex.printStackTrace();
			badlists.add(url);
			return null;
		}
		
		//System.out.println(handler.getCSList());
		
		//System.out.println((System.currentTimeMillis()-t1)/1000);
		
		Placelist cslist = handler.getPlacelist();
		cslist.setSource("CS");
		cslist.setSourceUrl(url);
		
		if(cslist.getAuthor() == null) {
			badlists.add(url);
			cslist = null;
		}
		
		return cslist;
	}
	
	private static List<List<String>> loadListUrls(String fileName) throws Exception {
		List<List<String>> urlcollection = new ArrayList<List<String>>();
		BufferedReader reader = new BufferedReader(new FileReader(fileName));
		String line = null;
		int counter = 0;
		List<String> urls = new ArrayList<String>();
		while((line = reader.readLine()) != null) {
			if(line.trim().length() > 0) {
				if(counter == 1000) {
					urlcollection.add(urls);
					counter = 0;
					urls = new ArrayList<String>();
				}
				urls.add(line.trim());
				counter++;
			}
		}
		reader.close();
		
		if(!urls.isEmpty()) urlcollection.add(urls);
		
		return urlcollection;
	}
	
	private static void parseCSLists(final String path) throws Exception {
		final List<List<String>> urlcollection = loadListUrls(path+"/listurls.txt");
		ExecutorService pool = Executors.newFixedThreadPool(urlcollection.size());
		for(int i = 0, n = urlcollection.size(); i < n; i++) {
			final int index = i;
			pool.execute(new Runnable() {
				public void run() {
					String url = null;
					try {
						System.out.println("Getting Part " + (index+1) + " of " + urlcollection.size());

						List<String> urls = urlcollection.get(index);

						PrintWriter writer = new PrintWriter(new FileWriter(path+"/listgrab/list" + index + ".txt"));
						JSONObject json = new JSONObject();
						JSONArray array = new JSONArray();
						for(int j = 0, k = urls.size(); j < k; j++) {
							url = urls.get(j);
							try {
								Placelist cslist = parseCSList(url);
								if(cslist != null && cslist.getName() != null) {
									array.put(cslist.toJSON());
								}
							}
							catch(Exception ex) {
								throw new IllegalStateException("Error while parsing " + url, ex);
							}
						}
						json.put("lists", array);
						writer.println(json.toString());
						writer.close();
					}
					catch(Exception ex1) {
						//if(ex1 instanceof IllegalStateException) throw (IllegalStateException)ex1;
						//throw new IllegalArgumentException(ex1);
						badlists.add(url);
						System.out.println("Failed " + url);
						ex1.printStackTrace();
					}
				}
			});
		}
		
		System.out.println("Waiting...");
		//make sure it has finished
		pool.shutdown();
		pool.awaitTermination(3600*24, TimeUnit.SECONDS);
		
//		System.out.println("Writing listCounters");
//		PrintWriter listCountWriter = new PrintWriter(new FileWriter(path+"/listcount.txt"));
//		for(Map.Entry<String, Integer> entry:listCount.entrySet()) {
//			listCountWriter.println(entry.getKey() + "|" + entry.getValue());
//		}
//		listCountWriter.close();
		
		System.out.println("Writing bad lists");
		PrintWriter badListsWriter = new PrintWriter(new FileWriter(path+"/badlists.txt"));
		for(String badurl:badlists) {
			badListsWriter.println(badurl);
		}
		badListsWriter.close();
	}
	
	private static class CSListContentHandler extends BodyContentHandler {
		private String element;
		private StringBuffer buffer = new StringBuffer();
		private Placelist list = new Placelist();
		
		private String listgroup;
		private String listingid;
		
		//if author not user e.g. http://www.citysearch.com/list/132861
		private String by;
		
		public void startElement(String uri, String localName, String qName, Attributes attributes) {
			if("h1".equals(localName)) {
				element = "title";
				buffer = new StringBuffer();
			}
			else if("a".equals(localName) && attributes.getLength() > 1 &&
				attributes.getValue(1).equals("user_list.coreMessage.1.author")) {
				element = "author";
				buffer = new StringBuffer();
				return;
			}
			else if("span".equals(localName) && attributes.getLength() > 0 && 
				attributes.getValue(0).equals("photo")) {
				element = "photo";
			}
			else if("img".equals(localName) && "photo".equals(element)) {
				element = null;
				Author author = list.getAuthor();
				if(author == null) author = new Author();
				author.setPhoto(attributes.getValue(0));
				list.setAuthor(author);
			}
			else if("p".equals(localName) && attributes.getLength() > 0 && 
				attributes.getValue(0).equals("listDate")) {
				element = "listdate";
				buffer = new StringBuffer();
				
				//if author not found by now
				if(list.getAuthor() == null || list.getAuthor().getId() == null) {
					Author author = list.getAuthor() == null ? new Author() : list.getAuthor();
					author.setName(by);
					setAuthor(author);
					list.setAuthor(author);
				}
			}
			else if("p".equals(localName) && "listdate".equals(element) &&
				attributes.getLength() > 0 && 
				attributes.getValue(0).equals("note")) {
				element = "description";
				buffer = new StringBuffer();
			}
			else if("div".equals(localName) && attributes.getLength() > 0 && 
				attributes.getValue(0).startsWith("listGroup")) {
				
				closePreviousListing();
				
				element = "listgroup";
			}
			else if("h2".equals(localName) && "listgroup".equals(element)) {
				element = "listgrouptitle";
				buffer = new StringBuffer();
			}
			else if("a".equals(localName) && attributes.getLength() > 1 && 
				attributes.getValue(1).startsWith("user_list.listings.") && 
				attributes.getValue(1).endsWith(".title")) {

				closePreviousListing();
				
				listingid = attributes.getValue(2);
		
				int index = listingid.indexOf("/profile/");
				int endindex = listingid.indexOf("/", index+10);
				listingid = listingid.substring(index+9, endindex);
				
				element = null;
			}
			else if("p".equals(localName) && listingid != null &&
				attributes.getLength() > 0 && 
				attributes.getValue(0).equals("note")) {
				element = "listingnote";
				buffer = new StringBuffer();
			}
		}
		
		public void endElement(String uri, String localName, String qName) {
			if("title".equals(element)) {
				list.setName(buffer.toString().trim());
				element = null;
			}
			else if("author".equals(element)) {
				Author author = list.getAuthor() == null ? new Author() : list.getAuthor();
				author.setName(buffer.toString().trim());
				setAuthor(author);
				list.setAuthor(author);
				element = null;
			}
			else if("description".equals(element)) {
				list.setDescription(buffer.toString().trim().replace('\n', ' '));
				element = null;
			}
			else if("listgroup".equals(element) && localName.equals("div")) {
				element = null;
			}
			else if("listdate".equals(element) && localName.equals("p")) {
				String date = buffer.toString();
				String dttoken = DATE_TOKEN;
				int index = date.indexOf(dttoken);
				if(index == -1) {
					index = date.indexOf(DATE_TOKEN1);
					dttoken = DATE_TOKEN1;
				}
				date = date.substring(index+dttoken.length()).trim();
				index = date.indexOf(", 19");
				if(index == -1) index = date.indexOf(", 20");
				date = date.substring(0, index+6);
				try {
					list.setCreated(DATE_FORMAT.parse(date).getTime());
				}
				catch(Exception ex) {
					throw new IllegalStateException(ex);
				}
			}
			else if("listgrouptitle".equals(element)) {
				listgroup = buffer.toString().trim();
				element = null;
			}
			else if("listingnote".equals(element)) {
				list.addToGroup(listgroup, listingid, buffer.toString().trim());
				listingid = null;
				element = null;
			}
		}		
		
		private synchronized static void setAuthor(Author author) {
			author.setId("CS_" + author.getName().replace(" ", "_"));
			Integer count = listCount.get(author.getId());
			if(count == null) listCount.put(author.getId(), ONE);
			else {
				count = new Integer(count.intValue()+1);
				listCount.put(author.getId(), count);
			}
		}
		
		public void endDocument() {
			closePreviousListing();
		}
		
		private void closePreviousListing() {
			//close previous if it didn't have note
			if(listingid != null && element == null) {
				list.addToGroup(listgroup, listingid, null);
				listingid = null;
			}
		}
		
		public void characters(char[] ch, int start, int length) {
			String text = new String(Arrays.copyOfRange(ch, start, start+length)).trim();
			buffer.append(text);
			if(text.startsWith("by ")) {
				by = text.substring(3, text.length());
				int index = by.indexOf("\n");
				if(index > -1) by = by.substring(0, index);
			}
		}
		
		public Placelist getPlacelist() {
			return list;
		}
	}
	
	public static void main(String[] args) throws Exception {
		//System.out.println(parseCSList("http://losangeles.citysearch.com/list/68892"));
		//System.out.println(parseCSList("http://boston.citysearch.com/list/205101"));
		//System.out.println(parseCSList("http://boston.citysearch.com/list/185201"));
		//System.out.println(parseCSList("http://boston.citysearch.com/list/207501"));
		//System.out.println(parseCSList("http://boston.citysearch.com/list/211761"));
		//System.out.println(parseCSList("http://boston.citysearch.com/list/199941"));
		//System.out.println(parseCSList("http://www.citysearch.com/list/132861"));
		
		//parseCSLists("/Users/imitrovic/Documents/workspace/idx/listurls.txt");
		parseCSLists(args[0]);
	}
}
