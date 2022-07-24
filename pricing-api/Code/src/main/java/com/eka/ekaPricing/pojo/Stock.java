package com.eka.ekaPricing.pojo;

import java.util.List;

public class Stock {
	private String refNo;
	private String GMRRefNo;
	private String product;
	private double deliveredPrice;
	private double qty;	
	private List<QualityAttributes> attributes;
	private String contractItemRefNo;
	private String qtyUnit;
	private double stockPrice;
	private List<Event> event;
	private String quality;
	private double pdPrice;
	private double fixedQty;
	private String contractRefNo;
	private List<FixationObject> fixationUsed;
	private String qtyUnitId;
	private double  densityValue;
	private String massUnitId;
	private String volumeUnitId;
	private double massToVolConversionFactor;
	private String densityVolumeQtyUnitId;
	private String gmrCreationDate;
	private String itemQtyUnit;
	private String itemQtyUnitId;
	private double qtyConversionRate;
	private double stockQtyInGmr;

	public String getRefNo() {
		return refNo;
	}

	public void setRefNo(String refNo) {
		this.refNo = refNo;
	}

	public String getGMRRefNo() {
		return GMRRefNo;
	}

	public void setGMRRefNo(String gMRRefNo) {
		GMRRefNo = gMRRefNo;
	}

	public String getProduct() {
		return product;
	}

	public void setProduct(String product) {
		this.product = product;
	}

	public double getDeliveredPrice() {
		return deliveredPrice;
	}

	public void setDeliveredPrice(double deliveredPrice) {
		this.deliveredPrice = deliveredPrice;
	}

	public double getQty() {
		return qty;
	}

	public void setQty(double qty) {
		this.qty = qty;
	}	

	public List<QualityAttributes> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<QualityAttributes> attributes) {
		this.attributes = attributes;
	}

	public String getQuality() {
		return quality;
	}

	public void setQuality(String quality) {
		this.quality = quality;
	}

	public String getContractItemRefNo() {
		return contractItemRefNo;
	}

	public void setContractItemRefNo(String contractItemRefNo) {
		this.contractItemRefNo = contractItemRefNo;
	}

	public double getStockPrice() {
		return stockPrice;
	}

	public void setStockPrice(double stockPrice) {
		this.stockPrice = stockPrice;
	}

	public List<Event> getEvent() {
		return event;
	}

	public void setEvent(List<Event> event) {
		this.event = event;
	}

	public String getQtyUnit() {
		return qtyUnit;
	}

	public void setQtyUnit(String qtyUnit) {
		this.qtyUnit = qtyUnit;
	}

	public double getPdPrice() {
		return pdPrice;
	}

	public void setPdPrice(double pdPrice) {
		this.pdPrice = pdPrice;
	}

	public double getFixedQty() {
		return fixedQty;
	}

	public void setFixedQty(double fixedQty) {
		this.fixedQty = fixedQty;
	}

	public String getContractRefNo() {
		return contractRefNo;
	}

	public void setContractRefNo(String contractRefNo) {
		this.contractRefNo = contractRefNo;
	}

	public List<FixationObject> getFixationUsed() {
		return fixationUsed;
	}

	public void setFixationUsed(List<FixationObject> fixationUsed) {
		this.fixationUsed = fixationUsed;
	}

	public String getQtyUnitId() {
		return qtyUnitId;
	}

	public void setQtyUnitId(String qtyUnitId) {
		this.qtyUnitId = qtyUnitId;
	}

	public double getDensityValue() {
		return densityValue;
	}

	public void setDensityValue(double densityValue) {
		this.densityValue = densityValue;
	}

	public String getMassUnitId() {
		return massUnitId;
	}

	public void setMassUnitId(String massUnitId) {
		this.massUnitId = massUnitId;
	}

	public String getVolumeUnitId() {
		return volumeUnitId;
	}

	public void setVolumeUnitId(String volumeUnitId) {
		this.volumeUnitId = volumeUnitId;
	}

	public double getMassToVolConversionFactor() {
		return massToVolConversionFactor;
	}

	public void setMassToVolConversionFactor(double massToVolConversionFactor) {
		this.massToVolConversionFactor = massToVolConversionFactor;
	}

	public String getDensityVolumeQtyUnitId() {
		return densityVolumeQtyUnitId;
	}

	public void setDensityVolumeQtyUnitId(String densityVolumeQtyUnitId) {
		this.densityVolumeQtyUnitId = densityVolumeQtyUnitId;
	}

	public String getGmrCreationDate() {
		return gmrCreationDate;
	}

	public void setGmrCreationDate(String gmrCreationDate) {
		this.gmrCreationDate = gmrCreationDate;
	}

	public String getItemQtyUnit() {
		return itemQtyUnit;
	}

	public void setItemQtyUnit(String itemQtyUnit) {
		this.itemQtyUnit = itemQtyUnit;
	}

	public String getItemQtyUnitId() {
		return itemQtyUnitId;
	}

	public void setItemQtyUnitId(String itemQtyUnitId) {
		this.itemQtyUnitId = itemQtyUnitId;
	}

	public double getQtyConversionRate() {
		return qtyConversionRate;
	}

	public void setQtyConversionRate(double qtyConversionRate) {
		this.qtyConversionRate = qtyConversionRate;
	}

	public double getStockQtyInGmr() {
		return stockQtyInGmr;
	}

	public void setStockQtyInGmr(double stockQtyInGmr) {
		this.stockQtyInGmr = stockQtyInGmr;
	}
			
}
