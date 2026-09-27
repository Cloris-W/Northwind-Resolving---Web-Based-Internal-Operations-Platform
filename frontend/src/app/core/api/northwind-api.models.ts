export interface CaseDto { caseId: string; accountId: string; category: string; priority: string; region: string; status: string; slaDays: number; openedAt: string; closedAt: string | null; assignedTeam: string; }
export interface CaseEvent { eventId: string; caseId: string; eventType: string; sourceSystem: string; timestamp: string; actor: string; description: string; metadata?: Record<string, unknown>; }
export interface CaseContext { case: CaseDto; latestBillingException: unknown | null; latestMeterReading: unknown | null; fieldVisits: FieldVisit[]; }
export interface FieldVisit { id: string; caseId: string; status: string; scheduledAt: string; completedAt: string | null; outcome: string | null; }
export interface CaseListResponse { items: CaseDto[]; page: { page: number; size: number; totalElements: number; totalPages: number }; }
export interface MeterReadingsResponse { accountId: string; readings: unknown[]; }
export interface BillingHistoryResponse { accountId: string; bills: unknown[]; corrections: unknown[]; }
export interface TransferCaseRequest { assignedTeam: string; reason?: string; }
export interface TransferCaseResponse { case: CaseDto; event: CaseEvent; }
export interface FieldVisitRequest { requestedFor: string; visitReason: string; meterId?: string; instructions?: string; }
export interface BillingException { id:string; accountId:string; caseId:string|null; riskScore:number; riskLevel:string; reasonCodes:string[]; status:string; region:string; createdAt:string; reviewedBy:string|null; }
export interface BillingExceptionPage { items: BillingException[]; page:{page:number;size:number;totalElements:number;totalPages:number}; }
export type BillingReviewRequest = { action:'VERIFY_READING'|'APPROVE'; reviewedBy:string; notes?:string } | { action:'REQUEST_FIELD_VISIT'; reviewedBy:string; fieldVisitRequest:FieldVisitRequest; notes?:string } | { action:'CORRECT_BILL'; reviewedBy:string; correction:{originalValue:number;correctedValue:number;reason:string;region:string}; notes?:string };
export interface AiCaseSummary { caseId:string; summary:string; classification:string; customerResponseDraft:string; generatedAt:string; model?:string; }
export interface AiCaseRecommendation { caseId:string; recommendation:string; rationale:string; generatedAt:string; advisory:true; model?:string; }
export interface AuditEvent { id:string; caseId:string; eventType:string; payloadHash:string; solanaSignature:string|null; status:'PENDING'|'SUBMITTED'|'VERIFIED'|'FAILED'; createdAt:string; }
