package com.where.data.parsers.cslists;

import org.json.JSONArray;
import org.json.JSONObject;

public class CSListAdder extends CSListMerger
{    
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
    
    private static JSONArray syncLists(String listFile1, String listFile2) throws Exception{
        
        JSONObject big = getObjectFromFile(listFile1);
        JSONArray bigList = big.getJSONArray("lists");
        
        JSONObject small = getObjectFromFile(listFile2);
        
        //Since all new values are going to be used...
        JSONArray syncedList = small.getJSONArray("lists");

        int max = bigList.length();
        int smallMax = syncedList.length();
        
        System.out.println("*** "+listFile1+" has "+max+" lists");
        System.out.println("*** "+listFile2+" has "+smallMax+" lists");

        boolean valid;
        JSONObject jo;
        
        //loop through the old lists, add if it doesn't already exist in syncedLists
        for(int i = 0; i < max; ++i)
        {
            valid = true;
            jo = bigList.getJSONObject(i);
            
            //loop through the toBeAdded Lists
            for(int j = 0; j < smallMax; ++j)
            {
                //if a dupe is found, be sure not to use the old version
                if(isEquivalent(jo, syncedList.getJSONObject(j)))
                    valid = false;
            }
            if(valid) syncedList.put(jo);
        }
        return syncedList;
    }
    
    
    public static void main(String[] args)
    {
        if(args == null || args.length != 3) {
            System.out.println("Usage: CSListAdder <OriginalList FilePath> <ToBeAdded FilePath> <Output FilePath>");
            return;
        }
        System.out.println("Syncing Lists...");
        try {
            generateList(syncLists(args[0], args[1]), args[2]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Done.");
    }

}
