# -*- coding: utf-8 -*-
"""
Created on Wed May 13 16:25:36 2020

@author: Narendra.Negi
"""
def returnProperties() :
    separator = "="
    keys = {}
    with open('environment.properties') as f:
        for line in f:
            if separator in line:
    
                # Find the name and value by splitting the string
                name, value = line.split(separator, 1)
    
                # Assign key value pair to dict
                # strip() removes white space from the ends of strings
                keys[name.strip()] = value.strip()
    return keys

