package com.where.data.citysearch;

import java.io.*;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.PorterStemFilter;
import org.apache.lucene.analysis.standard.*;
import org.apache.lucene.util.Version;

public class Analyzer extends org.apache.lucene.analysis.Analyzer {
    public TokenStream tokenStream(String fieldName, Reader reader) {
        TokenStream result = new StandardTokenizer(Version.LUCENE_30, reader);
        
 	    result = new StandardFilter(result);
 	    result = new LowerCaseFilter(result);
 	    result = new ASCIIFoldingFilter(result);
 	    result = new StopFilter(false, result, StopAnalyzer.ENGLISH_STOP_WORDS_SET, true);
 	    result = new PorterStemFilter(result);
               
        return result;
    }    
}