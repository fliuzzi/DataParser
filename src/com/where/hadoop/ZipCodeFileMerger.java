package com.where.hadoop;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

/**
 * 
 * @author fliuzzi
 *
 *			ZipCodeFileMerger.java
 *				ARG0:  zip-bucketed output dir 1
 *				ARG1:  zip-bucketed output dir 2
 *				ARG3:  where to write final zip-bucketed merge
 *
 *				note: we dont simply append to the files in arg0 because 
 *							we want original data to remain untouched.
 *
 *				in the end, we will end up with the final copy of zip code buckets containing 
 *					all YP, LIS, and yelp pois bucketed into files 
 *							according and named by their respective zip codes
 *
 */
public class ZipCodeFileMerger {
	
	File[] firstlist;
	File[] secondlist;
	String bucket1path,bucket2path;
	String outputdirpath;
	
	public ZipCodeFileMerger(File[] firstlist, File[] secondlist, String outputdirpath)
	{
		this.firstlist = firstlist;
		this.secondlist = secondlist;
		this.outputdirpath = outputdirpath;
		
		bucket1path = firstlist[0].getParent();
		bucket2path = secondlist[0].getParent();
	}
	
	public void start()
	{
		System.out.println(bucket1path+"\t"+bucket2path);
		
		for(File file: firstlist)
		{
			try {
				String name = file.getName();
				BufferedReader reader = new BufferedReader(new FileReader(file));
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void merge(BufferedReader filereader, String filename)
	{
		
		
	}

	public static void main(String[] args) {
		if(args.length != 3){ System.err.println("invalid args.  see usage in source."); return;}
		
		File firstdir = new File(args[0]);
		File seconddir = new File(args[1]);
		if(firstdir.isDirectory() && seconddir.isDirectory())
		{
			File[] list1 = firstdir.listFiles();
			File[] list2 = seconddir.listFiles();
			ZipCodeFileMerger zcfm = new ZipCodeFileMerger(list1,list2,args[2]);
			zcfm.start();
		}
		else
		{
			System.err.println("ALL ARGUMENTS MUST BE DIRECTORIES.");
			return;
		}
		
	}

}
