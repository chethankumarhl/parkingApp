import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Capture } from './capture';

describe('Capture', () => {
  let component: Capture;
  let fixture: ComponentFixture<Capture>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Capture],
    }).compileComponents();

    fixture = TestBed.createComponent(Capture);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
