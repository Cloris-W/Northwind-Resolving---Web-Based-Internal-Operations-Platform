import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { BillingQualityComponent } from './billing-quality.component';
import { NorthwindApiService } from '../../core/api/northwind-api.service';

describe('BillingQualityComponent', () => {
  let fixture: ComponentFixture<BillingQualityComponent>;
  let fail = false;
  let pending = false;
  let calls: Record<string, string>[] = [];
  let items: unknown[] = [];
  const api = {
    billingExceptions: (params: Record<string, string>) => {
      calls.push(params);
      if (pending) return { subscribe: () => ({}) } as never;
      if (fail) return throwError(() => new Error('test error'));
      return of({ items, page: { page: 0, size: 25, totalElements: items.length, totalPages: 1 } });
    },
  };
  beforeEach(async () => {
    fail = false;
    pending = false;
    calls = [];
    items = [];
    await TestBed.configureTestingModule({ imports: [BillingQualityComponent], providers: [provideZonelessChangeDetection(), provideRouter([]), { provide: NorthwindApiService, useValue: api }] }).compileComponents();
    fixture = TestBed.createComponent(BillingQualityComponent);
  });
  it('renders the Billing Quality Monitor and empty queue state', () => { fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('Billing Quality Monitor'); expect(fixture.nativeElement.textContent).toContain('No billing exceptions found.'); });
  it('shows a loading state while the queue request is pending', () => { pending = true; fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('Loading…'); });
  it('renders successful test-only queue entries and detail navigation', () => { items = [{ id: 'test-exception-id', accountId: 'test-account', caseId: null, riskScore: 60, riskLevel: 'HIGH', reasonCodes: ['TEST'], status: 'OPEN', region: 'Test Region', createdAt: '2026-01-01T00:00:00Z', reviewedBy: null }]; fixture.detectChanges(); const link = fixture.nativeElement.querySelector('a[href="/billing/exceptions/test-exception-id"]'); expect(link).toBeTruthy(); expect(fixture.nativeElement.textContent).toContain('HIGH 60'); });
  it('sends risk, status, and region filters to the API', () => { fixture.detectChanges(); fixture.componentInstance.riskLevel = 'HIGH'; fixture.componentInstance.status = 'OPEN'; fixture.componentInstance.region = 'Test Region'; fixture.componentInstance.load(); expect(calls.at(-1)).toEqual({ riskLevel: 'HIGH', status: 'OPEN', region: 'Test Region' }); });
  it('shows the API error state', () => { fail = true; fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('Billing exceptions could not be loaded.'); });
});
