package com.eka.ekaPricing.pojo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class CurveCalculatorFields {
	private LocalDate sd = null;
	private LocalDate ed = null;
	private LocalDate midEventDay = null;
	private int diffMonth = 0;
	private boolean skipMidIfEvent = false;
	private String curveCurr = null;
	private List<JSONObject> prevDataSet = new ArrayList<>();
	private JSONObject jsonObj = new JSONObject();
	private JSONArray curveArr = new JSONArray();
	private JSONArray collectionArray;
	private String curveCurrency;
	private String curveQty;
	private double originalPrice;
	private double convertedPrice;
	private double contractPrice;
	private PreviewData previewData;
	private String monthYear;
	private double priceWithoutDailyFx;
	private double avgPriceFx;

	public LocalDate getSd() {
		return sd;
	}

	public void setSd(LocalDate sd) {
		this.sd = sd;
	}

	public LocalDate getEd() {
		return ed;
	}

	public void setEd(LocalDate ed) {
		this.ed = ed;
	}

	public LocalDate getMidEventDay() {
		return midEventDay;
	}

	public void setMidEventDay(LocalDate midEventDay) {
		this.midEventDay = midEventDay;
	}

	public int getDiffMonth() {
		return diffMonth;
	}

	public void setDiffMonth(int diffMonth) {
		this.diffMonth = diffMonth;
	}

	public boolean isSkipMidIfEvent() {
		return skipMidIfEvent;
	}

	public void setSkipMidIfEvent(boolean skipMidIfEvent) {
		this.skipMidIfEvent = skipMidIfEvent;
	}

	public String getCurveCurr() {
		return curveCurr;
	}

	public void setCurveCurr(String curveCurr) {
		this.curveCurr = curveCurr;
	}

	public List<JSONObject> getPrevDataSet() {
		return prevDataSet;
	}

	public void setPrevDataSet(List<JSONObject> prevDataSet) {
		this.prevDataSet = prevDataSet;
	}

	public JSONObject getJsonObj() {
		return jsonObj;
	}

	public void setJsonObj(JSONObject jsonObj) {
		this.jsonObj = jsonObj;
	}

	public JSONArray getCurveArr() {
		return curveArr;
	}

	public void setCurveArr(JSONArray curveArr) {
		this.curveArr = curveArr;
	}

	public JSONArray getCollectionArray() {
		return collectionArray;
	}

	public void setCollectionArray(JSONArray collectionArray) {
		this.collectionArray = collectionArray;
	}

	public String getCurveCurrency() {
		return curveCurrency;
	}

	public void setCurveCurrency(String curveCurrency) {
		this.curveCurrency = curveCurrency;
	}

	public String getCurveQty() {
		return curveQty;
	}

	public void setCurveQty(String curveQty) {
		this.curveQty = curveQty;
	}

	public double getOriginalPrice() {
		return originalPrice;
	}

	public void setOriginalPrice(double originalPrice) {
		this.originalPrice = originalPrice;
	}

	public double getConvertedPrice() {
		return convertedPrice;
	}

	public void setConvertedPrice(double convertedPrice) {
		this.convertedPrice = convertedPrice;
	}

	public double getContractPrice() {
		return contractPrice;
	}

	public void setContractPrice(double contractPrice) {
		this.contractPrice = contractPrice;
	}

	public PreviewData getPreviewData() {
		return previewData;
	}

	public void setPreviewData(PreviewData previewData) {
		this.previewData = previewData;
	}

	public String getMonthYear() {
		return monthYear;
	}

	public void setMonthYear(String monthYear) {
		this.monthYear = monthYear;
	}

	public double getPriceWithoutDailyFx() {
		return priceWithoutDailyFx;
	}

	public void setPriceWithoutDailyFx(double priceWithoutDailyFx) {
		this.priceWithoutDailyFx = priceWithoutDailyFx;
	}

	public double getAvgPriceFx() {
		return avgPriceFx;
	}

	public void setAvgPriceFx(double avgPriceFx) {
		this.avgPriceFx = avgPriceFx;
	}
	
}
