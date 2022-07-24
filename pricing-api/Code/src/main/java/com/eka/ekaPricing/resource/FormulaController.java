package com.eka.ekaPricing.resource;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.json.JSONObject;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

import com.eka.ekaPricing.curveBuilder.BuilderExecutor;
import com.eka.ekaPricing.exception.PricingException;
import com.eka.ekaPricing.pojo.Contract;
import com.eka.ekaPricing.pojo.Formula;
import com.eka.ekaPricing.pojo.FormulaCalculatorWrapper;
import com.eka.ekaPricing.pojo.MultipleContractPayload;
import com.eka.ekaPricing.pojo.PayloadInput;
import com.eka.ekaPricing.pojo.PriceFormulaDetails;
import com.eka.ekaPricing.pojo.TriggerPriceDetails;
import com.eka.ekaPricing.service.FormulaService;
import com.eka.ekaPricing.service.PricingDetailsService;
import com.eka.ekaPricing.service.TriggerPricingDetailsService;
import com.eka.ekaPricing.standalone.ContractDataFetcher;
import com.eka.ekaPricing.standalone.ContractReCalculationHelper;
import com.eka.ekaPricing.standalone.ContractsProcessor;
import com.eka.ekaPricing.standalone.EODProcessor;
import com.eka.ekaPricing.standalone.FixationCancellationHelper;
import com.eka.ekaPricing.standalone.FormulaeCalculator;
import com.eka.ekaPricing.standalone.GMRUpdationHelper;
import com.eka.ekaPricing.standalone.MassToVolumeConversion;
import com.eka.ekaPricing.standalone.PricingResultStoreHelper;
import com.eka.ekaPricing.util.CommonValidator;
import com.eka.ekaPricing.util.ContextProvider;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

@RestController
@RequestMapping("/api/pricing")
public class FormulaController {

	@Autowired
	private FormulaService formulaService;

	@Autowired
	FormulaeCalculator formulaeCal;

	@Autowired
	ContextProvider tenantProvider;

	@Autowired
	ContractsProcessor processor;
	
	@Autowired
	ContractDataFetcher contractFetcher;
	
	@Autowired
	BuilderExecutor builderExecutor;
	
	@Autowired
	PricingResultStoreHelper pricingResultStoreHelper;
	
	@Autowired
	CommonValidator validator;
	
	@Autowired
	ContractReCalculationHelper contractReCalculationHelper;
	
	@Autowired
	GMRUpdationHelper gmrUpdationHelper;
	
	@Autowired
	MassToVolumeConversion masstovolumeconversion;
	
	@Autowired
	EODProcessor eodProcessor;
	
	@Autowired
	private PricingDetailsService pricingService;
	
	@Autowired
	private TriggerPricingDetailsService triggerPricingDetailsService;
	
	@Autowired
	FixationCancellationHelper fixationCancellationHelper;
	
	final static  Logger logger = ESAPI.getLogger(FormulaController.class);

	@RequestMapping(value = "/calculateFormula", method = RequestMethod.POST)
	public @ResponseBody Formula calculateFormula(@RequestBody FormulaCalculatorWrapper requestWrapper,
			HttpServletRequest request) {
		Contract contract = requestWrapper.getContract();
		Formula formula = requestWrapper.getFormula();

		try {
			formula = formulaService.calulateFormula(contract, formula, request);
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			logger.error(null, ESAPI.encoder().encodeForHTML(e.getMessage()));
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			logger.error(null, ESAPI.encoder().encodeForHTML(e.getMessage()));
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error(null, ESAPI.encoder().encodeForHTML(e.getMessage()));
			e.printStackTrace();
		}

		return formula;
	}

	@PostMapping("/pricePreviewList")
	public @ResponseBody List<Object> priceListForPreview(@RequestBody FormulaCalculatorWrapper requestWrapper,
			HttpServletRequest request) {

		Contract contract = requestWrapper.getContract();
		Formula formula = requestWrapper.getFormula();
		return formulaService.priceListForPreview(contract, formula, request);
	}

	/*
	 * @PostMapping("/execute") public String processExpression(@Valid @RequestBody
	 * CurveDetails curveDetails) throws Exception { String expression =
	 * curveDetails.getExpression(); List<Curve> curveList =
	 * curveDetails.getCurveList();
	 * 
	 * return formulaeCal.processExpression(expression, curveList); }
	 * 
	 * // Controller to return the data-set for selected time range.
	 * 
	 * @PostMapping("/curve/data") public String getDatSet(@Valid @RequestBody
	 * CurveDetails curveDetails) throws Exception { String expression =
	 * curveDetails.getExpression(); List<Curve> curveList =
	 * curveDetails.getCurveList(); List<JSONObject> dataSet = new ArrayList<>();
	 * dataSet = formulaeCal.getPreviewDataSet(expression, curveList); return
	 * dataSet.toString(); }
	 */

	/*@PostMapping("/execute")
	public String executeFormula(@Valid @RequestHeader("Authorization") String token,
			@RequestHeader("X-TenantID") String tenantID, @RequestBody CurveDetails curveDetails) throws Exception {
		if (null != curveDetails.getContractList() && curveDetails.getContractList().size() != 0) {
			return processor.processContract(curveDetails.getContractList(), tenantProvider).toString();
		}
		return formulaeCal.createFinalResponse(curveDetails, tenantProvider).toString();
	}*/

	@PostMapping("/formula")
	public Object execute(@Valid @RequestHeader("Authorization") String token,
			@RequestHeader("X-TenantID") String tenantID, @RequestBody PayloadInput payload,
			@RequestParam("mode") String mode) throws Exception {
		try {
			Gson gson = new Gson();
			String payloadToStore = gson.toJson(payload).toString();
			JSONObject payloadToSToreObj = new JSONObject(payloadToStore);
			logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("payload : " + gson.toJson(payload)));
			String cacheKey = formulaeCal.createCacheKey(payload);
			String pricingOutput = "";
			String ttl = "3";
			if(cacheKey.toLowerCase().contains("gmr")) {
				ttl = "30";
			}
			Object cachedValue = formulaeCal.retrieveCacheStored(cacheKey, ttl);
			if (null == cachedValue) {
				pricingOutput = formulaeCal.createFinalResponseForPayload(payload.getContract(),
						payload.getFormulaList(), tenantProvider, validator.cleanData(mode)).toString();
				if (null == payload.getFormulaList() || payload.getFormulaList().isEmpty()) {
					pricingResultStoreHelper.storePricingResult(tenantProvider, payloadToSToreObj,
							new JSONObject(pricingOutput));
				}
				boolean isCachingSuccess = formulaeCal.storeCache(cacheKey, pricingOutput, ttl);
				if (!isCachingSuccess) {
					logger.error(Logger.EVENT_FAILURE,
							ESAPI.encoder().encodeForHTML("cache not stored for : " + gson.toJson(payload)));
				}
			} else {
				pricingOutput = validator.cleanData(cachedValue.toString());
			}
			logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("final pricing output : " + pricingOutput));
			return validator.cleanData(pricingOutput);
		} catch (HttpStatusCodeException ex) {

			logger.error(Logger.EVENT_FAILURE, "Failed at Formula Controller with HttpStatusCodeException: "
					+ ex.getMessage() + ex.getResponseBodyAsString(), ex);

			throw ex;

		} catch (Exception ex) {

			logger.error(Logger.EVENT_FAILURE, "Failed at Formula Controller in final exception: " + ex.getMessage(),
					ex);

			throw ex;

		}
	}

	@PostMapping("/formulaMultiple")
	public Object execute(@Valid @RequestHeader("Authorization") String token,
			@RequestHeader("X-TenantID") String tenantID, @RequestBody MultipleContractPayload payload,
			@RequestParam("mode") String mode) throws Exception {
		return processor.processContractFromPayload(payload.getPayloadList(), tenantProvider,validator.cleanData(mode)).toString();
	}

	@GetMapping("/formula/edit")
	public boolean checkEdit(@Valid @RequestHeader("Authorization") String token,
			@RequestHeader("X-TenantID") String tenantID,
			@RequestParam(value = "pricingFormulaId") String pricingFormulaId) {
		return !contractFetcher.checkFormulaInContract(token,tenantID,pricingFormulaId);
	}
	
	@GetMapping("/curve/builder")
	public Object build(@RequestHeader("Authorization") String token, @RequestHeader("X-TenantID") String tenantID)
			throws PricingException {
//		builder.getCurves(tenantProvider);
		JSONObject result = new JSONObject();
		result.accumulate("trigger", builderExecutor.executeBuilder());
		return result.toString();
	}
//	For reevaluating contracts
	@PostMapping("/contract/evaluate")
	public Object recalculate(@RequestBody List<RevaluationObject> revalList) throws Exception{
		Set<String> curveList = new HashSet<String>();
		for(RevaluationObject revalObj : revalList) {
			curveList.add(revalObj.getInstrumentName());
		}
		List<String> filteredContractref = contractReCalculationHelper.fetchFilteredContracts(new ArrayList<String>(curveList));

		return contractReCalculationHelper.reEvaulateContracts(filteredContractref);
	}
	
//	For GMR cancellation
	@PostMapping("/gmr/cancel")
	public boolean updateGMR(@RequestBody List<GMRModificationObject> payload) {
		try {
			for(GMRModificationObject modificationObj : payload) {
				boolean isCancel = false;
				if(modificationObj.getGmr().getOperationType().toLowerCase().contains("cancel")) {
					isCancel = true;
				}
				gmrUpdationHelper.updateGMR(modificationObj.getGmr(), modificationObj.getInternalContractItemRefNo(), isCancel);
			}
			return true;
		} catch (Exception e) {
			logger.error(Logger.EVENT_FAILURE, "Failed at GMR Cancellation Exception: " + e.getMessage(), e);
			return false;
		}
	}
	
	// Density Conversion
	@PostMapping("/densityConversion/massToVolume")
	public ResponseEntity<Map<String, Object>>massToVolume(@Valid ContextProvider context,@RequestBody Object payload) throws Exception{
		ObjectMapper mapper = new ObjectMapper();
		JSONObject payloadJson = new JSONObject();
		Map<String,Object> responseObject = new HashMap<String,Object>();	
		ResponseEntity<Map<String, Object>> response = null;
		try {
			payloadJson = new JSONObject(mapper.writeValueAsString(payload));
			responseObject=masstovolumeconversion.getConversionRate(context, payloadJson);
			response = new ResponseEntity<Map<String, Object>>(responseObject, HttpStatus.OK);
			logger.info(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("final massToVolume output : " + response));
		}  catch (Exception e) {
			logger.error(Logger.EVENT_FAILURE, "Failed at Density Conversion Exception: " + e.getMessage(), e);
		}
		return response;
	}
	
	@PostMapping("/eod/prepare")
	public Object prepareEOD(@RequestBody List<String> contracts) throws Exception {
		return contractReCalculationHelper.reEvaulateContracts(contracts);
	}
	
	@PostMapping("/eod/execute")
	public Object executeEOD(@RequestBody List<String> items) throws PricingException {
		return eodProcessor.processEOD(items).toString();
	}
	
	@GetMapping("/formula/pricingDetails")
	public ResponseEntity<Object> getPricingDetails(HttpServletRequest request) throws HttpClientErrorException, HttpStatusCodeException, RestClientException, ParseException{

		logger.debug(Logger.EVENT_SUCCESS,
				ESAPI.encoder().encodeForHTML("Pricing Formula details getting - Initiated"));

		List<PriceFormulaDetails> pricingDetails = pricingService.pricingDetails(request);
		logger.info(Logger.EVENT_SUCCESS,
				ESAPI.encoder().encodeForHTML("The final size of the pricingDetails is" + pricingDetails.size()));

		return ResponseEntity.status(HttpStatus.OK).body(pricingDetails);

	}
	
	@GetMapping("/formula/triggerPricingDetails")
	public ResponseEntity<Object> getTriggerPricingDetails(HttpServletRequest request) throws HttpClientErrorException, HttpStatusCodeException, RestClientException, ParseException{

		logger.debug(Logger.EVENT_SUCCESS,
				ESAPI.encoder().encodeForHTML("Pricing Formula details getting - Initiated"));

		List<TriggerPriceDetails> triggerPricingDetails = triggerPricingDetailsService.triggerPricingDetails(request);
		logger.info(Logger.EVENT_SUCCESS,
				ESAPI.encoder().encodeForHTML("The final size of the triggerPricingDetails is" + triggerPricingDetails.size()));

		return ResponseEntity.status(HttpStatus.OK).body(triggerPricingDetails);

	}
	
	@PostMapping("/fixation/cancel")
	public ResponseEntity<Object> cancelPostFixation(@RequestBody FixationCancellationObject cancellationObject) throws PricingException {
		fixationCancellationHelper.cancelPostFixation(cancellationObject);
		return ResponseEntity.status(HttpStatus.OK).build();
	}
}
