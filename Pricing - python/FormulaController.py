# -*- coding: utf-8 -*-
"""
Created on Mon May  4 15:01:54 2020

@author: Narendra.Negi
"""
from flask import Flask, jsonify, request
import urllib.parse as urlparse
from urllib import parse
from PayloadHelper import executePricing

app = Flask(__name__)
@app.route('/api/pricing/formula',methods = ['POST'])
def execute():
    headers_post = request.headers
    url = request.url
    urlParsed = urlparse.urlparse(url)
    tag = urlparse.parse_qs(urlParsed.query)['tag'][0]
    auth = headers_post['Authorization']
    tenant = headers_post['X-TenantID']
    payload_json = request.get_json()
    result = executePricing(auth, payload_json, tag, tenant)
    return result
    
if __name__ == '__main__': 
    app.debug = False
    app.run(host = '0.0.0.0',port = 8180)
    