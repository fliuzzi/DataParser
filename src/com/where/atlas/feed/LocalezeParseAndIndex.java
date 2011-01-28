package com.where.atlas.feed;

import java.io.File;
import java.io.FileInputStream;


public class LocalezeParseAndIndex {

	public static void main(String [] args) throws Exception {
	    if(args.length != 3)
	    {
	        System.err.println("need 3 args");
	        return;
        }
		
	    LocalezeParserUtils parserutils = new LocalezeParserUtils(args[0],args[1],args[2]);
		LocalezeCollectAndIndex collector = new LocalezeCollectAndIndex(parserutils);
		
		String baseRecords = args[1] + "/BaseRecords.txt";
		
		new LocalezeParser(parserutils).parse(collector, new FileInputStream(new File(baseRecords)));
	}
}
