package com.where.atlas.feed;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;

import org.apache.lucene.spatial.geohash.GeoHashUtils;

import com.where.atlas.Address;
import com.where.atlas.Place;

/**
 * Localeze feed parser 
 * @author ajay - Jan 5, 2011
 */
public class LocalezeParser implements FeedParser {

	public void parse(PlaceCollector collector, InputStream ins) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(ins));
		String record = null;
		long count = 0;
		while ((record = br.readLine()) != null) { 
			try { 
				collector.collect(toPlace(record));
			} catch (Exception x) { 
				collector.collectBadInput(record, x);
			}
			if (count++ % 1000 == 0) { 
				System.out.println(new Date() + " -- " + count);
			}
		}
		
	}
	
	/**
	 * Convert a localeze record to a place object
	 * @param record - input localeze record
	 * @return place
	 */
    public Place toPlace(String record) {
        String [] bits = record.split("\\|");
        if (bits == null) { 
        	throw new RuntimeException("bits is null");
        } 
        if (bits.length < 47) {
        	throw new RuntimeException("bits.length (" + bits.length + ") < 47");
        }
        // new place
        Place place = new Place();
        place.setSource(Place.Source.LOCALEZE);
        place.setNativeId(Place.Source.LOCALEZE + ":" + bits[0].trim());
        if (bits[3].length() < bits[4].length()) { 
        	place.setShortname(bits[3]);
        	place.setName(bits[4]);
        } else { 
        	place.setShortname(bits[4]);
        	place.setName(bits[3]);
        }
        

        // address
        Address addr = new Address();
        String street1 = new String();
        for (int i=6; i<10; i++) { 
        	street1 += bits[i] + " ";
        }
        street1 = street1.replaceAll("\\s+", " ").trim();
        addr.setStreet1(street1);
        String street2 = new String();
        for (int i=10; i<13; i++) { 
        	street2 += bits[i] + " ";
        }
        street2 = street2.replaceAll("\\s+", " ").trim();
        addr.setStreet2(street2);
        addr.setNeighborhood(bits[13].trim());
        addr.setCity(bits[14].trim());
        addr.setState(bits[15].trim());
        
        
        boolean stringIsBlank = (bits[17].isEmpty()) || (bits[17]==null);
        addr.setZip(stringIsBlank ? bits[16].trim() : bits[16].trim() + "-" + bits[17].trim());
        
        place.setAddress(addr);
        
        // geo
        double lat = Double.parseDouble(bits[45].trim());
        double lng = Double.parseDouble(bits[46].trim());
        place.setLatlng(new double [] { lat, lng});
        String geohash = GeoHashUtils.encode(lat, lng);
        place.setGeohash(geohash);
        
        // phone
        place.setPhone(bits[34].trim() + "-" + bits[35].trim() + "-" + bits[36].trim());

        return place;
	}
}
