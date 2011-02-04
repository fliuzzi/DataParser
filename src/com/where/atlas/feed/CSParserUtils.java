package com.where.atlas.feed;

import gnu.trove.TIntIntHashMap;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import com.where.utils.CSIDHashMap;

public class CSParserUtils
{
    private String zipPath_;
    private String indexPath_;
    private String idMappingPath_;
    private boolean isAdvertiserFeed_;
    private PrintWriter locwordWriter_;
    private TIntIntHashMap csId2whereId_;
    private CSListingIndexer indexer;
    private CSListingIndexer spAltCategoryIndexer;
    
    
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
        
        
        
        if(isAdvertiser) {
            new File(indexPath + "/cat_6_all_alt").mkdirs();
            spAltCategoryIndexer = CSListingIndexer.newInstance(indexPath + "/cat_6_all_alt");
        }
        
        indexer = CSListingIndexer.newInstance(indexPath);
                
        //loads the csId2WhereId map into the indexer instance
        indexer.setcs2whereMapping(getCsId2whereId());
    }
    
    public CSListingIndexer getAdvertiserIndexer()
    {
        if(isAdvertiserFeed_) return spAltCategoryIndexer;
        
        return null;
    }
    
    public CSListingIndexer getIndexer(){
        return indexer;
    }
    
    
    //Maps the cs2whereids.txt files to a TIntIntHashMap using \t delim.  
    //                  K CS_id -> V where_id
    private TIntIntHashMap generateIdMap(String path) throws IOException
    {
        
        CSIDHashMap hashmapmaker = new CSIDHashMap(getIdMappingPath());
        try{
            hashmapmaker.readDocuments();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        
        return hashmapmaker.getIdMap();        
    }

    private void setIdMappingPath(String idMappingPath)
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
