package com.eka.ekaPricing.pojo;

import java.util.Date;
import java.util.List;

public class Contract {

	private String refNo;
	private String contractName;
	private String id;
	private Date startDate;
	private List<ContractItem> itemDetails;
	private int noOfItems;
	private String contractType;
	private String asOfDate;
	private String quality;
	private String product;
	private String contractRefNo;
	private String qualityName;
	private String profitCenter;
	private String strategy;
	private double contractQualityDensity;
	private String contractQualityMassUnit;
	private String contractQualityVolumeUnit;
	private String contractDraftId;
	private String incoTermId;
	private String incoTermName;
	private String locationName;
	private String locationType;
	private String isweightAvgFlag;

	public String getContractName() {
		return contractName;
	}

	public void setContractName(String contractName) {
		this.contractName = contractName;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public List<ContractItem> getContractItemList() {
		return itemDetails;
	}

	public void setContractItemList(List<ContractItem> itemDetails) {
		this.itemDetails = itemDetails;
	}

	public List<ContractItem> getItemDetails() {
		return itemDetails;
	}

	public void setItemDetails(List<ContractItem> itemDetails) {
		this.itemDetails = itemDetails;
	}

	public int getNoOfItems() {
		return noOfItems;
	}

	public void setNoOfItems(int noOfItems) {
		this.noOfItems = noOfItems;
	}

	public String getContractType() {
		return contractType;
	}

	public void setContractType(String contractType) {
		this.contractType = contractType;
	}

	public String getAsOfDate() {
		return asOfDate;
	}

	public void setAsOfDate(String asOfDate) {
		this.asOfDate = asOfDate;
	}

	public String getRefNo() {
		return refNo;
	}

	public void setRefNo(String refNo) {
		this.refNo = refNo;
	}
	
	public String getContractRefNo() {
		return contractRefNo;
	}

	public void setContractRefNo(String contractRefNo) {
		this.contractRefNo = contractRefNo;
	}

	public String getQuality() {
		return quality;
	}

	public void setQuality(String quality) {
		this.quality = quality;
	}

	public String getProduct() {
		return product;
	}

	public void setProduct(String product) {
		this.product = product;
	}

	public String getQualityName() {
		return qualityName;
	}

	public void setQualityName(String qualityName) {
		this.qualityName = qualityName;
	}

	public String getProfitCenter() {
		return profitCenter;
	}

	public void setProfitCenter(String profitCenter) {
		this.profitCenter = profitCenter;
	}

	public String getStrategy() {
		return strategy;
	}

	public void setStrategy(String strategy) {
		this.strategy = strategy;
	}

	public double getContractQualityDensity() {
		return contractQualityDensity;
	}

	public void setContractQualityDensity(double contractQualityDensity) {
		this.contractQualityDensity = contractQualityDensity;
	}

	public String getContractQualityMassUnit() {
		return contractQualityMassUnit;
	}

	public void setContractQualityMassUnit(String contractQualityMassUnit) {
		this.contractQualityMassUnit = contractQualityMassUnit;
	}

	public String getContractQualityVolumeUnit() {
		return contractQualityVolumeUnit;
	}

	public void setContractQualityVolumeUnit(String contractQualityVolumeUnit) {
		this.contractQualityVolumeUnit = contractQualityVolumeUnit;
	}

	public String getContractDraftId() {
		return contractDraftId;
	}

	public void setContractDraftId(String contractDraftId) {
		this.contractDraftId = contractDraftId;
	}

	public String getIncoTermId() {
		return incoTermId;
	}

	public void setIncoTermId(String incoTermId) {
		this.incoTermId = incoTermId;
	}

	public String getIncoTermName() {
		return incoTermName;
	}

	public void setIncoTermName(String incoTermName) {
		this.incoTermName = incoTermName;
	}

	public String getLocationName() {
		return locationName;
	}

	public void setLocationName(String locationName) {
		this.locationName = locationName;
	}

	public String getLocationType() {
		return locationType;
	}

	public void setLocationType(String locationType) {
		this.locationType = locationType;
	}

	public String getIsweightAvgFlag() {
		return isweightAvgFlag;
	}

	public void setIsweightAvgFlag(String isweightAvgFlag) {
		this.isweightAvgFlag = isweightAvgFlag;
	}

}
