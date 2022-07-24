package com.eka.ekaPricing.pojo;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

import com.eka.ekaPricing.pojo.formulas.Formula;
import com.eka.ekaPricing.pojo.PriceFormulaDetails;

@Component("contextInfo")
public class ContextInfo {
	private String tenantID;
	private String token;
	private String locale;
	private LinkedHashMap<String, LinkedHashMap> translation;
	private LinkedHashMap<String, LinkedHashMap> policy;
	private PricingProperties pricingProperties;
	private String requestId;
	private String sourceDeviceId;
	private HttpServletRequest request;
	private LocalDate asOfDate;
	private String lookbackDate = "";
	private Map<String, String> qtyKeyMapper = new HashMap<String, String>();
	private Map<String, String> currKeyValMap = new HashMap<String, String>();
	private Map<String, String> productKeyValMap = new HashMap<String, String>();
	private Map<String, String> priceUnitMap = new HashMap<String, String>();
	private Map<String, String> qualityUnitMap = new HashMap<String, String>();
	private String origin;
	private Map<String, Formula> formulaListMap;
	private List<PriceFormulaDetails> pricingDetailsList;
	public String getTenantID() {
		return tenantID;
	}

	public void setTenantID(String tenantID) {
		this.tenantID = tenantID;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	public LinkedHashMap<String, LinkedHashMap> getTranslation() {
		return translation;
	}

	public void setTranslation(LinkedHashMap<String, LinkedHashMap> translation) {
		this.translation = translation;
	}

	public LinkedHashMap<String, LinkedHashMap> getPolicy() {
		return policy;
	}

	public void setPolicy(LinkedHashMap<String, LinkedHashMap> policy) {
		this.policy = policy;
	}

	public PricingProperties getPricingProperties() {
		return pricingProperties;
	}

	public void setPricingProperties(PricingProperties pricingProperties) {
		this.pricingProperties = pricingProperties;
	}

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}
	
	public String getSourceDeviceId() {
		return sourceDeviceId;
	}

	public void setSourceDeviceId(String sourceDeviceId) {
		this.sourceDeviceId = sourceDeviceId;
	}

	public HttpServletRequest getRequest() {
		return request;
	}

	public void setRequest(HttpServletRequest request) {
		this.request = request;
	}

	public LocalDate getAsOfDate() {
		return asOfDate;
	}

	public void setAsOfDate(LocalDate asOfDate) {
		this.asOfDate = asOfDate;
	}

	public String getLookbackDate() {
		return lookbackDate;
	}

	public void setLookbackDate(String lookbackDate) {
		this.lookbackDate = lookbackDate;
	}

	public Map<String, String> getQtyKeyMapper() {
		return qtyKeyMapper;
	}

	public void setQtyKeyMapper(Map<String, String> qtyKeyMapper) {
		this.qtyKeyMapper = qtyKeyMapper;
	}

	public Map<String, String> getCurrKeyValMap() {
		return currKeyValMap;
	}

	public void setCurrKeyValMap(Map<String, String> currKeyValMap) {
		this.currKeyValMap = currKeyValMap;
	}

	public Map<String, String> getProductKeyValMap() {
		return productKeyValMap;
	}

	public void setProductKeyValMap(Map<String, String> productKeyValMap) {
		this.productKeyValMap = productKeyValMap;
	}

	public Map<String, String> getPriceUnitMap() {
		return priceUnitMap;
	}

	public void setPriceUnitMap(Map<String, String> priceUnitMap) {
		this.priceUnitMap = priceUnitMap;
	}

	public Map<String, String> getQualityUnitMap() {
		return qualityUnitMap;
	}

	public void setQualityUnitMap(Map<String, String> qualityUnitMap) {
		this.qualityUnitMap = qualityUnitMap;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}

	public Map<String, Formula> getFormulaListMap() {
		return formulaListMap;
	}

	public void setFormulaListMap(Map<String, Formula> formulaListMap) {
		this.formulaListMap = formulaListMap;
	}

	public List<PriceFormulaDetails> getPricingDetailsList() {
		return pricingDetailsList;
	}

	public void setPricingDetailsList(List<PriceFormulaDetails> pricingDetailsList) {
		this.pricingDetailsList = pricingDetailsList;
	}
	
}
