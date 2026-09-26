import { Routes } from '@angular/router';
import { CaseSearchComponent } from './pages/case-search/case-search.component';
import { CaseWorkspaceComponent } from './pages/case-workspace/case-workspace.component';

export const routes: Routes = [
  { path: '', component: CaseSearchComponent },
  { path: 'cases/:caseId', component: CaseWorkspaceComponent },
  { path: '**', redirectTo: '' }
];
