import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { NorthwindApiService } from '../../core/api/northwind-api.service';
import { CaseDto } from '../../core/api/northwind-api.models';

@Component({ selector: 'app-case-search', standalone: true, imports: [FormsModule, RouterLink], changeDetection: ChangeDetectionStrategy.OnPush, template: `
  <section><h1>Case Workspace</h1><p>Search by Case ID or Account ID.</p>
  <form (ngSubmit)="search()"><label>Case ID <input name="caseId" [(ngModel)]="caseId" /></label><label>Account ID <input name="accountId" [(ngModel)]="accountId" /></label><button [disabled]="loading()">Search</button></form>
  @if (loading()) { <p>Loading cases…</p> } @else if (error()) { <p role="alert">{{ error() }}</p> } @else if (searched() && results().length === 0) { <p>No matching cases were found.</p> }
  @if (results().length) { <ul>@for (caseItem of results(); track caseItem.caseId) { <li><a [routerLink]="['/cases', caseItem.caseId]">{{ caseItem.caseId }}</a> — {{ caseItem.status }}</li> }</ul> }
  </section>` })
export class CaseSearchComponent {
  caseId = ''; accountId = ''; readonly loading = signal(false); readonly error = signal(''); readonly searched = signal(false); readonly results = signal<CaseDto[]>([]);
  constructor(private readonly api: NorthwindApiService, private readonly router: Router) {}
  search() { const caseId = this.caseId.trim(); const accountId = this.accountId.trim(); this.error.set(''); this.results.set([]); this.searched.set(true); if (caseId) { void this.router.navigate(['/cases', caseId]); return; } if (!accountId) { this.error.set('Enter a Case ID or Account ID.'); return; } this.loading.set(true); this.api.searchByAccount(accountId).subscribe({ next: response => { this.results.set(response.items); this.loading.set(false); }, error: () => { this.error.set('Case search is unavailable. Try again.'); this.loading.set(false); } }); }
}
