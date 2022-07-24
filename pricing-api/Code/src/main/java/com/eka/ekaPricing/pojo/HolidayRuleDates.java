package com.eka.ekaPricing.pojo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class HolidayRuleDates {
	List<LocalDateTime> dateToBeUsed = new ArrayList<LocalDateTime>();
	List<LocalDateTime> datesList = new ArrayList<LocalDateTime>();

	public List<LocalDateTime> getDateToBeUsed() {
		return dateToBeUsed;
	}

	public void setDateToBeUsed(List<LocalDateTime> dateToBeUsed) {
		this.dateToBeUsed = dateToBeUsed;
	}

	public List<LocalDateTime> getDatesList() {
		return datesList;
	}

	public void setDatesList(List<LocalDateTime> datesList) {
		this.datesList = datesList;
	}

}
