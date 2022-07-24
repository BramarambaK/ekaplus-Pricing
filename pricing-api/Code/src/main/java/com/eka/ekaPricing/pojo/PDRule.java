package com.eka.ekaPricing.pojo;

public class PDRule {
	private String internalContractItemRefNo;
	private String pdRuleName;
	private String productAttribute;
	private double fromRange;
	private double toRange;
	private String tickValueType;
	private double tickSize;
	private double tickValue;
	private double baseValue;
	public double getBaseValue() {
		return baseValue;
	}
	public void setBaseValue(double baseValue) {
		this.baseValue = baseValue;
	}
	public String getInternalContractItemRefNo() {
		return internalContractItemRefNo;
	}
	public void setInternalContractItemRefNo(String internalContractItemRefNo) {
		this.internalContractItemRefNo = internalContractItemRefNo;
	}
	public String getPdRuleName() {
		return pdRuleName;
	}
	public void setPdRuleName(String pdRuleName) {
		this.pdRuleName = pdRuleName;
	}
	public String getProductAttribute() {
		return productAttribute;
	}
	public void setProductAttribute(String productAttribute) {
		this.productAttribute = productAttribute;
	}
	public double getFromRange() {
		return fromRange;
	}
	public void setFromRange(double fromRange) {
		this.fromRange = fromRange;
	}
	public double getToRange() {
		return toRange;
	}
	public void setToRange(double toRange) {
		this.toRange = toRange;
	}
	public String getTickValueType() {
		return tickValueType;
	}
	public void setTickValueType(String tickValueType) {
		this.tickValueType = tickValueType;
	}
	public double getTickSize() {
		return tickSize;
	}
	public void setTickSize(double tickSize) {
		this.tickSize = tickSize;
	}
	public double getTickValue() {
		return tickValue;
	}
	public void setTickValue(double tickValue) {
		this.tickValue = tickValue;
	}
	public String getAdjustmentType() {
		return adjustmentType;
	}
	public void setAdjustmentType(String adjustmentType) {
		this.adjustmentType = adjustmentType;
	}
	private String adjustmentType;
}
