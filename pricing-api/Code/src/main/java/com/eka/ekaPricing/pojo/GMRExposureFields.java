package com.eka.ekaPricing.pojo;

import java.time.LocalDateTime;
import java.util.List;

public class GMRExposureFields {
	private double qty;
	private List<LocalDateTime> realDates;
	private List<LocalDateTime> validDates;
	private GMR gmr;
	private double stockQtyInGmr;
	public double getQty() {
		return qty;
	}
	public void setQty(double qty) {
		this.qty = qty;
	}
	public List<LocalDateTime> getRealDates() {
		return realDates;
	}
	public void setRealDates(List<LocalDateTime> realDates) {
		this.realDates = realDates;
	}
	public List<LocalDateTime> getValidDates() {
		return validDates;
	}
	public void setValidDates(List<LocalDateTime> validDates) {
		this.validDates = validDates;
	}
	public GMR getGmr() {
		return gmr;
	}
	public void setGmr(GMR gmr) {
		this.gmr = gmr;
	}
	public double getStockQtyInGmr() {
		return stockQtyInGmr;
	}
	public void setStockQtyInGmr(double stockQtyInGmr) {
		this.stockQtyInGmr = stockQtyInGmr;
	}
	
}
