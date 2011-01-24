package com.where.atlas;

public class Place {

	public static enum Source { LOCALEZE, CS }

	
	private String whereId;
	private String shortname;
	private String name;
	private String phone;
	private Address address;
	private Source source;
	private String nativeId;
	private double [] latlng;
	private String geohash;

	public String getWhereId() {
                return whereId;
        }

        public void setWhereId(String whereId) {
                this.whereId = whereId;
        }

        public String getGeohash() {
		return geohash;
	}

	public void setGeohash(String geohash) {
		this.geohash = geohash;
	}

	public double[] getLatlng() {
		return latlng;
	}

	public void setLatlng(double[] latlng) {
		this.latlng = latlng;
	}


	public String getShortname() {
		return shortname;
	}

	public void setShortname(String shortname) {
		this.shortname = shortname;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public Source getSource() {
		return source;
	}

	public void setSource(Source source) {
		this.source = source;
	}

	public String getNativeId() {
		return nativeId;
	}

	public void setNativeId(String nativeId) {
		this.nativeId = nativeId;
	}
}
