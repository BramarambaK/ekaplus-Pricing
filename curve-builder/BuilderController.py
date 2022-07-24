# -*- coding: utf-8 -*-
"""
Created on Mon Oct 14 09:30:18 2019

@author: Narendra.Negi
"""

from flask import Flask, jsonify, request
import Builder
import urllib.parse as urlparse
from urllib import parse

app = Flask(__name__)

@app.route('/data/extrapolate',methods = ['POST'])
def api_extrapolate():
    headers_post = request.headers
    url = request.url
    num_Of_days = parse.parse_qs(parse.urlparse(url).query)['numOfDays'][0]
    if isinstance(num_Of_days, str):
        num_Of_days=int(num_Of_days)
    else:
        pass
#    params = request.params
#    print(params)
    auth = headers_post['Authorization']
    curveName = headers_post['Curve-Name']
    Builder.StartBuilding(auth=auth, num_Of_days=num_Of_days, curveName = curveName)
    msg = {"message":'success'}
    return msg

@app.route('/data/interpolate',methods = ['POST'])
def api_interpolate():
     headers_post = request.headers
     print(headers_post)
     auth = headers_post['Authorization']
     curveName = headers_post['Curve-Name']
     Builder.startInterpolation(auth=auth, num_Of_days=20, curveName = curveName)
     msg = {"message":'success'}
     return msg

if __name__ == '__main__': 
    app.debug = False
    app.run(host = '0.0.0.0',port = 8998)