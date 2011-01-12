package com.where.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.json.JSONObject;

public class HTTPUtils {
	private HTTPUtils() {}
	
	public static JSONObject readJSONFromPage(String url, boolean shouldCompress) throws Exception {
		return new JSONObject(readPage(url, shouldCompress));
	}
	
	public static JSONObject readJSONFromPage(String url) throws Exception {
		return new JSONObject(readPage(url, true));
	}
	
	public static URLConnection openInputStream(String url, boolean shouldCompress) throws Exception {
		URL u = new URL(url);
		URLConnection c = u.openConnection();
		c.setConnectTimeout(7000);
		c.setReadTimeout(15000);
		c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
		c.setRequestProperty("accept-charset", "UTF-8");
		if(shouldCompress) {
			c.setRequestProperty("Accept-Encoding", "gzip,deflate");
		}
		return c;
	}
	
	public static String readPageAsChromeBrowser(String url) throws Exception {
		HttpClient client = new HttpClient();
		GetMethod method = new GetMethod(url);
		method.setFollowRedirects(true);
		method.addRequestHeader("Content-Type", "application/json; charset=UTF-8");
		method.addRequestHeader("accept-charset", "UTF-8");
		method.addRequestHeader("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US) AppleWebKit/525.13 (KHTML, like Gecko) Chrome/0.A.B.C Safari/525.13");
		client.executeMethod(method);
		InputStream is = method.getResponseBodyAsStream();
		Scanner scanner = new Scanner(is);
		scanner.useDelimiter(System.getProperty("line.separator"));
		StringBuffer buffer = new StringBuffer();
		while(scanner.hasNext()) {
			buffer.append(scanner.next());
			buffer.append("\n");
		}
		scanner.close();
		method.releaseConnection();
		
		return buffer.toString();
	}	
	
	public static String readPage(String url) throws Exception {
		return readPage(url, true);
	}
	
	public static String readPage(String url, boolean shouldCompress) throws Exception {
		URLConnection c = openInputStream(url, shouldCompress);
		boolean isCompressed = c.getHeaderField("content-encoding") != null && c.getHeaderField("content-encoding").toLowerCase().equals("gzip");
		InputStream is = c.getInputStream();
		return readResponse(is, isCompressed);
	}

	private static String readResponse(InputStream stream, boolean isCompressed) throws IOException {
		InputStream is = isCompressed ? new GZIPInputStream(stream) : stream;
		BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
		String line = null;
		StringBuffer buffer = new StringBuffer();
		while((line = reader.readLine()) != null) {
			buffer.append(line);
			//buffer.append("\n");
		}
		reader.close();
		String responseText = buffer.toString().trim();
		return responseText;
	}	
	
	public static String post(String url, Map<String, String> params) throws IOException {
		StringBuffer data = new StringBuffer();
		for(Map.Entry<String, String> entry:params.entrySet()) {
			data.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
			data.append("=");
			data.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
			data.append("&");
		}
		
		String payload = data.toString();
		//remove &
		payload = payload.substring(0, payload.length()-1);

	    URLConnection conn = new URL(url).openConnection();
	    conn.setDoOutput(true);
	    OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
	    wr.write(payload);
	    wr.flush();

	    BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	    String line;
	    StringBuffer buffer = new StringBuffer();
	    while ((line = rd.readLine()) != null) {
	        buffer.append(line);
	        buffer.append("\n");
	    }
	    wr.close();
	    rd.close();
	    
	    return buffer.toString();
	}
}
