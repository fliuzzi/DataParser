package com.where.atlas.feed;

import gnu.trove.TLongLongHashMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.lucene.spatial.geohash.GeoHashUtils;

import com.where.place.Address;
import com.where.place.Place;



public class LocalezeParser implements FeedParser {
    
    LocalezeParserUtils parserutils;
    private TLongLongHashMap mappedExistingIDs = new TLongLongHashMap();  
    long[] existingWhereIDs;
    private long startId_ = 100*1000000 +1; //Give the first 100M to cs
    
    public LocalezeParser(LocalezeParserUtils Lparserutils)
    {
        parserutils=Lparserutils;
        mappedExistingIDs = parserutils.getIDMap();
        existingWhereIDs = mappedExistingIDs.getValues();
        
        //find whereid max
        for(int i=0;i<existingWhereIDs.length;i++)
            if(existingWhereIDs[i] > startId_)
                startId_=i;
        System.out.println("Max of existing whereids is: "+startId_);
    }

	public void parse(PlaceCollector collector, InputStream ins) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(ins));
		String record = null;
		while ((record = br.readLine()) != null) {
			try {
				collector.collect(toPlace(record));
			} catch (Exception x) {
				collector.collectBadInput(record, x);
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
        place.setNativeId(bits[0].trim());
    	place.setShortname(bits[3]);
    	place.setName(bits[4]);
        
    	//calc whereid
    	String whereIDtoWrite;
    	long pid = Long.parseLong(place.getNativeId());
        
        if(mappedExistingIDs.containsKey(pid)){
          //this pid already exists, pull whereid from .map
            whereIDtoWrite = new Long(mappedExistingIDs.get(pid)).toString();
        }
        else 
        {
            //increment and get max, use as new whereid
            whereIDtoWrite = new Long(++startId_).toString();
        }
        
        
        place.setWhereId(whereIDtoWrite);
    	
    	
        // address
        Address addr = new Address();
        String street1 = new String();
        for (int i=6; i<10; i++) {
        	street1 += bits[i] + " ";
        }
        street1 = street1.replaceAll("\\s+", " ").trim();
        addr.setAddress1(street1);
        String street2 = new String();
        for (int i=10; i<13; i++) { 
        	street2 += bits[i] + " ";
        }
        street2 = street2.replaceAll("\\s+", " ").trim();
        addr.setAddress2(street2);
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
        place.setPhone(bits[34].trim() + bits[35].trim() + bits[36].trim());

        return place;
	}
}
