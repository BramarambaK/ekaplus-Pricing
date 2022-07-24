import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormulaListComponent } from './formula-list.component';

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
import { ConfirmPopupComponent } from '../confirm-popup/comfirm-popup.component';

@NgModule({
  declarations: [FormulaListComponent, ConfirmPopupComponent],
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
    PipeModule
  ],
  exports: [FormulaListComponent, ConfirmPopupComponent]
})
export class FormulaListModule {}
