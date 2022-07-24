package com.eka.ekaPricing.pojo;

import java.util.List;

public class PayloadInput {
	Contract contract;
	List<Formula> formulaList;
	public Contract getContract() {
		return contract;
	}
	public void setContract(Contract contract) {
		this.contract = contract;
	}
	public List<Formula> getFormulaList() {
		return formulaList;
	}
	public void setFormulaList(List<Formula> formulaList) {
		this.formulaList = formulaList;
	}
	
}
