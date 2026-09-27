import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { AiCaseRecommendation, AiCaseSummary, BillingHistoryResponse, BillingException, BillingExceptionPage, BillingReviewRequest, CaseContext, CaseListResponse, CaseEvent, FieldVisit, FieldVisitRequest, MeterReadingsResponse, TransferCaseRequest, TransferCaseResponse } from './northwind-api.models';

@Injectable({ providedIn: 'root' })
export class NorthwindApiService {
  constructor(private readonly http: HttpClient) {}
  searchByAccount(accountId: string) { return this.http.get<CaseListResponse>('/api/cases', { params: new HttpParams().set('accountId', accountId) }); }
  caseContext(caseId: string) { return this.http.get<CaseContext>(`/api/cases/${encodeURIComponent(caseId)}`); }
  timeline(caseId: string) { return this.http.get<{ caseId: string; events: CaseEvent[] }>(`/api/cases/${encodeURIComponent(caseId)}/timeline`); }
  meterReadings(accountId: string) { return this.http.get<MeterReadingsResponse>(`/api/accounts/${encodeURIComponent(accountId)}/meter-readings`); }
  billingHistory(accountId: string) { return this.http.get<BillingHistoryResponse>(`/api/accounts/${encodeURIComponent(accountId)}/billing`); }
  transferCase(caseId: string, request: TransferCaseRequest, idempotencyKey: string) { return this.http.post<TransferCaseResponse>(`/api/cases/${encodeURIComponent(caseId)}/transfer`, request, { headers: this.idempotencyHeader(idempotencyKey) }); }
  requestFieldVisit(caseId: string, request: FieldVisitRequest, idempotencyKey: string) { return this.http.post<FieldVisit>(`/api/cases/${encodeURIComponent(caseId)}/field-visit`, request, { headers: this.idempotencyHeader(idempotencyKey) }); }
  private idempotencyHeader(idempotencyKey: string) { return new HttpHeaders().set('Idempotency-Key', idempotencyKey); }
  billingExceptions(params: Record<string,string>) { return this.http.get<BillingExceptionPage>('/api/billing/exceptions',{params:new HttpParams({fromObject:params})}); }
  billingException(id:string) { return this.http.get<BillingException>(`/api/billing/exceptions/${encodeURIComponent(id)}`); }
  reviewBillingException(id:string,request:BillingReviewRequest,key:string) { return this.http.post(`/api/billing/exceptions/${encodeURIComponent(id)}/review`,request,{headers:this.idempotencyHeader(key)}); }
  aiSummary(caseId: string) { return this.http.post<AiCaseSummary>(`/api/ai/cases/${encodeURIComponent(caseId)}/summary`, null); }
  aiRecommendation(caseId: string) { return this.http.post<AiCaseRecommendation>(`/api/ai/cases/${encodeURIComponent(caseId)}/recommendation`, null); }
}
