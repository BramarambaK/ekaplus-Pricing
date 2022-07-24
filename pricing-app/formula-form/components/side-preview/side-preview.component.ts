import {
  Component,
  OnInit,
  Input,
  OnChanges,
  SimpleChanges,
  Output,
  EventEmitter,
  ViewChild
} from '@angular/core';
import { NgbModalConfig, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable } from 'rxjs';

import * as moment from 'moment';
import { THIS_EXPR } from '@angular/compiler/src/output/output_ast';
import { PricingService } from '../../../config.service';
// import { UtilService } from '@app/views/Contracts-Physicals/physicals-app/utils/util.service';
import { UtilService } from '../../../utility.service';
import { NgxSpinnerService } from 'ngx-spinner';
import { ApplicationService } from '@app/views/application/application.service';

@Component({
  selector: 'sidepreview',
  templateUrl: './side-preview.component.html',
  styleUrls: ['./side-preview.component.scss'],
  providers: [NgbModalConfig, NgbModal]
})
export class NgbdModalConfig {
  public isCollapsed1 = false;
  public isCollapsed2 = true;
  public isCollapsed3 = true;
  public isCollapsed4 = true;
  loadingFlag = true;
  active;
  todayDate = Date.now();
  @Input() curveSelected: boolean;
  @Input() disableFormulaModification: boolean
  @Input() conversionPrice
  @Input() newCreateData;
  @Input() globalproperties;
  @Input() contractDetails;
  @Input() itemNo = 0;
  @Input() itemIndex = 0;
  @Input() curveGroup;
  @Input() fromPricing;
  @Input() passToPricingApi

  color = ['#0000FF', '#00FFFF', '#FFD700', '#008000', '#FFFF00'];
  validFormatofdatepipe:any
  tabledata: {};
  sidePreviewData1: any;
  itemNoDisplay = 0;
  sidePreviewData: any;
  contractid;
  objectKeys = Object.keys;
  formulaData: any = {
    contractID: ' ',
    executeByContract: false,
    expression: ' ',
    curveList: ' '
  };
  curveList;
  errorMessage: any;
  errorstatus1 = true;
  newContract: any;
  disabled: boolean = true;
  @ViewChild('confirm') apiErrorPopup;
  @Output() pricePreviewButton = new EventEmitter();

  servicePriceUnitId: {
    serviceKey: string;
    dependsOn: any[]; // mdm for offset depends on two fields(mand) ,so "null" has to pass for that
  }[];

  constructor(
    config: NgbModalConfig,
    private modalService: NgbModal,
    private data: PricingService,
    private dataFromapiSidepreview: PricingService,
    private util: UtilService,
    private spinner:NgxSpinnerService,
    private applicationService: ApplicationService
  ) {
    config.backdrop = 'static';
    config.keyboard = false;
  }

  ngOnInit() { }

  ngOnChanges(changes: SimpleChanges): void { }

  open(content) {
    if(this.disableFormulaModification){
    this.newCreateData.curves = this.curveGroup.getRawValue()
    }
    this.validDate();
    let data = Object.values(this.curveGroup.controls)
     if(!this.validateformula(data)){
      this.pricePreviewButton.emit(true);
              return;
     }
    
    if (this.fromPricing) {
      for (var i = 0; i < Object.keys(this.curveGroup.controls).length; i++) {
        if (
          this.curveGroup.controls[i].controls.priceQuoteRule.value ===
          'Custom Period Average'
        ) {
          if (
            this.curveGroup.controls[i].controls.endDate.invalid ||
            this.curveGroup.controls[i].controls.startDate.invalid
          ) {
            this.pricePreviewButton.emit(true);
            return;
          }
        }
      }
    } else {
      for (var i = 0; i < Object.keys(this.curveGroup.controls).length; i++) {
        if (
          this.curveGroup.controls[i].controls.priceQuoteRule.value ===
          'Custom Period Average'
        ) {
          if (
            this.curveGroup.controls[i].controls.endDate.invalid ||
            this.curveGroup.controls[i].controls.startDate.invalid
          ) {
            this.pricePreviewButton.emit(true);
            return;
          }
        } else if (
          this.curveGroup.controls[i].controls.priceQuoteRule.value ===
          'Event Offset Based'
        ) {
          // if (
          //   this.curveGroup.controls[i].controls.offsetType.invalid ||
          //   this.curveGroup.controls[i].controls.offset.invalid ||
          //   this.curveGroup.controls[i].controls.event.invalid
          // ) {
          //   this.pricePreviewButton.emit(true);
          //   return;
          // }
        } else if (
          this.curveGroup.controls[i].controls.priceQuoteRule.value ===
          'Lookback Pricing'
        ) {
          if (
            this.curveGroup.controls[i].controls.offsetType.invalid ||
            this.curveGroup.controls[i].controls.offset.invalid
          ) {
            this.pricePreviewButton.emit(true);
            return;
          }
        }

      }
    }

    this.loadingFlag = true;
    this.applicationService.setLoaderState({type: 'pricing', value: true})
    this.sidePreviewData = null;

    this.curveList = Object.values(this.newCreateData.curves);
    for (let i = 0; i < this.curveList.length; i++) {
      if (this.curveList[i].endDate) {
        this.curveList[i].endDate = this.util.getISO(this.curveList[i].endDate);
      }
      if (this.curveList[i].startDate) {
        this.curveList[i].startDate = this.util.getISO(
          this.curveList[i].startDate
        );
      }
      if (this.curveList[i].quotedPeriodDate) {
        this.curveList[i].quotedPeriodDate = this.util.getDateQuotedPeriod(
          this.curveList[i].quotedPeriodDate
        );
      }
      if (this.curveList[i].pricingPeriod) {
        this.curveList[i].pricingPeriod = this.util.getDateQuotedPeriod(
          this.curveList[i].pricingPeriod
        );
      }
    }

    if (this.contractDetails === '') {
      if (this.contractDetails.internalContractRefNo === '') {
      } else {
        this.contractDetails.patchValue({
          _id: this.contractDetails.internalContractRefNo
        });
      }
    }

    if (this.contractDetails.itemDetails) {
      this.contractDetails.itemDetails = this.contractDetails.itemDetails.map(
        item => {
          if (item.deliveryFromDate && item.deliveryToDate) {
            let deliveryF = {};
            let deliveryt = {};
            if (item.deliveryFromDate.hasOwnProperty('date')) {
              deliveryF['date'] = item.deliveryFromDate.date;
            } else {
              deliveryF = item.deliveryFromDate;
            }
            if (item.deliveryToDate.hasOwnProperty('date')) {
              deliveryt['date'] = item.deliveryToDate.date;
            } else {
              deliveryt = item.deliveryToDate;
            }
            let deliveryFromDate = this.util.getISO(deliveryF);
            let deliveryToDate = this.util.getISO(deliveryt);

            let final = {
              ...item,
              deliveryFromDate,
              deliveryToDate
            };
            return final;
          } else return item;
        }
      );
    }
    if (this.contractDetails.itemDetails) {
      this.newContract = Object.assign(
        {},
        this.contractDetails.itemDetails[this.itemIndex]
      );
      if (this.newContract) {
        this.newContract['qty'] = this.newContract.itemQty;
        this.newContract['qtyUnit'] = this.newContract.itemQtyUnitId;
        this.servicePriceUnitId = [
          {
            serviceKey: 'productPriceUnit',
            dependsOn: [this.newContract.productId]
          }
        ];
      }

      delete this.newContract.itemQty;
      delete this.newContract.itemQtyUnitId;
    } else {
      this.newContract = {};
    }

    let newFormulaExp = '';
    // for (let i = 0; i < this.formulaArray.length; i++) {
    //   curveExp = curveExp + this.formulaArray[i] + ' ';
    // }

    for (let i = 0; i < this.newCreateData.formulaExpression.length; i++) {
      newFormulaExp =
        newFormulaExp + this.newCreateData.formulaExpression[i] + ' ';
    }
    this.formulaData = {
      contract: {
        refNo: this.contractDetails._id || '',
        asOfDate: this.util.getISO(
          this.util.getMyDatePickerDate(new Date().toISOString())
        ),
        itemDetails: [this.newContract]
      },

      formulaList: [
        {
          formulaExpression: newFormulaExp,
          contractCurrencyPrice: this.newCreateData.contractCurrencyPrice,
          holidayRule: this.newCreateData.holidayRule,
          pricePrecision: this.newCreateData.pricePrecision,
          triggerPricing: this.newCreateData.triggerPricing,
          triggerPricingValue: this.newCreateData.triggerPricingValue,
          priceDifferential: this.newCreateData.priceDifferential,
          curveList: this.curveList
        }
      ]
    };
    if(this.passToPricingApi){
    this.newContract['pricingComponentList'] = this.passToPricingApi
    
    }
    let errorMsg = this.util.validateForm(this.curveList)
    if(!errorMsg){
    if (!this.fromPricing) {
      this.dataFromapiSidepreview
        .getMdmMeta('pricing_call',this.servicePriceUnitId)
        .subscribe((response :any)=> {
          for (let i = 0; i < response.productPriceUnit.length; i++) {
            if (
              response.productPriceUnit[i].key ===
              this.newContract.pricing.priceUnitId
            )
              this.newContract.pricing['priceUnit'] =
                response.productPriceUnit[i].value;
          }
          this.dataFromapiSidepreview
            .postApiWorkflow('pricing_call',this.formulaData)
            .subscribe(
              (dataFromapiSidepreview:any) => {
                this.sidePreviewData1 = dataFromapiSidepreview.data;
                if(this.sidePreviewData1.hasOwnProperty('description')){
                  this.applicationService.setLoaderState({type: 'pricing', value: false})
                  this.apiErrorPopup.open(this.sidePreviewData1.description);
                }
                else{
                this.sidePreviewData1 = this.sortDate(this.sidePreviewData1)
                this.loadingFlag = false;
                this.applicationService.setLoaderState({type: 'pricing', value: false})
                this.errorstatus1 = false
                this.modalService.open(content, {
                  windowClass: 'pricingSidePreview'
                });
              }
              },
              err => {
                this.applicationService.setLoaderState({type: 'pricing', value: false})
                var mess = err.error.errorLocalizedMessage
                mess = mess.hasOwnProperty('description') ? mess.description : mess
                this.loadingFlag = false;
                this.errorstatus1 = true;
                this.apiErrorPopup.open(mess);
              }
            );
        });
    }

    if (this.fromPricing) {
      this.dataFromapiSidepreview.postApiWorkflow('pricing_call',this.formulaData).subscribe(
        (dataFromapiSidepreview:any) => {
          this.sidePreviewData1 = dataFromapiSidepreview.data;
          if(this.sidePreviewData1.hasOwnProperty('description')){
            this.applicationService.setLoaderState({type: 'pricing', value: false})
            this.apiErrorPopup.open(this.sidePreviewData1.description);
          }
          else{
          this.sidePreviewData1 = this.sortDate(this.sidePreviewData1)


          this.loadingFlag = false;
          this.applicationService.setLoaderState({type: 'pricing', value: false})

          this.errorstatus1 = false;
          this.modalService.open(content, {
            windowClass: 'pricingSidePreview'
          });
        }
        },
        err => {
          this.applicationService.setLoaderState({type: 'pricing', value: false})
          var mess = JSON.parse(err.error.errorLocalizedMessage)
          this.loadingFlag = false;
          this.errorstatus1 = true;
          this.apiErrorPopup.open(mess.description);        }
      );
    }}
    else {
      this.applicationService.setLoaderState({type: 'pricing', value: false});
      this.apiErrorPopup.open(errorMsg);
    }
  }

  modifyData(data) {
    var cache = {};

    for (let i = 0; i < data.length; i++) {
      if (cache[data[i].curveName]) {
        cache[data[i].curveName] = [...cache[data[i].curveName], data[i]];
      } else {
        cache[data[i].curveName] = [data[i]];
      }
    }

    return cache;
  }
  onSaveCurvePreview() {
    let createdataPreview = {
      exprssion: this.newCreateData.expression,
      curveList: this.newCreateData.curve
    };
    this.formulaData = createdataPreview;
  }

  toggle(name) {
    this.active = name;
  }

  closing() {
    this.modalService.dismissAll();
    this.loadingFlag = true;
  }

  convertDate({ day, month, year }) {
    if (day < 10) {
      day = '0' + day;
    }
    if (month < 10) {
      month = '0' + month;
    }
    return `${year}-${month}-${day}`;
  }

  convertStrToFloat(ele) {
    return parseFloat(ele)

  }
  sortDate(obj: any) {
    var finalCurveData = obj.contract.itemDetails[
      this.itemNoDisplay
    ].priceDetails.curveData
    for (var i = 0; i < finalCurveData.length; i++) {

      finalCurveData[i].data.sort(function (a, b) {
        return new Date(a.date).getTime() - new Date(b.date).getTime();
      });
    }

    return obj

  }
  validateformula(data){
    for(let i=0;i<data.length; i++){
      if(this.conversionPrice[i] && data[i].controls.qtyUnitConversionFactor.value ===''||data[i].controls.qtyUnitConversionFactor.value ===null){
        this.apiErrorPopup.open('Please fill the Quantity Conversion');
        return false
      }
    }
    return true
  }
  validDate(){
    this.validFormatofdatepipe = this.globalproperties['date_format']
    let split = this.globalproperties['date_format'].split('')
    for(let i=0;i<split.length;i++){
      if(split[i]=='D')split[i]='d'
      else if(split[i]=='Y')split[i]='y'
      else if(split[i]=='m')split[i] = 'M'
    }
    this.validFormatofdatepipe = split.join('')
  }
  getFxValue(value,curvefromresponse){
    if(value){
      return Number(value).toFixed(5)
    }
    else {
      let fx = this.formulaData.formulaList[0].curveList.filter((curve)=>{
        if(curve.curveName===curvefromresponse.curveName){
          return curve
        }
       })
      fx = Array.isArray(fx)&& fx.length > 0 ? fx[0].fxInput : 1.00000
      if(fx == 0){
        return '0.00000'
      }
      return fx ? Number(fx).toFixed(5) : 1.00000
    }

  }
  getMonthYear(date,qtyDataarray){
    let monthYear = ''
    let monthYearArray = qtyDataarray.find(item => item.date == date)
    monthYear = monthYearArray && monthYearArray.hasOwnProperty('instrumentDeliveryMonth')?monthYearArray.instrumentDeliveryMonth : ''
    return monthYear
  }
  getPriceDefinition(priceFlag){
   if(priceFlag){
   if(priceFlag === 'e' || priceFlag === 'l'){
    return 'Last Best Avail.'
   }
   else if (priceFlag === 'f'){
     return 'Actuals'
   }
   else if (priceFlag === 'Forward'){
    return 'Forward'
  }
   else if (priceFlag === 'Forward(Estimated)'){
    return 'Forward(Estimated)'
  }
  else{
    return priceFlag
  }
}
  return ''
  }
  getAvgFxValue(res){
    if(res.hasOwnProperty('avgPriceFx')){
      return Number(res.avgPriceFx).toFixed(5)
    }
    let fx = this.formulaData.formulaList[0].curveList.filter((curve)=>{
      if(curve.curveName===res.curveName){
        return curve
      }
     })
     fx = Array.isArray(fx)&&fx.length > 0 ? fx[0].fxInput : 1.00000
    return fx ? Number(fx).toFixed(5) : 1.00000
  }
}

