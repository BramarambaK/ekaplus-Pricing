import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { TriggerPricing } from './trigger-pricing.component';

describe('TriggerPricing', () => {
  let component: TriggerPricing;
  let fixture: ComponentFixture<TriggerPricing>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [TriggerPricing]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TriggerPricing);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
