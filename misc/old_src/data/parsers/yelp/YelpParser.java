package com.where.data.parsers.yelp;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;

public class YelpParser {
	private YelpParser() {}
	
	@SuppressWarnings("unused")
	private static void parseYelp(String[] poi, PrintWriter writer) {
		String id = poi[0];
		String name = poi[1];
		String street = poi[2];
		String city = poi[3];
		String zip = poi[4];		
		String state = poi[5];
		
		char[] chrs = name.trim().toCharArray();
		StringBuffer buffer = new StringBuffer();
		for(char chr:chrs) {
			if(Character.isLetter(chr) || Character.isWhitespace(chr)) {
				buffer.append(chr);
			}
		}
		buffer.append(" ");
		buffer.append(city.trim());
		
		String yelpName = buffer.toString().toLowerCase().replace(' ', '-');
		
		String url = "http://www.yelp.com/biz/" + yelpName + "?rpp=1000";
		try {
			//System.out.println(url);
			String page = readPage(url);
			int index = page.indexOf("<meta name=\"y_key\" content=\"");
			int end = page.indexOf("\"", index+32);
			String ykey = page.substring(index+28, end);
			//System.out.println(ykey);
			index = page.indexOf("<link rel=\"alternate\" type=\"application/rss+xml\"");
			index = page.indexOf("http://", index+1);
			end = page.indexOf("\"", index+10);
			String rssurl = page.substring(index, end);
			//System.out.println(rssurl);
			index = page.indexOf("<meta property=\"og:title\" content=\"");
			end = page.indexOf("\"", index+38);
			String placeName = page.substring(index+35, end);
			index = page.indexOf("<span class=\"street-address\">");
			end = page.indexOf("</span>", index+32);
			String placeStreet = page.substring(index+29, end);
			index = page.indexOf("<span class=\"locality\">");
			end = page.indexOf("</span>", index+25);
			String placeCity = page.substring(index+23, end);
			index = page.indexOf("<span class=\"region\">");
			end = page.indexOf("</span>", index+22);
			String placeState = page.substring(index+21, end);
			index = page.indexOf("<span class=\"postal-code\">");
			end = page.indexOf("</span>", index+28);
			String placeZip = page.substring(index+26, end);
			
			writer.println("CS_" + id);
			
			index = page.indexOf("<h3 class=\"reviews-header ieSucks\">All Reviews</h3>");
			if(index > -1) {
				writer.println(ykey);
				writer.println(rssurl);
				writer.println(placeName);
				writer.println(placeStreet);
				writer.println(placeCity);
				writer.println(placeState);
				writer.println(placeZip);
				
				while((index = page.indexOf("<a href=\"/user_details?userid=", index)) > -1) {
					end = page.indexOf("\"", index+15);
					String userid = page.substring(index+30, end);
					index = page.indexOf("<img class=\"stars_", end);
					if(index > -1) {
						end = page.indexOf("\"", index+15);
						String rating = page.substring(index+18, end).trim();
						index = page.indexOf("<em class=\"dtreviewed smaller\">", end);
						if(index > -1) {
							end = page.indexOf("<span", index);
							String date = page.substring(index+31, end).trim();
							writer.println(userid + "," + id + "," + rating + "," + date);
						}
						index = end;
					}
					else break;
				}
			}
		}
		catch(Exception ex) {
			//ex.printStackTrace();
			//error.println(id + "|" + "|" + name + "|" + street + "|" + city + "|" + zip + "|" + state + "|" + url);
		}
	}
	
	public static String readPage(String url) throws Exception {
//		HttpClient client = new HttpClient();
//		GetMethod method = new GetMethod(url);
//		method.addRequestHeader("Content-Type", "application/json; charset=UTF-8");
//		method.addRequestHeader("accept-charset", "UTF-8");
//		client.executeMethod(method);
//		InputStream is = method.getResponseBodyAsStream();
		
		InputStream is = openInputStream(url);
		StringBuffer buffer = new StringBuffer();
		String line = null;
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		while((line = reader.readLine()) != null) {
			buffer.append(line);
			buffer.append("\n");
		}
		is.close();
//		method.releaseConnection();
		
		return buffer.toString();
	}
	
	public static InputStream openInputStream(String url) throws Exception {
		URL u = new URL(url);
		URLConnection c = u.openConnection();
		c.setRequestProperty("accept-charset", "UTF-8");
		return c.getInputStream();
	}
	
	public static void main(String[] args) throws Exception {


	}
}
