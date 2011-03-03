package com.where.atlas.feed.localeze;

import org.apache.lucene.document.Document;

import com.where.commons.util.StringUtil;
import com.where.place.Address;
import com.where.place.CSPlace;
import com.where.place.Place;

public class LocalezeUtil {
	public static final double DEFAULT_DISTANCE 			= 0;
	
	public static CSPlace generateListing(Document ltDoc) {
		return generateListing(ltDoc, -1);
	}
	
	public static CSPlace generateListing(Document ltDoc, double distance) {
		if(ltDoc == null) return null;
		CSPlace LEListing = new CSPlace();
        LEListing.setSource(Place.Source.LOCALEZE);

        Address loc = new Address();
        if(!StringUtil.isEmpty(ltDoc.get("companyname")))
        {
            LEListing.setName(ltDoc.get("companyname"));
        }
        if(!StringUtil.isEmpty(ltDoc.get("address")))
        {
            loc.setAddress1(ltDoc.get("address"));	                        
        }
        if(!StringUtil.isEmpty(ltDoc.get("city")))
        {
            loc.setCity(ltDoc.get("city"));
        }
        if(!StringUtil.isEmpty(ltDoc.get("state")))
        {
            loc.setState(ltDoc.get("state"));
        }
        if(!StringUtil.isEmpty(ltDoc.get("latitude_range")))
        {
            loc.setLat(Double.parseDouble(ltDoc.get("latitude_range")));
        }
        if(!StringUtil.isEmpty(ltDoc.get("longitude_range")))
        {
            loc.setLng(Double.parseDouble(ltDoc.get("longitude_range")));
        }
        if(!StringUtil.isEmpty(ltDoc.get("zip")))
        {
            loc.setZip(ltDoc.get("zip"));
        }
        if(!StringUtil.isEmpty(ltDoc.get("phone")))
        {
            LEListing.setPhone(ltDoc.get("phone"));
        }
        if(!StringUtil.isEmpty(ltDoc.get("whereid")))
        {
            LEListing.setWhereId(ltDoc.get("whereid"));
        }
        
        LEListing.setAddress(loc);
        if(distance >= 0)
        {
            LEListing.setDistance(distance);	                        
        } else {
        	LEListing.setDistance(DEFAULT_DISTANCE);
        }
        return LEListing;
	}
}
