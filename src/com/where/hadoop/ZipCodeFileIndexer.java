package com.where.hadoop;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.json.JSONArray;
import org.json.JSONException;

public class ZipCodeFileIndexer {
	
	protected BufferedReader reader;
	protected String writePath;
	protected AtomicLong entryCount;
	protected AtomicLong lineCount;
	
	public ZipCodeFileIndexer(BufferedReader br, String wPath)
	{
		entryCount= new AtomicLong(0);
		lineCount= new AtomicLong(0);
		reader = br;
		writePath = wPath;
	}
	
	public long getEntryCount()
	{
		return entryCount.get();
	}
	
	public long getLineCount()
	{
		return lineCount.get();
	}
	
	public void start() throws IOException, InterruptedException
	{
		System.out.println("Starting...");
		String line = null;
		
		ExecutorService thePool = Executors.newFixedThreadPool(36);
		
		while((line = reader.readLine()) != null) {
			
			final String line_ = line;
			
			@SuppressWarnings("unused")
			Future<?> fut = thePool.submit(
                    new Runnable(){ public void run(){
			
				try{
					parseEntry(line_);
					lineCount.incrementAndGet();
				}
				catch(Exception e)
				{
					System.err.println("ERROR on line:"+line_+"\n"+e.getMessage());
				}
            }});
		}
		
		thePool.shutdown();
        thePool.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);
		
	}
	
	public void parseEntry(String line) throws IOException, JSONException
	{
		String zipCode = line.substring(0,5);
		JSONArray jarray = new JSONArray(line.substring(line.indexOf("["),line.lastIndexOf("]")+1));
		
		writeFile(zipCode,jarray);
		
	}
	
	public void writeFile(String zip, JSONArray jarray) throws IOException
	{
		System.out.println("Setting up file: "+zip);
		writeFileEntry(new BufferedWriter(new FileWriter(writePath+"/"+zip)),jarray);
	}
	
	public void writeFileEntry(BufferedWriter writer, JSONArray jarray)
	{
		System.out.print("Writing entries...");
		try {
		
			for(int i = 0; i < jarray.length(); i++)
			{
				String toWrite = jarray.get(i).toString();
					writer.write(jarray.get(i).toString());
					writer.newLine();
					entryCount.incrementAndGet();
			}
			
			writer.close();
			
		} catch (IOException e) {
			System.err.println("ERROR WRITING ENTRY"+e.getMessage());
		} catch (JSONException e) {
			System.err.println(e.getMessage());
		}
		System.out.println("DONE");
	}
	
	public static void main(String[] args) throws IOException, InterruptedException {
		if( args.length != 2)
		{
			System.err.println("USAGE: takes mapreduce output (cat all parts together) " +
					"and creates a directory of files:\n " +
					"the name of each file is a zipcode and the file contains line-delimeted jsons of all" +
					"\nCS pois in that zip code area.");
			System.err.println("Arg0: mapreduce total output file\t Arg1: output directory to write thousands of zip files");
			return;
		}
		
		long startMillies = new Date().getTime();
		ZipCodeFileIndexer zcfi = new ZipCodeFileIndexer(new BufferedReader(new FileReader(args[0])),args[1]);
		zcfi.start();
		long endMillies = new Date().getTime();
		
		System.out.println("--------------------------------------------------------------------");
		System.out.println("Finished program in "+((endMillies-startMillies)/1000.0)+" seconds.");
		System.out.println("Wrote "+zcfi.getEntryCount()+ " entries in " + zcfi.getLineCount() + " zipcodes.");
	}
}
