package com.eka.ekaPricing.service;

import java.text.ParseException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

import com.eka.ekaPricing.pojo.TriggerPriceDetails;


@Service
public interface TriggerPricingDetailsService {

	List<TriggerPriceDetails> triggerPricingDetails(HttpServletRequest request) throws HttpStatusCodeException, HttpClientErrorException, RestClientException, ParseException;
	
	
}
