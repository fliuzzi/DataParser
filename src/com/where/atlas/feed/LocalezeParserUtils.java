package com.where.atlas.feed;

import gnu.trove.TLongLongHashMap;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;

import com.Ostermiller.util.StringTokenizer;
import com.where.commons.feed.citysearch.search.Analyzer;
import com.where.data.localeze.Category;

public class LocalezeParserUtils
{
    private String staticFilesPath_;
    private String companyFilesPath_;
    private String indexPath_;
    
    private static final String PID = "pid";
    private static final String DATA = "data";
    public static final String GEOHASH = "geohash";
    
    private TLongLongHashMap mappedExistingIDs;
    private static Map<Long, Category> categories = new HashMap<Long, Category>();
    private static Map<Long, Category> subcategories = new HashMap<Long, Category>();
    private static Map<Long, Category> subsubcategories = new HashMap<Long, Category>();
    private static Map<Long, String> companyCategories = new HashMap<Long, String>();

    
    public LocalezeParserUtils(String staticFilesPath,String companyFilesPath,
                                            String indexPath,String serializedMapPath)
    {
        staticFilesPath_=staticFilesPath;
        companyFilesPath_= companyFilesPath;
        indexPath_=indexPath;
        
        
        try{
            System.out.println("Populating category maps.");
            populateCategoryMap(staticFilesPath_); 
            populateSubcategoryMap(staticFilesPath_);
            populateSubSubcategoryMap(staticFilesPath_);
            System.out.println("Finished populating category maps.");
        }
        catch(Exception e){
            System.err.println("Error populating category maps!");
            e.printStackTrace();
            }
        
        try{
            System.out.println("Populating company categories.");
            populateCompanyCategories(companyFilesPath_, indexPath_);
            System.out.println("Finished populating company categories.");
        }
        catch(Exception e){
            System.err.println("Error populating company category maps!");
            e.printStackTrace();
        }
        
        try{
        //deserialize the existing whereid map
        mappedExistingIDs = deSerializeIDMap(serializedMapPath);
        }
        catch(Exception e){
            System.err.println("Error deserializing ID Map!");
            e.printStackTrace();
        }
    }
    
    public TLongLongHashMap getIDMap()
    {
        return mappedExistingIDs;
    }
    
    private TLongLongHashMap deSerializeIDMap(String fileName) throws IOException, ClassNotFoundException
    {
        ObjectInputStream ois = null;
        try
        {
            FileInputStream fis = new FileInputStream(fileName);
            BufferedInputStream buf = new BufferedInputStream(fis);
            ois = new ObjectInputStream(buf);
            return (TLongLongHashMap)ois.readObject();
        }
        finally
        {
            if(ois != null)
                ois.close();            
        }
    }
    
    //Parses Localeze File: CompanyHeadings.txt
    //Outputs to: HashMap companyCategories
    private static void populateCompanyCategories(String folder, String indexFolder) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(folder + "/CompanyHeadings.txt"));
        String line = null;
        int cnt = 0;
        while((line = reader.readLine()) != null) {
            if(++cnt % 1000 == 0){System.out.print("+");}
            if(cnt % 100000 == 0){System.out.println();}
            if(line.trim().length() > 0) {
                //StringTokenizer tokenizer = new StringTokenizer(line, "|");
                String [] splitter = line.split("\\|");
                Long pid = Long.valueOf(splitter[0].trim());
                Long categoryid = Long.valueOf(splitter[1].trim());

                
                Category category = subsubcategories.get(categoryid);
                if(category == null) category = subcategories.get(categoryid);
                if(category == null) category = categories.get(categoryid);
                if(category == null) throw new IllegalStateException("No Category can be found for PID " + pid + "line: " + line);
                
                String cats = companyCategories.get(pid);
                if(cats == null) cats = category.getName();
                else cats+= "|" + category.getName();
                
                companyCategories.put(pid, cats);
            }
        }
        reader.close();
        
        System.out.println("Finished populating map, indexing to /cmpcat..");
        writeToIndex(companyCategories, indexFolder + "/cmpcat");
        System.out.println("Done.");
        
        companyCategories.clear();
        companyCategories = null;
    }   

    private static void writeToIndex(Map<Long, String> map, String indexFolder) throws IOException {
        IndexWriter writer = newIndexWriter(indexFolder);
        for(Map.Entry<Long, String> e:map.entrySet()) {
            writeDocument(writer, e.getKey(), e.getValue());
        }
        writer.optimize();
        writer.close();
    }
    
    private static void writeDocument(IndexWriter writer, Long pid, String data) throws IOException {
        Document d = new Document();
        d.add(new Field(PID, String.valueOf(pid), Field.Store.YES, Field.Index.NOT_ANALYZED));
        d.add(new Field(DATA, data, Field.Store.YES, Field.Index.NOT_ANALYZED));
        writer.addDocument(d);
    }
    
    public static IndexWriter newIndexWriter(String indexFolder) throws IOException {
        File index = new File(indexFolder);
        index.mkdir();
        
        Directory directory = new NIOFSDirectory(index);
        IndexWriter writer = new IndexWriter(directory, new Analyzer(), true, MaxFieldLength.UNLIMITED);
        writer.setMergeFactor(100000);
        writer.setMaxMergeDocs(Integer.MAX_VALUE);        
        return writer;
    } 

    
    //Parses Localeze File: Categories.txt
    private static void populateCategoryMap(String folder) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(folder + "/Categories.txt"));
        String line = null;
        while((line = reader.readLine()) != null) {
            if(line.trim().length() > 0) {
                StringTokenizer tokenizer = new StringTokenizer(line, "|");
                Long id = Long.valueOf(tokenizer.nextToken().trim());
                Category category = new Category(id, tokenizer.nextToken().trim());
                categories.put(id, category);
            }
        }
        reader.close();
    }

    //Parses Localeze File: condensedHeadingDetail.txt
    private static void populateSubcategoryMap(String folder) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(folder + "/CondensedHeadingDetail.txt"));
        String line = null;
        while((line = reader.readLine()) != null) {
            if(line.trim().length() > 0) {
                StringTokenizer tokenizer = new StringTokenizer(line, "|");
                Long id = Long.valueOf(tokenizer.nextToken().trim());
                Category category = new Category(id, tokenizer.nextToken().trim());
                
                //if main category map doesnt have this id...
                if(!categories.containsKey(category.getId())) {
                    Category parent = categories.get(category.stemKey());
                    category.setParent(parent);
                    subcategories.put(id, category);
                }
            }
        }
        reader.close();
    }
    
    //Parses Localeze File: NormalizedHeadingDetail.txt
    private static void populateSubSubcategoryMap(String folder) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(folder + "/NormalizedHeadingDetail.txt"));
        String line = null;
        while((line = reader.readLine()) != null) {
            if(line.trim().length() > 0) {
                StringTokenizer tokenizer = new StringTokenizer(line, "|");
                Long id = Long.valueOf(tokenizer.nextToken().trim());
                Category category = new Category(id, tokenizer.nextToken().trim());
                if(!categories.containsKey(category.getId()) && !subcategories.containsKey(category.getId())) {
                    Category parent = subcategories.get(category.subStemKey());
                    category.setParent(parent);
                    subsubcategories.put(id, category);
                }
            }
        }
        reader.close();
    }
    
    

    public String getIndexPath()
    {
        return indexPath_;
    }
    
    public String getCompanyFilesPath()
    {
        return companyFilesPath_;
    }
    
    public String getStaticFilesPath()
    {
        return staticFilesPath_;
    }
}
