package com.eka.ekaPricing.pojo;



public class PriceFormulaDetails {
	private int seqNo;
	private String contractRefNo;
	private String contractItemRefNo;
	private String formulaExpression;
	private String curveName;
	private String curveUnit;
	private String pricePoint;
	private String priceType;
	private String priceQuotaRule;
	private String pricePeriod;
	private String startDate;
	private String endDate;
	private String quotedPeriod;
	private String event;
	private String offset;
	private String offsetType;
	private String differential;
	private String fxType;
	private String fxValue;
	private String differentialType;
	
	public int getSeqNo() {
		return seqNo;
	}

	public void setSeqNo(int seqNo) {
		this.seqNo = seqNo;
	}

	public String getPricePoint() {
		return pricePoint;
	}

	public void setPricePoint(String pricePoint) {
		this.pricePoint = pricePoint;
	}

	public String getPriceType() {
		return priceType;
	}

	public void setPriceType(String priceType) {
		this.priceType = priceType;
	}

	public String getDifferential() {
		return differential;
	}

	public void setDifferential(String differential) {
		this.differential = differential;
	}

	public String getFxValue() {
		return fxValue;
	}

	public void setFxValue(String fxValue) {
		this.fxValue = fxValue;
	}

	public String getContractRefNo() {
		return contractRefNo;
	}

	public void setContractRefNo(String contractRefNo) {
		this.contractRefNo = contractRefNo;
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

	public String getStartDate() {
		return startDate;
	}

	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	public String getEndDate() {
		return endDate;
	}

	public void setEndDate(String endDate) {
		this.endDate =  endDate;
	}

	public String getFxType() {
		return fxType;
	}

	public void setFxType(String fxType) {
		this.fxType = fxType;
	}

	public String getCurveUnit() {
		return curveUnit;
	}

	public void setCurveUnit(String curveUnit) {
		this.curveUnit = curveUnit;
	}

	public String getDifferentialType() {
		return differentialType;
	}

	public void setDifferentialType(String differentialType) {
		this.differentialType = differentialType;
	}
	
}