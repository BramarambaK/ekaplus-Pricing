package com.eka.ekaPricing.helper;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.eka.ekaPricing.standalone.ExpressionBuilder;

public class ExpressionBuilderTest {

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

	@Test
	public void buildExpressionTest() throws Exception{
		List<String> curveList = new ArrayList<>();
		String expression = "0.5*exp1+1.4*exp2+8.1*exp3";

		curveList.add("exp1");
		curveList.add("exp2");
		curveList.add("exp3");
		String expectedExpression = "0.5*{{exp1}}+1.4*{{exp2}}+8.1*{{exp3}}";
		ExpressionBuilder expressionBuilder = new ExpressionBuilder();
		String actualExpression = expressionBuilder.buildExpression(curveList, expression);
		assertEquals(expectedExpression, actualExpression);

	}

}
