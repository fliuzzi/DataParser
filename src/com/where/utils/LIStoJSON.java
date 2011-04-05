package com.where.utils;

import java.util.Iterator;
import java.util.List;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.json.JSONObject;


/**
 * 
 * @author fliuzzi
 *
 *			LIStoJSON.java
 *				Goes through one of our LIS with detail JSON lucene indices 
 *					and converts it to line delimeted json elements 
 *								(for map reduce or whatever other purposes)
 *
 *		ARG0: lisDetailJSON     ARG1: outputFile
 *
 *		TODO: NOTE: due to a  bug in the json detail generator, im generating without reviews for now
 * 					this doesnt matter for what i need because this is being used mainly as a deduping 
 * 								framework on EMR
 *
 */
public class LIStoJSON extends IndexProcessor{

	
	protected BufferedWriter writer;
	protected long count;
	
	public LIStoJSON(String index, BufferedWriter bw) {
		super(index);
		count = 0;
		writer = bw;
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 2) return;
		
		LIStoJSON lisjson = new LIStoJSON(args[0],new BufferedWriter(new FileWriter(args[1])));
		System.out.println("Initialized.  Starting run. (+ means ~10%)");
		lisjson.readDocuments();

	}
	
	public void collect(JSONObject json) throws IOException
	{
		writer.write(json.toString());
		writer.newLine();
		
		if(count % 350000 == 0)
			System.out.print("+");
	}

	@Override
	public void processDocument(Document doc) throws Exception {
		count++;
		
		//get back a list of all fields in the doc
		List<Fieldable> fields = doc.getFields();
		Iterator<Fieldable> it = fields.iterator();
		JSONObject json = new JSONObject();
		
		
		//iterate through them and add all key value to json
		while(it.hasNext())
		{
			Fieldable field = it.next();
			String key = field.name();
			String value = field.stringValue();
			
			//handle the details json
			if(key.equals("details")){
				JSONObject details = new JSONObject(value);
				details.remove("reviews");
				
				json.put(key, details);
			}
			else{
				json.put(key, value);
			}
		}
		
		//then write.
		collect(json);
	}

	@Override
	public void finishProcessing() throws Exception {
		writer.close();
		System.out.println("\nDONE.  Wrote "+count+" listings to json.");
	}

}
