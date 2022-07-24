package com.eka.ekaPricing.resource;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RevaluationObject {
	@JsonProperty("Instrument Name")
	private String instrumentName;

	public String getInstrumentName() {
		return instrumentName;
	}

	public void setInstrumentName(String instrumentName) {
		this.instrumentName = instrumentName;
	}
}