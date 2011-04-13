package com.where.tests;

import org.apache.lucene.document.Document;
import org.json.JSONObject;

import com.where.commons.feed.citysearch.CSListing;
import com.where.commons.feed.citysearch.CSListingFactory;
import com.where.utils.IndexProcessor;


/**
 * 
 * @author fliuzzi
 *
 */
public class LIS_mini_details_test extends IndexProcessor{
	
	boolean allTestsPassed;
	
	public LIS_mini_details_test(String indexPath)
	{
		super(indexPath);
		allTestsPassed = true;
	}
	

	public static void main(String[] args) {
		if (args.length != 1) return;
		
		try{
			new LIS_mini_details_test(args[0]).readDocuments();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void processDocument(Document doc) throws Exception {
		JSONObject json = new JSONObject(doc.get("mini_details"));
		
		CSListing listing = CSListingFactory.fromJSON(json);
		boolean passing = true;
		
		passing = passing && json.optJSONArray("ids").getJSONObject(1).optString("where").equals(listing.getWhereId());
		passing = passing && json.optJSONArray("ids").getJSONObject(0).optString("cs").equals(listing.getListingId());
		passing = passing && json.optString("name") == listing.getName();
		passing = passing && json.optJSONObject("location").toString().equals(listing.getAddress().toJSON().toString());
		
		if(passing){
			System.out.println("PASSED.");
			allTestsPassed = allTestsPassed && true;
		}
		else{
			System.err.println("FAILED.");
			allTestsPassed = allTestsPassed && false;
		}
	}

	@Override
	public void finishProcessing() throws Exception {
		if(allTestsPassed)
			System.out.println("ALL TESTS PASSED!");
		else
			System.err.println("SOME TESTS FAILED, TRY AGAIN N00b");
	}

}
