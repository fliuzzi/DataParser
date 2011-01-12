package com.where.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.lucene.search.spell.Dictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class DidYouMeanIndex { 
	public static void main(String[] args) throws IOException {
		if (args.length != 2) { 
			System.out.println("Usage: java com.where.commons.feed.citysearch.DidYouMeanIndex SpellCheckerIndexDir wordFilePath"); 
			System.exit(1);
		}
		
		String spellCheckDir = args[0]; 
		String wordFilePath = args[1]; 
		//String advWordFilePath = args[2];
		//String indexField = CSListingDocumentFactory.DYM;
		
		System.out.println("Now build SpellChecker index..."); 
		File file = new File(spellCheckDir);
		file.mkdirs();
		Directory dir = FSDirectory.open(file); 
		SpellChecker spell = new SpellChecker(dir);	
		long startTime = System.currentTimeMillis();
		//Directory dir2 = FSDirectory.open(new File(indexDir)); 
		//IndexReader r = IndexReader.open(dir2);
		//try {
			//spell.indexDictionary(new LuceneDictionary(r, indexField)); 
			PlaceDictionary dict = new PlaceDictionary(new String[] {wordFilePath});
			spell.indexDictionary(dict);
		//} 
		//finally {
			//r.close();
		//} 
		dir.close(); 
		//dir2.close(); 
		long endTime = System.currentTimeMillis(); 
		System.out.println(" took " + (endTime-startTime) + " milliseconds");
	}
	
	private static class PlaceDictionary implements Dictionary {
		private Set<String> words = new HashSet<String>();
		
		public PlaceDictionary(String[] filePaths) {
			synchronized(this) {
				for(String filePath:filePaths) {
					try {
						BufferedReader reader = new BufferedReader(new FileReader(filePath));
						String line = null;
						while((line = reader.readLine()) != null) {
							line = line.trim();
							if(line.endsWith(",")) {
								line = line.substring(0, line.length()-1);
							}
							if(line.length() >= 3) {
								words.add(line);
							}
						}
						reader.close();
					}
					catch(Exception ignored) {}
				}
			}
		}
		
		public synchronized Iterator<String> getWordsIterator() {
			return words.iterator();
		}
	}
}