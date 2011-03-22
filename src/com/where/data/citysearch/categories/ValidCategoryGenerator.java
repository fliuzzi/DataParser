package com.where.data.citysearch.categories;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;



import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


public class ValidCategoryGenerator
{
    public static String [] cats = new String [] 
    {
            "Restaurants",
            "Bars & Pubs",
            "Food & Dining",
            "Butcher Shops",
            "Dessert Shops",
            "Gift Shop",
            "Hobby Shops",
            "Food Stores",
            "Gift Shops",
            "Shopping",
            "Attractions",
            "Gifts & Novelties",
            "Musical Entertainers",
            "Entertainers",
            "Arts & Entertainment",
            "Live Theatre",
            "Performing Arts Events",
            "Music Events",
            "Dance Events",
            "Pet Stores",
            "Community Events",
            "Dog Trainers",
            "Pet Grooming",
            "Festivals",
            "Housing Rental",
            "Bookstores",
            "Recreational Activities",
            "Movie Rentals",
            "Art Houses",
            "Movie Theaters",
            "Bars & Clubs",
            "Sports Stadiums & Athletic Fields",
            "Real Estate Sales & Services",
            "Shopping",
            "Sales Events",
            "Sporting Goods",
            "Exercise"
    };
    
    private static HashSet<String> goodCats = new HashSet<String>();
    
    
    public static void main(String [] args) throws Exception
    {
        goodCats.addAll(Arrays.asList(cats));
        String fname = args[0];
        String output = args[1];
        BufferedReader reader = new BufferedReader(new FileReader(new File(fname)));
        Object json = new JSONParser().parse(reader);
        reader.close();
        JSONObject arr = (JSONObject) json;
        for(String cat: cats)
        {
            recurseNodes(cat, arr);
        }
        BufferedWriter bw = new BufferedWriter(new FileWriter(new File(output)));
        for(String cat: goodCats)
        {
            bw.write(cat);
            bw.newLine();
        }
        bw.close();
        
    }
    
    private static void saveTree(JSONObject obj)
    {
        Set<?> keys = obj.keySet();
        for(Object o : keys)
        {
            String key = o.toString();
            goodCats.add(key);
            
            Object val = obj.get(o);
            if(val instanceof JSONObject)
            {
                saveTree((JSONObject)val);
            }
        }        
    }
    
    public static void recurseNodes(String lookup, JSONObject obj)
    {
        Set<?> keys = obj.keySet();
        for(Object o : keys)
        {
            String key = o.toString();
            
            Object val = obj.get(o);
            if(val instanceof JSONObject)
            {
                if(lookup.equals(key))
                {
                    saveTree((JSONObject)val);
                }
                else if(val != null)
                {
                    recurseNodes(lookup, (JSONObject)val);
                }                
            }
        }
        
    }
}
