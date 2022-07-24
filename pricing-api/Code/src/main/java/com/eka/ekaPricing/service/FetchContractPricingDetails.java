package com.eka.ekaPricing.service;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.eka.ekaPricing.pojo.Contract;
import com.eka.ekaPricing.pojo.ContractItem;
import com.eka.ekaPricing.pojo.formulas.PriceDifferential;
import com.eka.ekaPricing.pojo.PriceFormulaDetails;
import com.eka.ekaPricing.pojo.Pricing;
import com.eka.ekaPricing.pojo.StatusCollection;
import com.eka.ekaPricing.pojo.formulas.Curve;
import com.eka.ekaPricing.pojo.formulas.Formula;
import com.eka.ekaPricing.standalone.RestTemplateGetRequestBodyFactory;
import com.eka.ekaPricing.util.CommonValidator;
import com.eka.ekaPricing.util.ContextProvider;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

@Service
public class FetchContractPricingDetails implements PricingDetailsService {

	private static final Logger logger = ESAPI.getLogger(FetchContractPricingDetails.class);

	@Value("${eka.contract.data.api}")
	private String contractAPI;

	@Value("${eka.formula.data.api}")
	private String formulaAPI;

	@Value("${pricingType}")
	private String	pricingType;
	
	@Value("${eka.pricing.udid}")
	private String pricingUDID;
	
	@Value("${eka.contract.url}")
	private String connectHost;
	
	@Autowired
	CommonValidator validator;
	
	@Autowired
	RestTemplateGetRequestBodyFactory restTemplateGetRequestBody;
	
	@Autowired
	ContextProvider context;

	private static final String EVENTOFFSETBASED = "Event Offset Based";
	private static final String CUSTOMPERIODAVERAGE = "Custom Period Average";
	private static final String NOTAPPLICABLE = "Not Applicable";
	private static final String OBJECT_HEADER = "X-ObjectAction";
	private static final String COLLECTIONNAME = "Contract_Pricing_Configuration";
	private static final String COLLECTIONHEADERS = "collection_pricing_details_headers";
	private static final String COLLECTIONMAPPING = "platform_pricing_details_mapping";
	private static final String DATEFORMAT = "yyyy-MM-dd'T'HH:mm:ss";
	private static final String DATEPATTERN = "dd-MMM-yyyy";
	
	@Autowired
	public RestTemplate restTemplate;

	@Override
	public List<PriceFormulaDetails> pricingDetails(HttpServletRequest request)
			throws HttpStatusCodeException,HttpClientErrorException, RestClientException, ParseException{

		logger.info(Logger.EVENT_SUCCESS,ESAPI.encoder().encodeForHTML("Pricing Formula details API- Inside pricingDetails Method - Initiated"));
		List<PriceFormulaDetails> pricingDetailsList =new ArrayList<>();
		ResponseEntity<String> responseEntity = null;
		
		ResponseEntity<String> responseEntity1 = null;

		// need to get headers
		HttpHeaders headers = getHeaders(request);
		HttpEntity<Object> requestBody = new HttpEntity<Object>(headers);

		responseEntity = restTemplate.exchange(formulaAPI, HttpMethod.GET, requestBody, String.class);
		logger.info(Logger.EVENT_SUCCESS,
				ESAPI.encoder().encodeForHTML("formulaAPI" + formulaAPI));
		FetchContractPricingDetails pricingService = new FetchContractPricingDetails();
		context.getCurrentContext().setFormulaListMap(pricingService.getAllFormulas(responseEntity));
	
		responseEntity1 = getData(request);
		pricingDetailsList = getAllPricingDetails(responseEntity1);
		context.getCurrentContext().setPricingDetailsList(pricingDetailsList);
		//collection mapper add to collection.
		storeStatusDetailsInCollection(context.getCurrentContext().getPricingDetailsList(), headers);
		logger.info(Logger.EVENT_SUCCESS,ESAPI.encoder().encodeForHTML("Pricing Formula details service getting - Ended "));

		return pricingDetailsList;
	}

	public Map<String, Formula> getAllFormulas(ResponseEntity<String> responseEntity) {

		Type typeOfT;

		Map<String, Formula> formulaMap = null;

		if (null != responseEntity && HttpStatus.OK == responseEntity.getStatusCode()) {
			
			logger.info(Logger.EVENT_SUCCESS,ESAPI.encoder().encodeForHTML("response entity is not empty and status code is " + responseEntity.getStatusCode()));

			typeOfT = new TypeToken<Collection<Formula>>() {
			}.getType();

			List<Formula> formulaList = new Gson().fromJson(String.valueOf(responseEntity.getBody()), typeOfT);

			logger.info(Logger.EVENT_SUCCESS,ESAPI.encoder().encodeForHTML("The size of the formulaList is" + formulaList.size()));

			formulaMap = formulaList.stream().collect(Collectors.toMap(Formula::getId, Function.identity()));

			
		} else {
			logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("Error in response, responseEntity is empty " + responseEntity));

		}
		return formulaMap;

	}

	public List<PriceFormulaDetails> getAllPricingDetails(ResponseEntity<String> responseEntity) {

		logger.info(Logger.EVENT_SUCCESS,ESAPI.encoder().encodeForHTML("Pricing Formula details API- Inside getAllPricingDetails Method - Initiated"));
		List<PriceFormulaDetails> pricingDetailsList =new ArrayList<>();
		PriceFormulaDetails pricingDetails;
		List<ContractItem> itemDetails;
		Pricing pricing;
		List<String> pricingFormulaIds = new ArrayList<>();
		Type typeOfT;
		Formula formulaObj;

		if (null != responseEntity && HttpStatus.OK == responseEntity.getStatusCode()) {
			logger.info(Logger.EVENT_SUCCESS,ESAPI.encoder().encodeForHTML("response entity is not empty and status code is " + responseEntity.getStatusCode()));

			typeOfT = new TypeToken<Collection<Contract>>() {
			}.getType();

			List<Contract> contractList = new Gson().fromJson(String.valueOf(responseEntity.getBody()), typeOfT);
			int i=1;
			for (Contract contract : contractList) {
								
				itemDetails = contract.getItemDetails();
				
				if (null != itemDetails) {
					for (ContractItem contractItem : itemDetails) { 
						pricing = contractItem.getPricing();

						if (null != pricing) {
							if (null != pricing.getPriceTypeId() && pricingType.equals(pricing.getPriceTypeId()))
								logger.debug(Logger.EVENT_SUCCESS,ESAPI.encoder().encodeForHTML("pricingFormulaId is " + pricing.getPricingFormulaId()));

								pricingFormulaIds.add(pricing.getPricingFormulaId());

							if (context.getCurrentContext().getFormulaListMap().containsKey(pricing.getPricingFormulaId())) {
								logger.debug(Logger.EVENT_SUCCESS,ESAPI.encoder().encodeForHTML("formulaListMap contains pricingFormulaId -------- "));

								formulaObj = context.getCurrentContext().getFormulaListMap().get(pricing.getPricingFormulaId());
								List<PriceDifferential> priceDifferential = formulaObj.getPriceDifferential();
								List<Curve> curveList = formulaObj.getCurves();
								if(curveList.size()>0) {
									for (Curve curve : curveList) { 

									pricingDetails = new PriceFormulaDetails();
									if(null!=contract.getContractRefNo()) {
									    pricingDetails.setSeqNo(i);
										pricingDetails.setContractRefNo(contract.getContractRefNo());
										pricingDetails.setContractItemRefNo(contract.getContractRefNo()+"."+contractItem.getItemNo());
										pricingDetails.setFormulaExpression(formulaObj.getNewFormulaExp());

										pricingDetails.setCurveName(curve.getCurveName());
										pricingDetails.setPricePoint(curve.getPricePoint());
										pricingDetails.setPriceType(curve.getPriceType());
										pricingDetails.setQuotedPeriod(curve.getQuotedPeriod());
										pricingDetails.setPriceQuotaRule(curve.getPriceQuoteRule());
										pricingDetails.setCurveUnit(curve.getCurveUnit());
										
										if(!StringUtils.isEmpty(curve.getPriceQuoteRule())&& curve.getPriceQuoteRule().equals(EVENTOFFSETBASED))
										{
											pricingDetails.setPricePeriod(NOTAPPLICABLE);
											pricingDetails.setEvent(curve.getEvent());
											pricingDetails.setOffset(curve.getOffset());
											pricingDetails.setOffsetType(curve.getOffsetType());
												pricingDetails.setStartDate(null);
												pricingDetails.setEndDate(null);
										}else if(!StringUtils.isEmpty(curve.getPriceQuoteRule())&& curve.getPriceQuoteRule().equals(CUSTOMPERIODAVERAGE))
										{
											pricingDetails.setPricePeriod(NOTAPPLICABLE);
											String startDate = curve.getStartDate().toString();
											if(!StringUtils.isEmpty(startDate)) {
													startDate=getFormattedDate(startDate);
													pricingDetails.setStartDate(startDate);
												}else {
													pricingDetails.setStartDate(null);
										}
											String endDate =curve.getEndDate().toString();
											if(!StringUtils.isEmpty(endDate)) {
													endDate=getFormattedDate(endDate);
													pricingDetails.setEndDate(endDate);
												}else {
													pricingDetails.setEndDate(null);
											}
											pricingDetails.setEvent(NOTAPPLICABLE);
											pricingDetails.setOffset(NOTAPPLICABLE);
											pricingDetails.setOffsetType(NOTAPPLICABLE);
										}
										else
										{
											pricingDetails.setPricePeriod(curve.getPeriod());
											pricingDetails.setEvent(NOTAPPLICABLE);
											pricingDetails.setOffset(NOTAPPLICABLE);
											pricingDetails.setOffsetType(NOTAPPLICABLE);
												pricingDetails.setStartDate(null);
												pricingDetails.setEndDate(null);
										}
										pricingDetails.setDifferential(curve.getDifferential());
										pricingDetails.setFxType(curve.getFxType());
										pricingDetails.setFxValue(curve.getFxInput());
										pricingDetails.setDifferentialType(priceDifferential.get(0).getDifferentialType());
										pricingDetailsList.add(pricingDetails);
										}
										
									}
								}else {
									pricingDetails = new PriceFormulaDetails();
									if(null!=contract.getContractRefNo()) {
										pricingDetails.setSeqNo(i);
										pricingDetails.setContractRefNo(contract.getContractRefNo());
										pricingDetails.setContractItemRefNo(contract.getContractRefNo()+"."+contractItem.getItemNo());
										pricingDetails.setFormulaExpression(formulaObj.getNewFormulaExp());
										pricingDetails.setCurveName(NOTAPPLICABLE);
										pricingDetails.setPricePoint(NOTAPPLICABLE);
										pricingDetails.setPriceType(NOTAPPLICABLE);
										pricingDetails.setQuotedPeriod(NOTAPPLICABLE);
										pricingDetails.setPriceQuotaRule(NOTAPPLICABLE);
										pricingDetails.setCurveUnit(NOTAPPLICABLE);
										pricingDetails.setPricePeriod(NOTAPPLICABLE);
										pricingDetails.setEvent(NOTAPPLICABLE);
										pricingDetails.setOffset(NOTAPPLICABLE);
										pricingDetails.setOffsetType(NOTAPPLICABLE);
										pricingDetails.setStartDate(null);
										pricingDetails.setEndDate(null);
										pricingDetails.setDifferential(NOTAPPLICABLE);
										pricingDetails.setFxType(NOTAPPLICABLE);
										pricingDetails.setFxValue(NOTAPPLICABLE);
										pricingDetails.setDifferentialType(priceDifferential.get(0).getDifferentialType());
										pricingDetailsList.add(pricingDetails);
									}
									
								}
								i++;
							}
						}
					}
				}
			}
		} else {
			logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("Error in response, responseEntity is empty " + responseEntity));

		}
		return pricingDetailsList;
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

	public ResponseEntity<String> getData(HttpServletRequest request) {
		HttpHeaders headers = httpHeadersReplicate(request);
		Map<String, String> queryParams = new HashMap<>();
		queryParams.put("contractState", "trade");
		queryParams.put("itemDetails.pricing.priceTypeId", "FormulaPricing");
		
		headers.add(OBJECT_HEADER, "READ");
		headers.add("ttl", "0");
		HttpEntity<String> httpEntity = new HttpEntity<>(headers);
		logger.debug(Logger.EVENT_SUCCESS,
				ESAPI.encoder().encodeForHTML("Headers : " + headers));

		UriComponentsBuilder builder = UriComponentsBuilder
				.fromHttpUrl(contractAPI);

		if (queryParams != null) {
			for (Entry<String, String> entry : queryParams.entrySet()) {
				builder.queryParam(entry.getKey(), entry.getValue());

			}
		}
		String url = builder.toUriString();
		logger.info(Logger.EVENT_SUCCESS,
				ESAPI.encoder().encodeForHTML("URL " + url));

		ResponseEntity<String> responseEntity = null;
			responseEntity = restTemplateGetRequestBody.getRestTemplate().exchange(
					url, HttpMethod.GET, httpEntity, String.class);
			
			logger.info(Logger.EVENT_SUCCESS,
					ESAPI.encoder().encodeForHTML("response in contracts " + responseEntity.getStatusCode()));
		return responseEntity;
	}
	
	public HttpHeaders httpHeadersReplicate(HttpServletRequest request) {
		HttpHeaders headers = new HttpHeaders();
		Enumeration<String> headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String headerName = headerNames.nextElement();
			headers.set(headerName,validator.cleanData(request.getHeader(headerName)));
		}
		return headers;
	}
	
	public void storeStatusDetailsInCollection(List<PriceFormulaDetails> priceFormulaDetails,HttpHeaders headers) 
			throws HttpStatusCodeException, HttpClientErrorException, RestClientException{
		
		logger.info(Logger.EVENT_SUCCESS, "storeStatusDetailsInCollection method initiated ---- ");
		String statusCollectionEndPoint =  connectHost + "/collectionmapper/" + pricingUDID+"/"+pricingUDID+"/updateCollectionRecords";
		Gson gson = new Gson();
		logger.info(Logger.EVENT_SUCCESS,"statusCollectionEndPoint URI ---- "+statusCollectionEndPoint);
		StatusCollection statusCollection = new StatusCollection();
		statusCollection.setCollectionName(COLLECTIONNAME);
		statusCollection.setCollectionHeaderProperty(COLLECTIONHEADERS);
		statusCollection.setCollectionConnectMapProperty(COLLECTIONMAPPING);
		List<Map<String, Object>> serviceKeyList = new ArrayList<>();
		if(null!=priceFormulaDetails) {
			for (PriceFormulaDetails pricedetails : priceFormulaDetails) {
				Map<String, Object> serviceKeyMap = new HashMap<>();
				serviceKeyMap.put("seqNo", pricedetails.getSeqNo());
				serviceKeyMap.put("contractRefNo", pricedetails.getContractRefNo());
				serviceKeyMap.put("contractItemRefNo", pricedetails.getContractItemRefNo());
				serviceKeyMap.put("formulaExpression", pricedetails.getFormulaExpression());
				serviceKeyMap.put("curveName", pricedetails.getCurveName());
				serviceKeyMap.put("curveUnit", pricedetails.getCurveUnit());
				serviceKeyMap.put("pricePoint", pricedetails.getPricePoint());
				serviceKeyMap.put("priceType", pricedetails.getPriceType());
				serviceKeyMap.put("priceQuotaRule", pricedetails.getPriceQuotaRule());
				serviceKeyMap.put("pricePeriod", pricedetails.getPricePeriod());
				serviceKeyMap.put("startDate", pricedetails.getStartDate());
				serviceKeyMap.put("endDate", pricedetails.getEndDate());
				serviceKeyMap.put("quotedPeriod", pricedetails.getQuotedPeriod());
				serviceKeyMap.put("event", pricedetails.getEvent());
				serviceKeyMap.put("offset", pricedetails.getOffset());
				serviceKeyMap.put("offsetType", pricedetails.getOffsetType());
				serviceKeyMap.put("differential", pricedetails.getDifferential());
				serviceKeyMap.put("fxType", pricedetails.getFxType());
				serviceKeyMap.put("fxValue", pricedetails.getFxValue());
				serviceKeyMap.put("differentialType", pricedetails.getDifferentialType());
				serviceKeyList.add(serviceKeyMap);
				}
				statusCollection.setCollectionData(serviceKeyList);
		}

		ResponseEntity<Object> response = null;
		try {
			logger.info(Logger.EVENT_SUCCESS, "Time before updating PriceFormulaDetails execution");
			
			HttpEntity<Object> finalrequestBody = new HttpEntity<Object>(gson.toJson(statusCollection), headers);
			logger.info(Logger.EVENT_SUCCESS, "Making a PUT call to update status in collection  details at endpoint: "
					+ statusCollectionEndPoint + " with request payload: " + finalrequestBody.toString().length());
			response = restTemplate.exchange(statusCollectionEndPoint, HttpMethod.POST, finalrequestBody, Object.class);
			logger.info(Logger.EVENT_SUCCESS, "Time after PriceFormulaDetails contract execution");

		} catch (HttpClientErrorException he) {
			logger.error(Logger.EVENT_FAILURE,"HttpClientErrorException inside save of contract() while calling status collection API -> ",he);
		} catch (HttpStatusCodeException hs) {
			logger.error(Logger.EVENT_FAILURE,"HttpStatusCodeException inside save of contract() while calling status collection API -> ",hs);
		} catch (RestClientException ex) {
			logger.error(Logger.EVENT_FAILURE,"RestClientException inside save of contract() -> while calling status collection API ", ex);
		} catch (Exception ex) {
			logger.error(Logger.EVENT_FAILURE,"Exception inside save of contract() -> while calling status collection API ", ex);
		}

		logger.info(Logger.EVENT_SUCCESS, "response from status collection API is " + response);

	}
	
	public static String getFormattedDate(String inputDate){

		String formattedDate = null;

		SimpleDateFormat dateFormat = new SimpleDateFormat(DATEFORMAT);
		String isoDatePattern = DATEPATTERN;
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(isoDatePattern);
		Date date = null;
		try {
			date = dateFormat.parse(inputDate);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			logger.error(Logger.EVENT_FAILURE, "Error in parsing date " + e);
		}
		formattedDate = simpleDateFormat.format(date);
		
		return formattedDate;
	}
	
}
