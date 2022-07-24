package com.eka.ekaPricing.helper;

import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.eka.ekaPricing.exception.PricingException;
import com.eka.ekaPricing.pojo.CurveDetails;
import com.eka.ekaPricing.pojo.TriggerPrice;
import com.eka.ekaPricing.pojo.TriggerPriceProperties;
import com.eka.ekaPricing.util.ContextProvider;

public class TriggerPriceHelperTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	/*@Test
	public void applyTriggerPricePositiveTest() throws PricingException {

		TriggerPriceCalculator triggerPriceCalculator = new TriggerPriceCalculator();
		long itemQty = 100;
		String pricePerDay = "70";

		TriggerPrice triggerObject = new TriggerPrice();
		LocalDateTime today = LocalDateTime.now();
		String triggerDate = "2019-04-01T00:00:00+0000";
		int quantity = 20;
		int price = 70;
		triggerObject.setTriggerDate(triggerDate);
		triggerObject.setPrice(price);
		triggerObject.setQuantity(quantity);
		List<TriggerPrice> triggerPriceList = new ArrayList<>();
		triggerPriceList.add(triggerObject);

		CurveDetails curveDetails = new CurveDetails();
		curveDetails.setTriggerPriceList(triggerPriceList);

		double ExpectedValue = 70;
		double ActualValue = triggerPriceCalculator.applyTriggerPrice(curveDetails, itemQty, pricePerDay, today);
		assertEquals(ExpectedValue, ActualValue, 0.001);

	}

	@Test
	public void applyTriggerPriceNegativeTest() throws PricingException {

		TriggerPriceCalculator triggerPriceCalculator = new TriggerPriceCalculator();
		long itemQty = 100;
		String pricePerDay = "70";

		TriggerPrice triggerObject = new TriggerPrice();

		LocalDateTime today = LocalDateTime.now();
		String triggerDate = "2019-04-01T00:00:00+0000";
		int quantity = 20;
		int price = 90;
		triggerObject.setTriggerDate(triggerDate);
		triggerObject.setPrice(price);
		triggerObject.setQuantity(quantity);
		triggerObject.setFxrate(1);
		triggerObject.setQtyConversion(1);
		List<TriggerPrice> triggerPriceList = new ArrayList<>();
		triggerPriceList.add(triggerObject);

		CurveDetails curveDetails = new CurveDetails();
		curveDetails.setTriggerPriceList(triggerPriceList);

		double ExpectedValue = 74;

		double ActualValue = triggerPriceCalculator.applyTriggerPrice(curveDetails, itemQty, pricePerDay, today);
		assertEquals(ExpectedValue, ActualValue, 0.001);
	}*/

	@Test
	public void applyTriggerPriceFutureDateTest() throws Exception {

		TriggerPriceCalculator triggerPriceCalculator = new TriggerPriceCalculator();
		long itemQty = 100;
		String pricePerDay = "70";

		TriggerPrice triggerObject = new TriggerPrice();
		DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
		LocalDateTime today = LocalDateTime.now();
		String isoDateTom = today.plusDays(1).toString();
		isoDateTom = isoDateTom.substring(0, isoDateTom.length() - 4);
		isoDateTom = isoDateTom + "+00:00";
		LocalDateTime tom = LocalDateTime.parse(isoDateTom, formatter);
		String triggerDate = tom.toString();

		int quantity = 20;
		int price = 90;
		triggerObject.setTriggerDate(triggerDate);
		triggerObject.setPrice(price);
		triggerObject.setQuantity(quantity);
		List<TriggerPrice> triggerPriceList = new ArrayList<>();
		triggerPriceList.add(triggerObject);

		CurveDetails curveDetails = new CurveDetails();
		curveDetails.setTriggerPriceList(triggerPriceList);

		double ExpectedValue = 70;
		ContextProvider tenantContextProvider = new ContextProvider();
		TriggerPriceProperties triggerProps = new TriggerPriceProperties();
		double ActualValue = triggerPriceCalculator.applyTriggerPrice(curveDetails, itemQty, pricePerDay, today,
				tenantContextProvider, triggerProps);
		assertEquals(ExpectedValue, ActualValue, 0.001);
	}
}
