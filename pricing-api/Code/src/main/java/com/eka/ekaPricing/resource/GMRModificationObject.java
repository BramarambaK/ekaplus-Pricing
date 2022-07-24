package com.eka.ekaPricing.resource;

import com.eka.ekaPricing.pojo.GMR;

public class GMRModificationObject {
	private GMR gmr;
	private String internalContractItemRefNo;
	public GMR getGmr() {
		return gmr;
	}
	public void setGmr(GMR gmr) {
		this.gmr = gmr;
	}
	public String getInternalContractItemRefNo() {
		return internalContractItemRefNo;
	}
	public void setInternalContractItemRefNo(String internalContractItemRefNo) {
		this.internalContractItemRefNo = internalContractItemRefNo;
	}
}
