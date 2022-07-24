package com.eka.ekaPricing.pojo;

import java.util.List;

public class GMRStatusObject {
	private String internalContractRefNo;
	private String internalContractItemRefNo;
	private String gmrRefNo;
	private String gmrId;
	private String gmrQty;
	private double gmrFixedQty;
	private double gmrUnFixedQty;
	private double gmrCancelledQty;
	private String gmrStatus;
	private List<FixationObject> fixationUsed;
	private String qtyUnitId;
	private String qtyUnitVal;
	public String getInternalContractRefNo() {
		return internalContractRefNo;
	}
	public void setInternalContractRefNo(String internalContractRefNo) {
		this.internalContractRefNo = internalContractRefNo;
	}
	public String getInternalContractItemRefNo() {
		return internalContractItemRefNo;
	}
	public void setInternalContractItemRefNo(String internalContractItemRefNo) {
		this.internalContractItemRefNo = internalContractItemRefNo;
	}
	public String getGmrRefNo() {
		return gmrRefNo;
	}
	public void setGmrRefNo(String gmrRefNo) {
		this.gmrRefNo = gmrRefNo;
	}
	public String getGmrId() {
		return gmrId;
	}
	public void setGmrId(String gmrId) {
		this.gmrId = gmrId;
	}
	public String getGmrQty() {
		return gmrQty;
	}
	public void setGmrQty(String gmrQty) {
		this.gmrQty = gmrQty;
	}
	public double getGmrFixedQty() {
		return gmrFixedQty;
	}
	public void setGmrFixedQty(double gmrFixedQty) {
		this.gmrFixedQty = gmrFixedQty;
	}
	public double getGmrUnFixedQty() {
		return gmrUnFixedQty;
	}
	public void setGmrUnFixedQty(double gmrUnFixedQty) {
		this.gmrUnFixedQty = gmrUnFixedQty;
	}
	public double getGmrCancelledQty() {
		return gmrCancelledQty;
	}
	public void setGmrCancelledQty(double gmrCancelledQty) {
		this.gmrCancelledQty = gmrCancelledQty;
	}
	public String getGmrStatus() {
		return gmrStatus;
	}
	public void setGmrStatus(String gmrStatus) {
		this.gmrStatus = gmrStatus;
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
	public String getQtyUnitVal() {
		return qtyUnitVal;
	}
	public void setQtyUnitVal(String qtyUnitVal) {
		this.qtyUnitVal = qtyUnitVal;
	}
	
}
