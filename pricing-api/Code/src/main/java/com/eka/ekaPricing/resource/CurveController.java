package com.eka.ekaPricing.resource;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

import com.eka.ekaPricing.exception.PricingException;
import com.eka.ekaPricing.pojo.HolidayRuleDetails;
import com.eka.ekaPricing.service.CurveService;
import com.eka.ekaPricing.util.ContextProvider;

@CrossOrigin(origins="http://localhost:4200")
@RestController
@RequestMapping("/curve")
public class CurveController {
	@Autowired
	public CurveService curveService;

	@Autowired
	ContextProvider contextProvider;

	/**
	 * API to be called for event based type price quote rule to apply holiday rules on the pricing logic
	 * @param holidayRuleDtl
	 * @param request
	 * @return
	 * @throws PricingException 
	 */
	@PostMapping("/applyHolidayRule")
	public String applyHolidayRule(@Valid @RequestHeader("Authorization") String token,
			@RequestHeader("X-TenantID") String tenantID,@RequestHeader("X-Locale") String locale,@RequestBody HolidayRuleDetails holidayRuleDtl) throws PricingException {	
		return curveService.applyHolidayRule(holidayRuleDtl,contextProvider);
	}	

	/**
	 * API to be called to refresh the list of base curve names of futures type
	 * @param appName -pricing
	 * @param objName	- curve
	 * @param token
	 * @param tenantID
	 * @param locale
	 * @return
	 */
	@PostMapping(value = "/seedCurveData/{app_name}/{obj_name}", consumes = { "application/json" }, produces = {
	"application/json" })
	public String seedCurveData(@Valid @PathVariable("app_name") String appName, @PathVariable("obj_name") String objName, 
			@RequestHeader("Authorization") String token,@RequestHeader("X-TenantID") String tenantID,@RequestHeader("X-Locale") String locale) {		
		curveService.seedCurveData(contextProvider,appName,objName);		
		return "Base Curve Names Added/Upadted";
	}


	/**
	 * API to be called to refresh the list of base curve names of all price types
	 * @param appName -pricing
	 * @param objName	- curve
	 * @param token
	 * @param tenantID
	 * @param locale
	 * @return
	 * @throws PricingException 
	 * @throws RestClientException 
	 * @throws HttpStatusCodeException 
	 * @throws HttpClientErrorException 
	 */
	@PostMapping(value = "/seedCurveDataForAllPriceTypes/{app_name}/{obj_name}", consumes = { "application/json" }, produces = {
	"application/json" })
	public String seedCurveDataForAllPriceTypes(@Valid @PathVariable("app_name") String appName, @PathVariable("obj_name") String objName,
			@RequestHeader("Authorization") String token,@RequestHeader("X-TenantID") String tenantID,@RequestHeader("X-Locale") String locale) throws PricingException {		
		curveService.seedCurveDataForAllPriceTypes(contextProvider,appName,objName);		
		return "Base Curve Names Added/Upadted";
	}

}
