package com.where.data.parsers.cslists;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
//takes two files..take this make new class that will add as well as replace
//create new CSListsAdder

public class CSListMerger
{
    protected static JSONObject getObjectFromFile(String file) throws IOException, JSONException
    {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = null;
        StringBuffer buffer = new StringBuffer();
        while((line = reader.readLine()) != null) {
            buffer.append(line);
        }
        reader.close();
        JSONObject json = new JSONObject(buffer.toString());
        return json;
    }
    
    private static boolean isEquivalent(JSONObject fromBig, JSONObject fromSmall)
    {
        String nameBig = fromBig.optString("name");
        String nameSmall = fromSmall.optString("name");
        JSONObject sourceBig = fromBig.optJSONObject("source");
        JSONObject sourceSmall = fromSmall.optJSONObject("source");
        
        if(sourceSmall != null && nameBig != null && nameSmall != null && sourceBig != null) {
        	String uriBig = sourceBig.optString("url");
        	String uriSmall = sourceSmall.optString("url");
        	if(uriBig != null && uriSmall != null && uriBig.equalsIgnoreCase(uriSmall) && nameSmall.equalsIgnoreCase(nameBig)) return true;
        }
        return false;
    }
    
    private static JSONArray mergeLists(String listFile1, String listFile2) throws Exception {
        JSONObject big = getObjectFromFile(listFile1);
        JSONArray bigList = big.getJSONArray("lists");
    
        JSONObject small = getObjectFromFile(listFile2);
        JSONArray smallList = small.getJSONArray("lists");
        JSONArray retval = new JSONArray();

        int max = bigList.length();
        int smallMax = smallList.length();
        
        System.out.println("*** "+listFile1+" has "+max+" lists");
        System.out.println("*** "+listFile2+" has "+smallMax+" lists");
        
        for(int i = 0; i < max; ++i)
        {
            boolean valid = true;
            JSONObject jo = bigList.getJSONObject(i);
            for(int j = 0; j < smallMax; ++j)
            {
                if(isEquivalent(jo, smallList.getJSONObject(j)))
                {
                    
                    valid = false;
                    JSONObject n = new JSONObject(smallList.getJSONObject(j).toString());
                    
//                    String uri = n.optJSONObject("source").optString("url");   // this was all done to deal with the fact that jim gave us stuff with the same uri
//                    String name = n.optString("name");
//                    if(name.indexOf("Helio") != -1) uri += "/1";
//                    else if(name.startsWith("Cristie")) uri += "/2"; 
//                    else if(name.startsWith("Jeff")) uri += "/3";
//                    else if(name.startsWith("Clive")) uri += "/4";
//                    else if(name.startsWith("Barbara")) uri += "/5";
//                    n.optJSONObject("source").put("url", uri);
                    retval.put(n);  // we want to replace any equivalent one with the new version
                    System.out.println("** Replacing : "+n.optString("name")+" - "+n.optJSONObject("source").optString("url"));
                    break;
                }
            }
            if(valid)
            {
                retval.put(jo);
            }
        }
        
        
        return retval;
    }

    protected static void generateList(JSONArray arr, String filepath) throws Exception
    {
        System.out.println("** Final list has "+arr.length()+" lists");
        JSONObject finallist = new JSONObject();
        finallist.put("lists", arr);
        PrintWriter writer = new PrintWriter(new FileWriter(filepath));
		writer.println(finallist.toString());
		writer.close();
    }

    public static void main(String[] args)
    {
        if(args == null || args.length != 3) {
        	System.out.println("Usage: CSListMerger <BigList FilePath> <SmallList FilePath> <Output FilePath>");
        	return;
        }
        System.out.println("Start");
        try {
        	generateList(mergeLists(args[0], args[1]), args[2]);
        } catch (Exception e) {
        	e.printStackTrace();
        }
        System.out.println("Done");
    }
}
