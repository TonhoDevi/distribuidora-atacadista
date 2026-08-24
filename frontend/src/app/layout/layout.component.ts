import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
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
  { label: 'Dashboard', path: '/', icon: 'dashboard' },
  { label: 'Clientes', path: '/customers', icon: 'people' },
  { label: 'Produtos', path: '/products', icon: 'inventory_2' },
  { label: 'Pedidos', path: '/orders', icon: 'shopping_cart' },
  { label: 'Usuários', path: '/users', icon: 'admin_panel_settings', roles: ['ADMIN'] },
];

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [
    RouterLink,
    RouterLinkActive,
    RouterOutlet,
    MatToolbarModule,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
  ],
  templateUrl: './layout.component.html',
  styleUrl: './layout.component.scss',
})
export class LayoutComponent {
  // `authService` precisa ser inicializado (via inject()) ANTES de `navItems`,
  // porque field initializers rodam em ordem de declaração — se `authService`
  // viesse só de parâmetro de constructor, `navItems` tentaria usar `this.authService`
  // ainda undefined (mesmo bug de "used before initialization" que os forms tinham).
  authService = inject(AuthService);
  private router = inject(Router);

  navItems = NAV_ITEMS.filter((item) => !item.roles || this.authService.hasRole(...item.roles));

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
