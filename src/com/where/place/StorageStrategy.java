package com.where.place;


public interface StorageStrategy {

	public String create(Place place);
	
	public void update(String placeId, Place place);
	
	public Place delete(String placeId);
	
	public Place findByNativeId(String nativeId);
	
}
