package com.eka.ekaPricing.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONObject;
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

import com.eka.ekaPricing.pojo.TriggerPriceDetails;
import com.eka.ekaPricing.standalone.RestTemplateGetRequestBodyFactory;
import com.eka.ekaPricing.util.CommonValidator;

@Service
public class FetchTriggerPricingDetails implements TriggerPricingDetailsService {

	private static final Logger logger = ESAPI.getLogger(FetchTriggerPricingDetails.class);

	@Value("${eka.contract.data.api}")
	private String contractAPI;

	@Value("${eka.pricing.triggerPrice.objectUUID}")
	private String triggerPriceUUID;

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
	
	List<TriggerPriceDetails> triggerPriceDetailsList = null;
	
	private static final String DATEFORMAT = "yyyy-MM-dd'T'HH:mm:ss";
	private static final String DATEFORMAT1 = "yyyy-MM-dd";
	private static final String DATEPATTERN = "dd-MMM-yyyy";
	
	@Autowired
	public RestTemplate restTemplate;

	@Override
	public List<TriggerPriceDetails> triggerPricingDetails(HttpServletRequest request)
			throws HttpStatusCodeException,HttpClientErrorException, RestClientException, ParseException{

		logger.info(Logger.EVENT_SUCCESS,ESAPI.encoder().encodeForHTML("Pricing Formula details API- Inside pricingDetails Method - Initiated"));
		triggerPriceDetailsList = new ArrayList<>();
		ResponseEntity<String> responseEntity = null;
		String triggerPriceUrl=null;
		// need to get headers
		HttpHeaders headers = getHeaders(request);
		HttpEntity<Object> requestBody = new HttpEntity<Object>(headers);
		triggerPriceUrl = validator.cleanData(connectHost + "/data/" + pricingUDID + "/" + triggerPriceUUID);
		responseEntity = restTemplate.exchange(triggerPriceUrl, HttpMethod.GET, requestBody, String.class);
		logger.info(Logger.EVENT_SUCCESS,
				ESAPI.encoder().encodeForHTML("triggerPriceUrl" + triggerPriceUrl));

		triggerPriceDetailsList = getAllTriggerPricingDetails(responseEntity,request);
		
		logger.info(Logger.EVENT_SUCCESS,ESAPI.encoder().encodeForHTML("Pricing Formula details service getting - Ended "));

		return triggerPriceDetailsList;
	}

	public List<TriggerPriceDetails> getAllTriggerPricingDetails(ResponseEntity<String> responseEntity,HttpServletRequest request){

		logger.info(Logger.EVENT_SUCCESS,ESAPI.encoder().encodeForHTML("Pricing Formula details API- Inside getAllPricingDetails Method - Initiated"));

		TriggerPriceDetails triggerPricingDetails;

		if (null != responseEntity && HttpStatus.OK == responseEntity.getStatusCode()) {
			logger.info(Logger.EVENT_SUCCESS,ESAPI.encoder().encodeForHTML("response entity is not empty and status code is " + responseEntity.getStatusCode()));
			
			JSONArray arr = new JSONArray(responseEntity.getBody());
			
			for (int i = 0; i < arr.length(); i++) {   
		         JSONObject rec = arr.getJSONObject(i); 
					triggerPricingDetails = new TriggerPriceDetails();
					String sys__state =rec.optString("sys__state").toString();
					if(StringUtils.isEmpty(sys__state) && !StringUtils.isEmpty(rec.optString("contractRefNo"))){
						String contractItemRefNo=rec.optString("contractRefNo");
						String[] contractRefNo = contractItemRefNo.split("\\.");
						
						triggerPricingDetails.setContractRefNo(contractRefNo[0]);
						triggerPricingDetails.setContractItemRefNo(rec.optString("contractRefNo"));
						triggerPricingDetails.setFixationRefNo(rec.optString("fixationRefNo"));
						triggerPricingDetails.setFixationStage(rec.optString("execution"));
						triggerPricingDetails.setItemQty(rec.optDouble("itemQty"));
					    triggerPricingDetails.setItemQtyUnit(rec.optString("itemQtyUnit"));
						triggerPricingDetails.setItemFixedQtyAvaiable(rec.optDouble("itemFixedQtyAvailable"));
						String[] basePrice = rec.optString("basePrice").split("\\ ");
						if(!StringUtils.isEmpty(basePrice[0])) {
							double basePrice1 = Double.parseDouble(basePrice[0]);
							triggerPricingDetails.setBasePrice(basePrice1);
						}
						triggerPricingDetails.setBasePriceQtyUnit(basePrice[1]);
						String[] differential = rec.optString("differential").split("\\ ");
						if(!StringUtils.isEmpty(differential[0])) {
							double differential1 = Double.parseDouble(differential[0]);
							triggerPricingDetails.setDifferential(differential1);
						}
						triggerPricingDetails.setDifferentialQtyUnit(differential[1]);
						triggerPricingDetails.setAvgPrice(rec.optDouble("avgPrice"));
						triggerPricingDetails.setAvgPriceQtyUnit(rec.optString("priceUnitIdDisplayName")); 
						triggerPricingDetails.setTotalValue(rec.optDouble("calculatedPrice"));
						triggerPricingDetails.setQuantity(rec.optDouble("quantity"));
						triggerPricingDetails.setCpName(rec.optString("cpName"));
						triggerPricingDetails.setQuantityunitconversion(rec.optDouble("quantityunitconversion"));
						triggerPricingDetails.setInternalContractRefNo(rec.optString("internalContractRefNo"));
						triggerPricingDetails.setInternalContractItemRefNo(rec.optString("internalContractItemRefNo"));
						if(!Double.isNaN(rec.optDouble("tableFxrate"))) {
							triggerPricingDetails.setFxRate(rec.optDouble("tableFxrate"));
						}else {
							triggerPricingDetails.setFxRate(0);
						}
						String tableFixationDate = rec.optString("tableFixationDate");
						if(!StringUtils.isEmpty(tableFixationDate)) {
							 tableFixationDate=getFormattedDate1(tableFixationDate);
							triggerPricingDetails.setPricingDate(tableFixationDate);
						}else {
							triggerPricingDetails.setPricingDate(null);
						}
						if(!Double.isNaN(rec.optDouble("kfactor"))) {
							triggerPricingDetails.setKfactor(rec.optDouble("kfactor"));
						}else {
							triggerPricingDetails.setKfactor(0);
						}
					if(rec.optString("execution").equalsIgnoreCase("Post- exection")) {
						triggerPricingDetails.setGmrRefNo(rec.optString("gmrRefNoDisplayName"));
						triggerPricingDetails.setGmrQty(rec.optDouble("gmrQty"));
						triggerPricingDetails.setGmrQtyUnit(rec.optString("gmrQtyUnit"));
						triggerPricingDetails.setGmrFixedQty(rec.optDouble("itemFixedQty"));
						triggerPricingDetails.setInternalGmrRefNo(rec.optString("gmrRefNo"));
					}
					String activityDate = rec.optString("activityDate");
					if(!StringUtils.isEmpty(activityDate)) {
						activityDate=getFormattedDate(activityDate);
						triggerPricingDetails.setFixationDate(activityDate);
					}else {
						triggerPricingDetails.setFixationDate(null);
					}
						triggerPriceDetailsList.add(triggerPricingDetails);
				}
				
			}
			                 
		} else {
			logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("Error in response, responseEntity is empty " + responseEntity));

		}
		return triggerPriceDetailsList;
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
	
	public static String getFormattedDate1(String inputDate){

		String formattedDate = null;

		SimpleDateFormat dateFormat = new SimpleDateFormat(DATEFORMAT1);
		String isoDatePattern = DATEPATTERN;
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(isoDatePattern);
		Date date = null;
		try {
			date = dateFormat.parse(inputDate);
			formattedDate = simpleDateFormat.format(date);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			logger.error(Logger.EVENT_FAILURE, "Error in parsing date " + e);
			return formattedDate;
		}

		return formattedDate;
	}
	
}
