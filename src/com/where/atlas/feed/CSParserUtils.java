package com.where.atlas.feed;

import gnu.trove.TIntIntHashMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class CSParserUtils
{
    private String zipPath_;
    private String indexPath_;
    private String idMappingPath_;
    private boolean isAdvertiserFeed_;
    private PrintWriter locwordWriter_;
    private TIntIntHashMap csId2whereId_;
    
    
    public CSParserUtils(String zipPath,String indexPath, 
                            String idMappingPath,boolean isAdvertiser) throws IOException
    {
        setZipPath(zipPath);
        setIndexPath(indexPath);
        setIdMappingPath(idMappingPath);
        setAdvertiserFeed(isAdvertiser);
        
        //prepare locword file (wont be written until later)
        setLocwordWriter(new PrintWriter(new FileWriter(new File(indexPath+".locwords"))));
        
        //generate id map
        setCsId2whereId(generateIdMap(idMappingPath));

    }
    
    //Maps the cs2whereids.txt files to a TIntIntHashMap using \t delim.  
    //                  K CS_id -> V where_id
    private TIntIntHashMap generateIdMap(String path) throws IOException
    {
        TIntIntHashMap map = new TIntIntHashMap();

        BufferedReader theReader = new BufferedReader(new FileReader(new File(path)));
        String line;
        line = theReader.readLine();
        while( (line = theReader.readLine()) != null)
        {
            line = line.trim();
            String [] ids = line.split("\t");
            int cs = Integer.parseInt(ids[0]);
            int where = Integer.parseInt(ids[1]);
            map.put(cs, where);
        }
        return map;
    }

    public void setIdMappingPath(String idMappingPath)
    {
        idMappingPath_ = idMappingPath;
    }

    public String getIdMappingPath()
    {
        return idMappingPath_;
    }

    public void setIndexPath(String indexPath)
    {
        indexPath_ = indexPath;
    }

    public String getIndexPath()
    {
        return indexPath_;
    }

    public void setZipPath(String zipPath)
    {
        zipPath_ = zipPath;
    }

    public String getZipPath()
    {
        return zipPath_;
    }

    public void setAdvertiserFeed(boolean isAdvertiserFeed)
    {
        isAdvertiserFeed_ = isAdvertiserFeed;
    }

    public boolean isAdvertiserFeed()
    {
        return isAdvertiserFeed_;
    }

    public void setLocwordWriter(PrintWriter locwordWriter)
    {
        locwordWriter_ = locwordWriter;
    }

    public PrintWriter getLocwordWriter()
    {
        return locwordWriter_;
    }

    public void setCsId2whereId(TIntIntHashMap csId2whereId)
    {
        csId2whereId_ = csId2whereId;
    }

    public TIntIntHashMap getCsId2whereId()
    {
        return csId2whereId_;
    }
}
