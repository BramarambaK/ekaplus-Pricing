# -*- coding: utf-8 -*-
"""
Created on Mon Feb 10 14:14:13 2020

@author: Narendra.Negi
"""
import json
import requests
import pandas as pd
from datetime import date, timedelta
import datetime
from PropertyFetcher import returnProperties

formatter =  '%Y-%m-%dT%H:%M:%S'

def getDatesPostHoliday(auth, exchange, start_Date, end_Date, holidayRule, tenant):
    
    delta = end_Date - start_Date       # as timedelta
    data = []
    for i in range(delta.days + 1):
        day = start_Date + timedelta(days=i)
        dateObj = {}
        dateObj['Date'] = day
        dateObj['To Be Used'] = day
        dateObj['Is Used'] = 0
        data.append(dateObj)
    dates = pd.DataFrame(data)
    if(len(auth)==0) :
        return dates
    holidays = fetchHoliday(auth, exchange, tenant)
    dates = calculateWorkingDays(dates, holidayRule, holidays)
    return dates
    

def fetchHoliday(auth, exchange, tenant):
    keys = returnProperties()
    connectHost = keys['connect_host']
    pricingUDID = keys['pricingUDID']
    headers = {
    "Authorization": auth,
    "Content-Type": 'application/json',
    "X-TenantID": tenant,
    "ttl": '10'
    }
    filter = []
    obj1={}
    obj1['fieldName'] = 'Exchange'
    obj1['value'] = exchange
    obj1['operator'] = 'eq'
    filter.append(obj1)
    body = {}
    criteria={}
    criteria['filter'] = filter
    body['collectionName'] = 'DS-Holiday'
    body['criteria'] = criteria
    body = json.dumps(body)
    uri = connectHost + "/collectionmapper/" + pricingUDID+"/"+pricingUDID+"/fetchCollectionRecords"
    print('getting holidays')
    resp = requests.post(uri, headers = headers, data = body)
    holidayJson = resp.json()
    #print(holidayJson)
    holidays = []
    for i in range(0, len(holidayJson)):
        holidays.append(datetime.datetime.strptime(holidayJson[i]['Holiday Date'], formatter))
    return holidays

def calculateWorkingDays(dates, holidayRule, holidays) :
    dates = pd.DataFrame(dates)
    for i in range(len(dates)) :
        if(holidayRule == 'Prior Business Day' and checkHoliday(dates['Date'][i], holidays)) :
            days = datetime.timedelta(1)
            new_day = dates['Date'][i] - days
            while(checkHoliday(new_day, holidays)) :
                new_day = new_day - days
            dates.at[i, 'To Be Used'] = new_day 
            #dates['To Be Used'][i] = new_day
        elif (holidayRule == 'Next Business Day' and checkHoliday(dates['Date'][i], holidays)) :
            days = datetime.timedelta(1)
            new_day = dates['Date'][i] + days
            while(checkHoliday(new_day, holidays)) :
                new_day = new_day + days
            dates.at[i, 'To Be Used'] = new_day
        elif (holidayRule == 'Ignore Weekends' and checkHoliday(dates['Date'][i], holidays)) :
            dates.drop([i], axis=0)
    #print(dates)
    return dates

def checkHoliday(dateObj, holidays) :
    dayNo = dateObj.weekday()
    if(dayNo>4 or date in holidays) :
        return True
    else :
        return False
            