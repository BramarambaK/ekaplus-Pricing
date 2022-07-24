
package com.eka.ekaPricing.service;

import static org.junit.Assert.assertEquals;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.eka.ekaPricing.helper.HolidayRuleCalculator;
import com.eka.ekaPricing.pojo.ContextInfo;
import com.eka.ekaPricing.pojo.CurveDetails;
import com.eka.ekaPricing.pojo.HolidayRuleDetails;
import com.eka.ekaPricing.pojo.PriceDifferential;
import com.eka.ekaPricing.pojo.PricingProperties;
import com.eka.ekaPricing.repository.CurveRepository;
import com.eka.ekaPricing.standalone.FXCurveFetcher;
import com.eka.ekaPricing.util.ContextProvider;

@RunWith(MockitoJUnitRunner.Silent.class)
public class CurveServiceTest {

	@InjectMocks
	public CurveService curveService;

	@Mock
	PricingProperties pricingProps;
	@Mock
	RestTemplate restTemplate;

	@Mock
	HttpServletRequest request;
	
	@Mock
	FXCurveFetcher rateFetcher;
	@Mock
	HolidayRuleCalculator holidayCalculator;
	
	List<JSONObject> holidayDataSetExpected;

	@Mock
	public ContextProvider contextProvider;

	@Mock
	ContextInfo contextInfo;
	
	@Mock
	CurveRepository curveRepository;
	
	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	
	
	@Test
	public void applyHolidayRuleTest() throws Exception {
		
		holidayDataSetExpected = new ArrayList<JSONObject>();
		JSONObject ExpectedValueObject1 = new JSONObject();

		ExpectedValueObject1.put("date", "2018-12-14T00:00");
		ExpectedValueObject1.put("dateToBeUsed", "2018-12-14T00:00");

		holidayDataSetExpected.add(ExpectedValueObject1);

		this.curveService = new CurveService();
		ReflectionTestUtils.setField(this.curveService, "collectionURL", "http://localhost:8080/");
		ReflectionTestUtils.setField(this.curveService, "holidayCollectionName", "test");
		ReflectionTestUtils.setField(this.curveService, "restTemplate", restTemplate);
		ReflectionTestUtils.setField(this.curveService, "holidayCalculator", holidayCalculator);
		ReflectionTestUtils.setField(this.curveService, "pricingProps", pricingProps);
		HolidayRuleDetails holidayRuleDtl = new HolidayRuleDetails();

		List<LocalDateTime> dateRange = new ArrayList<>();

		dateRange.add(LocalDateTime.parse("2018-12-14T00:00"));

		holidayRuleDtl.setDateRange(dateRange);

		String holidayRule = "Ignore";

		String exchangeName = "ICE";

		holidayRuleDtl.setExchangeName(exchangeName);

		holidayRuleDtl.setHolidayRule(holidayRule);

		List<Object> HolidayDateList = new ArrayList<>();

		HolidayDateList.add(holidayRuleDtl);

		String token = "12121";
		Mockito.when(contextProvider.getCurrentContext()).thenReturn(contextInfo);
		Mockito.when(contextProvider.getCurrentContext().getToken()).thenReturn(token);

		HttpHeaders headers = new HttpHeaders();

		headers.set("Authorization", token);

		HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);

		JSONObject objectData = new JSONObject();
		objectData.put("Exchange", "ICE");
		objectData.put("Commodity", "Sugar");
		objectData.put("Price Source", "Thomson Reuters");
		objectData.put("Holiday Details", "New Years Day");
		objectData.put("Holiday Date", "2018-01-01T00:00:00");
		objectData.put("Day", "Monday");

		JSONArray arrayData = new JSONArray();
		arrayData.put(objectData);

		JSONObject responseBody = new JSONObject().put("data", arrayData);

		ResponseEntity<Object> result = new ResponseEntity<Object>(responseBody.toMap(), HttpStatus.ACCEPTED);

		Mockito.doReturn(result).when(restTemplate).exchange(ArgumentMatchers.anyString(),
				ArgumentMatchers.any(org.springframework.http.HttpMethod.class), ArgumentMatchers.<HttpEntity<?>>any(),
				ArgumentMatchers.<Class<Object>>any());

		List<LocalDateTime> holidayList = new ArrayList<LocalDateTime>();

		if (result != null) {
			Object body = result.getBody();
			Object listOfMap = ((Map) body).get("data");
			for (Object excMap : ((List<Object>) listOfMap)) {
				String holidayDate = (String) ((Map) excMap).get("Holiday Date");
				String exchName = (String) ((Map) excMap).get("Exchange");
				if (exchName.equalsIgnoreCase(exchangeName)) {
					holidayList.add(LocalDateTime.parse(holidayDate));
				}
			}
		}

		Mockito.when(holidayCalculator.processHolidayRule(holidayRule, dateRange, holidayList))
				.thenReturn(holidayDataSetExpected);

		String expectedData = holidayDataSetExpected.toString();
		String actualData = curveService.applyHolidayRule(holidayRuleDtl, contextProvider);
		assertEquals(expectedData, actualData);
	}
	
	@Test
	public void applyDifferentialPricePremiumTest() throws Exception {
		double expectedPrice = 80;
		String pricePerDay = "70";
		CurveDetails curveDetails = new CurveDetails();
		PriceDifferential priceDifferential = new PriceDifferential();
		String differentialType = "Premium";
		String differentialUnit = "USD";
		double differentialValue = 10;
		priceDifferential.setDifferentialType(differentialType);
		priceDifferential.setDifferentialUnit(differentialUnit);
		priceDifferential.setDifferentialValue(differentialValue);
		
		
		List<PriceDifferential> priceDifferentialList = new ArrayList<>();
		priceDifferentialList.add(priceDifferential);
		curveDetails.setPriceDifferentialList(priceDifferentialList);
		String contractCurr = "USD";	
		
		Date tradeDate = dateFormat.parse("2018-08-20") ;
		double actualPrice = curveService.applyDifferentialPrice(curveDetails, pricePerDay, contractCurr, tradeDate);
		
		assertEquals(expectedPrice, actualPrice, 0.001);
		
	}
	
	
	@Test
	public void applyDifferentialPricePremiumCurrTest() throws Exception {
		double expectedPrice = 80;
		String pricePerDay = "70";
		CurveDetails curveDetails = new CurveDetails();
		PriceDifferential priceDifferential = new PriceDifferential();
		String differentialType = "Premium";
		String differentialUnit = "EUR";
		double differentialValue = 10;
		priceDifferential.setDifferentialType(differentialType);
		priceDifferential.setDifferentialUnit(differentialUnit);
		priceDifferential.setDifferentialValue(differentialValue);
		
		
		List<PriceDifferential> priceDifferentialList = new ArrayList<>();
		priceDifferentialList.add(priceDifferential);
		curveDetails.setPriceDifferentialList(priceDifferentialList);
		String contractCurr = "USD";		
		
		Date tradeDate = dateFormat.parse("2018-08-20") ;
		double FXRate = 100.00;
		
		
		double actualPrice = curveService.applyDifferentialPrice(curveDetails, pricePerDay, contractCurr, tradeDate);
		Mockito.when(rateFetcher.getFXRate(differentialUnit, contractCurr, tradeDate)).thenReturn(FXRate);
		assertEquals(expectedPrice, actualPrice, 0.001);
		 
	}
	@Test
	public void applyDifferentialPriceDiscountTest() throws Exception {
		double expectedPrice = 60;
		String pricePerDay = "70";
		CurveDetails curveDetails = new CurveDetails();
		PriceDifferential priceDifferential = new PriceDifferential();
		priceDifferential.setDifferentialType("Discount");
		priceDifferential.setDifferentialUnit("USD");
		priceDifferential.setDifferentialValue(10);
		List<PriceDifferential> priceDifferentialList = new ArrayList<>();
		priceDifferentialList.add(priceDifferential);
		curveDetails.setPriceDifferentialList(priceDifferentialList);
		String contractCurr = "USD";
		
		Date tradeDate = dateFormat.parse("2018-08-20") ;
		double actualPrice = curveService.applyDifferentialPrice(curveDetails, pricePerDay, contractCurr, tradeDate);
		assertEquals(expectedPrice, actualPrice, 0.001);
		
	}
	
	@Test
	public void applyDifferentialPriceS_CurveTest() throws Exception {
		double expectedPrice = 70;
		String pricePerDay = "80";
		CurveDetails curveDetails = new CurveDetails();
		PriceDifferential priceDifferential = new PriceDifferential();
		priceDifferential.setDifferentialType("S-Curve");
		priceDifferential.setDifferentialUnit("USD");
		priceDifferential.setDifferentialValue(10);
		priceDifferential.setDiffLowerThreashold(50);
		priceDifferential.setDiffUpperThreshold(70);
		List<PriceDifferential> priceDifferentialList = new ArrayList<>();
		priceDifferentialList.add(priceDifferential);
		curveDetails.setPriceDifferentialList(priceDifferentialList);
		String contractCurr = "USD";
		
		Date tradeDate = dateFormat.parse("2018-08-20") ;
		double actualPrice = curveService.applyDifferentialPrice(curveDetails, pricePerDay, contractCurr, tradeDate);
		assertEquals(expectedPrice, actualPrice, 0.001);
		
	}
	
	@Test
	public void seedCurveDataTest() {
		this.curveService = new CurveService();
		ReflectionTestUtils.setField(this.curveService, "curveSeedURL", "http://localhost:8080/");
		ReflectionTestUtils.setField(this.curveService, "restTemplate", restTemplate);
		ReflectionTestUtils.setField(this.curveService, "curveRepository", curveRepository);
		ReflectionTestUtils.setField(this.curveService, "pricingProps", pricingProps);
		
		String appName = "pricing";
		String objName = "PR";
		String token = "12121";
		Mockito.when(pricingProps.getPlatform_url()).thenReturn("172.182.1.1:91");
		Mockito.when(contextProvider.getCurrentContext()).thenReturn(contextInfo);
		Mockito.when(contextProvider.getCurrentContext().getToken()).thenReturn(token);

		HttpHeaders headers = new HttpHeaders();

		headers.set("Authorization", token);
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		headers.add("Content-Type", "application/json");		
		headers.add("clientId", "0");
		HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);

		JSONObject objectData = new JSONObject();
		objectData.put("product","Corn");
		objectData.put("price_point", "Forward");
		objectData.put("access", "Available");
		objectData.put("Derivative Type", "Futures");
		objectData.put("price_sub_type", "Forward");
		objectData.put("asset_class", "Ags");
		objectData.put("name","CBOT Corn Futures");
		objectData.put("trade_type","Exchange");
		objectData.put("published_extrapolated","Published");
		objectData.put("Publisher","Thomson Reuters");
		objectData.put("id",156);

		List<JSONObject> arrayData = new ArrayList<>();
		arrayData.add(objectData);

		JSONObject responseBody = new JSONObject().put("data", arrayData);
		
		ResponseEntity<Object> result = new ResponseEntity<Object>(responseBody.toMap(), HttpStatus.ACCEPTED);

		Mockito.doReturn(result).when(restTemplate).exchange(ArgumentMatchers.anyString(),
				ArgumentMatchers.any(org.springframework.http.HttpMethod.class), ArgumentMatchers.<HttpEntity<?>>any(),
				ArgumentMatchers.<Class<Object>>any());

		List<LocalDateTime> holidayList = new ArrayList<LocalDateTime>();
		List<JSONObject> curveList = new ArrayList<JSONObject>();
		if(result != null) {						
			Object obj = result.getBody();	
			Object data = ((Map)obj).get("data");
			for (Object ob : ((List<Object>)data)) {
				 String curveName = (String)((Map)ob).get("name");
				 String pricePoint = (String)((Map)ob).get("price_point");
				 String product = (String)((Map)ob).get("product");
				 String access = (String)((Map)ob).get("access");
				 String derivativeType = (String)((Map)ob).get("Derivative Type");
				 String priceSubType = (String)((Map)ob).get("price_sub_type");
				 String assetClass = (String)((Map)ob).get("asset_class");
				 String tradeType = (String)((Map)ob).get("trade_type");		 
				 String publisher = (String)((Map)ob).get("Publisher");			 
				 String publishedExtrapolated = (String)((Map)ob).get("published_extrapolated");				 
				 int id = (Integer)((Map)ob).get("id");
				 JSONObject dataObj = new JSONObject();				 
				 dataObj.accumulate("curveName", curveName);
				 dataObj.accumulate("pricePoint", pricePoint);
				 dataObj.accumulate("product", product);
				 dataObj.accumulate("access", access);
				 dataObj.accumulate("derivativeType", derivativeType);
				 dataObj.accumulate("priceSubType", priceSubType);
				 dataObj.accumulate("assetClass", assetClass);
				 dataObj.accumulate("tradeType", tradeType);				 
				 dataObj.accumulate("publisher", publisher);
				 dataObj.accumulate("publishedExtrapolated", publishedExtrapolated);
				 dataObj.accumulate("id", id);
				 curveList.add(dataObj);
			}			
		}
		curveService.seedCurveData(contextProvider, appName, objName);
	}
}
