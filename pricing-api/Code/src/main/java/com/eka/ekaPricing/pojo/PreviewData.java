package com.eka.ekaPricing.pojo;

import java.util.Map;

import org.json.JSONArray;

public class PreviewData {
	private String collapse;
	private String exchange;
	private String curveName;
	private double curvePrice;
	private String curveCurrency;
	private double coefficient;
	private String curveQtyUnit;
	private String qtyUnit;
	private String priceUnit;
	private String qpStartDate;
	private String qpEndDate;
	private double pricedQty;
	private double unPricedQty;
	private JSONArray data;
	private JSONArray qtyData;
	private String componentName;
	private JSONArray exposureArray;
	private String baseQtyUnit;
	private String status;
	private String remarks;
	private double qtyConversionUnit;
	private double actualConversionUnit;
	private String valuationInstrument;
	private double baseContractualConversionFactor;
	private double avgPriceFx;
	private Map<String, Double> gmrToItemConversionFactor;
	private double totalGmrQty;
	
	public String getCollapse() {
		return collapse;
	}
	public void setCollapse(String collapse) {
		this.collapse = collapse;
	}
	public String getExchange() {
		return exchange;
	}
	public void setExchange(String exchange) {
		this.exchange = exchange;
	}
	public String getCurveName() {
		return curveName;
	}
	public void setCurveName(String curveName) {
		this.curveName = curveName;
	}
	public double getCurvePrice() {
		return curvePrice;
	}
	public void setCurvePrice(double curvePrice) {
		this.curvePrice = curvePrice;
	}
	public String getCurveCurrency() {
		return curveCurrency;
	}
	public void setCurveCurrency(String curveCurrency) {
		this.curveCurrency = curveCurrency;
	}
	public double getCoefficient() {
		return coefficient;
	}
	public void setCoefficient(double coefficient) {
		this.coefficient = coefficient;
	}
	public String getCurveQtyUnit() {
		return curveQtyUnit;
	}
	public void setCurveQtyUnit(String curveQtyUnit) {
		this.curveQtyUnit = curveQtyUnit;
	}
	public String getQtyUnit() {
		return qtyUnit;
	}
	public void setQtyUnit(String qtyUnit) {
		this.qtyUnit = qtyUnit;
	}
	public String getPriceUnit() {
		return priceUnit;
	}
	public void setPriceUnit(String priceUnit) {
		this.priceUnit = priceUnit;
	}
	public String getQpStartDate() {
		return qpStartDate;
	}
	public void setQpStartDate(String qpStartDate) {
		this.qpStartDate = qpStartDate;
	}
	public String getQpEndDate() {
		return qpEndDate;
	}
	public void setQpEndDate(String qpEndDate) {
		this.qpEndDate = qpEndDate;
	}
	public double getPricedQty() {
		return pricedQty;
	}
	public void setPricedQty(double pricedQty) {
		this.pricedQty = pricedQty;
	}
	public double getUnPricedQty() {
		return unPricedQty;
	}
	public void setUnPricedQty(double unPricedQty) {
		this.unPricedQty = unPricedQty;
	}
	public JSONArray getData() {
		return data;
	}
	public void setData(JSONArray data) {
		this.data = data;
	}
	public JSONArray getQtyData() {
		return qtyData;
	}
	public void setQtyData(JSONArray qtyData) {
		this.qtyData = qtyData;
	}
	public String getComponentName() {
		return componentName;
	}
	public void setComponentName(String componentName) {
		this.componentName = componentName;
	}
	public JSONArray getExposureArray() {
		return exposureArray;
	}
	public void setExposureArray(JSONArray exposureArray) {
		this.exposureArray = exposureArray;
	}
	public String getBaseQtyUnit() {
		return baseQtyUnit;
	}
	public void setBaseQtyUnit(String baseQtyUnit) {
		this.baseQtyUnit = baseQtyUnit;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getRemarks() {
		return remarks;
	}
	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}
	public double getQtyConversionUnit() {
		return qtyConversionUnit;
	}
	public void setQtyConversionUnit(double qtyConversionUnit) {
		this.qtyConversionUnit = qtyConversionUnit;
	}
	public double getActualConversionUnit() {
		return actualConversionUnit;
	}
	public void setActualConversionUnit(double actualConversionUnit) {
		this.actualConversionUnit = actualConversionUnit;
	}
	public String getValuationInstrument() {
		return valuationInstrument;
	}
	public void setValuationInstrument(String valuationInstrument) {
		this.valuationInstrument = valuationInstrument;
	}
	public double getBaseContractualConversionFactor() {
		return baseContractualConversionFactor;
	}
	public void setBaseContractualConversionFactor(double baseContractualConversionFactor) {
		this.baseContractualConversionFactor = baseContractualConversionFactor;
	}
	public double getAvgPriceFx() {
		return avgPriceFx;
	}
	public void setAvgPriceFx(double avgPriceFx) {
		this.avgPriceFx = avgPriceFx;
	}
	public Map<String, Double> getGmrToItemConversionFactor() {
		return gmrToItemConversionFactor;
	}
	public void setGmrToItemConversionFactor(Map<String, Double> gmrToItemConversionFactor) {
		this.gmrToItemConversionFactor = gmrToItemConversionFactor;
	}
	public double getTotalGmrQty() {
		return totalGmrQty;
	}
	public void setTotalGmrQty(double totalGmrQty) {
		this.totalGmrQty = totalGmrQty;
	}
	
}
