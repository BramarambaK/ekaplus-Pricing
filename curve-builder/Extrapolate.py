# -*- coding: utf-8 -*-
"""
Created on Thu Oct 10 13:47:57 2019

@author: Narendra.Negi
"""

import numpy as np
import pandas as pd
import matplotlib.pylab as plt
from statsmodels.tsa.stattools import adfuller
from datetime import timedelta

def extrapolate_data(num_of_days, dataset):
    
    indexDataSet = dataset
    indexDataSet.dropna(inplace = True)
    #print('dataset')
    #print(indexDataSet)
    
       
    test_stationary(indexDataSet)
    logindex = np.log(indexDataSet)
    
    test_stationary(logindex)
    datasetshifting = indexDataSet - indexDataSet.shift()
    
    datasetshifting.dropna(inplace = True)
    from statsmodels.tsa.seasonal import seasonal_decompose
    decomposition = seasonal_decompose(np.log(indexDataSet), freq=7)
    
    trend = decomposition.trend
    seasonal = decomposition.seasonal
    residual = decomposition.resid
    
    plt.subplot(411)
    plt.plot(np.log(indexDataSet), label = 'Original')
    plt.legend(loc = 'best')
    plt.subplot(412)
    plt.plot(trend, label = 'Trend')
    plt.legend(loc = 'best')
    plt.subplot(413)
    plt.plot(seasonal, label = 'Seasonality')
    plt.legend(loc = 'best')
    plt.subplot(414)
    plt.plot(residual, label = 'Residuality')
    plt.legend(loc = 'best')
    plt.tight_layout()
    
    decomposed_logdata = residual
    decomposed_logdata.dropna(inplace=True)
    
    from statsmodels.tsa.stattools import acf, pacf
    lag_acf = acf(logindex, nlags = 10)
    lag_pacf = pacf(logindex, nlags = 10, method='ols')
    
    plt.subplot(121)
    plt.plot(lag_acf)
    plt.axhline(y=0,linestyle='--',color='gray')
    plt.axhline(y=-1.96/np.sqrt(len(logindex)),linestyle='--',color='gray')
    plt.axhline(y=1.96/np.sqrt(len(logindex)),linestyle='--',color='gray')
    plt.title('Autocorrelation Function')
    
    plt.subplot(122)
    plt.plot(lag_pacf)
    plt.axhline(y=0,linestyle='--',color='gray')
    plt.axhline(y=-1.96/np.sqrt(len(logindex)),linestyle='--',color='gray')
    plt.axhline(y=1.96/np.sqrt(len(logindex)),linestyle='--',color='gray')
    plt.title('Partial Autocorrelation Function')
    plt.tight_layout()
    
    from statsmodels.tsa.arima_model import ARIMA
    model = ARIMA(indexDataSet, order=(1,1,1))
    result_AR = model.fit(disp=-1)
    plt.plot(datasetshifting, color='black')
    plt.plot(result_AR.fittedvalues, color='red')
    plt.title('MSS: %.4f'% sum((result_AR.fittedvalues-datasetshifting['Settle Price'])**2))
    print('Plotting AR graph - AR')
    
    model = ARIMA(indexDataSet, order=(0,1,1))
    result_MA = model.fit(disp=-1)
    plt.plot(datasetshifting)
    plt.plot(result_MA.fittedvalues, color='red')
    plt.title('MSS: %.4f'% sum((result_MA.fittedvalues-datasetshifting['Settle Price'])**2))
    print('Plotting AR graph - MA')
    
    model = ARIMA(indexDataSet, order=(1,1,1))
    result_ARIMA = model.fit(disp=-1)
    plt.plot(datasetshifting)
    plt.plot(result_ARIMA.fittedvalues, color='red')
    plt.title('MSS: %.4f'% sum((result_ARIMA.fittedvalues-datasetshifting['Settle Price'])**2))
    print('Plotting AR graph - ARIMA')
    
    predictions_arema_diff = pd.Series(result_ARIMA.fittedvalues, copy= True)
    #print(predictions_arema_diff.head())
    
    predictions_arema_diff_cumsum = predictions_arema_diff.cumsum()
    #print(predictions_arema_diff_cumsum.head())
    
    prediction_ARIMA_log = pd.Series(indexDataSet['Settle Price'].ix[0], index = indexDataSet.index)
    prediction_ARIMA_log = prediction_ARIMA_log.add(predictions_arema_diff_cumsum, fill_value = 0)
    prediction_ARIMA_log.head()
    
    predictions_ARIMA = np.exp(prediction_ARIMA_log)
    plt.plot(indexDataSet,color='red')
    plt.plot(predictions_ARIMA,color='blue')
    
    result_ARIMA.plot_predict(1,100)
    result_ARIMA.forecast(steps=num_of_days)
    
    predictedValues = result_ARIMA.forecast(steps=num_of_days)[0]
    dataset['Pricing Date'] = dataset.index
    #print(dataset.columns.values)
    lastDate = dataset['Pricing Date'][len(dataset)-1]
    dataset = dataset.reset_index(drop = True)
    #print("^^^^^^^^^^^^^^^^^^^^^^^^^^^^\n",dataset)
    #lastDate = datetime.strptime(last_date_in_dataset, '%Y-%m-%d %H:%M:%S')
    dates = []
    for i in range(0, len(predictedValues)) :
        dates.append(lastDate + timedelta(days=1+i))
    #print(predictedValues)
    predicted_dataset = pd.DataFrame({dataset.columns[0] : predictedValues, 
                                      dataset.columns[1] : dates})
    dataset = dataset.append(predicted_dataset, ignore_index = True)
    return predicted_dataset

def test_stationary(timeseries):
    #Determining rolling statistics
    moving_average = timeseries.rolling(window=7).mean()
    movingSTD = timeseries.rolling(window=7).std()
    
    #Plot rolling statistics
    orig = plt.plot(timeseries,color='blue',label='Original')
    mean = plt.plot(moving_average,color='red',label='Rolling Mean')
    std = plt.plot(movingSTD,color='black',label='Rollong STD')
    plt.legend(loc='best')
    plt.title('Rolling Mean & Standard Deviation')
    plt.show(block=False)
    
    #Perform DIckey Fuller Test
    print('Results of Dickey-Fuller test')
    print(timeseries)
    dftest = adfuller(timeseries['Settle Price'], autolag='AIC')
    dfoutput = pd.Series(dftest[0:4], index=['Test Statistics', 'p-value', '#Lags Used', 'Number of observations used'])
    for key,value in dftest[4].items():
        dfoutput['Critical value (%s) '%key] = value
    print(dfoutput)
 
def convertToDMY(date_obj):
    date_str = date_obj.strftime("%d")+'-'+date_obj.strftime("%m")+'-'+date_obj.strftime("%Y")
    return date_str