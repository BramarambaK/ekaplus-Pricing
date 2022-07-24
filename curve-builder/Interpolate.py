# -*- coding: utf-8 -*-
"""
Created on Mon Oct 14 13:20:14 2019

@author: Narendra.Negi
"""

import pandas as pd

def do_interpolate(df) :
    df['Pricing Date'] = pd.to_datetime(df['Pricing Date'], dayfirst = True)
    df.index = df['Pricing Date']
    df_interpol = df.resample('1D').mean()
    
    df_interpol['Settle Price'] = df_interpol['Settle Price'].interpolate()
    
    return df_interpol