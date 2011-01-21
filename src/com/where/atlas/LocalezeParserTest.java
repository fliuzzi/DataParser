package com.where.atlas;

import java.io.File;
import java.io.FileInputStream;

import com.where.atlas.feed.BasicCollector;
import com.where.atlas.feed.LocalezeParser;

public class LocalezeParserTest {

	public static void main(String [] args) throws Exception {
		
		BasicCollector bc = new BasicCollector();
		//bc.setPlaceDao(new MongoMorphiaPlaceDao());
		new LocalezeParser().parse(bc, new FileInputStream(new File("testdata/localeze_sample.txt")));
	}
}
