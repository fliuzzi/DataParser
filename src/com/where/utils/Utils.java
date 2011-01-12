package com.where.utils;

import java.security.MessageDigest;
import java.util.Scanner;

public class Utils {
	private static String hexits = "0123456789abcdef";
	private static final double EARTH_RADIUS_IN_MILES = 3958.75;
	
	private static MessageDigest MD5;
	
	static {
		try {
			MD5 = MessageDigest.getInstance("MD5");
		}
		catch(Exception ignored) {}
	}
	
	private Utils() {}
	
	public static String hash(String text) {
		byte[] digest = MD5.digest(text.getBytes());
		return toHex(digest);
	}
	
	public static String toHex(byte[] block) {
		StringBuffer buf = new StringBuffer();

		for (int i = 0; i < block.length; ++i) {
			buf.append(hexits.charAt((block[i] >>> 4) & 0xf));
			buf.append(hexits.charAt(block[i] & 0xf));
		}

		return buf + "";
	}
	
	public static void setLineDelimiter(Scanner scanner) {
		scanner.useDelimiter(System.getProperty("line.separator"));
	}
	
	public static double calculateDistanceInMiles(double lat1, double lng1, double lat2, double lng2) {
		double dLat = Math.toRadians(lat2-lat1);
		double dLng = Math.toRadians(lng2-lng1);
		double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
			Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
			Math.sin(dLng/2) * Math.sin(dLng/2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
		double dist = EARTH_RADIUS_IN_MILES * c;
		return dist;
	}
}
