import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class TableService {

  constructor(private http: HttpClient) { }

  formatHeader(fields, allowed) {
    const header = [];
    Object.keys(fields).forEach((k) => {
      if (allowed.indexOf(k) >= 0)
        header.push(Object.assign(fields[k], { field: k, sort: true, filter: true }));
    });
    return header;
  }
  formatHeaderFromContract() {

  }

  // Filters the data for distinct values for a column to be used in Table filter
  filter_distinct(array, property) {
    var unique = {};
    var distinct = [];
    for (var i in array) {
      if (typeof (unique[array[i][property]]) == "undefined") {
        distinct.push({ label: array[i][property], value: array[i][property] });
      }
      unique[array[i][property]] = 0;
    }
    return distinct;
  }

  //If display record is a object it display's its value otherwise prints the string
  display(obj) {
    if (obj.value)
      return obj.value;
    else return obj;
  }

  displayHeader(obj: any) {
    if (obj.labelKey)
      return obj[obj.labelKey];
    else return obj;
  }

  isSort(obj) {
    if (typeof obj.sort !== 'undefined')
      return obj.sort
    else
      return true;
  }

  isFilter(obj) {
    if (typeof obj.filter !== 'undefined')
      return obj.filter
    else
      return true;
  }


}
