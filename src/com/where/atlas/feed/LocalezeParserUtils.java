package com.where.atlas.feed;

public class LocalezeParserUtils
{
    private String dataPath_;
    private String oldIndexPath_;
    
    
    public LocalezeParserUtils(String dataPath,String oldIndexPath)
    {
        dataPath_=dataPath;
        oldIndexPath_=oldIndexPath;
    }
    public String getDataPath()
    {
        return dataPath_;
    }

    public String getOldIndexPath()
    {
        return oldIndexPath_;
    }
}
