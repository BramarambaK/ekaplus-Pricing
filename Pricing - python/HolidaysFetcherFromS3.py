# -*- coding: utf-8 -*-
"""
Created on Thu Sep  3 10:46:26 2020

@author: Narendra.Negi
"""

import json
import boto3
from PropertyFetcher import returnProperties

s3 = boto3.client('s3')
def lambda_handler(event, context):
    bucket = 'ds-holiday-for-power'
    key = create_path(tenant = event['tenant'], folder_in_s3='Holiday/'+event['exchange']+'/holiday.json')
    response = s3.get_object(Bucket=bucket, Key=key)
    #print(response)
    content = response['Body']
    #print(1,content)
    jsonObject = json.loads(content.read())
    for i in range(0, len(jsonObject)) :
        print(jsonObject[i])
        
def create_path(tenant, folder_in_s3) :
    keys = returnProperties()
    pricingUDID = keys['pricingUDID']
    path = tenant+'/'+pricingUDID+'/'+folder_in_s3
    return path