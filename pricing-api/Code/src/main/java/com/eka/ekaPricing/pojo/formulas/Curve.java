
package com.eka.ekaPricing.pojo.formulas;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * Model Class - Curve - which is written for getting pricing formula details rest api's
 * 
 * @author Rushikesh Bhosale
 *
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "pricePoint",
    "quotedPeriod",
    "quotedPeriodDate",
    "priceType",
    "priceQuoteRule",
    "period",
    "startDate",
    "endDate",
    "event",
    "offsetType",
    "offset",
    "fxType",
    "fxInput",
    "differential",
    "curveName",
    "curveUnit"
})
public class Curve {

    @JsonProperty("pricePoint")
    private String pricePoint;
    @JsonProperty("quotedPeriod")
    private String quotedPeriod;
    @JsonProperty("quotedPeriodDate")
    private Object quotedPeriodDate;
    @JsonProperty("priceType")
    private String priceType;
    @JsonProperty("priceQuoteRule")
    private String priceQuoteRule;
    @JsonProperty("period")
    private String period;
    @JsonProperty("startDate")
    private Object startDate;
    @JsonProperty("endDate")
    private Object endDate;
    @JsonProperty("event")
    private String event;
    @JsonProperty("offsetType")
    private String offsetType;
    @JsonProperty("offset")
    private String offset;
    @JsonProperty("fxType")
    private String fxType;
    @JsonProperty("fxInput")
    private String fxInput;
    @JsonProperty("differential")
    private String differential;
    @JsonProperty("curveName")
    private String curveName;
    @JsonProperty("curveUnit")
    private String curveUnit;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("pricePoint")
    public String getPricePoint() {
        return pricePoint;
    }

    @JsonProperty("pricePoint")
    public void setPricePoint(String pricePoint) {
        this.pricePoint = pricePoint;
    }

    @JsonProperty("quotedPeriod")
    public String getQuotedPeriod() {
        return quotedPeriod;
    }

    @JsonProperty("quotedPeriod")
    public void setQuotedPeriod(String quotedPeriod) {
        this.quotedPeriod = quotedPeriod;
    }

    @JsonProperty("quotedPeriodDate")
    public Object getQuotedPeriodDate() {
        return quotedPeriodDate;
    }

    @JsonProperty("quotedPeriodDate")
    public void setQuotedPeriodDate(Object quotedPeriodDate) {
        this.quotedPeriodDate = quotedPeriodDate;
    }

    @JsonProperty("priceType")
    public String getPriceType() {
        return priceType;
    }

    @JsonProperty("priceType")
    public void setPriceType(String priceType) {
        this.priceType = priceType;
    }

    @JsonProperty("priceQuoteRule")
    public String getPriceQuoteRule() {
        return priceQuoteRule;
    }

    @JsonProperty("priceQuoteRule")
    public void setPriceQuoteRule(String priceQuoteRule) {
        this.priceQuoteRule = priceQuoteRule;
    }

    @JsonProperty("period")
    public String getPeriod() {
        return period;
    }

    @JsonProperty("period")
    public void setPeriod(String period) {
        this.period = period;
    }

    @JsonProperty("startDate")
    public Object getStartDate() {
        return startDate;
    }

    @JsonProperty("startDate")
    public void setStartDate(Object startDate) {
        this.startDate = startDate;
    }

    @JsonProperty("endDate")
    public Object getEndDate() {
        return endDate;
    }

    @JsonProperty("endDate")
    public void setEndDate(Object endDate) {
        this.endDate = endDate;
    }

    @JsonProperty("event")
    public String getEvent() {
        return event;
    }

    @JsonProperty("event")
    public void setEvent(String event) {
        this.event = event;
    }

    @JsonProperty("offsetType")
    public String getOffsetType() {
        return offsetType;
    }

    @JsonProperty("offsetType")
    public void setOffsetType(String offsetType) {
        this.offsetType = offsetType;
    }

    @JsonProperty("offset")
    public String getOffset() {
        return offset;
    }

    @JsonProperty("offset")
    public void setOffset(String offset) {
        this.offset = offset;
    }

    @JsonProperty("fxType")
    public String getFxType() {
        return fxType;
    }

    @JsonProperty("fxType")
    public void setFxType(String fxType) {
        this.fxType = fxType;
    }

    @JsonProperty("fxInput")
    public String getFxInput() {
        return fxInput;
    }

    @JsonProperty("fxInput")
    public void setFxInput(String fxInput) {
        this.fxInput = fxInput;
    }

    @JsonProperty("differential")
    public String getDifferential() {
        return differential;
    }

    @JsonProperty("differential")
    public void setDifferential(String differential) {
        this.differential = differential;
    }

    @JsonProperty("curveName")
    public String getCurveName() {
        return curveName;
    }

    @JsonProperty("curveName")
    public void setCurveName(String curveName) {
        this.curveName = curveName;
    }

    @JsonProperty("curveUnit")
	public String getCurveUnit() {
		return curveUnit;
	}

    @JsonProperty("curveUnit")
	public void setCurveUnit(String curveUnit) {
		this.curveUnit = curveUnit;
	}

	@JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
