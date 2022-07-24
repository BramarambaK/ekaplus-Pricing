import { NgModule, Component } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { FormulaFormComponent } from './formula-form/formula-form.component';

import {
  ConfigService,
  CtrmLayoutComponent,
  DefaultLayoutComponent
} from '@eka-framework/layout';
import { AuthenticationGuard } from '@eka-framework/core';
import { FormulaListComponent } from './formula-list/formula-list.component';
import { TriggerPricing } from './trigger-pricing/trigger-pricing.component';
import { EnvService } from '@eka-framework/layout/env.service';

const routes: Routes = [
  {
    path: 'pricing',
    component: CtrmLayoutComponent,
    resolve: { data: ConfigService },
    canActivate: [AuthenticationGuard],
    data: {
      title: 'Pricing'
    },
    children: [
      {
        path: 'formula/triggerprice',
        component: TriggerPricing
      },
      {
        path: 'formula/view',
        component: FormulaListComponent
      },
      {
        path: 'formula/:action',
        component: FormulaFormComponent
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class PricingRoutingModule {}
