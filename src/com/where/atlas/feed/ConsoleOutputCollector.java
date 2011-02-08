package com.where.atlas.feed;

import com.where.place.Place;

public class ConsoleOutputCollector implements PlaceCollector {

	@Override
	public void collect(Place place) {
		System.out.println(place);
	}

	@Override
	public void collectBadInput(Object input, Exception reason) {
		System.err.println(input + " reason: " + reason.getMessage());
	}

}
