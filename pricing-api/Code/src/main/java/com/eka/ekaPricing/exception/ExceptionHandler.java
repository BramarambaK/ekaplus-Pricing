package com.eka.ekaPricing.exception;

import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExceptionHandler {
	final static Logger logger = ESAPI.getLogger(ExceptionHandler.class);
	@org.springframework.web.bind.annotation.ExceptionHandler(Exception.class)
	public final ResponseEntity<ErrorMessage> handleException(Exception ex) throws Exception {
		ErrorMessage em = new ErrorMessage();
		HttpStatus status = null;
		logger.error(Logger.EVENT_SUCCESS, "From ExceptionHandler"+ex.getMessage(),ex);
		if (ex.getMessage().contains("406 Not Acceptable") || ex.getMessage().contains("406 null")) {
			status = HttpStatus.NOT_ACCEPTABLE;
			em.setCode(status.value());
			em.setDescription(ex.getMessage());
			em.setStackTrace(ex.getStackTrace());
			em.setErrorLocalizedMessage(ex.getMessage());
		} else if (ex.getMessage().contains("JSON parse error:") || ex.getMessage().contains("400 Bad Request")
				|| ex.getMessage().contains("Pricing Exception - ")) {
			status = HttpStatus.BAD_REQUEST;
			em.setCode(status.value());
			em.setDescription(ex.getMessage());
			em.setStackTrace(ex.getStackTrace());
			em.setErrorLocalizedMessage(ex.getMessage());
		} else if (ex.getMessage().contains("401 Unauthorized")) {
			status = HttpStatus.UNAUTHORIZED;
			em.setCode(status.value());
			em.setDescription(ex.getMessage());
			em.setStackTrace(ex.getStackTrace());
			em.setErrorLocalizedMessage(ex.getMessage());
		} else {
			status = HttpStatus.INTERNAL_SERVER_ERROR;
			em.setCode(status.value());
			em.setDescription(ex.getMessage());
			em.setStackTrace(ex.getStackTrace());
			em.setErrorLocalizedMessage("Pricing Exception");
		}
		return new ResponseEntity<ErrorMessage>(
				new ErrorMessage(em.getCode(), em.getDescription(), em.getStackTrace(), em.getErrorLocalizedMessage()),
				status);
	}

}
