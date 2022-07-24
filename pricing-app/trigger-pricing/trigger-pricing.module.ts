import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { PricingRoutingModule } from '../pricing-routing.module';
import { Ng2SearchPipeModule } from 'ng2-search-filter';
import { BrowserModule } from '@angular/platform-browser';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { HttpClientModule } from '@angular/common/http';
import { EkaCommonModule } from '@eka-framework/modules/common';
import { PipeModule } from '../pipe/pipe.module';

import { TableModule } from 'primeng/table';
import { MultiSelectModule } from 'primeng/primeng';
import { DropdownModule } from 'primeng/primeng';
import { TriggerPricing } from './trigger-pricing.component';
import { MyDatePickerModule } from 'mydatepicker';
import { FormulaListModule } from '../formula-list/formula-list.module';

@NgModule({
  declarations: [TriggerPricing],
  imports: [
    CommonModule,
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    Ng2SearchPipeModule,
    EkaCommonModule,
    PricingRoutingModule,

    NgbModule,
    FormsModule,
    TableModule,
    MultiSelectModule,
    DropdownModule,
    ReactiveFormsModule,
    HttpClientModule,
    Ng2SearchPipeModule,
    PipeModule,
    MyDatePickerModule,
    FormulaListModule
  ],
  exports: [TriggerPricing]
})
export class TriggerPricingModule {}
