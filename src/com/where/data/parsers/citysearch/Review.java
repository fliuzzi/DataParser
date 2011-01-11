package com.where.data.parsers.citysearch;

import java.io.Serializable;

import org.json.simple.JSONObject;

@SuppressWarnings("unchecked")
public class Review implements Serializable {
	private static final long serialVersionUID = -670240118769968303L;
	
	private String title;
	private String author;
	private String review;
	private String pros;
	private String cons;
	private String rating;
	private String date;
	
	private Attribution attribution;
	
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getAuthor() {
		return author;
	}
	public void setAuthor(String author) {
		this.author = author;
	}
	public String getReview() {
		return review;
	}
	public void setReview(String review) {
		this.review = review;
	}
	public String getPros() {
		return pros;
	}
	public void setPros(String pros) {
		this.pros = pros;
	}
	public String getCons() {
		return cons;
	}
	public void setCons(String cons) {
		this.cons = cons;
	}
	public double getRating() {
		try {
			return rating == null ? 0 : Double.parseDouble(rating)/2;
		}
		catch(Exception ex) {
			return 0;
		}
	}
	public void setRating(String rating) {
		this.rating = rating;
	}
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}	
	
	public Attribution getAttribution() {
		return attribution;
	}
	public void setAttribution(String source, String sourceid, String logo, String url) {
		this.attribution = new Attribution(source, sourceid, logo, url);
	}

	private static class Attribution implements Serializable {
		private static final long serialVersionUID = 7016661478292179121L;
		
		private String source;
		private String sourceid;
		private String logo;
		private String url;
		
		public Attribution(String source, String sourceid, String logo, String url) {
			this.source = source;
			this.sourceid = sourceid;
			this.logo = logo;
			this.url = url;
		}
		

		public JSONObject toJSON() {
			JSONObject json = new JSONObject();
			json.put("source", source);
			json.put("sourceid", sourceid);
			json.put("logo", logo);
			json.put("url", url);
			return json;
		}
	}
	
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		
		if(title != null) {
			json.put("title", title);
		}
		
		if(author != null) {
			json.put("author", author);
		}
		
		if(review != null) {
			json.put("review", review);
		}
		
		if(pros != null) {
			json.put("pros", pros);
		}
		
		if(cons != null) {
			json.put("cons", cons);
		}
		
		if(rating != null) {
			json.put("rating", rating);
		}
		
		if(date != null) {
			json.put("date", date);
		}
		
		if(attribution != null) {
			json.put("attribution", attribution.toJSON());
		}
		
		return json;
	}
}