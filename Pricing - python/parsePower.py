# -*- coding: utf-8 -*-
"""
Created on Wed Feb 19 13:10:06 2020

@author: Narendra.Negi
"""

import json

with open("power prices.json", 'r') as f:
    payload_str = f.read()

print(json.loads(payload_str))