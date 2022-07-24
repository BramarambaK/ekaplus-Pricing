package com.eka.ekaPricing.pojo;

import java.util.List;

import org.json.JSONObject;

public class TriggerPriceProperties {
	private JSONObject itemObj;
	private String precision;
	private GMR gmr;
	private List<String> currencyList;
	private List<String> qtyUnitList;
	private String holidayRule;
	private List<String> productIdList;
	private String contractItemQty;
	private String quality;
	private double baseQtyUnitConversion;
	public JSONObject getItemObj() {
		return itemObj;
	}
	public void setItemObj(JSONObject itemObj) {
		this.itemObj = itemObj;
	}
	public String getPrecision() {
		return precision;
	}
	public void setPrecision(String precision) {
		this.precision = precision;
	}
	public GMR getGmr() {
		return gmr;
	}
	public void setGmr(GMR gmr) {
		this.gmr = gmr;
	}
	public List<String> getCurrencyList() {
		return currencyList;
	}
	public void setCurrencyList(List<String> currencyList) {
		this.currencyList = currencyList;
	}
	public List<String> getQtyUnitList() {
		return qtyUnitList;
	}
	public void setQtyUnitList(List<String> qtyUnitList) {
		this.qtyUnitList = qtyUnitList;
	}
	public String getHolidayRule() {
		return holidayRule;
	}
	public void setHolidayRule(String holidayRule) {
		this.holidayRule = holidayRule;
	}
	public List<String> getProductIdList() {
		return productIdList;
	}
	public void setProductIdList(List<String> productIdList) {
		this.productIdList = productIdList;
	}
	public String getContractItemQty() {
		return contractItemQty;
	}
	public void setContractItemQty(String contractItemQty) {
		this.contractItemQty = contractItemQty;
	}
	public String getQuality() {
		return quality;
	}
	public void setQuality(String quality) {
		this.quality = quality;
	}
	public double getBaseQtyUnitConversion() {
		return baseQtyUnitConversion;
	}
	public void setBaseQtyUnitConversion(double baseQtyUnitConversion) {
		this.baseQtyUnitConversion = baseQtyUnitConversion;
	}
	
}
