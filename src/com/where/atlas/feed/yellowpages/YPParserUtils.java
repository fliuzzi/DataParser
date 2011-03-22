package com.where.atlas.feed.yellowpages;



import java.io.File;
import java.io.FileFilter;
import java.io.IOException;


public class YPParserUtils
{
    
    private String targetPath_;
    private int parserType;
    
    
    public static class InnerRating
    {
        public long userId_;
        public int rating_;
    }
    
    public YPParserUtils(String targetFile, int parserChoice) throws IOException
    {
        targetPath_ = targetFile;
        parserType = parserChoice;
    }
    
    public int getParserType()
    {
    	return parserType;
    }
    
    // returns a File array of all files in path 'dirName' that end with .rating
    public static File [] getFiles(String dirName)
    {
        File dir = new File(dirName);

        return dir.listFiles(
                new FileFilter() {
                    public boolean accept(File file) {
                        return file.isFile() && file.getName().endsWith(".rating");
                    }
                });
    }

    public String getTargetPath()
    {
        return targetPath_;
    }
    
}
