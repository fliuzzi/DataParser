package com.where.data.parsers.citysearch;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ParseUtils {
	private ParseUtils() {}
	
	public static Element getChildByName(Element start, String name) {
		NodeList children = start.getChildNodes();
		if(children == null) return null;
		
		for(int i = 0, n = children.getLength(); i < n; i++) {
			if(name.equals(children.item(i).getNodeName())) {
				return (Element)children.item(i);
			}
		}
		return null;
	}
	
	public static String getChildValueByName(Element start, String name) {
		Element child = getChildByName(start, name);
		if(child == null) return null;
		
		return child.getChildNodes().item(0) != null ? child.getChildNodes().item(0).getNodeValue() : null;
	}	
	
	public static String getAttributeValue(Element e, String attribute) {
		return e.getAttribute(attribute);
	}
	
	public static String getValue(Element start) {	
		try {
			return start.getChildNodes().item(0).getNodeValue();
		}
		catch(Exception ignored) {
			return null;
		}
	}			
}
