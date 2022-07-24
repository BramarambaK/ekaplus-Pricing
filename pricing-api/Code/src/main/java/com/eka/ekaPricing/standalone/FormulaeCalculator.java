package com.eka.ekaPricing.standalone;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.eka.ekaPricing.cache.RedisCacheManager;
import com.eka.ekaPricing.exception.PricingException;
import com.eka.ekaPricing.helper.TriggerPriceCalculator;
import com.eka.ekaPricing.pojo.Contract;
import com.eka.ekaPricing.pojo.ContractItem;
import com.eka.ekaPricing.pojo.Curve;
import com.eka.ekaPricing.pojo.CurveCalculatorFields;
import com.eka.ekaPricing.pojo.CurveDetails;
import com.eka.ekaPricing.pojo.Event;
import com.eka.ekaPricing.pojo.Formula;
import com.eka.ekaPricing.pojo.GMR;
import com.eka.ekaPricing.pojo.GMRQPDateDetails;
import com.eka.ekaPricing.pojo.GMRQualityDetails;
import com.eka.ekaPricing.pojo.GMRStatusObject;
import com.eka.ekaPricing.pojo.HolidayRuleDates;
import com.eka.ekaPricing.pojo.HolidayRuleDetails;
import com.eka.ekaPricing.pojo.PDSchedule;
import com.eka.ekaPricing.pojo.PayloadInput;
import com.eka.ekaPricing.pojo.PreviewData;
import com.eka.ekaPricing.pojo.PricingComponent;
import com.eka.ekaPricing.pojo.QualityAdjustment;
import com.eka.ekaPricing.pojo.QualityAttributes;
import com.eka.ekaPricing.pojo.Stock;
import com.eka.ekaPricing.pojo.TieredFields;
import com.eka.ekaPricing.pojo.TieredPricingItem;
import com.eka.ekaPricing.pojo.TriggerPrice;
import com.eka.ekaPricing.pojo.TriggerPriceProperties;
import com.eka.ekaPricing.service.CurveService;
import com.eka.ekaPricing.util.CommonValidator;
import com.eka.ekaPricing.util.ContextProvider;
import com.eka.ekaPricing.util.JsonConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

@Component
public class FormulaeCalculator {

	@Autowired
	CollectionDataFetcher fetcher;
	@Autowired
	CurveDataFetcher curveFetcher;
	@Autowired
	PeriodFetcher periodFetcher;
	@Autowired
	FormulaData formulaData;
	@Autowired
	com.eka.ekaPricing.standalone.ExpressionBuilder expressionBuilder;
	@Autowired
	TriggerPriceCalculator triggerPriceCalculator;
	@Autowired
	ContractDataFetcher pricePrecisionFetcher;
	@Autowired
	CurveService curveService;
	@Autowired
	FXCurveFetcher rateFetcher;
	@Autowired
	FormulaFetcher formFetcher;
	@Autowired
	JsonConverter jcon;
	@Autowired
	ContractItemFetcher itemsFetcher;
	@Autowired
	QuantityFXFetcher qtyRateFetcher;
	@Autowired
	CurveQuantitySetter qtySetter;
	@Autowired
	MDMServiceFetcher mdmFetcher;
	@Autowired
	ComponentFetcher compFetcher;
	@Autowired
	RedisCacheManager redisCacheManager;
	@Autowired
	GMRCreationHelper gmrCreationHelper;
	@Autowired
	TieredPricingObjectFetcher tieredPricingObjectFetcher;
	@Autowired 
	ExposureAppendHelper exposureAppendHelper;
	@Autowired
	TriggerPriceFetcher triggerPriceFetcher;
	@Autowired
	ErrorMessageFetcher messageFetcher;
	@Autowired
	KafkaProducerhelper kafkaProducerHelper;
	@Autowired
	ContextProvider context;
	@Autowired
	GMRDetailsFetcher gmrDetailsFetcher;
	@Autowired
	CommonValidator validator;
	@Autowired
	ExpiryCalenderFetcher expiryCalenderFetcher;
	@Autowired
	CollectionDataFetcher collectionDataFetcher;
	@Autowired
	GMRStatusObjectFetcher gmrStatusObjectFetcher;
	@Autowired
	GMRModificationHelper gmrModificationHelper;
	@Autowired 
	ConnectExposureData connectExposureData;
	@Autowired 
	MassToVolumeConversion massToVolumeConversion;

	private static final String UNPRICEDOPENPOSITIONS = "Unpriced Open Positions";
	private static final String PRICEDOPENPOSITIONS = "Priced Open Positions";
	private static final String UNPRICEDSTOCKSRECEIVED = "Unpriced Stocks Received";
	private static final String PRICEDSTOCKSRECEIVED = "Priced Stocks Received";
	private static final String UNPRICEDSTOCKSDELIVERED = "Unpriced Stocks Delivered";
	private static final String PRICEDSTOCKSDELIVERED = "Priced Stocks Delivered";
	private static final String OPENPURCHASE = "Open Purchase";
	private static final String INVENTORY = "Inventory";
	private static final String OPENSALES = "Open Sales";
	private static final String PRICERISK = "Price Risk";
	private static final String DELIVERYRISK = "Delivery Risk";
	private static final String NORISK = "No Risk";
	
//	private HashMap<String, String> exchangeMap = new HashMap<String, String>();
//	private LocalDate asOfDate = null;
//	private String lookbackDate = "";
//	private List<String> internalPriceUnitIdList = new ArrayList<String>();
	final static Logger logger = ESAPI.getLogger(FormulaeCalculator.class);

	public List<JSONObject> processExpression(List<CurveDetails> detailsList, List<JSONObject> itemList,
			ContextProvider tenantProvider, List<String> currencyList, List<String> qtyUnitList,
			List<String> productIdList, String contractItemQty, List<PricingComponent> compList,
			String internalContractRefNo, List<TieredPricingItem> tieredPricingItemList, String contractType,
			List<String> internalPriceUnitIdList, String quality,double contractQualityDensity,
			String contractQualityMassUnit,String contractQualityVolumeUnit,String locationType,Contract contract) throws Exception {
		String resultString;
		List<JSONObject> resultDataSet = new ArrayList<>();
		HashMap<String, String> exchangeMap = new HashMap<String, String>();
		JSONObject itemDetailsObj = new JSONObject();
		JSONObject GMRDetailsObj = new JSONObject();
		JSONArray stockoutArr = new JSONArray();
		JSONArray gmrOutArr = new JSONArray();
		JSONObject stockDetailsObj = new JSONObject();
		List<JSONObject> objList = new ArrayList<JSONObject>();
		List<GMR> gmrList = new ArrayList<GMR>();
		double unpricedQuantity = 0;
		double pricedQuantity = 0;
		String marketPrice = null;
		int i = 0;
		if (itemList.size() == 0) {
			List<Curve> curveDetails = detailsList.get(i).getCurveList();
			List<TriggerPrice> triggerPriceList = detailsList.get(i).getTriggerPriceList();
			String expp = detailsList.get(i).getExpression();
			String holidayRule = detailsList.get(i).getHolidayRule();
			String expression = calculateCurve(expp, curveDetails, null, detailsList.get(i).getPricePrecision(), -1,
					null, tenantProvider, currencyList, qtyUnitList, holidayRule, productIdList);
			if(expression.contains("MIN") || expression.contains("AVG") || expression.contains("MAX")) {
				expression = curveFetcher.calculateAggregate(expression, tenantProvider);
			}
			Expression e = null;
			try {
				e = new ExpressionBuilder(expression).build();
			} catch (Exception exc) {
				throw new PricingException(
						messageFetcher.fetchErrorMessage(tenantProvider, "001", new ArrayList<String>()));
			}
			double evalData = 0;
			try {
				evalData = e.evaluate();
			} catch (Exception exc) {
				throw new PricingException(
						messageFetcher.fetchErrorMessage(tenantProvider, "001", new ArrayList<String>()));
			}
			resultString = Double.toString(evalData);
			List<JSONObject> previewDataSet = getPreviewDataSet(curveDetails, null, tenantProvider, holidayRule,
					productIdList.get(i), contractItemQty, gmrList, expp, contractType, exchangeMap, contractQualityDensity,
					 contractQualityMassUnit, contractQualityVolumeUnit,quality,locationType,contract);
			JSONObject dataSet = new JSONObject();
			Date tradeDate = null;
			double previewPrice = 0.0d;
			if (detailsList.get(i).getPriceDifferentialList() != null
					&& detailsList.get(i).getPriceDifferentialList().size() > 0) {
				tradeDate = new Date();
				previewPrice = curveService.applyDifferentialPrice(detailsList.get(i), resultString,
						currencyList.get(i).trim(), tradeDate);
				resultString = Double.toString(previewPrice);
			}
			dataSet = putValue(dataSet, "contractPrice", resultString);
			dataSet.put("curveData", previewDataSet);
			dataSet = putValue(dataSet, "marketPrice", "");
			dataSet = putValue(dataSet, "originalExpression", detailsList.get(i).getOriginalExp());
			dataSet = putValue(dataSet, "priceUnit", currencyList.get(i) + "/MT");
//			dataSet = putValue(dataSet, "itemID", "");
//			dataSet = putValue(dataSet, "itemDeliveryFrom", "");
//			dataSet = putValue(dataSet, "itemDeliveryTo", "");
			dataSet = putValue(dataSet, "pricedQuantity", "");
			dataSet = putValue(dataSet, "unpricedQuantity", "");
			dataSet = putValue(dataSet, "quantityUnit", "");

			stockDetailsObj = putValue(stockDetailsObj, "stockRefNo", "");
			stockDetailsObj = putValue(stockDetailsObj, "refNo", "");
			stockDetailsObj = putValue(stockDetailsObj, "stockQty", "");
			stockDetailsObj = putValue(stockDetailsObj, "stockQtyUnit", "");
			stockDetailsObj = putValue(stockDetailsObj, "stockPrice", "");
			stockoutArr.put(stockDetailsObj);
			GMRDetailsObj = putValue(GMRDetailsObj, "GMRRefNo", "");
			GMRDetailsObj = putValue(GMRDetailsObj, "price", "");
//			GMRDetailsObj = putValue(GMRDetailsObj, "ContractItemExecutedQuantity", "");
			GMRDetailsObj.put("stocks", stockoutArr);
			itemDetailsObj = putValue(itemDetailsObj, "refNo", "");
			itemDetailsObj = putValue(itemDetailsObj, "QPStartDate", "");
			itemDetailsObj = putValue(itemDetailsObj, "QPEndDate", "");
			resultDataSet.add(dataSet);
			itemDetailsObj.put("priceDetails", dataSet);
			itemDetailsObj.put("gmrDetails", GMRDetailsObj);
//			itemDetailsObj.put("stock", stockoutArr);
			objList.add(itemDetailsObj);

		} else {
			if (context.getCurrentContext().getLookbackDate().length() == 0) {
				String lookBackDate = itemList.get(0).optString("deliveryFromDate", "");
				context.getCurrentContext().setLookbackDate(lookBackDate);
			}

			for (JSONObject itemObj : itemList) {
				stockoutArr = new JSONArray();
				itemDetailsObj = new JSONObject();
				GMRDetailsObj = new JSONObject();
				stockDetailsObj = new JSONObject();
				String itemQty = itemObj.optString("qty", "0");
				resultDataSet.clear();
				List<Curve> curveDetails = detailsList.get(i).getCurveList();
				List<TriggerPrice> triggerPriceList = detailsList.get(i).getTriggerPriceList();
				String expp = detailsList.get(i).getExpression();
				String precision = null;
				try {
					precision = itemObj.getString("pricePrecision");
				} catch (Exception e) {
					precision = detailsList.get(i).getPricePrecision();
				}
				String holidayRule = detailsList.get(i).getHolidayRule();
				String expression = calculateCurve(expp, curveDetails, itemObj, precision, i, null, tenantProvider,
						currencyList, qtyUnitList, holidayRule, productIdList);
				while(expression.contains("MIN") || expression.contains("MAX") || expression.contains("AVG")) {
					expression = curveFetcher.calculateAggregate(expression, tenantProvider);
				}
//				expression = curveFetcher.calculateAggregate(expression);
				Expression ex = null;
				try {
					ex = new ExpressionBuilder(expression).build();
				} catch (Exception exc) {
					throw new PricingException(
							messageFetcher.fetchErrorMessage(tenantProvider, "001", new ArrayList<String>()));
				}
				if(curveService.checkZero(Double.parseDouble(itemQty))) {
					itemQty = itemObj.optString("itemQty");
				}
				curveDetails = qtySetter.setQuanity(expp, curveDetails, Double.parseDouble(itemQty), itemObj,
						tenantProvider, exchangeMap, context.getCurrentContext().getAsOfDate(), holidayRule, compList);
				resultString = Double.toString(ex.evaluate());
				String rawExpForMarketPrice = expp;
				String marketPriceExp = calculateMarketPrice(rawExpForMarketPrice, curveDetails, itemObj, precision, i,
						tenantProvider, currencyList, qtyUnitList, holidayRule, productIdList);
				while (marketPriceExp.contains("MIN") || marketPriceExp.contains("MAX")
						|| marketPriceExp.contains("AVG")) {
					marketPriceExp = curveFetcher.calculateAggregate(marketPriceExp, tenantProvider);
				}
				ex = new ExpressionBuilder(marketPriceExp).build();
				marketPrice = Double.toString(ex.evaluate());
				String itemQtyUnit = mdmFetcher.getQuantityKey(null, contractItemQty, null);
				String[] baseQtyUnitId = mdmFetcher.getBaseQtyUnit(tenantProvider, productIdList.get(0));
			    double baseQtyConversionFactor = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productIdList.get(0), itemQtyUnit,baseQtyUnitId[0]);
				double previewPrice = 0;
				Date tradeDate = null;
				TriggerPriceProperties triggerProps = new TriggerPriceProperties();
				triggerProps.setCurrencyList(currencyList);
				triggerProps.setGmr(null);
				triggerProps.setHolidayRule(holidayRule);
				triggerProps.setItemObj(itemObj);
				triggerProps.setPrecision(precision);
				triggerProps.setProductIdList(productIdList);
				triggerProps.setQtyUnitList(qtyUnitList);
				triggerProps.setContractItemQty(contractItemQty);
				triggerProps.setQuality(quality);
				triggerProps.setBaseQtyUnitConversion(baseQtyConversionFactor);
				if (detailsList.get(i).isTriggerPriceEnabled() && !curveDetails.isEmpty()) {
					resultString = evaluateTriggerPrice(detailsList.get(i), tenantProvider, productIdList.get(i),
							qtyUnitList.get(i), itemQty, resultString, triggerProps);
					
				}
				if (detailsList.get(i).getPriceDifferentialList() != null
						&& detailsList.get(i).getPriceDifferentialList().size() > 0) {
					tradeDate = new Date();
					previewPrice = curveService.applyDifferentialPrice(detailsList.get(i), resultString,
							currencyList.get(i).trim(), tradeDate);
					resultString = Double.toString(previewPrice);
				}
				JSONArray movArray = itemObj.optJSONArray("gmrDetails");
				if(movArray != null) {
					movArray = sortMovementArr(movArray);
				}
				int j = 0;
				List<Stock> stockList = null;
				List<QualityAttributes> stockQualityList = null;
				QualityAdjustment qualityAdjustment = null;
				QualityAttributes qualityAttributes = null;
				String qualityAdjustmentName = null;
				String deliveredQuality = null;
				String premiumOrDiscount = null;
				String calculationType = null;
				String PDScheduleType = null;
				List<String> curveNamesList = new ArrayList<String>();
				curveDetails.forEach(cd -> curveNamesList.add(cd.getCurveName()));
				
				stockList = new ArrayList<Stock>();
				List<Double> stockPriceList = new ArrayList<Double>();
				List<GMRStatusObject> gmrStatusObjectList = gmrStatusObjectFetcher
						.fetchGMRStatusObject(itemObj.optString("refNo"));
				JSONArray gmrDataArr = gmrCreationHelper.fetchGMRData(itemObj.optString("refNo"));
				List<String> storedGMRList = new ArrayList<String>();
				Map<String, Double> gmrStoredPriceMap = new HashMap<String, Double>();
				boolean isGMRFixationCompleted = false;
				if (null != movArray) {
					for (int movInd = 0; movInd < movArray.length(); movInd++) {
						for (int gmrInd = 0; gmrInd < gmrDataArr.length(); gmrInd++) {
							JSONObject gmrData = gmrDataArr.optJSONObject(gmrInd);
							if (!gmrStatusObjectList.isEmpty() && gmrData.optString("internalGMRRefNo")
									.equalsIgnoreCase(movArray.optJSONObject(movInd).optString("refNo", ""))) {
								for (GMRStatusObject statusObj : gmrStatusObjectList) {
									if (null != statusObj.getGmrRefNo()
											&& !statusObj.getGmrStatus().equalsIgnoreCase("cancelled")
											&& statusObj.getGmrRefNo().equals(gmrData.optString("internalGMRRefNo"))
											&& curveService.checkZero(statusObj.getGmrUnFixedQty())) {
										storedGMRList.add(gmrData.optString("internalGMRRefNo"));
										isGMRFixationCompleted = true;
										gmrStoredPriceMap.put(gmrData.optString("internalGMRRefNo"),
												gmrData.optDouble("estimatedPrice"));
									}
									double qtyInStoredStatus = Double.parseDouble(statusObj.getGmrQty());
									double qtyInStoredGMR = gmrData.optDouble("executedQuantity");
									double qtyInNewGMR = 0;
									JSONArray stockArray = movArray.optJSONObject(movInd).optJSONArray("stocks");
									for(int stockInd=0; stockInd<stockArray.length();stockInd++) {
										JSONObject stockObj = stockArray.optJSONObject(stockInd);
										qtyInNewGMR = qtyInNewGMR + stockObj.optDouble("qty");
									}
									boolean allFixationsUsed = false;
									for(TriggerPrice trigger : detailsList.get(i).getTriggerPriceList()) {
										if(curveService.checkZero(trigger.getItemFixedQtyAvailable())) {
											allFixationsUsed = true;
										}
										else {
											allFixationsUsed = false;
											break;
										}
										
									}
									if(null != statusObj.getGmrRefNo()
											&& !statusObj.getGmrStatus().equalsIgnoreCase("cancelled")
											&& statusObj.getGmrRefNo().equals(gmrData.optString("internalGMRRefNo"))
											&& curveService.checkZero(qtyInStoredStatus-qtyInNewGMR)
											&& curveService.checkZero(qtyInStoredGMR-qtyInNewGMR)
											&& allFixationsUsed) {
										storedGMRList.add(gmrData.optString("internalGMRRefNo"));
										isGMRFixationCompleted = true;
										gmrStoredPriceMap.put(gmrData.optString("internalGMRRefNo"),
												gmrData.optDouble("estimatedPrice"));
									}
								}
								
							}
						}
					}
					
				}
				if (movArray != null) {// parsing stock details
					while (j < movArray.length()) {
						JSONObject gmrObj = movArray.getJSONObject(j);
						String gmrRef = gmrObj.optString("refNo", "");
						String titleTransferStatus = gmrObj.optString("titleTransferStatus", "");
						String storageLocation = gmrObj.optString("storageLocation", "");
						String loadingLocType = gmrObj.optString("loadingLocType", "");
						String loadingLocName = gmrObj.optString("loadingLocName", "");
						String destinationLocType = gmrObj.optString("destinationLocType", "");
						String destinationLocName = gmrObj.optString("destinationLocName", "");
						String vesselName = gmrObj.optString("vesselName", "");
						GMR gmr = new GMR();
						gmr.setRefNo(gmrRef);
						gmr.setTitleTransferStatus(titleTransferStatus);
						gmr.setStorageLocation(storageLocation);
						gmr.setLoadingLocType(loadingLocType);
						gmr.setLoadingLocName(loadingLocName);
						gmr.setDestinationLocType(destinationLocType);
						gmr.setDestinationLocName(destinationLocName);
						gmr.setVesselName(vesselName);
						if(!storedGMRList.contains(gmrRef)) {
							isGMRFixationCompleted = false;
						}
						if(null!=compList && !compList.isEmpty()) {
							compList = compFetcher.fetchComponent(tenantProvider, itemObj.optString("refNo"), gmrRef);
							if (!compList.isEmpty()) {
								String expressionAfterBuilding = detailsList.get(i).getOriginalExp();
								expressionAfterBuilding = expressionBuilder.buildExpression(curveNamesList,
										expressionAfterBuilding);
								calculateComponent(curveDetails, compList, expressionAfterBuilding, 0, tenantProvider);
								expressionAfterBuilding = expressionBuilder.simplifyExpression(expressionAfterBuilding,
										compList);
								expp = expressionAfterBuilding;
							}
							
						}
						
						JSONArray stockArray = gmrObj.optJSONArray("stocks");
						int k = 0;
						if (stockArray != null && stockArray.length() > 0) {
							while (k < stockArray.length()) {
								JSONObject stockObj = stockArray.getJSONObject(k);
								Stock stock = new Stock();
								if(!StringUtils.isEmpty(gmrObj.optString("gmrCreationDate"))) {
									stock.setGmrCreationDate(gmrObj.optString("gmrCreationDate"));
								}
								String stockRefNo = stockObj.optString("refNo");
								String contractItemRefNo = itemObj.optString("refNo");
								double stockQty = stockObj.optDouble("qty", 0l);
								double massToVolConFactor = stockObj.optDouble("massToVolConversionFactor", 0l);
								List<Event> eventList = new ArrayList<Event>();
								JSONArray eventArr = stockObj.optJSONArray("event");
								int eventInd = 0;
								while (eventInd < eventArr.length()) {
									JSONObject eventObject = eventArr.getJSONObject(eventInd);
									Event e = new Event();
									e.setName(eventObject.optString("name", ""));
									e.setDate(eventObject.optString("date"));
									eventList.add(e);
									eventInd++;
								}
								GMR gmrForPrice = new GMR();
								gmrForPrice.setRefNo(gmrRef);
								gmrForPrice.setStockRef(stockRefNo);
								gmrForPrice.setEvent(eventList);
								
								gmr.setRefNo(gmrRef);
								gmr.setStockRef(stockRefNo);
								gmr.setEvent(eventList);
								/*
								 *Setting the curve level calculated price to zero because it will reset when GMR details are passed. 
								 * */								
								for(Curve c: curveDetails) {
									c.setCalculatedPrice(0.0);
								}
								String rawExpForStockPrice = expp;
								String stockPriceExp = calculateCurve(rawExpForStockPrice, curveDetails, itemObj, precision, i,
										gmr, tenantProvider, currencyList, qtyUnitList, holidayRule, productIdList);
								while (stockPriceExp.contains("MIN") || stockPriceExp.contains("MAX")
										|| stockPriceExp.contains("AVG")) {
									stockPriceExp = curveFetcher.calculateAggregate(stockPriceExp, tenantProvider);
								}
								ex = new ExpressionBuilder(stockPriceExp).build();
								double stockLevelPrice = ex.evaluate();
//								double stockLevelPrice = Double.parseDouble(engine.eval(
//										calculateCurve(expp, curveDetails, itemObj, precision, i, gmr, tenantProvider))
//										.toString());
								double stockLevelPreviewPrice = 0;
								if (detailsList.get(i).getPriceDifferentialList() != null
										&& detailsList.get(i).getPriceDifferentialList().size() > 0) {
									tradeDate = new Date();
									stockLevelPreviewPrice = curveService.applyDifferentialPrice(detailsList.get(i), stockLevelPrice+"",
											currencyList.get(i).trim(), tradeDate);
									stockLevelPrice = stockLevelPreviewPrice;
								}
								int ind = 0;
								JSONArray qualityArray = stockObj.optJSONArray("attributes");
								stockQualityList = new ArrayList<QualityAttributes>();
								if (qualityArray != null && qualityArray.length() > 0) {
									while (ind < qualityArray.length()) {
										qualityAttributes = new QualityAttributes();
										JSONObject qualityObj = qualityArray.getJSONObject(ind);
										qualityAdjustmentName = qualityObj.optString("name");
										deliveredQuality = qualityObj.optString("value", "0.0");
										qualityAttributes.setName(qualityAdjustmentName);
										qualityAttributes.setValue(deliveredQuality);
										stockQualityList.add(qualityAttributes);
										ind++;
									}
								}
								stockLevelPrice = curveService.applyPremiumDiscountOnStocks(stockQualityList,
										stockLevelPrice, Double.parseDouble(resultString), tenantProvider,
										itemObj.optString("refNo", ""));
								stockLevelPrice = expressionBuilder.applyPrecision(stockLevelPrice, Integer.parseInt(precision));
								String itmeQtyUnitId = mdmFetcher.getQuantityKey(tenantProvider, itemObj.getString("qtyUnit"),
										productIdList.get(i));
								double QtyConRate = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productIdList.get(i),
										stockObj.optString("qtyUnit"), itmeQtyUnitId);
								if(!curveService.checkZero(stock.getMassToVolConversionFactor())) {
									QtyConRate = stock.getMassToVolConversionFactor();
								}
								double stockQtyInGMR = stockQty;
								stockQty = stockQty * QtyConRate;
								stockPriceList.add(stockLevelPrice);
								stock.setRefNo(stockRefNo);
								stock.setGMRRefNo(gmrRef);
								stock.setContractRefNo(internalContractRefNo);
								stock.setContractItemRefNo(contractItemRefNo);
								stock.setQty(stockQty); // Stock Qty in Item Qty conversion
								stock.setStockQtyInGmr(stockQtyInGMR); // New field for GMR Qty
								stock.setQtyUnit(stockObj.optString("qtyUnit"));
								stock.setQtyUnitId(stockObj.optString("qtyUnit"));
								stock.setDensityValue(stockObj.optDouble("densityValue"));
								stock.setMassUnitId(stockObj.optString("massUnitId"));
								stock.setVolumeUnitId(stockObj.optString("volumeUnitId"));
								stock.setMassToVolConversionFactor(stockObj.optDouble("massToVolConversionFactor"));
								stock.setDensityVolumeQtyUnitId(stockObj.optString("densityVolumeQtyUnitId"));
								stock.setQuality(stockObj.optString("quality"));
								stock.setItemQtyUnit(contractItemQty);
								stock.setItemQtyUnitId(itemQtyUnit);
								stock.setQtyConversionRate(QtyConRate);
								
								stock.setAttributes(stockQualityList);
								stockList.add(stock);
								k++;
							}
						}
						gmr.setStocks(stockList);
						gmrList.add(gmr);
						j++;
					}
				}
				if(detailsList.get(i).getTriggerPriceList().size()==0) {
					detailsList.get(i).setTriggerPriceEnabled(false);
				}
				List<TriggerPrice> triggerList = triggerPriceFetcher.fetchTriggerPriceDetails(null, itemObj.optString("refNo"));
				if (movArray != null && movArray.length()>0) {
					if (detailsList.get(i).isTriggerPriceEnabled() && !isGMRFixationCompleted) {
						stockList = triggerPriceCalculator.applyTriggerPriceAtStock(triggerList, stockList, null,
								stockPriceList, false, gmrStatusObjectList, detailsList.get(i).getCurveList(),
								context.getCurrentContext().getAsOfDate(), storedGMRList, tieredPricingItemList, false,
								gmrDataArr);
					} else {
						stockList = triggerPriceCalculator.applyTriggerPriceAtStock(new ArrayList<TriggerPrice>(),
								stockList, null, stockPriceList, false, gmrStatusObjectList,
								detailsList.get(i).getCurveList(), context.getCurrentContext().getAsOfDate(),
								storedGMRList, tieredPricingItemList, false, gmrDataArr);
					}
				}
				// parsing contract quality
				JSONArray itemQualityArray = itemObj.optJSONArray("attributes");
				boolean forContract = false;
				JSONArray itemPDArray = itemObj.optJSONArray("pdSchedule");
				List<QualityAttributes> itemQualityList = new ArrayList<QualityAttributes>();
				List<QualityAdjustment> details = new ArrayList<QualityAdjustment>();
				List<PDSchedule> itemPDList = new ArrayList<PDSchedule>();
				int indx = 0;
				int pdIndx = 0;
				JSONArray pdDetArray = null;
				String rangeOrder = null;
				String rangeLower = null;
				String rangeUpper = null;
				String stepSize = null;
				String stepValue = null;
				String rangeType = null;
				String itemQualityName = null;
				String itemQualityVal = null;
				String attrName = null;
				String priceUnit = null;
				String basePD = null;
				PDSchedule pdSchedule = null;
				JSONObject detObj = null;
				if (itemQualityArray != null && itemQualityArray.length() > 0) {
					while (indx < itemQualityArray.length()) {
						qualityAttributes = new QualityAttributes();
						JSONObject itemQualityObj = itemQualityArray.getJSONObject(indx);
						itemQualityName = itemQualityObj.get("name").toString();
						itemQualityVal = itemQualityObj.get("value").toString();
						qualityAttributes.setName(itemQualityName);
						qualityAttributes.setValue(itemQualityVal);
						itemQualityList.add(qualityAttributes);
						indx++;
					}
				}
				if (itemPDArray != null && itemPDArray.length() > 0) {
					while (pdIndx < itemPDArray.length()) {
						pdSchedule = new PDSchedule();
						JSONObject itemPDObj = itemPDArray.getJSONObject(pdIndx);
						attrName = itemPDObj.get("attributeName").toString();
						priceUnit = itemPDObj.get("priceUnit").toString();
						pdSchedule.setAttributeName(attrName);
						pdSchedule.setPriceUnit(priceUnit);
						pdDetArray = itemPDObj.optJSONArray("details");
						int detIndx = 0;
						details = new ArrayList<QualityAdjustment>();
						if (pdDetArray != null && pdDetArray.length() > 0) {
							while (detIndx < pdDetArray.length()) {
								detObj = pdDetArray.getJSONObject(detIndx);
								qualityAdjustment = new QualityAdjustment();
								PDScheduleType = detObj.get("rateType").toString();
								if (PDScheduleType != null && !PDScheduleType.equals("")) {
									premiumOrDiscount = detObj.get("premiumOrDiscount").toString();
									calculationType = detObj.optString("pdInPercOrRate", "Rate").toString();
									basePD = detObj.optString("basePD", "0.0").toString();
									stepSize = detObj.optString("stepSize", "0.0");
									stepValue = detObj.optString("pdIncValue", "0.0");
									qualityAdjustment.setRateType(PDScheduleType);
									qualityAdjustment.setPremiumOrDiscount(premiumOrDiscount);
									qualityAdjustment.setPdInPercOrRate(calculationType);
									qualityAdjustment.setBasePD(Double.parseDouble(basePD));
									qualityAdjustment.setStepSize(Double.parseDouble(stepSize));
									qualityAdjustment.setPdIncValue(Double.parseDouble(stepValue));
									if (PDScheduleType.equalsIgnoreCase("Range")) {
										forContract = true;// PDschedule to be calculated for contract price only in
															// case of Range type
										rangeOrder = detObj.optString("rangeOrder", "A");
										rangeLower = detObj.optString("rangeLower", "0.0");
										rangeUpper = detObj.optString("rangeUpper", "0.0");
										rangeType = detObj.optString("rangeType", "Independent");
										qualityAdjustment.setRangeOrder(rangeOrder);
										qualityAdjustment.setRangeLower(rangeLower);
										qualityAdjustment.setRangeUpper(rangeUpper);
										qualityAdjustment.setRangeType(rangeType);
									}
								}
								detIndx++;
								details.add(qualityAdjustment);
							}
							pdSchedule.setDetails(details);
						}
						itemPDList.add(pdSchedule);
						pdIndx++;
					}
				}
				if (stockList != null && stockList.size() > 0) {
					forContract = false;
					curveService.qualityAdjustment(stockList, itemQualityList, itemPDList, resultString, forContract);
				} else {
					stockList = new ArrayList<Stock>();
					if (forContract) {
						double priceWithQlty = curveService.qualityAdjustment(null, itemQualityList, itemPDList,
								resultString, forContract);
						resultString = Double.toString(priceWithQlty);
					}
				}
				List<JSONObject> previewDataSet =null;
				if(!curveDetails.isEmpty()) {
				
					previewDataSet = getPreviewDataSet(curveDetails, itemObj, tenantProvider, holidayRule,
						productIdList.get(i), contractItemQty, gmrList, expp, contractType, exchangeMap,contractQualityDensity,
						 contractQualityMassUnit, contractQualityVolumeUnit,quality,locationType,contract);
				}else {
					
					if (detailsList.get(i).isTriggerPriceEnabled()) {
						resultString = evaluateTriggerPrice(detailsList.get(i), tenantProvider, productIdList.get(i),
								qtyUnitList.get(i), itemQty, resultString, triggerProps);
					
					/*previewDataSet=getAbsoluteExpForTriggerPrice(itemObj,detailsList.get(i), tenantProvider, holidayRule,
							productIdList.get(0), contractItemQty, gmrList, expp, contractType, exchangeMap,quality,
							contractQualityDensity,contractQualityMassUnit,contractQualityVolumeUnit,qtyUnitList.get(i));*/
					
					}
						previewDataSet=getAbsoluteExp(itemObj, tenantProvider, holidayRule,
								productIdList.get(i), contractItemQty, gmrList, expp, contractType, exchangeMap,quality,
								contractQualityDensity,contractQualityMassUnit,contractQualityVolumeUnit,qtyUnitList.get(i),locationType,contract);
				   
				}
				double[] qtyArr = calculatePricedQuantity(curveDetails, itemObj, tenantProvider, exchangeMap);
				pricedQuantity = qtyArr[0];
				unpricedQuantity = qtyArr[1];
				JSONObject dataSet = new JSONObject();
				double precisedPrice = Double.parseDouble(resultString);
				String actualPrice = String.valueOf(precisedPrice);;
				precisedPrice = expressionBuilder.applyPrecision(precisedPrice, Integer.parseInt(precision));
				resultString = String.valueOf(precisedPrice);
				dataSet = putValue(dataSet, "contractPrice", resultString);
				dataSet = putValue(dataSet, "actualPrice", actualPrice);
				dataSet = putValue(dataSet, "marketPrice", marketPrice);
				dataSet = putValue(dataSet, "originalExpression", detailsList.get(i).getOriginalExp());
				dataSet = putValue(dataSet, "priceUnit", currencyList.get(i) + "/" + qtyUnitList.get(i));
				dataSet = putValue(dataSet, "internalPriceUnitId", internalPriceUnitIdList.get(i));
				dataSet.put("curveData", previewDataSet);
				dataSet = putValue(dataSet, "pricedQuantity", Double.toString(pricedQuantity));
				dataSet = putValue(dataSet, "unpricedQuantity", Double.toString(unpricedQuantity));
				dataSet = putValue(dataSet, "quantityUnit", qtyUnitList.get(i));
				double pricedPercentage = 0d;
				double unpricedPercentage = 0d;

				for (Curve c : curveDetails) {
					double pricedDays = c.getPricedDays();
					double unpricedDays = c.getUnPricedDays();
					pricedPercentage =pricedPercentage + pricedDays+unpricedDays;
					
				}
				
				/*				in case the expression is purely numbers and no curves are included the contract will be 100% priced.
				 * 				jira id: CPR-729
				 */
				if(curveDetails.size()==0) {
					pricedPercentage = 100;
				}
				
				dataSet.accumulate("pricedPercentage", pricedPercentage);
				dataSet.accumulate("unpricedPercentage", unpricedPercentage);

				Map<String, JSONArray> gmrMap = new HashMap<String, JSONArray>();
				double openQuantity = itemObj.optDouble("qty");
				double executedQuantity = 0;
				for (Stock st : stockList) {
					String gmrRefNo = st.getGMRRefNo();
					executedQuantity = executedQuantity + st.getQty();
					double stockQty = st.getQty();
					/*Transferring this qty conversion code above 
					String itmeQtyUnitId = mdmFetcher.getQuantityKey(tenantProvider, itemObj.getString("qtyUnit"),
							productIdList.get(i));
					  if (!st.getQtyUnit().equalsIgnoreCase(itmeQtyUnitId)) {
						double QtyConRate = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productIdList.get(i),
								st.getQtyUnit(), itmeQtyUnitId);
						if (QtyConRate == 1) {
							logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML(
									"Qty exchange rate is 1 for " + st.getQtyUnit() + " to " + itmeQtyUnitId));
						}
						if(!curveService.checkZero(st.getMassToVolConversionFactor())) {
							QtyConRate = st.getMassToVolConversionFactor();
						}
						stockQty = stockQty * QtyConRate;

					}*/
					openQuantity = openQuantity - stockQty;
					if (gmrMap.containsKey(gmrRefNo)) {
						JSONArray stList = gmrMap.get(gmrRefNo);
						JSONObject stObject = new JSONObject(st);
						stObject.remove("deliveredQuality");
						stObject.remove("contractItemRefNo");
						stObject.remove("qualityAttrList");
						stObject.remove("GMRRefNo");
						stObject.remove("deliveredPrice");
						stObject.remove("attributes");
						stList.put(stObject);
						gmrMap.put(gmrRefNo, stList);
					} else {
						JSONArray stList = new JSONArray();
						JSONObject stObject = new JSONObject(st);
						stObject.remove("deliveredQuality");
						stObject.remove("contractItemRefNo");
						stObject.remove("qualityAttrList");
						stObject.remove("GMRRefNo");
						stObject.remove("deliveredPrice");
						stObject.remove("attributes");
						stList.put(stObject);
						gmrMap.put(gmrRefNo, stList);
					}
				}

				if (null != movArray && movArray.length() > 0) {
					for (int l = 0; l < movArray.length(); l++) {
						GMRDetailsObj = new JSONObject();
						JSONObject movObj = movArray.optJSONObject(l);
						GMRDetailsObj = putValue(GMRDetailsObj, "refNo", movObj.optString("refNo"));
						JSONArray stArr = gmrMap.get(movObj.optString("refNo"));
						GMRDetailsObj.put("stocks", stArr);
						movObj.put("isWeightAvgFlag", contract.getIsweightAvgFlag());
						double gmrLevelPrice = triggerPriceCalculator.getStockValue(stArr, tenantProvider);
						double newTriggerPrice = triggerPriceCalculator.validGMRPriceForAdjustments(
								gmrStatusObjectList, stockPriceList, movObj, triggerPriceList);
						if (gmrStoredPriceMap.containsKey(movObj.optString("refNo"))) {
							gmrLevelPrice = gmrStoredPriceMap.get(movObj.optString("refNo"));
							if(!curveService.checkZero(newTriggerPrice)) {
								gmrLevelPrice = newTriggerPrice;
							}
						}
						double actualgmrLevelPrice = gmrLevelPrice;
						gmrLevelPrice = expressionBuilder.applyPrecision(gmrLevelPrice, Integer.parseInt(precision));
						GMRDetailsObj.put("price", gmrLevelPrice);
						GMRDetailsObj.put("actualPrice", actualgmrLevelPrice);
						GMRDetailsObj.put("pdPrice", triggerPriceCalculator.getPdPriceValue(stArr, tenantProvider));
						List<GMRStatusObject> latestGmrStatusObjectList = gmrStatusObjectFetcher
								.fetchGMRStatusObject(itemObj.optString("refNo"));
						GMRStatusObject gmrStatusObject = null;
						for(GMRStatusObject statusObj: latestGmrStatusObjectList) {
							if (statusObj.getGmrRefNo().equalsIgnoreCase(movObj.optString("refNo"))
									&& statusObj.getGmrStatus().equalsIgnoreCase("FULLY FIXED")) {
								gmrStatusObject = statusObj;
							}
						}
						if(null!=gmrStatusObject) {
							GMRDetailsObj.put("pricedQty", gmrStatusObject.getGmrFixedQty());
							GMRDetailsObj.put("unpricedQty", 0);
							GMRDetailsObj.put("isFullyPriced", "Y");
						}
						else {
							if (!curveDetails.isEmpty()) {
								double[] pricedArr = calculatePricedQuantityForGMR(stArr, curveDetails,
										context.getCurrentContext().getAsOfDate(), tenantProvider, detailsList.get(i),
										itemObj, gmrList.get(l), exchangeMap, false);
								GMRDetailsObj.put("pricedQty", pricedArr[0]);
								GMRDetailsObj.put("unpricedQty", pricedArr[1]);
								if (pricedArr[1] != 0) {
									GMRDetailsObj.put("isFullyPriced", "N");
								} else {
									GMRDetailsObj.put("isFullyPriced", "Y");
								}
							}
							else {
								double qty = 0;
								for (int index = 0; index < stArr.length(); index++) {
									JSONObject stckObject = stArr.optJSONObject(index);
									double stockQty = stckObject.optDouble("qty");
									qty = qty + stockQty;
								}
								GMRDetailsObj.put("isFullyPriced", "Y");
								GMRDetailsObj.put("pricedQty", qty);
								GMRDetailsObj.put("unpricedQty", 0);
							}
						}
						
//						GMRDetailsObj = putValue(GMRDetailsObj, "ContractItemExecutedQuantity", "");
						gmrOutArr.put(GMRDetailsObj);
						gmrCreationHelper.createGMR(tenantProvider, internalContractRefNo,
								itemObj.optString("refNo", ""), movObj.optString("refNo"),
								Double.toString(executedQuantity), Double.toString(openQuantity),
								Double.toString(gmrLevelPrice), itemObj.getString("qtyUnit"), GMRDetailsObj.toString(),
								movObj.toString());
					}
				}
//				kafkaProducerHelper.push(gmrOutArr.toString(), context.getCurrentContext().getRequestId());
				itemDetailsObj = putValue(itemDetailsObj, "refNo", itemObj.optString("refNo", ""));
				itemDetailsObj = putValue(itemDetailsObj, "qPStartDate", itemObj.optString("deliveryFromDate", ""));
				itemDetailsObj = putValue(itemDetailsObj, "qPEndDate", itemObj.optString("deliveryToDate", ""));

				resultDataSet.add(dataSet);
				itemDetailsObj.put("priceDetails", dataSet);
				if (gmrOutArr.length() > 0) {
					itemDetailsObj.put("gmrDetails", gmrOutArr);
				}
//				itemDetailsObj.put("stock", stockoutArr);
				objList.add(itemDetailsObj);
				i++;
			}

		}

		return objList;
	}

	public String calculateCurve(String exp, List<Curve> curveDetails, JSONObject itemObj, String precision, int index,
			GMR gmr, ContextProvider tenantProvider, List<String> currencyList, List<String> qtyUnitList,
			String holidayRule, List<String> productIdList) throws Exception {
		
//		boolean
		if (exp.contains("{{")) {
			int ind = exp.indexOf("{{");
			int last = exp.indexOf("}}");

			String desiredCurve = exp.substring(ind + 2, last);
			for (Curve c : curveDetails) {
				boolean gmrAvail = false;
				if(null!=gmr && null!=c.getGmrQPDetailsList()) {
					for(GMRQPDateDetails gqd: c.getGmrQPDetailsList()) {
						if(gqd.getGmr().getRefNo().equals(gmr.getRefNo()) && gqd.getGmr().getStockRef().equals(gmr.getStockRef())) {
							gmrAvail = true;
							break;
						}
					}
					if(gmrAvail) {
						continue;
					}
				}
				if (c.getCurveName().equals(desiredCurve) && curveService.checkZero(c.getCalculatedPrice())) {
					int currInd = index;
					if (index == -1) {
						currInd = 0;
					}
					CurveCalculatorFields fields = curveFetcher.calculateFormulae(c, itemObj, precision, c.getFxType(),
							currencyList.get(currInd).trim(), gmr, context.getCurrentContext().getLookbackDate(),
							context.getCurrentContext().getAsOfDate(), tenantProvider, holidayRule);
					double rate = c.getFxInput();
					if (rate == 0) {
						rate = 1;
					}
					
					//gmr has conversion factor else
					double qtyConversionRate = 0.0;
					if(null!=gmr && gmr.getMassToVolumeConversionFactor()!=0) {
						qtyConversionRate = gmr.getMassToVolumeConversionFactor();
					}else {
						qtyConversionRate = c.getQtyUnitConversionFactor();
					}
					
					if (qtyConversionRate == 0) {
						qtyConversionRate = -1;
					}
					String curveCurr = fields.getCurveCurrency();
					String unit = fields.getCurveQty();
					if (c.getDifferential() == null || c.getDifferential().equals("")) {
						c.setDifferential("+0");
					}
					int indexPrecision = c.getIndexPrecision();
//					if(c.getIndexPrecision()!=0) {
//						indexPrecision = c.getIndexPrecision();
//					}
					double data = 0d;
					double priceForCurveWithoutFX = fields.getPriceWithoutDailyFx();
					double avgFx = fields.getAvgPriceFx();
					if (!StringUtils.isEmpty(c.getDifferentialUnit())) {
						String differentialQtyUnit = c.getDifferentialUnit()
								.substring(c.getDifferentialUnit().indexOf("/") + 1, c.getDifferentialUnit().length()).trim();
						String differentialCurrencyUnit = c.getDifferentialUnit().substring(0,
								c.getDifferentialUnit().indexOf("/"));
						double differential = calculateMassToVolume(differentialCurrencyUnit, currencyList.get(index),
								rate, qtyConversionRate, tenantProvider, differentialQtyUnit, qtyUnitList.get(index),
								productIdList.get(index), Double.parseDouble(c.getDifferential()));
						differential = expressionBuilder.applyPrecision(differential, indexPrecision);
						
						data = fields.getOriginalPrice() + differential;
						priceForCurveWithoutFX = priceForCurveWithoutFX + differential;
					}
					else {
						data = fields.getOriginalPrice() + Double.parseDouble(c.getDifferential());
						priceForCurveWithoutFX = priceForCurveWithoutFX + Double.parseDouble(c.getDifferential());
						data = expressionBuilder.applyPrecision(data, indexPrecision);
					}
					
					if(null==gmr) {
						c.setCurvePrice(priceForCurveWithoutFX);
						c.setAvgFx(avgFx);
					}
					if (curveCurr == null) {
						List<String> params = new ArrayList<String>();
						params.add(c.getCurveName());		
						throw new PricingException(
								messageFetcher.fetchErrorMessage(tenantProvider, "002", params));
					}
					if (index != -1 && !curveCurr.trim().equals(currencyList.get(index).trim())
							&& c.getFxType().equals("Fixed")) {
						avgFx = c.getFxInput();
						if(avgFx==0) {
							avgFx=1;
						}
						String priceCurrency = currencyList.get(index).trim();
						String fromUnit = mdmFetcher.getCurrencyKey(tenantProvider, curveCurr, qtyUnitList.get(index),
								productIdList.get(index));
						String toUnit = mdmFetcher.getCurrencyKey(tenantProvider, priceCurrency, qtyUnitList.get(index),
								productIdList.get(index));
						if (StringUtils.isEmpty(fromUnit) || StringUtils.isEmpty(toUnit)
								|| !fromUnit.equals(toUnit)) {
							double curveCurrRate = mdmFetcher.getCurrencyUnitConversionRate(tenantProvider, curveCurr);
							double priceCurrRate = mdmFetcher.getCurrencyUnitConversionRate(tenantProvider,
									priceCurrency);
							avgFx = avgFx * (curveCurrRate / priceCurrRate);
							rate = rate * (curveCurrRate / priceCurrRate);
							if(null==gmr) {
								c.setCurvePrice(priceForCurveWithoutFX);
								c.setAvgFx(avgFx);
							}
						}
						if (rate == -1) {
							throw new PricingException(
									messageFetcher.fetchErrorMessage(tenantProvider, "003", new ArrayList<String>()));
						}
						data = data * rate;
					}
					if (index != -1 && !curveCurr.trim().equals(currencyList.get(index).trim())
							&& c.getFxType().equals("Curve") && fields.getOriginalPrice() != 0d
							&& fields.getConvertedPrice() != 0d) {
						data = fields.getConvertedPrice();
					}
					if (qtyUnitList.size() > 0 && !unit.equals(qtyUnitList.get(index)) && null != tenantProvider) {
						if (qtyConversionRate > 0) {
							data = data/qtyConversionRate;
							c.setQtyUnitConversionFactor(qtyConversionRate);
						}
						else {
							String fromUnit = mdmFetcher.getQuantityKey(tenantProvider, unit, productIdList.get(index));
							String toUnit = mdmFetcher.getQuantityKey(tenantProvider, qtyUnitList.get(index),
									productIdList.get(index));
							double qtyFxRate = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productIdList.get(index),
									fromUnit, toUnit);
							c.setQtyUnitConversionFactor(qtyFxRate);
//							double qtyFxRate = qtyRateFetcher.getQtyFx(tenantProvider, unit, qtyUnitList.get(index));
							if (qtyFxRate == 1) {
								logger.info(Logger.EVENT_SUCCESS,ESAPI.encoder().encodeForHTML("Qty exchange rate is 1 for "+unit+ " to "+qtyUnitList.get(index)));
							}
							data = data / qtyFxRate;
						}
					}
//					data = expressionBuilder.applyPrecision(data, Integer.parseInt(precision));
					if(null==gmr) {
						c.setCalculatedPrice(data);
					}
					if (null == new Double(c.getQtyUnitConversionFactor())
							|| Double.isNaN(c.getQtyUnitConversionFactor())
							|| curveService.checkZero(c.getQtyUnitConversionFactor())) {
						c.setQtyUnitConversionFactor(1);
					}
					exp = exp.replaceFirst(Pattern.quote(exp.substring(ind + 1, last + 1)), "" + data);
					return calculateCurve(exp, curveDetails, itemObj, precision, index, gmr, tenantProvider,
							currencyList, qtyUnitList, holidayRule, productIdList);
				}

			}

		}
		return exp;
	}

	public String calculateMarketPrice(String exp, List<Curve> curveDetails, JSONObject itemObj, String precision,
			int index, ContextProvider tenantProvider, List<String> currencyList,
			List<String> qtyUnitList, String holidayRule, List<String> productIdList)
			throws Exception {
		if (exp.contains("{{")) {
			int ind = exp.indexOf("{{");
			int last = exp.indexOf("}}");

			String desiredCurve = exp.substring(ind + 2, last);
			for (Curve c : curveDetails) {
				if (c.getCurveName().equals(desiredCurve)) {
					int currInd = index;
					if (index == -1) {
						currInd = 0;
					}
					CurveCalculatorFields fields = curveFetcher.calculateMarketPrices(c, itemObj, precision,
							c.getFxType(), currencyList.get(currInd).trim(),
							context.getCurrentContext().getLookbackDate(),
							context.getCurrentContext().getAsOfDate(), tenantProvider);
					double data = fields.getOriginalPrice();
					String curveCurr = fields.getCurveCurrency();
					String unit = fields.getCurveQty();
					if (curveCurr == null) {
						throw new PricingException(
								messageFetcher.fetchErrorMessage(tenantProvider, "002", new ArrayList<String>()));
					}
					if (index != -1 && !curveCurr.trim().equals(currencyList.get(index).trim())
							&& c.getFxType().equals("Fixed")) {
						double rate = c.getFxInput();
						if (rate == 0) {
							rate = 1;
						}
						String priceCurrency = currencyList.get(index).trim();
						String fromUnit = mdmFetcher.getCurrencyKey(tenantProvider, curveCurr, qtyUnitList.get(index),
								productIdList.get(index));
						String toUnit = mdmFetcher.getCurrencyKey(tenantProvider, priceCurrency, qtyUnitList.get(index),
								productIdList.get(index));
						if (StringUtils.isEmpty(fromUnit) || StringUtils.isEmpty(toUnit)
								|| !fromUnit.equals(toUnit)) {
							rate = rate * (mdmFetcher.getCurrencyUnitConversionRate(tenantProvider, curveCurr))
									/ (mdmFetcher.getCurrencyUnitConversionRate(tenantProvider, priceCurrency));
						}
						if (rate == -1) {
							throw new PricingException(
									messageFetcher.fetchErrorMessage(tenantProvider, "003", new ArrayList<String>()));
						}
						data = data * rate;
					}
					if (index != -1 && !curveCurr.trim().equals(currencyList.get(index).trim())
							&& c.getFxType().equals("Curve") && fields.getOriginalPrice() != 0d
							&& fields.getConvertedPrice() != 0d) {
						data = fields.getConvertedPrice();
					}
					if (!unit.equals(qtyUnitList.get(index)) && null != tenantProvider) {
						String fromUnit = mdmFetcher.getQuantityKey(tenantProvider, unit, productIdList.get(index));
						String toUnit = mdmFetcher.getQuantityKey(tenantProvider, qtyUnitList.get(index),
								productIdList.get(index));
						double qtyFxRate = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productIdList.get(index),
								fromUnit, toUnit);
//						double qtyFxRate = qtyRateFetcher.getQtyFx(tenantProvider, unit, qtyUnitList.get(index));
						if (qtyFxRate == 1) {
							logger.info(Logger.EVENT_SUCCESS,ESAPI.encoder().encodeForHTML("Qty exchange rate is 1 for "+unit+ " to "+qtyUnitList.get(index)));
						}
						data = data / qtyFxRate;
					}
					exp = exp.replace(exp.substring(ind + 1, last + 1), "" + data);
					return calculateMarketPrice(exp, curveDetails, itemObj, precision, index, tenantProvider,
							currencyList, qtyUnitList, holidayRule, productIdList);
				}

			}

		}
		return exp;
	}
	
	public List<JSONObject> getPreviewDataSet(List<Curve> curveDetails, JSONObject jobj, ContextProvider tenantProvider,
			String holidayRule, String productId, String contractItemQty, List<GMR> gmrList, String exp,
			String contractType, Map<String, String> exchangeMap,double contractQualityDensity,String contractQualityMassUnit,
			String contractQualityVolumeUnit ,String quality,String locationType,Contract contract) throws Exception {

		List<JSONObject> previewDataSet = new ArrayList<>();
		if(gmrList.size()==0 && contractType!=null) {
			gmrList = gmrListFromConnect(contract,contractItemQty,jobj,productId,gmrList);
		}
		int i = 0;
		List<String> list= new  ArrayList<String>();
		List<String> formExp= expressionBuilder.getExpForm(exp ,list);
		String baseQtyUnit ="";
		double baseContractualConversionFactor=0.0;
		String internalContractItemRefNo = contract.getItemDetails().get(0).getInternalItemRefNo();
		if(null==internalContractItemRefNo) {
			internalContractItemRefNo=contract.getItemDetails().get(0).getRefNo();
		}
		List<TriggerPrice> triggerPriceList = new ArrayList<TriggerPrice>();
		if(null!=contractType) {
			triggerPriceList = triggerPriceFetcher.fetchTriggerPriceDetails(tenantProvider, internalContractItemRefNo);
		}
		for (Curve c : curveDetails) {
			c.setExp(true);
			if(triggerPriceList.size()>0) {
				double totalTriggerPrice = 0;
				for (TriggerPrice triggerPrice : triggerPriceList) {
					 totalTriggerPrice = totalTriggerPrice+triggerPrice.getQuantity();
				}
				c.setTotalTriggerPriceQty(totalTriggerPrice);
				logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("Total TriggerPrice Qty  ")+totalTriggerPrice);
			}
			
			JSONObject curveObj = curveFetcher.getPreviewDataSet(c, jobj, i,
					context.getCurrentContext().getLookbackDate(),
					context.getCurrentContext().getAsOfDate(), tenantProvider, holidayRule, productId,
					contractItemQty, gmrList, exp, contractQualityDensity,
					 contractQualityMassUnit, contractQualityVolumeUnit,locationType,triggerPriceList,contract);
			JSONObject obj = curveObj;
			JSONArray newerExposureArr = new JSONArray();
			JSONArray exposureArray = new JSONArray();
			double totalPricedQty=c.getPricedQty();
			double totalUnpricedQty=c.getUnpricedQty();
			JSONArray newQtyData = new JSONArray();
			JSONArray qtyData = new JSONArray();
			    qtyData = curveObj.optJSONArray("qtyData");
				exposureArray = curveObj.optJSONArray("exposureArray");
				if (exposureArray != null && exposureArray.length() > 0 && contractType!=null) {
					for(int k=0;k < exposureArray.length();k++) {
						JSONObject exposureObj = exposureArray.getJSONObject(k);
						JSONObject qtyObj = qtyData.getJSONObject(k);
						double unPriceQty = qtyObj.optDouble("unPricedQty", 0l);
						double priceQty = qtyObj.optDouble("pricedQty", 0l);
						double unpricedQtyInBaseQtyUnit = qtyObj.optDouble("unpricedQuantityInBaseQtyUnit", 0l);
						double pricedQtyInBaseQtyUnit = qtyObj.optDouble("pricedQuantityInBaseQtyUnit", 0l);
						double unPrice = exposureObj.optDouble("unPricedQty", 0l);
						double price = exposureObj.optDouble("pricedQty", 0l);
						double unpricedBaseQtyUnit = exposureObj.optDouble("unpricedQuantityInBaseQtyUnit", 0l);
						double pricedBaseQtyUnit = exposureObj.optDouble("pricedQuantityInBaseQtyUnit", 0l);
						if(contractType.equals("S") && formExp.get(i).length()>0) {
							if(price!=0) {
								if(formExp.get(i).contains("-")) {
									price = price*-1;
									priceQty=priceQty*-1;
									pricedQtyInBaseQtyUnit=pricedQtyInBaseQtyUnit*-1;
									pricedBaseQtyUnit=pricedBaseQtyUnit*-1;
								}
								if(gmrList.size()!=0) {
									exposureObj.put("riskType", NORISK);
									exposureObj.put("exposureType", INVENTORY);
									exposureObj.put("exposureSubType", PRICEDSTOCKSDELIVERED);
								}else {
									exposureObj.put("riskType", DELIVERYRISK);
									exposureObj.put("exposureType", OPENSALES);
									exposureObj.put("exposureSubType", PRICEDOPENPOSITIONS);
								}
							}else {
								if(formExp.get(i).contains("-")) {
									unPrice = unPrice*-1;
									unPriceQty=unPriceQty*-1;
									unpricedQtyInBaseQtyUnit=unpricedQtyInBaseQtyUnit*-1;
									unpricedBaseQtyUnit=unpricedBaseQtyUnit*-1;
								}
								if(gmrList.size()!=0) {
									exposureObj.put("riskType", PRICERISK);
									exposureObj.put("exposureType", INVENTORY);
									exposureObj.put("exposureSubType", UNPRICEDSTOCKSDELIVERED);
								}else {
									exposureObj.put("riskType", PRICERISK);
									exposureObj.put("exposureType", OPENSALES);
									exposureObj.put("exposureSubType", UNPRICEDOPENPOSITIONS);
								}
							}
							
						}else if(contractType.equals("P") && formExp.get(i).length()>0) {
							if(price!=0) {
								if(formExp.get(i).contains("+")) {
									price = price*-1;
									priceQty=priceQty*-1;
									pricedQtyInBaseQtyUnit=pricedQtyInBaseQtyUnit*-1;
									pricedBaseQtyUnit=pricedBaseQtyUnit*-1;
								}
								if(gmrList.size()!=0) {
									exposureObj.accumulate("riskType", NORISK);
									exposureObj.accumulate("exposureType", INVENTORY);
									exposureObj.accumulate("exposureSubType", PRICEDSTOCKSRECEIVED);
								}else {
									exposureObj.accumulate("riskType", DELIVERYRISK);
									exposureObj.accumulate("exposureType", OPENPURCHASE);
									exposureObj.accumulate("exposureSubType", PRICEDOPENPOSITIONS);
								}
							}else {
								if(formExp.get(i).contains("+")) {
									unPrice = unPrice*-1;
									unPriceQty=unPriceQty*-1;
									unpricedQtyInBaseQtyUnit=unpricedQtyInBaseQtyUnit*-1;
									unpricedBaseQtyUnit=unpricedBaseQtyUnit*-1;
								}
								if(gmrList.size()!=0) {
									exposureObj.accumulate("riskType", PRICERISK);
									exposureObj.accumulate("exposureType", INVENTORY);
									exposureObj.accumulate("exposureSubType", UNPRICEDSTOCKSRECEIVED);
								}else {
									exposureObj.accumulate("riskType", PRICERISK);
									exposureObj.accumulate("exposureType", OPENPURCHASE);
									exposureObj.accumulate("exposureSubType", UNPRICEDOPENPOSITIONS);
								}
							}
						}
						qtyObj.put("unPricedQty", unPriceQty);
						qtyObj.put("pricedQty", priceQty);
						qtyObj.put("unpricedQuantityInBaseQtyUnit", unpricedQtyInBaseQtyUnit);
						qtyObj.put("pricedQuantityInBaseQtyUnit", pricedQtyInBaseQtyUnit);
						
						exposureObj.put("unPricedQty", unPrice);
						exposureObj.put("pricedQty", price);
						exposureObj.put("unpricedQuantityInBaseQtyUnit", unpricedBaseQtyUnit);
						exposureObj.put("pricedQuantityInBaseQtyUnit", pricedBaseQtyUnit);
						newQtyData.put(qtyObj);
						newerExposureArr.put(exposureObj);
					}
				}
			
			exchangeMap.put(c.getCurveName(), obj.getString("exchange"));
			previewDataSet.add(curveObj);
			
			 baseQtyUnit = curveObj.optString("baseQtyUnit");
			 baseContractualConversionFactor = curveObj.optDouble("baseContractualConversionFactor");
			//apply method for quality curve.
			if(null!=contractType && gmrList.size()>0) {
				if(totalPricedQty!=0 || totalUnpricedQty!=0) {
					previewDataSet= getQualityCurveDetailsForGMR(jobj, i,context.getCurrentContext().getAsOfDate(), tenantProvider,productId,	contractItemQty,
											gmrList, contractQualityDensity,contractQualityMassUnit, contractQualityVolumeUnit,quality,
											totalPricedQty,totalUnpricedQty,previewDataSet,contractType,curveObj,c);
				}
			}
			
			i++;
						
			}
		if(null!=contractType && gmrList.size()==0) {
			previewDataSet= getQualityCurveDetailsForContract(jobj, i,context.getCurrentContext().getAsOfDate(), tenantProvider,productId,	contractItemQty,
				gmrList, contractQualityDensity,contractQualityMassUnit, contractQualityVolumeUnit,quality,previewDataSet,contractType,baseQtyUnit,
				baseContractualConversionFactor);
		}
		return previewDataSet;
	}
	
	public List<JSONObject> getAbsoluteExp(JSONObject jobj, ContextProvider tenantProvider,
			String holidayRule, String productId, String contractItemQty, List<GMR> gmrList, String exp,
			String contractType, Map<String, String> exchangeMap, String quality,double contractQualityDensity,
			String contractQualityMassUnit,String contractQualityVolumeUnit, String quantityUnit,String locationType, Contract contract) throws Exception {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			DateTimeFormatter expiryDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
			Curve c= new Curve();
			String fromDate1 = jobj.optString("deliveryFromDate", "");
			String toDate1 = jobj.optString("deliveryToDate", "");
			List<JSONObject> previewDataSet = new ArrayList<>();
			LocalDateTime fromDate = sdf.parse(fromDate1).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			LocalDateTime toDate = sdf.parse(toDate1).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			LocalDate asOf=context.getCurrentContext().getAsOfDate();
			Map<String, JSONArray> mapExpCallArray = new HashMap<String,JSONArray>();
			double qty = jobj.optDouble("qty");
			if(Double.isNaN(qty) || curveService.checkZero(qty)) {
				qty = jobj.optDouble("itemQty");
				jobj.put("qty",qty);
			}
			String internalContractItemRefNo=jobj.optString("refNo", "");
			if(StringUtils.isEmpty(internalContractItemRefNo)) {
				internalContractItemRefNo=jobj.optString("internalItemRefNo", "");
			}
			String status="";
			String remarks="";
			String qualityName=null;
			String[] qualityArr = mdmFetcher.getQualityExchangeUnit(tenantProvider,quality);
			c.setCurveName(qualityArr[0]);
			c.setPricePoint(qualityArr[2]);
			c.setExchange(qualityArr[3]);
			Map<String , JSONArray> mapExpArr=getExpiryArr(qualityArr[0], tenantProvider,mapExpCallArray);
			JSONArray expArr = mapExpArr.get(qualityArr[0]);
			String valuationPriceUnit=qualityArr[1];
			status=qualityArr[4];
			remarks=qualityArr[5];
			if(!valuationPriceUnit.isEmpty()) {
				if (valuationPriceUnit.contains("/")) {
					String qtyUnit = valuationPriceUnit.substring(valuationPriceUnit.indexOf("/") + 1, valuationPriceUnit.length()).trim();
					String currency = valuationPriceUnit.substring(0, valuationPriceUnit.indexOf("/"));
				c.setCurveQty(qtyUnit);
				c.setCurveCurrency(currency);
				}
			}
			c.setPricedQty(qty);
			c.setPriceQuoteRule("");
			c.setComponent("");
			CurveCalculatorFields fields = new CurveCalculatorFields();
			fields.setSd(fromDate.toLocalDate());
			fields.setEd(toDate.toLocalDate());
		
			 if(gmrList.size()==0 && contractType!=null) {
					gmrList = gmrListFromConnect(contract,contractItemQty,jobj,productId,gmrList);
				}	
			
			Map<String, Double> gmrToItemConversionFactor= new HashMap<String, Double>();
			String itemQtyUnit = mdmFetcher.getQuantityKey(null, contractItemQty, null);
			String CurveQtyUnitID = mdmFetcher.getQuantityKey(null, c.getCurveQty(), null);
			String itemQtyUnitCheck=massToVolumeConversion.getMassVolumeQtyCheck(productId,itemQtyUnit);
			double totalGmrQty=0.0;
			double contractualConversionFactor=0.0;
			double baseContractualConversionFactor=0.0;
			Map<String, Double> baseQtyConversionFactor =new HashMap<String, Double>();
			String[] baseQtyUnitId = mdmFetcher.getBaseQtyUnit(tenantProvider, productId);
			status=baseQtyUnitId[1];
			remarks=baseQtyUnitId[2];
			String baseQtyUnit = mdmFetcher.getContractQty(null, baseQtyUnitId[0], null);
			if(gmrList.size()>0) {
					GMR gmr=gmrList.get(0);
						for (Stock st : gmr.getStocks()) {
							totalGmrQty=totalGmrQty + st.getQty();
						
					}
						gmrToItemConversionFactor=curveFetcher.gmrDensityVolumeConversionGMRQty(gmrList,jobj,productId,tenantProvider,
								 itemQtyUnit,  contractQualityDensity, contractQualityMassUnit,
								 contractQualityVolumeUnit, totalGmrQty,  itemQtyUnitCheck); 
						
						baseQtyConversionFactor=curveFetcher.gmrDensityVolumeConversion(gmrList,jobj,productId,tenantProvider,itemQtyUnit,baseQtyUnitId[0],contractQualityDensity, 
								contractQualityMassUnit, contractQualityVolumeUnit,totalGmrQty,contractItemQty,c.getCurveQty(),1, false,gmrToItemConversionFactor,itemQtyUnitCheck);
				
				}else {
					if(contractQualityDensity!=0.0) {
						contractualConversionFactor=curveFetcher.contractualConversionFactor(productId,itemQtyUnit,CurveQtyUnitID, contractQualityDensity, contractQualityMassUnit,
								 contractQualityVolumeUnit, tenantProvider);
						
						baseContractualConversionFactor=curveFetcher.contractualConversionFactor(productId,itemQtyUnit,baseQtyUnitId[0], contractQualityDensity, contractQualityMassUnit,
								 contractQualityVolumeUnit, tenantProvider);
						
				}else {
					contractualConversionFactor = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productId, itemQtyUnit,
						CurveQtyUnitID);
					
					baseContractualConversionFactor = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productId, itemQtyUnit,
							baseQtyUnitId[0]);
				  }
				}
			
			JSONArray newerExposureArr = new JSONArray();
			Map<LocalDate, Integer> dateCountMap = new LinkedHashMap<LocalDate, Integer>();
			HolidayRuleDates holidayRuleDates = curveFetcher.getListOfDaysPostHolidayRule(fields, c.getExchange(), tenantProvider,
					holidayRule, c);
			List<LocalDateTime> validDates = holidayRuleDates.getDateToBeUsed();
			c.setPricedDays(validDates.size());
			for (LocalDateTime date : validDates) {
				if (dateCountMap.containsKey(date.toLocalDate())) {
					dateCountMap.put(date.toLocalDate(), dateCountMap.get(date.toLocalDate()) + 1);
				} else {
					dateCountMap.put(date.toLocalDate(), 1);
				}
			}
			JSONArray exposureArray = new JSONArray();
			Map<String, GMRQualityDetails> gmrQuality = new HashMap<String,GMRQualityDetails>();
			double actualConversionFactorGMR=0.0;
			String curveQtyUnit="";
			String currency="";
		if(gmrList.size()>0) {
			for (GMR gmr1 : gmrList) {
				  for (Stock st : gmr1.getStocks()) {
				   if (st.getGMRRefNo().equals(gmr1.getRefNo())) {
					  qualityName=st.getQuality();
					  if(null!=qualityName) {
							quality=mdmFetcher.getQualityUnitId(productId, qualityName);
						}
					}
					
				  }
					 qualityArr = mdmFetcher.getQualityExchangeUnit(tenantProvider,quality);
					 valuationPriceUnit=qualityArr[1];
					if(!valuationPriceUnit.isEmpty()) {
						if (valuationPriceUnit.contains("/")) {
							curveQtyUnit = valuationPriceUnit.substring(valuationPriceUnit.indexOf("/") + 1, valuationPriceUnit.length()).trim();
							currency = valuationPriceUnit.substring(0, valuationPriceUnit.indexOf("/"));
						}
					}
					CurveQtyUnitID = mdmFetcher.getQuantityKey(null, curveQtyUnit, null);
					String CurveQtyUnitCheck=massToVolumeConversion.getMassVolumeQtyCheck(productId,CurveQtyUnitID);
					actualConversionFactorGMR=curveFetcher.gmrActualDensityVolumeConversion(gmr1,jobj,productId,tenantProvider,itemQtyUnit,CurveQtyUnitID,
							contractQualityDensity, contractQualityMassUnit, contractQualityVolumeUnit,totalGmrQty,contractItemQty,curveQtyUnit,
							1,true,gmrToItemConversionFactor,gmr1.getRefNo(),itemQtyUnitCheck,CurveQtyUnitCheck);
					GMRQualityDetails gmrQualityDetails = new GMRQualityDetails();
					gmrQualityDetails.setActualConversionFactorGMR(actualConversionFactorGMR);
					gmrQualityDetails.setCurveName(qualityArr[0]);
					gmrQualityDetails.setCurveQty(curveQtyUnit);
					gmrQualityDetails.setQuality(qualityName);
					gmrQualityDetails.setCurveQtyUnitId(CurveQtyUnitID);
					gmrQualityDetails.setCurveQtyUnitCheck(CurveQtyUnitCheck);
					mapExpArr=getExpiryArr(qualityArr[0], tenantProvider,mapExpCallArray);
					
					gmrQuality.put(gmr1.getRefNo(), gmrQualityDetails);
			   }
					gmrCreationHelper.updateGMRWeightedAvgConversionFactor(tenantProvider,internalContractItemRefNo,
							itemQtyUnit, contractItemQty,gmrList,1,itemQtyUnitCheck,gmrToItemConversionFactor,gmrQuality);
					
					 for (GMR gmr : gmrList) {
						for (Stock st : gmr.getStocks()) {
							if (st.getGMRRefNo().equals(gmr.getRefNo())) {
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
								
						    JSONObject exposureObj = new JSONObject();
						    LocalDate gmrCreationDate = asOf;
							if(!StringUtils.isEmpty(st.getGmrCreationDate())) {
							gmrCreationDate = sdf.parse(st.getGmrCreationDate()).toInstant()
										.atZone(ZoneId.systemDefault()).toLocalDate();
							}
							exposureObj.put("date", gmrCreationDate);
							GMRQualityDetails gmrQualityDetails=gmrQuality.get(st.getGMRRefNo());
							if(null!= contractType && contractType.equals("P")) {
								exposureObj.put("pricedQty", st.getStockQtyInGmr()*gmrToItemConversionFactor.get(st.getGMRRefNo()));
								exposureObj.accumulate("pricedQuantityInBaseQtyUnit", st.getStockQtyInGmr()*baseQtyConversionFactor.get(st.getGMRRefNo())
										*gmrToItemConversionFactor.get(st.getGMRRefNo()));
								exposureObj.put("riskType", NORISK);
								exposureObj.put("exposureType", INVENTORY);
								exposureObj.put("exposureSubType", PRICEDSTOCKSRECEIVED);
								exposureObj.put("contractualConversionFactor", 0);
								exposureObj.put("actualConversionFactor", gmrQualityDetails.getActualConversionFactorGMR());
								exposureObj.put("gmrRefNo", st.getGMRRefNo());
								exposureObj.put("quality", gmrQualityDetails.getQuality());
								exposureObj.put("curveName", gmrQualityDetails.getCurveName());
								exposureObj.put("curveQtyUnit", gmrQualityDetails.getCurveQty());
								exposureObj.put("titleTransferStatus", gmr.getTitleTransferStatus());
								exposureObj.put("locationName", locationName);
							}else {
								exposureObj.put("pricedQty", st.getStockQtyInGmr()*gmrToItemConversionFactor.get(st.getGMRRefNo())*-1);
								exposureObj.accumulate("pricedQuantityInBaseQtyUnit", st.getStockQtyInGmr()*baseQtyConversionFactor.get(st.getGMRRefNo())*
										gmrToItemConversionFactor.get(st.getGMRRefNo())*-1);
								exposureObj.put("riskType", NORISK);
								exposureObj.put("exposureType", INVENTORY);
								exposureObj.put("exposureSubType", PRICEDSTOCKSDELIVERED);
								exposureObj.put("contractualConversionFactor", 0);
								exposureObj.put("actualConversionFactor", gmrQualityDetails.getActualConversionFactorGMR());
								exposureObj.put("gmrRefNo", st.getGMRRefNo());
								exposureObj.put("quality", gmrQualityDetails.getQuality());
								exposureObj.put("curveName", gmrQualityDetails.getCurveName());
								exposureObj.put("curveQtyUnit", gmrQualityDetails.getCurveQty());
								exposureObj.put("titleTransferStatus", gmr.getTitleTransferStatus());
								exposureObj.put("locationName", locationName);
							}
							exposureObj.put("unPricedQty", 0);
							exposureObj.put("unpricedQuantityInBaseQtyUnit", 0);
							
							JSONArray expArray = mapExpArr.get(gmrQualityDetails.getCurveName());
							String monthYear=getMonthYearForDate(gmrCreationDate, expArray);
							exposureObj.put("instrumentDeliveryMonth", monthYear);
							exposureObj.put("pricedPercentage", (st.getStockQtyInGmr()*100)/qty);
							exposureObj.put("unpricedPercentage", 0);
							exposureArray.put(exposureObj);
						}	
					}
	             }
					
				}else {
					// For Contract Creation
						JSONObject exposureObj = new JSONObject();
						exposureObj.put("date", asOf);
						if(null!= contractType && contractType.equals("P")) {
							exposureObj.put("pricedQty", qty);
							exposureObj.put("pricedQuantityInBaseQtyUnit", qty*baseContractualConversionFactor);
							exposureObj.put("riskType", DELIVERYRISK);
							exposureObj.put("exposureType", OPENPURCHASE);
							exposureObj.put("exposureSubType", PRICEDOPENPOSITIONS);
							exposureObj.put("contractualConversionFactor", contractualConversionFactor);
							exposureObj.put("actualConversionFactor", 0);
						}else {
							exposureObj.put("pricedQty", qty*-1);
							exposureObj.put("pricedQuantityInBaseQtyUnit", qty*baseContractualConversionFactor*-1);
							exposureObj.put("riskType", DELIVERYRISK);
							exposureObj.put("exposureType", OPENSALES);
							exposureObj.put("exposureSubType", PRICEDOPENPOSITIONS);
							exposureObj.put("contractualConversionFactor", contractualConversionFactor);
							exposureObj.put("actualConversionFactor", 0);
						}
						exposureObj.put("unPricedQty", 0);
						exposureObj.put("unpricedQuantityInBaseQtyUnit", 0);
						
						String monthYear=getMonthYearForDate(asOf, expArr);
						exposureObj.put("instrumentDeliveryMonth", monthYear);
						exposureObj.accumulate("pricedPercentage", 100);
						exposureObj.accumulate("unpricedPercentage", 0);
						exposureArray.put(exposureObj);
					
					}
					
			for (int expIndex = 0; expIndex < exposureArray.length(); expIndex++) {
				JSONObject exposureObject = new JSONObject(exposureArray.getJSONObject(expIndex),
						JSONObject.getNames(exposureArray.getJSONObject(expIndex)));
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
			PreviewData previewData = new PreviewData();
			previewData.setCollapse("0");
			previewData.setExchange(c.getExchange());
			previewData.setCurveName(c.getCurveName());
			previewData.setCurvePrice(c.getCurvePrice());
			previewData.setCurveCurrency(c.getCurveCurrency());
			previewData.setCoefficient(1);
			previewData.setCurveQtyUnit(c.getCurveQty());
			previewData.setQtyUnit(contractItemQty);
			previewData.setBaseQtyUnit(baseQtyUnit);
			previewData.setPriceUnit(c.getCurveCurrency() + "/" + c.getCurveQty());
			previewData.setQpStartDate(collectionDataFetcher.constructDateStr(fromDate.toLocalDate()));
			previewData.setQpEndDate(collectionDataFetcher.constructDateStr(toDate.toLocalDate()));
			previewData.setData(new JSONArray());
			previewData.setPricedQty(c.getPricedQty());
			previewData.setUnPricedQty(c.getUnpricedQty());
			previewData.setStatus(status);
			previewData.setRemarks(remarks);
			previewData.setValuationInstrument("Physical");
			if(!StringUtils.isEmpty(c.getComponent())) {
				previewData.setComponentName(c.getComponent());
			}
			previewData.setQtyData(exposureArray);
			previewData.setExposureArray(newerExposureArr);
			
			previewDataSet.add(new JSONObject(previewData));
			return previewDataSet;
		
	}
	
	
	public List<JSONObject> getAbsoluteExpForTriggerPrice(JSONObject jobj, CurveDetails curveDetails, ContextProvider tenantProvider,
			String holidayRule, String productId, String contractItemQty, List<GMR> gmrList, String exp,
			String contractType, Map<String, String> exchangeMap, String quality,double contractQualityDensity,
			String contractQualityMassUnit,String contractQualityVolumeUnit,String quantityUnit) throws Exception {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			String fromDate1 = jobj.optString("deliveryFromDate", "");
			String toDate1 = jobj.optString("deliveryToDate", "");
			List<JSONObject> previewDataSet = new ArrayList<>();
			LocalDateTime fromDate = sdf.parse(fromDate1).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			LocalDateTime toDate = sdf.parse(toDate1).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			
		String itemQtyUnit = mdmFetcher.getQuantityKey(null, contractItemQty, null);
		String CurveQtyUnitID = mdmFetcher.getQuantityKey(null, curveDetails.getCurveList().get(0).getCurveQty(), null);
		double qtyConversionFactor=0.0;
		if(contractQualityDensity!=0.0) {
			JSONObject payloadJson = new JSONObject();
			payloadJson.accumulate("productId", productId);
			payloadJson.accumulate("sourceUnitId", itemQtyUnit);
			payloadJson.accumulate("destinationUnitId", CurveQtyUnitID);
			payloadJson.accumulate("massUnitId", contractQualityMassUnit);
			payloadJson.accumulate("volumeUnitId", contractQualityVolumeUnit);
			payloadJson.accumulate("densityValue", contractQualityDensity);
			Map<String, Object> getConversionRate= massToVolumeConversion.getConversionRate(tenantProvider,payloadJson);
			String ConversionFactor =  getConversionRate.get("conversionFactor").toString();
			qtyConversionFactor= Double.parseDouble(ConversionFactor);
			if(qtyConversionFactor==0.0) {
				qtyConversionFactor = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productId, itemQtyUnit,
						CurveQtyUnitID);
			}
		}else {
		 qtyConversionFactor = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productId, itemQtyUnit,
				CurveQtyUnitID);
		}
		String[] baseQtyUnitId = mdmFetcher.getBaseQtyUnit(tenantProvider, productId);
		String baseQtyUnit = mdmFetcher.getContractQty(null, baseQtyUnitId[0], null);
		  JSONArray exposureArray = new JSONArray();
		  JSONArray newerExposureArr = new JSONArray();
			if (null != curveDetails.getCurveList().get(0).getTriggerPriceExposure()) {
				exposureArray = curveDetails.getCurveList().get(0).getTriggerPriceExposure();
			}
			for (int expIndex = 0; expIndex < exposureArray.length(); expIndex++) {
				JSONObject exposureObject = new JSONObject(exposureArray.getJSONObject(expIndex),
						JSONObject.getNames(exposureArray.getJSONObject(expIndex)));
				
				exposureObject.put("pricedQty", exposureObject.optDouble("pricedQty", 0) * qtyConversionFactor);
				exposureObject.put("unPricedQty", exposureObject.optDouble("unPricedQty", 0) * qtyConversionFactor);
				
				newerExposureArr.put(exposureObject);
			}
			PreviewData previewData = new PreviewData();
			previewData.setCollapse("0");
			previewData.setExchange(curveDetails.getCurveList().get(0).getExchange());
			previewData.setCurveName(curveDetails.getCurveList().get(0).getCurveName());
			previewData.setCurvePrice(curveDetails.getCurveList().get(0).getCurvePrice());
			previewData.setCurveCurrency(curveDetails.getCurveList().get(0).getCurveCurrency());
			previewData.setCoefficient(curveDetails.getCurveList().get(0).getCoefficient());
			previewData.setCurveQtyUnit(curveDetails.getCurveList().get(0).getCurveQty());
			previewData.setQtyUnit(contractItemQty);
			previewData.setBaseQtyUnit(baseQtyUnit);
			previewData.setPriceUnit(curveDetails.getCurveList().get(0).getCurveCurrency() + "/" + curveDetails.getCurveList().get(0).getCurveQty());
			previewData.setQpStartDate(collectionDataFetcher.constructDateStr(fromDate.toLocalDate()));
			previewData.setQpEndDate(collectionDataFetcher.constructDateStr(toDate.toLocalDate()));
			previewData.setData(new JSONArray());
			previewData.setPricedQty(curveDetails.getCurveList().get(0).getPricedQty());
			previewData.setUnPricedQty(curveDetails.getCurveList().get(0).getUnpricedQty());
			previewData.setStatus(baseQtyUnitId[1]);
			previewData.setRemarks(baseQtyUnitId[2]);
			previewData.setQtyConversionUnit(qtyConversionFactor);
			previewData.setValuationInstrument("Physical");
			if(!StringUtils.isEmpty(curveDetails.getCurveList().get(0).getComponent())) {
				previewData.setComponentName(curveDetails.getCurveList().get(0).getComponent());
			}
			previewData.setQtyData(exposureArray);
			previewData.setExposureArray(newerExposureArr);
			
			previewDataSet.add(new JSONObject(previewData));
			return previewDataSet;
		
	}

	/*public List<JSONObject> getPreviewDataSet(List<Curve> curveDetails, JSONObject jobj,
			ContextProvider tenantProvider, String holidayRule, String productId, String contractItemQty, List<GMR> gmrList, String exp) throws Exception {

		List<JSONObject> previewDataSet = new ArrayList<>();
		int i = 0;
		for (Curve c : curveDetails) {
			JSONObject curveObj = curveFetcher.getPreviewDataSet(c, jobj, i, lookbackDate, asOfDate,
					tenantProvider, holidayRule, productId, contractItemQty, gmrList, exp);
			JSONObject obj = curveObj;
			exchangeMap.put(c.getCurveName(), obj.getString("exchange"));
			previewDataSet.add(curveObj);
			i++;
		}
		return previewDataSet;
	}*/

	public double[] calculatePricedQuantity(List<Curve> curveDetails, JSONObject itemObj,
			ContextProvider tenantProvider, Map<String, String> exchangeMap) throws JSONException, ParseException, PricingException {
//		System.out.println("check 1");
		double[] resArr = new double[2];
		double priced = 0.0f;
		double qty = itemObj.optDouble("qty");
		if(Double.isNaN(qty) || curveService.checkZero(qty)) {
			qty = itemObj.optDouble("itemQty");
			itemObj.put("qty",qty);
		}
		double unpriced = qty;
		int daysToConsider = 0;
		int dateDiff = 0;
		LocalDateTime fromDate = curveFetcher.convertISOtoLocalDate(itemObj.optString("deliveryFromDate", ""))
				.atStartOfDay();
		LocalDateTime toDate = curveFetcher.convertISOtoLocalDate(itemObj.optString("deliveryToDate", ""))
				.atStartOfDay();
		LocalDateTime current = context.getCurrentContext().getAsOfDate().atStartOfDay();
		LocalDateTime checkerDate = fromDate;
		List<JSONArray> holidayObjList = new ArrayList<JSONArray>();
		int curveCount = 0;
		int tempDateDiff = 0;
		for (Curve c : curveDetails) {
			checkerDate = fromDate;
			List<LocalDateTime> dateList = new ArrayList<LocalDateTime>();
			tempDateDiff = 0;
			while (checkerDate.isBefore(toDate) || checkerDate.isEqual(toDate)) {
				dateList.add(checkerDate);
				tempDateDiff++;
				checkerDate = checkerDate.plusDays(1);
			}
			if (dateDiff < tempDateDiff) {
				dateDiff = tempDateDiff;
			}
			String exchange = exchangeMap.get(c.getCurveName().trim());
			HolidayRuleDetails holidayRuleDetail = new HolidayRuleDetails();
			holidayRuleDetail.setExchangeName(exchange);
			holidayRuleDetail.setDateRange(dateList);
			holidayRuleDetail.setHolidayRule("Prior Business Day");
			JSONArray holidayObj = new JSONArray(curveService.applyHolidayRule(holidayRuleDetail, tenantProvider));
			holidayObjList.add(holidayObj);
			curveCount++;
		}
		int i = 0;
		while (i < dateDiff) {
			boolean flag = false;
//			i=0;
			for (int j = 0; j < curveCount; j++) {
				JSONArray holidayMapperObj = holidayObjList.get(j);
				JSONObject holObj = holidayMapperObj.getJSONObject(i);
				if (!holObj.getString("date").equals(holObj.getString("dateToBeUsed"))) {
					flag = true;
				} else {
					flag = false;
				}
			}
			if (flag == true) {
				daysToConsider++;
			}
			i++;
		}
		checkerDate = fromDate;
		i = 0;
		while (i < dateDiff) {
			boolean flag = false;
			if (checkerDate.isEqual(current) || checkerDate.isAfter(current)) {
				break;
			}
			for (int j = 0; j < curveCount; j++) {
				JSONArray holidayMapperObj = holidayObjList.get(j);
				JSONObject holObj = holidayMapperObj.getJSONObject(i);
				if (!holObj.getString("date").equals(holObj.getString("dateToBeUsed"))) {
					flag = true;
				} else {
					flag = false;
				}
			}
			if (flag == true) {
				priced = priced + (unpriced / daysToConsider);
			}
			checkerDate = checkerDate.plusDays(1);
			i++;
		}
		unpriced = unpriced - priced;
		resArr[0] = priced;
		resArr[1] = unpriced;
		return resArr;
	}

	public double[] calculatePricedQuantityForGMR(JSONArray stArr, List<Curve> curveDetails, LocalDate asOfDate,
			ContextProvider tenantProvider, CurveDetails cDetails, JSONObject itemObj, GMR gmr,
			Map<String, String> exchangeMap, boolean isFlat) throws Exception {
		double pricedDays = 0;
		double totalDays = 0;
		double[] resArr = new double[2];
		double itemQty = itemObj.optDouble("qty");
		double triggerQty = cDetails.getTriggerQty();
		DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
		LocalDateTime fromDate = null;
		LocalDateTime toDate = null;
		if (isFlat || (null != curveDetails && curveDetails.size() == 0)) {
			fromDate = curveFetcher.convertISOtoLocalDate(itemObj.optString("deliveryFromDate")).atStartOfDay();
			toDate = curveFetcher.convertISOtoLocalDate(itemObj.optString("deliveryToDate")).atStartOfDay();
		}
		else {
			fromDate = curveDetails.stream().map(c -> c.getGmrQPDetailsList().stream().filter(g -> g.getGmr().getRefNo().equalsIgnoreCase(gmr.getRefNo())).findFirst().get().getQpFromDate()).min(LocalDate::compareTo).get().atStartOfDay();
			toDate = curveDetails.stream().map(c -> c.getGmrQPDetailsList().stream().filter(g -> g.getGmr().getRefNo().equalsIgnoreCase(gmr.getRefNo())).findFirst().get().getQpToDate()).max(LocalDate::compareTo).get().plusDays(1)
					.atStartOfDay();
		}
		for (Curve c : curveDetails) {
			List<LocalDateTime> dateList = new ArrayList<LocalDateTime>();
			dateList = Stream.iterate(fromDate, date -> date.plusDays(1))
					.limit(ChronoUnit.DAYS.between(fromDate, toDate)).collect(Collectors.toList());
			String exchange = exchangeMap.get(c.getCurveName().trim());
			HolidayRuleDetails holidayRuleDetail = new HolidayRuleDetails();
			holidayRuleDetail.setExchangeName(exchange);
			holidayRuleDetail.setDateRange(dateList);
			holidayRuleDetail.setHolidayRule("Prior Business Day");
			JSONArray holidayObj = new JSONArray(curveService.applyHolidayRule(holidayRuleDetail, tenantProvider));
			Set<LocalDateTime> workingDaysSet = new HashSet<LocalDateTime>();
			for (int i = 0; i < holidayObj.length(); i++) {
				JSONObject dayObj = holidayObj.optJSONObject(i);
				if(dayObj.optString("dateToBeUsed").equals("NA")) {
					continue;
				}
				LocalDateTime day = LocalDateTime.parse(dayObj.optString("dateToBeUsed"), formatter);
				LocalDateTime originalDay = LocalDateTime.parse(dayObj.optString("date"), formatter);
				if (day.isAfter(fromDate) || day.isEqual(fromDate) || originalDay.isAfter(fromDate)
						|| originalDay.isEqual(fromDate)) {
					workingDaysSet.add(day);
				}
			}
			for (LocalDateTime day : workingDaysSet) {
				if (!day.isAfter(asOfDate.atStartOfDay())) {
					pricedDays++;
				}
			}
			totalDays = totalDays + workingDaysSet.size();
		}
		double qty = 0;
		for (int i = 0; i < stArr.length(); i++) {
			JSONObject stckObject = stArr.optJSONObject(i);
			double stockQty = stckObject.optDouble("qty");
			qty = qty + stockQty;
		}
		if(null != curveDetails && curveDetails.size() == 0) {
			resArr[0] = qty;
			resArr[1] = 0;
			return resArr;
		}
		double pricedQty = 0;
		double unPricedQty = 0;
		if(curveService.checkZero(totalDays)) {
			resArr[0] = 0;
			resArr[1] = qty;
		}
		else if(itemQty == triggerQty) {
			resArr[0] = qty;
			resArr[1] = 0;
		}
		else {
			pricedQty = (pricedDays / totalDays) * qty;
			unPricedQty = qty - pricedQty;
			resArr[0] = pricedQty;
			resArr[1] = unPricedQty;
		}
		
		return resArr;
	}

/*	public JSONObject createFinalResponse(CurveDetails curveDetails, ContextProvider tenantProvider) throws Exception {
//		String precision;
		List<String> productIdList = new ArrayList<String>();
		List<String> currencyList = new ArrayList<String>();
		List<String> qtyUnitList = new ArrayList<String>();
		resultDataSet.clear();
		JSONObject contractObj = new JSONObject();
		curveDetails.setOriginalExp(curveDetails.getExpression());
		List<JSONObject> itemList = new ArrayList<JSONObject>();
		if (curveDetails.getContractID() != null) {
			itemList = periodFetcher.fetchContractItems(tenantProvider, curveDetails.getContractID());
		}
		
		List<CurveDetails> detailsList = new ArrayList<>();
		if (itemList.size() == 0) {
			List<String> curveList = new ArrayList<String>();
			List<Curve> cList = curveDetails.getCurveList();
			for (Curve c : cList) {
				curveList.add(c.getCurveName());
			}
			String exp = expressionBuilder.buildExpression(curveList, curveDetails.getExpression());
			curveDetails.setExpression(exp);
			detailsList.add(curveDetails);
		} else if (curveDetails.isExecuteByContract()) {
			detailsList = formulaData.populateCurveData(itemList, tenantProvider);
		} else if (isDefaultPreview(curveDetails)) {
			detailsList = formulaData.populateDefaultCurveData(curveDetails);
		} else {
			List<String> curveList = new ArrayList<String>();
			List<Curve> cList = curveDetails.getCurveList();
			for (Curve c : cList) {
				curveList.add(c.getCurveName());
			}
			String exp = expressionBuilder.buildExpression(curveList, curveDetails.getExpression());
			int i = 0;
			while (i < itemList.size()) {

				curveDetails.setExpression(exp);
				detailsList.add(curveDetails);
				i++;
			}
		}
		for (JSONObject itemObj : itemList) {
			itemObj.put("pricePrecision", curveDetails.getPricePrecision());
			JSONObject pricingObj = itemObj.getJSONObject("pricing");
			String currency = null;
			String qtyUnit = null;
			try {
				currency = pricingObj.getString("priceUnit");
//				System.out.println("currency : "+currency);
			} catch (Exception e) {
				currency = "USD";
			}
			if (null == currency || currency.isEmpty()) {
				currency = "USD";
			}
			if (currency.contains("/")) {
				qtyUnit = currency.substring(currency.indexOf("/") + 1, currency.length()).trim();
				currency = currency.substring(0, currency.indexOf("/"));
			}
			currencyList.add(currency);
			qtyUnitList.add(qtyUnit);
		}
		contractObj.put("itemDetails",
				processExpression(detailsList, itemList, tenantProvider, currencyList, qtyUnitList, productIdList, "", null, "", null));
		resultDataSet = processExpression(detailsList, itemList, tenantProvider, currencyList, qtyUnitList,
				productIdList, "", null, "", null);
		JSONObject finalObject = new JSONObject();
		JSONObject inputObj = new JSONObject();
		inputObj.put("contractID", curveDetails.getContractID());
		inputObj.put("exp", curveDetails.getExpression());
		inputObj.put("curves", curveDetails.getCurveList());
		finalObject.put("input", inputObj);
		finalObject.put("priceDetails", resultDataSet);

		return finalObject;
	}

	private boolean isDefaultPreview(CurveDetails curveDetails) {
		for (Curve c : curveDetails.getCurveList()) {
			if (null == c.getPricePoint() || c.getPricePoint().isEmpty()) {
				return true;
			}
		}
		return false;
	}
*/
	public double calculateContractQuantity(List<JSONObject> itemList) {
		double qtySum = 0;
		if (itemList.size() == 0) {
			return qtySum;
		}
		for (JSONObject itemObj : itemList) {
			qtySum = qtySum + itemObj.getDouble("itemQty");
		}
		return qtySum;
	}

	public JSONObject putValue(JSONObject jObj, String key, String val) {
		if (null == val || val.isEmpty()) {
			jObj.put(key, "");
		} else {
			jObj.put(key, val);
		}
		return jObj;
	}

	public JSONObject createFinalResponseForPayload(Contract contract, List<Formula> formulaList,
			ContextProvider tenantProvider, String mode) throws Exception {
		boolean isPricePreview = false;
		String origin = context.getCurrentContext().getOrigin();
		if (null != formulaList && formulaList.size() > 0) {
			isPricePreview = true;
		}
		checkForGMROperation(contract);
		List<String> internalPriceUnitIdList = new ArrayList<String>();
		String modeLocal=validator.cleanData(mode);
		List<String> currencyList = new ArrayList<String>();
		List<String> qtyUnitList = new ArrayList<String>();
		List<String> productIdList = new ArrayList<String>();
		List<PricingComponent> compList = new ArrayList<PricingComponent>();
		List<TriggerPrice> triggerPricing = new ArrayList<TriggerPrice>();
		String internalContractItemRefNo = "";
		boolean isTieredRequest = false;
		String itemRefNo = "";
//		productIdList.clear();
		boolean isTRMRequest = false;
		if(StringUtils.isEmpty(formulaList)) {
			isTRMRequest = true;
		}
		List<TieredPricingItem> tieredPricingItemList = new ArrayList<TieredPricingItem>();
		JSONObject inputObj = new JSONObject();
		JSONObject contractObj = new JSONObject();
		List<Formula> fList = new ArrayList<Formula>();
		Map<String, Formula> formulaMap = new HashMap<String, Formula>();
		String contractQtyUnit = "";
		String priceType = "";
		Map<String, List<String>> formulaCurveListMap = new HashMap<String, List<String>>();
		if (null == contract.getAsOfDate() || contract.getAsOfDate().isEmpty()) {
			throw new PricingException(
					messageFetcher.fetchErrorMessage(tenantProvider, "004", new ArrayList<String>()));
		}
		if (null != contract && !contract.getItemDetails().isEmpty()) {
			for (ContractItem ci : contract.getItemDetails()) {
				String internalPriceUnitId = null;
				String priceUnitId = null;
				if (null != ci.getPricing()) {
					internalPriceUnitId = ci.getPricing().getInternalPriceUnitId();
					priceUnitId = ci.getPricing().getPriceUnitId();
				}
				if (!StringUtils.isEmpty(internalPriceUnitId)) {
					internalPriceUnitIdList.add(internalPriceUnitId);
				} else if (!StringUtils.isEmpty(priceUnitId)) {
					internalPriceUnitIdList.add(priceUnitId);
				}else {
					logger.error(Logger.EVENT_FAILURE, ESAPI.encoder()
							.encodeForHTML("empty internal price unit ID"));
					throw new PricingException("internalPriceUnitId not available");
				}
				if (ci.getProductId() != null) {
					productIdList.add(ci.getProductId());
				}
				
			}
		}
		LocalDate date	 = curveFetcher.convertISOtoLocalDate(contract.getAsOfDate());
		context.getCurrentContext().setAsOfDate(date);
		if (null != contract.getRefNo() && !contract.getRefNo().isEmpty() && null != contract) {
			inputObj = putValue(inputObj, "contractId", validator.cleanData(contract.getId()));
			contractObj = putValue(contractObj, "refNo", validator.cleanData(contract.getRefNo()));
			contractObj = putValue(contractObj, "asOfDate", validator.cleanData(context.getCurrentContext().getAsOfDate().toString()));
			JSONArray itemArray = itemsFetcher.getItemForContract(tenantProvider, contract.getRefNo(), contract);
			if (!StringUtils.isEmpty(contract.getItemDetails().get(0).getPricing().getPricingFormulaId())
					&& (null == formulaList || formulaList.size() == 0)) {
				String pricingFormulaID = contract.getItemDetails().get(0).getPricing().getPricingFormulaId();
				JSONObject formulaObj = formFetcher.getFormula(tenantProvider, pricingFormulaID);
				if (null == formulaObj) {
					throw new PricingException(
							messageFetcher.fetchErrorMessage(tenantProvider, "009", new ArrayList<String>()));
				}
				if (null == formulaList) {
					formulaList = new ArrayList<Formula>();
				}
				Formula f = jcon.convertJsonToFormula(formulaObj);
				if (null == formulaList) {
					formulaList = new ArrayList<Formula>();
				}
				formulaList.add(f);
				compList = compFetcher.fetchComponentForContractCreation(contract.getRefNo(),
						contract.getItemDetails().get(0).getItemNo());
			}
			tieredPricingItemList = tieredPricingObjectFetcher.fetchTieredPricingObjectsForContractCreation(
					contract.getRefNo(), contract.getItemDetails().get(0).getItemNo());
			if (null != tieredPricingItemList && !tieredPricingItemList.isEmpty() && !isPricePreview) {
				formulaList = new ArrayList<Formula>();
				isTieredRequest = true;
			}
			if ((null == formulaList || formulaList.size() == 0) && null != itemArray) {
				List<String> internalItemRefList = new ArrayList<String>();
				if (null == itemArray || itemArray.length() == 0) {
					throw new PricingException(
							messageFetcher.fetchErrorMessage(tenantProvider, "005", new ArrayList<String>()));
				}
				for (ContractItem ci : contract.getContractItemList()) {
					internalItemRefList.add(ci.getRefNo());
				}
				if(null!=itemArray) {
					itemsFetcher.getItemForContractDensity(itemArray, contract.getItemDetails().get(0).getRefNo(),contract);
				}
//				fetching items with internalContractRefNo
				for (int k = 0; k < itemArray.length(); k++) {
					JSONObject contractItem = itemArray.optJSONObject(k);
					String internalRefId = contractItem.optString("internalItemRefNo", "");
					if (!internalItemRefList.contains(internalRefId)) {
						continue;
					}
					contract.getItemDetails().get(0).getPricing()
							.setPricingStrategy(contractItem.optJSONObject("pricing").optString("pricingStrategy"));
					contract.setQuality(contractItem.optString("quality"));
					triggerPricing = triggerPriceFetcher.fetchTriggerPriceDetails(tenantProvider, internalRefId);
					itemRefNo = contract.getContractRefNo()+"."+(++k);
					internalContractItemRefNo = internalRefId;
					priceType = contractItem.optJSONObject("pricing").optString("priceTypeId");
					if (contractItem.optString("productId", "").length() == 0) {
						throw new PricingException(
								messageFetcher.fetchErrorMessage(tenantProvider, "006", new ArrayList<String>()));
					}
					if (contractItem.optString("itemQtyUnitId", "").length() == 0) {
						throw new PricingException(
								messageFetcher.fetchErrorMessage(tenantProvider, "007", new ArrayList<String>()));
					}
					String contractQtyUnitID = contractItem.optString("itemQtyUnitId", "");
					mdmFetcher.populateMDMData(contractItem.optString("productId", ""));
					contractQtyUnit = mdmFetcher.getContractQty(tenantProvider, contractQtyUnitID,
							contractItem.optString("productId", ""));
					if (contractQtyUnit.length() == 0) {
						throw new PricingException(
								messageFetcher.fetchErrorMessage(tenantProvider, "008", new ArrayList<String>()));
					}
					if (productIdList.size() == 0) {
						productIdList.add(contractItem.optString("productId", ""));
						contract.setProduct(
								mdmFetcher.getProductValue(tenantProvider, contractItem.optString("productId")));
					}
					JSONObject pricingObject = contractItem.optJSONObject("pricing");
					String pricingFormulaID = pricingObject.optString("pricingFormulaId", "");
					if(null==pricingFormulaID || pricingFormulaID.length()==0) {
						continue;
					}
					JSONObject formulaObj = formFetcher.getFormula(tenantProvider, pricingFormulaID);
					if (null == formulaObj) {
						throw new PricingException(
								messageFetcher.fetchErrorMessage(tenantProvider, "009", new ArrayList<String>()));
					}
					Formula f = jcon.convertJsonToFormula(formulaObj);
					fList.add(f);
				}
				formulaList = fList;
				if (!StringUtils.isEmpty(origin) && origin.toLowerCase().contains("reevaluation")) {
					List<String> activeGMRList = gmrDetailsFetcher.fetchActiveGMRFromTRM(internalContractItemRefNo);
					if (null != activeGMRList && activeGMRList.size() > 0) {
						List<GMR> gmrDetails = gmrDetailsFetcher.getGMRListFromConnect(internalContractItemRefNo,
								activeGMRList);
						if (null != gmrDetails && gmrDetails.size() > 0
								&& contract.getItemDetails().get(0).getGmrDetails().size() == 0) {
							contract.getItemDetails().get(0).setGmrDetails(gmrDetails);
						}
					}
				}
				
			}
			else {
				internalContractItemRefNo = contract.getContractItemList().get(0).getInternalItemRefNo();
				priceType = contract.getContractItemList().get(0).getPricing().getPriceTypeId();
				contract.setQuality(contract.getContractItemList().get(0).getQuality());
				if(null!=itemArray) {
				itemsFetcher.getItemForContractDensity(itemArray, internalContractItemRefNo,contract);
				}
				if(null!=contract.getContractRefNo()) {
					itemRefNo = contract.getContractRefNo()+"."+contract.getContractItemList().get(0).getItemNo();
				}
				triggerPricing = triggerPriceFetcher.fetchTriggerPriceDetails(tenantProvider, internalContractItemRefNo);
				if (null == compList || compList.isEmpty()) {
					compList = contract.getContractItemList().get(0).getPricingComponentList();
				}
				if (null == tieredPricingItemList || tieredPricingItemList.isEmpty()) {
					tieredPricingItemList = contract.getContractItemList().get(0).getTieredItemList();
				}

			}

			if (StringUtils.isEmpty(priceType)) {
				throw new PricingException(
						messageFetcher.fetchErrorMessage(tenantProvider, "010", new ArrayList<String>()));
			}
			for (Formula f : formulaList) {
				inputObj.put("curves", f.getCurveList());
				if (!formulaMap.containsKey(f.getId())) {
					formulaMap.put(f.getId(), f);
				}
				if (!formulaCurveListMap.containsKey(f.getId())) {
					List<String> nameList = new ArrayList<String>();
					for (Curve c : f.getCurveList()) {
						nameList.add(c.getCurveName());
						if(triggerPricing.size()>0) {
						   c.setTriggerPriceExecution(true);
						}
					}
					formulaCurveListMap.put(f.getId(), nameList);
				}

			}
			List<JSONObject> itemList = new ArrayList<JSONObject>();
			List<CurveDetails> detailsList = new ArrayList<CurveDetails>();
			if(contractQtyUnit.length()==0) {
				String contractQtyUnitID = contract.getItemDetails().get(0).getQtyUnit();
				if(StringUtils.isEmpty(contractQtyUnitID)) {
					contractQtyUnitID = contract.getItemDetails().get(0).getItemQtyUnitId();
				}
				mdmFetcher.populateMDMData(productIdList.get(0));
				contractQtyUnit = mdmFetcher.getContractQty(tenantProvider, contractQtyUnitID,
						productIdList.get(0));
				if (contractQtyUnit.length() == 0) {
					throw new PricingException(
							messageFetcher.fetchErrorMessage(tenantProvider, "008", new ArrayList<String>()));
				}
			}
//			iterating contractItems
			for (ContractItem ci : contract.getContractItemList()) {
				CurveDetails curveDetails = new CurveDetails();
				if (productIdList.size() == 0) {
					productIdList.add(ci.getProductId());
					mdmFetcher.populateMDMData(ci.getProductId());
					contract.setProduct(mdmFetcher.getProductValue(tenantProvider, ci.getProductId()));
				}else {
					mdmFetcher.populateMDMData(productIdList.get(0));
					contract.setProduct(mdmFetcher.getProductValue(tenantProvider, productIdList.get(0)));
				}
				JSONObject obj = new JSONObject(ci);
				if (null == tieredPricingItemList || tieredPricingItemList.isEmpty()) {
					if(contract.getContractItemList().get(0).getPricing().getPricingStrategy().equals("pricingStrategy-003")) {
						tieredPricingItemList = tieredPricingObjectFetcher.fetchTieredPricingObjects(tenantProvider,
								internalContractItemRefNo);
						
						if(tieredPricingItemList.isEmpty() || null == tieredPricingItemList){
							tieredPricingItemList = tieredPricingObjectFetcher.fetchTieredPricingObjectsForContractCreation(
									contract.getContractDraftId(), contract.getItemDetails().get(0).getItemNo());
						}
					}else {
					tieredPricingItemList = tieredPricingObjectFetcher.fetchTieredPricingObjects(tenantProvider,
							ci.getRefNo());
					}
				}
				ci.setTieredItemList(tieredPricingItemList);
				Formula f = new Formula();
				if ((null != tieredPricingItemList && tieredPricingItemList.size() == 0) || isPricePreview) {
					if (formulaList.size() == 1 && (null == ci.getPricing().getPricingFormulaId()
							|| !formulaList.contains(ci.getPricing().getPricingFormulaId()))) {
						f = formulaList.get(0);
					} else {
						f = formulaMap.get(ci.getPricing().getPricingFormulaId());
					}
					if(null == f || StringUtils.isEmpty(f.getFormulaExpression())) {
						throw new PricingException(
								messageFetcher.fetchErrorMessage(tenantProvider, "009", new ArrayList<String>()));
					}
					String expression = f.getFormulaExpression();
					String expressionAfterBuilding = expressionBuilder.buildExpression(formulaCurveListMap.get(f.getId()),
							expression);
					if(priceType.toLowerCase().contains("formula") && (null==compList || compList.isEmpty())) {
						compList = compFetcher.fetchComponent(tenantProvider, internalContractItemRefNo, null);
						calculateComponent(f.getCurveList(), compList, expressionAfterBuilding, 0, tenantProvider);
						expressionAfterBuilding = expressionBuilder.simplifyExpression(expressionAfterBuilding, compList);
					}
					else if(priceType.toLowerCase().contains("formula") && !compList.isEmpty()) {
						calculateComponent(f.getCurveList(), compList, expressionAfterBuilding, 0, tenantProvider);
						expressionAfterBuilding = expressionBuilder.simplifyExpression(expressionAfterBuilding, compList);
					}
					curveDetails.setExpression(expressionAfterBuilding);
					curveDetails.setOriginalExp(f.getFormulaExpression());
					curveDetails.setPricePrecision(f.getPricePrecision());
					curveDetails.setCurveList(f.getCurveList());
					curveDetails.setTriggerPriceEnabled(true);
					curveDetails.setTriggerPriceList(triggerPricing);
					curveDetails.setHolidayRule(f.getHolidayRule());
					curveDetails.setPriceDifferentialList(f.getPriceDifferential());
					obj.put("pricePrecision", f.getPricePrecision());
					if (null == f.getPricePrecision() || f.getPricePrecision().isEmpty()) {
						obj.put("pricePrecision", "2");
					}
					itemList.add(obj);
					detailsList.add(curveDetails);
				}
				else {
					Gson gson = new Gson();
					itemList.add(new JSONObject(gson.toJson(ci)));
				}
				
			}
			for (JSONObject itemObj : itemList) {
				JSONObject pricingObj = itemObj.getJSONObject("pricing");
				String currency = null;
				String qtyUnit = null;
				try {
					currency = pricingObj.optString("priceUnit");
					if (StringUtils.isEmpty(currency)) {
						currency = mdmFetcher.getPriceUnitValue(productIdList.get(0),
								pricingObj.optString("priceUnitId"));
						if (StringUtils.isEmpty(pricingObj.optString("priceUnitId"))
								&& StringUtils.isEmpty(internalPriceUnitIdList.get(0))) {
							logger.error(Logger.EVENT_FAILURE, ESAPI.encoder()
									.encodeForHTML("empty internal price unit ID for " + pricingObj.toString()));
							throw new PricingException("internalPriceUnitId not available");
						}
						if(StringUtils.isEmpty(internalPriceUnitIdList.get(0))) {
							internalPriceUnitIdList.add(0, pricingObj.optString("priceUnitId"));
						}
						else {
							internalPriceUnitIdList.add(pricingObj.optString("priceUnitId"));
						}
					}
//					System.out.println("currency : "+currency);
				} catch (Exception e) {
					currency = "USD";
				}
				if (currency.contains("/")) {
					qtyUnit = currency.substring(currency.indexOf("/") + 1, currency.length()).trim();
					currency = currency.substring(0, currency.indexOf("/"));
				}
				currencyList.add(currency);
				qtyUnitList.add(qtyUnit);
			}
			if (null != tieredPricingItemList && tieredPricingItemList.size() > 0 && !isPricePreview) {
				isTieredRequest = true;
				contractObj.put("itemDetails",
						processExpressionForTieredPricing(detailsList, itemList, tenantProvider, currencyList,
								qtyUnitList, productIdList, contractQtyUnit, compList, contract.getRefNo(),
								tieredPricingItemList , contract.getContractType(), internalPriceUnitIdList, contract.getQuality(),contract.getContractQualityDensity(),
								contract.getContractQualityMassUnit(),contract.getContractQualityVolumeUnit(),contract.getLocationType(),contract));
			} else {
				contractObj.put("itemDetails",
						processExpression(detailsList, itemList, tenantProvider, currencyList, qtyUnitList,
								productIdList, contractQtyUnit, compList, contract.getRefNo(), tieredPricingItemList,
								contract.getContractType(), internalPriceUnitIdList, contract.getQuality(),contract.getContractQualityDensity(),
								contract.getContractQualityMassUnit(),contract.getContractQualityVolumeUnit(),contract.getLocationType(),contract));
			}
			
		}

		else {
			inputObj = putValue(inputObj, "contractId", "");
			List<CurveDetails> detailsList = new ArrayList<CurveDetails>();
			List<JSONObject> itemList = new ArrayList<JSONObject>();
//			We will get only one formula when using pricing app independently
			Formula f = formulaList.get(0);
			CurveDetails cd = new CurveDetails();
			cd.setCurveList(f.getCurveList());
			cd.setPricePrecision(f.getPricePrecision());
			for (Formula form : formulaList) {
				inputObj.put("curves", form.getCurveList());
				if (!formulaMap.containsKey(form.getId())) {
					formulaMap.put(form.getId(), form);
				}
				if (!formulaCurveListMap.containsKey(form.getId())) {
					List<String> nameList = new ArrayList<String>();
					for (Curve c : form.getCurveList()) {
						nameList.add(c.getCurveName());
					}
					formulaCurveListMap.put(form.getId(), nameList);
				}

			}
			cd.setExpression(
					expressionBuilder.buildExpression(formulaCurveListMap.get(f.getId()), f.getFormulaExpression()));
			cd.setOriginalExp(f.getFormulaExpression());
			cd.setPriceDifferentialList(f.getPriceDifferential());
			cd.setHolidayRule(f.getHolidayRule());
//			System.out.println("f.getPriceDifferential() : "+f.getPriceDifferential().);
			detailsList.add(cd);
//			System.out.println("f.getContractCurrencyPrice()"+f.getContractCurrencyPrice());
			contractObj.put("refNo", "");
			contractObj.put("asOfDate", context.getCurrentContext().getAsOfDate());
			currencyList.add(f.getContractCurrencyPrice());

			contractObj.put("itemDetails",
					processExpression(detailsList, itemList, tenantProvider, currencyList, qtyUnitList, productIdList,
							contractQtyUnit, compList, "", tieredPricingItemList, contract.getContractType(),internalPriceUnitIdList,contract.getQuality(),
							contract.getContractQualityDensity(),contract.getContractQualityMassUnit(),contract.getContractQualityVolumeUnit(),
							contract.getLocationType(),contract));
		}

		JSONObject finalObject = new JSONObject();
		if (null == modeLocal || !modeLocal.contains("Detailed")) {
			JSONArray itemArr = contractObj.optJSONArray("itemDetails");
			for (int itemCount = 0; itemCount < itemArr.length(); itemCount++) {
				JSONObject itemObj = itemArr.getJSONObject(itemCount).optJSONObject("priceDetails");
				itemObj.remove("curveData");
				itemArr.getJSONObject(itemCount).put("priceDetails", itemObj);
			}
			isTRMRequest=false;
		}
		finalObject.put("contract", contractObj);
		if(isTRMRequest) {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.add("Authorization", validator.cleanData(context.getCurrentContext().getToken()));
			headers.add("X-TenantID", validator.cleanData(context.getCurrentContext().getTenantID()));
			headers.add("requestId", validator.cleanData(context.getCurrentContext().getRequestId()));
			headers.add("sourceDeviceId", validator.cleanData(context.getCurrentContext().getSourceDeviceId()));
			headers.add("X-Locale", "en_US");
			String body = "";
			List<Map<String, Object>> connectPayloadList = new ArrayList<>();
			if(isTieredRequest) {
				for (TieredPricingItem tieredPricingItem : tieredPricingItemList) {
					body = exposureAppendHelper.createBodyObject(finalObject, contract, tieredPricingItem.getFormulaObj(), itemRefNo);
					if(!itemRefNo.isEmpty()) {
						connectPayloadList=connectExposureData.updateConnectExposureBody(tenantProvider, finalObject, contract, tieredPricingItem.getFormulaObj(), itemRefNo);
				}
				}
			}
			else {
				body = exposureAppendHelper.createBodyObject(finalObject, contract, formulaList.get(0), itemRefNo);
				if(!itemRefNo.isEmpty()) {
					connectPayloadList=connectExposureData.updateConnectExposureBody(tenantProvider, finalObject, contract, formulaList.get(0), itemRefNo);
			}
				
			}
			final String exposureBody = body;
			try {
			if(!itemRefNo.isEmpty()) {
			connectExposureData.updateConnectExposure(tenantProvider, connectPayloadList);
			}
			}catch(Exception e) {
				logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("exception while saving data in DB"),e);
				
			}
			exposureAppendHelper.uploadExposure(exposureBody, headers);
		}
//		Below code to store TRM request into kafka topic
		if(isTRMRequest) {
			JSONObject kafkaOutput = new JSONObject(finalObject, JSONObject.getNames(finalObject));
			TimeZone tz = TimeZone.getTimeZone("UTC");
			//DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
			df.setTimeZone(tz);
			String nowAsISO = df.format(new Date());
			kafkaOutput.getJSONObject("contract").put("asOfDate", nowAsISO);
			kafkaProducerHelper.push(finalObject.toString(), context.getCurrentContext().getRequestId());
		}
		if(finalObject.has("exposureArray")) {
			finalObject.remove("exposureArray");
		}
		return finalObject;
	}
	
	public void calculateComponent(List<Curve> curveList, List<PricingComponent> compList, String exp, int curveInd,
			ContextProvider tenantProvider) throws PricingException {
		while (exp.contains("{{")) {
			if (curveInd == curveList.size()) {
				throw new PricingException(
						messageFetcher.fetchErrorMessage(tenantProvider, "011", new ArrayList<String>()));
			}
			int ind = exp.indexOf("{{");
			int last = exp.indexOf("}}");
			String desiredCurve = exp.substring(ind, last + 2);
			String extractedCurve = "";
			for (PricingComponent ci : compList) {
				String curveWithComp = ci.getProductComponentName() + "* " + desiredCurve;
				if (exp.contains(curveWithComp)) {
					extractedCurve = exp.substring(ind + 1, last + 1);
					;
					curveList.get(curveInd).setComponent(ci.getProductComponentName());
					break;
				}
			}
			exp = exp.replaceFirst(Pattern.quote(desiredCurve), extractedCurve);
			calculateComponent(curveList, compList, exp, curveInd + 1, tenantProvider);
		}
	}
	
	public Object retrieveCacheStored(String cacheKey, String ttl) {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		try {
			resultMap = redisCacheManager.retrieveFromCache(cacheKey, ttl);
		} catch (PricingException e) {
			logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("exception while fetching cache"));
			e.printStackTrace();
		}
		if(null==resultMap || resultMap.isEmpty()) {
			return null;
		}
		return resultMap.get(cacheKey+"");
	}
	
	public boolean storeCache(String cacheKey, String cachedOutput, String ttl) {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		resultMap.put(cacheKey, (Object) cachedOutput);
		try {
			boolean isCachingSuccess = redisCacheManager.addInCache(resultMap, cacheKey, ttl);
			return isCachingSuccess;
		} catch (PricingException e) {
			logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("exception while storing to cache"));
			e.printStackTrace();
		}
		return false;
	}
	
	public String createCacheKey(PayloadInput payload) {
		String cacheKey = "";
		String contractRef = payload.getContract().getRefNo();
		String itemNo = payload.getContract().getItemDetails().get(0).getItemNo();
		String contractItemRef = payload.getContract().getContractItemList().get(0).getRefNo();
		if(null != payload.getFormulaList() && !payload.getFormulaList().isEmpty()) {
			String diff = "NA";
			try { 
				if(!StringUtils.isEmpty(payload.getFormulaList().get(0).getCurveList().get(0).getDifferential())) {
					diff = payload.getFormulaList().get(0).getCurveList().get(0).getDifferential();
				}
			}
			catch (Exception e) {
				diff = "NA";
			}
			cacheKey = contractRef + payload.getFormulaList().get(0).getFormulaExpression() + diff;
			return cacheKey;
		}
		List<GMR> gmrList = payload.getContract().getContractItemList().get(0).getGmrDetails();
		if(null == gmrList || gmrList.isEmpty()) {
			cacheKey = contractRef + "/"+ (contractItemRef == null ? "NA" : contractItemRef) + "/" +itemNo;
		}
		else {
			
			cacheKey = contractRef + "/"+ (contractItemRef == null ? "NA" : contractItemRef) + "/" +itemNo;
			for(GMR gmr: gmrList) {
				cacheKey = cacheKey + "-" +gmr.getRefNo();
				List<Stock> stockList = gmr.getStocks();
				for(Stock stock: stockList) {
					cacheKey = cacheKey + "/"+ stock.getRefNo();
				}
			}
		}
		return cacheKey;
	}
	
	public double processTiered(List<TieredPricingItem> tieredPricingItemList, ContextProvider context,
			JSONObject itemObj, int index, GMR gmr, List<String> currencyList, List<String> qtyUnitList,
			List<String> productIdList, double prevStockQty) throws Exception {
		double finalContractPrice = 0d;
		double finalCeiling = 0;
		double gmrQty = 0d;
		double gmrQtyPerSplit = 0d;
		if(null!=gmr) {
			gmrQty = gmr.getMovementQty();
		}
		List<Double>ceilingList = new ArrayList<Double>();
		for (TieredPricingItem t : tieredPricingItemList) {
			double splitQuantity = t.getSplitCeiling() - t.getSplitFloor();
			
			if (null != gmr) {
				if (t.getSplitCeiling() <= prevStockQty || t.getUsedQtyInGMR()==splitQuantity) {
					continue;
				}
				if (curveService.checkZero(gmrQty) || curveService.checkNegative(gmrQty)) {
					break;
				}
				if (gmrQty <= (splitQuantity - t.getUsedQtyInGMR())) {
					gmrQtyPerSplit = gmrQty;
					gmrQty = 0;
					t.setUsedQtyInGMR(t.getUsedQtyInGMR() + gmrQtyPerSplit);
					
				} 
				else if ((splitQuantity > t.getUsedQtyInGMR()) && gmrQty > (splitQuantity - t.getUsedQtyInGMR())) {
					gmrQtyPerSplit = splitQuantity - t.getUsedQtyInGMR();
					gmrQty = gmrQty - gmrQtyPerSplit;
					t.setUsedQtyInGMR(t.getUsedQtyInGMR() + gmrQtyPerSplit);
				}
				else {
					gmrQty = gmrQty - splitQuantity;
					gmrQtyPerSplit = splitQuantity;
					t.setUsedQtyInGMR(t.getUsedQtyInGMR() + gmrQtyPerSplit);
				}

			}
			int precision = 0;
			ceilingList.add(t.getSplitCeiling());
			Formula f = t.getFormulaObj();
			double tieredContractPrice = 0d;
			try {
				precision = Integer.parseInt(f.getPricePrecision());
			}
			catch (Exception e) {
				precision = 2;
			}
			if (!StringUtils.isEmpty(gmr)) {
				for (Curve c : f.getCurveList()) {
					c.setCalculatedPrice(0);
				}
			}
			if (t.getTieredLevelPrice() == 0 || gmr != null) {
				String expression = calculateCurve(f.getFormulaExpression(), f.getCurveList(), itemObj,
						Integer.toString(precision), index, gmr, context, currencyList, qtyUnitList, f.getHolidayRule(),
						productIdList);
				while (expression.contains("MIN") || expression.contains("MAX") || expression.contains("AVG")) {
					expression = curveFetcher.calculateAggregate(expression, context);
				}
				Expression ex = null;
				try {
					ex = new ExpressionBuilder(expression).build();
				} catch (Exception exc) {
					throw new PricingException(
							messageFetcher.fetchErrorMessage(context, "001", new ArrayList<String>()));
				}
				tieredContractPrice = ex.evaluate();
				t.setTieredLevelPrice(tieredContractPrice);
			} else {
				tieredContractPrice = t.getTieredLevelPrice();
			}
			if (tieredPricingItemList.size() == 1) {
				return tieredContractPrice;
			}
			if (null != gmr) {
				finalCeiling = gmr.getMovementQty();
				finalContractPrice = finalContractPrice + (tieredContractPrice * gmrQtyPerSplit);
			}
			else if (t.getSplitCeiling() > finalCeiling) {
				finalCeiling = t.getSplitCeiling();
				finalContractPrice = finalContractPrice + (tieredContractPrice * (t.getSplitCeiling() - t.getSplitFloor()));
			}
			finalContractPrice = expressionBuilder.applyPrecision(finalContractPrice, precision);
		}
		if (curveService.checkZero(finalCeiling)) {
			throw new PricingException(messageFetcher.fetchErrorMessage(context, "012", new ArrayList<String>()));
		}
		return finalContractPrice/finalCeiling;
	}
	
	public String evaluateTriggerPrice(CurveDetails curveDetail, ContextProvider tenantProvider, String product,
			String qty, String itemQty, String contractPrice, TriggerPriceProperties triggerProps) throws Exception {
		String result = contractPrice;
		double previewPrice = 0;
		double triggerQty = 0;
		List<TriggerPrice> triggerList = curveDetail.getTriggerPriceList();
		if (null != triggerList && triggerList.size() > 0) {
			for (TriggerPrice trigger : triggerList) {
				if (null == trigger.getPriceU() || trigger.getPriceU().isEmpty()) {
					throw new PricingException(
							messageFetcher.fetchErrorMessage(tenantProvider, "013", new ArrayList<String>()));
				}
				triggerQty = triggerQty + trigger.getQuantity();
				/*Quantity and FX conversion is now happening at trigger price UI screen, hence commenting below code
				 * 
				String unit = trigger.getPriceU();
				unit = unit.substring(unit.indexOf("/") + 1, unit.length());
				String fromUnit = mdmFetcher.getQuantityKey(tenantProvider, unit, product);
				String toUnit = mdmFetcher.getQuantityKey(tenantProvider, qty, product);
				if (!fromUnit.equals(toUnit)) {
					double qtyFxRate = mdmFetcher.getQtyUnitConversionRate(tenantProvider, product, fromUnit, toUnit);
					if (qtyFxRate == 1) {
						logger.info(Logger.EVENT_SUCCESS,
								ESAPI.encoder().encodeForHTML("Qty exchange rate is 1 for " + unit + " to " + qty));
					}
					trigger.setQtyConversion(qtyFxRate);
				} else {
					trigger.setQtyConversion(1);
				}*/
			}
			curveDetail.setTriggerPriceList(triggerList);
			curveDetail.setTriggerQty(triggerQty);
			previewPrice = triggerPriceCalculator.applyTriggerPrice(curveDetail, Double.parseDouble(itemQty),
					result, context.getCurrentContext().getAsOfDate().atStartOfDay(), tenantProvider, triggerProps);
			result = Double.toString(previewPrice);
		}
//		curveDetail.setTriggerList(triggerList);
		return result;
	}
	
	public List<JSONObject> processExpressionForTieredPricing(List<CurveDetails> detailsList, List<JSONObject> itemList,
			ContextProvider tenantProvider, List<String> currencyList, List<String> qtyUnitList,
			List<String> productIdList, String contractItemQtyUnit, List<PricingComponent> compList,
			String internalContractRefNo, List<TieredPricingItem> tieredPricingItemList, String contractType,
			List<String> internalPriceUnitIdList, String quality,double contractQualityDensity,
			String contractQualityMassUnit,String contractQualityVolumeUnit,String locationType,Contract contract) throws Exception {
		HashMap<String, String> exchangeMap = new HashMap<String, String>();
		List<JSONObject> resultDataSet = new ArrayList<>();
		JSONArray tieredObjectList = new JSONArray();
		JSONObject itemObj = itemList.get(0);
		String pricingStrategy = itemObj.optJSONObject("pricing").optString("pricingStrategy");
		boolean isFlat = false;
		if(pricingStrategy.equals("pricingStrategy-003")) {
			isFlat = true;
		}
		List<TriggerPrice> triggerPriceList = new ArrayList<TriggerPrice>();
		String internalContractItemRefNo = itemObj.optString("internalItemRefNo", "");
		triggerPriceList = triggerPriceFetcher.fetchTriggerPriceDetails(tenantProvider, internalContractItemRefNo);
		
		List<GMR> gmrList = new ArrayList<GMR>();
		Date tradeDate = null;
		List<JSONObject> curveData = new ArrayList<JSONObject>();
		JSONArray gmrDetails = new JSONArray();
		double processedTieredQty = 0d;
		List<TieredFields> tieredFieldsList = getFormulaForTiers(tenantProvider, detailsList, tieredPricingItemList, currencyList,
				qtyUnitList, productIdList, itemObj, compList, exchangeMap);
		int indexForTier = 0;
		for (TieredPricingItem tieredPricingItem : tieredPricingItemList) {
			TieredFields tieredFields = tieredFieldsList.get(indexForTier);
			JSONObject tieredObj = tieredFields.getTieredObj();
			CurveDetails curveDetails = tieredFields.getCurveDetails();
			double tieredQuantity = tieredFields.getTieredQuantity();
			Formula f = tieredFields.getFormula();
			tieredObj.put("tieredID", tieredPricingItem.getSplitId());
			
			detailsList.add(0, curveDetails);
			
			JSONArray movArray = itemObj.optJSONArray("gmrDetails");
			if(movArray != null) {
				movArray = sortMovementArr(movArray);
			}
			int j = 0;
			List<Stock> stockList = null;
			List<QualityAttributes> stockQualityList = null;
			QualityAttributes qualityAttributes = null;
			String qualityAdjustmentName = null;
			String deliveredQuality = null;
			
			stockList = new ArrayList<Stock>();
			JSONArray stArr = new JSONArray();
			List<Double> stockPriceList = new ArrayList<Double>();
			double qtyForGMR = tieredQuantity + processedTieredQty;
			List<GMRStatusObject> gmrStatusObjectList = gmrStatusObjectFetcher
					.fetchGMRStatusObject(itemObj.optString("refNo"));
			JSONArray gmrDataArr = gmrCreationHelper.fetchGMRData(itemObj.optString("refNo"));
			List<String> storedGMRList = new ArrayList<String>();
			Map<String, Double> gmrStoredPriceMap = new HashMap<String, Double>();
			boolean isGMRFixationCompleted = false;
			if (null != movArray) {
				for (int movInd = 0; movInd < movArray.length(); movInd++) {
					for (int gmrInd = 0; gmrInd < gmrDataArr.length(); gmrInd++) {
						JSONObject gmrData = gmrDataArr.optJSONObject(gmrInd);
						if (!gmrStatusObjectList.isEmpty() && gmrData.optString("internalGMRRefNo")
								.equalsIgnoreCase(movArray.optJSONObject(movInd).optString("refNo", ""))) {
							for (GMRStatusObject statusObj : gmrStatusObjectList) {
								if (null != statusObj.getGmrRefNo()
										&& !statusObj.getGmrStatus().equalsIgnoreCase("cancelled")
										&& statusObj.getGmrRefNo().equals(gmrData.optString("internalGMRRefNo"))
										&& curveService.checkZero(statusObj.getGmrUnFixedQty())) {
									storedGMRList.add(gmrData.optString("internalGMRRefNo"));
									isGMRFixationCompleted = true;
									gmrStoredPriceMap.put(gmrData.optString("internalGMRRefNo"),
											gmrData.optDouble("estimatedPrice"));
								}
								double qtyInStoredStatus = Double.parseDouble(statusObj.getGmrQty());
								double qtyInStoredGMR = gmrData.optDouble("executedQuantity");
								double qtyInNewGMR = 0;
								JSONArray stockArray = movArray.optJSONObject(movInd).optJSONArray("stocks");
								for(int stockInd=0; stockInd<stockArray.length();stockInd++) {
									JSONObject stockObj = stockArray.optJSONObject(stockInd);
									qtyInNewGMR = qtyInNewGMR + stockObj.optDouble("qty");
								}
								boolean allFixationsUsed = false;
								for(TriggerPrice trigger : detailsList.get(0).getTriggerPriceList()) {
									if(curveService.checkZero(trigger.getItemFixedQtyAvailable())) {
										allFixationsUsed = true;
									}
									else {
										allFixationsUsed = false;
										break;
									}
									
								}
								if(null != statusObj.getGmrRefNo()
										&& !statusObj.getGmrStatus().equalsIgnoreCase("cancelled")
										&& statusObj.getGmrRefNo().equals(gmrData.optString("internalGMRRefNo"))
										&& curveService.checkZero(qtyInStoredStatus-qtyInNewGMR)
										&& curveService.checkZero(qtyInStoredGMR-qtyInNewGMR)
										&& allFixationsUsed) {
									storedGMRList.add(gmrData.optString("internalGMRRefNo"));
									isGMRFixationCompleted = true;
									gmrStoredPriceMap.put(gmrData.optString("internalGMRRefNo"),
											gmrData.optDouble("estimatedPrice"));
								}
							}
							
						}
					}
				}
				
			}
			if (movArray != null && indexForTier==0) {// parsing stock details
				while (j < movArray.length()) {
					JSONObject gmrObj = movArray.getJSONObject(j);
					JSONObject movObj = new JSONObject(movArray.getJSONObject(j),
							JSONObject.getNames(movArray.getJSONObject(j)));
					String gmrRef = gmrObj.optString("refNo", "");
					String titleTransferStatus = gmrObj.optString("titleTransferStatus", "");
					String storageLocation = gmrObj.optString("storageLocation", "");
					String loadingLocType = gmrObj.optString("loadingLocType", "");
					String loadingLocName = gmrObj.optString("loadingLocName", "");
					String destinationLocType = gmrObj.optString("destinationLocType", "");
					String destinationLocName = gmrObj.optString("destinationLocName", "");
					String vesselName = gmrObj.optString("vesselName", "");
					GMR gmr = new GMR();
					triggerPriceList = new ArrayList<TriggerPrice>();
					gmr.setRefNo(gmrRef);
					if(!storedGMRList.contains(gmrRef)) {
						isGMRFixationCompleted = false;
					}
					compList = compFetcher.fetchComponent(tenantProvider, itemObj.optString("refNo"), null);
					JSONArray stockArray = gmrObj.optJSONArray("stocks");
					int k = 0;
					double stockSum = 0d;
					double prevStockQty = 0d;
					double openQuantity = itemObj.optDouble("qty");
					if (stockArray != null && stockArray.length() > 0) {
						while (k < stockArray.length()) {
							JSONObject stockObj = stockArray.getJSONObject(k);
							Stock stock = new Stock();
							String stockRefNo = stockObj.optString("refNo");
							String contractItemRefNo = itemObj.optString("refNo");
							double stockQty = stockObj.optDouble("qty", 0l);
							double stockQtyInGMR = stockQty;
							double massToVolConFactor = stockObj.optDouble("massToVolConversionFactor", 0l);
							stock.setQtyUnit(stockObj.optString("qtyUnit"));
//							double stockQtyUnit = stockObj.optString("qtyUnit", "");
							String itmeQtyUnitId = mdmFetcher.getQuantityKey(tenantProvider, itemObj.getString("qtyUnit"),
									productIdList.get(0));
							if (!stock.getQtyUnit().equalsIgnoreCase(itmeQtyUnitId)) {
								double QtyConRate = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productIdList.get(0),
										stock.getQtyUnit(), itmeQtyUnitId);
								if (QtyConRate == 1) {
									logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML(
											"Qty exchange rate is 1 for " + stock.getQtyUnit() + " to " + itmeQtyUnitId));
								}
								if(!curveService.checkZero(stock.getMassToVolConversionFactor())) {
									QtyConRate = stock.getMassToVolConversionFactor();
								}
								stock.setQtyConversionRate(QtyConRate);
								stockQty = stockQty * QtyConRate;

							}
							stockSum = stockSum + stockQty;
							List<Event> eventList = new ArrayList<Event>();
							JSONArray eventArr = stockObj.optJSONArray("event");
							int eventInd = 0;
							while (eventInd < eventArr.length()) {
								JSONObject eventObject = eventArr.getJSONObject(eventInd);
								Event e = new Event();
								e.setName(eventObject.optString("name", ""));
								e.setDate(eventObject.optString("date"));
								eventList.add(e);
								eventInd++;
							}
							
							GMR gmrForPrice = new GMR();
							gmrForPrice.setRefNo(gmrRef);
							gmrForPrice.setStockRef(stockRefNo);
							gmrForPrice.setEvent(eventList);
							gmrForPrice.setMovementQty(new Double(stockQty).longValue());
							
							gmr.setRefNo(gmrRef);
							gmr.setStockRef(stockRefNo);
							gmr.setEvent(eventList);
							gmr.setMovementQty(new Double(stockQty).longValue());
							gmr.setMassToVolumeConversionFactor(massToVolConFactor);
							gmr.setTitleTransferStatus(titleTransferStatus);
							gmr.setStorageLocation(storageLocation);
							gmr.setLoadingLocType(loadingLocType);
							gmr.setLoadingLocName(loadingLocName);
							gmr.setDestinationLocType(destinationLocType);
							gmr.setDestinationLocName(destinationLocName);
							gmr.setVesselName(vesselName);
							/*
							 *Setting the curve level calculated price to zero because it will reset when GMR details are passed. 
							 * */								
							for(Curve c: f.getCurveList()) {
								c.setCalculatedPrice(0.0);
							}
							double stockLevelPrice = 0d;
							if(isFlat) {
								TieredPricingItem tier = getTierForFlat(tieredPricingItemList, gmr.getMovementQty());
								f = tier.getFormulaObj();
								double[] pricedArr = calculatePricedQuantityForGMR(stArr, f.getCurveList(),
										context.getCurrentContext().getAsOfDate(), tenantProvider, detailsList.get(0),
										itemObj, gmr, exchangeMap, isFlat);
								gmrObj.put("pricedQty", pricedArr[0]);
								gmrObj.put("unpricedQty", pricedArr[1]);
								if (pricedArr[1] != 0) {
									gmrObj.put("isFullyPriced", "N");
								} else {
									gmrObj.put("isFullyPriced", "Y");
								}
//								List<TieredPricingItem> listForFlatTiered  = new ArrayList<TieredPricingItem>();
//								listForFlatTiered.add(getTierForFlat(tieredPricingItemList, gmr.getMovementQty()));
								stockLevelPrice = tier.getTieredLevelPrice();
							}
							else {
								stockLevelPrice = processTiered(tieredPricingItemList, tenantProvider, itemObj, 0,
										gmr, currencyList, qtyUnitList, productIdList, prevStockQty);
							}
							qtyForGMR = qtyForGMR - stockQty;
							double stockLevelPreviewPrice = 0;
							if (detailsList.get(0).getPriceDifferentialList() != null
									&& detailsList.get(0).getPriceDifferentialList().size() > 0) {
								tradeDate = new Date();
								stockLevelPreviewPrice = curveService.applyDifferentialPrice(detailsList.get(0), stockLevelPrice+"",
										currencyList.get(0).trim(), tradeDate);
								stockLevelPrice = stockLevelPreviewPrice;
							}
							stockPriceList.add(stockLevelPrice);
							stockObj.put("stockPrice", stockLevelPrice);
//							stockObj.remove("event");
//							stockObj.remove("quality");
//							stockObj.remove("deliveredPrice");

							stock.setRefNo(stockRefNo);
							stock.setGMRRefNo(gmrRef);
							stock.setContractItemRefNo(contractItemRefNo);
							stock.setContractRefNo(internalContractRefNo);
							stock.setQty(stockQty); // Stock Qty in Item Qty unit
							stock.setStockQtyInGmr(stockQtyInGMR); // New field for GMR Qty
							stock.setQtyUnitId(stockObj.optString("qtyUnitId"));
							stock.setDensityValue(stockObj.optDouble("densityValue"));
							stock.setMassUnitId(stockObj.optString("massUnitId"));
							stock.setVolumeUnitId(stockObj.optString("volumeUnitId"));
							stock.setMassToVolConversionFactor(stockObj.optDouble("massToVolConversionFactor"));
							stock.setDensityVolumeQtyUnitId(stockObj.optString("densityVolumeQtyUnitId"));
							stock.setQuality(stockObj.optString("quality"));
							stock.setItemQtyUnit(contractItemQtyUnit);
							String itemQtyUnitId = mdmFetcher.getQuantityKey(null, contractItemQtyUnit, null);
							stock.setItemQtyUnitId(itemQtyUnitId);
							JSONArray qualityArray = stockObj.optJSONArray("attributes");
							stockQualityList = new ArrayList<QualityAttributes>();
							int ind = 0;
							if (qualityArray != null && qualityArray.length() > 0) {
								while (ind < qualityArray.length()) {
									qualityAttributes = new QualityAttributes();
									JSONObject qualityObj = qualityArray.getJSONObject(ind);
									qualityAdjustmentName = qualityObj.optString("name");
									deliveredQuality = qualityObj.optString("value", "0.0");
									qualityAttributes.setName(qualityAdjustmentName);
									qualityAttributes.setValue(deliveredQuality);
									stockQualityList.add(qualityAttributes);
									ind++;
								}
							}
							stock.setAttributes(stockQualityList);
							stArr.put(stockObj);
							stockList.add(stock);
							prevStockQty = prevStockQty + stockQty;
							k++;
						}
					}
					if(detailsList.get(0).getTriggerPriceList().size()==0) {
						detailsList.get(0).setTriggerPriceEnabled(false);
					}
					if (movArray != null && movArray.length()>0) {
						if (detailsList.get(0).isTriggerPriceEnabled() && !isGMRFixationCompleted) {
							stockList = triggerPriceCalculator.applyTriggerPriceAtStock(
									detailsList.get(0).getTriggerPriceList(), stockList, null, stockPriceList, false,
									gmrStatusObjectList, detailsList.get(0).getCurveList(),
									context.getCurrentContext().getAsOfDate(), storedGMRList, tieredPricingItemList, isFlat, gmrDataArr);
						} else {
							stockList = triggerPriceCalculator.applyTriggerPriceAtStock(new ArrayList<TriggerPrice>(),
									stockList, null, stockPriceList, false, gmrStatusObjectList,
									detailsList.get(0).getCurveList(), context.getCurrentContext().getAsOfDate(),
									storedGMRList, tieredPricingItemList, isFlat, gmrDataArr);
						}
					}
					gmr.setStocks(stockList);
					double gmrLevelPrice = triggerPriceCalculator.getStockValue(new JSONArray(stockList), tenantProvider);
					double newTriggerPrice = triggerPriceCalculator.validGMRPriceForAdjustments(
							gmrStatusObjectList, stockPriceList, movObj, triggerPriceList);
					if (gmrStoredPriceMap.containsKey(movObj.optString("refNo"))) {
						gmrLevelPrice = gmrStoredPriceMap.get(movObj.optString("refNo"));
						if(!curveService.checkZero(newTriggerPrice)) {
							gmrLevelPrice = newTriggerPrice;
						}
					}
					List<GMRStatusObject> latestGmrStatusObjectList = gmrStatusObjectFetcher
							.fetchGMRStatusObject(itemObj.optString("refNo"));
					GMRStatusObject gmrStatusObject = null;
					for(GMRStatusObject statusObj: latestGmrStatusObjectList) {
						if (statusObj.getGmrRefNo().equalsIgnoreCase(movObj.optString("refNo"))
								&& statusObj.getGmrStatus().equalsIgnoreCase("FULLY FIXED")) {
							gmrStatusObject = statusObj;
						}
					}
					if(null!=gmrStatusObject) {
						gmrObj.put("pricedQty", gmrStatusObject.getGmrFixedQty());
						gmrObj.put("unpricedQty", 0);
						gmrObj.put("isFullyPriced", "Y");
					}
					else {
						if(isFlat) {
							TieredPricingItem tier = getTierForFlat(tieredPricingItemList, gmr.getMovementQty());
							f = tier.getFormulaObj();
							double[] pricedArr = calculatePricedQuantityForGMR(stArr, f.getCurveList(),
									context.getCurrentContext().getAsOfDate(), tenantProvider, detailsList.get(0),
									itemObj, gmr, exchangeMap, isFlat);
							gmrObj.put("pricedQty", pricedArr[0]);
							gmrObj.put("unpricedQty", pricedArr[1]);
							if (pricedArr[1] != 0) {
								gmrObj.put("isFullyPriced", "N");
							} else {
								gmrObj.put("isFullyPriced", "Y");
							}
						}
						else {
							double[] pricedArr = calculatePricedQuantityForGMR(stArr, f.getCurveList(),
									context.getCurrentContext().getAsOfDate(), tenantProvider, detailsList.get(0),
									itemObj, gmr, exchangeMap, false);
							gmrObj.put("pricedQty", pricedArr[0]);
							gmrObj.put("unpricedQty", pricedArr[1]);
							if (pricedArr[1] != 0) {
								gmrObj.put("isFullyPriced", "N");
							} else {
								gmrObj.put("isFullyPriced", "Y");
							}	
						}
					}
					
					gmrObj.put("price", gmrLevelPrice);
//					gmrObj.remove("executedQty");
//					gmrObj.remove("currentGMRQty");
//					gmrObj.remove("movementQty");
					gmrDetails.put(gmrObj);
					gmrCreationHelper.createGMR(tenantProvider, internalContractRefNo, itemObj.optString("refNo", ""),
							gmrObj.optString("refNo"), Double.toString(qtyForGMR),
							Double.toString(openQuantity - qtyForGMR), Double.toString(gmrLevelPrice),
							itemObj.getString("qtyUnit"), gmrObj.toString(), movObj.toString());
					gmrList.add(gmr);
					j++;
				}
			}
			
			indexForTier++;
			List<JSONObject> previewDataSet =null;
			if(!isFlat) {
				if(!curveDetails.getCurveList().isEmpty()) {
			
				previewDataSet = getPreviewDataSet(f.getCurveList(), itemObj, tenantProvider, f.getHolidayRule(),
						productIdList.get(0), contractItemQtyUnit, gmrList, f.getFormulaExpression(), contractType, exchangeMap, contractQualityDensity,
						 contractQualityMassUnit, contractQualityVolumeUnit,quality,locationType,contract);
			}else {
				itemObj.put("qty",tieredQuantity);
			   previewDataSet=getAbsoluteExp(itemObj, tenantProvider, f.getHolidayRule(),
							productIdList.get(0), contractItemQtyUnit, gmrList, f.getFormulaExpression(), contractType, exchangeMap,quality,
							contractQualityDensity,contractQualityMassUnit,contractQualityVolumeUnit,qtyUnitList.get(0),locationType,contract);
			   
			}
		
		}
			/*List<JSONObject> previewDataSet = getPreviewDataSet(f.getCurveList(), itemObj, tenantProvider, f.getHolidayRule(),
					productIdList.get(0), contractItemQtyUnit, gmrList, f.getFormulaExpression(), contractType, exchangeMap);*/
			logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("For Tiered pricing Split previewDataSet" + previewDataSet));
			if(null!=previewDataSet) {
			tieredObj.put("curveData", previewDataSet);
			for(int index = 0; index<previewDataSet.size(); index++) {
					for(JSONObject previewDataObj: previewDataSet) {
						previewDataObj.put("split", tieredObj.optString("tieredID"));
						logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML(
								"For Tiered pricing Split  " + tieredObj.optString("tieredID")));
						logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML(
								"For Tiered pricing Split previewDataObj" + previewDataObj));
					}
					curveData.add(previewDataSet.get(index));
			}
			tieredObjectList.put(tieredObj);
			processedTieredQty = processedTieredQty + tieredQuantity;
			}
		}
		
		if(isFlat) {
			//if(triggerPriceList.isEmpty()) {
			 curveData=contractTieredFLatExp(tieredPricingItemList,  contractType,productIdList,gmrList,itemList.get(0),tenantProvider,
					              contractItemQtyUnit,exchangeMap,quality,tieredFieldsList,context.getCurrentContext().getAsOfDate(),curveData,
									contractQualityDensity,contractQualityMassUnit,contractQualityVolumeUnit,qtyUnitList,locationType,contract,triggerPriceList);
			 
			 logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML(
						"For Tiered pricing " + curveData));
		
			 /*}else {
		   curveData=expFLatExpForTriggerPrice(tieredPricingItemList,  contractType,productIdList,gmrList,itemList.get(0),tenantProvider,
		              contractItemQtyUnit,exchangeMap,quality,tieredFieldsList,context.getCurrentContext().getAsOfDate(),curveData,qtyUnitList,
						contractQualityDensity,contractQualityMassUnit,contractQualityVolumeUnit);

			 logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML(
			"For Tiered Flat Trigger Pricing" + curveData));
	   }*/
		
	}
		
		/*if(isFlat && !gmrList.isEmpty() && triggerPriceList.isEmpty()) {
			 curveData=gmrTieredFLatExp(tieredPricingItemList,  contractType,productIdList,gmrList,itemList.get(0),tenantProvider,
					              contractItemQtyUnit,exchangeMap,quality,tieredFieldsList,context.getCurrentContext().getAsOfDate(),curveData,
									contractQualityDensity,contractQualityMassUnit,contractQualityVolumeUnit,qtyUnitList);
			 
			 logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML(
						"For Tiered pricing GMR" + curveData));
		
	   }*/
		
		
		JSONObject dataSet = new JSONObject();
		JSONObject itemDetailsObj = new JSONObject();
		List<JSONObject> objList = new ArrayList<JSONObject>();
		double contractPrice = 0;
		if(isFlat) {
			contractPrice = processTieredForFlat(tieredPricingItemList, itemObj.optDouble("qty"));
		}
		else {
			contractPrice = processTiered(tieredPricingItemList, tenantProvider, itemObj, 0, null, currencyList,
					qtyUnitList, productIdList, 0);
		}
		dataSet = putValue(dataSet, "contractPrice", contractPrice+"");
		dataSet.put("curveData", curveData);
		dataSet = putValue(dataSet, "marketPrice", "");
//		dataSet = putValue(dataSet, "originalExpression", detailsList.get(0).getOriginalExp());
		dataSet = putValue(dataSet, "priceUnit", currencyList.get(0) + "/" + qtyUnitList.get(0));
		dataSet = putValue(dataSet, "pricedQuantity", "");
		dataSet = putValue(dataSet, "unpricedQuantity", "");
		dataSet = putValue(dataSet, "quantityUnit", qtyUnitList.get(0));
		dataSet = putValue(dataSet, "internalPriceUnitId", internalPriceUnitIdList.get(0));
		double pricedPercentage = 0;
		double totalQuantity = 0;
		for(TieredPricingItem t: tieredPricingItemList) {
			double splitQuantity = t.getSplitCeiling() - t.getSplitFloor();
			pricedPercentage = pricedPercentage + (t.getPricedPercentage() * splitQuantity);
			totalQuantity = totalQuantity+ splitQuantity;
		}
		if(curveService.checkZero(pricedPercentage)) {
			dataSet.accumulate("pricedPercentage", 0);
			dataSet.accumulate("unpricedPercentage", 100);
		}
		else {
			pricedPercentage = pricedPercentage/totalQuantity;
			dataSet.accumulate("pricedPercentage", pricedPercentage);
			dataSet.accumulate("unpricedPercentage", 100 - pricedPercentage);
		}

		itemDetailsObj.accumulate("refNo", itemObj.optString("refNo"));
		itemDetailsObj = putValue(itemDetailsObj, "qPStartDate", itemObj.optString("deliveryFromDate", ""));
		itemDetailsObj = putValue(itemDetailsObj, "qPEndDate", itemObj.optString("deliveryToDate", ""));
		itemDetailsObj.put("gmrDetails", gmrDetails);
		resultDataSet.add(dataSet);
		itemDetailsObj.put("priceDetails", dataSet);
//		itemDetailsObj.put("stock", stockoutArr);
		objList.add(itemDetailsObj);
		return objList;
	}
		
	public double calculateMassToVolume(String fromUnit, String toUnit, double rate, double qtyFxRate,
			ContextProvider tenantProvider, String fromQtyUnit, String toQtyUnit, String productId, double price)
			throws Exception {
		double data = price;
		if (curveService.checkZero(data)) {
			return data;
		}
		if (rate > 1) {
			data = data * rate;
		} else if (StringUtils.isEmpty(fromUnit) || StringUtils.isEmpty(toUnit)
				|| !fromUnit.equals(toUnit)) {
//			String fromUnitKey = mdmFetcher.getCurrencyKey(tenantProvider, fromUnit, fromQtyUnit, productId);
//			String toUnitKey = mdmFetcher.getCurrencyKey(tenantProvider, toUnit, fromQtyUnit, productId);
			rate = rate * (mdmFetcher.getCurrencyUnitConversionRate(tenantProvider, fromUnit))
					/ (mdmFetcher.getCurrencyUnitConversionRate(tenantProvider, toUnit));
		}
		if (rate == -1) {
			throw new PricingException(
					messageFetcher.fetchErrorMessage(tenantProvider, "003", new ArrayList<String>()));
		}
		data = data * rate;

		if (qtyFxRate > 1) {
			data = data / qtyFxRate;

			return data;
		}

		String fromQtyUnitKey = mdmFetcher.getQuantityKey(tenantProvider, fromQtyUnit, productId);
		String toQtyUnitKey = mdmFetcher.getQuantityKey(tenantProvider, toQtyUnit, productId);

		qtyFxRate = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productId, fromQtyUnitKey, toQtyUnitKey);
//		double qtyFxRate = qtyRateFetcher.getQtyFx(tenantProvider, unit, qtyUnitList.get(index));
		if (qtyFxRate == 1) {
			logger.info(Logger.EVENT_SUCCESS,
					ESAPI.encoder().encodeForHTML("Qty exchange rate is 1 for " + fromQtyUnit + " to " + toQtyUnit));
		}
		if (curveService.checkZero(qtyFxRate)) {
			throw new PricingException(
					messageFetcher.fetchErrorMessage(tenantProvider, "014", new ArrayList<String>()));
		}
		data = data / qtyFxRate;

		return data;
	}
	
	public List<TieredFields> getFormulaForTiers(ContextProvider tenantProvider, List<CurveDetails> detailsList,
			List<TieredPricingItem> tieredPricingItemList, List<String> currencyList, List<String> qtyUnitList,
			List<String> productIdList, JSONObject itemObj, List<PricingComponent> compList,
			HashMap<String, String> exchangeMap) throws Exception {
		List<TieredFields> tieredFieldsList = new ArrayList<TieredFields>();
		List<TieredPricingItem> currentTieredPricingItemList = new ArrayList<TieredPricingItem>();
		List<TriggerPrice> triggerPriceList = new ArrayList<TriggerPrice>();
		String internalContractItemRefNo = itemObj.optString("internalItemRefNo", "");
		if(StringUtils.isEmpty(internalContractItemRefNo)) {
			internalContractItemRefNo = itemObj.optString("refNo", "");
		}
		triggerPriceList = triggerPriceFetcher.fetchTriggerPriceDetails(tenantProvider, internalContractItemRefNo);
		for (TieredPricingItem tieredPricingItem : tieredPricingItemList) {
			tieredPricingItem.setUsedQtyInGMR(0);
			TieredFields tieredFields = new TieredFields();
			JSONObject tieredObj = new JSONObject();
			double tieredQuantity = 1 + tieredPricingItem.getSplitCeiling() - tieredPricingItem.getSplitFloor();
			List<String> curveNames = new ArrayList<String>();
			JSONObject formulaObj = formFetcher.getFormula(tenantProvider, tieredPricingItem.getFormulaId());
			if (null == formulaObj) {
				throw new PricingException(
						messageFetcher.fetchErrorMessage(tenantProvider, "009", new ArrayList<String>()));
			}
			Formula f = jcon.convertJsonToFormula(formulaObj);
			for (Curve c : f.getCurveList()) {
				curveNames.add(c.getCurveName());
			}
			String expression = f.getFormulaExpression();
			String expressionAfterBuilding = expressionBuilder.buildExpression(curveNames, expression);
			currentTieredPricingItemList.add(tieredPricingItem);
			if(null==compList || compList.isEmpty()) {
				compList = compFetcher.fetchComponent(tenantProvider, itemObj.optString("refNo", ""), null);
				calculateComponent(f.getCurveList(), compList, expressionAfterBuilding, 0, tenantProvider);
				expressionAfterBuilding = expressionBuilder.simplifyExpression(expressionAfterBuilding, compList);
			}
			else if(!compList.isEmpty()) {
				calculateComponent(f.getCurveList(), compList, expressionAfterBuilding, 0, tenantProvider);
				expressionAfterBuilding = expressionBuilder.simplifyExpression(expressionAfterBuilding, compList);
			}
			f.setFormulaExpression(expressionAfterBuilding);
			tieredPricingItem.setFormulaObj(f);
			tieredPricingItem.setCurveList(f.getCurveList());
			double tieredLevelPrice = processTiered(currentTieredPricingItemList, tenantProvider, itemObj, 0, null,
					currencyList, qtyUnitList, productIdList, 0);
			f.setCurveList(qtySetter.setQuanity(expressionAfterBuilding, f.getCurveList(), tieredQuantity, itemObj,
					tenantProvider, exchangeMap, context.getCurrentContext().getAsOfDate(), f.getHolidayRule(),
					compList));
			tieredObj.put("tieredLevelPrice", tieredLevelPrice);
			double pricedPercentage = 0d;
			for (Curve c : f.getCurveList()) {
				double pricedQtyForCurve = c.getPricedQty();
				double unpricedQtyForCurve = c.getUnpricedQty();
				pricedPercentage =pricedPercentage + pricedQtyForCurve * 100 / (pricedQtyForCurve + unpricedQtyForCurve);
			}
			
			/*				in case the expression is purely numbers and no curves are included the contract will be 100% priced.
			 * 				jira id: CPR-729
			 */
			if(f.getCurveList().size()==0) {
				pricedPercentage = 100;
			}
			else {
				pricedPercentage = pricedPercentage/f.getCurveList().size();
			}
			tieredPricingItem.setPricedPercentage(pricedPercentage);
			tieredPricingItem.setUnpricedPercentage(100 - pricedPercentage);
			CurveDetails curveDetails = new CurveDetails();
			curveDetails.setExpression(expressionAfterBuilding);
			curveDetails.setOriginalExp(f.getFormulaExpression());
			curveDetails.setPricePrecision(f.getPricePrecision());
			curveDetails.setCurveList(f.getCurveList());
			curveDetails.setTriggerPriceEnabled(true);
			curveDetails.setTriggerPriceList(triggerPriceList);
			curveDetails.setHolidayRule(f.getHolidayRule());
			curveDetails.setPriceDifferentialList(f.getPriceDifferential());
			tieredFields.setCurveDetails(curveDetails);
			tieredFields.setFormula(f);
			tieredFields.setTieredObj(tieredObj);
			tieredFields.setTieredQuantity(tieredQuantity);
			tieredFieldsList.add(tieredFields);
		}
		return tieredFieldsList;
	}
	
	public double processTieredForFlat(List<TieredPricingItem> tieredPricingItemList, double qty) {
		double price = 0;;
		for(TieredPricingItem tier: tieredPricingItemList) {
			if(qty>tier.getSplitCeiling()) {
				continue;
			}
			price =  tier.getTieredLevelPrice();
		}
		return price;
	}
	
	public TieredPricingItem getTierForFlat(List<TieredPricingItem> tieredPricingItemList, double qty) throws PricingException {
		TieredPricingItem tieredPricingItem = null;
		for(TieredPricingItem tier: tieredPricingItemList) {
			tieredPricingItem = tier;
			if(qty>tier.getSplitCeiling()) {
				continue;
			}
			else if(qty>tier.getSplitFloor() && qty<=tier.getSplitCeiling()) {
				return tier;
			}
		}
		if(null == tieredPricingItem) {
			throw new PricingException("Invalid Tier setup");
		}
		return tieredPricingItem;
	}
	
	public JSONArray sortMovementArr(JSONArray jsonArr) {

		JSONArray sortedJsonArray = new JSONArray();

	    List<JSONObject> jsonValues = new ArrayList<JSONObject>();
	    for (int i = 0; i < jsonArr.length(); i++) {
	        jsonValues.add(jsonArr.getJSONObject(i));
	    }
	    Collections.sort( jsonValues, new Comparator<JSONObject>() {
	        private static final String KEY_NAME = "refNo";

	        @Override
	        public int compare(JSONObject a, JSONObject b) {
	            String valA = new String();
	            String valB = new String();
	            valA = (String) a.get(KEY_NAME);
                valB = (String) b.get(KEY_NAME);
	            return valA.compareTo(valB);
	        }
	    });

	    for (int i = 0; i < jsonArr.length(); i++) {
	        sortedJsonArray.put(jsonValues.get(i));
	    }
	    
	    return sortedJsonArray;
	
	}
	
	public List<JSONObject> gmrTieredFLatExp(List<TieredPricingItem> tieredPricingItemList, String contractType,List<String> productIdList,
			List<GMR> gmrList,JSONObject itemObj,ContextProvider tenantProvider,String contractItemQtyUnit,HashMap<String, String> exchangeMap,
			String quality,List<TieredFields> tieredFieldsList, LocalDate asOfDate, List<JSONObject> curveData,double contractQualityDensity,
			String contractQualityMassUnit,String contractQualityVolumeUnit,List<String> qtyUnitList,String locationType,Contract contract,
			List<TriggerPrice> triggerPriceList) throws Exception  {
		List<JSONObject> previewDataSet =null;
		int index= 0;
		double qty=0.0;
		double quantity=itemObj.getDouble("qty");
		 if(Double.isNaN(quantity) || curveService.checkZero(quantity)) {
			 quantity = itemObj.optDouble("itemQty");
  			itemObj.put("qty",quantity);
  		}
		LocalDateTime asOf = asOfDate.atStartOfDay();
		for(TieredPricingItem tier: tieredPricingItemList) {
			TieredFields tieredFields = tieredFieldsList.get(index);
			JSONObject tieredObj = tieredFields.getTieredObj();
	        CurveDetails curveDetails = tieredFields.getCurveDetails();
			Formula f = tieredFields.getFormula();
			int j=0;
			JSONArray movArray = itemObj.optJSONArray("gmrDetails");
			while (j < movArray.length()) {
			  JSONObject gmrObj = movArray.getJSONObject(j);
			  JSONArray stockArray = gmrObj.optJSONArray("stocks");
				qty=stockArray.getJSONObject(0).optDouble("qty", 0l);
			if((qty > tier.getSplitFloor()) && (qty < tier.getSplitCeiling()+1)) {
				if(!curveDetails.getCurveList().isEmpty() && null!=gmrList) {
					for(Curve c:f.getCurveList()) {
						String exchange = exchangeMap.get(c.getCurveName());
						c.setQty(qty);
						double[] pricedQty = qtySetter.calculatePricedQuantity(c, itemObj, tenantProvider, exchange, asOf, f.getHolidayRule());
						c.setPricedQty(pricedQty[0]);
						c.setUnpricedQty(pricedQty[1]);
					 }
						previewDataSet = getPreviewDataSet(f.getCurveList(), itemObj, tenantProvider, f.getHolidayRule(),
								productIdList.get(0), contractItemQtyUnit, gmrList, f.getFormulaExpression(), contractType, exchangeMap, 
								contractQualityDensity, contractQualityMassUnit, contractQualityVolumeUnit,quality,locationType,contract);
					
					}else {
						itemObj.put("qty",qty);
						   previewDataSet=getAbsoluteExp(itemObj, tenantProvider, f.getHolidayRule(),
										productIdList.get(0), contractItemQtyUnit, gmrList, f.getFormulaExpression(), contractType, exchangeMap,quality,
										contractQualityDensity,contractQualityMassUnit,contractQualityVolumeUnit,qtyUnitList.get(0),locationType,contract);
						   itemObj.put("qty",quantity);
						}
					
				
				
				
			if(null!=previewDataSet) {
				tieredObj.put("curveData", previewDataSet);
				for(int index1 = 0; index1<previewDataSet.size(); index1++) {
						curveData.add(curveFetcher.createTieredExposure(previewDataSet, tieredObj).get(index1));
				}
			}
			
		}
			j++;
			}
			index++;
			
		}
		JSONArray movArray = itemObj.optJSONArray("gmrDetails");
		qty=0.0;
		for(int i=0;i<movArray.length();i++) {
			JSONObject gmrObj = movArray.getJSONObject(i);
			JSONArray stockArray = gmrObj.optJSONArray("stocks");
			qty=qty+stockArray.getJSONObject(0).optDouble("qty", 0l);
			
			}
		 qty=itemObj.getDouble("qty")-qty;
		if(qty!=0) {
			
			int i=0;
			for(TieredPricingItem tier: tieredPricingItemList) {
				TieredFields tieredFields = tieredFieldsList.get(i);
				JSONObject tieredObj = tieredFields.getTieredObj();
		        CurveDetails curveDetails = tieredFields.getCurveDetails();
				Formula f = tieredFields.getFormula();
				
				if((qty > tier.getSplitFloor()) && (qty < tier.getSplitCeiling()+1)) {
					if(!curveDetails.getCurveList().isEmpty() && null!=gmrList) {
						for(Curve c:f.getCurveList()) {
							String exchange = exchangeMap.get(c.getCurveName());
							c.setQty(qty);
							double[] pricedQty = qtySetter.calculatePricedQuantity(c, itemObj, tenantProvider, exchange, asOf, f.getHolidayRule());
							c.setPricedQty(pricedQty[0]);
							c.setUnpricedQty(pricedQty[1]);
						}
							previewDataSet = getPreviewDataSet(f.getCurveList(), itemObj, tenantProvider, f.getHolidayRule(),
									productIdList.get(0), contractItemQtyUnit, gmrList, f.getFormulaExpression(), contractType, exchangeMap,
									contractQualityDensity,contractQualityMassUnit, contractQualityVolumeUnit,quality,locationType,contract);
						 
						}else {
							itemObj.put("qty",qty);
							   previewDataSet=getAbsoluteExp(itemObj, tenantProvider, f.getHolidayRule(),
											productIdList.get(0), contractItemQtyUnit, gmrList, f.getFormulaExpression(), contractType, exchangeMap,quality,
											contractQualityDensity,contractQualityMassUnit,contractQualityVolumeUnit,qtyUnitList.get(0),locationType,contract);
							   itemObj.put("qty",quantity);
							}
						
					
					
				if(null!=previewDataSet) {
					tieredObj.put("curveData", previewDataSet);
					for(int index1 = 0; index1<previewDataSet.size(); index1++) {
							curveData.add(curveFetcher.createTieredExposure(previewDataSet, tieredObj).get(index1));
					}
				}
				
			}
				i++;
			}
			
	}
		
		return curveData;
	}
	
	public List<JSONObject> contractTieredFLatExp(List<TieredPricingItem> tieredPricingItemList, String contractType,List<String> productIdList,
			List<GMR> gmrList,JSONObject itemObj,ContextProvider tenantProvider,String contractItemQtyUnit,HashMap<String, String> exchangeMap,
			String quality,List<TieredFields> tieredFieldsList, LocalDate asOfDate, List<JSONObject> curveData,double contractQualityDensity,
			String contractQualityMassUnit,String contractQualityVolumeUnit,List<String> qtyUnitList,String locationType,Contract contract,
			List<TriggerPrice> triggerPriceList) throws Exception  {
		List<JSONObject> previewDataSet =null;
		int index= 0;
		double quantity=itemObj.optDouble("qty");
		 if(Double.isNaN(quantity) || curveService.checkZero(quantity)) {
			 quantity = itemObj.optDouble("itemQty");
  			itemObj.put("qty",quantity);
  		}
		 
		 String resultString = "";
		 String itemQtyUnit = mdmFetcher.getQuantityKey(null, contractItemQtyUnit, null);
		 String[] baseQtyUnitId = mdmFetcher.getBaseQtyUnit(tenantProvider, productIdList.get(0));
	     double baseQtyConversionFactor = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productIdList.get(0), itemQtyUnit,baseQtyUnitId[0]);
		 int j=0;
		 int k=0;
		LocalDateTime asOf = asOfDate.atStartOfDay();
		JSONArray movArray = itemObj.optJSONArray("gmrDetails");
		if (null != movArray) {
		  while (j < movArray.length()) {
		   JSONObject gmrObj = movArray.getJSONObject(j);
		   JSONArray stockArray = gmrObj.optJSONArray("stocks");
			if (stockArray != null && stockArray.length() > 0) {
				while (k < stockArray.length()) {
					JSONObject stockObj = stockArray.getJSONObject(k);
					String stockRefNo = stockObj.optString("refNo");
					double stockQty = stockObj.optDouble("qty", 0l);
					for (GMR gmr : gmrList) {
						for (Stock st : gmr.getStocks()) {
							if (st.getRefNo().equals(stockRefNo)) {
								st.setQty(stockQty);
								}
						    }
						}
					
					k++;
				}
			  }
			j++;
		  }
		}
		
		for(TieredPricingItem tier: tieredPricingItemList) {
			TieredFields tieredFields = tieredFieldsList.get(index);
			JSONObject tieredObj = tieredFields.getTieredObj();
	        CurveDetails curveDetails = tieredFields.getCurveDetails();
			Formula f = tieredFields.getFormula();
			if((quantity > tier.getSplitFloor()) && (quantity < tier.getSplitCeiling()+1)) {
				resultString= String.valueOf(tier.getTieredLevelPrice());
				double precisedPrice = tieredPriceValue(resultString, itemObj,curveDetails,productIdList,qtyUnitList,contractItemQtyUnit, 
						baseQtyConversionFactor, quality, quantity, tenantProvider);
				tier.setTieredLevelPrice(precisedPrice);
				if(!curveDetails.getCurveList().isEmpty()) {
					for(Curve c:f.getCurveList()) {
						String exchange = exchangeMap.get(c.getCurveName());
						c.setQty(quantity);
						double[] pricedQty = qtySetter.calculatePricedQuantity(c, itemObj, tenantProvider, exchange, asOf, f.getHolidayRule());
						c.setPricedQty(pricedQty[0]);
						c.setUnpricedQty(pricedQty[1]);
					 }
					
					previewDataSet = getPreviewDataSet(f.getCurveList(), itemObj, tenantProvider, f.getHolidayRule(),
							productIdList.get(0), contractItemQtyUnit, gmrList, f.getFormulaExpression(), contractType, exchangeMap,
							contractQualityDensity,contractQualityMassUnit, contractQualityVolumeUnit,quality,locationType,contract);
					
					
					}else {
						itemObj.put("qty",quantity);
						   previewDataSet=getAbsoluteExp(itemObj, tenantProvider, f.getHolidayRule(),
										productIdList.get(0), contractItemQtyUnit, gmrList, f.getFormulaExpression(), contractType, exchangeMap,quality,
										contractQualityDensity,contractQualityMassUnit,contractQualityVolumeUnit,qtyUnitList.get(0),locationType,contract);
						   itemObj.put("qty",quantity);
						}
				
			if(null!=previewDataSet) {
				tieredObj.put("curveData", previewDataSet);
				for(int index1 = 0; index1<previewDataSet.size(); index1++) {
						curveData.add(curveFetcher.createTieredExposure(previewDataSet, tieredObj).get(index1));
				}
			}
			
		}
			index++;
			
		}
		
		return curveData;
	}
	
	public double tieredPriceValue(String resultString,JSONObject itemObj,CurveDetails curveDetails,List<String> productIdList,List<String> qtyUnitList,
			String contractItemQtyUnit,double baseQtyConversionFactor,String quality,double quantity,ContextProvider tenantProvider) throws Exception {
		String precision = null;
		try {
			precision = itemObj.getString("pricePrecision");
		} catch (Exception e) {
			precision = curveDetails.getPricePrecision();
		}
		TriggerPriceProperties triggerProps = new TriggerPriceProperties();
		triggerProps.setGmr(null);
		triggerProps.setHolidayRule(curveDetails.getHolidayRule());
		triggerProps.setItemObj(itemObj);
		triggerProps.setPrecision(precision);
		triggerProps.setProductIdList(productIdList);
		triggerProps.setQtyUnitList(qtyUnitList);
		triggerProps.setContractItemQty(contractItemQtyUnit);
		triggerProps.setQuality(quality);
		triggerProps.setBaseQtyUnitConversion(baseQtyConversionFactor);
		String qty1 = Double.toString(quantity);
		resultString = evaluateTriggerPrice(curveDetails, tenantProvider, productIdList.get(0),
					qtyUnitList.get(0), qty1, resultString, triggerProps);
		
		double precisedPrice = Double.parseDouble(resultString);
		return precisedPrice;
		
	}
	
	public List<JSONObject> expFLatExpForTriggerPrice(List<TieredPricingItem> tieredPricingItemList, String contractType,List<String> productIdList,
			List<GMR> gmrList,JSONObject itemObj,ContextProvider tenantProvider,String contractItemQtyUnit,HashMap<String, String> exchangeMap,
			String quality,List<TieredFields> tieredFieldsList, LocalDate asOfDate, List<JSONObject> curveData,List<String> qtyUnitList,
			double contractQualityDensity,String contractQualityMassUnit,String contractQualityVolumeUnit) throws Exception  {
		List<JSONObject> previewDataSet =null;
		String resultString = null;
		int index= 0;
		double qty=0.0;
		double quantity = itemObj.optDouble("qty");
		 if(Double.isNaN(quantity) || curveService.checkZero(quantity)) {
			 quantity = itemObj.optDouble("itemQty");
  			itemObj.put("qty",quantity);
  		}
		 String precision = null;
		 String itemQtyUnit = mdmFetcher.getQuantityKey(null, contractItemQtyUnit, null);
		 String[] baseQtyUnitId = mdmFetcher.getBaseQtyUnit(tenantProvider, productIdList.get(0));
	     double baseQtyConversionFactor = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productIdList.get(0), itemQtyUnit,baseQtyUnitId[0]);
		LocalDateTime asOf = asOfDate.atStartOfDay();
		for(TieredPricingItem tier: tieredPricingItemList) {
			TieredFields tieredFields = tieredFieldsList.get(index);
			JSONObject tieredObj = tieredFields.getTieredObj();
	        CurveDetails curveDetails = tieredFields.getCurveDetails();
			Formula f = tieredFields.getFormula();
			int j=0;
			List<TriggerPrice> triggerList = curveDetails.getTriggerPriceList();
				qty=triggerList.get(0).getQuantity();
			if((qty > tier.getSplitFloor()) && (qty < tier.getSplitCeiling()+1)) {
				if(!curveDetails.getCurveList().isEmpty()) {
					for(Curve c:f.getCurveList()) {
						String exchange = exchangeMap.get(c.getCurveName());
						c.setQty(quantity);
						double[] pricedQty = qtySetter.calculatePricedQuantity(c, itemObj, tenantProvider, exchange, asOf, f.getHolidayRule());
						c.setPricedQty(pricedQty[0]);
						c.setUnpricedQty(pricedQty[1]);
											
					 }
					try {
						precision = itemObj.getString("pricePrecision");
					} catch (Exception e) {
						precision = curveDetails.getPricePrecision();
					}
					
					TriggerPriceProperties triggerProps = new TriggerPriceProperties();
					triggerProps.setGmr(null);
					triggerProps.setHolidayRule(curveDetails.getHolidayRule());
					triggerProps.setItemObj(itemObj);
					triggerProps.setPrecision(precision);
					triggerProps.setProductIdList(productIdList);
					triggerProps.setQtyUnitList(qtyUnitList);
					triggerProps.setContractItemQty(contractItemQtyUnit);
					triggerProps.setQuality(quality);
					triggerProps.setBaseQtyUnitConversion(baseQtyConversionFactor);
					String qty1 = Double.toString(quantity);
					resultString = evaluateTriggerPrice(curveDetails, tenantProvider, productIdList.get(0),
							qtyUnitList.get(0), qty1, resultString, triggerProps);
					
						/*previewDataSet = getPreviewDataSet(f.getCurveList(), itemObj, tenantProvider, f.getHolidayRule(),
								productIdList.get(0), contractItemQtyUnit, gmrList, f.getFormulaExpression(), contractType, exchangeMap,
								contractQualityDensity, contractQualityMassUnit, contractQualityVolumeUnit,quality);*/
					
					
					}else {
						TriggerPriceProperties triggerProps = new TriggerPriceProperties();
						triggerProps.setGmr(null);
						triggerProps.setHolidayRule(curveDetails.getHolidayRule());
						triggerProps.setItemObj(itemObj);
						triggerProps.setPrecision(precision);
						triggerProps.setProductIdList(productIdList);
						triggerProps.setQtyUnitList(qtyUnitList);
						triggerProps.setContractItemQty(contractItemQtyUnit);
						triggerProps.setQuality(quality);
						triggerProps.setBaseQtyUnitConversion(baseQtyConversionFactor);
						String qty1 = Double.toString(quantity);
						resultString = evaluateTriggerPrice(curveDetails, tenantProvider, productIdList.get(0),
								qtyUnitList.get(0), qty1, resultString, triggerProps);
						/*previewDataSet=getAbsoluteExpForTriggerPrice(itemObj,curveDetails, tenantProvider, f.getHolidayRule(),
								productIdList.get(0), contractItemQtyUnit, gmrList, f.getFormulaExpression(), contractType, exchangeMap,quality,
								contractQualityDensity,contractQualityMassUnit,contractQualityVolumeUnit,qtyUnitList.get(0));*/
						}
				
			if(null!=previewDataSet) {
				tieredObj.put("curveData", previewDataSet);
				for(int index1 = 0; index1<previewDataSet.size(); index1++) {
						curveData.add(curveFetcher.createTieredExposure(previewDataSet, tieredObj).get(index1));
				}
			}
			
		}
			index++;
			
		}
		
		return curveData;
	}
	public void checkForGMROperation(Contract contract) throws Exception {
		List<GMR> gmrList = contract.getItemDetails().get(0).getGmrDetails();
		List<GMR> cancelledGMR = new ArrayList<GMR>();
		if(null!=gmrList) {
			for(GMR gmr : gmrList) {
				if(StringUtils.isEmpty(gmr.getOperationType())) {
					continue;
				}
				String gmrOperation = gmr.getOperationType().toLowerCase();
				if(null == gmr.getStocks() || gmr.getStocks().isEmpty() || gmrOperation.equals("cancel")) {
					cancelledGMR.add(gmr);
					gmrCreationHelper.deleteGMR(gmr.getRefNo(), contract.getItemDetails().get(0).getRefNo());
					gmrCreationHelper.deleteGMRConvFactor(gmr.getRefNo(), contract.getItemDetails().get(0).getRefNo());
				}
				if(gmrOperation.equals("modify") || gmrOperation.equals("cancel")) {
					try {
						gmrModificationHelper.modifyGMR(gmr, contract.getItemDetails().get(0).getRefNo());
					} catch (PricingException e) {
						// TODO Auto-generated catch block
						logger.error(Logger.EVENT_FAILURE, ESAPI.encoder()
								.encodeForHTML("Exception while processing gmr modification"),e);
						throw new PricingException("Exception while processing gmr modification");
					}
				}
			}
		}
		for(GMR gmr: cancelledGMR) {
			gmrList.remove(gmr);
		}
		
	}
	
	@SuppressWarnings("unchecked")
	private List<JSONObject> getQualityCurveDetailsForGMR(JSONObject jobj, int i, LocalDate asOfDate, ContextProvider tenantProvider,  String productId,
			String contractItemQty, List<GMR> gmrList,double contractQualityDensity,String contractQualityMassUnit, String contractQualityVolumeUnit
			,String quality,double totalPricedQty, double totalUnPricedQty,List<JSONObject> previewDataSet,String contractType,
			JSONObject curveObj,Curve c) throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String fromDate1= jobj.optString("deliveryFromDate", "");
		String toDate1 = jobj.optString("deliveryToDate", "");
		LocalDateTime fromDate = sdf.parse(fromDate1).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
		LocalDateTime toDate = sdf.parse(toDate1).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
		Map<String, JSONArray> mapExpCallArray = new HashMap<String,JSONArray>();
		double qty = jobj.optDouble("qty");
		if(Double.isNaN(qty) || curveService.checkZero(qty)) {
			qty = jobj.optDouble("itemQty");
			jobj.put("qty",qty);
		}
		String internalContractItemRefNo=jobj.optString("refNo", "");
		if(StringUtils.isEmpty(internalContractItemRefNo)) {
			internalContractItemRefNo=jobj.optString("internalItemRefNo", "");
		}
		String qualityName=null;
		String[] qualityArr = mdmFetcher.getQualityExchangeUnit(tenantProvider,quality);
		String curveName=qualityArr[0];
		String exchange=qualityArr[3];
		String valuationPriceUnit=qualityArr[1];
		String status=qualityArr[4];
		String remarks=qualityArr[5];
		Map<String , JSONArray> mapExpArr=getExpiryArr(curveName, tenantProvider,mapExpCallArray);
		JSONArray expArr = mapExpArr.get(curveName);
		
		String curveQtyUnit="";
		String currency="";
		String gmrCurveQtyUnit="";
		String gmrCurrency="";
		if(!valuationPriceUnit.isEmpty()) {
			if (valuationPriceUnit.contains("/")) {
				curveQtyUnit = valuationPriceUnit.substring(valuationPriceUnit.indexOf("/") + 1, valuationPriceUnit.length()).trim();
				currency = valuationPriceUnit.substring(0, valuationPriceUnit.indexOf("/"));
			}
		}
		double contractualConversionFactor=0;
		double actualConversionFactorGMR=0;
		Map<String, GMRQualityDetails> gmrQuality = new HashMap<String,GMRQualityDetails>();
		Map<String, Double> gmrToItemConversionFactor = new HashMap<String,Double>();
		String itemQtyUnit = mdmFetcher.getQuantityKey(null, contractItemQty, null);
		String CurveQtyUnitID = mdmFetcher.getQuantityKey(null, curveQtyUnit, null);
		String baseQtyUnit = curveObj.optString("baseQtyUnit");
		String itemQtyUnitCheck=massToVolumeConversion.getMassVolumeQtyCheck(productId,itemQtyUnit);
		double baseContractualConversionFactor = curveObj.optDouble("baseContractualConversionFactor");
	    if(contractQualityDensity!=0.0) {
		 contractualConversionFactor=curveFetcher.contractualConversionFactor(productId,itemQtyUnit,CurveQtyUnitID, contractQualityDensity, contractQualityMassUnit,
				 contractQualityVolumeUnit, tenantProvider);
		
		}else {
			contractualConversionFactor = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productId, itemQtyUnit,
				CurveQtyUnitID);
		}
	    
		JSONArray exposureArray = new JSONArray();
		JSONArray newerExposureArr = new JSONArray();
		JSONArray newQtyData = new JSONArray();
		String gmrRefNo=null;
		if(gmrList.size()>0) {
			double totalGmrQty=curveObj.optDouble("totalGmrQty");
			logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("Total GMR Qty")+totalGmrQty);
			String str=curveObj.opt("gmrToItemConversionFactor").toString();
			ObjectMapper oMapper = new ObjectMapper();
			gmrToItemConversionFactor= oMapper.readValue(str, Map.class);
			
				for (GMR gmr1 : gmrList) {
				  for (Stock st : gmr1.getStocks()) {
				   if (st.getGMRRefNo().equals(gmr1.getRefNo())) {
					  qualityName=st.getQuality();
					  if(null!=qualityName) {
							quality=mdmFetcher.getQualityUnitId(productId, qualityName);
						}
					}
					
				  }
				  if(quality.length()>0) {
					  qualityArr = mdmFetcher.getQualityExchangeUnit(tenantProvider,quality);
						 valuationPriceUnit=qualityArr[1];
						if(!valuationPriceUnit.isEmpty()) {
							if (valuationPriceUnit.contains("/")) {
								gmrCurveQtyUnit = valuationPriceUnit.substring(valuationPriceUnit.indexOf("/") + 1, valuationPriceUnit.length()).trim();
								gmrCurrency = valuationPriceUnit.substring(0, valuationPriceUnit.indexOf("/"));
							}
						}
				  }
					
					CurveQtyUnitID = mdmFetcher.getQuantityKey(null, gmrCurveQtyUnit, null);
					String CurveQtyUnitCheck=massToVolumeConversion.getMassVolumeQtyCheck(productId,CurveQtyUnitID);
					actualConversionFactorGMR=curveFetcher.gmrActualDensityVolumeConversion(gmr1,jobj,productId,tenantProvider,itemQtyUnit,CurveQtyUnitID,
							contractQualityDensity, contractQualityMassUnit, contractQualityVolumeUnit,totalGmrQty,contractItemQty,gmrCurveQtyUnit,
							i+1,true,gmrToItemConversionFactor,gmr1.getRefNo(),itemQtyUnitCheck,CurveQtyUnitCheck);
					GMRQualityDetails gmrQualityDetails = new GMRQualityDetails();
					gmrQualityDetails.setActualConversionFactorGMR(actualConversionFactorGMR);
					gmrQualityDetails.setCurveName(qualityArr[0]);
					gmrQualityDetails.setCurveQty(gmrCurveQtyUnit);
					gmrQualityDetails.setQuality(qualityName);
					gmrQualityDetails.setCurveQtyUnitId(CurveQtyUnitID);
					gmrQualityDetails.setCurveQtyUnitCheck(CurveQtyUnitCheck);
					mapExpArr=getExpiryArr(qualityArr[0], tenantProvider,mapExpCallArray);
					gmrQuality.put(gmr1.getRefNo(), gmrQualityDetails);
			   }
				
					gmrCreationHelper.updateGMRWeightedAvgConversionFactor(tenantProvider,internalContractItemRefNo,
							itemQtyUnit, contractItemQty,gmrList,i+1,itemQtyUnitCheck,gmrToItemConversionFactor,gmrQuality);
				
					 JSONArray qtyData = new JSONArray();
					 JSONArray qtyData1 = new JSONArray();
					 JSONArray exposureArray1 = new JSONArray();
					 qtyData= curveObj.optJSONArray("qtyData");
					 exposureArray = curveObj.optJSONArray("exposureArray");
					 if (exposureArray != null && exposureArray.length() > 0 && contractType!=null) {
							for(int k=0;k < exposureArray.length();k++) {
								JSONObject exposureObj = exposureArray.getJSONObject(k);
								JSONObject qtyObj = qtyData.getJSONObject(k);
								double priceQty = qtyObj.optDouble("pricedQty", 0l);
								double priceBaseQty = qtyObj.optDouble("pricedQuantityInBaseQtyUnit", 0l);
								String riskType = exposureObj.getString("riskType");
								String exposureType = exposureObj.getString("exposureType");
								String exposureSubType = exposureObj.getString("exposureSubType");
								double contractualConversionFactor1 = qtyObj.optDouble("contractualConversionFactor", 0l);
								double actualConversionFactor1 = qtyObj.optDouble("actualConversionFactor", 0l);
								String date = qtyObj.optString("date");
								LocalDate pricingDate = sdf.parse(date).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
								
								//For Market valuation Curve
								if(priceQty!=0) {
									if(null!=qtyObj.optString("gmrRefNo")) {
										gmrRefNo=qtyObj.optString("gmrRefNo");
									}
									if(priceQty<0 && null!= contractType && contractType.equals("P")) {
										qtyObj.put("pricedQty", priceQty*-1);
										qtyObj.put("pricedQuantityInBaseQtyUnit", priceBaseQty*-1);
									}
									if(priceQty>0 && null!= contractType && contractType.equals("S")) {
										qtyObj.put("pricedQty", priceQty*-1);
										qtyObj.put("pricedQuantityInBaseQtyUnit", priceBaseQty*-1);
									}
									qtyObj.put("riskType", riskType);
									qtyObj.put("exposureType", exposureType);
									qtyObj.put("exposureSubType", exposureSubType);
									GMRQualityDetails gmrQualityDetails=gmrQuality.get(gmrRefNo);
									if(actualConversionFactor1!=0) {
										qtyObj.put("actualConversionFactor", gmrQualityDetails.getActualConversionFactorGMR());
										qtyObj.put("contractualConversionFactor", contractualConversionFactor1);
										qtyObj.put("quality", gmrQualityDetails.getQuality());
										qtyObj.put("curveName", gmrQualityDetails.getCurveName());
										qtyObj.put("curveQtyUnit", gmrQualityDetails.getCurveQty());
										JSONArray expArray = mapExpArr.get(gmrQualityDetails.getCurveName());
										String monthYear=getMonthYearForDate(pricingDate, expArray);
										qtyObj.put("instrumentDeliveryMonth", monthYear);
									}else {
										String monthYear=getMonthYearForDate(pricingDate, expArr);
										qtyObj.put("actualConversionFactor", actualConversionFactor1);
										qtyObj.put("contractualConversionFactor", contractualConversionFactor);
										qtyObj.put("instrumentDeliveryMonth", monthYear);
									}
									
									newQtyData.put(qtyObj);
								}else {
									// For Normal Curve
									qtyData1.put(qtyObj);
									exposureArray1.put(exposureObj);
								}
							}
					    }
					    double remainigQty= qty-totalGmrQty;
					    if(remainigQty>0 && !c.getPriceQuoteRule().equals("Event Offset Based")) {
					    	JSONObject exposureObj = new JSONObject();
							exposureObj.put("date", asOfDate);
							if(null!= contractType && contractType.equals("S")) {
								exposureObj.put("pricedQty", remainigQty*-1);
								exposureObj.put("pricedQuantityInBaseQtyUnit", remainigQty*baseContractualConversionFactor*-1);
								exposureObj.put("riskType", NORISK);
								exposureObj.put("exposureType", INVENTORY);
								exposureObj.put("exposureSubType", PRICEDSTOCKSDELIVERED);
							}else {
								exposureObj.put("pricedQty", remainigQty);
								exposureObj.put("pricedQuantityInBaseQtyUnit", remainigQty*baseContractualConversionFactor);
								exposureObj.accumulate("riskType", NORISK);
								exposureObj.accumulate("exposureType", INVENTORY);
								exposureObj.accumulate("exposureSubType", PRICEDSTOCKSRECEIVED);
							}
							exposureObj.put("unPricedQty", 0);
							exposureObj.accumulate("unpricedQuantityInBaseQtyUnit", 0);
							
							String monthYear=getMonthYearForDate(asOfDate, expArr);
							exposureObj.put("instrumentDeliveryMonth", monthYear);
							exposureObj.accumulate("pricedPercentage", (remainigQty*100)/qty);
							exposureObj.accumulate("unpricedPercentage", 0);
							exposureObj.accumulate("actualConversionFactor", 0);
							exposureObj.accumulate("contractualConversionFactor", contractualConversionFactor);
							exposureObj.put("gmrRefNo", "Outturn Loss");
							newQtyData.put(exposureObj);
					    }
					 curveObj.remove("qtyData");
					 curveObj.remove("exposureArray");
					 curveObj.put("qtyData", qtyData1);
					 curveObj.put("exposureArray", exposureArray1);
			}
		
		for (int expIndex = 0; expIndex < newQtyData.length(); expIndex++) {
			JSONObject exposureObject = new JSONObject(newQtyData.getJSONObject(expIndex),
					JSONObject.getNames(newQtyData.getJSONObject(expIndex)));
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
		
		PreviewData previewData = new PreviewData();
		previewData.setCollapse("0");
		previewData.setExchange(exchange);
		previewData.setCurveName(curveName);
		previewData.setCurvePrice(0);
		previewData.setCurveCurrency(currency);
		previewData.setCoefficient(1);
		previewData.setCurveQtyUnit(curveQtyUnit);
		previewData.setQtyUnit(contractItemQty);
		previewData.setBaseQtyUnit(baseQtyUnit);
		previewData.setPriceUnit(currency + "/" + curveQtyUnit);
		previewData.setQpStartDate(collectionDataFetcher.constructDateStr(fromDate.toLocalDate()));
		previewData.setQpEndDate(collectionDataFetcher.constructDateStr(toDate.toLocalDate()));
		previewData.setData(new JSONArray());
		previewData.setPricedQty(totalPricedQty);
		previewData.setUnPricedQty(totalUnPricedQty);
		previewData.setStatus(status);
		previewData.setRemarks(remarks);
		previewData.setValuationInstrument("Physical");
		previewData.setQtyData(newQtyData);
		previewData.setExposureArray(newerExposureArr);
		previewDataSet.add(new JSONObject(previewData));
		return previewDataSet;
	}
	
	private List<JSONObject> getQualityCurveDetailsForContract(JSONObject jobj, int i, LocalDate asOfDate, ContextProvider tenantProvider,  String productId,
			String contractItemQty, List<GMR> gmrList,double contractQualityDensity,String contractQualityMassUnit, String contractQualityVolumeUnit
			,String quality,List<JSONObject> previewDataSet,String contractType, String baseQtyUnit,double baseContractualConversionFactor) throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String fromDate1= jobj.optString("deliveryFromDate", "");
		String toDate1 = jobj.optString("deliveryToDate", "");
		LocalDateTime fromDate = sdf.parse(fromDate1).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
		LocalDateTime toDate = sdf.parse(toDate1).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
		Map<String, JSONArray> mapExpCallArray = new HashMap<String,JSONArray>();
		double qty = jobj.optDouble("qty");
		if(Double.isNaN(qty) || curveService.checkZero(qty)) {
			qty = jobj.optDouble("itemQty");
			jobj.put("qty",qty);
		}
		double totalPricedQty=qty;
		double totalUnPricedQty=0;
		String internalContractItemRefNo=jobj.optString("refNo", "");
		if(StringUtils.isEmpty(internalContractItemRefNo)) {
			internalContractItemRefNo=jobj.optString("internalItemRefNo", "");
		}
		String[] qualityArr = mdmFetcher.getQualityExchangeUnit(tenantProvider,quality);
		String curveName=qualityArr[0];
		String exchange=qualityArr[3];
		String valuationPriceUnit=qualityArr[1];
		String status=qualityArr[4];
		String remarks=qualityArr[5];
		Map<String , JSONArray> mapExpArr=getExpiryArr(curveName, tenantProvider,mapExpCallArray);
		JSONArray expArr = mapExpArr.get(curveName);
		
		String curveQtyUnit="";
		String currency="";
		if(!valuationPriceUnit.isEmpty()) {
			if (valuationPriceUnit.contains("/")) {
				curveQtyUnit = valuationPriceUnit.substring(valuationPriceUnit.indexOf("/") + 1, valuationPriceUnit.length()).trim();
				currency = valuationPriceUnit.substring(0, valuationPriceUnit.indexOf("/"));
			}
		}
		double contractualConversionFactor=0;
		String itemQtyUnit = mdmFetcher.getQuantityKey(null, contractItemQty, null);
		String CurveQtyUnitID = mdmFetcher.getQuantityKey(null, curveQtyUnit, null);
	    if(contractQualityDensity!=0.0) {
		 contractualConversionFactor=curveFetcher.contractualConversionFactor(productId,itemQtyUnit,CurveQtyUnitID, contractQualityDensity, contractQualityMassUnit,
				 contractQualityVolumeUnit, tenantProvider);
		
		}else {
			contractualConversionFactor = mdmFetcher.getQtyUnitConversionRate(tenantProvider, productId, itemQtyUnit,
				CurveQtyUnitID);
		}
	    
		JSONArray newerExposureArr = new JSONArray();
		JSONArray newQtyData = new JSONArray();
				LocalDate date=asOfDate;
			
				JSONObject exposureObj = new JSONObject();
				exposureObj.put("date", date);
				if(null!= contractType && contractType.equals("S")) {
						exposureObj.put("pricedQty", qty *-1);
						exposureObj.put("unPricedQty", 0);
						exposureObj.accumulate("pricedQuantityInBaseQtyUnit", qty*baseContractualConversionFactor*-1);
					
				}else {
					exposureObj.put("pricedQty", qty);
					exposureObj.put("unPricedQty", 0);
					exposureObj.accumulate("pricedQuantityInBaseQtyUnit", qty*baseContractualConversionFactor);
				}
				exposureObj.accumulate("unpricedQuantityInBaseQtyUnit",  0);
				String monthYear=getMonthYearForDate(date, expArr);
				exposureObj.put("instrumentDeliveryMonth", monthYear);
				exposureObj.put("contractualConversionFactor", contractualConversionFactor);
				exposureObj.put("actualConversionFactor", 0);
				exposureObj.put("pricedPercentage", (totalPricedQty /qty) * 100);
				exposureObj.put("unpricedPercentage", (totalUnPricedQty / qty) * 100);
	           newQtyData.put(exposureObj);
		
			 JSONObject exposureObject = new JSONObject(newQtyData.getJSONObject(0),
						JSONObject.getNames(newQtyData.getJSONObject(0)));
				exposureObject.put("pricedQty", exposureObject.optDouble("pricedQty", 0) * contractualConversionFactor);
				exposureObject.put("unPricedQty", exposureObject.optDouble("unPricedQty", 0) * contractualConversionFactor);
			    newerExposureArr.put(exposureObject);
			    
		PreviewData previewData = new PreviewData();
		previewData.setCollapse("0");
		previewData.setExchange(exchange);
		previewData.setCurveName(curveName);
		previewData.setCurvePrice(0);
		previewData.setCurveCurrency(currency);
		previewData.setCoefficient(1);
		previewData.setCurveQtyUnit(curveQtyUnit);
		previewData.setQtyUnit(contractItemQty);
		previewData.setBaseQtyUnit(baseQtyUnit);
		previewData.setPriceUnit(currency + "/" + curveQtyUnit);
		previewData.setQpStartDate(collectionDataFetcher.constructDateStr(fromDate.toLocalDate()));
		previewData.setQpEndDate(collectionDataFetcher.constructDateStr(toDate.toLocalDate()));
		previewData.setData(new JSONArray());
		previewData.setPricedQty(totalPricedQty);
		previewData.setUnPricedQty(totalUnPricedQty);
		previewData.setStatus(status);
		previewData.setRemarks(remarks);
		previewData.setValuationInstrument("Physical");
		previewData.setQtyData(newQtyData);
		previewData.setExposureArray(newerExposureArr);
		previewDataSet.add(new JSONObject(previewData));
		return previewDataSet;
	}
	public List<GMR> gmrListFromConnect(Contract contract, String contractItemQty,JSONObject jobj,String productId,List<GMR> gmrList) throws Exception{
		
		String internalContractItemRefNo = contract.getItemDetails().get(0).getInternalItemRefNo();
		if(null==internalContractItemRefNo) {
			internalContractItemRefNo=contract.getItemDetails().get(0).getRefNo();
		}
		List <String> list = new ArrayList<>();
		if(gmrList.size()>0) {
			GMR gmr=gmrList.get(0);
			for (Stock st : gmr.getStocks()) {
				list.add(st.getGMRRefNo());
			}
		}
		JSONArray gmrDataArr = gmrCreationHelper.fetchGMRData(internalContractItemRefNo);
		List<GMR> gmrListFromConnect = new ArrayList<GMR>();
		List<Stock> stockList = new ArrayList<Stock>();
				for (int gmrInd = 0; gmrInd < gmrDataArr.length(); gmrInd++) {
					JSONObject gmrData = gmrDataArr.optJSONObject(gmrInd);
					String payload = gmrData.optString("inputPayload");
					JSONObject gmrObj =null;
					if(null!=payload && !payload.isEmpty()) {
						  gmrObj = new JSONObject(payload);
						  String gmrRef = gmrObj.optString("refNo", "");
						  if(!list.contains(gmrRef)) {
							String titleTransferStatus = gmrObj.optString("titleTransferStatus", "");
							String storageLocation = gmrObj.optString("storageLocation", "");
							String loadingLocType = gmrObj.optString("loadingLocType", "");
							String loadingLocName = gmrObj.optString("loadingLocName", "");
							String destinationLocType = gmrObj.optString("destinationLocType", "");
							String destinationLocName = gmrObj.optString("destinationLocName", "");
							String vesselName = gmrObj.optString("vesselName", "");
							GMR gmr = new GMR();
							gmr.setRefNo(gmrRef);
							gmr.setTitleTransferStatus(titleTransferStatus);
							gmr.setStorageLocation(storageLocation);
							gmr.setLoadingLocType(loadingLocType);
							gmr.setLoadingLocName(loadingLocName);
							gmr.setDestinationLocType(destinationLocType);
							gmr.setDestinationLocName(destinationLocName);
							gmr.setVesselName(vesselName);
							
							JSONArray stockArray = gmrObj.optJSONArray("stocks");
							int k = 0;
							if (stockArray != null && stockArray.length() > 0) {
								while (k < stockArray.length()) {
									JSONObject stockObj = stockArray.getJSONObject(k);
									Stock stock = new Stock();
									if(!StringUtils.isEmpty(gmrObj.optString("gmrCreationDate"))) {
										stock.setGmrCreationDate(gmrObj.optString("gmrCreationDate"));
									}
									String stockRefNo = stockObj.optString("refNo");
									String contractItemRefNo = jobj.optString("refNo");
									double QtyConRate =0;
									double stockQty = stockObj.optDouble("qty", 0l);
									double stockQtyInGMR = stockQty;
									if(null==gmrList || gmrList.size()==0) {
										String itmeQtyUnitId = mdmFetcher.getQuantityKey(context, contractItemQty,
										productId);
									    QtyConRate = mdmFetcher.getQtyUnitConversionRate(context, productId,
										stockObj.optString("qtyUnit"), itmeQtyUnitId);
										if(!curveService.checkZero(stock.getMassToVolConversionFactor())) {
											QtyConRate = stock.getMassToVolConversionFactor();
										}
										stockQty = stockQty * QtyConRate;
									}
									List<Event> eventList = new ArrayList<Event>();
									JSONArray eventArr = stockObj.optJSONArray("event");
									int eventInd = 0;
									while (eventInd < eventArr.length()) {
										JSONObject eventObject = eventArr.getJSONObject(eventInd);
										Event e = new Event();
										e.setName(eventObject.optString("name", ""));
										e.setDate(eventObject.optString("date"));
										eventList.add(e);
										eventInd++;
									}
									gmr.setRefNo(gmrRef);
									gmr.setStockRef(stockRefNo);
									gmr.setEvent(eventList);
									
									stock.setRefNo(stockRefNo);
									stock.setGMRRefNo(gmrRef);
									stock.setContractRefNo(contract.getRefNo());
									stock.setContractItemRefNo(contractItemRefNo);
									stock.setQty(stockQty); // Stock Qty in Item Qty unit
									stock.setStockQtyInGmr(stockQtyInGMR); // New field for GMR Qty
									stock.setQtyUnit(stockObj.optString("qtyUnit"));
									stock.setQtyUnitId(stockObj.optString("qtyUnit"));
									stock.setDensityValue(stockObj.optDouble("densityValue"));
									stock.setMassUnitId(stockObj.optString("massUnitId"));
									stock.setVolumeUnitId(stockObj.optString("volumeUnitId"));
									stock.setMassToVolConversionFactor(stockObj.optDouble("massToVolConversionFactor"));
									stock.setDensityVolumeQtyUnitId(stockObj.optString("densityVolumeQtyUnitId"));
									stock.setQuality(stockObj.optString("quality"));
									stock.setItemQtyUnit(contractItemQty);
									stock.setQtyConversionRate(QtyConRate);
									stockList.add(stock);
									k++;
								}
							}
							gmr.setStocks(stockList);
							gmrListFromConnect.add(gmr);
					    }
				     }
		         }
				return gmrListFromConnect;
	        }
	
	
	public Map<String , JSONArray> getExpiryArr(String curveName,ContextProvider tenantProvider,Map<String, JSONArray> mapExpCallArray)throws Exception{
		JSONArray expiryArr=null;
		if(mapExpCallArray.get(curveName)==null) {
			Curve c = new Curve();
			c.setCurveName(curveName);
			expiryArr=expiryCalenderFetcher.getData(c, tenantProvider);
			mapExpCallArray.put(c.getCurveName(), expiryArr);
		}
		return mapExpCallArray;
	}
	
	public String getMonthYearForDate(LocalDate pricingDate, JSONArray expCal) throws PricingException {
		DateTimeFormatter expiryDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
		String monthYear = "";
		if(null!=expCal && expCal.length()>0) {
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
			
		if (StringUtils.isEmpty(monthYear)) {
			String month = pricingDate.getMonth().toString().substring(0, 3);
			String year = Integer.toString(pricingDate.getYear());
			monthYear=month+year;
		}
			return monthYear;
	  }
	
}