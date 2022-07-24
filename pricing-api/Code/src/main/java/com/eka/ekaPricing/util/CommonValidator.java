package com.eka.ekaPricing.util;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class CommonValidator {

	public String cleanData(String str){
		//Use Untrusted Data validation routine as below
				String s = Normalizer.normalize(null!= str?str:"", Form.NFKC);
				// Validate
				Pattern pattern = Pattern.compile("[<>]");
				Matcher matcher = pattern.matcher(s);
				if (matcher.find()) {
				  // Found blacklisted tag
				  throw new IllegalStateException("Untrusted Data detected");
				} else {
					return StringUtils.normalizeSpace(s);
				}
	}

}