package com.eka.ekaPricing.resource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eka.ekaPricing.service.HealthCheckService;

@RestController
@RequestMapping("/api/healthcheck")
public class HealthCheckController {

	@Autowired
	HealthCheckService healthCheckService;

	private static final Logger LOGGER = ESAPI
			.getLogger(HealthCheckController.class);

	@PostMapping
	public Map<String, Object> executeHealthCheck(HttpServletRequest request)
			throws Exception {

		LOGGER.info(Logger.EVENT_SUCCESS, "health check initiated");

		Map<String, Object> returnMap = new HashMap<>();

		List<Map<String, Object>> healthCheckResponseList = healthCheckService
				.execute();

		returnMap.put("data", healthCheckResponseList);
		LOGGER.info(Logger.EVENT_SUCCESS, "health check completed");

		return returnMap;
	}

}
