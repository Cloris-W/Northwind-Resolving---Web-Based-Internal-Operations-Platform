import { Routes } from '@angular/router';
import { CaseSearchComponent } from './pages/case-search/case-search.component';
import { CaseWorkspaceComponent } from './pages/case-workspace/case-workspace.component';
import { BillingQualityComponent } from './pages/billing-quality/billing-quality.component';
import { BillingExceptionDetailComponent } from './pages/billing-exception-detail/billing-exception-detail.component';
import { AuditComponent } from './pages/audit/audit.component';

export const routes: Routes = [
  { path: '', component: CaseSearchComponent },
  { path: 'cases/:caseId', component: CaseWorkspaceComponent },
  { path: 'billing/exceptions', component: BillingQualityComponent },
  { path: 'billing/exceptions/:id', component: BillingExceptionDetailComponent },
  { path: 'audit', component: AuditComponent },
  { path: '**', redirectTo: '' }
];
