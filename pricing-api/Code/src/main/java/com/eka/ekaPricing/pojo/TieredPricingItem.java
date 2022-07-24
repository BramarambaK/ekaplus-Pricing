package com.eka.ekaPricing.pojo;

import java.util.List;

public class TieredPricingItem {
	private String internalContractRefNo;
	private String internalContractItemRefNo;
	private String splitId;
	private double splitCeiling;
	private double splitFloor;
	private String formulaId;
	private double tieredLevelPrice;
	private List<Curve> curveList;
	private Formula formulaObj;
	private double usedQtyInGMR;
	private double pricedPercentage;
	private double unpricedPercentage;
	
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

	public String getSplitId() {
		return splitId;
	}

	public void setSplitId(String splitId) {
		this.splitId = splitId;
	}

	public double getSplitCeiling() {
		return splitCeiling;
	}

	public void setSplitCeiling(double splitCeiling) {
		this.splitCeiling = splitCeiling;
	}

	public double getSplitFloor() {
		return splitFloor;
	}

	public void setSplitFloor(double splitFloor) {
		this.splitFloor = splitFloor;
	}

	public String getFormulaId() {
		return formulaId;
	}

	public void setFormulaId(String formulaId) {
		this.formulaId = formulaId;
	}

	public double getTieredLevelPrice() {
		return tieredLevelPrice;
	}

	public void setTieredLevelPrice(double tieredLevelPrice) {
		this.tieredLevelPrice = tieredLevelPrice;
	}

	public List<Curve> getCurveList() {
		return curveList;
	}

	public void setCurveList(List<Curve> curveList) {
		this.curveList = curveList;
	}

	public Formula getFormulaObj() {
		return formulaObj;
	}

	public void setFormulaObj(Formula formulaObj) {
		this.formulaObj = formulaObj;
	}

	public double getUsedQtyInGMR() {
		return usedQtyInGMR;
	}

	public void setUsedQtyInGMR(double usedQtyInGMR) {
		this.usedQtyInGMR = usedQtyInGMR;
	}

	public double getPricedPercentage() {
		return pricedPercentage;
	}

	public void setPricedPercentage(double pricedPercentage) {
		this.pricedPercentage = pricedPercentage;
	}

	public double getUnpricedPercentage() {
		return unpricedPercentage;
	}

	public void setUnpricedPercentage(double unpricedPercentage) {
		this.unpricedPercentage = unpricedPercentage;
	}
	
}
