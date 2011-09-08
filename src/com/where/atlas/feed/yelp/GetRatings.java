package com.where.atlas.feed.yelp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GetRatings {
	static FileReader ids_fileReader = null;
	static BufferedReader ids_buff = null;
	static String ids_input_dir = "/home/qimeng/inputForTest";

	static FileReader json_fileReader = null;
	static BufferedReader json_buff = null;
	static String json_input_dir = "/home/qimeng/inputjson/";
	static HashMap<Long, Long> hashMap = new HashMap<Long, Long>();

	static FileWriter result_fileWriter = null;
	static String result_dir = "/home/qimeng/outPutForTest";
	static BufferedWriter result_buff = null;
	
	static AtomicLong atomicN = new AtomicLong();
	static HashMap<String, Long> id_map = new HashMap<String, Long>();
	static String id_hash_dir = "/home/qimeng/id_hash";
	static FileWriter id_hash_file = null;
	static BufferedWriter id_hash_buff = null;
	public static final void main(String[] args){
		try {
			ids_fileReader = new FileReader(ids_input_dir);
			ids_buff = new BufferedReader(ids_fileReader);
			String input_line = null;
			while((input_line = ids_buff.readLine()) != null){

				String[] inputs = input_line.split(",");

				Long yelp_id = Long.parseLong(inputs[0]);
				Long where_id = Long.parseLong(inputs[1]);
				hashMap.put(yelp_id, where_id);
			}
			System.out.println("HashMap done!!!!");
			
			result_fileWriter = new FileWriter(result_dir);
			result_buff = new BufferedWriter(result_fileWriter);
			
			id_hash_file = new FileWriter(id_hash_dir);
			id_hash_buff = new BufferedWriter(id_hash_file);
			System.out.println("initialized buffers");

			File input_directory = new File(json_input_dir);
			final File[] input_files = input_directory.listFiles();
			
			int counter = 0;
			for(int i=0; i<input_files.length;  i++){
				json_fileReader = new FileReader(input_files[i]);
				json_buff = new BufferedReader(json_fileReader);
				
				String json_line = null;
				while((json_line = json_buff.readLine()) != null){
					try {
						JSONObject json = new JSONObject(json_line);
						Long place_id = null;
						if((place_id = hashMap.get(json.optLong("_id"))) != null){
							JSONArray jsar = json.optJSONArray("reviews");
							if(jsar != null){
								int jsar_size = jsar.length();
								for(int j=0; j<jsar_size; j++){
									JSONObject user = (JSONObject)jsar.get(j);
									String userHash = user.optString("user_id");
									Long userId;
									if(id_map.get(userHash) == null){
										userId = atomicN.getAndIncrement();
										id_map.put(userHash, userId);
										id_hash_buff.write(userHash +","+userId);
										id_hash_buff.newLine();
									}
									else {
										userId = id_map.get(userHash);
									}
									
//									System.out.println(user.optString("user_id")+","+place_id+","+user.optString("rating"));
									result_buff.write(userId +","+place_id+","+user.optString("rating"));
									result_buff.newLine();
									result_buff.flush();
								}
							}

						}
					} catch (JSONException e) {
						e.printStackTrace();
					}		
				}	
				
				counter++;
				if(counter % 30 == 0){
					System.out.println(counter + " files processed");
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try{
				id_hash_buff.flush();
				id_hash_buff.close();
				id_hash_file.close();
				ids_buff.close();
				ids_fileReader.close();
				json_buff.close();
				json_fileReader.close();
				result_buff.close();
				result_fileWriter.close();
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
	} 
	
}
