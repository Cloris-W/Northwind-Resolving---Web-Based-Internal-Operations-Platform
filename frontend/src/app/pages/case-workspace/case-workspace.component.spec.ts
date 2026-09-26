import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { CaseWorkspaceComponent } from './case-workspace.component';
import { NorthwindApiService } from '../../core/api/northwind-api.service';

describe('CaseWorkspaceComponent', () => {
  let fixture: ComponentFixture<CaseWorkspaceComponent>;
  const api = { caseContext: () => of({ case: { caseId: 'NW-100001', accountId: 'ACC-943644', category: 'BILLING', priority: 'MEDIUM', region: 'Ashford', status: 'CLOSED', slaDays: 20, openedAt: '2024-10-01T00:00:00Z', closedAt: null, assignedTeam: 'UNASSIGNED_IMPORT' }, latestBillingException: null, latestMeterReading: null, fieldVisits: [] }), timeline: () => of({ caseId: 'NW-100001', events: [{ eventId: '1', caseId: 'NW-100001', eventType: 'COMPLAINT_CREATED', sourceSystem: 'CASETRACK', timestamp: '2024-10-01T00:00:00Z', actor: 'CSV_IMPORT', description: 'Imported complaint record' }] }), billingHistory: () => of({ accountId: 'ACC-943644', bills: [], corrections: [] }), meterReadings: () => of({ accountId: 'ACC-943644', readings: [] }) };
  beforeEach(async () => { await TestBed.configureTestingModule({ imports: [CaseWorkspaceComponent], providers: [provideZonelessChangeDetection(), provideRouter([]), { provide: ActivatedRoute, useValue: { snapshot: { paramMap: new Map([['caseId', 'NW-100001']]) } } }, { provide: NorthwindApiService, useValue: api }] }).compileComponents(); fixture = TestBed.createComponent(CaseWorkspaceComponent); });
  it('renders case data and truthful empty source-data sections', () => { fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('NW-100001'); expect(fixture.nativeElement.textContent).toContain('No account-level meter readings'); expect(fixture.nativeElement.textContent).toContain('No FieldForce visits'); });
});
