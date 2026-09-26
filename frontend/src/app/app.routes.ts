import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { LoginComponent } from './features/auth/login.component';
import { CatalogComponent } from './features/catalog/catalog.component';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'catalog' },
  { path: 'login', component: LoginComponent },
  { path: 'catalog', component: CatalogComponent, canActivate: [authGuard] },
  // Add one route + canActivate per feature (dashboard, sales, production, centro, materials,
  // finance) here as you build each module, following the 'catalog' route as the example.
  { path: '**', redirectTo: 'catalog' },
];
