package com.eka.ekaPricing.pojo;

import java.util.Date;
import java.util.List;

public class GMR {
	private String GMRId;
	private String refNo;
	private String deliveryRefNo;
	private String contractRefNo;
	private String contractItemRefNo;
	private String billOfLadingDate;
	private String estimatedBillOfLadingDate;
	private long currentGMRQty;
	private String GMRStatus;
	private String currentGMRQtyUnit;
	private String activityRefNo;
	private String activityDate;
	private long executedQty;
	private List<Stock> stocks;
	private Date shipmentPeriodFrom;
	private Date shipmentPeriodTo;
	private long movementQty;
	private List<Event> event;
	private String stockRef;
	private String operationType;
	private double massToVolumeConversionFactor;
	private String gmrCreationDate;
	private String titleTransferStatus;
	private String storageLocation;
	private String loadingLocType;
	private String loadingLocName;
	private String destinationLocType;
	private String destinationLocName;
	private String vesselName;

	public String getGMRId() {
		return GMRId;
	}

	public void setGMRId(String gMRId) {
		GMRId = gMRId;
	}

	public String getRefNo() {
		return refNo;
	}

	public void setRefNo(String refNo) {
		this.refNo = refNo;
	}

	public String getDeliveryRefNo() {
		return deliveryRefNo;
	}

	public void setDeliveryRefNo(String deliveryRefNo) {
		this.deliveryRefNo = deliveryRefNo;
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

	public String getBillOfLadingDate() {
		return billOfLadingDate;
	}

	public void setBillOfLadingDate(String billOfLadingDate) {
		this.billOfLadingDate = billOfLadingDate;
	}

	public String getEstimatedBillOfLadingDate() {
		return estimatedBillOfLadingDate;
	}

	public void setEstimatedBillOfLadingDate(String estimatedBillOfLadingDate) {
		this.estimatedBillOfLadingDate = estimatedBillOfLadingDate;
	}

	public long getCurrentGMRQty() {
		return currentGMRQty;
	}

	public void setCurrentGMRQty(long currentGMRQty) {
		this.currentGMRQty = currentGMRQty;
	}

	public String getGMRStatus() {
		return GMRStatus;
	}

	public void setGMRStatus(String gMRStatus) {
		GMRStatus = gMRStatus;
	}

	public String getCurrentGMRQtyUnit() {
		return currentGMRQtyUnit;
	}

	public void setCurrentGMRQtyUnit(String currentGMRQtyUnit) {
		this.currentGMRQtyUnit = currentGMRQtyUnit;
	}

	public String getActivityRefNo() {
		return activityRefNo;
	}

	public void setActivityRefNo(String activityRefNo) {
		this.activityRefNo = activityRefNo;
	}

	public String getActivityDate() {
		return activityDate;
	}

	public void setActivityDate(String activityDate) {
		this.activityDate = activityDate;
	}

	public long getExecutedQty() {
		return executedQty;
	}

	public void setExecutedQty(long executedQty) {
		this.executedQty = executedQty;
	}

	public Date getShipmentPeriodFrom() {
		return shipmentPeriodFrom;
	}

	public void setShipmentPeriodFrom(Date shipmentPeriodFrom) {
		this.shipmentPeriodFrom = shipmentPeriodFrom;
	}

	public Date getShipmentPeriodTo() {
		return shipmentPeriodTo;
	}

	public void setShipmentPeriodTo(Date shipmentPeriodTo) {
		this.shipmentPeriodTo = shipmentPeriodTo;
	}

	public long getMovementQty() {
		return movementQty;
	}

	public void setMovementQty(long movementQty) {
		this.movementQty = movementQty;
	}

	public List<Event> getEvent() {
		return event;
	}

	public void setEvent(List<Event> event) {
		this.event = event;
	}

	public List<Stock> getStocks() {
		return stocks;
	}

	public void setStocks(List<Stock> stocks) {
		this.stocks = stocks;
	}

	public String getStockRef() {
		return stockRef;
	}

	public void setStockRef(String stockRef) {
		this.stockRef = stockRef;
	}

	public String getOperationType() {
		return operationType;
	}

	public void setOperationType(String operationType) {
		this.operationType = operationType;
	}

	public double getMassToVolumeConversionFactor() {
		return massToVolumeConversionFactor;
	}

	public void setMassToVolumeConversionFactor(double massToVolumeConversionFactor) {
		this.massToVolumeConversionFactor = massToVolumeConversionFactor;
	}

	public String getGmrCreationDate() {
		return gmrCreationDate;
	}

	public void setGmrCreationDate(String gmrCreationDate) {
		this.gmrCreationDate = gmrCreationDate;
	}

	public String getTitleTransferStatus() {
		return titleTransferStatus;
	}

	public void setTitleTransferStatus(String titleTransferStatus) {
		this.titleTransferStatus = titleTransferStatus;
	}

	public String getStorageLocation() {
		return storageLocation;
	}

	public void setStorageLocation(String storageLocation) {
		this.storageLocation = storageLocation;
	}

	public String getLoadingLocType() {
		return loadingLocType;
	}

	public void setLoadingLocType(String loadingLocType) {
		this.loadingLocType = loadingLocType;
	}

	public String getLoadingLocName() {
		return loadingLocName;
	}

	public void setLoadingLocName(String loadingLocName) {
		this.loadingLocName = loadingLocName;
	}

	public String getDestinationLocType() {
		return destinationLocType;
	}

	public void setDestinationLocType(String destinationLocType) {
		this.destinationLocType = destinationLocType;
	}

	public String getDestinationLocName() {
		return destinationLocName;
	}

	public void setDestinationLocName(String destinationLocName) {
		this.destinationLocName = destinationLocName;
	}

	public String getVesselName() {
		return vesselName;
	}

	public void setVesselName(String vesselName) {
		this.vesselName = vesselName;
	}
	
}
