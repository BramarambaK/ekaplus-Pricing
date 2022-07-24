package com.eka.ekaPricing.exception;

public class PricingException extends Exception {
	private static final long serialVersionUID = 1L;
	public PricingException() {
		super();
	}
	public PricingException(String message) {
		super("Pricing Exception - "+message);
	}
}
