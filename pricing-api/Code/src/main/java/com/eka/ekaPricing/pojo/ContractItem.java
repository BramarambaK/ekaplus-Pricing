package com.eka.ekaPricing.pojo;

import java.util.List;

public class ContractItem {
	private String itemNo;
	private String productId;
	private String qty;
	private String itemQtyUnitId;
	private Pricing pricing;
	private String deliveryFromDate;
	private String deliveryToDate;
	private String contractType;
	private String contractRefNo;
	private String executedQuantity;
	private String openQty;
	private String qtyUnit;
	private String payInCur;
	private String product;
	private List<PDSchedule> pdSchedule;
	private List<GMR> gmrDetails;
	private String refNo;
	private String valuationInstrument;
	private String expiryDate;
	private String quality;
	private List<QualityAttributes> attributes;
	private String internalItemRefNo;
	private List<PricingComponent> pricingComponentList;
	private List<TieredPricingItem> tieredItemList;
	private double itemQty;

	public String getInternalItemRefNo() {
		return internalItemRefNo;
	}

	public void setInternalItemRefNo(String internalItemRefNo) {
		this.internalItemRefNo = internalItemRefNo;
	}

	public String getItemNo() {
		return itemNo;
	}

	public void setItemNo(String itemNo) {
		this.itemNo = itemNo;
	}

	public String getProductId() {
		return productId;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}

	public String getQty() {
		return qty;
	}

	public void setQty(String qty) {
		this.qty = qty;
	}

	public String getItemQtyUnitId() {
		return itemQtyUnitId;
	}

	public void setItemQtyUnitId(String itemQtyUnitId) {
		this.itemQtyUnitId = itemQtyUnitId;
	}

	public Pricing getPricing() {
		return pricing;
	}

	public void setPricing(Pricing pricing) {
		this.pricing = pricing;
	}

	public String getDeliveryFromDate() {
		return deliveryFromDate;
	}

	public void setDeliveryFromDate(String deliveryFromDate) {
		this.deliveryFromDate = deliveryFromDate;
	}

	public String getDeliveryToDate() {
		return deliveryToDate;
	}

	public void setDeliveryToDate(String deliveryToDate) {
		this.deliveryToDate = deliveryToDate;
	}

	public String getContractType() {
		return contractType;
	}

	public void setContractType(String contractType) {
		this.contractType = contractType;
	}

	public String getContractRefNo() {
		return contractRefNo;
	}

	public void setContractRefNo(String contractRefNo) {
		this.contractRefNo = contractRefNo;
	}

	public String getExecutedQuantity() {
		return executedQuantity;
	}

	public void setExecutedQuantity(String executedQuantity) {
		this.executedQuantity = executedQuantity;
	}

	public String getOpenQty() {
		return openQty;
	}

	public void setOpenQty(String openQty) {
		this.openQty = openQty;
	}

	public String getPayInCur() {
		return payInCur;
	}

	public void setPayInCur(String payInCur) {
		this.payInCur = payInCur;
	}

	public String getProduct() {
		return product;
	}

	public void setProduct(String product) {
		this.product = product;
	}	

	public List<PDSchedule> getPdSchedule() {
		return pdSchedule;
	}

	public void setPdSchedule(List<PDSchedule> pdSchedule) {
		this.pdSchedule = pdSchedule;
	}

	public String getRefNo() {
		return refNo;
	}

	public void setRefNo(String refNo) {
		this.refNo = refNo;
	}

	public String getValuationInstrument() {
		return valuationInstrument;
	}

	public void setValuationInstrument(String valuationInstrument) {
		this.valuationInstrument = valuationInstrument;
	}

	public String getExpiryDate() {
		return expiryDate;
	}

	public void setExpiryDate(String expiryDate) {
		this.expiryDate = expiryDate;
	}

	public String getQtyUnit() {
		return qtyUnit;
	}

	public void setQtyUnit(String qtyUnit) {
		this.qtyUnit = qtyUnit;
	}

	public List<GMR> getGmrDetails() {
		return gmrDetails;
	}

	public void setGmrDetails(List<GMR> gmrDetails) {
		this.gmrDetails = gmrDetails;
	}

	public String getQuality() {
		return quality;
	}

	public void setQuality(String quality) {
		this.quality = quality;
	}

	public List<QualityAttributes> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<QualityAttributes> attributes) {
		this.attributes = attributes;
	}

	public List<PricingComponent> getPricingComponentList() {
		return pricingComponentList;
	}

	public void setPricingComponentList(List<PricingComponent> pricingComponentList) {
		this.pricingComponentList = pricingComponentList;
	}

	public List<TieredPricingItem> getTieredItemList() {
		return tieredItemList;
	}

	public void setTieredItemList(List<TieredPricingItem> tieredItemList) {
		this.tieredItemList = tieredItemList;
	}

	public double getItemQty() {
		return itemQty;
	}

	public void setItemQty(double itemQty) {
		this.itemQty = itemQty;
	}
	
}
