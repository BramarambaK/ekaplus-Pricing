package com.eka.ekaPricing.pojo;

import java.util.List;

public class PDSchedule {
	private String attributeName;
	private String priceUnit;
	private List<QualityAdjustment> details;
	public String getAttributeName() {
		return attributeName;
	}
	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}
	public String getPriceUnit() {
		return priceUnit;
	}
	public void setPriceUnit(String priceUnit) {
		this.priceUnit = priceUnit;
	}
	public List<QualityAdjustment> getDetails() {
		return details;
	}
	public void setDetails(List<QualityAdjustment> details) {
		this.details = details;
	}
}
