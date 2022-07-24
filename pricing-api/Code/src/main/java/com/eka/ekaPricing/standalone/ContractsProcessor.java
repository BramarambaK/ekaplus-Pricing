package com.eka.ekaPricing.standalone;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.eka.ekaPricing.pojo.ContextInfo;
import com.eka.ekaPricing.pojo.Contract;
import com.eka.ekaPricing.pojo.CurveDetails;
import com.eka.ekaPricing.pojo.Formula;
import com.eka.ekaPricing.pojo.MultipleContractPayload;
import com.eka.ekaPricing.pojo.PayloadInput;
import com.eka.ekaPricing.util.CommonValidator;
import com.eka.ekaPricing.util.ContextProvider;

@Component
public class ContractsProcessor {
	@Autowired
	FormulaeCalculator cal;
	static List<JSONObject> resultList = new ArrayList<JSONObject>();
	List<String> contractListFeed;
	List<PayloadInput> payloadContractsFeed;
	static ContextProvider context;
	ContextInfo t1;
	@Autowired
	CommonValidator validator;
	class ContractWorker implements Runnable {
		int j;

		public ContractWorker(int i) {
			j = i;
		}

		@Override
		public void run() {
			CurveDetails cDetail = new CurveDetails();
			cDetail.setContractID(contractListFeed.get(j));
			cDetail.setExecuteByContract(true);
			context.setCurrentContext(t1);
			try {
//				ContractsProcessor.resultList.add(cal.createFinalResponse(cDetail, context));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	public List<JSONObject> processContract(List<String> contractList, ContextProvider tenantProvider) {
		resultList.clear();
		contractListFeed = contractList;
		context = tenantProvider;
		t1 = tenantProvider.getCurrentContext();
		ExecutorService executor = Executors.newFixedThreadPool(5);

		for (int j = 0; j < contractList.size(); j++) {
			Runnable worker = new ContractWorker(j);
			executor.execute(worker);

		}
		executor.shutdown();
		while (!executor.isTerminated()) {
		}
		return resultList;
	}
	
	class PayloadContractWorker implements Runnable {
		int j;
		String mode;

		public PayloadContractWorker(int i, String mode) {
			j = i;
			this.mode = mode;
		}

		@Override
		public void run() {
//			CurveDetails cDetail = new CurveDetails();
//			cDetail.setContractID(contractListFeed.get(j));
//			cDetail.setExecuteByContract(true);
			context.setCurrentContext(t1);
			Contract contract = payloadContractsFeed.get(j).getContract();
			List<Formula> formulaList = payloadContractsFeed.get(j).getFormulaList();
			try {
				ContractsProcessor.resultList.add(cal.createFinalResponseForPayload(contract, formulaList, context, validator.cleanData(mode)));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
	
	public List<JSONObject> processContractFromPayload(List<PayloadInput> contractList, ContextProvider tenantProvider, String mode) {
		resultList.clear();
		payloadContractsFeed = contractList;
		context = tenantProvider;
		t1 = tenantProvider.getCurrentContext();
		ExecutorService executor = Executors.newFixedThreadPool(5);

		for (int j = 0; j < contractList.size(); j++) {
			Runnable worker = new PayloadContractWorker(j, validator.cleanData(mode));
			executor.execute(worker);

		}
		executor.shutdown();
		while (!executor.isTerminated()) {
		}
		return resultList;
	}
}
