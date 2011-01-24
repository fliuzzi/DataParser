package com.where.atlas.feed;

import java.io.IOException;
import java.io.Writer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.where.atlas.Place;
import com.where.atlas.dao.PlaceDao;

public class BasicCollector implements PlaceCollector {

	private Log LOG = LogFactory.getLog(BasicCollector.class);
	
	private PlaceDao placeDao;
	private Writer writer;
	
	public void setPlaceDao(PlaceDao placeDao) {
		this.placeDao = placeDao;
	}
	
	public void setWriter(Writer writer) { 
		this.writer = writer; 
	}

	@Override
	public void collect(Place place) {
		Place exists = placeDao.findByNativeId(place.getNativeId());
		if (exists != null) { 
			// TODO reconcile the two places or defer
			//LOG.warn("place exists: " + TextUtil.toString(exists));
			return;
		}
		placeDao.create(place);
	}


	@Override
	public void collectBadInput(Object input, Exception reason) {
		try {
			writer.write(input.toString() + "\n");
			writer.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
