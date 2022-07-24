package com.eka.ekaPricing.resource;

import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.eka.ekaPricing.pojo.HolidayRuleDetails;
import com.eka.ekaPricing.service.CurveService;
import com.eka.ekaPricing.util.ContextProvider;

@RunWith(MockitoJUnitRunner.Silent.class)
public class CurveControllerTest {

	@InjectMocks
	CurveController curvecontroller;

	@Mock
	public CurveService curveservicer;

	@Mock
	public HttpServletRequest request;
	@Mock
	public ContextProvider contextProvider;

	@Test
	public void ApplyHolidayRuleTest() throws Exception {
		HolidayRuleDetails holidayRuleDtl = new HolidayRuleDetails();
		List<LocalDateTime> dateRange = new ArrayList<>();
		dateRange.add(LocalDateTime.parse("2018-12-15T00:00"));
		holidayRuleDtl.setDateRange(dateRange);
		String holidayRule = "Next Business Day";
		String exchangeName = "ICE";
		String token = "token";
		String tenantId="boliden";
		String locale="en_US";
		holidayRuleDtl.setExchangeName(exchangeName);
		holidayRuleDtl.setHolidayRule(holidayRule);
		Mockito.when(curveservicer.applyHolidayRule(holidayRuleDtl, contextProvider)).thenReturn("2018-12-17T00:00");
		String Svalue = curvecontroller.applyHolidayRule(token,tenantId,locale,holidayRuleDtl );
		assertEquals(Svalue, "2018-12-17T00:00");
	}
	
	@Test
	public void seedCurveDataTest() throws Exception {
		HolidayRuleDetails holidayRuleDtl = new HolidayRuleDetails();
		List<LocalDateTime> dateRange = new ArrayList<>();
		dateRange.add(LocalDateTime.parse("2018-12-15T00:00"));
		holidayRuleDtl.setDateRange(dateRange);
		String holidayRule = "Next Business Day";
		String exchangeName = "ICE";
		String token = "token";
		String tenantId="boliden";
		String locale="en_US";
		holidayRuleDtl.setExchangeName(exchangeName);
		holidayRuleDtl.setHolidayRule(holidayRule);
		String appName = "Pricing";
		String objName = "PR";
		String tenantID = "tenantId";
		Mockito.doNothing().when(curveservicer).seedCurveData(contextProvider, appName,objName);
		String expectedValue = "Base Curve Names Added/Upadted";
		String Svalue = curvecontroller.seedCurveData(appName,objName,token,tenantID,locale);
		assertEquals("Base Curve Names Added/Upadted", Svalue);
	}

	
}
