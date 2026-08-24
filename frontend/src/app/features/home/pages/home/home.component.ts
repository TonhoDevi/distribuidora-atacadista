import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../auth/services/auth.service';
import { Role } from '../../../auth/models/auth.model';

interface DashboardCard {
  title: string;
  description: string;
  path: string;
  icon: string;
  roles?: Role[];
}

const CARDS: DashboardCard[] = [
  { title: 'Clientes', description: 'Cadastro de clientes', path: '/customers', icon: 'people' },
  { title: 'Produtos', description: 'Catálogo de produtos', path: '/products', icon: 'inventory_2' },
  { title: 'Pedidos', description: 'Criação e consulta de pedidos', path: '/orders', icon: 'shopping_cart' },
  {
    title: 'Usuários',
    description: 'Gestão de usuários do sistema',
    path: '/users',
    icon: 'admin_panel_settings',
    roles: ['ADMIN'],
  },
];

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink, MatCardModule, MatIconModule],
  template: `
    <h1>Bem-vindo, {{ authService.getUsername() }}</h1>
    <div class="cards-grid">
      @for (card of cards; track card.path) {
        <a [routerLink]="card.path" class="card-link">
          <mat-card>
            <mat-card-content class="card-content">
              <div class="card-icon">
                <mat-icon>{{ card.icon }}</mat-icon>
              </div>
              <div>
                <h3>{{ card.title }}</h3>
                <p>{{ card.description }}</p>
              </div>
            </mat-card-content>
          </mat-card>
        </a>
      }
    </div>
  `,
  styles: [
    `
      .cards-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
        gap: 16px;
        margin-top: 16px;
      }
      .card-link {
        text-decoration: none;
        color: inherit;
      }
      .card-link mat-card {
        transition: transform 0.15s ease, box-shadow 0.15s ease;
      }
      .card-link:hover mat-card {
        transform: translateY(-3px);
        box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
      }
      .card-content {
        display: flex;
        align-items: center;
        gap: 16px;
      }
      .card-icon {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 48px;
        height: 48px;
        min-width: 48px;
        border-radius: 50%;
        background: var(--mat-sys-primary-container);
        color: var(--mat-sys-on-primary-container);
      }
      .card-content h3 {
        margin: 0;
      }
      .card-content p {
        margin: 4px 0 0;
        color: var(--mat-sys-on-surface-variant);
      }
    `,
  ],
})
export class HomeComponent {
  // Mesmo motivo do LayoutComponent: `authService` precisa existir antes de
  // `cards` rodar seu filter (field initializers executam em ordem de declaração).
  authService = inject(AuthService);

  cards = CARDS.filter((card) => !card.roles || this.authService.hasRole(...card.roles));
}
