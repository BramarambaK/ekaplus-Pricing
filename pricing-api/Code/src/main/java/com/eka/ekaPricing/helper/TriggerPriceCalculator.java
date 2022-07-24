package com.eka.ekaPricing.helper;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.eka.ekaPricing.exception.PricingException;
import com.eka.ekaPricing.pojo.Curve;
import com.eka.ekaPricing.pojo.CurveCalculatorFields;
import com.eka.ekaPricing.pojo.CurveDetails;
import com.eka.ekaPricing.pojo.FixationObject;
import com.eka.ekaPricing.pojo.Formula;
import com.eka.ekaPricing.pojo.GMR;
import com.eka.ekaPricing.pojo.GMRStatusObject;
import com.eka.ekaPricing.pojo.HolidayRuleDates;
import com.eka.ekaPricing.pojo.Stock;
import com.eka.ekaPricing.pojo.TieredPricingItem;
import com.eka.ekaPricing.pojo.TriggerPrice;
import com.eka.ekaPricing.pojo.TriggerPriceProperties;
import com.eka.ekaPricing.service.CurveService;
import com.eka.ekaPricing.standalone.CollectionDataFetcher;
import com.eka.ekaPricing.standalone.CurveDataFetcher;
import com.eka.ekaPricing.standalone.ErrorMessageFetcher;
import com.eka.ekaPricing.standalone.ExpiryCalenderFetcher;
import com.eka.ekaPricing.standalone.FormulaFetcher;
import com.eka.ekaPricing.standalone.FormulaeCalculator;
import com.eka.ekaPricing.standalone.GMRStatusObjectCreationHelper;
import com.eka.ekaPricing.standalone.MDMServiceFetcher;
import com.eka.ekaPricing.standalone.UpdateTriggerPriceObjectHelper;
import com.eka.ekaPricing.util.ContextProvider;
import com.eka.ekaPricing.util.JsonConverter;
import com.google.gson.Gson;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

@Component
public class TriggerPriceCalculator {
	@Autowired
	CurveService curveService;
	@Autowired
	ErrorMessageFetcher messageFetcher;
	@Autowired
	FormulaFetcher formulafetcher;
	@Autowired
	ContextProvider contextProvider;
	@Autowired
	FormulaeCalculator formCalculator;
	@Autowired
	JsonConverter jcon;
	@Autowired
	CurveDataFetcher curveFetcher;
	@Autowired
	UpdateTriggerPriceObjectHelper updateTriggerPriceObjectHelper;
	@Autowired
	GMRStatusObjectCreationHelper gmrStatusObjectCreationHelper;
	@Autowired
	CurveDataFetcher curveDataFetcher;
	@Autowired
	MDMServiceFetcher mdmFetcher;
	@Autowired
	ExpiryCalenderFetcher expiryCalenderFetcher;
	@Autowired
	CollectionDataFetcher collectionDataFetcher;
	
	final static Logger logger = ESAPI.getLogger(TriggerPriceCalculator.class);
	
	private static final String DEFAULT_PRICE_VALUE = "0.0";

	public double applyTriggerPrice(CurveDetails curveDetails, double itemQty, String pricePerDay, LocalDateTime asOf,
			ContextProvider tenantProvider, TriggerPriceProperties triggerProps) throws Exception {
		Optional<String> pricePerDayOpt = Optional.ofNullable(pricePerDay);
		double pricePerDayVal = Double.parseDouble((pricePerDayOpt.orElse(DEFAULT_PRICE_VALUE)).toString());
		List<TriggerPrice> triggerPriceList = curveDetails.getTriggerPriceList();
		double finalPrice = 0;
		double unitTriggerPrice = 0;
		double triggerQty = itemQty;
		if(null!=triggerPriceList && triggerPriceList.size()>0) {
			/*No Exposures required for  Pre and Post Execution*/
			
			//calculateTriggerPriceExposures(curveDetails.getCurveList(), asOf.toLocalDate(), triggerPriceList,tenantProvider,triggerProps);
		}
		if (itemQty > 0) {
			for (TriggerPrice triggerPrice : triggerPriceList) {
				if (null != triggerPrice.getExecution() && triggerPrice.getExecution().equals("Post- exection")) {
					continue;
				}
				if (null != triggerPrice.getFixationStatus()
						&& triggerPrice.getFixationStatus().equalsIgnoreCase("cancelled")) {
					continue;
				}
				DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
				String date = triggerPrice.getTriggerDate();
				if (date.isEmpty()) {
					throw new PricingException("invalid trigger price details");
				}
				if (!date.substring(date.length() - 4, date.length()).contains(":")) {
					date = date.substring(0, date.length() - 2) + ":"
							+ date.substring(date.length() - 2, date.length());
				}
				LocalDateTime triggerDate = LocalDateTime.parse(date, formatter);
				if (triggerDate.toLocalDate().isEqual(asOf.toLocalDate())
						|| triggerDate.toLocalDate().isBefore(asOf.toLocalDate())) {
					triggerQty = triggerQty - triggerPrice.getQuantity();
					if (curveService.checkZero(triggerPrice.getQuantity()) && 
							!curveService.checkZero(triggerPrice.getItemFixedQtyAvailable())) {
						throw new PricingException(
								messageFetcher.fetchErrorMessage(tenantProvider, "013", new ArrayList<String>()));
					}
					if (triggerPrice.getFixationMethod().equalsIgnoreCase("manual")) {
						unitTriggerPrice = unitTriggerPrice + (triggerPrice.getPrice() * triggerPrice.getQuantity());
					} else {
						String formulaId = triggerPrice.getFormulaId();
						double formulaPrice = getPriceForFormulaFxation(formulaId, triggerProps.getItemObj(),
								triggerProps.getPrecision(), null, triggerProps.getCurrencyList(),
								triggerProps.getQtyUnitList(), triggerProps.getHolidayRule(),
								triggerProps.getProductIdList());
						unitTriggerPrice = unitTriggerPrice + formulaPrice;
					}
				}
			}
			if (unitTriggerPrice > 0) {
				finalPrice = (unitTriggerPrice + (triggerQty * pricePerDayVal)) / itemQty;
			} else {
				finalPrice = pricePerDayVal;
			}
		}
		return finalPrice;
	}

	public List<Stock> applyTriggerPriceAtStock(List<TriggerPrice> triggerPriceList, List<Stock> stockList, GMR gmr,
			List<Double> stockLevelPrice, boolean isTiered, List<GMRStatusObject> gmrStatusObjectList,
			List<Curve> curveList, LocalDate asOf, List<String> storedGMRList, List<TieredPricingItem> tieredList,
			boolean isFlat, JSONArray gmrDataArr) throws Exception {
		triggerPriceList = sortTriggerPriceData(triggerPriceList);
		List<Stock> resList = new ArrayList<Stock>();
		double[] resArr = new double[stockList.size()];
		double triggerQtySum = 0d;
		for(TriggerPrice t: triggerPriceList) {
			triggerQtySum = triggerQtySum + t.getQuantity();
//			t.setConsumed(false);
//			t.setConsumedQuantity(0);
//			try {
//				double price = t.getCalculatedPrice() / t.getQuantity();
//				t.setPrice(price);
//			}
//			catch (Exception e) {
//				throw new PricingException("Invalid Trigger Price Data");
//			}
		}
		
	// Changes for Stock ref no	
		GMRStatusObject gmrStatusObj = null;
		try {
		for(Stock stock : stockList) {
			  
			  List<GMRStatusObject> listGmrStatusObj = new ArrayList<GMRStatusObject>();
			  gmrStatusObjectList.forEach(statusObj -> {
				  if (statusObj.getGmrRefNo().equals(stock.getGMRRefNo())
							&& !statusObj.getGmrStatus().equals("CANCELLED")) {
					  listGmrStatusObj.add(statusObj);
						return;
					}
				});
			  if(listGmrStatusObj.size()>0) {
				  gmrStatusObj=listGmrStatusObj.get(0);
			  }
			  
			List<FixationObject> fixationObjList = new ArrayList<FixationObject>();
			if(null!=gmrStatusObj) {
				fixationObjList.addAll(gmrStatusObj.getFixationUsed());
			}
			
			AtomicReference<Integer> counter = new AtomicReference<>(0);
			fixationObjList.forEach(fixationObj -> { 
			    // second loop for filtered elements
			    fixationObj.getStocks().stream().filter(stockObject -> !stockObject.optString("stock").contains(stock.getRefNo())).forEach(stockObject -> { 
			    	List<JSONObject> stocksAvl = new ArrayList<JSONObject>();
			    	stockObject.put("stock", stock.getRefNo());
					stockObject.put("qty", fixationObj.getFixationQty());
					stocksAvl.add(stockObject);
					counter.getAndUpdate(value -> value + 1);
			    });
			});
					
			if(counter.get()>0) {
				gmrStatusObjectCreationHelper.createGMRStatusObject(gmrStatusObj);
			}
		 }
		}catch (Exception e) {
			throw new PricingException("Error while updating the Stock ref no");
		}
				
		double stockQtySum = 0d;
		for (Stock st : stockList) {
			stockQtySum = stockQtySum + st.getQty();
		}
		if (null == triggerPriceList || triggerPriceList.size()==0) {
			resArr = getStockPrice(stockList, 0, stockLevelPrice, 0, isTiered);
		}
		resArr = getstockPriceForStockGreaterThanTriggerPrice(triggerPriceList, stockList, stockLevelPrice, gmr,
				isTiered, asOf, storedGMRList, gmrStatusObjectList, tieredList, isFlat, gmrDataArr);
		int i = 0;
		for (Stock stock : stockList) {
			stock.setStockPrice(resArr[i] / stock.getQty());
			resList.add(stock);
			i++;
		}
		return resList;
	}

	public double[] getStockPrice(List<Stock> stockList, double qty, List<Double> stockLevelPrice, double triggerPrice,
			boolean isTiered) {
		double result = 0.0d;
		double[] resArr = new double[stockList.size()];
		int i = 0;
		for (Stock stock : stockList) {
			result = 0;
			if(stock.getQty() < qty && isTiered) {
				result = result + (stock.getQty() * triggerPrice);
				qty = qty - stock.getQty();
			}
			else if (stock.getQty() <= qty) {
//				List<Stock> tempList = (List<Stock>) stockList.remove(0);
				result = result + (qty * triggerPrice);
				qty = qty - stock.getQty();
			} else {
				
				result = result + (qty * triggerPrice) + ((stock.getQty() - qty) * stockLevelPrice.get(i));
				qty = 0;
			}
			resArr[i] = result;
			i++;
		}
		return resArr;
	}
	
	public double[] getstockPriceForStockGreaterThanTriggerPrice(List<TriggerPrice> triggerPriceList,
			List<Stock> stockList, List<Double> stockLevelPrice, GMR gmr, boolean isTiered, LocalDate asOf,
			List<String> storedGMRList, List<GMRStatusObject> gmrStatusObjectList, List<TieredPricingItem> tieredList,
			boolean isFlat, JSONArray gmrDataArr) throws Exception {

		double[] resArr = new double[stockList.size()];
		Map<LocalDate, Double> dateMap = new HashMap<LocalDate, Double>();
		int i = 0;
		for (Stock stock : stockList) {
			double qtyConversionFactor = stock.getQtyConversionRate();
			if(curveService.checkZero(qtyConversionFactor)) {
				qtyConversionFactor = 1;
			}
			
			GMRStatusObject statusObjectToCheckPartial = null;
			for(GMRStatusObject statusObj : gmrStatusObjectList) {
				if (statusObj.getGmrRefNo().equals(stock.getGMRRefNo())
						&& !statusObj.getGmrStatus().equals("CANCELLED")) {
					statusObjectToCheckPartial = statusObj;
					break;
				}
			}
			if(null!=stock.getFixationUsed()) {
				double fixedSum = 0;
				for(FixationObject fixation: stock.getFixationUsed()) {
					fixedSum = fixedSum + fixation.getFixationQty();
				}
				if(curveService.checkZero(stock.getQty()-fixedSum)) {
					resArr[i] = 0;
					i++;
					continue;
				}
			}
			boolean isPartial = false;
			List<FixationObject> fixationListForPartial = new ArrayList<FixationObject>();
			if(null!=statusObjectToCheckPartial) {
				fixationListForPartial.addAll(statusObjectToCheckPartial.getFixationUsed());
				if(!curveService.checkZero(statusObjectToCheckPartial.getGmrUnFixedQty())) {
					isPartial = true;
				}
			}
			if(storedGMRList.contains(stock.getGMRRefNo()) && !isPartial && null != statusObjectToCheckPartial) {
				resArr[i] = 0;
				i++;
				continue;	
			}
			if (fixationListForPartial.size() > 0 && isPartial) {
				resArr[i] = evaluateTriggerPriceForPartiallyFixedGMR(stock.getQty(), fixationListForPartial, stockLevelPrice.get(i),
						triggerPriceList, stock, tieredList, isFlat, gmrDataArr);
			}
			else {
				double stockQty = stock.getQty();
				double res = 0d;
				double fixedQty = 0;
				fixedQty = stock.getFixedQty();
				List<FixationObject> fixationList = new ArrayList<FixationObject>();
				fixationList = stock.getFixationUsed();
				if(null==fixationList) {
					fixationList = new ArrayList<FixationObject>();
				}
				String gmrCreatedDate = "";
				for(int count=0; count<gmrDataArr.length(); count++) {
					JSONObject storedJsonObj = gmrDataArr.getJSONObject(count);
					if(storedJsonObj.optString("internalGMRRefNo").equalsIgnoreCase(stock.getGMRRefNo())) {
						gmrCreatedDate = storedJsonObj.optString("sys__createdOn");
						break;
					}
				}
				for (TriggerPrice triggerPrice : triggerPriceList) {
//					checking if post fixation is for current iterated stock. if now skipping
					if (null != triggerPrice.getExecution() && null != triggerPrice.getGmrRefNo()
							&& (triggerPrice.getExecution().equals("Post- exection") || 
									triggerPrice.getExecution().equals("Post- execution"))
							&& !triggerPrice.getGmrRefNo().equals(stock.getGMRRefNo())) {
						continue;
					}
					LocalDate triggerDate = curveFetcher.convertISOtoLocalDate(triggerPrice.getTriggerDate());
					if(triggerPrice.isConsumed() || curveService.checkZero(triggerPrice.getItemFixedQtyAvailable())) {
						continue;
					}
					if (!isGMRValidForFixation(Long.toString(triggerPrice.getSys__createdOn()), gmrCreatedDate)
							&& (triggerPrice.getExecution().equals("Pre- exection") || 
									triggerPrice.getExecution().equals("Pre-execution"))) {
						continue;
					}
					if(curveService.checkZero(stockQty)) {
						continue;
					}
					if (null != triggerPrice.getFixationStatus()
							&& triggerPrice.getFixationStatus().equalsIgnoreCase("cancelled")) {
						continue;
					}
//					if(!stock.getGMRRefNo().equals(gmr.getRefNo())) {
//						continue;
//					}
					try {
						double price = triggerPrice.getPrice();
						FixationObject fixationObject = new FixationObject();
						if(stockQty >= triggerPrice.getItemFixedQtyAvailable()) {
							res = res + (triggerPrice.getItemFixedQtyAvailable() * price);
							stockQty = stockQty - triggerPrice.getItemFixedQtyAvailable();
							stock.setFixedQty(fixedQty + triggerPrice.getItemFixedQtyAvailable());
							triggerPrice.setConsumedQuantity(triggerPrice.getItemFixedQtyAvailable());
							triggerPrice.setConsumed(true);
							triggerPrice.setQuantity(0);
							fixationObject.setFixationQty(triggerPrice.getItemFixedQtyAvailable());
							fixationObject.setFixationQtyInGMRQtyUnit(triggerPrice.getItemFixedQtyAvailable()/qtyConversionFactor);
							fixationObject.setFixedPrice(triggerPrice.getItemFixedQtyAvailable() * price);
							fixationObject.setGmrRef(stock.getGMRRefNo());
							List<JSONObject> stocksAvl = new ArrayList<JSONObject>();
							if(null != fixationObject.getStocks()) {
								stocksAvl = fixationObject.getStocks();
							}
							JSONObject stockObject = new JSONObject();
							stockObject.put("stock", stock.getRefNo());
							stockObject.put("qty", fixationObject.getFixationQty());
							stocksAvl.add(stockObject);
							fixationObject.setStocks(stocksAvl);
							double fixedQrtAvl = triggerPrice.getItemFixedQtyAvailable();
							fixedQrtAvl = fixedQrtAvl - fixationObject.getFixationQty();
							triggerPrice.setItemFixedQtyAvailable(fixedQrtAvl);
						}
						else {
							res = res + (stockQty * price);
							triggerPrice.setConsumedQuantity(triggerPrice.getConsumedQuantity() + stockQty);
							stock.setFixedQty(fixedQty + stockQty);
							triggerPrice.setQuantity(triggerPrice.getQuantity() - stockQty);
							fixationObject.setFixationQty(stockQty);
							fixationObject.setFixationQtyInGMRQtyUnit(triggerPrice.getItemFixedQtyAvailable()/qtyConversionFactor);
							fixationObject.setFixedPrice(stockQty * price);
							fixationObject.setGmrRef(stock.getGMRRefNo());
							List<JSONObject> stocksAvl = new ArrayList<JSONObject>();
							if(null != fixationObject.getStocks()) {
								stocksAvl = fixationObject.getStocks();
							}
							JSONObject stockObject = new JSONObject();
							stockObject.put("stock", stock.getRefNo());
							stockObject.put("qty", fixationObject.getFixationQty());
							stocksAvl.add(stockObject);
							fixationObject.setStocks(stocksAvl);
							stockQty = 0;
							double fixedQrtAvl = triggerPrice.getItemFixedQtyAvailable();
							fixedQrtAvl = fixedQrtAvl - fixationObject.getFixationQty();
							triggerPrice.setItemFixedQtyAvailable(fixedQrtAvl);
						}
						if (dateMap.containsKey(triggerDate)) {
							dateMap.put(triggerDate, dateMap.get(triggerDate) + fixationObject.getFixationQty());
						}
						else {
							dateMap.put(triggerDate, fixationObject.getFixationQty());
						}
						fixationObject.setFixatonNumber(triggerPrice.getFixationRefNo());
						fixationList.add(fixationObject);
						updateTriggerPriceObjectHelper.updateTriggerPrice(triggerPrice);
					} catch (Exception e) {
						throw new PricingException("Invalid Trigger Price Data");
					}
				}
				stock.setFixationUsed(fixationList);
				if (!curveService.checkZero(stockQty)) {
					double stockLevelPrices = stockLevelPrice.get(i);
					if (isFlat) {
						stockLevelPrices = formCalculator.getTierForFlat(tieredList, stockQty).getTieredLevelPrice();
					}
					res = res + (stockQty * stockLevelPrices);
				}
				resArr[i] = res;
			}
			
			
			i++;
		}
		updateStatusObject(stockList);
//		removing below code and putting corrected code into a new method updateStatusObject
		
//		if(triggerPriceList.size()>0) {
//			GMRStatusObject gmrStatusObject = new GMRStatusObject();
//			double fixedQty = 0d;
//			double qty = 0d;
//			GMRStatusObject prevStatusObj = null;
//			List<FixationObject> fixationUsed = new ArrayList<FixationObject>();
//			for(Stock st: stockList) {
//				String gmrRef = st.getGMRRefNo();
//				for(GMRStatusObject statusObject: gmrStatusObjectList) {
//					if(statusObject.getGmrRefNo().equals(gmrRef)) {
//						prevStatusObj = statusObject;
//						break;
//					}
//				}
//				if(storedGMRList.contains(st.getGMRRefNo())) {
//					continue;
//				}
//				gmrStatusObject.setInternalContractRefNo(st.getContractRefNo());
//				gmrStatusObject.setInternalContractItemRefNo(st.getContractItemRefNo());
//				gmrStatusObject.setGmrRefNo(st.getGMRRefNo());
//				
//				fixedQty = fixedQty + st.getFixedQty();
//				fixationUsed.addAll(st.getFixationUsed());
//				qty = qty + st.getQty();
//			}
//			if (null != prevStatusObj && curveService.checkZero(fixedQty)
//					&& !curveService.checkZero(prevStatusObj.getGmrFixedQty())
//					&& fixationUsed.size() == prevStatusObj.getFixationUsed().size()) {
//				logger.error(Logger.EVENT_FAILURE, "Skipping update of GMRStatusObject as Partially Fixed gmr status remains same");
//			}
//			else if(null != prevStatusObj && curveService.checkZero(fixedQty)
//					&& !curveService.checkZero(prevStatusObj.getGmrFixedQty())
//					&& fixationUsed.size() > prevStatusObj.getFixationUsed().size())  {
//				fixedQty = 0;
//				for(FixationObject fixation : fixationUsed) {
//					fixedQty = fixedQty + fixation.getFixationQty();
//				}
//				gmrStatusObject.setGmrFixedQty(fixedQty);
//				gmrStatusObject.setFixationUsed(fixationUsed);
//				gmrStatusObject.setGmrUnFixedQty(qty - fixedQty);
//				gmrStatusObject.setGmrQty(Double.toString(qty));
//				if(qty - fixedQty > 0) {
//					gmrStatusObject.setGmrStatus("PARTIALLY FIXED");
//				}
//				else {
//					gmrStatusObject.setGmrStatus("FULLY FIXED");
//				}
//				gmrStatusObjectCreationHelper.createGMRStatusObject(gmrStatusObject);
//			}
//			else {
//				gmrStatusObject.setGmrFixedQty(fixedQty);
//				gmrStatusObject.setFixationUsed(fixationUsed);
//				gmrStatusObject.setGmrUnFixedQty(qty - fixedQty);
//				gmrStatusObject.setGmrQty(Double.toString(qty));
//				if(qty - fixedQty > 0) {
//					gmrStatusObject.setGmrStatus("PARTIALLY FIXED");
//				}
//				else {
//					gmrStatusObject.setGmrStatus("FULLY FIXED");
//				}
//				gmrStatusObjectCreationHelper.createGMRStatusObject(gmrStatusObject);
//			}
//		}
		
		return resArr;
	}

	public double getStockValue(JSONArray stockArray, ContextProvider tenantProvider) throws PricingException {
		if (null == stockArray || stockArray.length() == 0) {
			return 0;
		}
		double result = 0d;
		double totalQty = 0l;
		for (int i = 0; i < stockArray.length(); i++) {
			JSONObject stockObj = stockArray.optJSONObject(i);
			double qty = stockObj.optDouble("qty", 0);
			double price = stockObj.optDouble("stockPrice", 0);
			if(curveService.checkZero(price)) {
				continue;
			}
			result = result + (qty * price);
			totalQty = totalQty + qty;
		}
		if(totalQty == 0 && curveService.checkZero(result)) {
			return 0;
		}
		if (totalQty == 0) {
			throw new PricingException(
					messageFetcher.fetchErrorMessage(tenantProvider, "039", new ArrayList<String>()));
		}
		return result / totalQty;
	}

	public double getPdPriceValue(JSONArray stockArray, ContextProvider tenantProvider) throws PricingException {
		if (null == stockArray || stockArray.length() == 0) {
			return 0;
		}
		double result = 0d; 
		double totalQty = 0l;
		for (int i = 0; i < stockArray.length(); i++) {
			JSONObject stockObj = stockArray.optJSONObject(i);
			double qty = stockObj.optDouble("qty", 0);
			double price = stockObj.optDouble("pdPrice", 0);
			result = result + (qty * price);
			totalQty = totalQty + qty;
		}
		if (totalQty == 0) {
			throw new PricingException(
					messageFetcher.fetchErrorMessage(tenantProvider, "039", new ArrayList<String>()));
		}
		return result / totalQty;
	}
	
	public List<TriggerPrice> sortTriggerPriceData(List<TriggerPrice> triggerPriceList) {
		Collections.sort(triggerPriceList, new Comparator<TriggerPrice>() {
			@Override
			public int compare(TriggerPrice t1, TriggerPrice t2) {
				return Long.compare(t1.getSys__createdOn(), t2.getSys__createdOn());
			}
		}); 
		return triggerPriceList;
	}
	
	public double getPriceForFormulaFxation(String formulaId, JSONObject itemObj, String precision, GMR gmr, List<String> currencyList, List<String> qtyUnitList,
			String holidayRule, List<String> productIdList) throws Exception {
		JSONObject formulaObj = formulafetcher.getFormula(null, formulaId);
		Formula f = jcon.convertJsonToFormula(formulaObj);
		String exp = f.getFormulaExpression();
		List<Curve> curveDetails = f.getCurveList();
		String expression = formCalculator.calculateCurve(exp, curveDetails, itemObj, precision, 0, gmr, null, currencyList, qtyUnitList, holidayRule, productIdList);
		while(expression.contains("MIN") || expression.contains("MAX") || expression.contains("AVG")) {
			expression = curveFetcher.calculateAggregate(expression, null);
		}
//		expression = curveFetcher.calculateAggregate(expression);
		Expression ex = null;
		try {
			ex = new ExpressionBuilder(expression).build();
		} catch (Exception exc) {
			throw new PricingException(
					messageFetcher.fetchErrorMessage(null, "001", new ArrayList<String>()));
		}
		String resultString = Double.toString(ex.evaluate());
		try {
			return Double.parseDouble(resultString);
		}
		catch(Exception e) {
			throw new PricingException(
					messageFetcher.fetchErrorMessage(null, "001", new ArrayList<String>()));
		}
	}
//	Exposure calculation in case of trigger pricing, not being used
	public void calculateTriggerPriceExposures(List<Curve> curveList, LocalDate asOf,
			List<TriggerPrice> triggerPriceList,ContextProvider tenantProvider, TriggerPriceProperties triggerProps) throws Exception {
		DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
		String execution=null; 
		if(!curveList.isEmpty()) {
			for(Curve c: curveList) {
				JSONArray exposureArray = new JSONArray();
				JSONArray expArray = new JSONArray();
				double qty = c.getQty();
				double qty1 = qty;
				LocalDate latestDate = c.getQpFromDate();
				HolidayRuleDates holidayRuleDates = c.getHolidayRuleDates();
				List<LocalDateTime> validDatesList = holidayRuleDates.getDateToBeUsed();

				for (TriggerPrice triggerPrice : triggerPriceList) {
					String date = triggerPrice.getTriggerDate();
					 execution =triggerPrice.getExecution();
					if (!date.substring(date.length() - 4, date.length()).contains(":")) {
						date = date.substring(0, date.length() - 2) + ":"
								+ date.substring(date.length() - 2, date.length());
					}
					LocalDate triggerDate = LocalDateTime.parse(date, formatter).toLocalDate();
					if (latestDate.isBefore(triggerDate) || latestDate.isEqual(triggerDate)) {
						latestDate = triggerDate.plusDays(1);
						if (latestDate.getDayOfWeek().getValue() == 6) {
							latestDate = triggerDate.plusDays(2);
						} else if (latestDate.getDayOfWeek().getValue() == 7) {
							latestDate = triggerDate.plusDays(1);
						}
					}
					JSONObject exposureObj = new JSONObject();
					exposureObj.put("date", triggerDate);
					exposureObj.put("pricedQty", triggerPrice.getQuantity());
					exposureObj.put("unPricedQty", 0);
					exposureObj.accumulate("pricedQuantityInBaseQtyUnit", triggerPrice.getQuantity()*triggerProps.getBaseQtyUnitConversion());
					exposureObj.accumulate("unpricedQuantityInBaseQtyUnit", 0);
					String month = triggerDate.getMonth().toString().substring(0, 3);
					String year = Integer.toString(triggerDate.getYear());
					exposureObj.put("instrumentDeliveryMonth", month + year);
					exposureObj.accumulate("pricedPercentage", (triggerPrice.getQuantity() / qty1) * 100);
					exposureObj.accumulate("unpricedPercentage", 0);
					exposureArray.put(exposureObj);
					qty = qty - triggerPrice.getQuantity();
				}
				
				Map<LocalDate, Integer> dateCountMap = new LinkedHashMap<LocalDate, Integer>();
				for (LocalDateTime date : validDatesList) {
					if (dateCountMap.containsKey(date.toLocalDate())) {
						dateCountMap.put(date.toLocalDate(), dateCountMap.get(date.toLocalDate()) + 1);
					} else {
						dateCountMap.put(date.toLocalDate(), 1);
					}
				}
				int count=0;
				Set<Entry<LocalDate, Integer>> flagMapEntrySet = dateCountMap.entrySet();
				for(LocalDateTime date : validDatesList) {
					if(!asOf.isBefore(date.toLocalDate())) {
						continue;
					}
					count++;
				}
				if(count==0 && qty>0) {
					LocalDate date=latestDate.minusDays(1);
					double dailyPercentage = (qty*100)/(qty1);
					JSONObject exposureObj = new JSONObject();
					exposureObj.put("date", date);
					exposureObj.put("unPricedQty", qty);
					exposureObj.put("pricedQty", 0);
					exposureObj.accumulate("unpricedQuantityInBaseQtyUnit", qty*triggerProps.getBaseQtyUnitConversion());
					exposureObj.accumulate("pricedQuantityInBaseQtyUnit", 0);
					String month = date.getMonth().toString().substring(0, 3);
					String year = Integer.toString(date.getYear());
					exposureObj.put("instrumentDeliveryMonth", month + year);
					exposureObj.accumulate("unpricedPercentage", dailyPercentage);
					exposureObj.accumulate("pricedPercentage", 0);
					exposureArray.put(exposureObj);
					
				}else if(count>0 && qty>0){
					double dailyPricedQty = qty/count;
					double dailyPercentage = (qty*100)/(qty1*count);
					if(!c.getPriceQuoteRule().equals("Event Offset Based")) {
						for(Entry<LocalDate, Integer> entry : flagMapEntrySet) {
							JSONObject expObj = new JSONObject();
							if(entry.getKey().isBefore(latestDate)) {
								continue;
							}
							expObj.accumulate("date", entry.getKey());
							int numOfDaysUsed = 1;
							if(dateCountMap.containsKey(entry.getKey())) {
								numOfDaysUsed = dateCountMap.get(entry.getKey());
							}
							if(entry.getKey().isAfter(asOf)) {
								expObj.accumulate("pricedQty", 0);
								expObj.accumulate("unPricedQty", dailyPricedQty * entry.getValue());
								expObj.accumulate("pricedPercentage", 0);
								expObj.accumulate("unpricedPercentage", dailyPercentage * numOfDaysUsed);
								expObj.accumulate("pricedQuantityInBaseQtyUnit", 0);
								expObj.accumulate("unpricedQuantityInBaseQtyUnit", dailyPricedQty * numOfDaysUsed*triggerProps.getBaseQtyUnitConversion());
							}
							else {
								expObj.accumulate("unPricedQty", 0);
								expObj.accumulate("pricedQty", dailyPricedQty * entry.getValue());
								expObj.accumulate("unpricedPercentage", 0);
								expObj.accumulate("pricedPercentage", dailyPercentage * numOfDaysUsed);
								expObj.accumulate("pricedQuantityInBaseQtyUnit", dailyPricedQty * numOfDaysUsed*triggerProps.getBaseQtyUnitConversion());
								expObj.accumulate("unpricedQuantityInBaseQtyUnit", 0);
							}
							exposureArray.put(expObj);
						}
					}
					else {
						JSONObject exposureObj = new JSONObject();
						LocalDate lastDayOfMonth = c.getQpToDate().with(TemporalAdjusters.lastDayOfMonth());
						LocalDate priceDate = lastDayOfMonth;
						String month = priceDate.getMonth().toString().substring(0, 3);
						String year = Integer.toString(priceDate.getYear());
						exposureObj.put("instrumentDeliveryMonth", month+year);
						exposureObj.put("date", lastDayOfMonth);
						if(!lastDayOfMonth.isAfter(asOf)) {
							exposureObj.put("pricedQty", qty);
							exposureObj.put("unPricedQty", 0);
							exposureObj.accumulate("pricedPercentage", (qty/c.getQty())*100);
							exposureObj.accumulate("unpricedPercentage", 0);
							exposureObj.accumulate("pricedQuantityInBaseQtyUnit", qty*triggerProps.getBaseQtyUnitConversion());
							exposureObj.accumulate("unpricedQuantityInBaseQtyUnit", 0);
						}
						else {
							exposureObj.put("pricedQty", 0);
							exposureObj.put("unPricedQty", qty);
							exposureObj.accumulate("unpricedPercentage", (qty/c.getQty())*100);
							exposureObj.accumulate("pricedPercentage", 0);
							exposureObj.accumulate("pricedQuantityInBaseQtyUnit", 0);
							exposureObj.accumulate("unpricedQuantityInBaseQtyUnit", qty*triggerProps.getBaseQtyUnitConversion());
						}
						exposureArray.put(exposureObj);
					}
				}
				
				c.setTriggerPriceExposure(exposureArray);
			}
		}else {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			DateTimeFormatter expiryDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
			Curve c= new Curve();
			JSONArray exposureArray = new JSONArray();
			JSONArray expArray = new JSONArray();
			String fromDate1 = triggerProps.getItemObj().optString("deliveryFromDate", "");
			String toDate1 = triggerProps.getItemObj().optString("deliveryToDate", "");
			LocalDateTime fromDate = sdf.parse(fromDate1).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			LocalDateTime toDate = sdf.parse(toDate1).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			double qty = triggerProps.getItemObj().optDouble("qty");
			if(Double.isNaN(qty) || curveService.checkZero(qty)) {
				qty = triggerProps.getItemObj().optDouble("itemQty");
				triggerProps.getItemObj().put("qty",qty);
			}
			double qty1 = qty;
			String[] qualityArr = mdmFetcher.getQualityExchangeUnit(tenantProvider, triggerProps.getQuality());
			c.setCurveName(qualityArr[0]);
			c.setPricePoint(qualityArr[2]);
			c.setExchange(qualityArr[3]);
			String valuationPriceUnit=qualityArr[1];
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
			JSONArray expiryArr = expiryCalenderFetcher.getData(c, tenantProvider);
			c.setExpiryArr(expiryArr);
			JSONArray collectionArray = new JSONArray();
			JSONArray arr = new JSONArray();
			fields.setSd(fromDate.toLocalDate());
			fields.setEd(toDate.toLocalDate());
			if(!c.getPricePoint().equalsIgnoreCase("forward")) {
				if(null == c.getCollectionArray() || c.getCollectionArray().size() == 0) {
					collectionArray = collectionDataFetcher.triggerRequest(c, tenantProvider, fields.getSd(),
							fields.getEd(), asOf);
				}
			}else {
				   for (int i = 0; i < expiryArr.length(); i++) {
						JSONObject expiryObj = expiryArr.getJSONObject(i);
						String lastTradeDateStr = expiryObj.optString("Last Trade Date");
						LocalDate lastTradeDate = LocalDateTime.parse(lastTradeDateStr, expiryDateFormatter).toLocalDate();
						if (lastTradeDate.isBefore(fromDate.toLocalDate())) {
							continue;
						}
						c.setMonthYear(expiryObj.optString("MONTH/YEAR"));
						if(null == c.getCollectionArray() || c.getCollectionArray().size() == 0) {
							c.setPricePoint("Forward");
							arr = collectionDataFetcher.fetchMarketPricesForV2(c, tenantProvider, fields, asOf);
					       collectionArray = collectionDataFetcher.mergeIntoFinal(arr, collectionArray);
					       if(lastTradeDate.isAfter(toDate.toLocalDate())) {
								break;
							}
					    }
					}
				}
			if ((null != collectionArray && collectionArray.length()>0)) {
				curveFetcher.setupMarketDataCollection(fields, c, collectionArray, tenantProvider, asOf);
			}
			
			HolidayRuleDates holidayRuleDates = curveFetcher.getListOfDaysPostHolidayRule(fields, c.getExchange(), tenantProvider,
					triggerProps.getHolidayRule(), c);
			List<LocalDateTime> validDates = holidayRuleDates.getDateToBeUsed();
			LocalDate latestDate = fromDate.toLocalDate();
			
			for (TriggerPrice triggerPrice : triggerPriceList) {
				String date = triggerPrice.getTriggerDate();
				 execution =triggerPrice.getExecution();
				if (!date.substring(date.length() - 4, date.length()).contains(":")) {
					date = date.substring(0, date.length() - 2) + ":"
							+ date.substring(date.length() - 2, date.length());
				}
				LocalDate triggerDate = LocalDateTime.parse(date, formatter).toLocalDate();
				if (latestDate.isBefore(triggerDate) || latestDate.isEqual(triggerDate)) {
					latestDate = triggerDate.plusDays(1);
					if (latestDate.getDayOfWeek().getValue() == 6) {
						latestDate = triggerDate.plusDays(2);
					} else if (latestDate.getDayOfWeek().getValue() == 7) {
						latestDate = triggerDate.plusDays(1);
					}
				}
				JSONObject exposureObj = new JSONObject();
				exposureObj.put("date", triggerDate);
				exposureObj.put("pricedQty", triggerPrice.getQuantity());
				exposureObj.put("unPricedQty", 0);
				exposureObj.put("pricedQuantityInBaseQtyUnit", triggerPrice.getQuantity()*triggerProps.getBaseQtyUnitConversion());
				exposureObj.put("unpricedQuantityInBaseQtyUnit", 0);
				String month = triggerDate.getMonth().toString().substring(0, 3);
				String year = Integer.toString(triggerDate.getYear());
				exposureObj.put("instrumentDeliveryMonth", month + year);
				exposureObj.accumulate("pricedPercentage", (triggerPrice.getQuantity() / qty1) * 100);
				exposureObj.accumulate("unpricedPercentage", 0);
				expArray.put(exposureObj);
				qty = qty - triggerPrice.getQuantity();
			}
			
			for (int expIndex = 0; expIndex < expArray.length(); expIndex++) {
				JSONObject exposureObject = expArray.getJSONObject(expIndex);
				for (int expIndex1 = expIndex + 1; expIndex1 < expArray.length(); expIndex1++) {
					JSONObject exposureObject1 = expArray.getJSONObject(expIndex1);
					LocalDate date1 = LocalDate.parse(exposureObject.optString("date"));
					LocalDate date2 = LocalDate.parse(exposureObject1.optString("date"));
					if (date1.equals(date2)) {
						Double pricedQty = exposureObject.optDouble("pricedQty");
						Double unPricedQty = exposureObject.optDouble("unPricedQty");
						Double pricedQty1 = exposureObject1.optDouble("pricedQty");
						Double unPricedQty1 = exposureObject1.optDouble("unPricedQty");
						Double pricedQuantityInBaseQtyUnit = exposureObject.optDouble("pricedQuantityInBaseQtyUnit");
						Double unpricedQuantityInBaseQtyUnit = exposureObject.optDouble("unpricedQuantityInBaseQtyUnit");
						Double pricedQuantityInBaseQtyUnit1 = exposureObject1.optDouble("pricedQuantityInBaseQtyUnit");
						Double unpricedQuantityInBaseQtyUnit1 = exposureObject1.optDouble("unpricedQuantityInBaseQtyUnit");
						Double PricedPercentage = exposureObject.optDouble("pricedPercentage");
						Double unPricedPercentage = exposureObject.optDouble("unpricedPercentage");
						Double PricedPercentage1 = exposureObject1.optDouble("pricedPercentage");
						Double unPricedPercentage1 = exposureObject1.optDouble("unpricedPercentage");
						exposureObject.put("pricedQty", pricedQty + pricedQty1);
						exposureObject.put("unPricedQty", unPricedQty + unPricedQty1);
						exposureObject.put("pricedPercentage", (PricedPercentage + PricedPercentage1));
						exposureObject.put("unpricedPercentage", (unPricedPercentage + unPricedPercentage1));
						exposureObject.put("pricedQuantityInBaseQtyUnit", pricedQuantityInBaseQtyUnit + pricedQuantityInBaseQtyUnit1);
						exposureObject.put("unpricedQuantityInBaseQtyUnit", unpricedQuantityInBaseQtyUnit + unpricedQuantityInBaseQtyUnit1);

						expArray.remove(expIndex1);
					}

				}
				exposureArray.put(exposureObject);
			}
			Map<LocalDate, Integer> dateCountMap = new LinkedHashMap<LocalDate, Integer>();
			for (LocalDateTime date : validDates) {
				if (dateCountMap.containsKey(date.toLocalDate())) {
					dateCountMap.put(date.toLocalDate(), dateCountMap.get(date.toLocalDate()) + 1);
				} else {
					dateCountMap.put(date.toLocalDate(), 1);
				}
			}
			int count=0;
			Set<Entry<LocalDate, Integer>> flagMapEntrySet = dateCountMap.entrySet();
			for(LocalDateTime date : validDates) {
				if(date.toLocalDate().isBefore(latestDate)) {
					continue;
				}
				count++;
			}
			double dailyPricedQty = qty/count;
			double dailyPercentage = (qty*100)/(qty1*count);
			if(!c.getPriceQuoteRule().equals("Event Offset Based")) {
				for(Entry<LocalDate, Integer> entry : flagMapEntrySet) {
					JSONObject expObj = new JSONObject();
					if(entry.getKey().isBefore(latestDate)) {
						continue;
					}
					expObj.accumulate("date", entry.getKey());
					int numOfDaysUsed = 1;
					if(dateCountMap.containsKey(entry.getKey())) {
						numOfDaysUsed = dateCountMap.get(entry.getKey());
					}
					if(entry.getKey().isAfter(asOf)) {
						expObj.accumulate("pricedQty", 0);
						expObj.accumulate("unPricedQty", dailyPricedQty * entry.getValue());
						expObj.accumulate("pricedPercentage", 0);
						expObj.accumulate("unpricedPercentage", dailyPercentage * numOfDaysUsed);
						expObj.accumulate("pricedQuantityInBaseQtyUnit", 0);
						expObj.accumulate("unpricedQuantityInBaseQtyUnit", dailyPricedQty * entry.getValue()*triggerProps.getBaseQtyUnitConversion());
					}
					else {
						expObj.accumulate("unPricedQty", 0);
						expObj.accumulate("pricedQty", dailyPricedQty * entry.getValue());
						expObj.accumulate("unpricedPercentage", 0);
						expObj.accumulate("pricedPercentage", dailyPercentage * numOfDaysUsed);
						expObj.accumulate("pricedQuantityInBaseQtyUnit", dailyPricedQty * entry.getValue()*triggerProps.getBaseQtyUnitConversion());
						expObj.accumulate("unpricedQuantityInBaseQtyUnit", 0);
					}
					exposureArray.put(expObj);
				}
			}
			else {
				JSONObject exposureObj = new JSONObject();
				LocalDate lastDayOfMonth = c.getQpToDate().with(TemporalAdjusters.lastDayOfMonth());
				LocalDate priceDate = lastDayOfMonth;
				String month = priceDate.getMonth().toString().substring(0, 3);
				String year = Integer.toString(priceDate.getYear());
				exposureObj.put("instrumentDeliveryMonth", month+year);
				exposureObj.put("date", lastDayOfMonth);
				if(lastDayOfMonth.isAfter(asOf)) {
					exposureObj.put("pricedQty", qty);
					exposureObj.put("unPricedQty", 0);
					exposureObj.accumulate("pricedPercentage", (qty/c.getQty())*100);
					exposureObj.accumulate("unpricedPercentage", 0);
					exposureObj.put("pricedQuantityInBaseQtyUnit", qty*triggerProps.getBaseQtyUnitConversion());
					exposureObj.put("unpricedQuantityInBaseQtyUnit", 0);
				}
				else {
					exposureObj.put("pricedQty", 0);
					exposureObj.put("unPricedQty", qty);
					exposureObj.accumulate("unpricedPercentage", (qty/c.getQty())*100);
					exposureObj.accumulate("pricedPercentage", 0);
					exposureObj.put("pricedQuantityInBaseQtyUnit", 0);
					exposureObj.put("unpricedQuantityInBaseQtyUnit", qty*triggerProps.getBaseQtyUnitConversion());
				}
				exposureArray.put(exposureObj);
			}
			c.setTriggerPriceExposure(exposureArray);
			curveList.add(c);
		}
	}

	public double evaluateTriggerPriceForPartiallyFixedGMR(double stockQty, List<FixationObject> fixationUsed,
			double stockLevelPrice, List<TriggerPrice> triggerPriceList, Stock stock,
			List<TieredPricingItem> tieredList, boolean isFlat, JSONArray gmrDataArr) throws PricingException {
		double qtyConversionFactor = stock.getQtyConversionRate();
		if(curveService.checkZero(qtyConversionFactor)) {
			qtyConversionFactor = 1;
		}
		String gmrCreatedDate = "";
		for(int count=0; count<gmrDataArr.length(); count++) {
			JSONObject storedJsonObj = gmrDataArr.getJSONObject(count);
			if(storedJsonObj.optString("internalGMRRefNo").equalsIgnoreCase(stock.getGMRRefNo())) {
				gmrCreatedDate = storedJsonObj.optString("sys__createdOn");
				break;
			}
		}
		double stockQtyForCalculation = stockQty;
		double prevQtyForStock = 0;
		List<FixationObject> fixationUsedInStock = new ArrayList<FixationObject>();
		for(TriggerPrice triggerPrice : triggerPriceList) {
			if (null != triggerPrice.getExecution() && null != triggerPrice.getGmrRefNo()
					&& (triggerPrice.getExecution().equals("Post- exection") || 
							triggerPrice.getExecution().equals("Post- execution"))
					&& !triggerPrice.getGmrRefNo().equals(stock.getGMRRefNo())) {
				continue;
			}
//			LocalDate triggerDate = curveFetcher.convertISOtoLocalDate(triggerPrice.getTriggerDate());
			if (!isGMRValidForFixation(Long.toString(triggerPrice.getSys__createdOn()), gmrCreatedDate)
					&& (triggerPrice.getExecution().equals("Pre- exection") || 
							triggerPrice.getExecution().equals("Pre-execution"))) {
				continue;
			}
			if(triggerPrice.isConsumed() || curveService.checkZero(triggerPrice.getItemFixedQtyAvailable())) {
				for (int gmrInd = 0; gmrInd < gmrDataArr.length(); gmrInd++) {
					JSONObject gmrData = gmrDataArr.optJSONObject(gmrInd);
					if(gmrData.optString("internalGMRRefNo").equalsIgnoreCase(stock.getGMRRefNo())) {
						String payload = gmrData.optString("payload");
						if(StringUtils.isEmpty(payload.trim())) {
							payload = gmrData.optString("inputPayload");
						}
						JSONObject payloadJson = new JSONObject(payload);
						JSONArray stockArray = payloadJson.getJSONArray("stocks");
						for(int i =0; i<stockArray.length(); i++) {
							JSONObject stockObj = stockArray.getJSONObject(i);
							if(stockObj.optString("refNo").equalsIgnoreCase(stock.getRefNo())) {
								prevQtyForStock = stockObj.optDouble("qty");
							}

						}
					}

				}
				for(FixationObject fixationObj: fixationUsed) {
					if(fixationObj.getFixatonNumber().equals(triggerPrice.getFixationRefNo())) {
						if(curveService.checkZero(prevQtyForStock)) {
							stockQtyForCalculation = stockQtyForCalculation - fixationObj.getFixationQty();
						}
						else if (fixationObj.getFixationQty() > prevQtyForStock) {
							for (JSONObject stockObject : fixationObj.getStocks()) {
								if (stockObject.optString("stock").contains(stock.getRefNo())) {
									FixationObject fixationObjectForStock = new FixationObject();
									double storedQtyInFixations = stockObject.optDouble("qty");
									stockQtyForCalculation = stockQtyForCalculation - storedQtyInFixations;
									fixationObj.setFixedPrice(storedQtyInFixations
											* (fixationObj.getFixedPrice() / fixationObj.getFixationQty()));
									fixationObj.setFixationQty(storedQtyInFixations);
									fixationObj.setFixationQtyInGMRQtyUnit(storedQtyInFixations/qtyConversionFactor);
									List<JSONObject> stocksAvl = new ArrayList<JSONObject>();
									stocksAvl.add(stockObject);
									Gson gson = new Gson();
									JSONObject fixationJson = new JSONObject(gson.toJson(fixationObj));
									JSONObject clonedFixation = new JSONObject(fixationJson,
											JSONObject.getNames(fixationJson));
									fixationObjectForStock = gson.fromJson(clonedFixation.toString(),
											FixationObject.class);
									fixationObjectForStock.setStocks(stocksAvl);
									fixationUsedInStock.add(fixationObjectForStock);
								}
							}

						}
						else {
							for(JSONObject stockObject: fixationObj.getStocks()) {
								if(stockObject.optString("stock").contains(stock.getRefNo())) {
									stockQtyForCalculation = stockQtyForCalculation - stockObject.optDouble("qty");
								}
							}
						}
					}
				}
				continue;
			}
			if(curveService.checkZero(stockQtyForCalculation)) {
				continue;
			}
			if (null != triggerPrice.getFixationStatus()
					&& triggerPrice.getFixationStatus().equalsIgnoreCase("cancelled")) {
				continue;
			}
			double price = triggerPrice.getPrice();
			if(curveService.checkZero(triggerPrice.getItemFixedQtyAvailable())) {
				continue;
			}
			FixationObject fixationObject = new FixationObject();
			if(stockQtyForCalculation >= triggerPrice.getItemFixedQtyAvailable()) {
				fixationObject.setFixationQty(triggerPrice.getItemFixedQtyAvailable());
				fixationObject.setFixationQtyInGMRQtyUnit(stockQtyForCalculation/qtyConversionFactor);
				fixationObject.setFixedPrice(triggerPrice.getItemFixedQtyAvailable() * price);
				fixationObject.setGmrRef(stock.getGMRRefNo());
				List<JSONObject> stocksAvl = new ArrayList<JSONObject>();
				if(null != fixationObject.getStocks()) {
					stocksAvl = fixationObject.getStocks();
				}
				JSONObject stockObject = new JSONObject();
				stockObject.put("stock", stock.getRefNo());
				stockObject.put("qty", fixationObject.getFixationQty());
				stocksAvl.add(stockObject);
				fixationObject.setStocks(stocksAvl);
				double fixedQrtAvl = triggerPrice.getItemFixedQtyAvailable();
				fixedQrtAvl = fixedQrtAvl - fixationObject.getFixationQty();
				triggerPrice.setItemFixedQtyAvailable(fixedQrtAvl);
			}
			else {
				fixationObject.setFixationQty(stockQtyForCalculation);
				fixationObject.setFixationQtyInGMRQtyUnit(stockQtyForCalculation/qtyConversionFactor);
				fixationObject.setFixedPrice(stockQtyForCalculation * price);
				fixationObject.setGmrRef(stock.getGMRRefNo());
				List<JSONObject> stocksAvl = new ArrayList<JSONObject>();
				if(null != fixationObject.getStocks()) {
					stocksAvl = fixationObject.getStocks();
				}
				JSONObject stockObject = new JSONObject();
				stockObject.put("stock", stock.getRefNo());
				stockObject.put("qty", fixationObject.getFixationQty());
				stocksAvl.add(stockObject);
				fixationObject.setStocks(stocksAvl);
				double fixedQrtAvl = triggerPrice.getItemFixedQtyAvailable();
				fixedQrtAvl = fixedQrtAvl - fixationObject.getFixationQty();
				triggerPrice.setItemFixedQtyAvailable(fixedQrtAvl);
			}
			fixationObject.setFixatonNumber(triggerPrice.getFixationRefNo());
			if(!fixationUsedInStock.isEmpty()) {
				fixationUsedInStock.add(fixationObject);
			}
			fixationUsed.add(fixationObject);
			
			updateTriggerPriceObjectHelper.updateTriggerPrice(triggerPrice);
		}
		double result = 0;
		double fixedQty = 0;
		if(fixationUsedInStock.isEmpty() && !curveService.checkZero(prevQtyForStock)) {
			boolean fixationPresentForStock = false;
			List<FixationObject> fixationObjectsForStocks = new ArrayList<FixationObject>();
			for(FixationObject fixation: fixationUsed) {
				for(JSONObject stockFixationObj : fixation.getStocks()) 
				if(stockFixationObj.optString("stock").equals(stock.getRefNo())) {
					fixationPresentForStock = true;
					fixationObjectsForStocks.add(fixation);
				}
			}
			if(fixationPresentForStock) {
				stock.setFixationUsed(fixationObjectsForStocks);
				for(FixationObject fixation : fixationObjectsForStocks) {
					fixedQty = fixedQty + fixation.getFixationQty();
					result = result + (fixation.getFixedPrice());
				}
			}
			
		}
		else if(!curveService.checkZero(prevQtyForStock)){
			stock.setFixationUsed(fixationUsedInStock);
			for(FixationObject fixation : fixationUsedInStock) {
				fixedQty = fixedQty + fixation.getFixationQty();
				result = result + (fixation.getFixedPrice());
			}
		}
		
		
		if(stockQty > fixedQty) {
			double stockLevelPrices = stockLevelPrice;
			if (isFlat) {
				stockLevelPrices = formCalculator.getTierForFlat(tieredList, stockQty - fixedQty).getTieredLevelPrice();
			}
			result = result + (stockQty - fixedQty) * stockLevelPrices;
		}
		return result;
	}
	
	public void updateStatusObject(List<Stock> stockList) throws Exception {
		Map<String, List<Stock>> gmrStockMap = new HashMap<String, List<Stock>>();
		String qtyUnitId = "";
		double qty = 0;
		String qtyVal = "";
		for (Stock st : stockList) {
			qty = qty + st.getQty();
			if (null == st.getFixationUsed()) {
				continue;
			}
			qtyUnitId = st.getItemQtyUnitId(); //added Item Qty
			qtyVal = st.getItemQtyUnit();
			if (gmrStockMap.containsKey(st.getGMRRefNo())) {
				List<Stock> prevVal = gmrStockMap.get(st.getGMRRefNo());
				prevVal.add(st);
				gmrStockMap.put(st.getGMRRefNo(), prevVal);
			} else {
				List<Stock> freshList = new ArrayList<Stock>();
				freshList.add(st);
				gmrStockMap.put(st.getGMRRefNo(), freshList);
			}
		}
		for(String gmrRef : gmrStockMap.keySet()) {
			List<Stock> stockListForSingleGMR = gmrStockMap.get(gmrRef);
			double fixedQty = 0;
			GMRStatusObject gmrStatusObject = new GMRStatusObject();
			for (Stock st : stockListForSingleGMR) {
				List<FixationObject> fixationUsed = st.getFixationUsed();
				gmrStatusObject.setInternalContractRefNo(st.getContractRefNo());
				gmrStatusObject.setInternalContractItemRefNo(st.getContractItemRefNo());
				gmrStatusObject.setGmrRefNo(st.getGMRRefNo());
				if (null != fixationUsed && fixationUsed.isEmpty()) {
					continue;
				}
				for(FixationObject fixation: fixationUsed) {
					fixedQty = fixedQty + fixation.getFixationQty();
				}
				List<FixationObject> fixations = gmrStatusObject.getFixationUsed();
				if(null == fixations) {
					fixations = new ArrayList<FixationObject>();
				}
				if (null != fixationUsed) {
					fixations.addAll(fixationUsed);
				}
				gmrStatusObject.setFixationUsed(fixations);
			}
			if(null == gmrStatusObject.getFixationUsed() || gmrStatusObject.getFixationUsed().size()==0) {
				continue;
			}
			gmrStatusObject.setGmrQty(Double.toString(qty));
			gmrStatusObject.setQtyUnitId(qtyUnitId);
			gmrStatusObject.setGmrFixedQty(fixedQty);
			gmrStatusObject.setGmrUnFixedQty(qty - fixedQty);
			if(qtyVal.isEmpty()) {
				qtyVal = mdmFetcher.getContractQty(null, qtyUnitId, "");
			}
			if(qty - fixedQty > 0) {
				gmrStatusObject.setGmrStatus("PARTIALLY FIXED");
			}
			else {
				gmrStatusObject.setGmrStatus("FULLY FIXED");
			}
			gmrStatusObject.setQtyUnitVal(qtyVal);
			gmrStatusObject = removeDuplicatesForGMRStatusObject(gmrStatusObject);
			if(!curveService.checkNegative(gmrStatusObject.getGmrUnFixedQty())) {
				gmrStatusObjectCreationHelper.createGMRStatusObject(gmrStatusObject);
			}
		}
		
	}
	
	public Map<LocalDate, Double> sortDateMapByDate(Map<LocalDate, Double> dateMap) {
		Map<LocalDate, Double> sorted = new TreeMap<>();
		sorted.putAll(dateMap);
		return sorted;
	}
	
	public GMRStatusObject removeDuplicatesForGMRStatusObject(GMRStatusObject gmrStatusObject) {
		Map<String, FixationObject> fixationMap = new HashMap<String, FixationObject>();
		for (FixationObject fixation : gmrStatusObject.getFixationUsed()) {
			if (fixationMap.containsKey(fixation.getFixatonNumber())) {
				FixationObject updatedFixation = fixationMap.get(fixation.getFixatonNumber());
				updatedFixation.setFixationQty(updatedFixation.getFixationQty() + fixation.getFixationQty());
				updatedFixation.setFixedPrice(updatedFixation.getFixedPrice() + fixation.getFixedPrice());
				List<JSONObject> stocks = updatedFixation.getStocks();
				stocks.addAll(fixation.getStocks());
				fixationMap.put(fixation.getFixatonNumber(), updatedFixation);
			}
			else {
				fixationMap.put(fixation.getFixatonNumber(), fixation);
			}
		}
		Set<Entry<String, FixationObject>> fixationEntries = fixationMap.entrySet();
		List<FixationObject> fixationUsed = new ArrayList<FixationObject>();
		for(Entry<String, FixationObject> e: fixationEntries) {
			fixationUsed.add(e.getValue());
		}
		gmrStatusObject.setFixationUsed(fixationUsed);
		return gmrStatusObject;
	}
	
	public boolean isGMRValidForFixation(String sysCreatedOn, String gmrCreationDate) throws PricingException {
//		DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
		if(gmrCreationDate.isEmpty()) {
			return true;
		}
		LocalDateTime gmrCreatedDate;
		LocalDateTime fixationCreatedDate;
		try {
			if(sysCreatedOn.contains("+")) {
				fixationCreatedDate = curveDataFetcher.convertISOtoLocalDateTime(sysCreatedOn);
			}
			else {
				fixationCreatedDate = Instant.ofEpochMilli(Long.parseLong(sysCreatedOn)).atZone(ZoneId.systemDefault())
						.toLocalDateTime();
			}
			if(gmrCreationDate.contains(".")) {
				gmrCreationDate = gmrCreationDate.substring(0, gmrCreationDate.indexOf("."));
			}
			gmrCreatedDate = Instant.ofEpochMilli(Long.parseLong(gmrCreationDate)).atZone(ZoneId.systemDefault())
					.toLocalDateTime();
			if(gmrCreatedDate.isAfter(fixationCreatedDate)) {
				return true;
			}
		}
		catch (Exception e) {
			logger.error(Logger.EVENT_FAILURE, "Invalid dates : ", e);
			throw new PricingException(messageFetcher.fetchErrorMessage(null, "024", new ArrayList<String>()));
		}
		return false;
	}
	
	public double validGMRPriceForAdjustments(List<GMRStatusObject> statusObjectList, List<Double> stockPriceList,
			JSONObject movObj, List<TriggerPrice> triggerPriceList) {
		double validGMRPrice = 0;
		double gmrQty = 0;
		double weightAvgPrice = 0;
		String weightAvgFlag = null;
		if(movObj.has("isWeightAvgFlag")) {
		weightAvgFlag = movObj.getString("isWeightAvgFlag");
		}
		for (GMRStatusObject statusObj : statusObjectList) {
			if (movObj.optString("refNo").equals(statusObj.getGmrRefNo())) {
				Map<String, List<FixationObject>> stockQtyFixedMap = new HashMap<String, List<FixationObject>>();
				for (FixationObject fixation : statusObj.getFixationUsed()) {
//					double qty = fixation.getFixationQty();
//					double price = fixation.getFixedPrice();// its qty*price
					List<JSONObject> stocksUsed = fixation.getStocks();
					for (JSONObject st : stocksUsed) {
						String stock = st.optString("stock");
						if (stockQtyFixedMap.containsKey(stock)) {
							List<FixationObject> fixationValue = stockQtyFixedMap.get(stock);
							fixationValue.add(fixation);
							stockQtyFixedMap.put(stock, fixationValue);
						} else {
							List<FixationObject> fixationValue = new ArrayList<FixationObject>();
							fixationValue.add(fixation);
							stockQtyFixedMap.put(stock, fixationValue);
						}
					}

				}
				JSONArray stocks = movObj.optJSONArray("stocks");
				try {
					for(int i=0; i<stocks.length(); i++) {
						JSONObject stockObj = stocks.optJSONObject(i);
						gmrQty = gmrQty + stockObj.optDouble("qty");
						double stockQty = stockObj.optDouble("qty");
						for(FixationObject fixation: stockQtyFixedMap.get(stockObj.optString("refNo"))) {
							double price = fixation.getFixedPrice()/fixation.getFixationQty();
							TriggerPrice validFixation = getValidFixation(triggerPriceList, fixation.getFixatonNumber());
							if (null != validFixation) {
								price = validFixation.getPrice();
							}
							for(JSONObject jObj : fixation.getStocks()) {
								if(!jObj.optString("stock").equals(stockObj.optString("refNo"))) {
									continue;
								}
								stockQty = stockQty - fixation.getFixationQty();
								validGMRPrice = validGMRPrice + price*fixation.getFixationQty();
							}
						}
						weightAvgPrice =  validGMRPrice/statusObj.getGmrFixedQty();
						if("Y".equalsIgnoreCase(weightAvgFlag)) {
							validGMRPrice = validGMRPrice + stockQty* weightAvgPrice;
						}else {
						    validGMRPrice = validGMRPrice + stockQty*stockPriceList.get(i);
						}
					}
				}
				catch (NullPointerException e) {
					logger.error(Logger.EVENT_FAILURE, "GMR modification case", e);
					return 0;
				}
			}
		}
		validGMRPrice = validGMRPrice / gmrQty;
		return validGMRPrice;

	}
	
	public TriggerPrice getValidFixation(List<TriggerPrice> triggerPriceList, String fixationNo) {
		for(TriggerPrice tp: triggerPriceList) {
			if(tp.getFixationRefNo().equalsIgnoreCase(fixationNo)) {
				return tp;
			}
		}
		return null;
	}
}