import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ReactiveFormsModule } from '@angular/forms';
import { PricingRoutingModule } from './pricing-routing.module';
import { NgbModule, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { HttpClientModule } from '@angular/common/http';
import { Ng2SearchPipeModule } from 'ng2-search-filter';
import { FormulaListModule } from './formula-list/formula-list.module';
import { FormulaFormModule } from './formula-form/formula-form.module';
import { EkaCommonModule } from '@eka-framework/modules/common';
import { PipeModule } from './pipe/pipe.module';
import { pricingComponent } from './pricing.component';
import { MyDatePickerModule } from 'mydatepicker';
import { TriggerPricingModule } from './trigger-pricing/trigger-pricing.module';
import { ConfirmPopupComponent } from './confirm-popup/comfirm-popup.component';

@NgModule({
  declarations: [pricingComponent],
  imports: [
    BrowserModule,
    PricingRoutingModule,
    NgbModule,
    FormsModule,
    EkaCommonModule,
    ReactiveFormsModule,
    HttpClientModule,
    Ng2SearchPipeModule,
    FormulaFormModule,
    FormulaListModule,
    PipeModule,
    TriggerPricingModule
  ],
  providers: [HttpClientModule, NgbModal],
  bootstrap: [],
  exports: [pricingComponent]
})
export class PricingModule {}
