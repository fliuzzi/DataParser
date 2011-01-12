package com.where.utils;

import com.where.commons.feed.citysearch.CSListing;
import com.where.commons.feed.citysearch.search.query.Profile;
import com.where.places.lists.MemcacheUtil;

public final class CSListingUtil {
	private CSListingUtil() {}
	
	public static CSListing loadProfile(Profile profile, String placeid) {
		if(profile == null || placeid == null) return null;
		if(!profile.hasCache()) {
			profile.setCache(MemcacheUtil.getCache());
		}
		return profile.loadProfile(placeid);
	}
	
	public static CSListing loadProfile(Profile profile, String id, String type) {
		if(profile == null || id == null || type == null) return null;
		if(!profile.hasCache()) {
			profile.setCache(MemcacheUtil.getCache());
		}
		return profile.loadProfile(id, type);
	}
}
