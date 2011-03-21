package com.where.atlas.feed.yellowpages;

import org.json.JSONArray;

import com.where.atlas.feed.PlaceCollector;
import com.where.place.Place;
import com.where.place.YPPlace;

public class YPJSONCollector implements PlaceCollector{
	
	private JSONArray places;
	
	public YPJSONCollector()
	{
		places = new JSONArray();
	}

	@Override
	public void collect(Place place) {
		YPPlace ypplace = (YPPlace)place;
		places.put(ypplace.toJSON());
	}

	@Override
	public void collectBadInput(Object input, Exception reason) {
				
	}
	
	public JSONArray getJSON()
	{
		return places;
	}

}
