package com.where.atlas.feed;

import com.where.atlas.Place;

/**
 * Collector interface. To be used by parsers etc.
 * @author ajay - Jan 6, 2011
 */
public interface PlaceCollector {

	/**
	 * Collect a new place
	 * @param place - place to collect
	 */
	public void collect(Place place);

	/**
	 * Log bad input. 
	 * @param input - bad input 
	 * @param reason - exception that caused it
	 */
	public void collectBadInput(Object input, Exception reason);
}
