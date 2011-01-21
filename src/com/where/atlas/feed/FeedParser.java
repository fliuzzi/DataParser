package com.where.atlas.feed;

import java.io.InputStream;

/**
 * Feed parser interface
 * @author ajay - Jan 6, 2011
 */
public interface FeedParser {

	/**
	 * Parse an input stream and send places to a collector
	 * @param collector - collector to use
	 * @param ins - input stream
	 * @throws Exception on any errors
	 */
	public void parse(PlaceCollector collector, InputStream ins) throws Exception;
	
}
