import { Injectable } from '@angular/core';
import * as moment from 'moment';
import { PricingService } from './config.service';
export interface IMyMonthLabels {
  [month: number]: string;
}
export interface IMyDefaultMonth {
  defMonth: string;
}
@Injectable({
  providedIn: 'root'
})
export class UtilService {
  constructor(private dateFormat:PricingService) { }

  getUnixDate(date) {
    if (date) {
      return this.getMomentDate(date).valueOf();
    }
    return null;
  }

  getItemListDate(date) {
    if (date) {
      let _temp = this.getMomentDate(date);
      return _temp.format('DDMMYYYY');
    }
    return null;
  }

  getDateFormatted(date) {
    if (date) {
      if (typeof date === 'object') {
        return date.formatted;
      }
      else return date
    }
  }

  getMyDatePickerDate(date) {
    if (date) {
      if (typeof date === 'object') {
        return date;
      } else {
        let temp: any;
        temp = this.getMomentDate(date);
        let date_new = {
          date: {
            year: temp.year(),
            month: temp.month() + 1,
            day: temp.date()
          }
        };
        return date_new;
      }
    }
    return null;
  }

  getMomentDate(date) {
    if (date) {
      if (typeof date === 'object') {
        if (date.hasOwnProperty('formatted')) {
          return moment(date.formatted, this.dateFormat.globaldateformat);
        } else if (date.hasOwnProperty('date')) {
          let _temp = { ...date.date };
          _temp.month -= 1;
          return moment(_temp);
        }
      } else if (typeof date === 'string') {
        if (/([0-3][0-9]-[0-1][0-9]-[0-9]{4})/.test(date)) {
          return moment(date, 'DD-MM-YYYY');
        } else if (/([0-9]{4}-[0-1][0-9]-[0-3][0-9])/.test(date)) {
          date = date.slice(0, 11);
          return moment(date, 'YYYY-MM-DD');
        }
      } else if (typeof date === 'number') {
        return moment(date);
      }
    }
  }

  getISO(date) {
    if (date) {
      if (moment(date, moment.ISO_8601).isValid()) {
        return date;
      } else {
        return this.getMomentDate(date).format('YYYY-MM-DDTHH:mm:ss.SSS+0000');
      }
    } else {
      return null;
    }
  }
  getDateQuotedPeriod(date) {
    if (moment(date).isValid()) {
      var x = Number(date.getMonth()) + 1
      var month = String(x)
      if(month.length === 1)month = '0' + month
      return month + '-' + date.getFullYear()
  }
  else return date
}
getDateFromQuotedDate(date){
 return new Date( date.split('-')[0] + '-02-'+ date.split('-')[1])
}
validateForm(curves) {
  var validmsg = ''
  if (Array.isArray(curves)) {
    validmsg = curves.reduce((m, curve) => {
      if (curve.pricePoint === 'Forward') {
        if ((curve.priceQuoteRule === 'Delivery Period Average' || curve.priceQuoteRule === 'Prompt Period Avg' || curve.priceQuoteRule === 'Settlement Date') && (curve.pricingPeriod === '' || curve.quotedPeriodDate === '')) {
          m = 'Please Fill Pricing Period and Quoted Period'
        }
        else if (curve.priceQuoteRule === 'Custom Period Average' && (curve.startDate === '' || curve.endDate == '')) {
          m = 'Please Fill Price Period'
        }
        else if (curve.priceQuoteRule === 'Event Offset Based' && curve.event === '') {
          m = 'Please Fill Event'
        }
      }
      else {
        if ((curve.priceQuoteRule === 'Event Offset Based') && curve.event === '') {
          m = 'Please Fill Event'
        }
        else if (curve.priceQuoteRule === 'Custom Period Average' && (curve.startDate === '' || curve.endDate == '')) {
          m = 'Please Fill Price Period'
        }
      }
      return m
    }, '')
  }

  return validmsg === '' ? false : validmsg;
}
}
