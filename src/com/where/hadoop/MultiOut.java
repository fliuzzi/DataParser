package com.where.hadoop;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.lib.MultipleTextOutputFormat;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * 
 * @author fliuzzi
 *					
 *		MultiOut.java - aids in outputting multiple files in a MR job
 */
public class MultiOut extends MultipleTextOutputFormat<NullWritable, Text>{
	private LineParser parser = new LineParser();
	
	protected String generateFileNameForKeyValue(NullWritable key, Text value, String leafFileName)
	{
		parser.parse(value);
		
		return parser.generateKey();
	}
	
	public class LineParser {

		@SuppressWarnings("unused")
		private org.apache.hadoop.io.Text value;  //Storing the value that is parsed.
		private String[] spliced;  //the array of what is parsed.

		//Can specify which tab to return.
		//If don't want to that's cool.
		public LineParser()
		{
			value = null;
			spliced = null;
		}

		
		/*
		 * The real method that parses the Text input.  Will later add functionality on how to set the splicing parameter.
		 */
		public void parse(org.apache.hadoop.io.Text value2)
		{
			value = value2;
			String temp = value2.toString();
			spliced = temp.split("\n");
		}
		
		
		//CALL PARSE FIRST!!
		public String generateKey()
		{
			if(spliced.length > 0)
			{
				try {
					JSONObject json = new JSONObject(spliced[0]);
					JSONObject location = json.optJSONObject("location");
					
					String zipcode = json.optString("zip");
					
					
					if(zipcode.length() > 0)
					{
						if(zipcode.length() > 5)
							zipcode = zipcode.substring(0,5);
						
						return zipcode;
					}
					else if(location != null) //YP and Yelp support
					{
						zipcode = location.optString("zip");
						
						if(zipcode.length() > 5)
							zipcode = zipcode.substring(0,5);
						
						return zipcode;
					}
				} catch (JSONException e) {
					System.err.println("Error generating key"+e.getMessage());
				}
				
			}
			return "null"; //TODO: better solution
		}
		
		
		
		public String getFirst()
		{
			return get(0);
		}
		
		public String get(int index)
		{
			return spliced[index];
		}
		
		public String[] getSpliced()
		{
			return spliced;
		}
	}
}
