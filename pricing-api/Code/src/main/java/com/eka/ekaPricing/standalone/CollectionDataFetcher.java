package com.eka.ekaPricing.standalone;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ValueRange;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import com.eka.ekaPricing.exception.PricingException;
import com.eka.ekaPricing.pojo.Curve;
import com.eka.ekaPricing.pojo.CurveCalculatorFields;
import com.eka.ekaPricing.pojo.PricingProperties;
import com.eka.ekaPricing.util.ContextProvider;

@Component
public class CollectionDataFetcher {

	@Autowired
	RestTemplateGetRequestBodyFactory restTemplateGetWityBody;
	@Autowired
	CurveDataFetcher curveDataFetcher;
	@Autowired
	ErrorMessageFetcher messageFetcher;
	@Autowired
	ContextProvider contextProvider;

	private String accessToken = null;
	private String tenantId = null;

	@Value("${eka.client.param}")
	private String paramVals;

	@Value("${eka.pricing.collection}")
	private String collection;
	
	@Value("${eka.contract.url}")
	private String connectHost;
	
	@Value("${eka.pricing.udid}")
	private String pricingUDID;

	public String getAccessToken() {
		return accessToken;
	}

	public String getTenantId() {
		return tenantId;
	}
	final static  org.owasp.esapi.Logger logger = ESAPI.getLogger(CollectionDataFetcher.class);
	
	public JSONArray triggerRequest(Curve c, ContextProvider tenantProvider, LocalDate fromDate, LocalDate toDate, LocalDate asOf)
			throws Exception {
		String fromDay = fromDate.getDayOfWeek().toString().toLowerCase();
		if(fromDay.contains("sat")) {
			fromDate = fromDate.minusDays(1);
		}
		else if(fromDay.contains("sun")) {
			fromDate = fromDate.minusDays(2);
		}
		if(!c.getPricePoint().equalsIgnoreCase("forward")) {
			accessToken = tenantProvider.getCurrentContext().getToken();
			tenantId = tenantProvider.getCurrentContext().getTenantID();
			fromDate = fromDate.minusMonths(1);
			fromDate = fromDate.withDayOfMonth(1);
			toDate = toDate.plusDays(5);
			JSONArray arr = getData(c, fromDate, toDate, asOf, tenantProvider);
			return arr;
		}
		
		LocalDate spotDate = fromDate;
		/*
		 * Identifying the spot month and using that month we are identifying nearby months in below code
		 * For one month/year there should only be a unique prompt date.
		 * */
		if(c.getQuotedPeriod().equals("Date")) {
			String quotedPeriodDate = c.getQuotedPeriodDate();
			if(null==quotedPeriodDate || quotedPeriodDate.length()==0) {
				throw new PricingException(
						messageFetcher.fetchErrorMessage(tenantProvider, "015", new ArrayList<String>()));
			}
			int month = Integer.parseInt(quotedPeriodDate.substring(0, quotedPeriodDate.indexOf("-")));
			int year = Integer.parseInt(quotedPeriodDate.substring(quotedPeriodDate.indexOf("-")+1));
			LocalDate monthYearDate = LocalDate.of(year, month, 1);
			spotDate = monthYearDate;
			c.setMonthYear(createMonthYear(monthYearDate));
		}
		else if(StringUtils.isEmpty(c.getMonthYear())){
			c.setMonthYear(createMonthYear(spotDate));
		}
		else if(!StringUtils.isEmpty(c.getMonthYear())){
			spotDate = curveDataFetcher.calculateLocalDateFromMonthYearString(c.getMonthYear(), tenantProvider);
		}
		int numMonthNearby = 0;
		if(!c.getQuotedPeriod().equals("Date")) {
			if(c.getQuotedPeriod().equalsIgnoreCase("m")) {
				numMonthNearby = 0;
			}
			else {
				numMonthNearby = Integer.parseInt(c.getQuotedPeriod().substring(c.getQuotedPeriod().length()-1));
			}
		}
		if (tenantProvider.getCurrentContext().getToken() != null) {
			accessToken = tenantProvider.getCurrentContext().getToken();
			tenantId = tenantProvider.getCurrentContext().getTenantID();
			JSONArray arr = getData(c, toDate, fromDate, asOf, tenantProvider);
			JSONArray finalArr = new JSONArray();
			if(arr.length()==0) {
				if(!c.getQuotedPeriod().equals("Date")) {
					int spotCount = 0;
					if(spotCount > 0) {
						List<String> params = new ArrayList<String>();
						params.add(c.getCurveName());
						throw new PricingException(messageFetcher.fetchErrorMessage(tenantProvider, "032", params));
					}
					spotCount++;
					spotDate = spotDate.plusMonths(numMonthNearby);
					c.setMonthYear(createMonthYear(spotDate));
				}
			}
			else {
				if(!c.getQuotedPeriod().equals("Date") && numMonthNearby>0) {
					spotDate = spotDate.plusMonths(numMonthNearby);
					c.setMonthYear(createMonthYear(spotDate));
				}
			}
			arr = getData(c, toDate, fromDate, asOf, tenantProvider);
			finalArr = arr;
			if(arr.length()==0) {
				List<String> params = new ArrayList<String>();
				params.add(c.getMonthYear());
				throw new PricingException(messageFetcher.fetchErrorMessage(tenantProvider, "033", params));
			}
//			System.out.println("++++******   arr: "+arr.toString());
			/*
			 * Keeping track of count as we will go ahead 3 months to check for future prices. Consulted PDM team.
			 * */
			int count = 0;
			arr = new JSONArray();
//			if (c.getQuotedPeriod().contains("m+")) {
			while ((finalArr.length() == 0 || (finalArr.length() > 0 && !checkPricingBeforePrompt(
					finalArr.getJSONObject(finalArr.length() - 1).optString("Prompt Date"), toDate))
					|| (arr.length() > 0 && !checkPricingBeforePrompt(
							arr.getJSONObject(arr.length() - 1).optString("Prompt Date"), toDate)))
					&& count < 3) {
				if (arr.length() > 0) {
					finalArr = mergeIntoFinal(arr, finalArr);
					arr = new JSONArray();
					continue;
				}
				spotDate = spotDate.plusMonths(1);
				c.setMonthYear(createMonthYear(spotDate));
				arr = getData(c, toDate, fromDate, asOf, tenantProvider);
				if(arr.length() == 0) {
					count++;
				}
				
			}
			List<String> params = new ArrayList<String>();
			
			if (arr.length() == 0 && finalArr.length() == 0) {
				params.add(c.getMonthYear());
				throw new PricingException(messageFetcher.fetchErrorMessage(tenantProvider, "033", params));
			}
			if (arr.length() > 0
					&& !checkPricingBeforePrompt(arr.getJSONObject(arr.length() - 1).optString("Prompt Date"),
							toDate)) {
				params.add(c.getQuotedPeriodDate());
				throw new PricingException(messageFetcher.fetchErrorMessage(tenantProvider, "033", params));
			}
				/*
				 * Merging arr into final array for use case where pricing days are distributed
				 * over different nearby months
				 */
				finalArr = mergeIntoFinal(arr, finalArr);
//			}
			finalArr = sortJsonArr(finalArr);
//			System.out.println("++++******   finalArr: "+finalArr.toString());
			return finalArr;
		} else {
			throw new NullPointerException("token can not be empty");

		}

	}
	

	public JSONArray getData(Curve c, LocalDate toDate, LocalDate fromDate, LocalDate asOf,
			ContextProvider tenantProvider) throws Exception {
		boolean flag = false;
		if (c.getCurveName().equals("")) {
			flag = true;
		}
		PricingProperties pricingProps = contextProvider.getCurrentContext().getPricingProperties();
		String clientURL = pricingProps.getPlatform_url();
		String uri = connectHost + "/collectionmapper/" + pricingUDID+"/"+pricingUDID+"/fetchCollectionRecords";
		if (flag) {
			uri = clientURL + "/spring/smartapp/collection/data/?" + paramVals+"&limit=10000";
		}
		
		String toDateStr = constructDateStr(toDate);
		String fromDateStr = constructDateStr(fromDate);
		JSONObject fieldName = new JSONObject();
		JSONArray filterArr = new JSONArray();
		JSONObject obj1 = new JSONObject();
		obj1.put("fieldName", "Pricing Date");
		obj1.put("value", toDateStr);
		obj1.put("operator", "gt");
		JSONObject obj2 = new JSONObject();
		obj2.put("fieldName", "Pricing Date");
		obj2.put("value", fromDateStr);
		obj2.put("operator", "lt");
		JSONObject obj3 = new JSONObject();
		obj3.put("fieldName", "Instrument Name");
		obj3.put("value", c.getCurveName());
		obj3.put("operator", "eq");
		if(c.getPricePoint().equalsIgnoreCase("Spot")) {
			filterArr.put(obj1);
			filterArr.put(obj2);
		}
		filterArr.put(obj3);
		if(c.getPricePoint().equalsIgnoreCase("Forward")) {
			JSONObject monthYearObj = new JSONObject();
			monthYearObj.put("fieldName", "Month/Year");
			monthYearObj.put("value", c.getMonthYear());
			monthYearObj.put("operator", "eq");
			filterArr.put(monthYearObj);
		}
		JSONObject outerObj = new JSONObject();
		
		outerObj.put("skip", "0");
		outerObj.put("limit", "10000");
		outerObj.put("collectionName", "DS-Market Prices");
		
		fieldName.put("filter", filterArr);
		JSONArray sortArr = new JSONArray();
		JSONObject sortObj = new JSONObject();
		sortObj.put("fieldName", "Pricing Date");
		sortObj.put("direction", "ASC");
		sortArr.put(sortObj);
		fieldName.put("sort", sortArr);
		
		outerObj.put("criteria", fieldName);
		
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add("Authorization", contextProvider.getCurrentContext().getToken());
		headers.add("X-TenantID", contextProvider.getCurrentContext().getTenantID());
//		headers.add("X-Remote-User", "ekaApp");
		headers.add("ttl", "100");
		if(!StringUtils.isEmpty(contextProvider.getCurrentContext().getRequestId())) {
			headers.add("requestId", contextProvider.getCurrentContext().getRequestId());
		}
		if(!StringUtils.isEmpty(contextProvider.getCurrentContext().getSourceDeviceId())) {
			headers.add("sourceDeviceId", contextProvider.getCurrentContext().getSourceDeviceId());
		}
		HttpEntity<String> entity = new HttpEntity<String>(outerObj.toString(), headers);
		JSONArray arr = null;
		try {
			logger.info(Logger.EVENT_SUCCESS,ESAPI.encoder().encodeForHTML("checking time : "+LocalDateTime.now()));
			logger.info(Logger.EVENT_SUCCESS,ESAPI.encoder().encodeForHTML("collection fetcher major- entity: "+entity));
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
//			JSONObject obj = new JSONObject(response.getBody());
			
			arr = new JSONArray(response.getBody());
			if (arr.length() == 0 && c.getPricePoint().equalsIgnoreCase("Spot")) {
				arr = getLatestRecord(headers, fromDateStr, toDateStr, c, restTemplate, uri, asOf, contextProvider);
			}
		} catch (Exception e) {
			logger.error(Logger.EVENT_FAILURE,ESAPI.encoder().encodeForHTML("exception at " + uri + " : " + e.getMessage()));
			List<String> params = new ArrayList<String>();
			params.add("DS-Market Prices");
			throw new PricingException(
					messageFetcher.fetchErrorMessage(contextProvider, "035", params));
		}
		logger.info(Logger.EVENT_SUCCESS,ESAPI.encoder().encodeForHTML("checking time post : "+LocalDateTime.now()));
		return arr;
	}

	public JSONArray getLatestRecord(HttpHeaders headers, String fromDateStr, String toDateStr, Curve c,
			RestTemplate restTemplate, String uri, LocalDate asOf, ContextProvider tenantProvider) throws PricingException {
		JSONObject fieldName = new JSONObject();
		JSONArray filterArr = new JSONArray();
		JSONObject obj1 = new JSONObject();
		obj1.put("fieldName", "Pricing Date");
		obj1.put("value", fromDateStr);
		obj1.put("operator", "lt");
		JSONObject obj2 = new JSONObject();
		obj2.put("fieldName", "Instrument Name");
		obj2.put("value", c.getCurveName());
		obj2.put("operator", "eq");
		filterArr.put(obj1);
		filterArr.put(obj2);
		fieldName.put("filter", filterArr);
		JSONArray sortArr = new JSONArray();
		JSONObject sortObj = new JSONObject();
		sortObj.put("fieldName", "Pricing Date");
		sortObj.put("direction", "DESC");
		sortArr.put(sortObj);
		fieldName.put("sort", sortArr);
		HttpEntity<String> entity = new HttpEntity<String>(fieldName.toString(), headers);
		JSONArray arr = null;
		try {
			logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("collection fetcher latest- entity: "+entity));
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
			JSONObject obj = new JSONObject(response.getBody());
			arr = obj.getJSONArray("data");
			if(arr.length()==0) {
				return arr;
			}
			JSONArray resArr = getSameDayRecords(arr);
			return resArr;
		} catch (Exception e) {
			logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("exception at " + uri + " : " + e.getMessage()));
			throw new PricingException(
					messageFetcher.fetchErrorMessage(tenantProvider, "015", new ArrayList<String>()));
		}
	}
	
	public JSONArray getSameDayRecords(JSONArray dataArr) {
		JSONArray resArr = new JSONArray();
		JSONObject jobj = dataArr.getJSONObject(0);
		resArr.put(jobj);
		for(int i=1; i<dataArr.length(); i++) {
			JSONObject nextObj = dataArr.getJSONObject(i);
			if(!jobj.optString("Pricing Date").equals(nextObj.opt("Pricing Date"))) {
				break;
			}
			resArr.put(nextObj);
		}
		return resArr;
	}
	
	public String constructDateStr(LocalDate date) {
		String monthStr = "";
		String dateStr = "";
		if (date.getMonthValue() < 10) {
			monthStr = "0" + date.getMonthValue();
		} else {
			monthStr = "" + date.getMonthValue();
		}
		if (date.getDayOfMonth() < 10) {
			dateStr = "0" + date.getDayOfMonth();
		} else {
			dateStr = "" + date.getDayOfMonth();
		}
		String dateString = date.getYear() + "-" + monthStr + "-" + dateStr + "T00:00:00+0000";
		return dateString;
	}

	public String[] constructPromptDateArr(String dateStr) {
		int month = Integer.parseInt(dateStr.substring(0, dateStr.indexOf("-")));
		int year = Integer.parseInt(dateStr.substring(dateStr.indexOf("-")+1));
		
		LocalDate promptDate = LocalDate.of(year, month, 1);
		ValueRange range = promptDate.range(ChronoField.DAY_OF_MONTH);
		String startDate = "";
		String endDate = "";
		
		startDate = constructDateStr(promptDate);
		LocalDate maxRange = LocalDate.of(year, month, ((Long) range.getMaximum()).intValue()).plusDays(1);
		endDate = constructDateStr(maxRange);
		
		String[] rangArr = new String[2];
		rangArr[0] = startDate;
		rangArr[1] = endDate;
		
		return rangArr;
	}

	public String createMonthYear(LocalDate date) {
		String month = date.getMonth().name();
		month = month.substring(0, 3).toUpperCase();
		int year = date.getYear();
		String monthYear = month+year;
		return monthYear;
	}
	/*
	 * Method to check if pricing date is before prompt date
	 * */
	public boolean checkPricingBeforePrompt(String promptStr, LocalDate endDate) {
		
		LocalDate promptDate = convertCollectiionStrToLocalDate(promptStr);
		if(promptDate.isAfter(endDate) || promptDate.isEqual(endDate)) {
			return true;
		}
		return false;
	}
	
	public LocalDate convertCollectiionStrToLocalDate(String date) {
		if(date.contains("T")) {
			date = date.substring(0, date.indexOf("T"));
		}
		LocalDate localDate = LocalDate.parse(date);
		return localDate;
	}
	
	public JSONArray mergeIntoFinal(JSONArray arr, JSONArray finalArr) {
		for(int i=0; i<arr.length(); i++) {
			JSONObject jObj = arr.optJSONObject(i);
			finalArr.put(jObj);
		}
		return finalArr;
	}
	
	public JSONArray sortJsonArr(JSONArray jsonArr) {
		JSONArray sortedJsonArray = new JSONArray();

	    List<JSONObject> jsonValues = new ArrayList<JSONObject>();
	    for (int i = 0; i < jsonArr.length(); i++) {
	        jsonValues.add(jsonArr.getJSONObject(i));
	    }
	    Collections.sort( jsonValues, new Comparator<JSONObject>() {
	        private static final String KEY_NAME = "Pricing Date";

	        @Override
	        public int compare(JSONObject a, JSONObject b) {
	            String valA = new String();
	            String valB = new String();
	            valA = (String) a.get(KEY_NAME);
                LocalDate date1 = convertCollectiionStrToLocalDate(valA);
                valB = (String) b.get(KEY_NAME);
                LocalDate date2 = convertCollectiionStrToLocalDate(valB);
	            return date1.compareTo(date2);
	        }
	    });

	    for (int i = 0; i < jsonArr.length(); i++) {
	        sortedJsonArray.put(jsonValues.get(i));
	    }
	    
	    return sortedJsonArray;
	}
	
	public JSONArray fetchMarketPricesForV2(Curve c, ContextProvider tenantProvider, CurveCalculatorFields fieldObj,
			LocalDate asOf) throws Exception {
		accessToken = tenantProvider.getCurrentContext().getToken();
		tenantId = tenantProvider.getCurrentContext().getTenantID();
		LocalDate spotDate = fieldObj.getSd();
		if(StringUtils.isEmpty(c.getMonthYear())) {
			c.setMonthYear(createMonthYear(spotDate));
		}
		boolean isFullyForward = false;
		
		JSONArray pricesArr = getData(c, fieldObj.getSd(), fieldObj.getEd(), asOf, tenantProvider);
		if(fieldObj.getSd().isAfter(asOf) || fieldObj.getSd().isEqual(asOf)) {
			isFullyForward = true;
			if(null!=pricesArr && pricesArr.length()==0) {
				return new JSONArray();
			}
		}
		List<String> params = new ArrayList<String>();
		if (!isFullyForward && pricesArr.length() == 0) {
			params.add(fieldObj.getSd().toString());
			throw new PricingException(messageFetcher.fetchErrorMessage(tenantProvider, "032", params));	
		}
		int count = 0;
		JSONArray arr = new JSONArray();
		while ((pricesArr.length() == 0
				|| (pricesArr.length() > 0 && !checkPricingBeforePrompt(
						pricesArr.getJSONObject(pricesArr.length() - 1).optString("Prompt Date"), fieldObj.getEd()))
				|| (arr.length() > 0
						&& !checkPricingBeforePrompt(arr.getJSONObject(arr.length() - 1).optString("Prompt Date"),
								fieldObj.getEd())))
				&& count < 3) {
			if (arr.length() > 0) {
				pricesArr = mergeIntoFinal(arr, pricesArr);
				arr = new JSONArray();
				continue;
			}
			spotDate = spotDate.plusMonths(1);
			c.setMonthYear(createMonthYear(spotDate));
			arr = getData(c, fieldObj.getEd(), fieldObj.getSd(), asOf, tenantProvider);
			count++;
		}
		if (!isFullyForward && arr.length() == 0 && pricesArr.length() == 0) {
			params.add(c.getMonthYear());
			throw new PricingException(messageFetcher.fetchErrorMessage(tenantProvider, "033", params));
		}
		if (arr.length() > 0 && !checkPricingBeforePrompt(arr.getJSONObject(arr.length() - 1).optString("Prompt Date"),
				fieldObj.getEd())) {
			params.add(c.getQuotedPeriodDate());
			throw new PricingException(messageFetcher.fetchErrorMessage(tenantProvider, "033", params));
		}
		/*
		 * Merging arr into final array for use case where pricing days are distributed
		 * over different nearby months
		 */
		pricesArr = mergeIntoFinal(arr, pricesArr);
		pricesArr = sortJsonArr(pricesArr);
		return pricesArr;
	}
}