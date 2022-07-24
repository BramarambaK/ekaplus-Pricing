import { TestBed } from '@angular/core/testing';

import { PricingService } from './config.service';

describe('ConfigService', () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  it('should be created', () => {
    const service: PricingService = TestBed.get(PricingService);
    expect(service).toBeTruthy();
  });
});
