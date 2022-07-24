package com.eka.ekaPricing.pojo;

import java.time.LocalDate;

public class GMRQPDateDetails {

	private GMR gmr;
	private LocalDate qpFromDate;
	private LocalDate qpToDate;
	public GMR getGmr() {
		return gmr;
	}
	public void setGmr(GMR gmr) {
		this.gmr = gmr;
	}
	public LocalDate getQpFromDate() {
		return qpFromDate;
	}
	public void setQpFromDate(LocalDate qpFromDate) {
		this.qpFromDate = qpFromDate;
	}
	public LocalDate getQpToDate() {
		return qpToDate;
	}
	public void setQpToDate(LocalDate qpToDate) {
		this.qpToDate = qpToDate;
	}
}
