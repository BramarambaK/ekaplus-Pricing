import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { NgbdModalConfig } from './side-preview.component';

describe('NgbdModalConfig', () => {
  let component: NgbdModalConfig;
  let fixture: ComponentFixture<NgbdModalConfig>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ NgbdModalConfig ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(NgbdModalConfig);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
