# -*- coding: utf-8 -*-
"""
Created on Thu Oct 10 16:06:06 2019

@author: Narendra.Negi
"""

import requests
import json
import pandas as pd
from datetime import date
from PropertyFetcher import returnProperties

def update_collection(constants, dataset, auth) :
    keys = returnProperties()
    platform_url = keys['platform_url']
    print('here for : ', constants['Month/Year'])
    format = "%Y-%m-%d %H:%M:%S"
    month_year = constants['Month/Year']
    exchange = constants['Exchange']
    instrument_name = constants['Instrument Name']
    publish_Date = date.today().strftime(format)
    prompt_date = constants['Prompt Date']
    prompt_date = prompt_date.replace("T"," ")
    derivative_type = constants['Derivative Type']
    trade_type = constants['Trade Type']
    price_unit = constants['Price Unit']
    dataArr = []
    for i in range(0, len(dataset)) :
        dataArr.append([publish_Date, dataset['Pricing Date'][i].strftime(format), 
                        prompt_date, derivative_type, exchange, instrument_name, 
                        month_year, trade_type, dataset['Settle Price'][i], 
                        price_unit, 'Extrapolated'])
    url = platform_url+"/collection/v1/append/data"
    headers = {
        "Authorization": auth,
        "Content-Type": "application/json",
        "Accept": "application/json"
    }
    body = {}
    body['collectionName'] = 'MarketPrices'
    body['collectionData'] = dataArr
    body['format'] = 'JSON'
    body = json.dumps(body)
    resp = requests.put(url, headers = headers, data = body)
    return resp.content
    
def update_collection_Interpolate(auth, curveName) :
    keys = returnProperties()
    platform_url = keys['platform_url']
    dataset = pd.read_csv('AirPassenger.csv')
    print(dataset)
    dataArr = []
    for i in range(0, len(dataset)):
        dataArr.append([dataset['Pricing Date'][i], dataset['Settle Price'][i]])
    print(dataArr)
    def json_list(list):
        lst = []
        for pn in list:
            lst.append([pn[0],"",curveName,float(pn[1])])
#        print(lst)
        return json.dumps(lst)
    
    url = platform_url+"/collection/v1/append/data"
    headers = {
        "Authorization": auth,
        "Content-Type": "application/json",
        "Accept": "application/json"
    }
    body = {}
    body['collectionName'] = 'MarketPrices'
#    body['collectionDescription'] = 'MarketPrices'
#    body['dataLoadOption'] = 'append'
    collectionHeader = []
    
    d = {}
    d['fieldName'] = 'Pricing Date'
    d['dataType'] = 'Date'
    collectionHeader.append(d)
    
    d1 = {}
    d1['fieldName'] = 'Settle Price'
    d1['dataType'] = 'Number'
    collectionHeader.append(d1)
    
    d2 = {}
    d2['fieldName'] = 'Instrument Name'
    d2['dataType'] = 'String'
    collectionHeader.append(d2)
    
    d3 = {}
    d3['fieldName'] = 'Exchange'
    d3['dataType'] = 'String'
    collectionHeader.append(d3)
    
#    body['collectionHeader'] = collectionHeader
    body['collectionData'] = json.loads(json_list(dataArr))
    body['format'] = 'JSON'
    body = json.dumps(body)
    print('body')
    print(body)
    resp = requests.put(url, headers = headers, data = body)
    print(resp.content)