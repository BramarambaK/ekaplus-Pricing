# -*- coding: utf-8 -*-
"""
Created on Tue Feb  4 20:27:30 2020

@author: Narendra.Negi
"""

import datetime
from PricePicker import calculatePrice
from MarketPricesFactory import getPricesForTag
from ExpiryCalendarFetcher import fetch_calendar
from datetime import timedelta
from dateutil.relativedelta import relativedelta

def fetchPrice(curveProperty, auth, holiday_rule, tag, tenant) :
    for i in range(0, len(curveProperty)) :
        curve = curveProperty[i]
        if('collectionData' in curve) :
            calculatePrice(curve, auth, holiday_rule, tenant, tag)
        else :
            monthDiff = 0
            month_year_format = '%b%Y'
            format_str = '%Y-%m-%d'
            if(curve['pricePoint']=='Forward'):
                pricingPeriodMonth = curve['pricingPeriod']
                quotedPeriodMonth = curve['quotedPeriodDate']
                quotedPeriod = curve['quotedPeriod']
                try :
                    pricingPeriodDate = datetime.strptime(pricingPeriodMonth, month_year_format).strftime(format_str)
                    quotedPeriodDate = datetime.strptime(quotedPeriodMonth, month_year_format).strftime(format_str)
                    monthDiff = (quotedPeriodDate.year - pricingPeriodDate.year) * 12 + (quotedPeriodDate.month - pricingPeriodDate.month)
                except:
                    
                month_year = str()
                if(quotedPeriod=='Date') :
                    quotedPeriodDate = curve['quotedPeriodDate']
                    monthInd = quotedPeriodDate.index('-')
                    monthNum = int(quotedPeriodDate[:monthInd])
                    yearNum = int(quotedPeriodDate[monthInd+1:])
                    month_year = datetime.date(1900, monthNum, 1).strftime('%B')[:3].upper()+str(yearNum)
                    curve['monthYear'] = month_year
                    collectionData = retrieve_collection_common(tag, auth, curve, tenant)
                 
                elif quotedPeriod == 'Prompt Period Avg' :
                    calendar = fetch_calendar(auth, curve, tenant)
                    obj = process_expiry_calendar(calendar, quotedPeriodMonth)
                    curve['startDate'] = obj['start_date']
                    curve['endDate'] = obj['end_date']
                    if monthDiff == 0 :
                        curve['monthYear'] = quotedPeriodMonth
                    else :
                        adjusted_date = datetime.strptime(quotedPeriodMonth, month_year_format).strftime(format_str)
                        adjusted_date = adjusted_date + relativedelta(months=+monthDiff)
                        curve['monthYear'] = adjusted_date.strftime('%B')[:3].upper()
                    collectionData = retrieve_collection_common(tag, auth, curve, tenant)
                    
                elif quotedPeriod == 'Delivery Period Average' :
                    calendar = fetch_calendar(auth, curve, tenant)
                    
                elif quotedPeriod == 'Event Offset Based' :
                    
                elif tag =='Power' :
                    
                    collectionData = getPricesForTag(tag, auth, curve, tenant)
                    
                else :
                    print('need to code for else cond')
                
                curve['collectionData'] = collectionData
                calculatePrice(curve, auth, holiday_rule, tenant, tag)
            #print(curve['calculatedPrice'])
            
def process_expiry_calendar(calendar, month) :
    format_str = '%Y-%m-%dT%H:%M:%S'
    return_object = {}
    prev_last_date=  ''
    for i in range (0, len(calendar)) :
        expiry_obj = calendar[i]
        if expiry_obj['MONTH/YEAR'] == month :
            if len(prev_last_date) == 0 :
                print('previous month data not available')
            else :
                start_Date = datetime.datetime.strptime(prev_last_date, format_str)
                start_Date = start_Date + timedelta(1)
                return_object['start_date'] = start_Date
                end_Date = datetime.datetime.strptime(prev_last_date, expiry_obj['Last Trade Date'])
                return_object['end_date'] = end_Date
                return return_object
        else :
            prev_last_date = expiry_obj['Last Trade Date']

def retrieve_collection_common(tag, auth, curve, tenant) :
    if(len(collectionData)==0) :
        for j in range (0, 4) :
            print('no data for',month_year)
            monthNum = monthNum+1
            if(monthNum>12) :
                monthNum = 1
                yearNum = yearNum + 1
            month_year = datetime.date(1900, monthNum, 1).strftime('%B')[:3].upper()+str(yearNum)
            curve['monthYear'] = month_year
            collectionData = getPricesForTag(tag, auth, curve, tenant)
            if(len(collectionData) > 0) :
                break
   
