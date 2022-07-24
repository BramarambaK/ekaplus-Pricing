import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OrderBy } from './order.pipe';
import { ReversePipe } from './reverse.pipe';
import { SearchPipe } from './search.pipe';

@NgModule({
  declarations: [OrderBy, ReversePipe, SearchPipe],
  imports: [
    CommonModule
  ],
  exports: [OrderBy, ReversePipe, SearchPipe]
})
export class PipeModule { }
