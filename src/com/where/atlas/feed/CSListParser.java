package com.where.atlas.feed;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;

import org.apache.lucene.document.Document;
import org.json.JSONArray;
import org.json.JSONObject;

import com.where.atlas.CSListPlace;
import com.where.utils.Utils;

public class CSListParser implements FeedParser
{
    CSListParserUtils parserutils;
    
    
    public CSListParser(CSListParserUtils csparserutils)
    {
        parserutils = csparserutils;
    }
    
    public void parse(PlaceCollector collector, InputStream ins) throws Exception
    {
        //line-parse cslist.json file and store all non-null to StringBuffer buffer
        BufferedReader reader = new BufferedReader(new InputStreamReader(ins));
        String line = null;
        StringBuffer buffer = new StringBuffer();
        while((line = reader.readLine()) != null) {
            buffer.append(line);
        }
        reader.close();
    
        PrintWriter dymwriter = new PrintWriter(new FileWriter(parserutils.getDymFilePath()));
        int count = 0;
        JSONObject json = new JSONObject(buffer.toString());
        JSONArray lists = json.getJSONArray("lists");
        System.out.println(lists.length() + " lists in JSON");
        for(int i = 0, n = lists.length(); i < n; i++) {
            CSListPlace pl = CSListPlace.fromJSON(lists.getJSONObject(i));
            
            //set the hashed placelistID
            pl.setId(Utils.hash(pl.getSourceUrl()));
            
            //collect!
            collector.collect(pl);
        }
        
    
    }
}
