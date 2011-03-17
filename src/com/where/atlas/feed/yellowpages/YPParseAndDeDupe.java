package com.where.atlas.feed.yellowpages;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;




public class YPParseAndDeDupe
{
    
    public static class WriterWorker implements Runnable{
        
        String toWrite;
        BufferedWriter writer;
        
        public WriterWorker(String s, BufferedWriter w)
        {
            toWrite = s;
            writer = w;
        }
        
        
        public void run(){
            
            try{
                writer.write(toWrite);
                writer.newLine();
            }
            
            catch(Throwable t){
                System.err.println("Err writing");
            }
        }
    }
    
    
    public static class WriterThread{
        
        private BufferedWriter writer;
        ExecutorService myPool;
        
        public WriterThread(BufferedWriter w){
            writer = w;
            
            //keep single threaded!
            myPool = Executors.newFixedThreadPool(1);
        }
        
        public void addTask(String t)
        {
            myPool.execute(new WriterWorker(t,writer));
        }
        
        public void finish() throws IOException,InterruptedException
        {
            writer.close();
            myPool.shutdown();
            myPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);
        }
    
    }
    
    
    public static void main(String[] args)
    {
        if(args.length != 3)
        {
            System.err.println("USAGE: program will go through a directory of yelp raw data zips");
            System.err.println("ARG1: directory of raw data .zips");
            System.err.println("ARG2: CS 'lis' index to de-dupe against");
            System.err.println("ARG3: Output flatfile target path");
            return;
        }
        try{
            
            File zipDirectory = new File(args[0]);
            File[] zipFiles = zipDirectory.listFiles();
            
            if(zipFiles == null){
                System.err.println("Directory Error");
                return;
            }
            else{
                final YPRawDataParser parser = new YPRawDataParser(new YPParserUtils(args[1],args[2]));
                
                
                
                for(int x = 0; x < zipFiles.length; x++)
                {
                    ExecutorService thePool = Executors.newFixedThreadPool(5);
                    System.out.println("Zip File #"+(x+1)+" : " + zipFiles[x].getName());
                    final ZipFile zipFile = new ZipFile(zipFiles[x]);
                                        
                    for (Enumeration<? extends ZipEntry> e = zipFile.entries(); e.hasMoreElements();) {
                        
                        final ZipEntry entry = (ZipEntry) e.nextElement();
                        
                        
                        @SuppressWarnings("unused")
                        Future<?> fut = thePool.submit(
                                new Runnable(){ public void run(){
                                    try{
                                        parser.parse(
                                                new YPJSONCollector(), zipFile.getInputStream(entry));
                                        System.out.println("Finished XML File: " + entry.getName());
                                    }
                                    catch(IOException io){
                                        throw(new RuntimeException(io));
                                    }}});
                        
                    }
                    
                    thePool.shutdown();
                    thePool.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);
                    System.out.println("\nFinished zip" + zipFile.getName());
                    
                    
                    
                    parser.resetCounter();
                    zipFile.close();
                }
                
                
                parser.closeWriter();
            }
        }
        catch(Throwable e){
            e.printStackTrace();
            }
        
        

    }

}
