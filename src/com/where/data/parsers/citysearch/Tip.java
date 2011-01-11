package com.where.data.parsers.citysearch;

import java.io.Serializable;

import org.json.simple.JSONObject;

public class Tip implements Serializable {
	private static final long serialVersionUID = -4322598889135342234L;

	private String title;
	private String text;
	
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		if(title != null) {
			json.put("title", title);
		}
		
		if(text != null) {
			json.put("text", text);
		}
		
		return json;
	}
}
