# -*- coding: utf-8 -*-
"""
Created on Thu Mar 26 13:28:01 2020

@author: Narendra.Negi
"""
from PricesAPI import fetchCollectionData
from PricesAPI import fetchPowerPrices
    
class PriceTagger:
    def callAPI(self, tag, authToken, curveObj, tenant):
        prices = []
        prices = self.tagIdentifier(tag, authToken, curveObj, tenant)
        return prices
    def tagIdentifier(self, tag, authToken, curveObj, tenant):
        prices = []
        if tag == 'Crude' :
            prices = self.getPricesForCrude(authToken, curveObj, tenant)
        elif tag == 'Power' :
            prices = self.getPricesForPower(curveObj)
        else :
            print('incorrect tag')
        return prices
    def getPricesForCrude(self, authToken, curveObj, tenant):
        prices = []
        prices = fetchCollectionData(authToken, curveObj, tenant)
        return prices
    def getPricesForPower(self, curveObj):
        prices = []
        prices = fetchPowerPrices(curveObj)
        marketPrices = []
        for i in range(0, len(prices)) :
            price_obj = {}
            #print(prices[i])
            price_obj['Pricing Date'] = prices[i]['Pricing Date']
            price_obj['Settle Price'] = prices[i]['Settle Price']
            price_obj['Exchange'] = 'ICE'
            price_obj['Price Unit'] = 'USD/MT'
            marketPrices.append(price_obj)
        return marketPrices

def getPricesForTag(tag, authToken, curveObj, tenant) :
       pt = PriceTagger()
       prices = []
       prices = pt.callAPI(tag, authToken, curveObj, tenant)
       return prices
