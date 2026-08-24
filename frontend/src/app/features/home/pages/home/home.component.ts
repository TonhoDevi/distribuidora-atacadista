import { Component } from '@angular/core';
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
              <mat-icon>{{ card.icon }}</mat-icon>
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
      .card-content {
        display: flex;
        align-items: center;
        gap: 16px;
      }
      .card-content mat-icon {
        font-size: 36px;
        width: 36px;
        height: 36px;
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
  cards = CARDS.filter((card) => !card.roles || this.authService.hasRole(...card.roles));

  constructor(public authService: AuthService) {}
}
