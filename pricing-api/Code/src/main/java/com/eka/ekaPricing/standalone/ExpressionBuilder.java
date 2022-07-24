package com.eka.ekaPricing.standalone;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.regex.Pattern;

import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Logger;
import org.springframework.stereotype.Component;

import com.eka.ekaPricing.exception.PricingException;
import com.eka.ekaPricing.pojo.PricingComponent;

@Component
public class ExpressionBuilder {
	final static Logger logger = ESAPI.getLogger(ExpressionBuilder.class);
	public String buildExpression(List<String> curveList, String expression) throws PricingException {
		String resStr = "";
		String exp = expression.replace(" ", "");
		for (int i = 0; i < exp.length() - 1; i++) {
			if ((exp.charAt(i) == '+' || exp.charAt(i) == '-' || exp.charAt(i) == '*' || exp.charAt(i) == '/')
					&& (exp.charAt(i + 1) == '+' || exp.charAt(i + 1) == '-' || exp.charAt(i + 1) == '*'
							|| exp.charAt(i + 1) == '/')) {
				throw new PricingException("Invalid Expression");
			}
		}
		char beginingIndex = exp.charAt(0);
		boolean indexCheck = Pattern.compile("^[A-Za-z0-9]").matcher(beginingIndex + "").matches();
		if (!indexCheck) {
			throw new PricingException("Invalid Expression");
		}

		for (String cName : curveList) {
			if (expression.contains(cName)) {
				resStr = resStr + expression.substring(0, expression.indexOf(cName)) + "{{" + cName + "}}";
				expression = expression.substring(expression.indexOf(cName) + cName.length());
			}
		}
		resStr = resStr + expression;
		return resStr;

	}

//Simplifying expression by value of components
	public String simplifyExpression(String exp, List<PricingComponent> compList) throws PricingException {
		for (PricingComponent ci : compList) {
			if (exp.contains(ci.getProductComponentName() + "* ")) {
				try {
					exp = exp.replace(ci.getProductComponentName(), Double.toString(ci.getComponentPercentage()/100));
				} catch (Exception e) {
					logger.error(Logger.EVENT_SUCCESS, ESAPI.encoder().encodeForHTML("Invalid component % for : "+ci.getProductComponentName()));
					throw new PricingException("Invalid component % for : "+ci.getProductComponentName());
				}
			}
		}
		return exp;
	}
	
	public double applyPrecision(double price, int precision) {
		BigDecimal priceInBigDecimal = BigDecimal.valueOf(price);
		try {
			return priceInBigDecimal.setScale(precision, RoundingMode.HALF_UP).doubleValue();
		}
		catch (Exception e) {
			logger.error(Logger.EVENT_FAILURE, ESAPI.encoder().encodeForHTML("Exception while applying precision "), e);
		}
		return 0;
	}
	
	public List<String> getExpForm(String exp,List<String> list){
		
		exp = exp.replaceAll("[0-9*.]", "");
		exp = exp.replaceAll(" ", "");
		String arr = "";
		if (exp.contains("{{")) {
			int ind = exp.indexOf("{{");
			int last = exp.indexOf("}}");
			if (ind == 0) {
				list.add("+");
			} else {
				arr = exp.substring(ind - 1, ind);
				list.add(arr);
			}
			String desiredCurve = exp.substring(ind, last + 2);
			String extractedCurve = "";
			exp = exp.replaceFirst(Pattern.quote(desiredCurve), extractedCurve);
			return getExpForm(exp, list);

		}
	
	return list;
  }

}
