# -*- coding: utf-8 -*-
"""
Created on Thu Oct 10 17:15:06 2019

@author: Narendra.Negi
"""
import time

from CollectionFetcher import fetch_collection
from CollectionFetcher import fetch_collection_Interpolation
from CollectionAppendHelper import update_collection_Interpolate
from Interpolate import do_interpolate

def StartBuilding(auth, num_Of_days, curveName):
    #Downloading data
    res = fetch_collection(auth, curveName, num_Of_days)
    return res
        
def startInterpolation(auth, num_Of_days, curveName):
    #Downloading data
    res = fetch_collection_Interpolation(auth, curveName)
    time.sleep(2)
    #Doing prediction
    res1 = 1
    if(res==0):
      res1 = do_interpolate()
    time.sleep(2)
    #Uploading data
    if(res1==0):
        update_collection_Interpolate(auth, curveName)