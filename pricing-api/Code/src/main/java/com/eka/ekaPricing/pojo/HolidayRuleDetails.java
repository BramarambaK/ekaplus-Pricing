package com.eka.ekaPricing.pojo;

import java.time.LocalDateTime;
import java.util.List;

public class HolidayRuleDetails {
	private String exchangeName;
	private String holidayRule;
	private List<LocalDateTime> dateRange;
	public String getExchangeName() {
		return exchangeName;
	}
	public void setExchangeName(String exchangeName) {
		this.exchangeName = exchangeName;
	}
	public String getHolidayRule() {
		return holidayRule;
	}
	public void setHolidayRule(String holidayRule) {
		this.holidayRule = holidayRule;
	}
	public List<LocalDateTime> getDateRange() {
		return dateRange;
	}
	public void setDateRange(List<LocalDateTime> dateRange) {
		this.dateRange = dateRange;
	}
	
	

}
