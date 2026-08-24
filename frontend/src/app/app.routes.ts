import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/pages/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: '',
    canActivate: [authGuard],
    // Placeholder: quando Customers/Products/Orders existirem, isto vira
    // um layout com rotas filhas lazy-loaded por feature, ex:
    // { path: 'customers', loadChildren: () => import('./features/customers/customers.routes')... }
    loadComponent: () =>
      import('./features/home/pages/home/home.component').then((m) => m.HomeComponent),
  },
  { path: '**', redirectTo: 'login' },
];
