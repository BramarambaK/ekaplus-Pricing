package com.eka.ekaPricing.pojo;

import org.json.JSONObject;

public class TieredFields {
	private JSONObject tieredObj;
	private Formula formula;
	private CurveDetails curveDetails;
	private double tieredQuantity;
	public JSONObject getTieredObj() {
		return tieredObj;
	}
	public void setTieredObj(JSONObject tieredObj) {
		this.tieredObj = tieredObj;
	}
	public Formula getFormula() {
		return formula;
	}
	public void setFormula(Formula formula) {
		this.formula = formula;
	}
	public CurveDetails getCurveDetails() {
		return curveDetails;
	}
	public void setCurveDetails(CurveDetails curveDetails) {
		this.curveDetails = curveDetails;
	}
	public double getTieredQuantity() {
		return tieredQuantity;
	}
	public void setTieredQuantity(double tieredQuantity) {
		this.tieredQuantity = tieredQuantity;
	}

}