package com.eka.ekaPricing.standalone;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.ValueRange;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.eka.ekaPricing.exception.PricingException;
import com.eka.ekaPricing.helper.HolidayRuleCalculator;
import com.eka.ekaPricing.pojo.Contract;
import com.eka.ekaPricing.pojo.Curve;
import com.eka.ekaPricing.pojo.CurveCalculatorFields;
import com.eka.ekaPricing.pojo.CurveMarketData;
import com.eka.ekaPricing.pojo.Event;
import com.eka.ekaPricing.pojo.FXDetails;
import com.eka.ekaPricing.pojo.GMR;
import com.eka.ekaPricing.pojo.GMRExposureFields;
import com.eka.ekaPricing.pojo.GMRQPDateDetails;
import com.eka.ekaPricing.pojo.HolidayRuleDates;
import com.eka.ekaPricing.pojo.HolidayRuleDetails;
import com.eka.ekaPricing.pojo.PreviewData;
import com.eka.ekaPricing.pojo.Stock;
import com.eka.ekaPricing.pojo.TriggerPrice;
import com.eka.ekaPricing.service.CurveService;
import com.eka.ekaPricing.util.ContextProvider;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

@Component
public class CurveDataFetcher {

	@Autowired
	FXCurveFetcher rateFetcher;
	@Autowired
	HolidayRuleCalculator holidayRuleCalculator;
	@Autowired
	CurveService curveService;
	@Autowired
	CollectionDataFetcher collectionDataFetcher;
	@Autowired
	MDMServiceFetcher mdmFetcher;
	@Autowired
	ExpiryCalenderFetcher expiryCalenderFetcher;
	@Autowired
	ErrorMessageFetcher messageFetcher;
	@Autowired
	com.eka.ekaPricing.standalone.ExpressionBuilder expressionBuilder;
	@Autowired 
	MassToVolumeConversion massToVolumeConversion;
	@Autowired
	ForwardCollectionDataFetcher forwardCollectionDataFetcher;
	@Autowired
	GMRCreationHelper gmrCreationHelper;
	@Autowired
	FormulaeCalculator formulaeCalculator;

	final static  Logger logger = ESAPI.getLogger(CurveDataFetcher.class);

	public CurveCalculatorFields calculateFormulae(Curve c, JSONObject itemObj, String precision, String fxType,
			String contractCurr, GMR gmr, String lookbackDate, LocalDate asOf, ContextProvider tenantProvider,
			String holidayRule) throws Exception {
		double res = 0;
		JSONArray forwardPrices = forwardCollectionDataFetcher.getForwardMarketData(c, asOf);
		if (c.getPricePoint().equals("Forward") && (c.getQuotedPeriod() == null || c.getQuotedPeriod().isEmpty())) {
			throw new PricingException(
					messageFetcher.fetchErrorMessage(tenantProvider, "015", new ArrayList<String>()));
		}
		if (c.getPriceQuoteRule().equals("Contract Period Average") && !c.getPeriod().equals("Delivery Period")) {
			throw new PricingException(
					messageFetcher.fetchErrorMessage(tenantProvider, "016", new ArrayList<String>()));
		}
		if (null != c.getOffset() && (null == c.getOffsetType() || c.getOffsetType().isEmpty())
				&& c.getPriceQuoteRule().equals("Event Offset Based")) {
			throw new PricingException(
					messageFetcher.fetchErrorMessage(tenantProvider, "017", new ArrayList<String>()));
		}
		
		//Map<LocalDate, Double> fxRateMap = new HashMap<LocalDate, Double>();
		Map<LocalDate, FXDetails> fxRateMap = new HashMap<LocalDate, FXDetails>();
//		Set<Date> expiryDateSet = new HashSet<Date>();
		String exchangeCode = "";
		CurveCalculatorFields fields = new CurveCalculatorFields();
		if (!StringUtils.isEmpty(c.getVersion()) && c.getVersion().equals("V2")) {
			fields = initializeV2(new CurveCalculatorFields(), c, itemObj, gmr, lookbackDate, asOf, tenantProvider);
		} else {
			fields = initialize(new CurveCalculatorFields(), c, itemObj, gmr, lookbackDate, asOf, tenantProvider);
		}
		List<CurveMarketData> collectionData = c.getCollectionArray();
		validatePricesData(tenantProvider, collectionData, c, forwardPrices);
		if (forwardPrices.length() == 0 && (null == collectionData || collectionData.isEmpty())) {
			List<String> params = new ArrayList<String>();
			params.add(c.getCurveName());
			throw new PricingException(messageFetcher.fetchErrorMessage(tenantProvider, "018", params));
		}
		if(null == collectionData || collectionData.isEmpty()) {
			JSONObject forwardObj = forwardPrices.optJSONObject(0);
			exchangeCode = forwardObj.optString("Exchange");
			c.setExchange(exchangeCode);
			String priceUnit = forwardObj.optString("Price/Unit");
			fields.setCurveCurrency(priceUnit.substring(0, priceUnit.indexOf("/")));
			c.setCurveCurrency(priceUnit.substring(0, priceUnit.indexOf("/")));
			fields.setCurveQty(priceUnit.substring(priceUnit.indexOf("/") + 1, priceUnit.length()));
			c.setCurveQty(priceUnit.substring(priceUnit.indexOf("/") + 1, priceUnit.length()));
		}
		else {
			for(CurveMarketData cmd : collectionData) {
				exchangeCode = cmd.getExchange();
//				expiryDateSet.add(Date.from(cmd.getPromptDate().atStartOfDay().toInstant(ZoneOffset.UTC)));
				if (null != fields.getPreviewData().getCurveCurrency()) {
					logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("skipping as price unit already populated"));
				} else {
					try {
						String priceUnit = cmd.getPriceUnit();
						fields.setCurveCurrency(priceUnit.substring(0, priceUnit.indexOf("/")));
						c.setCurveCurrency(priceUnit.substring(0, priceUnit.indexOf("/")));
						fields.setCurveQty(priceUnit.substring(priceUnit.indexOf("/") + 1, priceUnit.length()));
						c.setCurveQty(priceUnit.substring(priceUnit.indexOf("/") + 1, priceUnit.length()));
					}
					catch(Exception e) {
						logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("price unit is not in proper format"));
					}
				}
			}
		}
		
		if(null==gmr) {
			c.setQpFromDate(fields.getSd());
			c.setQpToDate(fields.getEd());
		}
		else {
			List<GMRQPDateDetails> gmrQPDetailsList =  c.getGmrQPDetailsList();
			if(null==gmrQPDetailsList) {
				gmrQPDetailsList = new ArrayList<GMRQPDateDetails>();
			}
			GMR curveLevelGMR = new GMR();
			curveLevelGMR.setRefNo(gmr.getRefNo());
			curveLevelGMR.setStockRef(gmr.getStockRef());
			curveLevelGMR.setEvent(gmr.getEvent());
			GMRQPDateDetails gmrQPDetails = new GMRQPDateDetails();
			gmrQPDetails.setGmr(curveLevelGMR);
			gmrQPDetails.setQpFromDate(fields.getSd());
			gmrQPDetails.setQpToDate(fields.getEd());
			gmrQPDetailsList.add(gmrQPDetails);
			c.setGmrQPDetailsList(gmrQPDetailsList);
		}
		if (fields.getDiffMonth() > 3) {
			throw new PricingException(
					messageFetcher.fetchErrorMessage(tenantProvider, "019", new ArrayList<String>()));
		}
		res = 0d;
		int validDaysCounter = 0;
		List<LocalDateTime> validDaysList = new ArrayList<LocalDateTime>();
		List<LocalDateTime> realDatesList = new ArrayList<LocalDateTime>();
		Map<LocalDate, Double> priceMap = new HashMap<LocalDate, Double>();
		Map<LocalDate, Double> priceFxMap = new HashMap<LocalDate, Double>();
		Map<LocalDate, String> priceFlagMap = new HashMap<LocalDate, String>();
		Map<LocalDate, String> priceFxFlagMap = new HashMap<LocalDate, String>();
		Map<LocalDate, String> monthFlagMap = new HashMap<LocalDate, String>();
		Map<LocalDate, CurveMarketData> priceDetailsMap = c.getPriceDetailsMap();
		Map<LocalDate, Double> priceWithoutFxMap = new HashMap<LocalDate, Double>();
		if (exchangeCode.length() > 0) {
			HolidayRuleDates holidayRuleDatesObj = getListOfDaysPostHolidayRule(fields, exchangeCode, tenantProvider, holidayRule, c);
			validDaysList = holidayRuleDatesObj.getDateToBeUsed();
			realDatesList = holidayRuleDatesObj.getDatesList();
			c.setHolidayRuleDates(holidayRuleDatesObj);
		} else {
			logger.error(Logger.EVENT_FAILURE,
					ESAPI.encoder().encodeForHTML("exchange code not available for Curve: " + c.getCurveName()));
			throw new PricingException(
					messageFetcher.fetchErrorMessage(tenantProvider, "002", new ArrayList<String>()));
		}
		if (!c.getCurveCurrency().equals(contractCurr) && fxType.equalsIgnoreCase("curve")) {
			fxRateMap = rateFetcher.getFxRatesFromCurve(c.getFxCurve(), validDaysList.get(0).minusDays(1).toLocalDate(),
					validDaysList.get(validDaysList.size() - 1).plusDays(1).toLocalDate(),
					tenantProvider.getCurrentContext().getToken(), c.getCurveCurrency(), contractCurr, asOf);
		}
		if (c.getPriceQuoteRule().equals("Event Offset Based") && fields.isSkipMidIfEvent()
				&& realDatesList.contains(fields.getMidEventDay().atStartOfDay())) {
			int indOfMid = realDatesList.indexOf(fields.getMidEventDay().atStartOfDay());
			validDaysList.remove(indOfMid);
			realDatesList.remove(indOfMid);
			HolidayRuleDates holidayRuleDatesObj = new HolidayRuleDates();
			holidayRuleDatesObj.setDatesList(realDatesList);
			holidayRuleDatesObj.setDateToBeUsed(validDaysList);
			c.setHolidayRuleDates(holidayRuleDatesObj);
			fields.setSkipMidIfEvent(false);
		}
		if(c.getPriceQuoteRule().equals("Event Offset Based") && c.getOffsetType().equals("Day")) {
			String offset = c.getOffset();
			String[] offArr = offset.split("-");
			int offsetSum = 0;
			for(int i=0; i<offArr.length;i++) {
				offsetSum = offsetSum+Integer.parseInt(offArr[i]);
			}
			while(offsetSum>validDaysList.size()) {
				fields.setEd(fields.getEd().plusDays(1));
				HolidayRuleDates holidayRuleDatesObj = getListOfDaysPostHolidayRule(fields, exchangeCode, tenantProvider, holidayRule, c);
				validDaysList = holidayRuleDatesObj.getDateToBeUsed();
				realDatesList = holidayRuleDatesObj.getDatesList();
				if (c.getPriceQuoteRule().equals("Event Offset Based") && fields.isSkipMidIfEvent()
						&& validDaysList.contains(fields.getMidEventDay().atStartOfDay())) {
					if(validDaysList.contains(fields.getMidEventDay().atStartOfDay())) {
						validDaysList.remove(fields.getMidEventDay().atStartOfDay());
					}
					if(realDatesList.contains(fields.getMidEventDay().atStartOfDay())) {
						realDatesList.remove(fields.getMidEventDay().atStartOfDay());
					}
				}
				if(null==c.getHolidayRuleDates()) {
					c.setHolidayRuleDates(holidayRuleDatesObj);
				}
			}
		}
		
		if(c.getPriceQuoteRule().equals("Settlement Date") && c.getOffsetType().equals("Day")) {
			String offset = c.getOffset().trim();
			int prev = Integer.parseInt(offset.substring(0, offset.indexOf("-")));
			if(prev==0) {
				validDaysList.remove(0);
				realDatesList.remove(0);
				HolidayRuleDates holidayRuleDatesObj = new HolidayRuleDates();
				holidayRuleDatesObj.setDatesList(realDatesList);
				holidayRuleDatesObj.setDateToBeUsed(validDaysList);
				c.setHolidayRuleDates(holidayRuleDatesObj);
				
			}
		}
		
		if (c.getPriceQuoteRule().equals("Settlement Date") && fields.isSkipMidIfEvent()
				&& realDatesList.contains(fields.getMidEventDay().atStartOfDay()) && c.getOffsetType().equals("Day")) {
			int indOfMid = realDatesList.indexOf(fields.getMidEventDay().atStartOfDay());
			validDaysList.remove(indOfMid);
			realDatesList.remove(indOfMid);
			HolidayRuleDates holidayRuleDatesObj = new HolidayRuleDates();
			holidayRuleDatesObj.setDatesList(realDatesList);
			holidayRuleDatesObj.setDateToBeUsed(validDaysList);
			c.setHolidayRuleDates(holidayRuleDatesObj);
			fields.setSkipMidIfEvent(false);
		}
		double priceWithoutFX = 0d;
		double avgPriceFx = 0d;
//		price tags in below code means	f = fixed, e = estimated, l = look back
		for (validDaysCounter = 0; validDaysCounter < validDaysList.size(); validDaysCounter++) {
			String monthYear="";
			LocalDate currentDate = validDaysList.get(validDaysCounter).toLocalDate();
			if (priceDetailsMap.containsKey(currentDate)) {
				boolean usedForwardPrice = false;
				CurveMarketData marketData = priceDetailsMap.get(currentDate);
				monthYear = marketData.getMonthYear();
				JSONObject forwardPriceObj = checkForwardPrices(forwardPrices, asOf, currentDate, c);
				double forwardPrice = forwardPriceObj.optDouble("price");
				double price = 0d;
				if(!curveService.checkZero(forwardPrice)) {
					price = forwardPrice;
					monthYear = getMonthYearForDate(currentDate, c);
					usedForwardPrice = true;
				}
				else {
					try {
						price = Double.parseDouble(marketData.getPrice());
					}
					catch (Exception e) {
						List<String> params = new ArrayList<String>();
						params.add(c.getCurveName());
						throw new PricingException(messageFetcher.fetchErrorMessage(tenantProvider, "020", params));
					}
				}
				priceWithoutFX = priceWithoutFX + price;
				priceWithoutFxMap.put(realDatesList.get(validDaysCounter).toLocalDate(), price);
				if (!c.getCurveCurrency().equals(contractCurr) && c.getFxType().toLowerCase().equals("curve")) {
					
					avgPriceFx=avgPriceFx+fxRateMap.get(realDatesList.get(validDaysCounter).toLocalDate()).getFxValue();
					price = price * fxRateMap.get(realDatesList.get(validDaysCounter).toLocalDate()).getFxValue();
					priceFxMap.put(realDatesList.get(validDaysCounter).toLocalDate(), fxRateMap.get(realDatesList.get(validDaysCounter).toLocalDate()).getFxValue());
					priceFxFlagMap.put(realDatesList.get(validDaysCounter).toLocalDate(), fxRateMap.get(realDatesList.get(validDaysCounter).toLocalDate()).getFxDef());
				}
				boolean isEstimated = checkEstimated(monthYear, currentDate, c);
				priceMap.put(realDatesList.get(validDaysCounter).toLocalDate(), price);
				if(realDatesList.get(validDaysCounter).isEqual(validDaysList.get(validDaysCounter))) {
					if(usedForwardPrice && !forwardPriceObj.optString("tag").isEmpty()) {
						priceFlagMap.put(realDatesList.get(validDaysCounter).toLocalDate(),
								forwardPriceObj.optString("tag"));
					}
					else if(isEstimated) {
						priceFlagMap.put(realDatesList.get(validDaysCounter).toLocalDate(), "Estimated");
					}
					else {
						priceFlagMap.put(realDatesList.get(validDaysCounter).toLocalDate(), "f");
					}
					monthFlagMap.put(realDatesList.get(validDaysCounter).toLocalDate(), monthYear);
				}
				else {
					priceFlagMap.put(realDatesList.get(validDaysCounter).toLocalDate(), "e");
					monthFlagMap.put(realDatesList.get(validDaysCounter).toLocalDate(), monthYear);
				}
			} else {
				String month = currentDate.getMonth().toString().substring(0, 3);
				String year = Integer.toString(currentDate.getYear());
				monthYear=month+year;
				JSONObject forwardPriceObj = checkForwardPrices(forwardPrices, asOf, currentDate, c);
				double forwardPrice = forwardPriceObj.optDouble("price");
				double price = 0;
				if(!curveService.checkZero(forwardPrice)) {
					monthYear = c.getMonthYear();
					price = forwardPrice;
					monthYear = getMonthYearForDate(currentDate, c);
				}
				else {
						if(c.getPriceQuoteRule().equalsIgnoreCase("Settlement Date")) {
							String settlementMonthYear = getMonthYearForDate(currentDate, c);
							price = findLastKnownPriceForSettlementDate(priceDetailsMap, validDaysList.get(validDaysCounter).toLocalDate(),
									c.getFirstPricingDate(), tenantProvider, asOf, c,settlementMonthYear);
						}else {
					price = findLastKnownPrice(priceDetailsMap, validDaysList.get(validDaysCounter).toLocalDate(),
							c.getFirstPricingDate(), tenantProvider, asOf, c);
						}
				}
				priceWithoutFX = priceWithoutFX + price;
				priceWithoutFxMap.put(realDatesList.get(validDaysCounter).toLocalDate(), price);
				if (!c.getCurveCurrency().equals(contractCurr) && c.getFxType().toLowerCase().equals("curve")) {
					double validFx = fxRateMap.get(currentDate).getFxValue();
					avgPriceFx = avgPriceFx + validFx;
					price = price * validFx;
					priceFxMap.put(realDatesList.get(validDaysCounter).toLocalDate(), validFx);
					priceFxFlagMap.put(realDatesList.get(validDaysCounter).toLocalDate(),
							fxRateMap.get(realDatesList.get(validDaysCounter).toLocalDate()).getFxDef());
//					For holidays replacing fx values with valid fx value
					priceFxMap.put(realDatesList.get(validDaysCounter).toLocalDate(), validFx);
				}
				priceMap.put(realDatesList.get(validDaysCounter).toLocalDate(), price);
				if(forwardPriceObj.optString("tag").isEmpty()) {
					priceFlagMap.put(realDatesList.get(validDaysCounter).toLocalDate(), "l");
				}
				else {
					priceFlagMap.put(realDatesList.get(validDaysCounter).toLocalDate(),
							forwardPriceObj.optString("tag"));
				}
				monthFlagMap.put(realDatesList.get(validDaysCounter).toLocalDate(),
						getMonthYearForDate(realDatesList.get(validDaysCounter).toLocalDate(), c));
			}
		}
	
		c.setPriceMapGMR(priceMap);
		c.setPriceWithoutFXMap(priceWithoutFxMap);
		c.setPriceFlagMapGMR(priceFlagMap);
		c.setMonthFlagMapGMR(monthFlagMap);
		c.setPriceFxMapGMR(priceFxMap);
		c.setPriceFxFlagMapGMR(priceFxFlagMap);
		if(null==gmr) {
			c.setPriceMap(priceMap);
			c.setPriceFlagMap(priceFlagMap);
			c.setMonthFlagMap(monthFlagMap);
			c.setPriceFxMap(priceFxMap);
			c.setPriceFxFlagMap(priceFxFlagMap);
		}
		for(double d: priceMap.values()) {
			res = res+d;
		}
		res = res/priceMap.size();
		priceWithoutFX = priceWithoutFX/priceMap.size();
		int indexPrecision = c.getIndexPrecision();
//		if(c.getIndexPrecision()!=0) {
//			indexPrecision = ;
//		}
		res = expressionBuilder.applyPrecision(res, indexPrecision);
		if (!c.getCurveCurrency().equals(contractCurr) && c.getFxType().toLowerCase().equals("curve")) {
			avgPriceFx=avgPriceFx/priceMap.size();
			avgPriceFx = expressionBuilder.applyPrecision(avgPriceFx, indexPrecision);
			}else {
				if(c.getFxInput()!=0) {
					avgPriceFx = expressionBuilder.applyPrecision(c.getFxInput(), indexPrecision);
				}else {
					avgPriceFx =1;
				}
				
			}
		priceWithoutFX = expressionBuilder.applyPrecision(priceWithoutFX, indexPrecision);
		fields.setOriginalPrice(res);
		fields.setConvertedPrice(res);
		fields.setPriceWithoutDailyFx(priceWithoutFX);
		fields.setAvgPriceFx(avgPriceFx);
		return fields;

	}

	public CurveCalculatorFields calculateMarketPrices(Curve c, JSONObject itemObj, String precision, String fxType,
			String contractCurr, String lookbackDate, LocalDate asOf, ContextProvider tenantProvider) throws Exception {
		CurveCalculatorFields fields = new CurveCalculatorFields();
		if (!StringUtils.isEmpty(c.getVersion()) && c.getVersion().equals("V2")) {
			fields = initializeV2(new CurveCalculatorFields(), c, itemObj, null, lookbackDate, asOf, tenantProvider);
		} else {
			fields = initialize(new CurveCalculatorFields(), c, itemObj, null, lookbackDate, asOf, tenantProvider);
		}
		Map<LocalDate, Double> priceMap = c.getPriceWithoutFXMap();
		if (priceMap.isEmpty()) {
			List<String> params = new ArrayList<String>();
			params.add(c.getCurveName());
			throw new PricingException(messageFetcher.fetchErrorMessage(tenantProvider, "018", params));
		}
		if (priceMap.containsKey(asOf)) {
			fields.setOriginalPrice(priceMap.get(asOf));
			fields.setConvertedPrice(priceMap.get(asOf));
		} else {
			if(null == c.getPriceDetailsMap() || c.getPriceDetailsMap().isEmpty()) {
				fields.setOriginalPrice(0);
				fields.setConvertedPrice(0);
			}
			else {
				fields.setOriginalPrice(
						findLastKnownPrice(c.getPriceDetailsMap(), asOf, c.getFirstPricingDate(), tenantProvider, asOf, c));
				fields.setConvertedPrice(
						findLastKnownPrice(c.getPriceDetailsMap(), asOf, c.getFirstPricingDate(), tenantProvider, asOf, c));
			}
		}
		
		fields.setCurveCurrency(c.getCurveCurrency());
		fields.setCurveQty(c.getCurveQty());
		
		return fields;
	}

	public JSONObject getPreviewDataSet(Curve c, JSONObject jobj, int index, String lookbackDate, LocalDate asOf,
			ContextProvider tenantProvider, String holidayRule, String productId, String contractItemQty,
			List<GMR> gmrList, String exp,double contractQualityDensity,String contractQualityMassUnit,
			String contractQualityVolumeUnit,String locationType,List<TriggerPrice> triggerPriceList,Contract contract) throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Map<LocalDate, CurveMarketData> priceDetailsMap = c.getPriceDetailsMap();
		CurveCalculatorFields fields = new CurveCalculatorFields();
		String itemQtyUnit = mdmFetcher.getQuantityKey(null, contractItemQty, null);
		String CurveQtyUnitID = mdmFetcher.getQuantityKey(null, c.getCurveQty(), null);
		
		Map<String, Double> actualConversionFactor = new HashMap<String,Double>();
		Map<String, Double> baseActualConversionFactor = new HashMap<String,Double>();
		Map<String, Double> gmrToItemConversionFactor = new HashMap<String,Double>();
		double contractualConversionFactor=0.0;
		double baseContractualConversionFactor=0.0;
		double totalGmrQty=0.0;
		double gmrQty=0.0;
		String[] baseQtyUnitId = mdmFetcher.getBaseQtyUnit(tenantProvider, productId);
		String baseQtyUnit = mdmFetcher.getContractQty(null, baseQtyUnitId[0], null);
		String itemQtyUnitCheck=massToVolumeConversion.getMassVolumeQtyCheck(productId,itemQtyUnit);
		if(gmrList.size()>0) {
					gmrToItemConversionFactor=gmrDensityVolumeConversionGMRQty(gmrList,jobj,productId,tenantProvider,
							 itemQtyUnit,  contractQualityDensity, contractQualityMassUnit,
							 contractQualityVolumeUnit, totalGmrQty,  itemQtyUnitCheck); 
					
				baseActualConversionFactor=gmrDensityVolumeConversion(gmrList,jobj,productId,tenantProvider,itemQtyUnit,baseQtyUnitId[0],contractQualityDensity,
						contractQualityMassUnit, contractQualityVolumeUnit,totalGmrQty,contractItemQty,c.getCurveQty(),index+1,false,gmrToItemConversionFactor,itemQtyUnitCheck);
				
				for (GMR gmr : gmrList) {
					  for (Stock st : gmr.getStocks()) {
					   if (st.getGMRRefNo().equals(gmr.getRefNo())) {
						   gmrQty=gmrQty + st.getStockQtyInGmr()*(gmrToItemConversionFactor.get(st.getGMRRefNo()));
						 }
					 }
				}
				
				totalGmrQty = gmrCreationHelper.getTotalGmrQty(jobj.optString("refNo"),gmrList)+gmrQty;
			
			}
				if(contractQualityDensity!=0.0) {
					contractualConversionFactor=contractualConversionFactor(productId,itemQtyUnit,CurveQtyUnitID, contractQualityDensity, contractQualityMassUnit,
							 contractQualityVolumeUnit, tenantProvider);
					
					baseContractualConversionFactor=contractualConversionFactor(productId,itemQtyUnit,baseQtyUnitId[0], contractQualityDensity, contractQualityMassUnit,
							 contractQualityVolumeUnit, tenantProvider);
					
			}else {
				contractualConversionFactor = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productId, itemQtyUnit,
					CurveQtyUnitID);
				
				baseContractualConversionFactor = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productId, itemQtyUnit,
						baseQtyUnitId[0]);
			}
		
		if(!StringUtils.isEmpty(c.getVersion()) &&  c.getVersion().equals("V2")) {
			fields = initializeV2(new CurveCalculatorFields(), c, jobj, null, lookbackDate, asOf, tenantProvider);
		}
		else {
			fields = initialize(new CurveCalculatorFields(), c, jobj, null, lookbackDate, asOf, tenantProvider);
		}
		String exchangeCode = c.getExchange();
		Map<LocalDate, Double> priceMap = c.getPriceWithoutFXMap();
		JSONArray curveArr = new JSONArray();
		if(null!=c.getPriceFxMap() && !c.getPriceFxFlagMap().isEmpty()) {
			priceMap.forEach((k, v) -> curveArr.put(new JSONObject().put("date", k).put("price", v).put("priceFlag", c.getPriceFlagMap().get(k)).
					put("instrumentMonth", c.getMonthFlagMap().get(k)).put("priceFx", c.getPriceFxMap().get(k)).put("priceFlagFx", c.getPriceFxFlagMap().get(k))));
		}else {
			priceMap.forEach((k, v) -> curveArr.put(new JSONObject().put("date", k).put("price", v).put("priceFlag", c.getPriceFlagMap().get(k)).
					put("instrumentMonth", c.getMonthFlagMap().get(k)).put("priceFx", c.getAvgFx()).put("priceFlagFx", "")));
		}
		JSONArray exposureArr = new JSONArray();
		JSONArray newerExposureArr = new JSONArray();
		double dailyPricedQty = getDailyPricedQty(c);
		double totalQty = c.getQty();
		Map<LocalDate, Integer> dateCountMap = new LinkedHashMap<LocalDate, Integer>();
		List<LocalDateTime> validDatesList = null;
		List<LocalDateTime> realDates = null;
		if (null != c.getTriggerPriceExposure()) {
			exposureArr = c.getTriggerPriceExposure();
		}
		else {
			if (c.getPriceQuoteRule().equals("Event Offset Based") && null == c.getGmrQPDetailsList() && c.isExp() == true) {
				HolidayRuleDates holidayRuleDates = getListOfDaysPostHolidayRule(fields, c.getExchange(), tenantProvider,
						holidayRule, c);
				validDatesList = holidayRuleDates.getDateToBeUsed();
				realDates = holidayRuleDates.getDateToBeUsed();
				dailyPricedQty = totalQty;
			} else {
				validDatesList = c.getHolidayRuleDates().getDateToBeUsed();
				realDates = c.getHolidayRuleDates().getDatesList();
			}
			for (LocalDateTime date : validDatesList) {
				if (dateCountMap.containsKey(date.toLocalDate())) {
					dateCountMap.put(date.toLocalDate(), dateCountMap.get(date.toLocalDate()) + 1);
				} else {
					dateCountMap.put(date.toLocalDate(), 1);
				}
			}
			Set<Entry<LocalDate, Integer>> flagMapEntrySet = dateCountMap.entrySet();

//			Aggregate functions use in exposure is to be done as part of another task.
			if (exp.contains("MIN") || exp.contains("MAX") || exp.contains("AVG")) {
//				Not in a for loop. Code incoming in coming days.
			} else if (c.getPriceQuoteRule().equals("Event Offset Based") && gmrList.size() > 0) {
				int count = checkEventNamefromConnect(contract,contractItemQty,jobj,productId,c.getEvent(),gmrList);
				List<GMRExposureFields> gmrExposureFieldList = new ArrayList<GMRExposureFields>();
				for (GMR gmr : gmrList) {
					GMRExposureFields gmrExposureField = new GMRExposureFields();
					List<Event> eventList = gmr.getEvent();
					List<Event> eventList1 = new LinkedList<>();
					int i=0;
					String eventName="";
					String eventDate="";
					for (Event e : eventList) {
						if(eventList.size()>1){
							i++;
							if(i==1) {
								eventName=e.getName();
								eventDate=e.getDate();
							 }
						if(i==2 && e.getName().equalsIgnoreCase("RollOver")) {
							eventName=e.getName();
							eventDate=e.getDate();
						 }
						}else {
							eventName=e.getName();
							eventDate=e.getDate();
						}
						
					}
					Event e = new Event();
					e.setName(eventName);
					e.setDate(eventDate);
					eventList1.add(e);
					gmr.setEvent(eventList1);
					gmrExposureField.setGmr(gmr);
					for (Stock st : gmr.getStocks()) {
						if (st.getGMRRefNo().equals(gmr.getRefNo())) {
							gmrExposureField.setQty(gmrExposureField.getQty() + st.getQty());
							gmrExposureField.setStockQtyInGmr(gmrExposureField.getStockQtyInGmr() + st.getStockQtyInGmr());
						}
					}
					gmrExposureFieldList.add(gmrExposureField);
				}
				exposureArr = getExposureForGMR(gmrExposureFieldList, asOf, c.getQty(), c, holidayRule, tenantProvider,
						jobj, baseContractualConversionFactor,baseActualConversionFactor,actualConversionFactor,
						contractualConversionFactor,gmrToItemConversionFactor,locationType,triggerPriceList,jobj,totalGmrQty,count);
			
			} else if(!c.getPriceQuoteRule().equals("Event Offset Based") && gmrList.size() > 0){
				double qty = totalQty;
				
		for (GMR gmr : gmrList) {
		 for (Stock st : gmr.getStocks()) {
			if (st.getGMRRefNo().equals(gmr.getRefNo())) {
				JSONObject exposureObj = new JSONObject();
				LocalDate gmrCreationDate = asOf;
				if(!StringUtils.isEmpty(st.getGmrCreationDate())) {
					gmrCreationDate = sdf.parse(st.getGmrCreationDate()).toInstant()
							.atZone(ZoneId.systemDefault()).toLocalDate();
				}
				
				exposureObj.put("date", gmrCreationDate);
				exposureObj.put("pricedQty", st.getStockQtyInGmr()*gmrToItemConversionFactor.get(st.getGMRRefNo()));
				exposureObj.put("unPricedQty", 0);
				exposureObj.accumulate("pricedQuantityInBaseQtyUnit", st.getStockQtyInGmr()*baseActualConversionFactor.get(st.getGMRRefNo())
						*gmrToItemConversionFactor.get(st.getGMRRefNo()));
				exposureObj.accumulate("unpricedQuantityInBaseQtyUnit", 0);
				exposureObj.put("gmrRefNo", st.getGMRRefNo());
				exposureObj.put("titleTransferStatus", gmr.getTitleTransferStatus());
				exposureObj.put("contractualConversionFactor", 0);
				exposureObj.put("actualConversionFactor", 1);
				String locationName=gmr.getStorageLocation();
				if(StringUtils.isEmpty(locationName)) {
					if(!StringUtils.isEmpty(locationType)) {
						if(locationType.equalsIgnoreCase("ORIGINATION")) {
							locationName=gmr.getLoadingLocName();
						}else {
							locationName=gmr.getDestinationLocName();
						}
					}
					
				}
				exposureObj.put("locationName", locationName);
				
				if(priceDetailsMap.get(gmrCreationDate)!=null) {
					if(!c.getPricePoint().equalsIgnoreCase("forward")) {
						String month = gmrCreationDate.getMonth().toString().substring(0, 3);
						String year = Integer.toString(gmrCreationDate.getYear());
						exposureObj.put("instrumentDeliveryMonth", month+year);
					}
					else {
						exposureObj.put("instrumentDeliveryMonth", priceDetailsMap.get(gmrCreationDate).getMonthYear());
					}
					
				}else {
					String month = gmrCreationDate.getMonth().toString().substring(0, 3);
					String year = Integer.toString(gmrCreationDate.getYear());
					exposureObj.put("instrumentDeliveryMonth", month+year);
				}
				exposureObj.accumulate("pricedPercentage", (st.getStockQtyInGmr()*100)/qty);
				exposureObj.accumulate("unpricedPercentage", 0);
				exposureArr.put(exposureObj);
			}
		  }
		}
				int count=0;
				for(LocalDateTime date : realDates) {
					if(!asOf.isBefore(date.toLocalDate())) {
						continue;
					}
					count++;
				}
				
				if(totalGmrQty>qty) {
					qty=totalGmrQty;
				}
				
				if(triggerPriceList.size()>0) {
					qty=qty-c.getTotalTriggerPriceQty();
				}
			
				if(count>0){
					double dailyPricedQty1 = qty/realDates.size();
					double dailyPercentage = 100/count;
						for(Entry<LocalDate, Integer> entry : flagMapEntrySet) {
							JSONObject expObj = new JSONObject();
							if(!asOf.isBefore(entry.getKey())) {
								continue;
							}
							
							int numOfDaysUsed = 1;
							if(dateCountMap.containsKey(entry.getKey())) {
								numOfDaysUsed = dateCountMap.get(entry.getKey());
							}
							if(entry.getKey().isAfter(asOf)) {
								expObj.accumulate("date", entry.getKey());
								expObj.accumulate("pricedQty", 0);
								expObj.accumulate("unPricedQty", dailyPricedQty1 * entry.getValue());
								expObj.accumulate("pricedPercentage", 0);
								expObj.accumulate("unpricedPercentage", dailyPercentage * numOfDaysUsed);
								expObj.accumulate("pricedQuantityInBaseQtyUnit", 0);
								expObj.accumulate("unpricedQuantityInBaseQtyUnit", dailyPricedQty1 * numOfDaysUsed*baseContractualConversionFactor);
								expObj.accumulate("contractualConversionFactor", contractualConversionFactor);
								expObj.accumulate("actualConversionFactor", 0);
								
							}
							if(priceDetailsMap.get(asOf)!=null) {
								if(!c.getPricePoint().equalsIgnoreCase("forward")) {
									String month = asOf.getMonth().toString().substring(0, 3);
									String year = Integer.toString(asOf.getYear());
									expObj.put("instrumentDeliveryMonth", month+year);
								}
								else {
									expObj.put("instrumentDeliveryMonth", priceDetailsMap.get(asOf).getMonthYear());
								}
								
							}else {
								String month = asOf.getMonth().toString().substring(0, 3);
								String year = Integer.toString(asOf.getYear());
								expObj.put("instrumentDeliveryMonth", month+year);
							}
							exposureArr.put(expObj);
						}
    				}
				
			}else {
				if(triggerPriceList.size()>0) {
					totalQty=totalQty-c.getTotalTriggerPriceQty();
				}
				String lastDelMonth = "";
				LocalDate lastPromptDate = null;
				double dailyPercentage = 100.0/validDatesList.size();
				dailyPricedQty=totalQty/realDates.size();
				for (Entry<LocalDate, Integer> entry : flagMapEntrySet) {
					JSONObject expObj = new JSONObject();
					if (entry.getKey().isAfter(asOf)) {
						expObj.accumulate("date", entry.getKey());
						expObj.accumulate("pricedQty", 0);
						expObj.accumulate("unPricedQty", dailyPricedQty * entry.getValue());
						expObj.put("pricedPercentage", 0);
						expObj.put("unpricedPercentage", dailyPercentage *entry.getValue());
						expObj.accumulate("pricedQuantityInBaseQtyUnit", 0);
						expObj.accumulate("unpricedQuantityInBaseQtyUnit", dailyPricedQty *entry.getValue()*baseContractualConversionFactor);
						expObj.accumulate("contractualConversionFactor", contractualConversionFactor);
						expObj.accumulate("actualConversionFactor", 0);
						
						if(priceDetailsMap.get(entry.getKey())!=null) {
							if(!c.getPricePoint().equalsIgnoreCase("forward")) {
								LocalDate priceDate = entry.getKey();
								String month = priceDate.getMonth().toString().substring(0, 3);
								String year = Integer.toString(priceDate.getYear());
								expObj.put("instrumentDeliveryMonth", month+year);
							}
							else {
								expObj.put("instrumentDeliveryMonth", priceDetailsMap.get(entry.getKey()).getMonthYear());
							}
							
							lastDelMonth = priceDetailsMap.get(entry.getKey()).getMonthYear();
							lastPromptDate = priceDetailsMap.get(entry.getKey()).getPromptDate();
						}
						else if(null!=lastPromptDate && entry.getKey().isBefore(lastPromptDate)) {
							if(!c.getPricePoint().equalsIgnoreCase("forward")) {
								LocalDate priceDate = entry.getKey();
								String month = priceDate.getMonth().toString().substring(0, 3);
								String year = Integer.toString(priceDate.getYear());
								expObj.put("instrumentDeliveryMonth", month+year);
							}
							else {
								expObj.put("instrumentDeliveryMonth", lastDelMonth);
							}
						}else {
							if(!c.getPricePoint().equalsIgnoreCase("forward")) {
								LocalDate priceDate = entry.getKey();
								String month = priceDate.getMonth().toString().substring(0, 3);
								String year = Integer.toString(priceDate.getYear());
								expObj.put("instrumentDeliveryMonth", month+year);
							}else {
								String monthYear = getMonthYearForDate(entry.getKey(), c);
								expObj.put("instrumentDeliveryMonth", monthYear);
							}
						}
					
						exposureArr.put(expObj);
					}
				 }
			}
		}
		
		for (int expIndex = 0; expIndex < exposureArr.length(); expIndex++) {
			JSONObject exposureObject = new JSONObject(exposureArr.getJSONObject(expIndex),
					JSONObject.getNames(exposureArr.getJSONObject(expIndex)));
			if(exposureObject.optDouble("actualConversionFactor")!=0) {
				double actualConversionFactor1=exposureObject.optDouble("actualConversionFactor");
				exposureObject.put("pricedQty", exposureObject.optDouble("pricedQty", 0) * actualConversionFactor1);
				exposureObject.put("unPricedQty", exposureObject.optDouble("unPricedQty", 0) * actualConversionFactor1);
			}else if(exposureObject.optDouble("contractualConversionFactor")!=0){
				exposureObject.put("pricedQty", exposureObject.optDouble("pricedQty", 0) * contractualConversionFactor);
				exposureObject.put("unPricedQty", exposureObject.optDouble("unPricedQty", 0) * contractualConversionFactor);
			}
			
			newerExposureArr.put(exposureObject);
		}
		PreviewData previewData = fields.getPreviewData();
		previewData.setCollapse("0");
		previewData.setExchange(exchangeCode);
		previewData.setCurveName(c.getCurveName());
		previewData.setCurvePrice(c.getCurvePrice());
		previewData.setCurveCurrency(c.getCurveCurrency());
		previewData.setCoefficient(c.getCoefficient());
		previewData.setCurveQtyUnit(c.getCurveQty());
		previewData.setQtyUnit(contractItemQty);
		previewData.setBaseQtyUnit(baseQtyUnit);
		previewData.setPriceUnit(c.getCurveCurrency() + "/" + c.getCurveQty());
		previewData.setQpStartDate(collectionDataFetcher.constructDateStr(c.getQpFromDate()));
		previewData.setQpEndDate(collectionDataFetcher.constructDateStr(c.getQpToDate()));
		previewData.setData(curveArr);
		previewData.setPricedQty(c.getPricedQty());
		previewData.setUnPricedQty(c.getUnpricedQty());
		previewData.setStatus(baseQtyUnitId[1]);
		previewData.setRemarks(baseQtyUnitId[2]);
		if(!StringUtils.isEmpty(c.getComponent())) {
			previewData.setComponentName(c.getComponent());
		}
		previewData.setQtyData(exposureArr);
		previewData.setExposureArray(newerExposureArr);
		previewData.setBaseContractualConversionFactor(baseContractualConversionFactor);
		previewData.setAvgPriceFx(c.getAvgFx());
		previewData.setGmrToItemConversionFactor(gmrToItemConversionFactor);
		previewData.setTotalGmrQty(totalGmrQty);
		return new JSONObject(previewData);
	}

	public CurveCalculatorFields initialize(CurveCalculatorFields fieldObj, Curve c, JSONObject itemObj, GMR gmr,
			String lookbackDate, LocalDate asOf, ContextProvider tenantProvider) throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
		if (c.getPricePoint().equals("Forward") && c.getQuotedPeriod().toLowerCase().contains("m+")) {
			fieldObj.setDiffMonth(Integer.parseInt(c.getQuotedPeriod().substring(c.getQuotedPeriod().length()-1)));
		}
		PreviewData prevDataObj = new PreviewData();
		fieldObj.setPreviewData(prevDataObj);
		if ((c.getPriceQuoteRule().equals("Contract Period Average")
				|| c.getPriceQuoteRule().equals("ContractPeriodAverage")) && itemObj != null) {
			fieldObj.setSd(convertISOtoLocalDate(itemObj.optString("deliveryFromDate", "")));
			fieldObj.setEd(convertISOtoLocalDate(itemObj.optString("deliveryToDate", "")));
		} else if (c.getPriceQuoteRule().equals("Event Offset Based") && itemObj != null) {
			String eventDateStr = null;
			if (null != gmr) {
				List<Event> eventList = gmr.getEvent();
				for (Event e : eventList) {
					if (e.getName().contentEquals(c.getEvent())) {
						eventDateStr = e.getDate();
					}
				}
			}
			if (null == eventDateStr) {
				LocalDate delFromDate = sdf.parse(itemObj.get("deliveryFromDate").toString()).toInstant()
						.atZone(ZoneId.systemDefault()).toLocalDate();
				LocalDate delToDate = sdf.parse(itemObj.get("deliveryToDate").toString()).toInstant()
						.atZone(ZoneId.systemDefault()).toLocalDate();
				long numOfDays = delFromDate.until(delToDate, ChronoUnit.DAYS) / 2;
				eventDateStr = delFromDate.plusDays(numOfDays).toString();
			}
			String offset = c.getOffset();
			if (null == offset) {
				throw new PricingException(
						messageFetcher.fetchErrorMessage(tenantProvider, "017", new ArrayList<String>()));
			}
			int prev = 0;
			int mid = 0;
			int next = 0;
			if (!c.getEvent().equals("Week of BL")) {
				prev = Integer.parseInt(offset.substring(0, offset.indexOf("-")));
				mid = Integer.parseInt(offset.substring(offset.indexOf("-") + 1, offset.lastIndexOf("-")));
				next = Integer.parseInt(offset.substring(offset.lastIndexOf("-") + 1, offset.length()));
			}
			fieldObj.setMidEventDay(sdf.parse(eventDateStr).toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
			if (c.getEvent().equals("Week of BL")) {
				LocalDate weekDay = fieldObj.getMidEventDay();
				if (offset.contains("-")) {
					offset = offset.substring(0, offset.indexOf("-"));
				}
				DayOfWeek dayVar = null;
				int num = 0;
				if (offset.contains("Mon")) {
					dayVar = DayOfWeek.MONDAY;
				} else if (offset.contains("Tue")) {
					dayVar = DayOfWeek.TUESDAY;
				} else if (offset.contains("Wed")) {
					dayVar = DayOfWeek.WEDNESDAY;
				} else if (offset.contains("Thu")) {
					dayVar = DayOfWeek.THURSDAY;
				} else if (offset.contains("Fri")) {
					dayVar = DayOfWeek.FRIDAY;
				} else if (offset.contains("Sat")) {
					dayVar = DayOfWeek.SATURDAY;
				} else if (offset.contains("Sun")) {
					dayVar = DayOfWeek.SUNDAY;
				}

				if (null == dayVar) {
					throw new PricingException(
							messageFetcher.fetchErrorMessage(tenantProvider, "017", new ArrayList<String>()));
				}
				int daysToConsider = 0;
				if (weekDay.getDayOfWeek() == DayOfWeek.MONDAY) {
					daysToConsider = 4;
				} else {
					daysToConsider = 6;
				}
				if (weekDay.getDayOfWeek().compareTo(dayVar) < -1) {
					num = weekDay.getDayOfWeek().getValue() - dayVar.getValue();
					weekDay = weekDay.minusDays(num);
				} else if (weekDay.getDayOfWeek().compareTo(dayVar) > 1) {
					num = dayVar.getValue() - weekDay.getDayOfWeek().getValue();
					weekDay = weekDay.plusDays(num);
				}
				fieldObj.setSd(weekDay);
				fieldObj.setEd(weekDay.plusDays(daysToConsider));

			} else if (c.getOffsetType().equals("Day")) {

				fieldObj.setSd(sdf.parse(eventDateStr).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
						.minusDays(prev));
				if(mid == 0 && next == 0) {
					fieldObj.setEd(fieldObj.getSd());
				}
				else if (mid == 0 && prev == 0) {
					fieldObj.setEd(sdf.parse(eventDateStr).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
							.plusDays(next));
					fieldObj.setSd(sdf.parse(eventDateStr).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
							.plusDays(1));
				}
				else {
					fieldObj.setEd(sdf.parse(eventDateStr).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
							.plusDays(next));
				}
				
			} else if (c.getOffsetType().equals("Month")) {
				int year = fieldObj.getMidEventDay().getYear();
				int month = fieldObj.getMidEventDay().getMonthValue();
				if (prev >= month) {
					year = year - 1;
					month = (month + 12) - prev;
				}
				String startDate = Integer.toString(year) + "-" + Integer.toString(month) + "-" + "01";
				ValueRange range = sdf.parse(startDate).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
						.range(ChronoField.DAY_OF_MONTH);
				String endDate = Integer.toString(year) + "-" + Integer.toString(month) + "-" + range.getMaximum();
				fieldObj.setSd(sdf.parse(startDate).toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
				fieldObj.setEd(sdf.parse(endDate).toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
			} else if (c.getOffsetType().equals("Quarter")) {
				int year = fieldObj.getMidEventDay().getYear();
				int month = fieldObj.getMidEventDay().getMonthValue();
				int ident = 0;
				if (month < 3) {
					year = year - 1;
					month = 13;
				}
				ident = month / 3;
				if ((month % 3) == 0) {
					ident = (month / 3) - 1;
				}

				int startMonth = (ident * 3) - 2;
				int endMonth = ident * 3;
				String startDate = Integer.toString(year) + "-" + Integer.toString(startMonth) + "-" + "01";
				String endDate = Integer.toString(year) + "-" + Integer.toString(endMonth) + "-" + "01";
				ValueRange range = sdf.parse(endDate).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
						.range(ChronoField.DAY_OF_MONTH);
				endDate = Integer.toString(year) + "-" + Integer.toString(endMonth) + "-" + range.getMaximum();
				fieldObj.setSd(sdf.parse(startDate).toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
				fieldObj.setEd(sdf.parse(endDate).toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
			} else {
				throw new PricingException(
						messageFetcher.fetchErrorMessage(tenantProvider, "021", new ArrayList<String>()));
			}
			if (mid == 0) {
				fieldObj.setSkipMidIfEvent(true);
			}
			fieldObj.setMidEventDay(sdf.parse(eventDateStr).toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
		} else if (c.getPriceQuoteRule().equals("Lookback Pricing")) {
			String offset = c.getOffset().trim();
			int prev = 0;
			int mid = 0;
			int next = 0;
			prev = Integer.parseInt(offset.substring(0, offset.indexOf("-")));
			mid = Integer.parseInt(offset.substring(offset.indexOf("-") + 1, offset.lastIndexOf("-")));
			next = Integer.parseInt(offset.substring(offset.lastIndexOf("-") + 1, offset.length()));
			if (next == 0) {
				throw new PricingException(
						messageFetcher.fetchErrorMessage(tenantProvider, "022", new ArrayList<String>()));
			}

			LocalDate eventDay = asOf;
			int eventYear = eventDay.getYear();
			int eventMonth = eventDay.getMonthValue();
			if (lookbackDate.length() == 0) {
				throw new PricingException(
						messageFetcher.fetchErrorMessage(tenantProvider, "023", new ArrayList<String>()));
			}
			int lookbackMonth = sdf.parse(lookbackDate).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
					.getMonthValue();
			int lookbackYear = sdf.parse(lookbackDate).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
					.getYear();
			String[] datesArr = new String[2];
			if (eventYear < lookbackYear || eventMonth < lookbackMonth) {
				datesArr = lookBackDurationCalculator(prev, mid, next, lookbackYear, lookbackMonth, true, lookbackDate,
						tenantProvider);
			} else {
				datesArr = lookBackDurationCalculator(prev, mid, next, eventYear, eventMonth, false, lookbackDate,
						tenantProvider);
			}

			fieldObj.setSd(sdf.parse(datesArr[0]).toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
			fieldObj.setEd(sdf.parse(datesArr[1]).toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
		} else if (null == c.getStartDate() || null == c.getEndDate()) {
			throw new PricingException(
					messageFetcher.fetchErrorMessage(tenantProvider, "024", new ArrayList<String>()));
		} else {
			fieldObj.setSd(convertISOtoLocalDate(c.getStartDate()));
			fieldObj.setEd(convertISOtoLocalDate(c.getEndDate()));
		}
		if(null == c.getCollectionArray() || c.getCollectionArray().size() == 0 || gmr!=null) {
			JSONArray CollectionArray = collectionDataFetcher.triggerRequest(c, tenantProvider, fieldObj.getSd(),
					fieldObj.getEd(), asOf);
			List<CurveMarketData> marketDataList = new ArrayList<CurveMarketData>();
			Map<LocalDate, CurveMarketData> priceDetailsMap = new HashMap<LocalDate, CurveMarketData>();
			for(int k=0 ; k<CollectionArray.length(); k++) {
				JSONObject dailyObj = CollectionArray.getJSONObject(k);
				CurveMarketData curveMarketData = new CurveMarketData();
				curveMarketData.setExchange(dailyObj.optString("Exchange"));
				if(null==c.getExchange()) {
					c.setExchange(dailyObj.optString("Exchange"));
				}
				curveMarketData.setInstrumentName(dailyObj.optString("Instrument Name"));
				if (!dailyObj.has(c.getPriceType())) {
					curveMarketData.setPrice(dailyObj.optString("NA"));
				}
				else {
					curveMarketData.setPrice(dailyObj.optString(c.getPriceType()));
				}
				curveMarketData.setPriceUnit(dailyObj.optString("Price Unit"));
				String pricingDate = dailyObj.optString("Pricing Date");
				String promptDate = dailyObj.optString("Prompt Date");
				if(pricingDate.contains("T")) {
					pricingDate = pricingDate.replaceAll("T", " ");
				}
				if(promptDate.contains("T")) {
					promptDate = promptDate.replaceAll("T", " ");
				}
				try {
					curveMarketData.setPricingDate(LocalDate.parse(pricingDate, formatter1));
					if(c.getPricePoint().equalsIgnoreCase("forward")) {
						curveMarketData.setPromptDate(LocalDate.parse(promptDate, formatter1));
					}
				}
				catch (Exception e) {
					logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("pricing or prompt date not in correct format"));
					continue;
				}
				if (c.getPricePoint().equalsIgnoreCase("forward")
						&& curveMarketData.getPricingDate().isAfter(curveMarketData.getPromptDate())) {
					logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("skipping the prices for pricing days after prompt date"));
					continue;
				}
				if(!priceDetailsMap.containsKey(LocalDate.parse(pricingDate, formatter1))) {
					priceDetailsMap.put(LocalDate.parse(pricingDate, formatter1), curveMarketData);
				}
				if(null==c.getFirstPricingDate()) {
					c.setFirstPricingDate(curveMarketData.getPricingDate());
				}
				curveMarketData.setMonthYear(dailyObj.optString("Month/Year"));
				marketDataList.add(curveMarketData);
			}
			c.setPriceDetailsMap(priceDetailsMap);
			c.setCollectionArray(marketDataList);
		}
		return fieldObj;
	}

	public String fetchCurrency(JSONArray collectionData, Curve c) {

		for (int i = 0; i < collectionData.length(); i++) {

			JSONObject jObj = (JSONObject) collectionData.get(i);
			if (!jObj.get("Name").equals(c.getCurveName())) {
				continue;
			}
			String curr = jObj.getString("Currency Code");
			return curr;
		}
		return null;
	}

	public String fetchUnitQty(JSONArray collectionData, Curve c) {

		for (int i = 0; i < collectionData.length(); i++) {

			JSONObject jObj = (JSONObject) collectionData.get(i);
			if (!jObj.get("Name").equals(c.getCurveName())) {
				continue;
			}
			String unit = jObj.getString("Lot Units");
			return unit;
		}
		return null;
	}

	public String[] lookBackDurationCalculator(int prev, int mid, int next, int eventYear, int eventMonth,
			boolean isFuture, String lookbackDate, ContextProvider tenantProvider) throws ParseException, PricingException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		LocalDate asOf = LocalDate.of(eventYear, eventMonth, 1);
		LocalDate lookbackfromDate = sdf.parse(lookbackDate).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		if (asOf.isBefore(lookbackfromDate)) {
			/*Removing this exception to handle the feature as part of CPR-1581
			 * throw new PricingException(
					messageFetcher.fetchErrorMessage(tenantProvider, "025", new ArrayList<String>()));*/
			asOf = lookbackfromDate;
		}
		List<LocalDate> lookbackSlot = getLookbackSlot(lookbackfromDate, next, asOf);

		int slotCount = 0;
		LocalDate slotDate = LocalDate.now();
		if (asOf.equals(lookbackfromDate)) {
			slotDate = asOf;
		} else {
			while (slotCount < lookbackSlot.size() && !asOf.isBefore(lookbackSlot.get(slotCount))) {
				slotCount++;
			}

			slotDate = lookbackSlot.get(slotCount - 1);
		}

		LocalDate initialDate = slotDate.minusMonths(prev + mid);
		LocalDate finalDate = initialDate.plusMonths(prev-1);
		String startDate = null;
		String endDate = null;

		startDate = Integer.toString(initialDate.getYear()) + "-" + Integer.toString(initialDate.getMonthValue()) + "-"
				+ "01";
		endDate = Integer.toString(finalDate.getYear()) + "-" + Integer.toString(finalDate.getMonthValue()) + "-"
				+ "01";
		ValueRange range = sdf.parse(endDate).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
				.range(ChronoField.DAY_OF_MONTH);
		endDate = Integer.toString(finalDate.getYear()) + "-" + Integer.toString(finalDate.getMonthValue()) + "-"
				+ range.getMaximum();

		String[] resSet = new String[2];
		if (null == startDate || null == endDate) {
			throw new PricingException(
					messageFetcher.fetchErrorMessage(tenantProvider, "023", new ArrayList<String>()));
		}
		resSet[0] = startDate;
		resSet[1] = endDate;

		return resSet;
	}
	
	public List<LocalDate> getLookbackSlot(LocalDate lookbackDate, int noOfMonth, LocalDate asOf) {
		List<LocalDate> lookbackSlot = new ArrayList<LocalDate>();
		while(!lookbackDate.isAfter(asOf)) {
			lookbackSlot.add(lookbackDate);
			lookbackDate = lookbackDate.plusMonths(noOfMonth);
		}
		
		return lookbackSlot;
	}

	public LocalDate convertISOtoLocalDate(String ISO) throws PricingException {
		org.joda.time.LocalDate jodaDate = new org.joda.time.LocalDate();
		try {
			DateTime dateTime = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ").withZone(DateTimeZone.UTC)
					.parseDateTime(ISO);
			jodaDate =  dateTime.toLocalDate();
		}
		catch (Exception e) {
			DateTime dateTime = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ").withZone(DateTimeZone.UTC)
					.parseDateTime(ISO);
			jodaDate =  dateTime.toLocalDate();
		}
		return java.time.LocalDate.of(jodaDate.getYear(), jodaDate.getMonthOfYear(), jodaDate.getDayOfMonth());
	}
	
	public LocalDateTime convertISOtoLocalDateTime(String ISO) throws PricingException {
		org.joda.time.LocalDateTime jodaDate = new org.joda.time.LocalDateTime();
		try {
			DateTime dateTime = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ").withZone(DateTimeZone.UTC)
					.parseDateTime(ISO);
			jodaDate =  dateTime.toLocalDateTime();
		}
		catch (Exception e) {
			DateTime dateTime = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ").withZone(DateTimeZone.UTC)
					.parseDateTime(ISO);
			jodaDate =  dateTime.toLocalDateTime();
		}
		return java.time.LocalDateTime.of(jodaDate.getYear(), jodaDate.getMonthOfYear(), jodaDate.getDayOfMonth(), jodaDate.getHourOfDay(), jodaDate.getMinuteOfHour());
	}

	public HolidayRuleDates getListOfDaysPostHolidayRule(CurveCalculatorFields fields, String exchange,
			ContextProvider tenantProvider, String holidayRule, Curve c) throws JSONException, PricingException {
		DateTimeFormatter expiryDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
		LocalDateTime fromDate = fields.getSd().atStartOfDay();
		LocalDateTime toDate = fields.getEd().plusDays(1).atStartOfDay();
		List<LocalDateTime> datesToBeUsed = new ArrayList<LocalDateTime>();
		List<LocalDateTime> dateList = new ArrayList<LocalDateTime>();
		if (c.getPriceQuoteRule().equals("Event Offset Based")) {
			int noOfDays = c.getOffsetDays();
			LocalDate startDate = fromDate.withDayOfMonth(1).toLocalDate();
			if (c.isExp() == true && null == c.getGmrQPDetailsList()) {
				LocalDate date = fields.getEd().with(TemporalAdjusters.lastDayOfMonth());
				dateList.add(date.atStartOfDay());
			} else if (c.getOffsetType().equals("Month") && !c.getMonthDefinition().equalsIgnoreCase("NA")) {

				if (!StringUtils.isEmpty(c.getMonthDefinition()) && c.getMonthDefinition().equals("EOM")) {
					startDate = startDate.withDayOfMonth(startDate.getMonth().maxLength());
				}
				int initializedValue = startDate.getMonthValue();
				Period age = Period.between(fromDate.toLocalDate(), toDate.toLocalDate());
				int finalizedValue = initializedValue + age.getYears() * 12 + age.getMonths();
				while (initializedValue != finalizedValue) {
					int count = 0;
					while (count < noOfDays) {
						if (c.getMonthDefinition().equals("BOM")) {
							dateList.add(startDate.plusDays(count).atStartOfDay());
						} else if (c.getMonthDefinition().equals("EOM")) {
							dateList.add(startDate.minusDays(count).atStartOfDay());
						} else {
							throw new PricingException(
									messageFetcher.fetchErrorMessage(tenantProvider, "026", new ArrayList<String>()));
						}
						count++;
					}
					initializedValue++;
					startDate = startDate.plusMonths(1).withDayOfMonth(1);
					if (!StringUtils.isEmpty(c.getMonthDefinition()) && c.getMonthDefinition().equals("EOM")) {
						startDate = startDate.withDayOfMonth(startDate.getMonth().maxLength());
					}
				}
			} else {
				dateList = Stream.iterate(fromDate, date -> date.plusDays(1))
						.limit(ChronoUnit.DAYS.between(fromDate, toDate)).collect(Collectors.toList());
			}
		} else if (c.getPriceQuoteRule().equals("Settlement Date") && c.getOffsetType().equals("Month")) {
			JSONArray expiryArr = expiryCalenderFetcher.getData(c, tenantProvider);
			LocalDate startDatePricing=null;
			LocalDate datePricing=null;
			LocalDate priceDate=null;
			String offset = c.getOffset().trim();
			
			int prev = Integer.parseInt(offset.substring(0, offset.indexOf("-")));
			int next = Integer.parseInt(offset.substring(offset.lastIndexOf("-") + 1, offset.length()));
			String pricePeriod= c.getPricingPeriod();
			
			priceDate = calculateLocalDateFromMonthYearString(pricePeriod, tenantProvider);
			
			startDatePricing = priceDate.minusMonths(prev);
			datePricing = startDatePricing;
			int lengthMonthYear = prev+next;
			
			for(int i=0;i<=lengthMonthYear;i++) {
				startDatePricing = datePricing.plusMonths(i);
				String quotedMonthYear = startDatePricing.getMonth().toString().substring(0, 3).toUpperCase()
						+ Integer.toString(priceDate.getYear());
				
				JSONObject currMonthExpiryObj = new JSONObject();
				for (int j = 0; j < expiryArr.length(); j++) {
					JSONObject expiryObj = expiryArr.getJSONObject(j);
					if (expiryObj.optString("MONTH/YEAR").equals(quotedMonthYear)) {
						currMonthExpiryObj = expiryObj;
					} 
				}
				try {
					LocalDateTime priceDdate = LocalDateTime.parse(currMonthExpiryObj.optString("Settlement Date"),
							expiryDateFormatter);
			        dateList.add(priceDdate);
				}
				catch (Exception e) {
					throw new PricingException(
							messageFetcher.fetchErrorMessage(tenantProvider, "030", new ArrayList<String>()));
				}

				logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("Settlement Date range:" + dateList));

			}
		} else {
			dateList = Stream.iterate(fromDate, date -> date.plusDays(1))
					.limit(ChronoUnit.DAYS.between(fromDate, toDate)).collect(Collectors.toList());
		}
		
		//Below code is to skip the middle month or week in case of event type pricing
		if(null!=fields.getMidEventDay()) {
			List<LocalDateTime> exclusionList = new ArrayList<LocalDateTime>();
			LocalDateTime event = fields.getMidEventDay().atStartOfDay();
			LocalDateTime firstExclusionDate = event;
			if(c.getOffsetType().equals("Month")) {
				firstExclusionDate = LocalDateTime.of(event.getYear(), event.getMonth(), 1, 0, 0);
				while(firstExclusionDate.getMonthValue() == event.getMonthValue()) {
					exclusionList.add(firstExclusionDate);
					firstExclusionDate = firstExclusionDate.plusDays(1);
				}
			}
			else if(c.getOffsetType().equals("Week")) {
				firstExclusionDate = event.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
				int dayCount = 0;
				while(dayCount < 7) {
					exclusionList.add(firstExclusionDate);
					firstExclusionDate = firstExclusionDate.plusDays(1);
					dayCount++;
				}
			}
			
			for(LocalDateTime date : exclusionList) {
				if (c.getPriceQuoteRule().equals("Event Offset Based") && null == c.getGmrQPDetailsList() && c.isExp() == true) {
					if(!dateList.contains(date)) {
						dateList.remove(date);
					}
				}else {
				   if(dateList.contains(date)) {
					dateList.remove(date);
				  }
				}
			}
		}
		
		
		HolidayRuleDetails holidayRuleDetail = new HolidayRuleDetails();
		if (null != holidayRule && !holidayRule.trim().isEmpty()) {
			holidayRuleDetail.setExchangeName(exchange);
			holidayRuleDetail.setDateRange(dateList);
			if (dateList.size() == 1 && holidayRule.equals("Ignore Holidays")) {
				holidayRuleDetail.setHolidayRule("Prior Business Day");
			} else {
				holidayRuleDetail.setHolidayRule(holidayRule);
			}
		} else {
			holidayRuleDetail.setExchangeName(exchange);
			holidayRuleDetail.setDateRange(dateList);
			holidayRuleDetail.setHolidayRule("Prior Business Day");
		}
		JSONArray holidayObj = new JSONArray(curveService.applyHolidayRule(holidayRuleDetail, tenantProvider));
		DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

		for (int i = 0; i < holidayObj.length(); i++) {
			JSONObject jobj = holidayObj.getJSONObject(i);
//For ignore weekend rule, holidayRuleCalculator will return NA for holiday and weekend. Handling the same.		
			if (jobj.optString("dateToBeUsed").equals("NA")) {
				dateList.set(i, null);
				continue;
			}
			datesToBeUsed.add(LocalDateTime.parse(jobj.optString("dateToBeUsed"), formatter));
		}
		if (c.getPriceQuoteRule().equals("Prompt Period Avg")) {
			while(null == dateList.get(0)) {
				dateList.remove(0);
			}
			while (!dateList.get(0).equals(datesToBeUsed.get(0))) {
				dateList.remove(0);
				datesToBeUsed.remove(0);
			}
		}
		while (dateList.remove(null)) {
		}
		HolidayRuleDates holidayRuleDatesObj = new HolidayRuleDates();
		holidayRuleDatesObj.setDatesList(dateList);
		holidayRuleDatesObj.setDateToBeUsed(datesToBeUsed);
		return holidayRuleDatesObj;
	}

	public double getDailyPricedQty(Curve c) {
		double pricedQty = c.getPricedQty();
		int pricedDays = c.getPricedDays();
		double unpricedQty = c.getUnpricedQty();
		int unpricedDays = c.getUnPricedDays();
		if (pricedDays == 0 && unpricedDays == 0) {
			return 0;
		} 
		else if(pricedDays == 0 && unpricedDays != 0) {
			return unpricedQty/unpricedDays;
		}
		else {
			return pricedQty / pricedDays;
		}
	}

	public double findLastKnownPrice(Map<LocalDate, CurveMarketData> priceDetailsMap, LocalDate date,
			LocalDate firstPricingDate, ContextProvider tenantProvider, LocalDate asOf, Curve c) throws Exception {
		if (date.isBefore(firstPricingDate)) {
			JSONObject lastKnownPriceObj = getLastKnownPricesForFutureCurves(c, date, asOf, tenantProvider);
			if(null!=lastKnownPriceObj) {
				return lastKnownPriceObj.optDouble(c.getPriceType());
			}
			List<String> params = new ArrayList<String>();
			params.add(date.toString());		
			throw new PricingException(
					messageFetcher.fetchErrorMessage(tenantProvider, "027", params));
		}
		while(!date.isBefore(firstPricingDate)) {
			if(priceDetailsMap.containsKey(date)) {
				try {
					return Double.parseDouble(priceDetailsMap.get(date).getPrice());
				}
				catch (Exception e){
					return 0;
				}
			}
			else {
				date = date.minusDays(1);
			}
		}
		List<String> params = new ArrayList<String>();
		params.add(date.toString());		
		throw new PricingException(
				messageFetcher.fetchErrorMessage(tenantProvider, "027", params));
	}
	
	public double findLastKnownPriceForSettlementDate(Map<LocalDate, CurveMarketData> priceDetailsMap, LocalDate date,
			LocalDate firstPricingDate, ContextProvider tenantProvider, LocalDate asOf, Curve c,String settlementMonthYear) throws Exception {
		if (date.isBefore(firstPricingDate)) {
				 JSONObject settlementPriceObj = getLastKnownPricesForFutureCurvesSettlementDate(c, date, asOf, tenantProvider);
			 
			if(null!=settlementPriceObj) {
				return settlementPriceObj.optDouble(c.getPriceType());
			}
			List<String> params = new ArrayList<String>();
			params.add(settlementMonthYear);		
			throw new PricingException(
					messageFetcher.fetchErrorMessage(tenantProvider, "032", params));
		}
		while(!date.isBefore(firstPricingDate)) {
			if(priceDetailsMap.containsKey(date) && priceDetailsMap.get(date).getMonthYear().equalsIgnoreCase(settlementMonthYear)) {
				try {
					return Double.parseDouble(priceDetailsMap.get(date).getPrice());
				}
				catch (Exception e){
					return 0;
				}
			}
			else {
				date = date.minusDays(1);
			}
		}
		List<String> params = new ArrayList<String>();
		params.add(settlementMonthYear);		
		throw new PricingException(
				messageFetcher.fetchErrorMessage(tenantProvider, "032", params));
	}
	
	public String findLastKnownMonth(Map<LocalDate, CurveMarketData> priceDetailsMap, LocalDate date,
			LocalDate firstPricingDate, ContextProvider tenantProvider, LocalDate asOf, Curve c) throws Exception {
		if (date.isBefore(firstPricingDate)) {
			JSONObject lastKnownPriceObj = getLastKnownPricesForFutureCurves(c, date, asOf, tenantProvider);
			if (null != lastKnownPriceObj) {
				return lastKnownPriceObj.optString("MONTH/YEAR");
			}
			List<String> params = new ArrayList<String>();
			params.add(date.toString());		
			throw new PricingException(
					messageFetcher.fetchErrorMessage(tenantProvider, "049", params));
		}
		while(!date.isBefore(firstPricingDate)) {
			if(priceDetailsMap.containsKey(date)) {
				return priceDetailsMap.get(date).getMonthYear();
			}
			else {
				date = date.minusDays(1);
			}
		}
		List<String> params = new ArrayList<String>();
		params.add(date.toString());		
		throw new PricingException(
				messageFetcher.fetchErrorMessage(tenantProvider, "049", params));
	}
	
//calculating exposures when event is passed in GMR. 	Jira: EPC-1337
	public JSONArray getExposureForGMR(List<GMRExposureFields> gmrExposureFieldList, LocalDate asOf, double itemQty,
			Curve c, String holidayRule, ContextProvider tenantProvider, JSONObject itemObj, double baseContractualConversionFactor, 
			Map<String, Double> baseActualConversionFactor,Map<String, Double>  actualConversionFactor,double contractualConversionFactor,
			Map<String, Double> gmrToItemConversionFactor,String locationType,List<TriggerPrice> triggerPriceList,JSONObject jobj,double totalGmrQty,
			int count) throws PricingException, ParseException, Exception {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		JSONArray expArray = new JSONArray();
		double unpricedQty = itemQty;
		LocalDate asOnDate = null;
		Map<LocalDate, CurveMarketData> priceDetailsMap =c.getPriceDetailsMap();
		JSONArray collectionArray = new JSONArray();
		Map<LocalDate, Integer> dateCountMap = new HashMap<LocalDate, Integer>();
		LocalDate finalDateInGMR = null;
		List<LocalDateTime> validDatesList = new ArrayList<LocalDateTime>();
		 for (GMRExposureFields gmrExpField : gmrExposureFieldList) {
			 List<Event> eventList = gmrExpField.getGmr().getEvent();
				for (Event e : eventList) {
					asOnDate = sdf.parse(e.getDate()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
					if (e.getName().contentEquals(c.getEvent())) {
						count++;
						getDateRangeForGMR(c, holidayRule, gmrExpField, tenantProvider, itemObj,asOf,collectionArray);
						validDatesList.addAll(gmrExpField.getValidDates());
					
					for (LocalDateTime date : validDatesList) {
						if (dateCountMap.containsKey(date.toLocalDate())) {
							dateCountMap.put(date.toLocalDate(), dateCountMap.get(date.toLocalDate()) + 1);
						} else {
							dateCountMap.put(date.toLocalDate(), 1);
						}
					}
					 double gmrQty=gmrExpField.getStockQtyInGmr();
					 double dailyQty = gmrQty / gmrExpField.getRealDates().size();
					for (LocalDateTime date : gmrExpField.getValidDates()) {
						JSONObject expObj = new JSONObject();
						if (dateCountMap.containsKey(date.toLocalDate())) {
								expObj.put("date", date);
								expObj.put("pricedQty", dateCountMap.get(date.toLocalDate()) * dailyQty* gmrToItemConversionFactor.get(gmrExpField.getGmr().getRefNo()));
								unpricedQty = unpricedQty - (dateCountMap.get(date.toLocalDate()) * dailyQty);
								expObj.put("unPricedQty", 0);
								expObj.put("pricedQuantityInBaseQtyUnit", dateCountMap.get(date.toLocalDate()) * 
										dailyQty *baseActualConversionFactor.get(gmrExpField.getGmr().getRefNo()) * gmrToItemConversionFactor.get(gmrExpField.getGmr().getRefNo()));
								expObj.put("unpricedQuantityInBaseQtyUnit", 0);
								expObj.put("pricedPercentage", dateCountMap.get(date.toLocalDate())*(dailyQty/ itemQty) * 100);
								expObj.put("unpricedPercentage", 0);
								expObj.put("contractualConversionFactor", 0);
								expObj.put("actualConversionFactor", 1);
								expObj.put("gmrRefNo", gmrExpField.getGmr().getRefNo());
								expObj.put("titleTransferStatus", gmrExpField.getGmr().getTitleTransferStatus());
								String locationName=gmrExpField.getGmr().getStorageLocation();
								if(StringUtils.isEmpty(locationName)) {
									if(!StringUtils.isEmpty(locationType)) {
										if(locationType.equalsIgnoreCase("ORIGINATION")) {
											locationName=gmrExpField.getGmr().getLoadingLocName();
										}else {
											locationName=gmrExpField.getGmr().getDestinationLocName();
										}
									}
									
								}
								expObj.put("locationName", locationName);
								
								if (null != priceDetailsMap.get(date.toLocalDate())) {
									expObj.put("instrumentDeliveryMonth",
											priceDetailsMap.get(date.toLocalDate()).getMonthYear());
								} else {
									String month = date.getMonth().toString().substring(0, 3);
									String year = Integer.toString(date.getYear());
									expObj.put("instrumentDeliveryMonth", month + year);
								}
							dateCountMap.remove(date.toLocalDate());
							if (null == finalDateInGMR || finalDateInGMR.isBefore(date.toLocalDate())) {
								finalDateInGMR = date.toLocalDate().plusDays(1);
							}
						
							expArray.put(expObj);
						}
					}
					validDatesList.removeAll(gmrExpField.getValidDates());
				}else {
					asOnDate = sdf.parse(e.getDate()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
					finalDateInGMR =asOnDate;
					expArray= getExpForNotEventDate(gmrExposureFieldList, asOf, c.getQty(), c, holidayRule, tenantProvider,
							itemObj,  baseContractualConversionFactor,baseActualConversionFactor,actualConversionFactor,gmrToItemConversionFactor,locationType,
							expArray,gmrExpField);
				}
			
		}
	   }
		 
		 if(count!=0) {
			 LocalDateTime date =null;
			 double remainingQty= itemQty-totalGmrQty;
			 if(null!=asOnDate && asOnDate.isAfter(finalDateInGMR)) {
					finalDateInGMR=asOnDate;
					date = finalDateInGMR.with(TemporalAdjusters.lastDayOfMonth()).atStartOfDay();
				}else {
				 date = finalDateInGMR.with(TemporalAdjusters.lastDayOfMonth()).atStartOfDay();
			}
			 //Market Valuation Curve
			if (remainingQty>0) {
				JSONObject expObj = new JSONObject();
					expObj.put("date", date);
					expObj.put("pricedQty", remainingQty);
					expObj.put("unPricedQty", 0);
					expObj.put("pricedQuantityInBaseQtyUnit", remainingQty*baseContractualConversionFactor);
					expObj.put("unpricedQuantityInBaseQtyUnit", 0);
					expObj.put("pricedPercentage", ((itemQty - totalGmrQty)/ itemQty) * 100);
					expObj.put("unpricedPercentage", 0);
					expObj.put("contractualConversionFactor", contractualConversionFactor);
					expObj.put("actualConversionFactor", 0);
					expObj.put("gmrRefNo", "Outturn Loss");
					if (null != priceDetailsMap.get(date.toLocalDate())) {
						expObj.put("instrumentDeliveryMonth",
								priceDetailsMap.get(date.toLocalDate()).getMonthYear());
					} else {
						String month = date.getMonth().toString().substring(0, 3);
						String year = Integer.toString(date.getYear());
						expObj.put("instrumentDeliveryMonth", month + year);
					}
					expArray.put(expObj);
			}
			// Normal Curve
			if(totalGmrQty>itemQty) {
				itemQty=totalGmrQty;
			 }
			
			if(triggerPriceList.size()>0) {
				 itemQty=itemQty-c.getTotalTriggerPriceQty();
				}
			
				JSONObject expObj = new JSONObject();
				expObj.put("date", date);
				expObj.put("pricedQty", 0);
				expObj.put("unPricedQty", itemQty);
				expObj.put("pricedQuantityInBaseQtyUnit", 0);
				expObj.put("unpricedQuantityInBaseQtyUnit", itemQty*baseContractualConversionFactor);
				expObj.put("pricedPercentage", 0);
				expObj.put("unpricedPercentage", 100);
				expObj.put("contractualConversionFactor", contractualConversionFactor);
				expObj.put("actualConversionFactor", 0);
				if (null != priceDetailsMap.get(date.toLocalDate())) {
					expObj.put("instrumentDeliveryMonth",
							priceDetailsMap.get(date.toLocalDate()).getMonthYear());
				} else {
					String month = date.getMonth().toString().substring(0, 3);
					String year = Integer.toString(date.getYear());
					expObj.put("instrumentDeliveryMonth", month + year);
				}
					expArray.put(expObj);
			
			
		 }
		 
		
		return expArray;
	}

	public void getDateRangeForGMR(Curve c, String holidayRule, GMRExposureFields gmrExposureField,
			ContextProvider tenantProvider, JSONObject itemObj,LocalDate asOf,JSONArray collectionArray ) throws PricingException, ParseException,Exception {
		GMR gmr = gmrExposureField.getGmr();
		int nearbyMonths = 0;
		CurveCalculatorFields fields = new CurveCalculatorFields();
		if(c.getQuotedPeriod().contains("M+")) {
			nearbyMonths = Integer.parseInt(c.getQuotedPeriod().substring(c.getQuotedPeriod().length()-1));
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String eventName = c.getEvent();
		JSONArray expiryArr = expiryCalenderFetcher.getData(c, tenantProvider);
	     LocalDate asOnDate = null;
		String dateStr="";
		for (Event e : gmr.getEvent()) {
			if (e.getName().equals(eventName)) {
				asOnDate = sdf.parse(e.getDate()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			}
		}
		dateStr=asOnDate.toString();
		if (dateStr.length() == 0) {
			LocalDate delFromDate = sdf.parse(itemObj.get("deliveryFromDate").toString()).toInstant()
					.atZone(ZoneId.systemDefault()).toLocalDate();
			LocalDate delToDate = sdf.parse(itemObj.get("deliveryToDate").toString()).toInstant()
					.atZone(ZoneId.systemDefault()).toLocalDate();
			long numOfDays = delFromDate.until(delToDate, ChronoUnit.DAYS) / 2;
			dateStr = delFromDate.plusDays(numOfDays).toString();
		}
		LocalDate eventDate = sdf.parse(dateStr).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		String offset = c.getOffset();
		int prev = 0;
		int mid = 0;
		int next = 0;
		LocalDate startDate = null;
		LocalDate endDate = null;
		try {
			prev = Integer.parseInt(offset.substring(0, offset.indexOf("-")));
			mid = Integer.parseInt(offset.substring(offset.indexOf("-") + 1, offset.lastIndexOf("-")));
			next = Integer.parseInt(offset.substring(offset.lastIndexOf("-") + 1, offset.length()));
		} catch (Exception e) {
			throw new PricingException(
					messageFetcher.fetchErrorMessage(tenantProvider, "017", new ArrayList<String>()));
		}
		if (c.getOffsetType().equals("Day")) {
			startDate = eventDate.minusDays(prev);
		if(mid == 0 && next == 0) {
			endDate= eventDate;
		}
		else if (mid == 0 && prev == 0) {
			endDate=eventDate.plusDays(next);
			startDate=eventDate.plusDays(1);
		}
		else {
			endDate=eventDate.plusDays(next);
		 }
		fields.setSd(startDate);
		fields.setEd(endDate);
		if ((null == c.getCollectionArray() || c.getCollectionArray().size() == 0
				|| gmr != null) && !c.getPricePoint().equalsIgnoreCase("forward")) {
			collectionArray = collectionDataFetcher.triggerRequest(c, tenantProvider, fields.getSd(),
					fields.getEd(), asOf);
			setupMarketDataCollection(fields, c, collectionArray, tenantProvider, asOf);
		  }
		}
		else if(c.getOffsetType().equals("Week")) {
			 startDate = eventDate.minusWeeks(prev).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
			 endDate = eventDate.plusWeeks(next).with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
			 fields.setSd(startDate);
			 fields.setEd(endDate);
			 if ((null == c.getCollectionArray() || c.getCollectionArray().size() == 0
						|| gmr != null) && !c.getPricePoint().equalsIgnoreCase("forward")) {
					collectionArray = collectionDataFetcher.triggerRequest(c, tenantProvider, fields.getSd(),
							fields.getEd(), asOf);
					setupMarketDataCollection(fields, c, collectionArray, tenantProvider, asOf);
				}
			 
		}else if(c.getOffsetType().equals("Month")) {
			startDate=eventDate.minusMonths(prev).with(TemporalAdjusters.firstDayOfMonth());
			endDate = eventDate.plusMonths(next).with(TemporalAdjusters.lastDayOfMonth());
			fields.setSd(startDate);
			fields.setEd(endDate);
			if ((null == c.getCollectionArray() || c.getCollectionArray().size() == 0
					|| gmr != null) && !c.getPricePoint().equalsIgnoreCase("forward")) {
				collectionArray = collectionDataFetcher.triggerRequest(c, tenantProvider, fields.getSd(),
						fields.getEd(), asOf);
				setupMarketDataCollection(fields, c, collectionArray, tenantProvider, asOf);
			}
		}
		
		if((null == c.getCollectionArray() || c.getCollectionArray().size() == 0
				|| gmr != null) && c.getPricePoint().equalsIgnoreCase("forward")) {
			collectionArray = processEventBasedGMRPricing(expiryArr, collectionArray,c, nearbyMonths, gmr, tenantProvider, fields, asOf);
			setupMarketDataCollection(fields, c, collectionArray, tenantProvider, asOf);
		}
		
		if (null == startDate || null == endDate) {
			throw new PricingException(
					messageFetcher.fetchErrorMessage(tenantProvider, "017", new ArrayList<String>()));
		}
		fields.setSd(startDate);
		fields.setEd(endDate);
		HolidayRuleDates holidayRuleDates = getListOfDaysPostHolidayRule(fields, c.getExchange(), tenantProvider,
				holidayRule, c);
		List<LocalDateTime> validDates = holidayRuleDates.getDateToBeUsed();
		List<LocalDateTime> realDates = holidayRuleDates.getDatesList();
		
		if (mid == 0 && c.getOffsetType().equals("Day")) {
			validDates.remove(eventDate.atStartOfDay());
			realDates.remove(eventDate.atStartOfDay());
		}else if (mid == 0 && c.getOffsetType().equals("Week")) {
			LocalDate  eventStart = eventDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
			LocalDate  eventEnd = eventDate.with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
			
			int i=realDates.indexOf(eventStart.atStartOfDay());
			int j =realDates.indexOf(eventEnd.atStartOfDay());
			for(int k=i; k<=j ;k++) {
				validDates.remove(i);
				realDates.remove(i);
			}
			
		}else if (mid == 0 && c.getOffsetType().equals("Month")) {
			LocalDate  eventStart = eventDate.with(TemporalAdjusters.firstDayOfMonth());
			LocalDate  eventEnd = eventDate.with(TemporalAdjusters.lastDayOfMonth());
			int i=0;
			int j=0;
			if(c.getMonthDefinition().equals("BOM")) {
			  i=realDates.indexOf(eventStart.atStartOfDay());
				while(!eventStart.isAfter(eventEnd)) {
					if(realDates.contains(eventStart.atStartOfDay())) {
						j =realDates.indexOf(eventStart.atStartOfDay());
					}
					eventStart=eventStart.plusDays(1);
			  }
			}
			else if(c.getMonthDefinition().equals("EOM")) {
				 j=realDates.indexOf(eventEnd.atStartOfDay());
				while(!eventStart.isAfter(eventEnd)) {
					if(realDates.contains(eventStart.atStartOfDay())) {
						i =realDates.indexOf(eventStart.atStartOfDay());
					}
					eventStart.plusDays(1);
			  }
			}
			 
			for(int k=i; k<=j ;k++) {
				validDates.remove(i);
				realDates.remove(i);
			}
			
		}
		
		gmrExposureField.setRealDates(realDates);
		gmrExposureField.setValidDates(validDates);
	}
	
	public JSONArray getExpForNotEventDate(List<GMRExposureFields> gmrExposureFieldList, LocalDate asOf, double itemQty,
			Curve c, String holidayRule, ContextProvider tenantProvider, JSONObject itemObj, double baseContractualConversionFactor,
			Map<String, Double> baseActualConversionFactor,Map<String, Double>  actualConversionFactor,
			Map<String, Double> gmrToItemConversionFactor,String locationType,JSONArray expArray,GMRExposureFields gmrExpField)
			throws PricingException, ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String dateStr = "";
		String dateStr1 = "";
		Map<LocalDate, CurveMarketData> priceDetailsMap =c.getPriceDetailsMap();
			 List<Event> eventList = gmrExpField.getGmr().getEvent();
				for (Event e : eventList) {
					dateStr1 = e.getDate();
				}
				
				double gmrQty=gmrExpField.getStockQtyInGmr();
		List<GMRQPDateDetails> gmrDetails=c.getGmrQPDetailsList();
		c.setGmrQPDetailsList(null);
			LocalDate delFromDate = sdf.parse(itemObj.get("deliveryFromDate").toString()).toInstant()
					.atZone(ZoneId.systemDefault()).toLocalDate();
			LocalDate delToDate = sdf.parse(itemObj.get("deliveryToDate").toString()).toInstant()
					.atZone(ZoneId.systemDefault()).toLocalDate();
			long numOfDays = delFromDate.until(delToDate, ChronoUnit.DAYS) / 2;
			dateStr = delFromDate.plusDays(numOfDays).toString();
			LocalDate eventDate = sdf.parse(dateStr).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			LocalDate eventDateGMR = sdf.parse(dateStr1).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			LocalDate lastDate = eventDate.with(TemporalAdjusters.lastDayOfMonth());
			CurveCalculatorFields fields = new CurveCalculatorFields();
			fields.setSd(eventDate);
			fields.setEd(eventDate);
			if(eventDateGMR.isAfter(lastDate)){
				fields.setSd(eventDateGMR);
				fields.setEd(eventDateGMR);
			}
		HolidayRuleDates holidayRuleDates = getListOfDaysPostHolidayRule(fields, c.getExchange(), tenantProvider,
				holidayRule, c);
		List<LocalDateTime> validDates = holidayRuleDates.getDateToBeUsed();
		c.setGmrQPDetailsList(gmrDetails);
		for (LocalDateTime date : validDates) {
			JSONObject expObj = new JSONObject();
			expObj.put("date", date);
				expObj.put("pricedQty", gmrQty*gmrToItemConversionFactor.get(gmrExpField.getGmr().getRefNo()));
				expObj.put("unPricedQty", 0);
				expObj.put("pricedPercentage", 100);
				expObj.put("unpricedPercentage", 0);
				expObj.put("pricedQuantityInBaseQtyUnit", gmrQty*baseActualConversionFactor.get(gmrExpField.getGmr().getRefNo())
						*gmrToItemConversionFactor.get(gmrExpField.getGmr().getRefNo()));
				expObj.put("unpricedQuantityInBaseQtyUnit", 0);
				if (null != priceDetailsMap.get(date.toLocalDate())) {
					expObj.put("instrumentDeliveryMonth",
							priceDetailsMap.get(date.toLocalDate()).getMonthYear());
				} else {
					String month = date.getMonth().toString().substring(0, 3);
					String year = Integer.toString(date.getYear());
					expObj.put("instrumentDeliveryMonth", month + year);
				}
				expObj.put("contractualConversionFactor", 0);
				expObj.put("actualConversionFactor", 1);
				expObj.put("gmrRefNo", gmrExpField.getGmr().getRefNo());
				expObj.put("titleTransferStatus", gmrExpField.getGmr().getTitleTransferStatus());
				String locationName=gmrExpField.getGmr().getStorageLocation();
				if(StringUtils.isEmpty(locationName)) {
					if(!StringUtils.isEmpty(locationType)) {
						if(locationType.equalsIgnoreCase("ORIGINATION")) {
							locationName=gmrExpField.getGmr().getLoadingLocName();
						}else {
							locationName=gmrExpField.getGmr().getDestinationLocName();
						}
					}
					
				}
				expObj.put("locationName", locationName);
				expArray.put(expObj);
			
		}
		return expArray;
	 
	}

	public String calculateAggregate(String exp, ContextProvider tenantProvider) throws PricingException {
		if(!exp.contains(",")) {
			throw new PricingException(
					messageFetcher.fetchErrorMessage(tenantProvider, "028", new ArrayList<String>()));
		}
		String expPostSimplify = "";
		String outerMethod = "";
		Map<String, String> aggregateSubMap = new HashMap<String, String>();
		if(exp.contains("MIN")) {
			expPostSimplify = separateAggregateMethod(exp, aggregateSubMap, "MIN");
			outerMethod = "MIN";
		}
		else if(exp.contains("MAX")) {
			expPostSimplify = separateAggregateMethod(exp, aggregateSubMap, "MAX");
			outerMethod = "MAX";
		}
		else if(exp.contains("AVG")) {
			expPostSimplify = separateAggregateMethod(exp, aggregateSubMap, "AVG");
			outerMethod = "AVG";
		}
		
		if(expPostSimplify.isEmpty()) {
			return exp;
		}
		Iterator<String> outerItr = null;
		outerItr = aggregateSubMap.keySet().iterator();
		String outerKey = outerItr.next();
		String outerAggregate = outerKey;
		
		while(expPostSimplify.contains("MIN")) {
			expPostSimplify = deduceInternalAggregateMethod(expPostSimplify, aggregateSubMap, "MIN", tenantProvider);
		}
		
		while(expPostSimplify.contains("MAX")) {
			expPostSimplify = deduceInternalAggregateMethod(expPostSimplify, aggregateSubMap, "MAX", tenantProvider);
		}
		
		while(expPostSimplify.contains("AVG")) {
			expPostSimplify = deduceInternalAggregateMethod(expPostSimplify, aggregateSubMap, "AVG", tenantProvider);
		}
		
		Double finalValue = calculateAggregateValue(expPostSimplify, outerMethod, tenantProvider);
		exp = exp.replace(outerAggregate, finalValue+"");
		return exp;
		
	}
	
	public String separateAggregateMethod(String str, Map<String, String> aggregateSubMap, String methodName) {
		int init = str.indexOf(methodName)+3;
		Stack<Character> brcketStack = new Stack<Character>();
		int i = init;
		int finall = 0;
		while(i<str.length()) {
			if(str.charAt(i)=='(') {
				brcketStack.push('(');
			}
			else if(str.charAt(i)==')') {
				brcketStack.pop();
			}
			if(brcketStack.size()==0) {
				finall = i;
				break;
			}
			i++;
		}
		String csString = str.substring(init+1, finall);
		if(aggregateSubMap!=null) {
			aggregateSubMap.clear();
			aggregateSubMap.put(str.substring(init-3, finall+1), csString);
		}
		return csString;
	}
	
	public double calculateAggregateValue(String aggStr, String method, ContextProvider tenantProvider)
			throws PricingException {
		String[] csArr = aggStr.split(",");
		if(csArr.length<0) {
			throw new PricingException(
					messageFetcher.fetchErrorMessage(tenantProvider, "028", new ArrayList<String>()));
		}
		double res = createParsable(csArr[0], tenantProvider);
		if(method.equalsIgnoreCase("min")) {
			for(int j=1; j<csArr.length;j++) {
				if(createParsable(csArr[j], tenantProvider)<res) {
					res = createParsable(csArr[j], tenantProvider);
				}
			}
		}
		else if(method.equalsIgnoreCase("max")) {
			for(int j=1; j<csArr.length;j++) {
				if(createParsable(csArr[j], tenantProvider)>res) {
					res = createParsable(csArr[j], tenantProvider);
				}
			}
		}
		else if(method.equalsIgnoreCase("avg")) {
			for(int j=1; j<csArr.length;j++) {
				res = res+createParsable(csArr[j], tenantProvider);
			}
			res = res/csArr.length;
		}
		return res;
	}
	
	public Double createParsable(String stringToBeParsed, ContextProvider tenantProvider) throws PricingException {
		Expression e = null;
		try {
			e = new ExpressionBuilder(stringToBeParsed.trim()).build();
		} catch (Exception exc) {
			throw new PricingException(
					messageFetcher.fetchErrorMessage(tenantProvider, "029", new ArrayList<String>()));
		}
		return e.evaluate();
	}
	
	public String deduceInternalAggregateMethod(String expPostSimplify, Map<String, String> aggregateSubMap,
			String method, ContextProvider tenantProvider) throws PricingException {
		separateAggregateMethod(expPostSimplify, aggregateSubMap, method);
		Iterator<String> itr = aggregateSubMap.keySet().iterator();
		String key = itr.next();
		expPostSimplify = expPostSimplify.replace(key,
				Double.toString(calculateAggregateValue(aggregateSubMap.get(key), method, tenantProvider)));
		return expPostSimplify;
	}
	
	public List<JSONObject> createTieredExposure(List<JSONObject> previewDataSet, JSONObject tieredObj) {
		for(JSONObject previewDataObj: previewDataSet) {
			previewDataObj.put("split", tieredObj.optString("tieredID"));
		}
		return previewDataSet;
	}
	
	public CurveCalculatorFields initializeV2(CurveCalculatorFields fieldObj, Curve c, JSONObject itemObj, GMR gmr,
			String lookbackDate, LocalDate asOf, ContextProvider tenantProvider) throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		DateTimeFormatter expiryDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
		if(c.getQuotedPeriod().equalsIgnoreCase("m")) {
			c.setQuotedPeriod("M+0");
		}

		JSONArray expiryArr = expiryCalenderFetcher.getData(c, tenantProvider);
		c.setExpiryArr(expiryArr);
		JSONArray collectionArray = new JSONArray();
		LocalDate asOnDate = asOf;
		if (null != gmr) {
			List<Event> eventList = gmr.getEvent();
			for (Event e : eventList) {
				if (e.getName().contentEquals("Bill of Lading") && c.isActualPricing()) {
					asOnDate = sdf.parse(e.getDate()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
					asOf = sdf.parse(e.getDate()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
				}
			}
		}
		String asOnMonthYear = collectionDataFetcher.createMonthYear(asOnDate);
		String pricingPeriodMonth = c.getPricingPeriod();
		String quotedPeriodMonth = c.getQuotedPeriodDate();
		PreviewData prevDataObj = new PreviewData();
		fieldObj.setPreviewData(prevDataObj);
		LocalDate dateForQuotedPeriod = LocalDate.now();
		LocalDate dateForPricingPeriod = LocalDate.now();
		try {
			dateForQuotedPeriod = calculateLocalDateFromMonthYearString(quotedPeriodMonth, tenantProvider);
			dateForPricingPeriod = calculateLocalDateFromMonthYearString(pricingPeriodMonth, tenantProvider);
		}
		catch (Exception e) {
			logger.info(Logger.EVENT_SUCCESS,
					ESAPI.encoder().encodeForHTML("exception while parsiing quoted period and pricing period"));
		}
		
		int nearbyMonths = dateForQuotedPeriod.getMonthValue() - dateForPricingPeriod.getMonthValue();
//		If quoted period is selected next year nearbyMonths value will be negative
		if (nearbyMonths < 0) {
			nearbyMonths = 12 + nearbyMonths;
		}
		
		if (null != gmr && c.isActualPricing()) {
			pricingPeriodMonth = asOnMonthYear;
			quotedPeriodMonth = collectionDataFetcher.createMonthYear(asOnDate.plusMonths(nearbyMonths));
			dateForQuotedPeriod = calculateLocalDateFromMonthYearString(quotedPeriodMonth, tenantProvider);
			dateForPricingPeriod = calculateLocalDateFromMonthYearString(pricingPeriodMonth, tenantProvider);
		}
		else if(null != gmr && c.isActualQuoted()) {
			quotedPeriodMonth = asOnMonthYear;
			pricingPeriodMonth = collectionDataFetcher.createMonthYear(asOnDate.minusMonths(nearbyMonths));
			dateForQuotedPeriod = calculateLocalDateFromMonthYearString(quotedPeriodMonth, tenantProvider);
			dateForPricingPeriod = calculateLocalDateFromMonthYearString(pricingPeriodMonth, tenantProvider);
		}
		boolean pricesAvl = false;
		switch (c.getPriceQuoteRule()) {
		case "Prompt Period Avg":
			String quotedMonthYear = dateForPricingPeriod.getMonth().toString().substring(0, 3).toUpperCase()
			+ Integer.toString(dateForPricingPeriod.getYear());
			dateForPricingPeriod = dateForPricingPeriod.minusMonths(1);
			String prevMonthYear = dateForPricingPeriod.getMonth().toString().substring(0, 3).toUpperCase()
					+ Integer.toString(dateForPricingPeriod.getYear());
			fieldObj = getQpDates(fieldObj, prevMonthYear, quotedMonthYear, expiryArr, tenantProvider);
			c.setMonthYear(fieldObj.getMonthYear());
			if(nearbyMonths==0) {
				if(null == c.getCollectionArray() || c.getCollectionArray().size() == 0 || gmr!=null) {
					collectionArray = collectionDataFetcher.fetchMarketPricesForV2(c, tenantProvider, fieldObj, asOf);
					setupMarketDataCollection(fieldObj, c, collectionArray, tenantProvider, asOf);
				}
				
			}
			else {
				c.setQuotedPeriod("M+"+nearbyMonths);
				if(null == c.getCollectionArray() || c.getCollectionArray().size() == 0 || gmr!=null) {
					collectionArray = collectionDataFetcher.triggerRequest(c, tenantProvider, fieldObj.getSd(),
							fieldObj.getEd(), asOf);
					setupMarketDataCollection(fieldObj, c, collectionArray, tenantProvider, asOf);
				}
				
			}
			break;
		case "Delivery Period Average":
			if(!c.getPricePoint().equalsIgnoreCase("forward")) {
				fieldObj.setSd(convertISOtoLocalDate(itemObj.optString("deliveryFromDate", "")));
				fieldObj.setEd(convertISOtoLocalDate(itemObj.optString("deliveryToDate", "")));
				if(null == c.getCollectionArray() || c.getCollectionArray().size() == 0 || gmr!=null) {
					collectionArray = collectionDataFetcher.triggerRequest(c, tenantProvider, fieldObj.getSd(),
							fieldObj.getEd(), asOf);
					setupMarketDataCollection(fieldObj, c, collectionArray, tenantProvider, asOf);
				}
				pricesAvl = true;
			}
			else {
				JSONArray arr = new JSONArray();
				for (int i = 0; i < expiryArr.length(); i++) {
					JSONObject expiryObj = expiryArr.getJSONObject(i);
					String lastTradeDateStr = expiryObj.optString("Last Trade Date");
					LocalDate lastTradeDate = LocalDateTime.parse(lastTradeDateStr, expiryDateFormatter).toLocalDate();
					boolean isFullyForward = false;
					if(dateForPricingPeriod.isAfter(asOf)) {
						isFullyForward = true;
					}
					if (lastTradeDate.isBefore(dateForPricingPeriod)) {
						continue;
					}
					if(collectionArray.length()==0) {
						if(!isFullyForward || (isFullyForward && null == fieldObj.getSd())) {
							fieldObj.setSd(dateForPricingPeriod);
						}
					}
					LocalDate lastDate = dateForPricingPeriod.with(TemporalAdjusters.lastDayOfMonth());
					fieldObj.setEd(lastDate);
					c.setMonthYear(expiryObj.optString("MONTH/YEAR"));
					if (nearbyMonths == 0) {
//						c.setQuotedPeriod("M+" + nearbyMonths);
						if(null == c.getCollectionArray() || c.getCollectionArray().size() == 0 || gmr!=null) {
							arr = collectionDataFetcher.fetchMarketPricesForV2(c, tenantProvider, fieldObj, asOf);
							collectionArray = collectionDataFetcher.mergeIntoFinal(arr, collectionArray);
						}
						
					} else {
						c.setQuotedPeriod("M+" + nearbyMonths);
						if(null == c.getCollectionArray() || c.getCollectionArray().size() == 0 || gmr!=null) {
							arr = collectionDataFetcher.triggerRequest(c, tenantProvider, fieldObj.getSd(),
									fieldObj.getEd(), asOf);
							collectionArray = collectionDataFetcher.mergeIntoFinal(arr, collectionArray);
						}
						
					}
					pricesAvl = true;
//					After the whole pricing period is covered in separate months, breaking out of loop
					if(lastTradeDate.isAfter(lastDate)) {
						break;
					}
					
				}
				if(collectionArray.length()==0 && !pricesAvl) {
					throw new PricingException(
							messageFetcher.fetchErrorMessage(tenantProvider, "029", new ArrayList<String>()));
				}
				if ((null != collectionArray && collectionArray.length()>0) || gmr != null) {
					setupMarketDataCollection(fieldObj, c, collectionArray, tenantProvider, asOf);
				}
			}
			
			break;
		case "Event Offset Based":
			if(c.getQuotedPeriod().contains("M+")) {
				nearbyMonths = Integer.parseInt(c.getQuotedPeriod().substring(c.getQuotedPeriod().length()-1));
			}
		    String eventDateStr = null;
			if (null != gmr) {
				List<Event> eventList = gmr.getEvent();
				for (Event e : eventList) {
					if (e.getName().contentEquals(c.getEvent())) {
						eventDateStr = e.getDate();
						asOnDate = sdf.parse(eventDateStr).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
						
					}
				}
			}
			LocalDate delFromDate =null;
			LocalDate delToDate = null;
			if (null == eventDateStr) {
				 delFromDate = sdf.parse(itemObj.get("deliveryFromDate").toString()).toInstant()
						.atZone(ZoneId.systemDefault()).toLocalDate();
				 delToDate = sdf.parse(itemObj.get("deliveryToDate").toString()).toInstant()
						.atZone(ZoneId.systemDefault()).toLocalDate();
				long numOfDays = delFromDate.until(delToDate, ChronoUnit.DAYS) / 2;
				eventDateStr = delFromDate.plusDays(numOfDays).toString();
			}
			LocalDate eventDate = sdf.parse(eventDateStr).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			String offset = c.getOffset();
			if (null == offset) {
				throw new PricingException(
						messageFetcher.fetchErrorMessage(tenantProvider, "017", new ArrayList<String>()));
			}
			int prev = 0;
			int mid = 0;
			int next = 0;
			prev = Integer.parseInt(offset.substring(0, offset.indexOf("-")));
			mid = Integer.parseInt(offset.substring(offset.indexOf("-") + 1, offset.lastIndexOf("-")));
			next = Integer.parseInt(offset.substring(offset.lastIndexOf("-") + 1, offset.length()));
			if(mid==0) {
				fieldObj.setSkipMidIfEvent(true);
			}
			switch (c.getOffsetType()) {
			case "Day":
				
				fieldObj.setSd(eventDate.minusDays(prev));
				if(mid == 0 && next == 0) {
					fieldObj.setEd(fieldObj.getSd());
				}
				else if (mid == 0 && prev == 0) {
					fieldObj.setEd(eventDate.plusDays(next));
					fieldObj.setSd(eventDate.plusDays(1));
				}
				else {
					fieldObj.setEd(eventDate.plusDays(next));
				}
				if ((null == c.getCollectionArray() || c.getCollectionArray().size() == 0
						|| gmr != null) && !c.getPricePoint().equalsIgnoreCase("forward")) {
					collectionArray = collectionDataFetcher.triggerRequest(c, tenantProvider, fieldObj.getSd(),
							fieldObj.getEd(), asOf);
					setupMarketDataCollection(fieldObj, c, collectionArray, tenantProvider, asOf);
				}
				if(mid==0) {
					fieldObj.setMidEventDay(eventDate);
				}
				break;
			case "Week":
				LocalDate sd = eventDate.minusWeeks(prev).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
				LocalDate ed = eventDate.plusWeeks(next).with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
				fieldObj.setSd(sd);
				fieldObj.setEd(ed);
				if(mid==0) {
					fieldObj.setMidEventDay(eventDate);
				}
				if ((null == c.getCollectionArray() || c.getCollectionArray().size() == 0
						|| gmr != null) && !c.getPricePoint().equalsIgnoreCase("forward")) {
					collectionArray = collectionDataFetcher.triggerRequest(c, tenantProvider, fieldObj.getSd(),
							fieldObj.getEd(), asOf);
					setupMarketDataCollection(fieldObj, c, collectionArray, tenantProvider, asOf);
				}
				break;
			case "Month":
				fieldObj.setSd(eventDate.minusMonths(prev).with(TemporalAdjusters.firstDayOfMonth()));
				fieldObj.setEd(eventDate.plusMonths(next).with(TemporalAdjusters.lastDayOfMonth()));
				if(mid==0) {
					fieldObj.setMidEventDay(eventDate);
				}
				if ((null == c.getCollectionArray() || c.getCollectionArray().size() == 0
						|| gmr != null) && !c.getPricePoint().equalsIgnoreCase("forward")) {
					collectionArray = collectionDataFetcher.triggerRequest(c, tenantProvider, fieldObj.getSd(),
							fieldObj.getEd(), asOf);
					setupMarketDataCollection(fieldObj, c, collectionArray, tenantProvider, asOf);
				}
				break;
			default:

			}if(null == c.getGmrQPDetailsList() && c.isExp() == true) {
				fieldObj.setSd(eventDate);
				fieldObj.setEd(eventDate);
			}
			if((null == c.getCollectionArray() || c.getCollectionArray().size() == 0
					|| gmr != null) && c.getPricePoint().equalsIgnoreCase("forward")) {
				collectionArray = processEventBasedPricing(expiryArr, c, nearbyMonths, gmr, tenantProvider, fieldObj, asOf);
				setupMarketDataCollection(fieldObj, c, collectionArray, tenantProvider, asOf);
			}
			break;
		case "Custom Period Average":
			if (c.getQuotedPeriod().toLowerCase().contains("m+")) {
				nearbyMonths = Integer.parseInt(c.getQuotedPeriod().substring(c.getQuotedPeriod().length() - 1));
			}
			if(!c.getPricePoint().equalsIgnoreCase("forward")) {
				fieldObj.setSd(convertISOtoLocalDate(c.getStartDate()));
				fieldObj.setEd(convertISOtoLocalDate(c.getEndDate()));
				if(null == c.getCollectionArray() || c.getCollectionArray().size() == 0 || gmr!=null) {
					collectionArray = collectionDataFetcher.triggerRequest(c, tenantProvider, fieldObj.getSd(),
							fieldObj.getEd(), asOf);
				}
				pricesAvl = true;
				
			}
			else {
				JSONArray arr = new JSONArray();
				dateForPricingPeriod = convertISOtoLocalDate(c.getStartDate());
				LocalDate finalDate = convertISOtoLocalDate(c.getEndDate());
				for (int i = 0; i < expiryArr.length(); i++) {
					if(null!= fieldObj.getEd() && fieldObj.getEd().equals(finalDate)) {
						break;
					}
					JSONObject expiryObj = expiryArr.getJSONObject(i);
					String lastTradeDateStr = expiryObj.optString("Last Trade Date");
					LocalDate lastTradeDate = LocalDateTime.parse(lastTradeDateStr, expiryDateFormatter).toLocalDate();
					if (lastTradeDate.isBefore(dateForPricingPeriod)) {
						continue;
					}
					if(collectionArray.length()==0) {
						fieldObj.setSd(dateForPricingPeriod);
					}
					
					if(lastTradeDate.isBefore(finalDate)) {
						fieldObj.setEd(lastTradeDate);
					}
					else {
						fieldObj.setEd(finalDate);
					}
					c.setMonthYear(expiryObj.optString("MONTH/YEAR"));
					if (nearbyMonths == 0) {
						if(null == c.getCollectionArray() || c.getCollectionArray().size() == 0 || gmr!=null) {
							arr = collectionDataFetcher.fetchMarketPricesForV2(c, tenantProvider, fieldObj, asOf);
							collectionArray = collectionDataFetcher.mergeIntoFinal(arr, collectionArray);
						}
						
					} else {
						c.setQuotedPeriod("M+" + nearbyMonths);
						if(null == c.getCollectionArray() || c.getCollectionArray().size() == 0 || gmr!=null) {
							arr = collectionDataFetcher.triggerRequest(c, tenantProvider, fieldObj.getSd(),
									fieldObj.getEd(), asOf);
							collectionArray = collectionDataFetcher.mergeIntoFinal(arr, collectionArray);
						}
					}
					pricesAvl = true;
//					After the whole pricing period is covered in separate months, breaking out of loop
					
					if(lastTradeDate.isAfter(finalDate)) {
						break;
					}
					else {
						dateForPricingPeriod = lastTradeDate;
					}
				}
			}
			if(collectionArray.length()==0 && !pricesAvl) {
				throw new PricingException(
						messageFetcher.fetchErrorMessage(tenantProvider, "029", new ArrayList<String>()));
			}
			if ((null != collectionArray && collectionArray.length()>0) || gmr != null) {
				setupMarketDataCollection(fieldObj, c, collectionArray, tenantProvider, asOf);
			}
			fieldObj.setSd(convertISOtoLocalDate(c.getStartDate()));
			fieldObj.setEd(convertISOtoLocalDate(c.getEndDate()));
			
			break;
		case "Lookback Pricing":
			offset = c.getOffset().trim();
			prev = Integer.parseInt(offset.substring(0, offset.indexOf("-")));
			mid = Integer.parseInt(offset.substring(offset.indexOf("-") + 1, offset.lastIndexOf("-")));
			next = Integer.parseInt(offset.substring(offset.lastIndexOf("-") + 1, offset.length()));
			if (next == 0) {
				throw new PricingException(
						messageFetcher.fetchErrorMessage(tenantProvider, "022", new ArrayList<String>()));
			}

			LocalDate eventDay = asOf;
			int eventYear = eventDay.getYear();
			int eventMonth = eventDay.getMonthValue();
			if (lookbackDate.length() == 0) {
				throw new PricingException(
						messageFetcher.fetchErrorMessage(tenantProvider, "023", new ArrayList<String>()));
			}
			LocalDate lookback = convertISOtoLocalDate(lookbackDate);
			int lookbackMonth = sdf.parse(lookbackDate).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
					.getMonthValue();
			int lookbackYear = sdf.parse(lookbackDate).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
					.getYear();
			String[] datesArr = new String[2];
			if (lookback.isAfter(asOf)) {
				datesArr = lookBackDurationCalculator(prev, mid, next, lookbackYear, lookbackMonth, true, lookbackDate,
						tenantProvider);
			} else {
				datesArr = lookBackDurationCalculator(prev, mid, next, eventYear, eventMonth, false, lookbackDate,
						tenantProvider);
			}

			fieldObj.setSd(sdf.parse(datesArr[0]).toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
			fieldObj.setEd(sdf.parse(datesArr[1]).toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
			if(null == c.getCollectionArray() || c.getCollectionArray().size() == 0 || gmr!=null) {
				collectionArray = collectionDataFetcher.triggerRequest(c, tenantProvider, fieldObj.getSd(),
						fieldObj.getEd(), asOf);
				setupMarketDataCollection(fieldObj, c, collectionArray, tenantProvider, asOf);
			}
			break;
		case "Settlement Date":
			//throw new PricingException("This pricing feature is not released yet");
			String pricingMonthYear = dateForPricingPeriod.getMonth().toString().substring(0, 3).toUpperCase()
			+ Integer.toString(dateForPricingPeriod.getYear());
			c.setMonthYear(fieldObj.getMonthYear());
			LocalDate pricingPeriod = calculateLocalDateFromMonthYearString(c.getPricingPeriod(), tenantProvider);
			LocalDate quotePeriod = calculateLocalDateFromMonthYearString(c.getQuotedPeriodDate(), tenantProvider);
			
			if(!pricingPeriod.equals(quotePeriod)) {
				throw new PricingException("Pricing Month & Quoted Month should be same");
			}
			
			switch (c.getOffsetType()) {
		case "NA":
			fieldObj = getPrDates(fieldObj, pricingMonthYear,expiryArr, tenantProvider);
			c.setMonthYear(fieldObj.getMonthYear());
			if(nearbyMonths==0) {
				if(null == c.getCollectionArray() || c.getCollectionArray().size() == 0 || gmr!=null) {
					collectionArray = collectionDataFetcher.fetchMarketPricesForV2(c, tenantProvider, fieldObj, asOf);
					setupMarketDataCollection(fieldObj, c, collectionArray, tenantProvider, asOf);
				}
				
			}
			break;
		case "Day":
			offset = c.getOffset().trim();
			prev = Integer.parseInt(offset.substring(0, offset.indexOf("-")));
			mid = Integer.parseInt(offset.substring(offset.indexOf("-") + 1, offset.lastIndexOf("-")));
			next = Integer.parseInt(offset.substring(offset.lastIndexOf("-") + 1, offset.length()));
			fieldObj = getPrDates(fieldObj, pricingMonthYear,expiryArr, tenantProvider);
			eventDate = fieldObj.getSd();
			logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("Settlement Date:" +eventDate));
			fieldObj.setSd(eventDate.minusDays(prev));
			if(mid == 0 && next == 0) {
				fieldObj.setEd(eventDate);
			}
			else if (mid == 0 && prev == 0) {
				fieldObj.setSd(eventDate);
				fieldObj.setEd(eventDate.plusDays(next));
			}else if(prev==0) {
				fieldObj.setSd(eventDate.minusDays(1));
				fieldObj.setEd(eventDate.plusDays(next));
			}
			else {
				fieldObj.setEd(eventDate.plusDays(next));
			}
			if(null == c.getCollectionArray() || c.getCollectionArray().size() == 0 || gmr!=null) {
				collectionArray = processSettlementDatePricing(expiryArr, c, nearbyMonths, gmr, tenantProvider, fieldObj, asOf);
				setupMarketDataCollection(fieldObj, c, collectionArray, tenantProvider, asOf);
			}
			if(mid==0) {
				fieldObj.setMidEventDay(eventDate);
				fieldObj.setSkipMidIfEvent(true);
			}
			
			break;
					
		case "Month":
			offset = c.getOffset().trim();
			
			prev = Integer.parseInt(offset.substring(0, offset.indexOf("-")));
			mid = Integer.parseInt(offset.substring(offset.indexOf("-") + 1, offset.lastIndexOf("-")));
			next = Integer.parseInt(offset.substring(offset.lastIndexOf("-") + 1, offset.length()));
			
			fieldObj = getPrDates(fieldObj, pricingMonthYear,expiryArr, tenantProvider);
			c.setMonthYear(fieldObj.getMonthYear());
			eventDate = fieldObj.getSd();
			
			logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("Settlement Date:" +eventDate));
			
			fieldObj.setSd(eventDate.minusMonths(prev).with(TemporalAdjusters.firstDayOfMonth()));
			//fieldObj.setEd(eventDate.plusMonths(next).with(TemporalAdjusters.lastDayOfMonth()));
			if(mid == 0 && next == 0) {
				 
				fieldObj.setEd(eventDate.with(TemporalAdjusters.lastDayOfMonth()));
			}
			else if (mid == 0 && prev == 0) {
				fieldObj.setSd(eventDate.with(TemporalAdjusters.firstDayOfMonth()));
				fieldObj.setEd(eventDate.plusMonths(next).with(TemporalAdjusters.lastDayOfMonth()));
			}
			else {
				fieldObj.setEd(eventDate.plusMonths(next).with(TemporalAdjusters.lastDayOfMonth()));
			}
			
			if(null == c.getCollectionArray() || c.getCollectionArray().size() == 0 || gmr!=null) {
				collectionArray = processSettlementDatePricing(expiryArr, c, nearbyMonths, gmr, tenantProvider, fieldObj, asOf);
				setupMarketDataCollection(fieldObj, c, collectionArray, tenantProvider, asOf);
			}
			if(mid==0) {
				fieldObj.setMidEventDay(eventDate);
				fieldObj.setSkipMidIfEvent(true);
			}
			
			break;
		}
			
		default:
			break;
		}
		return fieldObj;
	}
	
	public LocalDate calculateLocalDateFromMonthYearString(String monthYear, ContextProvider tenantProvider) throws PricingException {
		String month = "";
		int year = 0;
		try {
			month = monthYear.substring(0, 1).toUpperCase() + monthYear.substring(1, 3).toLowerCase();
			if(month.contains("-")) {
				month = month.replaceAll("-", "");
			}
			year = Integer.parseInt(monthYear.substring(3, monthYear.length()));
		} catch (Exception e) {
			logger.error(Logger.EVENT_FAILURE,
					ESAPI.encoder().encodeForHTML("Incorrect month Year String : " + monthYear));
			throw new PricingException(
					messageFetcher.fetchErrorMessage(tenantProvider, "031", new ArrayList<String>()));
		}
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		DateTimeFormatter formatterMMM = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
		DateTimeFormatter formatterfullMonth = DateTimeFormatter.ofPattern("dd-MMMM-yyyy");
		LocalDate date = LocalDate.now();
		try {
			date = LocalDate.parse("01-"+month+"-"+year, formatter);
		}
		catch (DateTimeParseException e) {
			date = LocalDate.parse("01-"+month+"-"+year, formatterMMM);
		}
		catch (Exception e) {
			date = LocalDate.parse("01-"+month+"-"+year, formatterfullMonth);
		}
		
		return date;
	}
	
	public CurveCalculatorFields getQpDates(CurveCalculatorFields fieldObj, String prevMonthYear,
			String currentMonthYear, JSONArray expiryArr, ContextProvider tenantProvider) throws PricingException {
		DateTimeFormatter expiryDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
		JSONObject prevMonthExpiryObj = new JSONObject();
		JSONObject currMonthExpiryObj = new JSONObject();
		for (int i = 0; i < expiryArr.length(); i++) {
			JSONObject expiryObj = expiryArr.getJSONObject(i);
			if (expiryObj.optString("MONTH/YEAR").equals(prevMonthYear)) {
				prevMonthExpiryObj = expiryObj;

			} else if (expiryObj.optString("MONTH/YEAR").equals(currentMonthYear)) {
				currMonthExpiryObj = expiryObj;
			}
		}
		try {
			LocalDateTime startDate = LocalDateTime
					.parse(prevMonthExpiryObj.optString("Last Trade Date"), expiryDateFormatter).plusDays(1);
			LocalDateTime endDate = LocalDateTime.parse(currMonthExpiryObj.optString("Last Trade Date"),
					expiryDateFormatter);
			fieldObj.setSd(startDate.toLocalDate());
			fieldObj.setEd(endDate.toLocalDate());
			fieldObj.setMonthYear(currMonthExpiryObj.optString("MONTH/YEAR"));
		}
		catch (Exception e) {
			throw new PricingException(
					messageFetcher.fetchErrorMessage(tenantProvider, "030", new ArrayList<String>()));
		}
		
		return fieldObj;
	}
	
	public CurveCalculatorFields getPrDates(CurveCalculatorFields fieldObj,
			String currentMonthYear, JSONArray expiryArr, ContextProvider tenantProvider) throws PricingException {
		DateTimeFormatter expiryDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
		JSONObject currMonthExpiryObj = new JSONObject();
		for (int i = 0; i < expiryArr.length(); i++) {
			JSONObject expiryObj = expiryArr.getJSONObject(i);
			if (expiryObj.optString("MONTH/YEAR").equals(currentMonthYear)) {
				currMonthExpiryObj = expiryObj;
			} 
		}
		try {
			LocalDateTime priceDdate = LocalDateTime.parse(currMonthExpiryObj.optString("Settlement Date"),
					expiryDateFormatter);
			fieldObj.setSd(priceDdate.toLocalDate());
			fieldObj.setEd(priceDdate.toLocalDate());
			fieldObj.setMonthYear(currMonthExpiryObj.optString("MONTH/YEAR"));
		}
		catch (Exception e) {
			throw new PricingException(
					messageFetcher.fetchErrorMessage(tenantProvider, "030", new ArrayList<String>()));
		}
		
		return fieldObj;
	}
	
	public void setupMarketDataCollection(CurveCalculatorFields fieldObj, Curve c, JSONArray CollectionArray,
			ContextProvider tenantProvider, LocalDate asOf) throws Exception {
		DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
		DateTimeFormatter expiryDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
		List<CurveMarketData> marketDataList = new ArrayList<CurveMarketData>();
		Map<LocalDate, CurveMarketData> priceDetailsMap = new HashMap<LocalDate, CurveMarketData>();
		for (int k = 0; k < CollectionArray.length(); k++) {
			JSONObject dailyObj = CollectionArray.getJSONObject(k);
			CurveMarketData curveMarketData = new CurveMarketData();
			curveMarketData.setExchange(dailyObj.optString("Exchange"));
			if (null == c.getExchange()) {
				c.setExchange(dailyObj.optString("Exchange"));
			}
			curveMarketData.setInstrumentName(dailyObj.optString("Instrument Name"));
			if (!dailyObj.has(c.getPriceType())) {
				curveMarketData.setPrice(dailyObj.optString("NA"));
			} else {
				curveMarketData.setPrice(dailyObj.optString(c.getPriceType()));
			}
			curveMarketData.setPriceUnit(dailyObj.optString("Price Unit"));
			String pricingDate = dailyObj.optString("Pricing Date");
			String promptDate = dailyObj.optString("Prompt Date");
			if (pricingDate.contains("T")) {
				pricingDate = pricingDate.replaceAll("T", " ");
			}
			if (promptDate.contains("T")) {
				promptDate = promptDate.replaceAll("T", " ");
			}
			try {
				curveMarketData.setPricingDate(LocalDate.parse(pricingDate, formatter1));
				if(asOf.isBefore(LocalDate.parse(pricingDate, formatter1)) && c.isActualPricing()) {
					continue;
				}
				if (c.getPricePoint().equalsIgnoreCase("forward")) {
					curveMarketData.setPromptDate(LocalDate.parse(promptDate, formatter1));
				}
			} catch (Exception e) {
				logger.info(Logger.EVENT_SUCCESS,
						ESAPI.encoder().encodeForHTML("pricing or prompt date not in correct format"));
				continue;
			}
			if (c.getPricePoint().equalsIgnoreCase("forward")
					&& curveMarketData.getPricingDate().isAfter(curveMarketData.getPromptDate())) {
				logger.info(Logger.EVENT_SUCCESS,
						ESAPI.encoder().encodeForHTML("skipping the prices for pricing days after prompt date"));
				continue;
			}
			boolean isValidDate = false;
			if (c.getPriceQuoteRule().equals("Delivery Period Average")
					|| c.getPriceQuoteRule().equals("Custom Period Average")
					|| c.getPriceQuoteRule().equals("Event Offset Based")) {
				JSONArray expiryArr = c.getExpiryArr();
				String expiryMonthYear = "";
				for (int i = 0; i < expiryArr.length(); i++) {
					JSONObject expiryObj = expiryArr.getJSONObject(i);
					LocalDateTime lastDate = LocalDateTime.parse(expiryObj.optString("Last Trade Date"),
							expiryDateFormatter);
					if (!lastDate.toLocalDate().isBefore(LocalDate.parse(pricingDate, formatter1))) {
						expiryMonthYear = expiryObj.optString("MONTH/YEAR");
						break;
					}
				}
				if (!expiryMonthYear.isEmpty() && c.getQuotedPeriod().toLowerCase().contains("m+")) {
					LocalDate expiryMonthYearToDate = calculateLocalDateFromMonthYearString(expiryMonthYear,
							tenantProvider);
					int nearbyMonths = Integer.parseInt(c.getQuotedPeriod().toLowerCase().replace("m+", ""));
					expiryMonthYearToDate = expiryMonthYearToDate.plusMonths(nearbyMonths);
					if (collectionDataFetcher.createMonthYear(expiryMonthYearToDate)
							.equals(dailyObj.optString("Month/Year"))) {
						isValidDate = true;
					}
				}
			}
			
			if(c.getPriceQuoteRule().equals("Settlement Date")) {
				JSONArray expiryArr = c.getExpiryArr();
				String expiryMonthYear = "";
				for (int i = 0; i < expiryArr.length(); i++) {
					JSONObject expiryObj = expiryArr.getJSONObject(i);
					LocalDateTime lastDate = LocalDateTime.parse(expiryObj.optString("Settlement Date"),
							expiryDateFormatter);
					if (!lastDate.toLocalDate().isBefore(LocalDate.parse(pricingDate, formatter1))) {
						expiryMonthYear = expiryObj.optString("MONTH/YEAR");
						break;
					}
				}
				if (!expiryMonthYear.isEmpty() && c.getQuotedPeriod().toLowerCase().contains("m+")) {
					LocalDate expiryMonthYearToDate = calculateLocalDateFromMonthYearString(expiryMonthYear,
							tenantProvider);
					int nearbyMonths = Integer.parseInt(c.getQuotedPeriod().toLowerCase().replace("m+", ""));
					expiryMonthYearToDate = expiryMonthYearToDate.plusMonths(nearbyMonths);
					if (collectionDataFetcher.createMonthYear(expiryMonthYearToDate)
							.equals(dailyObj.optString("Month/Year"))) {
						isValidDate = true;
					}
				}
				if (!priceDetailsMap.containsKey(LocalDate.parse(pricingDate, formatter1)) && isValidDate) {
					priceDetailsMap.put(LocalDate.parse(pricingDate, formatter1), curveMarketData);
				}
			}
			else {
				if (!priceDetailsMap.containsKey(LocalDate.parse(pricingDate, formatter1)) || isValidDate) {
					priceDetailsMap.put(LocalDate.parse(pricingDate, formatter1), curveMarketData);
				}
			}
			if (null == c.getFirstPricingDate()) {
				c.setFirstPricingDate(curveMarketData.getPricingDate());
			}
			curveMarketData.setMonthYear(dailyObj.optString("Month/Year"));
			marketDataList.add(curveMarketData);
		}
		c.setPriceDetailsMap(priceDetailsMap);
		c.setCollectionArray(marketDataList);
	}
	
	public JSONArray processEventBasedPricing(JSONArray expiryArr, Curve c, int nearbyMonths, GMR gmr,
			ContextProvider tenantProvider, CurveCalculatorFields fieldObj, LocalDate asOf)
			throws Exception {
		DateTimeFormatter expiryDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
		LocalDate startDate = fieldObj.getSd();
		LocalDate endDate = fieldObj.getEd();
		LocalDateTime lastTradeDate = null;
		JSONArray arr = new JSONArray();
		JSONArray collectionArray = new JSONArray();
		for (int i = 0; i < expiryArr.length(); i++) {
			JSONObject expiryObj = expiryArr.getJSONObject(i);
			if (null != lastTradeDate && !lastTradeDate.isBefore(endDate.atStartOfDay())) {
				break;
			}
			if (!LocalDateTime.parse(expiryObj.optString("Last Trade Date"), expiryDateFormatter)
					.isEqual(startDate.atStartOfDay()) && !LocalDateTime.parse(expiryObj.optString("Last Trade Date"), expiryDateFormatter)
					.isAfter(startDate.atStartOfDay())) {
				
				lastTradeDate = LocalDateTime.parse(expiryObj.optString("Last Trade Date"), expiryDateFormatter);
			
			continue;
				
			} else {
				c.setMonthYear(expiryObj.optString("MONTH/YEAR"));
				if(endDate.isAfter(
						LocalDateTime.parse(expiryObj.optString("Last Trade Date"), expiryDateFormatter).toLocalDate())) {
					fieldObj.setEd(
							LocalDateTime.parse(expiryObj.optString("Last Trade Date"), expiryDateFormatter).toLocalDate());
				}
				else {
					fieldObj.setEd(endDate);
				}
				if (nearbyMonths == 0) {
//					c.setQuotedPeriod("M+" + nearbyMonths);
					if (null == c.getCollectionArray() || c.getCollectionArray().size() == 0 || gmr != null) {
						arr = collectionDataFetcher.fetchMarketPricesForV2(c, tenantProvider, fieldObj, asOf);
						collectionArray = collectionDataFetcher.mergeIntoFinal(arr, collectionArray);
					}

				} else {
					c.setQuotedPeriod("M+" + nearbyMonths);
					if (null == c.getCollectionArray() || c.getCollectionArray().size() == 0 || gmr != null) {
						arr = collectionDataFetcher.triggerRequest(c, tenantProvider, fieldObj.getSd(),
								fieldObj.getEd(), asOf);
						collectionArray = collectionDataFetcher.mergeIntoFinal(arr, collectionArray);
					}

				}
				lastTradeDate = LocalDateTime.parse(expiryObj.optString("Last Trade Date"), expiryDateFormatter);
			}

		}
		return collectionArray;
	}
	
	public JSONArray processSettlementDatePricing(JSONArray expiryArr, Curve c, int nearbyMonths, GMR gmr,
			ContextProvider tenantProvider, CurveCalculatorFields fieldObj, LocalDate asOf)
			throws Exception {
		DateTimeFormatter expiryDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
		LocalDate startDate = fieldObj.getSd();
		LocalDate endDate = fieldObj.getEd();
		LocalDateTime settlementDate = null;
		JSONArray arr = new JSONArray();
		JSONArray collectionArray = new JSONArray();
		for (int i = 0; i < expiryArr.length(); i++) {
			JSONObject expiryObj = expiryArr.getJSONObject(i);
			if (null != settlementDate && !settlementDate.isBefore(endDate.atStartOfDay())) {
				break;
			}
			if (!LocalDateTime.parse(expiryObj.optString("Settlement Date"), expiryDateFormatter)
					.isEqual(startDate.atStartOfDay()) && !LocalDateTime.parse(expiryObj.optString("Settlement Date"), expiryDateFormatter)
					.isAfter(startDate.atStartOfDay())) {
				
				settlementDate = LocalDateTime.parse(expiryObj.optString("Settlement Date"), expiryDateFormatter);
			
			continue;
				
			} else {
				c.setMonthYear(expiryObj.optString("MONTH/YEAR"));
				if(endDate.isAfter(
						LocalDateTime.parse(expiryObj.optString("Settlement Date"), expiryDateFormatter).toLocalDate())) {
					fieldObj.setEd(
							LocalDateTime.parse(expiryObj.optString("Settlement Date"), expiryDateFormatter).toLocalDate());
				}
				else {
					fieldObj.setEd(endDate);
				}
				if (nearbyMonths == 0) {
//					c.setQuotedPeriod("M+" + nearbyMonths);
					if (null == c.getCollectionArray() || c.getCollectionArray().size() == 0 || gmr != null) {
						arr = collectionDataFetcher.fetchMarketPricesForV2(c, tenantProvider, fieldObj, asOf);
						collectionArray = collectionDataFetcher.mergeIntoFinal(arr, collectionArray);
					}

				} else {
					c.setQuotedPeriod("M+" + nearbyMonths);
					if (null == c.getCollectionArray() || c.getCollectionArray().size() == 0 || gmr != null) {
						arr = collectionDataFetcher.triggerRequest(c, tenantProvider, fieldObj.getSd(),
								fieldObj.getEd(), asOf);
						collectionArray = collectionDataFetcher.mergeIntoFinal(arr, collectionArray);
					}

				}
				settlementDate = LocalDateTime.parse(expiryObj.optString("Settlement Date"), expiryDateFormatter);
			}

		}
		return collectionArray;
	}
	
	public JSONArray processEventBasedGMRPricing(JSONArray expiryArr,JSONArray collectionArray,Curve c, int nearbyMonths, GMR gmr,
			ContextProvider tenantProvider, CurveCalculatorFields fieldObj, LocalDate asOf)
			throws Exception {
		DateTimeFormatter expiryDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
		LocalDate startDate = fieldObj.getSd();
		LocalDate endDate = fieldObj.getEd();
		LocalDateTime lastTradeDate = null;
		JSONArray arr = new JSONArray();
		for (int i = 0; i < expiryArr.length(); i++) {
			JSONObject expiryObj = expiryArr.getJSONObject(i);
			if (null != lastTradeDate && lastTradeDate.isAfter(endDate.atStartOfDay())) {
				break;
			}
			if (!LocalDateTime.parse(expiryObj.optString("Last Trade Date"), expiryDateFormatter)
					.isEqual(startDate.atStartOfDay()) && !LocalDateTime.parse(expiryObj.optString("Last Trade Date"), expiryDateFormatter)
					.isAfter(startDate.atStartOfDay())) {
				
				lastTradeDate = LocalDateTime.parse(expiryObj.optString("Last Trade Date"), expiryDateFormatter);
			
			continue;
				
			}else {
				c.setMonthYear(expiryObj.optString("MONTH/YEAR"));
				if(endDate.isAfter(
						LocalDateTime.parse(expiryObj.optString("Last Trade Date"), expiryDateFormatter).toLocalDate())) {
					fieldObj.setEd(
							LocalDateTime.parse(expiryObj.optString("Last Trade Date"), expiryDateFormatter).toLocalDate());
					
				}
				else {
					fieldObj.setEd(endDate);
				}
				if (nearbyMonths == 0) {
					if (null == c.getCollectionArray() || c.getCollectionArray().size() == 0 || gmr != null) {
						arr = collectionDataFetcher.fetchMarketPricesForV2(c, tenantProvider, fieldObj, asOf);
						collectionArray = collectionDataFetcher.mergeIntoFinal(arr, collectionArray);
					}

				} else {
					c.setQuotedPeriod("M+" + nearbyMonths);
					if (null == c.getCollectionArray() || c.getCollectionArray().size() == 0 || gmr != null) {
						arr = collectionDataFetcher.triggerRequest(c, tenantProvider, fieldObj.getSd(),
								fieldObj.getEd(), asOf);
						collectionArray = collectionDataFetcher.mergeIntoFinal(arr, collectionArray);
					}

				}
				lastTradeDate = LocalDateTime.parse(expiryObj.optString("Last Trade Date"), expiryDateFormatter);
				fieldObj.setSd(lastTradeDate.toLocalDate().plusDays(1));
			}

		}
		return collectionArray;
	}
		
	public void validatePricesData(ContextProvider tenantProvider, List<CurveMarketData> collectionData, Curve c,
			JSONArray forwardPrices) throws Exception {
		int count=0;
		if(null == collectionData) {
			collectionData = new ArrayList<CurveMarketData>();
		}
		for (int k = 0; k < collectionData.size(); k++) {
			CurveMarketData curveMarketData = collectionData.get(k);
			if(null==curveMarketData.getPrice() || curveMarketData.getPrice().isEmpty()) {
				count++;
			}else {
				break;
			}
		}
		int validForwardPrices = 0;
		for (int i = 0; i < forwardPrices.length(); i++) {
			JSONObject forwardObj = forwardPrices.optJSONObject(i);
			double forwardPrice = forwardObj.optDouble("Forward Prices");
			if(!curveService.checkZero(forwardPrice)) {
				validForwardPrices++;
			}
		}
		if(count==collectionData.size() && validForwardPrices==0) {
			List<String> params = new ArrayList<String>();
			params.add(c.getCurveName());
			throw new PricingException(messageFetcher.fetchErrorMessage(tenantProvider, "018", params));
		}
	}
	

	public JSONObject getLastKnownPricesForFutureCurves(Curve c, LocalDate date, LocalDate asOf,
			ContextProvider tenantProvider) throws Exception {
		DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
		JSONArray expiryArr = c.getExpiryArr();
		LocalDate endDate = date;
		LocalDate startDate = endDate.with(TemporalAdjusters.firstDayOfMonth());
		DateTimeFormatter expiryDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
		for (int i = 0; i < expiryArr.length(); i++) {
			JSONObject expiryObj = expiryArr.getJSONObject(i);
			LocalDate lastTradeDateForExpiryObj = LocalDateTime
					.parse(expiryObj.optString("Last Trade Date"), expiryDateFormatter).toLocalDate();
			if (lastTradeDateForExpiryObj.isAfter(endDate) || lastTradeDateForExpiryObj.equals(endDate)) {
				c.setMonthYear(expiryObj.optString("MONTH/YEAR"));
				break;
			}
		}
		JSONArray collectionData = collectionDataFetcher.triggerRequest(c, tenantProvider, startDate, endDate, asOf);
		for(int i = collectionData.length()-1 ; i>0; i--) {
			JSONObject jobj = collectionData.getJSONObject(i);
			String pricingDate = jobj.optString("Pricing Date");
			if (pricingDate.contains("T")) {
				pricingDate = pricingDate.replaceAll("T", " ");
			}
			if(LocalDate.parse(pricingDate, formatter1).isAfter(date)) {
				continue;
			}
			else {
				return jobj;
			}
		}
		return null;
	}
	
	public JSONObject getLastKnownPricesForFutureCurvesSettlementDate(Curve c, LocalDate date, LocalDate asOf,
			ContextProvider tenantProvider) throws Exception {
		DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
		JSONArray expiryArr = c.getExpiryArr();
		LocalDate endDate = date;
		LocalDate startDate = endDate.with(TemporalAdjusters.firstDayOfMonth());
		DateTimeFormatter expiryDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
		for (int i = 0; i < expiryArr.length(); i++) {
			JSONObject expiryObj = expiryArr.getJSONObject(i);
			LocalDate settlementDateForExpiryObj = LocalDateTime
					.parse(expiryObj.optString("Settlement Date"), expiryDateFormatter).toLocalDate();
			if (settlementDateForExpiryObj.isAfter(endDate) || settlementDateForExpiryObj.equals(endDate)) {
				c.setMonthYear(expiryObj.optString("MONTH/YEAR"));
				break;
			}
		}
		JSONArray collectionData = collectionDataFetcher.triggerRequest(c, tenantProvider, startDate, endDate, asOf);
		for(int i = collectionData.length()-1 ; i>0; i--) {
			JSONObject jobj = collectionData.getJSONObject(i);
			String pricingDate = jobj.optString("Pricing Date");
			if (pricingDate.contains("T")) {
				pricingDate = pricingDate.replaceAll("T", " ");
			}
			if(LocalDate.parse(pricingDate, formatter1).isAfter(date)) {
				continue;
			}
			else {
				return jobj;
			}
		}
		return null;
	}
	
	public Map<String, Double> gmrDensityVolumeConversion(List<GMR> gmrList,JSONObject jobj,String productId,ContextProvider tenantProvider,
			String itemQtyUnit,String CurveQtyUnitID,double contractQualityDensity,String contractQualityMassUnit,String contractQualityVolumeUnit,
			double totalGmrQty,String contractItemQty,String curveQtyUnit,int index, boolean type,
			Map<String, Double> weightedAvgConversionFactorGMRQty, String itemQtyUnitCheck) throws Exception  {
		Map<String, Double> actualConversionFactorMap = new HashMap<String,Double>();
		double weightedAvgConversionFactor=0.0;
		
		String curveQtyUnitCheck=massToVolumeConversion.getMassVolumeQtyCheck(productId,CurveQtyUnitID);
		if(itemQtyUnitCheck.isEmpty() || curveQtyUnitCheck.isEmpty() || itemQtyUnitCheck.equals(curveQtyUnitCheck)) {
			weightedAvgConversionFactor = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productId, itemQtyUnit,
					CurveQtyUnitID);
				for (GMR gmr : gmrList) {
					actualConversionFactorMap.put(gmr.getRefNo(), weightedAvgConversionFactor);
			   }
		}else {
			for (GMR gmr : gmrList) {
				 totalGmrQty=0.0;
				   double actualConversionFactor=0.0;
					double totalActualConversionFactor=0.0;
					double conversionFactor=0.0;
		  for (Stock st : gmr.getStocks()) {
			  if (st.getGMRRefNo().equals(gmr.getRefNo())) {
				double gmrQty=st.getStockQtyInGmr();
				  totalGmrQty=totalGmrQty+gmrQty;
			if(!st.getDensityVolumeQtyUnitId().isEmpty()) {
				String densityVolUnit= st.getDensityVolumeQtyUnitId();
				String gmrQtyUnit= st.getQtyUnit();
				String qtyunitCheck=massToVolumeConversion.getMassVolumeQtyCheck(productId,gmrQtyUnit);
				String densityVolUnitCheck=massToVolumeConversion.getMassVolumeQtyCheck(productId,densityVolUnit);
				double massToVolConversionFactor=st.getMassToVolConversionFactor();
				if(!qtyunitCheck.isEmpty() && qtyunitCheck.equals("Weight") && massToVolConversionFactor!=0.0) {
					massToVolConversionFactor=1/massToVolConversionFactor;
				}
				if(!qtyunitCheck.isEmpty() && !densityVolUnitCheck.isEmpty() && !qtyunitCheck.equals(densityVolUnitCheck)) {
					if(qtyunitCheck.equals("Weight") && densityVolUnitCheck.equals("Volume")) {
						
						actualConversionFactor=contractualConversionFactor(productId,itemQtyUnit,CurveQtyUnitID, massToVolConversionFactor, gmrQtyUnit,
								densityVolUnit, tenantProvider);
						if(actualConversionFactor==0.0) {
							// To call Contractual Conversion Factor
								
								if(contractQualityDensity!=0.0) {
									actualConversionFactor=contractualConversionFactor(productId,itemQtyUnit,CurveQtyUnitID, contractQualityDensity, contractQualityMassUnit,
											contractQualityVolumeUnit, tenantProvider);
									if(actualConversionFactor==0.0) {
										actualConversionFactor = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productId, itemQtyUnit,
												CurveQtyUnitID);
									}
									
								}else {
									actualConversionFactor = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productId, itemQtyUnit,
											CurveQtyUnitID);
								}
								
								conversionFactor=gmrQty*actualConversionFactor;
							
							
						}else {
							conversionFactor=gmrQty*actualConversionFactor;
						}
					}else if(qtyunitCheck.equals("Volume") && densityVolUnitCheck.equals("Weight")){
						actualConversionFactor=contractualConversionFactor(productId,itemQtyUnit,CurveQtyUnitID, massToVolConversionFactor, densityVolUnit,
								gmrQtyUnit, tenantProvider);
						if(actualConversionFactor==0.0) {
							// To call Contractual Conversion Factor	
								if(contractQualityDensity!=0.0) {
									actualConversionFactor=contractualConversionFactor(productId,itemQtyUnit,CurveQtyUnitID, contractQualityDensity, contractQualityMassUnit,
											contractQualityVolumeUnit, tenantProvider);
									if(actualConversionFactor==0.0) {
										actualConversionFactor = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productId, itemQtyUnit,
												CurveQtyUnitID);
									}
									
								}else {
									actualConversionFactor = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productId, itemQtyUnit,
											CurveQtyUnitID);
								}
								
								conversionFactor=gmrQty*actualConversionFactor;
							
							
						}else {
							conversionFactor=gmrQty*actualConversionFactor;
						}
					   
					}
				}else {
					if(contractQualityDensity!=0.0) {
						actualConversionFactor=contractualConversionFactor(productId,itemQtyUnit,CurveQtyUnitID, contractQualityDensity, contractQualityMassUnit,
								contractQualityVolumeUnit, tenantProvider);
						}else {
						actualConversionFactor = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productId, itemQtyUnit,
							CurveQtyUnitID);
					}
					
					conversionFactor=gmrQty*actualConversionFactor;
				}
				
			}else{
				// here need to check if density value not recieved from GMR payload.
				if(contractQualityDensity!=0.0) {
					actualConversionFactor=contractualConversionFactor(productId,itemQtyUnit,CurveQtyUnitID, contractQualityDensity, contractQualityMassUnit,
							contractQualityVolumeUnit, tenantProvider);
				}else {
					actualConversionFactor = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productId, itemQtyUnit,
						CurveQtyUnitID);
				}
				
				conversionFactor=gmrQty*actualConversionFactor;
			}
			totalActualConversionFactor=totalActualConversionFactor+conversionFactor;
			}
		  }
		  weightedAvgConversionFactor=totalActualConversionFactor/totalGmrQty;
		  if(Double.isNaN(weightedAvgConversionFactor) || curveService.checkZero(weightedAvgConversionFactor)) {
				weightedAvgConversionFactor= 1;
			}
		  actualConversionFactorMap.put(gmr.getRefNo(), weightedAvgConversionFactor);
		}
		}
	
		
		return actualConversionFactorMap;
		
	}
	
	public Map<String, Double> gmrDensityVolumeConversionGMRQty(List<GMR> gmrList,JSONObject jobj,String productId,ContextProvider tenantProvider,
			String itemQtyUnit,double contractQualityDensity,String contractQualityMassUnit,
			String contractQualityVolumeUnit,double totalGmrQty,String itemQtyUnitCheck) throws Exception  {
		Map<String, Double> actualConversionFactorGMRMap = new HashMap<String,Double>();
		double weightedAvgConversionFactor=0.0;
		for (GMR gmr : gmrList) {
			 totalGmrQty=0.0;
			    double actualConversionFactor=0.0;
				double totalActualConversionFactor=0.0;
				double conversionFactor=0.0;
			for (Stock st : gmr.getStocks()) {
		  if (st.getGMRRefNo().equals(gmr.getRefNo())) {
			  double gmrQty=st.getStockQtyInGmr();
			totalGmrQty=totalGmrQty+gmrQty;
			String gmrQtyUnit= st.getQtyUnit();
			String gmrQtyUnitCheck=massToVolumeConversion.getMassVolumeQtyCheck(productId,gmrQtyUnit);
			if(itemQtyUnitCheck.isEmpty() || gmrQtyUnitCheck.isEmpty() || itemQtyUnitCheck.equals(gmrQtyUnitCheck)) {
				weightedAvgConversionFactor = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productId, gmrQtyUnit,
						itemQtyUnit);
				actualConversionFactorGMRMap.put(st.getGMRRefNo(), weightedAvgConversionFactor);
			}else {
			
			if(!st.getDensityVolumeQtyUnitId().isEmpty()) {
				String densityVolUnit= st.getDensityVolumeQtyUnitId();
				String densityVolUnitCheck=massToVolumeConversion.getMassVolumeQtyCheck(productId,densityVolUnit);
				double massToVolConversionFactor=st.getMassToVolConversionFactor();
				if(!gmrQtyUnitCheck.isEmpty() && gmrQtyUnitCheck.equals("Weight") && massToVolConversionFactor!=0.0) {
					massToVolConversionFactor=1/massToVolConversionFactor;
				}
				if(!gmrQtyUnitCheck.isEmpty() && !densityVolUnitCheck.isEmpty() && !gmrQtyUnitCheck.equals(densityVolUnitCheck)) {
					if(gmrQtyUnitCheck.equals("Weight") && densityVolUnitCheck.equals("Volume")) {
						actualConversionFactor=contractualConversionFactor(productId,gmrQtyUnit,itemQtyUnit, massToVolConversionFactor, gmrQtyUnit,
								densityVolUnit, tenantProvider);
						if(actualConversionFactor==0.0) {
							// To call Contractual Conversion Factor
								
						 if(contractQualityDensity!=0.0) {
							actualConversionFactor=contractualConversionFactor(productId,gmrQtyUnit,itemQtyUnit, contractQualityDensity, contractQualityMassUnit,
											contractQualityVolumeUnit, tenantProvider);
							if(actualConversionFactor==0.0) {
										actualConversionFactor = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productId, gmrQtyUnit,
												itemQtyUnit);
								}
									
						}else {
								actualConversionFactor = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productId, gmrQtyUnit,
										itemQtyUnit);
							}
								
							conversionFactor=gmrQty*actualConversionFactor;
							
							
						}else {
							conversionFactor=gmrQty*actualConversionFactor;
						}
					}else if(gmrQtyUnitCheck.equals("Volume") && densityVolUnitCheck.equals("Weight")){
						actualConversionFactor=contractualConversionFactor(productId,gmrQtyUnit,itemQtyUnit, massToVolConversionFactor, densityVolUnit,
								gmrQtyUnit, tenantProvider);
						if(actualConversionFactor==0.0) {
							// To call Contractual Conversion Factor	
								if(contractQualityDensity!=0.0) {
									actualConversionFactor=contractualConversionFactor(productId,gmrQtyUnit,itemQtyUnit, contractQualityDensity, contractQualityMassUnit,
											contractQualityVolumeUnit, tenantProvider);
									if(actualConversionFactor==0.0) {
										actualConversionFactor = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productId, gmrQtyUnit,
												itemQtyUnit);
									}
									
								}else {
									actualConversionFactor = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productId, gmrQtyUnit,
											itemQtyUnit);
								}
								
								conversionFactor=gmrQty*actualConversionFactor;
							
							
						}else {
							conversionFactor=gmrQty*actualConversionFactor;
						}
					   
					}
				}else {
					if(contractQualityDensity!=0.0) {
						actualConversionFactor=contractualConversionFactor(productId,gmrQtyUnit,itemQtyUnit, contractQualityDensity, contractQualityMassUnit,
								contractQualityVolumeUnit, tenantProvider);
						if(actualConversionFactor==0.0) {
							actualConversionFactor = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productId, gmrQtyUnit,
									itemQtyUnit);
						}
					}else {
						actualConversionFactor = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productId, gmrQtyUnit,
								itemQtyUnit);
					}
					
					conversionFactor=gmrQty*actualConversionFactor;
				}
				
			}else{
				// here need to check if density value not recieved from GMR payload.
				if(contractQualityDensity!=0.0) {
					actualConversionFactor=contractualConversionFactor(productId,gmrQtyUnit,itemQtyUnit, contractQualityDensity, contractQualityMassUnit,
							contractQualityVolumeUnit, tenantProvider);
					if(actualConversionFactor==0.0) {
						actualConversionFactor = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productId, gmrQtyUnit,
								itemQtyUnit);
					}
				}else {
					actualConversionFactor = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productId, gmrQtyUnit,
							itemQtyUnit);
				}
				
				conversionFactor=gmrQty*actualConversionFactor;
			}
			totalActualConversionFactor=totalActualConversionFactor+conversionFactor;
			}
		  
		  }
		  
			}
			weightedAvgConversionFactor=totalActualConversionFactor/totalGmrQty;
			if(Double.isNaN(weightedAvgConversionFactor) || curveService.checkZero(weightedAvgConversionFactor)) {
				weightedAvgConversionFactor= 1;
			}
		  if(!actualConversionFactorGMRMap.containsKey(gmr.getRefNo())) {
			  actualConversionFactorGMRMap.put(gmr.getRefNo(), weightedAvgConversionFactor);
		  }
		}
		return actualConversionFactorGMRMap;
	}
	
	
	public double gmrActualDensityVolumeConversion(GMR gmr,JSONObject jobj,String productId,ContextProvider tenantProvider,
			String itemQtyUnit,String CurveQtyUnitID,double contractQualityDensity,String contractQualityMassUnit,String contractQualityVolumeUnit,
			double totalGmrQty,String contractItemQty,String curveQtyUnit,int index, boolean type,
			Map<String, Double> weightedAvgConversionFactorGMRQty, String gmrRefNo,String itemQtyUnitCheck,String curveQtyUnitCheck) throws PricingException  {
		double weightedAvgConversionFactor=0.0;
		
		if(itemQtyUnitCheck.isEmpty() || curveQtyUnitCheck.isEmpty() || itemQtyUnitCheck.equals(curveQtyUnitCheck)) {
			try {
				weightedAvgConversionFactor = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productId, itemQtyUnit,
						CurveQtyUnitID);
			} catch (Exception e) {
				logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("exception at Quantity Unit Conversion rate" + e.getMessage()));
				throw new PricingException(
						messageFetcher.fetchErrorMessage(tenantProvider, "052", new ArrayList<String>()));
			}
				
		}else {
				 totalGmrQty=0.0;
				   double actualConversionFactor=0.0;
				   double totalActualConversionFactor=0.0;
				   double conversionFactor=0.0;
		  for (Stock st : gmr.getStocks()) {
			  if (st.getGMRRefNo().equals(gmrRefNo)) {
				double gmrQty=st.getStockQtyInGmr();
				  totalGmrQty=totalGmrQty+gmrQty;
				 
			if(!st.getDensityVolumeQtyUnitId().isEmpty()) {
				String densityVolUnit= st.getDensityVolumeQtyUnitId();
				String gmrQtyUnit= st.getQtyUnit();
				String qtyunitCheck="";
				String densityVolUnitCheck="";
				try {
					qtyunitCheck = massToVolumeConversion.getMassVolumeQtyCheck(productId,gmrQtyUnit);
					densityVolUnitCheck = massToVolumeConversion.getMassVolumeQtyCheck(productId,densityVolUnit);
				} catch (Exception e) {
					logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("exception at Mass To Volume Conversion class qtyCheck" + e.getMessage()));
					throw new PricingException(
							messageFetcher.fetchErrorMessage(tenantProvider, "053", new ArrayList<String>()));
				}
				double massToVolConversionFactor=st.getMassToVolConversionFactor();
				if(!qtyunitCheck.isEmpty() && qtyunitCheck.equals("Weight") && massToVolConversionFactor!=0.0) {
					massToVolConversionFactor=1/massToVolConversionFactor;
				}
				if(!qtyunitCheck.isEmpty() && !densityVolUnitCheck.isEmpty() && !qtyunitCheck.equals(densityVolUnitCheck)) {
					if(qtyunitCheck.equals("Weight") && densityVolUnitCheck.equals("Volume")) {
						
						actualConversionFactor=contractualConversionFactor(productId,itemQtyUnit,CurveQtyUnitID, massToVolConversionFactor, gmrQtyUnit,
								densityVolUnit, tenantProvider);
						if(actualConversionFactor==0.0) {
							// To call Contractual Conversion Factor
								
								if(contractQualityDensity!=0.0) {
									actualConversionFactor=contractualConversionFactor(productId,itemQtyUnit,CurveQtyUnitID, contractQualityDensity, contractQualityMassUnit,
											contractQualityVolumeUnit, tenantProvider);
									if(actualConversionFactor==0.0) {
											try {
												actualConversionFactor = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productId, itemQtyUnit,
														CurveQtyUnitID);
											} catch (Exception e) {
												logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("exception at Quantity Unit Conversion rate" + e.getMessage()));
												throw new PricingException(
														messageFetcher.fetchErrorMessage(tenantProvider, "052", new ArrayList<String>()));
											}
									}
									
								}else {
									try {
										actualConversionFactor = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productId, itemQtyUnit,
												CurveQtyUnitID);
									} catch (Exception e) {
										logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("exception at Quantity Unit Conversion rate" + e.getMessage()));
										throw new PricingException(
												messageFetcher.fetchErrorMessage(tenantProvider, "052", new ArrayList<String>()));
									}
								}
								
								conversionFactor=gmrQty*actualConversionFactor;
							
							
						}else {
							conversionFactor=gmrQty*actualConversionFactor;
						}
					}else if(qtyunitCheck.equals("Volume") && densityVolUnitCheck.equals("Weight")){
						actualConversionFactor=contractualConversionFactor(productId,itemQtyUnit,CurveQtyUnitID, massToVolConversionFactor, densityVolUnit,
								gmrQtyUnit, tenantProvider);
						if(actualConversionFactor==0.0) {
							// To call Contractual Conversion Factor	
								if(contractQualityDensity!=0.0) {
									actualConversionFactor=contractualConversionFactor(productId,itemQtyUnit,CurveQtyUnitID, contractQualityDensity, contractQualityMassUnit,
											contractQualityVolumeUnit, tenantProvider);
									if(actualConversionFactor==0.0) {
										try {
											actualConversionFactor = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productId, itemQtyUnit,
													CurveQtyUnitID);
										} catch (Exception e) {
											logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("exception at Quantity Unit Conversion rate" + e.getMessage()));
											throw new PricingException(
													messageFetcher.fetchErrorMessage(tenantProvider, "052", new ArrayList<String>()));
										}
									}
									
								}else {
									try {
										actualConversionFactor = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productId, itemQtyUnit,
												CurveQtyUnitID);
									} catch (Exception e) {
										logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("exception at Quantity Unit Conversion rate" + e.getMessage()));
										throw new PricingException(
												messageFetcher.fetchErrorMessage(tenantProvider, "052", new ArrayList<String>()));
									}
								}
								
								conversionFactor=gmrQty*actualConversionFactor;
							
							
						}else {
							conversionFactor=gmrQty*actualConversionFactor;
						}
					   
					}
				}else {
					if(contractQualityDensity!=0.0) {
						actualConversionFactor=contractualConversionFactor(productId,itemQtyUnit,CurveQtyUnitID, contractQualityDensity, contractQualityMassUnit,
								contractQualityVolumeUnit, tenantProvider);
						}else {
						try {
							actualConversionFactor = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productId, itemQtyUnit,
								CurveQtyUnitID);
						} catch (Exception e) {
							logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("exception at Quantity Unit Conversion rate" + e.getMessage()));
							throw new PricingException(
									messageFetcher.fetchErrorMessage(tenantProvider, "052", new ArrayList<String>()));
						}
					}
					
					conversionFactor=gmrQty*actualConversionFactor;
				}
				
			}else{
				// here need to check if density value not recieved from GMR payload.
				if(contractQualityDensity!=0.0) {
					actualConversionFactor=contractualConversionFactor(productId,itemQtyUnit,CurveQtyUnitID, contractQualityDensity, contractQualityMassUnit,
							contractQualityVolumeUnit, tenantProvider);
				}else {
					try {
						actualConversionFactor = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productId, itemQtyUnit,
							CurveQtyUnitID);
					} catch (Exception e) {
						logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("exception at Quantity Unit Conversion rate" + e.getMessage()));
						throw new PricingException(
								messageFetcher.fetchErrorMessage(tenantProvider, "052", new ArrayList<String>()));
					}
				}
				
				conversionFactor=gmrQty*actualConversionFactor;
			}
			totalActualConversionFactor=totalActualConversionFactor+conversionFactor;
			}
		  }
		  weightedAvgConversionFactor=totalActualConversionFactor/totalGmrQty;
		}
		
		if(Double.isNaN(weightedAvgConversionFactor) || curveService.checkZero(weightedAvgConversionFactor)) {
			return 1;
		}else {
			return weightedAvgConversionFactor;
		}
	}
	
	public JSONObject checkForwardPrices(JSONArray forwardPrices, LocalDate asOf, LocalDate pricingDate, Curve c)
			throws PricingException {
//		String monthYear = getMonthYearForDate(pricingDate, c);
		JSONObject resObj = new JSONObject();
		LocalDate firstDeliveryDate = c.getQpFromDate();
		
		if (pricingDate.isBefore(asOf) || pricingDate.isEqual(asOf)) {
			if(!firstDeliveryDate.isEqual(asOf) && !pricingDate.isEqual(asOf)) {
				resObj.put("price", 0);
				return resObj;
			}
		}
		resObj = getBestAvailableForwardPrice(forwardPrices, pricingDate, c);
		/*DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
		double bestPrice = 0;
		for (int i = 0; i < forwardPrices.length(); i++) {
			JSONObject forwardPriceObject = forwardPrices.optJSONObject(i);
			String pricingDateStr = forwardPriceObject.optString("Pricing Date");
			if (pricingDateStr.contains("T")) {
				pricingDateStr = pricingDateStr.replaceAll("T", " ");
			}
			LocalDate pricingDateInObject = LocalDate.parse(pricingDateStr, formatter1);
			if (pricingDateInObject.isAfter(pricingDate)) {
				break;
			}
//			for spot curves month/year will be empty
			if (!c.getPricePoint().toLowerCase().equals("forward")
					|| forwardPriceObject.optString("Month/Year").equalsIgnoreCase(monthYear)) {
				bestPrice = forwardPriceObject.optDouble("Forward Prices");
				if (pricingDate.equals(pricingDateInObject)) {
					resObj.put("tag", "Forward");
					resObj.put("price", bestPrice);
					return resObj;
				}
			}
		}
		resObj.put("tag", "Forward (Estimated)");
		resObj.put("price", bestPrice);*/
		return resObj;
	}
	
	public boolean checkEstimated(String monthYear, LocalDate pricingDate, Curve c) throws PricingException {
		if(getMonthYearForDate(pricingDate, c).equalsIgnoreCase(monthYear)) {
			return false;
		}
		return true;
	}
	
	public String getMonthYearForDate(LocalDate pricingDate, Curve c) throws PricingException {
		JSONArray expCal = c.getExpiryArr();
		DateTimeFormatter expiryDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
		String monthYear = "";
		if(c.getPriceQuoteRule().equalsIgnoreCase("Settlement Date")) {
			for (int i = 0; i < expCal.length(); i++) {
				JSONObject expObj = expCal.optJSONObject(i);
				LocalDate settlementDate = LocalDateTime.parse(expObj.optString("Settlement Date"), expiryDateFormatter)
						.toLocalDate();
				if(!settlementDate.isBefore(pricingDate)) {
					monthYear =  expObj.optString("MONTH/YEAR");
					break;
				}
			}
		}else {
			for (int i = 0; i < expCal.length(); i++) {
				JSONObject expObj = expCal.optJSONObject(i);
				LocalDate lastTradeDate = LocalDateTime.parse(expObj.optString("Last Trade Date"), expiryDateFormatter)
						.toLocalDate();
				if(!lastTradeDate.isBefore(pricingDate)) {
					monthYear =  expObj.optString("MONTH/YEAR");
					break;
				}
			}
		}
		if (!StringUtils.isEmpty(monthYear)) {
			String quotedPeriod = c.getQuotedPeriod();
			if (!StringUtils.isEmpty(quotedPeriod)) {
				int nearbyMonths = Integer.parseInt(c.getQuotedPeriod().substring(c.getQuotedPeriod().length() - 1));
				LocalDate monthYearDate = calculateLocalDateFromMonthYearString(monthYear, null);
				monthYear = collectionDataFetcher.createMonthYear(monthYearDate.plusMonths(nearbyMonths));
				return monthYear;
			}
		}
		String month = pricingDate.getMonth().toString().substring(0, 3);
		String year = Integer.toString(pricingDate.getYear());
		monthYear=month+year;
		return monthYear;
	}
	
	public double contractualConversionFactor(String productId,String sourceUnitId,String destinationUnitId,double densityValue,String massUnitId,
		String volumeUnitId,ContextProvider tenantProvider) throws PricingException {
			Map<String, Object> getConversionRate = new HashMap<String,Object>();	
			String ConversionFactor = "";
			double contractualConversionFactor=0;
			JSONObject payloadJson = new JSONObject();
				payloadJson.put("productId", productId);
				payloadJson.put("sourceUnitId", sourceUnitId);
				payloadJson.put("destinationUnitId", destinationUnitId);
				payloadJson.put("densityValue", densityValue);
				payloadJson.put("massUnitId", massUnitId);
				payloadJson.put("volumeUnitId", volumeUnitId);
		
			try {
				getConversionRate= massToVolumeConversion.getConversionRate(tenantProvider,payloadJson);
			} catch (Exception e) {
				logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("exception at converting Mass To Volume Conversion unit" + e.getMessage()));
				throw new PricingException(
						messageFetcher.fetchErrorMessage(tenantProvider, "054", new ArrayList<String>()));
			}
			ConversionFactor =  getConversionRate.get("conversionFactor").toString();
			contractualConversionFactor= Double.parseDouble(ConversionFactor);
			if(contractualConversionFactor==0.0) {
			try {
				contractualConversionFactor = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productId, sourceUnitId,
						destinationUnitId);
			} catch (Exception e) {
				logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("exception at Quantity Unit Conversion rate" + e.getMessage()));
				throw new PricingException(
						messageFetcher.fetchErrorMessage(tenantProvider, "052", new ArrayList<String>()));
			}
	  		}
		return contractualConversionFactor;
	}
	
	public JSONObject getBestAvailableForwardPrice(JSONArray forwardPrices, LocalDate pricingDate, Curve c) throws PricingException {
		DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
		Map<String, List<JSONObject>> priceMap = new HashMap<String, List<JSONObject>>();
		if(null == c.getForwardPriceMap() || c.getForwardPriceMap().isEmpty()) {
			for(int i=0; i< forwardPrices.length(); i++) {
				JSONObject forwardPriceObject = forwardPrices.optJSONObject(i);
				if(priceMap.containsKey(forwardPriceObject.optString("Month/Year"))) {
					List<JSONObject> objList = priceMap.get(forwardPriceObject.optString("Month/Year"));
					objList.add(forwardPriceObject);
					priceMap.put(forwardPriceObject.optString("Month/Year"), objList);
				}
				else {
					List<JSONObject> objList = new ArrayList<JSONObject>();
					objList.add(forwardPriceObject);
					priceMap.put(forwardPriceObject.optString("Month/Year"), objList);
				}
			}
		}
		else {
			priceMap = c.getForwardPriceMap();
		}

		String monthYear = getMonthYearForDate(pricingDate, c);
		double bestPrice = 0d;
		JSONObject res = new JSONObject();
//		we will go back 6 months till we find prices for curve in a monthYear
		for(int i=0; i<6; i++) {
			if(priceMap.containsKey(monthYear)) {
				for(JSONObject jobj : priceMap.get(monthYear)) {
					String pricingDateStr = jobj.optString("Pricing Date");
					if (pricingDateStr.contains("T")) {
						pricingDateStr = pricingDateStr.replaceAll("T", " ");
					}
					LocalDate pricingDateInObject = LocalDate.parse(pricingDateStr, formatter1);
					if(pricingDate.equals(pricingDateInObject)) {
						res.put("price", jobj.optDouble("Forward Prices"));
						if(i==0) {
							res.put("tag", "Forward");
						}
						else {
							res.put("tag", "Forward (Estimated)");
						}
						return res;
					}
					else if(pricingDate.isAfter(pricingDateInObject)) {
						bestPrice = jobj.optDouble("Forward Prices");
					}
					else {
						continue;
					}
				}
			}
			if(!curveService.checkZero(bestPrice)) {
				res.put("price", bestPrice);
				res.put("tag", "Forward (Estimated)");
				return res;
			}
			LocalDate dateForMonthYear = calculateLocalDateFromMonthYearString(monthYear, null);
			dateForMonthYear = dateForMonthYear.minusMonths(1);
			monthYear = collectionDataFetcher.createMonthYear(dateForMonthYear);
		}
		return res.put("price", 0);
	}
	
	public int checkEventNamefromConnect(Contract contract, String contractItemQty,JSONObject jobj,String productId,String contractEventName,List<GMR> gmrList) throws Exception  {
		int count=0;
		List<GMR> gmrListFromConnect = new ArrayList<GMR>();
		gmrListFromConnect = formulaeCalculator.gmrListFromConnect(contract,contractItemQty,jobj,productId,gmrList);
		for (GMR gmr : gmrListFromConnect) {
			List<Event> eventList = gmr.getEvent();
			int i=0;
			String eventName="";
			for (Event e : eventList) {
				if(eventList.size()>1){
					i++;
					if(i==1) {
						eventName=e.getName();
					 }
				if(i==2 && e.getName().equalsIgnoreCase("RollOver")) {
					eventName=e.getName();
				 }
				}else {
					eventName=e.getName();
				}
			}
			if(eventName.equalsIgnoreCase(contractEventName)) {
				count++;
				return count;
			}
		}
		return count;
		
	}

}