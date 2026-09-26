import { ChangeDetectionStrategy, Component, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { NorthwindApiService } from '../../core/api/northwind-api.service';
import { BillingHistoryResponse, CaseContext, CaseEvent, MeterReadingsResponse } from '../../core/api/northwind-api.models';

@Component({ selector: 'app-case-workspace', standalone: true, imports: [DatePipe, RouterLink], changeDetection: ChangeDetectionStrategy.OnPush, template: `
  <a routerLink="/">Back to search</a>
  @if (loading()) { <p>Loading case workspace…</p> } @else if (error()) { <p role="alert">{{ error() }}</p> } @else { @if (context(); as workspace) {
    <section><h1>{{ workspace.case.caseId }}</h1><p>{{ workspace.case.status }} · {{ workspace.case.priority }} · {{ workspace.case.category }}</p><dl><dt>Account</dt><dd>{{ workspace.case.accountId }}</dd><dt>Region</dt><dd>{{ workspace.case.region }}</dd><dt>SLA days</dt><dd>{{ workspace.case.slaDays }}</dd><dt>Assigned team</dt><dd>{{ workspace.case.assignedTeam }}</dd></dl></section>
    <section><h2>Timeline</h2>@if (timeline().length) { <ol>@for (event of timeline(); track event.eventId) { <li><strong>{{ event.timestamp | date:'medium' }}</strong> — {{ event.description }} <small>({{ event.sourceSystem }})</small></li> }</ol> } @else { <p>No timeline events are available.</p> }</section>
    <section><h2>Billing</h2>@if (billing()?.bills?.length || billing()?.corrections?.length) { <p>Canonical billing records are available.</p> } @else { <p>No detailed invoice or canonical correction history is available from the supplied source data.</p> }</section>
    <section><h2>Meter readings</h2>@if (meter()?.readings?.length) { <p>Account meter readings are available.</p> } @else { <p>No account-level meter readings are available from the supplied source data.</p> }</section>
    <section><h2>Field visits</h2>@if (workspace.fieldVisits.length) { <ul>@for (visit of workspace.fieldVisits; track visit.id) { <li>{{ visit.status }} — {{ visit.scheduledAt | date:'medium' }}</li> }</ul> } @else { <p>No FieldForce visits have been recorded.</p> }</section>
  } }` })
export class CaseWorkspaceComponent implements OnInit {
  readonly loading = signal(true); readonly error = signal(''); readonly context = signal<CaseContext | null>(null); readonly timeline = signal<CaseEvent[]>([]); readonly billing = signal<BillingHistoryResponse | null>(null); readonly meter = signal<MeterReadingsResponse | null>(null);
  constructor(private readonly route: ActivatedRoute, private readonly api: NorthwindApiService) {}
  ngOnInit() { const caseId = this.route.snapshot.paramMap.get('caseId'); if (!caseId) { this.error.set('A Case ID is required.'); this.loading.set(false); return; } this.api.caseContext(caseId).subscribe({ next: context => { this.context.set(context); forkJoin({ timeline: this.api.timeline(caseId), billing: this.api.billingHistory(context.case.accountId), meter: this.api.meterReadings(context.case.accountId) }).subscribe({ next: result => { this.timeline.set(result.timeline.events); this.billing.set(result.billing); this.meter.set(result.meter); this.loading.set(false); }, error: () => { this.error.set('Case context could not be fully loaded. Try again.'); this.loading.set(false); } }); }, error: () => { this.error.set('Case not found or unavailable.'); this.loading.set(false); } }); }
}
