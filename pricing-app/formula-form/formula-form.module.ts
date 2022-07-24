import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormulaFormComponent } from './formula-form.component';

import { ModalComponent } from './components/modal/modal.component';
import { NgbdModalConfig } from './components/side-preview/side-preview.component';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { Ng2SearchPipeModule } from 'ng2-search-filter';
import { HttpClientModule } from '@angular/common/http';
import { PricingRoutingModule } from '../pricing-routing.module';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { FormulaListModule } from '../formula-list/formula-list.module';
import { EkaCommonModule } from '@eka-framework/modules/common';
import { TableModule } from 'primeng/table';
import { MultiSelectModule } from 'primeng/primeng';
import { DropdownModule } from 'primeng/primeng';
import { PipeModule } from '../pipe/pipe.module';
import { MyDatePickerModule } from 'mydatepicker';
import { CalendarModule } from 'primeng/primeng';
import { AutoSizeInputModule } from 'ngx-autosize-input';
import { TagInputModule } from 'ngx-chips';

@NgModule({
  declarations: [FormulaFormComponent, ModalComponent, NgbdModalConfig],
  imports: [
    CommonModule,
    TagInputModule,
    ReactiveFormsModule,
    FormsModule,
    Ng2SearchPipeModule,
    EkaCommonModule,
    PricingRoutingModule,
    NgbModule,
    AutoSizeInputModule,
    FormsModule,
    TableModule,
    MultiSelectModule,
    DropdownModule,
    ReactiveFormsModule,
    HttpClientModule,
    Ng2SearchPipeModule,
    PipeModule,
    MyDatePickerModule,
    CalendarModule,

    FormulaListModule
  ],
  providers: [],
  exports: [FormulaFormComponent, ModalComponent, NgbdModalConfig]
})
export class FormulaFormModule {}
