package com.eka.ekaPricing.pojo;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties
public class CurveData {
	private String Id;
	private String exchangeCode;
	private String pricePoint;
	private String average;
	private String instrumentId;
	private String product;
	private Date extractionDate;
	private String priceSubType;
	private String name;
	private String closePrice;
	public String getId() {
		return Id;
	}
	public void setId(String id) {
		Id = id;
	}
	public String getExchangeCode() {
		return exchangeCode;
	}
	public void setExchangeCode(String exchangeCode) {
		this.exchangeCode = exchangeCode;
	}
	public String getPricePoint() {
		return pricePoint;
	}
	public void setPricePoint(String pricePoint) {
		this.pricePoint = pricePoint;
	}
	public String getAverage() {
		return average;
	}
	public void setAverage(String average) {
		this.average = average;
	}
	public String getInstrumentId() {
		return instrumentId;
	}
	public void setInstrumentId(String instrumentId) {
		this.instrumentId = instrumentId;
	}
	public String getProduct() {
		return product;
	}
	public void setProduct(String product) {
		this.product = product;
	}
	public Date getExtractionDate() {
		return extractionDate;
	}
	public void setExtractionDate(Date extractionDate) {
		this.extractionDate = extractionDate;
	}
	public String getPriceSubType() {
		return priceSubType;
	}
	public void setPriceSubType(String priceSubType) {
		this.priceSubType = priceSubType;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getClosePrice() {
		return closePrice;
	}
	public void setClosePrice(String closePrice) {
		this.closePrice = closePrice;
	}
	public Date getTradeDate() {
		return tradeDate;
	}
	public void setTradeDate(Date tradeDate) {
		this.tradeDate = tradeDate;
	}
	private Date tradeDate;	
	

}
