package com.where.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import gnu.trove.TLongLongHashMap;

import org.apache.lucene.document.Document;

public class WhereIDMapper extends IndexProcessor
{
    TLongLongHashMap map_ = new TLongLongHashMap();
    private String outputFile_;

    public WhereIDMapper(String path, String output)
    {
        super(path);
        outputFile_ = output;
    }

    public void processDocument(Document doc) throws Exception{
        //grab pid and whereids from doc
        String pids = doc.get("pid");
        String whereids = doc.get("whereid");

        //convert to long arrays
        long pid = Long.parseLong(pids);
        long whereid = Long.parseLong(whereids);
        
        //add to map
        map_.put(pid, whereid);
    }

    public void finishProcessing() throws Exception{
        serialize(map_,outputFile_);
    }

    public static void serialize(TLongLongHashMap IDMap, String fileName) throws IOException
    {
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName));
        oos.writeObject(IDMap);
        oos.close();
    }

    
    public static void main(String args[]) throws Exception{
        if(args.length != 2) return;
        
        String inputfile = args[0];
        String outputfile = args[1];
        WhereIDMapper mapper = new WhereIDMapper(inputfile,outputfile);
        mapper.readDocuments();
    }
}
