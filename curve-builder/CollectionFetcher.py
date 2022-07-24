# -*- coding: utf-8 -*-
"""
Created on Thu Oct 10 16:04:49 2019

@author: Narendra.Negi
"""

import requests
import csv
from Extrapolate import convertToDMY
from datetime import datetime
from Extrapolate import extrapolate_data
from CollectionAppendHelper import update_collection
from Interpolate import do_interpolate
import pandas as pd
from PropertyFetcher import returnProperties

def fetch_collection(auth, curveName, num_Of_days):
    keys = returnProperties()
    platform_url = keys['platform_url']
    #print('****************',num_Of_days)
    url = platform_url+"/collection/v1?collectionName=DS-Market Prices&limit=10000"
    headers = {
        "Authorization": auth,
        "Content-Type": "application/json",
        "Accept": "application/json"
    }
    
    body = {}
    filters = []
    f1 = {}
    f1['fieldName'] = 'Instrument Name'
    f1['value'] = curveName
    f1['operator'] = 'eq'
    filters.append(f1)
    f2 = {}
    f2['fieldName'] = 'Published Extrapolated'
    f2['value'] = 'Published'
    f2['operator'] = 'eq'
    f2 = {}
    f2['fieldName'] = 'Month/Year'
    f2['value'] = 'DEC2019'
    f2['operator'] = 'eq'
    filters.append(f2)
    body['filter'] = filters
    sort = []
    s1={}
    s1['fieldName'] = 'Pricing Date'
    s1['direction'] = 'ASC'
    sort.append(s1) 
    body['sort'] = sort
    #print(body)
    resp = requests.get(url, headers = headers, json = body)
    #print('resp', resp)
    data = resp.json()['data']
    #print('data',data)
    df = pd.DataFrame(data)
    columns = df.columns
    
    #print(columns)
    df.columns = df.columns.str.replace('/', '')
    #df.columns = df.columns.str.replace(' ', '')
    column_vals = df.values[0]
    constants = {}
    for j in range(0, len(columns)) :
        #print(columns[j], column_vals)
        
        if columns[j]!='Pricing date' and columns[j]!='Settle Price' :
            constants[columns[j]] = column_vals[j]
    print(df)
    months = []
    months = df.MonthYear.unique()
    prompts = []
    prompts = df['Prompt Date'].unique()
    #print("months",months)
    for i in range(0, len(months)) :
        constants['Month/Year'] = months[i]
        constants['Prompt Date'] = prompts[i]
        exp_check = df['MonthYear']==months[i]
        
        final_df = df[exp_check]
        dataset = final_df[['Pricing Date', 'Settle Price']]
        #print('dataset',dataset)
        interpolatedData = do_interpolate(dataset)
        #print('interpolatedData',interpolatedData)
        #print("Columns of df interpol:-", interpolatedData)
        #print(type(num_Of_days))
        extrapolated_data = extrapolate_data(num_Of_days, interpolatedData)
        #print('extrapolated_data',extrapolated_data)
        i=i+1
        update_collection(constants, extrapolated_data, auth)

def fetch_collection_Interpolation(auth, curveName):
    keys = returnProperties()
    platform_url = keys['platform_url']
    url = platform_url+"/collection/v1?collectionName=DS-Market Prices"
    headers = {
        "Authorization": auth,
        "Content-Type": "application/json",
        "Accept": "application/json"
    }
#    params = {
#        "collectionName": "MarketPrices"
#    }
    
    body = {}
    filters = []
    f1 = {}
    f1['fieldName'] = 'Instrument Name'
    f1['value'] = curveName
    f1['operator'] = 'eq'
    filters.append(f1)
    body['filter'] = filters
    sort = []
    s1={}
    s1['fieldName'] = 'Pricing Date'
    s1['direction'] = 'ASC'
    sort.append(s1) 
    body['sort'] = sort
#    print(headers)
    resp = requests.get(url, headers = headers, json = body)
    data = resp.json()
    f = open('AirPassenger.csv', 'r+')
    f.truncate(0)
    f.close
    dataArr = data['data']
    with open('AirPassenger.csv', 'w', newline='') as f:
        thewriter = csv.writer(f)
        thewriter.writerow(['Pricing Date', 'Settle Price'])
        for i in range(0, len(dataArr)):
            date_obj = dataArr[i].get('Pricing Date')
            if('T' in date_obj) :
                date_obj = date_obj.replace('T', ' ')
            date_obj = datetime.strptime(date_obj, '%Y-%m-%d %H:%M:%S')
            date_str = convertToDMY(date_obj)
            thewriter.writerow([date_str, dataArr[i].get('Settle Price')])
    return 0