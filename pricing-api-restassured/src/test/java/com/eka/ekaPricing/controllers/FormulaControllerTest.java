package com.eka.ekaPricing.controllers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;

import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.springframework.util.ResourceUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import io.restassured.RestAssured;
import io.restassured.http.Method;
import io.restassured.parsing.Parser;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

public class FormulaControllerTest {

	String token = null;
	String tenant = null;
	String userName = null;
	String password = null;
	String eka_connect_host = null;
	Map<String, Object> requestPayload = new HashMap<String, Object>();

	private static final String tokenGenerationApiPath = "/api/authenticate";

	@BeforeTest
	public void setUp() throws Exception {

		Properties prop = new Properties();
		prop.load(new FileInputStream(ResourceUtils.getFile("classpath:RestAssuredTest.properties")));
		tenant = prop.getProperty("tenant");
		userName = prop.getProperty("userName");
		password = prop.getProperty("password");
		URL url = new URL((String) prop.getProperty("eka_connect_host"));
		RestAssured.baseURI = "http://" + url.getHost();
		RestAssured.port = url.getPort();
		token = authenticateUser(userName, password);
	}
	
	//@Test(enabled = true)
	public void testGetPropertyByName() {
		given().log().all().header("Authorization", token).header("X-TenantID", tenant)
				.header("Content-Type", "application/json").when().request("GET", "/property/eka_recommendation_host")
				.then().assertThat().statusCode(200)
				.body("propertyValue", is("http://172.16.0.165:4400"));
	}
//	Rest assured for collection Mapper API
	@Test(enabled = true)
	public void testCollectionMapper() throws Exception{
		String payloadString = "{\"skip\":\"0\",\"limit\":\"1000\",\"collectionName\":\"DS-Market Prices\",\"criteria\":{\"sort\":[{\"fieldName\":\"Pricing Date\",\"direction\":\"ASC\"}],\"filter\":[{\"fieldName\":\"Month/Year\",\"value\":\"NOV2019\",\"operator\":\"eq\"},{\"fieldName\":\"Instrument Name\",\"value\":\"NYMEX Light Sweet Crude Oil(WTI) Futures\",\"operator\":\"eq\"}]}}";;
		Properties prop = new Properties();
		prop.load(new FileInputStream(ResourceUtils.getFile("classpath:RestAssuredTest.properties")));
		URL url = new URL((String) prop.getProperty("eka_connect_host"));
		RestAssured.baseURI = "http://" + url.getHost();
		RestAssured.port = url.getPort();
		Response pricingResponse = callAPI(Method.POST, "/collectionmapper/84d7b167-1d9f-406d-b974-bea406a25f9a/84d7b167-1d9f-406d-b974-bea406a25f9a/fetchCollectionRecords",
				generatePayloadFromString(payloadString));
		verify202OKResponse(pricingResponse);
		JsonPath jspath = new JsonPath(pricingResponse.asString());
		Assert.assertTrue(jspath.getString("[0]").contains("Instrument Name"));
		//pricingResponse.then().assertThat().body("[0]", containsString("Instrument Name"));
	}
	
	@Test(enabled = true)
	public void testPricing() throws Exception {
//		Simple pricing call test
		String payloadString = "{\"contract\":{\"refNo\":\"5e0f0544d6018000014d168b\",\"asOfDate\":\"2020-01-03T00:00:00.000+0000\",\"itemDetails\":[{\"itemNo\":1,\"productId\":\"PDM-4751\",\"quality\":\"QAT-5694\",\"pricing\":{\"priceTypeId\":\"FormulaPricing\",\"priceUnitId\":\"PPU-7226\",\"payInCurId\":\"CM-M-7\",\"priceUnit\":\"USD/MT\"},\"deliveryFromDate\":\"2019-09-03T00:00:00.000+0000\",\"deliveryToDate\":\"2019-09-11T00:00:00.000+0000\",\"qty\":122,\"qtyUnit\":\"QUM-M-8\",\"pricingComponentList\":[]}]},\"formulaList\":[{\"formulaExpression\":\"AVG( 10 , 20 ) \",\"holidayRule\":\" \",\"pricePrecision\":\"2\",\"triggerPricing\":[],\"triggerPriceEnabled\":false,\"priceDifferential\":[{\"differentialType\":\"Discount\",\"differentialValue\":\"\",\"differentialUnit\":\"\",\"diffLowerThreashold\":\"\",\"diffUpperThreshold\":\"\"},{\"differentialType\":\"S-Curve\",\"differentialValue\":\"\",\"differentialUnit\":\"\",\"diffLowerThreashold\":\"\",\"diffUpperThreshold\":\"\"}],\"curveList\":[]}]}";
		Properties prop = new Properties();
		prop.load(new FileInputStream(ResourceUtils.getFile("classpath:RestAssuredTest.properties")));
		URL url = new URL((String) prop.getProperty("eka_pricing_host"));
		RestAssured.baseURI = "http://" + url.getHost();
		RestAssured.port = url.getPort();
		Response pricingResponse = callAPI(Method.POST, "api/pricing/formula?mode=Detailed",
				generatePayloadFromString(payloadString));
		verify200OKResponse(pricingResponse);
		pricingResponse.then().assertThat().body("contract.itemDetails[0].priceDetails.contractPrice", is("15.0"));
//		Test for failing Pricing
		payloadString = "{\"contract\":{\"refNo\":\"5ebc9f7bdc0e820001c903d5\",\"itemDetails\":[{\"itemNo\":\"1\",\"productId\":\"PDM-4721\",\"qty\":\"\",\"pricing\":{\"priceUnitId\":\"PPU-7152\",\"priceUnit\":\"USD/MT\",\"priceTypeId\":\"FormulaPricing\"},\"deliveryFromDate\":\"2019-10-01T00:00:00.000+0000\",\"deliveryToDate\":\"2019-10-05T00:00:00.000+0000\",\"qtyUnit\":\"QUM-7073\",\"quality\":\"QAT-5725\",\"pricingComponentList\":[]}],\"noOfItems\":0,\"asOfDate\":\"2020-05-14T00:00:00.000+0000\"},\"formulaList\":[{\"formulaExpression\":\"NYMEX Light Sweet Crude Oil(WTI) Futures \",\"pricePrecision\":\"2\",\"curveList\":[{\"curveName\":\"NYMEX Light Sweet Crude Oil(WTI) Futures\",\"priceType\":\"Settle Price\",\"startDate\":\"\",\"endDate\":\"\",\"pricePoint\":\"Forward\",\"quotedPeriod\":\"M+1\",\"quotedPeriodDate\":\"\",\"priceQuoteRule\":\"Event Offset Based\",\"period\":\"Delivery Period\",\"offsetType\":\"Month\",\"offset\":\"00-1-01\",\"fxInput\":0,\"fxType\":\"Fixed\",\"fxCurve\":\"\",\"differential\":\"\",\"pricedDays\":0,\"unPricedDays\":0,\"qty\":0,\"pricedQty\":0,\"unpricedQty\":0,\"calculatedPrice\":0,\"curvePrice\":0,\"missingDays\":0,\"coefficient\":0,\"priceMap\":{},\"priceDetailsMap\":{},\"priceFlagMap\":{},\"differentialUnit\":\"\",\"qtyUnitConversionFactor\":0.1364,\"version\":\"V2\",\"pricingPeriod\":\"\",\"isActualPricing\":false,\"isActualQuoted\":false,\"monthDefinition\":\"BOM\",\"offsetDays\":10}],\"triggerPriceEnabled\":false,\"triggerPricing\":[],\"priceDifferential\":[{\"differentialType\":\"Discount\",\"diffUpperThreshold\":0,\"diffLowerThreashold\":0,\"differentialValue\":0,\"differentialUnit\":\"\"},{\"differentialType\":\"S-Curve\",\"diffUpperThreshold\":0,\"diffLowerThreashold\":0,\"differentialValue\":0,\"differentialUnit\":\"\"}],\"holidayRule\":\" \"}]}";
		pricingResponse = callAPI(Method.POST, "api/pricing/formula?mode=Detailed",
				generatePayloadFromString(payloadString));
		verify500OKResponse(pricingResponse);
//		API for only holiday rule
		payloadString = "{\"contract\":{\"refNo\":\"5e3ba8ac6522050001ae4b29\",\"itemDetails\":[{\"itemNo\":\"1\",\"productId\":\"PDM-4721\",\"qty\":\"1234\",\"pricing\":{\"pricingFormulaId\":\"5e3ba9746522050001ae4b2f\",\"priceUnitId\":\"PPU-7152\",\"priceUnit\":\"USD/MT\",\"priceTypeId\":\"FormulaPricing\"},\"deliveryFromDate\":\"2019-10-01T00:00:00.000+0000\",\"deliveryToDate\":\"2019-10-23T00:00:00.000+0000\",\"qtyUnit\":\"QUM-2063\",\"quality\":\"QAT-5725\",\"internalItemRefNo\":\"PCI-11391\",\"pricingComponentList\":[]}],\"noOfItems\":0,\"asOfDate\":\"2020-02-06T00:00:00.000+0000\"},\"formulaList\":[{\"formulaExpression\":\"NYMEX Light Sweet Crude Oil(WTI) Futures \",\"pricePrecision\":\"2\",\"curveList\":[{\"curveName\":\"NYMEX Light Sweet Crude Oil(WTI) Futures\",\"priceType\":\"Settle Price\",\"startDate\":\"\",\"endDate\":\"\",\"pricePoint\":\"Forward\",\"quotedPeriod\":\"m+1\",\"quotedPeriodDate\":\"\",\"priceQuoteRule\":\"Contract Period Average\",\"period\":\"Delivery Period\",\"event\":\"\",\"offsetType\":\"Month\",\"offset\":\"Mon\",\"fxInput\":1.0,\"fxType\":\"Fixed\",\"fxCurve\":\"\",\"differential\":\"\",\"pricedDays\":0,\"unPricedDays\":0,\"qty\":0.0,\"pricedQty\":0.0,\"unpricedQty\":0.0,\"calculatedPrice\":0.0,\"curvePrice\":0.0,\"missingDays\":0,\"coefficient\":0.0,\"priceMap\":{},\"priceDetailsMap\":{},\"priceFlagMap\":{}}],\"triggerPriceEnabled\":false,\"triggerPricing\":[],\"priceDifferential\":[{\"differentialType\":\"Discount\",\"diffUpperThreshold\":0.0,\"diffLowerThreashold\":0.0,\"differentialValue\":0.0,\"differentialUnit\":\"USD\"},{\"differentialType\":\"S-Curve\",\"diffUpperThreshold\":0.0,\"diffLowerThreashold\":0.0,\"differentialValue\":0.0,\"differentialUnit\":\"\"}],\"holidayRule\":\"Next Business Day\"}]}";
		pricingResponse = callAPI(Method.POST, "api/pricing/formula?mode=Detailed",
				generatePayloadFromString(payloadString));
		verify200OKResponse(pricingResponse);
		JsonPath jspath = new JsonPath(pricingResponse.asString());
		Assert.assertEquals(jspath.getDouble("contract.itemDetails[0].priceDetails.contractPrice"), 52.56521739130435);
//		API for S-Curve
		payloadString = "{\"contract\":{\"refNo\":\"5e3ba8ac6544050001ae4b29\",\"asOfDate\":\"2020-02-06T00:00:00.000+0000\",\"itemDetails\":[{\"internalItemRefNo\":\"PCI-11391\",\"itemNo\":1,\"productId\":\"PDM-4721\",\"quality\":\"QAT-5725\",\"tolerance\":1,\"toleranceType\":\"Percentage\",\"toleranceLevel\":\"Buyer\",\"pricing\":{\"priceTypeId\":\"FormulaPricing\",\"pricingFormulaId\":\"5e3ba9746522050001ae4b2f\",\"priceDf\":0.001,\"priceUnitId\":\"PPU-7152\",\"payInCurId\":\"CM-M-7\",\"priceUnit\":\"USD/MT\"},\"shipmentMode\":\"Truck\",\"deliveryFromDate\":\"2019-10-01T00:00:00.000+0000\",\"deliveryToDate\":\"2019-10-23T00:00:00.000+0000\",\"loadingLocationGroupTypeId\":\"City\",\"originationCityId\":\"CIM-M4-589345\",\"originationCountryId\":\"CYM-M4-20589\",\"destinationLocationGroupTypeId\":\"City\",\"destinationCityId\":\"CIM-M4-589441\",\"destinationCountryId\":\"CYM-M4-20561\",\"paymentDueDate\":{\"date\":{\"year\":2020,\"month\":2,\"day\":13}},\"profitCenterId\":\"CPC-K-10412\",\"strategyAccId\":\"CSS-K-7332\",\"isOptionalOrigination\":\"N\",\"isOptionalDestination\":\"N\",\"optionalLoadingDetails\":[],\"optionalDischargeDetails\":[],\"estimates\":[],\"isDeleted\":false,\"unweighedPiPctType\":\"Percentage\",\"latePaymentInterestDetails\":{\"physicalItemInterestId\":\"PII-7836\",\"interestRateType\":\"Variable\",\"variableType\":\"LIBORUSD\",\"variableTypeText\":\"\",\"interestRate\":0,\"isCompounding\":\"Y\"},\"taxScheduleCountryId\":\"CYM-M-40\",\"taxScheduleId\":\"TSS-20\",\"isOptionalFieldsEnabled\":false,\"qty\":1234,\"qtyUnit\":\"QUM-2063\",\"pricingComponentList\":[]}]},\"formulaList\":[{\"formulaExpression\":\"NYMEX Light Sweet Crude Oil(WTI) Futures \",\"holidayRule\":\"Next Business Day\",\"pricePrecision\":\"2\",\"triggerPricing\":[],\"triggerPriceEnabled\":false,\"priceDifferential\":[{\"differentialType\":\"Discount\",\"differentialValue\":\"10\",\"differentialUnit\":\"USD\",\"diffLowerThreashold\":\"\",\"diffUpperThreshold\":\"\"},{\"differentialType\":\"S-Curve\",\"differentialValue\":\"\",\"differentialUnit\":\"\",\"diffLowerThreashold\":\"55\",\"diffUpperThreshold\":\"60\"}],\"curveList\":[{\"pricePoint\":\"Forward\",\"quotedPeriod\":\"m+1\",\"quotedPeriodDate\":\"\",\"priceType\":\"Settle Price\",\"priceQuoteRule\":\"Contract Period Average\",\"period\":\"Delivery Period\",\"startDate\":\"\",\"endDate\":\"\",\"event\":\"\",\"offsetType\":\"Month\",\"offset\":\"Mon\",\"fxType\":\"Fixed\",\"fxInput\":1,\"fxCurve\":\"\",\"differential\":\"\",\"curveName\":\"NYMEX Light Sweet Crude Oil(WTI) Futures\"}]}]}";
		pricingResponse = callAPI(Method.POST, "api/pricing/formula?mode=Detailed",
				generatePayloadFromString(payloadString));
		verify200OKResponse(pricingResponse);
		jspath = new JsonPath(pricingResponse.asString());
		Assert.assertEquals(jspath.getDouble("contract.itemDetails[0].priceDetails.contractPrice"), 55.0);
//		API for Discount
		payloadString = "{\"contract\":{\"refNo\":\"5e3ba8ac6652050001ae4b29\",\"itemDetails\":[{\"itemNo\":\"1\",\"productId\":\"PDM-4721\",\"qty\":\"1234\",\"pricing\":{\"pricingFormulaId\":\"5e3ba9746522050001ae4b2f\",\"priceUnitId\":\"PPU-7152\",\"priceUnit\":\"USD/MT\",\"priceTypeId\":\"FormulaPricing\"},\"deliveryFromDate\":\"2019-10-01T00:00:00.000+0000\",\"deliveryToDate\":\"2019-10-23T00:00:00.000+0000\",\"qtyUnit\":\"QUM-2063\",\"quality\":\"QAT-5725\",\"internalItemRefNo\":\"PCI-11391\",\"pricingComponentList\":[]}],\"noOfItems\":0,\"asOfDate\":\"2020-02-06T00:00:00.000+0000\"},\"formulaList\":[{\"formulaExpression\":\"NYMEX Light Sweet Crude Oil(WTI) Futures \",\"pricePrecision\":\"2\",\"curveList\":[{\"curveName\":\"NYMEX Light Sweet Crude Oil(WTI) Futures\",\"priceType\":\"Settle Price\",\"startDate\":\"\",\"endDate\":\"\",\"pricePoint\":\"Forward\",\"quotedPeriod\":\"m+1\",\"quotedPeriodDate\":\"\",\"priceQuoteRule\":\"Contract Period Average\",\"period\":\"Delivery Period\",\"event\":\"\",\"offsetType\":\"Month\",\"offset\":\"Mon\",\"fxInput\":1.0,\"fxType\":\"Fixed\",\"fxCurve\":\"\",\"differential\":\"\",\"pricedDays\":0,\"unPricedDays\":0,\"qty\":0.0,\"pricedQty\":0.0,\"unpricedQty\":0.0,\"calculatedPrice\":0.0,\"curvePrice\":0.0,\"missingDays\":0,\"coefficient\":0.0,\"priceMap\":{},\"priceDetailsMap\":{},\"priceFlagMap\":{}}],\"triggerPriceEnabled\":false,\"triggerPricing\":[],\"priceDifferential\":[{\"differentialType\":\"Discount\",\"diffUpperThreshold\":0.0,\"diffLowerThreashold\":0.0,\"differentialValue\":0.0,\"differentialUnit\":\"USD\"},{\"differentialType\":\"S-Curve\",\"diffUpperThreshold\":60.0,\"diffLowerThreashold\":55.0,\"differentialValue\":0.0,\"differentialUnit\":\"\"}],\"holidayRule\":\"Next Business Day\"}]}";
		pricingResponse = callAPI(Method.POST, "api/pricing/formula?mode=Detailed",
				generatePayloadFromString(payloadString));
		verify200OKResponse(pricingResponse);
		jspath = new JsonPath(pricingResponse.asString());
		Assert.assertEquals(jspath.getDouble("contract.itemDetails[0].priceDetails.contractPrice"), 55.0);
		
//		Test for pricing API at EOD
		payloadString = "{\"contract\":{\"refNo\":\"PCM-10857\",\"itemDetails\":[{\"qty\":\"10000\",\"pricing\":{\"priceType\":\"FormulaPricing\",\"priceUnit\":\"USD/MT\",\"internalPriceUnitId\":\"PPU-7152\"},\"deliveryFromDate\":\"2020-01-08T00:00:00+0530\",\"deliveryToDate\":\"2020-01-21T00:00:00+0530\",\"qtyUnit\":\"MT\",\"pdSchedule\":[],\"refNo\":\"PCI-11286\",\"expiryDate\":\"2020-01-09T15:47:44+0530\",\"quality\":\"Gasoil 10ppm\"}],\"noOfItems\":0,\"asOfDate\":\"2020-01-09T15:47:43+0530\"}}";
		pricingResponse = callAPI(Method.POST, "api/pricing/formula?mode=Detailed",
				generatePayloadFromString(payloadString));
		verify200OKResponse(pricingResponse);
		pricingResponse.then().assertThat().body("contract.itemDetails[0].priceDetails.contractPrice", is("52.5"));
//		Test for GMR pricing call along with holidayRule, trigger Price and discount/S-curve
		payloadString = "{\"contract\":{\"refNo\":\"PCM-10971\",\"itemDetails\":[{\"qty\":\"1234\",\"pricing\":{\"priceType\":\"FormulaPricing\",\"priceUnit\":\"USD/MT\",\"internalPriceUnitId\":\"PPU-7152\"},\"deliveryFromDate\":\"2019-10-01T00:00:00+0530\",\"deliveryToDate\":\"2019-10-23T00:00:00+0530\",\"qtyUnit\":\"BBL\",\"pdSchedule\":[],\"gmrDetails\":[{\"refNo\":\"GMR-9872\",\"currentGMRQty\":0,\"executedQty\":0,\"stocks\":[{\"refNo\":\"GRD-9784\",\"deliveredPrice\":0.0,\"qty\":2.0,\"qtyUnit\":\"QUM-2063\",\"stockPrice\":0.0,\"event\":[{\"name\":\"Bill of Lading\",\"date\":\"2020-02-06T00:00:00+0530\"}],\"quality\":\"Gasoil 10ppm\",\"pdPrice\":0.0}],\"movementQty\":0}],\"refNo\":\"PCI-11391\",\"expiryDate\":\"2020-02-06T16:23:20+0530\",\"quality\":\"Gasoil 10ppm\"}],\"noOfItems\":0,\"asOfDate\":\"2020-02-06T16:23:20+0530\"}}";
		pricingResponse = callAPI(Method.POST, "api/pricing/formula?mode=Detailed",
				generatePayloadFromString(payloadString));
		verify200OKResponse(pricingResponse);
		jspath = new JsonPath(pricingResponse.asString());
//		Test for trigger price - new functionality
		payloadString = "{\"contract\":{\"refNo\":\"PCM-11431\",\"itemDetails\":[{\"qty\":\"1200\",\"pricing\":{\"priceType\":\"FormulaPricing\",\"priceUnit\":\"USD/MT\",\"internalPriceUnitId\":\"PPU-7226\"},\"deliveryFromDate\":\"2020-04-21T00:00:00+0530\",\"deliveryToDate\":\"2020-04-29T00:00:00+0530\",\"qtyUnit\":\"MT\",\"pdSchedule\":[],\"gmrDetails\":[],\"refNo\":\"PCI-11879\",\"expiryDate\":\"2020-05-15T12:14:02+0530\",\"quality\":\"Brent Light\"}],\"noOfItems\":0,\"asOfDate\":\"2020-05-15T12:14:02+0530\"}}";
		pricingResponse = callAPI(Method.POST, "api/pricing/formula?mode=Detailed",
				generatePayloadFromString(payloadString));
		verify200OKResponse(pricingResponse);
		pricingResponse.then().assertThat().body("contract.itemDetails[0].priceDetails.contractPrice", is("53.0775"));
		Assert.assertEquals(jspath.getDouble("contract.itemDetails[0].gmrDetails[0].price"), 51.595654);
//		Test for tiered pricing, 3 splits with different formula
		payloadString = "{\"contract\":{\"refNo\":\"PCM-11538\",\"itemDetails\":[{\"qty\":\"1000\",\"pricing\":{\"priceType\":\"FormulaPricing\",\"priceUnit\":\"USD/MT\",\"internalPriceUnitId\":\"PPU-7152\"},\"deliveryFromDate\":\"2020-05-15T00:00:00+0530\",\"deliveryToDate\":\"2020-05-28T00:00:00+0530\",\"qtyUnit\":\"MT\",\"pdSchedule\":[],\"gmrDetails\":[],\"refNo\":\"PCI-11990\",\"expiryDate\":\"2020-05-15T13:03:08+0530\",\"quality\":\"Gasoil 10ppm\"}],\"noOfItems\":0,\"asOfDate\":\"2020-05-15T13:03:08+0530\"}}";
		pricingResponse = callAPI(Method.POST, "api/pricing/formula?mode=Detailed",
				generatePayloadFromString(payloadString));
		verify200OKResponse(pricingResponse);
		pricingResponse.then().assertThat().body("contract.itemDetails[0].priceDetails.contractPrice", is("92.87096774193549"));
//		Test for tiered pricing - with GMR
		payloadString = "{\"contract\":{\"refNo\":\"PCM-11538\",\"itemDetails\":[{\"qty\":\"1000\",\"pricing\":{\"priceType\":\"FormulaPricing\",\"priceUnit\":\"USD/MT\",\"internalPriceUnitId\":\"PPU-7152\"},\"deliveryFromDate\":\"2020-05-15T00:00:00+0530\",\"deliveryToDate\":\"2020-05-28T00:00:00+0530\",\"qtyUnit\":\"MT\",\"pdSchedule\":[],\"gmrDetails\":[{\"refNo\":\"GMR-10244\",\"currentGMRQty\":0,\"executedQty\":0,\"stocks\":[{\"refNo\":\"GRD-10070\",\"deliveredPrice\":0,\"qty\":100,\"qtyUnit\":\"QUM-2063\",\"stockPrice\":0,\"event\":[{\"name\":\"Bill of Lading\",\"date\":\"2020-05-15T00:00:00+0530\"}],\"quality\":\"Gasoil 10ppm\",\"pdPrice\":0}],\"movementQty\":0}],\"refNo\":\"PCI-11990\",\"expiryDate\":\"2020-05-15T15:55:46+0530\",\"quality\":\"Gasoil 10ppm\"}],\"noOfItems\":0,\"asOfDate\":\"2020-05-15T15:55:46+0530\"}}";
		pricingResponse = callAPI(Method.POST, "api/pricing/formula?mode=Detailed",
				generatePayloadFromString(payloadString));
		verify200OKResponse(pricingResponse);
		pricingResponse.then().assertThat().body("contract.itemDetails[0].priceDetails.contractPrice", is("92.87096774193549"));
//		Formula version: 2, custom period average
		payloadString = "{\"contract\":{\"refNo\":\"5ebc9f7bdc0e820001c903d5\",\"itemDetails\":[{\"itemNo\":\"1\",\"productId\":\"PDM-4721\",\"qty\":\"1000\",\"pricing\":{\"priceUnitId\":\"PPU-7152\",\"priceUnit\":\"USD/MT\",\"priceTypeId\":\"FormulaPricing\"},\"deliveryFromDate\":\"2019-10-01T00:00:00.000+0000\",\"deliveryToDate\":\"2019-10-05T00:00:00.000+0000\",\"qtyUnit\":\"QUM-7073\",\"quality\":\"QAT-5725\",\"pricingComponentList\":[]}],\"noOfItems\":0,\"asOfDate\":\"2020-05-14T00:00:00.000+0000\"},\"formulaList\":[{\"formulaExpression\":\"NYMEX Light Sweet Crude Oil(WTI) Futures \",\"pricePrecision\":\"2\",\"curveList\":[{\"curveName\":\"NYMEX Light Sweet Crude Oil(WTI) Futures\",\"priceType\":\"Settle Price\",\"startDate\":\"\",\"endDate\":\"\",\"pricePoint\":\"Forward\",\"quotedPeriod\":\"M+1\",\"quotedPeriodDate\":\"\",\"priceQuoteRule\":\"Event Offset Based\",\"period\":\"Delivery Period\",\"offsetType\":\"Month\",\"offset\":\"00-1-01\",\"fxInput\":0,\"fxType\":\"Fixed\",\"fxCurve\":\"\",\"differential\":\"\",\"pricedDays\":0,\"unPricedDays\":0,\"qty\":0,\"pricedQty\":0,\"unpricedQty\":0,\"calculatedPrice\":0,\"curvePrice\":0,\"missingDays\":0,\"coefficient\":0,\"priceMap\":{},\"priceDetailsMap\":{},\"priceFlagMap\":{},\"differentialUnit\":\"\",\"qtyUnitConversionFactor\":0.1364,\"version\":\"V2\",\"pricingPeriod\":\"\",\"isActualPricing\":false,\"isActualQuoted\":false,\"monthDefinition\":\"BOM\",\"offsetDays\":10}],\"triggerPriceEnabled\":false,\"triggerPricing\":[],\"priceDifferential\":[{\"differentialType\":\"Discount\",\"diffUpperThreshold\":0,\"diffLowerThreashold\":0,\"differentialValue\":0,\"differentialUnit\":\"\"},{\"differentialType\":\"S-Curve\",\"diffUpperThreshold\":0,\"diffLowerThreashold\":0,\"differentialValue\":0,\"differentialUnit\":\"\"}],\"holidayRule\":\" \"}]}";
		pricingResponse = callAPI(Method.POST, "api/pricing/formula?mode=Detailed",
				generatePayloadFromString(payloadString));
		verify200OKResponse(pricingResponse);
		pricingResponse.then().assertThat().body("contract.itemDetails[0].priceDetails.contractPrice", is("53.0"));
//		Formula version: 2, Delivery period average
		payloadString = "{\"contract\":{\"refNo\":\"5ebc9f7bdc0e820001c903eed5\",\"itemDetails\":[{\"itemNo\":\"1\",\"productId\":\"PDM-4721\",\"qty\":\"100\",\"pricing\":{\"pricingFormulaId\":\"5ebca612dc0e820001c903d9\",\"priceUnitId\":\"PPU-7152\",\"priceUnit\":\"USD/MT\",\"priceTypeId\":\"FormulaPricing\"},\"deliveryFromDate\":\"2020-05-12T00:00:00.000+0000\",\"deliveryToDate\":\"2020-05-27T00:00:00.000+0000\",\"qtyUnit\":\"QUM-2063\",\"quality\":\"QAT-5725\",\"pricingComponentList\":[]}],\"noOfItems\":0,\"asOfDate\":\"2020-05-14T00:00:00.000+0000\"},\"formulaList\":[{\"formulaExpression\":\"NYMEX Light Sweet Crude Oil(WTI) Futures \",\"pricePrecision\":\"2\",\"curveList\":[{\"curveName\":\"NYMEX Light Sweet Crude Oil(WTI) Futures\",\"priceType\":\"Settle Price\",\"startDate\":\"\",\"endDate\":\"\",\"pricePoint\":\"Forward\",\"quotedPeriod\":\"M\",\"quotedPeriodDate\":\"10-2019\",\"priceQuoteRule\":\"Delivery Period Average\",\"period\":\"Delivery Period\",\"event\":\"\",\"offsetType\":\"Day\",\"fxInput\":0,\"fxType\":\"Fixed\",\"fxCurve\":\"\",\"differential\":\"\",\"pricedDays\":0,\"unPricedDays\":0,\"qty\":0,\"pricedQty\":0,\"unpricedQty\":0,\"calculatedPrice\":0,\"curvePrice\":0,\"missingDays\":0,\"coefficient\":0,\"priceMap\":{},\"priceDetailsMap\":{},\"priceFlagMap\":{},\"differentialUnit\":\"\",\"qtyUnitConversionFactor\":0.1364,\"version\":\"V2\",\"pricingPeriod\":\"10-2019\",\"isActualPricing\":false,\"isActualQuoted\":false,\"monthDefinition\":\"BOM\",\"offsetDays\":0}],\"triggerPriceEnabled\":false,\"triggerPricing\":[],\"priceDifferential\":[{\"differentialType\":\"Discount\",\"diffUpperThreshold\":0,\"diffLowerThreashold\":0,\"differentialValue\":0,\"differentialUnit\":\"\"},{\"differentialType\":\"S-Curve\",\"diffUpperThreshold\":0,\"diffLowerThreashold\":0,\"differentialValue\":0,\"differentialUnit\":\"\"}],\"holidayRule\":\"\"}]}";
		pricingResponse = callAPI(Method.POST, "api/pricing/formula?mode=Detailed",
				generatePayloadFromString(payloadString));
		verify200OKResponse(pricingResponse);
		pricingResponse.then().assertThat().body("contract.itemDetails[0].priceDetails.contractPrice", is("51.903225806451616"));
//		Formula version: 2, Event fpr day
		payloadString = "{\"contract\":{\"refNo\":\"15ebd6c1ddc0e8200019ce3dd\",\"asOfDate\":\"2020-05-14T00:00:00.000+0000\",\"itemDetails\":[{\"itemNo\":1,\"productId\":\"PDM-4721\",\"quality\":\"QAT-5725\",\"productSpecs\":\"Gasoil,Gasoil 10ppm\",\"dailyMonthly\":\"Daily\",\"dailyMonthlyQty\":\"500.00\",\"dailyMonthlyUnit\":\"QUM-2063\",\"tolerance\":null,\"toleranceMax\":null,\"toleranceMin\":null,\"toleranceType\":\"Percentage\",\"toleranceLevel\":\"Buyer\",\"qualityPdScheduleId\":null,\"pricing\":{\"priceTypeId\":\"FormulaPricing\",\"pricingStrategy\":\"pricingStrategy-001\",\"pricingFormulaId\":null,\"priceDf\":null,\"priceUnitId\":\"PPU-7152\",\"fxBasisToPayin\":null,\"differentialPrice\":null,\"differentialPriceUnit\":null,\"payInCurId\":\"CM-M-7\",\"priceContractDefId\":null,\"futureInstrumentText\":null,\"priceFutureContractId\":null,\"priceMonthText\":null,\"futurePrice\":null,\"futurePriceUnitId\":null,\"basisPrice\":null,\"basisPriceUnitId\":null,\"fxInstToBasis\":null,\"priceInclusiveOfTax\":null,\"earliestBy\":null,\"priceLastFixDayBasedOn\":null,\"optionsToFix\":null,\"fixationMethod\":null,\"priceUnit\":\"USD/MT\"},\"shipmentMode\":null,\"deliveryFromDate\":\"2019-10-01T00:00:00.000+0000\",\"deliveryToDate\":\"2019-10-03T00:00:00.000+0000\",\"loadingLocationGroupTypeId\":null,\"originationCityId\":null,\"originationCountryId\":null,\"destinationLocationGroupTypeId\":null,\"destinationCityId\":null,\"destinationCountryId\":null,\"paymentDueDate\":null,\"profitCenterId\":null,\"strategyAccId\":null,\"taxScheduleCountryId\":null,\"taxScheduleId\":null,\"inspectionCompany\":null,\"latePaymentInterestDetails\":{\"physicalItemInterestId\":1,\"interestRateType\":\"Variable\",\"variableType\":\"LIBORUSD\",\"interestRate\":null,\"isCompounding\":true,\"frequency\":null},\"unweighedPiPctType\":\"Percentage\",\"unweighedPiPctValue\":null,\"laycanStartDate\":null,\"laycanEndDate\":null,\"optionalLoadingDetails\":[{\"originationCountryId\":null,\"originationCityId\":null,\"freightDf\":null,\"freightDfPriceUnitId\":null,\"internalOptOriginationId\":null,\"optOriginInstanceDeleted\":null}],\"optionalDischargeDetails\":[{\"destinationCountryId\":null,\"destinationCityId\":null,\"freightDf\":null,\"freightDfPriceUnitId\":null,\"internalOptDestinationId\":null,\"optDestInstanceDeleted\":null}],\"valuationFormula\":null,\"isOptionalFieldsEnabled\":false,\"isRenewable\":\"N\",\"isRinContract\":\"N\",\"rinEquivalanceValue\":null,\"rinQuantity\":null,\"rinUnit\":null,\"packingTypeId\":null,\"packingSizeId\":null,\"customEvent\":null,\"customEventDate\":null,\"qty\":1000,\"qtyUnit\":\"QUM-2063\",\"pricingComponentList\":[]}]},\"formulaList\":[{\"formulaExpression\":\"NYMEX Light Sweet Crude Oil(WTI) Futures \",\"holidayRule\":\" \",\"pricePrecision\":\"2\",\"triggerPricing\":[],\"isActualPricing\":false,\"isActualQuoted\":false,\"priceDifferential\":[{\"differentialType\":\"Discount\",\"differentialValue\":\"\",\"differentialUnit\":\"\",\"diffLowerThreashold\":\"\",\"diffUpperThreshold\":\"\"},{\"differentialType\":\"S-Curve\",\"differentialValue\":\"\",\"differentialUnit\":\"\",\"diffLowerThreashold\":\"\",\"diffUpperThreshold\":\"\"}],\"curveList\":[{\"pricePoint\":\"Forward\",\"quotedPeriod\":\"M\",\"version\":\"V2\",\"exposureEnabled\":true,\"quotedPeriodDate\":\"\",\"monthDefinition\":\"BOM\",\"offsetDays\":\"\",\"pricingPeriod\":\"\",\"priceType\":\"Settle Price\",\"priceQuoteRule\":\"Event Offset Based\",\"period\":\"Delivery Period\",\"startDate\":\"\",\"endDate\":\"\",\"event\":null,\"offsetType\":\"Day\",\"offset\":\"03-1-12\",\"fxType\":\"Fixed\",\"fxInput\":\"\",\"fxCurve\":\"\",\"differential\":\"\",\"qtyUnitConversionFactor\":0.1364,\"differentialUnit\":\"\",\"curveName\":\"NYMEX Light Sweet Crude Oil(WTI) Futures\"}]}]}";
		pricingResponse = callAPI(Method.POST, "api/pricing/formula?mode=Detailed",
				generatePayloadFromString(payloadString));
		verify200OKResponse(pricingResponse);
		pricingResponse.then().assertThat().body("contract.itemDetails[0].priceDetails.contractPrice", is("52.0625"));
		
		
	}
		
//	@Test(enabled = true)
	public void testGetPropertyByNameWithoutToken() {
		given().log().all().header("X-TenantID", tenant)
				.header("Content-Type", "application/json").when().request("GET", "/property/eka_recommendation_host")
				.then().assertThat().statusCode(401)
				.body("localizedMessage", containsString("Error in User Authentication"));
	}

	private String authenticateUser(String username, String password) throws UnsupportedEncodingException {
		Map<String, Object> body = new HashMap<String, Object>();
		body.put("userName", username);
		body.put("password", password);
		String base64encodedUsernamePassword = Base64.getEncoder()
				.encodeToString((username + ":" + password).getBytes("utf-8"));
		Response response = given().header("Content-Type", "application/json")
				.header("Authorization", "Basic " + base64encodedUsernamePassword).header("X-TenantID", tenant)
				.body(body).when().post(tokenGenerationApiPath);
		JsonPath jsonPath = new JsonPath(response.asString());
		return jsonPath.getString("auth2AccessToken.access_token");
	}
	
	private Map<String, Object> generatePayloadFromString(String payload) {
		return new JSONObject(payload).toMap();
	}
	
	private Response callAPI(Method httpMethod, String path, Map<String, Object> payload) {
		RestAssured.registerParser("text/plain", Parser.JSON);
		switch (httpMethod) {
		case GET:
			return given().log().all().header("Authorization", token).header("X-TenantID", tenant)
					.header("X-Locale", "en_US").header("Content-Type", "application/json").when()
					.request(httpMethod.name(), path);
		case POST:
		case PUT:
		case DELETE:
			return given().log().all().header("Authorization", token).header("X-TenantID", tenant)
					.header("X-Locale", "en_US").header("Content-Type", "application/json").header("ttl", "10").with().body(payload).when()
					.request(httpMethod.name(), path);
		}
		return null;
	}
	
	private void verify200OKResponse(Response response) {
		Assert.assertEquals(response.getStatusCode(), HttpStatus.SC_OK);
	}
	
	private void verify500OKResponse(Response response) {
		Assert.assertEquals(response.getStatusCode(), HttpStatus.SC_INTERNAL_SERVER_ERROR);
	}
	
	private void verify202OKResponse(Response response) {
		Assert.assertEquals(response.getStatusCode(), HttpStatus.SC_ACCEPTED);
	}
}
