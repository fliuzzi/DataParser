package com.where.atlas.feed;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


public class CSParseAndIndex {

        private static String zipPath, indexPath, idMappingPath;
    
        public static void main(String[] args) throws IOException{
      
            if(args.length != 2){System.err.println("need 2 args"); return;}
            if(!args[0].endsWith(".zip")){System.err.println("req args: .zip path, idx to_write path"); return;}


            //store paths in member vars
            zipPath = args[0];       //path of enhanced CS data
            indexPath = args[1];     //where to write index file
            idMappingPath = zipPath;
            int idx = idMappingPath.lastIndexOf("/");
            idMappingPath = idMappingPath.substring(0, idx);
            idMappingPath += "/csid2whereid.txt"; //path of cs2whereid conversion .txt
            
            System.out.println("Paths: \nZip Path: "+zipPath+"\nIndex Path: "+indexPath+"\nidMappingPath: "+idMappingPath);
            
            boolean isAdvertiserFeed = zipPath.indexOf("advertiser") > -1;
            
            //create indexPath (output)
            new File(indexPath).mkdirs();
            
            CSParserUtils csparserutils = new CSParserUtils(zipPath,indexPath,idMappingPath,isAdvertiserFeed);
            
            new CSParser(csparserutils).parse(new CSCollectAndIndex(csparserutils),new FileInputStream(zipPath));
            
            //close locwordWriter
            csparserutils.getLocwordWriter().close();
        }
}
