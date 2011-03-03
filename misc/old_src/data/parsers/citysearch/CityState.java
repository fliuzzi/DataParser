package com.where.data.parsers.citysearch;

import java.io.Serializable;

public class CityState implements Serializable {
	private static final long serialVersionUID = -1918840748896264276L;

	private String city;
	private String state;
	
	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
	}
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
}
