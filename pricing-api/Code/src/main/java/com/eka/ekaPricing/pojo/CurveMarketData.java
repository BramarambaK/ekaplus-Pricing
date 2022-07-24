package com.eka.ekaPricing.pojo;

import java.time.LocalDate;

public class CurveMarketData {
	private String instrumentName;
	private String exchange;
	private String priceUnit;
	private LocalDate pricingDate;
	private LocalDate promptDate;
	private String price;
	private String monthYear;
	public String getInstrumentName() {
		return instrumentName;
	}
	public void setInstrumentName(String instrumentName) {
		this.instrumentName = instrumentName;
	}
	public String getExchange() {
		return exchange;
	}
	public void setExchange(String exchange) {
		this.exchange = exchange;
	}
	public String getPriceUnit() {
		return priceUnit;
	}
	public void setPriceUnit(String priceUnit) {
		this.priceUnit = priceUnit;
	}
	public LocalDate getPricingDate() {
		return pricingDate;
	}
	public void setPricingDate(LocalDate pricingDate) {
		this.pricingDate = pricingDate;
	}
	public LocalDate getPromptDate() {
		return promptDate;
	}
	public void setPromptDate(LocalDate promptDate) {
		this.promptDate = promptDate;
	}
	public String getPrice() {
		return price;
	}
	public void setPrice(String price) {
		this.price = price;
	}
	public String getMonthYear() {
		return monthYear;
	}
	public void setMonthYear(String monthYear) {
		this.monthYear = monthYear;
	}
	
	
}
