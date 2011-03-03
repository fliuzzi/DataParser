package com.where.data.parsers.citysearch;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONObject;

public class TermFreq implements Serializable {
	private static final long serialVersionUID = 2722807692585859256L;
	
	private String term;
	private int freq;
	private int normalized;
	private Set<String> asInPhrases = new HashSet<String>();
	
	public TermFreq(String term, int freq) {
		this.term = term;
		this.freq = freq;
	}

	public String getTerm() {
		return term;
	}

	public int getFreq() {
		return freq;
	}
	
	public void increaseFreq() {
		freq++;
	}
	
	public void normalizeFreq(int normalized) {
		this.normalized = normalized;
	}
	
	public int getNormalizedFreq() {
		return normalized;
	}
	
	public void addAsIn(String asIn) {
		asIn = clean(asIn, term);
		if(asIn != null) asInPhrases.add(asIn);
	}
	
	public Set<String> getAsInPhrases() {
		return asInPhrases;
	}
	
	private static String clean(String phrase, String term) {
		char[] chrs = phrase.toCharArray();
		if(Character.isLetter(chrs[chrs.length-1])) {
			if(isPart(phrase, term)) return phrase;
			return null;
		}
		else {
			phrase = phrase.substring(0, phrase.length()-1);
			if(isPart(phrase, term)) return phrase;
			return null;
		}
	}
	
	private static boolean isPart(String phrase, String term) {
		String[] split = phrase.split(" ");
		boolean isPart = false;
		for(String s:split) {
			if(s.startsWith(term)) return true;
		}
		return isPart;
	}
	
	public JSONObject toJSON() {
		try {
			JSONObject json = new JSONObject();
			json.put("term", term);
			json.put("magnitude", freq);
			
//			if(!asInPhrases.isEmpty()) {
//				JSONArray a = new JSONArray();
//				for(String asIn:asInPhrases) {
//					a.put(asIn);
//				}
//				json.put("asIn", a);
//			}
			
			return json;
		}
		catch(Exception ex) {
			throw new IllegalStateException(ex);
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null || !(obj instanceof TermFreq)) return false;
		
		return term.equals(((TermFreq)obj).term);
	}
	
	@Override 
	public int hashCode() {
		return 17+37*term.hashCode();
	}
}
