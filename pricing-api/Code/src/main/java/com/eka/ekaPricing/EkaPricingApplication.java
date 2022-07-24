
package com.eka.ekaPricing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.handler.MappedInterceptor;

import com.eka.ekaPricing.interceptor.PropertyInterceptor;
import com.eka.ekaPricing.pojo.PricingProperties;
import com.eka.ekaPricing.pojo.ValidateToken;
import com.eka.ekaPricing.util.ContextProvider;

@SpringBootApplication
@EnableAspectJAutoProxy
@EnableAsync
public class EkaPricingApplication {
	final static  Logger logger = ESAPI.getLogger(EkaPricingApplication.class);
	public static void main(String[] args) {
		SpringApplication.run(EkaPricingApplication.class, args);

	}

	public static class MyException extends RuntimeException {

		/**
		 * 
		 */
		private static final long serialVersionUID = 8558952424579196425L;

	}

	@Bean
	@Autowired
	public MappedInterceptor getPropertyInterceptor(PropertyInterceptor propertyInterceptor) {
		return new MappedInterceptor(new String[] {}, new String[] {"/common/getManifestInfo","/logger/**"}, propertyInterceptor);
	}

	@Component
	public static class ValidateTokenAction {
		@Autowired
		ContextProvider context;
		public ValidateToken judgeToken(HttpServletRequest request) {
			PricingProperties pricingProps = context.getCurrentContext().getPricingProperties();
			String token = request.getHeader("Authorization");
			ResponseEntity<ValidateToken> result = null;
			ValidateToken tokenObj = null;
			if (token == null) {
				throw new MyException();
			} else {
				// Externalize the URI
				String validateURL = pricingProps.getEka_validateToken_url();
				final String uri = validateURL + token;
				RestTemplate restTemplate = new RestTemplate();
				HttpHeaders headers = new HttpHeaders();
				headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
				HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);
				result = restTemplate.exchange(uri, HttpMethod.POST, entity, ValidateToken.class);

			}
			if (null != result) {
				tokenObj = result.getBody();
			}
			return tokenObj;
		}
	}

	@ControllerAdvice
	public static class MyExceptionHandler {
		@ExceptionHandler(MyException.class)
		@ResponseBody
		public Map<String, Object> handler() {
			Map<String, Object> m1 = new HashMap<String, Object>();
			m1.put("status", "error");
			m1.put("message", "Sorry, your provided token information expired or not exists.");
			return m1;
		}
	}

	@Bean
	public FilterRegistrationBean<CorsFilter> corsFilter() {
		List<String> exposedHeaderList = new ArrayList<String>();
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		CorsConfiguration config = new CorsConfiguration();
		exposedHeaderList.add("Authorization");
		exposedHeaderList.add("Accept");
		exposedHeaderList.add("Cache-Control");
		config.setAllowCredentials(true);
		config.addAllowedOrigin("*");
		config.addAllowedHeader("*");
		config.addAllowedMethod("PUT");
		config.addAllowedMethod("GET");
		config.addAllowedMethod("POST");
		config.addAllowedMethod("OPTIONS");
		config.addAllowedMethod("DELETE");
		config.setExposedHeaders(exposedHeaderList);
		source.registerCorsConfiguration("/**", config);
		FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<CorsFilter>(new CorsFilter(source));
		bean.setOrder(0);
		return bean;
	}

	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {

		// Do any additional configuration here

		return builder.build();

	}

}
