package com.where.data.parsers.localeze;

import gnu.trove.TLongLongHashMap;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.spatial.geohash.GeoHashUtils;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;

import com.Ostermiller.util.StringTokenizer;
import com.where.commons.feed.citysearch.search.Analyzer;
import com.where.commons.util.StringUtil;

public class LocalezeParser {
    private static final String PID = "pid";
	private static final String DATA = "data";
	public static final String GEOHASH = "geohash";
	private static Map<Long, Category> categories = new HashMap<Long, Category>();
	private static Map<Long, Category> subcategories = new HashMap<Long, Category>();
	private static Map<Long, Category> subsubcategories = new HashMap<Long, Category>();
	private static Map<Long, String> companyCategories = new HashMap<Long, String>();
	
	private static TLongLongHashMap mappedExistingIDs = new TLongLongHashMap();

	//TODO: need to change this once we have these stored
	private static int startId_ = 100*1000000 +1; //Give the first 100M to cs

	//Parses Localeze File: Categories.txt
    // each line is stored into one Category object with an idNum and a String Category Name
	//     and finally stored into a hashmap
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
	//Acts as populateCategoryMap + links Category to parent category (by stemkey)
	//stores category into HashMap subcategories
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
	//Acts just like populateSubCategoryMap
	//stores category into HashMap subsubcategories
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

	//Parses Localeze File: CompanyHeadings.txt
	//Outputs to: HashMap companyCategories
	private static void populateCompanyCategories(String folder, String indexFolder) throws Exception {
		BufferedReader reader = new BufferedReader(new FileReader(folder + "/CompanyHeadings.txt"));
		String line = null;
		System.out.println("about to start populating company categories");
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
		
		writeToIndex(companyCategories, indexFolder + "/cmpcat");
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
	
    private static IndexWriter newIndexWriter(String indexFolder) throws IOException {
		File index = new File(indexFolder);
		index.mkdir();
		
        Directory directory = new NIOFSDirectory(index);
        IndexWriter writer = new IndexWriter(directory, new Analyzer(), true, MaxFieldLength.UNLIMITED);
        writer.setMergeFactor(100000);
        writer.setMaxMergeDocs(Integer.MAX_VALUE);        
        return writer;
	} 
	
    private static void writeDoc(String line, BufferedWriter err, IndexWriter writer, IndexSearcher searcher, AtomicLong MAXwhereid) throws IOException
    {
        String [] pieces = line.split("\\|");
        Document doc = new Document();
        String rawPid = pieces[0];
        int len = pieces.length;
        if(len < 47)
        {
            err.newLine();                
            err.write("# " + pieces.length + " short");
            err.write(line);
            err.write("#/short");
            err.newLine();                
            return;            
        }
        double lat = -1;
        double lng = -1;
        try
        {
            lat = Double.parseDouble(pieces[45].trim());
            lng = Double.parseDouble(pieces[46].trim());
        }
        catch(Exception e)
        {
            err.newLine();                
            err.write("#bad  ");
            err.write(line);
            err.write(" #/bad");
            err.newLine();                
            return;
        }

        
        
        
        
        long pid = Long.parseLong(rawPid);
        
        doc.add(new Field("pid", rawPid, Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field("rawname", pieces[3].toLowerCase().trim(), Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field("companyname", pieces[4].toLowerCase().trim(), Field.Store.YES, Field.Index.ANALYZED_NO_NORMS));
        
        
        String whereIDtoWrite;
        
        if(mappedExistingIDs.containsKey(pid)){
          //this pid already exists, pull whereid from .map
            whereIDtoWrite = new Long(mappedExistingIDs.get(pid)).toString();
        }
        else 
        {
            //increment and get max, use as new whereid
            whereIDtoWrite = new Long(MAXwhereid.incrementAndGet()).toString();
        }


        
        doc.add(new Field("whereid", whereIDtoWrite, Field.Store.YES, Field.Index.NOT_ANALYZED));
        
        
        
        
        
        String streetAddress = pieces[6] + " "; 
        String tmp = pieces[7].trim(); //predirection
        if(!StringUtil.isEmpty(tmp)){ streetAddress += (tmp + " ");}
        tmp = pieces[8].trim(); //streetname
        if(!StringUtil.isEmpty(tmp)){ streetAddress += (tmp + " ");}

        tmp = pieces[9].trim(); //streettype
        if(!StringUtil.isEmpty(tmp)){ streetAddress += (tmp + " ");}

        tmp = pieces[10].trim(); //postdirectional
        if(!StringUtil.isEmpty(tmp)){ streetAddress += (tmp + " ");}

        tmp = pieces[11].trim(); //apartment type
        if(!StringUtil.isEmpty(tmp)){ streetAddress += (tmp + " ");}

        tmp = pieces[12].trim(); //apartment number
        if(!StringUtil.isEmpty(tmp)){ streetAddress += (tmp + " ");}

        doc.add(new Field("address", streetAddress, Field.Store.YES, Field.Index.ANALYZED));
        
        tmp = pieces[14].trim(); //city
        if(!StringUtil.isEmpty(tmp))
        { 
            doc.add(new Field("city", tmp, Field.Store.YES, Field.Index.ANALYZED));
        }

        tmp = pieces[15].trim(); //state
        if(!StringUtil.isEmpty(tmp))
        { 
            doc.add(new Field("state", tmp, Field.Store.YES, Field.Index.ANALYZED));
        }

        tmp = pieces[16].trim(); //zip
        if(!StringUtil.isEmpty(tmp))
        { 
            doc.add(new Field("zip", tmp, Field.Store.YES, Field.Index.ANALYZED));
        }
        
        TopDocs td = searcher.search(new TermQuery(new Term("pid", Long.toString(pid))), 1);
        ScoreDoc [] sd = td.scoreDocs;
        if(sd.length > 0)
        {
            Document tmpdoc= searcher.getIndexReader().document(sd[0].doc);
            String cats  = tmpdoc.get(DATA);
            if(!StringUtil.isEmpty(cats))
            {
                doc.add(new Field("category", cats, Store.YES, Index.NOT_ANALYZED));                   
            }
        }

        //Phone format in cs is no spaces, so we keep it the same for comparison purposes later
        String phone = pieces[34].trim() + pieces[35].trim() + pieces[36].trim();
        doc.add(new Field("phone", phone, Field.Store.YES, Field.Index.NOT_ANALYZED));        
                
        doc.add(new NumericField("latitude_range",  Store.YES,true).setDoubleValue(lat));   
        doc.add(new NumericField("longitude_range" , Store.YES,true).setDoubleValue(lng));

        String geohash = GeoHashUtils.encode(lat, lng);
        doc.add(new Field(GEOHASH, geohash, Field.Store.YES, Field.Index.NOT_ANALYZED));
        
        writer.addDocument(doc);        
    }
    

    public static TLongLongHashMap deSerializeCache(String fileName) throws IOException, ClassNotFoundException
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
    
    private static void populateMainIndex(String inputDir, String mainIndexDir) throws IOException, InterruptedException, ExecutionException
    {
        String idRaw = mainIndexDir;
        File index = new File(idRaw);
        index.mkdir();
        Directory directory = new NIOFSDirectory(index);
        final IndexWriter writer = new IndexWriter(directory, new SimpleAnalyzer(), true, MaxFieldLength.UNLIMITED);
        writer.setMergeFactor(100000);
        writer.setMaxMergeDocs(Integer.MAX_VALUE);        
        String inputFileName = inputDir + "/BaseRecords.txt";
        String logFile = inputDir + "/errs.txt";
        final BufferedWriter err = new BufferedWriter(new FileWriter(logFile));
        
        BufferedReader bfr = new BufferedReader(new FileReader(new File(inputFileName)));
        String line = "";
        final IndexSearcher searcher = new IndexSearcher(new NIOFSDirectory(new File(mainIndexDir + "/cmpcat")));
        ExecutorService thePool = Executors.newFixedThreadPool(4);
        int cnt = 0;
        ArrayList<Future<?>> jobs = new ArrayList<Future<?>>();
        
        //find whereID max
        long[] existingWhereIDs = mappedExistingIDs.getValues();
        
        int IDmax=startId_;
        for(int i=0;i<existingWhereIDs.length;i++)
            if(existingWhereIDs[i] > IDmax)
                IDmax=i;
        System.out.println("Max of existing whereids is: "+IDmax);
        
        //start atomic long at max value
        final AtomicLong currentWhereId = new AtomicLong(IDmax);
        while((line = bfr.readLine()) != null)
        {
            if(++cnt % 5000 == 0){System.out.print("+");}
            if(cnt % 200000 == 0){System.out.println();}

           
        
            final String cp = line.trim();
            
            Future<?> fut = thePool.submit(new Runnable(){public void run(){try
                                                                            {
                                                                                writeDoc(cp, err, writer, searcher, currentWhereId);
                                                                            } 
                                                                            catch (IOException e)
                                                                            {
                                                                                throw(new RuntimeException(e));
                                                                            }}});
            jobs.add(fut);
            if(cnt % 2000 == 0)
            {
                //every 2k lines we clean up
                for(Future<?> job: jobs)
                {
                    job.get();
                }
                jobs.clear();
            }
            if(cnt %500000 == 0)
            {
                writer.optimize();
            }
        }
        
        //shutdown will wait for jobs to get cleaned up, so we don't need
        //to do anything special here        System.out.println("populating main index");       

        thePool.shutdown();
        bfr.close();
        writer.optimize();
        writer.close();
    }

    
    
	public static void main(String[] args) throws Exception
	{
	    if(args.length == 4){
	        mappedExistingIDs = deSerializeCache(args[3]); //read in deserialized map2whereIDs
	        
    	    System.out.println("populating category maps");
    	    populateCategoryMap(args[0]); 
    		populateSubcategoryMap(args[0]);
    		populateSubSubcategoryMap(args[0]);
            System.out.println("finished populating category maps");
            System.out.println("populating company categories");
    		populateCompanyCategories(args[1], args[2]);
    	    System.out.println("finishedpopulating company categories");
            System.out.println("populating main index");
    		populateMainIndex(args[1], args[2]);
            System.out.println("finished populating main index");
	    }
	    else {throw(new Exception("need 4 args"));}
	}
}