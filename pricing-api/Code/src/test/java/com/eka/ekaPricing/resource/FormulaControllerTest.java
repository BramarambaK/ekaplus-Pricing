package com.eka.ekaPricing.resource;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.eka.ekaPricing.pojo.Formula;
import com.eka.ekaPricing.pojo.PayloadInput;
import com.eka.ekaPricing.standalone.ContractsProcessor;
import com.eka.ekaPricing.standalone.FormulaeCalculator;
import com.eka.ekaPricing.util.ContextProvider;

@RunWith(MockitoJUnitRunner.Silent.class)
public class FormulaControllerTest {
	
	
	
	@InjectMocks
	FormulaController formulaController;
	
	@Mock
	ContractsProcessor processor;
	
	@Mock
	ContextProvider tenantProvider;
	
	@Mock
	FormulaeCalculator formulaeCal;
	
	/*@Test
	public void executeSuccessTest() throws Exception {
		
	
		CurveDetails curveDetails = new CurveDetails();
		List<String> contractList = new ArrayList<>();
		contractList.add("val1");
		contractList.add("val2");
		curveDetails.setContractList(contractList);
		String token = "auth";
		String tenantId = "id";
		List<JSONObject> myList  = new ArrayList<>();
		JSONObject obj1 = new JSONObject();
		obj1.put("name", "megha");
		myList.add(obj1);
		Mockito.when(processor.processContract(curveDetails.getContractList(), tenantProvider)).thenReturn(myList);
		 String expectedValue = myList.toString();
		String actualValue = formulaController.executeFormula(token, tenantId, curveDetails);
		assertEquals(expectedValue,actualValue);
	}
	@Test
	public void executeFailureTest() throws Exception {
		
		
		CurveDetails curveDetails = new CurveDetails();
		String token = "auth";
		String tenantId = "id";
		List<JSONObject> myList  = new ArrayList<>();
		JSONObject obj1 = new JSONObject();
		obj1.put("name", "author");
		myList.add(obj1);
		Mockito.when(formulaeCal.createFinalResponse(curveDetails, tenantProvider)).thenReturn(obj1);
		 String expectedValue = obj1.toString();
		String actualValue = formulaController.executeFormula(token, tenantId, curveDetails);
		assertEquals(expectedValue,actualValue);
	}*/
	
	@Test
	public void executeTest() throws Exception {
		PayloadInput payload = new PayloadInput();
		List<Formula> formulaList = new ArrayList<Formula>();
		payload.setFormulaList(formulaList);
		Formula f = new Formula();
		formulaList.add(f);
		JSONObject obj1 = new JSONObject();
		obj1.put("name", "author");
		String token = "auth";
		String tenantId = "id";
		String source = "Detailed";
		Mockito.when(formulaeCal.createFinalResponseForPayload(payload.getContract(), payload.getFormulaList(), tenantProvider, source)).thenReturn(obj1);
		String expectedValue = obj1.toString();
		String actualValue = formulaController.execute(token, tenantId, payload, source).toString();
		assertEquals(expectedValue,actualValue);
	}


}
