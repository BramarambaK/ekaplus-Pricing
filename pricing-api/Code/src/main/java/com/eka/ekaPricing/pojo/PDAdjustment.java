package com.eka.ekaPricing.pojo;

public class PDAdjustment {
	public String getProductId() {
		return productId;
	}
	public void setProductId(String productId) {
		this.productId = productId;
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
	public double getBaseValue() {
		return baseValue;
	}
	public void setBaseValue(double baseValue) {
		this.baseValue = baseValue;
	}
	public String getPdValue() {
		return pdValue;
	}
	public void setPdValue(String pdValue) {
		this.pdValue = pdValue;
	}
	private String productId;
	private String internalContractItemRefNo;
	private String pdRuleName;
	private String productAttribute;
	private double baseValue;
	private String pdValue;
}
