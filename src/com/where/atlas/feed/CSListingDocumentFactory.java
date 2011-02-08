package com.where.atlas.feed;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.spatial.geohash.GeoHashUtils;
import org.apache.lucene.util.Version;

import com.where.commons.feed.citysearch.search.BaseAnalyzer;
import com.where.commons.feed.citysearch.search.NoStemAnalyzer;
import com.where.commons.feed.citysearch.search.budget.ExcludedCategories;
import com.where.commons.util.LocationUtil;
import com.where.commons.util.StringUtil;
import com.where.data.parsers.citysearch.Category;
import com.where.data.parsers.citysearch.Offer;
import com.where.place.Address;
import com.where.place.CSPlace;
import com.where.util.cache.ICache;

public class CSListingDocumentFactory {
	public static final String INFO_DOCUMENT = "BudgetInfoDocument";
	
	public static final String CATNAME = "catname";
	public static final String CATID = "catid";
	public static final String CATIDX_SUFFIX = "cat_index";
	public static final String LOCIDX_SUFFIX = "loc_index";
	
	
	public static final String VERSION = "BudgetVersion";
	public static final String INFO = "BudgetInfo";
	
	public static final String LISTING_ID = "listingid";
	public static final String WHERE_ID = "whereid";
	
	public static final String DIRECTORY_LISTING_ID = "dirlistingid";
	public static final String LISTING_ID_PREFIX = "listingid";
	public static final String PHONE = "phone";
	public static final String PHONE_PREFIX = "tel";
	
	public static final String NAME = "name";
    public static final String RAW_NAME = "raw_name";
    public static final String MATCH_NAME = "match_name";
    public static final String WORD = "word";
	
	public static final String CITY   = "city";
	public static final String ZIP    = "zip";
	public static final String STATE  = "state";
	public static final String NBH    = "neighborhood";
	public static final String MARKET = "market";
	public static final String MARKET_CITY = "market_city";
	public static final String MARKET_CITY_STATE = "market_city_state";
	public static final String CITY_ZIP = "city_zip";
	public static final String NBH_CITY = "nbh_city";
	public static final String CITY_STATE = "city_state";
	public static final String CITY_STATE_ZIP = "city_state_zip";
	public static final String NBH_CITY_STATE = "nbh_city_state";
	public static final String LOCATION = "location";
	public static final String LOCATION1 = "location1";
	public static final String LOCATION2 = "location2";
    public static final String STREET_ADDRESS = "street_address";
	
	public static final String LATITUDE = "latitude";
	public static final String LONGITUDE = "longitude";

	public static final String LATITUDE_RANGE = "latitude_range";
	public static final String LONGITUDE_RANGE = "longitude_range";

	
	public static final String CATEGORY_PREFIX = "category";
	public static final String PARENT_CATEGORY_PREFIX = "parentcategory";
	public static final String CATEGORIES = "categories";
	public static final String CATEGORY = "category";
	public static final String META_CATEGORY = "metacategory";
	
	public static final String EXCLUDED_PUBLISHERS = "excludedpubs";
	
	public static final String HAS_OFFER = "offer";
	
	public static final String RATING = "rating";
	public static final String USER_REVIEW_COUNT = "reviews";
    public static final String USER_REVIEW_AVG= "reviews_avg";
    public static final String USER_REVIEW_SCORE= "reviews_score";
	
	public static final String LIST_NAME = "list";
	public static final String LIST_URL = "listurl";
	
	public static final String POI = "poi";
	
	public static final String GEOHASH = "geohash";
	
	public static final String PPE = "ppe";
	public static final String PPE_NUMBER = "nppe";
	
	public static final String PRICE_LEVEL = "pricelevel";
	public static final String THUMB_URL = "thumburl";

	public static final String META = "meta";
	
	public static final String DYM = "dym";
	
	public static final String UPDATED = "updated";
	
	private static final Log logger = LogFactory.getLog(CSListingDocumentFactory.class);

	public static final String COUNT = "count";

	protected static class CategorySynMatchCreator
	{
		private HashMap<String, String> mapping_;
		
		protected void loadMapping()
		{
			mapping_.put("restaurant", "dinner");
			mapping_.put("restaurant", "lunch");			
		}
		
		public String synCats(String category)
		{
			if(StringUtil.isEmpty(category)){return "";}
			String toMatch = category.toLowerCase();
			StringBuffer retval = new StringBuffer();
			for(Entry<String, String> entry: mapping_.entrySet())
			{
				if(toMatch.contains(entry.getKey()))
				{
					retval.append(" " + entry.getValue());
				}
			}
			return retval.toString().trim();
		}
		
		public CategorySynMatchCreator()
		{
			mapping_ = new HashMap<String, String>();
			loadMapping();
		}
	}

	
	private CSListingDocumentFactory() {}
	
	protected static CategorySynMatchCreator catMapper_ = new CategorySynMatchCreator();
	
	public static Analyzer getAnalyzerWrapper()
	{
		PerFieldAnalyzerWrapper theWrapper =
			new PerFieldAnalyzerWrapper(new NoStemAnalyzer());
		Analyzer latlongAnalyzer = new StandardAnalyzer(Version.LUCENE_30);
		theWrapper.addAnalyzer(LATITUDE, latlongAnalyzer);
		theWrapper.addAnalyzer(LONGITUDE, latlongAnalyzer);
		theWrapper.addAnalyzer(LOCATION, new BaseAnalyzer());		
		return theWrapper;
	}
	
	public static double countScore(double count)
	{
	    //NOTE: the coefficients here were determined empirically with a
	    // graphing calculator to get a sigmoid with knees around 10 and 100
	    return 100/(1+ Math.exp(-.9*(count-85)));
	}
	
	public static Document createDocument(CSPlace poi) {
		Document document = new Document();
		
		document.add(new Field(LISTING_ID, String.valueOf(poi.getListingId()), Field.Store.YES, Field.Index.NOT_ANALYZED));
		document.add(new Field(WHERE_ID, poi.getWhereId(), Field.Store.YES, Field.Index.NOT_ANALYZED));
		
		document.add(new Field(DIRECTORY_LISTING_ID, "cs" + poi.getListingId() + (poi.getWhereId() != null ? " w" + poi.getWhereId() : ""), Field.Store.NO, Field.Index.ANALYZED));

		if(poi.isUpdated()) {
			document.add(new Field(UPDATED, "1", Field.Store.YES, Field.Index.NOT_ANALYZED));
		}

		NumericField catField = new NumericField(CATEGORY, Field.Store.YES, true);
		document.add(catField);
		boolean hasCat = false;
		
		
		List<Category> cats = poi.getCategories();
		if(cats != null && cats.size() > 0)			
		{
			hasCat = true;				
			for(Category cat : cats)
			{
				int catId = Integer.parseInt(cat.getId());
				catField.setIntValue(catId);
				hasCat = true;
			}
		}
		if(!hasCat)
		{
			catField.setIntValue(-1);
		}
				
		document.add(new Field(NAME, poi.getName(), Field.Store.YES, Field.Index.ANALYZED));
        document.add(new Field(RAW_NAME, poi.getName(), Field.Store.YES, Field.Index.NOT_ANALYZED));
        document.add(new Field(MATCH_NAME, poi.getName().toLowerCase().trim(), Field.Store.YES, Field.Index.NOT_ANALYZED));
		
		if(poi.getPhone() != null) {
			document.add(new Field(PHONE, cleanPhone(poi.getPhone()), Field.Store.YES, Field.Index.NOT_ANALYZED));
		}
		
		addLocationFields(poi, document);
				
		document.add(new NumericField(LATITUDE_RANGE,  Store.YES,true).setDoubleValue(poi.getAddress().getLat()));	
		document.add(new NumericField(LONGITUDE_RANGE, Store.YES,true).setDoubleValue(poi.getAddress().getLng()));

		String geohash = GeoHashUtils.encode(poi.getAddress().getLat(), poi.getAddress().getLng());
		document.add(new Field(GEOHASH, geohash, Field.Store.YES, Field.Index.NOT_ANALYZED));
		
		document.add(new Field(HAS_OFFER, poi.getOffer() != null ? "true" : "false", Field.Store.NO, Field.Index.NOT_ANALYZED));
		
		
		
		/*
		 *******************
		 * TODO: reimple.    removing for now: backwards compat.    --Frankie
		 *******************
		 * 
		int reviewCount = poi.userReviews().size();
		document.add(new Field(USER_REVIEW_COUNT, Integer.toString(reviewCount), Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
		
		double avgReview = 0.0;
		if(reviewCount > 0)
		{
		    double sumRating = 0.0;
		    for(Review review : poi.userReviews())
		    {
		        sumRating += review.getRating();		        
		    }
		    avgReview = sumRating/avgReview;
		}
		
		double revScore = countScore(reviewCount)*avgReview;
        document.add(new Field(USER_REVIEW_AVG, Double.toString(avgReview), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
        document.add(new Field(USER_REVIEW_SCORE, Double.toString(revScore), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));

        */
        
        
        document.add(new Field(PPE, String.valueOf(poi.getPpe()), Field.Store.YES, Field.Index.NOT_ANALYZED));
		NumericField ppe = new NumericField(PPE_NUMBER);
		ppe.setDoubleValue(poi.getPpe());
		document.add(ppe);
		
		if(poi.getPriceLevel() != null) {
			document.add(new Field(PRICE_LEVEL, String.valueOf(poi.getPriceLevel().length()), Field.Store.NO, Field.Index.NOT_ANALYZED));
		}
		if(!StringUtil.isEmpty(poi.getThumbUrl()))
		{
			document.add(new Field(THUMB_URL, poi.getThumbUrl(), Field.Store.YES, Field.Index.NOT_ANALYZED));			
		}
		
		addCSListing(poi, document);
		
		if(poi.getMenuUrl() != null && poi.getThumbUrl() != null) {
			document.setBoost(document.getBoost()*1.08f);
		}
		else { 
			if(poi.getMenuUrl() != null) {
				document.setBoost(document.getBoost()*1.03f);
			}
			
			if(poi.getThumbUrl() != null) {
				document.setBoost(document.getBoost()*1.03f);
			}
		}
		
		return document;
	}

	
	private static void addFieldValue(String field, String value, Document doc)
	{
		if(!StringUtil.isEmpty(value))
		{
			doc.add(new Field(field, raw2addressFormat(value), Field.Store.YES, Field.Index.NOT_ANALYZED));			
		}
	}
	
	private static Pattern metroMatch = Pattern.compile("(.*?)\\p{Space}+(\\w\\w)\\p{Space}+(metro|area)\\p{Space}*$");
	private static void addLocationFields(CSPlace poi, Document doc)
	{
		if(poi == null) {return;}		
		Address addr = poi.getAddress();
		if(addr == null){return;}
		String city = addr.getCity();
		if(!StringUtil.isEmpty(city)){city = city.toLowerCase().trim();}
		String state = addr.getState();
		if(!StringUtil.isEmpty(state)){state = state.toLowerCase().trim();}
		String zip = addr.getZip();
		if(zip == null){zip = "";}
		addFieldValue(CITY, city, doc);
		addFieldValue(ZIP, zip, doc);
		addFieldValue(CITY_ZIP, city + " " + zip, doc);
		addFieldValue(CITY_STATE, city + " " + state, doc);
		addFieldValue(CITY_STATE_ZIP, city + " " + state + " " + zip, doc);		
        addFieldValue(STREET_ADDRESS, addr.getAddress1(), doc);     

		//THis is a comma separated list of nbhs, which doesn't help much
		//addFieldValue(fieldName, poi.getNeighborhood(), doc);

		List<String> neighborhoods = poi.neighborhoods();
		if(neighborhoods == null){return;}
		for(String nbh : neighborhoods)
		{
			doc.add(new Field(NBH, raw2addressFormat(nbh), Field.Store.YES, Field.Index.NOT_ANALYZED));
			String s1 = raw2addressFormat(nbh) + " " + city;
			doc.add(new Field(NBH_CITY, s1.trim(), 
					Field.Store.YES, Field.Index.NOT_ANALYZED));
			s1 = s1 + " " + state;
			doc.add(new Field(NBH_CITY_STATE, s1.trim(), 
					Field.Store.YES, Field.Index.NOT_ANALYZED));						
		}			

		List<String> markets = poi.markets();
		if(markets == null){return;}

		Matcher m = metroMatch.matcher("");
		String grp1 = "";
		String grp2 = "";
		for(String mkt : markets)
		{
			mkt = raw2addressFormat(mkt);
			if(!StringUtil.isEmpty(mkt))
			{
				m.reset(mkt);
				if(m.matches())
				{
					if(m.groupCount() == 3)
					{
						grp1 = m.group(1);
						grp2 = m.group(2);
						doc.add(new Field(MARKET_CITY, grp1, Field.Store.YES, Field.Index.NOT_ANALYZED));										
						doc.add(new Field(MARKET_CITY_STATE, grp1 + " " + grp2, Field.Store.YES, Field.Index.NOT_ANALYZED));										
						doc.add(new Field(MARKET, mkt, Field.Store.YES, Field.Index.NOT_ANALYZED));																			
					}				
				}
				else
				{
					doc.add(new Field(MARKET, mkt, Field.Store.YES, Field.Index.NOT_ANALYZED));										
				}				
			}
		}					
	}
	
	public static void toLocationWords(CSPlace poi, PrintWriter writer) {
		writer.println( raw2addressFormat(poi.getAddress().getCity()));
		writer.println( raw2addressFormat(poi.getAddress().getZip()));		
		writer.println( raw2addressFormat(poi.getAddress().getCity() + " " + poi.getAddress().getState()));
		for(String n:poi.neighborhoods()) {
			writer.println( raw2addressFormat(n));
			writer.println( raw2addressFormat(n + " " + poi.getAddress().getCity().trim()));
			writer.println( raw2addressFormat(n + " " + 
							poi.getAddress().getCity()) + " " +
							poi.getAddress().getState());

		}
		
		for(String m:poi.markets()) {
			writer.println(raw2addressFormat(m.trim()));
		}
	}

	
	public static String cleanPhone(String phone) {
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
	
	public static void toWords(CSPlace poi, PrintWriter writer) {
		collectLines(poi.getName(), writer);
		
		for(Category c : poi.categories()) {
			collectLines(c.getName(), writer);	
			if(c.getParent() != null) {
				collectLines(c.getParent().getName(), writer);	
			}
		}
		
	}
		
	public static String raw2addressFormat(String input)
	{
		if(StringUtil.isEmpty(input)){return input;}
		input = input.toLowerCase();
		input = input.replaceAll("\\p{P}+", "");
		input = input.replaceAll("\\s+", " ");
		input = input.trim();
		
		return input;
	}
	
	private static void collectLines(String text, PrintWriter writer) {
		if(text == null) return;
		
		List<String> tokens = StringUtil.split(text.toLowerCase());
		StringBuffer buffer = new StringBuffer();
		for(String token:tokens) {
			if(!StopAnalyzer.ENGLISH_STOP_WORDS_SET.contains(token)) {
				buffer.append(token);
				buffer.append(" ");
			}
		}
		String line = buffer.toString().trim();
		writer.println(line);
		
		buffer = new StringBuffer();
		for(String token:tokens) {
			buffer.append(token);
			buffer.append(" ");
		}
		String line1 = buffer.toString().trim();
		if(!line.equals(line1)) {
			writer.println(line1);
		}
		
	}
	
	public static Document createCategoryDocument(Category category) {
		Document document = new Document();
		document.add(new Field(META_CATEGORY, analyzeCategoryName(category.getName()), Field.Store.NO, Field.Index.NOT_ANALYZED));
		document.add(new Field(CATEGORY, category.getId(), Field.Store.YES, Field.Index.NOT_ANALYZED));
		
		return document;
	}
	
	public static String analyzeCategoryName(String name) {
		String[] split = name.split(" ");
		StringBuffer buffer = new StringBuffer();
		for(String s:split) {
			buffer.append(s.toLowerCase().trim());
		}
		//All this seems to do is remove multiple spaces and 
		//downcase
		return buffer.toString();
	}
	
	public static Document createCategoryDocument(CSPlace poi) {
		if(poi.categories().isEmpty()) return null;
		
		Document document = new Document();
		
		String formatLat = LocationUtil.formatCoord(poi.getAddress().getLat(), 1);
		String formatLng = LocationUtil.formatCoord(poi.getAddress().getLng(), 2);
		
		document.add(new Field(LATITUDE, formatLat, Field.Store.NO, Field.Index.ANALYZED));
		document.add(new Field(LONGITUDE, formatLng, Field.Store.NO, Field.Index.ANALYZED));
		
		String geohash = GeoHashUtils.encode(poi.getAddress().getLat(), poi.getAddress().getLng());
		document.add(new Field(GEOHASH, geohash, Field.Store.NO, Field.Index.NOT_ANALYZED));
		
		StringBuffer meta = new StringBuffer();
		for(Category c:poi.categories()) {
			if(!c.isPayment()) {
				document.add(new Field(CATEGORIES, c.getId()+"&"+c.getName()+(c.getParent() != null?"&"+c.getParent().getId()+"&"+c.getParent().getName():""), Field.Store.YES, Field.Index.NOT_ANALYZED));
				meta.append(CATEGORY_PREFIX);
				meta.append(c.getId());
				meta.append(" ");
				if(c.getParent() != null && c.getParent().getName() != null) {
					meta.append(PARENT_CATEGORY_PREFIX);
					meta.append(c.getParent().getId());
					meta.append(" ");
				}
				break;
			}
		}
		
		String metatxt = meta.toString();
		if(metatxt.trim().length() > 0) {
			document.add(new Field(META_CATEGORY, metatxt, Field.Store.NO, Field.Index.ANALYZED));
		}
		
		excludeFromPublishers(poi, document);
		
		return document;
	}
	
	private static void addCSListing(CSPlace poi, Document document) {
		try {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			ObjectOutputStream oout = new ObjectOutputStream(bout);
			oout.writeObject(poi.toCSListing());
			oout.close();
			
			byte[] bytes = bout.toByteArray();
			Field toStore = new Field(POI,bytes,0,bytes.length, Field.Store.YES);
			toStore.setOmitNorms(true);
            toStore.setOmitTermFreqAndPositions(true);
			
			document.add(toStore);
		}
		catch(Exception ex) {
			logger.error("Error creating document " + poi.getName() + " " + poi.getAddress().getCity(), ex);
			
			throw new IllegalArgumentException(ex);
		}
	}
	
	public static CSPlace createCSListing(Document document, int documentId) {
		return createCSListing(document, documentId, null, false);
	}
	
	public static CSPlace createCSListing(Document document, int documentId, ICache cache, boolean checkcache) {
		if(document == null) return null;
		try {
			CSPlace poi = null;
			if(checkcache && cache != null) {
				Object cachedob = cache.getObject(""+documentId);
				if(cachedob != null) poi = (CSPlace) cachedob;
			} 
			if(poi == null) {
				byte[] bytes = document.getBinaryValue(POI);
				if(bytes != null) {			
					ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
					poi = (CSPlace)in.readObject();
					in.close();
				} else poi = LocalezeUtil.generateListing(document);
				if(poi != null && cache != null) {
					cache.setObject(""+documentId, poi);
				}
			}
			return poi;
		}
		catch(Exception ex) {
			logger.error("Error extracting CSListing " + document.get(NAME), ex);
			
			throw new IllegalArgumentException(ex);
		}
	}
	
	public static CSPlace getCSListingFromCache(int docid, ICache cache) {
		if(cache == null) return null;
		Object cachedob = cache.getObject(""+docid);
		if(cachedob == null) return null;
		return (CSPlace) cachedob;
	}
	
	public static Document createAdDocument(CSPlace poi) {
		Document document = new Document();
		
		document.add(new Field(LISTING_ID, String.valueOf(poi.getListingId()), Field.Store.YES, Field.Index.NOT_ANALYZED));
		document.add(new Field(NAME, poi.getName(), Field.Store.YES, Field.Index.ANALYZED));
		
		document.add(new Field(LATITUDE, LocationUtil.formatCoord(poi.getAddress().getLat(), 1), Field.Store.NO, Field.Index.ANALYZED));
		document.add(new Field(LONGITUDE, LocationUtil.formatCoord(poi.getAddress().getLng(), 2), Field.Store.NO, Field.Index.ANALYZED));
				
		String geohash = GeoHashUtils.encode(poi.getAddress().getLat(), poi.getAddress().getLng());
		document.add(new Field(GEOHASH, geohash, Field.Store.NO, Field.Index.NOT_ANALYZED));
				
		if(!StringUtil.isEmpty(poi.getCategory()))
		{
			String[] split = poi.getCategory().split(" ");
			String cat = split[0].trim() + " " + split[1];
			document.add(new Field(CATEGORY, cat, Field.Store.YES, Field.Index.NOT_ANALYZED));
		}
		
		excludeFromPublishers(poi, document);
		
		document.add(new Field(HAS_OFFER, poi.getOffer() != null ? "true" : "false", Field.Store.YES, Field.Index.NOT_ANALYZED));
				
		document.add(new Field(PPE, String.valueOf(poi.getPpe()), Field.Store.YES, Field.Index.NOT_ANALYZED));
		NumericField ppe = new NumericField(PPE_NUMBER);
		ppe.setDoubleValue(poi.getPpe());
		document.add(ppe);
		
		StringBuffer buffer = new StringBuffer();
		
		boost(poi.getName(), buffer, 5);
		
		if(poi.getCategory() != null) {
			boost(poi.getCategory(), buffer, 5);
		}
				
		document.add(new Field(META, buffer.toString(), Field.Store.NO, Field.Index.ANALYZED));
		
		addCSListing(poi, document);
		
		return document;
	}	
	
	private static void excludeFromPublishers(CSPlace poi, Document document) {
		StringBuffer buffer = new StringBuffer();
		
		Map<String, Set<String>> excluded = ExcludedCategories.excludedCategoriesbyPublisher();
		for(Entry<String, Set<String>> entry : excluded.entrySet()) {
			String pubid = entry.getKey();
			Set<String> categories = entry.getValue();
			
			List<Category> poicategories = poi.categories();
			for(Category c:poicategories) {
				if(categories.contains(c.getId()) ||
					(c.getParent() != null && categories.contains(c.getParent().getId()))		
				) {
					buffer.append(pubid);
					buffer.append(" ");
					break;
				}
			}
		}
		
		document.add(new Field(EXCLUDED_PUBLISHERS, buffer.toString().trim(), Field.Store.NO, Field.Index.ANALYZED));
	}
	
	public static Document createLocationDocument(CSPlace poi) {
		Document document = new Document();
				
		document.add(new Field(META, poi.getAddress().getCity().toLowerCase().trim(), Field.Store.NO, Field.Index.ANALYZED));
		document.add(new Field(CITY, poi.getAddress().getCity().toLowerCase().trim(), Field.Store.YES, Field.Index.NOT_ANALYZED));
		document.add(new Field(STATE, poi.getAddress().getState().toLowerCase().trim(), Field.Store.YES, Field.Index.NOT_ANALYZED));
		
		return document;
	}			
	
	
	public static CSPlace createAdCSListing(Document document) {
		try {
		    CSPlace poi = new CSPlace();
			poi.setListingId(document.get(LISTING_ID));
			poi.setName(document.get(NAME));
			poi.setPpe(Double.parseDouble(document.get(PPE)));
			String category = document.get(CATEGORY);
			if(category != null && category.trim().length() > 0) {
				String[] split = category.split(" ");
				poi.addCategory(new Category(split[1], split[0]));
			}
			if(document.get(HAS_OFFER).equals("true")) {
				Offer offer = new Offer();
				poi.setOffer(offer);
			}
			return poi;
		}
		catch(Exception ex) {
			logger.error("Error extracting CSListing " + document.get(NAME) + " " + document.get(CITY), ex);
			
			throw new IllegalArgumentException(ex);
		}
	}
	
	public static void boost(String value, StringBuffer collector, int boost) {
		if(value == null || value.trim().length() == 0) return;
		
		for(int i = 0; i < boost; i++) {
			collector.append(value);
			collector.append(" ");
		}
	}
	
	public static String formatUrl(String url) {
		StringBuffer buffer = new StringBuffer();
		char[] chrs = url.toCharArray();
		for(char chr:chrs) {
			if(Character.isDigit(chr) || Character.isLetter(chr)) {
				buffer.append(chr);
			}
		}
		return buffer.toString();
	}
}