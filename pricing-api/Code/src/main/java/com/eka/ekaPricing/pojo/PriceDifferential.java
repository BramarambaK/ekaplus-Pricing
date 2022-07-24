package com.eka.ekaPricing.pojo;

public class PriceDifferential implements Comparable<PriceDifferential>{
	
	private String differentialType;
	private double diffUpperThreshold;
	private double diffLowerThreashold;
	private double differentialValue;
	private String differentialUnit;
	
	public String getDifferentialType() {
		return differentialType;
	}
	public void setDifferentialType(String differentialType) {
		this.differentialType = differentialType;
	}
	public double getDiffUpperThreshold() {
		return diffUpperThreshold;
	}
	public void setDiffUpperThreshold(double diffUpperThreshold) {
		this.diffUpperThreshold = diffUpperThreshold;
	}
	public double getDiffLowerThreashold() {
		return diffLowerThreashold;
	}
	public void setDiffLowerThreashold(double diffLowerThreashold) {
		this.diffLowerThreashold = diffLowerThreashold;
	}
	public double getDifferentialValue() {
		return differentialValue;
	}
	public void setDifferentialValue(double differentialValue) {
		this.differentialValue = differentialValue;
	}
	public String getDifferentialUnit() {
		return differentialUnit;
	}
	public void setDifferentialUnit(String differentialUnit) {
		this.differentialUnit = differentialUnit;
	}	
	@Override
	public int compareTo(PriceDifferential obj) {
		int val = this.differentialType.compareTo(obj.differentialType);		
		return val;
	}
	

}
