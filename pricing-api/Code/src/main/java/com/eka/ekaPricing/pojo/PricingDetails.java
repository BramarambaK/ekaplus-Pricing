package com.eka.ekaPricing.pojo;

/**
 * Model Class - PricingDetails - which is written for getting pricing formula details rest api's
 * 
 * @author Rushikesh Bhosale
 *
 */


public class PricingDetails {

	private String itemNo;
	private String contractItemRefNo;
	private String formulaExpression;
	
	private String quotedPeriod;
	private String priceQuotaRule;
	private String pricePeriod;

	private String curveName;

	private String event;
	private String offset;
	private String offsetType;

	
	public String getItemNo() {
		return itemNo;
	}

	public void setItemNo(String itemNo) {
		this.itemNo = itemNo;
	}

	public String getContractItemRefNo() {
		return contractItemRefNo;
	}

	public void setContractItemRefNo(String contractItemRefNo) {
		this.contractItemRefNo = contractItemRefNo;
	}

	public String getFormulaExpression() {
		return formulaExpression;
	}

	public void setFormulaExpression(String formulaExpression) {
		this.formulaExpression = formulaExpression;
	}

	public String getQuotedPeriod() {
		return quotedPeriod;
	}

	public void setQuotedPeriod(String quotedPeriod) {
		this.quotedPeriod = quotedPeriod;
	}

	public String getPriceQuotaRule() {
		return priceQuotaRule;
	}

	public void setPriceQuotaRule(String priceQuotaRule) {
		this.priceQuotaRule = priceQuotaRule;
	}

	public String getPricePeriod() {
		return pricePeriod;
	}

	public void setPricePeriod(String pricePeriod) {
		this.pricePeriod = pricePeriod;
	}

	public String getCurveName() {
		return curveName;
	}

	public void setCurveName(String curveName) {
		this.curveName = curveName;
	}

	public String getEvent() {
		return event;
	}

	public void setEvent(String event) {
		this.event = event;
	}

	public String getOffset() {
		return offset;
	}

	public void setOffset(String offset) {
		this.offset = offset;
	}

	public String getOffsetType() {
		return offsetType;
	}

	public void setOffsetType(String offsetType) {
		this.offsetType = offsetType;
	}
	
	
	
	
	
	
	
}
