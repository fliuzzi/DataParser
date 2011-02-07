package com.where.place;

import org.json.JSONObject;


public class Address {

	private String address1;
	private String address2;
	private String neighborhood;
	private String city;
	private String state;
	private String zip;
	private String country;
	
	private double lat = Double.NaN;
    private double lng = Double.NaN;

	public String getAddress1() {
		return address1;
	}

	public void setAddress1(String street1) {
		this.address1 = street1;
	}

	public String getAddress2() {
		return address2;
	}

	public void setAddress2(String street2) {
		this.address2 = street2;
	}

	public String getNeighborhood() {
		return neighborhood;
	}

	public void setNeighborhood(String neighborhood) {
		this.neighborhood = neighborhood;
	}

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

	public String getZip() {
		return zip;
	}

	public void setZip(String zip) {
		this.zip = zip;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}
	
	public double getLat() {
        return lat;
    }
    public void setLat(double lat) {
        this.lat = lat;
    }
    public double getLng() {
        return lng;
    }
    public void setLng(double lng) {
        this.lng = lng;
    }
	
	public JSONObject toJSON() {
        try {
            JSONObject address = new JSONObject();
            
            if(getAddress1() != null) {
                address.put("address1", getAddress1());
            }
            if(getAddress2() != null) {
                address.put("address2", getAddress2());
            }
            if(getNeighborhood() != null) {
                address.put("neighborhood", getNeighborhood());
            }
            if(getCity() != null) {
                address.put("city", getCity());
            }
            if(getState() != null) {
                address.put("state", getState());
            }
            if(getZip() != null) {
                address.put("zip", getZip());
            }
            if(getCountry() != null) {
                address.put("country", getCountry());
            }
            
            address.put("lat", getLat());
            address.put("lng", getLng());
            
            return address;
        }
        catch(Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
