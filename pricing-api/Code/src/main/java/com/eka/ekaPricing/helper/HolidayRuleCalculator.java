package com.eka.ekaPricing.helper;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class HolidayRuleCalculator {
	private static final String PRIOR_BUSINESS_DAY = "Prior Business Day";
	private static final String NEXT_BUSINESS_DAY = "Next Business Day";
	private static final String PRIOR_UNIQUE_BUSINESS_DAY = "Prior Unique Business Day";
	private static final String NEXT_UNIQUE_BUSINESS_DAY = "Next Unique Business Day";
	private static final String CLOSEST_BUSINESS_DAY_FORWARDS = "Closest Business Day/Forwards";
	private static final String CLOSEST_BUSINESS_DAY_BACKWARDS = "Closest Business Day/Backwards";
	private static final String CLOSEST_UNIQUE_BUSINESS_DAY_FORWARDS = "Closest Unique Business Day/Forward";
	private static final String CLOSEST_UNIQUE_BUSINESS_DAY_BACKWARDS = "Closest Unique Business Day/Backwards";
	private static final String IGNORE = "Ignore";
	private static final String DATE_LBL = "date";
	private static final String DATE_TOBE_USED_LBL = "dateToBeUsed";
	private static final String FIRST_DAY_OF_WEEK = "firstDayOfWeek";
	private static final String LAST_DAY_OF_WEEK = "lastDayOfWeek";
	private static final String PRIOR = "Prior";
	private static final String NEXT = "Next";
	private static final String CLOSEST = "Closest";
	private static final String UNIQUE = "Unique";
	
	private Set<DayOfWeek> weekend = EnumSet.of( DayOfWeek.SATURDAY , DayOfWeek.SUNDAY );
	private static Map<String,DayOfWeek> dayOfWeekMap = new HashMap<String,DayOfWeek>();	
	private ZoneId zone = ZoneId.systemDefault();
	static	{
		dayOfWeekMap.put("firstDayOfWeek",DayOfWeek.MONDAY);		
		dayOfWeekMap.put("tuesday",DayOfWeek.TUESDAY);
		dayOfWeekMap.put("wednesday",DayOfWeek.WEDNESDAY );
		dayOfWeekMap.put("thursday",DayOfWeek.THURSDAY );
		dayOfWeekMap.put("lastDayOfWeek",DayOfWeek.FRIDAY);
	}
	private TreeSet<LocalDateTime> minMaxDateSet = null;
	
	/**
	 * @param holidayRule
	 * @param dateRange
	 * @param holidayList
	 * @return	method returns list of original dates and corresponding new dates to be used based on holiday and weekend rules
	 */
	public List<JSONObject> processHolidayRule(String holidayRule,List<LocalDateTime> dateRange,List<LocalDateTime> holidayList) {	
		JSONObject jsonObj = null;
		List<JSONObject> holidayDataSet = new ArrayList<>();
		Collections.sort(dateRange);
		minMaxDateSet = new TreeSet<LocalDateTime>();
		minMaxDateSet.add(dateRange.get(0));
		minMaxDateSet.add(dateRange.get(dateRange.size()-1));
		for(LocalDateTime dte :dateRange)
		{		
			LocalDateTime currentDate = dte;
			DayOfWeek dow = currentDate.getDayOfWeek();		
			Boolean todayIsWeekend = weekend.contains( dow );		
			LocalDateTime startOfWeek = currentDate ;		
			String dayOfWeek = null;
			/*
			 * No change of date's in case of IGNORE holiday rule. Return the same set of dates.
			 */
			if(holidayRule.equalsIgnoreCase(IGNORE)) {
				 startOfWeek = currentDate;
			}else {
				try {
					do {
					if( todayIsWeekend ) {				
						dayOfWeek = decodeWeekendRule(holidayRule,currentDate);
						if(dayOfWeek.equalsIgnoreCase(FIRST_DAY_OF_WEEK)) {
							startOfWeek = currentDate.with( TemporalAdjusters.next(  dayOfWeekMap.get(dayOfWeek)) );				
						}else if(dayOfWeek.equalsIgnoreCase(LAST_DAY_OF_WEEK)) {
							startOfWeek = currentDate.with( TemporalAdjusters.previous( dayOfWeekMap.get(dayOfWeek)) );				
						}else {
							startOfWeek = LocalDateTime.parse(dayOfWeek);
						}
					} else {
					    startOfWeek = currentDate;
					}			
					startOfWeek = decodeHolidayRule(holidayRule, startOfWeek,holidayList);	
					if(null!=startOfWeek) {
						minMaxDateSet.add(startOfWeek);
					}
					}while(todayIsWeekend = weekend.contains(startOfWeek.getDayOfWeek()));
				}catch(DateTimeParseException  e) {
					e.printStackTrace();
				}catch(Exception e1) {
					e1.printStackTrace();
				}
			}
			jsonObj = new JSONObject();
			jsonObj.accumulate(DATE_LBL, currentDate.toString());
			String dayStr = currentDate.getDayOfWeek().toString().toLowerCase();
			if (holidayRule.equalsIgnoreCase("Ignore Holidays")
					&& (null == startOfWeek || dayStr.contains("sat") || dayStr.contains("sun") || holidayList.contains(currentDate))) {
				jsonObj.accumulate(DATE_TOBE_USED_LBL, "NA");
			}
			else { 
				jsonObj.accumulate(DATE_TOBE_USED_LBL, startOfWeek.toString());
			}
			holidayDataSet.add(jsonObj);						
		}		
		minMaxDateSet = null;
		return holidayDataSet;
	}	
	/**
	 * @param holidayRule
	 * @param dateRange
	 * @return	Date in string format based on the holiday rule in case the dates fall in weekend list
	 */
	private String decodeWeekendRule(String holidayRule,LocalDateTime dateRange) {
		String dayOfWeek = null;		
		if(holidayRule.equalsIgnoreCase(PRIOR_BUSINESS_DAY)) {
			dayOfWeek = LAST_DAY_OF_WEEK;
		}else if(holidayRule.equalsIgnoreCase(NEXT_BUSINESS_DAY)) {
			dayOfWeek = FIRST_DAY_OF_WEEK;
		}else if(holidayRule.equalsIgnoreCase(PRIOR_UNIQUE_BUSINESS_DAY)) {	
			LocalDateTime minDate = minMaxDateSet.first();			
			dateRange = minDate.minusDays(1);							
			dayOfWeek = dateRange.toString();
		}else if(holidayRule.equalsIgnoreCase(NEXT_UNIQUE_BUSINESS_DAY)) {
			LocalDateTime maxDate = minMaxDateSet.last();
			dateRange = maxDate.plusDays(1);						
			dayOfWeek = dateRange.toString();
		}else if(holidayRule.equalsIgnoreCase(CLOSEST_BUSINESS_DAY_FORWARDS)) {
			if(dateRange.getDayOfWeek().equals(DayOfWeek.SATURDAY)) {
				dateRange = dateRange.minusDays(1);
			} else if(dateRange.getDayOfWeek().equals(DayOfWeek.SUNDAY)) {
				dateRange = dateRange.plusDays(1);
			}else {				
				/*
				 * *Condition for case where Friday is a holiday in which case we have to move forward to Monday
				 * Else part contains logic for days that falls in holiday list
				 */
				if(dateRange.getDayOfWeek().equals(DayOfWeek.FRIDAY)) {
					dateRange = dateRange.plusDays(3);
				}else {
					dateRange = dateRange.plusDays(1);
				}
			}
			dayOfWeek = dateRange.toString();
		}else if(holidayRule.equalsIgnoreCase(CLOSEST_BUSINESS_DAY_BACKWARDS)) {
			if(dateRange.getDayOfWeek().equals(DayOfWeek.SATURDAY)) {
				dateRange = dateRange.minusDays(1);
			} else if(dateRange.getDayOfWeek().equals(DayOfWeek.SUNDAY)) {
				dateRange = dateRange.plusDays(1);
			}
			else {
				/*
				 * *Condition for case where Monday is a holiday in which case we have to move backward to Friday
				 * Else part contains logic for days that falls in holiday list
				 */
				if(dateRange.getDayOfWeek().equals(DayOfWeek.MONDAY)) {
					dateRange = dateRange.minusDays(3);
				}else {
					dateRange = dateRange.minusDays(1);
				}
			}
			dayOfWeek = dateRange.toString();
		}else if(holidayRule.equalsIgnoreCase(CLOSEST_UNIQUE_BUSINESS_DAY_FORWARDS)) {	
			LocalDateTime minDate = minMaxDateSet.first();
			LocalDateTime maxDate = minMaxDateSet.last();	
			long priorDays = ChronoUnit.DAYS.between(minDate, dateRange);
			long nextDays = ChronoUnit.DAYS.between(dateRange,maxDate);			
			if(priorDays > nextDays) {
				dateRange = maxDate.plusDays(1);
			}else if(priorDays < nextDays){
				dateRange = minDate.minusDays(1);
			}else {
				dateRange = maxDate.plusDays(1);
			}			
			dayOfWeek = dateRange.toString();
		}else if(holidayRule.equalsIgnoreCase(CLOSEST_UNIQUE_BUSINESS_DAY_BACKWARDS)) {
			LocalDateTime minDate = minMaxDateSet.first();
			LocalDateTime maxDate = minMaxDateSet.last();	
			long priorDays = ChronoUnit.DAYS.between(minDate, dateRange);			
			long nextDays = ChronoUnit.DAYS.between(dateRange,maxDate);			
			if(priorDays > nextDays) {
				dateRange = maxDate.plusDays(1);
			}else if(priorDays < nextDays){
				dateRange = minDate.minusDays(1);
			}else {
				dateRange = minDate.minusDays(1);
			}			
			dayOfWeek = dateRange.toString();			
		}else {
			dayOfWeek = FIRST_DAY_OF_WEEK;
		}
		return dayOfWeek;
	}
	
	/**
	 * @param holidayRule
	 * @param dayOfWeek
	 * @param holidayList
	 * @return	The new date in case the current date falls in the holiday list
	 */
	private LocalDateTime decodeHolidayRule(String holidayRule,LocalDateTime dayOfWeek,List<LocalDateTime> holidayList) {		
		while(holidayList.contains(dayOfWeek)){
			if(holidayRule.contains(UNIQUE) || holidayRule.startsWith(CLOSEST)) {
				try {
					dayOfWeek = LocalDateTime.parse(decodeWeekendRule(holidayRule, dayOfWeek));
				}catch(DateTimeParseException  e) {
					e.printStackTrace();
				}
				minMaxDateSet.add(dayOfWeek);
			}
			else if(holidayRule.startsWith(PRIOR)) {
				dayOfWeek = dayOfWeek.minusDays(1);
			}
			else if(holidayRule.equals("Ignore Holidays")) {
				dayOfWeek = null;
			}
			else {
				dayOfWeek = dayOfWeek.plusDays(1);
			}					
		}
		return dayOfWeek;
		
	}

}