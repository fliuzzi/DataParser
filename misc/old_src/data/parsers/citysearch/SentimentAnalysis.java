package com.where.data.parsers.citysearch;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.util.AttributeImpl;

import opennlp.tools.lang.english.PosTagger;
import opennlp.tools.postag.POSDictionary;

//import com.where.commons.feed.citysearch.CSListing;
//import com.where.commons.feed.citysearch.Review;
//import com.where.commons.feed.citysearch.TermFreq;
import com.where.commons.feed.citysearch.nlp.Phrases;
import com.where.commons.feed.citysearch.search.NoStemAnalyzer;
import com.where.commons.stem.PorterStemmer;
import com.where.commons.util.StringUtil;

public class SentimentAnalysis {
	private SentimentAnalysis() {}
	
	private static Log logger = LogFactory.getLog("SentimentAnalysis");
	
	private static PosTagger tagger;
	private static Set<String> nlpFilter = new HashSet<String>();
	private static Set<String> stopWords = new HashSet<String>();
	private static PorterStemmer stemmer = new PorterStemmer();
	
	static {
//		nlpFilter.add("IN");
//		nlpFilter.add("PDT");
//		nlpFilter.add("DT");
//		nlpFilter.add("PRP");
//		nlpFilter.add("PRP$");
//		nlpFilter.add("RB");
//		nlpFilter.add("RBR");
//		nlpFilter.add("RBS");
//		nlpFilter.add("VB");
//		nlpFilter.add("VBD");
//		nlpFilter.add("VBN");
//		nlpFilter.add("VBZ");
//		nlpFilter.add("VBP");
//		nlpFilter.add("VBG$");
		
		nlpFilter.add("NN");
		nlpFilter.add("NNS");
		nlpFilter.add("NNP"); 
		nlpFilter.add("NNPS");
		nlpFilter.add("VBN");
		nlpFilter.add("VBD");
		nlpFilter.add("JJ");
		nlpFilter.add("JJS");
		
		//nlp this!
		stopWords.add("i've");
		stopWords.add("been");
		stopWords.add("can't");
		stopWords.add("went");
		stopWords.add("had");
		stopWords.add("end");
		stopWords.add("ordered");
		stopWords.add("review");
		stopWords.add("reviews");
		stopWords.add("other");
		stopWords.add("were");
		stopWords.add("food");
		stopWords.add("place");
		stopWords.add("time");
		stopWords.add("restaurant");
		stopWords.add("restaurants");
	}
	
	public static void extractTermFreqs(CSListing poi) {
		if(tagger == null) return;
		
		try {
			List<Review> reviews = poi.userReviews();
			List<Review> editorials = poi.editorials();
			
			if(reviews.isEmpty() && editorials.isEmpty()) return;
			
			Map<String, TermFreq> terms = new HashMap<String, TermFreq>();
			
			for(Review review:reviews) {				
				nlp(poi, review, terms);
			}
			
			for(Review review:editorials) {				
				nlp(poi, review, terms);
			}
			
			if(terms.isEmpty()) return;
							
			List<TermFreq> freqs = new ArrayList<TermFreq>();
			for(Map.Entry<String, TermFreq> entry:terms.entrySet()) {
				freqs.add(entry.getValue());
			}
			terms.clear();
			terms = null;
				
			Collections.sort(freqs, new Comparator<TermFreq>() {
				public int compare(TermFreq tf1, TermFreq tf2) {
					return tf2.getFreq()-tf1.getFreq();
				}
			});	
			
			List<TermFreq> top = new ArrayList<TermFreq>();
			for(int i = 0, n = freqs.size(); i < n && i < 10; i++) {
				top.add(freqs.get(i));
			}
			freqs.clear();
			freqs = null;
			
			if(!top.isEmpty()) {
				int low = top.get(top.size()-1).getFreq();
				for(TermFreq f:top) {
					f.normalizeFreq(f.getFreq()/low);
				}
				
				poi.setTermFreqs(top);
				
				updateAsIn(poi);
			}
		}
		catch(Exception ex) {
			logger.error("Error while doing nlp", ex);
		}
	}
	
	private static void nlp(CSListing poi, Review review, Map<String, TermFreq> terms) throws Exception {
		StringReader reader = new StringReader(removePlaceName(review, poi));
		
		NoStemAnalyzer analyzer = new NoStemAnalyzer();
		TokenStream stream = analyzer.tokenStream(null, reader);
		stream.reset();
					
		while(stream.incrementToken()) {
			for(Iterator<AttributeImpl> it = stream.getAttributeImplsIterator();it.hasNext();) {
				AttributeImpl attr = it.next();
				if(attr.getClass().equals(org.apache.lucene.analysis.tokenattributes.TermAttributeImpl.class)) {
					String term = ((org.apache.lucene.analysis.tokenattributes.TermAttributeImpl)attr).term();
					//Do this in Analyzer, write Filter
					String tagged = tagger.tag(term);
					if(isOKPOSTag(tagged)) {
						stemmer.reset();
						String key = stemmer.stem(term);
						
						TermFreq tf = terms.get(key);
						if(tf == null) tf = new TermFreq(term, 1);
						else tf = new TermFreq(term, tf.getFreq()+1);
						
						terms.put(key, tf);
					}
				}
			}
		}
		
		stream.end();
		stream.close();
		reader.close();		
	}	
	
	private static boolean isOKPOSTag(String tagged) {
		//System.out.println(tagged);
		
		int index = tagged.indexOf("/");
		String tag = tagged.substring(index+1).trim();
		String value = tagged.substring(0, index);
		
		//more stop words here but look at NLP to figure this out
		if(stopWords.contains(value)) return false;
		
		return nlpFilter.contains(tag);
		//return tag.startsWith("NN") || tag.equals("VBD") || tag.equals("VBN");
		//return !nlpFilter.contains(tag);
	}
	
	private static String removePlaceName(Review review, CSListing poi)
	{
		if(review == null || StringUtil.isEmpty(review.getReview()) || poi == null ||
		   StringUtil.isEmpty(poi.getName()))
		{
			return "";
		}

		StringBuilder all = new StringBuilder(review.getReview().toLowerCase());		
		if(review.getPros() != null)
		{
			all.append(review.getPros().toLowerCase() + " ");
		}
		if(review.getCons() != null)
		{
			all.append(review.getCons().toLowerCase());
		}
		String toRemove = poi.getName().toLowerCase().trim();
		return removeString(all, toRemove);
	}
	
	public static String removeString(StringBuilder all, String toRemove) {
		StringBuffer buffer = new StringBuffer();
		int index = -1;
		int trlen = toRemove.length();
		int startIndex = 0;
		boolean needMore = true;
		int lastIndex = 0;
		while((index = all.indexOf(toRemove,startIndex)) > -1 && index+trlen <= all.length()) {
			buffer.append(all.substring(startIndex, index));
			startIndex = index+trlen;
			if(index+trlen == all.length())
			{
				needMore = false;
			}
			lastIndex = index;
		}
		if(needMore)
		{
			buffer.append(all.substring(lastIndex, all.length()-1));			
		}
		return buffer.toString().trim();
	}
	
	private static void updateAsIn(CSListing poi) {
		List<String> phrases = extractPhrases(poi);

		List<TermFreq> top = poi.getTermFreqs();
		for(TermFreq tf:top) {
			List<TermFreq> phraseList = new ArrayList<TermFreq>();
			for(String phrase:phrases) {
				boolean isPart = false;
				List<String> split = StringUtil.split(phrase.trim());
				for(String s:split) {
					if(s.startsWith(tf.getTerm())) isPart = true;
				}
				if(isPart) {
					TermFreq tf1 = new TermFreq(phrase, 1);
					int index = phraseList.indexOf(tf1);
					if(index == -1) phraseList.add(tf1);
					else phraseList.get(index).increaseFreq();
				}
			}
			
			Collections.sort(phraseList, new Comparator<TermFreq>() {
				public int compare(TermFreq tf1, TermFreq tf2) {
					return tf2.getFreq()-tf1.getFreq();
				}
			});	
			
			for(TermFreq tf2:phraseList) {
				if(tf.getAsInPhrases().size() > 2) break;
				tf.addAsIn(tf2.getTerm());
			}
			
			phraseList.clear();
			phraseList = null;
		}
	}
	
	private static List<String> extractPhrases(CSListing poi) { 
		List<String> phrases = new ArrayList<String>();
		List<Review> editorials = poi.editorials();
		for(Review review:editorials) {
			phrases.addAll(Phrases.split(review.getReview() + (review.getPros() != null ? " " + review.getPros() : "") + (review.getCons() != null ? " " + review.getCons() : "")));
		}
		List<Review> reviews = poi.userReviews();
		for(Review review:reviews) {
			phrases.addAll(Phrases.split(review.getReview() + (review.getPros() != null ? " " + review.getPros() : "") + (review.getCons() != null ? " " + review.getCons() : "")));
		}
		return phrases;
	}
	
	public static void setTagger(String dictFolder) {
		try {
			tagger = new PosTagger(dictFolder + "/tag.bin.gz", new POSDictionary(dictFolder + "/tagdict"));
		}
		catch(Exception ex) {
			throw new IllegalArgumentException("Failed to load Dictionary files", ex);
		}
	}	
}
