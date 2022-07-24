# -*- coding: utf-8 -*-
"""
Created on Thu Mar 26 18:00:30 2020

@author: Narendra.Negi
"""
import json
import requests
from PropertyFetcher import returnProperties

def fetchCollectionData(auth, curve, tenant) :
    try :
        keys = returnProperties()
        connectHost = keys['connect_host']
        pricingUDID = keys['pricingUDID']
        headers = {
            "Authorization": auth,
            "Content-Type": "application/json",
            "Accept": "application/json",
            "X-TenantID": tenant,
            "ttl": "10"
        }
        filter = []
        sort = []
        obj1={}
        obj1['fieldName'] = 'Month/Year'
        obj1['value'] = curve['monthYear']
        obj1['operator'] = 'eq'
        obj2 = {}
        obj2['fieldName'] = 'Instrument Name'
        obj2['value'] = curve['curveName']
        obj2['operator'] = 'eq'
        filter.append(obj1)
        filter.append(obj2)
        obj3 = {}
        obj3['fieldName'] = 'Pricing Date'
        obj3["direction"] = "ASC"
        sort.append(obj3)
        body = {}
        criteria={}
        criteria['sort'] = sort
        criteria['filter'] = filter
        body['skip'] = '0'
        body['limit'] = '1000'
        body['collectionName'] = 'DS-Market Prices'
        body['criteria'] = criteria
        body = json.dumps(body)
        uri = connectHost + "/collectionmapper/" + pricingUDID+"/"+pricingUDID+"/fetchCollectionRecords"
        print('getting collection')
        resp = requests.post(uri, headers = headers, data = body)
        #print('fetched collection', resp.json())
        collectionData = []
        collectionData = resp.json()
        if len(collectionData) == 0 :
            raise Exception('Prices not available for ',curve)
        return collectionData
    except :
        raise Exception('Error while fetching prices')
        

def fetchPowerPrices(curveObj,apiOrLambda="LAMBDA"):
    try:
        print('here')
        uri = 'https://e4cfykly4e.execute-api.us-east-2.amazonaws.com/dev/price'
        headers = {
                "Content-Type": "application/json"
          }
        
        body = {}
        header = {}
        header['X-TenantID'] = 'trm910'
        header['X-AccessToken'] = ''
        header['X-Platform-URL'] = ''
        header['X-Platform-HOST'] = ''
        payload = {}
        
        payload['startDate'] = curveObj['sd']
        payload['endDate'] = curveObj['ed']
        payload['startTime'] = curveObj['startTime']
        payload['endTime'] = curveObj['endTime']
        curves = [0]
        curve = {}
        curve['name'] = curveObj['curveName']
        curve['priceSubType'] = '30 Mins(Subhourly)'
        curves[0] = curve
        payload['curves'] = curves
        
        body['headers'] = header
        body['body'] = payload
        body = json.dumps(body)
        if apiOrLambda == "API":
            resp = requests.post(uri, headers = headers, data = body)
            #print(type(json.dumps(resp)))
            #pricesJson = []
            pricesJson = resp.json()
        else:
            resp = get_market_prices_from_lambda(body)
            pricesJson = json.loads(resp)
    	
        collectionData = []
        
        if len(pricesJson) == 0 :
            raise Exception('Prices not available for ',curve)
        
        for price in pricesJson:
            dailyJson = {}
            dailyJson['Pricing Date'] = price[0]
            dailyJson['Instrument Name'] = price[1]
            dailyJson['Settle Price'] = price[7]
            dailyJson['Start Time'] = price[3]
            dailyJson['End Time'] = price[4]
            #print('bhbhb', dailyJson)
            collectionData.append(dailyJson)
        return collectionData
    except :
        print('throwing except')
        raise Exception('Error while fetching prices')
        
	
	
def get_market_prices_from_lambda(payload):
    lambda_client = boto3.client("lambda")
    response = lambda_client.invoke(
        FunctionName='get_market_price',
        InvocationType='RequestResponse',
        Payload=payload
    )
    prices=response["Payload"].read()
    
    return prices