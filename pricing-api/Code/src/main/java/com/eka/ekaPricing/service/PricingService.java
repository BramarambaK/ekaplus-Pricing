package com.eka.ekaPricing.service;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.eka.ekaPricing.pojo.Contract;
import com.eka.ekaPricing.pojo.ContractItem;
import com.eka.ekaPricing.pojo.Pricing;
import com.eka.ekaPricing.pojo.PricingDetails;
import com.eka.ekaPricing.pojo.formulas.Curve;
import com.eka.ekaPricing.pojo.formulas.Formula;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


/**
 * Service class which contains getting pricing formula details rest api's
 * 
 * @author Rushikesh Bhosale
 *
 */

@Service
public class PricingService implements IPricingService {

	private static final Logger logger = ESAPI.getLogger(PricingService.class);

	@Value("${eka.contract.data.api}")
	private String contractAPI;

	@Value("${eka.formula.data.api}")
	private String formulaAPI;

	@Value("${pricingType}")
	private String	pricingType;
	
	Map<String, Formula> formulaListMap = null;

	private static final String EVENTOFFSETBASED = "Event Offset Based";
	private static final String NOTAPPLICABLE = "Not Applicable";
	
	@Autowired
	public RestTemplate restTemplate;

	@SuppressWarnings("unchecked")
	@Override
	public List<PricingDetails> pricingDetails(HttpServletRequest request)
			throws HttpStatusCodeException,HttpClientErrorException, RestClientException {

		logger.debug(Logger.EVENT_SUCCESS,ESAPI.encoder().encodeForHTML("Pricing Formula details API- Inside pricingDetails Method - Initiated"));

		ResponseEntity<String> responseEntity = null;

		List<PricingDetails> pricingDetailsList = null;

		// need to get headers
		HttpHeaders headers = getHeaders(request);

		HttpEntity<Object> requestBody = new HttpEntity<Object>(headers);

		/* ----------------------- CALL TO FORMULA API STARTED------------------- */
		responseEntity = restTemplate.exchange(formulaAPI, HttpMethod.GET, requestBody, String.class);

		logger.debug(Logger.EVENT_SUCCESS,ESAPI.encoder().encodeForHTML("Calling Formula API, responseEntity is " + responseEntity));

		PricingService pricingService = new PricingService();

		formulaListMap = pricingService.getAllFormulas(responseEntity);
		
		logger.debug(Logger.EVENT_SUCCESS,ESAPI.encoder().encodeForHTML("Calling Formula API Ended"));

		/* ----------------------- CALL TO FORMULA API ENED------------------- */

		/* ----------------------- CALL TO CONTRACT API STARTED------------------- */
		
		responseEntity = restTemplate.exchange(contractAPI, HttpMethod.GET, requestBody, String.class);

		logger.debug(Logger.EVENT_SUCCESS,ESAPI.encoder().encodeForHTML("Calling contractAPI, responseEntity is " + responseEntity));

		pricingDetailsList = getAllPricingDetails(responseEntity);

		/* ----------------------- CALL TO CONTRACT API ENDED------------------- */

		logger.debug(Logger.EVENT_SUCCESS,ESAPI.encoder().encodeForHTML("Pricing Formula details service getting - Ended "));

		return pricingDetailsList;
	}

	public Map<String, Formula> getAllFormulas(ResponseEntity<String> responseEntity) {

		logger.debug(Logger.EVENT_SUCCESS,ESAPI.encoder().encodeForHTML("Pricing Formula details API- Inside getAllFormulas Method - Initiated, responseEntity is " + responseEntity));

		Type typeOfT;

		Map<String, Formula> formulaMap = null;

		if (null != responseEntity && HttpStatus.OK == responseEntity.getStatusCode()) {
			
			logger.debug(Logger.EVENT_SUCCESS,ESAPI.encoder().encodeForHTML("response entity is not empty and status code is " + responseEntity.getStatusCode()));

			typeOfT = new TypeToken<Collection<Formula>>() {
			}.getType();

			List<Formula> formulaList = new Gson().fromJson(String.valueOf(responseEntity.getBody()), typeOfT);

			logger.debug(Logger.EVENT_SUCCESS,ESAPI.encoder().encodeForHTML("The size of the formulaList is" + formulaList.size()));

			formulaMap = formulaList.stream().collect(Collectors.toMap(Formula::getId, Function.identity()));

			
		} else {
			logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("Error in response, responseEntity is empty " + responseEntity));

		}
		return formulaMap;

	}

	public List<PricingDetails> getAllPricingDetails(ResponseEntity<String> responseEntity) {

		logger.debug(Logger.EVENT_SUCCESS,ESAPI.encoder().encodeForHTML("Pricing Formula details API- Inside getAllPricingDetails Method - Initiated"));

		PricingDetails pricingDetails;
		List<PricingDetails> pricingList = new ArrayList<>();
		List<ContractItem> itemDetails;
		Pricing pricing;
		List<String> pricingFormulaIds = new ArrayList<>();
		Type typeOfT;
		Formula formulaObj;

		if (null != responseEntity && HttpStatus.OK == responseEntity.getStatusCode()) {
			logger.debug(Logger.EVENT_SUCCESS,ESAPI.encoder().encodeForHTML("response entity is not empty and status code is " + responseEntity.getStatusCode()));

			typeOfT = new TypeToken<Collection<Contract>>() {
			}.getType();

			List<Contract> contractList = new Gson().fromJson(String.valueOf(responseEntity.getBody()), typeOfT);
			logger.debug(Logger.EVENT_SUCCESS,ESAPI.encoder().encodeForHTML("The size of the contractList is" + contractList.size()));

			for (Contract contract : contractList) {
								
				itemDetails = contract.getItemDetails();
				
				if (null != itemDetails) {
					for (ContractItem contractItem : itemDetails) { 
						pricing = contractItem.getPricing();

						if (null != pricing) {
							if (null != pricing.getPriceTypeId() && pricingType.equals(pricing.getPriceTypeId()))
								logger.debug(Logger.EVENT_SUCCESS,ESAPI.encoder().encodeForHTML("pricingFormulaId is " + pricing.getPricingFormulaId()));

								pricingFormulaIds.add(pricing.getPricingFormulaId());

							if (formulaListMap.containsKey(pricing.getPricingFormulaId())) {
								logger.debug(Logger.EVENT_SUCCESS,ESAPI.encoder().encodeForHTML("formulaListMap contains pricingFormulaId -------- "));

								formulaObj = formulaListMap.get(pricing.getPricingFormulaId());

								List<Curve> curveList = formulaObj.getCurves();
								
								for (Curve curve : curveList) { 

									pricingDetails = new PricingDetails();

									pricingDetails.setItemNo(contractItem.getItemNo());
									pricingDetails.setContractItemRefNo(contract.getContractRefNo());
									pricingDetails.setFormulaExpression(formulaObj.getNewFormulaExp());

									pricingDetails.setCurveName(curve.getCurveName());
									pricingDetails.setQuotedPeriod(curve.getQuotedPeriod());
									pricingDetails.setPriceQuotaRule(curve.getPriceQuoteRule());
									pricingDetails.setPricePeriod(curve.getPeriod());
									
									if(curve.getPriceQuoteRule().equals(EVENTOFFSETBASED))
									{
										pricingDetails.setEvent(curve.getEvent());
										pricingDetails.setOffset(curve.getOffset());
										pricingDetails.setOffsetType(curve.getOffsetType());
									}
									else
									{
										pricingDetails.setEvent(NOTAPPLICABLE);
										pricingDetails.setOffset(NOTAPPLICABLE);
										pricingDetails.setOffsetType(NOTAPPLICABLE);
									}
									
									pricingList.add(pricingDetails);
								}
							}
						}
					}
				}
			}
		} else {
			logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("Error in response, responseEntity is empty " + responseEntity));

		}
		return pricingList;
	}

	public HttpHeaders getHeaders(HttpServletRequest request) {

		logger.debug(Logger.EVENT_SUCCESS,ESAPI.encoder().encodeForHTML("Pricing Formula details API- Inside getHeaders Method - Initiated"));

		HttpHeaders headers = new HttpHeaders();

		Enumeration<?> names = request.getHeaderNames();

		while (names.hasMoreElements()) {

			String name = (String) names.nextElement();
			headers.add(name, request.getHeader(name));
		}
		return headers;

	}

}
