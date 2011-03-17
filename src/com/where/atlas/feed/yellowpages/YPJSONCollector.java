package com.where.atlas.feed.yellowpages;

import com.where.atlas.feed.PlaceCollector;
import com.where.place.Place;

public class YPJSONCollector implements PlaceCollector{

	@Override
	public void collect(Place place) {
		//System.out.println(place);
	}

	@Override
	public void collectBadInput(Object input, Exception reason) {
				
	}

}
