import { Component, Input, SimpleChange, OnInit } from '@angular/core';
import { PricingService } from './config.service';

@Component({
  selector: 'pricing',
  templateUrl: './pricing.component.html',
  styleUrls: []
})
export class pricingComponent implements OnInit {
  @Input() fromPricing = true;
  @Input() indexPricing = false;
  @Input() contractId = '123456';
  @Input() outerModal;
  @Input() contractDetails;
  @Input() valuation;
  @Input() fromContractPopup;
  @Input() editFormulaId;
  @Input() DataForPricing;
  @Input() itemId;
  @Input() itemIndex;
  @Input() mdmPriceUnit;
  @Input() formulaId;
  @Input() disableFormulaModification = false;
  @Input() fromViewContractPage = false;

  currentComponent;

  ngOnChanges(changes) {
    console.log(this.itemIndex);

    console.log(changes);
    if (changes.outerModal) {
      this.outerModal = changes.outerModal.currentValue;
    }

    if(changes.fromViewContractPage){
      this.fromViewContractPage = changes.fromViewContractPage.currentValue;
    }
  }
  constructor(private service: PricingService) {
    this.service.fromPricing = this.fromPricing;
    this.service.currentComponent.subscribe(data => {
      this.currentComponent = data;
    });
  }
  ngOnInit() {
    this.service.outerModal = this.outerModal;
  }
}
