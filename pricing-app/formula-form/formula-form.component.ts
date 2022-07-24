import {
  Component,
  OnInit,
  createPlatformFactory,
  ElementRef,
  ViewChild,
  AfterViewInit,
  Input
} from '@angular/core';
import { NgbModal, ModalDismissReasons } from '@ng-bootstrap/ng-bootstrap';
import { TagInputModule } from 'ngx-chips';

import { HttpClient } from '@angular/common/http';
import {
  FormGroup,
  FormControl,
  FormBuilder,
  FormArray,
  Validators
} from '@angular/forms';

import { FormLayoutService } from '@app/views/application/dynamicFormBuilder/form-components/form-layout.service';
import { ActivatedRoute, Router } from '@angular/router';

import { PricingService } from '../config.service';
import { THIS_EXPR } from '@angular/compiler/src/output/output_ast';
import { IMyDateModel, IMyDpOptions, IMyDefaultMonth } from 'mydatepicker';
import * as moment from 'moment';
import { UtilService } from '../utility.service';
import { isEmpty } from 'rxjs/operators';
import { ApplicationService } from '@app/views/application/application.service';
import { TagModel } from 'ngx-chips/core/accessor';
import { Observable, of } from 'rxjs';
import 'rxjs/add/observable/of';
import { filter } from 'rxjs/operators';
// import { currentId } from 'async_hooks';

TagInputModule.withDefaults({
  dropdown: {
    displayBy: 'curveExpression',
    identifyBy: 'curveExpressionArray'
    // add here other default values for tag-input-dropdown
  }
});

@Component({
  selector: 'app-formula-form',
  templateUrl: './formula-form.component.html',
  styleUrls: ['./formula-form.component.scss']
})
export class FormulaFormComponent implements OnInit {
  // priceFormulaForm: FormGroup;

  curveData;
  curveDetailsMeta: any;
  curve;
  selectedSettleDate = false
  @Input() editFormulaId;
  @Input() fromContractPopup = false;
  @Input() indexPricing = false;
  arr = [];
  curveName;
  subCurrency= {
  'USD':{
    'isSubCurrency':'N',
    'parent':'NA'
  },
  'EUR':{
    'isSubCurrency':'N',
    'parent':'NA'
  }
  }
  @Input() fromPricing = true;
  @Input() valuation = false;
  @Input() itemNo;
  @Input() outerModal;
  @Input() contractDetails = {
    currentItem: 0,
    itemDetails: [{ itemQty: 0, deliveryFromDate: { date: '' }, deliveryToDate: { date: "" }, itemQtyUnitId: '', pricing: { payInCurId: '', priceUnitId: '', priceUnit: '',pricingFormulaId:'' }, productId: '',densityFactor:'',densityVolumeQtyUnitId:'',densityMassQtyUnitId:'',useDensityInPricingExposure:'' }],
    contractCurrencyPrice: ''
  };
  @Input() DataForPricing;
  @Input() itemId;
  @Input() itemIndex;
  @Input() mdmPriceUnit;
  @Input() formulaId;
  @Input() disableFormulaModification = false;

  @Input() fromViewContractPage = false;

  @ViewChild('confirm') SaveButtonPopup;

  // contractDetails = {
  //   currentItem: 0,
  //   itemDetails: [
  //     {
  //       itemQty: 0,
  //       pricing: {
  //         pricingFormulaId: ''
  //       }
  //     }
  //   ]
  // };



  selectedexpression;
  save1 = false;
  dateToday;
  ArrayForPriceDifferential = []
  formulacreateform = new FormGroup({
    category: new FormControl('', Validators.required),
    name: new FormControl('', Validators.required),
    contractCurrencyPrice: new FormControl(''),
    pricePrecision: new FormControl('2'),
    expression: new FormControl([])
  });
  holiday = new FormGroup({
    triggerValue: new FormControl(''),
    holidayRule: new FormControl(' '),
    priceDifferential: this.fb.array([]),
    triggerPricing: this.fb.array([])
  });
  completeForm;
  model;
  formFieldService;
  postService;
  response: any;
  curveArray;
  curveGroup;
  curveSelected = false;
  curveNewCurveNam;
  curveNewCurveExp;
  expression = {
    category: '',
    curveName: '',
    curveExpression: '',
    curveExpressionArray: [],
    includedCurve: []
  };
  triggerPricingData = [];
  mdmDataSend = [
    {
      serviceKey: 'pricePoint'
    },

    {
      serviceKey: 'priceType'
    },

    {
      serviceKey: 'priceQuoteRule'
    },
    {
      serviceKey: 'holidayRule'
    },
    {
      serviceKey: 'period'
    },
    {
      serviceKey: 'defineActualsEventType'
    },
   
    {
      serviceKey: 'quotedPeriod'
    },
    {
      serviceKey: 'fx'
    },
    {
      serviceKey: 'priceDifferential'
    }
  ];
  mdmData;
  setClickedRow: Function;
  selectedExpression;
  selectedRow: any;
  displayDropdown = false;
  displayButton = false;
  search;
  searchForCurveFromDropdown = '';
  showEdit = false;
  showName = false;
  saveBox = false;
  newCreateData;
  newTriggerPrice = true;
  triggerPriceRetrived = false;

  triggerPriceArrLength;
  createnewData = {
    formulaExpression: '',
    pricePrecision: '',
    contractCurrencyPrice: '',
    holidayRule: '',
    triggerPricing: [],
    priceDifferential: [],
    curves: []
  };
  order = 'value';
  ascending = true;
  action;

  editFormulaData;
  edit2;
  baseCurve = [];
  curveBaseData;
  curveBaseDataModal;
  lookbackInput = '';
  weekInput = '';
  offsetInputChange = '';
  _curveGroup;
  digits = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9'];
  submitted = false;
  pricePreviewButton = false;

  @ViewChild('box') searchInput: ElementRef;
  @ViewChild('save') popup;
  contractPopup;
  DuplicateFormulaName: boolean = false;
  color = ['#0000FF', '#00FFFF', '#FFD700', '#008000', '#FFFF00'];
  serviceOffset: { serviceKey: string; dependsOn: string[] }[];
  outputServiceOffset = {
    offsetType: [],
    offsetValue: []
  };
  showCurvename: any = '';
  counterForName: number = 0;
  fxDifferent = [];
  copyCurveGroup: any;
  currencyContractUnit:any
  conversionPrice = [];
  finalise = false;
  arrayexp: string[];
  formulaArray = [];
  newFormulaExp: string;
  changedPriceQuoteRule: any;
  priceUnit: any;
  saveCurve: boolean = true;
  fxRateFromAgency = []
  fxRateFromAgencyAll: any;
  componentOfProduct = []
  componentPricingEnabled=true
  passToPricingApi: any = []
  pricePointArray = {}
  fxDifferentqtyUnit: any=[];
  differentialUnitArray:any = []
  conversionQtyUnit: any = [];
  maxDateValue:any
  minDateValue:any
  conditionalpriceQuoteRule: any=[];
  currencyCont: any;
  globalproperties: Object;

  constructor(
    private modalService: NgbModal,
    private configService: PricingService,
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private uti: UtilService,
    private router: Router,
    private appService: ApplicationService
  ) {
    this.action = route.snapshot.params.action;
    configService.postApiWorkflow('seed_curve',{}).subscribe((response: any) => { });
    configService.getApiWorkflow('fxratepricing',false,{
      "collectionName": "DS-Market Fx Rate",
      "getAllRecords": true
  }).subscribe((response: any) => { 
      this.fxRateFromAgencyAll = response.data
      this.fxRateFromAgencyAll = this.fxRateFromAgencyAll.reduce((acc,item)=>{
        acc[item['Instrument Name']] = [{'Instrument Name' : item['Instrument Name'],'Currency Pair' : item['Currency Pair']}]
        return acc
      },{})
      for(let key  in this.fxRateFromAgencyAll){
        this.fxRateFromAgency.push(key); 
      }
    });
    // BaseCurve
    configService.getApiWorkflow('basecurve_list').subscribe((response: any) => {
      this.curveBaseData = response.data[0];

      for (let i = 0; i < this.curveBaseData.data.length; i++) {
        this.baseCurve.push({
          curveName: this.curveBaseData.data[i].map.curveName,
          curveExpression: this.curveBaseData.data[i].map.curveName,
          curveExpressionArray: [this.curveBaseData.data[i].map.curveName],
          curveType: 'Basic',
          category: 'Pricing',
          includedCurve: [this.curveBaseData.data[i].map.curveName]
        });
      }
      configService.getApiWorkflow('curve_list').subscribe((response:any) => {
        this.curveData = response.data;
        this.baseCurve = Array.from(this.baseCurve);
        this.curveData.push.apply(this.curveData, this.baseCurve);
      });
      this.upadatePricePoint()
    });

    //base curve integration end

    configService.getMdmMeta('formula_save',this.mdmDataSend).subscribe((response:any) => {
      this.mdmData = response;
      this.DisableForName()
      this.splitPriceDiffrential(this.mdmData.priceDifferential)
      if (this.fromPricing) {
        this.mdmData.priceQuoteRule = this.mdmData.priceQuoteRule.filter(
          this.checkDropDownForPriceQuoteRule
        );
      }

      if (
        this.action === 'edit' ||
        this.action === 'copy' ||
        this.configService.formulaListToForm
      ) {
        if (!this.configService.formulaListToForm) {
          this.editFormulaId = route.snapshot.queryParams.id;
        } else this.editFormulaId = this.configService.formulaListToForm._id;
        this.newTriggerPrice = false;

        configService.getApiWorkflow('formula_list',this.editFormulaId).subscribe((response:any) => {
          this.editFormulaData = response.data;
          this.edit2 = this.editFormulaData[0];

          if (this.action === 'edit' || this.configService.formulaListToForm) {

            if(this.valuation){
              this.editFormulaData[0].category = 'M2M'
            }
            this.formulacreateform.patchValue({
              category: this.editFormulaData[0].category,
              name: this.editFormulaData[0].formulaName,
              contractCurrencyPrice: this.contractDetails.itemDetails[this.itemIndex].pricing.priceUnitId,
              expression: this.editFormulaData[0].formulaExpression,
              pricePrecision: this.editFormulaData[0].pricePrecision
            });
          } else if (this.action === 'copy') {
            this.formulacreateform.patchValue({
              category: this.editFormulaData[0].category,
              name: '',
              contractCurrencyPrice: this.editFormulaData[0]
                .contractCurrencyPrice,
              expression: this.editFormulaData[0].formulaExpression,
              pricePrecision: this.editFormulaData[0].pricePrecision
            });
          }

          this.expression.includedCurve = this.editFormulaData[0].includedCurves;
          this.expression.curveExpression = this.editFormulaData[0].formulaExpression;

          for (let i = 0; i < this.expression.includedCurve.length; i++) {
            this.arr.push(this.expression.includedCurve[i]);

            // this.expression.includedCurve = Array.from(new Set(this.arr));
          }

          this.saveBox = false;
          this.formulacreateform.patchValue({
            expression: this.editFormulaData[0].formulaExpression
          });

          this.createFormGroup(this.expression);

          this.curveGroup.valueChanges.subscribe(data => {
            this.createnewData.curves = data;
          });
          for (var key in this.editFormulaData[0].curves) {
            if (this.editFormulaData[0].curves.hasOwnProperty(key)) {
              if (this.editFormulaData[0].curves[key].startDate) {
                this.editFormulaData[0].curves[ key].startDate = this.uti.getMyDatePickerDate(this.editFormulaData[0].curves[key].startDate);
              }
              if (this.editFormulaData[0].curves[key].endDate) {
                this.editFormulaData[0].curves[key].endDate = this.uti.getMyDatePickerDate(this.editFormulaData[0].curves[key].endDate);
              }
              if (this.editFormulaData[0].curves[key].quotedPeriodDate) {
                this.editFormulaData[0].curves[key].quotedPeriodDate = this.uti.getDateFromQuotedDate(this.editFormulaData[0].curves[key].quotedPeriodDate)
              }
              if (this.editFormulaData[0].curves[key].pricingPeriod) {
                this.editFormulaData[0].curves[key].pricingPeriod = this.uti.getDateFromQuotedDate(this.editFormulaData[0].curves[key].pricingPeriod)
              }
              if (this.editFormulaData[0].curves[key].priceQuoteRule) {
                if (this.editFormulaData[0].curves[key].priceQuoteRule === 'Lookback Pricing') {
                  this.editFormulaData[0].curves[key].event = null
                }
                this.OnChangePriceQuoteRule(this.editFormulaData[0].curves[key].priceQuoteRule, key)
                this.resetFormForValution(this.editFormulaData[0].curves[key].priceQuoteRule,key)
                this.changedPriceQuoteRule = this.editFormulaData[0].curves[
                  key
                ].priceQuoteRule;
              }
              if (this.editFormulaData[0].curves[key].event) {
                this.onChangeOffset(
                  this.editFormulaData[0].curves[key].event,
                  key
                );
              }
              if (this.getValueFromMdm(this.mdmPriceUnit, this.contractDetails.itemDetails[this.itemIndex].pricing.priceUnitId).split('/')[0] !== this.getValueFromMdm(this.mdmPriceUnit, this.editFormulaData[0].contractCurrencyPrice).split('/')[0]) {
                this.editFormulaData[0].curves[key].fxInput = ''
              }
            }
          }

          this.curveGroup.patchValue(this.editFormulaData[0].curves);
          this.fxCheck();

          this.curveGroup.valueChanges.subscribe(dataForCurvesOnChange => {
            this.createnewData.curves = dataForCurvesOnChange;
          });
          this.showEdit = true;
          this.holiday.patchValue({
            triggerValue: this.editFormulaData[0].triggerValue,
            holidayRule: this.editFormulaData[0].holidayRule,
            priceDifferential: this.editFormulaData[0].priceDifferential
          });
          this.triggerPricingData = this.editFormulaData[0].triggerPricing;
          if (this.triggerPricingData != null)
            this.createnewData.triggerPricing = [...this.triggerPricingData];
          if (this.triggerPricingData.length > 0) {
            this.triggerPriceRetrived = true;
            this.triggerPriceArrLength = this.triggerPricingData.length;
          } else {
            this.newTriggerPrice = true;
          }
        });
      }
    });
    // configService.getConfig().subscribe(response => {
    //   this.response = response;
    // });

    configService.getConfigMeta().subscribe(response => {
      this.curveDetailsMeta = response;
      if(this.valuation){
        this.curveDetailsMeta.fields.contractCurrencyPrice[
          this.curveDetailsMeta.fields.contractCurrencyPrice.labelKey
        ] = 'Market Price Unit'
      }
    });

    this.setClickedRow = function (index) {
      this.selectedRow = index;
    };

  }

  arrayToObject = array =>
    array.reduce((obj, item) => {
      obj[item.id] = item;
      return obj;
    }, {});

  get triggerPricing() {
    return this.holiday.get('triggerPricing') as FormArray;
  }

  get priceDifferential() {
    return this.holiday.get('priceDifferential') as FormArray;
  }

  addTriggerPricing() {
    this.triggerPricing.push(
      this.fb.group({
        triggerDate: [''],
        quantity: [''],
        price: ['']
      })
    );
  }

  addPriceDifferential() {
    let len = this.priceDifferential.value.length;
    let len2 = len - 1;

    if (len2 >= 0) {
      if (this.priceDifferential.value[0].differentialType === null) {
        return;
      }
    }

    this.priceDifferential.push(
      this.fb.group({
        differentialType: ['Discount'],
        differentialValue: [''],
        differentialUnit: [''],
        diffLowerThreashold: [''],
        diffUpperThreshold: ['']
      })
    );

    // this.priceDifferential.push(
    //   this.fb.group({
    //     differentialType: ['S-Curve'],
    //     differentialValue: [''],
    //     differentialUnit: [''],
    //     diffLowerThreashold: [''],
    //     diffUpperThreshold: ['']
    //   })
    // );

  }
  open(content) {
    this.modalService.open(content, { size: 'lg' });
  }
  ngOnChanges(changes) {
    if(this.componentPricingEnabled && (changes.contractDetails.currentValue || changes.DataForPricing.currentValue)){
      let details:any
      if(changes.contractDetails.currentValue) details = this.contractDetails
     else if(changes.DataForPricing.currentValue){details = this.DataForPricing
      this.itemIndex  = this.itemId}
    this.configService.getComponent(details).subscribe((response: any) => {
      for(let i=0;i<response.data.length;i++){
        if(Number(response.data[i].itemNo)==this.itemIndex + 1 && Number(response.data[i].componentPercentage)>=0  && response.data[i].gmrRefNo ==='NA'){
        this.componentOfProduct.push({ComponentWithComposition:response.data[i].productComponentName+' - '+response.data[i].componentPercentage + '%',Component:response.data[i].productComponentName})
        this.passToPricingApi.push(response.data[i])
        }
      }
     });
     
    }
    this.configService.getMdmMeta('pricing_call',this.mdmDataSend).subscribe((response:any) => {
      this.mdmData = response;
      if (this.fromPricing) {
        this.mdmData.priceQuoteRule = this.mdmData.priceQuoteRule.filter(
          this.checkDropDownForPriceQuoteRule
        );
      }
      if (this.fromContractPopup === true) {
        this.formulacreateform.get('contractCurrencyPrice').disable();
        this.itemIndex = this.itemId;
        let id = this.DataForPricing.itemDetails[this.itemId].pricing
          .pricingFormulaId;
        if(this.formulaId){
          id = this.formulaId;
        }
        this.configService.getApiWorkflow('formula_list',id).subscribe((response:any) => {
          this.contractPopup = response.data;
          if(this.valuation){
            this.contractPopup[0].category = 'M2M'
          }
          this.formulacreateform.patchValue({
            category: this.contractPopup[0].category,
            name: this.contractPopup[0].formulaName,
            contractCurrencyPrice: this.contractDetails.itemDetails[this.itemIndex].pricing.priceUnitId,
            expression: this.contractPopup[0].formulaExpression,
            pricePrecision: this.contractPopup[0].pricePrecision
          });
          this.expression.includedCurve = this.contractPopup[0].includedCurves;
          this.expression.curveExpression = this.contractPopup[0].formulaExpression;
          this.saveBox = false;
          this.formulacreateform.patchValue({
            expression: this.contractPopup[0].formulaExpression
          });
          this.fxCheck()

          this.createFormGroup(this.expression);
          this.curveGroup.valueChanges.subscribe(data => {
            this.createnewData.curves = data;
          });
          for (var key in this.contractPopup[0].curves) {
            if (this.contractPopup[0].curves.hasOwnProperty(key)) {
              if (this.contractPopup[0].curves[key].startDate) {
                this.contractPopup[0].curves[key].startDate = this.uti.getMyDatePickerDate(this.contractPopup[0].curves[key].startDate);
              }
              if (this.contractPopup[0].curves[key].endDate) {
                this.contractPopup[0].curves[key].endDate = this.uti.getMyDatePickerDate(this.contractPopup[0].curves[key].endDate);
              }
              if (this.contractPopup[0].curves[key].quotedPeriodDate) {
                this.contractPopup[0].curves[key].quotedPeriodDate = this.uti.getDateFromQuotedDate(this.contractPopup[0].curves[key].quotedPeriodDate)
              }
              if (this.contractPopup[0].curves[key].pricingPeriod) {
                this.contractPopup[0].curves[key].pricingPeriod = this.uti.getDateFromQuotedDate(this.contractPopup[0].curves[key].pricingPeriod)
              }
             
              if (this.contractPopup[0].curves[key].priceQuoteRule) {
                if (this.contractPopup[0].curves[key].priceQuoteRule === 'Lookback Pricing') {
                  this.contractPopup[0].curves[key].event = null
                }
                this.OnChangePriceQuoteRule(this.contractPopup[0].curves[key].priceQuoteRule, key)
                this.resetFormForValution(this.contractPopup[0].curves[key].priceQuoteRule,key)
                this.changedPriceQuoteRule = this.contractPopup[0].curves[
                  key
                ].priceQuoteRule;
              }
              if (this.contractPopup[0].curves[key].event) {
                this.onChangeOffset(
                  this.contractPopup[0].curves[key].event,
                  key
                );
              }
              if (this.getValueFromMdm(this.mdmPriceUnit, this.contractDetails.itemDetails[this.itemIndex].pricing.priceUnitId).split('/')[0] !== this.getValueFromMdm(this.mdmPriceUnit, this.contractPopup[0].contractCurrencyPrice).split('/')[0]) {
                this.contractDetails[0].curves[key].fxInput = ''
              }
            }
          }

          this.curveGroup.patchValue(this.contractPopup[0].curves);
          this.fxCheck();

          this.curveGroup.valueChanges.subscribe(dataForCurvesOnChange => {
            this.createnewData.curves = dataForCurvesOnChange;
          });
          this.showEdit = true;
          this.holiday.patchValue({
            triggerValue: this.contractPopup[0].triggerValue,
            holidayRule: this.contractPopup[0].holidayRule,
            priceDifferential: this.contractPopup[0].priceDifferential,
          });

          this.expression;
          this.disablecompeleteform()
        });
      }
    });
    if (this.fromContractPopup) {
      this.contractDetails = this.DataForPricing;
      if(this.contractPopup)
      this.newCreateData = this.contractPopup[0];
    }

    if (this.valuation === true) {
      this.formulacreateform.patchValue({
        category: 'M2M'
      });
    }
    
    if(changes.fromViewContractPage){
      this.fromViewContractPage = changes.fromViewContractPage.currentValue;
    }

  }

  get f() {
    return this.formulacreateform.controls;
  }
  DisableForName() {
    if (!this.fromPricing || !this.fromContractPopup) {
      if(!this.valuation)
      this.formulacreateform.patchValue({
        category: 'Pricing'
      });
      else{
        this.formulacreateform.patchValue({
          category: 'M2M'
        });

      }
      if (this.configService.formulaListToForm !== null) {
        this.formulacreateform.get('name').disable();
      }
      this.formulacreateform.get('category').disable();

    }
  }

  ngOnInit() {
    this.curveData;
    this.baseCurve;
    this.save1;
    this.showEdit;
    this.expression;
    this.checkChangesSidePrivew();
    this.addPriceDifferential();
    this.dateToday = new Date();
    this.formulacreateform.patchValue({
      contractCurrencyPrice: this.contractDetails.itemDetails[this.itemIndex].pricing.priceUnitId
    });
    this.currencyContractUnit =this.getValueFromMdm(this.mdmPriceUnit, this.formulacreateform.controls.contractCurrencyPrice.value)
    this.currencyCont = this.currencyContractUnit
    this.setCurrency(this.currencyContractUnit.split('/')[0])
    if (this.outerModal) {
      this.configService.outerModal = this.outerModal;
    }
    if (!this.fromPricing) {
      this.formulacreateform.get('contractCurrencyPrice').disable();
    }

    if (this.fromPricing) this.appService.setTitle('Formula Builder');
    this.disablecompeleteform()

    this.configService.layout('seed_curve').subscribe((layout:any) => {
      this.globalproperties = layout.properties;
      this.configService.globaldateformat = this.globalproperties['date_format'].toUpperCase()
      this.datePickerAbove['dateFormat'] =  this.globalproperties['date_format'].toLowerCase()
      this.deliveryFromDateOptions['dateFormat'] =  this.globalproperties['date_format'].toLowerCase()
      this.deliveryToDateOptions['dateFormat'] =  this.globalproperties['date_format'].toLowerCase()
    });

  }
  checkDropDownForPriceQuoteRule(dropdownData) {
    if (dropdownData.value === 'Custom Period Average') {
      return dropdownData;
    }
  }
  fxCheck() {
    if (this.mdmPriceUnit && this.curveBaseData) {
      for (var i = 0; i < this.expression.includedCurve.length; i++) {
            for (var j = 0; j < this.curveBaseData.data.length; j++) {
            if (this.expression.includedCurve[i] ===this.curveBaseData.data[j].map.curveName) {
              if(!this.differentialUnitArray[i])
            this.differentialUnitArray.push(this.curveBaseData.data[j].map.priceUnit)
            if (this.curveGroup && this.curveGroup.value[i]['differentialUnit']) this.curveGroup.value[i]['differentialUnit'] = this.curveBaseData.data[j].map.priceUnit;
            if (this.currencyContractUnit.split('/')[0] ===this.curveBaseData.data[j].map.priceUnit.split('/')[0]) {
              if(this.fxDifferent[i])
              this.fxDifferent[i] = false;
              else this.fxDifferent.push(false)
              if ( this.curveGroup && this.curveGroup.controls){
              this.curveGroup.controls[i].patchValue({fxInput: this.currencyCont==this.currencyContractUnit ?1 : 1});
              }
            } else {
              if(this.curveBaseData.data[j].map.priceUnit.split('/')[0] in this.subCurrency &&this.subCurrency[this.curveBaseData.data[j].map.priceUnit.split('/')[0]].isSubCurrency === 'N' ){
              if(this.fxDifferent[i])
              this.fxDifferent[i] = true;
              else this.fxDifferent.push(true)
              this.conversionPrice[i] =this.curveBaseData.data[j].map.priceUnit.split('/')[0] +'->' +this.currencyContractUnit.split('/')[0];
            }
            else if(this.curveBaseData.data[j].map.priceUnit.split('/')[0] in this.subCurrency &&this.subCurrency[this.curveBaseData.data[j].map.priceUnit.split('/')[0]].isSubCurrency === 'Y' && this.subCurrency[this.curveBaseData.data[j].map.priceUnit.split('/')[0]].parent!==this.currencyContractUnit.split('/')[0]){
              if(this.fxDifferent[i])
              this.fxDifferent[i] = true;
              else this.fxDifferent.push(true)
              this.conversionPrice[i] = this.subCurrency[this.curveBaseData.data[j].map.priceUnit.split('/')[0]].parent+'->' +this.currencyContractUnit.split('/')[0];
            }
            else if(this.curveBaseData.data[j].map.priceUnit.split('/')[0] in this.subCurrency &&this.subCurrency[this.curveBaseData.data[j].map.priceUnit.split('/')[0]].isSubCurrency === 'Y' && this.subCurrency[this.curveBaseData.data[j].map.priceUnit.split('/')[0]].parent===this.currencyContractUnit.split('/')[0]){
              if(this.fxDifferent[i])
              this.fxDifferent[i] = false;
              else this.fxDifferent.push(false)
              if (this.curveGroup) this.curveGroup.value[i]['fxInput'] = 1;
            }
            else {
              if(this.fxDifferent[i])
              this.fxDifferent[i] = true;
              else this.fxDifferent.push(true)
              this.setCurrency(this.curveBaseData.data[j].map.priceUnit.split('/')[0],false,i,j)
            
            }
          }
            if (this.currencyContractUnit.split('/')[1] ===this.curveBaseData.data[j].map.priceUnit.split('/')[1]) {
              if(this.fxDifferentqtyUnit[i])
              this.fxDifferentqtyUnit[i] = false;
              else this.fxDifferentqtyUnit.push(false)
              if ( this.curveGroup && this.curveGroup.controls){
                this.curveGroup.controls[i].patchValue({qtyUnitConversionFactor: 1});
              }
            } else {
              if(this.fxDifferentqtyUnit[i])
              this.fxDifferentqtyUnit[i] = true;
              else this.fxDifferentqtyUnit.push(true)
              
              this.conversionQtyUnit[i]=this.curveBaseData.data[j].map.priceUnit.split('/')[1] +'->' +this.currencyContractUnit.split('/')[1];
              
              if(this.curveGroup && this.curveGroup.value[i]['qtyUnitConversionFactor']){
              this.quantityConversionFromSetup(this.curveBaseData.data[j].map.priceUnit.split('/')[1],this.currencyContractUnit.split('/')[1],i)
              } 
            }
          }
        }
      }
    }
  }

  getValueFromMdm(arrOfDropdown, id) {
    for (var i = 0; i < arrOfDropdown.length; i++) {
      if (arrOfDropdown[i].key === id) {
        return arrOfDropdown[i].value
      }
    }
    return 'No Value Found'
  }

  checkChangesSidePrivew() {
    this.formulacreateform.valueChanges.subscribe(data => {
      this.createnewData.formulaExpression = data.expression;
      this.createnewData.contractCurrencyPrice = data.contractCurrencyPrice;
      this.createnewData.pricePrecision = data.pricePrecision;
    });

    this.holiday.valueChanges.subscribe((data: any) => {
      this.createnewData.holidayRule = data.holidayRule;
      if (this.newTriggerPrice) {
        this.createnewData.triggerPricing = data.triggerPricing;
      }
      if (this.triggerPriceRetrived) {
        this.createnewData.triggerPricing[this.triggerPriceArrLength] =
          data.triggerPricing[0];
      }
      this.createnewData.priceDifferential = data.priceDifferential;
    });

    this.holiday.get('triggerValue').valueChanges.subscribe((data: any) => {
      if(data === '' || data === false){
        this.priceDifferential.controls[0].reset({
          differentialType: 'Discount',
          differentialValue: '',
          differentialUnit: '',
          diffLowerThreashold: '',
          diffUpperThreshold: ''
        });
        if(this.triggerPricing.controls[0]){
          this.triggerPricing.controls = [];
        }
        this.holiday.patchValue({
          holidayRule : ' ',
          priceDifferential : [],
          triggerPricing : []
        })
      }
    });
  }

  toggle() {
    this.configService.changeCurrentComponent('listing');
  }
  displayCount(count) {
    this.search = count.curveExpression;
    this.expression = count;

    this.formulacreateform.patchValue({
      expression: this.expression.curveExpression
    });
    this.createFormGroup(count);
  }
  validateExpression(content11) {
    var curveDetail: any = Object.values(this.curveGroup.value);
    let errorMsg = this.uti.validateForm(curveDetail)
    if(!errorMsg){
    this.appService.setLoaderState({type: 'pricing', value: true})
    if(this.fromViewContractPage === true) {
      let rawdata = this.curveGroup.getRawValue()
      curveDetail= Object.keys(rawdata).map(function(key)  
      {  
       return rawdata[key];  
     });  
    }
    let newFormulaExp = '';
    for (let i = 0; i < this.formulacreateform.value.expression.length; i++) {
      newFormulaExp =
        newFormulaExp + this.formulacreateform.value.expression[i] + ' ';
    }
    var data = {
      contract: {
        refNo: 'jhkjdhb',
        asOfDate: this.uti.getISO(
          this.uti.getMyDatePickerDate(new Date().toISOString())
        ),
        itemDetails: [this.contractDetails.itemDetails[this.itemIndex]]
      },

      formulaList: [
        {
          formulaExpression: newFormulaExp,
          contractCurrencyPrice: ' ',
          holidayRule: this.holiday.value.holidayRule,
          pricePrecision: this.formulacreateform.value.pricePrecision,
          triggerPricing: this.createnewData.triggerPricing,
          triggerPricingValue: this.holiday.value.triggerValue,
          priceDifferential: this.holiday.value.priceDifferential,
          curveList: curveDetail
        }
      ]
    };
    if(this.passToPricingApi){
      data.contract.itemDetails[0]['pricingComponentList'] = this.passToPricingApi
      }
       if(data.contract.itemDetails[0].itemQty){
      data.contract.itemDetails[0]['qty'] = data.contract.itemDetails[0].itemQty;}
      if(data.contract.itemDetails[0].itemQtyUnitId){
      data.contract.itemDetails[0]['qtyUnit'] = data.contract.itemDetails[0].itemQtyUnitId;
      }
    var deliveryF = {}
    var deliveryt = {}
    if (data.contract.itemDetails[0].deliveryFromDate.hasOwnProperty('date')) {
      deliveryF['date'] = data.contract.itemDetails[0].deliveryFromDate.date;
    } else {
      deliveryF = data.contract.itemDetails[0].deliveryFromDate;
    }
    if (data.contract.itemDetails[0].deliveryToDate.hasOwnProperty('date')) {
      deliveryt['date'] = data.contract.itemDetails[0].deliveryToDate.date;
    } else {
      deliveryt = data.contract.itemDetails[0].deliveryToDate;
    }
    data.contract.itemDetails[0]['deliveryFromDate'] = this.uti.getISO(deliveryF);
    data.contract.itemDetails[0]['deliveryToDate'] = this.uti.getISO(deliveryt);
    data.contract.itemDetails[0].pricing.priceUnit = this.getValueFromMdm(this.mdmPriceUnit, this.contractDetails.itemDetails[this.itemIndex].pricing.priceUnitId)
    for (let i = 0; i < curveDetail.length; i++) {
      if (curveDetail[i].endDate) {
        curveDetail[i].endDate = this.uti.getISO(curveDetail[i].endDate);
      }
      if (curveDetail[i].startDate) {
        curveDetail[i].startDate = this.uti.getISO(
          curveDetail[i].startDate
        );
      }
      if (curveDetail[i].quotedPeriodDate) {
        curveDetail[i].quotedPeriodDate = this.uti.getDateQuotedPeriod(curveDetail[i].quotedPeriodDate);
      }
      if (curveDetail[i].pricingPeriod) {
        curveDetail[i].pricingPeriod = this.uti.getDateQuotedPeriod(curveDetail[i].pricingPeriod);
      }
     
    }
    this.configService.postApiWorkflow('pricing_call',data).subscribe(
      dataFromapiSidepreview => {
        this.onsubmit(content11);
      },
      err => {
        this.appService.setLoaderState({type: 'pricing', value: false})
        var message = JSON.parse(err.error.errorLocalizedMessage)
        this.popup.open(message.description);
      }
    );
    }
    else{
      this.popup.open(errorMsg);
    }
  }
  //formula creation
  onsubmit(content11) {
    this.newFormulaExp = '';
    for (let i = 0; i < this.formulacreateform.value.expression.length; i++) {
      this.newFormulaExp =
        this.newFormulaExp + this.formulacreateform.value.expression[i] + ' ';
    }
    this.DuplicateFormulaName = false;
    this.submitted = true;
    if (this.formulacreateform.invalid) {
      this.appService.setLoaderState({type: 'pricing', value: false})
      return;
    }

    if (this.action === 'edit') {

      
      this.curveArray = Object.values(this.curveGroup.value);
      for(let i=0;i<this.curveArray.length;i++){
        
        if (this.curveArray[i].quotedPeriodDate) {
          this.curveArray[i].quotedPeriodDate = this.uti.getDateQuotedPeriod(this.curveArray[i].quotedPeriodDate);
        }
        if (this.curveArray[i].pricingPeriod) {
          this.curveArray[i].pricingPeriod = this.uti.getDateQuotedPeriod(this.curveArray[i].pricingPeriod);
        }
        }


      let newCreate = {
        contract: this.contractDetails,

        category: this.formulacreateform.controls.category.value,
        newFormulaExp: this.newFormulaExp,
        formulaExpression: this.formulacreateform.value.expression,
        contractCurrencyPrice: this.formulacreateform.controls
          .contractCurrencyPrice.value,

        includedCurves: this.expression.includedCurve,
        curves: this.curveArray,
        formulaName: this.formulacreateform.controls.name.value,
        triggerValue: this.holiday.value.triggerValue,
        holidayRule: this.holiday.value.holidayRule,
        pricePrecision: this.formulacreateform.value.pricePrecision,
        priceDifferential: this.holiday.value.priceDifferential,
        _id: null
      };
     
      this.configService.associatedFormula(this.editFormulaId).subscribe(
        (data: any) => {
          if (data === true) {
            this.configService.formvaluepost('formula_save',newCreate);
          } else {
            this.popup.open(
              'This is Associated With Some Contract cannot modified'
            );
          }
        },
        err => {
          this.appService.setLoaderState({type: 'pricing', value: false})
          this.popup.open('Api error');
        }
      );

      // this.configService.formvalueput(newCreate, this.editFormulaId);
      if (!this.fromPricing) {
        this.configService.outerModal.dismiss(
          'base curve saved on used in contract'
        );
      } else {
        this.modalService.open(content11, { size: 'sm' });
      }
    } else {
      this.curveArray = Object.values(this.curveGroup.value);
      if(this.fromViewContractPage === true) {
        let rawdata = this.curveGroup.getRawValue()
        this.curveArray= Object.keys(rawdata).map(function(key)  
        {  
         return rawdata[key]; 
       });  
      }
      for(let i=0;i<this.curveArray.length;i++){
        
        if (this.curveArray[i].quotedPeriodDate) {
          this.curveArray[i].quotedPeriodDate = this.uti.getDateQuotedPeriod( this.curveArray[i].quotedPeriodDate);
        }
        if (this.curveArray[i].pricingPeriod) {
          this.curveArray[i].pricingPeriod = this.uti.getDateQuotedPeriod( this.curveArray[i].pricingPeriod);
        }
        }

      let newCreate = {
        contract: this.contractDetails,

        category: this.formulacreateform.controls.category.value,
        contractCurrencyPrice: this.formulacreateform.controls
          .contractCurrencyPrice.value,
        formulaExpression: this.formulacreateform.value.expression,
        newFormulaExp: this.newFormulaExp,
        includedCurves: this.expression.includedCurve,
        curves: this.curveArray,
        formulaName: this.formulacreateform.controls.name.value,
        triggerValue: this.holiday.value.triggerValue,
        holidayRule: this.holiday.value.holidayRule,
        pricePrecision: this.formulacreateform.value.pricePrecision,
        triggerPricing: this.holiday.value.triggerPricing,
        priceDifferential: this.holiday.value.priceDifferential,
        _id: null
      };
     
      if (!this.fromPricing) {
        if (this.configService.formulaListToForm)
          newCreate['_autoCreated'] = true;
        if (this.fromContractPopup) {
          newCreate['_autoCreated'] = true;
        }
      }

      this.configService.getApiWorkflow('formula_list').subscribe((responseData: any) => {
        let arr = responseData.data;
        if (!newCreate.hasOwnProperty("_autoCreated")) {
          for (let i = 0; i < arr.length; i++) {
            if (arr[i].formulaName === newCreate.formulaName) {
              this.DuplicateFormulaName = true;
            }
          }

          if (this.DuplicateFormulaName === false) {
            this.configService.postApiWorkflow('formula_save',newCreate).subscribe((data: any) => { 
              let deepClone = JSON.parse(JSON.stringify(newCreate));
              deepClone['_autoCreated'] = true;
              deepClone.triggerPricing = [] 
              deepClone.priceDifferential = this.setpricediffrential()
              this.configService.postApiWorkflow('formula_save',deepClone).subscribe((data: any) => {
                this.appService.setLoaderState({type: 'pricing', value: false})
                this.configService.changeContractFormula(data.data._id);

              },
              err => {
                this.appService.setLoaderState({type: 'pricing', value: false})
              });
            },
              err => {
                this.appService.setLoaderState({type: 'pricing', value: false})
              });
           
          } else {
            this.appService.setLoaderState({type: 'pricing', value: false})
            this.SaveButtonPopup.open('Formula Name already exists');
            return;
          }
          if (!this.fromPricing) {
            this.configService.outerModal.dismiss(
              'base curve saved on used in contract'
            );
          } else {
            this.SaveButtonPopup.open('Formula is Saved');
          }
        } else {
          if(this.contractDetails.itemDetails[this.itemIndex].pricing.pricingFormulaId === null){
            newCreate.triggerPricing = []
            newCreate.priceDifferential = this.setpricediffrential()        
            this.configService.postApiWorkflow('formula_save',newCreate).subscribe((data: any) => {
              this.configService.changeContractFormula(data.data._id);
              this.appService.setLoaderState({type: 'pricing', value: false})
            },
            err => {
              this.appService.setLoaderState({type: 'pricing', value: false})
            });}
            else{
              if(this.editFormulaId){
                this.contractDetails.itemDetails[this.itemIndex].pricing.pricingFormulaId = this.editFormulaId;
              }
              if(this.fromViewContractPage === true){
                newCreate._id = this.contractDetails.itemDetails[this.itemIndex].pricing.pricingFormulaId;
                this.configService.putApiWorkflow('formula_edit',newCreate,newCreate._id).subscribe((data: any) => {
                  this.appService.setLoaderState({type: 'pricing', value: false})
                  this.configService.changeContractFormula(data.data._id);
                },err => {
                  this.appService.setLoaderState({type: 'pricing', value: false})
                });
              }else{
                this.configService.postApiWorkflow('formula_save',newCreate).subscribe((data: any) => {
                  this.appService.setLoaderState({type: 'pricing', value: false})
                  this.configService.changeContractFormula(data.data._id);
                },err => {
                  this.appService.setLoaderState({type: 'pricing', value: false})
                });
              }
            }
          if (!this.fromPricing) {
            this.configService.outerModal.dismiss(
              'base curve saved on used in contract'
            );
          } else {
            this.SaveButtonPopup.open('Formula is Saved');
          }
        }
      });
    }
  }

  getPricePoint(exp,index) {
    for (let i = 0; i < this.curveBaseData.data.length; i++) {
      if (this.curveBaseData.data[i].map.curveName === exp) {
        this.upadatePriceQuoteRule(index,this.curveBaseData.data[i].map.pricePoint)
        return this.curveBaseData.data[i].map.pricePoint;
      }
    }
    this.upadatePriceQuoteRule(index,'Forward')
    return 'Forward';
  }
  getActualQuoted(exp,index){
    if(this.getPricePoint(exp,index) ==='Forward')return false
    else return {value: false, disabled: true}
  }

  upadatePricePoint(){
    // this.pricePointArray = this.mdmData.pricePoint

    for(let i = 0; i < this.curveBaseData.data.length; i++) {
      if(!this.pricePointArray[this.curveBaseData.data[i].map.curveName]){
        this.pricePointArray[this.curveBaseData.data[i].map.curveName]=[{key:this.curveBaseData.data[i].map.pricePoint,value:this.curveBaseData.data[i].map.pricePoint}]
      }
      else{
        this.pricePointArray[this.curveBaseData.data[i].map.curveName].push({key:this.curveBaseData.data[i].map.pricePoint,value:this.curveBaseData.data[i].map.pricePoint})
      }
    }
  }

getpriceType(element){
  for (let i = 0; i < this.curveBaseData.data.length; i++) {
    if (this.curveBaseData.data[i].map.curveName === element && this.curveBaseData.data[i].map.priceSubType==='Spot') {
      return 'Close'
    }
  }
 return 'Settle Price'
  
}
  //creating curve form
  createFormGroup(expression) {
    let _group = {};
    this.curveSelected = true;
    

    expression.includedCurve.forEach((element, i) => {
      this._curveGroup = this.fb.group({
        pricePoint: [this.getPricePoint(element,i)],
        quotedPeriod: ['M'],
        version:['V2'],
        exposureEnabled:[true],
        quotedPeriodDate: [''],
        isActualQuoted:[this.getActualQuoted(element,i)],
        isActualPricing:[false],
        monthDefinition:['BOM'],
        offsetDays:[''],
        pricingPeriod: [''],
        priceType: [this.getpriceType(element)],
        priceQuoteRule: ['Delivery Period Average'],
        period: ['Delivery Period'],
        startDate: ['', [Validators.required]],
        endDate: ['', [Validators.required]],
        event: ['', [Validators.required]],
        offsetType: ['Day', [Validators.required]],
        offset: ['Mon'],
        fxType: ['Fixed'],
        fxInput: [''],
        fxCurve:[''],
        differential: [''],
        indexPrecision:['5'],
        qtyUnitConversionFactor:[1],
        differentialUnit:[''],
        curveUnit:[this.getCurveUnit(i)],
        curveName: [element]
      });
      if (this.fromPricing) {
        this._curveGroup
          .get('priceQuoteRule')
          .setValue('Custom Period Average');
      }
      _group[i.toString()] = this._curveGroup;
    });

    this.curveGroup = this.fb.group(_group);

    // this.copyCurveGroup.push(this._curveGroup);
  }

  getFormGroup(val) {
    return this.curveGroup.get(val.toString());
  }

  getFormControl(group, control) {
    let _temp = this.curveGroup.get(group.toString()) as FormArray;

    return _temp.get(control);
  }

  add1(expr) {
    if (expr.match('^([-+/*(){}])$') || expr.match('^[+-]?([0-9]+([.][0-9]*)?|[.][0-9]+)$')) {
      this.hideDropdown()
    }
    else { this.searchForCurveFromDropdown = expr; }
    this.get();

    this.curveNewCurveExp = expr;
  }

  deleteCurve(expr) {
    for (let i = 0; i < this.curveData.length; i++) {
      if (expr == this.curveData[i].curveExpression) {
        this.save1 = false;
        break;
      }
    }
  }
  name(name) {
    this.curveName = name;
  }

  addcurveexp(expr1, expArray, expr2, type, a) {
    if (type === 'Custom') {
      this.expression.curveExpression = this.expression.curveExpression + expr1;
    } else {
      this.expression.curveExpression = this.expression.curveExpression + expr2;
    }

    this.formulaArray = this.formulacreateform.value.expression;
    for (let i = 0; i < expArray.length; i++) {
      this.formulaArray.push(expArray[i]);
    }

    // this.arrayexp = this.expression.curveExpression.split(regex);

    this.formulacreateform.patchValue({
      expression: this.formulaArray
    });
    // this.displayname = expr2;
    let inc = [];
    for (let i = 0; i < a.length; i++) {
      this.arr.push(a[i]);
      // this.expression.includedCurve = Array.from(new Set(this.arr));
      this.expression.includedCurve.push(a[i]);
    }
    this.expression.curveName = expr2;
    if (this.counterForName === 0 && this.showCurvename === '') {
      this.showCurvename = expr2;
      this.counterForName += 1;
    } else this.showCurvename = 'Custom';
  }

  addNewCurveName(a) {
    this.curveNewCurveNam = a;
  }

  onsubmitcurve() {
    let curveExp = '';
    for (let i = 0; i < this.formulaArray.length; i++) {
      curveExp = curveExp + this.formulaArray[i] + ' ';
    }
    console.log(curveExp);

    let newCreate = {
      curveName: this.curveNewCurveNam,
      curveExpression: curveExp,
      curveType: 'Custom',
      curveExpressionArray: this.formulaArray,
      includedCurve: this.expression.includedCurve
    };
    if (newCreate.curveName === '' || newCreate.curveExpression === '' || newCreate.curveName === undefined) {
      this.saveCurve = false
      return
    }
    this.configService.postApiWorkflow('curve_save',newCreate).subscribe();
    this.curveData.unshift(newCreate);
    this.saveBox = !this.saveBox;
    this.showCurvename = newCreate.curveName;
    this.showName = false;
    this.expression.curveName = this.curveNewCurveNam;

    this.showName = false;
  }
  toggleforsave() {
    this.save1 = !this.save1;
  }
  hideDropdown() {
    setTimeout(() => {
      this.displayDropdown = false;
    }, 300);
    this.curveSelected = false;

    this.showEdit = false;
  }
  setInputCurve(expression) {
    this.addcurveexp(
      expression.curveExpression,
      expression.curveExpressionArray,
      expression.curveName,
      expression.curveType,
      expression.includedCurve
    );
  }
  setCurveDetails() {
    this.upadatePricePoint()
    this.saveBox = false;
    console.log(this.formulacreateform.value.expression);

    // for (let i = 0; i < this.expression.includedCurve.length; i++) {
    //   for (let j = 0; j < this.formulacreateform.value.expression.length; j++) {
    //     if (
    //       this.expression.includedCurve[i] ===
    //       this.formulacreateform.value.expression[i]
    //     ) {
    //       break;
    //     } else if (j === this.formulacreateform.value.expression.length - 1) {
    //       this.expression.includedCurve.splice(i, 1);
    //     }
    //   }
    // }

    this.formulacreateform.patchValue({
      expression: this.formulacreateform.value.expression
    });
    //console.log(this.expression);
    this.createFormGroup(this.expression);

    this.createnewData.curves = this.curveGroup.value;
    this.curveGroup.valueChanges.subscribe(data => {
      this.createnewData.curves = data;
    });
    this.showEdit = true;
    this.curveSelected = true;
    this.fxDifferent = [];
    this.fxDifferent = new Array(this.expression.includedCurve.length).fill(
      true
    );
    this.differentialUnitArray=[]
    this.conversionPrice = []
    this.fxCheck();
  }

  editSaveCurve() {
    this.saveBox = true;
    // this.searchInput.nativeElement.focus();
    this.showName = false;
    this.showEdit = false;
    this.curveSelected = false;
    if(this.curveGroup){
      this.curveGroup.reset()
    }
    this.differentialUnitArray =[]
    this.conversionPrice = []
    this.fxDifferent = []
    this.fxDifferentqtyUnit=[]
  }

  formulaSavedForContract(modal) {
    modal.close('Close click');
    //this.configService.changeCurrentComponent('listing');
    this.outerModal.dismiss('base curve saved on used in contract');
  }
  aftersaveFormula() {
    if (this.fromPricing === true) {
      this.router.navigate(['../view'], {
        relativeTo: this.route
      });
    }
  }
  get() {
    return this.searchForCurveFromDropdown;
  }

  setTriggerPricingData() {
    var dataOfTriggerPricing;
    this.holiday.valueChanges.subscribe(data => (dataOfTriggerPricing = data));
    this.triggerPricingData.push(dataOfTriggerPricing);
  }

  isExist(val) {
    var mathRegex = /^\d+\s$/;
    if (val !== '') return val;
  }

  delWholeExpression(expression) {
    this.expression.curveExpression = '';
    for (let i = 0; i < this.expression.includedCurve.length; i++) {
      this.expression.includedCurve.splice(i);
    }
    this.showCurvename = '';
    // var ll = Math.parse(this.expression.curveExpression);
    let value = expression.match(/[^\s()*/%+-]+/).filter(this.isExist);
    var value2 = expression.split('/(+|-|*|/|=|>|<|>=|<=|&|||%|!|^|(|))$/');
    var value3 = expression.match(/<[^>]*>/gi);
  }
  delFromArray(expression) {
    //console.log(expression);
    var c = 0,
      k = 0;
    let lastletter = expression[expression.length - 1];
    if (lastletter.search(/[a-zA-Z]/i) + 1) {
      // expression = expression.subtr(0, expression.length - 1);

      let value = expression
        .match(/(([a-zA-Z])+\s)*(([a-zA-Z])+)/g)
        .filter(this.isExist);

      for (let i = 0; i < this.expression.includedCurve.length; i++) {
        for (let j = 0; j < value.length; j++) {
          if (value[j] === this.expression.includedCurve[i]) {
            c = 1;
            k = value[j + 1].length;
            break;
          }
        }
        if (c === 0) {
          this.expression.includedCurve.splice(i, i);
          this.expression.curveExpression = this.expression.curveExpression.slice(
            0,
            expression.length - k
          );
        } else {
          c = 0;
        }
      }
    }
  }

  weekChange(val, i) {
    let chIbn = val.split('-').join('');
    if (chIbn.length > 0) {
      chIbn = chIbn.match(new RegExp('.{1,3}', 'g')).join('-');
    }

    this.weekInput = chIbn;

    this.curveGroup.controls[i].controls.offset.setValue(this.weekInput);
    return this.curveGroup.controls[i].controls.offset.value;
  }
  offsetChange(val, i) {
    let chIbn = val.split('-').join('');
    if (chIbn.length > 0 && (this.curveGroup.controls[i].controls.offsetType.value==='Month'|| this.curveGroup.controls[i].controls.offsetType.value==='Month Average'||this.curveGroup.controls[i].controls.offsetType.value==='Price Period Average')&&  this.curveGroup.controls[i].controls.priceQuoteRule.value!=='Lookback Pricing') {
     chIbn = this.validateOffset(chIbn,'Month')
    }
    else  if (chIbn.length > 0 && (this.curveGroup.controls[i].controls.offsetType.value==='Month'|| this.curveGroup.controls[i].controls.offsetType.value==='Month Average'||this.curveGroup.controls[i].controls.offsetType.value==='Price Period Average') && this.curveGroup.controls[i].controls.priceQuoteRule.value==='Lookback Pricing') {
      chIbn = this.validateOffset(chIbn,'Month',this.curveGroup.controls[i].controls.priceQuoteRule.value)
     }
    else if (chIbn.length > 0 && this.curveGroup.controls[i].controls.offsetType.value==='Day') {
      chIbn = this.validateOffset(chIbn,'Day')
     }
     else if (chIbn.length > 0 && this.curveGroup.controls[i].controls.offsetType.value==='Week') {
      chIbn = this.validateOffset(chIbn,'Week')
     }
    else if(chIbn.length > 0){
      chIbn = chIbn.match(new RegExp('.{1,1}', 'g')).join('-');
      }

    this.offsetInputChange = chIbn;

    this.curveGroup.controls[i].controls.offset.setValue(
      this.offsetInputChange
    );
    return this.curveGroup.controls[i].controls.offset.value;
  }
  validateLetter(evt) {
    var charCode = evt.which ? evt.which : evt.keyCode;
    return !(charCode > 31 && (charCode < 48 || charCode > 57));
  }
  // validateLetterFrom(pos) {
  //   var charCode = evt.which ? evt.which : evt.keyCode;
  //   return !(charCode > 31 && (charCode < 48 || charCode > 57));
  // }
  OnChangePriceQuoteRule(event, i) {
    this.curveGroup.controls[i].get('offset').reset();
    if (
      event.split(': ')[1] === 'Lookback Pricing' ||
      event === 'Lookback Pricing'
    ) {
      let z = '';
      for (let j = 0; j < this.mdmData.priceQuoteRule.length; j++) {
        if (
          this.mdmData.priceQuoteRule[j].value === 'Lookback Pricing'
        ) {
          z = this.mdmData.priceQuoteRule[j].key;
          break;
        }
      }

      this.serviceOffset = [
        {
          serviceKey: 'offsetType',
          dependsOn: [z, 'null'] // mdm for offset depends on two fields(mand) ,so "null" has to pass for that
        }
      ];
      this.configService.getMdmMeta('pricing_call',this.serviceOffset).subscribe((response:any) => {
        this.outputServiceOffset = response;
        this.curveGroup.controls[i].get('offsetType').setValue('Month');
      });
    }
    else if (event.split(': ')[1] === 'Event Offset Based') {
      this.curveGroup.controls[i].get('event').reset();
    }
    if(event.split(': ')[1] === 'Settlement Date'){
      this.selectedSettleDate = true
    }
    else{
      this.selectedSettleDate = false
    }
  }

  onChangeOffset(event, i) {
    // this.curveGroup.controls[i].get('offset').reset();
    // let y = '';
    // let z = '';
    // for (let i = 0; i < this.mdmData.customEventAndBaseDateList.length; i++) {
    //   if (
    //     this.mdmData.customEventAndBaseDateList[i].value === event.split(': ')[1] ||
    //     this.mdmData.customEventAndBaseDateList[i].value === event
    //   ) {
    //     y = this.mdmData.customEventAndBaseDateList[i].key;
    //     break;
    //   }
    // }
    // for (let j = 0; j < this.mdmData.priceQuoteRule.length; j++) {
    //   if (this.changedPriceQuoteRule === 'Event Offset Based') {
    //     if (
    //       this.mdmData.priceQuoteRule[j].value === this.changedPriceQuoteRule
    //     ) {
    //       z = this.mdmData.priceQuoteRule[j].key;
    //       break;
    //     }
    //   }
    //   else {
    //     if (
    //       this.mdmData.priceQuoteRule[j].value ===
    //       this.curveGroup.controls[i].get('priceQuoteRule').value
    //     ) {
    //       z = this.mdmData.priceQuoteRule[j].key;
    //       break;
    //     }
    //   }
    // }

    // this.serviceOffset = [
    //   {
    //     serviceKey: 'offsetType',
    //     dependsOn: [z, y]
    //   },
    //   {
    //     serviceKey: 'offsetValue',
    //     dependsOn: ['offsetType-002']
    //   }
    // ];
    // this.configService.getMdmMeta('pricing_call', this.serviceOffset).subscribe((response:any) => {
    //   this.outputServiceOffset = response;
    // });
  }

  public showHideCurveDetails(event: Event): void {
    this.displayDropdown = true;
    // this.curveSelected = false;
  }
  //////////////// Date Validation And Format

  public datePickerAbove: IMyDpOptions = {
    dateFormat: 'dd-mm-yyyy',
    height: '28px',
    width: '230px',
    openSelectorTopOfInput: true
  };

  public deliveryFromDateOptions: IMyDpOptions = {
    dateFormat: 'dd-mm-yyyy',
    monthSelector: true,
    height: '28px',
    width: '230px'
  };
 

  deliveryToDateOptions: IMyDpOptions = {
    dateFormat: 'dd-mm-yyyy',
    height: '28px',
    width: '230px'
  };
  public defaultMonth: IMyDefaultMonth = {
    defMonth: '02/2017'
  };

  refreshDefaultMonth() {
    this.defaultMonth = {
      defMonth: '02/2017'
    };
  }

  onDateChanged(event: IMyDateModel) {
    let d: Date = event.jsdate;
    d.setDate(d.getDate() - 1);
    var date ={year: d.getFullYear(), month: d.getMonth() + 1, day: d.getDate()};
    this.disableUntil(date);
  }

  disableUntil(date) {
    let copy = this.getCopyOfOptions();
    copy.disableUntil = date;
    this.deliveryToDateOptions = copy;
  }
  getCopyOfOptions(): IMyDpOptions {
    return JSON.parse(JSON.stringify(this.deliveryToDateOptions));
  }

  // deliveryToDateOptions(deliveryToDateOptions: any): string {
  //   throw new Error("Method not implemented.");
  // }
  handleTaginputChange(event) {
    console.log(event);
  }
  removeFormulaArray(event) {
    for (let i = 0; i < this.expression.includedCurve.length; i++) {
      if (this.expression.includedCurve[i] === event) {
        this.expression.includedCurve.splice(i, 1);
      }
    }
    for (let i = 0; i < this.formulaArray.length; i++) {
      if (this.formulaArray[i] === event) {
        this.formulaArray.splice(i, 1);
      }
    }
  }
  addingToFormulaExp(event) {
    console.log(event);
  }
  editTag(event) {
    for (let i = 0; i < this.expression.includedCurve.length; i++) {
      if (event === this.expression.includedCurve[i]) return;
    }
  }
  splitPriceDiffrential(mdmdata) {
    var indexScurve = mdmdata.findIndex(x => x.value === "S-Curve")
    var swap = mdmdata[indexScurve]                         // swap
    mdmdata[indexScurve] = mdmdata[mdmdata.length - 1]
    mdmdata[mdmdata.length - 1] = swap
    this.ArrayForPriceDifferential.push(mdmdata.slice(0, 2))
    this.ArrayForPriceDifferential.push(mdmdata.slice(2, mdmdata.length))
  }
  onAdding(tag): Observable<string> {
    var flag = false
    if (tag.match('^([-+/*(){},])$') || tag.match('^[+-]?([0-9]+([.][0-9]*)?|[.][0-9]+)$')) {
      flag = true
    }
    if(tag.match('AVG') || tag.match('MIN') || tag.match('MAX')){
      return of(tag + '(')  
    }
    if (flag) {
      return of(tag);
    } else {
      return;
    }
  }
  
  AddAggregate(name){
   this.expression.curveExpression = this.expression.curveExpression + name;
   this.formulaArray = this.formulacreateform.value.expression;
   this.formulaArray.push(name+'(');
   this.formulacreateform.patchValue({
      expression: this.formulaArray
    });
  }

  callfxRate(value,i){
      var conv = this.conversionPrice[i].split('->')
      var convCorrect = conv[0]+'/'+conv[1]
      var conInv = conv[1]+'/'+conv[0]
      this.fxRateFromAgency = []

      for (let key in this.fxRateFromAgencyAll){
        if(convCorrect === this.fxRateFromAgencyAll[key][0]['Currency Pair'] || conInv === this.fxRateFromAgencyAll[key][0]['Currency Pair'])
        if(this.fxRateFromAgency.indexOf(key) === -1) {
          this.fxRateFromAgency.push(key);  
      }
      }
      return  this.fxRateFromAgency
  }
  
  AddComponent(name){
    this.expression.curveExpression = this.expression.curveExpression + name;
    this.formulaArray = this.formulacreateform.value.expression;
    this.formulaArray.push(name+'*');
    this.formulacreateform.patchValue({
       expression: this.formulaArray
     });
   }
   setpricediffrential(){
     return [ 
      {
          "differentialType" : "Discount",
          "diffLowerThreashold" : "",
          "differentialValue" : "",
          "diffUpperThreshold" : "",
          "differentialUnit" : ""
      }
  ]
   }

   quantityConversionFromSetup(fromQtyUnit,toQtyUnit,index){
     let productId = this.contractDetails.itemDetails[this.itemIndex].productId
     let fromQtyUnitId = ''
     let toQtyUnitId = ''
  
    let qtyPayload = [
      {
        serviceKey: 'physicalproductquantitylist',
        dependsOn:[productId]
      }
    ]
    var payload = [
      {
        serviceKey: 'quantityConversionFactor',
        dependsOn:[productId]
      }
    ]
    this.configService.getMdmMeta('pricing_call',qtyPayload).subscribe((qtyunit:any) => {
      for (var i = 0; i < qtyunit.physicalproductquantitylist.length; i++) {
        if (qtyunit.physicalproductquantitylist[i].value === fromQtyUnit) fromQtyUnitId = qtyunit.physicalproductquantitylist[i].key;
        if (qtyunit.physicalproductquantitylist[i].value === toQtyUnit) toQtyUnitId = qtyunit.physicalproductquantitylist[i].key;
      }
      if(fromQtyUnitId===''){
        this.popup.open(`Index Qty Unit, ${fromQtyUnit} is not available for this product`);
        return
      }
      if(toQtyUnitId===''){
        this.popup.open(`Contract Qty Unit, ${toQtyUnit} is not available for this product`);
        return
      }
      payload[0].dependsOn.push(fromQtyUnitId)
      payload[0].dependsOn.push(toQtyUnitId)
   if(this.contractDetails.itemDetails[0].hasOwnProperty('densityFactor') && this.contractDetails.itemDetails[0].densityFactor){
   this.configService.postApiWorkflow('density_conversion_pricing',{"densityValue":+(this.contractDetails.itemDetails[0].densityFactor),sourceUnitId:fromQtyUnitId,destinationUnitId:toQtyUnitId,productId:productId,massUnitId:this.contractDetails.itemDetails[0].densityMassQtyUnitId,volumeUnitId:this.contractDetails.itemDetails[0].densityVolumeQtyUnitId}).subscribe(
        (conversiondata:any) => {
          if(conversiondata.data.msg!='Success')
          this.configService.getMdmMeta('pricing_call',payload).subscribe((response:any) => {
            this.curveGroup.value[index]['qtyUnitConversionFactor'] = Number(response.quantityConversionFactor[0].value);
            this.curveGroup.patchValue(this.curveGroup.value);
          })
          else {
            this.curveGroup.value[index]['qtyUnitConversionFactor'] = +(conversiondata.data.conversionFactor)
            this.curveGroup.patchValue(this.curveGroup.value);
          }
        })
      }
    else{  
    this.configService.getMdmMeta('pricing_call',payload).subscribe((response:any) => {
      this.curveGroup.value[index]['qtyUnitConversionFactor'] = Number(response.quantityConversionFactor[0].value);
      this.curveGroup.patchValue(this.curveGroup.value);
    })
  }
  })
  
   }

   getpriceUnitValue(id){
     for(let i=0;i<this.mdmPriceUnit.length;i++){
       if(id === this.mdmPriceUnit[i].key) return this.mdmPriceUnit[i].value
     }
   }
   getPlaceholder(placeholder){
     if(placeholder === 'Day')return 'DD-D-DD'
     else if (placeholder === 'Month') return 'MM-M-MM'
     else if(placeholder==='Week') return 'W-W-W'
     else return ''
   }
   getLenth(type){
    if(type === 'Day')return '7'
    else if (type === 'Month') return '7'
    else if(type==='Week') return '5'
    else return '1'
  }
   resetoffset(i){
    this.curveGroup.controls[i].get('offset').reset()
   }
  getmin(i){
        return this.curveGroup.controls[i].get('pricingPeriod').value
  }
  getmax(i){
    if(this.curveGroup.controls[i].get('priceQuoteRule').value !== 'Settlement Date')
    return this.curveGroup.controls[i].get('quotedPeriodDate').value

    else{ 
     var FromNow = new Date();
    FromNow.setFullYear(FromNow.getFullYear() + 50);
    return FromNow
    }
  }
  callvalidation(key1,i){
    if(key1==='isActualQuoted')
    this.curveGroup.controls[i].get('isActualPricing').reset()
    else if(key1==='isActualPricing')
    this.curveGroup.controls[i].get('isActualQuoted').reset()
  }
  upadatePriceQuoteRule(index,pricePointValue){
    var mdm = this.mdmData.priceQuoteRule
    if(!this.conditionalpriceQuoteRule[index])
    this.conditionalpriceQuoteRule.push([])
  if(pricePointValue !=='Forward' && !this.valuation){
  this.conditionalpriceQuoteRule[index] = []
    for(let i=0;i<mdm.length;i++){
      if(mdm[i].value !=='Prompt Period Avg'&& mdm[i].value !=='Settlement Date'){
        this.conditionalpriceQuoteRule[index].push(mdm[i])
      }
    }
  }
  else if(pricePointValue !=='Forward' && this.valuation){
    this.conditionalpriceQuoteRule[index] = []
    for(let i=0;i<mdm.length;i++){
      if(mdm[i].value !=='Prompt Period Avg'&& mdm[i].value !=='Settlement Date'&& mdm[i].value !=='Event Offset Based'){
        this.conditionalpriceQuoteRule[index].push(mdm[i])
      }
    }
  }
  else if(pricePointValue =='Forward' && this.valuation){
    this.conditionalpriceQuoteRule[index] = []
    for(let i=0;i<mdm.length;i++){
      if( mdm[i].value !=='Settlement Date'&& mdm[i].value !=='Event Offset Based'){
        this.conditionalpriceQuoteRule[index].push(mdm[i])
      }
    }
  }
  else if(pricePointValue =='Forward' && !this.valuation){
    this.conditionalpriceQuoteRule[index] = []
    for(let i=0;i<mdm.length;i++){
      if( mdm[i].value !=='Lookback Pricing'){
        this.conditionalpriceQuoteRule[index].push(mdm[i])
      }
    }
  }
  else this.conditionalpriceQuoteRule[index] = this.mdmData.priceQuoteRule
  }
  validateOffset(chIbn,type = 'Day',priceQuoteRule = 'NC'){            // not compulsory
    let string = ''
    chIbn = chIbn.split('')
    if (type === 'Day'){
    for(let j=0;j<chIbn.length;j++){
      if(j==2 || j==3){
        if(j ==2 && Number(chIbn[j]) <= 1){
        string = string + '-' + chIbn[j]
        }
        else if(j ==3 && Number(chIbn[j]) <= 2){
          string = string + '-' + chIbn[j]
        }
      }
      else if((j==0)) {
        if(Number(chIbn[j]) <= 2)
        string = string + chIbn[j]
      }
      else {
        string = string + chIbn[j]
      }
    }
  }
  else if(type === 'Month'&& priceQuoteRule==='NC'){
    for(let j=0;j<chIbn.length;j++){
      if(j==2 || j==3){
        if(j ==2 && Number(chIbn[j]) <= 1){
        string = string + '-' + chIbn[j]
        }
        else if(j ==3 && Number(chIbn[j]) <= 1){
          string = string + '-' + chIbn[j]
        }
      }
      else if((j==0)) {
        if(Number(chIbn[j]) <= 1)
        string = string + chIbn[j]
      }
      else {
        if(Number(chIbn[j]) <= 2 && Number(chIbn[j-1])=== 1)
        string = string + chIbn[j]
        else if(Number(chIbn[j-1])===0 && Number(chIbn[j]) <= 9)
        string = string + chIbn[j]
      }
    }
  }
  else if(type === 'Month' && priceQuoteRule!=='NC'){
    for(let j=0;j<chIbn.length;j++){
      if(j==2 || j==3){
        if(j ==2 && Number(chIbn[j]) <= 1){
        string = string+ '-' + chIbn[j]
        }
        else if(j ==3 &&Number(chIbn[j-1]) == 1 &&  Number(chIbn[j]) <= 2){
          string = string + chIbn[j]
        }
        else if(j ==3 &&Number(chIbn[j-1]) == 0 &&  Number(chIbn[j]) <= 9){
          string = string  + chIbn[j]
        }
      }
      else if((j==0 )) {
        if(Number(chIbn[j]) <= 1)
        string = string + chIbn[j]
      }
      else if(( j==4)) {
        if(Number(chIbn[j]) <= 1)
        string = string + '-'+ chIbn[j]
      }
      else {
        if(Number(chIbn[j]) <= 2 && Number(chIbn[j-1])=== 1)
        string = string + chIbn[j]
        else if(Number(chIbn[j-1])===0 && Number(chIbn[j]) <= 9)
        string = string + chIbn[j]
      }
    }
  }
  else if(type ==='Week'){
    for(let j=0;j<chIbn.length;j++){
      if(j==1 || j==2){
        if(j ==1 && Number(chIbn[j]) <= 1){
        string = string + '-' + chIbn[j]
        }
        else if(j ==2 && Number(chIbn[j]) <= 5){
          string = string + '-' + chIbn[j]
        }
      }
      else if((j==0)) {
        if(Number(chIbn[j]) <= 5)
        string = string + chIbn[j]
      }
      else {
        string = string + chIbn[j]
      }
    }
  }
    return string
  }
  resetFormForValution(priceQuoteRule,i){
    if(this.valuation){
      if ((priceQuoteRule === 'Event Offset Based'|| priceQuoteRule === 'Settlement Date')&& this.curveGroup)this.curveGroup.at(i).reset()
      if(priceQuoteRule === 'Price Period Average'&& this.curveGroup && this.curveGroup.controls[i].get('pricePoint').value !== 'Forward')this.curveGroup.at(i).reset()
    }
  }
  setCurrency(currencyCode,contract=true,i=-1,j=-1){
    this.configService.getApiWorkflow('currencyConversion',false,{"currencyCode": currencyCode}).subscribe((currency: any) => { 
      if(currency.data.isSubCurrency ==='Y'){
         this.subCurrency[currencyCode] = {
           'isSubCurrency':'Y',
           'parent':currency.data.currencyCode,
           'conversionFactor' : currency.data.conversionFactor
         },
         this.subCurrency[currency.data.currencyCode] = {
          'isSubCurrency':'N',
          'parent':'NA'
        }
      }
      else{
        this.subCurrency[currencyCode] = {
          'isSubCurrency':'N',
          'parent':'NA'
        }
      }
      if(contract && this.subCurrency[currencyCode].isSubCurrency==='Y'){
        this.currencyContractUnit = this.subCurrency[currencyCode].parent +'/'+this.currencyContractUnit.split('/')[1]
        this.fxCheck()
        
      }
      if(!contract && i!=-1){
        if(this.fxDifferent[i])
        this.fxDifferent[i] = true;
        else this.fxDifferent.push(true)
        if(this.subCurrency[currencyCode].isSubCurrency==='N')
        this.conversionPrice[i] =this.curveBaseData.data[j].map.priceUnit.split('/')[0] +'->' +this.currencyContractUnit.split('/')[0];
        if(this.subCurrency[currencyCode].isSubCurrency==='Y'&& this.subCurrency[currencyCode].parent!==this.currencyContractUnit.split('/')[0])
        this.conversionPrice[i] = this.subCurrency[currencyCode].parent+'->' +this.currencyContractUnit.split('/')[0];
        else if(this.subCurrency[currencyCode].isSubCurrency==='Y'&& this.subCurrency[currencyCode].parent===this.currencyContractUnit.split('/')[0]) 
        {this.fxDifferent[i]=false; this.conversionPrice[i] = '';  if (this.curveGroup) this.curveGroup.value[i]['fxInput'] = 1;}
      }
  })
  }
  updateQuaotedValue(i){
    if(this.curveGroup.controls[i].get('priceQuoteRule').value === 'Settlement Date')
    this.curveGroup.controls[i].get('quotedPeriodDate').setValue(this.curveGroup.controls[i].get('pricingPeriod').value);
  }
  disablecompeleteform(){
    if(this.disableFormulaModification && this.curveGroup && this.curveGroup.controls){
    for(let key in this.curveGroup.controls){
      this.curveGroup.controls[key].controls['priceType'].disable({onlySelf: true});
      this.curveGroup.controls[key].controls['pricePoint'].disable({onlySelf: true});
      this.curveGroup.controls[key].controls['differentialUnit'].disable({onlySelf: true});
      this.curveGroup.controls[key].controls['qtyUnitConversionFactor'].disable({onlySelf: true});
      this.curveGroup.controls[key].controls['differential'].disable({onlySelf: true});
      this.curveGroup.controls[key].controls['indexPrecision'].disable({onlySelf: true});
      this.curveGroup.controls[key].controls['fxCurve'].disable({onlySelf: true});
      this.curveGroup.controls[key].controls['fxInput'].disable({onlySelf: true});
      this.curveGroup.controls[key].controls['fxType'].disable({onlySelf: true});
      this.curveGroup.controls[key].controls['offset'].disable({onlySelf: true});
      this.curveGroup.controls[key].controls['offsetType'].disable({onlySelf: true});
      this.curveGroup.controls[key].controls['event'].disable({onlySelf: true});
      this.curveGroup.controls[key].controls['endDate'].disable({onlySelf: true});
      this.curveGroup.controls[key].controls['startDate'].disable({onlySelf: true});
      this.curveGroup.controls[key].controls['period'].disable({onlySelf: true});
      this.curveGroup.controls[key].controls['pricePoint'].disable({onlySelf: true});
      this.curveGroup.controls[key].controls['isActualQuoted'].disable({onlySelf: true});
      this.curveGroup.controls[key].controls['isActualPricing'].disable({onlySelf: true});
      this.curveGroup.controls[key].controls['offsetDays'].disable({onlySelf: true});
      this.curveGroup.controls[key].controls['monthDefinition'].disable({onlySelf: true});
      this.curveGroup.controls[key].controls['pricingPeriod'].disable({onlySelf: true});
      this.curveGroup.controls[key].controls['quotedPeriod'].disable({onlySelf: true});
      this.curveGroup.controls[key].controls['priceQuoteRule'].disable({onlySelf: true});
    }
    
    }
  }
  getCurveUnit(index){
     return this.curveBaseData.data.find(item=>item.map.curveName === this.expression.includedCurve[index]).map.priceUnit
  }
  getFxInput(){  return this.curveGroup.get('fxInput') as FormArray }
}
