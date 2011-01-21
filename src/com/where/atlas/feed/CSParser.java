package com.where.atlas.feed;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.spatial.geohash.GeoHashUtils;

import com.where.atlas.Address;
import com.where.atlas.Place;
import com.where.data.parsers.citysearch.CitySearchParser;

public class CSParser {
        
        //Setup a logger
        private static final Log logger = LogFactory.getLog(CitySearchParser.class);

        
        /**
         * Parse a CitySearch .zip, storing locations to Place objects and
         *  passing places to a collector
         * @param collector - PlaceCollector child
         * @param ins - InputStream (.zip fileinputstream)
         */
        public void parse(PlaceCollector collector, InputStream ins) throws IOException {
                
                //CSdata comes in .zip
                ZipInputStream zis = new ZipInputStream(ins);
                
                ZipEntry zipEntry = null;
                int count = 0;
                String line = null;
                
                try{
                        while((zipEntry = zis.getNextEntry()) != null) {
                                boolean startLocationsFound = false;
                                
                                BufferedReader reader = new BufferedReader(new InputStreamReader(zis));
                                line = null;
                                while((line = reader.readLine()) != null) {
                                        if(!startLocationsFound) {
                                                int locationsIndex = line.indexOf("<locations ");
                                                if(locationsIndex > -1) {
                                                        line = line.substring(line.indexOf(">", locationsIndex) + 1);
                                                        startLocationsFound = true;
                                                }
                                        }
                                        
                                        //grabs everything in between locations tag and stores into line
                                        int locationsIndex = line.indexOf("</locations>");
                                        if(locationsIndex > -1) {
                                                line = line.substring(0, locationsIndex);
                                                
                                                //pass the String data in between <location> tag
                                                collector.collect(toPlace(line));
                                        }
                                }
                                zis.closeEntry();
                        }
                        zis.close();
                        
                        logger.info("Done. Extracted and Indexed " + count + " CS Listings");
                }
                
        
                catch(Exception ex) {
                        //pass bad location to badInputCollector
                        collector.collectBadInput(line, ex);
                        
                        logger.error("**Error parsing CS data to Place Object!**" + (zipEntry != null ? zipEntry.getName() : ""), ex);
                        throw new IllegalStateException(ex);
                }
        }
        
        
        /**
         * Convert a CS record to a place object
         * @param record - string in between [location] xml tags
         * @return Place object
         */
        public Place toPlace(String record) {
                // new place
                Place place = new Place();
                place.setSource(Place.Source.CS);
                
                //TODO: Places!
                /*
                place.setNativeId(Place.Source.CS + ":" + bits[0].trim());
                place.setShortname(bits[3]);
                place.setName(bits[3]);
                place.setAddress(Address object);
                */
                
                return place;
        }
}
