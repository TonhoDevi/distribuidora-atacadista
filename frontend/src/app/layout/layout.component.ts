import { Component, computed, inject } from '@angular/core';
import { BreakpointObserver } from '@angular/cdk/layout';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../features/auth/services/auth.service';
import { Role } from '../features/auth/models/auth.model';

interface NavItem {
  label: string;
  path: string;
  icon: string;
  roles?: Role[];
}

const NAV_ITEMS: NavItem[] = [
  { label: 'Painel', path: '/', icon: 'space_dashboard' },
  { label: 'Pedidos', path: '/orders', icon: 'local_shipping' },
  { label: 'Clientes', path: '/customers', icon: 'storefront' },
  { label: 'Produtos', path: '/products', icon: 'inventory_2' },
  { label: 'Usuários', path: '/users', icon: 'manage_accounts', roles: ['ADMIN'] },
];

const ROLE_LABELS: Record<Role, string> = {
  ADMIN: 'Administrador',
  GERENTE: 'Gerente',
  ANALISTA: 'Analista',
  CLIENTE: 'Cliente',
};

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [
    RouterLink,
    RouterLinkActive,
    RouterOutlet,
    MatSidenavModule,
    MatIconModule,
    MatButtonModule,
  ],
  templateUrl: './layout.component.html',
  styleUrl: './layout.component.scss',
})
export class LayoutComponent {
  // `authService` precisa ser inicializado (via inject()) ANTES de `navItems`,
  // porque field initializers rodam em ordem de declaração.
  authService = inject(AuthService);
  private router = inject(Router);
  private breakpoints = inject(BreakpointObserver);

  navItems = NAV_ITEMS.filter((item) => !item.roles || this.authService.hasRole(...item.roles));

  // Em telas estreitas o menu vira uma gaveta aberta por um botão.
  isNarrow = toSignal(
    this.breakpoints.observe('(max-width: 900px)').pipe(map((state) => state.matches)),
    { initialValue: false }
  );
  sidenavMode = computed(() => (this.isNarrow() ? 'over' : 'side'));

  roleLabel(): string {
    const role = this.authService.getRole();
    return role ? ROLE_LABELS[role] : '';
  }

  logout(): void {
    this.authService.logout().subscribe(() => this.router.navigate(['/login']));
  }
}
