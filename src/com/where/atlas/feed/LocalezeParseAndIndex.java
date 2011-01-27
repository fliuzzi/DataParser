package com.where.atlas.feed;

import java.io.File;
import java.io.FileInputStream;


public class LocalezeParseAndIndex {

	public static void main(String [] args) throws Exception {
		
		ConsoleOutputCollector collector = new ConsoleOutputCollector();
		new LocalezeParser().parse(collector, new FileInputStream(new File("/home/fliuzzi/data/localeze/LocalezeCompanyFiles/BaseRecords.txt")));
	}
}
