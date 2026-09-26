import { provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { CaseSearchComponent } from './case-search.component';
import { NorthwindApiService } from '../../core/api/northwind-api.service';

describe('CaseSearchComponent', () => {
  let fixture: ComponentFixture<CaseSearchComponent>;
  const api = { searchByAccount: (accountId: string) => of({ items: accountId === 'ACC-943644' ? [{ caseId: 'NW-100001', status: 'CLOSED' }] : [], page: { page: 0, size: 25, totalElements: 0, totalPages: 0 } }) };
  beforeEach(async () => { await TestBed.configureTestingModule({ imports: [CaseSearchComponent], providers: [provideZonelessChangeDetection(), provideRouter([]), { provide: NorthwindApiService, useValue: api }] }).compileComponents(); fixture = TestBed.createComponent(CaseSearchComponent); });
  it('searches an account and renders returned canonical cases', () => { fixture.componentInstance.accountId = 'ACC-943644'; fixture.componentInstance.search(); fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('NW-100001'); });
  it('shows an empty state when no cases match', () => { fixture.componentInstance.accountId = 'none'; fixture.componentInstance.search(); fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('No matching cases'); });
});
