# -*- coding: utf-8 -*-
"""
Created on Tue Feb  4 19:32:04 2020

@author: Narendra.Negi
"""

from PriceFetcher import fetchPrice
from OutputProcessor import processOutput
from datetime import datetime
import copy

def executePricing(auth, payload_json, tag, tenant) :
    power_payload = {}
    curveList = []
    power_payload = copy.deepcopy(payload_json)
    if power_payload.get('formulaList') is None:
        power_payload = createPayloadFOrDeliveryItem(power_payload, 'formula')
    contract_json = power_payload.get('contract')
    formula = power_payload.get('formulaList')[0]
    holiday_rule = formula.get('holidayRule')
    if(len(holiday_rule.strip())==0) :
        holiday_rule = 'Prior Business Day'
    expression = formula.get('formulaExpression')
    if isinstance(expression, list) :
        expression = formula.get('newFormulaExp')
    curveList = formula.get('curveList')
    curveProperty = curveList
    if tag == 'Power':
        delivery_items = power_payload['delivery_items']
        
        for c in range(0,len(delivery_items)):
            delivery_date = delivery_items[0]['deliveryDate']
            delivery_date = datetime.strptime(delivery_date, "%d-%m-%Y").strftime("%Y-%m-%dT%H:%M:%S.%f")
            for i in range(0,len(curveList)):
                curveProperty[i]['contractFromDate'] = contract_json.get('itemDetails')[0].get('deliveryFromDate')
                curveProperty[i]['contractToDate'] = contract_json.get('itemDetails')[0].get('deliveryToDate')
            fetchPrice(curveProperty, auth, holiday_rule, tag, tenant)
            update_price(curveList, expression, curveProperty, contract_json)
            createDeliveryItemResponse(power_payload, contract_json, payload_json, c, 'formula')
            if len(power_payload['valuationFormulaDetails']) > 0 :
                createPayloadFOrDeliveryItem(power_payload, 'valuation')
                formula = power_payload.get('formulaList')[0]
                curveList = formula.get('curveList')
                curveProperty = curveList
                expression = formula.get('newFormulaExp')
                for i in range(0,len(curveList)):
                    curveProperty[i]['contractFromDate'] = contract_json.get('itemDetails')[0].get('deliveryFromDate')
                    curveProperty[i]['contractToDate'] = contract_json.get('itemDetails')[0].get('deliveryToDate')
                fetchPrice(curveProperty, auth, holiday_rule, tag, tenant)
                update_price(curveList, expression, curveProperty, contract_json)
                createDeliveryItemResponse(power_payload, contract_json, payload_json, c, 'valuation')
        del payload_json['formulaDetails']
        del payload_json['valuationFormulaDetails']
        return payload_json
    else :
        for i in range(0,len(curveList)) :
            curveProperty[i]['contractFromDate'] = contract_json.get('itemDetails')[0].get('deliveryFromDate')
            curveProperty[i]['contractToDate'] = contract_json.get('itemDetails')[0].get('deliveryToDate')
        fetchPrice(curveProperty, auth, holiday_rule, tag, tenant)
    
    for j in range(0,len(curveList)) :
        curve = curveProperty[j]
        curve_name = curve['curveName']
        curve_price = curve['calculatedPrice']
        expression = expression.replace(curve_name, str(curve_price), 1)
    contract_json['contractPrice'] = eval(expression)
    output = processOutput(contract_json, formula)
    if tag == 'Power' :
        return createDeliveryItemResponse(power_payload, contract_json, payload_json)
    #print(eval(expression))
    #print(output)
    return output

#with open("payload.json", 'r') as f:
 #   payload_str = f.read()
    
#f.close()

def createPayloadFOrDeliveryItem(payload, condition):
    contract_json = {}
    contract_json['refNo'] = ""
    contract_json['asOfDate'] = ""
    item_details = []
    item = {}
    delivery_date = payload['startDate']
    delivery_date = datetime.strptime(delivery_date, "%d-%b-%Y").strftime("%Y-%m-%dT%H:%M:%S.%f")
    item['deliveryFromDate'] = delivery_date
    item['deliveryToDate'] = delivery_date
    pricing = {}
    pricing['priceUnit'] = payload['priceUnit']
    pricing['priceUnitId'] = 'PPU'
    item['pricing'] = pricing
    item_details.append(item)
    contract_json['itemDetails'] = item_details
    payload['contract'] = contract_json
    formula_list = []
    formula = {}
    if condition == 'valuation' :
        formula = payload['valuationFormulaDetails']
    else:
        formula = payload['formulaDetails']
    curves = []
    curves = formula['curves']
    formula['curveList'] = curves
    #del formula['Curves']
    for i in range (0, len(curves)) :
        curves[i]['startDate'] = datetime.strptime(payload['startDate'], "%d-%b-%Y").strftime("%Y-%m-%dT%H:%M:%S.%f")
        curves[i]['sd'] = payload['startDate']
        curves[i]['endDate'] = datetime.strptime(payload['endDate'], "%d-%b-%Y").strftime("%Y-%m-%dT%H:%M:%S.%f")
        curves[i]['ed'] = payload['endDate']
        curves[i]['startTime'] = payload['startTime']
        curves[i]['endTime'] = payload['endTime']
    formula_list.append(formula)
    payload['formulaList'] = formula_list
    return payload
    
def createDeliveryItemResponse(payload, contract_json, power_payload, c, condition):
    cal_price = contract_json['contractPrice']
    if condition=='valuation' :
        power_payload['delivery_items'][c]['marketPrice'] = cal_price
    else :
        power_payload['delivery_items'][c]['contractPrice'] = cal_price
    
    return power_payload

def update_price(curveList, expression, curveProperty, contract_json):
    for j in range(0,len(curveList)) :
        curve = curveProperty[j]
        curve_name = curve['curveName']
        curve_price = curve['calculatedPrice']
        expression = expression.replace(curve_name, str(curve_price), 1)
    contract_json['contractPrice'] = eval(expression)
