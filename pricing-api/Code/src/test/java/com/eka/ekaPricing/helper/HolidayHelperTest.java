package com.eka.ekaPricing.helper;

import static org.junit.Assert.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.catalina.mapper.Mapper;
import org.hamcrest.Matcher;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import com.eka.ekaPricing.helper.HolidayRuleCalculator;
import com.fasterxml.jackson.core.type.TypeReference;

import junit.framework.TestCase;

public class HolidayHelperTest extends TestCase {
	List<JSONObject> holidayDataSetExpected = new ArrayList<>();
	JSONObject ExpectedValueObject1 = new JSONObject();
	JSONObject ExpectedValueObject2 = new JSONObject();
	JSONObject ExpectedValueObject3 = new JSONObject();
	JSONObject ExpectedValueObject4 = new JSONObject();
	List<LocalDateTime> dateRange = new ArrayList<>();
	List<LocalDateTime> holidayList = new ArrayList<>();
	HolidayRuleCalculator holidayDateSetObject = new HolidayRuleCalculator();

	@Before
	public void setUp() throws Exception {
		dateRange.add(LocalDateTime.parse("2018-12-14T00:00"));
		dateRange.add(LocalDateTime.parse("2018-12-15T00:00"));
		dateRange.add(LocalDateTime.parse("2018-12-16T00:00"));
		dateRange.add(LocalDateTime.parse("2018-12-17T00:00"));

		holidayList.add(LocalDateTime.parse("2018-12-15T00:00"));
		holidayList.add(LocalDateTime.parse("2018-12-16T00:00"));
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testIgnore() {

		ExpectedValueObject1.put("date", "2018-12-14T00:00");
		ExpectedValueObject1.put("dateToBeUsed", "2018-12-14T00:00");
		ExpectedValueObject2.put("date", "2018-12-15T00:00");
		ExpectedValueObject2.put("dateToBeUsed", "2018-12-15T00:00");
		ExpectedValueObject3.put("date", "2018-12-16T00:00");
		ExpectedValueObject3.put("dateToBeUsed", "2018-12-16T00:00");
		ExpectedValueObject4.put("date", "2018-12-17T00:00");
		ExpectedValueObject4.put("dateToBeUsed", "2018-12-17T00:00");
		holidayDataSetExpected.add(ExpectedValueObject1);
		holidayDataSetExpected.add(ExpectedValueObject2);
		holidayDataSetExpected.add(ExpectedValueObject3);
		holidayDataSetExpected.add(ExpectedValueObject4);
		String holidayRule = "Ignore";
		List<JSONObject> holidayDataSetActual = holidayDateSetObject.processHolidayRule(holidayRule, dateRange,
				holidayList);
		for (int i = 0; i <= 3; i++) {
			assertEquals(holidayDataSetActual.get(i).get("date"), holidayDataSetExpected.get(i).get("date"));
			assertEquals(holidayDataSetActual.get(i).get("dateToBeUsed"),
					holidayDataSetExpected.get(i).get("dateToBeUsed"));
		}

	}

	public void testNextBusinessDay() {

		ExpectedValueObject1.put("date", "2018-12-14T00:00");
		ExpectedValueObject1.put("dateToBeUsed", "2018-12-14T00:00");
		ExpectedValueObject2.put("date", "2018-12-15T00:00");
		ExpectedValueObject2.put("dateToBeUsed", "2018-12-17T00:00");
		ExpectedValueObject3.put("date", "2018-12-16T00:00");
		ExpectedValueObject3.put("dateToBeUsed", "2018-12-17T00:00");
		ExpectedValueObject4.put("date", "2018-12-17T00:00");
		ExpectedValueObject4.put("dateToBeUsed", "2018-12-17T00:00");
		holidayDataSetExpected.add(ExpectedValueObject1);
		holidayDataSetExpected.add(ExpectedValueObject2);
		holidayDataSetExpected.add(ExpectedValueObject3);
		holidayDataSetExpected.add(ExpectedValueObject4);
		String holidayRule = "Next Business Day";

		List<JSONObject> holidayDataSetActual = holidayDateSetObject.processHolidayRule(holidayRule, dateRange,
				holidayList);
		for (int i = 0; i <= 3; i++) {
			assertEquals(holidayDataSetActual.get(i).get("date"), holidayDataSetExpected.get(i).get("date"));
			assertEquals(holidayDataSetActual.get(i).get("dateToBeUsed"),
					holidayDataSetExpected.get(i).get("dateToBeUsed"));
		}
	}

	public void testPriorUniqueBusinessDay() {
		ExpectedValueObject1.put("date", "2018-12-14T00:00");
		ExpectedValueObject1.put("dateToBeUsed", "2018-12-14T00:00");
		ExpectedValueObject2.put("date", "2018-12-15T00:00");
		ExpectedValueObject2.put("dateToBeUsed", "2018-12-13T00:00");
		ExpectedValueObject3.put("date", "2018-12-16T00:00");
		ExpectedValueObject3.put("dateToBeUsed", "2018-12-12T00:00");
		ExpectedValueObject4.put("date", "2018-12-17T00:00");
		ExpectedValueObject4.put("dateToBeUsed", "2018-12-17T00:00");
		holidayDataSetExpected.add(ExpectedValueObject1);
		holidayDataSetExpected.add(ExpectedValueObject2);
		holidayDataSetExpected.add(ExpectedValueObject3);
		holidayDataSetExpected.add(ExpectedValueObject4);
		String holidayRule = "Prior Unique Business Day";
		List<JSONObject> holidayDataSetActual = holidayDateSetObject.processHolidayRule(holidayRule, dateRange,
				holidayList);
		for (int i = 0; i <= 3; i++) {
			assertEquals(holidayDataSetActual.get(i).get("date"), holidayDataSetExpected.get(i).get("date"));
			assertEquals(holidayDataSetActual.get(i).get("dateToBeUsed"),
					holidayDataSetExpected.get(i).get("dateToBeUsed"));
		}
	}

	public void testNextUniqueBusinessDay() {
		ExpectedValueObject1.put("date", "2018-12-14T00:00");
		ExpectedValueObject1.put("dateToBeUsed", "2018-12-14T00:00");
		ExpectedValueObject2.put("date", "2018-12-15T00:00");
		ExpectedValueObject2.put("dateToBeUsed", "2018-12-18T00:00");
		ExpectedValueObject3.put("date", "2018-12-16T00:00");
		ExpectedValueObject3.put("dateToBeUsed", "2018-12-19T00:00");
		ExpectedValueObject4.put("date", "2018-12-17T00:00");
		ExpectedValueObject4.put("dateToBeUsed", "2018-12-17T00:00");
		holidayDataSetExpected.add(ExpectedValueObject1);
		holidayDataSetExpected.add(ExpectedValueObject2);
		holidayDataSetExpected.add(ExpectedValueObject3);
		holidayDataSetExpected.add(ExpectedValueObject4);
		String holidayRule = "Next Unique Business Day";
		List<JSONObject> holidayDataSetActual = holidayDateSetObject.processHolidayRule(holidayRule, dateRange,
				holidayList);
		for (int i = 0; i <= 3; i++) {
			assertEquals(holidayDataSetActual.get(i).get("date"), holidayDataSetExpected.get(i).get("date"));
			assertEquals(holidayDataSetActual.get(i).get("dateToBeUsed"),
					holidayDataSetExpected.get(i).get("dateToBeUsed"));
		}
	}

	public void testClosestBusinessDayForwards() {
		ExpectedValueObject1.put("date", "2018-12-14T00:00");
		ExpectedValueObject1.put("dateToBeUsed", "2018-12-14T00:00");
		ExpectedValueObject2.put("date", "2018-12-15T00:00");
		ExpectedValueObject2.put("dateToBeUsed", "2018-12-14T00:00");
		ExpectedValueObject3.put("date", "2018-12-16T00:00");
		ExpectedValueObject3.put("dateToBeUsed", "2018-12-17T00:00");
		ExpectedValueObject4.put("date", "2018-12-17T00:00");
		ExpectedValueObject4.put("dateToBeUsed", "2018-12-17T00:00");
		holidayDataSetExpected.add(ExpectedValueObject1);
		holidayDataSetExpected.add(ExpectedValueObject2);
		holidayDataSetExpected.add(ExpectedValueObject3);
		holidayDataSetExpected.add(ExpectedValueObject4);
		String holidayRule = "Closest Business Day/Forwards";
		List<JSONObject> holidayDataSetActual = holidayDateSetObject.processHolidayRule(holidayRule, dateRange,
				holidayList);
		for (int i = 0; i <= 3; i++) {
			assertEquals(holidayDataSetActual.get(i).get("date"), holidayDataSetExpected.get(i).get("date"));
			assertEquals(holidayDataSetActual.get(i).get("dateToBeUsed"),
					holidayDataSetExpected.get(i).get("dateToBeUsed"));
		}
	}

	public void testClosestBusinessDayBackwards() {
		ExpectedValueObject1.put("date", "2018-12-14T00:00");
		ExpectedValueObject1.put("dateToBeUsed", "2018-12-14T00:00");
		ExpectedValueObject2.put("date", "2018-12-15T00:00");
		ExpectedValueObject2.put("dateToBeUsed", "2018-12-14T00:00");
		ExpectedValueObject3.put("date", "2018-12-16T00:00");
		ExpectedValueObject3.put("dateToBeUsed", "2018-12-17T00:00");
		ExpectedValueObject4.put("date", "2018-12-17T00:00");
		ExpectedValueObject4.put("dateToBeUsed", "2018-12-17T00:00");
		holidayDataSetExpected.add(ExpectedValueObject1);
		holidayDataSetExpected.add(ExpectedValueObject2);
		holidayDataSetExpected.add(ExpectedValueObject3);
		holidayDataSetExpected.add(ExpectedValueObject4);
		String holidayRule = "Closest Business Day/Backwards";
		List<JSONObject> holidayDataSetActual = holidayDateSetObject.processHolidayRule(holidayRule, dateRange,
				holidayList);
		for (int i = 0; i <= 3; i++) {
			assertEquals(holidayDataSetActual.get(i).get("date"), holidayDataSetExpected.get(i).get("date"));
			assertEquals(holidayDataSetActual.get(i).get("dateToBeUsed"),
					holidayDataSetExpected.get(i).get("dateToBeUsed"));
		}
	}

	public void testClosestUniqueBusinessDayForward() {
		ExpectedValueObject1.put("date", "2018-12-14T00:00");
		ExpectedValueObject1.put("dateToBeUsed", "2018-12-14T00:00");
		ExpectedValueObject2.put("date", "2018-12-15T00:00");
		ExpectedValueObject2.put("dateToBeUsed", "2018-12-13T00:00");
		ExpectedValueObject3.put("date", "2018-12-16T00:00");
		ExpectedValueObject3.put("dateToBeUsed", "2018-12-18T00:00");
		ExpectedValueObject4.put("date", "2018-12-17T00:00");
		ExpectedValueObject4.put("dateToBeUsed", "2018-12-17T00:00");
		holidayDataSetExpected.add(ExpectedValueObject1);
		holidayDataSetExpected.add(ExpectedValueObject2);
		holidayDataSetExpected.add(ExpectedValueObject3);
		holidayDataSetExpected.add(ExpectedValueObject4);
		String holidayRule = "Closest Unique Business Day/Forward";
		List<JSONObject> holidayDataSetActual = holidayDateSetObject.processHolidayRule(holidayRule, dateRange,
				holidayList);
		for (int i = 0; i <= 3; i++) {
			assertEquals(holidayDataSetActual.get(i).get("date"), holidayDataSetExpected.get(i).get("date"));
			assertEquals(holidayDataSetActual.get(i).get("dateToBeUsed"),
					holidayDataSetExpected.get(i).get("dateToBeUsed"));
		}
	}

	public void testClosestUniqueBusinessDayBackward() {
		ExpectedValueObject1.put("date", "2018-12-14T00:00");
		ExpectedValueObject1.put("dateToBeUsed", "2018-12-14T00:00");
		ExpectedValueObject2.put("date", "2018-12-15T00:00");
		ExpectedValueObject2.put("dateToBeUsed", "2018-12-13T00:00");
		ExpectedValueObject3.put("date", "2018-12-16T00:00");
		ExpectedValueObject3.put("dateToBeUsed", "2018-12-18T00:00");
		ExpectedValueObject4.put("date", "2018-12-17T00:00");
		ExpectedValueObject4.put("dateToBeUsed", "2018-12-17T00:00");
		holidayDataSetExpected.add(ExpectedValueObject1);
		holidayDataSetExpected.add(ExpectedValueObject2);
		holidayDataSetExpected.add(ExpectedValueObject3);
		holidayDataSetExpected.add(ExpectedValueObject4);
		String holidayRule = "Closest Unique Business Day/Backwards";
		List<JSONObject> holidayDataSetActual = holidayDateSetObject.processHolidayRule(holidayRule, dateRange,
				holidayList);
		for (int i = 0; i <= 3; i++) {
			assertEquals(holidayDataSetActual.get(i).get("date"), holidayDataSetExpected.get(i).get("date"));
			assertEquals(holidayDataSetActual.get(i).get("dateToBeUsed"),
					holidayDataSetExpected.get(i).get("dateToBeUsed"));
		}
	}

}
