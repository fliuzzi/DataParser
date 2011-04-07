package com.where.hadoop;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 
 * @author fliuzzi
 *
 */
public class ZipCodeMapReduce extends Configured implements Tool  {
	
	public static class ZipMapper extends Mapper<LongWritable, Text, Text, Text>{
		
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
	        try {
	        	JSONObject json = new JSONObject(value.toString());
				
				String zipcode = json.optString("zip");
				
				// creates:  K: TEXT zip code  V: TEXT rest of json
				if(zipcode.length() > 0)
				{
					if(zipcode.length() > 5)
						zipcode = zipcode.substring(0,5);
					
					context.write(new Text(zipcode), value);
				}
			} catch (JSONException e) {
				System.err.println(e.getMessage());
			}
		}
	}
	
	public static class ZipReducer extends Reducer<Text,Text,Text,Text> {
		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException{
			
			JSONObject json = new JSONObject();
			System.out.println("Made new json...accumulating values from mapper...");
			
			StringBuilder strb = new StringBuilder();
			
			Iterator<Text> it = values.iterator();
			while(it.hasNext())
			{
				strb.append(it.next().toString());
				
				if(it.hasNext())
					strb.append(",");
			}
			
			
			System.out.println("Done accumulating: resulting strb: "+strb);
				
			context.write(key, new Text("["+strb.toString()+"]"));
		}
	}
	
	

	public static void main(String[] args) throws Exception {
		int ret = ToolRunner.run(new ZipCodeMapReduce(), args);
		System.exit(ret);
	}



	@Override
	public int run(String[] args) throws Exception {
		Job job = new Job(getConf());
		
		job.setJarByClass(ZipCodeMapReduce.class);
		job.setJobName("ZipCodeMapReduce");
		
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		
		job.setMapperClass(ZipMapper.class);
		//job.setCombinerClass(ZipReducer.class);
		job.setReducerClass(ZipReducer.class);
		
		
		
		job.setInputFormatClass(TextInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		
		
		FileInputFormat.setInputPaths(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
		
		boolean success = job.waitForCompletion(true);
		return success ? 0 : 1;
	}

}
