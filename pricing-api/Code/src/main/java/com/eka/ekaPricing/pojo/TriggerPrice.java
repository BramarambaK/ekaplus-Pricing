package com.eka.ekaPricing.pojo;

public class TriggerPrice {
	private String triggerDate;
	private double quantity;
	private double price;
	private String priceU;
	private double fxrate;
	private double qtyConversion;
	private double calculatedPrice;
	private boolean isConsumed;
	private double consumedQuantity;
	private long sys__createdOn;
	private String fixationMethod;
	private String formulaId;
	private String internalContractItemRefNo;
	private String internalContractRefNo;
	private String fixationRefNo;
	private double itemFixedQtyAvailable;
	private String execution;
	private String gmrRefNo;
	private String fixationStatus;
	private double cancelledFixationQty; 
	public String getTriggerDate() {
		return triggerDate;
	}
	public void setTriggerDate(String triggerDate) {
		this.triggerDate = triggerDate;
	}
	public double getQuantity() {
		return quantity;
	}
	public void setQuantity(double quantity) {
		this.quantity = quantity;
	}
	public double getPrice() {
		return price;
	}
	public void setPrice(double price) {
		this.price = price;
	}
	public String getPriceU() {
		return priceU;
	}
	public void setPriceU(String priceU) {
		this.priceU = priceU;
	}
	
	public double getFxrate() {
		return fxrate;
	}
	public void setFxrate(double fxrate) {
		this.fxrate = fxrate;
	}
	public double getQtyConversion() {
		return qtyConversion;
	}
	public void setQtyConversion(double qtyConversion) {
		this.qtyConversion = qtyConversion;
	}
	public double getCalculatedPrice() {
		return calculatedPrice;
	}
	public void setCalculatedPrice(double calculatedPrice) {
		this.calculatedPrice = calculatedPrice;
	}
	public boolean isConsumed() {
		return isConsumed;
	}
	public void setConsumed(boolean isConsumed) {
		this.isConsumed = isConsumed;
	}
	public double getConsumedQuantity() {
		return consumedQuantity;
	}
	public void setConsumedQuantity(double consumedQuantity) {
		this.consumedQuantity = consumedQuantity;
	}
	public long getSys__createdOn() {
		return sys__createdOn;
	}
	public void setSys__createdOn(long sys__createdOn) {
		this.sys__createdOn = sys__createdOn;
	}
	public String getFixationMethod() {
		return fixationMethod;
	}
	public void setFixationMethod(String fixationMethod) {
		this.fixationMethod = fixationMethod;
	}
	public String getFormulaId() {
		return formulaId;
	}
	public void setFormulaId(String formulaId) {
		this.formulaId = formulaId;
	}
	public String getInternalContractItemRefNo() {
		return internalContractItemRefNo;
	}
	public void setInternalContractItemRefNo(String internalContractItemRefNo) {
		this.internalContractItemRefNo = internalContractItemRefNo;
	}
	public String getInternalContractRefNo() {
		return internalContractRefNo;
	}
	public void setInternalContractRefNo(String internalContractRefNo) {
		this.internalContractRefNo = internalContractRefNo;
	}
	public String getFixationRefNo() {
		return fixationRefNo;
	}
	public void setFixationRefNo(String fixationRefNo) {
		this.fixationRefNo = fixationRefNo;
	}
	public double getItemFixedQtyAvailable() {
		return itemFixedQtyAvailable;
	}
	public void setItemFixedQtyAvailable(double itemFixedQtyAvailable) {
		this.itemFixedQtyAvailable = itemFixedQtyAvailable;
	}
	public String getExecution() {
		return execution;
	}
	public void setExecution(String execution) {
		this.execution = execution;
	}
	public String getGmrRefNo() {
		return gmrRefNo;
	}
	public void setGmrRefNo(String gmrRefNo) {
		this.gmrRefNo = gmrRefNo;
	}
	public String getFixationStatus() {
		return fixationStatus;
	}
	public void setFixationStatus(String fixationStatus) {
		this.fixationStatus = fixationStatus;
	}
	public double getCancelledFixationQty() {
		return cancelledFixationQty;
	}
	public void setCancelledFixationQty(double cancelledFixationQty) {
		this.cancelledFixationQty = cancelledFixationQty;
	}
}

