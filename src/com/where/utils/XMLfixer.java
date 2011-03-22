package com.where.utils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public class XMLfixer {

	private static StringBuffer convertToString(InputStream ins) throws IOException
	{
		BufferedInputStream bis = new BufferedInputStream(ins);
	    ByteArrayOutputStream buf = new ByteArrayOutputStream();
	    int result = bis.read();
	    while(result != -1) {
	      byte b = (byte)result;
	      buf.write(b);
	      result = bis.read();
	    }        
	    return new StringBuffer(buf.toString());	
	}
	
	private static InputStream convertToInputStream(String str) throws UnsupportedEncodingException
	{
			return new ByteArrayInputStream(str.getBytes("US-ASCII"));
	}
	
	@SuppressWarnings("unused")
	private static String replaceYPErrors(String buf)
	{
		StringBuilder builder = new StringBuilder(buf);
//		System.out.println("Replacing <product_services>");
//		while(builder.indexOf("<products_services>") > -1)
//		{
//			builder.delete(builder.indexOf("<products_services>"), builder.indexOf("</products_services>")+20);
//		}
		System.out.println("Replacing <hours>");
		while(builder.indexOf("<hours>") > -1)
		{
			builder.delete(builder.indexOf("<hours>"), builder.indexOf("</hours>")+8);
		}
		
		return builder.toString();
	}
	
	
	public static InputStream repairXML(InputStream ins)
	{
		String buf;
		try {
			buf = convertToString(ins).toString();
			buf = buf.replace("<general_info>","<general_info><![CDATA[").replace("</general_info>", "]]></general_info>");
			buf = buf.replace("<hours>","<hours><![CDATA[").replace("</hours>", "]]></hours>");
			buf = buf.replace("<products_services>","<products_services><![CDATA[").replace("</products_services>", "]]></products_services>");

			return convertToInputStream(buf.replace("&", ""));
		}
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		catch (IOException e1) {
			e1.printStackTrace();
		}
		return null;
		
	}

}
