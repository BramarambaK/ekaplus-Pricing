package com.eka.ekaPricing.service;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;

import com.eka.ekaPricing.pojo.PricingDetails;


/**
 * Service Interface which contains getting pricing formula details rest api's
 * 
 * @author Rushikesh Bhosale
 *
 */

@Service
public interface IPricingService {

	List<PricingDetails> pricingDetails(HttpServletRequest request);
	
	
}
