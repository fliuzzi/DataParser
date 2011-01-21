package com.where.atlas.dao;

import com.where.atlas.Place;

public interface PlaceDao {

	public String create(Place place);
	
	public void update(String placeId, Place place);
	
	public Place delete(String placeId);
	
	public Place findByNativeId(String nativeId);
	
}
