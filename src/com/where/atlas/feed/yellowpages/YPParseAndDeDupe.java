package com.where.atlas.feed.yellowpages;

import java.io.File;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.where.utils.XMLfixer;




public class YPParseAndDeDupe
{
    
    public static void main(String[] args)
    {
        if(args.length != 4)
        {
            System.err.println("USAGE: program will go through a directory of yellow pages raw data zips");
            System.err.println("ARG1: directory of raw data .zips");
            System.err.println("ARG2: Output flatfile target path");
            System.err.println("ARG3: Parseing type (int): 1)<listing>  2)<details>");
            System.err.println("ARG4: Thread count (int)");
            return;
        }
        
        try{
        	
            File zipDirectory = new File(args[0]);
            final File[] zipFiles = zipDirectory.listFiles();
            
            if(zipFiles == null){
                System.err.println("Directory Error");
                return;
            }
            else{
                final YPRawDataParser parser = new YPRawDataParser(new YPParserUtils(args[1],Integer.parseInt(args[2])));
                ExecutorService thePool = Executors.newFixedThreadPool(Integer.parseInt(args[3]));
                
                final YPJSONCollector collector = new YPJSONCollector();
                
                for(int x = 0; x < zipFiles.length; x++)
                {
                    final int lcv = x;
                	@SuppressWarnings("unused")
					Future<?> fut = thePool.submit(
                            new Runnable(){ public void run(){
                            	try{
                            	final ZipFile zipFile = new ZipFile(zipFiles[lcv]);
                            	
                            	for (Enumeration<? extends ZipEntry> e = zipFile.entries(); e.hasMoreElements();) {
                            		
                            		final ZipEntry entry = (ZipEntry) e.nextElement();
                        
                        
                                	System.out.println("Zip File #"+(lcv+1)+" : " + zipFiles[lcv].getName());
                                	
                                    parser.parse(collector, XMLfixer.repairXML(zipFile.getInputStream(entry)));
                                    
									zipFile.close();
                                    System.out.println("\nFinished zip" + zipFile.getName());
                            	}
                            }
                            catch(Exception e)
                            {}
                            
                            }});
                        
                    }
                    
                    thePool.shutdown();
                    thePool.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);
                    
                    parser.closeWriter();
                    System.out.println("Done!");
                }
                
                
                
            }
        catch(Throwable e){
            e.printStackTrace();
            }
        
        

    }

}
