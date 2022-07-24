# -*- coding: utf-8 -*-
"""
Created on Fri Mar 27 13:32:12 2020

@author: Narendra.Negi
"""

def processOutput(contract_json, formula) :
    contract = {}
    contract_item = []
    priceDetails = {}
    curveData = []
    for i in range(0, len(formula['curveList'])) :
        curveDataObj = {}
        priceUnit = formula['curveList'][i]['collectionData'][0]['Price Unit']
        index = priceUnit.index('/')+1
        curveDataObj['curveQtyUnit'] = priceUnit[index:]
        curveDataObj['priceUnit'] = priceUnit
        curveDataObj['qpStartDate'] = ""
        curveDataObj['pricedQty'] = ""
        curveDataObj['curveName'] = formula['curveList'][i]['curveName']
        curveDataObj['curvePrice'] = formula['curveList'][i]['calculatedPrice']
        curveDataObj['curveCurrency'] = priceUnit[:index-1]
        curveDataObj['qpEndDate'] = ""
        curveDataObj['coefficient'] = ""
        curveDataObj['qtyUnit'] = ""
        curveDataObj['exchange'] = formula['curveList'][0]['collectionData'][0]['Exchange']
        curveDataObj['qtyData'] = ""
        curveDataObj['unPricedQty'] = ""
        curveDataObj['collapse'] = ""
        curveDataObj['data'] = formula['curveList'][i]['previewSet']
        curveData.append(curveDataObj)
    priceDetails['pricedQuantity'] = ""
    priceDetails['pricedPercentage'] = ""
    priceDetails['unpricedPercentage'] = ""
    priceDetails['unpricedQuantity'] = ""
    priceDetails['priceUnit'] = contract_json['itemDetails'][0]['pricing']['priceUnit']
    priceDetails['originalExpression'] = formula['formulaExpression']
    priceDetails['internalPriceUnitId'] = contract_json['itemDetails'][0]['pricing']['priceUnitId']
    priceDetails['quantityUnit'] = ""
    priceDetails['contractPrice'] = contract_json['contractPrice']
    priceDetails['curveData'] = curveData
    
    item = {}
    item['refNo'] = ""
    item['qPStartDate'] = ""
    item['qPEndDate'] = ""
    item['priceDetails'] = priceDetails
    contract_item.append(item)
    contract['refNo'] = contract_json['refNo']
    contract['asOfDate'] = contract_json['asOfDate']
    contract['itemDetails'] = contract_item
    return contract
    