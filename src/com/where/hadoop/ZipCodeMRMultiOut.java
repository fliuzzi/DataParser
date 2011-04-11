package com.where.hadoop;

import java.io.IOException;
import java.util.Iterator;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.*;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 
 * @author fliuzzi
 *
 */
@SuppressWarnings("deprecation")
public class ZipCodeMRMultiOut extends Configured implements Tool  {
	
	public static class ZipMapper extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text>{
		

		@Override
		public void map(LongWritable key, Text value,
				OutputCollector<Text, Text> output, Reporter reporter)
				throws IOException {
			try {
	        	JSONObject json = new JSONObject(value.toString());
				
				String zipcode = json.optString("zip");
				
				// creates:  K: TEXT zip code  V: TEXT rest of json
				if(zipcode.length() > 0)
				{
					if(zipcode.length() > 5)
						zipcode = zipcode.substring(0,5);
					
					output.collect(new Text(zipcode), value);
				}
			} catch (JSONException e) {
				System.err.println(e.getMessage());
			}
		}
	}
	
	public static class ZipReducer extends MapReduceBase implements Reducer<Text,Text,NullWritable,Text> {
		@Override
		public void reduce(Text key, Iterator<Text> values,
				OutputCollector<NullWritable, Text> output, Reporter reporter)
				throws IOException {
			
			StringBuilder strb = new StringBuilder(); //.append(key.toString()+"\t");
			
			
			while(values.hasNext())
			{
				strb.append(values.next().toString());
				
				if(values.hasNext())
					strb.append("\t");
			}
			
			System.out.println("Done accumulating: resulting strb: "+strb);
				
			output.collect(NullWritable.get(), new Text(strb.toString()));
		}
	}


	public static void main(String[] args) throws Exception {
		int ret = ToolRunner.run(new ZipCodeMRMultiOut(), args);
		System.exit(ret);
	}



	@Override
	public int run(String[] args) throws Exception {
		
		
		JobConf conf = new JobConf(ZipCodeMRMultiOut.class);  

		conf.setJobName("ZipMultiFileBuckets");
		conf.setMapperClass(ZipMapper.class);
		conf.setMapOutputKeyClass(Text.class);
		conf.setReducerClass(ZipReducer.class);
		conf.setOutputKeyClass(NullWritable.class);
		conf.setOutputFormat(MultiOut.class);

	    FileInputFormat.setInputPaths(conf, new Path(args[0]));  
	    FileOutputFormat.setOutputPath(conf, new Path(args[1]));

		JobClient.runJob(conf);
		return 0;
	}

}
