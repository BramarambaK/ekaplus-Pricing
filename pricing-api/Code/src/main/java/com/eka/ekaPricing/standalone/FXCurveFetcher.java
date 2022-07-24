package com.eka.ekaPricing.standalone;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import com.eka.ekaPricing.pojo.FXDetails;
import com.eka.ekaPricing.pojo.PricingProperties;
import com.eka.ekaPricing.service.CurveService;
import com.eka.ekaPricing.util.CommonValidator;
import com.eka.ekaPricing.util.ContextProvider;

@Component
public class FXCurveFetcher {

	@Autowired
	CollectionDataFetcher collectionFetcher;
	@Autowired
	ContextProvider context;
	@Autowired
	RestTemplateGetRequestBodyFactory restTemplateGetWityBody;
	@Autowired
	CurveDataFetcher curveDataFetcher;
	@Autowired
	CurveService curveService;
	@Autowired
	ErrorMessageFetcher messageFetcher;
    @Autowired
	CommonValidator validator;
	//private String clientURL = pricingProps.getPlatform_url();

	@Value("${eka.fx.param}")
	private String paramVals;

	@Value("${eka.fx.param}")
	private String collection;
	
	@Value("${eka.contract.url}")
	private String connectHost;

	@Value("${eka.pricing.udid}")
	private String pricingUDID;
	final static  org.owasp.esapi.Logger logger = ESAPI.getLogger(CollectionDataFetcher.class);

	public double getFXRate(String base, String secondary, Date tradeDate) throws Exception {
		PricingProperties pricingProps = context.getCurrentContext().getPricingProperties();
		String accessToken = collectionFetcher.getAccessToken();
		String clientURL=pricingProps.getPlatform_url();
		long mills = convertDateToMills(tradeDate);
		String filter = "filter=%7BBase Currency Code:\"" + secondary + "\"," + "Secondary Currency Code:\"" + base
				+ "\"," + "Trade Date:" + "%7Btype: \"date\",valueinms:" + mills + ",operator:\"eq\"%7D" + "%7D";
		String uri = validator.cleanData(clientURL + "/spring/smartapp/collection/data/?" + paramVals + "&" + filter);

		Map<String, String> uriVariables = new HashMap<>();
		uriVariables.put("CollectionName", collection);
		RestTemplate template = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		headers.add("Authorization", validator.cleanData(accessToken));
		HttpEntity entity1 = new HttpEntity(headers);
		HttpEntity<String> data = null;
		try {
			data = template.exchange(uri, HttpMethod.GET, entity1, String.class, uriVariables);
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception();
		}
		JSONObject obj = new JSONObject(data.getBody()).getJSONArray("data").getJSONObject(0);
		double fxRate = (Double) obj.get("Universal Close Price");
		return 1 / fxRate;
	}

	public long convertDateToMills(Date tradeDate) {
//		SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
		Date date = tradeDate;
//		try {
//			date = (Date) formatter.parse("10-09-2018");
//		} catch (ParseException e) {
//			e.printStackTrace();
//		}
		long mills = date.getTime();
		return mills;
	}
	
	public Map<LocalDate, FXDetails> getFxRatesFromCurve(String curveName, LocalDate fromDate, LocalDate toDate, String accessToken,
			String fromUnit, String toUnit, LocalDate asOf) throws PricingException {
		
		Map<LocalDate, FXDetails> fxMap = new HashMap<LocalDate, FXDetails>();

		String uri = connectHost + "/collectionmapper/" + pricingUDID+"/"+pricingUDID+"/fetchCollectionRecords";
		JSONObject fieldName = new JSONObject();
		JSONArray filterArr = new JSONArray();
		
		JSONObject obj3 = new JSONObject();
		obj3.put("fieldName", "Instrument Name");
		obj3.put("value", curveName);
		obj3.put("operator", "eq");
		JSONArray sortArr = new JSONArray();
		JSONObject sortObj = new JSONObject();
		sortObj.put("fieldName", "Value Date");
		sortObj.put("direction", "ASC");
		sortArr.put(sortObj);
		fieldName.put("sort", sortArr);
		
		filterArr.put(obj3);
		fieldName.put("filter", filterArr);
		
		JSONObject outerObj = new JSONObject();
		outerObj.put("skip", "0");
		outerObj.put("limit", "10000");
		outerObj.put("collectionName", "DS-Market Fx Rate");
		
		fieldName.put("filter", filterArr);
		outerObj.put("criteria", fieldName);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add("Authorization", context.getCurrentContext().getToken());
		headers.add("X-TenantID", context.getCurrentContext().getTenantID());
//		headers.add("X-Remote-User", "ekaApp");
		headers.add("ttl", "100");
		if(!StringUtils.isEmpty(context.getCurrentContext().getRequestId())) {
			headers.add("requestId", context.getCurrentContext().getRequestId());
		}
		if(!StringUtils.isEmpty(context.getCurrentContext().getSourceDeviceId())) {
			headers.add("sourceDeviceId", context.getCurrentContext().getSourceDeviceId());
		}
		RestTemplate restTemplate = restTemplateGetWityBody.getRestTemplate();
		HttpEntity<String> entity = new HttpEntity<String>(outerObj.toString(), headers);
		try {
			logger.info(Logger.EVENT_SUCCESS,
					ESAPI.encoder().encodeForHTML("collection fetcher major- entity for FX curve: " + entity));
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
			JSONObject fxOuterObj = new JSONObject();
			fxOuterObj.put("data", new JSONArray(response.getBody()));
			fxMap = processFXPrices(fxOuterObj, asOf,fromDate, toDate,fromUnit,toUnit);
		} catch (Exception e) {
			logger.error(Logger.EVENT_FAILURE,
					ESAPI.encoder().encodeForHTML("exception at " + uri + " : " + e.getMessage()));
			throw new PricingException(
					messageFetcher.fetchErrorMessage(context, "042", new ArrayList<String>()));
		}
		return fxMap;
	}
	
	public Map<LocalDate, Double> processFX(JSONObject obj, String fromUnit, String toUnit) throws PricingException {
		Map<LocalDate, Double> fxMap = new TreeMap<LocalDate, Double>();
		JSONArray fxArray = obj.getJSONArray("data");
		int i = 0;
		while (i < fxArray.length()) {
			JSONObject fxObj = fxArray.getJSONObject(i);
			LocalDate date = curveDataFetcher.convertISOtoLocalDate(fxObj.optString("Value Date") + "+00:00");
//			double fxRate = fxObj.optDouble("Universal Close Price");
			double fxRate = fxObj.optDouble("Exchange Rate");
			if(curveService.checkZero(fxRate)) {
				throw new PricingException("FX rate is zero for : "+date);
			}
			if(fromUnit.equals(fxObj.optString("From Currency")) && toUnit.equals(fxObj.optString("To Currency"))) {
				fxMap.put(date, fxRate);
			}
			else if(toUnit.equals(fxObj.optString("From Currency")) && fromUnit.equals(fxObj.optString("To Currency"))) {
				fxMap.put(date, 1/fxRate);
			}
			else {
				throw new PricingException("From and To currency not matching with that of FX curve provided");
			}
			i++;
		}
		
		return fxMap;
	}
	
	public Map<LocalDate, FXDetails> processFXPrices(JSONObject obj,  LocalDate asOf,LocalDate fromDate, LocalDate toDate,
			String fromUnit, String toUnit) throws PricingException {
        Map<LocalDate, FXDetails> fxMap = new HashMap<LocalDate, FXDetails>();
        LocalDate date=fromDate.plusDays(1);
        while(!fromDate.isAfter(toDate)) {
        	if (date.isAfter(toDate.minusDays(1))) {
				break;
			}
        	FXDetails fx = processFXBestPrices(obj, date, asOf,fromUnit, toUnit);
            if(curveService.checkNegative(fx.getFxValue())) {
            	if(fxMap.size()>0) {
            		fx = fxMap.get(date.minusDays(1));
            	}else {
            	    fx = processFXBestPricesAsOfNotEqualPeriodDate(obj, date, asOf,fromUnit, toUnit);
            	}
            }
            if(curveService.checkZero(fx.getFxValue())) {
                List<String> params = new ArrayList<String>();
                params.add(date.toString());
                throw new PricingException(messageFetcher.fetchErrorMessage(context, "050", new ArrayList<String>()));
            }
            fxMap.put(date, fx);
            date=date.plusDays(1);
           
        }
      
        return fxMap;
    }
    public FXDetails processFXBestPrices(JSONObject obj, LocalDate date, LocalDate asOf,
    		String fromUnit, String toUnit) throws PricingException {
        JSONArray fxArray = obj.getJSONArray("data");
        int i = 0;
        double bestFx = 0;
        String fxDef=null;
        FXDetails fxDetails= new FXDetails();
        double bestFxForLesserPeriodEndDate = -1;
        while (i < fxArray.length()) {
            JSONObject fxObj = fxArray.getJSONObject(i);
            LocalDate valueDate;
            LocalDate periodEndDate = curveDataFetcher.convertISOtoLocalDate(fxObj.optString("Period End Date") + "+00:00");
            try {
            	valueDate = curveDataFetcher.convertISOtoLocalDate(fxObj.optString("Value Date") + "+00:00");
            } catch (Exception e) {
            	throw new PricingException(messageFetcher.fetchErrorMessage(context, "051", new ArrayList<String>()));
            }
            double fxRate = fxObj.optDouble("Exchange Rate");
            if(toUnit.equals(fxObj.optString("From Currency")) && fromUnit.equals(fxObj.optString("To Currency"))) {
				fxRate=1/fxRate;
			}
            if(!periodEndDate.isAfter(asOf)) {
            	if(valueDate.isBefore(date)) {
            		bestFxForLesserPeriodEndDate = fxRate;
            		fxDef="l";
            		i++;
            		continue;
                }
            	else if(valueDate.equals(date)) {
            		fxDetails.setFxValue(fxRate);
                	fxDetails.setFxDef("l");
                    return fxDetails;
            	}
            	else{
            		fxDetails.setFxValue(bestFxForLesserPeriodEndDate);
                	fxDetails.setFxDef(fxDef);
                    return fxDetails;
            	}
            }
            if(!periodEndDate.isEqual(asOf)) {
            	fxDetails.setFxValue(-1);
            	fxDetails.setFxDef("forward");
                return fxDetails;
            }
            if(valueDate.isBefore(date)) {
            	bestFx = fxRate;
        		fxDef="l";
            }
            else if(date.isEqual(valueDate)){
            	 bestFx = fxRate;
             	if(date.isBefore(asOf) || date.isEqual(asOf)) {
                 	fxDef="actuals";
             	}else {
             		fxDef="forward";
             	}
             	
            }
            else {
            	fxDetails.setFxValue(bestFx);
            	fxDetails.setFxDef(fxDef);
                return fxDetails;
            }
            i++;
        }
        
        return fxDetails;
    }
    
    public FXDetails processFXBestPricesAsOfNotEqualPeriodDate(JSONObject obj, LocalDate date, LocalDate asOf,
    		String fromUnit, String toUnit) throws PricingException {
        JSONArray fxArray = obj.getJSONArray("data");
        int i = 0;
        double bestFx = 0;
        String fxDef = null;
        FXDetails fxDetails= new FXDetails();
        while (i < fxArray.length()) {
            JSONObject fxObj = fxArray.getJSONObject(i);
            LocalDate valueDate;
            try {
            	valueDate = curveDataFetcher.convertISOtoLocalDate(fxObj.optString("Value Date") + "+00:00");
            } catch (Exception e) {
            	throw new PricingException(messageFetcher.fetchErrorMessage(context, "051", new ArrayList<String>()));
            }
            double fxRate = fxObj.optDouble("Exchange Rate");
            if(toUnit.equals(fxObj.optString("From Currency")) && fromUnit.equals(fxObj.optString("To Currency"))) {
				fxRate=1/fxRate;
			}
            //fx value not available for the particular date
            if(valueDate.isBefore(date)) {
            		bestFx = fxRate;
            		fxDef="l";
                
            }
          //fx value  available for the particular date
            else if(date.isEqual(valueDate)){
            	 bestFx = fxRate;
            	if(date.isBefore(asOf) || date.isEqual(asOf)) {
                	fxDef="actuals";
            	}else {
            		fxDef="forward(Estimated)";
            	}
            	
            }
          //fx value not available for the particular date
            else {
            	fxDetails.setFxValue(bestFx);
            	fxDetails.setFxDef(fxDef);
                return fxDetails;
            }
            i++;
        }
        fxDetails.setFxValue(bestFx);
    	fxDetails.setFxDef(fxDef);
        return fxDetails;
    }
	
}
