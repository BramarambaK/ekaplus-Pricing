
package com.eka.ekaPricing.pojo.formulas;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.eka.ekaPricing.pojo.formulas.PriceDifferential;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Model Class - Formula - which is written for getting pricing formula details rest api's
 * 
 * @author Rushikesh Bhosale
 *
 */


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "_id",
    "contract",
    "category",
    "contractCurrencyPrice",
    "formulaExpression",
    "newFormulaExp",
    "includedCurves",
    "curves",
    "formulaName",
    "triggerValue",
    "holidayRule",
    "pricePrecision",
    "triggerPriceEnabled",
    "triggerPricing",
   
    "userId",
    "sys__createdBy",
    "refType",
    "refTypeId",
    "object",
    "sys__UUID",
    "sys__createdOn",
    "priceDifferential"
})
public class Formula {

    @JsonProperty("_id")
    private String _id;
  
    @JsonProperty("category")
    private String category;
    @JsonProperty("contractCurrencyPrice")
    private String contractCurrencyPrice;
    @JsonProperty("formulaExpression")
    private List<String> formulaExpression = null;
    @JsonProperty("newFormulaExp")
    private String newFormulaExp;
    @JsonProperty("includedCurves")
    private List<String> includedCurves = null;
    @JsonProperty("curves")
    private List<Curve> curves = null;
    @JsonProperty("formulaName")
    private String formulaName;
    @JsonProperty("triggerValue")
    private String triggerValue;
    @JsonProperty("holidayRule")
    private String holidayRule;
    @JsonProperty("pricePrecision")
    private String pricePrecision;
    @JsonProperty("triggerPriceEnabled")
    private Boolean triggerPriceEnabled;
    @JsonProperty("triggerPricing")
    private List<Object> triggerPricing = null;
   
    @JsonProperty("userId")
    private String userId;
    @JsonProperty("sys__createdBy")
    private String sysCreatedBy;
    @JsonProperty("refType")
    private String refType;
    @JsonProperty("refTypeId")
    private String refTypeId;
    @JsonProperty("object")
    private String object;
    @JsonProperty("sys__UUID")
    private String sysUUID;
    @JsonProperty("sys__createdOn")
    private String sysCreatedOn;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();
    @JsonProperty("priceDifferential")
    private List<PriceDifferential> priceDifferential = null;
   

    @JsonProperty("_id")
    public String getId() {
        return _id;
    }

    @JsonProperty("_id")
    public void setId(String id) {
        this._id = id;
    }

   

    @JsonProperty("category")
    public String getCategory() {
        return category;
    }

    @JsonProperty("category")
    public void setCategory(String category) {
        this.category = category;
    }

    @JsonProperty("contractCurrencyPrice")
    public String getContractCurrencyPrice() {
        return contractCurrencyPrice;
    }

    @JsonProperty("contractCurrencyPrice")
    public void setContractCurrencyPrice(String contractCurrencyPrice) {
        this.contractCurrencyPrice = contractCurrencyPrice;
    }

    @JsonProperty("formulaExpression")
    public List<String> getFormulaExpression() {
        return formulaExpression;
    }

    @JsonProperty("formulaExpression")
    public void setFormulaExpression(List<String> formulaExpression) {
        this.formulaExpression = formulaExpression;
    }

    @JsonProperty("newFormulaExp")
    public String getNewFormulaExp() {
        return newFormulaExp;
    }

    @JsonProperty("newFormulaExp")
    public void setNewFormulaExp(String newFormulaExp) {
        this.newFormulaExp = newFormulaExp;
    }

    @JsonProperty("includedCurves")
    public List<String> getIncludedCurves() {
        return includedCurves;
    }

    @JsonProperty("includedCurves")
    public void setIncludedCurves(List<String> includedCurves) {
        this.includedCurves = includedCurves;
    }

    @JsonProperty("curves")
    public List<Curve> getCurves() {
        return curves;
    }

    @JsonProperty("curves")
    public void setCurves(List<Curve> curves) {
        this.curves = curves;
    }

    @JsonProperty("formulaName")
    public String getFormulaName() {
        return formulaName;
    }

    @JsonProperty("formulaName")
    public void setFormulaName(String formulaName) {
        this.formulaName = formulaName;
    }

    @JsonProperty("triggerValue")
    public String getTriggerValue() {
        return triggerValue;
    }

    @JsonProperty("triggerValue")
    public void setTriggerValue(String triggerValue) {
        this.triggerValue = triggerValue;
    }

    @JsonProperty("holidayRule")
    public String getHolidayRule() {
        return holidayRule;
    }

    @JsonProperty("holidayRule")
    public void setHolidayRule(String holidayRule) {
        this.holidayRule = holidayRule;
    }

    @JsonProperty("pricePrecision")
    public String getPricePrecision() {
        return pricePrecision;
    }

    @JsonProperty("pricePrecision")
    public void setPricePrecision(String pricePrecision) {
        this.pricePrecision = pricePrecision;
    }

    @JsonProperty("triggerPriceEnabled")
    public Boolean getTriggerPriceEnabled() {
        return triggerPriceEnabled;
    }

    @JsonProperty("triggerPriceEnabled")
    public void setTriggerPriceEnabled(Boolean triggerPriceEnabled) {
        this.triggerPriceEnabled = triggerPriceEnabled;
    }

    @JsonProperty("triggerPricing")
    public List<Object> getTriggerPricing() {
        return triggerPricing;
    }

    @JsonProperty("triggerPricing")
    public void setTriggerPricing(List<Object> triggerPricing) {
        this.triggerPricing = triggerPricing;
    }

   

    @JsonProperty("userId")
    public String getUserId() {
        return userId;
    }

    @JsonProperty("userId")
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @JsonProperty("sys__createdBy")
    public String getSysCreatedBy() {
        return sysCreatedBy;
    }

    @JsonProperty("sys__createdBy")
    public void setSysCreatedBy(String sysCreatedBy) {
        this.sysCreatedBy = sysCreatedBy;
    }

    @JsonProperty("refType")
    public String getRefType() {
        return refType;
    }

    @JsonProperty("refType")
    public void setRefType(String refType) {
        this.refType = refType;
    }

    @JsonProperty("refTypeId")
    public String getRefTypeId() {
        return refTypeId;
    }

    @JsonProperty("refTypeId")
    public void setRefTypeId(String refTypeId) {
        this.refTypeId = refTypeId;
    }

    @JsonProperty("object")
    public String getObject() {
        return object;
    }

    @JsonProperty("object")
    public void setObject(String object) {
        this.object = object;
    }

    @JsonProperty("sys__UUID")
    public String getSysUUID() {
        return sysUUID;
    }

    @JsonProperty("sys__UUID")
    public void setSysUUID(String sysUUID) {
        this.sysUUID = sysUUID;
    }

    @JsonProperty("sys__createdOn")
    public String getSysCreatedOn() {
        return sysCreatedOn;
    }

    @JsonProperty("sys__createdOn")
    public void setSysCreatedOn(String sysCreatedOn) {
        this.sysCreatedOn = sysCreatedOn;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }
    
    @JsonProperty("priceDifferential")
    public List<PriceDifferential> getPriceDifferential() {
		return priceDifferential;
	}
    
    @JsonProperty("priceDifferential")
	public void setPriceDifferential(List<PriceDifferential> priceDifferential) {
		this.priceDifferential = priceDifferential;
	}

}
