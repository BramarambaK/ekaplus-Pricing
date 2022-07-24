package com.eka.ekaPricing.resource;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

import com.eka.ekaPricing.exception.ErrorMessage;
import com.eka.ekaPricing.pojo.PricingDetails;
import com.eka.ekaPricing.service.IPricingService;

/**
 * Controller class which contains getting pricing formula details rest api's
 * 
 * @author Rushikesh Bhosale
 *
 */
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/pricing")
public class PricingController {

	private static final Logger logger = ESAPI.getLogger(PricingController.class);

	@Autowired
	private IPricingService pricingService;

	@GetMapping("/formula/details")
	public ResponseEntity<Object> getPricingDetails(HttpServletRequest request) {

		logger.debug(Logger.EVENT_SUCCESS,
				ESAPI.encoder().encodeForHTML("Pricing Formula details getting - Initiated"));

		// List the file based on injected dependency for storageService
		List<PricingDetails> pricingDetails = pricingService.pricingDetails(request);
		logger.info(Logger.EVENT_SUCCESS,
				ESAPI.encoder().encodeForHTML("The final size of the pricingDetails is" + pricingDetails.size()));

		return ResponseEntity.status(HttpStatus.OK).body(pricingDetails);

	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<String> handleExceptions(Exception exception) {

		ResponseEntity<String> responseEntity = null;

		logger.error(Logger.EVENT_FAILURE,
				ESAPI.encoder().encodeForHTML("Error in General Exception pricing formula API data: " + exception));

		responseEntity = new ResponseEntity<String>(
				"Error in General Exception pricing formula API data:" + exception.getMessage(),
				HttpStatus.INTERNAL_SERVER_ERROR);
		return responseEntity;
	}

	@ExceptionHandler(value = { HttpStatusCodeException.class })
	public final ResponseEntity<ErrorMessage> handleHttpStatusCodeException(HttpStatusCodeException ex) {

		logger.error(Logger.EVENT_FAILURE,
				ESAPI.encoder().encodeForHTML("HttpStatusCodeException inside pricing formula API() -> " + ex));

		ErrorMessage em = new ErrorMessage();
		HttpStatus status = null;

		status = ex.getStatusCode();
		em.setCode(status.value());
		em.setDescription(ex.getMessage());
		em.setStackTrace(ex.getStackTrace());

		return new ResponseEntity<ErrorMessage>(
				new ErrorMessage(em.getCode(), em.getDescription(), em.getStackTrace(), em.getErrorLocalizedMessage()),
				status);
	}

	@ExceptionHandler(value = { HttpClientErrorException.class })
	public final ResponseEntity<ErrorMessage> handleHttpClientErrorException(HttpClientErrorException exception) {


		logger.error(Logger.EVENT_FAILURE,
				ESAPI.encoder().encodeForHTML("HttpClientErrorException inside pricing formula API() -> " + exception));

		ErrorMessage em = new ErrorMessage();
		HttpStatus status = null;
		status = exception.getStatusCode();
		em.setCode(status.value());
		em.setDescription(exception.getMessage());
		em.setStackTrace(exception.getStackTrace());

		return new ResponseEntity<ErrorMessage>(
				new ErrorMessage(em.getCode(), em.getDescription(), em.getStackTrace(), em.getErrorLocalizedMessage()),
				status);
	}

	@ExceptionHandler(value = { RestClientException.class })
	public ResponseEntity<String> handleRestClientException(RestClientException exception) {
		ResponseEntity<String> responseEntity = null;

		logger.error(Logger.EVENT_FAILURE,
				ESAPI.encoder().encodeForHTML("RestClientException inside formula API() -> " + exception));

		responseEntity = new ResponseEntity<String>(
				"RestClientException inside formula API() -> " + exception.getMessage(),
				HttpStatus.INTERNAL_SERVER_ERROR);
		return responseEntity;
	}

}
