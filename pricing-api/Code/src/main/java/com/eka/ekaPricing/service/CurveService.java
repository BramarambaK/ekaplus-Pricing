package com.eka.ekaPricing.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.eka.ekaPricing.exception.PricingException;
import com.eka.ekaPricing.helper.HolidayRuleCalculator;
import com.eka.ekaPricing.pojo.CurveDetails;
import com.eka.ekaPricing.pojo.HolidayRuleDetails;
import com.eka.ekaPricing.pojo.PDAdjustment;
import com.eka.ekaPricing.pojo.PDRule;
import com.eka.ekaPricing.pojo.PDSchedule;
import com.eka.ekaPricing.pojo.PriceDifferential;
import com.eka.ekaPricing.pojo.PricingProperties;
import com.eka.ekaPricing.pojo.QualityAdjustment;
import com.eka.ekaPricing.pojo.QualityAttributes;
import com.eka.ekaPricing.pojo.Stock;
import com.eka.ekaPricing.repository.CurveRepository;
import com.eka.ekaPricing.standalone.ErrorMessageFetcher;
import com.eka.ekaPricing.standalone.FXCurveFetcher;
import com.eka.ekaPricing.standalone.PDAdjustmentFetcher;
import com.eka.ekaPricing.standalone.PDRuleFetcher;
import com.eka.ekaPricing.util.ContextProvider;
import com.google.gson.Gson;

@Service
public class CurveService {

	private static final String PRICE_DIFFERENTIAL_TYPE_PREMIUM = "Premium";
	private static final String PRICE_DIFFERENTIAL_TYPE_DISCOUNT = "Discount";
	private static final String PRICE_DIFFERENTIAL_TYPE_SCURVE = "S-Curve";
	private static final String QUALITY_ADJUSTMENT_TYPE_LINEAR = "Linear";
	private static final String QUALITY_ADJUSTMENT_TYPE_STEP = "Flat";	
	private static final String QUALITY_ADJUSTMENT_TYPE_RANGE = "Range";
	private static final String DEFAULT_PRICE_VALUE = "0.0";
	private static final String PREMIUM = "Premium";
	private static final String DISCOUNT = "Discount";
	private static final String RANGE_ASC = "asc";
	private static final String ABSOLUTE = "Rate";
	private static final String POSITIVE = "Positive";
	private static final String NEGATIVE = "Negative";
	private static final String INDEPENDENT = "Independent";
	private static final String CUMULATIVE = "Cumulative";

	private static final String priceTypeArray[] = {"futures", "spots"}; 

	private static final Logger logger = ESAPI.getLogger(CurveService.class);

	@Value("${collections.url}")
	private String collectionURL;
	@Value("${holiday.DS.name}")
	private String holidayCollectionName;
	@Value("${eka.curve.seed.url}")
	private String curveSeedURL;	

	@Autowired
	private HolidayRuleCalculator holidayCalculator;

	@Autowired
	private CurveRepository curveRepository;

	@Autowired 
	FXCurveFetcher rateFetcher;
	
	@Autowired
	PDRuleFetcher pdRuleFetcher;

	@Autowired
	RestTemplate restTemplate;
	
	@Autowired
	PDAdjustmentFetcher pdAdjustmentFetcher;
	
	@Autowired
	ErrorMessageFetcher messageFetcher;
	
	@Autowired
	ContextProvider context;

	@Value("${eka.seedcurvedata.url}")
	private String seedcurvedataURL;

	public String applyHolidayRule(HolidayRuleDetails holidayRuleDtl, ContextProvider contextProvider) throws PricingException {
		if (null == context) {
			context = contextProvider;
		}
		PricingProperties pricingProps = context.getCurrentContext().getPricingProperties();
		String exchangeName = holidayRuleDtl.getExchangeName();
		String holidayRule = holidayRuleDtl.getHolidayRule();
		List<LocalDateTime> dateRange = holidayRuleDtl.getDateRange();		
		List<JSONObject> holidayDataSet = new ArrayList<>();		
		String uri = pricingProps.getPlatform_url()+collectionURL+holidayCollectionName+"&limit=1000";		
		HttpHeaders headers = new HttpHeaders();
		String token =context.getCurrentContext().getToken();		
		headers.set("Authorization", token);
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		headers.add("Content-Type", "application/json");		
		headers.add("X-Locale", context.getCurrentContext().getLocale());
		headers.add("X-TenantID", context.getCurrentContext().getTenantID());
		headers.add("X-Remote-User", "ekaApp");
		HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);
		ResponseEntity<Object> result = restTemplate.exchange(uri, HttpMethod.GET, entity
				, Object.class);		
		List<LocalDateTime> holidayList =  new ArrayList<LocalDateTime>();
		if(result != null) {						
			Object obj = result.getBody();
			Object data = ((Map)obj).get("data");			
			for (Object ob : ((List<Object>)data)) {
				String holidayDate = (String)((Map)ob).get("Holiday Date");	
				String exchName = (String)((Map)ob).get("Exchange");			 
				if(exchName.equalsIgnoreCase(exchangeName)) {
					holidayList.add(LocalDateTime.parse(holidayDate));
				}
			}			
		}
		else {
			throw new PricingException(
					messageFetcher.fetchErrorMessage(context, "036", new ArrayList<String>()));
		}
		holidayDataSet = holidayCalculator.processHolidayRule(holidayRule,dateRange,holidayList);
		return holidayDataSet.toString();
	}

	/**
	 * Method will be called to seed base curve names of type futures
	 * @param token
	 * @param tenantID
	 * @param appName
	 * @param objName
	 */
	public void seedCurveData(ContextProvider contextProvider,String appName,String objName) {
		if (null == context) {
			context = contextProvider;
		}
		PricingProperties pricingProps = context.getCurrentContext().getPricingProperties();
		String uri = pricingProps.getPlatform_url()+curveSeedURL;				
		HttpHeaders headers = new HttpHeaders();
		String token =context.getCurrentContext().getToken(); 		
		headers.set("Authorization", token);
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		headers.add("Content-Type", "application/json");		
		headers.add("X-Locale", context.getCurrentContext().getLocale());
		headers.add("X-TenantID", context.getCurrentContext().getTenantID());
		headers.add("clientId", "0");
		HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);	
		List<JSONObject> curveList = new ArrayList<JSONObject>();
		try {
			ResponseEntity<Object> result = restTemplate.exchange(uri, HttpMethod.GET, entity, Object.class);		
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
					String priceUnit = (String)((Map)ob).get("price_unit");		
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
					dataObj.accumulate("priceUnit", priceUnit);
					curveList.add(dataObj);
				}			
			}
			else {
				throw new PricingException(
						messageFetcher.fetchErrorMessage(context, "037", new ArrayList<String>()));
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("data",curveList);
		curveRepository.upsertBaseCurves("baseCurveDetails", map, objName, appName);		
	}	


	/**
	 * Method will be called to seed base curve names of All Price Types
	 * @param token
	 * @param tenantID
	 * @param appName
	 * @param objName
	 * @throws PricingException 
	 */
	@SuppressWarnings("rawtypes")
	public void seedCurveDataForAllPriceTypes(ContextProvider contextProvider,String appName,String objName)
			throws HttpStatusCodeException,HttpClientErrorException, RestClientException, PricingException  {
		if (null == context) {
			context = contextProvider;
		}
		logger.info(Logger.EVENT_SUCCESS,ESAPI.encoder().encodeForHTML("Curve Data For All Price Types API- Inside seedCurveDataForAllPriceTypes Method - Initiated"));
		PricingProperties pricingProps = context.getCurrentContext().getPricingProperties();
		Gson gson = new Gson();
		String uri = pricingProps.getPlatform_url()+seedcurvedataURL;

		String token =context.getCurrentContext().getToken();

		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", token);
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		headers.add("Content-Type", "application/json");		
		headers.add("X-Locale", context.getCurrentContext().getLocale());
		headers.add("X-TenantID", context.getCurrentContext().getTenantID());

		Map<String, List<String>> priceTypeObject = new HashMap<>();
		List<String> priceTypeList = new ArrayList<>();

		for(String priceType: priceTypeArray) {
			priceTypeList.add(priceType);
		}

		priceTypeObject.put("PriceTypes", priceTypeList);

		HttpEntity<Object> requestBody = new HttpEntity<Object>(gson.toJson(priceTypeObject),headers);

		LinkedHashMap<?, ?> responseMap = null;
		ResponseEntity<Object> responseEntity = null;

		List<JSONObject> curveList = new ArrayList<JSONObject>();
		try {
			
				logger.info(Logger.EVENT_SUCCESS,ESAPI.encoder().encodeForHTML("Making a POST call to get All price types at endpoint: " + uri + " with request payload: " + requestBody));

				responseEntity = restTemplate.exchange(uri, HttpMethod.POST, requestBody, Object.class);		
			
			if(null != responseEntity) {
				responseMap = (LinkedHashMap) responseEntity.getBody();
				if(null != responseMap) {
					responseMap.forEach((keys,values) -> {

						LinkedHashMap priceTypeValues = (LinkedHashMap) values;
						if(null != priceTypeValues) {
							LinkedHashMap<?, ?> priceTypedata = (LinkedHashMap) priceTypeValues.get("data");
							if(null != priceTypedata) {
								priceTypedata.forEach((k, v) -> {

									LinkedHashMap curveValues = (LinkedHashMap) v;

									if(null != curveValues) {
										String curveName = (String)curveValues.get("name");
										String pricePoint = (String)curveValues.get("price_point");
										String product = (String)curveValues.get("product");
										String access = (String)curveValues.get("access");
										String derivativeType = (String)curveValues.get("Derivative Type");
										String priceSubType = (String)curveValues.get("price_sub_type");
										String assetClass = (String)curveValues.get("asset_class");
										String tradeType = (String)curveValues.get("trade_type");				 
										String publisher = (String)curveValues.get("Publisher");				 
										String publishedExtrapolated = (String)curveValues.get("published_extrapolated");	
										String priceUnit = (String)curveValues.get("price_unit");		

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
										dataObj.accumulate("priceUnit", priceUnit);
										curveList.add(dataObj);

									}
								});
							}
						}
					});
				}

				Map<String, Object> map = new HashMap<String, Object>();
				map.put("data",curveList);

				curveRepository.upsertBaseCurves("baseCurveDetails", map, objName, appName);	

				logger.debug(Logger.EVENT_SUCCESS,ESAPI.encoder().encodeForHTML("Curve Data For All Price Types API- Inside seedCurveDataForAllPriceTypes Method - Ended"));

			}
			else {
				throw new PricingException(
						messageFetcher.fetchErrorMessage(context, "037", new ArrayList<String>()));
			}
		}catch(Exception e) {
			logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("Main Exception inside seedCurveDataForAllPriceTypes() "+e));
			throw new PricingException(
					messageFetcher.fetchErrorMessage(context, "037", new ArrayList<String>()));
		}
	}	




	/**
	 * This method applies price Differential options on the final price
	 * @param curveDtls
	 * @param pricePerDay
	 * @return
	 */
	public double applyDifferentialPrice(CurveDetails curveDtls,String pricePerDay, String contractCurr, Date tradeDate) {
		Optional<String> pricePerDayOpt =  Optional.ofNullable(pricePerDay);		
		double finalPrice =Double.parseDouble((pricePerDayOpt.orElse(DEFAULT_PRICE_VALUE)).toString()) ;
		double upperThreashold =0.0;
		double lowerThreashold = 0.0;
		double fxRate =0.0;
		String currencyUnit = "USD";
		Collections.sort(curveDtls.getPriceDifferentialList());			
		try {
			for(PriceDifferential diffPrice : curveDtls.getPriceDifferentialList()) {			
				if(diffPrice.getDifferentialType().equalsIgnoreCase(PRICE_DIFFERENTIAL_TYPE_PREMIUM)) {				
					finalPrice +=  diffPrice.getDifferentialValue();
					currencyUnit = diffPrice.getDifferentialUnit();
					if(currencyUnit == null || currencyUnit.isEmpty()) {
						currencyUnit = contractCurr;
					}
					if(!currencyUnit.equalsIgnoreCase(contractCurr)) {
						fxRate = getFxRate(currencyUnit, contractCurr, tradeDate);
						if(fxRate == 0.0) {
							finalPrice += finalPrice * fxRate;
						}else {
							finalPrice = finalPrice * fxRate;
						}			
					}
				}else if(diffPrice.getDifferentialType().equalsIgnoreCase(PRICE_DIFFERENTIAL_TYPE_DISCOUNT)) {				
					finalPrice -= diffPrice.getDifferentialValue();		
					currencyUnit = diffPrice.getDifferentialUnit();
					if(currencyUnit == null || currencyUnit.isEmpty()) {
						currencyUnit = contractCurr;
					}
					if(!currencyUnit.equalsIgnoreCase(contractCurr)) {
						fxRate = getFxRate(currencyUnit, contractCurr, tradeDate);
						if(fxRate == 0.0) {
							finalPrice += finalPrice * fxRate;
						}else {
							finalPrice = finalPrice * fxRate;
						}
					}
				}else if(diffPrice.getDifferentialType().equalsIgnoreCase(PRICE_DIFFERENTIAL_TYPE_SCURVE)) {
					if(checkZero(diffPrice.getDiffUpperThreshold()) && checkZero(diffPrice.getDiffLowerThreashold())) {
						continue;
					}
					upperThreashold += diffPrice.getDiffUpperThreshold();
					lowerThreashold += diffPrice.getDiffLowerThreashold();				
					if(finalPrice > upperThreashold) {
						finalPrice = upperThreashold;					
					}else if(finalPrice < lowerThreashold) {
						finalPrice = lowerThreashold;					
					}				
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return Math.abs(finalPrice);
	}	
	/**
	 * Method to get fx rate for the given date. if not exist for the given date go back one day till rate is found
	 * @param currencyUnit
	 * @param contractCurr
	 * @param tradeDate
	 * @return
	 */
	private double getFxRate(String currencyUnit,String contractCurr,Date tradeDate) {
		double fxRate = 0.0;
		LocalDate tempDate = null;
		DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern( "EEE MMM dd HH:mm:ss z uuuu"  );
		int hitCount = 0;
		try {				
			do { 				
				fxRate = rateFetcher.getFXRate(currencyUnit, contractCurr, tradeDate);				
				tempDate = LocalDate.parse(tradeDate.toString(),dateFormat);
				tempDate = tempDate.minusDays(1);
				tradeDate = Date.from(tempDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
				hitCount++;
			}while(fxRate == 0.0d && hitCount<=7);			
		}catch(Exception e) {
			e.printStackTrace();
		}
		return fxRate;
	}

	/**
	 * Method to apply quality adjustment on the stock price
	 * @param stockList
	 * @param contractItemQualityList
	 * @param price
	 * @return
	 */
	public double qualityAdjustment(List<Stock> stockList,List<QualityAttributes> contractItemQualityList,List<PDSchedule> itemPDList,String price,boolean forContract) {
		Optional<String> priceOpt = Optional.ofNullable(price);
		double finalPrice = Double.parseDouble((priceOpt.orElse(DEFAULT_PRICE_VALUE)).toString());
		double adjustedStockPrice = 0.0;
		String premiumOrDiscount = null;
		String absoluteOrPercent = null;
		double deliveredRefVal = 0.0;		
		String PDScheduleType = null;
		List<QualityAttributes> stckQlist = new ArrayList<QualityAttributes>();
		List<QualityAdjustment> qaList = new ArrayList<QualityAdjustment>();
		if(forContract && stockList == null) {//for contract pricing
			for(QualityAttributes contractItemQlt : contractItemQualityList) {
				for(PDSchedule pdSchedule : itemPDList) {						
					if(contractItemQlt.getName().equalsIgnoreCase(pdSchedule.getAttributeName())) {
						qaList = pdSchedule.getDetails();
						for(QualityAdjustment qa : qaList) {
							premiumOrDiscount = qa.getPremiumOrDiscount();	
							absoluteOrPercent = qa.getPdInPercOrRate();
							//qualityAttrType = qa.getQualityAttrType();
							PDScheduleType = qa.getRateType();
							deliveredRefVal = Double.parseDouble(contractItemQlt.getValue());
							if(PDScheduleType.equalsIgnoreCase(QUALITY_ADJUSTMENT_TYPE_RANGE)) {
								adjustedStockPrice = rangeAdjustment(finalPrice,deliveredRefVal,qaList);
								break;
							}
						}
					}
				}
			}			
		}
		if(null != stockList) {
			//for stock price
			for(Stock stock: stockList) {
				adjustedStockPrice = stock.getStockPrice();
				double stockPrice = stock.getStockPrice();
				stckQlist = stock.getAttributes();
				for(QualityAttributes stkQlty : stckQlist) {
					for(QualityAttributes contractItemQlt : contractItemQualityList) {
						for(PDSchedule pdSchedule : itemPDList) {						
							if(stkQlty.getName().equalsIgnoreCase(contractItemQlt.getName()) && stkQlty.getName().equalsIgnoreCase(pdSchedule.getAttributeName())) {
								qaList = pdSchedule.getDetails();
								for(QualityAdjustment qa : qaList) {
									premiumOrDiscount = qa.getPremiumOrDiscount();	
									absoluteOrPercent = qa.getPdInPercOrRate();
									//qualityAttrType = qa.getQualityAttrType();
									PDScheduleType = qa.getRateType();
									deliveredRefVal = Double.parseDouble(stkQlty.getValue());
									if(PDScheduleType.equalsIgnoreCase(QUALITY_ADJUSTMENT_TYPE_LINEAR)) {	
										adjustedStockPrice = linearAdjustment(stock.getStockPrice(),absoluteOrPercent,premiumOrDiscount,deliveredRefVal,contractItemQlt.getValue());															
									}else if(PDScheduleType.equalsIgnoreCase(QUALITY_ADJUSTMENT_TYPE_STEP)) {
										adjustedStockPrice = stepAdjustment(stock.getStockPrice(),absoluteOrPercent,premiumOrDiscount,deliveredRefVal,contractItemQlt.getValue(),qa);										
									}else if(PDScheduleType.equalsIgnoreCase(QUALITY_ADJUSTMENT_TYPE_RANGE)) {
										adjustedStockPrice = rangeAdjustment(stock.getStockPrice(),deliveredRefVal,qaList);
										break;
									}
								}
							}
						}
					}
				}
				stock.setPdPrice(adjustedStockPrice-stockPrice);
				stock.setStockPrice(adjustedStockPrice);
			}
		}

		return adjustedStockPrice;
	}	
	/**
	 * Method to calculate linear quality adjustment
	 * @param finalPrice
	 * @param qualityAttrType
	 * @param absoluteOrPercent
	 * @param premiumOrDiscount
	 * @param stockDeliveredRefVal
	 * @param contractQuality
	 * @return
	 */
	private double linearAdjustment(double finalPrice,String absoluteOrPercent,String premiumOrDiscount,double stockDeliveredRefVal,String contractQualityVal) {
		double stockPrice = 0.0;		
		double contractValue = Double.parseDouble(contractQualityVal);
		/*qualityAttrType = qualityAttrType == null ?qualityAttrType=POSITIVE:qualityAttrType;
		if(qualityAttrType.equalsIgnoreCase(POSITIVE) && stockDeliveredRefVal > contractValue) {
			stockPrice +=premiumOrDiscount.equalsIgnoreCase(PREMIUM)?finalPrice * (stockDeliveredRefVal / contractValue)
								:finalPrice * (contractValue / stockDeliveredRefVal);
		}
		if(qualityAttrType.equalsIgnoreCase(NEGATIVE) && stockDeliveredRefVal < contractValue) {
			stockPrice +=premiumOrDiscount.equalsIgnoreCase(DISCOUNT)?finalPrice * (stockDeliveredRefVal / contractValue)
					:finalPrice * (contractValue / stockDeliveredRefVal);
		}	*/
		stockPrice +=premiumOrDiscount.equalsIgnoreCase(PREMIUM)?finalPrice * (stockDeliveredRefVal / contractValue)
				:finalPrice * (contractValue / stockDeliveredRefVal);
		return stockPrice;
	}
	/**
	 * Method to calculate step quality adjustment
	 * @param price
	 * @param qualityAttrType
	 * @param absoluteOrPercent
	 * @param premiumOrDiscount
	 * @param stockDeliveredRefVal
	 * @param contractQuality
	 * @return
	 */
	private double stepAdjustment(double price,String absoluteOrPercent,String premiumOrDiscount,double stockDeliveredRefVal,String contractQualityVal,QualityAdjustment qlityAdjust) {
		double stockPrice = 0.0;
		double PDValue = 0.0;		
		double contractValue = Double.parseDouble(contractQualityVal);		
		PDValue = ((contractValue - stockDeliveredRefVal) /qlityAdjust.getStepSize())
				*qlityAdjust.getPdIncValue();
		PDValue = Math.abs(PDValue);
		if(PDValue > 0.0d) {
			if(premiumOrDiscount.equalsIgnoreCase(PREMIUM)) {
				stockPrice += absoluteOrPercent.equalsIgnoreCase(ABSOLUTE)?price + PDValue:price + (price * PDValue/100);
			}else  if(premiumOrDiscount.equalsIgnoreCase(DISCOUNT) ){
				stockPrice += absoluteOrPercent.equalsIgnoreCase(ABSOLUTE)?price - PDValue:price - (price * PDValue/100);
			}
		}
		return stockPrice;
	}
	/**
	 * method to calculate range type quality adjustment
	 * @param price
	 * @param qualityAttrType
	 * @param absoluteOrPercent
	 * @param premiumOrDiscount
	 * @param stockDeliveredRefVal
	 * @param contractQuality
	 * @return
	 */
	private double rangeAdjustment(double price,double stockDeliveredRefVal,List<QualityAdjustment> qaList) {		
		String absoluteOrPercent = null;
		String premiumOrDiscount = null;		
		double stockPrice = 0.0;
		String rangeOrder = null;
		String lowerRange = null;
		String upperRange = null;
		String rangeCompare = null;
		double basePD = 0.0;
		double incrementalVal =0.0;							
		String[] upperRangeSplit = null;			
		int rangeIndx = 0;
		double rangePD = 0.0;	
		double threshold = 0.0;
		double thresholdVal = 0.0;
		double PDValue = 0.0;
		for(QualityAdjustment qa : qaList)  {
			rangeOrder = qa.getRangeOrder();
			upperRange = qa.getRangeUpper();
			lowerRange = qa.getRangeLower();
			premiumOrDiscount = qa.getPremiumOrDiscount();
			absoluteOrPercent = qa.getPdInPercOrRate();
			basePD = qa.getBasePD();
			incrementalVal = qa.getPdIncValue();
			upperRangeSplit = upperRange.split("[^0-9]+");
			if(upperRangeSplit.length >1) {
				rangeCompare = upperRangeSplit[0];
				upperRange = upperRangeSplit[1];									
			}else {
				rangeCompare = ">=";									
			}
			if(qa.getRangeType().equalsIgnoreCase(INDEPENDENT)) {
				if(stockDeliveredRefVal > Double.parseDouble(lowerRange) && stockDeliveredRefVal <= Double.parseDouble(upperRange)) {
					if(rangeOrder.equalsIgnoreCase(RANGE_ASC)) {
						PDValue += basePD+((stockDeliveredRefVal - Double.parseDouble(lowerRange))/qa.getStepSize()) * incrementalVal;
					}else {
						PDValue += basePD+((Double.parseDouble(upperRange) - stockDeliveredRefVal)/qa.getStepSize()) * incrementalVal;
					}

					break;
				}
			}else if(qa.getRangeType().equalsIgnoreCase(CUMULATIVE)) {
				if( rangeIndx == 0) {
					thresholdVal = Double.parseDouble(upperRange);
					rangePD = (thresholdVal/qa.getStepSize()) * incrementalVal + basePD;
				}else {
					thresholdVal = rangeOrder.equalsIgnoreCase(RANGE_ASC)?Double.parseDouble(lowerRange):Double.parseDouble(upperRange);					
				}
				threshold = rangeOrder.equalsIgnoreCase(RANGE_ASC)?stockDeliveredRefVal - thresholdVal:thresholdVal - stockDeliveredRefVal;
				if(rangeIndx == (qaList.size() -1)) {//calculate rangePD for the last element in the range
					rangePD = basePD+(threshold/qa.getStepSize()) * incrementalVal;	
				}else if( rangeIndx > 0){//calculate rangePD for the mid elements in the range
					rangePD = basePD+((Double.parseDouble(upperRange) - Double.parseDouble(lowerRange))/qa.getStepSize()) * incrementalVal;
				}				
				PDValue += rangePD;
			}
			rangeIndx++;
		}//for

		if(premiumOrDiscount.equalsIgnoreCase(PREMIUM) ) {
			stockPrice += absoluteOrPercent.equalsIgnoreCase(ABSOLUTE)?price + PDValue:price + (price * PDValue/100);								
		}else if(premiumOrDiscount.equalsIgnoreCase(DISCOUNT) ) {
			stockPrice += absoluteOrPercent.equalsIgnoreCase(ABSOLUTE)?price - PDValue:price - (price * PDValue/100);
		}	
		return stockPrice;
	}	

	public boolean checkZero(double val) {
		if(BigDecimal.ZERO.compareTo(BigDecimal.valueOf(val))==0) {
			return true;
		}
		return false;
	}

	public boolean checkNegative(double val) {
		if(BigDecimal.ZERO.compareTo(BigDecimal.valueOf(val))==1) {
			return true;
		}
		return false;
	}

	public double applyPremiumDiscountOnStocks(List<QualityAttributes> pdAttributes, double stockPrice,
			double contractPrice, ContextProvider context, String internalContractItemRefNo) throws PricingException {
		double adjustmentvalue = 0d;
		double adjustedPrice = 0d;
		List<PDAdjustment> adjustmentList = pdAdjustmentFetcher.fetchAdjustments(context, internalContractItemRefNo);
		Map<String, Double> attributeMap = new HashMap<String, Double>();
		if(adjustmentList.isEmpty()) {
			return stockPrice;
		}
		for (QualityAttributes attribute : pdAttributes) {
			attributeMap.put(attribute.getName(), Double.parseDouble(attribute.getValue()));
		}
		if(attributeMap.isEmpty()) {
			return stockPrice;
		}
		for (PDAdjustment pdAdjustment : adjustmentList) {
			Map<String, PDRule> ruleMap = pdRuleFetcher.fetchPDRules(context, internalContractItemRefNo,
					pdAdjustment.getPdRuleName());
			PDRule rule = ruleMap.get(pdAdjustment.getPdRuleName() + " " + pdAdjustment.getProductAttribute());
			double attributeValue = attributeMap.get(pdAdjustment.getProductAttribute());
			double baseValue = rule.getBaseValue();
			double qtyProcessed = 0d;
			while (qtyProcessed < attributeValue) {
				switch (rule.getTickValueType()) {
				case "Absolute":
					if (attributeValue >= rule.getFromRange() && attributeValue <= rule.getToRange()) {
						adjustmentvalue = adjustmentvalue
								+ ((attributeValue - baseValue) / rule.getTickSize()) * rule.getTickValue();
						qtyProcessed = attributeValue;
					} else if (attributeValue >= rule.getFromRange() && attributeValue > rule.getToRange()) {
						adjustmentvalue = adjustmentvalue
								+ ((rule.getToRange() - baseValue) / rule.getTickSize()) * rule.getTickValue();
						// qtyProcessed = qtyProcessed + rule.getToRange();
						baseValue = rule.getToRange();
					}
					break;
				case "Percentage":
					if (attributeValue >= rule.getFromRange() && attributeValue <= rule.getToRange()) {
						adjustmentvalue = adjustmentvalue + ((attributeValue - baseValue) / rule.getTickSize())
								* (rule.getTickValue() * contractPrice / 100);
						qtyProcessed = attributeValue;
					} else if (attributeValue >= rule.getFromRange() && attributeValue > rule.getToRange()) {
						adjustmentvalue = adjustmentvalue
								+ ((rule.getToRange() - baseValue) / rule.getTickSize()) * rule.getTickValue();
					}
					break;
				}
			}
			if (rule.getAdjustmentType().equalsIgnoreCase("discount")) {
				adjustedPrice = stockPrice - adjustmentvalue;
			} else {
				adjustedPrice = stockPrice + adjustmentvalue;
			}
		}
		return adjustedPrice;
	}

}


