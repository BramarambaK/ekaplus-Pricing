package com.eka.ekaPricing.pojo;

import java.util.List;

import org.json.JSONObject;

public class FixationObject {
	private String fixatonNumber;
	private double fixationQty;
	private double fixedPrice;
	private String gmrRef;
	private List<JSONObject> stocks;
	private double fixationQtyInGMRQtyUnit;
	public String getFixatonNumber() {
		return fixatonNumber;
	}
	public void setFixatonNumber(String fixatonNumber) {
		this.fixatonNumber = fixatonNumber;
	}
	public double getFixationQty() {
		return fixationQty;
	}
	public void setFixationQty(double fixationQty) {
		this.fixationQty = fixationQty;
	}
	public double getFixedPrice() {
		return fixedPrice;
	}
	public void setFixedPrice(double fixedPrice) {
		this.fixedPrice = fixedPrice;
	}
	public String getGmrRef() {
		return gmrRef;
	}
	public void setGmrRef(String gmrRef) {
		this.gmrRef = gmrRef;
	}
	public List<JSONObject> getStocks() {
		return stocks;
	}
	public void setStocks(List<JSONObject> stocks) {
		this.stocks = stocks;
	}
	public double getFixationQtyInGMRQtyUnit() {
		return fixationQtyInGMRQtyUnit;
	}
	public void setFixationQtyInGMRQtyUnit(double fixationQtyInGMRQtyUnit) {
		this.fixationQtyInGMRQtyUnit = fixationQtyInGMRQtyUnit;
	}
}
