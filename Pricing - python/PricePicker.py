# -*- coding: utf-8 -*-
"""
Created on Tue Feb  4 16:22:48 2020

@author: Narendra.Negi
"""
import datetime
from HolidayHelper import getDatesPostHoliday
import pandas as pd

def calculatePrice(curve, auth, holiday_rule, tenant, tag) :
    start_Date = datetime.time
    end_Date = datetime.time
    format1 = '%Y-%m-%dT%H:%M:%S.%f'
    price = 0.0
    previewArr = []
    if curve['collectionData'][0]['Exchange'] is None:
        exchange = 'ICE'
    else:
        exchange = curve['collectionData'][0]['Exchange']
    if(tag == 'Power') :
        if '+' in curve['contractFromDate'] :
            dateIndex = curve['contractFromDate'].index('+')
        else:
            dateIndex = len(curve['contractFromDate'])-1
        start_Date = datetime.datetime.strptime(curve['contractFromDate'][:dateIndex], format1)
        end_Date = datetime.datetime.strptime(curve['contractToDate'][:dateIndex], format1)
    elif(curve.get('priceQuoteRule')=='Custom Period Average') :
        dateIndex = curve['startDate'].index('+')
        start_Date = datetime.datetime.strptime(curve['startDate'][:dateIndex], format1)
        end_Date = datetime.datetime.strptime(curve['endDate'][:dateIndex], format1)
    elif(curve.get('priceQuoteRule')=='Contract Period Average') :
        if '+' in curve['contractFromDate'] :
            dateIndex = curve['contractFromDate'].index('+')
        else:
            dateIndex = len(curve['contractFromDate'])-1
        start_Date = datetime.datetime.strptime(curve['contractFromDate'][:dateIndex], format1)
        end_Date = datetime.datetime.strptime(curve['contractToDate'][:dateIndex], format1)
    dates = getDatesPostHoliday(auth, exchange, start_Date, end_Date, holiday_rule, tenant)
    dates = pd.DataFrame(dates)
    valid_days = dates['To Be Used']
    for i in range(0,len(valid_days)) :
        previewObj = {}
        currPrice = getPrice(curve['collectionData'], valid_days[i].to_pydatetime())
        #print(currPrice)
        price = price+currPrice
        previewObj['price'] = currPrice
        previewObj['date'] = valid_days[i].to_pydatetime()
        previewArr.append(previewObj)
    curve['calculatedPrice'] = price/len(previewArr)
    curve['previewSet'] = previewArr

def getPrice(collectionData, pricingDate) :
    for i in range(0, len(collectionData)) :
        currObj = collectionData[i]
        price = float(currObj['Settle Price'])
        return price