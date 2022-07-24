package com.eka.ekaPricing.pojo;

import java.util.List;
import java.util.Map;

public class StatusCollection {


	private String collectionName;
	private String collectionHeaderProperty;
	private String collectionConnectMapProperty;
	private String dataLoadOption;
	private List<Map<String, Object>> collectionData;
	private String format;

	public String getCollectionName() {
		return collectionName;
	}
	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}
	
	public String getDataLoadOption() {
		return dataLoadOption;
	}
	public void setDataLoadOption(String dataLoadOption) {
		this.dataLoadOption = dataLoadOption;
	}

	/*public List<Object> getCollectionData() {
		return collectionData;
	}
	public void setCollectionData(List<Object> collectionData) {
		this.collectionData = collectionData;
	}*/
	public List<Map<String, Object>> getCollectionData() {
		return collectionData;
	}
	public void setCollectionData(List<Map<String, Object>> serviceKeyList) {
		this.collectionData = serviceKeyList;
	}
	public String getFormat() {
		return format;
	}
	public void setFormat(String format) {
		this.format = format;
	}
	public String getCollectionHeaderProperty() {
		return collectionHeaderProperty;
	}
	public void setCollectionHeaderProperty(String collectionHeaderProperty) {
		this.collectionHeaderProperty = collectionHeaderProperty;
	}
	public String getCollectionConnectMapProperty() {
		return collectionConnectMapProperty;
	}
	public void setCollectionConnectMapProperty(String collectionConnectMapProperty) {
		this.collectionConnectMapProperty = collectionConnectMapProperty;
	}

}

