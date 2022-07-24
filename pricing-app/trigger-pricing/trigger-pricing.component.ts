import {
  Component,
  OnInit,
  NgZone,
  HostListener,
  Input,
  ViewChild
} from '@angular/core';

import { ActivatedRoute, Router } from '@angular/router';
import { PricingService } from '../config.service';

import { FormControl, FormBuilder, Validators } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { UtilService } from '../utility.service';
import * as moment from 'moment';
import { IMyDpOptions } from 'mydatepicker';

@Component({
  selector: 'triggerPrice',
  templateUrl: './trigger-pricing.component.html',
  styleUrls: ['./trigger-pricing.component.scss']
})
export class TriggerPricing implements OnInit {
  formulaDetails: any;
  color = ['#FF0000', '#00FFFF', '#FFD700', '#008000', '#FFFF00'];
  formulaDetailsShownEnabled = new FormControl();
  triggerPriceForm: any;
  dateToday = new Date();
  metaData: any;
  newDate: string;
  heading
  @ViewChild('save') popup;

  FormulaId: any;
  urlParams;
  qtyUnit: any;
  priceUnit: any;
  mdmResponse: any;
  totalQty: any;
  left: any;
  errLeft: boolean = false;
  SameUnit: boolean = false;

  constructor(
    private service: PricingService,
    private fb: FormBuilder,
    private util: UtilService
  ) { }

  ngOnInit() {
    this.urlParams = new URLSearchParams(window.location.search);
    this.triggerPriceForm = this.fb.group({
      triggerDate: ['',[Validators.required]],
      quantity: [''],
      price: [''],
      priceU: [''],
      fxrate: ['']
    });

    let date = new Date();
    this.triggerPriceForm.controls['triggerDate'].setValue({
      date: {
        year: date.getFullYear(),
        month: date.getMonth() + 1,
        day: date.getDate()
      }
    });

    this.service
      .getApiWorkflowForContract('contract_list',this.urlParams.get('internalContractRefNo'))
      .subscribe(
        (resul: any) => {
          var item = resul.data[0].itemDetails;
          this.heading = resul.data[0].contractRefNo
          for (var i = 0; i < item.length; i++) {
            if (
              item[i].internalItemRefNo ===
              this.urlParams.get('internalContractItemRefNo')
            ) {
              this.FormulaId = item[i].pricing.pricingFormulaId;
              this.heading = this.heading + ' ItemNo: ' + item[i].itemNo
              this.qtyUnit = item[i].itemQtyUnitId
              this.totalQty = item[i].itemQty

              this.priceUnit = item[i].pricing.priceUnitId
              var mdmdata = [
                {
                  serviceKey: 'productPriceUnit',
                  dependsOn: [item[i].productId, item[i].payInCurId] // mdm for offset depends on two fields(mand) ,so "null" has to pass for that
                },
                {
                  serviceKey: 'physicalproductquantitylist',
                  dependsOn: [item[i].productId] // mdm for offset depends on two fields(mand) ,so "null" has to pass for that
                }
              ];
            }
          }


          this.service.getApiWorkflow('formula_list',this.FormulaId).subscribe((res:any) => {
            this.formulaDetails = res.data[0];

            this.left = this.totalLeft()
            this.service.getMdmMeta('pricing_call',mdmdata).subscribe((response:any) => {
              this.mdmResponse = response
              console.log(this.mdmResponse)
              this.qtyUnit = this.ValueFromId(this.mdmResponse.physicalproductquantitylist, this.qtyUnit)
              this.priceUnit = this.ValueFromId(this.mdmResponse.productPriceUnit, this.priceUnit)

            });
          });

        },
        err => {
          var message;
          if (err.error.description) message = err.error.description;
          else message = err.error.message;
          // this.popup.open(message);
        }
      );

    this.service.getConfigMeta().subscribe(res => {
      this.metaData = res;
    });

    this.triggerPriceForm.get('priceU').valueChanges.subscribe(() => {
      if (this.priceUnit === this.triggerPriceForm.get('priceU').value) {
        this.triggerPriceForm.controls['fxrate'].value = 1
        this.SameUnit = true
      }
      else {
        this.SameUnit = false
      }
    })


  }
  addTriggerPrice() {
    // var datePipe = new DatePipe('en-US');
    // this.newDate = datePipe.transform(this.dateToday, 'dd/MM/yyyy');
    // this.newDate = this.util.getISO(this.dateToday);
    // this.newDate = moment().format('YYYY-MM-DDT00:00:00.000+0000');
    // if(this.triggerPriceForm.invalid){
    //   this.popup.open('Please Provide Date');
    //   return
    // }
    this.triggerPriceForm.value['triggerDate'] = this.util.getISO(this.triggerPriceForm.value['triggerDate']);
    if (this.formulaDetails.triggerPricing) {
      var left = this.totalLeft()
      if (left - this.triggerPriceForm.value['quantity'] < 0) {
        // this.popup.open('Priced Quantity not Greater Than Total Quantity')
        this.errLeft = true
        return
      }
    }
    this.formulaDetails.triggerPricing.unshift(this.triggerPriceForm.value);

    this.service.putApiWorkflow('formula_edit',this.formulaDetails, this.FormulaId).subscribe();
    this.triggerPriceForm.reset();
    window.location.href = window.location.origin + '/trm/#gridId/LOCI';
  }

  reset() {
    window.location.href = window.location.origin + '/trm/#gridId/LOCI';
  }

  ValueFromId(mdmres, id) {
    if (mdmres === 'undefined') {
      return id
    }
    for (var i = 0; i < mdmres.length; i++) {
      if (mdmres[i].key === id) {
        return mdmres[i].value

      }
    }
  }

  public myDatePickerOptions: IMyDpOptions = {
    dateFormat: 'dd-mm-yyyy',
    height: '25px'
  };

  totalLeft() {
    var left = this.totalQty;
    for (let i = 0; i < this.formulaDetails.triggerPricing.length; i++) {
      left = left - this.formulaDetails.triggerPricing[i].quantity
    }
    return left
  }

}
