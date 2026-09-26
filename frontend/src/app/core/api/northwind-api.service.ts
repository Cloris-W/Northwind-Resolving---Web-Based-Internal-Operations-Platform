import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { BillingHistoryResponse, CaseContext, CaseListResponse, CaseEvent, MeterReadingsResponse } from './northwind-api.models';

@Injectable({ providedIn: 'root' })
export class NorthwindApiService {
  constructor(private readonly http: HttpClient) {}
  searchByAccount(accountId: string) { return this.http.get<CaseListResponse>('/api/cases', { params: new HttpParams().set('accountId', accountId) }); }
  caseContext(caseId: string) { return this.http.get<CaseContext>(`/api/cases/${encodeURIComponent(caseId)}`); }
  timeline(caseId: string) { return this.http.get<{ caseId: string; events: CaseEvent[] }>(`/api/cases/${encodeURIComponent(caseId)}/timeline`); }
  meterReadings(accountId: string) { return this.http.get<MeterReadingsResponse>(`/api/accounts/${encodeURIComponent(accountId)}/meter-readings`); }
  billingHistory(accountId: string) { return this.http.get<BillingHistoryResponse>(`/api/accounts/${encodeURIComponent(accountId)}/billing`); }
}
